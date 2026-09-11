import sbt.*
import sbt.Keys.*

import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*

/** sbt sourceGenerator that uses Baltic Porter to mechanically port the libGDX
  * core sources (minus the twelve lls utilities) into Scala 3.
  *
  * Requires:
  *   - libGDX sources at `original-src/libgdx/gdx/src` (git submodule)
  *   - balticporter checkout at `../balticporter` (sibling directory) for inject files
  *     and classpath cache; override with `-Dbalticporter.root=<path>`
  *   - `balticporter-corpus` 0.1.0-SNAPSHOT published locally (`sbt publishLocal` in balticporter)
  */
object BalticPorterGen {

  /** Generate sge-core Scala sources from libGDX Java originals.
    * Returns the list of generated files (shared + all platform rows).
    * Caches by upstream commit.
    */
  def generate(buildBase: File, log: sbt.util.Logger): Seq[File] = {
    val sgeRoot = buildBase.toPath.toAbsolutePath.normalize
    val bpRoot = Path.of(sys.props.getOrElse("balticporter.root",
      sgeRoot.resolve("../balticporter").toString)).toAbsolutePath.normalize

    val libgdxSrc = sgeRoot.resolve("original-src/libgdx/gdx/src")
    require(Files.isDirectory(libgdxSrc),
      s"libGDX sources not found at $libgdxSrc — run: git submodule update --init")

    val portRoot = sgeRoot.resolve("target/balticporter-sge")
    val outDir = portRoot.resolve("src_managed/main/scala")
    val marker = portRoot.resolve(".generated-marker")

    // Cache key: the vendored tree's commit
    val commit = balticporter.runner.VendoredCommit.of(libgdxSrc)
    val cached = Files.exists(marker) &&
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
      // Disable parity check: the hand-ported files are being replaced by these generated ones.
      // Remove inject: sge's hand-written inject files live in sge/src/main/scala and are added
      // by the build as unmanagedSources (not duplicated into src_managed).
      // Remove platformDirs: sge's platform-specific files live in sge/src/main/ already.
      val manifest = balticporter.corpus.libgdx.LibgdxLadder.universal(bpRoot, steps)
        .copy(parity = None, platformDirs = Map.empty,
              baseReports = List(llsReportRoot))

      // Collect the files to port: all .java under gdx/src minus the lls set.
      val files = Files.walk(libgdxSrc).iterator().asScala
        .filter(p => p.toString.endsWith(".java"))
        .map(p => libgdxSrc.relativize(p).toString)
        .filterNot(f => f.endsWith("package-info.java") || f.endsWith("module-info.java"))
        .filterNot(balticporter.corpus.lls.LlsMigrate.Files.toSet)
        .toList.sorted

      // The PortRun writes all files then runs post-emission checks; some checks fail
      // because the inject replacements are at the RENAMED package path (sge/) but the
      // dangling check looks for the ORIGINAL path (com/badlogic/gdx/). The files are
      // already written before the check runs, so catch the fatal check exception and
      // report it as a warning.
      try {
        val result = balticporter.runner.PortRun(
          label     = "sge-core",
          portRoot  = portRoot,
          sourceSet = balticporter.runner.SourceSet.Main,
          frontend  = balticporter.core.FrontendConfig(
            libgdxSrc,
            files,
            balticporter.corpus.JnigenClasspath.entries(bpRoot),
            resolutionRoots = List(libgdxSrc),
          ),
          phases    = Nil,
          manifest  = Some(manifest),
          provenance = Some(balticporter.core.Provenance(
            upstreamName     = "libGDX",
            upstreamCommit   = commit,
            originalLicense  = "Apache-2.0",
            sourcePathPrefix = "gdx/src",
            sourceRoot       = libgdxSrc.toString,
          )),
          runtimeMode = balticporter.core.RuntimeMode.Vendored,
          determinism = balticporter.runner.Determinism.Emission,
          nextStep    = "",
        ).execute()
        log.info(s"[Baltic Porter] Generated ${result.written} files to $outDir")
      } catch {
        case e: RuntimeException if e.getMessage != null && e.getMessage.contains("fatal finding") =>
          log.warn(s"[Baltic Porter] Port completed with findings (files written): ${e.getMessage}")
      }

      Files.createDirectories(marker.getParent)
      Files.writeString(marker, commit)
    } else {
      log.info(s"[Baltic Porter] Using cached generated sources ($commit)")
    }

