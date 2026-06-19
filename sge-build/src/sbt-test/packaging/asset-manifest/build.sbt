import sge.sbt.SgePlugin
import sge.sbt.SgePlugin.autoImport._
import sge.sbt.SgePackaging
import multiarch.sbt.JvmPackaging

// Minimal SGE game project used purely to exercise the packaging tasks shipped
// by the sge-build plugin (ISS-562 scripted test). It enables SgePlugin (so the
// SGE packaging settings — sgeGenerateAssetManifest + releasePackage — are wired
// in) but DROPS the heavy `sge` library + extension dependencies that the JVM
// platform plugin adds by default. The scripted harness only has the *plugin*
// published locally, not the full SGE library for this exact version, and the
// packaging tasks under test (asset manifest + simple release package) don't
// need the engine on the classpath to run. The single source file is a plain
// `main` with no SGE imports so the project compiles standalone.
lazy val game = (projectMatrix in file("game"))
  .enablePlugins(SgePlugin)
  .settings(
    name         := "hello-sge",
    organization := "com.example",
    // Drop the sge engine + extension deps injected by SgeDesktopJvmPlatform —
    // not published for this scripted plugin version, and not needed to run the
    // packaging tasks under test.
    libraryDependencies := libraryDependencies.value.filterNot { m =>
      m.organization == "com.kubuszok" &&
      (m.name.startsWith("sge_") || m.name.startsWith("sge-extension"))
    },
    Compile / mainClass := Some("com.example.HelloGame"),
    // Don't fork test JVM with native lib paths we never built.
    fork := false
  )
  // JVM axis: exercises releasePackage (simple-mode launcher scripts + jars).
  .jvmPlatform()
  // JS (browser) axis: the sgeGenerateAssetManifest task is wired on the browser
  // platform (SgeBrowserPlatform / SgePackaging.browserSettings). It only reads
  // Compile resources, so it runs without a fullLinkJS step (no Scala.js linking).
  .jsPlatform()

// A tiny custom task that reads the generated manifest and fails loudly if any
// expected asset line is missing. This is what the scripted `test` script drives
// (in addition to `$ exists` checks) and what the CANARY regression must trip.
lazy val checkManifest = taskKey[Unit]("Assert assets.txt contains the expected asset entries")

checkManifest := {
  val manifest = (game.js(SgePlugin.scalaVersion) / Compile / SgePackaging.sgeGenerateAssetManifest).value
  if (!manifest.exists()) sys.error(s"[check] manifest does not exist: $manifest")
  val lines = IO.readLines(manifest)
  val body  = lines.mkString("\n")
  // The manifest format is "<type>:<relpath>:<size>:<mime>". We assert on the
  // relpath fragment so the test is robust to size/mime details.
  val expected = Seq("assets/hello.txt", "assets/data/x.json", "assets/data")
  val missing  = expected.filterNot(e => lines.exists(_.contains(e)))
  if (missing.nonEmpty) {
    sys.error(
      s"[check] manifest is missing expected entries: ${missing.mkString(", ")}\n" +
        s"--- assets.txt (${lines.size} lines) ---\n$body"
    )
  }
  // hello.txt must be classified as a text asset ("t:") with a size > 0.
  val helloLine = lines.find(_.contains("assets/hello.txt")).getOrElse(
    sys.error("[check] no line for assets/hello.txt")
  )
  if (!helloLine.startsWith("t:")) sys.error(s"[check] hello.txt not classified as text: $helloLine")
  val helloSize = helloLine.split(":")(2).toLong
  if (helloSize <= 0) sys.error(s"[check] hello.txt has non-positive size: $helloLine")
  // AndroidManifest.xml and assets.txt itself must be skipped.
  if (lines.exists(_.contains("AndroidManifest.xml"))) {
    sys.error("[check] AndroidManifest.xml must be excluded from the manifest")
  }
  streams.value.log.info(s"[check] manifest OK: ${lines.size} entries")
}

// Assert the simple-mode release package produced bin/ launchers + the app jar.
lazy val checkReleasePackage = taskKey[Unit]("Assert releasePackage produced launchers + jars")

checkReleasePackage := {
  val outDir  = (game.jvm(SgePlugin.scalaVersion) / JvmPackaging.releasePackage).value
  val appName = (game.jvm(SgePlugin.scalaVersion) / JvmPackaging.releaseAppName).value
  val unix    = outDir / "bin" / appName
  val bat     = outDir / "bin" / s"$appName.bat"
  val libDir  = outDir / "lib"
  if (!outDir.isDirectory) sys.error(s"[check] release dir missing: $outDir")
  if (!unix.exists()) sys.error(s"[check] unix launcher missing: $unix")
  if (!bat.exists()) sys.error(s"[check] windows launcher missing: $bat")
  if (!libDir.isDirectory || IO.listFiles(libDir).isEmpty) {
    sys.error(s"[check] lib/ missing or empty: $libDir")
  }
  val unixBody = IO.read(unix)
  if (!unixBody.contains("com.example.HelloGame")) {
    sys.error(s"[check] unix launcher does not reference main class:\n$unixBody")
  }
  streams.value.log.info(s"[check] release package OK: $outDir")
}
