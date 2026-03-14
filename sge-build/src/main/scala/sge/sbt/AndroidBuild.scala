package sge.sbt

import sbt._
import sbt.Keys._
import scala.sys.process.{Process => SysProcess}

/** sbt settings and tasks for building Android APKs from Scala — no Gradle.
  *
  * Build pipeline: scalac (JVM bytecode) → d8 (DEX) → aapt2 (package) → apksigner
  *
  * The Android SDK is only required when running android* tasks (androidDex, androidPackage, etc.).
  * Normal compilation works without the SDK — android.jar is added to unmanagedJars only if present.
  *
  * Usage in build.sbt:
  * {{{
  * lazy val `sge-android` = (project in file("sge-android"))
  *   .settings(AndroidBuild.settings *)
  *   .dependsOn(sge.jvm(SgePlugin.scalaVersion))
  * }}}
  */
object AndroidBuild {

  // ── Custom keys ─────────────────────────────────────────────────────

  val androidSdkRoot = taskKey[File]("Resolve or download Android SDK root directory")
  val androidMinSdk  = settingKey[Int]("Minimum Android SDK version")
  val androidTargetSdk = settingKey[Int]("Target Android SDK version")
  val androidBuildToolsVersion = settingKey[String]("Android build-tools version")

  val androidDex     = taskKey[File]("Compile classes to DEX format using d8")
  val androidPackage = taskKey[File]("Package APK using aapt2")
  val androidSign    = taskKey[File]("Sign APK using apksigner with debug keystore")
  val androidInstall = taskKey[Unit]("Install APK on connected device via adb")

  // ── Settings ────────────────────────────────────────────────────────

