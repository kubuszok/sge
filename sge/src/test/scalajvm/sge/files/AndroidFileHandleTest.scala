/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package files

import java.io.{ ByteArrayInputStream, File, FileDescriptor, InputStream }
import sge.platform.android.FilesOps

class AndroidFileHandleTest extends munit.FunSuite {

  // ── Stub FilesOps ─────────────────────────────────────────────────────

  private class StubFilesOps(
    internalFiles: Map[String, Array[Byte]] = Map.empty,
    internalDirs:  Map[String, Array[String]] = Map.empty,
    extStorage:    String | Null = null,
    localPath:     String = "/data/data/com.test/files/"
  ) extends FilesOps {

    override def openInternal(path: String): InputStream =
      internalFiles.get(path) match {
        case Some(data) => new ByteArrayInputStream(data)
        case None       => throw new java.io.IOException(s"Not found: $path")
      }

    override def listInternal(path: String): Array[String] =
      internalDirs.getOrElse(path, Array.empty)

    override def openInternalFd(path: String): (FileDescriptor, Long, Long) | Null = null

    override def internalFileLength(path: String): Long =
      internalFiles.get(path).map(_.length.toLong).getOrElse(-1L)

    override def localStoragePath:    String        = localPath
    override def externalStoragePath: String | Null = extStorage
  }

  private val ops = new StubFilesOps()

  // ── child ─────────────────────────────────────────────────────────────

  test("child appends to path") {
    val fh    = AndroidFileHandle(new File("dir"), FileType.Internal, ops)
    val child = fh.child("sub")
    assertEquals(child.name, "sub")
    assert(child.path.contains("dir"))
    assert(child.isInstanceOf[AndroidFileHandle])
  }

  test("child of empty path uses name directly") {
    val fh    = AndroidFileHandle(new File(""), FileType.Internal, ops)
    val child = fh.child("file.txt")
    assertEquals(child.name, "file.txt")
  }

  // ── sibling ───────────────────────────────────────────────────────────

  test("sibling replaces file name") {
    val fh  = AndroidFileHandle(new File("dir/a.txt"), FileType.Internal, ops)
    val sib = fh.sibling("b.txt")
    assertEquals(sib.name, "b.txt")
    assert(sib.path.contains("dir"))
    assert(sib.isInstanceOf[AndroidFileHandle])
  }

  test("sibling of root path throws") {
    val fh = AndroidFileHandle(new File(""), FileType.Internal, ops)
    intercept[utils.SgeError.FileReadError] {
      fh.sibling("x")
    }
  }

  // ── parent ────────────────────────────────────────────────────────────

  test("parent of nested path") {
    val fh = AndroidFileHandle(new File("dir/sub/file.txt"), FileType.Internal, ops)
    val p  = fh.parent()
    assertEquals(p.name, "sub")
    assert(p.isInstanceOf[AndroidFileHandle])
  }

  test("parent of root Absolute path returns /") {
    val fh = AndroidFileHandle(new File(""), FileType.Absolute, ops)
    val p  = fh.parent()
    assertEquals(p.file.getPath(), new File("/").getPath())
  }

  test("parent of root Internal path returns empty") {
    val fh = AndroidFileHandle(new File(""), FileType.Internal, ops)
    val p  = fh.parent()
    assertEquals(p.file.getPath(), "")
  }

  // ── read ──────────────────────────────────────────────────────────────

  test("read delegates to FilesOps for Internal files") {
    val data    = "hello".getBytes()
    val stubOps = new StubFilesOps(internalFiles = Map("test.txt" -> data))
    val fh      = AndroidFileHandle(new File("test.txt"), FileType.Internal, stubOps)
    val result  = fh.read().readAllBytes()
    assertEquals(new String(result), "hello")
  }

  test("read throws FileReadError for missing Internal file") {
    val fh = AndroidFileHandle(new File("missing.txt"), FileType.Internal, ops)
    intercept[utils.SgeError.FileReadError] {
      fh.read()
    }
  }

