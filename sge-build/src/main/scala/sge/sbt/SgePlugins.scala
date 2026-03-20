package sge.sbt

import sbt._
import sbt.Keys._

import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import scala.scalanative.sbtplugin.ScalaNativePlugin
import scala.scalanative.sbtplugin.ScalaNativePlugin.autoImport._

/** Base SGE project plugin.  Provides Scala 3 compiler settings, relaxed linting,
  * and common keys.  Enable on a projectMatrix or Project:
  * {{{
  * val game = (projectMatrix in file("game"))
  *   .enablePlugins(SgeProject)
  *   .settings(SgeProject.autoImport.sgeAppName := "My Game")
  * }}}
  */
object SgeProject extends AutoPlugin {

  override def trigger  = noTrigger
  override def requires = plugins.JvmPlugin

  object autoImport {
    val sgeAppName    = SgePackaging.sgeAppName
    val sgeProjectDir = settingKey[String](
      "Project directory name for scaladesktop source discovery. " +
        "Defaults to the project's base directory name."
    )
    val sgeRustLibDir = settingKey[File](
      "Directory containing Rust native-components build output. " +
        "Defaults to native-components/target/release under the build root."
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

  override def projectSettings: Seq[Setting[_]] =
    SgePlugin.commonSettings ++ SgePlugin.relaxedSettings ++ Seq(
      sgeProjectDir := baseDirectory.value.getName,
      // Default: use the SgeNativeLibs extraction target directory.
      // This is where JAR-extracted native libs end up (target/sge-native-libs/<platform>/).
      // For local development, override at ThisBuild:
      //   ThisBuild / SgeProject.autoImport.sgeRustLibDir :=
      //     (ThisBuild / baseDirectory).value / "native-components" / "target" / "release"
      sgeRustLibDir := target.value / "sge-native-libs" / Platform.host.classifier,
      sgeNativeLibDirs := Seq(sgeRustLibDir.value)
    )
}

/** JVM release plugin.  Enables JVM packaging (simple mode + distribution mode).
  *
  * For regular Project:
  * {{{
  * lazy val game = (project in file("game"))
  *   .enablePlugins(SgeProject, JvmReleases)
  * }}}
  *
  * For projectMatrix (per-axis):
  * {{{
  * .jvmPlatform(scalaVersions = Seq(sv), settings = JvmReleases.axisSettings)
  * }}}
  */
object JvmReleases extends AutoPlugin {

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
  lazy val axisSettings: Seq[Setting[_]] = jvmRuntime ++ SgePackaging.jvmSettings ++ SgePackaging.distSettings

  override def projectSettings: Seq[Setting[_]] = axisSettings

  /** JVM platform settings: fork for Panama FFM, library path, scaladesktop source dir. */
  private lazy val jvmRuntime: Seq[Setting[_]] = Seq(
    Compile / unmanagedSourceDirectories ++= {
      val base = (ThisBuild / baseDirectory).value
      val desktopDir = base / SgeProject.autoImport.sgeProjectDir.value / "src" / "main" / "scaladesktop"
      if (desktopDir.exists()) Seq(desktopDir) else Seq.empty
    },
    fork := true,
    javaOptions ++= {
      val rustLib = SgeProject.autoImport.sgeRustLibDir.value.getAbsolutePath
      val macFlags = if (Platform.host.isMac)
        Seq("-XstartOnFirstThread") else Seq.empty
      Seq(
        s"-Djava.library.path=$rustLib",
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
  *   .enablePlugins(SgeProject, BrowserReleases, ScalaJSPlugin)
  * }}}
  *
  * For projectMatrix (per-axis):
  * {{{
  * .jsPlatform(scalaVersions = Seq(sv), settings = BrowserReleases.axisSettings)
  * }}}
  */
object BrowserReleases extends AutoPlugin {

  override def trigger  = noTrigger
  override def requires = SgeProject

  object autoImport {
    val sgeBrowserTitle = SgePackaging.sgeBrowserTitle
    val sgeJsOutputDir  = SgePackaging.sgeJsOutputDir
  }

  /** Settings for projectMatrix JS axis.  Wires fullLinkJS to browser packaging. */
  lazy val axisSettings: Seq[Setting[_]] = SgePlugin.jsSettings ++ SgePackaging.browserSettings ++ Seq(
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
  *   .enablePlugins(SgeProject, NativeReleases, ScalaNativePlugin)
  * }}}
  *
  * For projectMatrix (per-axis):
  * {{{
  * .nativePlatform(scalaVersions = Seq(sv), settings = NativeReleases.axisSettings)
  * }}}
  */
object NativeReleases extends AutoPlugin {

  override def trigger  = noTrigger
  override def requires = SgeProject

  /** Settings for projectMatrix Native axis.  Wires nativeLink, SgeNativeLibs,
    * scaladesktop source directory, and native packaging.
    */
  lazy val axisSettings: Seq[Setting[_]] = nativeRuntime ++ SgeNativeLibs.settings ++ SgePackaging.nativeSettings ++ Seq(
    SgeNativeLibs.sgeNativeLibDir := SgeProject.autoImport.sgeRustLibDir.value,
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
      val base = (ThisBuild / baseDirectory).value
      val desktopDir = base / SgeProject.autoImport.sgeProjectDir.value / "src" / "main" / "scaladesktop"
      if (desktopDir.exists()) Seq(desktopDir) else Seq.empty
    }
  )
}

/** Android release plugin.  Enables Android APK building without Gradle.
  *
  * {{{
  * lazy val gameAndroid = (project in file("game-android"))
  *   .enablePlugins(SgeProject, AndroidReleases)
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
object AndroidReleases extends AutoPlugin {

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
