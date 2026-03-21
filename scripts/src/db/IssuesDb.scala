package sgedev
package db

import java.io.File
import java.time.LocalDate

/** Quality issues database operations. */
object IssuesDb {

  private val headers = List("id", "file_path", "category", "status", "severity", "description", "last_updated", "source")

  def run(args: List[String]): Unit = {
    args match {
      case Nil | "--help" :: _ =>
        println("""Usage: sge-dev db issues <command>
                  |
                  |Commands:
                  |  list [--status S] [--category C] [--file PATH] [--severity SEV] [--limit N] [--offset N]
                  |  add <file> <category> <severity> <description>
                  |  resolve <id> [--notes TEXT]
                  |  import     Parse all issue sources (quality-issues, bugs, reviews)
                  |  stats      Summary counts""".stripMargin)
      case "list" :: rest => list(Cli.parse(rest))
      case "add" :: rest => add(Cli.parse(rest))
      case "resolve" :: rest => resolve(Cli.parse(rest))
      case "import" :: _ => importAll()
      case "stats" :: _ => stats()
      case other :: _ =>
        Term.err(s"Unknown issues command: $other")
        sys.exit(1)
    }
  }

  def list(args: Cli.Args): Unit = {
    var table = load()
    args.flag("status").foreach(s => table = table.filter(_.getOrElse("status", "") == s))
    args.flag("category").foreach(c => table = table.filter(_.getOrElse("category", "") == c))
    args.flag("file").foreach(f => table = table.filter(_.getOrElse("file_path", "").contains(f)))
    args.flag("severity").foreach(s => table = table.filter(_.getOrElse("severity", "") == s))
    table = table.paginate(
      args.flag("limit").map(_.toInt),
      args.flag("offset").map(_.toInt)
    )
    printTable(table)
  }

  def add(args: Cli.Args): Unit = {
    val file = args.requirePositional(0, "file")
    val category = args.requirePositional(1, "category")
    val severity = args.requirePositional(2, "severity")
    val description = args.requirePositional(3, "description")
    val today = LocalDate.now().toString

    var table = load()
    val nextId = {
      val maxId = table.rows.flatMap(r => r.get("id").flatMap(parseIssueId)).maxOption.getOrElse(0)
      f"ISS-${maxId + 1}%03d"
    }
    val row = Map(
      "id" -> nextId,
      "file_path" -> file,
      "category" -> category,
      "status" -> "open",
      "severity" -> severity,
      "description" -> description,
      "last_updated" -> today
    )
    table = table.addRow(row)
    save(table)
    Term.ok(s"Added $nextId: $description")
  }

  def resolve(args: Cli.Args): Unit = {
    val id = args.requirePositional(0, "id")
    val today = LocalDate.now().toString
    val updates = scala.collection.mutable.Map[String, String](
      "status" -> "resolved",
      "last_updated" -> today
    )
    args.flag("notes").foreach(n => updates("description") = n)

    var table = load()
    val found = table.rows.exists(_.getOrElse("id", "") == id)
    if (!found) {
      Term.err(s"Issue not found: $id")
      sys.exit(1)
    }
    table = table.updateRow(_.getOrElse("id", "") == id, updates.toMap)
    save(table)
    Term.ok(s"Resolved: $id")
  }

