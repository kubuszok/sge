/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red tests for ISS-514 (textra .json.lzma Structured JSON fonts cannot be
 * loaded even though Font.getJsonExtension resolves to them first and a
 * complete LZMA codec — LzmaUtils — already exists in this module).
 *
 * Root cause being reproduced:
 *  - Font.getJsonExtension (sge-extension/textra/src/main/scala/sge/textra/
 *    Font.scala lines 3714-3721) probes .ubj.lzma, .json.lzma, .ubj, .dat,
 *    .json in upstream's order, so a distributed .json.lzma asset wins over
 *    a sibling .json.
 *  - Font.loadJSON (same file, lines 3357-3359) only special-cases "dat"
 *    (LZB) and otherwise feeds the raw file bytes to the UTF-8 JSON reader,
 *    so the resolved .json.lzma file is parsed as if it were plain JSON and
 *    crashes on binary LZMA garbage.
 *  - BitmapFontSupport.JsonFontData.load (BitmapFontSupport.scala lines
 *    142-147) explicitly throws for "ubj" and "lzma" with a STALE comment
 *    claiming LZMA "is not yet ported" — LzmaUtils.scala (2,580 lines, in
 *    the same package) is a full port of textratypist's LzmaUtils with
 *    working compress/decompress and zero callers.
 *
 * Upstream semantics being pinned (original-src/textratypist/src/main/java/
 * com/github/tommyettinger/textra/Font.java, loadJSON lines 3208-3245):
 *
 *   if ("lzma".equalsIgnoreCase(jsonHandle.extension())) {
 *     Lzma.decompress(bais, baos);
 *     if (name endsWith ".json.lzma") fnt = new JsonReader().parse(baos.toString("UTF-8"));
 *     else if (name endsWith ".ubj.lzma") fnt = new UBJsonReader().parse(...);
 *     else throw ...
 *   } else if ("ubj" ...) ...
 *
 * and the identical dispatch in BitmapFontSupport.java JsonFontData.load
 * (lines 142-168). The LZMA container is the standalone .lzma format
 * (5 properties bytes + 8-byte little-endian size + stream), exactly what
 * LzmaUtils.compress in this module produces — so the fixtures below are
 * built at runtime with the ported codec itself, which doubles as proof
 * that the dormant LzmaUtils round-trips correctly (see the control test).
 *
 * UBJSON note (out of scope here): SGE has no UBJsonReader → Json AST
 * bridge. sge.utils exposes typed UBJsonCodec[T] derivation (hearth
 * kindlings, see sge/src/main/scala/sge/utils/JsonCodecs.scala) used by
 * G3dBinaryModelLoader, but no codec for the generic Json AST that
 * loadJSON consumes. Therefore these tests deliberately do NOT pin .ubj /
 * .ubj.lzma behavior; the implementer needs either a UBJsonCodec[Json]
 * bridge or a documented unsupported-format error for those extensions.
 *
 * Headless usage: Font.canUseTextures = false is the upstream-supported
 * headless mode (same switch loadSad honors); the fixture also carries a
 * U+2588 glyph so finalizeJsonFont never builds a texture-backed solid
 * block. All assertions are on glyph metrics, never on textures.
 *
 * Platform placement: JVM and Native copies only (byte-identical), per the
 * ObjLoaderFanRedSuite precedent — the suite is pure in-memory data, but
 * instantiating FileHandle subclasses reaches base-class bodies that
 * reference java.io.FileInputStream / Class.getResourceAsStream, which the
 * Scala.js test linker rejects.
 *
 * These tests are written by the reproducer agent and MUST NOT be modified
 * by the fixer: they encode the original Java semantics, not the port's.
 */
package sge
package textra

import java.io.{ ByteArrayInputStream, ByteArrayOutputStream, InputStream, OutputStream }
import java.nio.charset.StandardCharsets

import sge.files.{ FileHandle, FileHandleStream, FileType }
import sge.noop.{ NoopAudio, NoopGraphics, NoopInput }

class TextraLzmaFontRedSuite extends munit.FunSuite {

