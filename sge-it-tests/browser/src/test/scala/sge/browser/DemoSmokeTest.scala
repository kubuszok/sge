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

  /** Locate the fastLinkJS output directory for a demo module. */
  private def findDemoJsDir(demoName: String, artifactName: String): Path = {
    val cwd        = Paths.get(System.getProperty("user.dir"))
    val candidates = Seq(
      cwd.resolve(s"demos/$demoName/target/js-3/$artifactName-fastopt"),
      cwd.resolve(s"../../demos/$demoName/target/js-3/$artifactName-fastopt").normalize
    )
    candidates.find(p => Files.isDirectory(p) && Files.exists(p.resolve("main.js"))).getOrElse {
      fail(
        s"Demo JS output not found for $demoName. Run 'sbt ${demoName}JS/fastLinkJS' first.\n" +
          s"Checked: ${candidates.mkString(", ")}"
      )
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

  /** Create a minimal HTML page that loads the demo JS and provides a canvas. */
  private def createTestHtml(jsDir: Path, width: Int = 800, height: Int = 600): Path = {
    val html =
      s"""<!DOCTYPE html>
         |<html>
         |<head><meta charset="utf-8"><title>SGE Demo Smoke Test</title></head>
         |<body style="margin:0;overflow:hidden">
         |<canvas id="canvas" width="$width" height="$height" style="display:block"></canvas>
         |<script type="text/javascript" src="main.js"></script>
         |</body>
         |</html>""".stripMargin
    val htmlPath = jsDir.resolve("index.html")
    Files.writeString(htmlPath, html)
    htmlPath
  }

  /** Run a full demo smoke test: load, wait for RAF frames, check for errors and rendering. */
  private def smokeTestDemo(demoName: String, artifactName: String, waitMs: Int = 5000): Unit = {
    val jsDir = findDemoJsDir(demoName, artifactName)
    createTestHtml(jsDir)
    val (server, port) = startServer(jsDir)

    try {
      val pw      = Playwright.create()
      val browser = pw.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))
      val context = browser.newContext()
      val page    = context.newPage()

      val errors = mutable.ArrayBuffer.empty[String]

      page.onConsoleMessage(msg => if (msg.`type`() == "error") errors += s"console.error: ${msg.text()}")
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

      browser.close()
      pw.close()
    } finally
      server.stop(0)
  }

  // ─── Procedural demos (no assets needed) ────────────────────────────

  test("Pong demo runs without errors and renders frames") {
    smokeTestDemo("pong", "sge-demo-pong")
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
}