  /** Import from ALL issue sources: quality-issues.md, bugs-and-ambiguities.md,
    * integration-test-gaps.md, and all docs/analysis/review-*.md files. */
  def importAll(): Unit = {
    val allRows = scala.collection.mutable.ListBuffer.empty[Map[String, String]]
    var issueNum = 0

    // 1. Quality issues
    val qualityPath = s"${Paths.docsDir}/progress/quality-issues.md"
    if (new File(qualityPath).exists()) {
      val (rows, nextNum) = importQualityIssues(qualityPath, issueNum)
      allRows ++= rows
      issueNum = nextNum
      Term.info(s"Parsed ${rows.size} entries from quality-issues.md")
    }

    // 2. Bugs and ambiguities
    val bugsPath = s"${Paths.docsDir}/progress/bugs-and-ambiguities.md"
    if (new File(bugsPath).exists()) {
      val (rows, nextNum) = importBugs(bugsPath, issueNum)
      allRows ++= rows
      issueNum = nextNum
      Term.info(s"Parsed ${rows.size} entries from bugs-and-ambiguities.md")
    }

    // 3. Integration test gaps
    val gapsPath = s"${Paths.docsDir}/progress/integration-test-gaps.md"
    if (new File(gapsPath).exists()) {
      val (rows, nextNum) = importTestGaps(gapsPath, issueNum)
      allRows ++= rows
      issueNum = nextNum
      Term.info(s"Parsed ${rows.size} entries from integration-test-gaps.md")
    }

    // 4. Review files (docs/analysis/review-*.md)
    val analysisDir = new File(s"${Paths.docsDir}/analysis")
    if (analysisDir.exists() && analysisDir.isDirectory) {
      val reviewFiles = analysisDir.listFiles()
        .filter(f => f.getName.startsWith("review-") && f.getName.endsWith(".md"))
        .sortBy(_.getName)
      for (reviewFile <- reviewFiles) {
        val (rows, nextNum) = importReview(reviewFile, issueNum)
        allRows ++= rows
        issueNum = nextNum
        Term.info(s"Parsed ${rows.size} entries from ${reviewFile.getName}")
      }
    }

    val table = Tsv.Table(headers, allRows.toList, List("# SGE Quality Issues Database"))
    save(table)
    Term.ok(s"Imported ${allRows.size} total issues")
  }

  private def importQualityIssues(path: String, startNum: Int): (List[Map[String, String]], Int) = {
    val lines = scala.io.Source.fromFile(path).getLines().toList
    val today = LocalDate.now().toString
    val rows = scala.collection.mutable.ListBuffer.empty[Map[String, String]]
    var currentCategory = ""
    var currentStatus = "open"
    var issueNum = startNum

    for (line <- lines) {
      if (line.startsWith("## ")) {
        val heading = line.drop(3).trim
        if (heading.contains("return")) { currentCategory = "return" }
        else if (heading.contains("null_cast") || heading.contains("null.asInstanceOf")) { currentCategory = "null_cast" }
        else if (heading.contains("null")) { currentCategory = "null" }
        else if (heading.contains("Java syntax")) { currentCategory = "java_syntax" }
        else if (heading.contains("TODO") || heading.contains("FIXME")) { currentCategory = "todo" }
        else if (heading.contains("License") || heading.contains("license")) { currentCategory = "license" }
        else if (heading.contains("ArrayBuffer")) { currentCategory = "other" }
        else { currentCategory = "other" }

        if (heading.contains("COMPLETE") || heading.contains("Complete")) {
          currentStatus = "resolved"
        } else {
          currentStatus = "open"
        }
      }

      if (line.startsWith("| ") && !line.startsWith("| Issue") && !line.startsWith("|---") && currentCategory.nonEmpty) {
        val cells = line.split("\\|").map(_.trim).filter(_.nonEmpty)
        if (cells.length >= 4) {
          val desc = cells(0).replaceAll("`", "")
          val filesAffected = cells(1)
          val status = if (cells.last.contains("Complete")) "resolved"
                       else if (cells.last.contains("Triaged")) "resolved"
                       else currentStatus
          val severity = if (filesAffected == "0") "info" else "minor"
          issueNum += 1
          rows += Map(
            "id" -> f"ISS-$issueNum%03d",
            "file_path" -> "(multiple)",
            "category" -> currentCategory,
            "status" -> status,
            "severity" -> severity,
            "description" -> s"$desc — $filesAffected files",
            "last_updated" -> today
          )
        }
      }
    }
    (rows.toList, issueNum)
  }

