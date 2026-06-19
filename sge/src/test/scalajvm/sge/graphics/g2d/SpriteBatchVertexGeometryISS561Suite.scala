/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Coverage tests for ISS-561 (batch F): plain SpriteBatch per-vertex
 * geometry. SpriteBatch had ZERO tests; PolygonSpriteBatch and SpriteCache
 * already have suites. This suite pins the PURE MATH that SpriteBatch.draw(...)
 * writes into its `vertices` float array: the 4 vertices per sprite, each
 * SPRITE_SIZE/4 = 5 floats wide (x, y, packed-color, u, v), 20 floats/sprite.
 *
 * Every expected value below is hand-derived from the original
 * com/badlogic/gdx/graphics/g2d/SpriteBatch.java (original-src/libgdx). Cited
 * Java line numbers refer to that file.
 *
 * Vertex layout per sprite (Java draw(Texture,x,y,width,height) lines 421-457,
 * mirrored by sge SpriteBatch.scala draw(...) lines 490-513):
 *   v0 @ idx+0..4 : (x1, y1, color, u,  v )
 *   v1 @ idx+5..9 : (x2, y2, color, u,  v2)
 *   v2 @ idx+10..14: (x3, y3, color, u2, v2)
 *   v3 @ idx+15..19: (x4, y4, color, u2, v )
 * idx advances by SPRITE_SIZE = 20.
 *
 * SEAM. SpriteBatch exposes `vertices: Array[Float]` (a public final val,
 * SpriteBatch.scala line 59) and `idx: Int` (public var, line 60). These ARE
 * the production output: flush() (line 890) just hands `vertices(0 until idx)`
 * to `mesh.setVertices` and renders — it transforms nothing. Reading them
 * directly after a draw (without flushing) captures exactly the float geometry
 * SpriteBatch produced, with no GL/mesh round-trip needed. This is the
 * cleanest seam: the math under test lives entirely in draw(...). The
 * flush-on-capacity test below ALSO exercises the upload path by observing
 * `idx` reset to 0 after a capacity-forced flush (lines 203-204, 918).
 *
 * Headless fixture mirrors PolygonSpriteBatchRedSuite: a directly-instantiated
 * ShaderProgram bypasses createDefaultShader()'s `if (!shader.compiled) throw`
 * check (NoopGL20.glCreateShader returns 0 so nothing "compiles" headlessly,
 * but ShaderProgram constructs fine and all its GL calls are no-ops). The
 * dummy texture is a Custom-type TextureData (Texture.load -> consumeCustomData,
 * no Pixmap), used only as an identity key + no-op bind. Its width/height = 4,
 * so switchTexture (line 963) sets invTexWidth = invTexHeight = 1/4 = 0.25f.
 */
package sge
package graphics
package g2d

import sge.graphics.glutils.ShaderProgram
import sge.noop.{ NoopGL20, NoopGraphics }

class SpriteBatchVertexGeometryISS561Suite extends munit.FunSuite {

  // --- Headless fixture ------------------------------------------------------

  /** NoopGL20 hands out buffer handle 0 (glGenBuffer == 0), which trips IndexBufferObject.bind's `if bufferHandle == 0 throw "No buffer allocated!"` in the SpriteBatch constructor's pre-bind step
    * (SpriteBatch.scala lines 130-134, taken because gl30 is empty -> VertexBufferObject, not VertexArray). Returning a nonzero handle lets the no-op bind/unbind succeed. Every other call delegates
    * to NoopGL20.
    */
  private object BufferGL20 extends GL20 {
    private val underlying: GL20 = NoopGL20
    export underlying.{ glGenBuffer as _, * }
    def glGenBuffer(): Int = 1
  }

  private def makeSge(): Sge =
    SgeTestFixture.testSge(graphics = new NoopGraphics() {
      override def gl20: GL20 = BufferGL20
    })

  /** Custom-type TextureData (same trick as PolygonSpriteBatchRedSuite): width/height = 4 so invTexWidth/invTexHeight = 0.25 after switchTexture. */
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

  private def dummyTexture(w: Int = 4, h: Int = 4)(using Sge): Texture = new Texture(new DummyTextureData(w, h))

  private def makeBatch(size: Int = 1000)(using Sge): SpriteBatch =
    new SpriteBatch(size, new ShaderProgram("void main(){}", "void main(){}"))

