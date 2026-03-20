package sgedev
package build

/** Build commands — self-contained, no Justfile dependency. */
object BuildCmd {

  /** Demo name → sbt release alias suffix (matches demos/build.sbt releaseAlias calls). */
  private val releaseNames: Map[String, String] = Map(
    "pong" -> "Pong",
    "space-shooter" -> "SpaceShooter",
    "tile-world" -> "TileWorld",
    "hex-tactics" -> "HexTactics",
    "curves" -> "Curves",
    "shader-lab" -> "ShaderLab",
    "viewer3d" -> "Viewer3d",
    "particles" -> "Particles",
    "net-chat" -> "NetChat",
    "viewports" -> "Viewports",
    "assets" -> "Assets"
  )

  /** Demo CLI name → additional archive name patterns (for findArchive matching).
    * Archives use different names depending on type:
    *   - Browser/Native: display name ("SGE Space Shooter-browser.tar.gz")
    *   - JVM: sbt project name ("sge-demo-spaceshooter-macos-aarch64.tar.gz") */
  private val archiveAliases: Map[String, List[String]] = Map(
    "space-shooter" -> List("spaceshooter"),
    "tile-world" -> List("tileworld"),
    "hex-tactics" -> List("hextactics"),
    "shader-lab" -> List("shaders", "shader lab"),
    "viewer3d" -> List("3d viewer"),
    "assets" -> List("asset showcase", "assets"),
    "net-chat" -> List("netchat")
  )

  def run(args: List[String]): Unit = {
    args match {
      case Nil | "--help" :: _ =>
        println("""Usage: sge-dev build <command>
                  |
                  |Commands:
                  |  compile [--jvm] [--js] [--native] [--all] [--errors-only] [--warnings]
                  |  compile-fmt           Compile, format, compile again
                  |  fmt                   Run scalafmt
                  |  publish-local [--jvm] [--js] [--native] [--all]
                  |  extensions [--tools] [--freetype] [--physics] [--all]
                  |  texture-pack [args]   Run TexturePacker CLI
                  |  kill-sbt              Kill running sbt server
                  |  release [--demo <name>] [--publish-first]  Build release archives
                  |  collect               Collect releases into demos/target/releases/
                  |  verify-native <demo>  Verify native release archive launches
                  |  verify-jvm <demo>     Verify JVM release archive launches
                  |  verify-jvm-intel <demo>  Verify x86_64 JVM under Rosetta
                  |  verify-browser <demo> Verify browser release archive structure
                  |  verify-releases [--demo <name>]  Run all verify-* for a demo""".stripMargin)
      case "compile" :: rest => compile(Cli.parse(rest))
      case "compile-fmt" :: _ => compileFmt()
      case "fmt" :: _ => sbt("scalafmtAll")
      case "publish-local" :: rest => publishLocal(Cli.parse(rest))
      case "extensions" :: rest => extensions(Cli.parse(rest))
      case "texture-pack" :: rest => sbt(s"sge-tools/run ${rest.mkString(" ")}")
      case "kill-sbt" :: _ => proc.ProcCmd.run(List("kill-sbt"))
      case "release" :: rest => release(Cli.parse(rest))
      case "collect" :: _ => collect()
      case "verify-native" :: rest =>
        val demo = rest.headOption.getOrElse { Term.err("Demo name required"); sys.exit(1); "" }
        verifyNative(demo)
      case "verify-jvm" :: rest =>
        val demo = rest.headOption.getOrElse { Term.err("Demo name required"); sys.exit(1); "" }
        verifyJvm(demo)
      case "verify-jvm-intel" :: rest =>
        val demo = rest.headOption.getOrElse { Term.err("Demo name required"); sys.exit(1); "" }
        verifyJvmIntel(demo)
      case "verify-browser" :: rest =>
        val demo = rest.headOption.getOrElse { Term.err("Demo name required"); sys.exit(1); "" }
        verifyBrowser(demo)
      case "verify-releases" :: rest => verifyReleases(Cli.parse(rest))
      case other :: _ =>
        Term.err(s"Unknown build command: $other")
        sys.exit(1)
    }
  }

