// sbt 2 Bazel-compatible gRPC remote-cache client (ISS-792). Inert unless
// `Global / remoteCache` is set to an endpoint — see project/RemoteCacheSetup.scala
// (BuildBuddy wiring, enabled only when an API key is available).
// https://www.scala-sbt.org/2.x/docs/en/reference/remote-cache-setup.html
addRemoteCachePlugin

// kubuszok plugin (bundles: sbt-git, sbt-scalafmt, sbt-scoverage, sbt-scalajs, sbt-scala-native, sbt-commandmatrix, sbt-pgp, and more)
// sbt-projectmatrix is merged into sbt 2.0 (no longer added separately).
addSbtPlugin("com.kubuszok" % "sbt-kubuszok" % "0.2.3")
// multiarch-scala (Platform, NativeProviderPlugin, ZigCross, JvmPackaging)
addSbtPlugin("com.kubuszok" % "sbt-multiarch-scala" % "0.4.0")
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
libraryDependencies += ("org.scala-js" % "scalajs-env-jsdom-nodejs_2.13" % "1.1.1").intransitive()

// Baltic Porter: Java->Scala 3 porting engine, runs as a sourceGenerator
resolvers += Resolver.defaultLocal
resolvers += "Central Portal Snapshots" at "https://central.sonatype.com/repository/maven-snapshots"
libraryDependencies += "com.kubuszok" %% "balticporter-corpus" % "ea3ad52f91f70fb1eeadf10c29d51d0bef98faa3-SNAPSHOT"
// Non-Java frontend: RAST readers, ParityDerive, body translators (Phase 3 proof)
libraryDependencies += "com.kubuszok" %% "balticporter-frontend-ts" % "ea3ad52f91f70fb1eeadf10c29d51d0bef98faa3-SNAPSHOT"
