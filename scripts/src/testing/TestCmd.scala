package sgedev
package testing

/** Test orchestration commands — self-contained, no Justfile dependency. */
object TestCmd {

  def run(args: List[String]): Unit = {
    args match {
      case Nil | "--help" :: _ =>
        println("""Usage: sge-dev test <command>
                  |
                  |Commands:
                  |  unit [--jvm] [--js] [--native] [--all] [--only SUITE]
                  |  integration [--desktop] [--browser] [--native-ffi] [--android] [--all]
                  |  regression [--jvm] [--js] [--native] [--android] [--all]
                  |  browser        Run Playwright browser tests
                  |  android {setup,start,stop,ensure,install,test,demo,...} [args]
                  |  extensions [--tools] [--freetype] [--physics] [--all]
                  |  verify         Full 4-platform verification gate""".stripMargin)
      case "unit" :: rest => unit(Cli.parse(rest))
      case "integration" :: rest => integration(Cli.parse(rest))
      case "regression" :: rest => regression(Cli.parse(rest))
      case "browser" :: _ => browser()
      case "android" :: rest => android(rest)
      case "extensions" :: rest => extensions(Cli.parse(rest))
      case "verify" :: _ => verify()
      case other :: _ =>
        Term.err(s"Unknown test command: $other")
        sys.exit(1)
    }
  }

  private def unit(args: Cli.Args): Unit = {
    val all = args.hasFlag("all")
    val runJvm = args.hasFlag("jvm") || (!args.hasFlag("js") && !args.hasFlag("native") && !all)
    val runJs = args.hasFlag("js") || all
    val runNative = args.hasFlag("native") || all
    val only = args.flag("only")

    only match {
      case Some(suite) =>
        sbt(s"sge/testOnly *$suite")
      case None =>
        if (runJvm) { Term.info("Running JVM unit tests..."); sbt("sge/test") }
        if (runJs) { Term.info("Running JS unit tests..."); sbt("sgeJS/test") }
        if (runNative) {
          Term.info("Running Native unit tests...")
          sgedev.native.NativeCmd.run(List("build", "--static"))
          sbt("sgeNative/test")
        }
    }
  }

  private def integration(args: Cli.Args): Unit = {
    val all = args.hasFlag("all")
    if (all || args.hasFlag("desktop")) {
      Term.info("Running desktop integration tests...")
      sgedev.native.NativeCmd.run(List("build"))
      sgedev.native.NativeCmd.run(List("angle", "setup"))
      sbt("sge-it-desktop/test")
    }
    if (all || args.hasFlag("browser")) {
      Term.info("Running browser integration tests...")
      sbt("demoJS/fastLinkJS")
      sbt("sge-it-browser/test")
    }
    if (all || args.hasFlag("native-ffi")) {
      Term.info("Running native FFI integration tests...")
      sgedev.native.NativeCmd.run(List("build", "--static"))
      sgedev.native.NativeCmd.run(List("angle", "setup"))
      sbt("sge-it-native-ffi/run")
    }
    if (all || args.hasFlag("android")) {
      Term.info("Running Android integration tests...")
      android(List("ensure"))
      sbt("sge-android-smoke/androidSign")
      sbt("sge-it-android/test")
    }
  }

