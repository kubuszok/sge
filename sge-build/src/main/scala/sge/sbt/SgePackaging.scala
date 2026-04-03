package sge.sbt

import multiarch.sbt.Platform

import sbt._
import sbt.Keys._
import sbt.complete.DefaultParsers._

import java.net.URI
import java.nio.file.{Files, StandardCopyOption}

/** Opt-in application packaging for SGE game projects.
  *
  * Two packaging modes are available:
  *
  * '''Simple mode''' (`sgePackage`) — produces a directory with launcher scripts
  * that rely on a system-installed JDK:
  * {{{
  * <app>/
  * ├── bin/
  * │   ├── <app>         (Unix launcher)
  * │   └── <app>.bat     (Windows launcher)
  * ├── lib/
  * │   └── *.jar         (application + dependency JARs)
  * └── native/
  *     └── *.{so,dylib,dll}  (platform shared libraries)
  * }}}
  *
  * '''Distribution mode''' (`sgePackagePlatform`, `sgePackageAll`) — produces
  * self-contained archives per target platform, bundling a jlinked JRE and
  * a native Roast launcher. No system JDK required by end users:
  * {{{
  * <app>-linux-x86_64.tar.gz
  * <app>-macos-aarch64.tar.gz   (contains .app bundle)
  * <app>-windows-x86_64.zip
  * }}}
  *
  * Enable simple mode with `.settings(SgePackaging.jvmSettings *)`.
  * Enable distribution mode by also adding `.settings(SgePackaging.distSettings *)`.
  */
object SgePackaging {

  // ── Keys: simple mode ─────────────────────────────────────────────

  val sgePackage       = taskKey[File]("Create a distributable SGE application package (simple mode)")
  val sgeAppName       = settingKey[String]("Application display name for the package")
  val sgeNativeLibDirs = settingKey[Seq[File]]("Directories containing native shared libraries to bundle")

  // ── Keys: distribution mode ───────────────────────────────────────

  val sgeTargets          = settingKey[Map[Platform, String]]("Target platforms and their JDK download URLs")
  val sgeJlinkModules     = settingKey[Seq[String]]("Java modules to include in the jlinked runtime")
  val sgeRoastVersion     = settingKey[String]("Roast native launcher version")
  val sgeVmArgs           = settingKey[Seq[String]]("JVM arguments passed via Roast config")
  val sgeUseZgc           = settingKey[Boolean]("Enable ZGC garbage collector on supported platforms")
  val sgeMacOsBundleId    = settingKey[String]("macOS bundle identifier (e.g. com.example.MyGame)")
  val sgeMacOsIcon        = settingKey[Option[File]]("Path to .icns file for macOS app bundle")
  val sgeCacheDir         = settingKey[File]("Download cache directory for JDKs and Roast binaries")
  val sgeRunOnFirstThread = settingKey[Boolean]("Run JVM on first thread (required for macOS graphics)")
  val sgeCrossNativeLibDir = settingKey[Option[File]](
    "Cross-compilation output root. When set, dist packaging uses <dir>/<platform-classifier>/ per platform."
  )
  val sgePackagePlatform  = inputKey[File]("Package for a single target platform")
  val sgePackageAll       = taskKey[Seq[File]]("Package for all configured target platforms")

  // ── Constants ─────────────────────────────────────────────────────

  private val DefaultRoastVersion = "1.5.0"

  private val DefaultJlinkModules = Seq(
    "java.base",
    "java.desktop",
    "java.logging",
    "java.management",
    "jdk.unsupported",
    "jdk.zipfs"
  )

  private val NativeLibExts = Set(".so", ".dylib", ".dll")

  // ── Simple mode implementation ────────────────────────────────────