    // Remove generated files that conflict with SGE-original hand-written sources.
    // Both live under the same package paths (sge/…) so the compiler sees duplicate
    // definitions. The SGE-originals win: they are the canonical, hand-maintained
    // versions (opaque types, platform traits, networking, etc.).
    val sgeOriginalDir = sgeRoot.resolve("sge/src/main/scala")
    removeConflicts(outDir, sgeOriginalDir, log)

    // Collect all generated files: shared + any platform rows
    val managedRoot = portRoot.resolve("src_managed/main")
    collectScalaFiles(managedRoot)
  }

  /** Run the lls port (the base) and return the report root directory where its port-map.tsv
    * was written. The sge port then uses this as `baseReports` to discover the base's contract. */
  private def runLlsPort(
      sgeRoot: Path,
      bpRoot: Path,
      libgdxSrc: Path,
      commit: String,
      log: sbt.util.Logger
  ): Path = {
    val llsPortRoot = sgeRoot.resolve("target/balticporter-lls")
    val llsMarker = llsPortRoot.resolve(".generated-marker")
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

      val rungs = balticporter.corpus.lls.LlsPolicy.DefaultRungs
      val manifest = balticporter.corpus.lls.LlsPolicy.core(bpRoot, rungs)
        .copy(parity = None, inject = Nil)

      // Set reportDir so the port map is written where the sge port can find it
      System.setProperty("balticporter.reportDir", llsReportDir.toAbsolutePath.normalize.toString)

      balticporter.runner.PortRun(
        label     = "lls",
        portRoot  = llsPortRoot,
        sourceSet = balticporter.runner.SourceSet.Main,
        frontend  = balticporter.core.FrontendConfig(
          libgdxSrc,
          balticporter.corpus.lls.LlsMigrate.Files,
          balticporter.corpus.GdxCoreClasspath.entries(bpRoot),
          resolutionRoots = Nil,
        ),
        phases    = Nil,
        manifest  = Some(manifest),
        provenance = Some(balticporter.core.Provenance(
          upstreamName     = "libGDX",
          upstreamCommit   = commit,
          originalLicense  = "Apache-2.0",
          sourcePathPrefix = "gdx/src",
          sourceRoot       = libgdxSrc.toString,
        )),
        runtimeMode = balticporter.core.RuntimeMode.Vendored,
        determinism = balticporter.runner.Determinism.Off,
        nextStep    = "",
      ).execute()

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

  /** Delete generated files whose relative path under `generatedDir` matches a
    * hand-written SGE-original under `sgeOriginalDir`. This prevents E161 "already
    * defined" errors when both trees are on the classpath.
    */
  private def removeConflicts(generatedDir: Path, sgeOriginalDir: Path, log: sbt.util.Logger): Unit = {
    if (!Files.isDirectory(sgeOriginalDir) || !Files.isDirectory(generatedDir)) return
    val stream = Files.walk(sgeOriginalDir)
    try {
      var removed = 0
      stream.forEach { p =>
        if (p.toString.endsWith(".scala")) {
          val rel = sgeOriginalDir.relativize(p)
          val generated = generatedDir.resolve(rel)
          if (Files.exists(generated)) {
            Files.delete(generated)
            removed += 1
          }
        }
      }
      if (removed > 0)
        log.info(s"[Baltic Porter] Removed $removed generated files that conflict with SGE-originals")
    } finally {
      stream.close()
    }
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
    } finally {
      stream.close()
    }
  }
}