  private def regression(args: Cli.Args): Unit = {
    val all = args.hasFlag("all")
    val jvm = args.hasFlag("jvm") || (!args.hasFlag("js") && !args.hasFlag("native") && !args.hasFlag("android") && !all)
    val js = args.hasFlag("js") || all
    val nat = args.hasFlag("native") || all
    val and = args.hasFlag("android") || all

    if (jvm) {
      Term.info("=== Regression test: JVM ===")
      sgedev.native.NativeCmd.run(List("build"))
      sgedev.native.NativeCmd.run(List("angle", "setup"))
      val result = Proc.run("sbt", List("--client", "regressionTest/run"), cwd = Some(Paths.projectRoot))
      println(result.stdout)
      if (result.stderr.nonEmpty) System.err.println(result.stderr)
      if (!result.stdout.contains("SMOKE_TEST_PASSED")) {
        Term.err("JVM regression test FAILED")
        sys.exit(1)
      }
      Term.ok("JVM regression test PASSED")
    }
    if (js) {
      Term.info("=== Regression test: JS (browser) ===")
      sbt("regressionTestJS/fastLinkJS")
      sbt("sge-it-browser/testOnly sge.browser.BrowserBootstrapTest")
      Term.ok("JS regression test PASSED")
    }
    if (nat) {
      Term.info("=== Regression test: Native ===")
      sgedev.native.NativeCmd.run(List("build", "--static"))
      sgedev.native.NativeCmd.run(List("angle", "setup"))
      val result = Proc.run("sbt", List("--client", "regressionTestNative/run"), cwd = Some(Paths.projectRoot))
      println(result.stdout)
      if (result.stderr.nonEmpty) System.err.println(result.stderr)
      if (!result.stdout.contains("SMOKE_TEST_PASSED")) {
        Term.err("Native regression test FAILED")
        sys.exit(1)
      }
      Term.ok("Native regression test PASSED")
    }
    if (and) {
      Term.info("=== Regression test: Android ===")
      android(List("ensure"))
      sbt("sge-android-smoke/androidSign")
      sbt("sge-it-android/test")
      Term.ok("Android regression test PASSED")
    }
  }

  private def browser(): Unit = {
    sbt("sge-it-browser/test")
  }

  private def extensions(args: Cli.Args): Unit = {
    val all = args.hasFlag("all")
    if (all || args.hasFlag("tools")) {
      Term.info("Testing sge-tools..."); sbt("sge-tools/test")
    }
    if (all || args.hasFlag("freetype")) {
      Term.info("Testing sge-freetype..."); sbt("sge-freetype/test")
    }
    if (all || args.hasFlag("physics")) {
      Term.info("Testing sge-physics..."); sbt("sge-physics/test")
    }
  }

  private def android(args: List[String]): Unit = {
    args match {
      case Nil => println("""Usage: sge-dev test android <command>
                            |
                            |Commands:
                            |  setup              Download Android SDK + emulator (one-time)
                            |  sdk-install <pkg>   Install an SDK package
                            |  avd-create <name> <image>  Create an AVD
                            |  start [--gui]       Start emulator (default: sge-test-36)
                            |  stop                Stop emulator
                            |  ensure [--gui]      Start emulator if not running
                            |  install <apk>       Install APK on device
                            |  uninstall <pkg>     Uninstall package
                            |  launch <activity>   Launch an activity
                            |  logcat [--lines N] [--errors]  Show logcat
                            |  logcat-clear        Clear logcat buffer
                            |  adb <args...>       Run raw adb command
                            |  pidof <pkg>         Check if Android process is running
                            |  test <demo>         Full test cycle on collected APK
                            |  test-all            Test all collected APKs
                            |  demo <name> [--gui] Build + test a demo on emulator
                            |  build-smoke         Build smoke test APK""".stripMargin)
      case "setup" :: _ => androidSdkSetup()
      case "sdk-install" :: rest =>
        val pkg = rest.headOption.getOrElse { Term.err("Package required"); sys.exit(1); "" }
        androidSdkInstall(pkg)
      case "avd-create" :: rest =>
        if (rest.length < 2) { Term.err("Usage: avd-create <name> <image>"); sys.exit(1) }
        androidAvdCreate(rest(0), rest(1))
      case "start" :: rest =>
        val gui = rest.contains("--gui")
        androidEmulatorStart(gui = gui)
      case "stop" :: _ => androidEmulatorStop()
      case "ensure" :: rest =>
        val gui = rest.contains("--gui")
        androidEnsureEmulator(gui = gui)
      case "install" :: rest =>
        val apk = rest.headOption.getOrElse { Term.err("APK path required"); sys.exit(1); "" }
        adb(List("install", "-r", apk))
      case "uninstall" :: rest =>
        val pkg = rest.headOption.getOrElse { Term.err("Package required"); sys.exit(1); "" }
        adb(List("uninstall", pkg))
      case "launch" :: rest =>
        val activity = rest.headOption.getOrElse { Term.err("Activity required"); sys.exit(1); "" }
        adb(List("shell", "am", "start", "-n", activity))
      case "logcat" :: rest =>
        val parsed = Cli.parse(rest)
        val lines = parsed.flagOrDefault("lines", "200")
        if (parsed.hasFlag("errors")) {
          val result = adbRun(List("logcat", "-d"))
          if (result.ok) {
            result.stdout.linesIterator.filter { line =>
              line.contains("FATAL") || line.contains("AndroidRuntime") ||
              line.contains("Exception") || line.contains("Error")
            }.filterNot { line =>
              line.contains("InputManager") || line.contains("FileUtils") ||
              line.contains("TapAndPay") || line.contains("gclu")
            }.foreach(println)
          }
        } else {
          adb(List("logcat", "-d", "-t", lines))
        }
      case "logcat-clear" :: _ =>
        adb(List("logcat", "-c"))
        println("Logcat cleared.")
      case "adb" :: rest => adb(rest)
      case "pidof" :: rest =>
        val pkg = rest.headOption.getOrElse { Term.err("Package required"); sys.exit(1); "" }
        val result = adbRun(List("shell", "pidof", pkg))
        if (result.ok && result.stdout.trim.nonEmpty) {
          println(s"$pkg is running (PID: ${result.stdout.trim})")
        } else {
          println(s"$pkg is NOT running")
        }
      case "test" :: rest =>
        if (rest.isEmpty) {
          androidTestAllCollected()
        } else {
          androidTestCollected(rest.head, gui = rest.contains("--gui"))
        }
      case "test-all" :: rest =>
        androidTestAllCollected(gui = rest.contains("--gui"))
      case "demo" :: rest =>
        val demo = rest.headOption.getOrElse { Term.err("Demo name required"); sys.exit(1); "" }
        val gui = rest.contains("--gui")
        androidDemo(demo, gui)
      case "build-smoke" :: _ => sbt("sge-android-smoke/androidSign")
      case other :: _ =>
        Term.err(s"Unknown android command: $other")
        sys.exit(1)
    }
  }

