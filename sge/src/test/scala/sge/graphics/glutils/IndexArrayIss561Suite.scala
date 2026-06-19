/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package graphics
package glutils

import java.nio.Buffer
import java.nio.ShortBuffer

/** ISS-561 batch F: client-memory buffer coverage for IndexArray.
  *
  * IndexArray is the GL-free, ShortBuffer-backed IndexData implementation. All expected values below are derived from the LibGDX Java original at com/badlogic/gdx/graphics/glutils/IndexArray.java
  * (line numbers cited inline) and the BufferUtils.copy semantics in sge.utils.BufferUtils.
  */
class IndexArrayIss561Suite extends munit.FunSuite {

  // Java ctor (IndexArray.java:35-46): empty == (maxIndices == 0); buffer is a
  // ShortBuffer view over a byteBuffer of (maxIndices*2) bytes; both flipped so
  // their limit starts at 0.
  test("ISS561 freshly constructed IndexArray reports zero numIndices and full capacity") {
    val ia = new IndexArray(8)
    try {
      // getNumIndices == buffer.limit() (IndexArray.java:50). flip() at ctor → limit 0.
      assertEquals(ia.numIndices, 0)
      // getNumMaxIndices == buffer.capacity() (IndexArray.java:55) == maxIndices.
      assertEquals(ia.numMaxIndices, 8)
      // buffer (getBuffer, IndexArray.java:107) starts flipped: position 0, limit 0.
      val buf = ia.getBuffer(false)
      assertEquals(buf.position(), 0)
      assertEquals(buf.limit(), 0)
    } finally ia.close()
  }

  // Java empty-buffer workaround (IndexArray.java:37-40,50,55): maxIndices==0 →
  // empty==true → getNumIndices/getNumMaxIndices return 0 even though a 1-short
  // buffer is actually allocated.
  test("ISS561 maxIndices=0 reports zero indices and zero max indices (empty workaround)") {
    val ia = new IndexArray(0)
    try {
      assertEquals(ia.numIndices, 0)
      assertEquals(ia.numMaxIndices, 0)
    } finally ia.close()
  }

  // setIndices(short[],offset,count) (IndexArray.java:71-77):
  //   buffer.clear(); buffer.put(indices,offset,count); buffer.flip();
  //   byteBuffer.position(0); byteBuffer.limit(count<<1).
  // Copies indices[offset .. offset+count) into buffer[0 .. count).
  test("ISS561 setIndices(array,offset,count) copies the exact sub-range to buffer start") {
    val ia = new IndexArray(8)
    try {
      val src = Array[Short](10, 20, 30, 40, 50, 60)
      // offset 2, count 3 → expect 30,40,50 at buffer indices 0,1,2.
      ia.setIndices(src, 2, 3)
      val buf = ia.getBuffer(false)
      // flip() → position 0, limit == count.
      assertEquals(buf.position(), 0)
      assertEquals(buf.limit(), 3)
      assertEquals(ia.numIndices, 3) // buffer.limit()
      assertEquals(ia.numMaxIndices, 8) // capacity unchanged
      // Exact contents — pin every copied slot so a wrong offset/count fails.
      assertEquals(buf.get(0): Short, 30.toShort)
      assertEquals(buf.get(1): Short, 40.toShort)
      assertEquals(buf.get(2): Short, 50.toShort)
      // Absolute get() must not move the position.
      assertEquals(buf.position(), 0)
    } finally ia.close()
  }

  // setIndices replaces prior contents (clear() at IndexArray.java:72): a second
  // call with a shorter count shrinks the limit and overwrites from index 0.
  test("ISS561 setIndices(array) called twice discards old indices and resets limit") {
    val ia = new IndexArray(8)
    try {
      ia.setIndices(Array[Short](1, 2, 3, 4, 5), 0, 5)
      assertEquals(ia.numIndices, 5)
      ia.setIndices(Array[Short](7, 8), 0, 2)
      val buf = ia.getBuffer(false)
      assertEquals(buf.position(), 0)
      assertEquals(buf.limit(), 2)
      assertEquals(ia.numIndices, 2)
      assertEquals(buf.get(0): Short, 7.toShort)
      assertEquals(buf.get(1): Short, 8.toShort)
    } finally ia.close()
  }