  private def compile(args: Cli.Args): Unit = {
    val targets = resolveTargets(args)
    val errorsOnly = args.hasFlag("errors-only")
    val warnings = args.hasFlag("warnings")

    for (target <- targets) {
      val cmd = s"$target/compile"
      Term.info(s"Compiling $target...")
      if (errorsOnly) {
        val result = Proc.run("sbt", List("--client", cmd), cwd = Some(Paths.projectRoot))
        result.stdout.linesIterator.foreach { line =>
          if (line.contains("[error]")) println(line)
        }
        result.stderr.linesIterator.foreach { line =>
          if (line.contains("[error]")) println(line)
        }
        if (!result.ok) sys.exit(result.exitCode)
      } else if (warnings) {
        val result = Proc.run("sbt", List("--client", cmd), cwd = Some(Paths.projectRoot))
        result.stdout.linesIterator.foreach { line =>
          if (line.contains("[warn]") || line.contains("[error]")) println(line)
        }
        result.stderr.linesIterator.foreach { line =>
          if (line.contains("[warn]") || line.contains("[error]")) println(line)
        }
        if (!result.ok) sys.exit(result.exitCode)
      } else {
        val code = Proc.exec("sbt", List("--client", cmd), cwd = Some(Paths.projectRoot))
        if (code != 0) sys.exit(code)
      }
    }
  }

  private def compileFmt(): Unit = {
    sbt("sge/compile")
    sbt("scalafmtAll")
    sbt("sge/compile")
  }

  private def publishLocal(args: Cli.Args): Unit = {
    val targets = resolveTargets(args)
    for (target <- targets) {
      Term.info(s"Publishing $target locally...")
      val code = Proc.exec("sbt", List("--client", s"$target/publishLocal"),
                           cwd = Some(Paths.projectRoot))
      if (code != 0) sys.exit(code)
    }
  }

  private def extensions(args: Cli.Args): Unit = {
    val all = args.hasFlag("all")
    if (all || args.hasFlag("tools")) {
      Term.info("Compiling sge-tools..."); sbt("sge-tools/compile")
    }
    if (all || args.hasFlag("freetype")) {
      Term.info("Compiling sge-freetype..."); sbt("sge-freetype/compile")
    }
    if (all || args.hasFlag("physics")) {
      Term.info("Compiling sge-physics..."); sbt("sge-physics/compile")
    }
  }

  private def release(args: Cli.Args): Unit = {
    if (args.hasFlag("publish-first")) {
      Term.info("Publishing SGE locally (all platforms)...")
      publishLocal(Cli.parse(List("--all")))
    }
    val demoName = args.flag("demo")
    if (demoName.isDefined) {
      val name = demoName.get
      val alias = releaseNames.getOrElse(name, {
        Term.err(s"Unknown demo: $name. Available: ${releaseNames.keys.toList.sorted.mkString(", ")}")
        sys.exit(1); ""
      })
      Term.info(s"Building release for $name...")
      demosSbt(s"release$alias")
    } else {
      Term.info("Building all demo releases...")
      demosSbt("releaseAll")
    }
  }

  private def collect(): Unit = {
    Term.info("Collecting releases...")
    demosSbt("collectReleases")
  }

  private def verifyNative(demo: String): Unit = {
    val (plat, _) = hostPlatform()
    val releasesDir = s"${Paths.projectRoot}/demos/target/releases"
    val archive = findArchive(releasesDir, demo, s"native-$plat")
    if (archive.isEmpty) {
      Term.err(s"No native archive found for $demo ($plat)")
      sys.exit(1)
    }
    Term.info(s"Verifying native package: ${archive.get}")
    val tmpDir = java.nio.file.Files.createTempDirectory("sge-verify").toFile
    try {
      extractArchive(archive.get, tmpDir.getAbsolutePath)
      val bin = findExecutable(tmpDir)
      if (bin.isEmpty) { Term.err("No executable found in archive"); sys.exit(1) }
      println(s"  Binary: ${bin.get}")
      val result = Proc.runWithTimeout(bin.get.getAbsolutePath, timeoutSec = 5)
      val output = result.stdout + result.stderr
      if (output.toLowerCase.contains("library not found") ||
          output.contains("UnsatisfiedLinkError") ||
          output.contains("cannot open shared object")) {
        Term.err("FAIL: Missing native library")
        println(output)
        sys.exit(1)
      }
      Term.ok("PASS: Native libs loaded correctly (app exited as expected in headless env)")
    } finally {
      deleteDir(tmpDir)
    }
  }

