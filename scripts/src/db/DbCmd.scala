package sgedev
package db

/** Database command dispatcher. */
object DbCmd {

  def run(args: List[String]): Unit = {
    args match {
      case Nil | "--help" :: _ | "-h" :: _ =>
        printUsage()
      case "migration" :: rest =>
        MigrationDb.run(rest)
      case "issues" :: rest =>
        IssuesDb.run(rest)
      case "audit" :: rest =>
        AuditDb.run(rest)
      case unknown :: _ =>
        Term.err(s"Unknown db subcommand: $unknown")
        printUsage()
        sys.exit(1)
    }
  }

  private def printUsage(): Unit = {
    println("""Usage: sge-dev db <subcommand>
              |
              |Subcommands:
              |  migration   Migration status database (605 files)
              |  issues      Quality issues database
              |  audit       Audit results database""".stripMargin)
  }
}
