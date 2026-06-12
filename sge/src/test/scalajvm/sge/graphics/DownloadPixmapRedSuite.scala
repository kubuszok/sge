/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red suite for ISS-523: Pixmap.downloadFromUrl was never ported.
 *
 * The original LibGDX Pixmap.java declares a static
 * `downloadFromUrl(String url, DownloadPixmapResponseListener responseListener)`
 * (Pixmap.java:205-235) that:
 *   - Pixmap.java:206-208 — issues an async HTTP GET via Gdx.net.sendHttpRequest
 *     (SGE replaces the Gdx.* globals with the `(using Sge)` context, so the
 *     ported method uses Sge().net.httpClient).
 *   - NetJavaImpl.java:195-245 — the request executes on a pooled background
 *     "NetThread" (thread factory naming at NetJavaImpl.java:144);
 *     handleHttpResponse is invoked on that net thread (NetJavaImpl.java:230)
 *     and failed(t) likewise (NetJavaImpl.java:239, 248).
 *   - Pixmap.java:211-222 — on response, the bytes are taken and the decode +
 *     downloadComplete(pixmap) are handed to Gdx.app.postRunnable, i.e. the
 *     listener's success callback runs on the render thread (also documented
 *     on the listener itself, Pixmap.java:480-481), never on the net thread.
 *   - Pixmap.java:215-220 — a decode failure inside the posted runnable is
 *     routed to failed(t) → downloadFailed(t).
 *   - Pixmap.java:225-228 — an HTTP-level failure calls
 *     responseListener.downloadFailed(t) directly from the net listener's
 *     failed(t) — no postRunnable, which is why Pixmap.java:484 documents
 *     "This might get called on a background thread."
 *
 * The SGE port (sge/src/main/scala/sge/graphics/Pixmap.scala) carries the
 * DownloadPixmapResponseListener trait (Pixmap.scala:484-493) complete with a
 * doc link to downloadFromUrl (Pixmap.scala:483), but the method itself does
 * not exist anywhere in the codebase — the listener is dead public API with
 * zero callers. The covenant was baselined without downloadFromUrl, so
 * `re-scale enforce verify` cannot notice the missing method.
 *
 * At the red sha this suite FAILS TO COMPILE ("value downloadFromUrl is not a
 * member of object sge.graphics.Pixmap") — that compile failure is the red.
 *
 * JVM-only: proving the contract needs a real transport (in-process
 * com.sun.net.httpserver + the real JVM sttp backend), the same pattern as
 * HttpBinaryBodyRedSuite; JS/Native have no in-process HTTP server harness in
 * this test infrastructure.
 */
package sge
package graphics

import com.sun.net.httpserver.{ HttpExchange, HttpServer }
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket as JServerSocket
import java.util.concurrent.{ ConcurrentLinkedQueue, CountDownLatch, TimeUnit }
import javax.imageio.ImageIO
import munit.FunSuite
import sge.net.{ ServerSocket, ServerSocketHints, SgeHttpClient, Socket, SocketHints }

class DownloadPixmapRedSuite extends FunSuite {

  private val imageWidth  = 3
  private val imageHeight = 2

  private var server: HttpServer    = scala.compiletime.uninitialized
  private var client: SgeHttpClient = scala.compiletime.uninitialized

  private def baseUrl: String = s"http://127.0.0.1:${server.getAddress.getPort}"

  /** Encodes a tiny imageWidth x imageHeight PNG fully in memory — Pixmap's byte-array constructor decodes PNG on the JVM (see PixmapIOPngRedSuite, which round-trips PNG bytes the same way). */
  private def encodePng(): Array[Byte] = {
    val image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB)
    var y     = 0
    while (y < imageHeight) {
      var x = 0
      while (x < imageWidth) {
        image.setRGB(x, y, 0xff000000 | ((x * 80) << 16) | ((y * 80) << 8) | 0x40)
        x += 1
      }
      y += 1
    }
    val out = new ByteArrayOutputStream()
    ImageIO.write(image, "png", out)
    out.toByteArray
  }

  private def respondWith(exchange: HttpExchange, status: Int, contentType: String, body: Array[Byte]): Unit = {
    exchange.getResponseHeaders.set("Content-Type", contentType)
    exchange.sendResponseHeaders(status, body.length.toLong)
    val os = exchange.getResponseBody
    try os.write(body)
    finally os.close()
  }

  override def beforeAll(): Unit = {
    val png = encodePng()
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0)
    server.createContext("/image.png", (exchange: HttpExchange) => respondWith(exchange, 200, "image/png", png))
    server.createContext(
      "/missing.png",
      (exchange: HttpExchange) => respondWith(exchange, 404, "text/html", "<html><body>not found</body></html>".getBytes("UTF-8"))
    )
    server.start()
    // Real client → real JVM sttp backend, the same path a game uses (mirrors
    // Gdx.net.sendHttpRequest at Pixmap.java:208 / NetJavaImpl.java:195).
    client = SgeHttpClient()
  }

  override def afterAll(): Unit = {
    if (client != null) client.close() // test-only null check: uninitialized lifecycle guard
    if (server != null) server.stop(0) // test-only null check: uninitialized lifecycle guard
  }

  /** Application whose postRunnable queues runnables like a real render loop — the test drains the queue to play the render thread's part (Pixmap.java:212 posts the decode there). */
  final private class RenderQueueApplication extends Application {
    val posted       = new ConcurrentLinkedQueue[Runnable]
    val postedSignal = new CountDownLatch(1)

    /** Runs every queued runnable on the calling thread, like one render-loop iteration. */
    def drain(): Unit =
      while (!posted.isEmpty)
        posted.poll().run()

    def applicationListener:              ApplicationListener         = throw new UnsupportedOperationException
    def graphics:                         Graphics                    = throw new UnsupportedOperationException
    def audio:                            Audio                       = throw new UnsupportedOperationException
    def input:                            Input                       = throw new UnsupportedOperationException
    def files:                            Files                       = throw new UnsupportedOperationException
    def net:                              Net                         = throw new UnsupportedOperationException
    def applicationType:                  Application.ApplicationType = Application.ApplicationType.HeadlessDesktop
    def version:                          Int                         = 0
    def javaHeap:                         Long                        = 0L
    def nativeHeap:                       Long                        = 0L
    def getPreferences(name: String):     Preferences                 = throw new UnsupportedOperationException
    def clipboard:                        sge.utils.Clipboard         = throw new UnsupportedOperationException
    def postRunnable(runnable: Runnable): Unit                        = {
      posted.add(runnable)
      postedSignal.countDown()
    }
    def exit():                                               Unit = ()
    def addLifecycleListener(listener:    LifecycleListener): Unit = ()
    def removeLifecycleListener(listener: LifecycleListener): Unit = ()
  }

  /** Net backed by the real HTTP client — downloadFromUrl must go through Sge().net (Pixmap.java:208). */
  final private class RealNet(realClient: SgeHttpClient) extends Net {
    def httpClient:                                                                                     SgeHttpClient = realClient
    def newServerSocket(protocol: Net.Protocol, hostname: String, port: Int, hints: ServerSocketHints): ServerSocket  =
      throw new UnsupportedOperationException
    def newServerSocket(protocol: Net.Protocol, port: Int, hints: ServerSocketHints): ServerSocket =
      throw new UnsupportedOperationException
    def newClientSocket(protocol: Net.Protocol, host: String, port: Int, hints: SocketHints): Socket =
      throw new UnsupportedOperationException
    def openURI(URI: String): Boolean = false
  }

  final private class RecordingListener extends Pixmap.DownloadPixmapResponseListener {
    val done = new CountDownLatch(1)
    @volatile var completed: Option[Pixmap]    = None
    @volatile var failure:   Option[Throwable] = None

    def downloadComplete(pixmap: Pixmap): Unit = {
      completed = Some(pixmap)
      done.countDown()
    }
    def downloadFailed(t: Throwable): Unit = {
      failure = Some(t)
      done.countDown()
    }
  }

  private def sgeWith(app: RenderQueueApplication): Sge =
    SgeTestFixture.testSge(application = app, net = new RealNet(client))

  /** Waits for the listener while repeatedly draining the render queue — covers callbacks routed through postRunnable (Pixmap.java:212-222) as well as direct ones (Pixmap.java:226-227). */
  private def awaitWithDrain(app: RenderQueueApplication, done: CountDownLatch, seconds: Long = 15L): Boolean = {
    val deadline = System.nanoTime() + seconds * 1000000000L
    var finished = done.await(50, TimeUnit.MILLISECONDS)
    while (!finished && System.nanoTime() < deadline) {
      app.drain()
      finished = done.await(50, TimeUnit.MILLISECONDS)
    }
    finished
  }

  test("success: downloadComplete receives the decoded Pixmap with the served dimensions, via Application.postRunnable") {
    val app      = new RenderQueueApplication
    val listener = new RecordingListener
    given Sge    = sgeWith(app)

    // Upstream contract: Pixmap.java:205-208 — static method, async GET via the net module.
    Pixmap.downloadFromUrl(s"$baseUrl/image.png", listener)

    // Pixmap.java:211-212 — the response handler posts the decode to the render loop.
    assert(
      app.postedSignal.await(15, TimeUnit.SECONDS),
      "downloadFromUrl must hand the response bytes to Application.postRunnable (Pixmap.java:212)"
    )

    // Pixmap.java:212-222 and Pixmap.java:480-481: downloadComplete is "Called on the render thread" — it must
    // not fire on the net thread before the application runs the posted runnable.
    assert(
      listener.completed.isEmpty,
      "downloadComplete must not fire before the render loop runs the posted runnable (Pixmap.java:212-222, Pixmap.java:480-481)"
    )
    assert(listener.failure.isEmpty, s"unexpected downloadFailed: ${listener.failure}")

    app.drain()
    assert(listener.done.await(15, TimeUnit.SECONDS), "listener was not called after draining the render queue")
    assert(listener.failure.isEmpty, s"downloadFailed instead of downloadComplete: ${listener.failure}")
    val pixmap = listener.completed.getOrElse(fail("downloadComplete was not called (Pixmap.java:217)"))
    // Pixmap.java:216 — new Pixmap(result, 0, result.length) decodes the downloaded bytes.
    assertEquals(pixmap.width, Pixels(imageWidth), "downloaded pixmap width (Pixmap.java:216-217)")
    assertEquals(pixmap.height, Pixels(imageHeight), "downloaded pixmap height (Pixmap.java:216-217)")
    pixmap.close()
  }

  test("404: a non-image error response surfaces through downloadFailed with the throwable") {
    val app      = new RenderQueueApplication
    val listener = new RecordingListener
    given Sge    = sgeWith(app)

    Pixmap.downloadFromUrl(s"$baseUrl/missing.png", listener)

    // Pixmap.java:215-220 — decoding the non-image body throws inside the posted runnable and is routed to
    // failed(t) → downloadFailed(t) (Pixmap.java:226-227); the drain loop also covers backends that report
    // HTTP errors via failed(t) directly.
    assert(awaitWithDrain(app, listener.done), "listener was not called for the 404 response")
    assert(listener.completed.isEmpty, "downloadComplete must not be called for a 404 response")
    assert(
      listener.failure.isDefined,
      "downloadFailed(t) must receive the failure for a 404/non-image response (Pixmap.java:215-220, 226-227)"
    )
  }

  test("connection refused: downloadFailed is called without requiring the render loop") {
    // Bind-and-close a socket so the port is guaranteed to refuse connections.
    val probe    = new JServerSocket(0)
    val deadPort =
      try probe.getLocalPort
      finally probe.close()

    val app      = new RenderQueueApplication
    val listener = new RecordingListener
    given Sge    = sgeWith(app)

    Pixmap.downloadFromUrl(s"http://127.0.0.1:$deadPort/image.png", listener)

    // Pixmap.java:225-228 — an HTTP-level failure invokes responseListener.downloadFailed(t) straight from the
    // net listener's failed(t), with no Gdx.app.postRunnable involved; Pixmap.java:484 documents that it "might
    // get called on a background thread". So the listener must complete although we never drain the render queue.
    assert(
      listener.done.await(15, TimeUnit.SECONDS),
      "downloadFailed must be invoked directly from the net failure callback, without a render-loop drain (Pixmap.java:226-227)"
    )
    assert(listener.completed.isEmpty, "downloadComplete must not be called when the connection is refused")
    assert(listener.failure.isDefined, "downloadFailed(t) must receive the connection failure (Pixmap.java:226-227)")
  }
}
