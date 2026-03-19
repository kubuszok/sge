package sgedev
package native

import java.io.File

/** Rust native library operations — self-contained, no Justfile dependency. */
object NativeCmd {

  private def nativeDir: String = s"${Paths.projectRoot}/native-components"
  private def crossOutDir: String = s"$nativeDir/target/cross"
  private val rustTargets = List(
    "x86_64-apple-darwin", "aarch64-apple-darwin",
    "x86_64-unknown-linux-gnu", "aarch64-unknown-linux-gnu",
    "x86_64-pc-windows-msvc", "aarch64-pc-windows-msvc"
  )

  def run(args: List[String]): Unit = {
    args match {
      case Nil | "--help" :: _ =>
        println("""Usage: sge-dev native <command>
                  |
                  |Commands:
                  |  build [--static] [--android] [--freetype] [--physics] [--all-features] [--features F]
                  |  cross <target>            Cross-compile for specific target
                  |  cross-all                 Build all 6 desktop targets + collect
                  |  cross-android [target]    Build for Android NDK targets
                  |  collect                   Collect cross-compiled artifacts
                  |  test                      Run Rust unit tests
                  |  setup-toolchain           Install cross-compilation toolchains
                  |  angle {setup,download,cross-collect}
                  |  release-prep              Full release preparation""".stripMargin)
      case "build" :: rest => build(Cli.parse(rest))
      case "cross" :: rest => cross(rest)
      case "cross-all" :: _ => crossAll()
      case "cross-android" :: rest => crossAndroid(rest)
      case "collect" :: _ => collect()
      case "test" :: _ => cargoExec(List("test"))
      case "setup-toolchain" :: _ => setupToolchain()
      case "angle" :: rest => angle(rest)
      case "release-prep" :: _ => releasePrepare()
      case other :: _ =>
        Term.err(s"Unknown native command: $other")
        sys.exit(1)
    }
  }

  private def build(args: Cli.Args): Unit = {
    if (args.hasFlag("static")) {
      // Build release and remove dylib/so so Scala Native links statically
      cargoExec(List("build", "--release"))
      new File(s"$nativeDir/target/release/libsge_native_ops.dylib").delete()
      new File(s"$nativeDir/target/release/libsge_native_ops.so").delete()
    } else if (args.hasFlag("android")) {
      crossAndroidAll()
    } else if (args.hasFlag("freetype")) {
      cargoExec(List("build", "--release", "--features", "freetype_support"))
    } else if (args.hasFlag("physics")) {
      cargoExec(List("build", "--release", "--features", "physics"))
    } else if (args.hasFlag("all-features")) {
      cargoExec(List("build", "--release", "--features", "all"))
    } else {
      val features = args.flag("features")
      features match {
        case Some(f) => cargoExec(List("build", "--release", "--features", f))
        case None => cargoExec(List("build", "--release"))
      }
    }
  }

  private def cross(args: List[String]): Unit = {
    val target = args.headOption.getOrElse {
      Term.err("Target required (e.g. x86_64-apple-darwin, aarch64-unknown-linux-gnu)")
      sys.exit(1)
      ""
    }
    crossTarget(target)
  }

  private def crossTarget(target: String): Unit = {
    Term.info(s"Building for $target...")
    // Add rustup target
    Proc.run("rustup", List("target", "add", target))

    if (target.contains("-apple-darwin")) {
      cargoExec(List("build", "--release", "--target", target))
    } else if (target.contains("-linux-gnu")) {
      // Use cargo-zigbuild for Linux targets
      Proc.exec("cargo", List("zigbuild", "--release", "--target", target), cwd = Some(nativeDir))
    } else if (target.contains("-windows-msvc")) {
      // Use cargo-xwin for Windows targets
      Proc.exec("cargo", List("xwin", "build", "--release", "--target", target), cwd = Some(nativeDir))
    } else {
      Term.err(s"Unknown target: $target")
      sys.exit(1)
    }
  }

  private def crossAll(): Unit = {
    for (target <- rustTargets) {
      println(s"=== Building $target ===")
      crossTarget(target)
    }
    println()
    println("=== Collecting artifacts ===")
    collect()
  }

  private def crossAndroid(args: List[String]): Unit = {
    args.headOption match {
      case Some(target) => crossAndroidTarget(target)
      case None => crossAndroidAll()
    }
  }

