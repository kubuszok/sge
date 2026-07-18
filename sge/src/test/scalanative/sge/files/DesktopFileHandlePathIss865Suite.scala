/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * ISS-865: cross-platform (Scala Native) behavioural coverage for the files
 * subsystem.
 *
 * The assets + files subsystems previously had JVM-only tests
 * (under sge/src/test/scalajvm/sge/{files,assets}), so the Scala Native
 * FileHandle behaviour was unpinned. Native reuses the shared `scaladesktop`
 * backend — the runtime FileHandle is a [[DesktopFileHandle]] backed by
 * java.io.File (fully supported by Scala Native's javalib) — but no native
 * test exercised its path algebra.
 *
 * This is the Native twin of sge/src/test/scalajvm/sge/files/
 * DesktopFileHandleTest.scala: BEHAVIOURAL exact-value tests for
 * name/extension/parent/child/sibling/path. Pure-logic (no I/O), deterministic
 * and GL-free. A Native/JVM divergence in the path algebra would fail here.
 */
package sge
package files

import java.io.File
import munit.FunSuite
import sge.utils.SgeError

class DesktopFileHandlePathIss865Suite extends FunSuite {

  private val ext = "/iss865-ext/"

  private def internal(path: String): FileHandle =
    DesktopFileHandle(path, FileType.Internal, ext)

  // ── path / name / extension (nested path) ───────────────────────────────

  test("ISS-865 Native: path is preserved for a forward-slash path") {
    assertEquals(internal("dir/sub/file.txt").path, "dir/sub/file.txt")
  }

  test("ISS-865 Native: name strips all parent segments") {
    assertEquals(internal("dir/sub/file.txt").name, "file.txt")
  }

  test("ISS-865 Native: extension is the substring after the last dot") {
    assertEquals(internal("dir/sub/file.txt").extension, "txt")
  }

  test("ISS-865 Native: nameWithoutExtension drops parents and extension") {
    assertEquals(internal("dir/sub/file.txt").nameWithoutExtension, "file")
  }

  test("ISS-865 Native: pathWithoutExtension keeps parents, drops extension") {
    assertEquals(internal("dir/sub/file.txt").pathWithoutExtension, "dir/sub/file")
  }

  test("ISS-865 Native: extension is empty when the name has no dot") {
    assertEquals(internal("dir/README").extension, "")
    assertEquals(internal("dir/README").nameWithoutExtension, "README")
  }

  test("ISS-865 Native: extension uses the LAST dot (multi-dot name)") {
    assertEquals(internal("a/archive.tar.gz").extension, "gz")
    assertEquals(internal("a/archive.tar.gz").nameWithoutExtension, "archive.tar")
  }

  // ── child ───────────────────────────────────────────────────────────────

  test("ISS-865 Native: child appends a segment to a non-empty path") {
    val c = internal("dir").child("sub")
    assertEquals(c.path, "dir/sub")
    assertEquals(c.name, "sub")
  }

  test("ISS-865 Native: child of the empty path is the name itself") {
    val c = internal("").child("file.txt")
    assertEquals(c.name, "file.txt")
    assertEquals(c.path, "file.txt")
  }

  test("ISS-865 Native: child returns a DesktopFileHandle") {
    assert(internal("dir").child("sub").isInstanceOf[DesktopFileHandle])
  }

  // ── sibling ─────────────────────────────────────────────────────────────

  test("ISS-865 Native: sibling replaces the last segment") {
    val s = internal("dir/a.txt").sibling("b.txt")
    assertEquals(s.path, "dir/b.txt")
    assertEquals(s.name, "b.txt")
  }

  test("ISS-865 Native: sibling of the root path throws FileReadError") {
    intercept[SgeError.FileReadError] {
      internal("").sibling("x")
    }
  }

  // ── parent ──────────────────────────────────────────────────────────────

  test("ISS-865 Native: parent of a nested path is the containing directory") {
    val p = internal("dir/sub/file.txt").parent()
    assertEquals(p.name, "sub")
    assertEquals(p.path, "dir/sub")
  }

  test("ISS-865 Native: parent of a top-level Internal file is the empty path") {
    assertEquals(internal("file.txt").parent().internalFile.getPath(), "")
  }

  // ── file() resolution for External / Internal ───────────────────────────

  test("ISS-865 Native: file() for External prepends the external storage path") {
    val fh = DesktopFileHandle("test.txt", FileType.External, ext)
    assertEquals(fh.file.getPath(), new File(ext, "test.txt").getPath())
  }

  test("ISS-865 Native: file() for Internal returns the raw file") {
    assertEquals(DesktopFileHandle("test.txt", FileType.Internal, ext).file.getPath(), "test.txt")
  }

  test("ISS-865 Native: file() for Absolute returns the raw file") {
    val fh = DesktopFileHandle("/tmp/test.txt", FileType.Absolute, ext)
    assertEquals(fh.file.getPath(), new File("/tmp/test.txt").getPath())
  }

  // ── equals / hashCode contract (path + fileType) ────────────────────────

  test("ISS-865 Native: equal path + fileType implies equal handles and hashCodes") {
    val a = internal("dir/x.txt")
    val b = internal("dir/x.txt")
    assertEquals(a, b)
    assertEquals(a.hashCode(), b.hashCode())
  }

  test("ISS-865 Native: differing fileType makes handles unequal") {
    val a = DesktopFileHandle("dir/x.txt", FileType.Internal, ext)
    val b = DesktopFileHandle("dir/x.txt", FileType.Classpath, ext)
    assertNotEquals(a, b)
  }
}
