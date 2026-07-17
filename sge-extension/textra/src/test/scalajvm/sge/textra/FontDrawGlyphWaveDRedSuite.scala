/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red tests for the textra Font.drawGlyph render-path fidelity cluster of
 * wave 2026-07-17-D (all citing TextraTypist upstream commit
 * 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4;
 * original-src/textratypist/src/main/java/com/github/tommyettinger/textra/
 * Font.java):
 *
 *   ISS-710 — JOSTLE mode ([?jostle]) parses and is defined but drawGlyph
 *     never applies the per-glyph pseudo-random offsets, so jostled text
 *     renders identically to plain text. Upstream Font.java:5340-5346 sets
 *     `jostled`, and 5593-5596 offsets xc/yt by
 *     `code % 5 - 2` / `(code >>> 6) % 5 - 2` before the quad is built.
 *
 *   ISS-711 — resume/pauseDistanceFieldShader are defined (Font.scala:2109,
 *     2130) but have ZERO call sites. Upstream Font.java:5352-5363, at the top
 *     of drawGlyph, switches shaders whenever the glyph's texture changes: if
 *     the new texture belongs to a parent it resumes the distance-field shader,
 *     otherwise it pauses it (batch.setShader(null)). Without that switch, a
 *     distance-field font mixed with plain atlas pages renders with the wrong
 *     shader.
 *
 *   ISS-712 — HALO/NEON/secondaryColor are computed AFTER the box-drawing
 *     branch (Font.scala:2688-2697) instead of before it (upstream
 *     Font.java:5380-5398, before the box branch at 5440). As a result the
 *     box-drawing outline sub-branch hardcodes PACKED_BLACK (Font.scala:2579)
 *     where upstream uses `secondaryColor` (Font.java:5465) — so a colored
 *     outline (e.g. [BLUE] outline mode) on a box-drawing glyph draws black.
 *
 *   ISS-713 — the pixel-size projection uses a hardcoded 960x540 backbuffer
 *     (Font.scala:2601-2602) instead of the real backbuffer
 *     (upstream Font.java:5510-5511, `Gdx.graphics.getBackBufferWidth()` /
 *     `getBackBufferHeight()`), so underline/strikethrough segment geometry is
 *     wrong at any resolution other than 960x540.
 *
 * These are render-path issues; there is no GL here. The harness builds a
 * headless Sge (NoopGraphics + NoopGL20, the proven pattern from
 * sge.graphics.g2d.SpriteBatchVertexGeometryISS561Suite: a Custom-type
 * TextureData lets `new Texture(...)` succeed with no GL) and a recording
 * Batch that captures every `draw(texture, verts, 0, 20)` call (the vertex
 * geometry drawGlyph emits) plus every `setShader` call. Each test inspects
 * those recorded vertices / shader calls — a state/computed-value assertion,
 * as the wave brief allows for render-path issues.
 *
 * These tests are written by the reproducer agent and MUST NOT be modified by
 * the fixer: they encode the original TextraTypist semantics, not the port's.
 */
package sge
package textra

import scala.collection.mutable.ArrayBuffer

import sge.graphics.{ Color, Texture }
import sge.graphics.g2d.{ Batch, TextureRegion }
import sge.graphics.glutils.ShaderProgram
import sge.math.{ Affine2, Matrix4 }
import sge.noop.{ NoopAudio, NoopGraphics, NoopInput }
import sge.files.{ FileHandle, FileType }
import sge.graphics.{ Pixmap, TextureData, TextureTarget }
import lowlevel.Nullable

class FontDrawGlyphWaveDRedSuite extends munit.FunSuite {

  // --- Headless Sge fixture --------------------------------------------------

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

  private object NoNet extends Net {
    import Net.*
    def httpClient:                                                                                         net.SgeHttpClient = net.SgeHttpClient.noop()
    def newServerSocket(protocol: Protocol, hostname: String, port: Int, hints: sge.net.ServerSocketHints): net.ServerSocket  = throw new UnsupportedOperationException
    def newServerSocket(protocol: Protocol, port:     Int, hints:   sge.net.ServerSocketHints):             net.ServerSocket  = throw new UnsupportedOperationException
    def newClientSocket(protocol: Protocol, host:     String, port: Int, hints: sge.net.SocketHints):       net.Socket        = throw new UnsupportedOperationException
    def openURI(URI:              String):                                                                  Boolean           = false
  }

