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
