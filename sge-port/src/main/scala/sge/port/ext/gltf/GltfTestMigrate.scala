package sge.port.ext.gltf

import balticporter.runner.ClasspathCache
import balticporter.core.{ FrontendConfig, Provenance, RuntimeMode }
import balticporter.runner.{ Determinism, PortRun, SourceSet, VendoredCommit }

import java.nio.file.{ Files, Path }

/** Port gdx-gltf's own JUnit suite through the same pipeline as `gltf/src`. A TEST FILE IS NOT A TEST (jbump/anim8 lesson again): `gltf/test` holds seven Java files but only ONE has any `@Test`
  * (eight, on `Attribute.compareTo`); the other six are LWJGL demos with `Test` in the name. [[testFiles]] names the real file; `gltf-measure` re-derives the count from the whole tree rather than
  * trusting this comment. A DEPENDENT of a DEPENDENT (via [[GltfMigrate]]).
  */
object GltfTestMigrate:

  /** The one file in `gltf/test` that is a suite. See the class comment for the other six. */
  val testFiles: List[String] =
    List("net/mgsx/gltf/scene3d/attributes/AttributesCompareTest.java")

  def main(args: Array[String]): Unit =
    val repoRoot = Path.of(sys.props.getOrElse("balticporter.root", ".")).toAbsolutePath.normalize
    val gltfSrc  = repoRoot.resolve("original-src/gdx-gltf/gltf/src").normalize
    val testRoot = repoRoot.resolve("original-src/gdx-gltf/gltf/test").normalize
    val gdxSrc   = repoRoot.resolve("original-src/libgdx/gdx/src").normalize

    // Not a glob: six of seven files are backend-driven demos no resolution root can supply.
    val missing = testFiles.filterNot(f => Files.exists(testRoot.resolve(f)))
    if missing.nonEmpty then
      System.err.println(s"[gltf-test] named test file(s) not found under $testRoot: ${missing.mkString(", ")}")
      sys.exit(1)

    PortRun(
      label = "sge-gltf-test",
      portRoot = repoRoot.resolve("ported/sge-gltf"),
      sourceSet = SourceSet.Test,
      // JUnit 4.12 (gdx-gltf's own version); an unresolved import resolves WRONGLY, not fails.
      frontend = FrontendConfig(testRoot, testFiles, GltfClasspath.resolve(repoRoot), resolutionRoots = List(gltfSrc, gdxSrc)),
      phases = Nil, // supplied by the manifest — the two sources are mutually exclusive
      manifest = Some(GltfPolicy.test(repoRoot)),
      provenance = Some(
        Provenance(
          upstreamName = "gdx-gltf",
          upstreamCommit = VendoredCommit.of(testRoot),
          originalLicense = "Apache-2.0",
          sourcePathPrefix = "gltf/test",
          sourceRoot = testRoot.toString
        )
      ),
      // The MAIN source set of this module is compiled beside this one and already resolves the
      // runtime; vendoring again would define every support type twice.
      runtimeMode = RuntimeMode.Dependency,
      determinism = Determinism.fromArgs(args.toSeq),
      nextStep = "just gltf-measure"
    ).execute()

/** gdx-gltf's TEST-scope dependency, for shadow-class resolution only. JUnit 4.12, the version `build.gradle` declares (`AshleyClasspath`'s read-the-declaration rule). Needed at FRONTEND time only;
  * `TestFrameworkTransform` converts the JUnit surface, so nothing from this jar reaches the emitted Scala.
  */
object GltfClasspath:

  /** the version `gdx-gltf/build.gradle` declares. */
  val Coordinates: List[String] = List("junit:junit:4.12")

  def resolve(repoRoot: Path): List[Path] =
    ClasspathCache.entries(repoRoot.resolve("out/gltf-test-classpath.txt"), "gltf-test", Coordinates)
