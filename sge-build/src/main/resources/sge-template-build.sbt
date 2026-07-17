// Example build.sbt for an SGE game project.
// With sbt-sge plugin, this replaces ~100 lines of boilerplate.
//
// In project/plugins.sbt, add:
//   addSbtPlugin("com.kubuszok" % "sge-build" % "<sge-version>") // replace with the latest published sge release

import sge.sbt.SgePlugin

lazy val game = (projectMatrix in file("game"))
  .enablePlugins(SgePlugin)
  .settings(
    name := "my-sge-game",
    organization := "com.example",
    // Uncomment to add extensions:
    // sgeExtensions := Set(SgeExtension.Noise, SgeExtension.FreeType),
    testFrameworks += new TestFramework("munit.Framework")
  )
  .jvmPlatform()
  .jsPlatform()
  .nativePlatform()