  /** Android build tasks only — no SgePlugin.commonSettings.
    *
    * Use this when adding Android APK support to a project that already
    * has its own Scala/sbt settings (e.g. projectMatrix JVM axis).
    * Adds: androidDex, androidPackage, androidSign, androidInstall tasks
    * plus android.jar on the compile classpath.
    */
  val taskSettings: Seq[Setting[_]] = Seq(
    // SDK config (static, no download)
    androidMinSdk := AndroidSdk.minSdkVersion,
    androidTargetSdk := AndroidSdk.targetSdkVersion,
    androidBuildToolsVersion := AndroidSdk.buildToolsVersion,

    // SDK resolution — task, only runs when android tasks are invoked
    androidSdkRoot := {
      val base = (ThisBuild / baseDirectory).value
      val log  = streams.value.log
      AndroidSdk.ensureSdk(base, log)
    },

    // Add android.jar to compile classpath IF present (non-blocking for sbt load).
    Compile / unmanagedJars ++= {
      val base = (ThisBuild / baseDirectory).value
      AndroidSdk.findSdkRoot(base).toSeq.flatMap { sdkRoot =>
        val jar = AndroidSdk.androidJar(sdkRoot)
        if (jar.exists()) Seq(Attributed.blank(jar)) else Seq.empty
      }
    },

    // PanamaPort runtime dependencies — needed for Android's Panama FFM backport.
    // These are AARs on Maven Central; AndroidDeps downloads, extracts classes.jar,
    // and adds them to the classpath so d8 includes them in the APK.
    Compile / unmanagedJars ++= {
      val cacheDir = streams.value.cacheDirectory / "panama-port-deps"
      val log      = streams.value.log
      AndroidDeps.resolvePanamaPort(cacheDir, log).map(Attributed.blank)
    },

    // Fork for JVM tests
    fork := true,

    // ── DEX compilation ───────────────────────────────────────────────
    androidDex := {
      val log     = streams.value.log
      val sdk     = androidSdkRoot.value
      val target  = crossTarget.value / "android"
      val dexDir  = target / "dex"
      IO.createDirectory(dexDir)

      // Depends on compilation
      val _ = (Compile / compile).value

      // Collect all class files from this project + dependencies
      val cp = (Compile / fullClasspath).value.map(_.data)
      val jars = cp.filter(f => f.isFile && f.getName.endsWith(".jar"))
      val classDirs = cp.filter(_.isDirectory)

      // Create a fat JAR of all classes for d8 input
      val fatJar = target / "classes.jar"
      log.info(s"Creating input JAR for d8: $fatJar")
      val jarEntries = scala.collection.mutable.Set[String]()
      val jarOut = new java.util.jar.JarOutputStream(new java.io.FileOutputStream(fatJar))
      try {
        // Add class directories
        classDirs.foreach { dir =>
          val base = dir.toPath
          val walker = java.nio.file.Files.walk(base)
          try {
            walker.forEach { path =>
              if (java.nio.file.Files.isRegularFile(path)) {
                val entry = base.relativize(path).toString.replace('\\', '/')
                if (jarEntries.add(entry)) {
                  jarOut.putNextEntry(new java.util.jar.JarEntry(entry))
                  java.nio.file.Files.copy(path, jarOut)
                  jarOut.closeEntry()
                }
              }
            }
          } finally walker.close()
        }
        // Add dependency JARs (excluding android.jar — it's compile-only)
        jars.filterNot(_.getName == "android.jar").foreach { jar =>
          val zipIn = new java.util.zip.ZipFile(jar)
          try {
            val entries = zipIn.entries()
            while (entries.hasMoreElements) {
              val entry = entries.nextElement()
              if (!entry.isDirectory && !entry.getName.startsWith("META-INF/")) {
                if (jarEntries.add(entry.getName)) {
                  jarOut.putNextEntry(new java.util.jar.JarEntry(entry.getName))
                  val is = zipIn.getInputStream(entry)
                  try is.transferTo(jarOut)
                  finally is.close()
                  jarOut.closeEntry()
                }
              }
            }
          } finally zipIn.close()
        }
      } finally jarOut.close()

      // Run d8
      val d8Path    = AndroidSdk.d8(sdk)
      val minApi    = androidMinSdk.value
      log.info(s"Running d8 (minApi=$minApi) ...")
      val d8Cmd = Seq(
        d8Path.getAbsolutePath,
        "--min-api", minApi.toString,
        "--output", dexDir.getAbsolutePath,
        fatJar.getAbsolutePath
      )
      val d8Exit = SysProcess(d8Cmd).!(log)
      if (d8Exit != 0) throw new RuntimeException(s"d8 failed with exit code $d8Exit")

      log.info(s"DEX output: $dexDir")
      dexDir
    },

    // ── APK packaging ─────────────────────────────────────────────────
    androidPackage := {
      val log    = streams.value.log
      val sdk    = androidSdkRoot.value
      val target = crossTarget.value / "android"
      val dexDir = androidDex.value

      val manifestDir = (Compile / resourceDirectory).value
      val manifest    = manifestDir / "AndroidManifest.xml"
      if (!manifest.exists()) {
        throw new RuntimeException(
          s"AndroidManifest.xml not found at $manifest — create it in src/main/resources/"
        )
      }

      val aapt2Path = AndroidSdk.aapt2(sdk)
      val apkBase   = target / "app-unsigned.apk"

      log.info("Packaging APK with aapt2...")
      val linkCmd = Seq(
        aapt2Path.getAbsolutePath,
        "link",
        "-o", apkBase.getAbsolutePath,
        "-I", AndroidSdk.androidJar(sdk).getAbsolutePath,
        "--manifest", manifest.getAbsolutePath,
        "--min-sdk-version", androidMinSdk.value.toString,
        "--target-sdk-version", androidTargetSdk.value.toString
      )
      val linkExit = SysProcess(linkCmd).!(log)
      if (linkExit != 0) throw new RuntimeException(s"aapt2 link failed with exit code $linkExit")

      // Add all DEX files (multi-dex: classes.dex, classes2.dex, classes3.dex, ...)
      val dexFiles = IO.listFiles(dexDir).filter(_.getName.endsWith(".dex")).map(_.getName).sorted
      addFilesToZip(apkBase, dexDir, dexFiles)

      // Add native libs from src/main/resources/lib/ (project-local)
      val nativeLibsDir = (Compile / resourceDirectory).value / "lib"
      if (nativeLibsDir.isDirectory) {
        val basePath = nativeLibsDir.toPath
        val nativeFiles = (nativeLibsDir ** "*").get.filter(_.isFile).flatMap { f =>
          val rel = basePath.relativize(f.toPath).toString.replace('\\', '/')
          Seq((f, s"lib/$rel"))
        }
        addFilesToZipWithPaths(apkBase, nativeFiles)
      }

      // Extract native .so files from dependency JARs (e.g. sge JAR bundles native/android-*/*.so)
      // and add them as lib/<abi>/*.so in the APK for Android's native lib loader.
      val cp = (Compile / fullClasspath).value.map(_.data)
      val extractedNativeLibs = extractNativeLibsFromJars(cp, target / "native-libs", log)
      if (extractedNativeLibs.nonEmpty) {
        addFilesToZipWithPaths(apkBase, extractedNativeLibs)
      }

      val aligned = target / "app-aligned.apk"
      val zPath   = AndroidSdk.zipalign(sdk)
      log.info("Zipaligning APK...")
      val zalignCmd = Seq(
        zPath.getAbsolutePath,
        "-f", "-p", "4",
        apkBase.getAbsolutePath,
        aligned.getAbsolutePath
      )
      val zalignExit = SysProcess(zalignCmd).!(log)
      if (zalignExit != 0) throw new RuntimeException(s"zipalign failed with exit code $zalignExit")

      log.info(s"Unsigned APK: $aligned")
      aligned
    },

    // ── APK signing ───────────────────────────────────────────────────
    androidSign := {
      val log     = streams.value.log
      val sdk     = androidSdkRoot.value
      val target  = crossTarget.value / "android"
      val aligned = androidPackage.value

      val debugKs = target / "debug.keystore"
      if (!debugKs.exists()) {
        log.info("Creating debug keystore...")
        val keytoolCmd = Seq(
          "keytool",
          "-genkeypair", "-v",
          "-keystore", debugKs.getAbsolutePath,
          "-alias", "androiddebugkey",
          "-keyalg", "RSA", "-keysize", "2048",
          "-validity", "10000",
          "-storepass", "android", "-keypass", "android",
          "-dname", "CN=Debug,O=SGE,L=Unknown,S=Unknown,C=US"
        )
        val ksExit = SysProcess(keytoolCmd).!(log)
        if (ksExit != 0) throw new RuntimeException(s"keytool failed with exit code $ksExit")
      }

      val signed = target / "app-debug.apk"
      IO.copyFile(aligned, signed)

      val signCmd = Seq(
        AndroidSdk.apksigner(sdk).getAbsolutePath,
        "sign",
        "--ks", debugKs.getAbsolutePath,
        "--ks-pass", "pass:android",
        "--ks-key-alias", "androiddebugkey",
        signed.getAbsolutePath
      )
      log.info("Signing APK...")
      val signExit = SysProcess(signCmd).!(log)
      if (signExit != 0) throw new RuntimeException(s"apksigner failed with exit code $signExit")

      log.info(s"Signed APK: $signed")
      signed
    },

    // ── Install on device ─────────────────────────────────────────────
    androidInstall := {
      val log    = streams.value.log
      val sdk    = androidSdkRoot.value
      val signed = androidSign.value

      val adb = sdk / "platform-tools" / "adb"
      if (!adb.exists()) {
        throw new RuntimeException(
          s"adb not found at $adb — install platform-tools via sdkmanager"
        )
      }
      val installCmd = Seq(adb.getAbsolutePath, "install", "-r", signed.getAbsolutePath)
      log.info("Installing APK on device...")
      val installExit = SysProcess(installCmd).!(log)
      if (installExit != 0) throw new RuntimeException(s"adb install failed with exit code $installExit")
      log.info("APK installed successfully.")
    }
  )