  private val packageJvm: Def.Initialize[Task[File]] = Def.task {
    val log     = streams.value.log
    val appName = sgeAppName.value
    val mainCls = (Compile / mainClass).value.getOrElse {
      sys.error("sgePackage requires Compile / mainClass to be set")
    }
    val outDir = target.value / "sge-package" / appName

    IO.delete(outDir)

    // ── lib/ — application JAR + dependency JARs
    val libDir = outDir / "lib"
    IO.createDirectory(libDir)
    val appJar = (Compile / packageBin).value
    IO.copyFile(appJar, libDir / appJar.getName)
    val deps = (Compile / dependencyClasspathAsJars).value.map(_.data)
    deps.foreach(jar => IO.copyFile(jar, libDir / jar.getName))

    // ── native/ — platform shared libraries
    val nativeDir = outDir / "native"
    IO.createDirectory(nativeDir)
    for {
      dir  <- sgeNativeLibDirs.value if dir.exists()
      file <- IO.listFiles(dir) if NativeLibExts.exists(file.getName.endsWith)
    } IO.copyFile(file, nativeDir / file.getName)

    // ── bin/ — launcher scripts
    val binDir   = outDir / "bin"
    IO.createDirectory(binDir)
    val jarNames = (appJar +: deps).map(_.getName)

    writeUnixLauncher(binDir / appName, jarNames, mainCls)
    writeWindowsLauncher(binDir / s"$appName.bat", jarNames, mainCls)

    log.info(s"[sge] Package created: ${outDir.getAbsolutePath}")
    log.info(s"[sge] Run with: ${(binDir / appName).getAbsolutePath}")
    outDir
  }

  private def writeUnixLauncher(file: File, jars: Seq[String], mainClass: String): Unit = {
    val cp = jars.map(j => "\"$APP_HOME/lib/" + j + "\"").mkString(":")
    val script =
      s"""#!/bin/sh
         |set -e
         |APP_HOME="$$(cd "$$(dirname "$$0")/.." && pwd)"
         |exec java \\
         |  --enable-native-access=ALL-UNNAMED \\
         |  -Djava.library.path="$$APP_HOME/native" \\
         |  -cp $cp \\
         |  $mainClass "$$@"
         |""".stripMargin
    IO.write(file, script)
    file.setExecutable(true)
  }

  private def writeWindowsLauncher(file: File, jars: Seq[String], mainClass: String): Unit = {
    val cp = jars.map(j => s"%APP_HOME%\\lib\\$j").mkString(";")
    val script =
      s"""@echo off
         |set APP_HOME=%~dp0..
         |java ^
         |  --enable-native-access=ALL-UNNAMED ^
         |  -Djava.library.path="%APP_HOME%\\native" ^
         |  -cp "$cp" ^
         |  $mainClass %*
         |""".stripMargin
    IO.write(file, script)
  }

  // ── Simple mode settings ──────────────────────────────────────────

  /** JVM packaging settings (simple mode).  Apply with `.settings(SgePackaging.jvmSettings *)`.
    * Note: `sgeNativeLibDirs` is wired to `sgeNativeLibDir` by SgeProject's defaults.
    * This only sets `sgeAppName` and the `sgePackage` task — the native lib dir is inherited.
    */
  lazy val jvmSettings: Seq[Setting[_]] = Seq(
    sgeAppName := name.value,
    sgePackage := packageJvm.value
  )

  // ── Distribution mode implementation ──────────────────────────────

  /** Open a URL following redirects across hosts (java.net.HttpURLConnection won't). */
  private def openWithRedirects(url: String, maxRedirects: Int): java.io.InputStream = {
    var currentUrl = url
    var remaining  = maxRedirects
    while (remaining > 0) {
      val conn = new URI(currentUrl).toURL.openConnection().asInstanceOf[java.net.HttpURLConnection]
      conn.setConnectTimeout(30000)
      conn.setReadTimeout(300000)
      conn.setInstanceFollowRedirects(false)
      val code = conn.getResponseCode
      if (code >= 300 && code < 400) {
        val location = conn.getHeaderField("Location")
        conn.disconnect()
        if (location == null) throw new RuntimeException(s"Redirect with no Location header from $currentUrl")
        currentUrl = location
        remaining -= 1
      } else if (code == 200) {
        return conn.getInputStream
      } else {
        conn.disconnect()
        throw new RuntimeException(s"HTTP $code from $currentUrl")
      }
    }
    throw new RuntimeException(s"Too many redirects for $url")
  }

