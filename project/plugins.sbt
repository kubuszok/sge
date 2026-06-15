// kubuszok plugin (bundles: sbt-git, sbt-scalafmt, sbt-scoverage, sbt-projectmatrix, sbt-scalajs, sbt-scala-native, sbt-pgp, and more)
addSbtPlugin("com.kubuszok" % "sbt-kubuszok" % "0.2.1")
// multiarch-scala (Platform, NativeProviderPlugin, ZigCross, JvmPackaging)
addSbtPlugin("com.kubuszok" % "sbt-multiarch-scala" % "0.2.0")
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
// jsdom DOM environment for Scala.js unit tests of browser components (ISS-672/ISS-536):
// the default Node.js Scala.js env has no document/window, so DOM-touching tests (e.g.
// BrowserGraphics) need a jsdom jsEnv. Requires the `jsdom` npm package (see package.json).
libraryDependencies += "org.scala-js" %% "scalajs-env-jsdom-nodejs" % "1.1.0"
