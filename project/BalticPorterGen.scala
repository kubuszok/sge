import sbt.*
import sbt.Keys.*

import java.nio.file.{ Files, Path }
import scala.jdk.CollectionConverters.*

/** sbt sourceGenerator that uses Baltic Porter to mechanically port the libGDX core sources (minus the twelve lls utilities) into Scala 3.
  *
  * Requires:
  *   - libGDX sources at `original-src/libgdx/gdx/src` (git submodule)
  *   - the `balticporter-engine` artifact pinned in `project/plugins.sbt` (the engine alone: it names no library)
  *   - sge's own porting policy in `sge-port/`: the Scala under `sge-port/src/main/scala` is compiled into this meta-build (`project/build.sbt`), the hand-written files it injects are read by path
  *     from `sge-port/overrides`, and the replacements sge compiles itself from `sge/src/main/scala` (read, never copied)
  *   - `cs` (coursier) on the PATH, to resolve the classpath libGDX's own sources are read against
  */
object BalticPorterGen {

  /** sge's own compiled tree: the replacements for the types the policy drops that sge compiles itself, read by the port for their surface and never copied. */
  val ProvidedSources = "sge/src/main/scala"

  /** Generate sge-core Scala sources from libGDX Java originals. Returns the list of generated files (shared + all platform rows). Caches by upstream commit.
    */
  // sbt evaluates the JVM/JS/Native rows' managedSources in parallel; the port writes one shared
  // output tree and hands state between runs through system properties, so the rows serialise here
  // and the later ones read the cache marker the first one wrote.
  def generate(buildBase: File, log: sbt.util.Logger): Seq[File] =
    BalticPorterGen.synchronized(generateUnlocked(buildBase, log))

