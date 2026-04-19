/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package utils

import java.io.{ ByteArrayInputStream, ByteArrayOutputStream }

class DataInputOutputTest extends munit.FunSuite {

  // ---- Round-trip: writeInt / readInt ----

  test("round-trip small positive int (optimizePositive = true)") {
    val baos = new ByteArrayOutputStream()
    val out  = DataOutput(baos)
    out.writeInt(42, optimizePositive = true)
    out.flush()

    val bais = new ByteArrayInputStream(baos.toByteArray)
    val in   = DataInput(bais)
    assertEquals(in.readInt(optimizePositive = true), 42)
  }

  test("round-trip zero (optimizePositive = true)") {
    val baos = new ByteArrayOutputStream()
    val out  = DataOutput(baos)
    out.writeInt(0, optimizePositive = true)
    out.flush()

    val bais = new ByteArrayInputStream(baos.toByteArray)
    val in   = DataInput(bais)
    assertEquals(in.readInt(optimizePositive = true), 0)
  }

  test("round-trip small positive int (optimizePositive = false)") {
    val baos = new ByteArrayOutputStream()
    val out  = DataOutput(baos)
    out.writeInt(42, optimizePositive = false)
    out.flush()

    val bais = new ByteArrayInputStream(baos.toByteArray)
    val in   = DataInput(bais)
    assertEquals(in.readInt(optimizePositive = false), 42)
  }

  test("round-trip negative int (optimizePositive = false)") {
    val baos = new ByteArrayOutputStream()
    val out  = DataOutput(baos)
    out.writeInt(-123, optimizePositive = false)
    out.flush()

    val bais = new ByteArrayInputStream(baos.toByteArray)
    val in   = DataInput(bais)
    assertEquals(in.readInt(optimizePositive = false), -123)
  }

  test("round-trip negative int (optimizePositive = true)") {
    val baos = new ByteArrayOutputStream()
    val out  = DataOutput(baos)
    out.writeInt(-1, optimizePositive = true)
    out.flush()

    val bais = new ByteArrayInputStream(baos.toByteArray)
    val in   = DataInput(bais)
    assertEquals(in.readInt(optimizePositive = true), -1)
  }

  test("round-trip large int") {
    val baos = new ByteArrayOutputStream()
    val out  = DataOutput(baos)
    out.writeInt(Int.MaxValue, optimizePositive = true)
    out.flush()

    val bais = new ByteArrayInputStream(baos.toByteArray)
    val in   = DataInput(bais)
    assertEquals(in.readInt(optimizePositive = true), Int.MaxValue)
  }

  test("round-trip Int.MinValue") {
    val baos = new ByteArrayOutputStream()
    val out  = DataOutput(baos)
    out.writeInt(Int.MinValue, optimizePositive = false)
    out.flush()

    val bais = new ByteArrayInputStream(baos.toByteArray)
    val in   = DataInput(bais)
    assertEquals(in.readInt(optimizePositive = false), Int.MinValue)
  }

  test("writeInt returns correct byte count for small values") {
    val baos = new ByteArrayOutputStream()
    val out  = DataOutput(baos)
    assertEquals(out.writeInt(0, optimizePositive = true), 1)
    assertEquals(out.writeInt(127, optimizePositive = true), 1)
  }

  test("writeInt returns correct byte count for medium values") {
    val baos = new ByteArrayOutputStream()
    val out  = DataOutput(baos)
    assertEquals(out.writeInt(128, optimizePositive = true), 2)
    assertEquals(out.writeInt(16383, optimizePositive = true), 2)
  }

  test("writeInt returns correct byte count for larger values") {
    val baos = new ByteArrayOutputStream()
    val out  = DataOutput(baos)
    assertEquals(out.writeInt(16384, optimizePositive = true), 3)
  }

  test("writeInt returns 5 for very large values") {
    val baos = new ByteArrayOutputStream()
    val out  = DataOutput(baos)
    assertEquals(out.writeInt(Int.MaxValue, optimizePositive = true), 5)
  }

  // ---- Round-trip: writeString / readString ----

  test("round-trip null string") {
    val baos = new ByteArrayOutputStream()
    val out  = DataOutput(baos)
    out.writeString(Nullable.empty)
    out.flush()

    val bais = new ByteArrayInputStream(baos.toByteArray)
    val in   = DataInput(bais)
    assert(in.readString().isEmpty)
  }

  test("round-trip empty string") {
    val baos = new ByteArrayOutputStream()
    val out  = DataOutput(baos)
    out.writeString(Nullable(""))
    out.flush()

    val bais = new ByteArrayInputStream(baos.toByteArray)
    val in   = DataInput(bais)
    val read = in.readString()
    assert(read.isDefined)
    assertEquals(read.getOrElse(fail("expected non-empty")), "")
  }

