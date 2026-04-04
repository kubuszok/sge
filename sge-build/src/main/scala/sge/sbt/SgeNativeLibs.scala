package sge.sbt

import multiarch.sbt.Platform

import sbt._
import sbt.Keys._

import java.nio.file.{Files, StandardCopyOption}
import java.util.jar.JarFile
import scala.collection.JavaConverters._

/** Native library bundling and extraction for Scala Native builds.
  *
  * Scala Native (as of 0.5.x) has no built-in mechanism for distributing
  * platform-specific native libraries (`.a`, `.so`, `.dylib`, `.dll`) through
  * library dependencies (see scala-native/scala-native#4800). This module
  * provides a workaround:
  *
  * '''Publishing side''' (SGE itself): Cross-compiled static libraries are
  * packaged into a platform-classifier JAR:
  * {{{
  * sge-native-libs_macos-aarch64.jar
  * └── native/
  *     ├── libsge_native_ops.a
  *     ├── libsge_native_ops.dylib  (optional, for JVM)
  *     └── ...
  * }}}
  *
  * '''Consumer side''' (game projects):
  * {{{
  * .settings(SgeNativeLibs.settings *)
  * .nativePlatform(scalaVersions = ..., settings = Seq(
  *   nativeConfig := {
  *     val c       = nativeConfig.value
  *     val libDir  = SgeNativeLibs.sgeNativeLibDir.value
  *     c.withLinkingOptions(c.linkingOptions ++ SgeNativeLibs.linkerFlags(libDir))
  *   }
  * ))
  * }}}
  *
  * For '''local development''' (SGE's own build), use `SgeNativeLibs.localSettings`
  * which points directly at the Rust build output without downloading anything.
  */
object SgeNativeLibs {

  // ── Keys ──────────────────────────────────────────────────────────

  val sgeNativeLibDir = settingKey[File](
    "Directory containing platform-specific native libraries for Scala Native linking"
  )

  val sgeNativeLibPlatform = settingKey[Platform](
    "Target platform for native library extraction. Defaults to the host platform."
  )

  val sgeNativeLibExtract = taskKey[File](
    "Extract native libraries from the sge-native-libs JAR for the target platform. " +
      "Returns the directory containing the extracted libraries."
  )

  val sgeNativeLibSourceDir = settingKey[Option[File]](
    "Local directory containing cross-compiled native libraries (for SGE's own build). " +
      "When set, bypasses JAR extraction and uses this directory directly."
  )

  // ── Library file patterns ─────────────────────────────────────────

  private val NativeLibExts = Set(".a", ".so", ".dylib", ".dll", ".lib")

  private def isNativeLib(name: String): Boolean =
    NativeLibExts.exists(name.endsWith)

  // ── JAR extraction ────────────────────────────────────────────────

  /** Extract native libraries from a JAR file.
    * Looks for entries under `native/` and copies them to the output directory.
    */
  private def extractFromJar(jar: File, outDir: File, log: sbt.util.Logger): Unit = {
    IO.createDirectory(outDir)
    val jarFile = new JarFile(jar)
    try {
      jarFile.entries().asScala
        .filter(e => !e.isDirectory && e.getName.startsWith("native/"))
        .foreach { entry =>
          val fileName = entry.getName.stripPrefix("native/")
          if (fileName.nonEmpty && isNativeLib(fileName)) {
            val target = outDir / fileName
            val is = jarFile.getInputStream(entry)
            try Files.copy(is, target.toPath, StandardCopyOption.REPLACE_EXISTING)
            finally is.close()
            log.info(s"[sge] Extracted native lib: $fileName")
          }
        }
    } finally jarFile.close()
  }

  /** Find the sge-native-libs JAR for the given platform in the classpath. */
  private def findNativeLibsJar(
      classpath: Seq[Attributed[File]],
      platform: Platform,
      log: sbt.util.Logger
  ): Option[File] = {
    // Look for a JAR whose name matches the platform classifier pattern
    val candidates = classpath.map(_.data).filter { jar =>
      val name = jar.getName
      name.startsWith("sge-native-libs") && name.contains(platform.classifier)
    }
    candidates.headOption.orElse {
      // Fallback: look for any sge-native-libs JAR and check its contents
      classpath.map(_.data).find { jar =>
        val name = jar.getName
        name.startsWith("sge-native-libs") && jar.isFile && {
          val jf = new JarFile(jar)
          try jf.entries().asScala.exists(e => e.getName.startsWith("native/") && isNativeLib(e.getName))
          finally jf.close()
        }
      }
    }
  }

