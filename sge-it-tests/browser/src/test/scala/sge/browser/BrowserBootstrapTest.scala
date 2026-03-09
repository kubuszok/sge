// SGE — Browser integration test: Bootstrap + subsystem checks
//
// Uses Playwright (JVM) to load the compiled Scala.js demo in a real headless
// Chromium browser and checks for runtime JavaScript errors. Catches:
// - ReferenceError from bare global references (Accelerometer, webkitAudioContext)
// - TypeError from null/undefined mishandling in WebGL wrapper
// - TypeError from Scala Array vs JS TypedArray conversions
// - NullPointerException from initialization order bugs
//
// Also verifies subsystem integration: WebGL context, canvas rendering,
// and absence of critical runtime errors beyond bootstrap.
//
// Prerequisites:
//   1. Build the demo JS: sbt --client 'demoJS/fastLinkJS'
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
import scala.jdk.CollectionConverters._

class BrowserBootstrapTest extends FunSuite {

  // Playwright browser launch + page load + 3s wait needs more than the default 30s
  override val munitTimeout: scala.concurrent.duration.Duration =
    scala.concurrent.duration.Duration(60, "s")

  /** Locate the fastLinkJS output directory for the demo module. */
  private def findDemoJsDir(): Path = {
    // sbt runs tests from the project root, so user.dir should be the repo root.
    // The demo fastLinkJS output is at: demo/target/js-3/sge-demo-fastopt/
    val cwd        = Paths.get(System.getProperty("user.dir"))
    val candidates = Seq(
      // From project root (sbt runs from here)
      cwd.resolve("demo/target/js-3/sge-demo-fastopt"),
      // From sge-it-tests/browser subdir (just in case)
      cwd.resolve("../../demo/target/js-3/sge-demo-fastopt").normalize
    )
    candidates.find(p => Files.isDirectory(p) && Files.exists(p.resolve("main.js"))).getOrElse {
      fail(
        s"Demo JS output not found. Run 'sbt demoJS/fastLinkJS' first.\n" +
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

  test("demo JS loads in browser without fatal errors") {
    val jsDir = findDemoJsDir()
    createTestHtml(jsDir)
    val (server, port) = startServer(jsDir)

    try {
      val pw      = Playwright.create()
      val browser = pw.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))
      val context = browser.newContext()
      val page    = context.newPage()

      val errors   = mutable.ArrayBuffer.empty[String]
      val warnings = mutable.ArrayBuffer.empty[String]

      // Capture console errors and uncaught exceptions
      page.onConsoleMessage(msg =>
        if (msg.`type`() == "error") errors += s"console.error: ${msg.text()}"
        else if (msg.`type`() == "warning") warnings += s"console.warn: ${msg.text()}"
      )
      page.onPageError(err => errors += s"page error: $err")

      // Navigate and wait for initial load
      page.navigate(s"http://localhost:$port/")
      page.waitForLoadState(LoadState.NETWORKIDLE)

      // Give the app time to initialize (requestAnimationFrame loop, WebGL setup, etc.)
      page.waitForTimeout(3000)

      // Report results
      if (warnings.nonEmpty) {
        System.err.println(s"Browser warnings (${warnings.size}):")
        warnings.foreach(w => System.err.println(s"  $w"))
      }

      assert(
        errors.isEmpty,
        s"Browser encountered ${errors.size} error(s) during startup:\n${errors.mkString("\n")}"
      )

      browser.close()
      pw.close()
    } finally
      server.stop(0)
  }

  test("WebGL context is available") {
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
      page.waitForTimeout(3000)

      // Check that a WebGL2 (or WebGL1) context can be obtained from the canvas
      val hasWebGL = page
        .evaluate(
          """(() => {
            |  const canvas = document.querySelector('canvas');
            |  if (!canvas) return 'no_canvas';
            |  const gl = canvas.getContext('webgl2') || canvas.getContext('webgl');
            |  return gl ? 'ok' : 'no_context';
            |})()""".stripMargin
        )
        .toString

      assert(
        hasWebGL == "ok",
        s"WebGL context not available: $hasWebGL"
      )

      browser.close()
      pw.close()
    } finally
      server.stop(0)
  }

  test("canvas has non-zero pixels after rendering") {
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
      // Wait a bit longer for render frames
      page.waitForTimeout(5000)

      // Sample a pixel from the canvas to verify rendering happened
      val pixelSum = page
        .evaluate(
          """(() => {
            |  const canvas = document.querySelector('canvas');
            |  if (!canvas) return -1;
            |  const gl = canvas.getContext('webgl2') || canvas.getContext('webgl');
            |  if (!gl) return -2;
            |  const pixels = new Uint8Array(4);
            |  gl.readPixels(50, 50, 1, 1, gl.RGBA, gl.UNSIGNED_BYTE, pixels);
            |  return pixels[0] + pixels[1] + pixels[2] + pixels[3];
            |})()""".stripMargin
        )
        .toString
        .toDouble
        .toInt

      // The demo clears to an HSV-cycling color, so at least some channel should be non-zero.
      // pixelSum of 0 means nothing was rendered (all black with 0 alpha).
      // A value > 0 means at least one color channel or alpha has content.
      assert(
        pixelSum > 0,
        s"Canvas pixel sum is $pixelSum — nothing appears to have been rendered"
      )

      browser.close()
      pw.close()
    } finally
      server.stop(0)
  }
}
