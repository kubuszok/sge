package sgedev
package native

import java.io.File

/** Rust native library operations — self-contained, no Justfile dependency.
  *
  * Note: Rust native library source has moved to the external sge-native-components repo.
  * Build commands (build, cross, cross-all, cross-android) require that repo cloned alongside sge.
  * ANGLE and curl download commands still work locally (they populate the staging directory).
  * CI extracts provider JARs to sge-deps/native-components/target/ as a staging area.
  */
object NativeCmd {

  private def nativeDir: String = s"${Paths.projectRoot}/sge-deps/native-components"
  private def crossOutDir: String = s"$nativeDir/target/cross"
  private val rustTargets = List(
    "x86_64-apple-darwin", "aarch64-apple-darwin",
    "x86_64-unknown-linux-gnu", "aarch64-unknown-linux-gnu",
    "x86_64-pc-windows-msvc", "aarch64-pc-windows-msvc"
  )

  // ANGLE pre-built binaries from sge-angle-natives GitHub Releases
  private val AngleVersion = "chromium-7151"
  private val AngleRepo = "kubuszok/sge-angle-natives"
  private val AngleBaseUrl = s"https://github.com/$AngleRepo/releases/download/$AngleVersion"

  private val anglePlatforms = List(
    "macos-aarch64", "macos-x86_64",
    "linux-x86_64", "linux-aarch64",
    "windows-x86_64", "windows-aarch64"
  )

  // Static curl pre-built libraries from stunnel/static-curl GitHub Releases.
  // Provides libcurl.a + all transitive deps (OpenSSL, nghttp2, zstd, brotli,
  // libidn2, etc.) for self-contained Scala Native releases — no system
  // libcurl or libidn2 needed.
  private val CurlVersion = "8.19.0"
  private val CurlBaseUrl = s"https://github.com/stunnel/static-curl/releases/download/$CurlVersion"

  // stunnel/static-curl uses "arm64" for macOS, "aarch64" for Linux/Windows
  private def curlAssetPlatform(platform: String): String = platform match {
    case "macos-aarch64" => "macos-arm64"
    case other => other
  }

  private def curlArchiveName(platform: String): String =
    s"curl-${curlAssetPlatform(platform)}-dev-$CurlVersion.tar.xz"

  private def hostAnglePlatform: String = {
    val os = System.getProperty("os.name", "").toLowerCase
    val arch = System.getProperty("os.arch", "")
    val osName = if (os.contains("mac")) "macos"
      else if (os.contains("linux")) "linux"
      else if (os.contains("win")) "windows"
      else { Term.err(s"Unsupported OS: $os"); sys.exit(1); "" }
    val archName = arch match {
      case "amd64" | "x86_64" => "x86_64"
      case "aarch64" | "arm64" => "aarch64"
      case _ => Term.err(s"Unsupported arch: $arch"); sys.exit(1); ""
    }
    s"$osName-$archName"
  }

