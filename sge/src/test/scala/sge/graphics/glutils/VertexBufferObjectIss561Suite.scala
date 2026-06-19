/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package graphics
package glutils

import java.nio.Buffer
import java.nio.ByteBuffer

import lowlevel.Nullable
import sge.noop.{ NoopGL20, NoopGraphics }

/** ISS-561 batch F: GL state-machine coverage for VertexBufferObject (the GL-backed VertexData counterpart of VertexArray).
  *
  * VertexBufferObject wraps an OpenGL array buffer object. All expected GL call sequences below are derived by hand-tracing the LibGDX Java original at
  * com/badlogic/gdx/graphics/glutils/VertexBufferObject.java (line numbers cited inline). A recording GL20 captures the EXACT ordered buffer-object call stream and the float payload handed to
  * glBufferData, so mutations of the state machine — dropping the isDirty guard, binding to handle 0 instead of bufferHandle, skipping glDeleteBuffer on dispose, or passing the wrong glBufferData
  * size — fail an assertion.
  *
  * The attribute used is VertexAttribute.Position() (3 float components → vertexSize == 12 bytes). Headlessly the ShaderProgram registers no attributes, so getAttributeLocation returns
  * AttributeLocation.notFound and bind() skips the glVertexAttribPointer setup loop (Java lines 192-200); only the glBindBuffer / glBufferData portion of bind() is exercised — which is exactly the
  * buffer state machine under test.
  */
class VertexBufferObjectIss561Suite extends munit.FunSuite {

  private val Handle = 7

  final private class RecordingGL20 extends GL20 {
    sealed trait Call
    final case class GenBuffer(handle: Int) extends Call
    final case class BindBuffer(target: Int, buffer: Int) extends Call
    final case class BufferData(target: Int, size: Int, usage: Int, floats: Vector[Float]) extends Call
    final case class DeleteBuffer(buffer: Int) extends Call

    val calls: scala.collection.mutable.ListBuffer[Call] = scala.collection.mutable.ListBuffer.empty

    private val underlying: GL20 = NoopGL20
    export underlying.{ glBindBuffer as _, glBufferData as _, glDeleteBuffer as _, glGenBuffer as _, * }

    override def glGenBuffer(): Int = {
      calls += GenBuffer(Handle)
      Handle
    }

    override def glBindBuffer(target: BufferTarget, buffer: Int): Unit =
      calls += BindBuffer(target.toInt, buffer)

    override def glBufferData(target: BufferTarget, size: Int, data: Buffer, usage: BufferUsage): Unit = {
      val floats = data match {
        case bb: ByteBuffer =>
          val view = bb.duplicate()
          view.order(bb.order()) // preserve native byte order for float decoding
          view.asInstanceOf[Buffer].position(0)
          view.asInstanceOf[Buffer].limit(size)
          val fb  = view.asFloatBuffer()
          val out = new Array[Float](fb.remaining())
          fb.get(out)
          out.toVector
        case _ => Vector.empty[Float]
      }
      calls += BufferData(target.toInt, size, usage.toInt, floats)
    }

    override def glDeleteBuffer(buffer: Int): Unit =
      calls += DeleteBuffer(buffer)
  }

  private def makeSge(glImpl: GL20): Sge =
    SgeTestFixture.testSge(graphics = new NoopGraphics() {
      override def gl20: GL20 = glImpl
    })

  /** A directly-instantiated ShaderProgram. Headlessly it never "compiles" and registers no attributes, so bind()'s per-attribute loop is a no-op — only the buffer calls run (same headless trick as
    * PolygonSpriteBatchRedSuite).
    */
  private def shader()(using Sge): ShaderProgram =
    new ShaderProgram("void main(){}", "void main(){}")

  private val ArrayBufferRaw = BufferTarget.ArrayBuffer.toInt
  private val StaticDrawRaw  = BufferUsage.StaticDraw.toInt
  private val DynamicDrawRaw = BufferUsage.DynamicDraw.toInt
  private val NoLocations    = Nullable.empty[Array[AttributeLocation]]

  // Position() == 3 float components == 12 bytes per vertex.
  private def positionAttr(): VertexAttribute = VertexAttribute.Position()

  // --- construction: exactly one glGenBuffer, counts from vertexSize -----------

