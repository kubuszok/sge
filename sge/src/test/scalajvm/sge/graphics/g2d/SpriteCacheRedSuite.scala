/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red tests for ISS-493 (SpriteCache port bugs).
 *
 * Every expected behaviour below is derived by hand-tracing the original
 * com/badlogic/gdx/graphics/g2d/SpriteCache.java (original-src/libgdx). Java
 * line numbers cited in the test comments refer to that file:
 *   - begin() guards: lines 852-854 — `if (drawing) throw ... "end must be
 *     called before begin."` (853) and `if (currentCache != null) throw ...
 *     "endCache must be called before begin"` (854). begin() must throw ONLY
 *     while a cache definition is still open; after endCache() set
 *     currentCache = null (line 236) begin() must succeed. The port inverts
 *     the second guard (SpriteCache.scala line 874 throws when
 *     `currentCache.isEmpty`), so begin() ALWAYS throws after a correct
 *     beginCache/add/endCache sequence and never throws during an open
 *     definition.
 *   - endCache() redefine branch: lines 230-233 — Java patches the vertex
 *     buffer in place (add() puts at the buffer position set by
 *     beginCache(int), line 264 / 194) and then restores the FULL extent:
 *     `position(0); limit(lastCache.offset + lastCache.maxCount)`. The port
 *     instead calls mesh.setVertices(vertices, 0, vertexIndex) with
 *     vertexIndex == cache.offset + cacheCount (SpriteCache.scala line 239),
 *     shrinking the mesh buffer and truncating every cache defined after the
 *     redefined one.
 *
 * Headless fixture: SpriteCache is constructed through its primary
 * constructor with a directly-instantiated ShaderProgram so that
 * createDefaultShader()'s `if (!shader.compiled) throw` check is bypassed
 * (NoopGL20.glCreateShader returns 0, so no shader ever "compiles" headlessly
 * — ShaderProgram itself constructs fine and all its GL calls are no-ops).
 * Mesh/VertexBufferObject construction only calls glGenBuffer, and the VBO
 * uploads (glBufferData) happen exclusively inside VertexBufferObject.bind
 * (VertexBufferObject.scala lines 176-180), which lets a recording GL20
 * observe the exact upload extent during draw(). Textures are dummy
 * Custom-type TextureData instances — add(Texture, Array[Float], Int, Int)
 * uses the texture only as an identity key.
 *
 * These tests are written by the reproducer agent and MUST NOT be modified by
 * the fixer: they encode the original Java semantics, not the port's.
 */
package sge
package graphics
package g2d

import sge.graphics.glutils.ShaderProgram
import sge.noop.{ NoopGL20, NoopGraphics }

class SpriteCacheRedSuite extends munit.FunSuite {

  // --- Headless fixture ------------------------------------------------------

  private def makeSge(glImpl: GL20): Sge =
    SgeTestFixture.testSge(graphics = new NoopGraphics() {
      override def gl20: GL20 = glImpl
    })

  /** Custom-type TextureData: Texture.load() goes straight to consumeCustomData (GLTexture.uploadImageData, Custom branch), never touching Pixmap/gdx2d. Unmanaged, so the texture is not registered
    * with the managed-textures map.
    */
  final private class DummyTextureData extends TextureData {
    def dataType:                                 TextureData.TextureDataType = TextureData.TextureDataType.Custom
    def isPrepared:                               Boolean                     = true
    def prepare():                                Unit                        = ()
    def consumePixmap():                          Pixmap                      = throw new UnsupportedOperationException("dummy texture has no pixmap")
    def disposePixmap:                            Boolean                     = false
    def consumeCustomData(target: TextureTarget): Unit                        = ()
    def width:                                    Int                         = 4
    def height:                                   Int                         = 4
    def getFormat:                                Pixmap.Format               = Pixmap.Format.RGBA8888
    def useMipMaps:                               Boolean                     = false
    def isManaged:                                Boolean                     = false
  }

