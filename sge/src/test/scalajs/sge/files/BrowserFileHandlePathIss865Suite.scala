/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * ISS-865: cross-platform (JS) behavioural coverage for the files subsystem.
 *
 * The assets + files subsystems previously had JVM-only tests
 * (under sge/src/test/scalajvm/sge/{files,assets}), so the browser
 * FileHandle behaviour was unpinned. On Scala.js the FileHandle at runtime is
 * always a [[BrowserFileHandle]] (constructed by [[BrowserFiles]]); it
 * reimplements every path/name operation as pure string logic because
 * java.io.File is a no-op shim on JS (getParent/getParentFile THROW there).
 *
 * These are BEHAVIOURAL exact-value tests that pin the browser path algebra to
 * the SAME results the JVM DesktopFileHandle produces (see the JVM twin,
 * sge/src/test/scalajvm/sge/files/DesktopFileHandleTest.scala) — so a JS/JVM
 * divergence in name/extension/parent/child/sibling/path would fail here.
 * They are pure-logic (no I/O), hence deterministic and GL-free.
 */
package sge
package files

import munit.FunSuite
import sge.utils.SgeError

class BrowserFileHandlePathIss865Suite extends FunSuite {

  private val files = new BrowserFiles(new BrowserAssetLoader)

  private def internal(path: String): FileHandle = files.internal(path)

  // ── path / name / extension (nested path) ───────────────────────────────

  test("ISS-865 JS: path is preserved verbatim for a forward-slash path") {
    assertEquals(internal("dir/sub/file.txt").path, "dir/sub/file.txt")
  }

  test("ISS-865 JS: name strips all parent segments") {
    assertEquals(internal("dir/sub/file.txt").name, "file.txt")
  }

  test("ISS-865 JS: extension is the substring after the last dot") {
    assertEquals(internal("dir/sub/file.txt").extension, "txt")
  }

  test("ISS-865 JS: nameWithoutExtension drops parents and extension") {
    assertEquals(internal("dir/sub/file.txt").nameWithoutExtension, "file")
  }

  test("ISS-865 JS: pathWithoutExtension keeps parents, drops extension") {
    assertEquals(internal("dir/sub/file.txt").pathWithoutExtension, "dir/sub/file")
  }

  test("ISS-865 JS: extension is empty when the name has no dot") {
    assertEquals(internal("dir/README").extension, "")
    assertEquals(internal("dir/README").nameWithoutExtension, "README")
  }

  test("ISS-865 JS: extension uses the LAST dot (multi-dot name)") {
    assertEquals(internal("a/archive.tar.gz").extension, "gz")
    assertEquals(internal("a/archive.tar.gz").nameWithoutExtension, "archive.tar")
  }

  // ── path normalization (backslash → forward slash) ──────────────────────

  test("ISS-865 JS: backslashes in the path are normalized to forward slashes") {
    assertEquals(internal("dir\\sub\\file.txt").path, "dir/sub/file.txt")
    assertEquals(internal("dir\\sub\\file.txt").name, "file.txt")
  }

  // ── child ───────────────────────────────────────────────────────────────

  test("ISS-865 JS: child appends a segment to a non-empty path") {
    val c = internal("dir").child("sub")
    assertEquals(c.path, "dir/sub")
    assertEquals(c.name, "sub")
  }

  test("ISS-865 JS: child of the empty path is the name itself") {
    val c = internal("").child("file.txt")
    assertEquals(c.path, "file.txt")
    assertEquals(c.name, "file.txt")
  }

  test("ISS-865 JS: child does not double a trailing slash") {
    assertEquals(internal("dir/").child("f").path, "dir/f")
  }

  test("ISS-865 JS: child returns another BrowserFileHandle") {
    assert(internal("dir").child("sub").isInstanceOf[BrowserFileHandle])
  }

  // ── sibling ─────────────────────────────────────────────────────────────

  test("ISS-865 JS: sibling replaces the last segment") {
    val s = internal("dir/a.txt").sibling("b.txt")
    assertEquals(s.path, "dir/b.txt")
    assertEquals(s.name, "b.txt")
  }

  test("ISS-865 JS: sibling of a top-level file has no parent prefix") {
    assertEquals(internal("a.txt").sibling("b.txt").path, "b.txt")
  }

  test("ISS-865 JS: sibling of the root path throws FileReadError") {
    intercept[SgeError.FileReadError] {
      internal("").sibling("x")
    }
  }

  // ── parent ──────────────────────────────────────────────────────────────

  test("ISS-865 JS: parent of a nested path drops the last segment") {
    val p = internal("dir/sub/file.txt").parent()
    assertEquals(p.path, "dir/sub")
    assertEquals(p.name, "sub")
  }

  test("ISS-865 JS: parent of a top-level file is the empty path") {
    assertEquals(internal("file.txt").parent().path, "")
  }
}