  /** Reads the 5 floats of vertex `v` (0..3) of the sprite stored at vertex-array offset `spriteIdx`. */
  private def vertex(batch: SpriteBatch, spriteIdx: Int, v: Int): (Float, Float, Float, Float, Float) = {
    val base = spriteIdx + v * 5
    val a    = batch.vertices
    (a(base), a(base + 1), a(base + 2), a(base + 3), a(base + 4))
  }

  // --- draw(texture, x, y, width, height): positions + default UVs -----------

  test("ISS561: draw(texture, x, y, w, h) writes the 4 corner positions and UVs at exact offsets") {
    // Java draw(Texture,x,y,width,height) lines 421-457: fx2 = x+width (445),
    // fy2 = y+height (446), u=0 v=1 u2=1 v2=0 (447-450). Corners (Java
    // 451-455, sge 490-513): v0=(x,y), v1=(x,fy2), v2=(fx2,fy2), v3=(fx2,y);
    // UVs v0=(u,v)=(0,1), v1=(u,v2)=(0,0), v2=(u2,v2)=(1,0), v3=(u2,v)=(1,1).
    given Sge   = makeSge()
    val batch   = makeBatch()
    val texture = dummyTexture()

    batch.begin()
    batch.draw(texture, 10f, 20f, 30f, 40f) // x=10 y=20 w=30 h=40 -> fx2=40 fy2=60
    // do NOT end()/flush(): read the raw vertices SpriteBatch produced.

    assertEquals(batch.idx, 20, "one sprite advances idx by SPRITE_SIZE = 20")

    // v0 bottom-left: (x, y, color, u=0, v=1)
    assertEquals(vertex(batch, 0, 0), (10f, 20f, Color.WHITE_FLOAT_BITS, 0f, 1f), "v0 = (x, y, white-bits, u=0, v=1)")
    // v1 top-left: (x, fy2, color, u=0, v2=0)
    assertEquals(vertex(batch, 0, 1), (10f, 60f, Color.WHITE_FLOAT_BITS, 0f, 0f), "v1 = (x, y+h, white-bits, u=0, v2=0)")
    // v2 top-right: (fx2, fy2, color, u2=1, v2=0)
    assertEquals(vertex(batch, 0, 2), (40f, 60f, Color.WHITE_FLOAT_BITS, 1f, 0f), "v2 = (x+w, y+h, white-bits, u2=1, v2=0)")
    // v3 bottom-right: (fx2, y, color, u2=1, v=1)
    assertEquals(vertex(batch, 0, 3), (40f, 20f, Color.WHITE_FLOAT_BITS, 1f, 1f), "v3 = (x+w, y, white-bits, u2=1, v=1)")

    // Pin the raw float offsets so a swapped corner or off-by-one layout fails.
    // (Mutation: writing fy2 at idx+1 instead of y, i.e. mixing up v0/v1's y,
    //  flips this; writing u2 at idx+3 swaps the U of the left edge.)
    assertEqualsFloat(batch.vertices(0), 10f, 0f, "vertices(0) = x1")
    assertEqualsFloat(batch.vertices(1), 20f, 0f, "vertices(1) = y1")
    assertEqualsFloat(batch.vertices(3), 0f, 0f, "vertices(3) = u  (left edge)")
    assertEqualsFloat(batch.vertices(4), 1f, 0f, "vertices(4) = v  (bottom edge)")
    assertEqualsFloat(batch.vertices(6), 60f, 0f, "vertices(6) = y2 = y+h (top-left corner)")
    assertEqualsFloat(batch.vertices(9), 0f, 0f, "vertices(9) = v2 (top edge)")
    assertEqualsFloat(batch.vertices(10), 40f, 0f, "vertices(10) = x3 = x+w")
    assertEqualsFloat(batch.vertices(13), 1f, 0f, "vertices(13) = u2 (right edge)")
    assertEqualsFloat(batch.vertices(18), 1f, 0f, "vertices(18) = u2 (right edge, v3)")
    assertEqualsFloat(batch.vertices(19), 1f, 0f, "vertices(19) = v (bottom edge, v3)")

    batch.close()
  }

  // --- draw(texture, x, y, srcX, srcY, srcW, srcH): UVs from src + invTex -----