  // ── Local directory lookup ────────────────────────────────────────

  /** Find platform-specific libraries in a local cross-compilation output directory.
    *
    * Expected layout:
    * {{{
    * <crossDir>/
    * ├── linux-x86_64/
    * │   └── libsge_native_ops.a
    * ├── macos-aarch64/
    * │   └── libsge_native_ops.a
    * └── ...
    * }}}
    */
  private def localPlatformDir(crossDir: File, platform: Platform): File =
    crossDir / platform.classifier

  // ── Settings ──────────────────────────────────────────────────────

  /** Settings for projects that consume SGE as a library dependency.
    * Resolves native libraries from the sge-native-libs classifier JAR.
    */
  lazy val settings: Seq[Setting[_]] = Seq(
    sgeNativeLibPlatform := Platform.host,
    sgeNativeLibSourceDir := None,
    sgeNativeLibExtract := {
      val log      = streams.value.log
      val platform = sgeNativeLibPlatform.value
      val outDir   = target.value / "sge-native-libs" / platform.classifier
      val cp       = (Compile / dependencyClasspathAsJars).value

      sgeNativeLibSourceDir.value match {
        case Some(crossDir) =>
          // Local mode: use cross-compilation output directly
          val platDir = localPlatformDir(crossDir, platform)
          if (!platDir.exists()) {
            sys.error(
              s"[sge] Native lib directory not found: ${platDir.getAbsolutePath}\n" +
                s"Run 'sge-dev native cross $platform && sge-dev native collect' to build it."
            )
          }
          log.info(s"[sge] Using local native libs: ${platDir.getAbsolutePath}")
          platDir

        case None =>
          // JAR mode: extract from dependency
          if (outDir.exists() && IO.listFiles(outDir).exists(f => isNativeLib(f.getName))) {
            log.info(s"[sge] Using cached native libs: ${outDir.getAbsolutePath}")
            outDir
          } else {
            findNativeLibsJar(cp, platform, log) match {
              case Some(jar) =>
                log.info(s"[sge] Extracting native libs from ${jar.getName}...")
                IO.delete(outDir)
                extractFromJar(jar, outDir, log)
                outDir
              case None =>
                sys.error(
                  s"[sge] Could not find sge-native-libs JAR for platform '$platform' " +
                    "in the classpath. Either:\n" +
                    "  1. Add sge-native-libs as a dependency, or\n" +
                    "  2. Set SgeNativeLibs.sgeNativeLibSourceDir to a local cross-compilation directory."
                )
            }
          }
      }
    },
    sgeNativeLibDir := {
      sgeNativeLibSourceDir.value match {
        case Some(crossDir) => localPlatformDir(crossDir, sgeNativeLibPlatform.value)
        case None           => target.value / "sge-native-libs" / sgeNativeLibPlatform.value.classifier
      }
    }
  )

  /** Settings for SGE's own build, pointing at the cross-compiled native lib staging directory.
    *
    * Native libs are built externally in sge-native-components and distributed as provider JARs.
    * CI extracts provider JARs to sge-deps/native-components/target/cross/ for packaging.
    * For local dev, clone sge-native-components alongside sge and the path resolves the same way.
    */
  def localSettings(crossDir: Option[File] = None): Seq[Setting[_]] =
    settings ++ Seq(
      sgeNativeLibSourceDir := Some(crossDir.getOrElse {
        (ThisBuild / baseDirectory).value / "sge-deps" / "native-components" / "target" / "cross"
      })
    )