  private def generateUnlocked(buildBase: File, log: sbt.util.Logger): Seq[File] = {
    val sgeRoot = buildBase.toPath.toAbsolutePath.normalize

    val libgdxSrc = sgeRoot.resolve("original-src/libgdx/gdx/src")
    val portRoot  = sgeRoot.resolve("target/balticporter-sge")
    val outDir    = portRoot.resolve("src_managed/main/scala")
    val marker    = portRoot.resolve(".generated-marker")

    // Cache key: everything the generated tree depends on (see `fingerprint`), readable without the
    // submodule's files — so a checkout that RECEIVED the generated tree (a CI job downloading the
    // `generate` job's output) reuses it and needs neither the submodule nor a generation run.
    // During development, force regeneration with -Dbalticporter.forceRegen=true, or delete
    // target/balticporter-sge/.generated-marker.
    val forceRegen = sys.props.getOrElse("balticporter.forceRegen", "false").toBoolean
    val expected   = fingerprint(sgeRoot)
    val cached     = !forceRegen && Files.exists(marker) &&
      Files.exists(outDir) &&
      Files.readString(marker).trim == expected

    if (!cached && !Files.isDirectory(libgdxSrc))
      sys.error(
        "[Baltic Porter] The generated sge-core sources are missing or stale (" + marker + " does not read `" + expected + "`) and the libGDX submodule is not " +
          "initialised. Run `git submodule update --init --depth=1 original-src/libgdx`, or place a generated tree with a matching marker under " + portRoot + "."
      )

    if (!cached) {
      val commit = balticporter.runner.VendoredCommit.of(libgdxSrc)
      log.info(s"[Baltic Porter] Generating sge-core sources from libGDX ($commit)")

      // The hand-written files the policy injects by path, and where the resolved frontend classpaths are cached.
      val overrides    = sgeRoot.resolve("sge-port/overrides")
      val classpathDir = sgeRoot.resolve("target/balticporter-classpath")

      // Step 1: Run the lls port first so its port-map.tsv is fresh and discoverable.
      // sge-core is a DEPENDENT of lls; it needs the base's published port map to answer
      // contract questions about the base's types.
      val llsReportRoot = runLlsPort(sgeRoot, classpathDir, libgdxSrc, commit, expected, log)

      // Step 2: Run the sge-core port as a dependent of the lls port.
      // The derive mechanism reads its spelling policy from a committed TSV file
      // (`sge-port/derived-policy.tsv`). The spliced members (formerly fromReference) are committed
      // as inline text in AddedMembers.scala, so no reference tree extraction is needed.
      // Platform rows: sge hand-writes its backend layers (sge/src/main/scala{jvm,js,native,desktop}); the
      // files the PORT owns per row (`async`: java's executor on JVM/Native, libGDX's GWT emulation on JS)
      // land in src_managed/<row>/scala, which build.sbt attaches to that row only (`platformSources`).
      val frozenPolicy = sgeRoot.resolve("sge-port/derived-policy.tsv")
      val manifest     = sge.port.LibgdxLadder
        .universal(
          overrides,
          provided = List(sgeRoot.resolve(ProvidedSources)),
          upstreamResources = sgeRoot.resolve("original-src/libgdx/gdx/res"),
          frozenDerivedPolicy = Some(frozenPolicy)
        )
        .copy(baseReports = List(llsReportRoot))

      // Collect the files to port: all .java under gdx/src minus the lls set.
      val files = Files
        .walk(libgdxSrc)
        .iterator()
        .asScala
        .filter(p => p.toString.endsWith(".java"))
        // slash-separated: `LlsMigrate.Files` names its files with `/`, and on Windows `relativize`
        // spells them with `\` — unnormalised, the twelve lls utilities were ported a second time
        // into sge-core and shadowed the published lls (Windows rows: 581 files written, 568 elsewhere)
        .map(p => libgdxSrc.relativize(p).toString.replace('\\', '/'))
        .filterNot(f => f.endsWith("package-info.java") || f.endsWith("module-info.java"))
        .filterNot(lowlevel.port.LlsMigrate.Files.toSet)
        .toList
        .sorted

      val result = balticporter.runner
        .PortRun(
          label = "sge-core",
          portRoot = portRoot,
          sourceSet = balticporter.runner.SourceSet.Main,
          frontend = balticporter.core.FrontendConfig(
            libgdxSrc,
            files,
            sge.port.JnigenClasspath.entries(classpathDir),
            resolutionRoots = List(libgdxSrc)
          ),
          phases = Nil,
          manifest = Some(manifest),
          provenance = Some(
            balticporter.core.Provenance(
              upstreamName = "libGDX",
              upstreamCommit = commit,
              originalLicense = "Apache-2.0",
              sourcePathPrefix = "gdx/src",
              sourceRoot = libgdxSrc.toString
            )
          ),
          runtimeMode = balticporter.core.RuntimeMode.Vendored,
          determinism = balticporter.runner.Determinism.Emission,
          nextStep = ""
        )
        .execute()
      log.info(s"[Baltic Porter] Generated ${result.written} files to $outDir")

      Files.createDirectories(marker.getParent)
      Files.writeString(marker, expected)
    } else {
      log.info(s"[Baltic Porter] Using cached generated sources ($expected)")
    }

    // Every generated file: a replacement sge compiles itself is read from its own tree (`providedSources`)
    // and never copied, so a file here that sge also declares is a duplicate definition, reported by the compiler.
    collectScalaFiles(portRoot.resolve("src_managed/main"))
  }

