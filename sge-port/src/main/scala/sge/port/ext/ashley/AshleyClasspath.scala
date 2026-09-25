package sge.port.ext.ashley

import balticporter.runner.ClasspathCache

import java.nio.file.Path

/** Ashley's TEST-scope dependencies, for shadow-class resolution only. JUnit 4 and Mockito; neither is translated (`TestFrameworkTransform` converts the JUnit surface, Mockito calls survive as
  * ordinary references). Versions are Ashley's OWN (JUnit 4.13.2, Mockito 1.10.19): `ComponentClassFactory` uses `org.mockito.asm`, removed in Mockito 2.x.
  */
object AshleyClasspath {

  /** the versions Ashley's own `build.gradle` declares. */
  val Coordinates: List[String] = List("junit:junit:4.13.2", "org.mockito:mockito-core:1.10.19")

  def resolve(repoRoot: Path): List[Path] =
    ClasspathCache.entries(repoRoot.resolve("out/ashley-test-classpath.txt"), "ashley-test", Coordinates)
}
