package sgedev
package compare

import java.io.File

/** LibGDX/SGE file comparison commands. */
object CompareCmd {

  def run(args: List[String]): Unit = {
    args match {
      case Nil | "--help" :: _ =>
        println("""Usage: sge-dev compare <command>
                  |
                  |Commands:
                  |  file <path>         Show Java and Scala file paths
                  |  package <pkg>       List files in a libgdx package
                  |  find <pattern>      Find files in libgdx
                  |  status [pkg]        Migration status (from db)
                  |  next-batch [-n N]   Suggest next files to convert""".stripMargin)
      case "file" :: rest => file(Cli.parse(rest))
      case "package" :: rest => listPackage(Cli.parse(rest))
      case "find" :: rest => findFiles(Cli.parse(rest))
      case "status" :: rest => status(Cli.parse(rest))
      case "next-batch" :: rest => nextBatch(Cli.parse(rest))
      case other :: _ =>
        Term.err(s"Unknown compare command: $other")
        sys.exit(1)
    }
  }

  private def file(args: Cli.Args): Unit = {
    val path = args.requirePositional(0, "path")
    val javaFile = s"${Paths.gdxSrc}/$path.java"
    val scalaFile = s"${Paths.sgeSrc}/$path.scala"
    val javaExists = new File(javaFile).exists()
    val scalaExists = new File(scalaFile).exists()

    println(s"  Java:  $javaFile ${if (javaExists) Term.green("(exists)") else Term.red("(missing)")}")
    println(s"  Scala: $scalaFile ${if (scalaExists) Term.green("(exists)") else Term.red("(missing)")}")
  }

  private def listPackage(args: Cli.Args): Unit = {
    val pkg = args.requirePositional(0, "package")
    val dir = new File(s"${Paths.gdxSrc}/$pkg")
    if (!dir.exists() || !dir.isDirectory) {
      Term.err(s"Package not found: $pkg")
      sys.exit(1)
    }
    val files = dir.listFiles().filter(_.getName.endsWith(".java")).sortBy(_.getName)
    files.foreach(f => println(s"  ${f.getName}"))
    println(s"\n  Total: ${files.length} files")
  }

  private def findFiles(args: Cli.Args): Unit = {
    val pattern = args.requirePositional(0, "pattern")
    // Walk libgdx source tree
    val root = new File(Paths.gdxSrc)
    findRecursive(root, pattern).foreach(f => println(s"  ${f.getPath}"))
  }

  private def findRecursive(dir: File, pattern: String): List[File] = {
    if (!dir.exists()) return Nil
    val files = dir.listFiles().toList
    val matches = files.filter(f => f.getName.contains(pattern))
    val subdirMatches = files.filter(_.isDirectory).flatMap(findRecursive(_, pattern))
    matches ++ subdirMatches
  }

  private def status(args: Cli.Args): Unit = {
    val pkg = args.positional.headOption
    pkg match {
      case Some(p) =>
        db.MigrationDb.list(Cli.Args(Map("package" -> p), Nil))
      case None =>
        db.MigrationDb.stats()
    }
  }

  private def nextBatch(args: Cli.Args): Unit = {
    val n = args.flagOrDefault("n", "5").toInt
    val table = Tsv.read(Paths.migrationTsv)
    val notStarted = table.filter(_.getOrElse("status", "") == "not_started")
    val batch = notStarted.rows.take(n)
    if (batch.isEmpty) {
      println("No unconverted files remaining!")
    } else {
      batch.foreach(r => println(s"  ${r.getOrElse("source_path", "")}"))
    }
  }
}