  private def verifyJvm(demo: String): Unit = {
    val (plat, os) = hostPlatform()
    val releasesDir = s"${Paths.projectRoot}/demos/target/releases"
    val archive = findArchive(releasesDir, demo, plat, excludePattern = Some("native|browser"))
    if (archive.isEmpty) {
      Term.err(s"No JVM archive found for $demo ($plat)")
      sys.exit(1)
    }
    Term.info(s"Verifying JVM package: ${archive.get}")
    val tmpDir = java.nio.file.Files.createTempDirectory("sge-verify").toFile
    try {
      extractArchive(archive.get, tmpDir.getAbsolutePath)
      val launcher = if (os == "Darwin") {
        findFileRecursive(tmpDir, f => f.getParentFile.getName == "MacOS" && f.canExecute)
      } else {
        findExecutable(tmpDir)
      }
      if (launcher.isEmpty) { Term.err("No launcher found in archive"); sys.exit(1) }
      println(s"  Launcher: ${launcher.get}")
      val result = Proc.runWithTimeout(launcher.get.getAbsolutePath, timeoutSec = 10)
      val output = result.stdout + result.stderr
      if (output.contains("UnsatisfiedLinkError") || output.toLowerCase.contains("library not found") ||
          output.contains("cannot open shared object") || output.contains("no suitable image found")) {
        Term.err("FAIL: Missing native library")
        println(output)
        sys.exit(1)
      }
      Term.ok("PASS: JVM + native libs loaded correctly (app exited as expected in headless env)")
    } finally {
      deleteDir(tmpDir)
    }
  }

  private def verifyBrowser(demo: String): Unit = {
    val releasesDir = s"${Paths.projectRoot}/demos/target/releases"
    val archive = findArchive(releasesDir, demo, "browser")
    if (archive.isEmpty) {
      Term.err(s"No browser archive found for $demo")
      sys.exit(1)
    }
    Term.info(s"Verifying browser package: ${archive.get}")
    val tmpDir = java.nio.file.Files.createTempDirectory("sge-verify").toFile
    try {
      extractArchive(archive.get, tmpDir.getAbsolutePath)
      val indexHtml = findFileRecursive(tmpDir, f => f.getName == "index.html")
      if (indexHtml.isEmpty) { Term.err("FAIL: No index.html found"); sys.exit(1) }
      val jsBundle = findFileRecursive(tmpDir, f => f.getName.endsWith(".js") && f.length() > 1000)
      if (jsBundle.isEmpty) { Term.err("FAIL: No JS bundle found"); sys.exit(1) }
      println(s"  index.html: ${indexHtml.get}")
      println(s"  JS bundle:  ${jsBundle.get} (${jsBundle.get.length() / 1024} KB)")
      Term.ok("PASS: Browser archive has index.html + JS bundle")
    } finally {
      deleteDir(tmpDir)
    }
  }

  private def verifyReleases(args: Cli.Args): Unit = {
    val demoName = args.flag("demo")
    val demos = if (demoName.isDefined) {
      List(demoName.get)
    } else {
      releaseNames.keys.toList.sorted
    }
    for (demo <- demos) {
      println(s"\n=== Verifying $demo ===")
      verifyNative(demo)
      verifyJvm(demo)
      verifyBrowser(demo)
    }
    Term.ok("All verifications passed")
  }

  private def verifyJvmIntel(demo: String): Unit = {
    val os = System.getProperty("os.name")
    val arch = System.getProperty("os.arch")
    if (!os.contains("Mac") || (arch != "aarch64" && arch != "arm64")) {
      println("SKIP: This command is for macOS ARM only (Rosetta verification)")
    } else {
      val releasesDir = s"${Paths.projectRoot}/demos/target/releases"
      val archive = findArchive(releasesDir, demo, "macos-x86_64", excludePattern = Some("native|browser"))
      if (archive.isEmpty) {
        Term.err(s"No JVM archive found for $demo (macos-x86_64)")
        sys.exit(1)
      }
      Term.info(s"Verifying Intel JVM package under Rosetta: ${archive.get}")
      val tmpDir = java.nio.file.Files.createTempDirectory("sge-verify").toFile
      try {
        extractArchive(archive.get, tmpDir.getAbsolutePath)
        val launcher = findFileRecursive(tmpDir, f => f.getParentFile.getName == "MacOS" && f.canExecute)
        if (launcher.isEmpty) { Term.err("No launcher found"); sys.exit(1) }
        println(s"  Launcher: ${launcher.get}")
        val result = Proc.run("arch", List("-x86_64", launcher.get.getAbsolutePath))
        val output = result.stdout + result.stderr
        if (output.contains("UnsatisfiedLinkError") || output.toLowerCase.contains("library not found") ||
            output.contains("no suitable image found")) {
          Term.err("FAIL: Missing native library (x86_64 under Rosetta)")
          println(output)
          sys.exit(1)
        }
        Term.ok("PASS: Intel JVM package works under Rosetta")
      } finally {
        deleteDir(tmpDir)
      }
    }
  }

