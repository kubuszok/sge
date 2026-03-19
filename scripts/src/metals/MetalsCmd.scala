package sgedev
package metals

import java.io.File

/** Metals LSP server management — self-contained, no Justfile dependency. */
object MetalsCmd {

  private def pidFile: String = s"${Paths.scriptsDir}/.metals-pid"

  def run(args: List[String]): Unit = {
    args match {
      case Nil | "--help" :: _ =>
        println("""Usage: sge-dev metals <command>
                  |
                  |Commands:
                  |  install    Install metals-mcp server via Coursier
                  |  start      Start metals server
                  |  stop       Stop metals server
                  |  status     Show server status""".stripMargin)
      case "install" :: _ => install()
      case "start" :: rest => start(Cli.parse(rest))
      case "stop" :: _ => stop()
      case "status" :: _ => status()
      case other :: _ =>
        Term.err(s"Unknown metals command: $other")
        sys.exit(1)
    }
  }

  private def install(): Unit = {
    val code = Proc.exec("cs", List("install", "metals-mcp"))
    if (code != 0) sys.exit(code)
  }

  private def start(args: Cli.Args): Unit = {
    val port = args.flagOrDefault("port", "7845")
    Term.info(s"Starting metals on port $port...")
    val code = Proc.exec("metals-mcp",
      List("--workspace", Paths.projectRoot, "--port", port, "--client", "claude",
           "--default-bsp-to-build-tool"),
      cwd = Some(Paths.projectRoot))
    if (code != 0) sys.exit(code)
  }

  private def stop(): Unit = {
    val pf = new File(pidFile)
    if (pf.exists()) {
      val pid = scala.io.Source.fromFile(pf).mkString.trim
      Term.info(s"Stopping metals (pid $pid)...")
      Proc.exec("kill", List(pid))
      pf.delete()
      Term.ok("Metals stopped")
    } else {
      Term.warn("No metals PID file found")
    }
  }

  private def status(): Unit = {
    val pf = new File(pidFile)
    if (pf.exists()) {
      val pid = scala.io.Source.fromFile(pf).mkString.trim
      val result = Proc.run("kill", List("-0", pid))
      if (result.ok) {
        println(s"Metals is running (pid $pid)")
      } else {
        println("Metals PID file exists but process is not running")
        pf.delete()
      }
    } else {
      println("Metals is not running")
    }
  }
}