  /** Download a file to the cache directory, skipping if already present. Returns the cached file. */
  private def downloadToCache(url: String, cacheDir: File, log: sbt.util.Logger): File = {
    // Derive a stable filename from the URL
    val fileName = url.split('/').last.split('?').head
    val cached   = cacheDir / fileName
    if (cached.exists()) {
      log.info(s"[sge] Using cached: ${cached.getName}")
      cached
    } else {
      IO.createDirectory(cacheDir)
      log.info(s"[sge] Downloading: $url")
      val tmpFile = Files.createTempFile(cacheDir.toPath, "download-", ".tmp")
      try {
        // Follow redirects manually (HttpURLConnection won't follow cross-host redirects)
        val in = openWithRedirects(url, maxRedirects = 5)
        try Files.copy(in, tmpFile, StandardCopyOption.REPLACE_EXISTING)
        finally in.close()
        Files.move(tmpFile, cached.toPath, StandardCopyOption.ATOMIC_MOVE)
        log.info(s"[sge] Downloaded: ${cached.getName} (${cached.length() / 1024 / 1024} MB)")
        cached
      } catch {
        case e: Exception =>
          Files.deleteIfExists(tmpFile)
          throw new RuntimeException(s"Failed to download $url: ${e.getMessage}", e)
      }
    }
  }

  /** Extract a .zip archive. Returns the extraction directory. */
  private def extractZip(archive: File, destDir: File, log: sbt.util.Logger): Unit = {
    IO.createDirectory(destDir)
    val zipFile = new java.util.zip.ZipFile(archive)
    try {
      val entries = zipFile.entries()
      while (entries.hasMoreElements) {
        val entry = entries.nextElement()
        val dest  = destDir.toPath.resolve(entry.getName)
        if (entry.isDirectory) {
          Files.createDirectories(dest)
        } else {
          Files.createDirectories(dest.getParent)
          val is = zipFile.getInputStream(entry)
          try Files.copy(is, dest, StandardCopyOption.REPLACE_EXISTING)
          finally is.close()
          // Preserve executable bit for binaries
          val name = entry.getName
          if (!name.contains(".") || name.endsWith(".sh") || name.contains("/bin/")) {
            dest.toFile.setExecutable(true)
          }
        }
      }
    } finally zipFile.close()
  }

  /** Extract a .tar.gz archive using the system `tar` command. */
  private def extractTarGz(archive: File, destDir: File, log: sbt.util.Logger): Unit = {
    IO.createDirectory(destDir)
    val proc = new ProcessBuilder("tar", "xzf", archive.getAbsolutePath, "-C", destDir.getAbsolutePath)
      .redirectErrorStream(true)
      .start()
    val exit = proc.waitFor()
    if (exit != 0) {
      val output = new String(proc.getInputStream.readAllBytes())
      throw new RuntimeException(s"tar extraction failed (exit $exit): $output")
    }
  }

  /** Extract an archive (auto-detects format). */
  private def extractArchive(archive: File, destDir: File, log: sbt.util.Logger): Unit = {
    val name = archive.getName.toLowerCase
    if (name.endsWith(".tar.gz") || name.endsWith(".tgz")) {
      extractTarGz(archive, destDir, log)
    } else if (name.endsWith(".zip") || name.endsWith(".exe.zip")) {
      extractZip(archive, destDir, log)
    } else {
      throw new RuntimeException(s"Unknown archive format: ${archive.getName}")
    }
  }

  /** Find the JDK root inside an extracted archive (the directory containing bin/java). */
  private def findJdkRoot(extractedDir: File): File = {
    // The JDK archive typically has a single top-level directory like jdk-22.0.2+9/
    val topLevel = IO.listFiles(extractedDir).filter(_.isDirectory)
    // Check each top-level dir for bin/java or bin/java.exe
    topLevel.find { dir =>
      (dir / "bin" / "java").exists() || (dir / "bin" / "java.exe").exists()
    }.orElse {
      // Maybe the extracted dir IS the JDK root (macOS .tar.gz can have Contents/Home/)
      topLevel.flatMap { dir =>
        val contentsHome = dir / "Contents" / "Home"
        if ((contentsHome / "bin" / "java").exists()) Some(contentsHome) else None
      }.headOption
    }.getOrElse {
      throw new RuntimeException(
        s"Could not find JDK root in ${extractedDir.getAbsolutePath}. " +
          s"Contents: ${topLevel.map(_.getName).mkString(", ")}"
      )
    }
  }

