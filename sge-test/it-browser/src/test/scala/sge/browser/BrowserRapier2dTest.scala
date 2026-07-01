// SGE — Rapier2D vendored global build smoke test via Playwright (ISS-679)
//
// Proves the vendored `rapier2d-compat.umd.js` (built by esbuild from
// sge-build/src/main/resources/rapier2d-compat/wrapper.mjs) actually exposes a
// working `window.RAPIER` in a real browser — the exact global that
// PhysicsExtension.load() reads on Scala.js, and that SgePackaging injects
// (before main.js) when sgeBrowserIncludeRapier2d := true.
//
// This is the real-browser counterpart to the sge-build scripted test
// (packaging/browser-rapier2d), which only asserts the <script> wiring.
//
// Prerequisites: npx playwright@1.49.0 install chromium
//
// Run: sbt --client 'sge-it-browser/testOnly sge.browser.BrowserRapier2dTest'

package sge.browser

import com.microsoft.playwright._
import com.microsoft.playwright.options.LoadState
import munit.FunSuite

import java.net.InetSocketAddress
import com.sun.net.httpserver.HttpServer
import java.nio.file.{ Files, Path, Paths }

class BrowserRapier2dTest extends FunSuite {

  override val munitTimeout: scala.concurrent.duration.Duration =
    scala.concurrent.duration.Duration(120, "s")

  private val UmdName = "rapier2d-compat.umd.js"

  /** Locate the vendored global build committed under the sge-build plugin resources. `Test / baseDirectory` is the build root (see build.sbt), so resolve from there. */
  private def locateUmd(): Path = {
    val root = Paths.get(System.getProperty("user.dir"))
    val umd  = root.resolve("sge-build/src/main/resources/rapier2d-compat").resolve(UmdName)
    assert(
      Files.isRegularFile(umd),
      s"vendored $UmdName not found at $umd — regenerate per sge-build/src/main/resources/rapier2d-compat/README.md"
    )
    umd
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
    (server, server.getAddress.getPort)
  }

  test("vendored rapier2d-compat.umd.js exposes a working window.RAPIER in Chromium") {
    val umd    = locateUmd()
    val tmpDir = Files.createTempDirectory("sge-rapier2d-umd")
    Files.copy(umd, tmpDir.resolve(UmdName))
    // Minimal page that includes ONLY the vendored global build — mirrors what
    // SgePackaging injects before main.js in a shipped physics browser game.
    val html =
      s"""<!DOCTYPE html>
         |<html>
         |<head><meta charset="utf-8"><title>SGE Rapier2D UMD smoke test</title></head>
         |<body>
         |<script src="$UmdName"></script>
         |</body>
         |</html>""".stripMargin
    Files.writeString(tmpDir.resolve("index.html"), html)

    val (server, port) = startServer(tmpDir)
    try {
      val pw      = Playwright.create()
      val browser = pw.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))
      val page    = browser.newContext().newPage()
      try {
        page.navigate(s"http://localhost:$port/")
        page.waitForLoadState(LoadState.NETWORKIDLE)

        // The global is present with the shape PhysicsExtension.obtainModule expects:
        // an object exposing an async init(). (PhysicsExtension.scala:81 / :67.)
        val hasGlobal = page.evaluate("() => typeof window.RAPIER === 'object' && typeof window.RAPIER.init === 'function'").asInstanceOf[java.lang.Boolean]
        assert(hasGlobal.booleanValue(), s"window.RAPIER / window.RAPIER.init missing after loading $UmdName")

        // Stronger proof: init() actually compiles+instantiates the inlined WASM
        // and the physics API surface (World) becomes constructible — exactly the
        // flow PhysicsExtension.load() performs. page.evaluate awaits the Promise.
        val initialized = page
          .evaluate(
            """() => window.RAPIER.init().then(() =>
              |  typeof window.RAPIER.World === 'function' && typeof window.RAPIER.RigidBodyDesc === 'function')""".stripMargin
          )
          .asInstanceOf[java.lang.Boolean]
        assert(initialized.booleanValue(), s"window.RAPIER.init() did not yield a usable physics API from $UmdName")
      } finally {
        browser.close()
        pw.close()
      }
    } finally
      server.stop(0)
  }
}