  private def crossAndroidAll(): Unit = {
    val targets = List("aarch64-linux-android", "armv7-linux-androideabi", "x86_64-linux-android")
    for (target <- targets) {
      println(s"=== Building $target ===")
      crossAndroidTarget(target)
    }
  }

  private def crossAndroidTarget(target: String): Unit = {
    // Prefer rustup cargo over Homebrew
    val hostTriple = {
      val r = Proc.run("rustc", List("-vV"))
      r.stdout.linesIterator.find(_.startsWith("host: ")).map(_.drop(6).trim)
        .getOrElse("aarch64-apple-darwin")
    }
    val rustupBin = s"${System.getProperty("user.home")}/.rustup/toolchains/stable-$hostTriple/bin"

    val androidNdk = s"${Paths.projectRoot}/demos/android-sdk/ndk/27.2.12479018"
    val ndkBin = s"$androidNdk/toolchains/llvm/prebuilt/darwin-x86_64/bin"

    val ndkPrefix = target match {
      case "aarch64-linux-android" => "aarch64-linux-android26"
      case "armv7-linux-androideabi" => "armv7a-linux-androideabi26"
      case "x86_64-linux-android" => "x86_64-linux-android26"
      case _ => Term.err(s"Unknown Android target: $target"); sys.exit(1); ""
    }

    val targetUnder = target.replace('-', '_')
    val env = Map(
      s"CC_$targetUnder" -> s"$ndkBin/$ndkPrefix-clang",
      s"CXX_$targetUnder" -> s"$ndkBin/$ndkPrefix-clang++",
      s"AR_$targetUnder" -> s"$ndkBin/llvm-ar",
      "ANDROID_NDK_HOME" -> androidNdk,
      "PATH" -> s"$rustupBin:${sys.env.getOrElse("PATH", "")}"
    )

    Proc.run("rustup", List("target", "add", target))
    val code = Proc.exec("cargo", List("build", "--release", "--target", target, "--features", "android"),
      cwd = Some(nativeDir), env = env)
    if (code != 0) sys.exit(code)
  }

  private def collect(): Unit = {
    val out = crossOutDir
    // Clean output directory
    val outDir = new File(out)
    if (outDir.exists()) deleteDir(outDir)

    for (target <- rustTargets) {
      val dir = s"$nativeDir/target/$target/release"
      val plat = target match {
        case "x86_64-apple-darwin" => "macos-x86_64"
        case "aarch64-apple-darwin" => "macos-aarch64"
        case "x86_64-unknown-linux-gnu" => "linux-x86_64"
        case "aarch64-unknown-linux-gnu" => "linux-aarch64"
        case "x86_64-pc-windows-msvc" => "windows-x86_64"
        case "aarch64-pc-windows-msvc" => "windows-aarch64"
        case _ => target
      }
      val platDir = new File(s"$out/$plat")
      platDir.mkdirs()

      // Copy all native library files
      val libPatterns = List(
        "libsge_native_ops.dylib", "libsge_native_ops.so", "sge_native_ops.dll",
        "libsge_native_ops.a", "sge_native_ops.lib", "sge_native_ops.dll.lib",
        "libsge_audio.dylib", "libsge_audio.so", "sge_audio.dll", "libsge_audio.a",
        "libglfw.dylib", "libglfw.so", "glfw3.dll", "libglfw3.a",
        "libEGL.dylib", "libEGL.so", "libEGL.dll",
        "libGLESv2.dylib", "libGLESv2.so", "GLESv2.dll"
      )
      val files = scala.collection.mutable.ListBuffer.empty[String]
      for (lib <- libPatterns) {
        val src = new File(s"$dir/$lib")
        if (src.exists()) {
          java.nio.file.Files.copy(src.toPath, new File(platDir, lib).toPath,
            java.nio.file.StandardCopyOption.REPLACE_EXISTING)
          files += lib
        }
      }
      if (files.nonEmpty) {
        println(s"  $plat: ${files.mkString(" ")}")
      }
    }
    println()
    println(s"Artifacts collected in $out/")
  }

