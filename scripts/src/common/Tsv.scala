package sgedev

import java.io.{BufferedReader, BufferedWriter, FileReader, FileWriter}
import scala.collection.mutable.ListBuffer

/** TSV reader/writer with typed headers and comment support. */
object Tsv {

  /** A TSV file with header names and data rows. */
  final case class Table(
    headers: List[String],
    rows: List[Map[String, String]],
    comments: List[String] = Nil
  ) {
    def filter(pred: Map[String, String] => Boolean): Table = copy(rows = rows.filter(pred))

    def find(pred: Map[String, String] => Boolean): Option[Map[String, String]] = rows.find(pred)

    def sortBy(key: String): Table = copy(rows = rows.sortBy(_.getOrElse(key, "")))

    def size: Int = rows.size

    /** Apply --limit and --offset pagination. */
    def paginate(limit: Option[Int], offset: Option[Int]): Table = {
      var r = rows
      offset.foreach(o => r = r.drop(o))
      limit.foreach(l => r = r.take(l))
      copy(rows = r)
    }

    def addRow(row: Map[String, String]): Table = copy(rows = rows :+ row)

    def updateRow(pred: Map[String, String] => Boolean, updates: Map[String, String]): Table = {
      copy(rows = rows.map { row =>
        if (pred(row)) row ++ updates else row
      })
    }

    def stats(key: String): Map[String, Int] = {
      rows.groupBy(_.getOrElse(key, "(empty)")).map { case (k, v) => k -> v.size }
    }
  }

  /** Read a TSV file. Lines starting with # are comments. First non-comment line with a leading # is the header. */
  def read(path: String): Table = {
    val reader = new BufferedReader(new FileReader(path))
    try {
      val comments = ListBuffer.empty[String]
      val dataRows = ListBuffer.empty[List[String]]
      var headers: List[String] = Nil
      var headerFound = false
      var line = reader.readLine()
      while (line != null) {
        if (!headerFound && line.startsWith("# ") && line.contains("\t")) {
          // Header line: "# col1\tcol2\tcol3"
          headers = line.drop(2).split("\t", -1).map(_.trim).toList
          headerFound = true
        } else if (line.startsWith("#")) {
          comments += line
        } else if (line.trim.nonEmpty) {
          if (!headerFound) {
            // First data line with no explicit header — try to detect as header
            val fields = line.split("\t", -1).toList
            if (fields.forall(f => f.nonEmpty && !f.exists(_.isDigit))) {
              headers = fields.map(_.trim)
              headerFound = true
            } else {
              // No header, use indices
              headers = fields.indices.map(i => s"col$i").toList
              headerFound = true
              dataRows += fields.map(unquote)
            }
          } else {
            dataRows += line.split("\t", -1).map(unquote).toList
          }
        }
        line = reader.readLine()
      }
      val rows = dataRows.toList.map { fields =>
        headers.zip(fields.padTo(headers.size, "")).toMap
      }
      Table(headers, rows, comments.toList)
    } finally {
      reader.close()
    }
  }

  /** Write a TSV table to a file. */
  def write(path: String, table: Table): Unit = {
    val writer = new BufferedWriter(new FileWriter(path))
    try {
      // Write comments
      table.comments.foreach { c =>
        writer.write(c)
        writer.newLine()
      }
      // Write header
      writer.write("# " + table.headers.mkString("\t"))
      writer.newLine()
      // Write rows
      table.rows.foreach { row =>
        val line = table.headers.map(h => quote(row.getOrElse(h, ""))).mkString("\t")
        writer.write(line)
        writer.newLine()
      }
    } finally {
      writer.close()
    }
  }

  private def quote(value: String): String = {
    if (value.contains("\t") || value.contains("\n") || value.contains("\"")) {
      "\"" + value.replace("\"", "\"\"") + "\""
    } else {
      value
    }
  }

  private def unquote(value: String): String = {
    val trimmed = value.trim
    if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length >= 2) {
      trimmed.substring(1, trimmed.length - 1).replace("\"\"", "\"")
    } else {
      trimmed
    }
  }
}
