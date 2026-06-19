/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Test coverage for ISS-561 (batch F): graphics Mesh — a core glutils-adjacent
 * class that had zero dedicated unit tests. Mesh wraps a VertexData + IndexData
 * pair and exposes data round-trips (setVertices/getVertices,
 * setIndices/getIndices, numVertices/numIndices/maxVertices/maxIndices) plus a
 * render() draw-call state machine that binds, issues glDrawElements (indexed)
 * or glDrawArrays (non-indexed), then unbinds.
 *
 * Every expected behaviour below is hand-traced from the original
 * com/badlogic/gdx/graphics/Mesh.java (original-src/libgdx). Java line numbers
 * cited refer to that file:
 *   - constructor (lines 87-93): stores vertices/indices/isVertexArray.
 *   - setVertices(float[]) (Java 270-273): this.vertices.setVertices(v, 0,
 *     v.length). Port: Mesh.scala lines 321-324.
 *   - getVertices(...) (Java 320-349 / port 407-424): copies from the backing
 *     FloatBuffer; max = numVertices * vertexSize / 4.
 *   - setIndices(short[]) (Java 379-382 / port 433-436): this.indices.setIndices(
 *     i, 0, i.length).
 *   - getIndices(...) (Java 437-453 / port 491-503): copies from the backing
 *     ShortBuffer; max = numIndices.
 *   - getNumIndices/getNumVertices/getMaxVertices/getMaxIndices (Java 455-470 /
 *     port 506-519): delegate to the underlying VertexData/IndexData. For a
 *     VertexArray (VertexArray.scala): numVertices = bufferLimit*4/vertexSize,
 *     and after setVertices the float-buffer limit == floatCount, so
 *     numVertices == floatCount / (vertexSize/4). numMaxVertices ==
 *     byteBuffer.capacity / vertexSize == construction capacity.
 *   - render(shader, primitiveType, offset, count, autoBind) (Java 621-662 /
 *     port 647-688): if count != 0: if autoBind bind(shader); then in the
 *     isVertexArray branch — if indices.getNumIndices() > 0 ->
 *     glDrawElements(primitiveType, count, GL_UNSIGNED_SHORT, buffer) after
 *     positioning the index ShortBuffer to `offset` (Java 628-633 / port
 *     653-657); else glDrawArrays(primitiveType, offset, count) (Java 635 /
 *     port 659); finally if autoBind unbind(shader).
 *
 * Headless strategy: the Mesh is constructed with
 * Mesh.VertexDataType.VertexArray so the underlying VertexData/IndexData are
 * client-memory (VertexArray / IndexArray) — no GL buffer object is allocated
 * at construction (no glGenBuffer path), exactly like the already-tested
 * VertexArray / IndexArray classes. render() still issues real GL draw calls,
 * so a recording GL20 below extends GL20, delegates everything to NoopGL20 via
 * `export`, and overrides ONLY glDrawElements / glDrawArrays to capture the
 * exact (mode, offset, count) and, for the indexed path, the index values the
 * draw would read. The vertex-attribute bind path (VertexArray.bind ->
 * shader.getAttributeLocation/enableVertexAttribute/setVertexAttribute) routes
 * through NoopGL20 and is a no-op headlessly; we additionally record bind /
 * unbind ordering by overriding glEnableVertexAttribArray.
 *
 * Mutations these tests catch (campaign requirement "a test no mutation can
 * break is not a test"):
 *   - render always uses glDrawArrays (drop the indexed branch, Java 627): the
 *     indexed-render test fails because no glDrawElements is recorded (and a
 *     glDrawArrays would appear instead).
 *   - render always uses glDrawElements (drop the non-indexed branch, Java 634):
 *     the no-index render test fails because a glDrawElements is recorded
 *     instead of glDrawArrays.
 *   - wrong count (e.g. uses maxIndices/numVertices instead of the caller's
 *     count): the draw-call count assertion fails (count pinned exactly).
 *   - offset ignored (e.g. positions the index buffer at 0 instead of offset,
 *     Java 631): the offset-passthrough test fails — the recorded first index
 *     would be index[0] instead of index[offset].
 *   - getVertices returns the wrong slice / numVertices uses the wrong divisor:
 *     the round-trip tests fail with mismatched float values / counts.
 */
package sge
package graphics

import sge.graphics.glutils.ShaderProgram
import sge.noop.{ NoopGL20, NoopGraphics }

class MeshISS561Suite extends munit.FunSuite {

  // --- A recorded draw call: which draw entry point, plus the args we pin -----
  //
  // For glDrawElements (indexed VertexArray path) we capture the FIRST `count`
  // shorts the buffer would feed the GL, read from the buffer's CURRENT position
  // (which the production code has set to `offset`) — so a dropped offset would
  // change the recorded index values.
  sealed private trait DrawCall
  final private case class DrawElements(mode: PrimitiveMode, count: Int, indices: Vector[Int]) extends DrawCall
  final private case class DrawArrays(mode: PrimitiveMode, first: Int, count: Int) extends DrawCall

  final private class RecordingGL20 extends GL20 {
    val draws:              scala.collection.mutable.ListBuffer[DrawCall] = scala.collection.mutable.ListBuffer.empty
    val enableAttribCalls:  scala.collection.mutable.ListBuffer[Int]      = scala.collection.mutable.ListBuffer.empty
    val disableAttribCalls: scala.collection.mutable.ListBuffer[Int]      = scala.collection.mutable.ListBuffer.empty

    private val underlying: GL20 = NoopGL20
    export underlying.{ glDisableVertexAttribArray as _, glDrawArrays as _, glDrawElements as _, glEnableVertexAttribArray as _, * }

    def glDrawElements(mode: PrimitiveMode, count: Int, `type`: DataType, indices: java.nio.Buffer): Unit = {
      val values = indices match {
        case sb: java.nio.ShortBuffer =>
          // Read from the CURRENT position the production code set (== offset),
          // without disturbing it (duplicate inherits position).
          val view = sb.duplicate()
          val out  = new Array[Short](scala.math.min(count, view.remaining()))
          view.get(out)
          out.toVector.map(_.toInt)
        case _ => Vector.empty[Int]
      }
      draws += DrawElements(mode, count, values)
    }

    def glDrawElements(mode: PrimitiveMode, count: Int, `type`: DataType, indices: Int): Unit =
      draws += DrawElements(mode, count, Vector.empty)

    def glDrawArrays(mode: PrimitiveMode, first: Int, count: Int): Unit =
      draws += DrawArrays(mode, first, count)

    override def glEnableVertexAttribArray(index: Int): Unit =
      enableAttribCalls += index

    override def glDisableVertexAttribArray(index: Int): Unit =
      disableAttribCalls += index
  }

  private def makeSge(glImpl: GL20): Sge =
    SgeTestFixture.testSge(graphics = new NoopGraphics() {
      override def gl20: GL20 = glImpl
    })

  /** A bare ShaderProgram: never compiles headlessly (NoopGL20.glCreateShader -> 0), but constructs fine and all its GL calls are no-ops. Mesh.bind/render use it only as the attribute-binding target.
    *
    * VertexArray.bind looks the attribute location up via the shader's location CACHE (getAttributeLocation, ShaderProgram.scala:918) and only emits glEnableVertexAttribArray when it is NOT notFound.
    * Headlessly the cache starts empty (no compile/fetchAttributes), so we prime it for "a_position" by calling disableVertexAttribute(name) once — that routes through fetchAttributeLocation, which
    * caches glGetAttribLocation's result (NoopGL20 -> 0, a valid location). After priming, bind/unbind reliably emit glEnableVertexAttribArray / glDisableVertexAttribArray so the autoBind tests have
    * an observable bracket.
    */
  private def makeShader(gl: RecordingGL20)(using Sge): ShaderProgram = {
    val s = new ShaderProgram("void main(){}", "void main(){}")
    s.disableVertexAttribute("a_position")
    // The priming call recorded a disable; clear all recorder state so each
    // test observes only the calls its own render() produces.
    gl.draws.clear()
    gl.enableAttribCalls.clear()
    gl.disableAttribCalls.clear()
    s
  }

  /** A 2-float Position attribute -> vertexSize == 8 bytes (numVertices == floatCount / 2). Built directly rather than via VertexAttribute.Position() (which is 3 components) to keep the float-count
    * <-> vertex-count math minimal and explicit.
    */
  private val pos2 = new VertexAttribute(VertexAttributes.Usage.Position, 2, "a_position")

  /** Headless mesh: VertexArray data type -> client-memory VertexData/IndexData, no GL buffer object at construction.
    */
  private def makeMesh(maxVertices: Int, maxIndices: Int)(using Sge): Mesh =
    new Mesh(Mesh.VertexDataType.VertexArray, false, maxVertices, maxIndices, VertexAttributes(pos2))

  // ===========================================================================
  // Vertex data round-trip
  // ===========================================================================

  test(
    "ISS561: setVertices then getVertices round-trips the exact floats; numVertices = floatCount/(vertexSize/4); maxVertices from capacity"
  ) {
    val gl    = new RecordingGL20()
    given Sge = makeSge(gl)
    val mesh  = makeMesh(maxVertices = 8, maxIndices = 0)

    // vertexSize for a 2-float Position is 8 bytes -> vertexSize/4 == 2 floats
    // per vertex. 4 vertices = 8 floats.
    assertEquals(mesh.vertexSize, 8, "2-float Position vertex is 8 bytes")
    assertEquals(mesh.maxVertices, 8, "maxVertices == construction capacity (VertexArray numMaxVertices, Java 460)")

    val verts = Array(10f, 11f, 20f, 21f, 30f, 31f, 40f, 41f) // 8 floats = 4 vertices
    mesh.setVertices(verts)

    assertEquals(mesh.numVertices, 4, "numVertices == floatCount(8) / (vertexSize/4 == 2) (Java getNumVertices, port 510-511)")

    val out = new Array[Float](8)
    mesh.getVertices(out)
    assertEquals(out.toVector, verts.toVector, "getVertices must copy back the exact floats that were set (Java 320-349)")
  }

  test("ISS561: getVertices with srcOffset+count returns exactly that sub-slice (no off-by-one, no wrong divisor)") {
    val gl    = new RecordingGL20()
    given Sge = makeSge(gl)
    val mesh  = makeMesh(maxVertices = 8, maxIndices = 0)

    val verts = Array(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f)
    mesh.setVertices(verts)

    // Copy 4 floats starting at float index 2 -> {3,4,5,6}.
    val out = new Array[Float](4)
    mesh.getVertices(2, 4, out, 0)
    assertEquals(
      out.toVector,
      Vector(3f, 4f, 5f, 6f),
      "getVertices(srcOffset=2, count=4) must return floats 2..5; a wrong slice/offset fails here"
    )
  }

  // ===========================================================================
  // Index data round-trip
  // ===========================================================================

  test("ISS561: setIndices then getIndices round-trips; numIndices is exact") {
    val gl    = new RecordingGL20()
    given Sge = makeSge(gl)
    val mesh  = makeMesh(maxVertices = 8, maxIndices = 12)

    val idx = Array[Short](0, 1, 2, 2, 3, 0)
    mesh.setIndices(idx)

    assertEquals(mesh.numIndices, 6, "numIndices == number of shorts set (Java getNumIndices, port 506-507)")
    assertEquals(mesh.maxIndices, 12, "maxIndices == construction capacity (Java getMaxIndices, port 518-519)")

    val out = new Array[Short](6)
    mesh.getIndices(out)
    assertEquals(out.toVector, idx.toVector, "getIndices must copy back the exact shorts that were set (Java 437-453)")
  }

  test("ISS561: a mesh with 0 indices reports numIndices == 0") {
    val gl    = new RecordingGL20()
    given Sge = makeSge(gl)
    val mesh  = makeMesh(maxVertices = 8, maxIndices = 0)

    assertEquals(
      mesh.numIndices,
      0,
      "a mesh constructed with maxIndices=0 (and no setIndices) has numIndices 0 (IndexArray.empty, port 49-50)"
    )
    assertEquals(mesh.maxIndices, 0, "maxIndices is 0 for a 0-capacity IndexArray")
  }

  // ===========================================================================
  // render() draw-call state machine
  // ===========================================================================

  test(
    "ISS561: render WITH indices issues glDrawElements(mode, count, UnsignedShort, ...) — exact mode/count, exact uploaded indices, no glDrawArrays"
  ) {
    val gl     = new RecordingGL20()
    given Sge  = makeSge(gl)
    val mesh   = makeMesh(maxVertices = 8, maxIndices = 12)
    val shader = makeShader(gl)

    mesh.setVertices(Array(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f)) // 4 vertices
    val idx = Array[Short](0, 1, 2, 2, 3, 0)
    mesh.setIndices(idx)

    // render(shader, Triangles, offset=0, count=6) -> indexed VertexArray path:
    // glDrawElements(Triangles, 6, UnsignedShort, buffer) (Java 632 / port 656).
    mesh.render(shader, PrimitiveMode.Triangles, 0, 6)

    assertEquals(gl.draws.size, 1, "indexed render must issue exactly one draw call")
    gl.draws.head match {
      case DrawElements(mode, count, indices) =>
        assertEquals(mode, PrimitiveMode.Triangles, "draw mode must be the caller's Triangles, not a hardcoded other mode")
        assertEquals(count, 6, "count must be the caller's 6, NOT maxIndices(12) or numVertices(4) (mutation: wrong count)")
        assertEquals(indices, Vector(0, 1, 2, 2, 3, 0), "the indexed draw must read exactly the uploaded indices from offset 0")
      case other =>
        fail(s"indexed render must call glDrawElements, not $other (mutation: render always uses glDrawArrays)")
    }
  }

  test("ISS561: render on a mesh with NO indices issues glDrawArrays(mode, 0, count) — NOT glDrawElements") {
    val gl     = new RecordingGL20()
    given Sge  = makeSge(gl)
    val mesh   = makeMesh(maxVertices = 8, maxIndices = 0)
    val shader = makeShader(gl)

    mesh.setVertices(Array(0f, 0f, 1f, 0f, 1f, 1f)) // 3 vertices, no indices

    // render(shader, Triangles, offset=0, count=3) -> non-indexed VertexArray
    // path: glDrawArrays(Triangles, 0, 3) (Java 635 / port 659).
    mesh.render(shader, PrimitiveMode.Triangles, 0, 3)

    assertEquals(gl.draws.size, 1, "non-indexed render must issue exactly one draw call")
    gl.draws.head match {
      case DrawArrays(mode, first, count) =>
        assertEquals(mode, PrimitiveMode.Triangles, "draw mode must be the caller's Triangles")
        assertEquals(first, 0, "glDrawArrays first must be the caller's offset 0")
        assertEquals(count, 3, "glDrawArrays count must be the caller's 3")
      case other =>
        fail(s"a mesh with no indices must call glDrawArrays, not $other (mutation: render always uses glDrawElements)")
    }
    assert(
      !gl.draws.exists(_.isInstanceOf[DrawElements]),
      "no glDrawElements may be issued when the mesh has no indices"
    )
  }

  test(
    "ISS561: render WITH indices passes offset through exactly — the index buffer is positioned to offset before glDrawElements"
  ) {
    val gl     = new RecordingGL20()
    given Sge  = makeSge(gl)
    val mesh   = makeMesh(maxVertices = 8, maxIndices = 12)
    val shader = makeShader(gl)

    mesh.setVertices(Array(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f)) // 4 vertices
    // 6 indices; drawing 3 of them starting at offset 3 must read {2,3,0}.
    val idx = Array[Short](0, 1, 2, 2, 3, 0)
    mesh.setIndices(idx)

    mesh.render(shader, PrimitiveMode.Triangles, 3, 3)

    assertEquals(gl.draws.size, 1)
    gl.draws.head match {
      case DrawElements(mode, count, indices) =>
        assertEquals(mode, PrimitiveMode.Triangles)
        assertEquals(count, 3, "count must be the caller's 3")
        assertEquals(
          indices,
          Vector(2, 3, 0),
          "offset=3 must position the index buffer at element 3 so the draw reads {2,3,0}; ignoring offset would read {0,1,2} (Java 631)"
        )
      case other =>
        fail(s"expected glDrawElements, got $other")
    }
  }

  test("ISS561: render with count == 0 issues no draw call and does not bind (Java 622 early return)") {
    val gl     = new RecordingGL20()
    given Sge  = makeSge(gl)
    val mesh   = makeMesh(maxVertices = 8, maxIndices = 12)
    val shader = makeShader(gl)

    mesh.setVertices(Array(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f))
    mesh.setIndices(Array[Short](0, 1, 2, 2, 3, 0))

    mesh.render(shader, PrimitiveMode.Triangles, 0, 0)

    assertEquals(gl.draws.size, 0, "count == 0 must short-circuit before any draw call (Java line 622)")
    assertEquals(gl.enableAttribCalls.size, 0, "count == 0 must short-circuit before bind() enables any vertex attribute")
  }

  test("ISS561: autoBind=true brackets the draw with bind (enable attrib) before and unbind (disable attrib) after") {
    val gl     = new RecordingGL20()
    given Sge  = makeSge(gl)
    val mesh   = makeMesh(maxVertices = 8, maxIndices = 12)
    val shader = makeShader(gl)

    mesh.setVertices(Array(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f))
    mesh.setIndices(Array[Short](0, 1, 2, 2, 3, 0))

    // Default autoBind is true (Java 81 / port 72). render -> bind(shader)
    // (VertexArray.bind enables the attribute), draw, unbind(shader)
    // (VertexArray.unbind disables it).
    mesh.render(shader, PrimitiveMode.Triangles, 0, 6)

    assertEquals(gl.draws.size, 1, "autoBind render still issues exactly one draw")
    assert(gl.enableAttribCalls.nonEmpty, "autoBind=true must bind the vertex attribute before drawing (Java 624)")
    assert(gl.disableAttribCalls.nonEmpty, "autoBind=true must unbind the vertex attribute after drawing (Java 661)")
  }

  test("ISS561: autoBind=false issues the draw without binding/unbinding vertex attributes") {
    val gl     = new RecordingGL20()
    given Sge  = makeSge(gl)
    val mesh   = makeMesh(maxVertices = 8, maxIndices = 12)
    val shader = makeShader(gl)

    mesh.setVertices(Array(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f))
    mesh.setIndices(Array[Short](0, 1, 2, 2, 3, 0))

    mesh.render(shader, PrimitiveMode.Triangles, 0, 6, autoBind = false)

    assertEquals(gl.draws.size, 1, "the draw is still issued with autoBind disabled")
    assertEquals(gl.enableAttribCalls.size, 0, "autoBind=false must NOT bind vertex attributes (Java 624 guarded by autoBind)")
    assertEquals(gl.disableAttribCalls.size, 0, "autoBind=false must NOT unbind vertex attributes (Java 661 guarded by autoBind)")
  }
}