  private def setupToolchain(): Unit = {
    println("Installing Rust cross-compilation tools...")
    println()
    println("1. cargo-zigbuild (for Linux targets):")
    Proc.exec("cargo", List("install", "cargo-zigbuild"))
    println()
    println("2. cargo-xwin (for Windows targets):")
    Proc.exec("cargo", List("install", "cargo-xwin"))
    println()
    println("3. Zig (required by cargo-zigbuild):")
    val hasBrew = Proc.run("which", List("brew")).ok
    if (hasBrew) {
      Proc.exec("brew", List("install", "zig"))
    } else {
      println("  Install Zig from https://ziglang.org/download/")
    }
    println()
    println("4. Adding Rust targets...")
    for (target <- rustTargets) {
      Proc.exec("rustup", List("target", "add", target))
    }
    println()
    println("Done! Run 'sge-dev native cross-all' to build all targets.")
  }

  private def angle(args: List[String]): Unit = {
    args match {
      case Nil => println("Usage: sge-dev native angle {setup,download,cross-collect}")
      case "setup" :: _ => angleSetup()
      case "download" :: _ => angleDownload()
      case "cross-collect" :: _ => angleCrossCollect()
      case other :: _ =>
        Term.err(s"Unknown angle command: $other")
        sys.exit(1)
    }
  }

  private def angleSetup(): Unit = {
    val dest = s"$nativeDir/target/release"
    new File(dest).mkdirs()

    // Check if already present
    if (new File(s"$dest/libEGL.dylib").exists() && new File(s"$dest/libGLESv2.dylib").exists()) {
      println(s"ANGLE libraries already present in $dest")
    } else {
      val os = System.getProperty("os.name")
      if (os.contains("Mac")) {
        // macOS: copy from Homebrew
        val cellarDirs = List("/opt/homebrew/Cellar/angle", "/usr/local/Cellar/angle")
        val angleLib = cellarDirs.flatMap { cellar =>
          val dir = new File(cellar)
          if (dir.exists()) {
            dir.listFiles().filter(_.isDirectory).sortBy(_.getName).lastOption
              .map(v => s"${v.getAbsolutePath}/lib")
          } else { None }
        }.headOption

        angleLib match {
          case Some(lib) =>
            Term.info(s"Copying ANGLE from $lib to $dest")
            java.nio.file.Files.copy(
              new File(s"$lib/libEGL.dylib").toPath,
              new File(s"$dest/libEGL.dylib").toPath,
              java.nio.file.StandardCopyOption.REPLACE_EXISTING)
            java.nio.file.Files.copy(
              new File(s"$lib/libGLESv2.dylib").toPath,
              new File(s"$dest/libGLESv2.dylib").toPath,
              java.nio.file.StandardCopyOption.REPLACE_EXISTING)
            // Fix install names
            Proc.exec("install_name_tool", List("-id", "@rpath/libEGL.dylib", s"$dest/libEGL.dylib"))
            Proc.exec("install_name_tool", List("-id", "@rpath/libGLESv2.dylib", s"$dest/libGLESv2.dylib"))
            Proc.run("install_name_tool", List("-change",
              "/opt/homebrew/opt/angle/lib/libEGL.dylib", "@rpath/libEGL.dylib",
              s"$dest/libGLESv2.dylib"))
            Proc.run("install_name_tool", List("-change",
              "/usr/local/opt/angle/lib/libEGL.dylib", "@rpath/libEGL.dylib",
              s"$dest/libGLESv2.dylib"))
            // Re-sign
            Proc.exec("codesign", List("--force", "--sign", "-", s"$dest/libEGL.dylib"))
            Proc.exec("codesign", List("--force", "--sign", "-", s"$dest/libGLESv2.dylib"))
            Term.ok(s"ANGLE libraries installed to $dest")
          case None =>
            Term.err("ANGLE not found in Homebrew. Install with: brew install startergo/angle/angle")
            sys.exit(1)
        }
      } else {
        Term.err(s"Automatic ANGLE setup not yet implemented for $os. Place libEGL and libGLESv2 in $dest manually.")
        sys.exit(1)
      }
    }
  }

