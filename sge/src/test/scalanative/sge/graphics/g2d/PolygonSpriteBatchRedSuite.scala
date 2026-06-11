/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red tests for ISS-496 (PolygonSpriteBatch.draw(texture, spriteVertices,
 * offset, count) multi-batch flush corruption).
 *
 * Every expected behaviour below is derived by hand-tracing the original
 * com/badlogic/gdx/graphics/g2d/PolygonSpriteBatch.java (original-src/libgdx).
 * Java line numbers cited in the test comments refer to that file, in the
 * draw(Texture, float[], int, int) overload (lines 744-789):
 *   - line 766-773: the triangle-index fill loop runs once, for the FIRST
 *     batch's triangleCount, leaving the local `triangleIndex` at
 *     initial + triangleCount (the quad pattern v,v+1,v+2,v+2,v+3,v is
 *     written for every quad of a full batch).
 *   - line 778: each iteration of the upload loop assigns the LOCAL
 *     `triangleIndex` to the field: `this.triangleIndex = triangleIndex;`.
 *   - lines 784-787: after a mid-call flush, when only a partial batch
 *     remains (`batch > count`), Java recomputes BOTH locals:
 *     `batch = Math.min(count, triangles.length / 6 * SPRITE_SIZE)` (785) and
 *     `triangleIndex = batch / SPRITE_SIZE * 6` (786) — so the next loop
 *     iteration's line-778 assignment gives the FINAL partial batch a
 *     triangle count proportional to the vertices it actually uploads.
 *   The port (sge/src/main/scala/sge/graphics/g2d/PolygonSpriteBatch.scala,
 *   lines 781-801) keeps assigning `this.triangleIndex = ti` (line 787) where
 *   `ti` is frozen at the FIRST batch's full triangle count; the recompute at
 *   line 797 writes the dead local `triangleIdx` instead. The final partial
 *   flush therefore renders the full first-batch index count — indices that
 *   reference vertices never uploaded in that flush.
 *
 * Observable: flush() (PolygonSpriteBatch.scala lines 1210-1232) does
 * `mesh.setIndices(triangles.slice(0, trianglesInBatch))` (line 1220) and
 * `mesh.render(shader, Triangles, 0, trianglesInBatch)` (line 1228) with
 * trianglesInBatch = this.triangleIndex. Under the headless fixture the Mesh
 * uses VertexDataType.VertexArray (NoopGraphics.gl30 is empty), so render
 * issues `glDrawElements(primitiveType, count, UnsignedShort, buffer)`
 * (Mesh.scala line 656) with count = trianglesInBatch and buffer = the index
 * ShortBuffer whose first `count` entries are exactly the indices drawn. A
 * recording GL20 below captures (count, index values) per flush.
 *
 * Capacity math (PolygonSpriteBatch(size = 8, shader) — Java semantics:
 * size vertices, size * 2 triangles):
 *   - vertices.length  = 8 * VERTEX_SIZE(5)  = 40 floats (= 2 quads of
 *     SPRITE_SIZE = 20 floats)
 *   - triangles.length = 16 * 3              = 48 shorts
 *   - draw(texture, 60 floats = 3 quads, 0, 60): first batch =
 *     min(min(60, 40 - 40 % 20), 48 / 6 * 20) = min(min(60, 40), 160) = 40
 *     floats, triangleCount = 40 / 20 * 6 = 12 (Java lines 752-755, first
 *     draw always takes the switchTexture branch).
 *   - iteration 1 uploads 40 floats (8 vertices), flushes mid-call with
 *     triangleIndex = 12 -> glDrawElements #1: count 12, indices
 *     0,1,2,2,3,0,4,5,6,6,7,4.
 *   - remaining count = 20, batch(40) > 20 -> Java recomputes batch =
 *     min(20, 160) = 20 and triangleIndex = 20 / 20 * 6 = 6 (lines 784-787).
 *   - iteration 2 uploads 20 floats (4 vertices); end() flushes ->
 *     glDrawElements #2 must have count 6, indices 0,1,2,2,3,0 (max index 3
 *     < 4 uploaded vertices). The port instead flushes count 12 with indices
 *     up to 7 — referencing vertices 4..7 that were never uploaded.
 *
 * Headless fixture (same pattern as SpriteCacheRedSuite): the batch is
 * constructed with a directly-instantiated ShaderProgram so that
 * createDefaultShader()'s `if (!shader.compiled) throw` check is bypassed
 * (NoopGL20.glCreateShader returns 0, so no shader ever "compiles" headlessly
 * — ShaderProgram itself constructs fine and all its GL calls are no-ops).
 * Textures are dummy Custom-type TextureData instances — this draw overload
 * uses the texture only as an identity key plus a no-op bind.
 *
 * These tests are written by the reproducer agent and MUST NOT be modified by
 * the fixer: they encode the original Java semantics, not the port's.
 */