  private def verify(): Unit = {
    Term.info("=== Phase 1: Unit tests (JVM + JS) ===")
    sbt("sge/test")
    sbt("sgeJS/test")
    println()
    Term.info("=== Phase 2: Regression tests (JVM + JS) ===")
    regression(Cli.Args(Map("jvm" -> "true"), Nil))
    regression(Cli.Args(Map("js" -> "true"), Nil))
    println()
    Term.info("=== Phase 3: Unit tests + regression (Native — static lib) ===")
    sgedev.native.NativeCmd.run(List("build", "--static"))
    sbt("sgeNative/test")
    regression(Cli.Args(Map("native" -> "true"), Nil))
    println()
    Term.info("=== Phase 4: Regression test (Android — emulator) ===")
    regression(Cli.Args(Map("android" -> "true"), Nil))
    println()
    Term.ok("============================================")
    Term.ok("  VERIFICATION PASSED — all platforms green")
    Term.ok("============================================")
  }

  // ── Android helpers ──────────────────────────────────────────

  private def sdkRoot(): String = {
    sys.env.getOrElse("ANDROID_HOME",
      sys.env.getOrElse("ANDROID_SDK_ROOT", s"${Paths.projectRoot}/android-sdk"))
  }

  private def adbPath(): String = s"${sdkRoot()}/platform-tools/adb"

  private def adb(args: List[String]): Unit = {
    val code = Proc.exec(adbPath(), args, cwd = Some(Paths.projectRoot))
    if (code != 0) sys.exit(code)
  }

  private def adbRun(args: List[String]): Proc.Result = {
    Proc.run(adbPath(), args, cwd = Some(Paths.projectRoot))
  }

  private def isEmulatorRunning: Boolean = {
    val result = adbRun(List("devices"))
    result.ok && result.stdout.contains("emulator-")
  }

