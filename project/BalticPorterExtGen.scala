import sbt.*

import java.nio.file.{ Files, Path }
import scala.jdk.CollectionConverters.*

/** sbt sourceGenerator that ports one sge extension's upstream Java library (a git submodule under `original-src/`) into Scala 3 with Baltic Porter, the way [[BalticPorterEcsGen]] ports Ashley.
  *
  * An extension is either a DEPENDENT of the generated sge-core (it resolves against libGDX: sge-core is generated first and its manifest is the base) or a BASE of its own (it imports nothing of
  * libGDX's and inherits no policy). Output: `target/balticporter-sge-<id>/src_managed/{main,test}/scala`; reports under `target/balticporter-reports/sge-<id>[-test]`.
  */
object BalticPorterExtGen {

  /** What a policy is built from. */
  final case class Ctx(sgeRoot: Path, reportRoot: Path) {

    /** the sge-core manifest, built as [[BalticPorterGen]] builds it, for a dependent to extend. */
    def core: balticporter.core.PortManifest =
      sge.port.LibgdxLadder
        .universal(
          sgeRoot.resolve("sge-port/overrides"),
          provided = List(sgeRoot.resolve(BalticPorterGen.ProvidedSources)),
          upstreamResources = sgeRoot.resolve("original-src/libgdx/gdx/res"),
          frozenDerivedPolicy = Some(sgeRoot.resolve("sge-port/derived-policy.tsv"))
        )
        .copy(baseReports = List(reportRoot))
  }

  /** One extension: its upstream tree, its policy and whether it resolves against sge-core. */
  final case class Ext(
    id:           String,
    upstreamName: String,
    license:      String,
    submodule:    String,
    mainDir:      String,
    dependent:    Boolean,
    main:         Ctx => balticporter.core.PortManifest,
    // the `sge-port/` files this extension's port reads, relative to `sge-port/` (its fingerprint)
    policy:        String => Boolean,
    testDir:       Option[String] = None,
    test:          Option[Ctx => balticporter.core.PortManifest] = None,
    classpath:     Ctx => List[Path] = _ => Nil,
    testClasspath: Ctx => List[Path] = _ => Nil,
    // a base port that retypes onto the support types ships them itself; a dependent reads core's
    runtimeMode: balticporter.core.RuntimeMode = balticporter.core.RuntimeMode.Dependency,
    // the main set's java files, relative to `mainDir` (an upstream with several source roots names them here)
    include: String => Boolean = _ => true,
    // other extensions this one depends on: generated first, so their port maps are published
    bases: List[Ext] = Nil,
    // java source roots it resolves against beyond libGDX's (a base extension's upstream tree)
    resolutionRoots: Ctx => List[Path] = _ => Nil
  ) {
    def module: String = s"sge-$id"
  }

  val Noise: Ext = Ext(
    id = "noise",
    upstreamName = "noise4j",
    license = "Apache-2.0",
    submodule = "original-src/noise4j",
    mainDir = "src",
    dependent = false,
    main = ctx => sge.port.ext.noise4j.Noise4jPolicy.core(ctx.sgeRoot),
    policy = rel => rel.startsWith("src/main/scala/sge/port/ext/noise4j/") || rel == "noise-derived-policy.tsv"
  )

  // guacamole's `core` and `gdx` modules (the GWT and desktop modules are platform backends sge does not
  // take): screenmanager's second dependency, a dependent of sge-core since its `gdx` module reads libGDX
  val Guacamole: Ext = Ext(
    id = "guacamole",
    upstreamName = "guacamole",
    license = "Apache-2.0",
    submodule = "original-src/guacamole",
    mainDir = ".",
    dependent = true,
    main = ctx => sge.port.ext.guacamole.GuacamolePolicy.core(ctx.core, List(ctx.reportRoot)),
    policy = rel => rel.startsWith("src/main/scala/sge/port/ext/guacamole/"),
    classpath = ctx => sge.port.ext.guacamole.GuacamolePolicy.classpath(ctx.sgeRoot),
    include = f => f.startsWith("core/src/main/java/") || f.startsWith("gdx/src/main/java/")
  )

