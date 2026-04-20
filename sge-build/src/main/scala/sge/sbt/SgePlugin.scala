package sge.sbt

import multiarch.sbt.{ JvmPackaging, Platform, ProjectMatrixOps => MultiArchProjectMatrixOps }

import sbt._
import sbt.Keys._
import sbt.internal.ProjectMatrix

/** Base SGE project plugin. Provides Scala 3 compiler settings, native lib directory
  * handling, and common keys. Enable on a projectMatrix or Project:
  * {{{
  * val game = (projectMatrix in file("game"))
  *   .enablePlugins(SgePlugin)
  *   .settings(JvmPackaging.releaseAppName := "My Game")
  *   .jvmPlatform(scalaVersions = Seq(SgePlugin.scalaVersion))
  *   .jsPlatform(scalaVersions = Seq(SgePlugin.scalaVersion))
  *   .nativePlatform(scalaVersions = Seq(SgePlugin.scalaVersion))
  * }}}
  *
  * Platform-specific plugins auto-trigger:
  *   - [[SgeDesktopJvmPlatform]] — when SgePlugin is enabled (JVM is the default desktop target)
  *   - [[SgeBrowserPlatform]] — when SgePlugin + ScalaJSPlugin are enabled
  *   - [[SgeDesktopNativePlatform]] — when SgePlugin + ScalaNativePlugin are enabled
  *   - [[SgeAndroidPlatform]] — when SgePlugin + AndroidPlugin are enabled
  *
  * For local development, set `ThisBuild / sgeNativeLibLocalDir` to point at
  * the Rust build output directory (bypasses JAR extraction):
  * {{{
  * ThisBuild / SgePlugin.autoImport.sgeNativeLibLocalDir := Some(
  *   (ThisBuild / baseDirectory).value / "native-components" / "target" / "release"
  * )
  * }}}
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 219
 * Covenant-baseline-methods: NativeCrossAxis,SgePlugin,androidDex,androidInstall,androidPackage,androidSign,autoImport,base,commonSettings,coreDep,desktopDir,desktopSharedDir,fromProps,globalSettings,jsPlatform,jsSettings,jvmPlatform,jvmRuntimeOpts,jvmSettings,macFlags,nativePlatform,nativeSettings,projectSettings,relaxedSettings,releaseAll,releaseAppName,releaseCacheDir,releaseJlinkModules,releaseMacOsBundleId,releaseMacOsIcon,releaseNativeLibDirs,releasePackage,releasePlatform,releaseRoastVersion,releaseTargets,releaseUseZgc,releaseVmArgs,requires,rustLib,scalaVersion,sgeNativeLibDir,sgeNativeLibLocalDir,sgePackageBrowser,sgePackageNative,sgeRelease,sgeVersion,trigger,withCrossNative
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
  */
object SgePlugin extends AutoPlugin {

  override def trigger  = noTrigger
  override def requires = plugins.JvmPlugin

  object autoImport {
    val sgeNativeLibLocalDir = settingKey[Option[File]](
      "Local directory containing native libraries (bypasses JAR extraction). " +
        "Set at ThisBuild scope for development builds. " +
        "When None, native libs are extracted from classpath JARs."
    )
    val sgeNativeLibDir = settingKey[File](
      "Resolved directory with platform-specific native libraries. " +
        "Uses sgeNativeLibLocalDir when set, otherwise JAR extraction target."
    )

    // Re-export JvmPackaging keys for convenience
    val releaseAppName       = JvmPackaging.releaseAppName
    val releasePackage       = JvmPackaging.releasePackage
    val releaseNativeLibDirs = JvmPackaging.releaseNativeLibDirs
    val releaseTargets       = JvmPackaging.releaseTargets
    val releaseJlinkModules  = JvmPackaging.releaseJlinkModules
    val releaseRoastVersion  = JvmPackaging.releaseRoastVersion
    val releaseVmArgs        = JvmPackaging.releaseVmArgs
    val releaseUseZgc        = JvmPackaging.releaseUseZgc
    val releaseMacOsBundleId = JvmPackaging.releaseMacOsBundleId
    val releaseMacOsIcon     = JvmPackaging.releaseMacOsIcon
    val releaseCacheDir      = JvmPackaging.releaseCacheDir
    val releasePlatform      = JvmPackaging.releasePlatform
    val releaseAll           = JvmPackaging.releaseAll

    // Re-export SGE-specific packaging keys
    val sgePackageBrowser = SgePackaging.sgePackageBrowser
    val sgePackageNative  = SgePackaging.sgePackageNative
    val sgeRelease        = SgePackaging.sgeRelease

    // Re-export Android keys from multiarch-scala
    val androidDex     = multiarch.sbt.AndroidBuild.androidDex
    val androidPackage = multiarch.sbt.AndroidBuild.androidPackage
    val androidSign    = multiarch.sbt.AndroidBuild.androidSign
    val androidInstall = multiarch.sbt.AndroidBuild.androidInstall

    // ── ProjectMatrix extensions ─────────────────────────────────────

    /** Re-export of multiarch-scala's [[multiarch.sbt.NativeCrossAxis]]. */
    val NativeCrossAxis  = multiarch.sbt.NativeCrossAxis
    type NativeCrossAxis = multiarch.sbt.NativeCrossAxis

