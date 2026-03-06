package sge.sbt

import sbt._
import sbt.Keys._

/** Shared SGE build settings for cross-platform Scala 3 game projects (JVM/JS/Native).
  *
  * This object provides common compiler flags, dependency management, and platform-specific
  * source directory wiring. It works both:
  *   - As a published sbt plugin (external projects add `addSbtPlugin(...)`)
  *   - As local shared source (SGE itself adds `sbt-sge/src/main/scala` to the meta-build)
  *
  * === Local self-hosting (in SGE's own `project/build.sbt`) ===
  * {{{
  * Compile / unmanagedSourceDirectories +=
  *   baseDirectory.value / ".." / "sbt-sge" / "src" / "main" / "scala"
  * }}}
  *
  * === External usage (in a game project's `build.sbt`) ===
  * {{{
  * lazy val game = (projectMatrix in file("game"))
  *   .settings(SgePlugin.commonSettings *)
  *   .dependsOn(...)
  *   .jvmPlatform(scalaVersions = Seq(SgePlugin.scalaVersion),
  *     settings = SgePlugin.jvmSettings() *)
  *   .jsPlatform(scalaVersions = Seq(SgePlugin.scalaVersion),
  *     settings = SgePlugin.jsSettings *)
  *   .nativePlatform(scalaVersions = Seq(SgePlugin.scalaVersion),
  *     settings = SgePlugin.nativeSettings() *)
  * }}}
  */
object SgePlugin {

  /** Scala version used by SGE. All modules must use this exact version. */
  val scalaVersion = "3.8.2"

  // ── Compiler flags ──────────────────────────────────────────────────

  /** Scala 3 compiler flags matching SGE conventions: braces required, -Werror, unused warnings. */
  val commonSettings: Seq[Setting[_]] = Seq(
    Keys.scalaVersion := SgePlugin.scalaVersion,
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-no-indent",
      "-rewrite",
      "-Werror",
      "-Wimplausible-patterns",
      "-Wrecurse-with-default",
      "-Wenum-comment-discard",
      "-Wunused:imports,privates,locals,patvars,nowarn",
      "-Wconf:cat=deprecation:info"
    )
  )

  /** Relaxed compiler flags for demo/example projects (disables unused warnings). */
  val relaxedSettings: Seq[Setting[_]] = Seq(
    scalacOptions --= Seq("-Wunused:imports,privates,locals,patvars,nowarn")
  )

  // ── Dependencies ────────────────────────────────────────────────────

  /** Add sge-core as a JVM dependency. For cross-platform, use %%% in your build.sbt directly. */
  def coreDep(version: String): Seq[Setting[_]] = Seq(
    libraryDependencies += "com.kubuszok" %% "sge-core" % version
  )

  // ── Platform settings ───────────────────────────────────────────────

  /** JVM platform settings: fork, Panama native access, Rust library path, scaladesktop source dir.
    *
    * @param rustLibPath absolute path to the Rust native-components build output directory.
    *                    Defaults to `native-components/target/release` under the build root.
    */
  def jvmSettings(rustLibPath: Option[String] = None): Seq[Setting[_]] = Seq(
    Compile / unmanagedSourceDirectories ++= desktopSharedDir.value,
    fork := true,
    javaOptions ++= jvmRuntimeOpts(rustLibPath).value,
    Test / fork := true,
    Test / javaOptions ++= jvmRuntimeOpts(rustLibPath).value
  )

  /** Scala.js platform settings. Currently empty — scalajs-dom comes transitively from sge-core. */
  val jsSettings: Seq[Setting[_]] = Seq.empty

  /** Scala Native platform settings: scaladesktop source dir, Rust library linking.
    *
    * @param rustLibPath absolute path to the Rust native-components build output directory.
    *                    Defaults to `native-components/target/release` under the build root.
    */
  def nativeSettings(rustLibPath: Option[String] = None): Seq[Setting[_]] = Seq(
    Compile / unmanagedSourceDirectories ++= desktopSharedDir.value
    // Scala Native linking (nativeConfig) must be configured by the caller because
    // sbt-scala-native types aren't on this plugin's classpath. See nativeLinkingSnippet.
  )

  // ── Helpers ─────────────────────────────────────────────────────────

  /** Code snippet for Scala Native linking configuration. Paste into your nativePlatform settings. */
  val nativeLinkingSnippet: String =
    """nativeConfig := {
      |  val base      = (ThisBuild / baseDirectory).value
      |  val c         = nativeConfig.value
      |  val isWindows = System.getProperty("os.name", "").toLowerCase.contains("win")
      |  val rustLibOpts = Seq(
      |    s"-L$base/native-components/target/release",
      |    "-lsge_native_ops"
      |  )
      |  val windowsOpts = if (isWindows) Seq("-lntdll") else Seq.empty
      |  c.withLinkingOptions(c.linkingOptions ++ rustLibOpts ++ windowsOpts)
      |}""".stripMargin

  // ── Internal ────────────────────────────────────────────────────────

  /** Discovers the scaladesktop/ shared source directory for JVM and Native desktop builds. */
  private val desktopSharedDir: Def.Initialize[Seq[File]] = Def.setting {
    val base = (ThisBuild / baseDirectory).value
    val projDir = baseDirectory.value
    val projName = projDir.getName
    val desktopDir = base / projName / "src" / "main" / "scaladesktop"
    if (desktopDir.exists()) Seq(desktopDir) else Seq.empty
  }

  /** JVM runtime options: library path for Rust + Homebrew libs, Panama native access. */
  private def jvmRuntimeOpts(rustLibPath: Option[String]): Def.Initialize[Seq[String]] = Def.setting {
    val rustLib = rustLibPath.getOrElse {
      ((ThisBuild / baseDirectory).value / "native-components" / "target" / "release").getAbsolutePath
    }
    val brewLib = if (sys.props("os.name").toLowerCase.contains("mac")) {
      val arm = "/opt/homebrew/lib"
      val x86 = "/usr/local/lib"
      s"${java.io.File.pathSeparator}$arm${java.io.File.pathSeparator}$x86"
    } else ""
    Seq(
      s"-Djava.library.path=$rustLib$brewLib",
      "--enable-native-access=ALL-UNNAMED"
    )
  }
}
