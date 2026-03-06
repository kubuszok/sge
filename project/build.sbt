// Include sbt-sge plugin source in the meta-build so that build.sbt can
// reference SgePlugin.commonSettings etc. directly — same source that
// gets published as the standalone sbt-sge plugin artifact.
Compile / unmanagedSourceDirectories +=
  baseDirectory.value / ".." / "sbt-sge" / "src" / "main" / "scala"
