package sge.sbt

import sbt._
import sbt.Keys._

import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import scala.scalanative.sbtplugin.ScalaNativePlugin
import scala.scalanative.sbtplugin.ScalaNativePlugin.autoImport._
import sbtprojectmatrix.ProjectMatrixKeys.projectMatrixBaseDirectory

/** Base SGE project plugin.  Provides Scala 3 compiler settings, relaxed linting,
  * and common keys.  Enable on a projectMatrix or Project:
  * {{{
  * val game = (projectMatrix in file("game"))
  *   .enablePlugins(SgeProject)
  *   .settings(SgeProject.autoImport.sgeAppName := "My Game")
  *   .jvmPlatform(scalaVersions = Seq(sv), settings = SgeProject.jvmAxis)
  *   .jsPlatform(scalaVersions = Seq(sv), settings = SgeProject.jsAxis)
  *   .nativePlatform(scalaVersions = Seq(sv), settings = SgeProject.nativeAxis)
  * }}}
  *
  * For local development, set `ThisBuild / sgeNativeLibLocalDir` to point at
  * the Rust build output directory (bypasses JAR extraction):
  * {{{
  * ThisBuild / SgeProject.autoImport.sgeNativeLibLocalDir := Some(
  *   (ThisBuild / baseDirectory).value / "native-components" / "target" / "release"
  * )
  * }}}
  */
object SgeProject extends AutoPlugin {

  override def trigger  = noTrigger
  override def requires = plugins.JvmPlugin

  object autoImport {
    val sgeAppName    = SgePackaging.sgeAppName
    val sgeNativeLibLocalDir = settingKey[Option[File]](
      "Local directory containing native libraries (bypasses JAR extraction). " +
        "Set at ThisBuild scope for development builds. " +
        "When None, native libs are extracted from classpath JARs."
    )
    val sgeNativeLibDir = settingKey[File](
      "Resolved directory with platform-specific native libraries. " +
        "Uses sgeNativeLibLocalDir when set, otherwise JAR extraction target."
    )
    val sgeNativeLibDirs = SgePackaging.sgeNativeLibDirs

    // Re-export packaging keys for convenience
    val sgePackage        = SgePackaging.sgePackage
    val sgePackagePlatform = SgePackaging.sgePackagePlatform
    val sgePackageAll     = SgePackaging.sgePackageAll
    val sgePackageBrowser = SgePackaging.sgePackageBrowser
    val sgePackageNative  = SgePackaging.sgePackageNative
    val sgeRelease        = SgePackaging.sgeRelease

    // Re-export Android keys
    val androidDex     = AndroidBuild.androidDex
    val androidPackage = AndroidBuild.androidPackage
    val androidSign    = AndroidBuild.androidSign
    val androidInstall = AndroidBuild.androidInstall
  }
  import autoImport._

  // Global scope default — lowest precedence, overridden by ThisBuild or project.
  override def globalSettings: Seq[Setting[_]] = Seq(
    sgeNativeLibLocalDir := None
  )

  override def projectSettings: Seq[Setting[_]] =
    SgePlugin.commonSettings ++ SgePlugin.relaxedSettings ++ Seq(
      // NOTE: sgeNativeLibLocalDir default is set in globalSettings (None).
      // Users override at ThisBuild scope in their build.sbt.
      sgeAppName := name.value,
      sgeNativeLibDir := sgeNativeLibLocalDir.value.getOrElse(
        target.value / "sge-native-libs" / Platform.host.classifier
      ),
      sgeNativeLibDirs := Seq(sgeNativeLibDir.value)
    )

  /** Zero-config JVM axis settings for projectMatrix. */
  def jvmAxis: Seq[Setting[_]] = SgeDesktopJvmPlatform.axisSettings

  /** Zero-config JS/Browser axis settings for projectMatrix. */
  def jsAxis: Seq[Setting[_]] = SgeBrowserPlatform.axisSettings

  /** Zero-config Scala Native axis settings for projectMatrix. */
  def nativeAxis: Seq[Setting[_]] = SgeDesktopNativePlatform.axisSettings
}

/** JVM release plugin.  Enables JVM packaging (simple mode + distribution mode),
  * forks for Panama FFM, wires java.library.path to native libs.
  *
  * For regular Project:
  * {{{
  * lazy val game = (project in file("game"))
  *   .enablePlugins(SgeProject, SgeDesktopJvmPlatform)
  * }}}
  *
  * For projectMatrix (per-axis):
  * {{{
  * .jvmPlatform(scalaVersions = Seq(sv), settings = SgeProject.jvmAxis)
  * }}}
  */
object SgeDesktopJvmPlatform extends AutoPlugin {

  override def trigger  = noTrigger
  override def requires = SgeProject

  object autoImport {
    val sgeTargets          = SgePackaging.sgeTargets
    val sgeJlinkModules     = SgePackaging.sgeJlinkModules
    val sgeRoastVersion     = SgePackaging.sgeRoastVersion
    val sgeVmArgs           = SgePackaging.sgeVmArgs
    val sgeUseZgc           = SgePackaging.sgeUseZgc
    val sgeCacheDir         = SgePackaging.sgeCacheDir
    val sgeCrossNativeLibDir = SgePackaging.sgeCrossNativeLibDir
  }

  /** Settings for projectMatrix JVM axis.  Includes JVM runtime config, scaladesktop
    * source directory discovery, and full packaging (simple + dist).
    */
  lazy val axisSettings: Seq[Setting[_]] = jvmRuntime ++ SgePackaging.jvmSettings ++ SgePackaging.distSettings ++ Seq(
    libraryDependencies += "com.kubuszok" % s"sge_${scalaBinaryVersion.value}" % SgePlugin.sgeVersion
  )