  /** What the generated tree depends on, as one line, readable on a shallow checkout WITHOUT the submodule's files: the engine artifact pinned in `project/plugins.sbt`, the lls-port version
    * (`Versions.lls`), the libGDX commit (the submodule's HEAD when it is initialised, else the commit this checkout records for it), this generator, the policy in `sge-port/` (sources and injected
    * files; line endings normalised, so every OS agrees), the replacements the port reads from sge's own tree ([[ProvidedSources]]) and the JDK feature version.
    */
  def fingerprint(sgeRoot: Path): String = {
    def git(dir: Path, args: String*): Option[String] = {
      val pb = new ProcessBuilder(("git" +: args)*)
      pb.directory(dir.toFile)
      pb.redirectErrorStream(true)
      val p   = pb.start()
      val out = new String(p.getInputStream.readAllBytes()).trim
      if (p.waitFor() == 0 && out.nonEmpty) Some(out) else None
    }
    val pin = """balticporter-engine" % "([^"]+)"""".r
      .findFirstMatchIn(Files.readString(sgeRoot.resolve("project/plugins.sbt")))
      .map(_.group(1))
      .getOrElse(sys.error("[Baltic Porter] project/plugins.sbt pins no balticporter-engine version"))
    // the base's policy: lls-port, at the version of the lls dependency (`project/plugins.sbt` reads the same line)
    val base      = Versions.lls
    val submodule = sgeRoot.resolve("original-src/libgdx")
    val libgdx    = (if (Files.exists(submodule.resolve(".git"))) git(submodule, "rev-parse", "HEAD") else None)
      .orElse(git(sgeRoot, "ls-tree", "HEAD", "original-src/libgdx").flatMap(_.split("\\s+").lift(2)))
      .getOrElse(
        sys.error("[Baltic Porter] cannot read the libGDX commit this checkout records (git ls-tree HEAD original-src/libgdx)")
      )
    val source    = Files.readString(sgeRoot.resolve("project/BalticPorterGen.scala")).replace("\r", "")
    val generator = java.security.MessageDigest.getInstance("SHA-256").digest(source.getBytes("UTF-8")).take(8).map(b => f"$b%02x").mkString
    // the JDK the generator runs on decides what a member overrides (`CharSequence.getChars` exists from 25 on)
    s"engine=$pin base=$base libgdx=$libgdx generator=$generator policy=${policyHash(sgeRoot.resolve("sge-port"))} provided=${policyHash(sgeRoot.resolve(ProvidedSources))} jdk=${java.lang.Runtime.version().feature()}"
  }

  /** One hash over every file under a directory (`sge-port/`: the policy's Scala and the files it injects; sge's own tree the port reads): each file's slash-separated relative path and its text
    * without carriage returns, in path order.
    */
  private def policyHash(policyRoot: Path): String = {
    if (!Files.isDirectory(policyRoot)) sys.error(s"[Baltic Porter] no porting policy: $policyRoot is not a directory")
    val digest = java.security.MessageDigest.getInstance("SHA-256")
    val stream = Files.walk(policyRoot)
    val files  =
      try
        stream
          .iterator()
          .asScala
          // a dot file (`.DS_Store`) is the operating system's, not the policy's
          .filter(p => Files.isRegularFile(p) && !p.getFileName.toString.startsWith("."))
          .map(p => policyRoot.relativize(p).toString.replace('\\', '/') -> p)
          .toList
          .sortBy(_._1)
      finally stream.close()
    files.foreach { case (rel, p) =>
      digest.update(rel.getBytes("UTF-8"))
      digest.update(0.toByte)
      digest.update(Files.readAllBytes(p).filter(_ != '\r'.toByte))
      digest.update(0.toByte)
    }
    digest.digest().take(8).map(b => f"$b%02x").mkString
  }

  /** The generated files of ONE platform row (`jvm`/`js`/`native`, sbt-projectmatrix's names): `src_managed/<row>/scala`, which only that row compiles. Generates first (cached, synchronized), so a
    * row's generator may run before or after the shared one.
    */
  def platformSources(buildBase: File, row: String, log: sbt.util.Logger): Seq[File] = {
    generate(buildBase, log)
    val sgeRoot = buildBase.toPath.toAbsolutePath.normalize
    collectScalaFiles(sgeRoot.resolve("target/balticporter-sge/src_managed").resolve(row))
  }