  // ── list ──────────────────────────────────────────────────────────────

  test("list delegates to FilesOps for Internal files") {
    val stubOps = new StubFilesOps(internalDirs = Map("assets" -> Array("a.txt", "b.png")))
    val fh      = AndroidFileHandle(new File("assets"), FileType.Internal, stubOps)
    val result  = fh.list()
    assertEquals(result.length, 2)
    assertEquals(result.map(_.name).toSet, Set("a.txt", "b.png"))
  }

  test("list with suffix filters Internal files") {
    val stubOps = new StubFilesOps(internalDirs = Map("assets" -> Array("a.txt", "b.png", "c.txt")))
    val fh      = AndroidFileHandle(new File("assets"), FileType.Internal, stubOps)
    val result  = fh.list(".txt")
    assertEquals(result.length, 2)
    assertEquals(result.map(_.name).toSet, Set("a.txt", "c.txt"))
  }

  // ── isDirectory ───────────────────────────────────────────────────────

  test("isDirectory returns true when listInternal has entries") {
    val stubOps = new StubFilesOps(internalDirs = Map("dir" -> Array("child")))
    val fh      = AndroidFileHandle(new File("dir"), FileType.Internal, stubOps)
    assert(fh.isDirectory())
  }

  test("isDirectory returns false when listInternal is empty") {
    val fh = AndroidFileHandle(new File("notadir"), FileType.Internal, ops)
    assert(!fh.isDirectory())
  }

  // ── exists ────────────────────────────────────────────────────────────

  test("exists returns true when openInternal succeeds") {
    val stubOps = new StubFilesOps(internalFiles = Map("found.txt" -> Array.emptyByteArray))
    val fh      = AndroidFileHandle(new File("found.txt"), FileType.Internal, stubOps)
    assert(fh.exists())
  }

  test("exists falls back to listInternal for directories") {
    val stubOps = new StubFilesOps(internalDirs = Map("mydir" -> Array("child")))
    val fh      = AndroidFileHandle(new File("mydir"), FileType.Internal, stubOps)
    assert(fh.exists())
  }

  test("exists returns false when both open and list fail") {
    val fh = AndroidFileHandle(new File("missing"), FileType.Internal, ops)
    assert(!fh.exists())
  }

  // ── length ────────────────────────────────────────────────────────────

  test("length returns file size for Internal files") {
    val data    = Array.fill(42)(0.toByte)
    val stubOps = new StubFilesOps(internalFiles = Map("sized.bin" -> data))
    val fh      = AndroidFileHandle(new File("sized.bin"), FileType.Internal, stubOps)
    assertEquals(fh.length(), 42L)
  }

  test("length returns 0 for unknown Internal files") {
    val fh = AndroidFileHandle(new File("unknown.bin"), FileType.Internal, ops)
    assertEquals(fh.length(), 0L)
  }

  // ── getFile with storage paths ────────────────────────────────────────

  test("getFile prepends external storage path for External files") {
    val stubOps = new StubFilesOps(extStorage = "/sdcard/")
    val fh      = AndroidFileHandle(new File("save.dat"), FileType.External, stubOps)
    assertEquals(fh.file.getPath(), new File("/sdcard/save.dat").getPath())
  }

  test("getFile prepends local storage path for Local files") {
    val fh = AndroidFileHandle(new File("local.dat"), FileType.Local, ops)
    assertEquals(fh.file.getPath(), new File("/data/data/com.test/files/local.dat").getPath())
  }

  test("getFile returns file as-is for Internal files") {
    val fh = AndroidFileHandle(new File("internal.txt"), FileType.Internal, ops)
    assertEquals(fh.file.getPath(), "internal.txt")
  }

  // ── String constructor ────────────────────────────────────────────────

  test("String constructor normalizes backslashes") {
    val fh = new AndroidFileHandle("assets\\sub\\file.txt", FileType.Internal, ops)
    assertEquals(fh.name, "file.txt")
    assert(fh.path.contains("assets/sub"))
  }
}
