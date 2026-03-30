// SGE plugin version: read from .sge-version (written by root build's writeDemoVersion task).
// Falls back to git SHA snapshot for fresh checkouts.
val sgePluginVersion: String = {
  val versionFile = new File(".sge-version")
  if (versionFile.exists())
    scala.io.Source.fromFile(versionFile).mkString.trim
  else
    sys.env.getOrElse("SGE_VERSION", {
      // git repo root is two levels up from demos/project/
      scala.util.Try(
        scala.sys.process.Process(Seq("git", "rev-parse", "HEAD"), new File("..")).!!.trim
      ).map(sha => s"$sha-SNAPSHOT").getOrElse("0.0.0-SNAPSHOT")
    })
}

// Consume sge-build as a published plugin (same as external users).
// Run `sbt publishLocal` in sge-build/ after any plugin changes.
addSbtPlugin("com.kubuszok" % "sge-build" % sgePluginVersion)