  /** The classpath RESOURCES the port ships, at the upstream paths the generated code names (`com/badlogic/gdx/graphics/g3d/shaders/default.vertex.glsl`, the default font, …):
    * `src_managed/main/resources`. Off the classpath, `DefaultShader` and `BitmapFont()` throw "Asset not found" at first use, on every platform.
    */
  def resources(buildBase: File, log: sbt.util.Logger): Seq[File] = {
    generate(buildBase, log)
    val root = buildBase.toPath.toAbsolutePath.normalize.resolve("target/balticporter-sge/src_managed/main/resources")
    if (!Files.isDirectory(root)) Nil
    else {
      val s = Files.walk(root)
      try s.iterator().asScala.filter(Files.isRegularFile(_)).map(_.toFile).toList.sortBy(_.getPath)
      finally s.close()
    }
  }

  /** Run the lls port (the base) and return the report root directory where its port-map.tsv was written. The sge port then uses this as `baseReports` to discover the base's contract.
    */
  private def runLlsPort(
    sgeRoot:      Path,
    classpathDir: Path,
    libgdxSrc:    Path,
    commit:       String,
    key:          String,
    log:          sbt.util.Logger
  ): Path = {
    val llsPortRoot = sgeRoot.resolve("target/balticporter-lls")
    val llsMarker   = llsPortRoot.resolve(".generated-marker")
    // The report dir is relative to the port root so the sge port can discover it.
    val reportRoot = sgeRoot.resolve("target/balticporter-reports")
    // reportDir is the CheckReport.dir; CheckReport.runDir adds "run-latest" under it.
    // So set reportDir to reportRoot/lls, and the port map ends up at reportRoot/lls/run-latest/port-map.tsv.
    val llsReportDir = reportRoot.resolve("lls")

    val llsCached = Files.exists(llsMarker) &&
      Files.exists(llsReportDir.resolve("run-latest/port-map.tsv")) &&
      Files.readString(llsMarker).trim == key

    if (!llsCached) {
      log.info(s"[Baltic Porter] Running lls base port first (required for sge-core contract)")

      // lls's own policy (the published `lls-port` artifact): it injects nothing and compares against nothing
      val manifest = lowlevel.port.LlsPolicy.core(lowlevel.port.LlsPolicy.DefaultRungs)

      // Set reportDir so the port map is written where the sge port can find it
      System.setProperty("balticporter.reportDir", llsReportDir.toAbsolutePath.normalize.toString)

      balticporter.runner
        .PortRun(
          label = "lls",
          portRoot = llsPortRoot,
          sourceSet = balticporter.runner.SourceSet.Main,
          frontend = balticporter.core.FrontendConfig(
            libgdxSrc,
            lowlevel.port.LlsMigrate.Files,
            lowlevel.port.GdxCoreClasspath.entries(classpathDir),
            resolutionRoots = Nil
          ),
          phases = Nil,
          manifest = Some(manifest),
          provenance = Some(
            balticporter.core.Provenance(
              upstreamName = "libGDX",
              upstreamCommit = commit,
              originalLicense = "Apache-2.0",
              sourcePathPrefix = "gdx/src",
              sourceRoot = libgdxSrc.toString
            )
          ),
          runtimeMode = balticporter.core.RuntimeMode.Vendored,
          determinism = balticporter.runner.Determinism.Off,
          nextStep = ""
        )
        .execute()

      // Clear reportDir so the sge port uses its own default
      System.clearProperty("balticporter.reportDir")

      Files.createDirectories(llsMarker.getParent)
      Files.writeString(llsMarker, key)
      log.info(s"[Baltic Porter] lls base port complete, port-map at $llsReportDir")
    } else {
      log.info(s"[Baltic Porter] Using cached lls base port ($commit)")
    }

    reportRoot
  }

  private def collectScalaFiles(dir: Path): Seq[File] = {
    if (!Files.isDirectory(dir)) return Seq.empty
    val stream = Files.walk(dir)
    try {
      val builder = Seq.newBuilder[File]
      stream.forEach { p =>
        if (p.toString.endsWith(".scala")) builder += p.toFile
      }
      builder.result()
    } finally
      stream.close()
  }

}