  /** Settings for SGE's own build, pointing at the default release directory (host platform only).
    *
    * Native libs are built externally in sge-native-components and distributed as provider JARs.
    * CI extracts provider JARs to sge-deps/native-components/target/release/ for linking and testing.
    */
  lazy val hostSettings: Seq[Setting[_]] =
    settings ++ Seq(
      sgeNativeLibSourceDir := None,
      sgeNativeLibDir := (ThisBuild / baseDirectory).value / "sge-deps" / "native-components" / "target" / "release"
    )

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
          "  Run 'sge-dev native build && sge-dev native angle setup' to build native libs for the host platform,\n" +
          "  or 'sge-dev native release-prep' for all platforms."
        val skipValidation = sys.env.get("SGE_SKIP_NATIVE_VALIDATION").contains("true")
        if (isCI && !skipValidation) sys.error(msg) else log.warn(msg)
      } else {
        // Group by platform
        val byPlatform = nativeMappings.groupBy { path =>
          val parts = path.stripPrefix("native/").split('/')
          if (parts.length >= 2) parts(0) else "unknown"
        }

        // Check host platform has libs (skip when SGE_SKIP_NATIVE_VALIDATION is set,
        // e.g. Android CI job that only has Android native libs, not desktop).
        val skipValidation = sys.env.get("SGE_SKIP_NATIVE_VALIDATION").contains("true")
        if (!skipValidation && !byPlatform.contains(host.classifier)) {
          sys.error(
            s"[sge] Native libraries missing for host platform '${host.classifier}'.\n" +
              s"  Run 'sge-dev native build && sge-dev native angle setup' to build them.\n" +
              s"  Present platforms: ${byPlatform.keys.mkString(", ")}"
          )
        }

        // In CI, all 6 platforms should be present (unless validation skipped)
        if (isCI && !skipValidation) {
          val missing = Platform.desktop.filterNot(p => byPlatform.contains(p.classifier))
          if (missing.nonEmpty) {
            sys.error(
              s"[sge] CI: Native libraries missing for platforms: ${missing.map(_.classifier).mkString(", ")}\n" +
                s"  Present platforms: ${byPlatform.keys.mkString(", ")}\n" +
                "  Run 'sge-dev native release-prep' to build all platforms."
            )
          }
        } else {
          // Local: warn about missing non-host platforms
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

  // ── JAR packaging helpers ─────────────────────────────────────────

  /** Create resource mappings for packaging native libraries into a JAR.
    *
    * Usage in build.sbt:
    * {{{
    * Compile / packageBin / mappings ++= SgeNativeLibs.jarMappings(
    *   baseDirectory.value / "native-components" / "target" / "cross" / platform
    * )
    * }}}
    */
  def jarMappings(nativeDir: File): Seq[(File, String)] = {
    if (!nativeDir.exists()) Seq.empty
    else {
      IO.listFiles(nativeDir)
        .filter(f => f.isFile && isNativeLib(f.getName))
        .map(f => f -> s"native/${f.getName}")
        .toSeq
    }
  }

  /** Additional linker flags not covered by native-bundle.json manifests.
    *
    * Library linking (-l flags, full paths, platform frameworks) is now handled
    * by the NativeLibBundle manifest system from sbt-multi-arch-release.
    * This method provides rpath entries for shared library resolution at runtime.
    */
  def linkerFlags(
      libDir: File,
      platform: Platform = Platform.host
  ): Seq[String] =
    if (platform.isMac) {
      // rpath entries so the binary can find ANGLE dylibs at runtime.
      // @rpath/libGLESv2.dylib is embedded in the ANGLE install names.
      // - libDir: for `sbt run` / local dev (ANGLE lives next to Rust libs)
      // - @executable_path: for packaged distributions (libs beside the binary)
      Seq(
        "-rpath", libDir.getAbsolutePath,
        "-rpath", "@executable_path"
      )
    } else if (platform.isLinux) {
      // rpath entries so the binary can find ANGLE .so files at runtime.
      // Without this, users would need to set LD_LIBRARY_PATH manually.
      // - libDir: for `sbt run` / local dev
      // - $ORIGIN: for packaged distributions (libs beside the binary)
      Seq(
        s"-Wl,-rpath,${libDir.getAbsolutePath}",
        "-Wl,-rpath,$ORIGIN"
      )
    } else Seq.empty
}
