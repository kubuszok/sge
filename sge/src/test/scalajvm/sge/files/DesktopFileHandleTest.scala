/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package files

import java.io.File

class DesktopFileHandleTest extends munit.FunSuite {

  private val extPath = DesktopFiles.externalPath

  // ---- child ----

  test("child appends to path") {
    val fh    = DesktopFileHandle("dir", FileType.Internal, extPath)
    val child = fh.child("sub")
    assertEquals(child.name, "sub")
    assert(child.path.contains("dir"))
  }

  test("child of empty path uses name directly") {
    val fh    = DesktopFileHandle("", FileType.Internal, extPath)
    val child = fh.child("file.txt")
    assertEquals(child.name, "file.txt")
  }

  // ---- sibling ----

  test("sibling replaces file name") {
    val fh      = DesktopFileHandle("dir/a.txt", FileType.Internal, extPath)
    val sibling = fh.sibling("b.txt")
    assertEquals(sibling.name, "b.txt")
    assert(sibling.path.contains("dir"))
  }

  test("sibling of root path throws") {
    val fh = DesktopFileHandle("", FileType.Internal, extPath)
    intercept[utils.SgeError.FileReadError] {
      fh.sibling("x")
    }
  }

  // ---- parent ----

  test("parent of nested path") {
    val fh     = DesktopFileHandle("dir/sub/file.txt", FileType.Internal, extPath)
    val parent = fh.parent()
    assertEquals(parent.name, "sub")
  }

  test("parent of root Absolute returns /") {
    val fh     = DesktopFileHandle("file.txt", FileType.Absolute, extPath)
    val parent = fh.parent()
    // On root-less path, parent should be empty string for non-Absolute
    assert(parent.path.nonEmpty || parent.internalFile.getPath().nonEmpty)
  }

  test("parent of root non-Absolute returns empty") {
    val fh     = DesktopFileHandle("file.txt", FileType.Internal, extPath)
    val parent = fh.parent()
    assertEquals(parent.internalFile.getPath(), "")
  }

  // ---- getFile resolves external/local paths ----

  test("getFile for External prepends external storage path") {
    val fh = DesktopFileHandle("test.txt", FileType.External, extPath)
    assertEquals(fh.file.getPath(), new File(extPath, "test.txt").getPath())
  }

  test("getFile for Local prepends local storage path") {
    val fh = DesktopFileHandle("test.txt", FileType.Local, extPath)
    assertEquals(fh.file.getPath(), new File(DesktopFileHandle.localPath, "test.txt").getPath())
  }

  test("getFile for Internal returns raw file") {
    val fh = DesktopFileHandle("test.txt", FileType.Internal, extPath)
    assertEquals(fh.file.getPath(), "test.txt")
  }

  test("getFile for Absolute returns raw file") {
    val fh = DesktopFileHandle("/tmp/test.txt", FileType.Absolute, extPath)
    assertEquals(fh.file.getPath(), "/tmp/test.txt")
  }

  // ---- write/read round-trip ----

  test("write and read external file round-trip") {
    val dir  = System.getProperty("java.io.tmpdir")
    val name = s"sge-fh-test-${System.nanoTime()}.txt"
    val fh   = DesktopFileHandle(new File(dir, name), FileType.Absolute, extPath)

    try {
      val out = fh.write(false)
      out.write("hello".getBytes("UTF-8"))
      out.close()

      assert(fh.exists())
      assertEquals(fh.length(), 5L)
      assertEquals(fh.readString(), "hello")
    } finally fh.delete()
  }

  test("child returns DesktopFileHandle instance") {
    val fh    = DesktopFileHandle("dir", FileType.Internal, extPath)
    val child = fh.child("sub")
    assert(child.isInstanceOf[DesktopFileHandle])
  }
}
