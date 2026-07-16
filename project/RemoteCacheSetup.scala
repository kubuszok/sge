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

  /** Value for `Global / remoteCache`. `None` (cache off) unless a key was resolved. */
  val endpoint: Option[URI] =
    apiKey.map(_ => uri("grpcs://remote.buildbuddy.io"))

  /** Values for `Global / remoteCacheHeaders`: the BuildBuddy auth header, when enabled. */
  val headers: Seq[String] =
    apiKey.map(key => s"x-buildbuddy-api-key=$key").toSeq

  /** Human-readable status for logs — never includes the key. */
  val status: String =
    if (forcedOff) "remote cache OFF (SGE_REMOTE_CACHE override)"
    else if (keyFromEnv.isDefined) "remote cache ON (BuildBuddy, key from BUILDBUDDY_API_KEY env)"
    else if (keyFromFile.isDefined) "remote cache ON (BuildBuddy, key from ~/.config/sge/buildbuddy-api-key)"
    else "remote cache OFF (no BuildBuddy API key found)"
}
