package sge.sbt

import multiarch.sbt.{ JvmPackaging, Platform }

import sbt._
import sbt.Keys._

/** SGE-specific application packaging.
  *
  * JVM packaging (simple + distribution modes) is delegated to [[multiarch.sbt.JvmPackaging]] from sbt-multiarch-scala. This object provides SGE-specific browser (Scala.js) and native (Scala Native)
  * packaging.
  *
  * ===JVM packaging (via JvmPackaging)===
  *
  * '''Simple mode''' (`releasePackage`) — directory with launcher scripts + system JDK. '''Distribution mode''' (`releasePlatform`, `releaseAll`) — self-contained archives with jlinked JRE.
  *
  * ===Browser packaging===
  *
  * `sgePackageBrowser` — packages Scala.js output as a browser-ready directory with HTML.
  *
  * ===Native packaging===
  *
  * `sgePackageNative` — packages Scala Native executable into a distributable directory.
  *
  * Covenant: full-port Covenant-baseline-spec-pass: 0 Covenant-baseline-loc: 225 Covenant-baseline-methods:
  * NativeLibExts,SgePackaging,_,appName,archive,binary,browserSettings,cmd,codesignAdHoc,createTarGzArchive,createZipArchive,dest,dirName,distSettings,exeName,exit,generateHtml,hasNativeLibs,hostIsMac,html,isWindows,jsDir,jvmSettings,log,nativeDir,nativeSettings,out,outDir,output,packageBrowserTask,packageNativeTask,parentDir,platform,proc,sgeBrowserTitle,sgeJsOutputDir,sgeNativeBinary,sgePackageBrowser,sgePackageNative,sgeRelease,title
  * Covenant-source-reference: SGE-original Covenant-verified: 2026-06-22
  */
object SgePackaging {

  // ── JVM packaging: delegated to JvmPackaging ──────────────────────

  /** JVM packaging settings (simple mode). */
  lazy val jvmSettings: Seq[Setting[_]] = JvmPackaging.jvmSettings

  /** Distribution packaging settings. Requires `jvmSettings` to also be applied. */
  lazy val distSettings: Seq[Setting[_]] = JvmPackaging.distSettings

  // ── Browser (Scala.js) packaging ───────────────────────────────────

  val sgePackageBrowser = taskKey[File]("Package Scala.js output as a browser-ready directory with HTML")
  val sgeBrowserTitle   = settingKey[String]("HTML page title for the browser package")

  /** Opt-in flag: when `true`, the browser package ships a vendored global build of `@dimforge/rapier2d-compat` (`rapier2d-compat.umd.js`, exposing `window.RAPIER`) and injects a `<script>` tag for
    * it BEFORE `main.js` in `index.html`. Physics-using JS games must set this so the bundler-free package can load the Rapier2D WASM backend (see PhysicsExtension.scala, which reads the `RAPIER`
    * global in the browser). Defaults to `false` so non-physics games are not bloated with the ~1.7MB Rapier bundle. ISS-679.
    *
    * Scope: this covers 2D physics (`rapier2d`) only. The 3D physics extension (`@dimforge/rapier3d-compat`) needs the same treatment as a follow-up under ISS-677.
    */
  val sgeBrowserIncludeRapier2d = settingKey[Boolean](
    "Ship the vendored rapier2d-compat global build (window.RAPIER) and inject its <script> before main.js — required for physics-using browser games (ISS-679)"
  )

  /** Classpath location (inside the sge-build plugin JAR) of the vendored rapier2d-compat global build. */
  private val Rapier2dUmdResource = "/rapier2d-compat/rapier2d-compat.umd.js"

  /** File name the vendored rapier2d-compat global build is copied to in the browser package. */
  private val Rapier2dUmdName = "rapier2d-compat.umd.js"

  /** The fullLinkJS output directory must be provided by the caller, since sbt-scalajs types are not on this plugin's classpath. Wire it in build.sbt:
    * {{{
    * SgePackaging.sgeJsOutputDir := (Compile / fullLinkJS / scalaJSLinkerOutputDirectory).value
    * }}}
    */
  val sgeJsOutputDir = taskKey[File]("Directory containing fullLinkJS output (main.js)")