  private def angleDownload(): Unit = {
    val cache = s"$nativeDir/target/angle-cache"
    val out = crossOutDir
    new File(cache).mkdirs()

    // macOS universal
    val macUrl = "https://github.com/nicedoc/angle-builder/releases/latest/download/angle-macos-universal.zip"
    downloadAndExtractAngle(cache, out, macUrl, "angle-macos-universal.zip",
      List("macos-aarch64", "macos-x86_64"), "dylib")

    // Windows x86_64
    val winUrl = "https://github.com/nicedoc/angle-builder/releases/latest/download/angle-windows-x86_64.zip"
    downloadAndExtractAngle(cache, out, winUrl, "angle-windows-x86_64.zip",
      List("windows-x86_64"), "dll")

    // Linux x86_64
    val linuxUrl = "https://github.com/nicedoc/angle-builder/releases/latest/download/angle-linux-x86_64.zip"
    downloadAndExtractAngle(cache, out, linuxUrl, "angle-linux-x86_64.zip",
      List("linux-x86_64"), "so")

    println()
    println("NOTE: Linux aarch64 and Windows aarch64 ANGLE not yet available.")
    println("      These platforms will use system GL if present.")
  }

  private def downloadAndExtractAngle(cache: String, out: String, url: String,
                                       zipName: String, platforms: List[String], ext: String): Unit = {
    val zipPath = s"$cache/$zipName"
    if (!new File(zipPath).exists()) {
      Term.info(s"Downloading $zipName...")
      val result = Proc.run("curl", List("-fSL", "-o", zipPath, url))
      if (!result.ok) {
        Term.warn(s"Download failed: $zipName")
      }
    }
    if (new File(zipPath).exists()) {
      val tmp = s"$cache/extract-${zipName.replace(".zip", "")}"
      deleteDir(new File(tmp))
      new File(tmp).mkdirs()
      Proc.exec("unzip", List("-qo", zipPath, "-d", tmp))
      for (plat <- platforms) {
        val platDir = new File(s"$out/$plat")
        platDir.mkdirs()
        copyAngleLib(tmp, s"$out/$plat", s"libEGL.$ext")
        copyAngleLib(tmp, s"$out/$plat", s"libGLESv2.$ext")
      }
      println(s"  $zipName -> ${platforms.mkString(", ")}")
    }
  }

  private def copyAngleLib(searchDir: String, destDir: String, libName: String): Unit = {
    findFileRecursive(new File(searchDir), libName).foreach { src =>
      java.nio.file.Files.copy(src.toPath, new File(s"$destDir/$libName").toPath,
        java.nio.file.StandardCopyOption.REPLACE_EXISTING)
    }
  }

  private def angleCrossCollect(): Unit = {
    val src = s"$nativeDir/target/release"
    val out = crossOutDir
    if (!new File(s"$src/libEGL.dylib").exists() || !new File(s"$src/libGLESv2.dylib").exists()) {
      Term.err(s"ANGLE libs not found in $src. Run 'sge-dev native angle setup' first.")
      sys.exit(1)
    }
    for (plat <- List("macos-aarch64", "macos-x86_64")) {
      val platDir = new File(s"$out/$plat")
      platDir.mkdirs()
      java.nio.file.Files.copy(new File(s"$src/libEGL.dylib").toPath,
        new File(platDir, "libEGL.dylib").toPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
      java.nio.file.Files.copy(new File(s"$src/libGLESv2.dylib").toPath,
        new File(platDir, "libGLESv2.dylib").toPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
      println(s"  ANGLE -> $out/$plat/")
    }
  }

  private def releasePrepare(): Unit = {
    crossAll()
    angleDownload()
  }

  private def cargoExec(args: List[String]): Unit = {
    val code = Proc.exec("cargo", args, cwd = Some(nativeDir))
    if (code != 0) sys.exit(code)
  }

  private def findFileRecursive(dir: File, name: String): Option[File] = {
    if (!dir.exists()) { None }
    else {
      val files = dir.listFiles()
      if (files == null) { None }
      else {
        files.find(f => f.isFile && f.getName == name).orElse(
          files.filter(_.isDirectory).flatMap(d => findFileRecursive(d, name)).headOption
        )
      }
    }
  }

  private def deleteDir(dir: File): Unit = {
    if (dir.exists()) {
      val files = dir.listFiles()
      if (files != null) {
        files.foreach { f =>
          if (f.isDirectory) deleteDir(f) else f.delete()
        }
      }
      dir.delete()
    }
  }
}
