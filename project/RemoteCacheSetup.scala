import java.net.URI

import sbt._

/** BuildBuddy remote-cache wiring for the sbt 2 Bazel-compatible gRPC cache (ISS-792).
  *
  * sbt 2 speaks the Bazel remote-cache protocol natively (via `addRemoteCachePlugin` in project/plugins.sbt); the endpoint + auth header settings consumed in build.sbt are `Global / remoteCache` and
  * `Global / remoteCacheHeaders` (https://www.scala-sbt.org/2.x/docs/en/reference/remote-cache-setup.html). BuildBuddy authenticates via the `x-buildbuddy-api-key` gRPC header
  * (https://www.buildbuddy.io/docs/guide-auth/).
  *
  * Key resolution order (first non-empty wins):
  *   1. env var `BUILDBUDDY_API_KEY` (CI: org-level Actions secret; empty on fork PRs)
  *   2. file `~/.config/sge/buildbuddy-api-key` (local dev; NOT committed, NOT in the repo)
  *
  * When no key is found the remote cache stays OFF (`remoteCache = None`) and the build behaves exactly as before — contributors and CI forks need no setup. Set `SGE_REMOTE_CACHE=0` (or
  * `off`/`false`) to force-disable even when a key is present (useful for debugging cache behaviour). The key is only ever placed in the in-memory gRPC header value — never logged and never written
  * to disk by this wiring, and `show Global / remoteCache` prints only the endpoint. (Note `remoteCacheHeaders` necessarily contains the key: avoid `show Global / remoteCacheHeaders` in logs.)
  *
  * Read/write policy: BuildBuddy's org API key is read-write, and every environment that has it (local dev, master CI, same-repo PR CI) both reads and writes the cache. Fork PRs have no secret, so
  * they cannot read or write. See docs/reviews/ci-cache-investigation-2026-07-16.md for the measurements behind this.
  *
  * Sharing domain: entries are namespaced per (ci|dev) × os × arch via the Bazel instance name (the URI path — see `instanceName` below), so machine-specific task outputs can never poison a different
  * OS/arch or cross the dev↔CI boundary (CI run 29498350131 post-mortem).
  */
object RemoteCacheSetup {

  private val forcedOff: Boolean =
    sys.env.get("SGE_REMOTE_CACHE").map(_.trim.toLowerCase).exists(v => v == "0" || v == "off" || v == "false")

  private val keyFromEnv: Option[String] =
    sys.env.get("BUILDBUDDY_API_KEY").map(_.trim).filter(_.nonEmpty)

  private val keyFile: File =
    new File(new File(sys.props("user.home")), ".config/sge/buildbuddy-api-key")

  private val keyFromFile: Option[String] =
    if (keyFile.isFile) Some(IO.read(keyFile).trim).filter(_.nonEmpty) else None

  /** The resolved API key, if any. Deliberately private: nothing outside the two derived values below should ever see (let alone print) it.
    */
  private val apiKey: Option[String] =
    if (forcedOff) None else keyFromEnv.orElse(keyFromFile)

  // ── Cache namespacing (ISS-792 cross-OS poisoning fix) ─────────────────
  // sbt 2.0.2's gRPC client turns the endpoint URI's PATH into the Bazel
  // `instance_name` sent with every ActionCache/CAS request (verified in
  // sbt-remote-cache/src/main/scala/sbt/internal/GrpcActionCacheStore.scala,
  // `apply`: `uri.getPath()` → `setInstanceName`), and BuildBuddy partitions
  // its cache by instance name (Bazel's --remote_instance_name concept).
  //
  // A single shared namespace poisoned consumers in CI run 29498350131:
  // machine-specific task outputs (Scala Native's discovered clang path,
  // dev-Mac-seeded compile outputs feeding scaladoc) were restored onto
  // other OSes/machines — e.g. linux's /usr/bin/clang virtualized to
  // 'D:\usr\bin\clang' on the windows leg. So the cache is shared only
  // within (ci|dev) × os × arch:
  //   * ci vs dev: CI runner images are homogeneous per OS; dev machines
  //     are not (and a dev seed did break CI's ubuntu docs job).
  //   * os/arch: linux-x64 CI jobs (compile-gate + the whole fan-out — the
  //     main win) still share; windows/macos legs each get their own
  //     namespace and can never see unix-seeded machine-specific outputs.
  // Rosetta legs isolate for free: an x64 JVM on ARM macOS reports x64.
  private val osFamily: String = {
    val os = sys.props.getOrElse("os.name", "unknown").toLowerCase
    if (os.contains("win")) "windows"
    else if (os.contains("mac") || os.contains("darwin")) "macos"
    else "linux"
  }

  private val osArch: String = sys.props.getOrElse("os.arch", "unknown").toLowerCase match {
    case "amd64" | "x86_64"  => "x64"
    case "aarch64" | "arm64" => "aarch64"
    case other               => other
  }

  private val context: String = if (sys.env.contains("CI")) "ci" else "dev"

  /** Bazel remote-cache instance name: the cache-sharing domain. */
  val instanceName: String = s"sge-$context-$osFamily-$osArch"

  /** Value for `Global / remoteCache`. `None` (cache off) unless a key was resolved. */
  val endpoint: Option[URI] =
    apiKey.map(_ => uri(s"grpcs://remote.buildbuddy.io/$instanceName"))

  /** Values for `Global / remoteCacheHeaders`: the BuildBuddy auth header, when enabled. */
  val headers: Seq[String] =
    apiKey.map(key => s"x-buildbuddy-api-key=$key").toSeq

  /** Human-readable status for logs — never includes the key. */
  val status: String =
    if (forcedOff) "remote cache OFF (SGE_REMOTE_CACHE override)"
    else if (keyFromEnv.isDefined) s"remote cache ON (BuildBuddy, instance '$instanceName', key from BUILDBUDDY_API_KEY env)"
    else if (keyFromFile.isDefined) s"remote cache ON (BuildBuddy, instance '$instanceName', key from ~/.config/sge/buildbuddy-api-key)"
    else "remote cache OFF (no BuildBuddy API key found)"
}