  private object NoFiles extends Files {
    def getFileHandle(path: String, fileType: FileType): FileHandle = throw new UnsupportedOperationException
    def classpath(path:     String):                     FileHandle = throw new UnsupportedOperationException
    def internal(path:      String):                     FileHandle = throw new UnsupportedOperationException
    def external(path:      String):                     FileHandle = throw new UnsupportedOperationException
    def absolute(path:      String):                     FileHandle = throw new UnsupportedOperationException
    def local(path:         String):                     FileHandle = throw new UnsupportedOperationException
    def externalStoragePath:                             String     = ""
    def isExternalStorageAvailable:                      Boolean    = false
    def localStoragePath:                                String     = ""
    def isLocalStorageAvailable:                         Boolean    = false
  }

  private def makeSge(bbWidth: Int = 960, bbHeight: Int = 540): Sge =
    Sge(
      StubApplication,
      new NoopGraphics(noopWidth = bbWidth, noopHeight = bbHeight),
      new NoopAudio(),
      NoFiles,
      new NoopInput(),
      NoNet
    )

  /** Custom-type TextureData so `new Texture(...)` needs no GL (only width/height + a no-op consume). */
  final private class DummyTextureData(w: Int, h: Int) extends TextureData {
    def dataType:                                 TextureData.TextureDataType = TextureData.TextureDataType.Custom
    def isPrepared:                               Boolean                     = true
    def prepare():                                Unit                        = ()
    def consumePixmap():                          Pixmap                      = throw new UnsupportedOperationException("dummy texture has no pixmap")
    def disposePixmap:                            Boolean                     = false
    def consumeCustomData(target: TextureTarget): Unit                        = ()
    def width:                                    Int                         = w
    def height:                                   Int                         = h
    def getFormat:                                Pixmap.Format               = Pixmap.Format.RGBA8888
    def useMipMaps:                               Boolean                     = false
    def isManaged:                                Boolean                     = false
  }

  private def newTexture()(using Sge): Texture = new Texture(new DummyTextureData(64, 64))

  /** A single recorded quad: which texture it targeted and a copy of the 20 vertex floats. */
  final private case class Quad(texture: Texture, verts: Array[Float])

  /** Batch that records the vertex geometry drawGlyph emits and every setShader call. All other Batch surface is inert. */
  final private class RecordingBatch extends Batch {
    val quads:           ArrayBuffer[Quad]    = ArrayBuffer.empty
    val shaderSets:      ArrayBuffer[Boolean] = ArrayBuffer.empty // true == set to empty/default (paused)
    private val _color:  Color                = new Color(1f, 1f, 1f, 1f)
    private val _projMx: Matrix4              = new Matrix4() // identity: values(0) == values(5) == 1f
    private val _tranMx: Matrix4              = new Matrix4()

    @scala.annotation.publicInBinary
    private[sge] def begin(): Unit = ()
    @scala.annotation.publicInBinary
    private[sge] def end(): Unit = ()

    def color_=(tint:              Color):                               Unit  = ()
    def setColor(r:                Float, g: Float, b: Float, a: Float): Unit  = ()
    def color:                                                           Color = _color
    def packedColor_=(packedColor: Float):                               Unit  = ()
    def packedColor:                                                     Float = 0f

    def draw(
      texture:   Texture,
      x:         Float,
      y:         Float,
      originX:   Float,
      originY:   Float,
      width:     Float,
      height:    Float,
      scaleX:    Float,
      scaleY:    Float,
      rotation:  Float,
      srcX:      Int,
      srcY:      Int,
      srcWidth:  Int,
      srcHeight: Int,
      flipX:     Boolean,
      flipY:     Boolean
    ):                                                                                                                                                                       Unit = ()
    def draw(texture: Texture, x: Float, y: Float, width: Float, height: Float, srcX:   Int, srcY:      Int, srcWidth: Int, srcHeight: Int, flipX: Boolean, flipY: Boolean): Unit = ()
    def draw(texture: Texture, x: Float, y: Float, srcX:  Int, srcY:     Int, srcWidth: Int, srcHeight: Int):                                                                Unit = ()
    def draw(texture: Texture, x: Float, y: Float, width: Float, height: Float, u:      Float, v:       Float, u2:     Float, v2:      Float):                               Unit = ()
    def draw(texture: Texture, x: Float, y: Float):                                                                                                                          Unit = ()
    def draw(texture: Texture, x: Float, y: Float, width: Float, height: Float):                                                                                             Unit = ()
    // The core primitive drawGlyph/drawBlockSequence funnel through (Font.scala:2139).
    def draw(texture: Texture, spriteVertices: Array[Float], offset: Int, count: Int): Unit =
      quads += Quad(texture, spriteVertices.slice(offset, offset + count))
    def draw(region: TextureRegion, x: Float, y: Float):                                                                                                                                 Unit = ()
    def draw(region: TextureRegion, x: Float, y: Float, width:   Float, height:  Float):                                                                                                 Unit = ()
    def draw(region: TextureRegion, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float):                     Unit = ()
    def draw(region: TextureRegion, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float, clockwise: Boolean): Unit = ()
    def draw(region: TextureRegion, width: Float, height: Float, transform: Affine2): Unit = ()

