package sgedev
package setup

import java.io.File

/** Idempotent development environment setup.
  *
  * Ensures all tools, targets, and dependencies are installed for building
  * SGE on all 9 targets (6 desktop + 3 Android). Safe to run multiple times —
  * skips anything already present.
  *
  * Usage: sge-dev setup [--ci]
  *   --ci    CI mode: skip interactive tools (sdkman, Homebrew prompts)
  */
object SetupCmd {

  private val desktopTargets = List(
    "x86_64-apple-darwin", "aarch64-apple-darwin",
    "x86_64-unknown-linux-gnu", "aarch64-unknown-linux-gnu",
    "x86_64-pc-windows-msvc", "aarch64-pc-windows-msvc"
  )

  private val androidTargets = List(
    "aarch64-linux-android", "armv7-linux-androideabi", "x86_64-linux-android"
  )

  def run(args: List[String]): Unit = {
    val isCI = args.contains("--ci") || sys.env.get("CI").contains("true")

    println("╔══════════════════════════════════════════════════╗")
    println("║  SGE Development Environment Setup              ║")
    println("╚══════════════════════════════════════════════════╝")
    println()

    step("1/6", "Git submodules", () => setupSubmodules())
    step("2/6", "JDK (Zulu 25 via sdkman)", () => setupJdk(isCI))
    step("3/6", "Android NDK", () => setupAndroidNdk(isCI))
    // Steps 4-6 are for native cross-compilation (now in sge-native-components repo).
    // Kept for developers who clone sge-native-components alongside sge.
    step("4/6", "Rust targets (sge-native-components)", () => setupRustTargets())
    step("5/6", "Zig (sge-native-components)", () => setupZig(isCI))
    step("6/6", "cargo-zigbuild + cargo-xwin (sge-native-components)", () => setupCargoTools())

    println()
    println("╔══════════════════════════════════════════════════╗")
    println("║  Setup complete!                                ║")
    println("╚══════════════════════════════════════════════════╝")
    println()
    println("  Build all native targets:  sge-dev native cross-all")
    println("  Build Android targets:     sge-dev native cross-android")
    println("  Run tests:                 sge-dev test unit --jvm")
  }

  private def step(num: String, name: String, action: () => Unit): Unit = {
    println(s"── [$num] $name ──")
    try {
      action()
    } catch {
      case e: Exception =>
        Term.err(s"  Failed: ${e.getMessage}")
    }
    println()
  }

  // ── 1. Git submodules (original-src/ reference repos) ──────────────
  // Note: miniaudio/glfw submodules moved to the external sge-native-components repo.

  private def setupSubmodules(): Unit = {
    // Check if any original-src submodule needs initialization
    val submodules = List(
      s"${Paths.projectRoot}/original-src/libgdx"
    )
    val needsInit = submodules.exists { dir =>
      val d = new File(dir)
      !d.exists() || d.listFiles() == null || d.listFiles().length == 0
    }
    if (needsInit) {
      Term.info("Initializing git submodules...")
      val code = Proc.exec("git", List("submodule", "update", "--init", "--recursive"),
        cwd = Some(Paths.projectRoot))
      if (code != 0) Term.err("  Failed to initialize submodules") else Term.ok("  Submodules initialized")
    } else {
      Term.ok("  Submodules already initialized")
    }
  }

  // ── 2. Rust targets ────────────────────────────────────────────────

  private def setupRustTargets(): Unit = {
    // Check which targets are already installed
    val result = Proc.run("rustup", List("target", "list", "--installed"))
    val installed = if (result.ok) result.stdout.linesIterator.toSet else Set.empty[String]

    val allTargets = desktopTargets ++ androidTargets
    val missing = allTargets.filterNot(installed.contains)

    if (missing.isEmpty) {
      Term.ok(s"  All ${allTargets.size} Rust targets installed")
    } else {
      Term.info(s"  Installing ${missing.size} Rust targets...")
      for (target <- missing) {
        Proc.exec("rustup", List("target", "add", target))
      }
      Term.ok(s"  Installed: ${missing.mkString(", ")}")
    }
  }

  // ── 3. Zig ─────────────────────────────────────────────────────────

  private def setupZig(isCI: Boolean): Unit = {
    val hasZig = Proc.run("zig", List("version")).ok
    if (hasZig) {
      val version = Proc.run("zig", List("version")).stdout.trim
      Term.ok(s"  Zig $version already installed")
    } else if (isCI) {
      Term.info("  Zig not found — in CI, use 'mlugg/setup-zig@v2' action")
    } else {
      val hasBrew = Proc.run("brew", List("--version")).ok
      if (hasBrew) {
        Term.info("  Installing Zig via Homebrew...")
        Proc.exec("brew", List("install", "zig"))
      } else {
        Term.warn("  Zig not found. Install from https://ziglang.org/download/")
      }
    }
  }

