/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * ISS-865: cross-platform (Scala Native) behavioural coverage for the
 * resource-read / write path of the files subsystem.
 *
 * Two complementary read paths are pinned on Native (both previously
 * JVM-only):
 *   1. an in-memory [[FileHandleStream]] subclass whose overridden read()
 *      feeds the base-class readString/readBytes decoders — proving the shared
 *      FileHandle decode logic behaves identically on Scala Native's javalib
 *      (same in-memory-handle precedent as ObjLoaderFanRedSuite /
 *      ParticleEffectIoRedSuite);
 *   2. a REAL filesystem write→exists→length→read round-trip through a
 *      java.io.File-backed absolute [[DesktopFileHandle]], proving Scala
 *      Native's FileOutputStream / FileInputStream I/O matches the JVM
 *      DesktopFileHandleTest round-trip.
 *
 * No GL context is required for any of this.
 */
package sge
package files

import java.io.{ ByteArrayInputStream, File, InputStream }
import java.nio.charset.StandardCharsets
import munit.FunSuite
import sge.utils.SgeError
import lowlevel.Nullable

class DesktopFileHandleReadIss865Suite extends FunSuite {

  private val ext = "/iss865-ext/"

  /** In-memory file: only read() is backed; base FileHandle decodes it. */
  final private class StringFileHandle(path: String, content: String) extends FileHandleStream(path) {
    override def read(): InputStream =
      new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))
  }

  // ── in-memory read: base-class decode logic on Native ───────────────────

  test("ISS-865 Native: readString decodes the backing stream as UTF-8") {
    assertEquals(new StringFileHandle("mem.txt", "hello world").readString(), "hello world")
  }

  test("ISS-865 Native: readString round-trips multibyte UTF-8 content") {
    val text = "grüße — 日本語"
    assertEquals(new StringFileHandle("mem.txt", text).readString(), text)
  }

  test("ISS-865 Native: readBytes returns the exact backing bytes") {
    val text = "abc123"
    assertEquals(
      new StringFileHandle("mem.txt", text).readBytes().toList,
      text.getBytes(StandardCharsets.UTF_8).toList
    )
  }

  test("ISS-865 Native: FileHandleStream reports exists=true and length=0 by contract") {
    val fh = new StringFileHandle("mem.txt", "x")
    assertEquals(fh.exists(), true)
    assertEquals(fh.length(), 0L)
  }

  // ── real filesystem round-trip through java.io.File ─────────────────────

  test("ISS-865 Native: write → exists → length → readString round-trips on the real filesystem") {
    val dir  = Nullable(System.getProperty("java.io.tmpdir")).getOrElse(".")
    val file = new File(dir, s"sge-iss865-native-${System.nanoTime()}.txt")
    val fh   = DesktopFileHandle(file, FileType.Absolute, ext)
    try {
      val out = fh.write(false)
      out.write("hello".getBytes(StandardCharsets.UTF_8))
      out.close()

      assert(fh.exists(), "file must exist after write")
      assertEquals(fh.length(), 5L)
      assertEquals(fh.readString(), "hello")
      assertEquals(fh.readBytes().toList, "hello".getBytes(StandardCharsets.UTF_8).toList)
    } finally fh.delete()
  }

  test("ISS-865 Native: exists is false for an absent absolute file") {
    val file = new File(Nullable(System.getProperty("java.io.tmpdir")).getOrElse("."), s"sge-iss865-absent-${System.nanoTime()}.bin")
    assertEquals(DesktopFileHandle(file, FileType.Absolute, ext).exists(), false)
  }

  test("ISS-865 Native: readString on an absent absolute file throws FileReadError") {
    val file = new File(Nullable(System.getProperty("java.io.tmpdir")).getOrElse("."), s"sge-iss865-absent-${System.nanoTime()}.bin")
    intercept[SgeError.FileReadError] {
      DesktopFileHandle(file, FileType.Absolute, ext).readString()
    }
  }
}