  /** Minimal Structured JSON font (msdf-atlas-gen/fontwriter shape) accepted by Font.loadJSON and BitmapFontSupport.JsonFontData:
    *   - "atlas" with "size" 32 (drives all metric scaling; "type" standard avoids distance-field shaders),
    *   - a space glyph (finalizeJsonFont requires ' ' or 'l'),
    *   - 'A' (unicode 65) with advance 0.5 -> xAdvance 0.5 * 32 = 16,
    *   - U+2588 FULL BLOCK (9608) so finalizeJsonFont skips creating a texture-backed solid block in headless mode.
    */
  private val FixtureJson: String =
    """{
      |  "atlas": { "type": "standard", "size": 32, "width": 256, "height": 256 },
      |  "metrics": { "emSize": 1, "lineHeight": 1.25, "ascender": 0.75, "descender": -0.25 },
      |  "glyphs": [
      |    { "unicode": 32, "advance": 0.25 },
      |    { "unicode": 65, "advance": 0.5,
      |      "planeBounds": { "left": 0.05, "bottom": 0.0, "right": 0.45, "top": 0.7 },
      |      "atlasBounds": { "left": 1.0, "bottom": 1.0, "right": 13.0, "top": 23.0 } },
      |    { "unicode": 9608, "advance": 0.5 }
      |  ],
      |  "kerning": []
      |}""".stripMargin

  private val fixtureJsonBytes: Array[Byte] = FixtureJson.getBytes(StandardCharsets.UTF_8)

  /** In-memory readable "file": only read() (and the base-class helpers built on it) is exercised. */
  final private class BytesFileHandle(fileName: String, bytes: Array[Byte]) extends FileHandleStream(fileName) {
    override def read(): InputStream = new ByteArrayInputStream(bytes)
  }

  /** In-memory writable "file" capturing everything written through write(false). */
  final private class CapturingFileHandle(fileName: String) extends FileHandleStream(fileName) {
    private val sink = new ByteArrayOutputStream()
    override def write(append: Boolean): OutputStream = sink
    def bytes:                           Array[Byte]  = sink.toByteArray
  }

  /** A path probed by getJsonExtension that must report absence. */
  final private class MissingFileHandle(fileName: String) extends FileHandleStream(fileName) {
    override def exists(): Boolean = false
  }

  /** The fixture compressed into the standalone .lzma container by the module's own (currently caller-less) LzmaUtils codec. */
  private lazy val fixtureLzmaBytes: Array[Byte] = {
    val compressed = new CapturingFileHandle("LzmaFixture.json.lzma")
    LzmaUtils.compress(new BytesFileHandle("LzmaFixture.json", fixtureJsonBytes), compressed)
    compressed.bytes
  }

  /** Files stub resolving internal() lookups from an in-memory map; absent names report exists() == false. */
  final private class MapFiles(handles: Map[String, FileHandle]) extends Files {
    def getFileHandle(path: String, fileType: FileType): FileHandle = internal(path)
    def classpath(path:     String):                     FileHandle = internal(path)
    def internal(path:      String):                     FileHandle = handles.getOrElse(path, new MissingFileHandle(path))
    def external(path:      String):                     FileHandle = internal(path)
    def absolute(path:      String):                     FileHandle = internal(path)
    def local(path:         String):                     FileHandle = internal(path)
    def externalStoragePath:                             String     = ""
    def isExternalStorageAvailable:                      Boolean    = false
    def localStoragePath:                                String     = ""
    def isLocalStorageAvailable:                         Boolean    = false
  }

  private object StubApplication extends Application {
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

  private object StubNet extends Net {
    import Net.*
    def httpClient:                                                                                         net.SgeHttpClient = net.SgeHttpClient.noop()
    def newServerSocket(protocol: Protocol, hostname: String, port: Int, hints: sge.net.ServerSocketHints): net.ServerSocket  = throw new UnsupportedOperationException
    def newServerSocket(protocol: Protocol, port:     Int, hints:   sge.net.ServerSocketHints):             net.ServerSocket  = throw new UnsupportedOperationException
    def newClientSocket(protocol: Protocol, host:     String, port: Int, hints: sge.net.SocketHints):       net.Socket        = throw new UnsupportedOperationException
    def openURI(URI:              String):                                                                  Boolean           = false
  }

  private def sgeWith(handles: Map[String, FileHandle]): Sge =
    Sge(StubApplication, new NoopGraphics(), new NoopAudio(), new MapFiles(handles), new NoopInput(), StubNet)

