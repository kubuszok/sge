package sge.port.ext.simplegraphs

import balticporter.runner.ClasspathCache
import balticporter.runner.PortConfig

import java.nio.file.{ Files, Path }

/** Port simple-graphs' own JUnit suite (`src/test/java`) through the same pipeline as `src/main/java`. The port is `.../ports/simplegraphs/test.conf`; 7 files, 16 `@Test` — the only behavioural
  * evidence this port can have. Exercises `Comparator` ordering, `java.util.stream` chain collapse, a colliding `hashCode`, and the collection surface a re-parented class covers. A DEPENDENT of
  * [[SimpleGraphsMigrate]] via `base = "main.conf"`.
  */
object SimpleGraphsTestMigrate:

  def main(args: Array[String]): Unit =
    SimpleGraphsClasspath.ensure(SimpleGraphsPort.repoRoot)
    PortConfig.load(SimpleGraphsPort.conf("test.conf"), args.toSeq).execute()

/** simple-graphs' TEST-scope dependency, for shadow-class resolution only — JUnit 4 (`build.gradle` declares exactly `junit:junit:4.12`). `TestFrameworkTransform` converts the JUnit surface to MUnit,
  * so the jar is a frontend input only. Written to a FILE rather than inlined, since a config naming a COMMAND is the strings-that-are-secretly-code the transform SPI exists to keep out.
  */
object SimpleGraphsClasspath:

  def cache(repoRoot: Path): Path = repoRoot.resolve("out/simplegraphs-test-classpath.txt")

  /** the version simple-graphs' own `build.gradle` declares. */
  val Coordinates: List[String] = List("junit:junit:4.12")

  /** Guarantee the cache file exists AND was resolved from the coordinates above, fetching once if not. A failure is FATAL rather than an empty classpath: an unresolved
    * `import static org.junit.Assert.assertEquals` resolves WRONGLY rather than failing, and the port emits nonsense and reports success.
    */
  def ensure(repoRoot: Path): Path =
    ClasspathCache.ensure(cache(repoRoot), "simple-graphs-test", Coordinates)
