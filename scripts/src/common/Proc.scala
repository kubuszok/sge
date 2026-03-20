package sgedev

import scala.sys.process.{Process, ProcessLogger}

/** Subprocess runner with stdout/stderr capture. */
object Proc {

  final case class Result(exitCode: Int, stdout: String, stderr: String) {
    def ok: Boolean = exitCode == 0
  }

  /** Run a command and capture output. */
  def run(cmd: String, args: List[String] = Nil, cwd: Option[String] = None,
          env: Map[String, String] = Map.empty): Result = {
    val cmdList = cmd :: args
    val stdoutBuf = new StringBuilder
    val stderrBuf = new StringBuilder
    val logger = ProcessLogger(
      line => { stdoutBuf.append(line); stdoutBuf.append('\n') },
      line => { stderrBuf.append(line); stderrBuf.append('\n') }
    )
    val cwdFile = cwd.map(new java.io.File(_))
    val envPairs = env.toSeq
    val exitCode = try {
      Process(cmdList, cwdFile, envPairs*).!(logger)
    } catch {
      case e: java.io.IOException =>
        stderrBuf.append(s"Failed to run: ${cmdList.mkString(" ")}: ${e.getMessage}\n")
        127
    }
    Result(exitCode, stdoutBuf.toString.stripTrailing, stderrBuf.toString.stripTrailing)
  }

  /** Run a command with a timeout (seconds). Tries `timeout`, then `gtimeout` (macOS),
    * then falls back to running without a timeout. */
  def runWithTimeout(cmd: String, args: List[String] = Nil, timeoutSec: Int = 10,
                     cwd: Option[String] = None): Result = {
    // Try GNU timeout (Linux), then gtimeout (macOS via coreutils), then no timeout
    val timeoutCmd = Seq("timeout", "gtimeout").find { t =>
      val check = try { Process(List("which", t)).!(ProcessLogger(_ => (), _ => ())) } catch { case _: Exception => 1 }
      check == 0
    }
    timeoutCmd match {
      case Some(t) =>
        run(t, timeoutSec.toString :: cmd :: args, cwd = cwd)
      case None =>
        // No timeout command available — run directly (best-effort)
        run(cmd, args, cwd = cwd)
    }
  }

  /** Spawn a command as a background process, redirecting output to a log file.
    * Returns the PID of the spawned process, or None on failure. */
  def spawn(cmd: String, args: List[String] = Nil, cwd: Option[String] = None,
            logFile: String = "/dev/null"): Option[Long] = {
    val cmdStr = (cmd :: args).map(a => s"'$a'").mkString(" ")
    val shell = s"$cmdStr > '$logFile' 2>&1 & echo $$!"
    val result = run("sh", List("-c", shell), cwd = cwd)
    if (result.ok) {
      result.stdout.trim.toLongOption
    } else {
      None
    }
  }

  /** Send a signal to a process by PID. Returns true if successful. */
  def signalProcess(pid: Long, signal: Int = 15): Boolean = {
    run("sh", List("-c", s"kill -$signal $pid")).ok
  }

  /** Check if a process with the given PID is alive. */
  def isAlive(pid: Long): Boolean = {
    run("sh", List("-c", s"kill -0 $pid 2>/dev/null")).ok
  }

  /** Run a command and stream output to console. Returns exit code. */
  def exec(cmd: String, args: List[String] = Nil, cwd: Option[String] = None,
           env: Map[String, String] = Map.empty): Int = {
    val cmdList = cmd :: args
    val cwdFile = cwd.map(new java.io.File(_))
    val envPairs = env.toSeq
    try {
      Process(cmdList, cwdFile, envPairs*).!
    } catch {
      case e: java.io.IOException =>
        Term.err(s"Failed to run: ${cmdList.mkString(" ")}: ${e.getMessage}")
        127
    }
  }
}
