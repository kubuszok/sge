/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red test 2 for ISS-493 (SpriteCache port bugs) — bounce 1.
 *
 * The expected behaviour below is derived by hand-tracing the original
 * com/badlogic/gdx/graphics/g2d/SpriteCache.java (original-src/libgdx). Java
 * line numbers cited refer to that file:
 *   - Java's endCache() redefine branch (lines 230-233) leaves PERSISTENT
 *     buffer state behind: `position(0); limit(lastCache.offset +
 *     lastCache.maxCount)`. That limit is not just the upload extent of the
 *     redefined draw — it is what the NEXT beginCache() reads to place a new
 *     cache: `currentCache = new Cache(caches.size, verticesBuffer.limit())`
 *     (line 175), followed by `verticesBuffer.compact()` (line 177) which
 *     moves the write position to the old limit. So after redefining a
 *     NON-LAST cache, a subsequently defined NEW cache starts at
 *     lastCache.offset + lastCache.maxCount, never aliasing existing caches.
 *   - The port's analogue of that buffer state is `vertexIndex`. The redefine
 *     branch (SpriteCache.scala lines 238-245 at ba298c8d) fixes the upload
 *     extent but never RESTORES vertexIndex, leaving it at the redefined
 *     cache's extent (offset + cacheCount). With cache 0 = floats [0,30) and
 *     cache 1 = floats [30,60), redefining cache 0 leaves vertexIndex = 30,
 *     so the next beginCache() hands the new cache offset 30 instead of 60
 *     (aliasing cache 1), add() overwrites cache 1's vertices in place, and
 *     the new-cache endCache() uploads mesh.setVertices(vertices, 0,
 *     vertexIndex = 60) instead of the Java extent 90.
 *
 * Headless fixture (same as SpriteCacheRedSuite): SpriteCache is constructed
 * through its primary constructor with a directly-instantiated ShaderProgram
 * so that createDefaultShader()'s `if (!shader.compiled) throw` check is
 * bypassed (NoopGL20.glCreateShader returns 0, so no shader ever "compiles"
 * headlessly — ShaderProgram itself constructs fine and all its GL calls are
 * no-ops). Mesh/VertexBufferObject construction only calls glGenBuffer, and
 * the VBO uploads (glBufferData) happen exclusively inside
 * VertexBufferObject.bind (VertexBufferObject.scala lines 176-180), which
 * lets a recording GL20 observe the exact upload extent during draw().
 * Textures are dummy Custom-type TextureData instances — add(Texture,
 * Array[Float], Int, Int) uses the texture only as an identity key.
 *
 * This test is written by the reproducer agent and MUST NOT be modified by
 * the fixer: it encodes the original Java semantics, not the port's.
 */
package sge
package graphics
package g2d

import sge.graphics.glutils.ShaderProgram
import sge.noop.{ NoopGL20, NoopGraphics }

class SpriteCacheRedefineRedSuite extends munit.FunSuite {

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

  // --- ISS-493 bounce 1: vertexIndex not restored after redefining a non-last cache ---

  test(
    "ISS-493: a NEW cache defined after redefining a non-last cache must start at the full extent, not alias the last cache (Java lines 175, 177, 230-233)"
  ) {
    // Java's redefine branch leaves the verticesBuffer at `position(0);
    // limit(lastCache.offset + lastCache.maxCount)` (lines 230-233). The next
    // beginCache() reads that limit as the new cache's offset (line 175:
    // `new Cache(caches.size, verticesBuffer.limit())`) and compact() (line
    // 177) moves the write position there, so with cache 0 = floats [0,30)
    // and cache 1 = floats [30,60), a new cache defined AFTER redefining
    // cache 0 must live at floats [60,90): add() appends behind cache 1 and
    // the new-cache endCache() flip() (line 211) sets the upload extent to
    // 90 floats.
    //
    // The port (SpriteCache.scala lines 238-245 at ba298c8d) never restores
    // vertexIndex in the redefine branch, leaving it at the redefined cache's
    // extent (30). The next beginCache() then hands the new cache offset 30
    // — aliasing cache 1 — add() overwrites cache 1's vertices in place, and
    // endCache() uploads only vertexIndex = 60 floats instead of 90.
    //
    // Observable: VertexBufferObject.bind uploads exactly the buffer extent
    // via glBufferData (VertexBufferObject.scala lines 176-180); the
    // recording GL20 below captures it during draw(2). The Cache objects
    // (and their offsets) are not publicly reachable (SpriteCache.caches is
    // private), so the upload extent + per-region content assertions pin the
    // bug.
    val recordingGL = new BufferDataRecordingGL20
    given Sge       = makeSge(recordingGL)
    val cache       = makeCache()
    val texture     = dummyTexture()

    val vertsA = quad(1000f) // cache 0's original content
    val vertsB = quad(2000f) // cache 1's content, must survive everything below
    val vertsC = quad(3000f) // cache 0's replacement content
    val vertsD = quad(4000f) // the NEW cache 2's content, must land at floats [60,90)

    cache.beginCache()
    cache.add(texture, vertsA, 0, 30)
    assertEquals(cache.endCache(), 0)

    cache.beginCache()
    cache.add(texture, vertsB, 0, 30)
    assertEquals(cache.endCache(), 1)

    // Redefine cache 0 (NOT the last cache) with the same image count.
    cache.beginCache(0)
    cache.add(texture, vertsC, 0, 30)
    assertEquals(cache.endCache(), 0)

    // Define a brand-new cache. Java: offset = verticesBuffer.limit() = 60
    // (lines 175, 230-233). Port at ba298c8d: offset = vertexIndex = 30.
    cache.beginCache()
    cache.add(texture, vertsD, 0, 30)
    assertEquals(cache.endCache(), 2, "the new cache defined after the redefinition must get cache ID 2")

    cache.begin()
    try cache.draw(2)
    finally cache.end()

    assert(recordingGL.arrayBufferUploads.nonEmpty, "draw(2) must upload the vertex buffer via glBufferData")
    val upload = recordingGL.arrayBufferUploads.last
    assertEquals(
      upload.length,
      90,
      "mesh upload must cover lastCache.offset + lastCache.maxCount = 90 floats after defining a new cache behind the redefined one (Java lines 175/177 place cache 2 at offset 60, line 211 flips to limit 90), not 60"
    )
    assertEquals(
      upload.slice(30, 60).toSeq,
      vertsB.toSeq,
      "cache 1's vertices (floats [30,60)) must survive the definition of the new cache 2 (Java line 175: cache 2 starts at the buffer limit 60, so add() never touches cache 1's region)"
    )
    assertEquals(
      upload.slice(60, 90).toSeq,
      vertsD.toSeq,
      "the new cache 2's content must land at floats [60,90) (Java lines 175/177: offset = verticesBuffer.limit() = 60 after the redefine's limit(lastCache.offset + lastCache.maxCount))"
    )
    assertEquals(
      upload.slice(0, 30).toSeq,
      vertsC.toSeq,
      "the redefined cache 0 content must remain patched in at floats [0,30) (Java lines 194/264)"
    )
  }
}
