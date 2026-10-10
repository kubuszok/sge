// Resolve THIS plugin from the locally-published version. `scripted` runs
// `publishLocal` for sge-build first, then injects the resulting version via
// the `plugin.version` system property (wired in sge-build/build.sbt). The
// published JAR carries the vendored rapier2d-compat global build under
// /rapier2d-compat/rapier2d-compat.umd.js, which sgePackageBrowser copies out
// when sgeBrowserIncludeRapier2d := true (ISS-679).
addSbtPlugin("com.kubuszok" % "sge-build" % sys.props("plugin.version"))

// sge-build may pin snapshot builds of its own plugin dependencies (sbt-multiarch-scala).
resolvers += "Maven Central Snapshots" at "https://central.sonatype.com/repository/maven-snapshots"
