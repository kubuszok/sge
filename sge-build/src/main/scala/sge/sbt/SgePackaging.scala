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
    val log     = streams.value.log
    val appName = JvmPackaging.releaseAppName.value
    val title   = sgeBrowserTitle.value
    val jsDir   = sgeJsOutputDir.value
    val outDir  = target.value / "sge-browser" / appName

    IO.delete(outDir)
    IO.createDirectory(outDir)

    // Copy all JS files from fullLinkJS output
    IO.listFiles(jsDir).foreach { f =>
      IO.copyFile(f, outDir / f.getName)
    }

    // Generate index.html
    val html = generateHtml(title, appName)
    IO.write(outDir / "index.html", html)

    log.info(s"[sge] Browser package: ${outDir.getAbsolutePath}")
    log.info(s"[sge] Serve with: python3 -m http.server -d ${outDir.getAbsolutePath}")
    outDir
  }

  private def generateHtml(title: String, appName: String): String =
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
       |  <script src="main.js"></script>
       |</body>
       |</html>
       |""".stripMargin

  /** Browser packaging settings. Apply to JS platform projects. Requires the caller to wire `sgeJsOutputDir` to `fullLinkJS / scalaJSLinkerOutputDirectory`.
    */
  lazy val browserSettings: Seq[Setting[_]] = Seq(
    sgeBrowserTitle := JvmPackaging.releaseAppName.value,
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

  private val NativeLibExts = Set(".so", ".dylib", ".dll")

  private val packageNativeTask: Def.Initialize[Task[File]] = Def.task {
    val log     = streams.value.log
    val appName = JvmPackaging.releaseAppName.value
    val binary  = sgeNativeBinary.value
    val outDir  = target.value / "sge-native" / appName

    IO.delete(outDir)
    IO.createDirectory(outDir)

    // Copy the native executable
    val isWindows = System.getProperty("os.name", "").toLowerCase.contains("win")
    val exeName   = if (isWindows) s"$appName.exe" else appName
    val dest      = outDir / exeName
    IO.copyFile(binary, dest)
    dest.setExecutable(true)

    // Ad-hoc sign the native binary on macOS so Gatekeeper doesn't block it
    if (hostIsMac) codesignAdHoc(dest, log)

    // Copy native shared libraries (if any)
    val nativeDir     = outDir / "lib"
    var hasNativeLibs = false
    for {
      dir <- JvmPackaging.releaseNativeLibDirs.value if dir.exists()
      file <- IO.listFiles(dir) if NativeLibExts.exists(file.getName.endsWith)
    } {
      if (!hasNativeLibs) { IO.createDirectory(nativeDir); hasNativeLibs = true }
      IO.copyFile(file, nativeDir / file.getName)
    }

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
    sgePackageNative := Def.uncached(packageNativeTask.value)
  )

  // ── Helpers ────────────────────────────────────────────────────────

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
