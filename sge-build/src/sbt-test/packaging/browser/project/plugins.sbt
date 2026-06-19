// Resolve THIS plugin from the locally-published version. `scripted` runs
// `publishLocal` for sge-build first, then injects the resulting version via
// the `plugin.version` system property (wired in sge-build/build.sbt).
addSbtPlugin("com.kubuszok" % "sge-build" % sys.props("plugin.version"))