    /** Extension methods for [[ProjectMatrix]] that apply SGE defaults.
      *
      * Delegates to [[multiarch.sbt.ProjectMatrixOps.Ops.withCrossNative]] for
      * cross-native axis generation, passing [[SgePlugin.scalaVersion]] as the default.
      */
    implicit class SgeProjectMatrixOps(val matrix: ProjectMatrix) extends AnyVal {
      /** `.jvmPlatform` with `scalaVersions` defaulted to [[SgePlugin.scalaVersion]]. */
      def jvmPlatform(settings: Seq[Setting[_]] = Seq.empty): ProjectMatrix =
        matrix.jvmPlatform(scalaVersions = Seq(SgePlugin.scalaVersion), settings = settings)

      /** `.jsPlatform` with `scalaVersions` defaulted to [[SgePlugin.scalaVersion]]. */
      def jsPlatform(settings: Seq[Setting[_]] = Seq.empty): ProjectMatrix =
        matrix.jsPlatform(scalaVersions = Seq(SgePlugin.scalaVersion), settings = settings)

      /** `.nativePlatform` with `scalaVersions` defaulted to [[SgePlugin.scalaVersion]]. */
      def nativePlatform(settings: Seq[Setting[_]] = Seq.empty): ProjectMatrix =
        matrix.nativePlatform(scalaVersions = Seq(SgePlugin.scalaVersion), settings = settings)

      /** Add cross-native compilation axes for all non-host desktop platforms.
        * Requires `zig` to be installed. Skipped silently if zig is unavailable.
        *
        * Delegates to [[multiarch.sbt.ProjectMatrixOps.Ops.withCrossNative]] using
        * [[SgePlugin.scalaVersion]] as the default Scala version for the subprojects.
        */
      def withCrossNative: ProjectMatrix =
        new MultiArchProjectMatrixOps.Ops(matrix).withCrossNative(SgePlugin.scalaVersion)
    }
  }
  import autoImport._

  // ── Version ─────────────────────────────────────────────────────────

  /** SGE version this plugin was built with. Use for library dependencies:
    * {{{ libraryDependencies += "com.kubuszok" %%% "sge" % SgePlugin.sgeVersion }}}
    *
    * Resolved at load time from the plugin JAR's properties, or from the
    * `.sge-version` file when running from source inclusion.
    */
  val sgeVersion: String = {
    val fromProps = Option(getClass.getResourceAsStream("/sge-build.properties")).map { is =>
      val p = new java.util.Properties()
      try p.load(is) finally is.close()
      p.getProperty("sge.version", "")
    }.filter(_.nonEmpty)
    fromProps.getOrElse {
      val f = new java.io.File(".sge-version")
      if (f.exists()) scala.io.Source.fromFile(f).mkString.trim
      else "0.0.0-SNAPSHOT"
    }
  }

  /** Scala version used by SGE. All modules must use this exact version. */
  val scalaVersion = "3.8.3"

  // ── Compiler flags ──────────────────────────────────────────────────

  /** Scala 3 compiler flags matching SGE conventions: braces required, -Werror, unused warnings. */
  val commonSettings: Seq[Setting[_]] = Seq(
    Keys.scalaVersion := SgePlugin.scalaVersion,
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-no-indent",
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

  /** Add sge as a dependency. For external game projects using the published plugin. */
  def coreDep(version: String): Seq[Setting[_]] = Seq(
    libraryDependencies += "com.kubuszok" %% "sge" % version
  )

  // ── Plugin settings ─────────────────────────────────────────────────

  override def globalSettings: Seq[Setting[_]] = Seq(
    sgeNativeLibLocalDir := None
  )

  override def projectSettings: Seq[Setting[_]] =
    commonSettings ++ relaxedSettings ++ Seq(
      JvmPackaging.releaseAppName := name.value,
      sgeNativeLibDir := sgeNativeLibLocalDir.value.getOrElse(
        target.value / "sge-native-libs" / Platform.host.classifier
      ),
      JvmPackaging.releaseNativeLibDirs := Seq(sgeNativeLibDir.value)
    )

  // ── Source-inclusion helpers (for root build.sbt) ───────────────────

  /** JVM platform settings: fork, Panama native access, scaladesktop source dir. */
  def jvmSettings(projectDir: String = "sge", rustLibPath: Option[String] = None): Seq[Setting[_]] = Seq(
    Compile / unmanagedSourceDirectories ++= desktopSharedDir(projectDir).value,
    fork := true,
    javaOptions ++= jvmRuntimeOpts(rustLibPath).value,
    Test / fork := true,
    Test / javaOptions ++= jvmRuntimeOpts(rustLibPath).value
  )

  /** Scala.js platform settings. Currently empty — scalajs-dom comes transitively from sge. */
  val jsSettings: Seq[Setting[_]] = Seq.empty

  /** Scala Native platform settings: scaladesktop source dir. */
  def nativeSettings(projectDir: String = "sge", rustLibPath: Option[String] = None): Seq[Setting[_]] = Seq(
    Compile / unmanagedSourceDirectories ++= desktopSharedDir(projectDir).value
  )

  // ── Internal ────────────────────────────────────────────────────────

  private def desktopSharedDir(projectDir: String): Def.Initialize[Seq[File]] = Def.setting {
    val base = (ThisBuild / baseDirectory).value
    val desktopDir = base / projectDir / "src" / "main" / "scaladesktop"
    if (desktopDir.exists()) Seq(desktopDir) else Seq.empty
  }

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