  // ── Helpers ──

  private def resolveTargets(args: Cli.Args): List[String] = {
    val all = args.hasFlag("all")
    val jvm = args.hasFlag("jvm") || (!args.hasFlag("js") && !args.hasFlag("native") && !all)
    val js = args.hasFlag("js") || all
    val native = args.hasFlag("native") || all

    val targets = scala.collection.mutable.ListBuffer.empty[String]
    if (jvm) targets += "sge"
    if (js) targets += "sgeJS"
    if (native) targets += "sgeNative"
    targets.toList
  }

  private def sbt(cmd: String): Unit = {
    val code = Proc.exec("sbt", List("--client", cmd), cwd = Some(Paths.projectRoot))
    if (code != 0) sys.exit(code)
  }

  private def demosSbt(cmd: String): Unit = {
    val code = Proc.exec("sbt", List("--client", cmd),
      cwd = Some(s"${Paths.projectRoot}/demos"))
    if (code != 0) sys.exit(code)
  }

  private def hostPlatform(): (String, String) = {
    val os = System.getProperty("os.name")
    val arch = System.getProperty("os.arch")
    val plat = (os, arch) match {
      case (o, "aarch64" | "arm64") if o.contains("Mac") => "macos-aarch64"
      case (o, _) if o.contains("Mac") => "macos-x86_64"
      case (o, "amd64") if o.contains("Linux") => "linux-x86_64"
      case (o, "aarch64") if o.contains("Linux") => "linux-aarch64"
      case _ => "unknown"
    }
    (plat, if (os.contains("Mac")) "Darwin" else if (os.contains("Linux")) "Linux" else "Unknown")
  }

  private def findArchive(dir: String, demo: String, platSuffix: String,
                          excludePattern: Option[String] = None): Option[String] = {
    val dirFile = new java.io.File(dir)
    if (!dirFile.exists()) { None }
    else {
      // Normalize: match "space-shooter", "space shooter", "spaceshooter" etc.
      val demoLower = demo.toLowerCase
      val demoSpaced = demoLower.replace("-", " ")
      val aliases = archiveAliases.getOrElse(demoLower, Nil)
      val allPatterns = (demoLower :: demoSpaced :: aliases).distinct
      val files = dirFile.listFiles().filter { f =>
        val name = f.getName.toLowerCase
        allPatterns.exists(p => name.contains(p)) &&
        name.contains(platSuffix.toLowerCase) &&
        (name.endsWith(".tar.gz") || name.endsWith(".zip")) &&
        excludePattern.forall(p => !name.matches(s".*($p).*"))
      }
      files.headOption.map(_.getAbsolutePath)
    }
  }

  private def extractArchive(archive: String, destDir: String): Unit = {
    if (archive.endsWith(".tar.gz")) {
      Proc.exec("tar", List("xzf", archive, "-C", destDir))
    } else {
      Proc.exec("unzip", List("-qo", archive, "-d", destDir))
    }
  }

  private def findExecutable(dir: java.io.File): Option[java.io.File] = {
    findFileRecursive(dir, f =>
      f.canExecute && !f.getName.endsWith(".dylib") && !f.getName.endsWith(".so") &&
      !f.getName.endsWith(".dll") && !f.getPath.contains("/runtime/"))
  }

  private def findFileRecursive(dir: java.io.File, pred: java.io.File => Boolean): Option[java.io.File] = {
    if (!dir.exists()) { None }
    else {
      val files = dir.listFiles()
      if (files == null) { None }
      else {
        files.find(f => f.isFile && pred(f)).orElse(
          files.filter(_.isDirectory).flatMap(d => findFileRecursive(d, pred)).headOption
        )
      }
    }
  }

  private def deleteDir(dir: java.io.File): Unit = {
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