  override def projectSettings: Seq[Setting[_]] = axisSettings

  /** JVM platform settings: fork for Panama FFM, library path, scaladesktop source dir. */
  private lazy val jvmRuntime: Seq[Setting[_]] = Seq(
    Compile / unmanagedSourceDirectories ++= {
      val desktopDir = projectMatrixBaseDirectory.value / "src" / "main" / "scaladesktop"
      if (desktopDir.exists()) Seq(desktopDir) else Seq.empty
    },
    fork := true,
    javaOptions ++= {
      val libDir = SgeProject.autoImport.sgeNativeLibDir.value.getAbsolutePath
      val macFlags = if (Platform.host.isMac)
        Seq("-XstartOnFirstThread") else Seq.empty
      Seq(
        s"-Djava.library.path=$libDir",
        "--enable-native-access=ALL-UNNAMED"
      ) ++ macFlags
    },
    Test / fork := true,
    Test / javaOptions ++= (javaOptions).value
  )
}

/** Browser release plugin.  Enables Scala.js browser packaging.
  *
  * For regular Project:
  * {{{
  * lazy val gameJs = (project in file("game-js"))
  *   .enablePlugins(SgeProject, SgeBrowserPlatform, ScalaJSPlugin)
  * }}}
  *
  * For projectMatrix (per-axis):
  * {{{
  * .jsPlatform(scalaVersions = Seq(sv), settings = SgeProject.jsAxis)
  * }}}
  */
object SgeBrowserPlatform extends AutoPlugin {

  override def trigger  = noTrigger
  override def requires = SgeProject

  object autoImport {
    val sgeBrowserTitle = SgePackaging.sgeBrowserTitle
    val sgeJsOutputDir  = SgePackaging.sgeJsOutputDir
  }

  /** Settings for projectMatrix JS axis.  Wires fullLinkJS to browser packaging. */
  lazy val axisSettings: Seq[Setting[_]] = SgePlugin.jsSettings ++ SgePackaging.browserSettings ++ Seq(
    libraryDependencies += "com.kubuszok" % s"sge_sjs1_${scalaBinaryVersion.value}" % SgePlugin.sgeVersion,
    scalaJSUseMainModuleInitializer := true,
    SgePackaging.sgeJsOutputDir := {
      val _ = (Compile / fullLinkJS).value
      (Compile / fullLinkJS / scalaJSLinkerOutputDirectory).value
    }
  )

  override def projectSettings: Seq[Setting[_]] = axisSettings
}

/** Native release plugin.  Enables Scala Native packaging and native library linking.
  *
  * For regular Project:
  * {{{
  * lazy val gameNative = (project in file("game-native"))
  *   .enablePlugins(SgeProject, SgeDesktopNativePlatform, ScalaNativePlugin)
  * }}}
  *
  * For projectMatrix (per-axis):
  * {{{
  * .nativePlatform(scalaVersions = Seq(sv), settings = SgeProject.nativeAxis)
  * }}}
  */
object SgeDesktopNativePlatform extends AutoPlugin {

  override def trigger  = noTrigger
  override def requires = SgeProject

  /** Settings for projectMatrix Native axis.  Wires nativeLink, SgeNativeLibs,
    * scaladesktop source directory, and native packaging.
    */
  lazy val axisSettings: Seq[Setting[_]] = nativeRuntime ++ SgeNativeLibs.settings ++ SgePackaging.nativeSettings ++ Seq(
    libraryDependencies += "com.kubuszok" % s"sge_native0.5_${scalaBinaryVersion.value}" % SgePlugin.sgeVersion,
    SgeNativeLibs.sgeNativeLibDir := SgeProject.autoImport.sgeNativeLibLocalDir.value.getOrElse(
      target.value / "sge-native-libs" / Platform.host.classifier
    ),
    SgePackaging.sgeNativeBinary := (Compile / nativeLink).value,
    nativeConfig := {
      val c      = nativeConfig.value
      val libDir = SgeNativeLibs.sgeNativeLibDir.value
      c.withLinkingOptions(c.linkingOptions ++ SgeNativeLibs.linkerFlags(libDir))
    }
  )

  override def projectSettings: Seq[Setting[_]] = axisSettings

  /** Native platform settings: scaladesktop source dir. */
  private lazy val nativeRuntime: Seq[Setting[_]] = Seq(
    Compile / unmanagedSourceDirectories ++= {
      val desktopDir = projectMatrixBaseDirectory.value / "src" / "main" / "scaladesktop"
      if (desktopDir.exists()) Seq(desktopDir) else Seq.empty
    }
  )
}

/** Android release plugin.  Enables Android APK building without Gradle.
  *
  * {{{
  * lazy val gameAndroid = (project in file("game-android"))
  *   .enablePlugins(SgeProject, SgeAndroidPlatform)
  *   .settings(
  *     Compile / mainClass := Some("com.example.game.AndroidMain")
  *   )
  * }}}
  *
  * Build pipeline: `androidDex` -> `androidPackage` -> `androidSign` -> `androidInstall`
  *
  * Requires `AndroidManifest.xml` in `src/main/resources/`.
  * The Android SDK is auto-downloaded when android tasks are first run.
  */
object SgeAndroidPlatform extends AutoPlugin {

  override def trigger  = noTrigger
  override def requires = SgeProject

  object autoImport {
    val androidSdkRoot           = AndroidBuild.androidSdkRoot
    val androidMinSdk            = AndroidBuild.androidMinSdk
    val androidTargetSdk         = AndroidBuild.androidTargetSdk
    val androidBuildToolsVersion = AndroidBuild.androidBuildToolsVersion
  }

  override def projectSettings: Seq[Setting[_]] = AndroidBuild.settings
}
