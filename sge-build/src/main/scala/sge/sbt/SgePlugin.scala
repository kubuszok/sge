package sge.sbt

import sbt._
import sbt.Keys._

/** Shared SGE build settings for cross-platform Scala 3 game projects (JVM/JS/Native).
  *
  * This object provides common compiler flags, dependency management, and platform-specific
  * source directory wiring. It works both:
  *   - As a published sbt plugin (external projects add `addSbtPlugin(...)`)
  *   - As local shared source (SGE itself adds `sge-build/src/main/scala` to the meta-build)
  *
  * === Local self-hosting (in SGE's own `project/build.sbt`) ===
  * {{{
  * Compile / unmanagedSourceDirectories +=
  *   baseDirectory.value / ".." / "sge-build" / "src" / "main" / "scala"
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

  /** Add sge as a JVM dependency. For cross-platform, use %%% in your build.sbt directly. */
  def coreDep(version: String): Seq[Setting[_]] = Seq(
    libraryDependencies += "com.kubuszok" %% "sge" % version
  )

  // ── Platform settings ───────────────────────────────────────────────

  /** JVM platform settings: fork, Panama native access, Rust library path, scaladesktop source dir.
    *
    * @param projectDir directory name of the project (e.g. "sge", "demo"). Used to locate
    *                   `scaladesktop/` source directory. Defaults to "sge".
    * @param rustLibPath absolute path to the Rust native-components build output directory.
    *                    Defaults to `native-components/target/release` under the build root.
    */
  def jvmSettings(projectDir: String = "sge", rustLibPath: Option[String] = None): Seq[Setting[_]] = Seq(
    Compile / unmanagedSourceDirectories ++= desktopSharedDir(projectDir).value,
    fork := true,
    javaOptions ++= jvmRuntimeOpts(rustLibPath).value,
    Test / fork := true,
    Test / javaOptions ++= jvmRuntimeOpts(rustLibPath).value
  )

  /** Scala.js platform settings. Currently empty — scalajs-dom comes transitively from sge. */
  val jsSettings: Seq[Setting[_]] = Seq.empty

  /** Scala Native platform settings: scaladesktop source dir, Rust library linking.
    *
    * @param projectDir directory name of the project (e.g. "sge", "demo"). Used to locate
    *                   `scaladesktop/` source directory. Defaults to "sge".
    * @param rustLibPath absolute path to the Rust native-components build output directory.
    *                    Defaults to `native-components/target/release` under the build root.
    */
  def nativeSettings(projectDir: String = "sge", rustLibPath: Option[String] = None): Seq[Setting[_]] = Seq(
    Compile / unmanagedSourceDirectories ++= desktopSharedDir(projectDir).value
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
      |    s"-L$base/sge-deps/native-components/target/release",
      |    "-lsge_native_ops"
      |  )
      |  val windowsOpts = if (isWindows) Seq("-lntdll") else Seq.empty
      |  c.withLinkingOptions(c.linkingOptions ++ rustLibOpts ++ windowsOpts)
      |}""".stripMargin

  // ── Native Library Packaging ───────────────────────────────────────

  /** @deprecated Use [[Platform.desktop]] instead. */
  val desktopPlatforms: Seq[String] = Platform.desktop.map(_.classifier)

  /** @deprecated Use [[AndroidAbi.all]] instead. */
  val androidAbis: Seq[String] = AndroidAbi.all.map(_.name)

  /** @deprecated Use [[Platform.host]] instead. */
  def hostPlatform: String = Platform.host.classifier

  /** @deprecated Use [[Platform#rustTarget]] instead. */
  def platformToRustTarget(platform: String): String = Platform.fromClassifier(platform).rustTarget

  /** @deprecated Use [[Platform#rustTarget]] instead (identical to Rust target). */
  def platformToScalaNativeTarget(platform: String): String = Platform.fromClassifier(platform).rustTarget

  // ── Internal ────────────────────────────────────────────────────────

  /** Discovers the scaladesktop/ shared source directory for JVM and Native desktop builds.
    *
    * projectMatrix subprojects have different baseDirectory values: the default-axis project
    * (JVM) gets the original directory (e.g. `sge/`), but non-default axes (Native) get
    * `.sbt/matrix/sgeNative/`. We use the explicit `projectDir` name to build the correct
    * path relative to the build root.
    */
  private def desktopSharedDir(projectDir: String): Def.Initialize[Seq[File]] = Def.setting {
    val base = (ThisBuild / baseDirectory).value
    val desktopDir = base / projectDir / "src" / "main" / "scaladesktop"
    if (desktopDir.exists()) Seq(desktopDir) else Seq.empty
  }

  /** JVM runtime options: library path for Rust native libs, Panama native access.
    *
    * All native libraries (libsge_native_ops, libsge_audio, libglfw) are built from vendored source by the Rust build system and placed in native-components/target/release/. ANGLE (libEGL, libGLESv2) is
    * bundled there as well. No external Homebrew or system library installations required.
    */
  private def jvmRuntimeOpts(rustLibPath: Option[String]): Def.Initialize[Seq[String]] = Def.setting {
    val rustLib = rustLibPath.getOrElse {
      ((ThisBuild / baseDirectory).value / "sge-deps" / "native-components" / "target" / "release").getAbsolutePath
    }
    val macFlags = if (sys.props("os.name").toLowerCase.contains("mac"))
      Seq("-XstartOnFirstThread") else Seq.empty
    Seq(
      s"-Djava.library.path=$rustLib",
      "--enable-native-access=ALL-UNNAMED"
    ) ++ macFlags
  }
}
