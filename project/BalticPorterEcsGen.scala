import sbt.*
import sbt.Keys.*

import java.nio.file.{ Files, Path }
import scala.jdk.CollectionConverters.*

/** sbt sourceGenerator that uses Baltic Porter to mechanically port the Ashley ECS sources (v2) (`original-src/ashley/ashley/src`) into Scala 3, as a dependent of the generated sge-core.
  *
  * Requires:
  *   - sge-core generated first (its port-map.tsv must be at `target/balticporter-reports/sge-core`)
  *   - ashley sources at `original-src/ashley/ashley/src` (git submodule)
  *   - the `balticporter-engine` artifact pinned in `project/plugins.sbt`
  *   - sge's porting policy in `sge-port/` (compiled into the meta-build)
  */
object BalticPorterEcsGen {

  /** Generate sge-ecs Scala sources from Ashley Java originals. Returns the list of generated files. Generates sge-core first (cached), then runs the ashley port as a dependent.
    */
  def generate(buildBase: File, log: sbt.util.Logger): Seq[File] =
    BalticPorterGen.synchronized(generateUnlocked(buildBase, log))

  private def generateUnlocked(buildBase: File, log: sbt.util.Logger): Seq[File] = {
    val sgeRoot = buildBase.toPath.toAbsolutePath.normalize

    // Step 1: Generate sge-core (cached, writes port-map.tsv to target/balticporter-reports/sge-core).
    BalticPorterGen.generate(buildBase, log)

    val ashleySrc = sgeRoot.resolve("original-src/ashley/ashley/src")
    val portRoot  = sgeRoot.resolve("target/balticporter-sge-ecs")
    val outDir    = portRoot.resolve("src_managed/main/scala")
    val marker    = portRoot.resolve(".generated-marker")

    val forceRegen = sys.props.getOrElse("balticporter.forceRegen", "false").toBoolean
    val expected   = ecsFingerprint(sgeRoot)
    val cached     = !forceRegen && Files.exists(marker) &&
      Files.exists(outDir) &&
      Files.readString(marker).trim == expected

    if (!cached && !Files.isDirectory(ashleySrc))
      sys.error(
        "[Baltic Porter] The generated sge-ecs sources are missing or stale (" + marker + " does not read `" + expected + "`) and the ashley submodule is not " +
          "initialised. Run `git submodule update --init --depth=1 original-src/ashley`, or place a generated tree with a matching marker under " + portRoot + "."
      )

    if (!cached) {
      val libgdxSrc    = sgeRoot.resolve("original-src/libgdx/gdx/src")
      val reportRoot   = BalticPorterGen.reportRoot(buildBase)
      val commit       = balticporter.runner.VendoredCommit.of(ashleySrc)
      val overrides    = sgeRoot.resolve("sge-port/overrides")
      val frozenPolicy = sgeRoot.resolve("sge-port/derived-policy.tsv")

      log.info(s"[Baltic Porter] Generating sge-ecs sources from Ashley ($commit)")

      // Build the core manifest the same way BalticPorterGen does, then extend it with ashley's policy.
      val coreManifest = sge.port.LibgdxLadder
        .universal(
          overrides,
          provided = List(sgeRoot.resolve(BalticPorterGen.ProvidedSources)),
          upstreamResources = sgeRoot.resolve("original-src/libgdx/gdx/res"),
          frozenDerivedPolicy = Some(frozenPolicy)
        )
        .copy(baseReports = List(reportRoot))

      val manifest = sge.port.ext.ashley.AshleyPolicy.core(coreManifest, sgeRoot, reportRoots = List(reportRoot))

      // Collect the files to port: all .java under ashley/src.
      val files = Files
        .walk(ashleySrc)
        .iterator()
        .asScala
        .filter(p => p.toString.endsWith(".java"))
        .map(p => ashleySrc.relativize(p).toString.replace('\\', '/'))
        .filterNot(f => f.endsWith("package-info.java") || f.endsWith("module-info.java"))
        .toList
        .sorted

      // The classpath for resolving ashley's references to libGDX types.
      val classpathDir = sgeRoot.resolve("target/balticporter-classpath")

      // Reports under the shared report root, where this run finds its bases' port maps and the test port finds this one's.
      System.setProperty("balticporter.reportDir", reportRoot.resolve("sge-ecs").toAbsolutePath.normalize.toString)
      try {
        val result = balticporter.runner
          .PortRun(
            label = "sge-ecs",
            portRoot = portRoot,
            sourceSet = balticporter.runner.SourceSet.Main,
            // libGDX core is a RESOLUTION root: parsed so every reference resolves, never emitted here.
            frontend = balticporter.core.FrontendConfig(
              ashleySrc,
              files,
              sge.port.JnigenClasspath.entries(classpathDir),
              resolutionRoots = List(libgdxSrc)
            ),
            phases = Nil,
            manifest = Some(manifest),
            provenance = Some(
              balticporter.core.Provenance(
                upstreamName = "Ashley",
                upstreamCommit = commit,
                originalLicense = "Apache-2.0",
                sourcePathPrefix = "ashley/src",
                sourceRoot = ashleySrc.toString
              )
            ),
            // NOT `Vendored`: the core already vendors the collection shims; vendoring again
            // would define every support type twice.
            runtimeMode = balticporter.core.RuntimeMode.Dependency,
            determinism = balticporter.runner.Determinism.Emission,
            nextStep = ""
          )
          .execute()
        log.info(s"[Baltic Porter] Generated ${result.written} ecs files to $outDir")
      } finally
        System.clearProperty("balticporter.reportDir")

      Files.createDirectories(marker.getParent)
      Files.writeString(marker, expected)
    } else {
      log.info(s"[Baltic Porter] Using cached generated ecs sources ($expected)")
    }

    // Collect generated files, excluding any that also exist in sge-ecs's hand-written source tree.
    val managedRoot  = portRoot.resolve("src_managed/main")
    val ecsUnmanaged = sgeRoot.resolve("sge-extension/ecs/src/main")
    collectScalaFiles(managedRoot, excludeDuplicatesOf = Some(ecsUnmanaged))
  }

