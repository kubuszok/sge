//> using scala 3.8.2
//> using platform scala-native
//> using options -deprecation -feature -no-indent -Werror
//> using dep io.github.cquiroz::scala-java-time::2.6.0

package sgedev

object Main {

  private val version = "0.1.0"

  def main(args: Array[String]): Unit = {
    args.toList match {
      case Nil | "--help" :: _ | "-h" :: _ =>
        printUsage()
      case "--version" :: _ =>
        println(s"sge-dev $version")
      case "hook" :: rest =>
        hook.HookCmd.run(rest)
      case "db" :: rest =>
        db.DbCmd.run(rest)
      case "git" :: rest =>
        git.GitCmd.run(rest)
      case "build" :: rest =>
        build.BuildCmd.run(rest)
      case "quality" :: rest =>
        quality.QualityCmd.run(rest)
      case "test" :: rest =>
        testing.TestCmd.run(rest)
      case "setup" :: rest =>
        setup.SetupCmd.run(rest)
      case "native" :: _ =>
        Term.err("The 'native' command has been removed.")
        Term.err("Native libraries are distributed as provider JARs — no local build needed.")
        Term.err("To build native components from source, see: https://github.com/kubuszok/sge-native-components")
        sys.exit(1)
      case "compare" :: rest =>
        compare.CompareCmd.run(rest)
      case "metals" :: rest =>
        metals.MetalsCmd.run(rest)
      case "proc" :: rest =>
        proc.ProcCmd.run(rest)
      case unknown :: _ =>
        Term.err(s"Unknown command: $unknown")
        printUsage()
        sys.exit(1)
    }
  }

  private def printUsage(): Unit = {
    println(s"""sge-dev $version — SGE development toolkit
               |
               |Usage: sge-dev <command> [args...]
               |
               |Commands:
               |  hook       PreToolUse validator (reads JSON from stdin)
               |  db         Database queries (migration, issues, audit)
               |  git        Git and GitHub operations
               |  build      Build commands (compile, fmt, publish-local)
               |  quality    Quality scans (grep, count, scan)
               |  test       Test orchestration (unit, integration, regression)
               |  setup      Idempotent dev environment setup (all tools + targets)
               |  native     (removed — native libs from provider JARs)
               |  compare    LibGDX/SGE file comparison
               |  metals     Metals LSP server management
               |  proc       Process listing and killing (project-scoped)
               |
               |Options:
               |  --help     Show this help
               |  --version  Show version""".stripMargin)
  }
}
