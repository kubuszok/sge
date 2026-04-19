package sge.sbt

import multiarch.sbt.{ JvmPackaging, Platform }

import sbt._
import sbt.Keys._

/** SGE-specific application packaging.
  *
  * JVM packaging (simple + distribution modes) is delegated to
  * [[multiarch.sbt.JvmPackaging]] from sbt-multiarch-scala.
  * This object provides SGE-specific browser (Scala.js) and native (Scala Native) packaging.
  *
  * === JVM packaging (via JvmPackaging) ===
  *
  * '''Simple mode''' (`releasePackage`) — directory with launcher scripts + system JDK.
  * '''Distribution mode''' (`releasePlatform`, `releaseAll`) — self-contained archives with jlinked JRE.
  *
  * === Browser packaging ===
  *
  * `sgePackageBrowser` — packages Scala.js output as a browser-ready directory with HTML.
  *
  * === Native packaging ===
  *
  * `sgePackageNative` — packages Scala Native executable into a distributable directory.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 307
 * Covenant-baseline-methods: NativeLibExts,SgePackaging,_,appName,archive,assetsDir,binary,browserSettings,cmd,codesignAdHoc,createTarGzArchive,createZipArchive,dest,dirName,dirs,distSettings,entries,exeName,exit,generateAssetManifestTask,generateHtml,hasNativeLibs,hostIsMac,html,isWindows,jsDir,jvmSettings,log,manifest,mimeType,n,nativeDir,nativeSettings,out,outDir,outFile,output,packageBrowserTask,packageNativeTask,parentDir,platform,proc,resDirs,sgeBrowserTitle,sgeGenerateAssetManifest,sgeJsOutputDir,sgeNativeBinary,sgePackageBrowser,sgePackageNative,sgeRelease,title
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
  */
object SgePackaging {

  // ── JVM packaging: delegated to JvmPackaging ──────────────────────

  /** JVM packaging settings (simple mode). */
  lazy val jvmSettings: Seq[Setting[_]] = JvmPackaging.jvmSettings

  /** Distribution packaging settings. Requires `jvmSettings` to also be applied. */
  lazy val distSettings: Seq[Setting[_]] = JvmPackaging.distSettings

  // ── Browser (Scala.js) packaging ───────────────────────────────────

  val sgePackageBrowser        = taskKey[File]("Package Scala.js output as a browser-ready directory with HTML")
  val sgeBrowserTitle          = settingKey[String]("HTML page title for the browser package")
  val sgeGenerateAssetManifest = taskKey[File]("Generate assets.txt manifest from project resources")

  /** The fullLinkJS output directory must be provided by the caller, since
    * sbt-scalajs types are not on this plugin's classpath. Wire it in build.sbt:
    * {{{
    * SgePackaging.sgeJsOutputDir := (Compile / fullLinkJS / scalaJSLinkerOutputDirectory).value
    * }}}
    */
  val sgeJsOutputDir = taskKey[File]("Directory containing fullLinkJS output (main.js)")

  private val generateAssetManifestTask: Def.Initialize[Task[File]] = Def.task {
    // Trigger resource generators first so generated files (e.g. test textures, audio) are present
    val _          = (Compile / managedResources).value
    val log        = streams.value.log
    val resDirs    = (Compile / unmanagedResourceDirectories).value ++ (Compile / managedResourceDirectories).value
    val outFile    = (Compile / resourceManaged).value / "assets.txt"

    val entries = scala.collection.mutable.ArrayBuffer[String]()
    val dirs    = scala.collection.mutable.Set[String]()

    resDirs.filter(_.exists()).foreach { base =>
      val baseDir = base.toPath
      (base ** "*").get.foreach { f =>
        if (f.isFile && f.getName != "AndroidManifest.xml" && f.getName != "assets.txt") {
          val rel  = baseDir.relativize(f.toPath).toString.replace('\\', '/')
          val size = f.length()
          val ext  = f.getName.toLowerCase match {
            case n if n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg") ||
              n.endsWith(".gif") || n.endsWith(".bmp") || n.endsWith(".webp") => "i"
            case n if n.endsWith(".wav") || n.endsWith(".ogg") || n.endsWith(".mp3") => "a"
            case n if n.endsWith(".json") || n.endsWith(".g3dj") || n.endsWith(".atlas") ||
              n.endsWith(".fnt") || n.endsWith(".txt") || n.endsWith(".xml") ||
              n.endsWith(".glsl") || n.endsWith(".vert") || n.endsWith(".frag") ||
              n.endsWith(".tmj") || n.endsWith(".tsj") || n.endsWith(".skin") => "t"
            case _ => "b" // binary fallback (g3db, etc.)
          }
          val mime = mimeType(f.getName)
          entries += s"$ext:$rel:$size:$mime"

          // Track parent directories
          var parent = rel
          while (parent.contains("/")) {
            parent = parent.substring(0, parent.lastIndexOf('/'))
            if (dirs.add(parent)) entries += s"d:$parent:0:"
          }
        }
      }
    }

    IO.write(outFile, entries.sorted.mkString("\n"))
    log.info(s"[sge] Asset manifest: ${entries.size} entries -> ${outFile.getAbsolutePath}")
    outFile
  }

