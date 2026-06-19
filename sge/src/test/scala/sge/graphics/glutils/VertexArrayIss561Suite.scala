/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package graphics
package glutils

/** ISS-561 batch F: client-memory buffer coverage for VertexArray.
  *
  * VertexArray is the GL-free, FloatBuffer-backed VertexData implementation. All expected values below are derived from the LibGDX Java original at com/badlogic/gdx/graphics/glutils/VertexArray.java
  * (line numbers cited inline) and the BufferUtils.copy semantics in sge.utils.BufferUtils.
  *
  * The attribute layout used throughout is a single VertexAttribute.Position() (3 float components → sizeInBytes 12), so attributes.vertexSize == 12 bytes and one vertex == 3 floats.
  */
class VertexArrayIss561Suite extends munit.FunSuite {

  // attributes.vertexSize == 12 (3 floats * 4 bytes) for VertexAttribute.Position()

  private def newArray(numVertices: Int): VertexArray =
    new VertexArray(numVertices, VertexAttribute.Position())

  // Java ctor (VertexArray.java:57-63): byteBuffer of (vertexSize*numVertices)
  // bytes; buffer is its FloatBuffer view; both flipped → limit 0.
  test("ISS561 freshly constructed VertexArray reports zero vertices and full max vertices") {
    val va = newArray(4)
    try {
      // getNumVertices == buffer.limit()*4/vertexSize (VertexArray.java:84). limit 0 → 0.
      assertEquals(va.numVertices, 0)
      // getNumMaxVertices == byteBuffer.capacity()/vertexSize (VertexArray.java:88).
      // capacity == vertexSize*4 == 48 → 48/12 == 4.
      assertEquals(va.numMaxVertices, 4)
      val buf = va.getBuffer(false)
      assertEquals(buf.position(), 0)
      assertEquals(buf.limit(), 0)
    } finally va.close()
  }

  // numMaxVertices is computed purely from the byte capacity (VertexArray.java:88):
  // 7 vertices → capacity 84 bytes → 84/12 == 7. Pins the division.
  test("ISS561 numMaxVertices derives from byteBuffer capacity / vertexSize") {
    val va = newArray(7)
    try
      assertEquals(va.numMaxVertices, 7)
    finally va.close()
  }

  // setVertices(float[],offset,count) (VertexArray.java:92-96):
  //   BufferUtils.copy(vertices,byteBuffer,count,offset); buffer.position(0); buffer.limit(count).
  // Copies vertices[offset .. offset+count) into buffer[0 .. count) (floats).
  test("ISS561 setVertices(array,offset,count) copies the exact float sub-range") {
    val va = newArray(4)
    try {
      val src = Array[Float](0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f)
      // offset 3, count 6 → expect 3,4,5,6,7,8 at buffer floats 0..5.
      va.setVertices(src, 3, 6)
      val buf = va.getBuffer(false)
      // position(0); limit(count) (VertexArray.java:94-95).
      assertEquals(buf.position(), 0)
      assertEquals(buf.limit(), 6)
      // getNumVertices == limit*4/vertexSize == 6*4/12 == 2.
      assertEquals(va.numVertices, 2)
      assertEquals(va.numMaxVertices, 4) // capacity unchanged
      assertEqualsFloat(buf.get(0), 3f, 0f)
      assertEqualsFloat(buf.get(1), 4f, 0f)
      assertEqualsFloat(buf.get(2), 5f, 0f)
      assertEqualsFloat(buf.get(3), 6f, 0f)
      assertEqualsFloat(buf.get(4), 7f, 0f)
      assertEqualsFloat(buf.get(5), 8f, 0f)
      assertEquals(buf.position(), 0) // absolute get() must not move position
    } finally va.close()
  }

  // numVertices reflects the limit set by setVertices: 12 floats / 3 floats-per-vertex
  // == 4 vertices (VertexArray.java:84). Pins the *4/vertexSize arithmetic.
  test("ISS561 numVertices equals limit*4/vertexSize after a full setVertices") {
    val va = newArray(4)
    try {
      val src = Array[Float](0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 11f)
      va.setVertices(src, 0, 12)
      assertEquals(va.getBuffer(false).limit(), 12)
      assertEquals(va.numVertices, 4) // 12*4/12
    } finally va.close()
  }