  val Anim8: Ext = Ext(
    id = "anim8",
    upstreamName = "anim8-gdx",
    license = "Apache-2.0",
    submodule = "original-src/anim8-gdx",
    mainDir = "src/main/java",
    dependent = true,
    main = ctx => sge.port.ext.anim8.Anim8Policy.core(ctx.core, ctx.sgeRoot, List(ctx.reportRoot)),
    policy = rel => rel.startsWith("src/main/scala/sge/port/ext/anim8/") || rel.startsWith("overrides-ext/anim8/") || rel == "anim8-derived-policy.tsv"
  )

  // not wired into sge-graphs yet: the generated sources do not compile (ISS-906); wired, sge-graphs
  // must depend on sge, whose tree ships the java-collection support types the port extends
  val Graphs: Ext = Ext(
    id = "graphs",
    upstreamName = "simple-graphs",
    license = "MIT",
    submodule = "original-src/simple-graphs",
    mainDir = "src/main/java",
    dependent = false,
    main = ctx => sge.port.ext.simplegraphs.SimpleGraphsPolicy.core(ctx.sgeRoot),
    policy = rel => rel.startsWith("src/main/scala/sge/port/ext/simplegraphs/") || rel.startsWith("overrides-ext/graphs") || rel == "graphs-derived-policy.tsv",
    testDir = Some("src/test/java"),
    test = Some(ctx => sge.port.ext.simplegraphs.SimpleGraphsPolicy.test(ctx.sgeRoot, List(ctx.reportRoot))),
    testClasspath = ctx => sge.port.ext.simplegraphs.SimpleGraphsPolicy.testClasspath(ctx.sgeRoot)
  )

  val Jbump: Ext = Ext(
    id = "jbump",
    upstreamName = "jbump",
    license = "Apache-2.0",
    submodule = "original-src/jbump",
    mainDir = "jbump/src",
    dependent = false,
    main = ctx => sge.port.ext.jbump.JbumpPolicy.core(ctx.sgeRoot),
    policy = rel => rel.startsWith("src/main/scala/sge/port/ext/jbump/") || rel.startsWith("overrides-ext/jbump/") || rel == "jbump-derived-policy.tsv"
  )

  /** the hand port an extension's spelling policy is derived from, read only until that policy is frozen in `sge-port/<id>-derived-policy.tsv` */
  private def reference(ctx: Ctx, id: String, handDir: String): Option[Path] =
    if (Files.exists(ctx.sgeRoot.resolve(s"sge-port/$id-derived-policy.tsv"))) None
    else Some(ctx.sgeRoot.resolve(handDir)).filter(p => Files.isDirectory(p))

  private def extPolicy(id: String, pkg: String): String => Boolean =
    rel => rel.startsWith(s"src/main/scala/sge/port/ext/$pkg/") || rel == s"$id-derived-policy.tsv"

  // gdx-vfx's `core` and `effects` modules (`gwt` is GWT's emulation), a dependent of sge-core
  val Vfx: Ext = Ext(
    id = "vfx",
    upstreamName = "gdx-vfx",
    license = "Apache-2.0",
    submodule = "original-src/gdx-vfx",
    mainDir = "gdx-vfx",
    dependent = true,
    main = ctx => sge.port.ext.vfx.VfxPolicy.core(ctx.core, ctx.sgeRoot, List(ctx.reportRoot), reference(ctx, "vfx", "sge-extension/vfx/src/main/scala")),
    policy = extPolicy("vfx", "vfx"),
    include = sge.port.ext.vfx.VfxPolicy.include
  )