  /** Download and extract a JDK, returning the JDK root directory. */
  private def resolveJdk(url: String, platform: Platform, cacheDir: File, log: sbt.util.Logger): File = {
    val jdkCache = cacheDir / "jdks"
    val archive  = downloadToCache(url, jdkCache, log)
    // Use a platform-specific extraction dir to avoid collisions
    val extractDir = jdkCache / s"extracted-${platform.classifier}"
    if (!extractDir.exists() || IO.listFiles(extractDir).isEmpty) {
      log.info(s"[sge] Extracting JDK for $platform...")
      IO.delete(extractDir)
      extractArchive(archive, extractDir, log)
    }
    findJdkRoot(extractDir)
  }

  /** Map SGE platform to Roast asset name. */
  private def roastAssetName(platform: Platform): String = {
    val roastOs = if (platform.isWindows) "win" else platform.os
    val arch    = platform.arch
    if (platform.isWindows) s"roast-$roastOs-$arch.exe.zip"
    else s"roast-$roastOs-$arch.zip"
  }

  /** Download Roast native launcher binary, returning the executable file. */
  private def resolveRoast(version: String, platform: Platform, cacheDir: File, log: sbt.util.Logger): File = {
    val asset     = roastAssetName(platform)
    val roastDir  = cacheDir / "roast" / version
    val url       = s"https://github.com/fourlastor-alexandria/roast/releases/download/$version/$asset"
    val archive   = downloadToCache(url, roastDir, log)

    val extractDir = roastDir / platform.classifier
    if (!extractDir.exists() || IO.listFiles(extractDir).isEmpty) {
      IO.delete(extractDir)
      extractZip(archive, extractDir, log)
    }

    // Find the Roast binary (it's the only file in the zip, possibly named "roast" or "roast.exe")
    IO.listFiles(extractDir)
      .find(f => f.isFile && (f.getName.startsWith("roast")))
      .getOrElse(throw new RuntimeException(s"Roast binary not found in $extractDir"))
  }

  /** Run jlink to create a minimal JRE.
    *
    * When the target OS matches the host OS, uses the target JDK's own jlink
    * binary directly (avoids version mismatch).
    *
    * For cross-OS targets (e.g. packaging Windows from macOS), extracts jlink
    * classes from the target JDK's jmod files and runs them via the host JVM's
    * classpath. This avoids both the "can't run .exe on macOS" problem and the
    * "jlink version X does not match target java.base version Y" mismatch.
    */
  private def runJlink(
      targetJdkRoot: File,
      targetPlatform: Platform,
      modules: Seq[String],
      outputDir: File,
      log: sbt.util.Logger
  ): File = {
    val targetJmodsDir = targetJdkRoot / "jmods"
    if (!targetJmodsDir.exists()) {
      throw new RuntimeException(
        s"jmods directory not found in target JDK: ${targetJmodsDir.getAbsolutePath}. " +
          "Ensure the JDK download URL points to a full JDK (not a JRE)."
      )
    }

    IO.delete(outputDir)

    val hostOs = Platform.host.os
    val targetOs = targetPlatform.os

    val cmd = if (targetOs == hostOs) {
      // Same OS: use the target JDK's own jlink binary (version always matches)
      val jlink = {
        val unix = targetJdkRoot / "bin" / "jlink"
        val exe  = targetJdkRoot / "bin" / "jlink.exe"
        if (unix.exists()) unix else if (exe.exists()) exe
        else throw new RuntimeException(
          s"jlink not found in target JDK at ${targetJdkRoot / "bin"}. " +
            "Ensure the JDK download URL points to a full JDK (not a JRE)."
        )
      }
      log.info(s"[sge] Using target JDK's jlink: ${jlink.getAbsolutePath}")
      Seq(
        jlink.getAbsolutePath,
        "--module-path", targetJmodsDir.getAbsolutePath,
        "--add-modules", modules.mkString(","),
        "--output", outputDir.getAbsolutePath,
        "--strip-debug",
        "--no-header-files",
        "--no-man-pages",
        "--compress", "zip-6"
      )
    } else {
      // Cross-OS: can't run target jlink binary, and host jlink's version check
      // uses Runtime.version() which can't be overridden. Copy the full target
      // JDK runtime instead (larger but always correct).
      log.info(s"[sge] Cross-OS: copying full target JDK runtime for ${targetPlatform.classifier}")
      IO.createDirectory(outputDir)
      val dirsToKeep = Set("bin", "lib", "conf", "legal")
      val children = targetJdkRoot.listFiles()
      if (children != null) {
        children.foreach { child =>
          if (dirsToKeep.contains(child.getName)) {
            IO.copyDirectory(child, outputDir / child.getName)
          }
        }
      }
      return outputDir
    }

    log.info(s"[sge] Running jlink with modules: ${modules.mkString(", ")}")
    val proc = new ProcessBuilder(cmd: _*)
      .redirectErrorStream(true)
      .start()
    val output = new String(proc.getInputStream.readAllBytes())
    val exit   = proc.waitFor()
    if (exit != 0) {
      throw new RuntimeException(s"jlink failed (exit $exit):\n$output")
    }

    outputDir
  }