    def flush():           Unit = ()
    def disableBlending(): Unit = ()
    def enableBlending():  Unit = ()

    def setBlendFunction(srcFunc:              Int, dstFunc:      Int):                                       Unit = ()
    def setBlendFunctionSeparate(srcFuncColor: Int, dstFuncColor: Int, srcFuncAlpha: Int, dstFuncAlpha: Int): Unit = ()
    def blendSrcFunc:                                                                                         Int  = 0
    def blendDstFunc:                                                                                         Int  = 0
    def blendSrcFuncAlpha:                                                                                    Int  = 0
    def blendDstFuncAlpha:                                                                                    Int  = 0

    def projectionMatrix:                                        Matrix4       = _projMx
    def transformMatrix:                                         Matrix4       = _tranMx
    def projectionMatrix_=(projection: Matrix4):                 Unit          = ()
    def transformMatrix_=(transform:   Matrix4):                 Unit          = ()
    def shader_=(shader:               Nullable[ShaderProgram]): Unit          = shaderSets += shader.isEmpty
    def shader:                                                  ShaderProgram = throw new IllegalStateException("RecordingBatch.shader must not be read by the drawGlyph paths under test")
    def blendingEnabled:                                         Boolean       = true
    def drawing:                                                 Boolean       = false

    def close(): Unit = ()
  }

  /** Builds a texture-backed GlyphRegion (offsetX not NaN so it takes the normal glyph path). */
  private def texturedGlyph(tex: Texture, w: Int, h: Int, xAdvance: Float): Font.GlyphRegion = {
    val gr = new Font.GlyphRegion(0f, 0f, xAdvance)
    gr.texture = tex
    gr.setRegion(0, 0, w, h)
    gr
  }

  // ==========================================================================
  // ISS-710 — JOSTLE offsets are never applied in drawGlyph
  // ==========================================================================

  test(
    "ISS-710: JOSTLE mode changes a glyph's drawn geometry (upstream Font.java:5593-5596); the port renders [?jostle] identically to plain text"
  ) {
    given Sge = makeSge()
    val tex   = newTexture()

    val font = new Font()
    font.mapping.put('A'.toInt, texturedGlyph(tex, 12, 16, 10f))
    font.cellWidth = 12f
    font.cellHeight = 16f
    font.originalCellHeight = 16f

    // Upstream applies a per-(x,y,char) offset, so at least one glyph position
    // must differ between plain and jostled rendering. Sweep several x
    // positions to be robust against the rare position whose offset is (0,0).
    var anyDifference = false
    var xi            = 0
    while (xi < 24 && !anyDifference) {
      val x = 5f + xi * 3f

      val plainBatch = new RecordingBatch
      font.drawGlyph(plainBatch, 'A'.toLong, x, 40f)

      val jostleBatch = new RecordingBatch
      font.drawGlyph(jostleBatch, 'A'.toLong | Font.JOSTLE, x, 40f)

      assertEquals(plainBatch.quads.size, 1, "a plain 'A' draws exactly one quad")
      assertEquals(jostleBatch.quads.size, 1, "a jostled 'A' draws exactly one quad")

      if (!plainBatch.quads.head.verts.sameElements(jostleBatch.quads.head.verts))
        anyDifference = true
      xi += 1
    }

    assert(
      anyDifference,
      "ISS-710: applying JOSTLE must shift the glyph's quad vertices for some position (upstream Font.java:5593-5596); the port never applies the offsets so plain and jostled are byte-identical everywhere"
    )
  }

  // ==========================================================================
  // ISS-711 — distance-field shader is never switched on a texture change
  // ==========================================================================

  test(
    "ISS-711: drawGlyph switches the distance-field shader when the glyph texture is not from a parent (upstream Font.java:5352-5363); the port never calls setShader"
  ) {
    given Sge = makeSge()
    val tex   = newTexture()

    val font = new Font()
    font.setDistanceField(Font.DistanceFieldType.SDF)
    font.mapping.put('A'.toInt, texturedGlyph(tex, 12, 16, 10f))
    font.cellWidth = 12f
    font.cellHeight = 16f
    font.originalCellHeight = 16f
    // parents is empty, so 'A's texture is NOT a parent texture: upstream
    // Font.java:5361-5362 -> pauseDistanceFieldShader(batch) -> batch.setShader(null).

    val batch = new RecordingBatch
    font.drawGlyph(batch, 'A'.toLong, 5f, 40f)

    assert(
      batch.shaderSets.nonEmpty,
      "ISS-711: drawing a distance-field glyph whose texture is not a parent must pause the shader via setShader (upstream Font.java:5362); the port's drawGlyph has no shader switch, so setShader is never called"
    )
  }

