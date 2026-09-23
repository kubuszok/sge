package sge.port.ext.gdxai

import balticporter.runner.ClasspathCache
import balticporter.core.{ FrontendConfig, Provenance, RuntimeMode }
import balticporter.runner.{ Determinism, PortRun, SourceSet, VendoredCommit }

import java.nio.file.{ Files, Path }
import scala.jdk.CollectionConverters.*

/** Migrate **gdx-ai's own JUnit suite** (`gdx-ai/tests`, 2 files / 10 `@Test`) through the TIR. Only two upstream files carry `@Test`; the separate `gdx-ai/tests` gradle project (111 files) declares
  * ZERO `@Test`, an LWJGL demo application. `ai-test-measure` censuses the two trees apart, since every wrong answer this library produced came from conflating them. A dependent OF a dependent (both
  * RESOLUTION ROOTS), manifest [[GdxAiPolicy.test]] extended.
  */
object GdxAiTestMigrate {

  def main(args: Array[String]): Unit = {
    val repoRoot = Path.of(sys.props.getOrElse("balticporter.root", ".")).toAbsolutePath.normalize
    val upstream = repoRoot.resolve("original-src/gdx-ai").normalize
    val testRoot = upstream.resolve("gdx-ai/tests").normalize
    val aiSrc    = upstream.resolve("gdx-ai/src").normalize
    val gdxSrc   = repoRoot.resolve("original-src/libgdx/gdx/src").normalize

    val files = Files
      .walk(testRoot)
      .iterator()
      .asScala
      .filter(p => p.toString.endsWith(".java"))
      .map(p => testRoot.relativize(p).toString)
      .filterNot(f => f.endsWith("package-info.java") || f.endsWith("module-info.java"))
      .toList
      .sorted

    PortRun(
      label = "sge-ai-test",
      portRoot = repoRoot.resolve("ported/sge-ai"),
      sourceSet = SourceSet.Test,
      frontend = FrontendConfig(
        testRoot,
        files,
        GdxAiTestClasspath.resolve(repoRoot),
        resolutionRoots = List(aiSrc, gdxSrc),
        // same exclusion as GdxAiMigrate, for a resolution ROOT (a
        // directory, no convert-list to filter): without it Spoon refuses
        // the model outright ("StandaloneFileSystem already defined").
        resolutionExcludes = List("com/badlogic/gdx/emu")
      ),
      phases = Nil, // supplied by the manifest — the two sources are mutually exclusive
      manifest = Some(GdxAiPolicy.test(repoRoot)),
      provenance = Some(
        Provenance(
          upstreamName = "gdx-ai",
          upstreamCommit = VendoredCommit.of(testRoot),
          originalLicense = "Apache-2.0",
          sourcePathPrefix = "gdx-ai/tests",
          sourceRoot = testRoot.toString
          // both upstream test files carry the Apache header; nothing to declare here.
        )
      ),
      // The MAIN source set of this module is compiled beside this one and already resolves the
      // runtime; vendoring again would define every support type twice.
      runtimeMode = RuntimeMode.Dependency,
      determinism = Determinism.fromArgs(args.toSeq),
      nextStep = "just ai-test-measure"
    ).execute()
  }
}

/** gdx-ai's suite dependencies, at the versions its own build declares. Cached like every other corpus suite's; RECORDED beside the cache so a coordinate-set mismatch is fatal rather than silent
  * (`ClasspathCache`).
  */
object GdxAiTestClasspath {

  /** `gdx-ai/build.gradle`'s own version, not aligned with Ashley's 4.13.2 -- guessing one cost `AshleyTestMigrate` twelve errors on a Mockito mismatch once.
    */
  val Coordinates: List[String] = List("junit:junit:4.12")

  def resolve(repoRoot: Path): List[Path] =
    ClasspathCache.entries(repoRoot.resolve("out/gdx-ai-test-classpath.txt"), "gdx-ai-test", Coordinates)
}
