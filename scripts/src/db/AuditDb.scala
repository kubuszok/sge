package sgedev
package db

import java.io.File
import java.time.LocalDate

/** Audit results database operations. */
object AuditDb {

  private val headers = List("file_path", "package", "status", "tested", "last_audited", "notes", "source")

  def run(args: List[String]): Unit = {
    args match {
      case Nil | "--help" :: _ =>
        println("""Usage: sge-dev db audit <command>
                  |
                  |Commands:
                  |  list [--status S] [--package P] [--tested T] [--limit N] [--offset N]
                  |  get <file_path>
                  |  set <file_path> --status S [--tested T] [--notes TEXT]
                  |  import     Parse all docs/audit/*.md
                  |  stats      Summary counts""".stripMargin)
      case "list" :: rest => list(Cli.parse(rest))
      case "get" :: rest => get(Cli.parse(rest))
      case "set" :: rest => set(Cli.parse(rest))
      case "import" :: _ => importAudit()
      case "stats" :: _ => stats()
      case other :: _ =>
        Term.err(s"Unknown audit command: $other")
        sys.exit(1)
    }
  }

  def list(args: Cli.Args): Unit = {
    var table = load()
    args.flag("status").foreach(s => table = table.filter(_.getOrElse("status", "") == s))
    args.flag("package").foreach(p => table = table.filter(_.getOrElse("package", "").contains(p)))
    args.flag("tested").foreach(t => table = table.filter(_.getOrElse("tested", "") == t))
    table = table.paginate(
      args.flag("limit").map(_.toInt),
      args.flag("offset").map(_.toInt)
    )
    printTable(table)
  }

  def get(args: Cli.Args): Unit = {
    val path = args.requirePositional(0, "file_path")
    val table = load()
    table.find(r => r.getOrElse("file_path", "").contains(path)) match {
      case Some(row) =>
        headers.foreach(h => println(s"  $h: ${row.getOrElse(h, "")}"))
      case None =>
        Term.err(s"Not found: $path")
        sys.exit(1)
    }
  }

  def set(args: Cli.Args): Unit = {
    val path = args.requirePositional(0, "file_path")
    val updates = scala.collection.mutable.Map.empty[String, String]
    args.flag("status").foreach(s => updates("status") = s)
    args.flag("tested").foreach(t => updates("tested") = t)
    args.flag("notes").foreach(n => updates("notes") = n)
    updates("last_audited") = LocalDate.now().toString

    var table = load()
    val found = table.rows.exists(_.getOrElse("file_path", "") == path)
    if (found) {
      table = table.updateRow(_.getOrElse("file_path", "") == path, updates.toMap)
    } else {
      val pkg = path.split("/").dropRight(1).lastOption.getOrElse("")
      val row = Map(
        "file_path" -> path,
        "package" -> pkg,
        "status" -> updates.getOrElse("status", "pass"),
        "tested" -> updates.getOrElse("tested", "no"),
        "last_audited" -> updates.getOrElse("last_audited", LocalDate.now().toString),
        "notes" -> updates.getOrElse("notes", "")
      )
      table = table.addRow(row)
    }
    save(table)
    Term.ok(s"Updated: $path")
  }

  def importAudit(): Unit = {
    val auditDir = s"${Paths.docsDir}/audit"
    val dir = new File(auditDir)
    if (!dir.exists() || !dir.isDirectory) {
      Term.err(s"Audit directory not found: $auditDir")
      sys.exit(1)
    }

    val rows = scala.collection.mutable.ListBuffer.empty[Map[String, String]]

    val mdFiles = dir.listFiles().filter(f => f.getName.endsWith(".md") && f.getName != "README.md")
      .sortBy(_.getName)

    for (mdFile <- mdFiles) {
      val lines = scala.io.Source.fromFile(mdFile).getLines().toList
      val pkgName = mdFile.getName.stripSuffix(".md").replace("-", ".")

      // Extract last_audited date from header if present
      val auditDate = lines.collectFirst {
        case line if line.contains("Last updated:") || line.contains("Audited:") =>
          extractDate(line)
      }.flatten.getOrElse(LocalDate.now().toString)

      // Try both format variants
      val formatARows = parseFormatA(lines, pkgName, auditDate)
      val formatBRows = parseFormatB(lines, pkgName, auditDate)

      // Use whichever format found more entries
      if (formatARows.size >= formatBRows.size) {
        rows ++= formatARows
      } else {
        rows ++= formatBRows
      }
    }

    // Deduplicate by file_path (keep last)
    val deduped = rows.toList.groupBy(_.getOrElse("file_path", "")).map(_._2.last).toList
      .sortBy(r => r.getOrElse("package", "") + "/" + r.getOrElse("file_path", ""))

    val table = Tsv.Table(headers, deduped, List("# SGE Audit Database"))
    save(table)
    Term.ok(s"Imported ${deduped.size} audit entries from ${mdFiles.length} files")
  }