  test("ISS561: draw(texture, x, y, srcX, srcY, srcW, srcH) derives UVs from src rect * invTex (0.25)") {
    // Java draw(Texture,x,y,srcX,srcY,srcWidth,srcHeight) (the 7-arg overload):
    //   u  = srcX * invTexWidth              v  = (srcY+srcHeight) * invTexHeight
    //   u2 = (srcX+srcWidth) * invTexWidth   v2 = srcY * invTexHeight
    //   fx2 = x + srcWidth                   fy2 = y + srcHeight
    // (sge SpriteBatch.scala lines 392-397). Texture is 4x4 so invTex = 0.25.
    // srcX=1 srcY=2 srcWidth=2 srcHeight=1:
    //   u  = 1*0.25 = 0.25   v  = (2+1)*0.25 = 0.75
    //   u2 = (1+2)*0.25 = 0.75   v2 = 2*0.25 = 0.5
    //   fx2 = 5 + 2 = 7      fy2 = 6 + 1 = 7
    given Sge   = makeSge()
    val batch   = makeBatch()
    val texture = dummyTexture(4, 4)

    batch.begin()
    batch.draw(texture, 5f, 6f, 1, 2, 2, 1)

    assertEquals(batch.idx, 20)
    assertEquals(vertex(batch, 0, 0), (5f, 6f, Color.WHITE_FLOAT_BITS, 0.25f, 0.75f), "v0 = (x, y, c, u=srcX*inv, v=(srcY+srcH)*inv)")
    assertEquals(vertex(batch, 0, 1), (5f, 7f, Color.WHITE_FLOAT_BITS, 0.25f, 0.5f), "v1 = (x, fy2, c, u, v2=srcY*inv)")
    assertEquals(vertex(batch, 0, 2), (7f, 7f, Color.WHITE_FLOAT_BITS, 0.75f, 0.5f), "v2 = (fx2, fy2, c, u2, v2)")
    assertEquals(vertex(batch, 0, 3), (7f, 6f, Color.WHITE_FLOAT_BITS, 0.75f, 0.75f), "v3 = (fx2, y, c, u2, v)")

    batch.close()
  }

  // --- draw(TextureRegion, x, y, w, h): UVs come from the region -------------

  test("ISS561: draw(region, x, y, w, h) maps region UVs into the corners (u, v2, u2, v swap vs raw)") {
    // Java draw(TextureRegion,x,y,width,height) lines 549-593:
    //   u = region.u   v = region.v2   u2 = region.u2   v2 = region.v
    // i.e. the region's v/v2 are intentionally SWAPPED into the batch's v/v2
    // because the region stores v at top, v2 at bottom. Region built with the
    // explicit (texture, u, v, u2, v2) ctor to pin exact non-trivial UVs and
    // avoid the 1x1-region center adjustment (TextureRegion.scala 128-135).
    given Sge   = makeSge()
    val batch   = makeBatch()
    val texture = dummyTexture(4, 4)
    val region  = new TextureRegion(texture, 0.1f, 0.2f, 0.7f, 0.8f) // u=.1 v=.2 u2=.7 v2=.8

    batch.begin()
    batch.draw(region, 3f, 4f, 5f, 6f) // fx2 = 8, fy2 = 10
    // batch-side: u=region.u=.1, v=region.v2=.8, u2=region.u2=.7, v2=region.v=.2

    assertEquals(batch.idx, 20)
    assertEquals(vertex(batch, 0, 0), (3f, 4f, Color.WHITE_FLOAT_BITS, 0.1f, 0.8f), "v0 = (x, y, c, region.u, region.v2)")
    assertEquals(vertex(batch, 0, 1), (3f, 10f, Color.WHITE_FLOAT_BITS, 0.1f, 0.2f), "v1 = (x, fy2, c, region.u, region.v)")
    assertEquals(vertex(batch, 0, 2), (8f, 10f, Color.WHITE_FLOAT_BITS, 0.7f, 0.2f), "v2 = (fx2, fy2, c, region.u2, region.v)")
    assertEquals(vertex(batch, 0, 3), (8f, 4f, Color.WHITE_FLOAT_BITS, 0.7f, 0.8f), "v3 = (fx2, y, c, region.u2, region.v2)")

    batch.close()
  }

  // --- setColor then draw: packed color in every vertex ----------------------

