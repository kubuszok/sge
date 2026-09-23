package sge.port.ext.visuiusl

import balticporter.runner.ClasspathCache
import balticporter.runner.PortConfig

import java.nio.file.Path

/** Port USL's own JUnit suite (`usl/src/test/java`) through the same pipeline as its main sources: `.../ports/visui-usl/test.conf`, 2 files, 7 `@Test` — seven of VisUI's nine live `@Test`s.
  * Best-shaped suite in the corpus: six live tests parse a `.usl` resource and compare against upstream's own `-expected.json`, a CONFORMANCE suite over the whole pipeline. `RemoteTest.testRemote`
  * stays `@Ignore`d. A DEPENDENT of [[UslMigrate]].
  */
object UslTestMigrate {

  def main(args: Array[String]): Unit = {
    UslTestClasspath.ensure(UslPort.repoRoot)
    PortConfig.load(UslPort.conf("test.conf"), args.toSeq).execute()
  }
}

/** USL's TEST-scope dependency, for shadow-class resolution only — JUnit 4 (`usl/build.gradle` + root `build.gradle`'s `junitVersion` pin `4.13.2`, `AshleyClasspath`'s read-the-declaration rule).
  * `TestFrameworkTransform` converts the JUnit surface to MUnit, so the jar is a frontend input only.
  */
object UslTestClasspath {

  def cache(repoRoot: Path): Path = repoRoot.resolve("out/usl-test-classpath.txt")

  /** the version `usl/build.gradle` + the root `build.gradle`'s `junitVersion` declare. */
  val Coordinates: List[String] = List("junit:junit:4.13.2")

  def ensure(repoRoot: Path): Path =
    ClasspathCache.ensure(cache(repoRoot), "usl-test", Coordinates)
}
