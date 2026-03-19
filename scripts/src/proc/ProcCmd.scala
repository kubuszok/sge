package sgedev
package proc

import java.io.File

/** Safe process listing and killing scoped to this repository. */
object ProcCmd {

  def run(args: List[String]): Unit = {
    args match {
      case Nil | "--help" :: _ =>
        println("""Usage: sge-dev proc <command>
                  |
                  |Commands:
                  |  list         List processes started from this repository
                  |  kill         Kill all project processes (sbt, forked JVMs)
                  |  kill-sbt     Kill sbt server and forked test JVMs""".stripMargin)
      case "list" :: _ => list()
      case "kill" :: _ => killAll()
      case "kill-sbt" :: _ => killSbt()
      case other :: _ =>
        Term.err(s"Unknown proc command: $other")
        sys.exit(1)
    }
  }

  private def list(): Unit = {
    val projectDir = Paths.projectRoot
    Term.info(s"Processes related to: $projectDir")
    println()

    val sbtProcs = findProcesses("sbt", projectDir)
    val forkProcs = findProcesses("sbt.ForkMain", "")
    val cargoProcs = findProcesses("cargo", projectDir)
    val scalaCliProcs = findProcesses("scala-cli", projectDir)

    val allProcs = sbtProcs ++ forkProcs ++ cargoProcs ++ scalaCliProcs
    if (allProcs.isEmpty) {
      println("  No project processes found.")
    } else {
      println(f"  ${"PID"}%-8s ${"TYPE"}%-15s ${"COMMAND"}%s")
      println(f"  ${"---"}%-8s ${"----"}%-15s ${"-------"}%s")
      allProcs.foreach { p =>
        println(f"  ${p.pid}%-8s ${p.procType}%-15s ${p.cmdSummary}%s")
      }
      println()
      println(s"  Total: ${allProcs.size} processes")
    }
  }

  private def killAll(): Unit = {
    val projectDir = Paths.projectRoot
    val sbtProcs = findProcesses("sbt", projectDir)
    val forkProcs = findProcesses("sbt.ForkMain", "")

    val allProcs = sbtProcs ++ forkProcs
    if (allProcs.isEmpty) {
      println("No project processes to kill.")
    } else {
      allProcs.foreach { p =>
        Term.info(s"Killing ${p.procType} (pid ${p.pid})")
        Proc.run("kill", List("-9", p.pid))
      }
      // Clean up sbt server socket
      cleanSbtSocket()
      Term.ok(s"Killed ${allProcs.size} processes")
    }
  }

  private def killSbt(): Unit = {
    // Try graceful shutdown first
    Term.info("Shutting down sbt server...")
    val result = Proc.run("sbt", List("--client", "shutdown"), cwd = Some(Paths.projectRoot))
    if (result.ok) {
      Term.ok("sbt server shut down gracefully")
    } else {
      // Force kill if graceful shutdown fails
      Term.warn("Graceful shutdown failed, force killing...")
      val sbtProcs = findProcesses("sbt", Paths.projectRoot)
      val forkProcs = findProcesses("sbt.ForkMain", "")
      (sbtProcs ++ forkProcs).foreach { p =>
        Proc.run("kill", List("-9", p.pid))
      }
      cleanSbtSocket()
      Term.ok("sbt processes force killed")
    }
  }

  private final case class ProcessInfo(pid: String, procType: String, cmdSummary: String)

  private def findProcesses(pattern: String, dirFilter: String): List[ProcessInfo] = {
    // Use ps to find matching processes
    val result = Proc.run("ps", List("aux"))
    if (!result.ok) {
      Nil
    } else {
      result.stdout.linesIterator.filter { line =>
        line.contains(pattern) &&
        (dirFilter.isEmpty || line.contains(dirFilter)) &&
        !line.contains("grep") &&
        !line.contains("sge-dev proc") // Don't match ourselves
      }.flatMap { line =>
        val parts = line.trim.split("\\s+", 11)
        if (parts.length >= 11) {
          val pid = parts(1)
          val cmd = parts(10)
          val procType = pattern match {
            case "sbt" if cmd.contains("ForkMain") => "sbt-fork"
            case "sbt" => "sbt"
            case "sbt.ForkMain" => "sbt-fork"
            case "cargo" => "cargo"
            case "scala-cli" => "scala-cli"
            case _ => "unknown"
          }
          val summary = if (cmd.length > 60) cmd.take(57) + "..." else cmd
          Some(ProcessInfo(pid, procType, summary))
        } else {
          None
        }
      }.toList
    }
  }

  private def cleanSbtSocket(): Unit = {
    // Clean up sbt server socket file
    val socketDir = new File(s"${System.getProperty("user.home")}/.sbt/1.0/server")
    if (socketDir.exists() && socketDir.isDirectory) {
      val subdirs = socketDir.listFiles()
      if (subdirs != null) {
        subdirs.foreach { subdir =>
          val sock = new File(subdir, "sock")
          if (sock.exists()) {
            sock.delete()
          }
        }
      }
    }
  }
}
