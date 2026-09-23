package sge.port.full

import balticporter.core.{ FrontendConfig, Provenance, RuntimeMode }
import balticporter.runner.{ Determinism, PortRun, SourceSet, VendoredCommit }

import java.nio.file.{ Files, Path }
import scala.jdk.CollectionConverters.*

/** Port libGDX's own JUnit suite (`gdx/test`) through the same pipeline as `gdx/src`. The port's only BEHAVIOURAL gate (`LibgdxCoreMigrate` measures only *compiles*). 221 `@Test` methods, ~900
  * assertions. `gdx/src` is a RESOLUTION root only, ported separately by [[LibgdxCoreMigrate]]; re-emitting it here would fork the output. The pipeline arrives from `LibgdxPolicy.core`'s manifest
  * (adds only `TestFrameworkTransform`), never restated.
  */
object LibgdxTestMigrate {

  def main(args: Array[String]): Unit = {
    val repoRoot = Path.of(sys.props.getOrElse("sge.root", ".")).toAbsolutePath.normalize
    val srcRoot  = repoRoot.resolve("original-src/libgdx/gdx/src").normalize
    val testRoot = repoRoot.resolve("original-src/libgdx/gdx/test").normalize

    // EXCLUDED: CharArrayTest (30 @Test) exercises com.badlogic.gdx.utils.CharArray, retargetted
    // to DynamicArray[Char]; sge ported no CharArrayTest either (its QueueBitsTest exercises the
    // stdlib replacements directly instead). Counted by test_discovery_guard, held in expected-lost.
    val excludedFiles = Set(
      "com/badlogic/gdx/utils/CharArrayTest.java"
    )

    val files = Files
      .walk(testRoot)
      .iterator()
      .asScala
      .filter(p => p.toString.endsWith(".java"))
      .map(p => testRoot.relativize(p).toString)
      .filterNot(f => f.endsWith("package-info.java") || f.endsWith("module-info.java"))
      .filterNot(f => excludedFiles(f))
      .toList
      .sorted

    PortRun(
      label = "sge-test",
      portRoot = repoRoot.resolve("ported/sge"),
      sourceSet = SourceSet.Test,
      // classpath stays Nil: the lls jar here costs 218 errors.
      frontend = FrontendConfig(testRoot, files, Nil, resolutionRoots = List(srcRoot)),
      phases = Nil, // supplied by the manifest — the two sources are mutually exclusive
      // A DEPENDENT of `LibgdxPolicy.core`: adds TestFrameworkTransform, inherits everything else;
      // ManifestAgreement verifies the 605 resolved-but-unconverted types agree with the base.
      manifest = Some(LibgdxPolicy.test(repoRoot, repoRoot.resolve("sge-port/overrides-full"))),
      provenance = Some(
        Provenance(
          upstreamName = "libGDX",
          upstreamCommit = VendoredCommit.of(testRoot),
          originalLicense = "Apache-2.0",
          sourcePathPrefix = "gdx/test",
          sourceRoot = testRoot.toString
        )
      ),
      // NOT `Vendored`: the MAIN source set of this same module already vendored the collection
      // shims, and the two sets are compiled together. Vendoring twice defines every support type
      // twice — which the JVM tolerates only while the copies agree and the Scala.js/Native
      // linkers reject outright.
      runtimeMode = RuntimeMode.Dependency,
      // NOTHING is injected alongside the converted suites (no `supportSources`): the suites
      // retype onto munit.FunSuite, a declared dependency, never a source this run emits. The rule
      // for `balticporter-runtime`: semantics the target LACKS become a dependency; shapes the
      // engine can emit correctly are emitted, and nothing ships.
      determinism = Determinism.fromArgs(args.toSeq),
      nextStep = "just gdx-test-measure"
    ).execute()
  }
}
