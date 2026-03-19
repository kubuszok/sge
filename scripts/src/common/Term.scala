package sgedev

/** Terminal output helpers with ANSI colors. */
object Term {

  private val isColorEnabled: Boolean = {
    val term = sys.env.getOrElse("TERM", "")
    val noColor = sys.env.contains("NO_COLOR")
    !noColor && term.nonEmpty
  }

  private def ansi(code: String, text: String): String = {
    if (isColorEnabled) s"\u001b[${code}m$text\u001b[0m" else text
  }

  def bold(text: String): String = ansi("1", text)
  def red(text: String): String = ansi("31", text)
  def green(text: String): String = ansi("32", text)
  def yellow(text: String): String = ansi("33", text)
  def blue(text: String): String = ansi("34", text)
  def cyan(text: String): String = ansi("36", text)
  def dim(text: String): String = ansi("2", text)

  def ok(msg: String): Unit = System.err.println(green(s"[ok] $msg"))
  def warn(msg: String): Unit = System.err.println(yellow(s"[warn] $msg"))
  def err(msg: String): Unit = System.err.println(red(s"[error] $msg"))
  def info(msg: String): Unit = System.err.println(blue(s"[info] $msg"))

  /** Format a simple table with aligned columns. */
  def table(headers: List[String], rows: List[List[String]]): String = {
    if (rows.isEmpty) return "(no results)"
    val allRows = headers :: rows
    val colWidths = allRows.transpose.map(_.map(_.length).max)
    allRows.map { row =>
      row.zip(colWidths).map { case (cell, width) =>
        cell.padTo(width, ' ')
      }.mkString("  ")
    }.mkString("\n")
  }
}
