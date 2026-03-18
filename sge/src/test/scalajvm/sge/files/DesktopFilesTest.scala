/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package files

import java.io.File

class DesktopFilesTest extends munit.FunSuite {

  private val files = DesktopFiles()

  // ---- storage paths ----

  test("externalStoragePath ends with separator") {
    assert(files.externalStoragePath.endsWith(File.separator))
  }

  test("externalStoragePath points to user home") {
    assertEquals(files.externalStoragePath, System.getProperty("user.home") + File.separator)
  }

  test("localStoragePath ends with separator") {
    assert(files.localStoragePath.endsWith(File.separator))
  }

  test("isExternalStorageAvailable is true") {
    assert(files.isExternalStorageAvailable)
  }

  test("isLocalStorageAvailable is true") {
    assert(files.isLocalStorageAvailable)
  }

  // ---- factory methods return correct FileType ----

  test("classpath returns Classpath type") {
    val fh = files.classpath("some/resource.txt")
    assertEquals(fh.fileType, FileType.Classpath)
    assertEquals(fh.path, "some/resource.txt")
  }

  test("internal returns Internal type") {
    val fh = files.internal("data/file.png")
    assertEquals(fh.fileType, FileType.Internal)
  }

  test("external returns External type") {
    val fh = files.external("test.txt")
    assertEquals(fh.fileType, FileType.External)
  }

  test("absolute returns Absolute type") {
    val fh = files.absolute("/tmp/test.txt")
    assertEquals(fh.fileType, FileType.Absolute)
  }

  test("local returns Local type") {
    val fh = files.local("local.dat")
    assertEquals(fh.fileType, FileType.Local)
  }

  test("getFileHandle with explicit type") {
    val fh = files.getFileHandle("path", FileType.External)
    assertEquals(fh.fileType, FileType.External)
    assertEquals(fh.path, "path")
  }
}
