package sgedev

import java.io.File

/** Project path resolution. */
object Paths {

  /** Find the project root by walking up to find build.sbt + sge/. */
  lazy val projectRoot: String = {
    var dir = new File(System.getProperty("user.dir"))
    var found: String = System.getProperty("user.dir") // fallback
    var searching = true
    while (dir != null && searching) {
      val buildSbt = new File(dir, "build.sbt")
      val sgeDir = new File(dir, "sge")
      if (buildSbt.exists() && sgeDir.exists()) {
        found = dir.getAbsolutePath
        searching = false
      } else {
        dir = dir.getParentFile
      }
    }
    found
  }

  def sgeSrc: String = s"$projectRoot/sge/src/main/scala/sge"
  def gdxSrc: String = s"$projectRoot/original-src/libgdx/gdx/src/com/badlogic/gdx"
  def dataDir: String = s"$projectRoot/scripts/data"
  def migrationTsv: String = s"$dataDir/migration.tsv"
  def issuesTsv: String = s"$dataDir/issues.tsv"
  def auditTsv: String = s"$dataDir/audit.tsv"
  def scriptsDir: String = s"$projectRoot/scripts"
  def docsDir: String = s"$projectRoot/docs"
}