  private def androidSdkSetup(): Unit = {
    sbt("sge-android-smoke/androidSdkRoot")
    val sdk = sdkRoot()
    val sdkmanager = s"$sdk/cmdline-tools/latest/bin/sdkmanager"
    Term.info("Installing emulator + system image...")
    Proc.exec(sdkmanager, List("--sdk_root=" + sdk, "emulator", "platform-tools",
      "system-images;android-35;google_apis;arm64-v8a"), cwd = Some(Paths.projectRoot),
      env = Map("JAVA_HOME" -> sys.env.getOrElse("JAVA_HOME", "")))
    val avdmanager = s"$sdk/cmdline-tools/latest/bin/avdmanager"
    Term.info("Creating AVD sge-test-avd...")
    Proc.exec(avdmanager, List("create", "avd", "--name", "sge-test-avd",
      "--package", "system-images;android-35;google_apis;arm64-v8a", "--force"),
      cwd = Some(Paths.projectRoot))
    Term.ok("Android SDK setup complete.")
  }

  private def androidSdkInstall(pkg: String): Unit = {
    val sdk = sdkRoot()
    val sdkmanager = s"$sdk/cmdline-tools/latest/bin/sdkmanager"
    Proc.exec(sdkmanager, List("--sdk_root=" + sdk, pkg), cwd = Some(Paths.projectRoot))
  }

  private def androidAvdCreate(name: String, image: String): Unit = {
    val sdk = sdkRoot()
    val avdmanager = s"$sdk/cmdline-tools/latest/bin/avdmanager"
    Proc.exec(avdmanager, List("create", "avd", "--name", name, "--package", image, "--force"),
      cwd = Some(Paths.projectRoot))
    Term.ok(s"AVD $name created.")
  }

  private def androidEmulatorStart(avd: String = "sge-test-36", gui: Boolean = false): Unit = {
    if (isEmulatorRunning) {
      println("Emulator already running.")
    } else {
      val sdk = sdkRoot()
      val emulator = s"$sdk/emulator/emulator"
      val windowFlag = if (gui) Nil else List("-no-window")
      Term.info(s"Starting emulator (AVD: $avd, gui: $gui)...")
      // Start in background
      val cmd = List(emulator, "-avd", avd) ++ windowFlag ++
        List("-gpu", "swiftshader_indirect", "-no-snapshot", "-no-boot-anim")
      val pb = new java.lang.ProcessBuilder(cmd*)
      pb.directory(new java.io.File(Paths.projectRoot))
      pb.redirectErrorStream(true)
      pb.start() // Fire and forget
      println("Waiting for device...")
      Proc.exec(adbPath(), List("wait-for-device"))
      // Wait for boot
      var booted = false
      while (!booted) {
        val result = adbRun(List("shell", "getprop", "sys.boot_completed"))
        if (result.ok && result.stdout.trim == "1") {
          booted = true
        } else {
          Thread.sleep(2000)
        }
      }
      Term.ok("Emulator booted.")
    }
  }

  private def androidEmulatorStop(): Unit = {
    adbRun(List("emu", "kill"))
    println("Emulator stopped.")
  }

  private def androidEnsureEmulator(gui: Boolean = false): Unit = {
    if (isEmulatorRunning) {
      println("Emulator already running.")
    } else {
      androidEmulatorStart(gui = gui)
    }
  }

  private val demoMap: Map[String, (String, String, String)] = Map(
    "pong" -> ("androidPong", "pong", "sge.demos.pong"),
    "space-shooter" -> ("androidSpaceShooter", "space-shooter", "sge.demos.spaceshooter"),
    "tile-world" -> ("androidTileWorld", "tile-world", "sge.demos.tileworld"),
    "hex-tactics" -> ("androidHexTactics", "hex-tactics", "sge.demos.hextactics"),
    "curve-playground" -> ("androidCurves", "curve-playground", "sge.demos.curves"),
    "shader-lab" -> ("androidShaderLab", "shader-lab", "sge.demos.shaders"),
    "viewer-3d" -> ("androidViewer3d", "viewer-3d", "sge.demos.viewer3d"),
    "particle-show" -> ("androidParticles", "particle-show", "sge.demos.particles"),
    "net-chat" -> ("androidNetChat", "net-chat", "sge.demos.netchat"),
    "viewport-gallery" -> ("androidViewports", "viewport-gallery", "sge.demos.viewports"),
    "asset-showcase" -> ("androidAssets", "asset-showcase", "sge.demos.assets")
  )

