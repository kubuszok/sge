package sge.port.full

import balticporter.runner.ClasspathCache

import java.nio.file.Path

/** The lls jar every port that retargets to `lowlevel.util.*` needs on its frontend classpath, so `FrontendConfig.internTypes` can read `isFinal` and parents from the class file. ONE definition;
  * every migrator calls `entries`.
  */
object LlsClasspath {

  val Coordinates: List[String] = List("com.kubuszok:lls_3:0.3.0")

  /** Only lls's own class files are wanted; the Scala stdlib is excluded. NOT for the gdx TEST port: with this jar on its Spoon classpath the test port reads 218 errors, so `LibgdxTestMigrate` keeps
    * `Nil`.
    */
  private val ExcludeArgs: List[String] = List(
    "--exclude",
    "org.scala-lang:scala3-library_3",
    "--exclude",
    "org.scala-lang:scala-library"
  )

  def cache(repoRoot: Path): Path = repoRoot.resolve("out/lls-classpath.txt")

  def entries(repoRoot: Path): List[Path] =
    ClasspathCache.entries(cache(repoRoot), "lls", Coordinates, ExcludeArgs)
}