  private def importBugs(path: String, startNum: Int): (List[Map[String, String]], Int) = {
    val lines = scala.io.Source.fromFile(path).getLines().toList
    val today = LocalDate.now().toString
    val rows = scala.collection.mutable.ListBuffer.empty[Map[String, String]]
    var issueNum = startNum
    var inTable = false

    for (line <- lines) {
      if (line.startsWith("| Location") || line.startsWith("|---")) {
        inTable = true
      } else if (inTable && line.startsWith("| ")) {
        val cells = line.split("\\|").map(_.trim).filter(_.nonEmpty)
        if (cells.length >= 3) {
          val location = cells(0).replaceAll("`", "").trim
          val description = cells(1).trim
          val resolution = cells(2).trim
          val isResolved = resolution.toLowerCase.contains("fixed") ||
            resolution.toLowerCase.contains("removed") ||
            resolution.toLowerCase.contains("verified") ||
            resolution.toLowerCase.contains("resolved") ||
            resolution.nonEmpty
          issueNum += 1
          rows += Map(
            "id" -> f"ISS-$issueNum%03d",
            "file_path" -> location,
            "category" -> "bug",
            "status" -> (if (isResolved) "resolved" else "open"),
            "severity" -> "major",
            "description" -> s"$description — Resolution: $resolution",
            "last_updated" -> today
          )
        }
      } else if (inTable && !line.startsWith("|")) {
        inTable = false
      }
    }
    (rows.toList, issueNum)
  }

  private def importTestGaps(path: String, startNum: Int): (List[Map[String, String]], Int) = {
    val lines = scala.io.Source.fromFile(path).getLines().toList
    val today = LocalDate.now().toString
    val rows = scala.collection.mutable.ListBuffer.empty[Map[String, String]]
    var issueNum = startNum
    var currentSection = ""

    for (line <- lines) {
      if (line.startsWith("## ") || line.startsWith("### ")) {
        currentSection = line.replaceAll("#+ ", "").trim
      }
      // Parse checkboxes
      if (line.trim.startsWith("- [")) {
        val isComplete = line.contains("[x]") || line.contains("[X]")
        val description = line.replaceAll("^\\s*-\\s*\\[.\\]\\s*", "").trim
        if (description.nonEmpty) {
          issueNum += 1
          rows += Map(
            "id" -> f"ISS-$issueNum%03d",
            "file_path" -> "(integration-test)",
            "category" -> "test_gap",
            "status" -> (if (isComplete) "resolved" else "open"),
            "severity" -> "minor",
            "description" -> s"$currentSection: $description",
            "last_updated" -> today
          )
        }
      }
    }
    (rows.toList, issueNum)
  }

