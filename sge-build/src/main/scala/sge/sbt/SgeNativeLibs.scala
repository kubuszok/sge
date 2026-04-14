package sge.sbt

import multiarch.sbt.Platform

import sbt._
import sbt.Keys._

/** SGE-specific native library validation and settings.
  *
  * Library extraction, JAR scanning, and linker flag merging are handled by
  * `NativeProviderPlugin` from `sbt-multiarch-scala`. This object provides:
  *   - JVM JAR validation (ensuring native shared libs are present before packaging)
  *   - The `sgeNativeLibDir` key for local development overrides
  */
object SgeNativeLibs {

  // ── JVM JAR native lib validation ──────────────────────────────────

  val sgeValidateNativeLibs = taskKey[Unit](
    "Validate native shared libraries are present for JVM JAR packaging"
  )

  /** Validation settings for the sge JVM axis. Checks that native shared libraries
    * are present in the `packageBin` mappings before packaging.
    */
  lazy val validationSettings: Seq[Setting[_]] = Seq(
    sgeValidateNativeLibs := {
      val log         = streams.value.log
      val jarMappings = (Compile / packageBin / mappings).value
      val isCI        = sys.env.get("CI").contains("true")
      val host        = Platform.host

      // Collect which platform/lib combos are present
      val nativeMappings = jarMappings.collect {
        case (_, path) if path.startsWith("native/") => path
      }

      if (nativeMappings.isEmpty) {
        val msg = "[sge] WARNING: No native shared libraries found in JAR mappings.\n" +
          "  Native libs come from sge-native-providers provider JARs on Maven.\n" +
          "  In CI, ensure the matching pnm-provider-sge-* artifacts are downloaded\n" +
          "  to sge-deps/native-components/target/release/ before packaging."
        val skipValidation = sys.env.get("SGE_SKIP_NATIVE_VALIDATION").contains("true")
        if (isCI && !skipValidation) sys.error(msg) else log.warn(msg)
      } else {
        // Group by platform
        val byPlatform = nativeMappings.groupBy { path =>
          val parts = path.stripPrefix("native/").split('/')
          if (parts.length >= 2) parts(0) else "unknown"
        }

        val skipValidation = sys.env.get("SGE_SKIP_NATIVE_VALIDATION").contains("true")
        if (!skipValidation && !byPlatform.contains(host.classifier)) {
          sys.error(
            s"[sge] Native libraries missing for host platform '${host.classifier}'.\n" +
              s"  Native libs come from sge-native-providers provider JARs on Maven —\n" +
              s"  ensure the matching pnm-provider-sge-* artifacts are present in\n" +
              s"  sge-deps/native-components/target/release/.\n" +
              s"  Present platforms: ${byPlatform.keys.mkString(", ")}"
          )
        }

        if (isCI && !skipValidation) {
          val missing = Platform.desktop.filterNot(p => byPlatform.contains(p.classifier))
          if (missing.nonEmpty) {
            sys.error(
              s"[sge] CI: Native libraries missing for platforms: ${missing.map(_.classifier).mkString(", ")}\n" +
                s"  Present platforms: ${byPlatform.keys.mkString(", ")}\n" +
                "  Ensure all 6 pnm-provider-sge-* artifacts are downloaded from\n" +
                "  the sge-native-providers Maven publication before packaging."
            )
          }
        } else {
          val missing = Platform.desktop.filterNot(p => byPlatform.contains(p.classifier))
          if (missing.nonEmpty) {
            log.warn(
              s"[sge] Native libraries missing for non-host platforms: ${missing.map(_.classifier).mkString(", ")}. " +
                "This is OK for local development; CI will require all platforms."
            )
          }
        }

        log.info(s"[sge] Native lib validation passed. Platforms: ${byPlatform.keys.mkString(", ")}")
        byPlatform.foreach { case (plat, libs) =>
          log.info(s"[sge]   $plat: ${libs.map(_.split('/').last).mkString(", ")}")
        }
      }
    },
    Compile / packageBin := ((Compile / packageBin) dependsOn sgeValidateNativeLibs).value
  )
}