  /** Write the Roast JSON configuration file. */
  private def writeRoastConfig(
      configFile: File,
      appName: String,
      jars: Seq[String],
      mainClass: String,
      vmArgs: Seq[String],
      useZgc: Boolean,
      runOnFirstThread: Boolean,
      platform: Platform,
      hasNativeLibs: Boolean
  ): Unit = {
    val cpEntries = jars.map(j => s""""app/$j"""").mkString(",\n    ")

    // Build vmArgs including native library path if needed
    val allVmArgs = {
      val base = vmArgs :+ "--enable-native-access=ALL-UNNAMED"
      val withNative =
        if (hasNativeLibs) base :+ s"-Djava.library.path=native"
        else base
      withNative
    }
    val vmArgsJson = allVmArgs.map(a => s""""$a"""").mkString(",\n    ")

    val isMac = platform.isMac
    val json =
      s"""{
         |  "classPath": [
         |    $cpEntries
         |  ],
         |  "mainClass": "$mainClass",
         |  "vmArgs": [
         |    $vmArgsJson
         |  ],
         |  "args": [],
         |  "useZgcIfSupportedOs": $useZgc,
         |  "runOnFirstThread": ${runOnFirstThread && isMac}
         |}
         |""".stripMargin
    IO.write(configFile, json)
  }

  /** Write a macOS Info.plist file. */
  private def writeInfoPlist(
      file: File,
      appName: String,
      bundleId: String,
      hasIcon: Boolean
  ): Unit = {
    val iconEntry = if (hasIcon) {
      s"""  <key>CFBundleIconFile</key>
         |  <string>$appName.icns</string>""".stripMargin
    } else ""

    val plist =
      s"""<?xml version="1.0" encoding="UTF-8"?>
         |<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
         |<plist version="1.0">
         |<dict>
         |  <key>CFBundleExecutable</key>
         |  <string>$appName</string>
         |  <key>CFBundleIdentifier</key>
         |  <string>$bundleId</string>
         |  <key>CFBundleName</key>
         |  <string>$appName</string>
         |  <key>CFBundleVersion</key>
         |  <string>1.0.0</string>
         |  <key>CFBundleShortVersionString</key>
         |  <string>1.0.0</string>
         |  <key>CFBundlePackageType</key>
         |  <string>APPL</string>
         |  <key>NSHighResolutionCapable</key>
         |  <true/>
         |$iconEntry
         |</dict>
         |</plist>
         |""".stripMargin
    IO.write(file, plist)
  }

  /** Assemble a distribution package for a single platform. Returns the archive file. */
  private def assemblePlatform(
      platform: Platform,
      appName: String,
      mainClass: String,
      appJar: File,
      deps: Seq[File],
      nativeLibDirs: Seq[File],
      crossNativeLibDir: Option[File],
      jlinkedRuntime: File,
      roastBinary: File,
      vmArgs: Seq[String],
      useZgc: Boolean,
      runOnFirstThread: Boolean,
      macOsBundleId: String,
      macOsIcon: Option[File],
      distDir: File,
      log: sbt.util.Logger
  ): File = {
    val isMac     = platform.isMac
    val isWindows = platform.isWindows
    val archiveName = s"$appName-${platform.classifier}"
    val workDir   = distDir / archiveName

    IO.delete(workDir)

    // Determine the root inside the archive
    val root = if (isMac) {
      val appBundle = workDir / s"$appName.app" / "Contents"
      IO.createDirectory(appBundle)
      appBundle
    } else {
      IO.createDirectory(workDir)
      workDir
    }

    // ── Roast launcher binary
    // Roast resolves app/<config>.json relative to its own directory, so on
    // macOS we place app/, runtime/, native/ inside MacOS/ alongside the
    // launcher binary (not directly under Contents/).
    val launcherDir = if (isMac) root / "MacOS" else root
    IO.createDirectory(launcherDir)
    val launcherName = if (isWindows) s"$appName.exe" else appName
    val launcher     = launcherDir / launcherName
    IO.copyFile(roastBinary, launcher)
    launcher.setExecutable(true)

    // ── app/ — JARs + Roast config (sibling of launcher for Roast path resolution)
    val appDir = launcherDir / "app"
    IO.createDirectory(appDir)

    // Filter out Android-only JARs that contain stub implementations
    // (android.jar throws RuntimeException("Stub!") on all methods).
    // PanamaPort JARs (Core, Unsafe, VarHandles, etc.) are Android-only too.
    val androidJarNames = Set("android.jar")
    val androidJarPrefixes = Seq(
      "Core-v", "Unsafe-v", "VarHandles-v", "LLVM-v",
      "SunCleanerStub-v", "R8Annotations-v", "SunUnsafeWrapper-v",
      "DexFile-v", "AndroidMisc-v"
    )
    def isAndroidOnlyJar(f: File): Boolean = {
      val name = f.getName
      androidJarNames.contains(name) ||
        androidJarPrefixes.exists(p => name.startsWith(p))
    }
    val filteredDeps = deps.filterNot(isAndroidOnlyJar)
    val allJars = appJar +: filteredDeps
    // Deduplicate JARs by name (dependency resolution can produce duplicates)
    val seenNames = scala.collection.mutable.Set.empty[String]
    val uniqueJars = allJars.filter { jar =>
      val name = jar.getName
      if (seenNames.contains(name)) false
      else { seenNames += name; true }
    }
    uniqueJars.foreach(jar => IO.copyFile(jar, appDir / jar.getName))

    // Resolve per-platform native lib directories: prefer cross/<classifier>/ when available
    val effectiveNativeLibDirs: Seq[File] = crossNativeLibDir match {
      case Some(crossDir) =>
        val platDir = crossDir / platform.classifier
        if (platDir.exists() && IO.listFiles(platDir).exists(f => NativeLibExts.exists(f.getName.endsWith))) {
          log.info(s"[sge] Using cross-compiled native libs from ${platDir.getAbsolutePath}")
          Seq(platDir)
        } else {
          log.warn(s"[sge] Cross native lib dir not found for ${platform.classifier}, falling back to default nativeLibDirs")
          nativeLibDirs
        }
      case None => nativeLibDirs
    }

    val hasNativeLibs = effectiveNativeLibDirs.exists { dir =>
      dir.exists() && IO.listFiles(dir).exists(f => NativeLibExts.exists(f.getName.endsWith))
    }
    writeRoastConfig(
      appDir / s"$appName.json",
      appName, uniqueJars.map(_.getName), mainClass,
      vmArgs, useZgc, runOnFirstThread, platform, hasNativeLibs
    )

    // ── runtime/ — jlinked JRE (sibling of launcher for Roast path resolution)
    val runtimeDir = launcherDir / "runtime"
    IO.copyDirectory(jlinkedRuntime, runtimeDir)

    // ── native/ — platform shared libraries (sibling of launcher)
    if (hasNativeLibs) {
      val nativeDir = launcherDir / "native"
      IO.createDirectory(nativeDir)
      for {
        dir  <- effectiveNativeLibDirs if dir.exists()
        file <- IO.listFiles(dir) if NativeLibExts.exists(file.getName.endsWith)
      } IO.copyFile(file, nativeDir / file.getName)
    }

    // ── macOS-specific: Info.plist + icon
    if (isMac) {
      writeInfoPlist(root / "Info.plist", appName, macOsBundleId, macOsIcon.isDefined)
      macOsIcon.foreach { icon =>
        val resourcesDir = root / "Resources"
        IO.createDirectory(resourcesDir)
        IO.copyFile(icon, resourcesDir / s"$appName.icns")
      }
    }

    // ── Ad-hoc code signing (macOS host only) ──
    // Sign dylibs, Roast launcher, and .app bundle so Gatekeeper doesn't block them.
    if (hostIsMac && (isMac || !isWindows)) {
      // Sign all native dylibs
      val nativeDir = launcherDir / "native"
      if (nativeDir.exists()) {
        IO.listFiles(nativeDir)
          .filter(f => f.getName.endsWith(".dylib"))
          .foreach(f => codesignAdHoc(f, log))
      }
      // Sign the Roast launcher binary
      codesignAdHoc(launcher, log)
      // Sign the .app bundle (deep sign covers all nested code)
      if (isMac) {
        val appBundle = workDir / s"$appName.app"
        val deepSign = new ProcessBuilder(
          "codesign", "--force", "--deep", "--sign", "-", appBundle.getAbsolutePath
        ).redirectErrorStream(true).start()
        val deepOut = new String(deepSign.getInputStream.readAllBytes())
        val deepExit = deepSign.waitFor()
        if (deepExit != 0) log.warn(s"[sge] Deep signing failed for $appName.app: $deepOut")
        else log.info(s"[sge] Ad-hoc signed: $appName.app (deep)")
      }
    }

    // ── Create archive
    val archiveFile = if (isWindows) {
      createZipArchive(workDir, distDir / s"$archiveName.zip", log)
    } else {
      createTarGzArchive(workDir, distDir / s"$archiveName.tar.gz", log)
    }

    log.info(s"[sge] Distribution package: ${archiveFile.getAbsolutePath} (${archiveFile.length() / 1024 / 1024} MB)")
    archiveFile
  }

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
    val base      = sourceDir.toPath
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

  // ── Distribution mode tasks ───────────────────────────────────────

  private val packagePlatformTask: Def.Initialize[InputTask[File]] = Def.inputTask {
    val platformStr = token(Space ~> StringBasic).parsed
    val platform    = Platform.fromClassifier(platformStr)
    val log         = streams.value.log
    val targets     = sgeTargets.value

    if (!targets.contains(platform)) {
      val available = if (targets.isEmpty) "none configured" else targets.keys.mkString(", ")
      sys.error(s"Platform '$platform' not in sgeTargets. Available: $available")
    }

    val appName  = sgeAppName.value
    val mainCls  = (Compile / mainClass).value.getOrElse {
      sys.error("sgePackagePlatform requires Compile / mainClass to be set")
    }
    val cacheDir = sgeCacheDir.value
    val distDir  = target.value / "sge-dist"

    log.info(s"[sge] Packaging $appName for $platform...")

    // Step 1: Resolve JARs
    val appJar = (Compile / packageBin).value
    val deps   = (Compile / dependencyClasspathAsJars).value.map(_.data)

    // Step 2: Download and extract target JDK
    val jdkRoot = resolveJdk(targets(platform), platform, cacheDir, log)

    // Step 3: Create jlinked runtime
    val runtimeDir = distDir / s"runtime-$platform"
    val jlinked    = runJlink(jdkRoot, platform, sgeJlinkModules.value, runtimeDir, log)

    // Step 4: Download Roast launcher
    val roast = resolveRoast(sgeRoastVersion.value, platform, cacheDir, log)

    // Step 5: Assemble and archive
    assemblePlatform(
      platform, appName, mainCls, appJar, deps,
      sgeNativeLibDirs.value, sgeCrossNativeLibDir.value, jlinked, roast,
      sgeVmArgs.value, sgeUseZgc.value, sgeRunOnFirstThread.value,
      sgeMacOsBundleId.value, sgeMacOsIcon.value,
      distDir, log
    )
  }

  private val packageAllTask: Def.Initialize[Task[Seq[File]]] = Def.task {
    val log      = streams.value.log
    val targets  = sgeTargets.value
    val appName  = sgeAppName.value
    val mainCls  = (Compile / mainClass).value.getOrElse {
      sys.error("sgePackageAll requires Compile / mainClass to be set")
    }
    val cacheDir = sgeCacheDir.value
    val distDir  = target.value / "sge-dist"
    val appJar   = (Compile / packageBin).value
    val deps     = (Compile / dependencyClasspathAsJars).value.map(_.data)
    val modules  = sgeJlinkModules.value
    val version  = sgeRoastVersion.value
    val vmArgs   = sgeVmArgs.value
    val useZgc   = sgeUseZgc.value
    val rft      = sgeRunOnFirstThread.value
    val bundleId = sgeMacOsBundleId.value
    val icon     = sgeMacOsIcon.value
    val nativeDirs = sgeNativeLibDirs.value
    val crossDir = sgeCrossNativeLibDir.value

    if (targets.isEmpty) {
      log.warn("[sge] sgeTargets is empty — no platforms to package.")
      Seq.empty
    } else {
      log.info(s"[sge] Packaging $appName for ${targets.size} platform(s): ${targets.keys.mkString(", ")}")

      targets.toSeq.map { case (platform, jdkUrl) =>
        log.info(s"[sge] ── $platform ──")

        val jdkRoot    = resolveJdk(jdkUrl, platform, cacheDir, log)
        val runtimeDir = distDir / s"runtime-$platform"
        val jlinked    = runJlink(jdkRoot, platform, modules, runtimeDir, log)
        val roast      = resolveRoast(version, platform, cacheDir, log)

        assemblePlatform(
          platform, appName, mainCls, appJar, deps,
          nativeDirs, crossDir, jlinked, roast,
          vmArgs, useZgc, rft, bundleId, icon,
          distDir, log
        )
      }
    }
  }

  // ── Distribution mode settings ────────────────────────────────────

  /** Distribution packaging settings.  Apply with `.settings(SgePackaging.distSettings *)`.
    * Requires `jvmSettings` to also be applied.
    */
  lazy val distSettings: Seq[Setting[_]] = Seq(
    sgeTargets          := Map.empty,
    sgeJlinkModules     := DefaultJlinkModules,
    sgeRoastVersion     := DefaultRoastVersion,
    sgeVmArgs           := Seq.empty,
    sgeUseZgc           := true,
    sgeMacOsBundleId    := s"com.sge.${name.value}",
    sgeMacOsIcon        := None,
    sgeCacheDir         := Path.userHome / ".cache" / "sge",
    sgeRunOnFirstThread := true,
    sgeCrossNativeLibDir := None,
    sgePackagePlatform  := packagePlatformTask.evaluated,
    sgePackageAll       := packageAllTask.value
  )

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
    val appName  = sgeAppName.value
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
    sgeBrowserTitle          := sgeAppName.value,
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

  private val packageNativeTask: Def.Initialize[Task[File]] = Def.task {
    val log     = streams.value.log
    val appName = sgeAppName.value
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
      dir  <- sgeNativeLibDirs.value if dir.exists()
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

  // ── macOS ad-hoc code signing ──────────────────────────────────────

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

  // ── Unified release ────────────────────────────────────────────────

  val sgeRelease = taskKey[Seq[File]]("Build all distribution packages: JVM (all platforms) + Browser + Native")
}
