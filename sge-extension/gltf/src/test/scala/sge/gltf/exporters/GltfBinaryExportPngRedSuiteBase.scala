/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red suite base for ISS-533 clause 1 — GLTFBinaryExporter.savePNG calls
 * sge.graphics.PixmapIO.writePNG through RAW java.lang reflection
 * (Class.forName at GLTFBinaryExporter.scala:161, Class.getMethod at :162,
 * Method.invoke at :163), which breaks the Scala.js / Scala Native baseline:
 * neither platform's javalib implements those reflection entry points, so any
 * test binary that reaches savePNG fails AT LINK TIME, not at run time.
 *
 * Expected behavior from the original source (original-src/gdx-gltf/gltf/src/
 * net/mgsx/gltf/exporters/GLTFBinaryExporter.java):
 *   - savePNG (lines 145-159) writes the pixmap as a PNG via
 *     PixmapIO.writePNG(file, pixmap), guarded by the explicit WebGL check at
 *     lines 146-147.
 *   - Upstream routes the call through LibGDX's ClassReflection FACADE
 *     (ClassReflection.forName / getMethod / Method.invoke, lines 152-154)
 *     solely "to avoid compilation error with GWT" (upstream comment, lines
 *     149-150). ClassReflection is emulated on every LibGDX backend including
 *     GWT, so upstream savePNG works on all of them.
 *   - SGE has no GWT backend and no ClassReflection facade, and
 *     sge.graphics.PixmapIO is plain shared source compiled on JVM, JS and
 *     Native alike (PixmapIO.scala:90, writePNG(FileHandle, Pixmap)). The
 *     faithful port is therefore the DIRECT call; raw java.lang.reflect is a
 *     port-introduced platform regression.
 *
 * Scope notes:
 *   - Upstream's image export entry point export(GLTFImage, Texture, String)
 *     (java lines 123-143 / scala lines 127-149) renders the texture through
 *     FrameBuffer + SpriteBatch and needs a live GL context. savePNG (called
 *     at java line 141 / scala line 147) is the exact reflection site named
 *     by ISS-533, so this suite calls it directly with a headless 2x2 pixmap
 *     and a byte-capturing FileHandleStream — no GL, no filesystem. The suite
 *     lives in package sge.gltf.exporters to reach the private[exporters]
 *     object.
 *   - One concrete GltfBinaryExportPngRedSuite per platform extends this base
 *     (test/scalajvm = GREEN control, test/scalajs + test/scalanative = RED).
 *     Because the red is a STATIC link error, the broken link takes down the
 *     whole sge-gltf JS/Native test binary, not just this suite — that blast
 *     radius is inherent to the bug (any downstream user linking savePNG hits
 *     the same wall) and is exactly what ISS-533 means by "breaking the
 *     Native/JS baseline".
 *
 * This test is written by the reproducer agent and MUST NOT be modified by the
 * fixer: it encodes the original Java semantics, not the port's.
 */
package sge
package gltf
package exporters

import java.io.{ ByteArrayOutputStream, OutputStream }

import sge.files.FileHandleStream
import sge.graphics.Pixmap
import sge.noop.{ NoopAudio, NoopGraphics, NoopInput }

abstract class GltfBinaryExportPngRedSuiteBase extends munit.FunSuite {

  import GltfBinaryExportPngRedSuiteBase.*

  private given Sge = Sge(NoopApplicationStub, new NoopGraphics(), new NoopAudio(), NoopFilesStub, new NoopInput(), NoopNetStub)

  private val Red   = 0xff0000ff // RGBA8888 opaque red
  private val Green = 0x00ff00ff // RGBA8888 opaque green

  /** 2x2 pixmap built with pure gdx2d ops — the same headless construction the shared Gdx2DPixmapTest exercises on every platform; no GL context needed. */
  private def makePixmap(): Pixmap = {
    val pixmap = new Pixmap(2, 2, Pixmap.Format.RGBA8888)
    pixmap.setBlending(Pixmap.Blending.None)
    pixmap.setColor(Red)
    pixmap.fill()
    pixmap.drawPixel(Pixels(1), Pixels(1), Green)
    pixmap
  }

  /** Captures the bytes savePNG writes "to disk" in memory (PNG.write uses file.write(false), PixmapIO.scala:217), so no platform needs a real filesystem. */
  final private class CapturingFileHandle extends FileHandleStream("gltf-export/image0.png") {
    val captured = new ByteArrayOutputStream()

    override def write(append: Boolean): OutputStream = captured
  }

