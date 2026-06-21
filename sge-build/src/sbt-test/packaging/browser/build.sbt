import sge.sbt.SgePlugin
import sge.sbt.SgePlugin.autoImport._
import sge.sbt.SgePackaging
import multiarch.sbt.JvmPackaging
import multiarch.sbt.MultiArchResourcesPlugin

// Minimal SGE game project used purely to exercise the BROWSER packaging task
// shipped by the sge-build plugin (`sgePackageBrowser`, ISS-562). It enables
// SgePlugin so the SGE browser packaging settings (SgePackaging.browserSettings:
// sgeBrowserTitle + sgePackageBrowser) are wired in on the JS axis
// (SgeBrowserPlatform triggers on SgePlugin + ScalaJSPlugin).
//
// Browser assets are EMBEDDED at compile time (multiarch-resources) instead of
// fetched via an assets.txt manifest. We wire
// MultiArchResourcesPlugin.embeddedResourcesSettings on the JS axis here (this
// mirrors what the real sge-core build does) so the scripted test can assert the
// generated, self-registering object is produced and registers the expected
// resource paths.
//
// KEY: `packageBrowserTask` reads `sgeJsOutputDir` — by default the plugin wires
// that to `Compile / fullLinkJS / scalaJSLinkerOutputDirectory`, which would run
// the full Scala.js linker. We OVERRIDE `sgeJsOutputDir` on the JS axis to a STUB
// directory containing a fake `main.js`, so `sgePackageBrowser` runs its real
// HTML/copy logic WITHOUT invoking fullLinkJS. This keeps the scripted test cheap
// while still exercising the packaging task end-to-end.
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
    // The generated embedded-resources object imports
    // multiarch.resources.EmbeddedResources, so the runtime library must be on
    // the classpath (sge-core depends on it the same way).
    libraryDependencies += "com.kubuszok" %% "multiarch-resources" % "0.3.0",
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
      // sbt 2.0 refuses to cache a File/Path task output; this stub produces a
      // directory as a side effect, so opt out of caching.
      SgePackaging.sgeJsOutputDir := Def.uncached {
        val stubDir = target.value / "stub-js"
        IO.createDirectory(stubDir)
        IO.write(stubDir / "main.js", "// fake fullLinkJS output for ISS-562 scripted test\n")
        stubDir
      }
    ) ++ MultiArchResourcesPlugin.embeddedResourcesSettings(
      objectName = "com.example.GeneratedEmbeddedResources"
    )
  )

// Custom task driven by the scripted `test` script. Reads the browser package
// output + the generated embedded-resources object and `sys.error`s if any
// expectation is missing. The CANARY assertion (assets/assets.txt ABSENT) must
// trip if the old fetch/manifest mechanism resurfaces.
lazy val checkBrowserPackage = taskKey[Unit]("Assert sgePackageBrowser emits index.html + main.js, the embedded-resources object is generated, and assets/assets.txt is ABSENT")

checkBrowserPackage := Def.uncached {
  // Force compilation so the embedded-resources sourceGenerator runs.
  val _       = (game.js(SgePlugin.scalaVersion) / Compile / compile).value
  val outDir  = (game.js(SgePlugin.scalaVersion) / SgePackaging.sgePackageBrowser).value
  val appName = (game.js(SgePlugin.scalaVersion) / JvmPackaging.releaseAppName).value
  val title   = (game.js(SgePlugin.scalaVersion) / SgePackaging.sgeBrowserTitle).value
  val srcMgd  = (game.js(SgePlugin.scalaVersion) / Compile / sourceManaged).value
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

  // ── the generated, self-registering embedded-resources object exists ────
  val genFile = srcMgd / "com" / "example" / "GeneratedEmbeddedResources.scala"
  if (!genFile.exists()) sys.error(s"[check] generated embedded-resources object missing: $genFile")
  val genBody = IO.read(genFile)
  if (!genBody.contains("object GeneratedEmbeddedResources")) {
    sys.error(s"[check] generated file does not define object GeneratedEmbeddedResources:\n$genFile")
  }
  if (!genBody.contains("EmbeddedResources.register")) {
    sys.error(s"[check] generated file does not self-register via EmbeddedResources.register:\n$genFile")
  }
  // It must register the embedded resource paths (classpath-absolute keys).
  val expectedKeys = Seq("/assets/hello.txt", "/assets/data/x.json")
  val missingKeys  = expectedKeys.filterNot(genBody.contains)
  if (missingKeys.nonEmpty) {
    sys.error(s"[check] generated object missing expected resource keys: ${missingKeys.mkString(", ")}\n$genFile")
  }

  // ── CANARY: the old assets.txt manifest mechanism must be GONE ─────────
  val assetsTxt = outDir / "assets" / "assets.txt"
  if (assetsTxt.exists()) {
    sys.error(s"[check] CANARY: assets/assets.txt must NOT exist (old fetch/manifest mechanism resurfaced): $assetsTxt")
  }
  val assetsDir = outDir / "assets"
  if (assetsDir.exists()) {
    sys.error(s"[check] CANARY: assets/ dir must NOT be emitted (assets are embedded now): $assetsDir")
  }

  log.info(s"[check] browser package OK: $outDir")
}