  /** Full settings including SgePlugin.commonSettings.
    *
    * Use this for standalone Android modules (e.g. sge-android-smoke).
    */
  val settings: Seq[Setting[_]] = SgePlugin.commonSettings ++ taskSettings


  // ── Helpers ─────────────────────────────────────────────────────────

  /** Adds files from a directory to an existing zip/APK. */
  private def addFilesToZip(apk: File, srcDir: File, fileNames: Seq[String]): Unit = {
    val tmpApk = new File(apk.getParent, apk.getName + ".tmp")
    val zipIn  = new java.util.zip.ZipFile(apk)
    val zipOut = new java.util.zip.ZipOutputStream(new java.io.FileOutputStream(tmpApk))
    try {
      // Copy existing entries (preserving compression method — resources.arsc must stay STORED)
      val entries = zipIn.entries()
      while (entries.hasMoreElements) {
        val entry    = entries.nextElement()
        val newEntry = new java.util.zip.ZipEntry(entry.getName)
        if (entry.getMethod == java.util.zip.ZipEntry.STORED) {
          newEntry.setMethod(java.util.zip.ZipEntry.STORED)
          newEntry.setSize(entry.getSize)
          newEntry.setCompressedSize(entry.getCompressedSize)
          newEntry.setCrc(entry.getCrc)
        }
        zipOut.putNextEntry(newEntry)
        val is = zipIn.getInputStream(entry)
        try is.transferTo(zipOut)
        finally is.close()
        zipOut.closeEntry()
      }
      // Add new files
      fileNames.foreach { name =>
        val f = srcDir / name
        if (f.exists()) {
          zipOut.putNextEntry(new java.util.zip.ZipEntry(name))
          val is = new java.io.FileInputStream(f)
          try is.transferTo(zipOut)
          finally is.close()
          zipOut.closeEntry()
        }
      }
    } finally {
      zipOut.close()
      zipIn.close()
    }
    IO.move(tmpApk, apk)
  }