  private val packageBrowserTask: Def.Initialize[Task[File]] = Def.task {
    // Assets are embedded into main.js at compile time by
    // MultiArchResourcesPlugin.embeddedResourcesSettings (base64) and served
    // synchronously at runtime via multiarch.resources.PlatformResources — there
    // is no assets.txt manifest and no fetch/preload step. This task only emits
    // index.html + the fullLinkJS output (main.js).
    val log             = streams.value.log
    val appName         = JvmPackaging.releaseAppName.value
    val title           = sgeBrowserTitle.value
    val jsDir           = sgeJsOutputDir.value
    val includeRapier2d = sgeBrowserIncludeRapier2d.value
    val outDir          = target.value / "sge-browser" / appName

    IO.delete(outDir)
    IO.createDirectory(outDir)

    // Copy all JS files from fullLinkJS output
    IO.listFiles(jsDir).foreach { f =>
      IO.copyFile(f, outDir / f.getName)
    }

    // Opt-in (ISS-679): ship the vendored rapier2d-compat global build so a
    // bundler-free physics game can load the Rapier2D WASM backend from the
    // `window.RAPIER` global. The compat variant inlines its WASM as base64, so
    // this single file is self-contained (no separate .wasm to serve).
    if (includeRapier2d) copyRapier2dUmd(outDir, log)

    // Generate index.html
    val html = generateHtml(title, appName, includeRapier2d)
    IO.write(outDir / "index.html", html)

    log.info(s"[sge] Browser package: ${outDir.getAbsolutePath}")
    log.info(s"[sge] Serve with: python3 -m http.server -d ${outDir.getAbsolutePath}")
    outDir
  }

  private def generateHtml(title: String, appName: String, includeRapier2d: Boolean): String = {
    // ISS-679: the Rapier2D global build must load BEFORE main.js so that
    // PhysicsExtension.load() finds `window.RAPIER` already defined.
    val rapierScript =
      if (includeRapier2d) s"""  <script src="$Rapier2dUmdName"></script>\n"""
      else ""
    s"""<!DOCTYPE html>
       |<html lang="en">
       |<head>
       |  <meta charset="UTF-8">
       |  <meta name="viewport" content="width=device-width, initial-scale=1.0">
       |  <title>$title</title>
       |  <style>
       |    * { margin: 0; padding: 0; box-sizing: border-box; }
       |    html, body { width: 100%; height: 100%; overflow: hidden; background: #000; }
       |    canvas { display: block; }
       |    #loading {
       |      position: absolute; top: 50%; left: 50%;
       |      transform: translate(-50%, -50%);
       |      color: #ccc; font-family: sans-serif; font-size: 1.2em;
       |    }
       |  </style>
       |</head>
       |<body>
       |  <div id="loading">Loading $appName&#8230;</div>
       |$rapierScript  <script src="main.js"></script>
       |</body>
       |</html>
       |""".stripMargin
  }

  /** Copy the vendored rapier2d-compat global build from the plugin JAR resources into `outDir`. The bundler-free browser package injects a `<script>` for it before `main.js`. ISS-679. */
  private def copyRapier2dUmd(outDir: File, log: sbt.util.Logger): Unit = {
    val in = getClass.getResourceAsStream(Rapier2dUmdResource)
    if (in == null) {
      sys.error(
        s"[sge] rapier2d-compat global build not found on the plugin classpath at $Rapier2dUmdResource — the sge-build plugin JAR is missing the vendored bundle (regenerate per sge-build/src/main/resources/rapier2d-compat/README.md)"
      )
    }
    try {
      val dest  = outDir / Rapier2dUmdName
      val bytes = in.readAllBytes()
      IO.write(dest, bytes)
      log.info(s"[sge] Bundled rapier2d-compat global build: ${dest.getName} (${bytes.length / 1024} KB)")
    } finally in.close()
  }

  /** Browser packaging settings. Apply to JS platform projects. Requires the caller to wire `sgeJsOutputDir` to `fullLinkJS / scalaJSLinkerOutputDirectory`.
    */
  lazy val browserSettings: Seq[Setting[_]] = Seq(
    sgeBrowserTitle := JvmPackaging.releaseAppName.value,
    // ISS-679: default off — non-physics games are not bloated with the ~1.7MB
    // Rapier2D bundle. Physics-using JS projects set this to true.
    sgeBrowserIncludeRapier2d := false,
    // sbt 2.0 caches task results and refuses to cache a File/Path output; this
    // packaging task produces a directory as a side effect, so opt out.
    sgePackageBrowser := Def.uncached(packageBrowserTask.value)
  )

  // ── Scala Native packaging ─────────────────────────────────────────

  val sgePackageNative = taskKey[File]("Package Scala Native executable into a distributable directory")

  /** The nativeLink output file must be provided by the caller, since sbt-scala-native types are not on this plugin's classpath. Wire it in build.sbt:
    * {{{
    * SgePackaging.sgeNativeBinary := (Compile / nativeLink).value
    * }}}
    */
  val sgeNativeBinary = taskKey[File]("Scala Native linked executable")

