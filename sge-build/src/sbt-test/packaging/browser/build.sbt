import sge.sbt.SgePlugin
import sge.sbt.SgePlugin.autoImport._
import sge.sbt.SgePackaging
import multiarch.sbt.JvmPackaging

// Minimal SGE game project used purely to exercise the BROWSER packaging task
// shipped by the sge-build plugin (`sgePackageBrowser`, ISS-562). It enables
// SgePlugin so the SGE browser packaging settings (SgePackaging.browserSettings:
// sgeBrowserTitle + sgeGenerateAssetManifest + sgePackageBrowser) are wired in on
// the JS axis (SgeBrowserPlatform triggers on SgePlugin + ScalaJSPlugin).
//
// KEY: `packageBrowserTask` reads `sgeJsOutputDir` — by default the plugin wires
// that to `Compile / fullLinkJS / scalaJSLinkerOutputDirectory`, which would run
// the full Scala.js linker. We OVERRIDE `sgeJsOutputDir` on the JS axis to a STUB
// directory containing a fake `main.js`, so `sgePackageBrowser` runs its real
// HTML/copy/manifest logic WITHOUT invoking fullLinkJS. This keeps the scripted
// test cheap while still exercising the packaging task end-to-end.
lazy val game = (projectMatrix in file("game"))
  .enablePlugins(SgePlugin)
  .settings(
    name         := "hello-sge-browser",
    organization := "com.example",
    // Drop the sge engine + extension deps injected by the SGE platform plugins —
    // not published for this scripted plugin version, and not needed to run the
    // packaging task under test (the stub bypasses any real Scala.js linking).
    libraryDependencies := libraryDependencies.value.filterNot { m =>
      m.organization == "com.kubuszok" &&
      (m.name.startsWith("sge_") || m.name.startsWith("sge-extension"))
    },
    Compile / mainClass := Some("com.example.HelloGame"),
    // A known HTML title we can assert on in checkBrowserPackage.
    SgePackaging.sgeBrowserTitle := "Hello SGE Browser",
    fork := false
  )
  // JS (browser) axis: SgeBrowserPlatform wires sgePackageBrowser here.
  // STUB OVERRIDE: replace the default sgeJsOutputDir (which depends on
  // fullLinkJS) with a task that materialises a fake fullLinkJS output dir
  // (target/stub-js/ with a fake main.js) and returns it. No Scala.js linking.
  // We call sbt-projectmatrix's `jsPlatform(scalaVersions, settings)` directly
  // (rather than the SGE implicit `jsPlatform(settings)`) because ProjectMatrix
  // already declares a `jsPlatform` member, so the implicit conversion never
  // triggers when arguments are passed. scalaVersions is set to SgePlugin's pin.
  .jsPlatform(
    scalaVersions = Seq(SgePlugin.scalaVersion),
    settings = Seq[Def.Setting[_]](
      SgePackaging.sgeJsOutputDir := {
        val stubDir = target.value / "stub-js"
        IO.createDirectory(stubDir)
        IO.write(stubDir / "main.js", "// fake fullLinkJS output for ISS-562 scripted test\n")
        stubDir
      }
    )
  )

// Custom task driven by the scripted `test` script. Reads the browser package
// output and `sys.error`s if any expectation is missing. This is what the CANARY
// regression must trip.
lazy val checkBrowserPackage = taskKey[Unit]("Assert sgePackageBrowser produced index.html, copied main.js, and assets/assets.txt")

checkBrowserPackage := {
  val outDir  = (game.js(SgePlugin.scalaVersion) / SgePackaging.sgePackageBrowser).value
  val appName = (game.js(SgePlugin.scalaVersion) / JvmPackaging.releaseAppName).value
  val title   = (game.js(SgePlugin.scalaVersion) / SgePackaging.sgeBrowserTitle).value
  val log     = streams.value.log

  if (!outDir.isDirectory) sys.error(s"[check] browser package dir missing: $outDir")

  // ── index.html exists and contains the expected fragments ──────────────
  val indexHtml = outDir / "index.html"
  if (!indexHtml.exists()) sys.error(s"[check] index.html missing: $indexHtml")
  val html = IO.read(indexHtml)
  if (!html.contains(s"<title>$title</title>")) {
    sys.error(s"[check] index.html missing title <title>$title</title>:\n$html")
  }
  if (!html.contains("""<script src="main.js"></script>""")) {
    sys.error(s"""[check] index.html missing <script src="main.js"></script> tag:\n$html""")
  }
  if (!html.contains(s"""<div id="loading">Loading $appName""")) {
    sys.error(s"""[check] index.html missing loading/app-name text for '$appName':\n$html""")
  }

  // ── main.js was copied from the (stubbed) fullLinkJS output ─────────────
  val mainJs = outDir / "main.js"
  if (!mainJs.exists()) sys.error(s"[check] main.js was not copied into the package: $mainJs")

  // ── assets/assets.txt exists and contains the asset entries ────────────
  val assetsTxt = outDir / "assets" / "assets.txt"
  if (!assetsTxt.exists()) sys.error(s"[check] assets/assets.txt missing: $assetsTxt")
  val manifestLines = IO.readLines(assetsTxt)
  val expectedAssets = Seq("assets/hello.txt", "assets/data/x.json")
  val missing = expectedAssets.filterNot(e => manifestLines.exists(_.contains(e)))
  if (missing.nonEmpty) {
    sys.error(
      s"[check] assets.txt missing expected entries: ${missing.mkString(", ")}\n" +
        s"--- assets.txt (${manifestLines.size} lines) ---\n${manifestLines.mkString("\n")}"
    )
  }

  log.info(s"[check] browser package OK: $outDir")
}
