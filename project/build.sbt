// Include sbt-sge plugin source in the meta-build so that build.sbt can
// reference SgePlugin.commonSettings etc. directly — same source that
// gets published as the standalone sbt-sge plugin artifact.
Compile / unmanagedSourceDirectories +=
  baseDirectory.value / ".." / "sge-build" / "src" / "main" / "scala"

// sge's porting policy (sge-port/) is compiled into the meta-build, so the source generator
// (project/BalticPorterGen.scala) calls it directly and a policy change needs no new engine artifact.
Compile / unmanagedSourceDirectories +=
  baseDirectory.value / ".." / "sge-port" / "src" / "main" / "scala"

// The hash of the policy sources this meta-build COMPILED, so the generator can refuse to run a stale
// policy: a server keeps the classes it loaded until `reload`, while the generator's fingerprint reads the
// sources on disk. Same digest as `BalticPorterGen.policyHash` (relative path, NUL, text without CR, NUL).
Compile / sourceGenerators += Def.task {
  val root   = (baseDirectory.value / ".." / "sge-port" / "src" / "main" / "scala").toPath.toAbsolutePath.normalize
  val digest = java.security.MessageDigest.getInstance("SHA-256")
  val stream = java.nio.file.Files.walk(root)
  val files  =
    try {
      val it = stream.iterator()
      val b  = List.newBuilder[(String, java.nio.file.Path)]
      while (it.hasNext) {
        val p = it.next()
        if (java.nio.file.Files.isRegularFile(p) && !p.getFileName.toString.startsWith("."))
          b += (root.relativize(p).toString.replace('\\', '/') -> p)
      }
      b.result().sortBy(_._1)
    } finally stream.close()
  files.foreach { case (rel, p) =>
    digest.update(rel.getBytes("UTF-8"))
    digest.update(0.toByte)
    digest.update(java.nio.file.Files.readAllBytes(p).filter(_ != '\r'.toByte))
    digest.update(0.toByte)
  }
  val hash = digest.digest().take(8).map(b => f"$b%02x").mkString
  val out  = (Compile / sourceManaged).value / "sge" / "port" / "CompiledPolicy.scala"
  streams.value.log.debug(s"[Baltic Porter] compiling sge-port policy $hash")
  IO.write(
    out,
    s"package sge.port\n\n/** The hash of the `sge-port/src/main/scala` sources this meta-build compiled (`project/build.sbt`). */\nobject CompiledPolicy {\n  val sourceHash: String = \"$hash\"\n}\n"
  )
  Seq(out)
}.taskValue

// braces, as in every sge module
scalacOptions += "-no-indent"
