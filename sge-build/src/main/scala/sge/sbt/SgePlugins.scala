package sge.sbt

import multiarch.sbt.{ AndroidPlugin, JvmPackaging, NativeExtractSettings, NativeProviderPlugin, Platform }

import sbt._
import sbt.Keys._

import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import scala.scalanative.sbtplugin.ScalaNativePlugin
import scala.scalanative.sbtplugin.ScalaNativePlugin.autoImport._
import sbtprojectmatrix.ProjectMatrixKeys.projectMatrixBaseDirectory

/** JVM desktop platform plugin. Auto-triggers when [[SgePlugin]] is enabled.
  *
  * Provides JVM runtime config (fork for Panama FFM, java.library.path),
  * scaladesktop source directory discovery, and JVM packaging (simple + distribution).
  *
  * Since JvmPlugin is always present in sbt, this plugin triggers for all projects
  * with SgePlugin enabled. JVM-specific settings (fork, javaOptions) are harmless on
  * JS/Native axis projects and get overridden by their respective plugins.
  */
object SgeDesktopJvmPlatform extends AutoPlugin {

  override def trigger  = allRequirements
  override def requires = SgePlugin

  object autoImport {
    val releaseTargets       = JvmPackaging.releaseTargets
    val releaseJlinkModules  = JvmPackaging.releaseJlinkModules
    val releaseRoastVersion  = JvmPackaging.releaseRoastVersion
    val releaseVmArgs        = JvmPackaging.releaseVmArgs
    val releaseUseZgc        = JvmPackaging.releaseUseZgc
    val releaseCacheDir      = JvmPackaging.releaseCacheDir
  }

  override def projectSettings: Seq[Setting[_]] = jvmRuntime ++
    SgePackaging.jvmSettings ++ SgePackaging.distSettings ++ Seq(
    libraryDependencies += "com.kubuszok" % s"sge_${scalaBinaryVersion.value}" % SgePlugin.sgeVersion
  )

  /** JVM platform settings: fork for Panama FFM, library path, scaladesktop source dir. */
  private lazy val jvmRuntime: Seq[Setting[_]] = Seq(
    Compile / unmanagedSourceDirectories ++= {
      val desktopDir = projectMatrixBaseDirectory.value / "src" / "main" / "scaladesktop"
      if (desktopDir.exists()) Seq(desktopDir) else Seq.empty
    },
    fork := true,
    javaOptions ++= {
      val libDir = SgePlugin.autoImport.sgeNativeLibDir.value.getAbsolutePath
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

/** Browser platform plugin. Auto-triggers when [[SgePlugin]] and `ScalaJSPlugin` are enabled.
  *
  * Provides Scala.js browser packaging: fullLinkJS wiring, asset manifest generation,
  * and HTML index page creation.
  */
object SgeBrowserPlatform extends AutoPlugin {

  override def trigger  = allRequirements
  override def requires = SgeDesktopJvmPlatform && ScalaJSPlugin

  object autoImport {
    val sgeBrowserTitle = SgePackaging.sgeBrowserTitle
    val sgeJsOutputDir  = SgePackaging.sgeJsOutputDir
  }

  override def projectSettings: Seq[Setting[_]] = SgePackaging.browserSettings ++ Seq(
    libraryDependencies += "com.kubuszok" % s"sge_sjs1_${scalaBinaryVersion.value}" % SgePlugin.sgeVersion,
    scalaJSUseMainModuleInitializer := true,
    SgePackaging.sgeJsOutputDir := {
      val _ = (Compile / fullLinkJS).value
      (Compile / fullLinkJS / scalaJSLinkerOutputDirectory).value
    },
    // Override JVM defaults from SgeDesktopJvmPlatform (which auto-triggers on all SgePlugin projects).
    // JS projects don't use desktop-shared sources, forking, or JVM options.
    fork := false,
    Compile / unmanagedSourceDirectories := {
      (Compile / unmanagedSourceDirectories).value.filterNot(_.getPath.endsWith("scaladesktop"))
    },
    Test / unmanagedSourceDirectories := {
      (Test / unmanagedSourceDirectories).value.filterNot(_.getPath.endsWith("scaladesktop"))
    }
  )
}

/** Native desktop platform plugin. Auto-triggers when [[SgePlugin]] and `ScalaNativePlugin`
  * are enabled.
  *
  * Provides NativeProviderPlugin integration (library extraction + linker flags),
  * scaladesktop source directory discovery, and native packaging.
  */
object SgeDesktopNativePlatform extends AutoPlugin {

  override def trigger  = allRequirements
  override def requires = SgePlugin && ScalaNativePlugin

  // scaladesktop source dir is already added by SgeDesktopJvmPlatform (which auto-triggers
  // on all SgePlugin projects). No need to add it again here.
  override def projectSettings: Seq[Setting[_]] =
    NativeProviderPlugin.projectSettings ++
    SgePackaging.nativeSettings ++ Seq(
    libraryDependencies += "com.kubuszok" % s"sge_native0.5_${scalaBinaryVersion.value}" % SgePlugin.sgeVersion,
    NativeExtractSettings.nativeLibSourceDir := SgePlugin.autoImport.sgeNativeLibLocalDir.value,
    SgePackaging.sgeNativeBinary := (Compile / nativeLink).value
  )
}

/** SGE-specific Android platform plugin. Auto-triggers when [[SgePlugin]] and
  * [[multiarch.sbt.AndroidPlugin]] are both enabled.
  *
  * Overrides the default `androidSdkCacheDir` to `<baseDirectory>/sge-deps/android-sdk`
  * so CI workflows that pre-populate that path keep working. Provides a hook for
  * future SGE-specific Android customizations on top of the generic Android build.
  */
object SgeAndroidPlatform extends AutoPlugin {

  override def trigger  = allRequirements
  override def requires = SgePlugin && AndroidPlugin

  override def projectSettings: Seq[Setting[_]] = Seq(
    AndroidPlugin.autoImport.androidSdkCacheDir := (ThisBuild / baseDirectory).value / "sge-deps" / "android-sdk"
  )
}