  /** Generate the ashley TEST sources. Returns the list of generated test files.
    */
  def generateTests(buildBase: File, log: sbt.util.Logger): Seq[File] =
    BalticPorterGen.synchronized(generateTestsUnlocked(buildBase, log))

  private def generateTestsUnlocked(buildBase: File, log: sbt.util.Logger): Seq[File] = {
    val sgeRoot = buildBase.toPath.toAbsolutePath.normalize

    // Ensure main sources are generated (cached).
    generate(buildBase, log)

    val ashleySrc = sgeRoot.resolve("original-src/ashley/ashley/src")
    val testRoot  = sgeRoot.resolve("original-src/ashley/ashley/tests")
    val portRoot  = sgeRoot.resolve("target/balticporter-sge-ecs")
    val outDir    = portRoot.resolve("src_managed/test/scala")
    val marker    = portRoot.resolve(".generated-test-marker")

    val forceRegen = sys.props.getOrElse("balticporter.forceRegen", "false").toBoolean
    val expected   = ecsFingerprint(sgeRoot) + " tests"
    val cached     = !forceRegen && Files.exists(marker) &&
      Files.exists(outDir) &&
      Files.readString(marker).trim == expected

    if (!cached && !Files.isDirectory(testRoot))
      sys.error(
        "[Baltic Porter] The ashley test submodule is not initialised. " +
          "Run `git submodule update --init --depth=1 original-src/ashley`."
      )

    if (!cached) {
      val libgdxSrc    = sgeRoot.resolve("original-src/libgdx/gdx/src")
      val commit       = balticporter.runner.VendoredCommit.of(testRoot)
      val overrides    = sgeRoot.resolve("sge-port/overrides")
      val frozenPolicy = sgeRoot.resolve("sge-port/derived-policy.tsv")
      val reportRoot   = BalticPorterGen.reportRoot(buildBase)

      log.info(s"[Baltic Porter] Generating sge-ecs test sources from Ashley ($commit)")

      val coreManifest = sge.port.LibgdxLadder
        .universal(
          overrides,
          provided = List(sgeRoot.resolve(BalticPorterGen.ProvidedSources)),
          upstreamResources = sgeRoot.resolve("original-src/libgdx/gdx/res"),
          frozenDerivedPolicy = Some(frozenPolicy)
        )
        .copy(baseReports = List(reportRoot))

      val manifest = sge.port.ext.ashley.AshleyPolicy.test(coreManifest, sgeRoot, reportRoots = List(reportRoot))

      val files = Files
        .walk(testRoot)
        .iterator()
        .asScala
        .filter(p => p.toString.endsWith(".java"))
        .map(p => testRoot.relativize(p).toString.replace('\\', '/'))
        .filterNot(f => f.endsWith("package-info.java") || f.endsWith("module-info.java"))
        .toList
        .sorted

      // Resolve test-scope dependencies (JUnit 4, Mockito).
      val testClasspath = sge.port.ext.ashley.AshleyClasspath.resolve(sgeRoot)
      val classpathDir  = sgeRoot.resolve("target/balticporter-classpath")

      System.setProperty("balticporter.reportDir", reportRoot.resolve("sge-ecs-test").toAbsolutePath.normalize.toString)
      try {
        val result = balticporter.runner
          .PortRun(
            label = "sge-ecs-test",
            portRoot = portRoot,
            sourceSet = balticporter.runner.SourceSet.Test,
            frontend = balticporter.core.FrontendConfig(
              testRoot,
              files,
              testClasspath ++ sge.port.JnigenClasspath.entries(classpathDir),
              resolutionRoots = List(ashleySrc, libgdxSrc)
            ),
            phases = Nil,
            manifest = Some(manifest),
            provenance = Some(
              balticporter.core.Provenance(
                upstreamName = "Ashley",
                upstreamCommit = commit,
                originalLicense = "Apache-2.0",
                sourcePathPrefix = "ashley/tests",
                sourceRoot = testRoot.toString
              )
            ),
            runtimeMode = balticporter.core.RuntimeMode.Dependency,
            determinism = balticporter.runner.Determinism.Emission,
            nextStep = ""
          )
          .execute()
        log.info(s"[Baltic Porter] Generated ${result.written} ecs test files to $outDir")
      } finally
        System.clearProperty("balticporter.reportDir")

      Files.createDirectories(marker.getParent)
      Files.writeString(marker, expected)
    } else {
      log.info(s"[Baltic Porter] Using cached generated ecs test sources ($expected)")
    }

    collectScalaFiles(portRoot.resolve("src_managed/test"), excludeDuplicatesOf = None)
  }

