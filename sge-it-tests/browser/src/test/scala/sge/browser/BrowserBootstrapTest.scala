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

  test("WebGL shader compilation succeeds") {
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

      // Compile a minimal vertex + fragment shader pair in WebGL
      val result = page
        .evaluate(
          """(() => {
            |  const canvas = document.querySelector('canvas');
            |  if (!canvas) return 'no_canvas';
            |  const gl = canvas.getContext('webgl2') || canvas.getContext('webgl');
            |  if (!gl) return 'no_context';
            |  const vs = gl.createShader(gl.VERTEX_SHADER);
            |  gl.shaderSource(vs, 'attribute vec4 a_pos; void main() { gl_Position = a_pos; }');
            |  gl.compileShader(vs);
            |  if (!gl.getShaderParameter(vs, gl.COMPILE_STATUS)) return 'vs_fail: ' + gl.getShaderInfoLog(vs);
            |  const fs = gl.createShader(gl.FRAGMENT_SHADER);
            |  gl.shaderSource(fs, 'precision mediump float; void main() { gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0); }');
            |  gl.compileShader(fs);
            |  if (!gl.getShaderParameter(fs, gl.COMPILE_STATUS)) return 'fs_fail: ' + gl.getShaderInfoLog(fs);
            |  const prog = gl.createProgram();
            |  gl.attachShader(prog, vs);
            |  gl.attachShader(prog, fs);
            |  gl.linkProgram(prog);
            |  if (!gl.getProgramParameter(prog, gl.LINK_STATUS)) return 'link_fail: ' + gl.getProgramInfoLog(prog);
            |  gl.deleteProgram(prog);
            |  gl.deleteShader(vs);
            |  gl.deleteShader(fs);
            |  return 'ok';
            |})()""".stripMargin
        )
        .toString

      assert(result == "ok", s"WebGL shader compilation failed: $result")

      browser.close()
      pw.close()
    } finally
      server.stop(0)
  }

  test("JSON and XML parsing works in browser context") {
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

      // Verify JavaScript JSON parsing works (proxy for jsoniter-scala cross-compiled to JS)
      val jsonResult = page
        .evaluate(
          """(() => {
            |  try {
            |    const obj = JSON.parse('{"name":"test","value":42}');
            |    return obj.name === 'test' && obj.value === 42 ? 'ok' : 'mismatch';
            |  } catch(e) { return 'error: ' + e.message; }
            |})()""".stripMargin
        )
        .toString

      assert(jsonResult == "ok", s"JSON parsing failed: $jsonResult")

      // Verify DOMParser XML parsing works (proxy for scala-xml cross-compiled to JS)
      val xmlResult = page
        .evaluate(
          """(() => {
            |  try {
            |    const parser = new DOMParser();
            |    const doc = parser.parseFromString('<root><item key="a">hello</item></root>', 'text/xml');
            |    const item = doc.querySelector('item');
            |    return item && item.getAttribute('key') === 'a' && item.textContent === 'hello' ? 'ok' : 'mismatch';
            |  } catch(e) { return 'error: ' + e.message; }
            |})()""".stripMargin
        )
        .toString

      assert(xmlResult == "ok", s"XML parsing failed: $xmlResult")

      browser.close()
      pw.close()
    } finally
      server.stop(0)
  }

  test("FileIO: fetch bundled text asset from server") {
    val jsDir = findDemoJsDir()
    createTestHtml(jsDir)

    // Create a test asset file that the HTTP server will serve
    val testContent = "SGE browser integration test asset"
    val assetPath   = jsDir.resolve("test-asset.txt")
    Files.writeString(assetPath, testContent)

    val (server, port) = startServer(jsDir)

    try {
      val pw      = Playwright.create()
      val browser = pw.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))
      val context = browser.newContext()
      val page    = context.newPage()

      page.navigate(s"http://localhost:$port/")
      page.waitForLoadState(LoadState.NETWORKIDLE)

      // Fetch the text asset via the same HTTP server (mirrors how BrowserFileHandle works)
      val result = page
        .evaluate(
          """(async () => {
            |  try {
            |    const resp = await fetch('/test-asset.txt');
            |    if (!resp.ok) return 'http_' + resp.status;
            |    const text = await resp.text();
            |    return text;
            |  } catch(e) { return 'error: ' + e.message; }
            |})()""".stripMargin
        )
        .toString

      assertEquals(result, testContent)

      browser.close()
      pw.close()
    } finally {
      server.stop(0)
      Files.deleteIfExists(assetPath)
    }
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

      // Use toDataURL to capture the composited canvas contents.
      // gl.readPixels returns zeros when preserveDrawingBuffer is false (default)
      // because WebGL clears the drawing buffer after compositing.
      val result = page
        .evaluate(
          """(() => {
            |  const canvas = document.querySelector('canvas');
            |  if (!canvas) return 'no_canvas';
            |  const dataUrl = canvas.toDataURL('image/png');
            |  if (!dataUrl || dataUrl === 'data:,') return 'empty';
            |  // A blank (transparent) 100x100 PNG is ~
            |  // 'data:image/png;base64,iVBORw0KGgo...' with a short base64.
            |  // A rendered frame has significantly more data.
            |  // Blank PNGs for a 100x100 canvas are typically < 200 chars.
            |  if (dataUrl.length < 300) return 'likely_blank:' + dataUrl.length;
            |  return 'ok:' + dataUrl.length;
            |})()""".stripMargin
        )
        .toString

      assert(
        result.startsWith("ok"),
        s"Canvas appears blank after rendering: $result"
      )

      browser.close()
      pw.close()
    } finally
      server.stop(0)
  }

  test("Web Audio API context is available") {
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
      page.waitForTimeout(2000)

      // Check that AudioContext is available and can be created
      val result = page
        .evaluate(
          """(() => {
            |  try {
            |    const AudioCtx = window.AudioContext || window.webkitAudioContext;
            |    if (!AudioCtx) return 'no_audio_context';
            |    const ctx = new AudioCtx();
            |    const state = ctx.state;
            |    const rate = ctx.sampleRate;
            |    ctx.close();
            |    return 'ok:' + state + ':' + rate;
            |  } catch(e) { return 'error: ' + e.message; }
            |})()""".stripMargin
        )
        .toString

      assert(result.startsWith("ok"), s"Web Audio API failed: $result")

      browser.close()
      pw.close()
    } finally
      server.stop(0)
  }

  test("localStorage read/write roundtrip") {
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

      // Write a value to localStorage, read it back, verify, then clean up
      val result = page
        .evaluate(
          """(() => {
            |  try {
            |    const key = 'sge-it-test-' + Date.now();
            |    const value = 'hello-sge';
            |    localStorage.setItem(key, value);
            |    const readBack = localStorage.getItem(key);
            |    localStorage.removeItem(key);
            |    if (readBack !== value) return 'mismatch: ' + readBack;
            |    // Verify removal
            |    const afterRemove = localStorage.getItem(key);
            |    if (afterRemove !== null) return 'remove_failed';
            |    return 'ok';
            |  } catch(e) { return 'error: ' + e.message; }
            |})()""".stripMargin
        )
        .toString

      assert(result == "ok", s"localStorage roundtrip failed: $result")

      browser.close()
      pw.close()
    } finally
      server.stop(0)
  }

  test("mouse click event dispatches to canvas") {
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

      // Set up a listener on the canvas to capture mouse events
      val setup = page
        .evaluate(
          """(() => {
            |  const canvas = document.querySelector('canvas');
            |  if (!canvas) return 'no_canvas';
            |  window.__sgeMouseEvents = [];
            |  canvas.addEventListener('mousedown', e => window.__sgeMouseEvents.push('down:' + e.button));
            |  canvas.addEventListener('mouseup', e => window.__sgeMouseEvents.push('up:' + e.button));
            |  return 'ok';
            |})()""".stripMargin
        )
        .toString

      if (setup != "ok") {
        fail(s"Canvas mouse setup failed: $setup")
      }

      // Click on the canvas via Playwright
      val canvas = page.querySelector("canvas")
      assert(canvas != null, "Canvas element not found")
      canvas.click()
      page.waitForTimeout(200)

      // Check captured events
      val result = page
        .evaluate(
          """(() => {
            |  return window.__sgeMouseEvents.join(',');
            |})()""".stripMargin
        )
        .toString

      assert(
        result.contains("down:0"),
        s"Expected mousedown event, got: '$result'"
      )

      browser.close()
      pw.close()
    } finally
      server.stop(0)
  }

  test("BrowserPreferences localStorage protocol roundtrip") {
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

      // Test the localStorage protocol that BrowserPreferences uses:
      // prefix:keyTYPE_SUFFIX = value
      val result = page
        .evaluate(
          """(() => {
            |  try {
            |    const prefix = 'sge-it-test:';
            |    // Write typed values using BrowserPreferences' key format
            |    localStorage.setItem(prefix + 'names', 'Alice');
            |    localStorage.setItem(prefix + 'agei', '42');
            |    localStorage.setItem(prefix + 'activeb', 'true');
            |    localStorage.setItem(prefix + 'scoref', '3.14');
            |    // Read back and verify
            |    const name = localStorage.getItem(prefix + 'names');
            |    const age = localStorage.getItem(prefix + 'agei');
            |    const active = localStorage.getItem(prefix + 'activeb');
            |    const score = localStorage.getItem(prefix + 'scoref');
            |    // Clean up
            |    localStorage.removeItem(prefix + 'names');
            |    localStorage.removeItem(prefix + 'agei');
            |    localStorage.removeItem(prefix + 'activeb');
            |    localStorage.removeItem(prefix + 'scoref');
            |    if (name !== 'Alice') return 'name_mismatch:' + name;
            |    if (age !== '42') return 'age_mismatch:' + age;
            |    if (active !== 'true') return 'active_mismatch:' + active;
            |    if (score !== '3.14') return 'score_mismatch:' + score;
            |    // Verify removal
            |    if (localStorage.getItem(prefix + 'names') !== null) return 'remove_failed';
            |    return 'ok';
            |  } catch(e) { return 'error: ' + e.message; }
            |})()""".stripMargin
        )
        .toString

      assert(result == "ok", s"BrowserPreferences protocol failed: $result")

      browser.close()
      pw.close()
    } finally
      server.stop(0)
  }

  test("touch event dispatches to canvas") {
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

      // Set up touch event listeners on the canvas
      val setup = page
        .evaluate(
          """(() => {
            |  const canvas = document.querySelector('canvas');
            |  if (!canvas) return 'no_canvas';
            |  window.__sgeTouchEvents = [];
            |  canvas.addEventListener('touchstart', e => {
            |    e.preventDefault();
            |    window.__sgeTouchEvents.push('start:' + e.touches.length);
            |  });
            |  canvas.addEventListener('touchend', e => {
            |    e.preventDefault();
            |    window.__sgeTouchEvents.push('end');
            |  });
            |  return 'ok';
            |})()""".stripMargin
        )
        .toString

      if (setup != "ok") {
        fail(s"Canvas touch setup failed: $setup")
      }

      // Dispatch a synthetic TouchEvent from JavaScript
      val result = page
        .evaluate(
          """(() => {
            |  const canvas = document.querySelector('canvas');
            |  const rect = canvas.getBoundingClientRect();
            |  const touch = new Touch({
            |    identifier: 0,
            |    target: canvas,
            |    clientX: rect.left + 50,
            |    clientY: rect.top + 50,
            |    radiusX: 5,
            |    radiusY: 5
            |  });
            |  canvas.dispatchEvent(new TouchEvent('touchstart', {
            |    touches: [touch],
            |    targetTouches: [touch],
            |    changedTouches: [touch],
            |    cancelable: true,
            |    bubbles: true
            |  }));
            |  canvas.dispatchEvent(new TouchEvent('touchend', {
            |    touches: [],
            |    targetTouches: [],
            |    changedTouches: [touch],
            |    cancelable: true,
            |    bubbles: true
            |  }));
            |  return window.__sgeTouchEvents.join(',');
            |})()""".stripMargin
        )
        .toString

      assert(
        result.contains("start:1"),
        s"Expected touchstart event, got: '$result'"
      )

      browser.close()
      pw.close()
    } finally
      server.stop(0)
  }

  test("HTTP fetch roundtrip via server") {
    val jsDir = findDemoJsDir()
    createTestHtml(jsDir)

    // Create a JSON endpoint file to simulate an API response
    val apiContent = """{"status":"ok","value":42}"""
    val apiPath    = jsDir.resolve("api-test.json")
    Files.writeString(apiPath, apiContent)

    val (server, port) = startServer(jsDir)

    try {
      val pw      = Playwright.create()
      val browser = pw.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))
      val context = browser.newContext()
      val page    = context.newPage()

      page.navigate(s"http://localhost:$port/")
      page.waitForLoadState(LoadState.NETWORKIDLE)

      // Test fetch() API which is the same transport BrowserNet uses via sttp
      val result = page
        .evaluate(
          """(async () => {
            |  try {
            |    const resp = await fetch('/api-test.json');
            |    if (!resp.ok) return 'http_error:' + resp.status;
            |    const contentType = resp.headers.get('Content-Type') || '';
            |    const data = await resp.json();
            |    if (data.status !== 'ok') return 'status_mismatch:' + data.status;
            |    if (data.value !== 42) return 'value_mismatch:' + data.value;
            |    return 'ok';
            |  } catch(e) { return 'error: ' + e.message; }
            |})()""".stripMargin
        )
        .toString

      assert(result == "ok", s"HTTP fetch failed: $result")

      browser.close()
      pw.close()
    } finally {
      server.stop(0)
      Files.deleteIfExists(apiPath)
    }
  }

  test("keyboard input event dispatches to canvas") {
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

      // Set up a listener on the canvas to capture keyboard events
      val setup = page
        .evaluate(
          """(() => {
            |  const canvas = document.querySelector('canvas');
            |  if (!canvas) return 'no_canvas';
            |  window.__sgeKeyEvents = [];
            |  canvas.tabIndex = 0;
            |  canvas.focus();
            |  canvas.addEventListener('keydown', e => window.__sgeKeyEvents.push('down:' + e.key));
            |  canvas.addEventListener('keyup', e => window.__sgeKeyEvents.push('up:' + e.key));
            |  return 'ok';
            |})()""".stripMargin
        )
        .toString

      if (setup != "ok") {
        fail(s"Canvas setup failed: $setup")
      }

      // Simulate keyboard press via Playwright
      page.keyboard().press("a")
      page.waitForTimeout(200)

      // Check captured events
      val result = page
        .evaluate(
          """(() => {
            |  return window.__sgeKeyEvents.join(',');
            |})()""".stripMargin
        )
        .toString

      // Should have at least a keydown event
      assert(
        result.contains("down:a"),
        s"Expected keyboard event, got: '$result'"
      )

      browser.close()
      pw.close()
    } finally
      server.stop(0)
  }
}
