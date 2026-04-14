// Example build.sbt for an SGE game project.
// With sbt-sge plugin, this replaces ~100 lines of boilerplate.
//
// In project/plugins.sbt, add:
//   addSbtPlugin("com.kubuszok" % "sge-build" % "0.1.0-SNAPSHOT")

import sge.sbt.SgePlugin

lazy val game = (projectMatrix in file("game"))
  .enablePlugins(SgePlugin)
  .settings(
    name := "my-sge-game",
    organization := "com.example",
    testFrameworks += new TestFramework("munit.Framework")
  )
  .jvmPlatform(scalaVersions = Seq(SgePlugin.scalaVersion))
  .jsPlatform(scalaVersions = Seq(SgePlugin.scalaVersion))
  .nativePlatform(scalaVersions = Seq(SgePlugin.scalaVersion))
