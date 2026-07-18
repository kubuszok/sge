// SGE — Browser integration test: SGE bootstrap + subsystem self-report
//
// Uses Playwright (JVM) to load the compiled Scala.js *regression* app in a real
// headless Chromium browser and asserts on SGE-OBSERVABLE behavior:
//
//   - the app boots with no un-allow-listed console errors (ISS-726 c2)
//   - SGE's OWN subsystems self-report healthy via the structured
//     `SGE-IT:<NAME>:<STATUS>:<detail>` console protocol the RegressionApp emits
//     (GL20, viewport, audio, files, input, Pixmap/Texture, ShaderProgram
//     compile+link, ModelBatch, input polling) — these can only PASS if SGE's
//     browser backend actually ran, so they fail on an SGE regression (ISS-726 c3)
//   - SGE rendered to the canvas IT created (800x600), not the blank harness
//     canvas — the exact-dimension + non-blank check replaces a rotted
//     `dataUrl.length > 300` floor that a blank 100x100 canvas already cleared
//     (ISS-726 c1)
//
// History (ISS-726 c3): the earlier revision of this suite ran a battery of
// PROXY tests that exercised plain Chromium APIs (`canvas.getContext('webgl2')`,
// `gl.compileShader`, `JSON.parse`, `new AudioContext()`, `localStorage`, raw
// `mousedown`/`keydown`/`TouchEvent` dispatch, `fetch()` of a file the test
// itself wrote) and therefore could NOT fail on any SGE regression. Each has been
// replaced by an SGE-observable assertion here, or deleted with its coverage
// cited — see the deleted-proxy inventory at the bottom of this file.
//
// Prerequisites:
//   1. Build the regression JS: sbt --client 'regressionTestJS/fastLinkJS'
//   2. Install Chromium for Playwright: npx playwright@1.49.0 install chromium
//
// Run: re-scale runner browser-it  (or  sbt --client 'sge-it-browser/test')

package sge.browser

import com.microsoft.playwright._
import com.microsoft.playwright.options.LoadState
import munit.FunSuite

import java.net.InetSocketAddress
import com.sun.net.httpserver.HttpServer
import java.nio.file.{ Files, Path, Paths }
import scala.collection.mutable

class BrowserBootstrapTest extends FunSuite {

  // Playwright browser launch + page load + the regression app's multi-scene run
  // (each scene ~3s, six scenes) needs more than the default 30s.
  override val munitTimeout: scala.concurrent.duration.Duration =
    scala.concurrent.duration.Duration(90, "s")

  /** Locate the fastLinkJS output directory for the demo module by walking the regression module's target tree for the `main.js` the linker emits. The exact path (the cross-version segment `js-3` vs
    * `scala-3.x`, and the `<module>-fastopt` dir name) varies by sbt/Scala.js toolchain version, so don't hard-code it.
    */
  private def findDemoJsDir(): Path = {
    val cwd = Paths.get(System.getProperty("user.dir"))
    // sbt-2.0 uses an out-of-tree layout: the regression app's fastLinkJS output is
    // <root>/target/out/sjs1/<scala>/sge-test-regression/sge-test-regression-fastopt/main.js
    // (NOT sge-test/regression/target/...). Locate the app `-fastopt` dir (not the
    // `-test-fastopt` one) by name anywhere under target.
    val root = cwd.resolve("target")
    findFastoptDir(root, "sge-test-regression").getOrElse {
      fail(
        "Demo JS output (sge-test-regression-fastopt/main.js) not found under " + root + ". " +
          "Run 'sbt regressionTestJS/fastLinkJS' first. (cwd=" + cwd + ")"
      )
    }
  }

  /** The `<artifact>-fastopt` directory (the application linker output, NOT the `<artifact>-test-fastopt` one) that contains `main.js`, anywhere under `root`.
    */
  private def findFastoptDir(root: Path, artifact: String): Option[Path] =
    if (!Files.isDirectory(root)) None
    else {
      val stream = Files.walk(root)
      try {
        val found = stream
          .filter(p =>
            Files.isRegularFile(p) && p.getFileName.toString == "main.js" &&
              p.getParent.getFileName.toString == s"$artifact-fastopt"
          )
          .findFirst()
        if (found.isPresent) Some(found.get.getParent) else None
      } finally stream.close()
    }