  private def angleLibExt(platform: String): String = {
    if (platform.startsWith("macos")) "dylib"
    else if (platform.startsWith("windows")) "dll"
    else "so"
  }

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
                  |  angle {setup,download,cross-collect,check}
                  |  curl  {setup,download,cross-collect,check}
                  |  release-prep              Full release preparation""".stripMargin)
      case "build" :: rest => build(Cli.parse(rest))
      case "cross" :: rest => cross(rest)
      case "cross-all" :: _ => crossAll()
      case "cross-android" :: rest => crossAndroid(rest)
      case "collect" :: _ => collect()
      case "test" :: _ => cargoExec(List("test"))
      case "setup-toolchain" :: _ => setupToolchain()
      case "angle" :: rest => angle(rest)
      case "curl" :: rest => curl(rest)
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

    val hostOs = System.getProperty("os.name").toLowerCase
    val code = if (target.contains("-apple-darwin")) {
      if (hostOs.contains("mac")) {
        // Native cross-arch compilation on macOS
        cargoExec(List("build", "--release", "--target", target))
        0 // cargoExec calls sys.exit on failure
      } else {
        // Cross-compile to macOS from Linux using zig (bundles macOS SDK)
        Proc.exec("cargo", List("zigbuild", "--release", "--target", target), cwd = Some(nativeDir))
      }
    } else if (target.contains("-linux-gnu")) {
      val hostArch = System.getProperty("os.arch")
      val isHostTarget = (target.contains("x86_64") && hostArch == "amd64") ||
                         (target.contains("aarch64") && (hostArch == "aarch64" || hostArch == "arm64"))
      if (hostOs.contains("linux") && isHostTarget) {
        // Native build on host — uses system headers (X11, etc.)
        Proc.exec("cargo", List("build", "--release", "--target", target), cwd = Some(nativeDir))
      } else {
        // Cross-arch via zigbuild (zig provides its own sysroot)
        Proc.exec("cargo", List("zigbuild", "--release", "--target", target), cwd = Some(nativeDir))
      }
    } else if (target.contains("-windows-msvc")) {
      // Use cargo-xwin for Windows targets. Needs llvm-lib on PATH
      // (from Homebrew LLVM, or system LLVM on CI).
      val llvmPath = if (new java.io.File("/opt/homebrew/opt/llvm/bin/llvm-lib").exists())
        s"/opt/homebrew/opt/llvm/bin:${sys.env.getOrElse("PATH", "")}"
      else
        sys.env.getOrElse("PATH", "")
      Proc.exec("cargo", List("xwin", "build", "--release", "--target", target),
        cwd = Some(nativeDir), env = Map("PATH" -> llvmPath))
    } else {
      Term.err(s"Unknown target: $target")
      sys.exit(1)
      1
    }
    if (code != 0) {
      Term.err(s"Failed to build for $target (exit code $code)")
      sys.exit(code)
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

    // Find Android NDK: check env vars first (CI), then local SDK dir
    val androidNdk = sys.env.getOrElse("ANDROID_NDK_HOME",
      sys.env.get("ANDROID_HOME").map(_ + "/ndk").flatMap { ndkParent =>
        val dir = new java.io.File(ndkParent)
        if (dir.exists()) dir.listFiles().filter(_.isDirectory).map(_.getAbsolutePath).sorted.lastOption
        else None
      }.getOrElse(s"${Paths.projectRoot}/sge-deps/android-sdk/ndk/27.2.12479018")
    )
    // Detect host OS for NDK prebuilt toolchain path
    val hostOs = System.getProperty("os.name").toLowerCase
    val ndkHostTag = if (hostOs.contains("mac")) "darwin-x86_64"
                     else if (hostOs.contains("linux")) "linux-x86_64"
                     else "windows-x86_64"
    val ndkBin = s"$androidNdk/toolchains/llvm/prebuilt/$ndkHostTag/bin"

    val ndkPrefix = target match {
      case "aarch64-linux-android" => "aarch64-linux-android26"
      case "armv7-linux-androideabi" => "armv7a-linux-androideabi26"
      case "x86_64-linux-android" => "x86_64-linux-android26"
      case _ => Term.err(s"Unknown Android target: $target"); sys.exit(1); ""
    }

    val targetUnder = target.replace('-', '_')
    val targetUpper = targetUnder.toUpperCase
    val linker = s"$ndkBin/$ndkPrefix-clang"
    val env = Map(
      s"CC_$targetUnder" -> linker,
      s"CXX_$targetUnder" -> s"$ndkBin/$ndkPrefix-clang++",
      s"AR_$targetUnder" -> s"$ndkBin/llvm-ar",
      // Override .cargo/config.toml linker (which hardcodes local macOS paths)
      s"CARGO_TARGET_${targetUpper}_LINKER" -> linker,
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
        "libGLESv2.dylib", "libGLESv2.so", "GLESv2.dll",
        // Static curl + transitive deps (from sge-dev native curl cross-collect)
        "libcurl.a", "libssl.a", "libcrypto.a", "libnghttp2.a", "libnghttp3.a",
        "libngtcp2.a", "libngtcp2_crypto_ossl.a", "libz.a", "libbrotlidec.a",
        "libbrotlicommon.a", "libidn2.a", "libunistring.a", "libcares.a",
        "libssh2.a", "libzstd.a", "libpsl.a"
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
      case Nil => println("Usage: sge-dev native angle {setup,download,cross-collect,check}")
      case "setup" :: _ => angleSetup()
      case "download" :: _ => angleDownload()
      case "cross-collect" :: _ => angleCrossCollect()
      case "check" :: _ => angleCheck()
      case other :: _ =>
        Term.err(s"Unknown angle command: $other")
        sys.exit(1)
    }
  }

  private def angleSetup(): Unit = {
    val dest = s"$nativeDir/target/release"
    new File(dest).mkdirs()
    val platform = hostAnglePlatform
    val ext = angleLibExt(platform)

    // Check if already present
    val eglFile = new File(s"$dest/libEGL.$ext")
    val glesFile = new File(s"$dest/libGLESv2.$ext")
    if (eglFile.exists() && glesFile.exists()) {
      println(s"ANGLE libraries already present in $dest")
    } else {
      // Download from sge-angle-natives releases
      val cacheDir = s"$nativeDir/target/angle-cache"
      new File(cacheDir).mkdirs()
      val archive = s"angle-$platform.tar.gz"
      val archivePath = s"$cacheDir/$archive"

      if (!new File(archivePath).exists()) {
        val url = s"$AngleBaseUrl/$archive"
        Term.info(s"Downloading ANGLE for $platform...")
        val result = Proc.run("curl", List("-fSL", "-o", archivePath, url))
        if (!result.ok) {
          Term.err(s"Download failed. URL: $url")
          Term.err("Fallback: install ANGLE via Homebrew (brew install startergo/angle/angle)")
          sys.exit(1)
        }
      }

      // Extract to temp dir then copy libs
      val tmp = s"$cacheDir/extract-$platform"
      deleteDir(new File(tmp))
      new File(tmp).mkdirs()
      Proc.exec("tar", List("xzf", archivePath, "-C", tmp))

      // Copy ANGLE libs to release dir
      copyAngleLib(tmp, dest, s"libEGL.$ext")
      copyAngleLib(tmp, dest, s"libGLESv2.$ext")

      // macOS: fix install names and re-sign
      if (platform.startsWith("macos")) {
        Proc.exec("install_name_tool", List("-id", "@rpath/libEGL.dylib", s"$dest/libEGL.dylib"))
        Proc.exec("install_name_tool", List("-id", "@rpath/libGLESv2.dylib", s"$dest/libGLESv2.dylib"))
        Proc.run("install_name_tool", List("-change",
          "/opt/homebrew/opt/angle/lib/libEGL.dylib", "@rpath/libEGL.dylib",
          s"$dest/libGLESv2.dylib"))
        Proc.exec("codesign", List("--force", "--sign", "-", s"$dest/libEGL.dylib"))
        Proc.exec("codesign", List("--force", "--sign", "-", s"$dest/libGLESv2.dylib"))
      }

      Term.ok(s"ANGLE $AngleVersion installed to $dest")
    }
  }

  private def angleDownload(): Unit = {
    val cache = s"$nativeDir/target/angle-cache"
    new File(cache).mkdirs()

    for (platform <- anglePlatforms) {
      val archive = s"angle-$platform.tar.gz"
      val archivePath = s"$cache/$archive"

      if (!new File(archivePath).exists()) {
        val url = s"$AngleBaseUrl/$archive"
        Term.info(s"Downloading $archive...")
        val result = Proc.run("curl", List("-fSL", "-o", archivePath, url))
        if (!result.ok) {
          Term.warn(s"Download failed: $archive (may not be available yet)")
        }
      }

      if (new File(archivePath).exists()) {
        val tmp = s"$cache/extract-$platform"
        deleteDir(new File(tmp))
        new File(tmp).mkdirs()
        Proc.exec("tar", List("xzf", archivePath, "-C", tmp))

        val out = crossOutDir
        val platDir = new File(s"$out/$platform")
        platDir.mkdirs()
        val ext = angleLibExt(platform)
        copyAngleLib(tmp, s"$out/$platform", s"libEGL.$ext")
        copyAngleLib(tmp, s"$out/$platform", s"libGLESv2.$ext")
        println(s"  $archive -> $platform")
      }
    }
  }

  private def copyAngleLib(searchDir: String, destDir: String, libName: String): Unit = {
    findFileRecursive(new File(searchDir), libName).foreach { src =>
      java.nio.file.Files.copy(src.toPath, new File(s"$destDir/$libName").toPath,
        java.nio.file.StandardCopyOption.REPLACE_EXISTING)
    }
  }

  private def angleCrossCollect(): Unit = {
    Term.info("Downloading ANGLE for all desktop platforms...")
    angleDownload()
    Term.ok("ANGLE cross-collect complete")
  }

  private def angleCheck(): Unit = {
    val dest = s"$nativeDir/target/release"
    val platform = hostAnglePlatform
    val ext = angleLibExt(platform)
    val egl = new File(s"$dest/libEGL.$ext")
    val gles = new File(s"$dest/libGLESv2.$ext")
    if (egl.exists() && gles.exists()) {
      Term.ok(s"ANGLE present for $platform in $dest")
    } else {
      Term.err(s"ANGLE NOT found for $platform in $dest")
      Term.err("Run: sge-dev native angle setup")
      sys.exit(1)
    }
  }

  // ── Curl static library management ────────────────────────────────

  private def curl(args: List[String]): Unit = {
    args match {
      case Nil => println("Usage: sge-dev native curl {setup,download,cross-collect,check}")
      case "setup" :: _ => curlSetup()
      case "download" :: _ => curlDownload()
      case "cross-collect" :: _ => curlCrossCollect()
      case "check" :: _ => curlCheck()
      case other :: _ =>
        Term.err(s"Unknown curl command: $other")
        sys.exit(1)
    }
  }

  /** Download static curl for the host platform into target/release/. */
  private def curlSetup(): Unit = {
    val dest = s"$nativeDir/target/release"
    new File(dest).mkdirs()
    val platform = hostAnglePlatform // same host detection

    // Check if already present (libcurl.a > 1MB indicates full static archive)
    val curlA = new File(s"$dest/libcurl.a")
    if (curlA.exists() && curlA.length() > 1000000) {
      println(s"Static curl already present in $dest")
      return
    }

    curlDownloadPlatform(platform, dest)
    Term.ok(s"Static curl $CurlVersion installed to $dest")
  }

  /** Download static curl for all 6 desktop platforms into target/cross/. */
  private def curlDownload(): Unit = {
    for (platform <- anglePlatforms) { // same 6 platforms as ANGLE
      val out = s"$crossOutDir/$platform"
      new File(out).mkdirs()
      curlDownloadPlatform(platform, out)
      println(s"  $platform: curl $CurlVersion")
    }
  }

  /** Download and extract curl static libs for a single platform. */
  private def curlDownloadPlatform(platform: String, destDir: String): Unit = {
    val cacheDir = s"$nativeDir/target/curl-cache"
    new File(cacheDir).mkdirs()
    val archive = curlArchiveName(platform)
    val archivePath = s"$cacheDir/$archive"

    if (!new File(archivePath).exists()) {
      val url = s"$CurlBaseUrl/$archive"
      Term.info(s"Downloading static curl for $platform...")
      val result = Proc.run("curl", List("-fSL", "-o", archivePath, url))
      if (!result.ok) {
        Term.err(s"Download failed. URL: $url")
        sys.exit(1)
      }
    }

    // Extract to temp dir
    val tmp = s"$cacheDir/extract-$platform"
    deleteDir(new File(tmp))
    new File(tmp).mkdirs()
    Proc.exec("tar", List("xf", archivePath, "-C", tmp))

    // Find the lib/ directory inside the extracted archive (e.g. curl-arm64/lib/)
    findDirRecursive(new File(tmp), "lib") match {
      case Some(libDir) =>
        // Copy all .a files to destination
        val aFiles = libDir.listFiles().filter(f => f.isFile && f.getName.endsWith(".a"))
        for (f <- aFiles) {
          java.nio.file.Files.copy(f.toPath, new File(s"$destDir/${f.getName}").toPath,
            java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        }
      case None =>
        Term.err(s"No lib/ directory found in curl archive for $platform")
    }
  }

  private def curlCrossCollect(): Unit = {
    Term.info("Downloading static curl for all desktop platforms...")
    curlDownload()
    Term.ok("Curl cross-collect complete")
  }

  private def curlCheck(): Unit = {
    val dest = s"$nativeDir/target/release"
    val curlA = new File(s"$dest/libcurl.a")
    val idn2A = new File(s"$dest/libidn2.a")
    if (curlA.exists() && idn2A.exists()) {
      Term.ok(s"Static curl present in $dest")
    } else {
      Term.err(s"Static curl NOT found in $dest")
      Term.err("Run: sge-dev native curl setup")
      sys.exit(1)
    }
  }

  private def releasePrepare(): Unit = {
    Term.info("=== Phase 1: Cross-compile Rust native libraries ===")
    crossAll()
    Term.info("=== Phase 2: Download ANGLE for all platforms ===")
    angleDownload()
    Term.info("=== Phase 3: Download static curl for all platforms ===")
    curlDownload()
    Term.ok("Release preparation complete")
  }

  private def cargoExec(args: List[String]): Unit = {
    val code = Proc.exec("cargo", args, cwd = Some(nativeDir))
    if (code != 0) sys.exit(code)
  }

  private def findDirRecursive(dir: File, name: String): Option[File] = {
    if (!dir.exists()) { None }
    else {
      val files = dir.listFiles()
      if (files == null) { None }
      else {
        files.find(f => f.isDirectory && f.getName == name).orElse(
          files.filter(_.isDirectory).flatMap(d => findDirRecursive(d, name)).headOption
        )
      }
    }
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
