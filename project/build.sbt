// Include sbt-sge plugin source in the meta-build so that build.sbt can
// reference SgePlugin.commonSettings etc. directly — same source that
// gets published as the standalone sbt-sge plugin artifact.
Compile / unmanagedSourceDirectories +=
  baseDirectory.value / ".." / "sge-build" / "src" / "main" / "scala"

// sge's porting policy (sge-port/) is compiled into the meta-build, so the source generator
// (project/BalticPorterGen.scala) calls it directly and a policy change needs no new engine artifact.
Compile / unmanagedSourceDirectories +=
  baseDirectory.value / ".." / "sge-port" / "src" / "main" / "scala"
// braces, as in every sge module
scalacOptions += "-no-indent"
