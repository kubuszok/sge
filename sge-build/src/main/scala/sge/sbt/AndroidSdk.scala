package sge.sbt

import sbt._
import sbt.Keys._

import java.io.File
import java.net.URI
import java.nio.file.{Files, Path}

/** Android SDK management for sbt — no Gradle required.
  *
  * Resolution order for SDK location:
  *   1. `ANDROID_HOME` environment variable
  *   2. `ANDROID_SDK_ROOT` environment variable (deprecated but still common)
  *   3. Local `android-sdk/` directory in the project root (auto-downloaded)
  *
  * When no SDK is found, `ensureSdk()` downloads Android command-line tools
  * and installs the minimum platform + build-tools needed for compilation.
  */
object AndroidSdk {

  /** Minimum Android API level supported (Android 8.0, required by PanamaPort). */
  val minSdkVersion: Int = 26

  /** Target Android API level for compilation. */
  val targetSdkVersion: Int = 35

  /** Build tools version. */
  val buildToolsVersion: String = "35.0.0"

  /** Platform string (e.g. "android-35"). */
  val platformVersion: String = s"android-$targetSdkVersion"

  // ── SDK Resolution ──────────────────────────────────────────────────

  /** Resolves the Android SDK root directory. Returns None if not found and auto-download is disabled. */
  def findSdkRoot(projectBase: File): Option[File] = {
    // 1. ANDROID_HOME env var
    sys.env.get("ANDROID_HOME").map(new File(_)).filter(_.isDirectory)
      // 2. ANDROID_SDK_ROOT env var
      .orElse(sys.env.get("ANDROID_SDK_ROOT").map(new File(_)).filter(_.isDirectory))
      // 3. Local android-sdk/ in project
      .orElse {
        val local = new File(projectBase, "android-sdk")
        if (local.isDirectory) Some(local) else None
      }
  }

  /** Resolves the SDK root, downloading if necessary. */
  def ensureSdk(projectBase: File, log: sbt.util.Logger): File = {
    findSdkRoot(projectBase).getOrElse {
      val localSdk = new File(projectBase, "android-sdk")
      log.info(s"No Android SDK found. Downloading to $localSdk ...")
      downloadSdk(localSdk, log)
      localSdk
    }
  }

  // ── SDK Paths ───────────────────────────────────────────────────────

  /** Path to android.jar (compile-time API stubs). */
  def androidJar(sdkRoot: File): File =
    sdkRoot / "platforms" / platformVersion / "android.jar"

  /** Path to d8 (DEX compiler). */
  def d8(sdkRoot: File): File = {
    val base = sdkRoot / "build-tools" / buildToolsVersion
    val d8   = base / "d8"
    if (d8.exists()) d8
    else {
      // Windows: d8.bat
      val bat = base / "d8.bat"
      if (bat.exists()) bat
      else throw new RuntimeException(s"d8 not found in ${base.getAbsolutePath}")
    }
  }

  /** Path to the R8 JAR (shared with d8 in build-tools/lib/d8.jar).
    *
    * R8 is Android's code shrinker, optimizer, and DEX compiler. It shares a JAR
    * with d8 but provides a more robust bytecode pipeline — particularly for lambda
    * desugaring of Scala 3 bytecode that d8 mishandles (VerifyError: wide register
    * index out of range).
    *
    * Invoked as: `java -cp <r8Jar> com.android.tools.r8.R8 [options]`
    */
  def r8Jar(sdkRoot: File): File = {
    val jar = sdkRoot / "build-tools" / buildToolsVersion / "lib" / "d8.jar"
    if (jar.exists()) jar
    else throw new RuntimeException(s"R8 JAR (lib/d8.jar) not found in ${(sdkRoot / "build-tools" / buildToolsVersion).getAbsolutePath}")
  }

  /** Path to aapt2 (resource compiler & packager). */
  def aapt2(sdkRoot: File): File = {
    val base = sdkRoot / "build-tools" / buildToolsVersion
    val aapt = base / "aapt2"
    if (aapt.exists()) aapt
    else {
      val exe = base / "aapt2.exe"
      if (exe.exists()) exe
      else throw new RuntimeException(s"aapt2 not found in ${base.getAbsolutePath}")
    }
  }

  /** Path to apksigner (APK signing tool). */
  def apksigner(sdkRoot: File): File = {
    val base = sdkRoot / "build-tools" / buildToolsVersion
    val sign = base / "apksigner"
    if (sign.exists()) sign
    else {
      val bat = base / "apksigner.bat"
      if (bat.exists()) bat
      else throw new RuntimeException(s"apksigner not found in ${base.getAbsolutePath}")
    }
  }