  /** Where the shared libraries the executable loads at run time are found (ANGLE's libEGL/libGLESv2 among them). [[SgeDesktopNativePlatform]] adds the directory the native providers are extracted
    * into, which is also the one the linker recorded.
    */
  val sgeNativeLibDirs = taskKey[Seq[File]]("Directories whose shared libraries (.dylib/.so/.dll) the native package ships next to the executable")

  private val NativeLibExts = Set(".so", ".dylib", ".dll")

  private def isSharedLib(name: String): Boolean =
    NativeLibExts.exists(name.endsWith) || name.matches(""".*\.so(\.\d+)+""")

  private val packageNativeTask: Def.Initialize[Task[File]] = Def.task {
    val log     = streams.value.log
    val appName = JvmPackaging.releaseAppName.value
    val binary  = sgeNativeBinary.value
    val outDir  = target.value / "sge-native" / appName
    val libDirs = sgeNativeLibDirs.value

    IO.delete(outDir)
    IO.createDirectory(outDir)

    // Copy the native executable
    val isWindows = System.getProperty("os.name", "").toLowerCase.contains("win")
    val exeName   = if (isWindows) s"$appName.exe" else appName
    val dest      = outDir / exeName
    IO.copyFile(binary, dest)
    dest.setExecutable(true)

    // The shared libraries go next to the executable: its only library search path is its own directory.
    val nativeDir     = outDir
    var hasNativeLibs = false
    for {
      dir <- libDirs if dir.exists()
      file <- IO.listFiles(dir) if file.isFile && isSharedLib(file.getName)
    } {
      hasNativeLibs = true
      IO.copyFile(file, nativeDir / file.getName)
    }
    if (!hasNativeLibs) log.warn(s"[sge] Native package ships no shared libraries (searched: ${libDirs.mkString(", ")})")

    relocatableRpath(dest, log)

    // Ad-hoc sign the native binary on macOS so Gatekeeper doesn't block it (after the rpath edit, which invalidates a signature)
    if (hostIsMac) codesignAdHoc(dest, log)

    // Create archive
    val platform = Platform.host
    val archive  = if (isWindows) {
      createZipArchive(outDir, target.value / "sge-native" / s"$appName-native-${platform.classifier}.zip", log)
    } else {
      createTarGzArchive(outDir, target.value / "sge-native" / s"$appName-native-${platform.classifier}.tar.gz", log)
    }

    log.info(s"[sge] Native package: ${archive.getAbsolutePath} (${archive.length() / 1024 / 1024} MB)")
    archive
  }

  /** Scala Native packaging settings. Apply to Native platform projects. Requires the caller to wire `sgeNativeBinary` to `Compile / nativeLink`.
    */
  lazy val nativeSettings: Seq[Setting[_]] = Seq(
    sgeNativeLibDirs := Def.uncached(JvmPackaging.releaseNativeLibDirs.value),
    sgePackageNative := Def.uncached(packageNativeTask.value)
  )

  // ── Helpers ────────────────────────────────────────────────────────

  /** Leaves `@executable_path` (macOS) or `$ORIGIN` (Linux) as the executable's only library search path. The linker also recorded the build machine's absolute library directory, which would make the
    * package load libraries from (or only run on) the machine that built it. Windows has no rpath: a DLL next to the executable is found.
    */
  private def relocatableRpath(exe: File, log: sbt.util.Logger): Unit =
    if (hostIsMac) {
      val rpaths = machORpaths(exe)
      rpaths.filterNot(_ == "@executable_path").foreach(p => runTool(Seq("install_name_tool", "-delete_rpath", p, exe.getAbsolutePath)))
      if (!rpaths.contains("@executable_path")) runTool(Seq("install_name_tool", "-add_rpath", "@executable_path", exe.getAbsolutePath))
      log.info(s"[sge] rpath of ${exe.getName}: ${machORpaths(exe).mkString(", ")}")
    } else if (sys.props("os.name").toLowerCase.contains("linux")) {
      val rewritten = rewriteElfRunpath(exe)
      if (rewritten == 0) log.warn(s"[sge] No rpath/runpath found in ${exe.getName}; left unchanged")
      else log.info(s"[sge] rpath of ${exe.getName}: $$ORIGIN")
    }

  /** The `LC_RPATH` entries of a Mach-O file, in order, read with `otool -l`. */
  private def machORpaths(file: File): List[String] = {
    val lines = runTool(Seq("otool", "-l", file.getAbsolutePath)).linesIterator.map(_.trim).toList
    lines
      .sliding(3)
      .collect {
        case List("cmd LC_RPATH", _, path) if path.startsWith("path ") =>
          path.stripPrefix("path ").replaceAll("""\s+\(offset \d+\)$""", "")
      }
      .toList
  }

