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
    val logFile = s"${Paths.scriptsDir}/.metals.log"

    // Check if already running
    val pf = new File(pidFile)
    if (pf.exists()) {
      val existingPid = scala.io.Source.fromFile(pf).mkString.trim.toLongOption
      if (existingPid.isDefined && Proc.isAlive(existingPid.get)) {
        Term.warn(s"Metals is already running (pid ${existingPid.get})")
        return
      }
      pf.delete() // stale PID file
    }

    Term.info(s"Starting metals on port $port (background)...")
    val pid = Proc.spawn("metals-mcp",
      List("--workspace", Paths.projectRoot, "--port", port, "--client", "claude",
           "--default-bsp-to-build-tool"),
      cwd = Some(Paths.projectRoot),
      logFile = logFile)
    pid match {
      case Some(p) =>
        val writer = new java.io.PrintWriter(pidFile)
        writer.print(p)
        writer.close()
        Term.ok(s"Metals started (pid $p, log: $logFile)")
      case None =>
        Term.err("Failed to start metals")
        sys.exit(1)
    }
  }

  private def stop(): Unit = {
    val pf = new File(pidFile)
    if (pf.exists()) {
      val pid = scala.io.Source.fromFile(pf).mkString.trim.toLongOption
      if (pid.isDefined) {
        Term.info(s"Stopping metals (pid ${pid.get})...")
        Proc.signalProcess(pid.get)
        pf.delete()
        Term.ok("Metals stopped")
      } else {
        Term.err("Invalid PID file")
        pf.delete()
      }
    } else {
      Term.warn("No metals PID file found")
    }
  }

  private def status(): Unit = {
    val pf = new File(pidFile)
    if (pf.exists()) {
      val pid = scala.io.Source.fromFile(pf).mkString.trim.toLongOption
      if (pid.isDefined && Proc.isAlive(pid.get)) {
        println(s"Metals is running (pid ${pid.get})")
      } else {
        println("Metals PID file exists but process is not running")
        pf.delete()
      }
    } else {
      println("Metals is not running")
    }
  }
}