  /** Start a simple HTTP server serving files from the given directories (first match wins). */
  private def startServer(rootDir: Path, extraRoots: Path*): (HttpServer, Int) = {
    val allRoots = rootDir +: extraRoots
    val server   = HttpServer.create(new InetSocketAddress(0), 0)
    server.createContext(
      "/",
      exchange => {
        val requestPath = exchange.getRequestURI.getPath.stripPrefix("/")
        val filePath    = if (requestPath.isEmpty) "index.html" else requestPath
        val fullPath    = allRoots.map(_.resolve(filePath)).find(p => Files.exists(p) && !Files.isDirectory(p))

        fullPath match {
          case Some(resolved) =>
            val bytes       = Files.readAllBytes(resolved)
            val contentType =
              if (filePath.endsWith(".js")) "application/javascript"
              else if (filePath.endsWith(".html")) "text/html"
              else if (filePath.endsWith(".png")) "image/png"
              else if (filePath.endsWith(".txt")) "text/plain"
              else "application/octet-stream"
            exchange.getResponseHeaders.set("Content-Type", contentType)
            exchange.sendResponseHeaders(200, bytes.length)
            exchange.getResponseBody.write(bytes)
            exchange.getResponseBody.close()
          case None =>
            val msg = s"404 Not Found: $filePath"
            exchange.sendResponseHeaders(404, msg.length)
            exchange.getResponseBody.write(msg.getBytes)
            exchange.getResponseBody.close()
        }
      }
    )
    server.start()
    val port = server.getAddress.getPort
    (server, port)
  }

  /** Create a minimal HTML page that loads the demo JS and provides a canvas.
    *
    * The `#canvas` here is only a harness placeholder (100x100): the regression app leaves `config.canvasId` unset, so `BrowserApplication.createCanvas()` creates and appends its OWN 800x600 canvas —
    * which is therefore the LAST `<canvas>` in the document. Tests read that last canvas, never this harness placeholder (see the render test / ISS-726 c1).
    */
  private def createTestHtml(jsDir: Path): Path = {
    val html =
      """<!DOCTYPE html>
        |<html>
        |<head><meta charset="utf-8"><title>SGE Browser IT Test</title></head>
        |<body>
        |<canvas id="canvas" width="100" height="100" style="display:block"></canvas>
        |<script type="text/javascript" src="main.js"></script>
        |</body>
        |</html>""".stripMargin
    val htmlPath = jsDir.resolve("index.html")
    Files.writeString(htmlPath, html)
    htmlPath
  }

  /** One captured browser run of the regression app. */
  final private case class Captured(
    errors:  Seq[String],
    markers: Map[String, (String, String)], // NAME -> (STATUS, detail)
    summary: Option[String]
  )

  /** Load the compiled regression Scala.js app in headless Chromium and capture:
    *   - real console errors, with benign resource 404s excused per [[BrowserConsole]] (ISS-726 c2);
    *   - the structured `SGE-IT:<NAME>:<STATUS>:<detail>` self-report lines the `RegressionApp` prints to `console.log` (ISS-726 c3);
    *   - the final `SMOKE_TEST_*` summary line, if reached.
    *
    * When `awaitSummary` is true, polls until the summary line appears (all scenes ran) or `capMs` elapses; otherwise simply waits `minWaitMs`. Playwright-Java dispatches console events synchronously
    * on the calling thread during `waitForTimeout`, so the plain mutable buffers below need no synchronization.
    */
  private def loadRegression(awaitSummary: Boolean, minWaitMs: Int = 5000, capMs: Int = 60000): Captured = {
    val jsDir = findDemoJsDir()
    createTestHtml(jsDir)
    // Also serve from the classes directory so regression test resources
    // (regression/test-texture.png, etc.) are accessible.
    val classesDir     = jsDir.getParent.resolve("classes")
    val (server, port) =
      if (Files.isDirectory(classesDir)) startServer(jsDir, classesDir) else startServer(jsDir)

    try {
      val pw      = Playwright.create()
      val browser = pw.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))
      val context = browser.newContext()
      val page    = context.newPage()

