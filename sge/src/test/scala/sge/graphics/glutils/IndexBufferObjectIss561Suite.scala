/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package graphics
package glutils

import java.nio.Buffer
import java.nio.ByteBuffer

import sge.noop.{ NoopGL20, NoopGraphics }

/** ISS-561 batch F: GL state-machine coverage for IndexBufferObject (the GL-backed IndexData counterpart of IndexArray).
  *
  * IndexBufferObject wraps an OpenGL element-array buffer object. All expected GL call sequences below are derived by hand-tracing the LibGDX Java original at
  * com/badlogic/gdx/graphics/glutils/IndexBufferObject.java (line numbers cited inline). A recording GL20 (below) captures the EXACT ordered sequence of buffer calls (glGenBuffer / glBindBuffer /
  * glBufferData / glDeleteBuffer) and the bytes handed to glBufferData, so a mutation of the state machine — dropping the isDirty guard, binding to handle 0 instead of bufferHandle, skipping
  * glDeleteBuffer on dispose, or passing the wrong glBufferData size — fails an assertion.
  */
class IndexBufferObjectIss561Suite extends munit.FunSuite {

  // The fixed nonzero handle glGenBuffer hands out. Java IndexBufferObject ctor
  // (line 87) stores Gdx.gl20.glGenBuffer() in bufferHandle; a zero handle would
  // make bind() throw "No buffer allocated!" (line 187), so the handle must be
  // both nonzero and the exact value threaded through every later GL call.
  private val Handle = 7

  /** A recording GL20 that captures the ordered buffer-object call stream. Every non-buffer GL call delegates to NoopGL20 via `export`. */
  final private class RecordingGL20 extends GL20 {
    sealed trait Call
    final case class GenBuffer(handle: Int) extends Call
    final case class BindBuffer(target: Int, buffer: Int) extends Call
    final case class BufferData(target: Int, size: Int, usage: Int, shorts: Vector[Short]) extends Call
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
      // Snapshot the uploaded bytes as shorts (the index payload) without
      // disturbing the source buffer's position/limit.
      val shorts = data match {
        case bb: ByteBuffer =>
          val view = bb.duplicate()
          view.order(bb.order()) // preserve native byte order for short decoding
          view.asInstanceOf[Buffer].position(0)
          view.asInstanceOf[Buffer].limit(size)
          val sb  = view.asShortBuffer()
          val out = new Array[Short](sb.remaining())
          sb.get(out)
          out.toVector
        case _ => Vector.empty[Short]
      }
      calls += BufferData(target.toInt, size, usage.toInt, shorts)
    }