  // setIndices(ShortBuffer) (IndexArray.java:79-88):
  //   pos = indices.position(); buffer.clear(); buffer.limit(indices.remaining());
  //   buffer.put(indices); buffer.flip(); indices.position(pos);
  // Copies indices.remaining() shorts and restores the source position.
  test("ISS561 setIndices(ShortBuffer) copies remaining() shorts and restores source position") {
    val ia = new IndexArray(8)
    try {
      val src = ShortBuffer.allocate(6)
      src.put(Array[Short](100, 101, 102, 103, 104, 105))
      src.asInstanceOf[Buffer].position(2) // remaining() == 4 → 102,103,104,105
      ia.setIndices(src)
      val buf = ia.getBuffer(false)
      assertEquals(buf.position(), 0)
      assertEquals(buf.limit(), 4) // == remaining at call time
      assertEquals(ia.numIndices, 4)
      assertEquals(buf.get(0): Short, 102.toShort)
      assertEquals(buf.get(1): Short, 103.toShort)
      assertEquals(buf.get(2): Short, 104.toShort)
      assertEquals(buf.get(3): Short, 105.toShort)
      // Source position restored to the value before the call (IndexArray.java:85).
      assertEquals(src.position(), 2)
    } finally ia.close()
  }

  // updateIndices(targetOffset,indices,offset,count) (IndexArray.java:91-96):
  //   pos=byteBuffer.position(); byteBuffer.position(targetOffset*2);
  //   BufferUtils.copy(indices,offset,byteBuffer,count); byteBuffer.position(pos).
  // Writes count shorts at SHORT index targetOffset (byte targetOffset*2) into
  // the shared byteBuffer, leaving the rest of the data and buffer's limit intact.
  test("ISS561 updateIndices writes at targetOffset without disturbing the rest") {
    val ia = new IndexArray(8)
    try {
      ia.setIndices(Array[Short](0, 1, 2, 3, 4, 5), 0, 6)
      assertEquals(ia.numIndices, 6)
      // Overwrite 2 shorts at short-index 2 with 91,92 → expect 0,1,91,92,4,5.
      ia.updateIndices(2, Array[Short](91, 92), 0, 2)
      val buf = ia.getBuffer(false)
      // updateIndices must NOT change buffer's view position/limit.
      assertEquals(buf.position(), 0)
      assertEquals(buf.limit(), 6)
      assertEquals(ia.numIndices, 6)
      assertEquals(buf.get(0): Short, 0.toShort) // untouched before target
      assertEquals(buf.get(1): Short, 1.toShort) // untouched before target
      assertEquals(buf.get(2): Short, 91.toShort) // written
      assertEquals(buf.get(3): Short, 92.toShort) // written
      assertEquals(buf.get(4): Short, 4.toShort) // untouched after target
      assertEquals(buf.get(5): Short, 5.toShort) // untouched after target
    } finally ia.close()
  }

  // updateIndices honours the source offset argument: copy starts at indices[offset].
  test("ISS561 updateIndices respects the source offset argument") {
    val ia = new IndexArray(8)
    try {
      ia.setIndices(Array[Short](0, 0, 0, 0), 0, 4)
      // From src offset 1, count 2 → 77,88 written at short-index 1.
      ia.updateIndices(1, Array[Short](66, 77, 88, 99), 1, 2)
      val buf = ia.getBuffer(false)
      assertEquals(buf.limit(), 4)
      assertEquals(buf.get(0): Short, 0.toShort)
      assertEquals(buf.get(1): Short, 77.toShort)
      assertEquals(buf.get(2): Short, 88.toShort)
      assertEquals(buf.get(3): Short, 0.toShort)
    } finally ia.close()
  }

  // getBuffer(forWriting) returns the SAME ShortBuffer regardless of the flag
  // (IndexArray.java:101-108: both overloads return `buffer`). It is a live view,
  // so its position/limit reflect the last setIndices.
  test("ISS561 getBuffer ignores forWriting flag and returns the live flipped buffer") {
    val ia = new IndexArray(8)
    try {
      ia.setIndices(Array[Short](5, 6, 7), 0, 3)
      val r = ia.getBuffer(false)
      val w = ia.getBuffer(true)
      assert(r eq w) // identical instance
      assertEquals(r.position(), 0)
      assertEquals(r.limit(), 3)
    } finally ia.close()
  }

  // bind()/unbind()/invalidate() are no-ops on the client array — IndexArray holds
  // no GL handle (IndexArray.java:111-120). They must not mutate the buffer state.
  test("ISS561 bind/unbind/invalidate are no-ops that leave buffer state untouched") {
    val ia = new IndexArray(8)
    try {
      ia.setIndices(Array[Short](9, 8, 7, 6), 0, 4)
      val before      = ia.getBuffer(false)
      val posBefore   = before.position()
      val limitBefore = before.limit()
      ia.bind()
      ia.unbind()
      ia.invalidate()
      val after = ia.getBuffer(false)
      assertEquals(after.position(), posBefore)
      assertEquals(after.limit(), limitBefore)
      assertEquals(after.get(0): Short, 9.toShort)
      assertEquals(after.get(1): Short, 8.toShort)
      assertEquals(after.get(2): Short, 7.toShort)
      assertEquals(after.get(3): Short, 6.toShort)
      assertEquals(ia.numIndices, 4)
    } finally ia.close()
  }
}