      val errors   = mutable.ArrayBuffer.empty[String]
      val markers  = mutable.LinkedHashMap.empty[String, (String, String)]
      var summary  = Option.empty[String]
      val warnings = mutable.ArrayBuffer.empty[String]

      page.onConsoleMessage { msg =>
        val text = msg.text()
        if (msg.`type`() == "error") {
          // ISS-726 c2: only KNOWN-benign resource 404s (favicon) are excused;
          // any other console.error — including a missing SGE asset — is real.
          if (!BrowserConsole.isBenignError(msg))
            errors += s"console.error: $text @ ${msg.location()}"
        } else if (msg.`type`() == "warning") {
          warnings += text
        } else if (text.startsWith("SGE-IT:")) {
          // SGE-IT:<NAME>:<STATUS>:<detail...>
          val parts = text.split(":", 4)
          if (parts.length >= 3)
            markers.update(parts(1), (parts(2), if (parts.length == 4) parts(3) else ""))
        } else if (text.startsWith("SMOKE_TEST_")) {
          summary = Some(text)
        }
      }
      page.onPageError(err => errors += s"page error: $err")

      page.navigate(s"http://localhost:$port/")
      page.waitForLoadState(LoadState.NETWORKIDLE)

      if (awaitSummary) {
        val deadline = System.currentTimeMillis() + capMs
        while (summary.isEmpty && System.currentTimeMillis() < deadline)
          page.waitForTimeout(500)
      } else {
        page.waitForTimeout(minWaitMs.toDouble)
      }

      if (warnings.nonEmpty) {
        System.err.println(s"Browser warnings (${warnings.size}):")
        warnings.foreach(w => System.err.println(s"  console.warn: $w"))
      }

