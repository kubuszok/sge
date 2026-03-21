package sgedev
package db

import java.io.File
import java.time.LocalDate

/** Migration status database operations. */
object MigrationDb {

  private val headers = List("source_path", "sge_path", "status", "category", "last_updated", "notes", "source", "source_sync_commit", "last_sync_date")

  def run(args: List[String]): Unit = {
    args match {
      case Nil | "--help" :: _ =>
        println("""Usage: sge-dev db migration <command>
                  |
                  |Commands:
                  |  list [--status S] [--category C] [--package P] [--limit N] [--offset N]
                  |  get <source_path>
                  |  set <source_path> --status S [--notes TEXT]
                  |  sync       Import from all progress tracking files
                  |  stats      Summary counts""".stripMargin)
      case "list" :: rest => list(Cli.parse(rest))
      case "get" :: rest => get(Cli.parse(rest))
      case "set" :: rest => set(Cli.parse(rest))
      case "sync" :: _ => sync()
      case "stats" :: _ => stats()
      case other :: _ =>
        Term.err(s"Unknown migration command: $other")
        sys.exit(1)
    }
  }

  def list(args: Cli.Args): Unit = {
    var table = load()
    args.flag("status").foreach { s =>
      table = table.filter(_.getOrElse("status", "") == s)
    }
    args.flag("category").foreach { c =>
      table = table.filter(_.getOrElse("category", "").contains(c))
    }
    args.flag("package").foreach { p =>
      table = table.filter(_.getOrElse("source_path", "").contains(s"/$p/"))
    }
    table = table.paginate(
      args.flag("limit").map(_.toInt),
      args.flag("offset").map(_.toInt)
    )
    printTable(table)
  }

  def get(args: Cli.Args): Unit = {
    val path = args.requirePositional(0, "source_path")
    val table = load()
    table.find(r => r.getOrElse("source_path", "").contains(path)) match {
      case Some(row) =>
        headers.foreach { h =>
          println(s"  $h: ${row.getOrElse(h, "")}")
        }
      case None =>
        Term.err(s"Not found: $path")
        sys.exit(1)
    }
  }

  def set(args: Cli.Args): Unit = {
    val path = args.requirePositional(0, "source_path")
    val updates = scala.collection.mutable.Map.empty[String, String]
    args.flag("status").foreach(s => updates("status") = s)
    args.flag("notes").foreach(n => updates("notes") = n)
    updates("last_updated") = LocalDate.now().toString

    var table = load()
    val found = table.rows.exists(_.getOrElse("source_path", "") == path)
    if (!found) {
      Term.err(s"Not found: $path")
      sys.exit(1)
    }
    table = table.updateRow(
      _.getOrElse("source_path", "") == path,
      updates.toMap
    )
    save(table)
    Term.ok(s"Updated: $path")
  }

  def sync(): Unit = {
    val allRows = scala.collection.mutable.ListBuffer.empty[Map[String, String]]
    val today = LocalDate.now().toString

    // 1. Main migration status TSV
    val srcPath = s"${Paths.docsDir}/progress/migration-status.tsv"
    if (new File(srcPath).exists()) {
      val srcTable = Tsv.read(srcPath)
      val rows = srcTable.rows.map { row =>
        val libgdxPath = row.getOrElse("source_path", row.values.headOption.getOrElse(""))
        val sgePath = row.getOrElse("sge_path", "")
        val status = row.getOrElse("status", "")
        val notes = row.getOrElse("notes", "")
        val category = inferCategory(libgdxPath, sgePath)
        Map(
          "source_path" -> libgdxPath,
          "sge_path" -> sgePath,
          "status" -> status,
          "category" -> category,
          "last_updated" -> today,
          "notes" -> notes,
          "source" -> "libgdx",
          "source_sync_commit" -> "",
          "last_sync_date" -> ""
        )
      }
      allRows ++= rows
      Term.info(s"Synced ${rows.size} entries from migration-status.tsv")
    }

    // 2. Test migration status TSV
    val testTsvPath = s"${Paths.docsDir}/progress/test-migration-status.tsv"
    if (new File(testTsvPath).exists()) {
      val testTable = Tsv.read(testTsvPath)
      val testRows = testTable.rows.map { row =>
        val libgdxTest = row.getOrElse("libgdx_test", row.values.headOption.getOrElse(""))
        val sgeTest = row.getOrElse("sge_test", "")
        val status = row.getOrElse("status", "") match {
          case "migrated" => "ai_converted"
          case "skipped_no_target" => "skipped"
          case "new_sge_test" => "ai_converted"
          case other => other
        }
        val notes = row.getOrElse("notes", "")
        val category = "test"
        Map(
          "source_path" -> libgdxTest,
          "sge_path" -> sgeTest,
          "status" -> status,
          "category" -> category,
          "last_updated" -> today,
          "notes" -> notes,
          "source" -> "libgdx",
          "source_sync_commit" -> "",
          "last_sync_date" -> ""
        )
      }
      allRows ++= testRows
      Term.info(s"Synced ${testRows.size} test entries from test-migration-status.tsv")
    }

    val table = Tsv.Table(headers, allRows.toList, List("# SGE Migration Database"))
    save(table)
    Term.ok(s"Synced ${allRows.size} total entries")
  }

  def stats(): Unit = {
    val table = load()
    println("=== Migration Status ===")
    val byStatus = table.stats("status").toList.sortBy(-_._2)
    byStatus.foreach { case (status, count) =>
      println(f"  $status%-20s $count%d")
    }
    println(f"  ${"Total"}%-20s ${table.size}%d")
    println()
    println("=== By Category ===")
    val byCat = table.stats("category").toList.sortBy(-_._2)
    byCat.foreach { case (cat, count) =>
      println(f"  $cat%-25s $count%d")
    }
  }

  private def inferCategory(libgdxPath: String, sgePath: String): String = {
    if (libgdxPath.contains("backends/gdx-backend-lwjgl3") ||
        libgdxPath.contains("backends/gdx-backend-headless") ||
        sgePath.contains("platform/desktop")) {
      "backend-desktop"
    } else if (libgdxPath.contains("backends/gdx-backends-gwt") ||
               sgePath.contains("platform/browser")) {
      "backend-browser"
    } else if (libgdxPath.contains("backends/gdx-backend-android") ||
               sgePath.contains("platform/android")) {
      "backend-android"
    } else if (libgdxPath.contains("backends/gdx-backend-robovm") ||
               sgePath.contains("platform/ios")) {
      "backend-ios"
    } else {
      "core"
    }
  }

  private def load(): Tsv.Table = {
    val path = Paths.migrationTsv
    if (new File(path).exists()) {
      Tsv.read(path)
    } else {
      Tsv.Table(headers, Nil, List("# SGE Migration Database"))
    }
  }

  private def save(table: Tsv.Table): Unit = {
    Tsv.write(Paths.migrationTsv, table)
  }

  private def printTable(table: Tsv.Table): Unit = {
    if (table.rows.isEmpty) {
      println("(no results)")
      return
    }
    val display = List("source_path", "status", "category", "notes")
    val headerRow = display
    val dataRows = table.rows.map(row => display.map(h => row.getOrElse(h, "")))
    println(Term.table(headerRow, dataRows))
  }
}