package sge
package graphics
package g2d

import sge.graphics.glutils.ShaderProgram
import sge.noop.NoopGraphics

class PolygonSpriteBatchRedSuite extends munit.FunSuite {

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

  /** size = 8: 8 vertices (40 floats = 2 quads) and 16 triangles (48 index slots), mirroring Java's PolygonSpriteBatch(int size, ShaderProgram) = size vertices, size * 2 triangles. */
  private def makeBatch()(using Sge): PolygonSpriteBatch =
    new PolygonSpriteBatch(8, new ShaderProgram("void main(){}", "void main(){}"))

  /** `quads` quads worth of quad-strip vertex data (quads * SPRITE_SIZE = quads * 20 floats) with recognizable values. */
  private def quadStrip(quads: Int): Array[Float] = Array.tabulate(quads * 20)(i => 1000f + i.toFloat)

  /** Records (count, indices) of every glDrawElements call — one call per PolygonSpriteBatch.flush() (flush -> Mesh.render -> glDrawElements, Mesh.scala line 656, VertexArray path). The buffer's
    * first `count` shorts are exactly the indices drawn in that flush.
    */
  final private class DrawElementsRecordingGL20 extends GL20 {
    val drawElementsCalls: scala.collection.mutable.ListBuffer[(Int, Vector[Int])] = scala.collection.mutable.ListBuffer.empty

    private val underlying: GL20 = NoopGL20
    export underlying.{ glDrawElements as _, * }

    def glDrawElements(mode: PrimitiveMode, count: Int, `type`: DataType, indices: java.nio.Buffer): Unit = {
      val values = indices match {
        case sb: java.nio.ShortBuffer =>
          val view = sb.duplicate()
          val out  = new Array[Short](scala.math.min(count, view.remaining()))
          view.get(out)
          out.toVector.map(_.toInt)
        case _ => Vector.empty[Int]
      }
      drawElementsCalls += ((count, values))
    }

    def glDrawElements(mode: PrimitiveMode, count: Int, `type`: DataType, indices: Int): Unit =
      drawElementsCalls += ((count, Vector.empty))
  }

  // --- Control: single batch within capacity (green at red-sha) ---------------

  test("ISS-496 control: a single-quad draw within capacity flushes 6 indices 0,1,2,2,3,0") {
    // count = 20 floats = 1 quad fits the 40-float vertex store: batch = count
    // (Java line 754: min(min(20, 40), 160) = 20), triangleCount = 6, no
    // mid-call flush. end() flushes once with triangleIndex = 6.
    val recordingGL = new DrawElementsRecordingGL20
    given Sge       = makeSge(recordingGL)
    val batch       = makeBatch()
    val texture     = dummyTexture()

    batch.begin()
    batch.draw(texture, quadStrip(1), 0, 20)
    batch.end()

    assertEquals(recordingGL.drawElementsCalls.size, 1, "one quad within capacity must render in exactly one flush")
    val (count, indices) = recordingGL.drawElementsCalls.head
    assertEquals(count, 6, "one quad = 6 indices (2 triangles)")
    assertEquals(indices, Vector(0, 1, 2, 2, 3, 0), "quad index pattern v,v+1,v+2,v+2,v+3,v starting at vertex 0 (Java lines 766-773)")
  }

  // --- Control: exact multiple of capacity (green at red-sha) -----------------