  /** Path to zipalign. */
  def zipalign(sdkRoot: File): File = {
    val base = sdkRoot / "build-tools" / buildToolsVersion
    val z    = base / "zipalign"
    if (z.exists()) z
    else {
      val exe = base / "zipalign.exe"
      if (exe.exists()) exe
      else throw new RuntimeException(s"zipalign not found in ${base.getAbsolutePath}")
    }
  }

  /** Path to the Android NDK (optional, for native library bundling). */
  def ndkPath(sdkRoot: File): Option[File] = {
    val ndk = sdkRoot / "ndk"
    if (ndk.isDirectory) {
      // Find latest installed NDK version
      ndk.listFiles().filter(_.isDirectory).sortBy(_.getName).lastOption
    } else None
  }

  // ── SDK Download ────────────────────────────────────────────────────

  /** URL pattern for Android command-line tools. */
  private def cmdlineToolsUrl: String = {
    val os = sys.props("os.name").toLowerCase match {
      case n if n.contains("linux") => "linux"
      case n if n.contains("mac")   => "mac"
      case n if n.contains("win")   => "win"
      case n => throw new RuntimeException(s"Unsupported OS for Android SDK download: $n")
    }
    // Latest command-line tools (version 16.0)
    s"https://dl.google.com/android/repository/commandlinetools-$os-11076708_latest.zip"
  }

  /** Downloads and installs a minimal Android SDK. */
  private def downloadSdk(targetDir: File, log: sbt.util.Logger): Unit = {
    val tmpZip = Files.createTempFile("android-cmdline-tools", ".zip")
    try {
      // Download command-line tools
      log.info(s"Downloading Android command-line tools...")
      val url  = new URI(cmdlineToolsUrl).toURL
      val conn = url.openConnection()
      conn.setConnectTimeout(30000)
      conn.setReadTimeout(120000)
      val in = conn.getInputStream
      try Files.copy(in, tmpZip, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
      finally in.close()

      // Extract to targetDir/cmdline-tools/latest/
      val cmdlineDir = targetDir.toPath.resolve("cmdline-tools").resolve("latest")
      Files.createDirectories(cmdlineDir)

      log.info(s"Extracting command-line tools to $cmdlineDir ...")
      val zipFile = new java.util.zip.ZipFile(tmpZip.toFile)
      try {
        val entries = zipFile.entries()
        while (entries.hasMoreElements) {
          val entry = entries.nextElement()
          // Strip "cmdline-tools/" prefix from the zip entries
          val name  = entry.getName.stripPrefix("cmdline-tools/")
          if (name.nonEmpty) {
            val dest = cmdlineDir.resolve(name)
            if (entry.isDirectory) {
              Files.createDirectories(dest)
            } else {
              Files.createDirectories(dest.getParent)
              val is = zipFile.getInputStream(entry)
              try Files.copy(is, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
              finally is.close()
              // Preserve executable bit
              if (name.endsWith(".sh") || !name.contains(".")) {
                dest.toFile.setExecutable(true)
              }
            }
          }
        }
      } finally zipFile.close()

      // Use sdkmanager to install platform and build-tools
      val sdkmanager = cmdlineDir.resolve("bin").resolve("sdkmanager").toFile
      sdkmanager.setExecutable(true)

      log.info(s"Installing platform $platformVersion and build-tools $buildToolsVersion ...")
      val args = Seq(
        sdkmanager.getAbsolutePath,
        s"--sdk_root=${targetDir.getAbsolutePath}",
        s"platforms;$platformVersion",
        s"build-tools;$buildToolsVersion"
      )
      val proc = new ProcessBuilder(args: _*)
        .redirectErrorStream(true)
        .start()
      // Auto-accept licenses
      val out = proc.getOutputStream
      // Send 'y' multiple times for license acceptance
      (1 to 10).foreach { _ =>
        out.write("y\n".getBytes)
        out.flush()
      }
      out.close()
      val exit = proc.waitFor()
      if (exit != 0) {
        val output = new String(proc.getInputStream.readAllBytes())
        throw new RuntimeException(s"sdkmanager failed (exit $exit): $output")
      }

      log.info(s"Android SDK installed at $targetDir")
    } finally {
      Files.deleteIfExists(tmpZip)
    }
  }
}
