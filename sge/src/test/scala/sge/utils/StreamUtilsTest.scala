/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package utils

import java.io.{ ByteArrayInputStream, ByteArrayOutputStream, Closeable, IOException }

class StreamUtilsTest extends munit.FunSuite {

  // ---- copyStream (InputStream -> OutputStream) ----

  test("copyStream copies all bytes to output stream") {
    val data   = "Hello, World!".getBytes("UTF-8")
    val input  = new ByteArrayInputStream(data)
    val output = new ByteArrayOutputStream()
    StreamUtils.copyStream(input, output)
    assertEquals(output.toByteArray.toSeq, data.toSeq)
  }

  test("copyStream with empty input produces empty output") {
    val input  = new ByteArrayInputStream(Array.empty[Byte])
    val output = new ByteArrayOutputStream()
    StreamUtils.copyStream(input, output)
    assertEquals(output.toByteArray.length, 0)
  }

  test("copyStream with custom buffer size") {
    val data   = Array.fill(10000)((scala.util.Random.nextInt(256) - 128).toByte)
    val input  = new ByteArrayInputStream(data)
    val output = new ByteArrayOutputStream()
    StreamUtils.copyStream(input, output, 64)
    assertEquals(output.toByteArray.toSeq, data.toSeq)
  }

  test("copyStream with explicit buffer") {
    val data   = "test data".getBytes("UTF-8")
    val input  = new ByteArrayInputStream(data)
    val output = new ByteArrayOutputStream()
    val buffer = new Array[Byte](4)
    StreamUtils.copyStream(input, output, buffer)
    assertEquals(output.toByteArray.toSeq, data.toSeq)
  }

  // ---- copyStreamToByteArray ----

  test("copyStreamToByteArray returns correct bytes") {
    val data  = "Hello".getBytes("UTF-8")
    val input = new ByteArrayInputStream(data)
    val arr   = StreamUtils.copyStreamToByteArray(input)
    assertEquals(arr.toSeq, data.toSeq)
  }

  test("copyStreamToByteArray with estimated size") {
    val data  = "Hello, World!".getBytes("UTF-8")
    val input = new ByteArrayInputStream(data)
    val arr   = StreamUtils.copyStreamToByteArray(input, 4)
    assertEquals(arr.toSeq, data.toSeq)
  }

  test("copyStreamToByteArray from empty stream") {
    val input = new ByteArrayInputStream(Array.empty[Byte])
    val arr   = StreamUtils.copyStreamToByteArray(input)
    assertEquals(arr.length, 0)
  }

  // ---- copyStreamToString ----

  test("copyStreamToString returns correct string") {
    val text  = "Hello, World!"
    val input = new ByteArrayInputStream(text.getBytes("UTF-8"))
    assertEquals(StreamUtils.copyStreamToString(input), text)
  }

  test("copyStreamToString with estimated size") {
    val text  = "test"
    val input = new ByteArrayInputStream(text.getBytes("UTF-8"))
    assertEquals(StreamUtils.copyStreamToString(input, 10), text)
  }

  test("copyStreamToString with charset") {
    val text  = "Hello"
    val input = new ByteArrayInputStream(text.getBytes("UTF-8"))
    assertEquals(StreamUtils.copyStreamToString(input, text.length, Nullable("UTF-8")), text)
  }

  test("copyStreamToString with null charset uses default") {
    val text  = "Hello"
    val input = new ByteArrayInputStream(text.getBytes)
    assertEquals(StreamUtils.copyStreamToString(input, text.length, Nullable.empty), text)
  }

  test("copyStreamToString from empty stream") {
    val input = new ByteArrayInputStream(Array.empty[Byte])
    assertEquals(StreamUtils.copyStreamToString(input), "")
  }

  // ---- closeQuietly ----

  test("closeQuietly does not throw on IOException") {
    val closeable = new Closeable {
      def close(): Unit = throw new IOException("fail")
    }
    // Should not throw
    StreamUtils.closeQuietly(closeable)
  }

  test("closeQuietly does not throw on null") {
    // Should not throw - null closeable
    StreamUtils.closeQuietly(null.asInstanceOf[Closeable])
  }

  test("closeQuietly calls close on valid closeable") {
    var closed    = false
    val closeable = new Closeable {
      def close(): Unit = closed = true
    }
    StreamUtils.closeQuietly(closeable)
    assert(closed)
  }

  // ---- OptimizedByteArrayOutputStream ----

  test("OptimizedByteArrayOutputStream avoids copy when full") {
    val obs = StreamUtils.OptimizedByteArrayOutputStream(5)
    obs.write(Array[Byte](1, 2, 3, 4, 5))
    val arr = obs.toByteArray
    // When count == buf.length, should return the backing array directly (same reference)
    assert(arr eq obs.buffer)
  }

  test("OptimizedByteArrayOutputStream copies when not full") {
    val obs = StreamUtils.OptimizedByteArrayOutputStream(10)
    obs.write(Array[Byte](1, 2, 3))
    val arr = obs.toByteArray
    assertEquals(arr.length, 3)
    // Should be a copy, not the backing buffer
    assert(!(arr eq obs.buffer))
  }

  test("OptimizedByteArrayOutputStream buffer accessor") {
    val obs = StreamUtils.OptimizedByteArrayOutputStream(8)
    obs.write(Array[Byte](10, 20, 30))
    val buf = obs.buffer
    assert(buf.length >= 3)
    assertEquals(buf(0), 10.toByte)
    assertEquals(buf(1), 20.toByte)
    assertEquals(buf(2), 30.toByte)
  }

  // ---- Constants ----

  test("DEFAULT_BUFFER_SIZE is 4096") {
    assertEquals(StreamUtils.DEFAULT_BUFFER_SIZE, 4096)
  }

  test("EMPTY_BYTES is empty array") {
    assertEquals(StreamUtils.EMPTY_BYTES.length, 0)
  }

  // ---- Large data ----

  test("copyStream handles large data") {
    val data   = Array.fill(100000)((scala.util.Random.nextInt(256) - 128).toByte)
    val input  = new ByteArrayInputStream(data)
    val output = new ByteArrayOutputStream()
    StreamUtils.copyStream(input, output)
    assertEquals(output.toByteArray.toSeq, data.toSeq)
  }
}