  /** Rewrites, in place, every ELF `DT_RPATH`/`DT_RUNPATH` string to `$ORIGIN`: a string of `:`-separated entries that are absolute paths or `$ORIGIN`. The new value is shorter than the old one and
    * is padded with NULs, so no offset in the file moves. Returns how many strings were rewritten.
    */
  private def rewriteElfRunpath(file: File): Int = {
    val bytes  = java.nio.file.Files.readAllBytes(file.toPath)
    val origin = "$ORIGIN".getBytes("US-ASCII")
    def isRunpath(s: String): Boolean =
      s.contains("$ORIGIN") && s.split(':').forall(e => e.startsWith("/") || e.startsWith("$ORIGIN")) && s.split(':').exists(_.startsWith("/"))
    var count = 0
    var i     = indexOf(bytes, origin, 0)
    while (i >= 0) {
      var start = i
      while (start > 0 && bytes(start - 1) != 0) start -= 1
      var end = i
      while (end < bytes.length && bytes(end) != 0) end += 1
      val s = new String(bytes, start, end - start, "US-ASCII")
      if (isRunpath(s)) {
        java.util.Arrays.fill(bytes, start, end, 0.toByte)
        System.arraycopy(origin, 0, bytes, start, origin.length)
        count += 1
      }
      i = indexOf(bytes, origin, end)
    }
    if (count > 0) java.nio.file.Files.write(file.toPath, bytes)
    count
  }

  private def indexOf(haystack: Array[Byte], needle: Array[Byte], from: Int): Int = {
    var i     = from
    var found = -1
    while (found < 0 && i <= haystack.length - needle.length) {
      var j = 0
      while (j < needle.length && haystack(i + j) == needle(j)) j += 1
      if (j == needle.length) found = i else i += 1
    }
    found
  }

  /** Runs a command and returns its output; throws with that output when it exits non-zero. */
  private def runTool(cmd: Seq[String]): String = {
    val proc   = new ProcessBuilder(cmd: _*).redirectErrorStream(true).start()
    val output = new String(proc.getInputStream.readAllBytes())
    val exit   = proc.waitFor()
    if (exit != 0) throw new RuntimeException(s"${cmd.mkString(" ")} failed (exit $exit): $output")
    output
  }

  /** Ad-hoc sign a file with `codesign --force --sign -`. Only meaningful on macOS. Non-fatal: logs a warning on failure instead of throwing.
    */
  private def codesignAdHoc(file: File, log: sbt.util.Logger): Unit = {
    val proc   = new ProcessBuilder("codesign", "--force", "--sign", "-", file.getAbsolutePath).redirectErrorStream(true).start()
    val output = new String(proc.getInputStream.readAllBytes())
    val exit   = proc.waitFor()
    if (exit != 0) log.warn(s"[sge] Ad-hoc signing failed for ${file.getName}: $output")
    else log.info(s"[sge] Ad-hoc signed: ${file.getName}")
  }

  /** Returns true when the build is running on macOS. */
  private def hostIsMac: Boolean =
    sys.props("os.name").toLowerCase.contains("mac")

  /** Create a .tar.gz archive preserving executable bits. */
  private def createTarGzArchive(sourceDir: File, archiveFile: File, log: sbt.util.Logger): File = {
    val parentDir = sourceDir.getParentFile
    val dirName   = sourceDir.getName
    val cmd       = Seq("tar", "czf", archiveFile.getAbsolutePath, "-C", parentDir.getAbsolutePath, dirName)
    val proc      = new ProcessBuilder(cmd: _*).redirectErrorStream(true).start()
    val exit      = proc.waitFor()
    if (exit != 0) {
      val output = new String(proc.getInputStream.readAllBytes())
      throw new RuntimeException(s"tar archive creation failed (exit $exit): $output")
    }
    archiveFile
  }

  /** Create a .zip archive. */
  private def createZipArchive(sourceDir: File, archiveFile: File, log: sbt.util.Logger): File = {
    val parentDir = sourceDir.getParentFile
    val out       = new java.util.zip.ZipOutputStream(new java.io.FileOutputStream(archiveFile))
    try {
      val allFiles = (sourceDir ** AllPassFilter).get().filter(_.isFile)
      allFiles.foreach { file =>
        val relativePath = parentDir.toPath.relativize(file.toPath).toString
        val entry        = new java.util.zip.ZipEntry(relativePath)
        out.putNextEntry(entry)
        IO.transfer(file, out)
        out.closeEntry()
      }
    } finally out.close()
    archiveFile
  }

  // ── Unified release ────────────────────────────────────────────────

  val sgeRelease = taskKey[Seq[File]]("Build all distribution packages: JVM (all platforms) + Browser + Native")
}