  test("ISS-496 control: a 4-quad draw (2 exactly-full batches) flushes 12 indices twice") {
    // count = 80 floats: batch = 40 (2 quads), triangleCount = 12. Iteration 1
    // uploads 40 floats and flushes mid-call; remaining count = 40 is NOT less
    // than batch (Java line 784: `if (batch > count)` is false), so no
    // recompute is needed — iteration 2 is another exactly-full batch and the
    // end() flush renders the same 12 indices.
    val recordingGL = new DrawElementsRecordingGL20
    given Sge       = makeSge(recordingGL)
    val batch       = makeBatch()
    val texture     = dummyTexture()

    batch.begin()
    batch.draw(texture, quadStrip(4), 0, 80)
    batch.end()

    val fullBatchIndices = Vector(0, 1, 2, 2, 3, 0, 4, 5, 6, 6, 7, 4)
    assertEquals(recordingGL.drawElementsCalls.size, 2, "4 quads against a 2-quad capacity must render in exactly two flushes")
    assertEquals(recordingGL.drawElementsCalls.toList, List((12, fullBatchIndices), (12, fullBatchIndices)))
  }

  // --- ISS-496: final partial batch must flush the recomputed triangle count --

  test(
    "ISS-496: the final partial batch of a multi-batch draw must flush 6 indices, not the first batch's 12 (Java lines 778, 784-787)"
  ) {
    // count = 60 floats = 3 quads against a 2-quad (40-float) capacity:
    // iteration 1 flushes 2 full quads (12 indices); the final partial batch
    // uploads 1 quad (20 floats = 4 vertices) and Java recomputes
    // triangleIndex = 20 / SPRITE_SIZE(20) * 6 = 6 (line 786) before the
    // line-778 assignment, so the end() flush must draw exactly 6 indices.
    // The port's final flush instead carries ti = 12 (PolygonSpriteBatch.scala
    // line 787; the line-797 recompute is a dead store into triangleIdx).
    val recordingGL = new DrawElementsRecordingGL20
    given Sge       = makeSge(recordingGL)
    val batch       = makeBatch()
    val texture     = dummyTexture()

    batch.begin()
    batch.draw(texture, quadStrip(3), 0, 60)
    batch.end()

    assertEquals(recordingGL.drawElementsCalls.size, 2, "3 quads against a 2-quad capacity must render in exactly two flushes")

    val (firstCount, firstIndices) = recordingGL.drawElementsCalls.head
    assertEquals(firstCount, 12, "the first (full) batch carries 2 quads = 12 indices")
    assertEquals(firstIndices, Vector(0, 1, 2, 2, 3, 0, 4, 5, 6, 6, 7, 4), "first-batch quad index pattern (Java lines 766-773)")

    val (finalCount, finalIndices) = recordingGL.drawElementsCalls.last
    assertEquals(
      finalCount,
      6,
      "the final partial batch uploads 1 quad (4 vertices), so Java's recompute (line 786: triangleIndex = batch / SPRITE_SIZE * 6 = 6) caps the flush at 6 indices"
    )
    assertEquals(finalIndices, Vector(0, 1, 2, 2, 3, 0), "the final partial flush draws only the first quad's indices")
  }

  test("ISS-496: no index drawn by the final partial flush may reference a vertex that flush never uploaded") {
    // Corruption signature: the final partial flush uploads vertexIndex = 20
    // floats = 4 vertices (indices 0..3 valid). The port flushes the first
    // batch's full 12 indices, which include 4,5,6,7 — out-of-range vertex
    // references rendering garbage (or trapping on strict GL drivers).
    val recordingGL = new DrawElementsRecordingGL20
    given Sge       = makeSge(recordingGL)
    val batch       = makeBatch()
    val texture     = dummyTexture()

    batch.begin()
    batch.draw(texture, quadStrip(3), 0, 60)
    batch.end()

    assertEquals(recordingGL.drawElementsCalls.size, 2)
    val (_, finalIndices) = recordingGL.drawElementsCalls.last
    val uploadedVertices  = 4 // 60 - 40 = 20 floats / VERTEX_SIZE(5) = 4 vertices in the final flush
    val outOfRange: Seq[Int] = finalIndices.filter(_ >= uploadedVertices)
    assert(
      outOfRange.isEmpty,
      s"final partial flush drew indices $finalIndices but only uploaded $uploadedVertices vertices — out-of-range references: $outOfRange (Java lines 784-787 recompute prevents this)"
    )
  }
}