  // Java ctor (VertexBufferObject.java:66-73): one glGenBuffer() (line 67);
  // byteBuffer capacity == vertexSize * numVertices, limit set to 0 (line 70).
  // getNumMaxVertices == capacity / vertexSize (line 94) == numVertices;
  // getNumVertices == buffer.limit()*4/vertexSize (line 89) == 0 while empty.
  test("ISS561 VertexBufferObject construction issues exactly one glGenBuffer and derives counts from vertexSize") {
    val gl    = new RecordingGL20
    given Sge = makeSge(gl)
    val vbo   = new VertexBufferObject(true, 4, positionAttr())
    try {
      assertEquals(gl.calls.toList, List(gl.GenBuffer(Handle)), "ctor must issue exactly one glGenBuffer, nothing else")
      assertEquals(vbo.numVertices, 0, "empty VBO: buffer.limit() == 0 → numVertices 0 (Java line 89)")
      assertEquals(vbo.numMaxVertices, 4, "capacity (12*4) / vertexSize (12) == 4 (Java line 94)")
      assertEquals(vbo.attributes.vertexSize, 12, "Position() == 3 floats == 12 bytes")
    } finally vbo.close()
  }

  // --- setVertices + bind(shader): glBindBuffer(handle) THEN glBufferData -------

  // Java setVertices (lines 140-145) only sets isDirty (not bound). bind(shader)
  // (lines 180-188): glBindBuffer to bufferHandle (line 183), and because
  // isDirty, glBufferData with byteBuffer.limit()==buffer.limit()*4==count*4
  // bytes (lines 185-186), then isDirty=false.
  test(
    "ISS561 setVertices then bind(shader): glBindBuffer(handle) precedes glBufferData with count*4 bytes and exact floats"
  ) {
    val gl    = new RecordingGL20
    given Sge = makeSge(gl)
    val vbo   = new VertexBufferObject(true, 4, positionAttr())
    val sh    = shader()
    try {
      gl.calls.clear() // drop ctor glGenBuffer
      // 2 vertices worth of position data = 6 floats.
      val verts = Array[Float](1f, 2f, 3f, 4f, 5f, 6f)
      vbo.setVertices(verts, 0, 6)
      assertEquals(gl.calls.toList, Nil, "setVertices on an unbound VBO must not touch GL")

      vbo.bind(sh)
      assertEquals(
        gl.calls.toList,
        List(
          gl.BindBuffer(ArrayBufferRaw, Handle),
          gl.BufferData(ArrayBufferRaw, 24, StaticDrawRaw, Vector[Float](1f, 2f, 3f, 4f, 5f, 6f))
        ),
        "bind must glBindBuffer(ArrayBuffer, bufferHandle) then glBufferData(count*4=24 bytes, exact floats, StaticDraw)"
      )
      // After upload, numVertices reflects the 6 floats / vertexSize... actually
      // buffer.limit()*4 / 12 = 6*4/12 = 2 vertices.
      assertEquals(vbo.numVertices, 2)
    } finally {
      vbo.close()
      sh.close()
    }
  }

  // --- second bind without setVertices: NO new glBufferData (isDirty cleared) ---

  // Java bind clears isDirty after upload (line 187); a second bind with no
  // intervening setVertices issues ONLY glBindBuffer (lines 183-188).
  //
  // MUTATION CAUGHT: dropping the `if (isDirty)` guard in bind()
  // (VertexBufferObject.scala line 176) — always re-uploading — adds a second
  // glBufferData here, failing this assertion.
  test("ISS561 a second bind(shader) with no setVertices re-binds but issues no new glBufferData (isDirty guard)") {
    val gl    = new RecordingGL20
    given Sge = makeSge(gl)
    val vbo   = new VertexBufferObject(true, 4, positionAttr())
    val sh    = shader()
    try {
      vbo.setVertices(Array[Float](1f, 2f, 3f), 0, 3)
      vbo.bind(sh)
      gl.calls.clear()
      vbo.bind(sh) // not dirty
      assertEquals(
        gl.calls.toList,
        List(gl.BindBuffer(ArrayBufferRaw, Handle)),
        "second bind must only glBindBuffer; an extra glBufferData means the isDirty guard was dropped"
      )
    } finally {
      vbo.close()
      sh.close()
    }
  }

  // --- unbind(shader): glBindBuffer(ArrayBuffer, 0) ----------------------------

