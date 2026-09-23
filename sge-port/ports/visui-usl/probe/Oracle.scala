/** The PORT half of USL's conformance oracle. Its twin is `OracleJava.java` beside it, and
  * `just usl-oracle-measure` runs both and diffs the transcripts line for line. */
object Oracle:

  def main(args: Array[String]): Unit =
    val stylesDir = new java.io.File(args(0))
    val knownGood = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(args(1))),
                               java.nio.charset.StandardCharsets.UTF_8)

    val fixtures = Option(stylesDir.listFiles((_, n) => n.endsWith(".usl")))
      .getOrElse(Array.empty[java.io.File])
    if fixtures.isEmpty then
      throw new IllegalStateException("no .usl fixtures under " + stylesDir)
    // Sorted, for the reason the java half states: a directory listing's order is the filesystem's.
    val sorted = fixtures.sortBy(_.getPath)

    println("fixtures: " + sorted.length)
    for f <- sorted do
      println("=== " + f.getName + " ===")
      // A THROW is transcript content rather than a harness failure — whether the port throws where
      // java throws is exactly what this gate asks. `Throwable`, deliberately: catching less would
      // turn a divergence into a crash and lose every fixture after it.
      try
        val out = sge.visui.usl.USL.parse(f)
        println("known-good: " + (if out == knownGood then "EXACT" else "DIFFERS"))
        println(out)
      catch
        case t: Throwable =>
          println("THREW " + t.getClass.getSimpleName + ": " + String.valueOf(t.getMessage).replace('\n', ' '))