  // updateVertices(targetOffset,vertices,sourceOffset,count) (VertexArray.java:99-104):
  //   pos=byteBuffer.position(); byteBuffer.position(targetOffset*4);
  //   BufferUtils.copy(vertices,sourceOffset,count,byteBuffer); byteBuffer.position(pos).
  // Writes count floats at FLOAT index targetOffset (byte targetOffset*4) into the
  // shared byteBuffer, leaving the rest of the data and buffer's limit intact.
  test("ISS561 updateVertices does a partial update at targetOffset without disturbing the rest") {
    val va = newArray(4)
    try {
      va.setVertices(Array[Float](0f, 1f, 2f, 3f, 4f, 5f), 0, 6)
      assertEquals(va.numVertices, 2) // 6*4/12
      // Overwrite 2 floats at float-index 2 with 20,21 → expect 0,1,20,21,4,5.
      va.updateVertices(2, Array[Float](20f, 21f), 0, 2)
      val buf = va.getBuffer(false)
      // updateVertices must NOT change the FloatBuffer view position/limit.
      assertEquals(buf.position(), 0)
      assertEquals(buf.limit(), 6)
      assertEquals(va.numVertices, 2)
      assertEqualsFloat(buf.get(0), 0f, 0f) // before target — untouched
      assertEqualsFloat(buf.get(1), 1f, 0f) // before target — untouched
      assertEqualsFloat(buf.get(2), 20f, 0f) // written
      assertEqualsFloat(buf.get(3), 21f, 0f) // written
      assertEqualsFloat(buf.get(4), 4f, 0f) // after target — untouched
      assertEqualsFloat(buf.get(5), 5f, 0f) // after target — untouched
    } finally va.close()
  }

  // updateVertices honours the source offset argument: copy starts at vertices[sourceOffset].
  test("ISS561 updateVertices respects the source offset argument") {
    val va = newArray(4)
    try {
      va.setVertices(Array[Float](0f, 0f, 0f, 0f), 0, 4)
      // From source offset 1, count 2 → 31,32 written at float-index 1.
      va.updateVertices(1, Array[Float](30f, 31f, 32f, 33f), 1, 2)
      val buf = va.getBuffer(false)
      assertEquals(buf.limit(), 4)
      assertEqualsFloat(buf.get(0), 0f, 0f)
      assertEqualsFloat(buf.get(1), 31f, 0f)
      assertEqualsFloat(buf.get(2), 32f, 0f)
      assertEqualsFloat(buf.get(3), 0f, 0f)
    } finally va.close()
  }

  // getBuffer(forWriting) returns the SAME FloatBuffer regardless of the flag
  // (VertexArray.java:73-80). Live view → position/limit reflect last setVertices.
  test("ISS561 getBuffer ignores forWriting flag and returns the live flipped buffer") {
    val va = newArray(4)
    try {
      va.setVertices(Array[Float](1f, 2f, 3f), 0, 3)
      val r = va.getBuffer(false)
      val w = va.getBuffer(true)
      assert(r eq w)
      assertEquals(r.position(), 0)
      assertEquals(r.limit(), 3)
    } finally va.close()
  }

  // isBound starts false and bind/unbind are the only things that toggle it
  // (VertexArray.java:43,150,174). With no GL bind invoked, it stays false and
  // the buffer state stays intact.
  test("ISS561 isBound starts false on the client array and buffer state is unaffected") {
    val va = newArray(4)
    try {
      va.setVertices(Array[Float](9f, 8f, 7f, 6f), 0, 4)
      assertEquals(va.isBound, false)
      val buf = va.getBuffer(false)
      assertEquals(buf.position(), 0)
      assertEquals(buf.limit(), 4)
      assertEqualsFloat(buf.get(0), 9f, 0f)
      assertEqualsFloat(buf.get(3), 6f, 0f)
    } finally va.close()
  }

  // invalidate() is a no-op (VertexArray.java:183-184): must not mutate buffer state.
  test("ISS561 invalidate is a no-op that leaves buffer state untouched") {
    val va = newArray(4)
    try {
      va.setVertices(Array[Float](4f, 5f, 6f), 0, 3)
      val before      = va.getBuffer(false)
      val posBefore   = before.position()
      val limitBefore = before.limit()
      va.invalidate()
      val after = va.getBuffer(false)
      assertEquals(after.position(), posBefore)
      assertEquals(after.limit(), limitBefore)
      assertEqualsFloat(after.get(0), 4f, 0f)
      assertEqualsFloat(after.get(1), 5f, 0f)
      assertEqualsFloat(after.get(2), 6f, 0f)
      assertEquals(va.numVertices, 1) // 3*4/12
    } finally va.close()
  }
}