  // ==========================================================================
  // ISS-712 — box-drawing outline uses hardcoded PACKED_BLACK, not secondaryColor
  // ==========================================================================

  test(
    "ISS-712: a colored outline on a box-drawing glyph draws in the outline colour (upstream Font.java:5465 secondaryColor); the port hardcodes PACKED_BLACK (Font.scala:2579)"
  ) {
    given Sge = makeSge()
    val tex   = newTexture()

    val font = new Font()
    font.cellWidth = 12f
    font.cellHeight = 16f
    font.originalCellHeight = 16f
    // A box-drawing glyph is signalled by offsetX == NaN (Font.scala:2534).
    val boxGlyph = new Font.GlyphRegion(Float.NaN, 0f, 12f)
    font.mapping.put(0x2500, boxGlyph)
    // The solid block region is what drawBlockSequence actually renders; it needs a texture.
    font.mapping.put(font.solidBlock.toInt, texturedGlyph(tex, 4, 4, 4f))

    // char 0x2500, with the BLACK_OUTLINE bit (outline token active) AND the
    // BLUE_OUTLINE alternate mode -> secondaryColor == PACKED_BLUE upstream.
    val glyph = 0x2500L | Font.BLACK_OUTLINE | Font.BLUE_OUTLINE

    val batch = new RecordingBatch
    font.drawGlyph(batch, glyph, 5f, 40f)

    // batchAlpha == 1 (batch colour a == 1) so batchAlpha1_5 == 1.
    val expectedBlue  = sge.textra.utils.ColorUtils.multiplyAlpha(font.PACKED_BLUE, 1f)
    val expectedBlack = sge.textra.utils.ColorUtils.multiplyAlpha(font.PACKED_BLACK, 1f)
    assert(expectedBlue != expectedBlack, "sanity: the blue and black outline colours differ")

    val drawnColours = batch.quads.map(_.verts(2)).toSet // vertices(2) is the packed colour of each block quad
    assert(batch.quads.nonEmpty, "the box-drawing glyph must draw at least one block quad")

    assert(
      drawnColours.contains(expectedBlue),
      "ISS-712: the box-drawing outline must be drawn in the colour-outline's secondaryColor (PACKED_BLUE, upstream Font.java:5465); the port draws it in hardcoded PACKED_BLACK (Font.scala:2579), so the blue outline colour is absent"
    )
  }

  // ==========================================================================
  // ISS-713 — underline geometry uses a hardcoded 960x540 backbuffer
  // ==========================================================================

  /** Draws an underlined 'A' under a backbuffer of the given width and returns the underline quad's vertex geometry. */
  private def underlineQuad(bbWidth: Int): Array[Float] = {
    given Sge    = makeSge(bbWidth = bbWidth, bbHeight = 540)
    val glyphTex = newTexture()
    val underTex = newTexture() // distinct texture so we can identify the underline quad

    val font = new Font()
    font.cellWidth = 12f
    font.cellHeight = 16f
    font.originalCellHeight = 16f
    font.mapping.put('A'.toInt, texturedGlyph(glyphTex, 12, 16, 10f))
    // 0x2500 is intentionally NOT mapped, so the underline takes the '_'
    // fallback path (Font.scala:2866-2916), which is the branch that scales
    // by xPx (Font.scala:2878-2880) derived from the backbuffer width.
    font.mapping.put('_'.toInt, texturedGlyph(underTex, 8, 3, 8f))

    val batch = new RecordingBatch
    font.drawGlyph(batch, 'A'.toLong | Font.UNDERLINE, 5f, 40f)

    val underlineQuads = batch.quads.filter(_.texture eq underTex)
    assertEquals(
      underlineQuads.size,
      1,
      "the underlined 'A' must draw exactly one fallback-underline quad (with the '_' texture)"
    )
    underlineQuads.head.verts
  }

  test(
    "ISS-713: underline geometry responds to the real backbuffer size (upstream Font.java:5510-5511); the port hardcodes 960x540 (Font.scala:2601-2602) so it never changes"
  ) {
    val at960  = underlineQuad(960)
    val at1920 = underlineQuad(1920)

    assert(
      !at960.sameElements(at1920),
      "ISS-713: with a projection matrix held fixed, the underline segment geometry must differ between a 960-wide and a 1920-wide backbuffer (xPx = 2 / (backBufferWidth * projVal0), upstream Font.java:5510); the port computes xPx from a hardcoded 960 (Font.scala:2601), so both backbuffers yield byte-identical geometry"
    )
  }
}
