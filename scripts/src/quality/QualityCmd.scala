package sgedev
package quality

/** Quality scan commands — self-contained, no Justfile dependency. */
object QualityCmd {

  def run(args: List[String]): Unit = {
    args match {
      case Nil | "--help" :: _ =>
        println("""Usage: sge-dev quality <command>
                  |
                  |Commands:
                  |  scan [--return] [--null] [--null-cast] [--java-syntax] [--todo] [--all] [--summary]
                  |  grep <pattern> [--count] [--files-only]
                  |  count <pattern>""".stripMargin)
      case "scan" :: rest => scan(Cli.parse(rest))
      case "grep" :: rest => grep(Cli.parse(rest))
      case "count" :: rest => count(Cli.parse(rest))
      case other :: _ =>
        Term.err(s"Unknown quality command: $other")
        sys.exit(1)
    }
  }

  private def scan(args: Cli.Args): Unit = {
    val all = args.hasFlag("all") || (!args.hasFlag("return") && !args.hasFlag("null") &&
              !args.hasFlag("null-cast") && !args.hasFlag("java-syntax") && !args.hasFlag("todo"))
    val summary = args.hasFlag("summary")

    if (all || args.hasFlag("return")) scanReturn(summary)
    if (all || args.hasFlag("null")) { if (all) println(); scanNull(summary) }
    if (all || args.hasFlag("null-cast")) { if (all) println(); scanNullCast(summary) }
    if (all || args.hasFlag("java-syntax")) { if (all) println(); scanJavaSyntax(summary) }
    if (all || args.hasFlag("todo")) { if (all) println(); scanTodo(summary) }
  }

  private def scanReturn(summary: Boolean): Unit = {
    println("=== return keyword (actual statements) ===")
    // Find return keywords, exclude @return doc tags, comments, throw messages, string literals
    val result = rg(List("\\breturn\\b", Paths.sgeSrc, "--type", "scala", "-n"))
    if (!result.ok || result.stdout.trim.isEmpty) {
      println("  (none)")
      println()
      println("  Files: 0  Occurrences: 0")
    } else {
      // Filter out false positives
      val matches = result.stdout.linesIterator.filter { line =>
        !line.contains("@return") &&
        !line.matches(".*\\*.*return.*") &&
        !line.contains("//.*return".r.findFirstIn(line).getOrElse("NOMATCH")) &&
        !line.contains("throw") &&
        !line.matches(".*\".*return.*\".*")
      }.toList
      reportMatches(matches, summary)
    }
  }

  private def scanNull(summary: Boolean): Unit = {
    println("=== null checks (== null / != null) ===")
    val result = rg(List("== null|!= null", Paths.sgeSrc, "--type", "scala", "-n"))
    if (!result.ok || result.stdout.trim.isEmpty) {
      println("  (none)")
      println()
      println("  Files: 0  Occurrences: 0")
    } else {
      val matches = result.stdout.linesIterator.filter { line =>
        !line.matches(".*//.*null.*") &&
        !line.matches(".*/\\*.*null.*") &&
        !line.matches(".*\\*.*null.*") &&
        !line.matches(".*\".*null.*\".*")
      }.toList
      reportMatches(matches, summary)
    }
  }

  private def scanNullCast(summary: Boolean): Unit = {
    println("=== null.asInstanceOf ===")
    val result = rg(List("null\\.asInstanceOf", Paths.sgeSrc, "--type", "scala", "-n"))
    if (!result.ok || result.stdout.trim.isEmpty) {
      println("  (none)")
      println()
      println("  Files: 0  Occurrences: 0")
    } else {
      val matches = result.stdout.linesIterator.toList
      reportMatches(matches, summary)
    }
  }

