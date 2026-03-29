// CI sets SGE_USE_PLUGIN=true to consume the published sge-build plugin,
// validating the end-user experience. In dev mode, include source directly.
if (sys.env.getOrElse("SGE_USE_PLUGIN", "false") == "true")
  addSbtPlugin("com.kubuszok" % "sge-build" % "0.1.0-SNAPSHOT")
else
  Compile / unmanagedSourceDirectories +=
    baseDirectory.value / ".." / ".." / "sge-build" / "src" / "main" / "scala"
