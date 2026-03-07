// Example build.sbt for an SGE game project.
// With sbt-sge plugin, this replaces ~100 lines of boilerplate.
//
// In project/plugins.sbt, add:
//   addSbtPlugin("com.kubuszok" % "sge-build" % "0.1.0-SNAPSHOT")

import sge.sbt.SgePlugin

lazy val game = (projectMatrix in file("game"))
  .settings(SgePlugin.commonSettings *)
  .settings(SgePlugin.coreDep("0.1.0-SNAPSHOT") *)
  .settings(
    name := "my-sge-game",
    organization := "com.example",
    testFrameworks += new TestFramework("munit.Framework")
  )
  .jvmPlatform(
    scalaVersions = Seq(SgePlugin.scalaVersion),
    settings = SgePlugin.jvmSettings()
  )
  .jsPlatform(
    scalaVersions = Seq(SgePlugin.scalaVersion),
    settings = SgePlugin.jsSettings ++ Seq(
      scalaJSUseMainModuleInitializer := true
    )
  )
  .nativePlatform(
    scalaVersions = Seq(SgePlugin.scalaVersion),
    settings = SgePlugin.nativeSettings() ++ Seq(
      nativeConfig := {
        val base      = (ThisBuild / baseDirectory).value
        val c         = nativeConfig.value
        val isWindows = System.getProperty("os.name", "").toLowerCase.contains("win")
        val rustLibOpts = Seq(
          s"-L$base/native-components/target/release",
          "-lsge_native_ops"
        )
        val windowsOpts = if (isWindows) Seq("-lntdll") else Seq.empty
        c.withLinkingOptions(c.linkingOptions ++ rustLibOpts ++ windowsOpts)
      }
    )
  )