  // screenmanager reads guacamole (generated first into the same module) and libGDX
  val Screens: Ext = Ext(
    id = "screens",
    upstreamName = "libgdx-screenmanager",
    license = "Apache-2.0",
    submodule = "original-src/libgdx-screenmanager",
    mainDir = "src/main/java",
    dependent = true,
    main = ctx =>
      sge.port.ext.screens.ScreensPolicy.core(Guacamole.main(ctx), ctx.sgeRoot, List(ctx.reportRoot), reference(ctx, "screens", "sge-extension/screens/src/main/scala")),
    policy = rel => extPolicy("screens", "screens")(rel) || Guacamole.policy(rel),
    classpath = ctx => sge.port.ext.guacamole.GuacamolePolicy.classpath(ctx.sgeRoot),
    bases = List(Guacamole),
    resolutionRoots = ctx => List("core/src/main/java", "gdx/src/main/java").map(d => ctx.sgeRoot.resolve("original-src/guacamole").resolve(d))
  )

  /** Generate the extension's main sources. Returns the generated files. */
  def generate(ext: Ext, buildBase: File, log: sbt.util.Logger): Seq[File] =
    BalticPorterGen.synchronized(run(ext, buildBase, log, testRun = false))

  /** Generate the extension's upstream tests (main first). Returns the generated test files. */
  def generateTests(ext: Ext, buildBase: File, log: sbt.util.Logger): Seq[File] =
    BalticPorterGen.synchronized {
      run(ext, buildBase, log, testRun = false)
      if (ext.testDir.isEmpty) Nil else run(ext, buildBase, log, testRun = true)
    }

  private def run(ext: Ext, buildBase: File, log: sbt.util.Logger, testRun: Boolean): Seq[File] = {
    val sgeRoot  = buildBase.toPath.toAbsolutePath.normalize
    val setName  = if (testRun) "test" else "main"
    val upstream = sgeRoot.resolve(ext.submodule)
    val mainRoot = upstream.resolve(ext.mainDir)
    val srcRoot  = if (testRun) upstream.resolve(ext.testDir.get) else mainRoot
    val portRoot = sgeRoot.resolve(s"target/balticporter-${ext.module}")
    val outDir   = portRoot.resolve(s"src_managed/$setName/scala")
    val marker   = portRoot.resolve(if (testRun) ".generated-test-marker" else ".generated-marker")
    val label    = if (testRun) s"${ext.module}-test" else ext.module
    val expected = fingerprint(ext, sgeRoot) + (if (testRun) " tests" else "")
    val cached   = !sys.props.getOrElse("balticporter.forceRegen", "false").toBoolean && Files.exists(marker) &&
      Files.exists(outDir) && Files.readString(marker).trim == expected

    if (!cached && !Files.isDirectory(srcRoot))
      sys.error(
        s"[Baltic Porter] The generated ${ext.module} sources are missing or stale ($marker does not read `$expected`) and the ${ext.upstreamName} submodule is not initialised. " +
          s"Run `git submodule update --init --depth=1 ${ext.submodule}`."
      )

    if (!cached) {
      // a dependent reads sge-core's published port map, so sge-core is generated first (cached)
      if (ext.dependent) BalticPorterGen.generate(buildBase, log)
      ext.bases.foreach(b => run(b, buildBase, log, testRun = false))
      val reportRoot = BalticPorterGen.reportRoot(buildBase)
      val ctx        = Ctx(sgeRoot, reportRoot)
      val manifest   = (if (testRun) ext.test.get else ext.main) (ctx)
      val commit     = balticporter.runner.VendoredCommit.of(srcRoot)
      log.info(s"[Baltic Porter] Generating ${ext.module} $setName sources from ${ext.upstreamName} ($commit)")

      val files = Files
        .walk(srcRoot)
        .iterator()
        .asScala
        .filter(p => p.toString.endsWith(".java"))
        .map(p => srcRoot.relativize(p).toString.replace('\\', '/'))
        .filterNot(f => f.endsWith("package-info.java") || f.endsWith("module-info.java"))
        .filter(f => testRun || ext.include(f))
        .toList
        .sorted

      val libgdxSrc   = sgeRoot.resolve("original-src/libgdx/gdx/src")
      val resolutions = (if (testRun) List(mainRoot) else Nil) ++ (if (ext.dependent) List(libgdxSrc) else Nil) ++ ext.resolutionRoots(ctx)
      val classpath   = (if (testRun) ext.testClasspath(ctx) else Nil) ++ ext.classpath(ctx) ++
        (if (ext.dependent) sge.port.JnigenClasspath.entries(sge.port.ClasspathCacheDir(sgeRoot)) else Nil)

      System.setProperty("balticporter.reportDir", reportRoot.resolve(label).toAbsolutePath.normalize.toString)
      try {
        val result = balticporter.runner
          .PortRun(
            label = label,
            portRoot = portRoot,
            sourceSet = if (testRun) balticporter.runner.SourceSet.Test else balticporter.runner.SourceSet.Main,
            frontend = balticporter.core.FrontendConfig(srcRoot, files, classpath, resolutionRoots = resolutions),
            phases = Nil,
            manifest = Some(manifest),
            provenance = Some(
              balticporter.core.Provenance(
                upstreamName = ext.upstreamName,
                upstreamCommit = commit,
                originalLicense = ext.license,
                sourcePathPrefix = sgeRoot.resolve(ext.submodule).relativize(srcRoot).toString.replace('\\', '/'),
                sourceRoot = srcRoot.toString
              )
            ),
            runtimeMode = ext.runtimeMode,
            determinism = balticporter.runner.Determinism.Emission,
            nextStep = ""
          )
          .execute()
        log.info(s"[Baltic Porter] Generated ${result.written} ${ext.module} $setName files to $outDir")
      } finally
        System.clearProperty("balticporter.reportDir")

      Files.createDirectories(marker.getParent)
      Files.writeString(marker, expected)
    } else {
      log.info(s"[Baltic Porter] Using cached generated ${ext.module} $setName sources ($expected)")
    }

    val root = portRoot.resolve(s"src_managed/$setName")
    if (!Files.isDirectory(root)) Nil
    else {
      val s = Files.walk(root)
      try s.iterator().asScala.filter(p => p.toString.endsWith(".scala")).map(_.toFile).toList.sortBy(_.getPath)
      finally s.close()
    }
  }