  // ── 4. cargo-zigbuild + cargo-xwin ─────────────────────────────────

  private def setupCargoTools(): Unit = {
    val tools = List("cargo-zigbuild" -> "zigbuild", "cargo-xwin" -> "xwin")
    for ((pkg, subcommand) <- tools) {
      val check = Proc.run("cargo", List(subcommand, "--version"))
      if (check.ok) {
        Term.ok(s"  $pkg already installed")
      } else {
        Term.info(s"  Installing $pkg...")
        Proc.exec("cargo", List("install", pkg, "--locked"))
      }
    }
  }

  // ── 5. LLVM tools (llvm-lib for Windows cross-compilation) ──────────

  private def setupLlvm(isCI: Boolean): Unit = {
    val hasLlvmLib = Proc.run("llvm-lib", List("--version")).ok ||
      Proc.run("sh", List("-c", "command -v llvm-lib")).ok
    if (hasLlvmLib) {
      Term.ok("  llvm-lib already available")
    } else {
      // Check if LLVM is installed via Homebrew but not on PATH
      val brewLlvmBin = "/opt/homebrew/opt/llvm/bin"
      val brewLlvmLib = new File(s"$brewLlvmBin/llvm-lib")
      if (brewLlvmLib.exists()) {
        Term.ok(s"  llvm-lib found at $brewLlvmBin (add to PATH)")
        Term.info(s"  Add to shell: export PATH=\"$brewLlvmBin:$$PATH\"")
      } else if (isCI) {
        Term.info("  Installing LLVM via Homebrew...")
        Proc.exec("brew", List("install", "llvm"))
      } else {
        val hasBrew = Proc.run("brew", List("--version")).ok
        if (hasBrew) {
          Term.info("  Installing LLVM via Homebrew (needed for llvm-lib in Windows cross-compilation)...")
          Proc.exec("brew", List("install", "llvm"))
          Term.info(s"  Add to shell: export PATH=\"$brewLlvmBin:$$PATH\"")
        } else {
          Term.warn("  llvm-lib not found. Install LLVM: brew install llvm")
        }
      }
    }
  }

  // X11 headers and LLVM tools are now handled by sge-native-components repo setup.

  // ── 6. Android NDK ─────────────────────────────────────────────────

  private def setupAndroidNdk(isCI: Boolean): Unit = {
    // Check env vars first, then local SDK dir
    val ndkHome = sys.env.get("ANDROID_NDK_HOME").orElse(
      sys.env.get("ANDROID_HOME").flatMap { home =>
        val ndkDir = new File(s"$home/ndk")
        if (ndkDir.exists()) {
          ndkDir.listFiles().filter(_.isDirectory).map(_.getAbsolutePath).sorted.lastOption
        } else None
      }
    ).orElse {
      val localNdk = new File(s"${Paths.projectRoot}/sge-deps/android-sdk/ndk/27.2.12479018")
      if (localNdk.exists()) Some(localNdk.getAbsolutePath) else None
    }

    if (ndkHome.isDefined) {
      Term.ok(s"  Android NDK found: ${ndkHome.get}")
    } else if (isCI) {
      Term.info("  Android NDK not found — in CI, use 'android-actions/setup-android' + sdkmanager")
    } else {
      Term.info("  Android NDK not found. Run 'sge-dev test android setup' to install.")
    }
  }

  // ── 7. JDK ─────────────────────────────────────────────────────────

  private def setupJdk(isCI: Boolean): Unit = {
    val javaVersion = Proc.run("java", List("-version"))
    if (javaVersion.ok) {
      // java -version outputs to stderr
      val versionStr = (javaVersion.stderr + javaVersion.stdout).trim
      val firstLine = versionStr.linesIterator.toList.headOption.getOrElse("")
      Term.ok(s"  JDK found: $firstLine")
      // Check if it's >= 22 (Panama FFM)
      val versionNum = firstLine.replaceAll("[^0-9.]", "").split("\\.").headOption.flatMap(_.toIntOption)
      versionNum match {
        case Some(v) if v >= 22 => Term.ok(s"  JDK $v has Panama FFM support")
        case Some(v) => Term.warn(s"  JDK $v detected — need JDK 22+ for Panama FFM (java.lang.foreign)")
        case None => ()
      }
    } else if (isCI) {
      Term.info("  JDK not found — in CI, use 'actions/setup-java' with Zulu 25")
    } else {
      Term.info("  JDK not found. Install via: sdk install java 25.0.2-zulu")
    }
  }
}
