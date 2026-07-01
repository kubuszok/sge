// Resolve THIS plugin from the locally-published version. `scripted` runs
// `publishLocal` for sge-build first, then injects the resulting version via
// the `plugin.version` system property (wired in sge-build/build.sbt). The
// published JAR carries the vendored rapier2d-compat global build under
// /rapier2d-compat/rapier2d-compat.umd.js, which sgePackageBrowser copies out
// when sgeBrowserIncludeRapier2d := true (ISS-679).
addSbtPlugin("com.kubuszok" % "sge-build" % sys.props("plugin.version"))
