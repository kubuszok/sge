package sge.sbt

import multiarch.sbt.Platform

import sbt._
import sbt.Keys._

/** SGE-specific native library validation and settings.
  *
  * Library extraction, JAR scanning, and linker flag merging are handled by `NativeProviderPlugin` from `sbt-multiarch-scala`. This object provides:
  *   - Release-time validation that the resolved `pnm-provider-sge-*` dependency actually carries every native binary its manifest promises, for every desktop platform (ISS-484)
  *   - The `sgeNativeLibDir` key for local development overrides
  *
  * Why the resolved dependency and not the sge JAR's own `native/` mappings: the desktop native libs are delivered to users through the `pnm-provider-sge-desktop` JAR, which sge declares as a POM dependency. A release could bundle (or extract) libs into the sge JAR and still ship a POM that omits the provider — leaving users with no natives. So the gate validates the dependency users actually resolve. It is invoked explicitly before the Sonatype publish (`sbt "sge / sgeValidateNativeLibs" ci-release` in release.yml) rather than auto-wired onto `makePom`/`publishLocal`, so local/demo `publishLocal` in CI is not blocked by a runtime-only provider gap — only the actual release is gated.
  *
  * Covenant: full-port Covenant-baseline-spec-pass: 0 Covenant-baseline-loc: 91 Covenant-baseline-methods: SgeNativeLibs,sgeValidateNativeLibs,validationSettings Covenant-source-reference:
  * SGE-original Covenant-verified: 2026-04-19
  */
object SgeNativeLibs {

  // ── pnm-provider-sge-* dependency validation ───────────────────────

  val sgeValidateNativeLibs = taskKey[Unit](
    "Validate the resolved pnm-provider-sge-* dependency carries every native binary its manifest promises, for all desktop platforms"
  )

  /** Minimum size (bytes) for a provider binary to count as a real shared library rather than an undersized non-functional one. The sge desktop natives (glfw/audio/native_ops) are hundreds of KB on every platform; the known Rosetta no-op binaries are ~16 KB, so 32 KB cleanly separates them. */
  private val MinLibBytes: Long = 32L * 1024L

  /** Read a provider JAR's `pnm-provider.json` content (empty string if absent) and the uncompressed size of every `native/...` entry. */
  private def readProviderJar(jar: File): (String, Map[String, Long]) = {
    val zf = new java.util.zip.ZipFile(jar)
    try {
      val sizes    = scala.collection.mutable.Map.empty[String, Long]
      var manifest = ""
      val entries  = zf.entries()
      while (entries.hasMoreElements) {
        val e = entries.nextElement()
        if (!e.isDirectory) {
          if (e.getName == "pnm-provider.json") {
            val is = zf.getInputStream(e)
            try manifest = scala.io.Source.fromInputStream(is, "UTF-8").mkString
            finally is.close()
          } else if (e.getName.startsWith("native/")) {
            sizes(e.getName) = e.getSize
          }
        }
      }
      (manifest, sizes.toMap)
    } finally zf.close()
  }

  /** Validation settings for the sge JVM axis. Defines the `sgeValidateNativeLibs` task; release.yml invokes it explicitly before `ci-release` so a failure blocks the Sonatype publish (sbt runs commands sequentially and stops on the first failure).
    */
  lazy val validationSettings: Seq[Setting[_]] = Seq(
    sgeValidateNativeLibs := {
      val log      = streams.value.log
      val report   = update.value
      val required = Platform.desktop.map(_.classifier).toSet

      val providerFiles = report
        .matching(moduleFilter(organization = "com.kubuszok", name = "pnm-provider-sge*"))
        .filter(_.getName.endsWith(".jar"))

      if (providerFiles.isEmpty) {
        sys.error(
          "[sge] No pnm-provider-sge-* dependency resolved for the sge JVM artifact.\n" +
            "  Native shared libraries are delivered to users via that provider JAR,\n" +
            "  declared as a dependency in the published POM. Without it, anything that\n" +
            "  resolves `sge` gets no natives. Ensure pnm-provider-sge-desktop is a\n" +
            "  declared dependency before publishing."
        )
      }

      val violations = providerFiles.flatMap { jar =>
        val (manifest, sizes) = readProviderJar(jar)
        NativeProviderValidation.violations(jar.getName, manifest, sizes, required, MinLibBytes)
      }

      if (violations.nonEmpty) {
        sys.error(
          "[sge] Native-lib release validation failed — the resolved provider JAR(s) do not\n" +
            "  carry every native binary their manifest promises for all desktop platforms:\n" +
            violations.map("    - " + _).mkString("\n")
        )
      }

      log.info(
        s"[sge] Native-lib validation passed for ${providerFiles.map(_.getName).mkString(", ")} " +
          s"(platforms: ${required.toList.sorted.mkString(", ")})."
      )
    }
  )
}