  /** Mapping from sge JAR native/ classifiers to APK lib/ ABI directories. */
  private val nativeClassifierToAbi: Map[String, String] = Map(
    "android-aarch64" -> "arm64-v8a",
    "android-armv7"   -> "armeabi-v7a",
    "android-x86_64"  -> "x86_64"
  )

  /** Extract native .so files from dependency JARs.
    *
    * Scans all JAR files on the classpath for entries matching `native/android-<arch>/<name>.so`,
    * extracts them to a temp directory, and returns (file, archivePath) pairs for APK inclusion.
    */
  private def extractNativeLibsFromJars(
      cp: Seq[File],
      extractDir: File,
      log: sbt.util.Logger
  ): Seq[(File, String)] = {
    IO.createDirectory(extractDir)
    val jars = cp.filter(f => f.isFile && f.getName.endsWith(".jar"))
    val result = scala.collection.mutable.Buffer[(File, String)]()

    jars.foreach { jar =>
      val zipIn = new java.util.zip.ZipFile(jar)
      try {
        import scala.jdk.CollectionConverters._
        zipIn.entries().asScala.foreach { entry =>
          val name = entry.getName
          // Match native/android-<classifier>/<libname>.so
          if (name.startsWith("native/android-") && name.endsWith(".so") && !entry.isDirectory) {
            val parts = name.split('/')
            if (parts.length == 3) {
              val classifier = parts(1)
              val libName    = parts(2)
              nativeClassifierToAbi.get(classifier).foreach { abi =>
                val outFile = extractDir / abi / libName
                IO.createDirectory(outFile.getParentFile)
                val is = zipIn.getInputStream(entry)
                try IO.transfer(is, outFile)
                finally is.close()
                val archivePath = s"lib/$abi/$libName"
                result += ((outFile, archivePath))
                log.info(s"  Extracted native lib: $archivePath")
              }
            }
          }
        }
      } finally zipIn.close()
    }

    result.toSeq
  }

  /** Adds files with custom archive paths to an existing zip/APK. */
  private def addFilesToZipWithPaths(apk: File, files: Seq[(File, String)]): Unit = {
    val tmpApk = new File(apk.getParent, apk.getName + ".tmp")
    val zipIn  = new java.util.zip.ZipFile(apk)
    val zipOut = new java.util.zip.ZipOutputStream(new java.io.FileOutputStream(tmpApk))
    try {
      val entries = zipIn.entries()
      while (entries.hasMoreElements) {
        val entry    = entries.nextElement()
        val newEntry = new java.util.zip.ZipEntry(entry.getName)
        if (entry.getMethod == java.util.zip.ZipEntry.STORED) {
          newEntry.setMethod(java.util.zip.ZipEntry.STORED)
          newEntry.setSize(entry.getSize)
          newEntry.setCompressedSize(entry.getCompressedSize)
          newEntry.setCrc(entry.getCrc)
        }
        zipOut.putNextEntry(newEntry)
        val is = zipIn.getInputStream(entry)
        try is.transferTo(zipOut)
        finally is.close()
        zipOut.closeEntry()
      }
      files.foreach { case (f, archivePath) =>
        if (f.isFile) {
          zipOut.putNextEntry(new java.util.zip.ZipEntry(archivePath))
          val is = new java.io.FileInputStream(f)
          try is.transferTo(zipOut)
          finally is.close()
          zipOut.closeEntry()
        }
      }
    } finally {
      zipOut.close()
      zipIn.close()
    }
    IO.move(tmpApk, apk)
  }
}
