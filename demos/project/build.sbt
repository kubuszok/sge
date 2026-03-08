// Load SgePlugin + SgePackaging source from the main repo (development mode).
// External projects would use:  addSbtPlugin("com.kubuszok" % "sge-build" % "0.1.0")
Compile / unmanagedSourceDirectories +=
  baseDirectory.value / ".." / ".." / "sge-build" / "src" / "main" / "scala"