  private def androidFullTest(apk: String, activity: String, pkg: String): Boolean = {
    println(s"=== Uninstalling $pkg ===")
    adbRun(List("uninstall", pkg))
    println(s"=== Installing $apk ===")
    adb(List("install", "-r", apk))
    println("=== Clearing logcat ===")
    adbRun(List("logcat", "-c"))
    println(s"=== Launching $activity ===")
    adb(List("shell", "am", "start", "-n", activity))
    println("=== Waiting 5s for startup ===")
    Thread.sleep(5000)
    println("=== Process status ===")
    val pidResult = adbRun(List("shell", "pidof", pkg))
    if (pidResult.ok && pidResult.stdout.trim.nonEmpty) {
      println(s"$pkg is running (PID: ${pidResult.stdout.trim})")
      true
    } else {
      println(s"$pkg crashed!")
      println("=== Crash log ===")
      val logResult = adbRun(List("logcat", "-d"))
      if (logResult.ok) {
        logResult.stdout.linesIterator.filter(l =>
          l.contains("FATAL") || l.contains("AndroidRuntime")).take(30).foreach(println)
      }
      false
    }
  }

  private def androidTestCollected(demo: String, gui: Boolean = false): Unit = {
    androidEnsureEmulator(gui = gui)
    val (_, dir, pkg) = demoMap.getOrElse(demo, {
      Term.err(s"Unknown demo: $demo. Available: ${demoMap.keys.toList.sorted.mkString(", ")}")
      sys.exit(1)
      ("", "", "")
    })
    val apk = s"${Paths.projectRoot}/demos/target/releases/$dir.apk"
    if (!new java.io.File(apk).exists()) {
      Term.err(s"$apk not found. Run 'cd demos && sbt --client releaseAll; sbt --client collectReleases' first.")
      sys.exit(1)
    }
    val activity = s"$pkg/.AndroidMain"
    println(s"=== Testing collected APK: $apk ===")
    if (!androidFullTest(apk, activity, pkg)) sys.exit(1)
  }

  private def androidTestAllCollected(gui: Boolean = false): Unit = {
    androidEnsureEmulator(gui = gui)
    var passed = 0
    var failed = 0
    val demos = List("pong", "space-shooter", "tile-world", "hex-tactics", "curve-playground",
      "shader-lab", "viewer-3d", "particle-show", "viewport-gallery", "asset-showcase")
    for (demo <- demos) {
      println()
      println("=" * 40)
      println(s"  Testing: $demo")
      println("=" * 40)
      val (_, dir, pkg) = demoMap(demo)
      val apk = s"${Paths.projectRoot}/demos/target/releases/$dir.apk"
      if (!new java.io.File(apk).exists()) {
        Term.warn(s"$apk not found, skipping")
        failed += 1
      } else {
        val activity = s"$pkg/.AndroidMain"
        if (androidFullTest(apk, activity, pkg)) passed += 1 else failed += 1
      }
    }
    println()
    println(s"=== Results: $passed passed, $failed failed ===")
    if (failed > 0) sys.exit(1)
  }

  private def androidDemo(demo: String, gui: Boolean): Unit = {
    androidEnsureEmulator(gui = gui)
    val (sbtAlias, dir, pkg) = demoMap.getOrElse(demo, {
      Term.err(s"Unknown demo: $demo. Available: ${demoMap.keys.toList.sorted.mkString(", ")}")
      sys.exit(1)
      ("", "", "")
    })
    val apk = s"demos/$dir/target/jvm-3/android/app-debug.apk"
    val activity = s"$pkg/.AndroidMain"
    Term.info(s"Building Android APK: $sbtAlias")
    val code = Proc.exec("sbt", List("--client", sbtAlias),
      cwd = Some(s"${Paths.projectRoot}/demos"))
    if (code != 0) sys.exit(code)
    Term.info("Installing and testing...")
    androidFullTest(s"${Paths.projectRoot}/$apk", activity, pkg)
  }

  private def sbt(cmd: String): Unit = {
    val code = Proc.exec("sbt", List("--client", cmd), cwd = Some(Paths.projectRoot))
    if (code != 0) {
      Term.err(s"Failed: sbt --client '$cmd'")
      sys.exit(code)
    }
  }
}