  /** The fingerprint for the ecs port: includes the core fingerprint and the ashley commit. */
  private def ecsFingerprint(sgeRoot: Path): String = {
    def git(dir: Path, args: String*): Option[String] = {
      val pb = new ProcessBuilder(("git" +: args)*)
      pb.directory(dir.toFile)
      pb.redirectErrorStream(true)
      val p   = pb.start()
      val out = new String(p.getInputStream.readAllBytes()).trim
      if (p.waitFor() == 0 && out.nonEmpty) Some(out) else None
    }
    val coreFingerprint = BalticPorterGen.fingerprint(sgeRoot)
    val submodule       = sgeRoot.resolve("original-src/ashley")
    val ashley          = (if (Files.exists(submodule.resolve(".git"))) git(submodule, "rev-parse", "HEAD") else None)
      .orElse(git(sgeRoot, "ls-tree", "HEAD", "original-src/ashley").flatMap(_.split("\\s+").lift(2)))
      .getOrElse(
        sys.error("[Baltic Porter] cannot read the ashley commit this checkout records")
      )
    val source    = Files.readString(sgeRoot.resolve("project/BalticPorterEcsGen.scala")).replace("\r", "")
    val generator = java.security.MessageDigest.getInstance("SHA-256").digest(source.getBytes("UTF-8")).take(8).map(b => f"$b%02x").mkString
    s"$coreFingerprint ashley=$ashley ecs-generator=$generator"
  }

  private def collectScalaFiles(dir: Path, excludeDuplicatesOf: Option[Path] = None): Seq[File] = {
    if (!Files.isDirectory(dir)) return Seq.empty
    val excluded: Set[String] = excludeDuplicatesOf
      .filter(Files.isDirectory(_))
      .map { excl =>
        val s = Files.walk(excl)
        try {
          val b = Set.newBuilder[String]
          s.forEach { p =>
            if (p.toString.endsWith(".scala")) b += excl.relativize(p).toString
          }
          b.result()
        } finally s.close()
      }
      .getOrElse(Set.empty)

    val stream = Files.walk(dir)
    try {
      val builder = Seq.newBuilder[File]
      stream.forEach { p =>
        if (p.toString.endsWith(".scala")) {
          val rel = dir.relativize(p).toString
          if (!excluded.contains(rel)) builder += p.toFile
        }
      }
      builder.result()
    } finally
      stream.close()
  }
}