  test("ISS-533 (1): GLTFBinaryExporter.savePNG writes a decodable PNG headlessly (GLTFBinaryExporter.java:145-159)") {
    val pixmap = makePixmap()
    try {
      val file = new CapturingFileHandle
      GLTFBinaryExporter.savePNG(file, pixmap)
      val bytes = file.captured.toByteArray()
      // PNG signature (PixmapIO.scala:188) — proves writePNG actually ran.
      assertEquals(
        bytes.take(8).toList,
        List[Byte](-119, 80, 78, 71, 13, 10, 26, 10),
        "savePNG must emit a PNG signature"
      )
      // Round-trip through the gdx2d decoder — identical path on all platforms.
      val decoded = new Pixmap(bytes, 0, bytes.length)
      try {
        assertEquals(decoded.width, Pixels(2), "decoded width")
        assertEquals(decoded.height, Pixels(2), "decoded height")
        assertEquals(decoded.getPixel(Pixels(0), Pixels(0)), Red, "pixel (0,0)")
        assertEquals(decoded.getPixel(Pixels(1), Pixels(0)), Red, "pixel (1,0)")
        assertEquals(decoded.getPixel(Pixels(0), Pixels(1)), Red, "pixel (0,1)")
        assertEquals(decoded.getPixel(Pixels(1), Pixels(1)), Green, "pixel (1,1)")
      } finally
        decoded.close()
    } finally
      pixmap.close()
  }
}

object GltfBinaryExportPngRedSuiteBase {

  // Minimal headless Sge fixture — mirrors sge/src/test/scala/sge/SgeTestFixture.scala,
  // which is not on the gltf extension's test classpath (no test->test dependency).
  // applicationType = HeadlessDesktop, so savePNG takes the reflection branch
  // (the WebGL guard at GLTFBinaryExporter.java:146-147 / scala :155-156 is not
  // what ISS-533 is about).
  private object NoopApplicationStub extends Application {
    def applicationListener:                                  ApplicationListener         = throw new UnsupportedOperationException
    def graphics:                                             Graphics                    = throw new UnsupportedOperationException
    def audio:                                                Audio                       = throw new UnsupportedOperationException
    def input:                                                Input                       = throw new UnsupportedOperationException
    def files:                                                Files                       = throw new UnsupportedOperationException
    def net:                                                  Net                         = throw new UnsupportedOperationException
    def applicationType:                                      Application.ApplicationType = Application.ApplicationType.HeadlessDesktop
    def version:                                              Int                         = 0
    def javaHeap:                                             Long                        = 0L
    def nativeHeap:                                           Long                        = 0L
    def getPreferences(name:              String):            Preferences                 = throw new UnsupportedOperationException
    def clipboard:                                            sge.utils.Clipboard         = throw new UnsupportedOperationException
    def postRunnable(runnable:            Runnable):          Unit                        = ()
    def exit():                                               Unit                        = ()
    def addLifecycleListener(listener:    LifecycleListener): Unit                        = ()
    def removeLifecycleListener(listener: LifecycleListener): Unit                        = ()
  }

  private object NoopFilesStub extends Files {
    def getFileHandle(path: String, fileType: sge.files.FileType): sge.files.FileHandle = throw new UnsupportedOperationException
    def classpath(path:     String):                               sge.files.FileHandle = throw new UnsupportedOperationException
    def internal(path:      String):                               sge.files.FileHandle = throw new UnsupportedOperationException
    def external(path:      String):                               sge.files.FileHandle = throw new UnsupportedOperationException
    def absolute(path:      String):                               sge.files.FileHandle = throw new UnsupportedOperationException
    def local(path:         String):                               sge.files.FileHandle = throw new UnsupportedOperationException
    def externalStoragePath:                                       String               = ""
    def isExternalStorageAvailable:                                Boolean              = false
    def localStoragePath:                                          String               = ""
    def isLocalStorageAvailable:                                   Boolean              = false
  }

  private object NoopNetStub extends Net {
    import Net.*
    def httpClient:                                                                                         sge.net.SgeHttpClient = sge.net.SgeHttpClient.noop()
    def newServerSocket(protocol: Protocol, hostname: String, port: Int, hints: sge.net.ServerSocketHints): sge.net.ServerSocket  = throw new UnsupportedOperationException
    def newServerSocket(protocol: Protocol, port:     Int, hints:   sge.net.ServerSocketHints):             sge.net.ServerSocket  = throw new UnsupportedOperationException
    def newClientSocket(protocol: Protocol, host:     String, port: Int, hints: sge.net.SocketHints):       sge.net.Socket        = throw new UnsupportedOperationException
    def openURI(URI:              String):                                                                  Boolean               = false
  }
}