      val captured = Captured(errors.toSeq, markers.toMap, summary)
      browser.close()
      pw.close()
      captured
    } finally
      server.stop(0)
  }

  // ── Bootstrap: no fatal errors (ISS-726 c2) ─────────────────────────

  test("regression app loads in browser with no un-allow-listed console errors") {
    val cap = loadRegression(awaitSummary = false)
    assert(
      cap.errors.isEmpty,
      s"Browser reported ${cap.errors.size} non-benign console error(s):\n${cap.errors.mkString("\n")}"
    )
  }

  // ── SGE subsystem self-report (ISS-726 c3) ──────────────────────────
  // Replaces the deleted proxy tests "WebGL context is available", "WebGL
  // shader compilation succeeds", "Web Audio API context is available" and
  // "FileIO: fetch bundled text asset". Each of those exercised a raw Chromium
  // API and would pass even with SGE completely broken. The regression app,
  // by contrast, drives SGE's OWN browser backend (BrowserApplication ->
  // WebGL20/30, ShaderProgram, ModelBatch, DefaultBrowserAudio, BrowserFiles,
  // DefaultBrowserInput, Pixmap/Texture) and self-reports each result via the
  // SGE-IT console protocol — so these assertions fail on a real SGE regression.
  test("regression app self-reports every SGE subsystem PASS (SGE-IT markers, not raw Chromium APIs)") {
    val cap = loadRegression(awaitSummary = true)

    assert(
      cap.summary.isDefined,
      s"regression app never reached its SMOKE_TEST summary; captured markers=${cap.markers.keySet.mkString(",")}"
    )

    // Exact expected set (ISS-726 c1): every SGE-IT subsystem check the browser
    // run must emit and PASS. Enumerated from a live headless-Chromium run
    // (2026-07-18). A dropped check (missing key) OR a regressed check (status
    // != PASS) fails here — there is no numeric floor to rot, and a newly added
    // subsystem check must be added to this set deliberately.
    val requiredPass = List(
      "GL20",
      "VIEWPORT",
      "AUDIO_ACCESS",
      "FILES_ACCESS",
      "INPUT_ACCESS",
      "PIXMAP_TEXTURE",
      "ASSET_LOAD",
      "SHADER_COMPILE",
      "SHADER_UNIFORM",
      "SHADER_GLERROR",
      "MODEL3D_SETUP",
      "MODEL3D_RENDER",
      "INPUT_POLL"
    )
    val missing = requiredPass.filterNot(cap.markers.contains)
    assert(
      missing.isEmpty,
      s"missing SGE-IT subsystem marker(s): ${missing.mkString(",")}; got ${cap.markers.keySet.mkString(",")}"
    )
    val notPass = requiredPass.filter(n => cap.markers(n)._1 != "PASS")
    assert(
      notPass.isEmpty,
      s"SGE subsystem check(s) not PASS: ${notPass.map(n => s"$n=${cap.markers(n)}").mkString(", ")}"
    )

    // Identity of the wired subsystems — prove the REAL browser backends are in
    // place, not a no-op fallback (a NoopAudio here would be an SGE regression).
    assert(
      cap.markers("AUDIO_ACCESS")._2.contains("DefaultBrowserAudio"),
      s"expected DefaultBrowserAudio, got AUDIO_ACCESS=${cap.markers("AUDIO_ACCESS")}"
    )
    assert(
      cap.markers("FILES_ACCESS")._2.contains("BrowserFiles"),
      s"expected BrowserFiles, got FILES_ACCESS=${cap.markers("FILES_ACCESS")}"
    )
    assert(
      cap.markers("VIEWPORT")._2.contains("800x600"),
      s"expected 800x600 viewport, got VIEWPORT=${cap.markers("VIEWPORT")}"
    )

    // Any FAILing SGE-IT check other than the ONE known pre-existing browser gap
    // must fail this suite. Known gap (out of this TEST-ONLY territory, reported
    // upward, not silenced): FILE_EXISTS — regression/test-data.txt is not among
    // the base64-embedded browser resources, so BrowserFileHandle text-read is
    // not exercised on JS. Any NEW subsystem FAILure trips this assertion.
    val knownFailing   = Set("FILE_EXISTS")
    val unexpectedFail = cap.markers.collect { case (n, (s, _)) if s == "FAIL" && !knownFailing(n) => n }
    assert(
      unexpectedFail.isEmpty,
      s"unexpected SGE subsystem FAILure(s): ${unexpectedFail.mkString(",")} (full markers: ${cap.markers.mkString(", ")})"
    )
  }

  // ── SGE render target (ISS-726 c1: fix rotted floor + canvas-index bug) ──
  // The prior "canvas has non-zero pixels" test read document.querySelector(
  // 'canvas') — the FIRST canvas, i.e. the blank 100x100 harness placeholder SGE
  // never draws to — and asserted dataUrl.length > 300. Measured on 2026-07-18 a
  // blank 100x100 canvas already yields an 806-char PNG, so that test PASSED
  // SPURIOUSLY without SGE rendering anything. This reads SGE's OWN canvas (the
  // last <canvas>, which BrowserApplication appends at its configured 800x600)
  // and compares against a same-dimension blank baseline computed at runtime —
  // no magic length constant, and rendering to the wrong/blank canvas fails.
  test("SGE renders to its own 800x600 canvas, not the blank harness canvas") {
    val jsDir = findDemoJsDir()
    createTestHtml(jsDir)
    val (server, port) = startServer(jsDir)

    try {
      val pw      = Playwright.create()
      val browser = pw.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))
      val context = browser.newContext()
      val page    = context.newPage()

      page.navigate(s"http://localhost:$port/")
      page.waitForLoadState(LoadState.NETWORKIDLE)
      page.waitForTimeout(6000)

      // Report the harness canvas, SGE's canvas, and a same-size blank baseline.
      val result = page
        .evaluate(
          """(() => {
            |  const cs = document.querySelectorAll('canvas');
            |  if (cs.length < 2) return 'ERR_no_sge_canvas:count=' + cs.length;
            |  const harness = cs[0];                 // blank 100x100 placeholder
            |  const sge = cs[cs.length - 1];         // BrowserApplication's own canvas
            |  const blank = document.createElement('canvas');
            |  blank.width = sge.width; blank.height = sge.height;
            |  const blankLen = blank.toDataURL('image/png').length;
            |  const harnessLen = harness.toDataURL('image/png').length;
            |  const sgeLen = sge.toDataURL('image/png').length;
            |  return 'ok w=' + sge.width + ' h=' + sge.height +
            |    ' harness=' + harnessLen + ' blank=' + blankLen + ' sge=' + sgeLen;
            |})()""".stripMargin
        )
        .toString

      assert(result.startsWith("ok "), s"SGE canvas probe failed: $result")
      val fields = result
        .stripPrefix("ok ")
        .split(' ')
        .map { kv =>
          val Array(k, v) = kv.split('=')
          k -> v
        }
        .toMap
      val w          = fields("w").toInt
      val h          = fields("h").toInt
      val blankLen   = fields("blank").toInt
      val harnessLen = fields("harness").toInt
      val sgeLen     = fields("sge").toInt

      // SGE created its own canvas at the configured size (distinct from the
      // 100x100 harness) — proves BrowserApplication.createCanvas ran.
      assertEquals((w, h), (800, 600), s"SGE canvas dimensions unexpected: ${w}x$h (result=$result)")
      // SGE's canvas holds rendered content — strictly more PNG data than an
      // identically-sized blank canvas. (A blank/solid canvas would not.)
      assert(
        sgeLen > blankLen,
        s"SGE canvas appears blank: sge=$sgeLen must exceed same-size blank=$blankLen (result=$result)"
      )
      // Guard the original bug directly: the harness canvas (what the old test
      // read) is blank, so reading it instead of SGE's canvas must NOT look
      // rendered — sge must dominate harness.
      assert(
        sgeLen > harnessLen,
        s"SGE canvas ($sgeLen) does not exceed the blank harness canvas ($harnessLen) — canvas-index regression (result=$result)"
      )

      browser.close()
      pw.close()
    } finally
      server.stop(0)
  }

  // ── Deleted proxy tests (ISS-726 c3) — inventory + coverage citations ──
  //
  // The following tests were removed because they exercised plain browser
  // runtime APIs and could not fail on any SGE regression. Where the SGE code
  // path they gestured at is covered, the covering suite is cited.
  //
  //  * "WebGL context is available"        -> raw canvas.getContext. SGE's real
  //    context creation is now asserted via SGE-IT:GL20 above.
  //  * "WebGL shader compilation succeeds" -> raw gl.compileShader. SGE's real
  //    ShaderProgram compile+link+uniform is now SGE-IT:SHADER_COMPILE/
  //    SHADER_UNIFORM/SHADER_GLERROR above.
  //  * "Web Audio API context is available"-> raw new AudioContext. SGE's audio
  //    backend is now SGE-IT:AUDIO_ACCESS(=DefaultBrowserAudio) above.
  //  * "FileIO: fetch bundled text asset"  -> fetch() of a file the test wrote to
  //    its own server. SGE file/asset loading is SGE-IT:FILES_ACCESS(=BrowserFiles)
  //    + PIXMAP_TEXTURE + ASSET_LOAD above. (The regression app's own FILE_EXISTS
  //    check for regression/test-data.txt is a known browser embedded-resource
  //    gap, tracked via the knownFailing set above.)
  //  * "JSON and XML parsing works"        -> raw JSON.parse / DOMParser, not
  //    SGE code. SGE's JSON (jsoniter) cross-compiles to JS and is covered by
  //    sge/src/test/scala/sge/utils/JsonTest.scala and the Tiled/G3d JSON suites,
  //    which run on the JS test matrix.
  //  * "localStorage read/write roundtrip" and "BrowserPreferences localStorage
  //    protocol roundtrip" -> raw localStorage; neither invoked SGE's
  //    BrowserPreferences. Residual: BrowserPreferences has no dedicated JS unit
  //    test (recommend adding one under sge/src/test/scalajs).
  //  * "mouse click event dispatches to canvas", "touch event dispatches to
  //    canvas", "keyboard input event dispatches to canvas" -> Chromium event
  //    dispatch to a test-added listener, not SGE input. SGE's DefaultBrowserInput
  //    event handling is covered by
  //    sge/src/test/scalajs/sge/input/BrowserInputKeyTypedTouchCancelRedSuite.scala
  //    and BrowserInputWheelVelocityRedSuite.scala; access is SGE-IT:INPUT_ACCESS
  //    + INPUT_POLL above.
  //  * "HTTP fetch roundtrip via server"   -> fetch() of a file the test wrote,
  //    not SGE's BrowserNet. Request/response modeling is covered by
  //    sge/src/test/scala/sge/net/SgeHttpRequestTest.scala and
  //    SgeHttpResponseTest.scala (cross-platform). Residual: BrowserNet's live
  //    fetch has no dedicated JS IT.
}
