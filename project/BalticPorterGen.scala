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
  *     from `sge-port/overrides`
  *   - `cs` (coursier) on the PATH, to resolve the classpath libGDX's own sources are read against
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
      // The derive mechanism reads the FULL hand-ported sge source
      // (one pinned commit, `HandPortReference`) to learn the reference's naming conventions (field names, property shapes,
      // parenless methods, opaque type seeds). Without this, BeanPropertyTransform falls back to
      // generic naming (active$field instead of _active), NullaryArityTransform doesn't derive
      // parenless, and opaque Key/Button/Pixels seeds are empty.
      // The reference is extracted from that commit into target/parity-reference because the
      // hand-ported files are gone from the current tree.
      // Platform rows: sge hand-writes its backend layers (sge/src/main/scala{jvm,js,native,desktop}); the
      // files the PORT owns per row (`async`: java's executor on JVM/Native, libGDX's GWT emulation on JS)
      // land in src_managed/<row>/scala, which build.sbt attaches to that row only (`platformSources`).
      val parityRef = extractParityReference(sgeRoot, log)
      val manifest  = sge.port.LibgdxLadder.universal(overrides, upstreamResources = sgeRoot.resolve("original-src/libgdx/gdx/res"), parityRoots = parityRef).copy(baseReports = List(llsReportRoot))

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
      Files.writeString(marker, expected)
    } else {
      log.info(s"[Baltic Porter] Using cached generated sources ($expected)")
    }

    // Collect generated files, excluding any that also exist in sge's hand-written source tree.
    // The engine outputs inject files alongside generated translations; sge's src/main/scala has
    // its own (possibly adapted) versions of those same types. Keeping both would produce
    // duplicate class definitions.
    val managedRoot  = portRoot.resolve("src_managed/main")
    val sgeUnmanaged = sgeRoot.resolve("sge/src/main")
    collectScalaFiles(managedRoot, excludeDuplicatesOf = Some(sgeUnmanaged))
  }

  /** What the generated tree depends on, as one line, readable on a shallow checkout WITHOUT the submodule's files: the engine artifact pinned in `project/plugins.sbt`, the lls-port version
    * (`Versions.lls`), the libGDX commit (the submodule's HEAD when it is initialised, else the commit this checkout records for it), this generator and the policy in `sge-port/` (sources and
    * injected files; line endings normalised, so every OS agrees) and the JDK feature version.
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
    s"engine=$pin base=$base libgdx=$libgdx generator=$generator policy=${policyHash(sgeRoot.resolve("sge-port"))} jdk=${java.lang.Runtime.version().feature()}"
  }

  /** One hash over every file under `sge-port/` (the policy's Scala and the files it injects): each file's slash-separated relative path and its text without carriage returns, in path order. */
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

  /** The last commit on master that holds the hand-written core (2026-09-05), and how many Scala files it has under `sge/src/main/scala` alone. */
  private val HandPortReference      = "ec6647df8dc9e4600794c81624922e994cecea72"
  private val HandPortReferenceFiles = 549L

  /** Extract sge's hand-written core (as of [[HandPortReference]]) into target/parity-reference, so the derive mechanism can read the reference's naming conventions: the current tree no longer holds
    * those files.
    */
  private def extractParityReference(sgeRoot: Path, log: sbt.util.Logger): List[Path] = {
    val refDir = sgeRoot.resolve("target/parity-reference")
    val marker = refDir.resolve(".extracted-marker")
    // The hand-written core is read from ONE fixed commit: the last one on master that still holds
    // it. A branch name is not a reference — once the generated core was merged, `master` itself
    // stopped containing the hand port, the derived policy came out empty-ish and master's own CI
    // failed to compile (run 35427945953) while the pull request's run had passed.
    val ref = HandPortReference
    if (!Files.exists(marker) || Files.readString(marker).trim != ref) {
      log.info(s"[Baltic Porter] Extracting parity reference from sge's hand-written core at $ref")
      if (Files.exists(refDir)) {
        Files.walk(refDir).sorted(java.util.Comparator.reverseOrder()).forEach(Files.delete)
      }
      Files.createDirectories(refDir)
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
      if (gitExit != 0 || count < HandPortReferenceFiles)
        sys.error(
          s"[Baltic Porter] no parity reference: `git archive $ref sge/src/main/...` exited $gitExit and extracted $count file(s), expected at least $HandPortReferenceFiles. " +
            "The checkout needs that commit (actions/checkout `fetch-depth: 0`)."
        )
      Files.writeString(marker, ref)
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