  private def mimeType(name: String): String = {
    val n = name.toLowerCase
    if (n.endsWith(".png")) "image/png"
    else if (n.endsWith(".jpg") || n.endsWith(".jpeg")) "image/jpeg"
    else if (n.endsWith(".gif")) "image/gif"
    else if (n.endsWith(".bmp")) "image/bmp"
    else if (n.endsWith(".webp")) "image/webp"
    else if (n.endsWith(".wav")) "audio/wav"
    else if (n.endsWith(".ogg")) "audio/ogg"
    else if (n.endsWith(".mp3")) "audio/mpeg"
    else if (n.endsWith(".json") || n.endsWith(".g3dj") || n.endsWith(".tmj") || n.endsWith(".tsj")) "application/json"
    else if (n.endsWith(".xml")) "application/xml"
    else if (n.endsWith(".txt") || n.endsWith(".atlas") || n.endsWith(".fnt") || n.endsWith(".skin")) "text/plain"
    else if (n.endsWith(".glsl") || n.endsWith(".vert") || n.endsWith(".frag")) "text/plain"
    else "application/octet-stream"
  }

  private val packageBrowserTask: Def.Initialize[Task[File]] = Def.task {
    val log      = streams.value.log
    val appName  = JvmPackaging.releaseAppName.value
    val title    = sgeBrowserTitle.value
    val jsDir    = sgeJsOutputDir.value
    val outDir   = target.value / "sge-browser" / appName
    val resDirs  = (Compile / unmanagedResourceDirectories).value ++ (Compile / managedResourceDirectories).value
    val manifest = sgeGenerateAssetManifest.value

    IO.delete(outDir)
    IO.createDirectory(outDir)

    // Copy all JS files from fullLinkJS output
    IO.listFiles(jsDir).foreach { f =>
      IO.copyFile(f, outDir / f.getName)
    }

    // Copy resources into assets/ subdirectory
    val assetsDir = outDir / "assets"
    IO.createDirectory(assetsDir)
    resDirs.filter(_.exists()).foreach { base =>
      val baseDir = base.toPath
      (base ** "*").get.foreach { f =>
        if (f.isFile) {
          val rel       = baseDir.relativize(f.toPath).toString.replace('\\', '/')
          val targetFile = assetsDir / rel
          IO.createDirectory(targetFile.getParentFile)
          IO.copyFile(f, targetFile)
        }
      }
    }

    // Copy manifest into assets/
    IO.copyFile(manifest, assetsDir / "assets.txt")

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

  /** Browser packaging settings. Apply to JS platform projects.
    * Requires the caller to wire `sgeJsOutputDir` to `fullLinkJS / scalaJSLinkerOutputDirectory`.
    */
  lazy val browserSettings: Seq[Setting[_]] = Seq(
    sgeBrowserTitle          := JvmPackaging.releaseAppName.value,
    sgeGenerateAssetManifest := generateAssetManifestTask.value,
    sgePackageBrowser        := packageBrowserTask.value
  )

  // ── Scala Native packaging ─────────────────────────────────────────

  val sgePackageNative = taskKey[File]("Package Scala Native executable into a distributable directory")

  /** The nativeLink output file must be provided by the caller, since
    * sbt-scala-native types are not on this plugin's classpath. Wire it in build.sbt:
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
    val nativeDir = outDir / "lib"
    var hasNativeLibs = false
    for {
      dir  <- JvmPackaging.releaseNativeLibDirs.value if dir.exists()
      file <- IO.listFiles(dir) if NativeLibExts.exists(file.getName.endsWith)
    } {
      if (!hasNativeLibs) { IO.createDirectory(nativeDir); hasNativeLibs = true }
      IO.copyFile(file, nativeDir / file.getName)
    }

    // Create archive
    val platform  = Platform.host
    val archive   = if (isWindows) {
      createZipArchive(outDir, target.value / "sge-native" / s"$appName-native-${platform.classifier}.zip", log)
    } else {
      createTarGzArchive(outDir, target.value / "sge-native" / s"$appName-native-${platform.classifier}.tar.gz", log)
    }

    log.info(s"[sge] Native package: ${archive.getAbsolutePath} (${archive.length() / 1024 / 1024} MB)")
    archive
  }

  /** Scala Native packaging settings. Apply to Native platform projects.
    * Requires the caller to wire `sgeNativeBinary` to `Compile / nativeLink`.
    */
  lazy val nativeSettings: Seq[Setting[_]] = Seq(
    sgePackageNative := packageNativeTask.value
  )

  // ── Helpers ────────────────────────────────────────────────────────

  /** Ad-hoc sign a file with `codesign --force --sign -`.  Only meaningful on macOS.
    * Non-fatal: logs a warning on failure instead of throwing.
    */
  private def codesignAdHoc(file: File, log: sbt.util.Logger): Unit = {
    val proc = new ProcessBuilder("codesign", "--force", "--sign", "-", file.getAbsolutePath)
      .redirectErrorStream(true).start()
    val output = new String(proc.getInputStream.readAllBytes())
    val exit = proc.waitFor()
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
    val cmd = Seq("tar", "czf", archiveFile.getAbsolutePath, "-C", parentDir.getAbsolutePath, dirName)
    val proc = new ProcessBuilder(cmd: _*)
      .redirectErrorStream(true)
      .start()
    val exit = proc.waitFor()
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
      val allFiles = (sourceDir ** AllPassFilter).get.filter(_.isFile)
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
