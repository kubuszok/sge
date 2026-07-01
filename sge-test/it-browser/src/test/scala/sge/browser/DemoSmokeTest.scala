// SGE — Demo smoke tests via Playwright
//
// Loads compiled Scala.js demo binaries in headless Chromium and verifies
// they start up without fatal errors and render non-blank frames.
//
// Prerequisites:
//   1. Build the demo JS: sbt --client 'pongJS/fastLinkJS'
//   2. Install Chromium for Playwright: npx playwright@1.49.0 install chromium
//
// Run: sbt 'sge-it-browser/test'  or  just test-browser

package sge.browser

import com.microsoft.playwright._
import com.microsoft.playwright.options.LoadState
import munit.FunSuite

import java.net.InetSocketAddress
import com.sun.net.httpserver.HttpServer
import java.nio.file.{ Files, Path, Paths }
import scala.collection.mutable

class DemoSmokeTest extends FunSuite {

  override val munitTimeout: scala.concurrent.duration.Duration =
    scala.concurrent.duration.Duration(120, "s")

  /** Locate the fastLinkJS output directory for a demo module by walking the demo's target tree for the `main.js` the linker emits — the cross-version path segment (`js-3` vs `scala-3.x`) and the
    * `<artifact>-fastopt` dir name vary by toolchain.
    */
  private def findDemoJsDir(demoName: String, artifactName: String): Path = {
    val cwd = Paths.get(System.getProperty("user.dir"))
    // demos are a separate sub-build; sbt-2.0's out-of-tree layout puts the app
    // fastLinkJS output at <root>/demos/.../<artifact>-fastopt/main.js (not
    // demos/<name>/target/js-3/...). Locate the app `-fastopt` dir by artifact name.
    val root = cwd.resolve("demos")
    findFastoptDir(root, artifactName).getOrElse {
      fail(
        s"Demo JS output ($artifactName-fastopt/main.js) not found for $demoName under $root. " +
          s"Run 'sbt ${demoName}JS/fastLinkJS' first. (cwd=$cwd)"
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

  /** Locate the packaged browser output for a demo produced by `sgePackageBrowser` (fullLinkJS + embedded resources + generated `index.html`). The task emits into
    * `<demoBase>/target/<js-segment>/sge-browser/<appName>/`, where `<appName>` is the demo's `releaseAppName` and `<js-segment>` (`js-3` vs `scala-3.x`) varies by toolchain — so we walk the demos
    * tree for a `main.js` whose parent directory is `<appName>` and whose grandparent is `sge-browser`, rather than hardcoding the version segment or an absolute path.
    */
  private def findPackagedBrowserDir(appName: String): Option[Path] = {
    val root = Paths.get(System.getProperty("user.dir")).resolve("demos")
    if (!Files.isDirectory(root)) None
    else {
      val stream = Files.walk(root)
      try {
        val found = stream
          .filter(p =>
            Files.isRegularFile(p) && p.getFileName.toString == "main.js" &&
              p.getParent.getFileName.toString == appName &&
              p.getParent.getParent.getFileName.toString == "sge-browser"
          )
          .findFirst()
        if (found.isPresent) Some(found.get.getParent) else None
      } finally stream.close()
    }
  }

  /** Start a simple HTTP server serving files from the given directory. */
  private def startServer(rootDir: Path): (HttpServer, Int) = {
    val server = HttpServer.create(new InetSocketAddress(0), 0)
    server.createContext(
      "/",
      exchange => {
        val requestPath = exchange.getRequestURI.getPath.stripPrefix("/")
        val filePath    = if (requestPath.isEmpty) "index.html" else requestPath
        val fullPath    = rootDir.resolve(filePath)

        if (Files.exists(fullPath) && !Files.isDirectory(fullPath)) {
          val bytes       = Files.readAllBytes(fullPath)
          val contentType =
            if (filePath.endsWith(".js")) "application/javascript"
            else if (filePath.endsWith(".html")) "text/html"
            else "application/octet-stream"
          exchange.getResponseHeaders.set("Content-Type", contentType)
          exchange.sendResponseHeaders(200, bytes.length)
          exchange.getResponseBody.write(bytes)
          exchange.getResponseBody.close()
        } else {
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
    * The inline patch script (which must run BEFORE `main.js` creates the GL context) forces `preserveDrawingBuffer: true` on every WebGL context the app requests. Without it a WebGL drawing buffer
    * is cleared right after compositing, so any later `toDataURL`/`drawImage`/`readPixels` reads an empty (transparent) buffer — which is exactly why the older non-blank heuristic was weak and why
    * the exact-pixel golden read (ISS-563) needs the buffer to persist. It only affects this test harness page, never production.
    */
  private def createTestHtml(jsDir: Path, width: Int = 800, height: Int = 600): Path = {
    val html =
      s"""<!DOCTYPE html>
         |<html>
         |<head><meta charset="utf-8"><title>SGE Demo Smoke Test</title>
         |<script type="text/javascript">
         |(function() {
         |  var orig = HTMLCanvasElement.prototype.getContext;
         |  HTMLCanvasElement.prototype.getContext = function(type, attrs) {
         |    if (type === 'webgl' || type === 'webgl2' || type === 'experimental-webgl') {
         |      attrs = Object.assign({}, attrs || {}, { preserveDrawingBuffer: true });
         |    }
         |    return orig.call(this, type, attrs);
         |  };
         |})();
         |</script>
         |</head>
         |<body style="margin:0;overflow:hidden">
         |<canvas id="canvas" width="$width" height="$height" style="display:block"></canvas>
         |<script type="text/javascript" src="main.js"></script>
         |</body>
         |</html>""".stripMargin
    val htmlPath = jsDir.resolve("index.html")
    Files.writeString(htmlPath, html)
    htmlPath
  }

  /** A golden pixel-readback expectation (ISS-563): the canvas pixel at (x, y) must equal RGB (r, g, b) within `tol` per channel and be fully opaque. */
  final private case class PixelGolden(x: Int, y: Int, r: Int, g: Int, b: Int, tol: Int)

  /** Run a full demo smoke test: load, wait for RAF frames, check for errors and rendering.
    *
    * @param jsDir
    *   when `None` (the default for every procedural demo), the served directory is located via [[findDemoJsDir]] (a raw fastLinkJS `<artifact>-fastopt` dir with no `index.html`), and a synthetic
    *   canvas harness [[createTestHtml]] is written there. When `Some(dir)` (used by the packaged AssetShowcase run), `dir` is served as-is: it is the real `sgePackageBrowser` output containing
    *   fullLinkJS `main.js` with embedded base64 assets plus a generated `index.html`, so we serve that packaged `index.html` (the app creates its own canvas) rather than overwriting it with the
    *   synthetic harness. Serving the real packaged output is the point of this override — assets are loaded through BrowserFileHandle/PlatformResources at startup, so a missing/broken asset surfaces
    *   as a console.error and fails the test.
    * @param pixelGolden
    *   when `Some` (ISS-563), after the RAF frames the canvas pixel at the given coords must match the expected RGBA — proving the scene rendered at the pixel level, not just "non-blank".
    */
  private def smokeTestDemo(
    demoName:     String,
    artifactName: String,
    waitMs:       Int = 5000,
    jsDir:        Option[Path] = None,
    pixelGolden:  Option[PixelGolden] = None
  ): Unit = {
    val servedDir = jsDir.getOrElse(findDemoJsDir(demoName, artifactName))
    // Only synthesize a canvas harness when the served directory has no index.html
    // (the raw fastLinkJS output). The packaged sgePackageBrowser output already
    // ships its own generated index.html, which we serve unchanged.
    if (!Files.exists(servedDir.resolve("index.html"))) createTestHtml(servedDir)
    val (server, port) = startServer(servedDir)

    try {
      val pw      = Playwright.create()
      val browser = pw.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))
      val context = browser.newContext()
      val page    = context.newPage()

      val errors = mutable.ArrayBuffer.empty[String]

      page.onConsoleMessage(msg =>
        if (msg.`type`() == "error") {
          val text = msg.text()
          // BrowserApplication fetches assets.txt at startup and gracefully handles
          // the 404 when no manifest exists — the browser still logs it as console.error.
          if (!text.contains("404") || !text.contains("Failed to load resource"))
            errors += s"console.error: $text"
        }
      )
      page.onPageError(err => errors += s"page error: $err")

      page.navigate(s"http://localhost:$port/")
      page.waitForLoadState(LoadState.NETWORKIDLE)
      page.waitForTimeout(waitMs.toDouble)

      // Check for fatal JS errors
      assert(
        errors.isEmpty,
        s"$demoName encountered ${errors.size} error(s):\n${errors.mkString("\n")}"
      )

      // Check that canvas rendered non-blank content
      val renderResult = page
        .evaluate(
          """(() => {
            |  const canvas = document.querySelector('canvas');
            |  if (!canvas) return 'no_canvas';
            |  const dataUrl = canvas.toDataURL('image/png');
            |  if (!dataUrl || dataUrl === 'data:,') return 'empty';
            |  if (dataUrl.length < 300) return 'likely_blank:' + dataUrl.length;
            |  return 'ok:' + dataUrl.length;
            |})()""".stripMargin
        )
        .toString

      assert(
        renderResult.startsWith("ok"),
        s"$demoName canvas appears blank: $renderResult"
      )

      // Count rendered frames via requestAnimationFrame
      val frameCount = page
        .evaluate(
          """(() => {
            |  return new Promise(resolve => {
            |    let count = 0;
            |    function tick() {
            |      count++;
            |      if (count >= 60) resolve(count);
            |      else requestAnimationFrame(tick);
            |    }
            |    requestAnimationFrame(tick);
            |  });
            |})()""".stripMargin
        )
        .toString
        .toDouble
        .toInt

      assert(frameCount >= 60, s"$demoName only rendered $frameCount frames (expected >=60)")

      // ── Pixel-level golden readback (ISS-563) ──────────────────────────
      // Beyond the non-blank heuristic above, prove an EXACT pixel value. We
      // snapshot the live WebGL canvas onto an offscreen 2D canvas via
      // drawImage (the drawing buffer is kept readable by the
      // preserveDrawingBuffer patch in createTestHtml) and read one pixel with
      // getImageData. The caller picks a pixel whose color is deterministic —
      // e.g. a corner that stays the scene's clear/background color every frame.
      //
      // BrowserApplication creates its OWN canvas and appends it to <body> when
      // config.canvasId is unset (which every demo's BrowserLauncher leaves
      // empty), so the rendered canvas is the LAST <canvas>, not the harness
      // '#canvas' that document.querySelector('canvas') would return first.
      pixelGolden.foreach { g =>
        val raw = page
          .evaluate(
            s"""(() => {
               |  const cs = document.querySelectorAll('canvas');
               |  if (cs.length === 0) return 'no_canvas';
               |  const canvas = cs[cs.length - 1];
               |  const off = document.createElement('canvas');
               |  off.width = canvas.width; off.height = canvas.height;
               |  const ctx = off.getContext('2d', { willReadFrequently: true });
               |  ctx.drawImage(canvas, 0, 0);
               |  const p = ctx.getImageData(${g.x}, ${g.y}, 1, 1).data;
               |  return p[0] + ',' + p[1] + ',' + p[2] + ',' + p[3];
               |})()""".stripMargin
          )
          .toString

        assert(raw != "no_canvas", s"$demoName: no canvas for pixel readback")
        val parts            = raw.split(',').map(_.toInt)
        val (pr, pg, pb, pa) = (parts(0), parts(1), parts(2), parts(3))
        val ok               =
          math.abs(pr - g.r) <= g.tol && math.abs(pg - g.g) <= g.tol &&
            math.abs(pb - g.b) <= g.tol && pa >= 250
        assert(
          ok,
          s"$demoName golden pixel mismatch at (${g.x},${g.y}): " +
            s"expected RGBA(${g.r},${g.g},${g.b},255) +/-${g.tol}, got RGBA($pr,$pg,$pb,$pa)"
        )
      }

      browser.close()
      pw.close()
    } finally
      server.stop(0)
  }

  // ─── Procedural demos (no assets needed) ────────────────────────────

  test("Pong demo runs without errors and renders frames") {
    smokeTestDemo("pong", "sge-demo-pong")
  }

  // Pixel-level golden (ISS-563): Pong clears the whole backbuffer every frame
  // with ScreenUtils.clear(0.05, 0.05, 0.1, 1) (PongGame.scala:130) and its
  // paddles start at x>=30 with score digits near center, so a top-left corner
  // pixel is deterministically the background color across every frame:
  // 0.05*255≈13, 0.1*255≈26 → RGBA(13,13,26,255). This asserts the EXACT
  // background color reached the composited canvas — not just that it is
  // non-blank. Corner (4,4) with a small per-channel tolerance for 8-bit
  // rounding; the tolerance is far narrower than the distance to black (0) or
  // white (255), so a blank or wrong-color canvas still fails.
  test("Pong demo renders the exact background clear color at a corner (ISS-563)") {
    smokeTestDemo(
      "pong",
      "sge-demo-pong",
      pixelGolden = Some(PixelGolden(x = 4, y = 4, r = 13, g = 13, b = 26, tol = 8))
    )
  }

  test("SpaceShooter demo runs without errors and renders frames") {
    smokeTestDemo("space-shooter", "sge-demo-spaceshooter")
  }

  test("Curves demo runs without errors and renders frames") {
    smokeTestDemo("curve-playground", "sge-demo-curves")
  }

  test("Viewports demo runs without errors and renders frames") {
    smokeTestDemo("viewport-gallery", "sge-demo-viewports")
  }

  test("ShaderLab demo runs without errors and renders frames") {
    smokeTestDemo("shader-lab", "sge-demo-shaders")
  }

  test("TileWorld demo runs without errors and renders frames") {
    smokeTestDemo("tile-world", "sge-demo-tileworld")
  }

  test("HexTactics demo runs without errors and renders frames") {
    smokeTestDemo("hex-tactics", "sge-demo-hextactics")
  }

  test("Viewer3D demo runs without errors and renders frames") {
    smokeTestDemo("viewer-3d", "sge-demo-viewer3d")
  }

  test("ParticleShow demo runs without errors and renders frames") {
    smokeTestDemo("particle-show", "sge-demo-particles")
  }

  test("NetChat demo runs without errors and renders frames") {
    smokeTestDemo("net-chat", "sge-demo-netchat")
  }

  // ─── Asset-backed demo (packaged fullLinkJS + embedded assets) ──────

  // AssetShowcase needs texture/audio assets. They are embedded (base64) into the
  // fullLinkJS `main.js` by `sgePackageBrowser` and loaded synchronously at startup
  // through BrowserFileHandle/PlatformResources — so this exercises the packaged
  // output AND real asset loading on JS (ISS-558). A missing/broken asset makes
  // BrowserFileHandle throw → console.error → this test fails (that is its canary).
  test("AssetShowcase demo (packaged fullLinkJS + embedded assets) runs without errors and renders frames") {
    val packaged = findPackagedBrowserDir("SGE Asset Showcase").getOrElse {
      fail(
        "Packaged browser output (sge-browser/SGE Asset Showcase/main.js) not found under demos. " +
          "Run 'assetShowcaseJS/sgePackageBrowser' first."
      )
    }
    smokeTestDemo("asset-showcase", "sge-demo-assets", jsDir = Some(packaged))
  }
}
