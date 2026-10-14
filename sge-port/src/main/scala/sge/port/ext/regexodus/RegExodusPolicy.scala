package sge.port.ext.regexodus

import balticporter.core.PortManifest
import balticporter.runner.ClasspathCache

import java.nio.file.Path

/** RegExodus' policy (`src/main/java` at 0.1.21, the version TextraTypist pins; 26 types: a pure-java regex engine descended from jregex). A BASE port: it imports nothing outside the JDK, so it
  * resolves against no other module and inherits no policy. Ported rather than replaced by `java.util.regex` so TextraTypist keeps RegExodus' own syntax and matching on every platform. `java.util`
  * stays as java wrote it: there is no hand port whose collections it would have to match.
  */
object RegExodusPolicy {

  /** `regexodus/emu` is GWT's super-source (a second `java.util.regex` and `Compatibility`), which upstream's own build does not compile for the JVM. */
  def include(f: String): Boolean = !f.startsWith("regexodus/emu/")

  def core(sgeRoot: Path): PortManifest =
    PortManifest(
      name = "sge-regexodus",
      governs = Set("regexodus"),
      packageRenames = Map("regexodus" -> "sge.regexodus"),
      surface = List(new balticporter.transform.MutableParamsTransform)
    )

  /** RegExodus' own JUnit suite, as a dependent of [[core]]. */
  def test(sgeRoot: Path, reportRoots: List[Path]): PortManifest =
    core(sgeRoot)
      .copy(baseReports = reportRoots)
      .extendedBy(
        PortManifest(
          name = "sge-regexodus-test",
          surface = List(
            new balticporter.transform.TestFrameworkTransform(),
            // LAST, deliberately: reads what the main port actually emitted
            balticporter.transform.PortMapTransform.forBasesIn(reportRoots, "sge-regexodus")
          )
        )
      )

  /** the test-scope classpath the frontend resolves the suite against: JUnit 4.13.2, RegExodus' own `build.gradle` pin. */
  def testClasspath(sgeRoot: Path): List[Path] =
    ClasspathCache.entries(sge.port.ClasspathCacheDir.file(sgeRoot, "regexodus-test-classpath.txt"), "regexodus-test", List("junit:junit:4.13.2"))
}