  /** Runs body in upstream's headless mode (no Texture objects), restoring the global afterwards. */
  private def headless[A](body: => A): A = {
    val previous = Font.canUseTextures
    Font.canUseTextures = false
    try body
    finally Font.canUseTextures = previous
  }

  private def assertFixtureFontLoaded(font: Font): Unit = {
    assertEquals(font.name, "LzmaFixture")
    assert(font.mapping.contains('A'.toInt), "glyph 'A' (unicode 65) from the fixture must be mapped")
    // advance 0.5 * atlas size 32 + widthAdjust 0 = 16
    assertEqualsFloat(font.mapping('A'.toInt).xAdvance, 16f, 0.001f, "glyph 'A' must keep its JSON metrics (advance 0.5 * size 32)")
  }

  test("ISS-514: Font.loadJSON loads a .json.lzma FileHandle (upstream Font.java lines 3218-3236)") {
    headless {
      val handle = new BytesFileHandle("LzmaFixture.json.lzma", fixtureLzmaBytes)
      // Upstream decompresses via Lzma.decompress and parses the result as UTF-8 JSON.
      // The port currently feeds the raw LZMA bytes to the JSON reader and crashes.
      val font = new Font(handle, new sge.graphics.g2d.TextureRegion(), 0f, 0f, 0f, 0f, false, true)
      assertFixtureFontLoaded(font)
    }
  }

  test("ISS-514: getJsonExtension prefers .json.lzma over .json AND the resolved file must then load (upstream order)") {
    headless {
      given Sge = sgeWith(
        Map(
          "LzmaFixture.json.lzma" -> new BytesFileHandle("LzmaFixture.json.lzma", fixtureLzmaBytes),
          "LzmaFixture.json" -> new BytesFileHandle("LzmaFixture.json", fixtureJsonBytes)
        )
      )
      val resolved = Font.getJsonExtension("LzmaFixture")
      // Upstream Font.getJsonExtension (Font.java lines 142-152) tries .ubj.lzma, .json.lzma, .ubj, .dat, .json in order.
      assertEquals(resolved, "LzmaFixture.json.lzma", "getJsonExtension must keep upstream's probe order: .json.lzma wins over .json")
      // ... and what it resolves to must actually be loadable, otherwise the preference order is a crash generator.
      val font = new Font(resolved, new sge.graphics.g2d.TextureRegion(), 0f, 0f, 0f, 0f, false, true)
      assertFixtureFontLoaded(font)
    }
  }

  test("ISS-514: BitmapFontSupport.JsonFontData loads a .json.lzma file (upstream BitmapFontSupport.java lines 150-168)") {
    // Currently throws RuntimeException("LZMA-compressed font loading is not yet supported: ...")
    // behind a stale "not yet ported" comment, while LzmaUtils sits unused in the same package.
    val data  = new BitmapFontSupport.JsonFontData(new BytesFileHandle("LzmaFixture.json.lzma", fixtureLzmaBytes))
    val glyph = data.getGlyph('A')
    assert(glyph.isDefined, "glyph 'A' from the LZMA-compressed fixture must be present")
    assertEquals(glyph.fold(-1)(_.xadvance), 16, "glyph 'A' xadvance must be round(advance 0.5 * size 32)")
  }

  test("ISS-514 control (green at red commit): the same fixture loads as plain .json") {
    headless {
      val handle = new BytesFileHandle("LzmaFixture.json", fixtureJsonBytes)
      val font   = new Font(handle, new sge.graphics.g2d.TextureRegion(), 0f, 0f, 0f, 0f, false, true)
      assertFixtureFontLoaded(font)
    }
  }

  test("ISS-514 control (green at red commit): LzmaUtils compress/decompress round-trips the fixture") {
    // Proves the dormant codec works, so wiring it into loadJSON / JsonFontData is pure plumbing.
    val decompressed = new CapturingFileHandle("LzmaFixture.roundtrip.json")
    LzmaUtils.decompress(new BytesFileHandle("LzmaFixture.json.lzma", fixtureLzmaBytes), decompressed)
    assertEquals(new String(decompressed.bytes, StandardCharsets.UTF_8), FixtureJson)
  }
}