    override def glDeleteBuffer(buffer: Int): Unit =
      calls += DeleteBuffer(buffer)
  }

  private def makeSge(glImpl: GL20): Sge =
    SgeTestFixture.testSge(graphics = new NoopGraphics() {
      override def gl20: GL20 = glImpl
    })

  private val ElementArrayBufferRaw = BufferTarget.ElementArrayBuffer.toInt
  private val StaticDrawRaw         = BufferUsage.StaticDraw.toInt
  private val DynamicDrawRaw        = BufferUsage.DynamicDraw.toInt

  // --- construction: exactly one glGenBuffer, handle stored, counts correct ---

  // Java ctor (IndexBufferObject.java:73-89): a single Gdx.gl20.glGenBuffer()
  // call (line 87); both buffers flipped so getNumIndices()==0 (line 105) and
  // getNumMaxIndices()==capacity (line 110). bufferHandle is private, so the
  // stored handle is asserted indirectly: a later bind() must glBindBuffer to
  // the exact value glGenBuffer returned (see the setIndices+bind test).
  test("ISS561 IndexBufferObject construction issues exactly one glGenBuffer and reports correct counts") {
    val gl    = new RecordingGL20
    given Sge = makeSge(gl)
    val ibo   = new IndexBufferObject(true, 8)
    try {
      assertEquals(gl.calls.toList, List(gl.GenBuffer(Handle)), "ctor must issue exactly one glGenBuffer, nothing else")
      // freshly constructed, flipped: numIndices == buffer.limit() == 0.
      assertEquals(ibo.numIndices, 0)
      // capacity == maxIndices (Java line 110).
      assertEquals(ibo.numMaxIndices, 8)
    } finally ibo.close()
  }

  // --- setIndices + bind: glBindBuffer(handle) THEN glBufferData(count*2) -------

  // Java setIndices(short[],offset,count) only sets isDirty (line 127) when NOT
  // bound; the actual upload happens in bind() (lines 186-196): glBindBuffer to
  // bufferHandle (line 189), and because isDirty, glBufferData with
  // byteBuffer.limit() == buffer.limit()*2 == count*2 bytes (lines 191-192),
  // then isDirty=false (line 193).
  test("ISS561 setIndices then bind: glBindBuffer(handle) precedes glBufferData with count*2 bytes and exact shorts") {
    val gl    = new RecordingGL20
    given Sge = makeSge(gl)
    val ibo   = new IndexBufferObject(true, 8)
    try {
      gl.calls.clear() // drop the ctor's glGenBuffer; focus on the bind sequence.
      val indices = Array[Short](10, 20, 30, 40, 50, 60)
      ibo.setIndices(indices, 0, 6)
      // setIndices while unbound performs NO GL call (Java: isBound is false).
      assertEquals(gl.calls.toList, Nil, "setIndices on an unbound IBO must not touch GL")

      ibo.bind()
      // EXACT ordered sequence: bind to the real handle, THEN upload.
      assertEquals(
        gl.calls.toList,
        List(
          gl.BindBuffer(ElementArrayBufferRaw, Handle),
          gl.BufferData(ElementArrayBufferRaw, 12, StaticDrawRaw, Vector[Short](10, 20, 30, 40, 50, 60))
        ),
        "bind() must glBindBuffer(ElementArrayBuffer, bufferHandle) then glBufferData(count*2=12 bytes, exact shorts, StaticDraw)"
      )
    } finally ibo.close()
  }

  // --- second bind without setIndices: NO new glBufferData (isDirty cleared) ----

  // Java bind() clears isDirty after the first upload (line 193); a second bind
  // with no intervening setIndices finds isDirty==false and issues ONLY
  // glBindBuffer (line 189), skipping glBufferData (lines 190-194).
  //
  // MUTATION CAUGHT: dropping the `if (isDirty)` guard in bind()
  // (IndexBufferObject.scala line 169) — always re-uploading — would add a
  // second glBufferData here, failing this assertion.
  test("ISS561 a second bind() with no setIndices re-binds but issues no new glBufferData (isDirty guard)") {
    val gl    = new RecordingGL20
    given Sge = makeSge(gl)
    val ibo   = new IndexBufferObject(true, 8)
    try {
      ibo.setIndices(Array[Short](1, 2, 3, 4), 0, 4)
      ibo.bind()
      gl.calls.clear()
      ibo.bind() // not dirty anymore
      assertEquals(
        gl.calls.toList,
        List(gl.BindBuffer(ElementArrayBufferRaw, Handle)),
        "second bind must only glBindBuffer; an extra glBufferData means the isDirty guard was dropped"
      )
    } finally ibo.close()
  }

  // --- setIndices WHILE bound uploads immediately ------------------------------

  // Java setIndices(...) when isBound==true uploads inline (lines 134-137):
  // glBufferData(byteBuffer.limit()==count<<1) and clears isDirty. So after a
  // bind, a fresh setIndices triggers an immediate glBufferData with the NEW
  // count*2 size, and the following bind does NOT re-upload.
  test("ISS561 setIndices while bound uploads immediately; the next bind does not re-upload") {
    val gl    = new RecordingGL20
    given Sge = makeSge(gl)
    val ibo   = new IndexBufferObject(true, 8)
    try {
      ibo.setIndices(Array[Short](1, 2), 0, 2)
      ibo.bind() // first upload: 2 shorts
      gl.calls.clear()
      // Now bound: setIndices must upload immediately with the new size.
      ibo.setIndices(Array[Short](5, 6, 7), 0, 3)
      assertEquals(
        gl.calls.toList,
        List(gl.BufferData(ElementArrayBufferRaw, 6, StaticDrawRaw, Vector[Short](5, 6, 7))),
        "setIndices while bound must glBufferData(count*2=6) inline (Java lines 134-137)"
      )
      gl.calls.clear()
      ibo.bind() // isDirty was cleared by the inline upload
      assertEquals(
        gl.calls.toList,
        List(gl.BindBuffer(ElementArrayBufferRaw, Handle)),
        "bind after an inline upload must not re-upload"
      )
    } finally ibo.close()
  }

  // --- unbind: glBindBuffer(target, 0) -----------------------------------------

  // Java unbind() (lines 199-202): glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0).
  //
  // MUTATION CAUGHT: binding to bufferHandle instead of 0 in unbind()
  // (IndexBufferObject.scala line 179) would record buffer==Handle, failing this.
  test("ISS561 unbind binds the element-array target to 0") {
    val gl    = new RecordingGL20
    given Sge = makeSge(gl)
    val ibo   = new IndexBufferObject(true, 8)
    try {
      ibo.setIndices(Array[Short](1, 2), 0, 2)
      ibo.bind()
      gl.calls.clear()
      ibo.unbind()
      assertEquals(
        gl.calls.toList,
        List(gl.BindBuffer(ElementArrayBufferRaw, 0)),
        "unbind must glBindBuffer(ElementArrayBuffer, 0), not the handle"
      )
    } finally ibo.close()
  }

  // --- dispose (close): glBindBuffer(target,0) then glDeleteBuffer(handle) ------

  // Java dispose() (lines 211-218): glBindBuffer(target, 0) then
  // glDeleteBuffer(bufferHandle), in that order, then bufferHandle=0.
  //
  // MUTATION CAUGHT: skipping glDeleteBuffer on dispose
  // (IndexBufferObject.scala line 192) — leaking the GL buffer — would drop the
  // DeleteBuffer(Handle) entry, failing this assertion. Reordering would too.
  test("ISS561 close unbinds (target,0) then deletes the exact handle, in order") {
    val gl    = new RecordingGL20
    given Sge = makeSge(gl)
    val ibo   = new IndexBufferObject(true, 8)
    gl.calls.clear() // drop ctor glGenBuffer
    ibo.close()
    assertEquals(
      gl.calls.toList,
      List(gl.BindBuffer(ElementArrayBufferRaw, 0), gl.DeleteBuffer(Handle)),
      "close must glBindBuffer(target,0) THEN glDeleteBuffer(bufferHandle); a missing delete leaks the GL buffer"
    )
    // bufferHandle is private; its reset to 0 (Java line 214) is asserted
    // behaviorally by the "bind after close throws" test below.
  }

  // --- bind after dispose throws (bufferHandle == 0) ---------------------------

  // Java bind() guards bufferHandle==0 (line 187): after dispose the handle is 0,
  // so bind() throws "No buffer allocated!".
  test("ISS561 bind after close throws because the handle was reset to 0") {
    val gl    = new RecordingGL20
    given Sge = makeSge(gl)
    val ibo   = new IndexBufferObject(true, 8)
    ibo.close()
    intercept[sge.utils.SgeError.GraphicsError](ibo.bind())
  }

  // --- invalidate: fresh glGenBuffer + dirty; next bind re-uploads -------------

  // Java invalidate() (lines 205-208): a NEW glGenBuffer() and isDirty=true (for
  // context-loss recovery). The next bind therefore re-binds to the (new) handle
  // and re-uploads via glBufferData.
  test("ISS561 invalidate issues a fresh glGenBuffer and forces the next bind to re-upload") {
    val gl    = new RecordingGL20
    given Sge = makeSge(gl)
    val ibo   = new IndexBufferObject(true, 8)
    try {
      ibo.setIndices(Array[Short](9, 8), 0, 2)
      ibo.bind()
      gl.calls.clear()
      ibo.invalidate()
      assertEquals(gl.calls.toList, List(gl.GenBuffer(Handle)), "invalidate must issue exactly one fresh glGenBuffer")
      gl.calls.clear()
      ibo.bind() // isDirty set by invalidate → must re-upload
      assertEquals(
        gl.calls.toList,
        List(
          gl.BindBuffer(ElementArrayBufferRaw, Handle),
          gl.BufferData(ElementArrayBufferRaw, 4, StaticDrawRaw, Vector[Short](9, 8))
        ),
        "bind after invalidate must re-bind and re-upload (isDirty was set)"
      )
    } finally ibo.close()
  }

  // --- usage reflects isStatic: DynamicDraw for a non-static IBO ---------------

  // Java ctor (line 88): usage = isStatic ? GL_STATIC_DRAW : GL_DYNAMIC_DRAW. A
  // non-static IBO must upload with GL_DYNAMIC_DRAW.
  test("ISS561 a non-static IndexBufferObject uploads with DynamicDraw usage") {
    val gl    = new RecordingGL20
    given Sge = makeSge(gl)
    val ibo   = new IndexBufferObject(false, 8)
    try {
      ibo.setIndices(Array[Short](3, 4), 0, 2)
      gl.calls.clear()
      ibo.bind()
      assertEquals(
        gl.calls.toList,
        List(
          gl.BindBuffer(ElementArrayBufferRaw, Handle),
          gl.BufferData(ElementArrayBufferRaw, 4, DynamicDrawRaw, Vector[Short](3, 4))
        ),
        "non-static IBO must pass DynamicDraw to glBufferData (Java line 88)"
      )
    } finally ibo.close()
  }
}