  private def dummyTexture()(using Sge): Texture = new Texture(new DummyTextureData)

  /** Non-indexed SpriteCache (useIndices = false, like Java's `new SpriteCache()`): 6 vertices x 5 floats = 30 floats per image. size = 4 images -> 120-float vertex store. */
  private def makeCache()(using Sge): SpriteCache =
    new SpriteCache(4, new ShaderProgram("void main(){}", "void main(){}"), false)

  /** One image worth of vertex data (30 floats) with recognizable values. */
  private def quad(base: Float): Array[Float] = Array.tabulate(30)(i => base + i.toFloat)

  /** Records the float content of every GL_ARRAY_BUFFER glBufferData upload. VertexBufferObject.bind uploads exactly `_buffer.limit() * 4` bytes (VertexBufferObject.scala lines 177-178), so the
    * recorded length IS the mesh's current vertex extent in floats.
    */
  final private class BufferDataRecordingGL20 extends GL20 {
    val arrayBufferUploads: scala.collection.mutable.ListBuffer[Array[Float]] = scala.collection.mutable.ListBuffer.empty

    private val underlying: GL20 = NoopGL20
    export underlying.{ glBufferData as _, * }

    def glBufferData(target: BufferTarget, size: Int, data: java.nio.Buffer, usage: BufferUsage): Unit =
      if (target == BufferTarget.ArrayBuffer) {
        val floats = data match {
          case bb: java.nio.ByteBuffer =>
            // duplicate() resets the byte order to BIG_ENDIAN; the VBO's buffer is native-ordered.
            val view = bb.duplicate().order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer()
            val out  = new Array[Float](size / 4)
            view.get(out)
            out
          case _ => Array.empty[Float]
        }
        arrayBufferUploads += floats
      }
  }

  // --- ISS-493 symptom 1: begin() state guard inverted ------------------------

  test("ISS-493: begin() after a completed beginCache/add/endCache cycle must not throw") {
    // Java begin() (line 854) throws "endCache must be called before begin"
    // only when currentCache != null, i.e. while a cache definition is still
    // OPEN. endCache() sets currentCache = null (line 236), so after a correct
    // definition sequence begin() must succeed. The port (SpriteCache.scala
    // line 874) throws when currentCache.isEmpty — the exact inversion — so
    // this begin() call currently throws IllegalStateException("endCache must
    // be called before begin") and the class is unusable for drawing (the
    // rendering {} bracket wraps begin()/end(), so it is un-bypassable).
    given Sge = makeSge(NoopGL20)
    val cache = makeCache()
    cache.beginCache()
    cache.add(dummyTexture(), quad(100f), 0, 30)
    assertEquals(cache.endCache(), 0, "first endCache must return cache ID 0")

    cache.begin() // must NOT throw (Java line 854: currentCache is null here)
    assert(cache.drawing, "begin() must put the SpriteCache into the drawing state (Java line 873)")
    cache.end()
    assert(!cache.drawing, "end() must leave the drawing state (Java line 879)")
  }

  test("ISS-493: begin() during an open cache definition must throw IllegalStateException (Java line 854)") {
    // Java: beginCache() sets currentCache (line 175), so a begin() before the
    // matching endCache() hits `if (currentCache != null) throw new
    // IllegalStateException("endCache must be called before begin")`. The
    // port's inverted guard (currentCache.isEmpty) lets begin() through here —
    // exactly the situation the guard exists to reject.
    given Sge = makeSge(NoopGL20)
    val cache = makeCache()
    cache.beginCache()
    val e = intercept[IllegalStateException](cache.begin())
    assert(
      e.getMessage.contains("endCache must be called before begin"),
      s"begin() during cache definition must cite the endCache guard (Java line 854), got: ${e.getMessage}"
    )
  }