  private def importReview(file: File, startNum: Int): (List[Map[String, String]], Int) = {
    val lines = scala.io.Source.fromFile(file).getLines().toList
    val today = LocalDate.now().toString
    val rows = scala.collection.mutable.ListBuffer.empty[Map[String, String]]
    var issueNum = startNum
    val source = file.getName.stripSuffix(".md").replace("review-", "")

    // Detect the review format based on content patterns
    var currentSection = ""
    var currentSeverity = "medium"

    for (i <- lines.indices) {
      val line = lines(i)

      // Track section headers
      if (line.startsWith("## ") || line.startsWith("### ")) {
        currentSection = line.replaceAll("#+ ", "").trim
        // Infer severity from section name
        val lower = currentSection.toLowerCase
        if (lower.contains("blocker") || lower.contains("critical") || lower.contains("error")) {
          currentSeverity = "critical"
        } else if (lower.contains("high") || lower.contains("incorrect") || lower.contains("stub")) {
          currentSeverity = "major"
        } else if (lower.contains("medium") || lower.contains("missing")) {
          currentSeverity = "minor"
        } else if (lower.contains("low") || lower.contains("style") || lower.contains("info")) {
          currentSeverity = "info"
        }
      }

      // Pattern: RR-NNN or CR-NNN issue IDs
      val issueIdPattern = "((?:RR|CR)-\\d+)".r
      val issueIds = issueIdPattern.findAllIn(line).toList

      if (issueIds.nonEmpty) {
        val id = issueIds.head
        // Extract the description (rest of the line after the ID, or the line itself)
        val description = line.replaceAll("^\\s*[-*]?\\s*", "")
          .replaceAll("^\\*\\*.*?\\*\\*:?\\s*", "").trim

        // Try to extract file path from the line or nearby lines
        val filePathPattern = "([a-zA-Z0-9_/]+\\.scala)".r
        val filePath = filePathPattern.findFirstIn(line)
          .orElse(if (i + 1 < lines.size) filePathPattern.findFirstIn(lines(i + 1)) else None)
          .getOrElse("(review)")

        // Extract severity from the line if present
        val lineSeverity = extractSeverityFromLine(line).getOrElse(currentSeverity)

        issueNum += 1
        rows += Map(
          "id" -> f"ISS-$issueNum%03d",
          "file_path" -> filePath,
          "category" -> s"review-$source",
          "status" -> "open",
          "severity" -> lineSeverity,
          "description" -> s"[$id] $description".take(200),
          "last_updated" -> today
        )
      } else if (line.startsWith("- ") || line.startsWith("* ")) {
        // Bullet point items that might be findings without IDs
        val content = line.replaceAll("^\\s*[-*]\\s*", "").trim
        // Only capture substantial findings (>20 chars, contains a file reference or specific issue description)
        val filePathPattern = "([a-zA-Z0-9_/]+\\.scala)".r
        val hasFileRef = filePathPattern.findFirstIn(content).isDefined

        if (hasFileRef && content.length > 20 && !content.startsWith("See ") && !content.startsWith("http")) {
          val filePath = filePathPattern.findFirstIn(content).getOrElse("(review)")

          issueNum += 1
          rows += Map(
            "id" -> f"ISS-$issueNum%03d",
            "file_path" -> filePath,
            "category" -> s"review-$source",
            "status" -> "open",
            "severity" -> currentSeverity,
            "description" -> content.take(200),
            "last_updated" -> today
          )
        }
      }
    }
    (rows.toList, issueNum)
  }

  private def extractSeverityFromLine(line: String): Option[String] = {
    val lower = line.toLowerCase
    if (lower.contains("[blocker]") || lower.contains("blocker")) Some("critical")
    else if (lower.contains("[high]") || lower.contains("severity: high")) Some("major")
    else if (lower.contains("[medium]") || lower.contains("severity: medium")) Some("minor")
    else if (lower.contains("[low]") || lower.contains("severity: low")) Some("info")
    else None
  }

  def stats(): Unit = {
    val table = load()
    println("=== Issues Summary ===")
    val byStatus = table.stats("status").toList.sortBy(-_._2)
    byStatus.foreach { case (s, c) => println(f"  $s%-15s $c%d") }
    println(f"  ${"Total"}%-15s ${table.size}%d")
    println()
    println("=== By Category ===")
    val byCat = table.stats("category").toList.sortBy(-_._2)
    byCat.foreach { case (cat, c) => println(f"  $cat%-20s $c%d") }
    println()
    println("=== By Severity ===")
    val bySev = table.stats("severity").toList.sortBy(-_._2)
    bySev.foreach { case (sev, c) => println(f"  $sev%-15s $c%d") }
  }

  private def parseIssueId(id: String): Option[Int] = {
    if (id.startsWith("ISS-")) {
      try { Some(id.drop(4).toInt) }
      catch { case _: NumberFormatException => None }
    } else { None }
  }

  private def load(): Tsv.Table = {
    val path = Paths.issuesTsv
    if (new File(path).exists()) Tsv.read(path)
    else Tsv.Table(headers, Nil, List("# SGE Quality Issues Database"))
  }

  private def save(table: Tsv.Table): Unit = {
    Tsv.write(Paths.issuesTsv, table)
  }

  private def printTable(table: Tsv.Table): Unit = {
    if (table.rows.isEmpty) { println("(no results)"); return }
    val display = List("id", "file_path", "category", "status", "severity", "description")
    println(Term.table(display, table.rows.map(r => display.map(h => r.getOrElse(h, "")))))
  }
}