  test("round-trip ASCII string") {
    val baos = new ByteArrayOutputStream()
    val out  = DataOutput(baos)
    out.writeString(Nullable("Hello, World!"))
    out.flush()

    val bais = new ByteArrayInputStream(baos.toByteArray)
    val in   = DataInput(bais)
    val read = in.readString()
    assert(read.isDefined)
    assertEquals(read.getOrElse(fail("expected non-empty")), "Hello, World!")
  }

  test("round-trip string with Unicode characters") {
    val baos = new ByteArrayOutputStream()
    val out  = DataOutput(baos)
    val str  = "Hello \u00e9\u00e8\u00ea"
    out.writeString(Nullable(str))
    out.flush()

    val bais = new ByteArrayInputStream(baos.toByteArray)
    val in   = DataInput(bais)
    val read = in.readString()
    assert(read.isDefined)
    assertEquals(read.getOrElse(fail("expected non-empty")), str)
  }

  test("round-trip string with 3-byte UTF-8 characters") {
    val baos = new ByteArrayOutputStream()
    val out  = DataOutput(baos)
    val str  = "\u4e16\u754c" // Chinese characters
    out.writeString(Nullable(str))
    out.flush()

    val bais = new ByteArrayInputStream(baos.toByteArray)
    val in   = DataInput(bais)
    val read = in.readString()
    assert(read.isDefined)
    assertEquals(read.getOrElse(fail("expected non-empty")), str)
  }

  test("round-trip multiple strings") {
    val baos = new ByteArrayOutputStream()
    val out  = DataOutput(baos)
    out.writeString(Nullable("first"))
    out.writeString(Nullable.empty)
    out.writeString(Nullable(""))
    out.writeString(Nullable("last"))
    out.flush()

    val bais = new ByteArrayInputStream(baos.toByteArray)
    val in   = DataInput(bais)
    assertEquals(in.readString().getOrElse(fail("expected non-empty")), "first")
    assert(in.readString().isEmpty)
    assertEquals(in.readString().getOrElse(fail("expected non-empty")), "")
    assertEquals(in.readString().getOrElse(fail("expected non-empty")), "last")
  }

  // ---- Round-trip: multiple ints ----

  test("round-trip multiple ints") {
    val baos   = new ByteArrayOutputStream()
    val out    = DataOutput(baos)
    val values = Seq(0, 1, 127, 128, 16383, 16384, -1, -1000, Int.MaxValue, Int.MinValue)
    values.foreach(v => out.writeInt(v, optimizePositive = false))
    out.flush()

    val bais = new ByteArrayInputStream(baos.toByteArray)
    val in   = DataInput(bais)
    values.foreach { expected =>
      assertEquals(in.readInt(optimizePositive = false), expected)
    }
  }

  // ---- Mixed data types ----

  test("round-trip mixed ints and strings") {
    val baos = new ByteArrayOutputStream()
    val out  = DataOutput(baos)
    out.writeInt(42, optimizePositive = true)
    out.writeString(Nullable("test"))
    out.writeInt(-7, optimizePositive = false)
    out.writeString(Nullable.empty)
    out.flush()

    val bais = new ByteArrayInputStream(baos.toByteArray)
    val in   = DataInput(bais)
    assertEquals(in.readInt(optimizePositive = true), 42)
    assertEquals(in.readString().getOrElse(fail("expected non-empty")), "test")
    assertEquals(in.readInt(optimizePositive = false), -7)
    assert(in.readString().isEmpty)
  }

  // ---- Edge: single character string ----

  test("round-trip single character string") {
    val baos = new ByteArrayOutputStream()
    val out  = DataOutput(baos)
    out.writeString(Nullable("x"))
    out.flush()

    val bais = new ByteArrayInputStream(baos.toByteArray)
    val in   = DataInput(bais)
    assertEquals(in.readString().getOrElse(fail("expected non-empty")), "x")
  }

  // ---- Edge: string with mixed ASCII and non-ASCII ----

  test("round-trip string starting with ASCII then non-ASCII") {
    val baos = new ByteArrayOutputStream()
    val out  = DataOutput(baos)
    val str  = "abc\u00e9xyz"
    out.writeString(Nullable(str))
    out.flush()

    val bais = new ByteArrayInputStream(baos.toByteArray)
    val in   = DataInput(bais)
    assertEquals(in.readString().getOrElse(fail("expected non-empty")), str)
  }
}
