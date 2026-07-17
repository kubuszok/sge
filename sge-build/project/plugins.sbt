addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.6")
// ISS-752: sge-build is a SEPARATE sbt build, so the ROOT build's publishing
// wiring (Sonatype target, PGP signing, the `ci-release` command) does not reach
// it — CI only ever published the plugin to Ivy-local. sbt-kubuszok is the SAME
// plugin the root meta-build uses (project/plugins.sbt): it bundles sbt-git
// (dynver), sbt-pgp (publishSigned), sbt-sonatype (localStaging / sonaRelease)
// and defines the `ci-release` command + the Sonatype `publishTo` wiring keyed
// off `projectType`. Adding it here gives sge-build the identical release path so
// the documented plugin (docs/getting-started.md) resolves from Maven Central.
addSbtPlugin("com.kubuszok" % "sbt-kubuszok" % "0.2.3")
// Mirror the root meta-build: force a single scala-xml suffix so sbt-scalafmt's
// 2.13 transitive (scalafmt-dynamic -> coursier_2.13 -> scala-xml_2.13) does not
// trip sbt 2.0's conflicting-cross-version-suffix guard against the _3 build sbt
// core supplies.
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