  /** Format A: Detailed per-file entries with `| Field | Value |` tables.
    * Each file gets a `### FileName.scala` section followed by a metadata table.
    */
  private def parseFormatA(lines: List[String], pkgName: String, defaultDate: String): List[Map[String, String]] = {
    val rows = scala.collection.mutable.ListBuffer.empty[Map[String, String]]
    var i = 0
    while (i < lines.size) {
      val line = lines(i)

      // Match ### FileName.scala section headers
      if (line.startsWith("### ") && !line.contains("SGE-Original") && !line.contains("Summary")) {
        val headerText = line.drop(4).trim
        // Could be "### FileName.scala" or "### FileName.scala — status"
        val fileName = headerText.split("\\s*[—–-]\\s*").head.trim
          .replaceAll("`", "").replaceAll("\\[|\\]\\(.*?\\)", "").trim

        if (fileName.endsWith(".scala") || fileName.endsWith(".java")) {
          // Look ahead for a metadata table
          var status = "pass"
          var tested = "no"
          var notes = ""
          var j = i + 1

          // Check for "— status" in the header line
          val headerStatus = extractStatusFromHeader(headerText)
          if (headerStatus.nonEmpty) status = headerStatus

          // Scan ahead for metadata table or content
          while (j < lines.size && !lines(j).startsWith("### ") && !lines(j).startsWith("---")) {
            val tableLine = lines(j)
            if (tableLine.startsWith("| ") && tableLine.contains("|")) {
              val cells = tableLine.split("\\|").map(_.trim).filter(_.nonEmpty)
              if (cells.length >= 2) {
                val key = cells(0).toLowerCase
                val value = cells(1).trim
                if (key.contains("status")) {
                  status = normalizeStatus(value)
                } else if (key.contains("tested")) {
                  tested = normalizeTested(value)
                }
              }
            }
            // Capture completeness/issues/renames as notes
            if (tableLine.startsWith("**Completeness**") || tableLine.startsWith("**Issues**") ||
                tableLine.startsWith("**TODOs**")) {
              val noteText = tableLine.replaceAll("\\*\\*.*?\\*\\*:?\\s*", "").trim
              if (noteText.nonEmpty && noteText != "None" && noteText != "none") {
                if (notes.nonEmpty) notes += "; "
                notes += noteText
              }
            }
            j += 1
          }

          rows += Map(
            "file_path" -> fileName,
            "package" -> pkgName,
            "status" -> status,
            "tested" -> tested,
            "last_audited" -> defaultDate,
            "notes" -> notes
          )
        }
      }
      i += 1
    }
    rows.toList
  }

