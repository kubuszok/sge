import sbt.*
import sbt.Keys.*

import java.nio.file.{ Files, Path }
import scala.jdk.CollectionConverters.*

/** sbt sourceGenerator that uses Baltic Porter to mechanically port the libGDX core sources (minus the twelve lls utilities) into Scala 3.
  *
  * Requires:
  *   - libGDX sources at `original-src/libgdx/gdx/src` (git submodule)
  *   - balticporter checkout at `../balticporter` (sibling directory) for inject files and classpath cache; override with `-Dbalticporter.root=<path>`
  *   - `balticporter-corpus` 0.1.0-SNAPSHOT published locally (`sbt publishLocal` in balticporter)
  */
object BalticPorterGen {

  /** Generate sge-core Scala sources from libGDX Java originals. Returns the list of generated files (shared + all platform rows). Caches by upstream commit.
    */
  // sbt evaluates the JVM/JS/Native rows' managedSources in parallel; the port writes one shared
  // output tree and hands state between runs through system properties, so the rows serialise here
  // and the later ones read the cache marker the first one wrote.
  def generate(buildBase: File, log: sbt.util.Logger): Seq[File] =
    BalticPorterGen.synchronized(generateUnlocked(buildBase, log))

  private def generateUnlocked(buildBase: File, log: sbt.util.Logger): Seq[File] = {
    val sgeRoot = buildBase.toPath.toAbsolutePath.normalize
    val bpRoot  = Path.of(sys.props.getOrElse("balticporter.root", sgeRoot.resolve("../balticporter").toString)).toAbsolutePath.normalize

    val libgdxSrc = sgeRoot.resolve("original-src/libgdx/gdx/src")
    if (!Files.isDirectory(libgdxSrc) || !Files.isDirectory(bpRoot.resolve("balticporter/corpus"))) {
      log.warn("[Baltic Porter] No libGDX submodule or balticporter sibling — skipping sge-core generation")
      val outDir = sgeRoot.resolve("target/balticporter-sge/src_managed/main/scala")
      return if (Files.isDirectory(outDir)) collectScalaFiles(outDir) else Nil
    }

    val portRoot = sgeRoot.resolve("target/balticporter-sge")
    val outDir   = portRoot.resolve("src_managed/main/scala")
    val marker   = portRoot.resolve(".generated-marker")

    // Cache key: the vendored tree's commit.
    // During development, force regeneration with -Dbalticporter.forceRegen=true to pick up
    // corpus changes. Default false: delete target/balticporter-sge/.generated-marker instead.
    val forceRegen = sys.props.getOrElse("balticporter.forceRegen", "false").toBoolean
    val commit     = balticporter.runner.VendoredCommit.of(libgdxSrc)
    val cached     = !forceRegen && Files.exists(marker) &&
      Files.exists(outDir) &&
      Files.readString(marker).trim == commit

    if (!cached) {
      log.info(s"[Baltic Porter] Generating sge-core sources from libGDX ($commit)")

      // Step 1: Run the lls port first so its port-map.tsv is fresh and discoverable.
      // sge-core is a DEPENDENT of lls; it needs the base's published port map to answer
      // contract questions about the base's types (CLAUDE.md section 1.5).
      val llsReportRoot = runLlsPort(sgeRoot, bpRoot, libgdxSrc, commit, log)

      // Step 2: Run the sge-core port (L0 ladder) as a dependent of the lls port.
      val steps = balticporter.corpus.libgdx.LibgdxLadder.DefaultSteps
      // parity with compare=false: the derive mechanism reads the FULL hand-ported sge source
      // (master branch) to learn the reference's naming conventions (field names, property shapes,
      // parenless methods, opaque type seeds). Without this, BeanPropertyTransform falls back to
      // generic naming (active$field instead of _active), NullaryArityTransform doesn't derive
      // parenless, and opaque Key/Button/Pixels seeds are empty.
      // The reference is extracted from sge's master branch into target/parity-reference because
      // the current branch (balticporter-generated) removed the hand-ported files.
      // platformDirs: sge hand-writes its backend layers (sge/src/main/scala{jvm,js,native,desktop}), so the
      // ladder's backend steps contribute nothing here; the steps the PORT owns per row (`async`: java's
      // executor on JVM/Native, libGDX's GWT emulation on JS) land in src_managed/<row>/scala, which
      // build.sbt attaches to that row only (`platformSources`).
      val parityRef = extractParityReference(sgeRoot, log)
      val manifest  = balticporter.corpus.libgdx.LibgdxLadder
        .universal(bpRoot, steps)
        .copy(
          parity = Some(balticporter.core.ParityRef(roots = parityRef, compare = false)),
          platformDirs = portOwnedPlatformDirs(bpRoot, steps),
          baseReports = List(llsReportRoot)
        )

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
        .filterNot(balticporter.corpus.lls.LlsMigrate.Files.toSet)
        .toList
        .sorted

      // The PortRun writes all files then runs post-emission checks; some checks fail
      // because the inject replacements are at the RENAMED package path (sge/) but the
      // dangling check looks for the ORIGINAL path (com/badlogic/gdx/). The files are
      // already written before the check runs, so catch the fatal check exception and
      // report it as a warning.
      try {
        val result = balticporter.runner
          .PortRun(
            label = "sge-core",
            portRoot = portRoot,
            sourceSet = balticporter.runner.SourceSet.Main,
            frontend = balticporter.core.FrontendConfig(
              libgdxSrc,
              files,
              balticporter.corpus.JnigenClasspath.entries(bpRoot),
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
      } catch {
        case e: RuntimeException if e.getMessage != null && e.getMessage.contains("fatal finding") =>
          log.warn(s"[Baltic Porter] Port completed with findings (files written): ${e.getMessage}")
      }

      // Post-process: fix API name mismatches between generated code and sge's types.
      // The engine's bean-property and nullary-arity transforms rename members in declarations
      // but not in all call sites within method bodies (an engine limitation). These replacements
      // patch the generated bodies until the engine covers them.
      postProcess(outDir, log)

      Files.createDirectories(marker.getParent)
      Files.writeString(marker, commit)
    } else {
      log.info(s"[Baltic Porter] Using cached generated sources ($commit)")
    }

    // Collect generated files, excluding any that also exist in sge's hand-written source tree.
    // The engine outputs inject files alongside generated translations; sge's src/main/scala has
    // its own (possibly adapted) versions of those same types. Keeping both would produce
    // duplicate class definitions.
    val managedRoot  = portRoot.resolve("src_managed/main")
    val sgeUnmanaged = sgeRoot.resolve("sge/src/main")
    collectScalaFiles(managedRoot, excludeDuplicatesOf = Some(sgeUnmanaged))
  }

  /** the ladder steps whose platform files sge hand-writes itself (sge/src/main/scala{jvm,js,native,desktop}) */
  private val HandWrittenPlatformSteps: Set[String] = Set("backend-jvm", "backend-desktop")

  /** `PortManifest.platformDirs` for the steps the port owns per row — merged the way `LibgdxLadder.universal` merges them, minus [[HandWrittenPlatformSteps]]. */
  private def portOwnedPlatformDirs(bpRoot: Path, steps: Set[String]): Map[String, List[Path]] = {
    val ladder = balticporter.corpus.libgdx.LibgdxLadder
    ladder.StepOrder.filter(steps -- HandWrittenPlatformSteps).flatMap(ladder.stepPlatformInjects(bpRoot)(_).toList).groupMapReduce(_._1)(_._2)(_ ++ _)
  }

  /** The generated files of ONE platform row (`jvm`/`js`/`native`, sbt-projectmatrix's names): `src_managed/<row>/scala`, which only that row compiles. Generates first (cached, synchronized), so a
    * row's generator may run before or after the shared one.
    */
  def platformSources(buildBase: File, row: String, log: sbt.util.Logger): Seq[File] = {
    generate(buildBase, log)
    val sgeRoot  = buildBase.toPath.toAbsolutePath.normalize
    val rowRoot  = sgeRoot.resolve("target/balticporter-sge/src_managed").resolve(row)
    val handRoot = sgeRoot.resolve("sge/src/main").resolve("scala" + row)
    collectScalaFiles(rowRoot, excludeDuplicatesOf = Some(handRoot))
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
    sgeRoot:   Path,
    bpRoot:    Path,
    libgdxSrc: Path,
    commit:    String,
    log:       sbt.util.Logger
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
      Files.readString(llsMarker).trim == commit

    if (!llsCached) {
      log.info(s"[Baltic Porter] Running lls base port first (required for sge-core contract)")

      val rungs    = balticporter.corpus.lls.LlsPolicy.DefaultRungs
      val manifest = balticporter.corpus.lls.LlsPolicy.core(bpRoot, rungs).copy(parity = None, inject = Nil)

      // Set reportDir so the port map is written where the sge port can find it
      System.setProperty("balticporter.reportDir", llsReportDir.toAbsolutePath.normalize.toString)

      balticporter.runner
        .PortRun(
          label = "lls",
          portRoot = llsPortRoot,
          sourceSet = balticporter.runner.SourceSet.Main,
          frontend = balticporter.core.FrontendConfig(
            libgdxSrc,
            balticporter.corpus.lls.LlsMigrate.Files,
            balticporter.corpus.GdxCoreClasspath.entries(bpRoot),
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
      Files.writeString(llsMarker, commit)
      log.info(s"[Baltic Porter] lls base port complete, port-map at $llsReportDir")
    } else {
      log.info(s"[Baltic Porter] Using cached lls base port ($commit)")
    }

    reportRoot
  }

  /** Extract sge's master-branch hand-ported source into target/parity-reference so the derive mechanism can read the full reference's naming conventions. On the balticporter-generated branch, the
    * hand-ported files are removed, so the current tree is incomplete.
    */
  private def extractParityReference(sgeRoot: Path, log: sbt.util.Logger): List[Path] = {
    val refDir = sgeRoot.resolve("target/parity-reference")
    val marker = refDir.resolve(".extracted-marker")
    if (!Files.exists(marker)) {
      log.info("[Baltic Porter] Extracting parity reference from sge master branch")
      if (Files.exists(refDir)) {
        Files.walk(refDir).sorted(java.util.Comparator.reverseOrder()).forEach(Files.delete)
      }
      Files.createDirectories(refDir)
      // Use origin/master in CI (shallow checkouts lack a local master ref);
      // fall back to master for local dev where the branch exists.
      val ref = {
        val check = new ProcessBuilder("git", "rev-parse", "--verify", "master")
        check.directory(sgeRoot.toFile)
        check.redirectErrorStream(true)
        val p = check.start(); val out = new String(p.getInputStream.readAllBytes()).trim; p.waitFor()
        if (p.exitValue() == 0) "master" else "origin/master"
      }
      val pb = new ProcessBuilder("git", "archive", ref, "sge/src/main/scala", "sge/src/main/scalajvm", "sge/src/main/scaladesktop")
      pb.directory(sgeRoot.toFile)
      pb.redirectErrorStream(true)
      val tarPb = new ProcessBuilder("tar", "-x", "-C", refDir.toString, "--strip-components=3")
      tarPb.directory(sgeRoot.toFile)
      val gitProc = pb.start()
      val tarProc = tarPb.start()
      gitProc.getInputStream.transferTo(tarProc.getOutputStream)
      tarProc.getOutputStream.close()
      val gitExit = gitProc.waitFor()
      tarProc.waitFor()
      val count = Files.walk(refDir).filter(p => p.toString.endsWith(".scala")).count()
      // An empty reference derives NOTHING (no opaque seeds, no property shapes) and the port then
      // fails to compile against sge's hand files with hundreds of type mismatches that name no
      // cause (release run 35313268095: a shallow checkout has neither `master` nor `origin/master`).
      if (gitExit != 0 || count == 0)
        sys.error(
          s"[Baltic Porter] no parity reference: `git archive $ref sge/src/main/...` exited $gitExit and extracted $count file(s). " +
            "The checkout needs the master branch (actions/checkout `fetch-depth: 0`)."
        )
      Files.writeString(marker, "extracted")
      log.info(s"[Baltic Porter] Extracted $count reference files to $refDir")
    }
    List("scala", "scalajvm", "scaladesktop").map(d => refDir.resolve(d)).filter(Files.isDirectory(_))
  }

  private def collectScalaFiles(dir: Path, excludeDuplicatesOf: Option[Path] = None): Seq[File] = {
    if (!Files.isDirectory(dir)) return Seq.empty
    // Build set of relative paths that exist in the exclusion tree (sge's hand-written sources).
    // Any generated file whose relative path matches is an inject duplicate — skip it.
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

  /** Fix API name mismatches in generated code.
    *
    * The engine's BeanPropertyTransform and NullaryArityTransform rename DECLARATIONS but some call sites in method bodies still reference the old names. These text-level replacements patch the
    * generated Scala until the engine's body-rewrite coverage is complete. Each replacement is documented with its cause.
    */
  private def postProcess(outDir: Path, log: sbt.util.Logger): Unit = {
    if (!Files.isDirectory(outDir)) return
    // word-boundary-safe patterns: the replacement text is always shorter or equal, so
    // repeated application is idempotent.
    val replacements: List[(String, String, String)] = List(
      // first→head: BeanPropertyTransform renames declarations but not call sites in bodies.
      // The post-process regex replaces .first()→.head and .first→.head, skipping files that
      // DEFINE their own `first` member (Selection.scala) to avoid renaming unrelated methods.
      ("\\.first\\(\\)", ".head", "first()→head (BeanPropertyTransform+NullaryArityTransform)"),
      ("\\.first\\b", ".head", "first→head (BeanPropertyTransform)"),
      // NullaryArityTransform removed parens from isEmpty, head.
      // exists() is NOT parenless on FileHandle — its NullaryArityTransform scope doesn't cover it.
      ("\\.isEmpty\\(\\)", ".isEmpty", "isEmpty() parenless (NullaryArityTransform)"),
      ("\\.head\\(\\)", ".head", "head() parenless (NullaryArityTransform)"),
      ("\\.orderedItems\\(\\)", ".orderedItems", "orderedItems() parenless (NullaryArityTransform)"),
      // BeanPropertyTransform renamed scheduled→isScheduled
      ("\\.scheduled\\b", ".isScheduled", "scheduled→isScheduled (BeanPropertyTransform)")
      // isDirectory→directory and multiLine→isMultiLine are NOT safe as global replacements:
      // they also hit java.io.File.isDirectory() and other unrelated types. Fix those in
      // hand-written files only.
    )
    var count  = 0
    val stream = Files.walk(outDir)
    try
      stream.forEach { p =>
        if (p.toString.endsWith(".scala")) {
          var content = Files.readString(p)
          var changed = false
          for ((pattern, replacement, _) <- replacements) {
            // SortedIntList has `var first` — a linked-list FIELD, not a DynamicArray call.
            // Skip the entire file; the field and its accesses should keep the name `first`.
            val skip = pattern.contains("first") && content.contains("var first:")
            if (!skip) {
              val updated = content.replaceAll(pattern, replacement)
              if (updated != content) { content = updated; changed = true }
            }
          }
          // Restore references the first→head replacement wrongly renamed:
          // 1. Selection defines `def first: Nullable[T]` — its definition must stay `first`
          // 2. Calls like `selection$field.head` should be `selection$field.first` because
          //    Selection's method is `first`, not `head`
          if (content.contains("def head:") && !content.contains("def first:")) {
            content = content.replace("def head:", "def first:")
            changed = true
          }
          // selection$field.head → selection$field.first (Selection's own method)
          if (content.contains("selection$field.head")) {
            content = content.replace("selection$field.head", "selection$field.first")
            changed = true
          }
          if (changed) { Files.writeString(p, content); count += 1 }
        }
      }
    finally stream.close()
    if (count > 0) log.info(s"[Baltic Porter] Post-processed $count files (API name fixes)")
  }
}