  private def scanJavaSyntax(summary: Boolean): Unit = {
    println("=== remaining Java syntax ===")
    val result = rg(List("\\b(public|static|void|boolean|implements)\\b", Paths.sgeSrc, "--type", "scala", "-n"))
    if (!result.ok || result.stdout.trim.isEmpty) {
      println("  (none)")
      println()
      println("  Files: 0  Occurrences: 0")
    } else {
      val matches = result.stdout.linesIterator.filter { line =>
        !line.contains("//") &&
        !line.contains("/*") &&
        !line.contains("*") &&
        !line.matches(".*\".*\".*")
      }.toList
      reportMatches(matches, summary)
    }
  }

  private def scanTodo(summary: Boolean): Unit = {
    println("=== TODO/FIXME/HACK/XXX markers ===")
    val result = rg(List("\\b(TODO|FIXME|HACK|XXX)\\b", Paths.sgeSrc, "--type", "scala", "-n"))
    if (!result.ok || result.stdout.trim.isEmpty) {
      println("  (none)")
      println()
      println("  Files: 0  Occurrences: 0")
    } else {
      val matches = result.stdout.linesIterator.toList
      reportMatches(matches, summary)
    }
  }

  private def reportMatches(matches: List[String], summary: Boolean): Unit = {
    if (matches.isEmpty) {
      println("  (none)")
      println()
      println("  Files: 0  Occurrences: 0")
    } else {
      if (!summary) {
        matches.foreach(println)
      }
      val files = matches.map(_.split(":").head).distinct
      println()
      println(s"  Files: ${files.size}  Occurrences: ${matches.size}")
      if (!summary) {
        println()
        println("  Top files:")
        val fileCounts = matches.map(_.split(":").head).groupBy(identity).map { case (f, occ) => (f, occ.size) }
        fileCounts.toList.sortBy(-_._2).take(10).foreach { case (f, c) =>
          println(f"    $c%4d $f%s")
        }
      }
    }
  }

  private def grep(args: Cli.Args): Unit = {
    val pattern = args.requirePositional(0, "pattern")
    if (args.hasFlag("count")) {
      val result = rg(List("-c", pattern, Paths.sgeSrc, "--type", "scala"))
      if (result.ok) {
        var totalFiles = 0
        var totalOcc = 0
        result.stdout.linesIterator.foreach { line =>
          println(line)
          val count = line.split(":").lastOption.flatMap(_.trim.toIntOption).getOrElse(0)
          if (count > 0) { totalFiles += 1; totalOcc += count }
        }
        println()
        println(s"  Files: $totalFiles  Occurrences: $totalOcc")
      }
    } else if (args.hasFlag("files-only")) {
      val result = rg(List("-l", pattern, Paths.sgeSrc, "--type", "scala"))
      if (result.ok) println(result.stdout)
    } else {
      rgExec(List(pattern, Paths.sgeSrc, "--type", "scala"))
    }
  }

  private def count(args: Cli.Args): Unit = {
    val pattern = args.requirePositional(0, "pattern")
    val result = rg(List("-c", pattern, Paths.sgeSrc, "--type", "scala"))
    if (result.ok) {
      var totalFiles = 0
      var totalOcc = 0
      result.stdout.linesIterator.foreach { line =>
        println(line)
        val count = line.split(":").lastOption.flatMap(_.trim.toIntOption).getOrElse(0)
        if (count > 0) { totalFiles += 1; totalOcc += count }
      }
      println()
      println(s"  Files: $totalFiles  Occurrences: $totalOcc")
    }
  }

  /** Run rg (ripgrep) and capture output. */
  private def rg(args: List[String]): Proc.Result = {
    // Try common rg locations
    val rgPath = findRg()
    Proc.run(rgPath, args, cwd = Some(Paths.projectRoot))
  }

  /** Run rg (ripgrep) and stream output. */
  private def rgExec(args: List[String]): Unit = {
    val rgPath = findRg()
    Proc.exec(rgPath, args, cwd = Some(Paths.projectRoot))
  }

  private def findRg(): String = {
    // Check common locations
    val candidates = List("/opt/homebrew/bin/rg", "/usr/local/bin/rg", "/usr/bin/rg", "rg")
    candidates.find(p => new java.io.File(p).exists()).getOrElse("rg")
  }
}