  /** Format B: Compact list format with `### FileName.scala — status` headers. */
  private def parseFormatB(lines: List[String], pkgName: String, defaultDate: String): List[Map[String, String]] = {
    val rows = scala.collection.mutable.ListBuffer.empty[Map[String, String]]
    var i = 0
    while (i < lines.size) {
      val line = lines(i)

      if (line.startsWith("### ") && !line.contains("Summary")) {
        val headerText = line.drop(4).trim
        val parts = headerText.split("\\s*[—–-]\\s*", 2)
        val fileName = parts.head.trim.replaceAll("`", "").trim

        if (fileName.endsWith(".scala") || fileName.endsWith(".java")) {
          val status = if (parts.length > 1) normalizeStatus(parts(1).trim) else "pass"
          val notes = if (i + 1 < lines.size && !lines(i + 1).startsWith("#") && lines(i + 1).trim.nonEmpty) {
            lines(i + 1).trim
          } else { "" }

          rows += Map(
            "file_path" -> fileName,
            "package" -> pkgName,
            "status" -> status,
            "tested" -> "no",
            "last_audited" -> defaultDate,
            "notes" -> notes
          )
        }

        // Also handle grouped entries like: - **FileName.scala** — description
        if (headerText.contains("SGE-Original") || headerText.contains("(") && headerText.contains("N/A")) {
          // Scan the bullet list below
          var j = i + 1
          while (j < lines.size && (lines(j).startsWith("- ") || lines(j).startsWith("  "))) {
            val bulletLine = lines(j).replaceAll("^-\\s*", "").trim
            val bulletFileName = bulletLine.replaceAll("\\*\\*", "").split("\\s*[—–-]\\s*").head.trim
            if (bulletFileName.endsWith(".scala") || bulletFileName.endsWith(".java")) {
              rows += Map(
                "file_path" -> bulletFileName,
                "package" -> pkgName,
                "status" -> "na",
                "tested" -> "no",
                "last_audited" -> defaultDate,
                "notes" -> "SGE-original"
              )
            }
            j += 1
          }
        }
      }

      // Also parse summary tables like: | `File.scala` | pass | Yes | notes |
      if (line.startsWith("| ") && !line.startsWith("| File") && !line.startsWith("| ---") &&
          !line.startsWith("| Field")) {
        val cells = line.split("\\|").map(_.trim).filter(_.nonEmpty)
        if (cells.length >= 2) {
          val fileName = cells(0).replaceAll("`", "").replaceAll("\\[|\\]\\(.*?\\)", "").trim
          if (fileName.endsWith(".scala") || fileName.endsWith(".java")) {
            val status = normalizeStatus(cells.lift(1).getOrElse("pass"))
            val tested = normalizeTested(cells.lift(2).getOrElse("no"))
            val notes = cells.lift(3).getOrElse("").trim

            rows += Map(
              "file_path" -> fileName,
              "package" -> pkgName,
              "status" -> status,
              "tested" -> tested,
              "last_audited" -> defaultDate,
              "notes" -> notes
            )
          }
        }
      }

      i += 1
    }
    rows.toList
  }

  private def extractStatusFromHeader(header: String): String = {
    val lower = header.toLowerCase
    if (lower.contains("pass")) "pass"
    else if (lower.contains("minor")) "minor_issues"
    else if (lower.contains("major")) "major_issues"
    else if (lower.contains("n/a") || lower.contains("na")) "na"
    else ""
  }

  private def normalizeStatus(s: String): String = {
    val lower = s.toLowerCase.replaceAll("[*`]", "").trim
    if (lower.contains("pass") || lower == "✓" || lower == "ok") "pass"
    else if (lower.contains("minor")) "minor_issues"
    else if (lower.contains("major")) "major_issues"
    else if (lower.contains("n/a") || lower == "na") "na"
    else "pass"
  }

  private def normalizeTested(s: String): String = {
    val lower = s.toLowerCase.replaceAll("[*`]", "").trim
    if (lower.startsWith("yes") || lower == "✓") "yes"
    else if (lower.contains("partial")) "partial"
    else "no"
  }

  private def extractDate(line: String): Option[String] = {
    val datePattern = "(\\d{4}-\\d{2}-\\d{2})".r
    datePattern.findFirstIn(line)
  }

  def stats(): Unit = {
    val table = load()
    println("=== Audit Summary ===")
    val byStatus = table.stats("status").toList.sortBy(-_._2)
    byStatus.foreach { case (s, c) => println(f"  $s%-20s $c%d") }
    println(f"  ${"Total"}%-20s ${table.size}%d")
    println()
    println("=== By Package ===")
    val byPkg = table.stats("package").toList.sortBy(_._1)
    byPkg.foreach { case (pkg, c) => println(f"  $pkg%-30s $c%d") }
    println()
    println("=== Test Coverage ===")
    val byTested = table.stats("tested").toList.sortBy(-_._2)
    byTested.foreach { case (t, c) => println(f"  $t%-15s $c%d") }
  }

  private def load(): Tsv.Table = {
    val path = Paths.auditTsv
    if (new File(path).exists()) Tsv.read(path)
    else Tsv.Table(headers, Nil, List("# SGE Audit Database"))
  }

  private def save(table: Tsv.Table): Unit = {
    Tsv.write(Paths.auditTsv, table)
  }

  private def printTable(table: Tsv.Table): Unit = {
    if (table.rows.isEmpty) { println("(no results)"); return }
    val display = List("file_path", "package", "status", "tested", "notes")
    println(Term.table(display, table.rows.map(r => display.map(h => r.getOrElse(h, "")))))
  }
}
