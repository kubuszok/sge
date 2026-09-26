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
// sge pins the engine directly while lls-port carries lls's own engine pin; two hash versions
// are not comparable under early-semver, and eviction alone picks whichever compares HIGHER,
// which can silently be lls-port's — so the `dependencyOverrides` below the pin makes sge's decide.
ThisBuild / libraryDependencySchemes += "com.kubuszok" %% "balticporter-engine" % VersionScheme.Always
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

// Baltic Porter: Java->Scala 3 porting engine, runs as a sourceGenerator. The engine names no library:
// how libGDX is ported is sge's own policy (sge-port/, compiled into this meta-build by project/build.sbt).
resolvers += "Central Portal Snapshots" at "https://central.sonatype.com/repository/maven-snapshots"
libraryDependencies += "com.kubuszok" %% "balticporter-engine" % "cab01460052467c1bc955862a152ca84bd7f2cd4-SNAPSHOT"
dependencyOverrides += "com.kubuszok" %% "balticporter-engine" % "cab01460052467c1bc955862a152ca84bd7f2cd4-SNAPSHOT"
// sge's port is a dependent of the lls port: the base's policy is lls's own, published as lls-port at the
// version of the lls dependency itself (`Versions.lls`, read as text: this file cannot see the meta-build's sources).
libraryDependencies += "com.kubuszok" %% "lls-port" % {
  val versions = IO.read(baseDirectory.value / "Versions.scala")
  """val lls\s*=\s*"([^"]+)"""".r.findFirstMatchIn(versions).map(_.group(1)).getOrElse(sys.error("project/Versions.scala states no lls version"))
}