  /** What an extension's generated tree depends on: sge-core's inputs (the engine pin, core's policy, the JDK), the extension's submodule commit, its own policy files and this generator. */
  private def fingerprint(ext: Ext, sgeRoot: Path): String = {
    def git(dir: Path, args: String*): Option[String] = {
      val pb = new ProcessBuilder(("git" +: args)*)
      pb.directory(dir.toFile)
      pb.redirectErrorStream(true)
      val p   = pb.start()
      val out = new String(p.getInputStream.readAllBytes()).trim
      if (p.waitFor() == 0 && out.nonEmpty) Some(out) else None
    }
    val submodule = sgeRoot.resolve(ext.submodule)
    val upstream  = (if (Files.exists(submodule.resolve(".git"))) git(submodule, "rev-parse", "HEAD") else None)
      .orElse(git(sgeRoot, "ls-tree", "HEAD", ext.submodule).flatMap(_.split("\\s+").lift(2)))
      .getOrElse(
        sys.error(
          s"[Baltic Porter] cannot read the ${ext.upstreamName} commit this checkout records (git ls-tree HEAD ${ext.submodule})"
        )
      )
    val source    = Files.readString(sgeRoot.resolve("project/BalticPorterExtGen.scala")).replace("\r", "")
    val generator = java.security.MessageDigest.getInstance("SHA-256").digest(source.getBytes("UTF-8")).take(8).map(b => f"$b%02x").mkString
    s"${BalticPorterGen.fingerprint(sgeRoot)} ${extensionInputs(ext, sgeRoot, upstream, generator)}"
  }

  /** One extension's own inputs, then each base extension's (a base's tree is an input of its dependents). */
  private def extensionInputs(ext: Ext, sgeRoot: Path, upstream: String, generator: String): String = {
    val own = s"${ext.id}=$upstream ext-generator=$generator ${ext.id}-policy=${BalticPorterGen.extensionPolicyHash(sgeRoot, ext.policy)}"
    (own :: ext.bases.map(b => fingerprint(b, sgeRoot).split(' ').dropWhile(!_.startsWith(s"${b.id}=")).mkString(" "))).mkString(" ")
  }
}
