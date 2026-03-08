package sge.sbt

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

  val sgeNativeLibPlatform = settingKey[String](
    "Target platform for native library extraction (e.g. 'macos-aarch64'). " +
      "Defaults to the host platform."
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
      platform: String,
      log: sbt.util.Logger
  ): Option[File] = {
    // Look for a JAR whose name matches the platform classifier pattern
    val candidates = classpath.map(_.data).filter { jar =>
      val name = jar.getName
      name.startsWith("sge-native-libs") && name.contains(platform)
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
  private def localPlatformDir(crossDir: File, platform: String): File =
    crossDir / platform

  // ── Settings ──────────────────────────────────────────────────────

  /** Settings for projects that consume SGE as a library dependency.
    * Resolves native libraries from the sge-native-libs classifier JAR.
    */
  lazy val settings: Seq[Setting[_]] = Seq(
    sgeNativeLibPlatform := SgePlugin.hostPlatform,
    sgeNativeLibSourceDir := None,
    sgeNativeLibExtract := {
      val log      = streams.value.log
      val platform = sgeNativeLibPlatform.value
      val outDir   = target.value / "sge-native-libs" / platform
      val cp       = (Compile / dependencyClasspathAsJars).value

      sgeNativeLibSourceDir.value match {
        case Some(crossDir) =>
          // Local mode: use cross-compilation output directly
          val platDir = localPlatformDir(crossDir, platform)
          if (!platDir.exists()) {
            sys.error(
              s"[sge] Native lib directory not found: ${platDir.getAbsolutePath}\n" +
                s"Run 'just rust-cross $platform && just rust-collect' to build it."
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
        case None           => target.value / "sge-native-libs" / sgeNativeLibPlatform.value
      }
    }
  )

  /** Settings for SGE's own build, pointing at the local Rust build output. */
  def localSettings(crossDir: Option[File] = None): Seq[Setting[_]] =
    settings ++ Seq(
      sgeNativeLibSourceDir := Some(crossDir.getOrElse {
        (ThisBuild / baseDirectory).value / "native-components" / "target" / "cross"
      })
    )

  /** Settings for SGE's own build, pointing at the default release directory (host platform only). */
  lazy val hostSettings: Seq[Setting[_]] =
    settings ++ Seq(
      sgeNativeLibSourceDir := None,
      sgeNativeLibDir := (ThisBuild / baseDirectory).value / "native-components" / "target" / "release"
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

  /** Linker flags snippet for use in nativeConfig. */
  def linkerFlags(libDir: File, libName: String = "sge_native_ops"): Seq[String] = {
    val isWindows = System.getProperty("os.name", "").toLowerCase.contains("win")
    val base = Seq(s"-L${libDir.getAbsolutePath}", s"-l$libName")
    if (isWindows) base :+ "-lntdll" else base
  }
}
