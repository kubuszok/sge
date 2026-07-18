/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * ISS-865: cross-platform (Scala Native) behavioural coverage for the
 * asset-loading resolver layer. The JVM twin
 * (sge/src/test/scalajvm/sge/assets/loaders/FileHandleResolverTest.scala) was
 * JVM-only; this pins the same Prefix path-rewrite algebra and resolution
 * chooser on Native, using a real java.io.File-backed [[DesktopFileHandle]]
 * (fully supported by Scala Native's javalib). No GL or real FS access.
 */
package sge
package assets
package loaders

import munit.FunSuite
import sge.files.{ DesktopFileHandle, FileHandle, FileType }

class FileHandleResolverIss865NativeSuite extends FunSuite {

  given Sge = SgeTestFixture.testSge()

  /** Returns a DesktopFileHandle (the Native runtime handle) — no FS access. */
  private class StubResolver extends FileHandleResolver {
    override def resolve(fileName: String): FileHandle =
      DesktopFileHandle(fileName, FileType.Absolute, "/iss865-ext/")
  }

  test("ISS-865 Native: Prefix resolver prepends the prefix to the filename") {
    val prefixed = new FileHandleResolver.Prefix(new StubResolver, "assets/")
    assertEquals(prefixed.resolve("textures/wall.png").path, "assets/textures/wall.png")
  }

  test("ISS-865 Native: Prefix resolver with an empty prefix passes through") {
    val prefixed = new FileHandleResolver.Prefix(new StubResolver, "")
    assertEquals(prefixed.resolve("textures/wall.png").path, "textures/wall.png")
  }

  test("ISS-865 Native: Prefix resolver base + prefix are mutable") {
    val prefixed = new FileHandleResolver.Prefix(new StubResolver, "v1/")
    assertEquals(prefixed.resolve("f.txt").path, "v1/f.txt")
    prefixed.prefix = "v2/"
    assertEquals(prefixed.resolve("f.txt").path, "v2/f.txt")
  }

  test("ISS-865 Native: ForResolution.choose picks the first descriptor at 0x0 back buffer") {
    val low  = FileHandleResolver.Resolution(480, 320, "low")
    val high = FileHandleResolver.Resolution(1920, 1080, "high")
    assertEquals(FileHandleResolver.ForResolution.choose(low, high).folder, "low")
  }

  test("ISS-865 Native: ForResolution.choose with a single descriptor returns it") {
    val only = FileHandleResolver.Resolution(1024, 768, "default")
    assertEquals(FileHandleResolver.ForResolution.choose(only).folder, "default")
  }
}