  // Java unbind (lines 224-240): after the disable loop, glBindBuffer(GL_ARRAY_BUFFER, 0).
  //
  // MUTATION CAUGHT: binding to bufferHandle instead of 0 in unbind()
  // (VertexBufferObject.scala line 214) records buffer==Handle, failing this.
  test("ISS561 unbind(shader) binds the array target to 0") {
    val gl    = new RecordingGL20
    given Sge = makeSge(gl)
    val vbo   = new VertexBufferObject(true, 4, positionAttr())
    val sh    = shader()
    try {
      vbo.setVertices(Array[Float](1f, 2f, 3f), 0, 3)
      vbo.bind(sh)
      gl.calls.clear()
      vbo.unbind(sh)
      assertEquals(
        gl.calls.toList,
        List(gl.BindBuffer(ArrayBufferRaw, 0)),
        "unbind must glBindBuffer(ArrayBuffer, 0), not the handle"
      )
    } finally {
      vbo.close()
      sh.close()
    }
  }

  // --- dispose (close): glBindBuffer(target,0) then glDeleteBuffer(handle) ------

  // Java dispose (lines 250-257): glBindBuffer(GL_ARRAY_BUFFER, 0) then
  // glDeleteBuffer(bufferHandle), in that order, then bufferHandle=0.
  //
  // MUTATION CAUGHT: skipping glDeleteBuffer on dispose
  // (VertexBufferObject.scala line 228) — leaking the GL buffer — drops the
  // DeleteBuffer(Handle) entry, failing this assertion.
  test("ISS561 close unbinds (target,0) then deletes the exact handle, in order") {
    val gl    = new RecordingGL20
    given Sge = makeSge(gl)
    val vbo   = new VertexBufferObject(true, 4, positionAttr())
    gl.calls.clear()
    vbo.close()
    assertEquals(
      gl.calls.toList,
      List(gl.BindBuffer(ArrayBufferRaw, 0), gl.DeleteBuffer(Handle)),
      "close must glBindBuffer(target,0) THEN glDeleteBuffer(bufferHandle); a missing delete leaks the GL buffer"
    )
  }

  // --- invalidate: fresh glGenBuffer + dirty; next bind re-uploads -------------

  // Java invalidate (lines 243-247): a NEW glGenBuffer() and isDirty=true. The
  // next bind re-binds to the (new) handle and re-uploads via glBufferData.
  test("ISS561 invalidate issues a fresh glGenBuffer and forces the next bind to re-upload") {
    val gl    = new RecordingGL20
    given Sge = makeSge(gl)
    val vbo   = new VertexBufferObject(true, 4, positionAttr())
    val sh    = shader()
    try {
      vbo.setVertices(Array[Float](9f, 8f, 7f), 0, 3)
      vbo.bind(sh)
      gl.calls.clear()
      vbo.invalidate()
      assertEquals(gl.calls.toList, List(gl.GenBuffer(Handle)), "invalidate must issue exactly one fresh glGenBuffer")
      gl.calls.clear()
      vbo.bind(sh)
      assertEquals(
        gl.calls.toList,
        List(
          gl.BindBuffer(ArrayBufferRaw, Handle),
          gl.BufferData(ArrayBufferRaw, 12, StaticDrawRaw, Vector[Float](9f, 8f, 7f))
        ),
        "bind after invalidate must re-bind and re-upload the 3 floats (isDirty was set)"
      )
    } finally {
      vbo.close()
      sh.close()
    }
  }

  // --- usage reflects isStatic: DynamicDraw for a non-static VBO ---------------

  // Java ctor (line 72): setUsage(isStatic ? GL_STATIC_DRAW : GL_DYNAMIC_DRAW).
  test("ISS561 a non-static VertexBufferObject uploads with DynamicDraw usage") {
    val gl    = new RecordingGL20
    given Sge = makeSge(gl)
    val vbo   = new VertexBufferObject(false, 4, positionAttr())
    val sh    = shader()
    try {
      vbo.setVertices(Array[Float](3f, 4f, 5f), 0, 3)
      gl.calls.clear()
      vbo.bind(sh, NoLocations)
      assertEquals(
        gl.calls.toList,
        List(
          gl.BindBuffer(ArrayBufferRaw, Handle),
          gl.BufferData(ArrayBufferRaw, 12, DynamicDrawRaw, Vector[Float](3f, 4f, 5f))
        ),
        "non-static VBO must pass DynamicDraw to glBufferData (Java line 72)"
      )
    } finally {
      vbo.close()
      sh.close()
    }
  }
}