  test("ISS-493: begin() called twice without end() must throw 'end must be called before begin.' (Java line 853)") {
    // Java: the first begin() succeeds (currentCache is null after endCache),
    // sets drawing = true (line 873); the second begin() trips the drawing
    // guard (line 853). On the current port the FIRST begin() already throws
    // ("endCache must be called before begin") because of the inverted
    // currentCache guard — symptom 1 of this issue.
    given Sge = makeSge(NoopGL20)
    val cache = makeCache()
    cache.beginCache()
    cache.add(dummyTexture(), quad(100f), 0, 30)
    cache.endCache()

    cache.begin() // must NOT throw — currently throws here (inverted guard)
    val e = intercept[IllegalStateException](cache.begin())
    assert(
      e.getMessage.contains("end must be called before begin"),
      s"second begin() must cite the drawing guard (Java line 853), got: ${e.getMessage}"
    )
    cache.end()
  }

  // --- ISS-493 symptom 2: redefining a non-last cache truncates the mesh ------

  test("ISS-493: redefining cache 0 of 2 must keep cache 1's vertices in the mesh (Java lines 230-233)") {
    // Java endCache() redefine branch never shrinks the buffer: add() patched
    // the data in place at position cache.offset (lines 194, 264) and endCache
    // restores the full extent `limit(lastCache.offset + lastCache.maxCount)`
    // (lines 230-233). With cache 0 = 30 floats at offset 0 and cache 1 = 30
    // floats at offset 30, the mesh extent after redefining cache 0 must still
    // be 60 floats: [0,30) = the redefined data, [30,60) = cache 1's original
    // data. The port's endCache calls mesh.setVertices(vertices, 0,
    // vertexIndex) with vertexIndex = 30 (SpriteCache.scala lines 184-189,
    // 239), shrinking the VBO to 30 floats and cutting cache 1 off.
    //
    // Observable: VertexBufferObject.bind uploads exactly the buffer extent
    // via glBufferData (VertexBufferObject.scala lines 176-180); the recording
    // GL20 below captures it during draw(1). NOTE: on the current port the
    // begin() call already throws IllegalStateException("endCache must be
    // called before begin") — symptom 1 of this same issue; once the guard is
    // fixed, the upload-extent assertions below pin the truncation bug
    // independently.
    val recordingGL = new BufferDataRecordingGL20
    given Sge       = makeSge(recordingGL)
    val cache       = makeCache()
    val texture     = dummyTexture()

    val vertsB = quad(2000f) // cache 1's content, must survive the redefinition
    val vertsC = quad(3000f) // cache 0's replacement content

    cache.beginCache()
    cache.add(texture, quad(1000f), 0, 30)
    assertEquals(cache.endCache(), 0)

    cache.beginCache()
    cache.add(texture, vertsB, 0, 30)
    assertEquals(cache.endCache(), 1)

    // Redefine cache 0 (NOT the last cache) with the same image count.
    cache.beginCache(0)
    cache.add(texture, vertsC, 0, 30)
    assertEquals(cache.endCache(), 0)

    cache.begin() // must NOT throw (Java line 854) — see symptom 1
    try cache.draw(1)
    finally cache.end()

    assert(recordingGL.arrayBufferUploads.nonEmpty, "draw(1) must upload the vertex buffer via glBufferData")
    val upload = recordingGL.arrayBufferUploads.last
    assertEquals(
      upload.length,
      60,
      "mesh upload must cover lastCache.offset + lastCache.maxCount = 60 floats after redefining a non-last cache (Java lines 230-233), not shrink to the redefined cache's extent"
    )
    assertEquals(
      upload.slice(30, 60).toSeq,
      vertsB.toSeq,
      "cache 1's vertices (floats [30,60)) must survive the redefinition of cache 0 (Java patches in place, lines 194/264, and never shrinks the buffer)"
    )
    assertEquals(
      upload.slice(0, 30).toSeq,
      vertsC.toSeq,
      "the redefined cache 0 content must be patched in at floats [0,30) (Java lines 194/264)"
    )
  }
}
