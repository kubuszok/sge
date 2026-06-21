// kubuszok plugin (bundles: sbt-git, sbt-scalafmt, sbt-scoverage, sbt-scalajs, sbt-scala-native, sbt-commandmatrix, sbt-pgp, and more)
// sbt-projectmatrix is merged into sbt 2.0 (no longer added separately).
addSbtPlugin("com.kubuszok" % "sbt-kubuszok" % "0.2.3")
// multiarch-scala (Platform, NativeProviderPlugin, ZigCross, JvmPackaging)
addSbtPlugin("com.kubuszok" % "sbt-multiarch-scala" % "0.3.0")
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
// jsdom DOM environment for Scala.js unit tests of browser components (ISS-672/ISS-536):
// the default Node.js Scala.js env has no document/window, so DOM-touching tests (e.g.
// BrowserGraphics) need a jsdom jsEnv. Requires the `jsdom` npm package (see package.json).
// scalajs-env-jsdom-nodejs is only published for Scala 2.10–2.13 (no _3). The
// sbt-2.0 meta-build runs on Scala 3, so pin the 2.13 artifact explicitly
// (JVM-only library; binary-compatible on a Scala 3 classpath). Mark it
// intransitive — its scalajs-js-envs / scalajs-env-nodejs / scalajs-logging
// deps are already provided as _3 by sbt-scalajs 1.22, and pulling their _2.13
// variants triggers a conflicting-cross-version-suffix error.
libraryDependencies += ("org.scala-js" % "scalajs-env-jsdom-nodejs_2.13" % "1.1.0").intransitive()
