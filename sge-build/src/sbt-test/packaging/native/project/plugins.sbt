// Resolve THIS plugin from the locally-published version. `scripted` runs
// `publishLocal` for sge-build first, then injects the resulting version via
// the `plugin.version` system property (wired in sge-build/build.sbt).
addSbtPlugin("com.kubuszok" % "sge-build" % sys.props("plugin.version"))

// sge-build may pin snapshot builds of its own plugin dependencies (sbt-multiarch-scala).
resolvers += "Maven Central Snapshots" at "https://central.sonatype.com/repository/maven-snapshots"