  test("ISS561: setColor packs Color.toFloatBits into the color slot of all 4 vertices") {
    // SpriteBatch.setColor (lines 163-166) sets colorPacked = _color.toFloatBits();
    // draw writes `color = this.colorPacked` at idx+2, +7, +12, +17 of the sprite.
    // r=1 g=0.5 b=0.25 a=1 -> exact bytes: a=255, b=round(0.25*255)=63,
    // g=round(0.5*255)=127, r=255. The toFloatBits encoding is the SAME
    // production call (Color.toFloatBits, Color.scala 335-338); asserting equality
    // to an independent Color instance's bits pins the color slot without
    // hard-coding NumberUtils internals.
    given Sge   = makeSge()
    val batch   = makeBatch()
    val texture = dummyTexture()

    val expectedBits = new Color(1f, 0.5f, 0.25f, 1f).toFloatBits()

    batch.begin()
    batch.setColor(1f, 0.5f, 0.25f, 1f)
    batch.draw(texture, 0f, 0f, 1f, 1f)

    assertEquals(batch.colorPacked, expectedBits, "colorPacked == Color(r,g,b,a).toFloatBits()")
    // color slot of every vertex (idx + 2/7/12/17).
    assertEquals(batch.vertices(2), expectedBits, "v0 color slot")
    assertEquals(batch.vertices(7), expectedBits, "v1 color slot")
    assertEquals(batch.vertices(12), expectedBits, "v2 color slot")
    assertEquals(batch.vertices(17), expectedBits, "v3 color slot")
    // and it must NOT be the default white bits (proves setColor took effect).
    assertNotEquals(batch.vertices(2), Color.WHITE_FLOAT_BITS, "non-white color reached the vertex")

    batch.close()
  }

  // --- accumulation within capacity, and capacity-forced flush ---------------

  test("ISS561: two draws within capacity accumulate 2*20 floats; the second sprite occupies offsets 20..39") {
    // No flush between draws (same texture, idx < length): each draw advances
    // idx by 20 and writes into the SAME vertices array at the running offset
    // (SpriteBatch.scala line 322 etc: this.idx = localIdx + 20).
    given Sge   = makeSge()
    val batch   = makeBatch()
    val texture = dummyTexture()

    batch.begin()
    batch.draw(texture, 0f, 0f, 1f, 1f)
    assertEquals(batch.idx, 20, "first draw -> idx 20")
    batch.draw(texture, 100f, 200f, 10f, 20f) // second sprite at offset 20, fx2=110 fy2=220
    assertEquals(batch.idx, 40, "second draw within capacity -> idx 40 (no flush)")

    // Second sprite's v0 lives at offsets 20..24: (x=100, y=200, color, u=0, v=1).
    assertEquals(vertex(batch, 20, 0), (100f, 200f, Color.WHITE_FLOAT_BITS, 0f, 1f), "sprite#2 v0 at offset 20")
    // v2 (top-right) at offsets 30..34: (fx2=110, fy2=220, color, u2=1, v2=0).
    assertEquals(vertex(batch, 20, 2), (110f, 220f, Color.WHITE_FLOAT_BITS, 1f, 0f), "sprite#2 v2 at offset 30")
    // First sprite still intact at offset 0.
    assertEquals(vertex(batch, 0, 0), (0f, 0f, Color.WHITE_FLOAT_BITS, 0f, 1f), "sprite#1 v0 untouched at offset 0")

    batch.close()
  }

  test("ISS561: a draw at full capacity (idx == vertices.length) forces a flush, resetting idx") {
    // Capacity = size * SPRITE_SIZE. With size=1, vertices.length = 20: the
    // first draw fills it (idx -> 20 == length). The SECOND draw with the SAME
    // texture hits the `else if (idx == vertices.length) flush()` branch
    // (SpriteBatch.scala lines 203-204), which resets idx to 0 (flush, line 918)
    // before writing the new sprite -> idx ends at 20, not 40.
    given Sge   = makeSge()
    val batch   = makeBatch(size = 1)
    val texture = dummyTexture()

    assertEquals(batch.vertices.length, 20, "size=1 -> 20-float vertex store (one sprite)")

    batch.begin()
    batch.draw(texture, 1f, 2f, 3f, 4f) // switchTexture flushes empty, then writes -> idx 20
    assertEquals(batch.idx, 20, "store is now full")
    batch.draw(texture, 5f, 6f, 7f, 8f) // idx == length -> flush() resets to 0, then write -> idx 20
    assertEquals(batch.idx, 20, "capacity-forced flush reset idx to 0 before the second sprite")

    // The store now holds ONLY the second sprite (the first was flushed out).
    assertEquals(vertex(batch, 0, 0), (5f, 6f, Color.WHITE_FLOAT_BITS, 0f, 1f), "second sprite is now at offset 0 after the forced flush")

    batch.close()
  }
}
