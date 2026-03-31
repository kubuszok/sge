/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package assets
package loaders

import sge.files.{ FileHandle, FileType }

class FileHandleResolverTest extends munit.FunSuite {

  given Sge = SgeTestFixture.testSge()

  /** A simple resolver that creates FileHandles with the given name (no actual FS access). */
  private class StubResolver extends FileHandleResolver {
    override def resolve(fileName: String): FileHandle =
      new FileHandle(new java.io.File(fileName), FileType.Absolute)
  }

  test("Prefix resolver prepends prefix to filename") {
    val base = new StubResolver()
    val prefixed = new FileHandleResolver.Prefix(base, "assets/")
    val result = prefixed.resolve("textures/wall.png")
    assert(result.path.contains("assets/textures/wall.png"))
  }

  test("Prefix resolver with empty prefix passes through") {
    val base = new StubResolver()
    val prefixed = new FileHandleResolver.Prefix(base, "")
    val result = prefixed.resolve("textures/wall.png")
    assert(result.path.contains("textures/wall.png"))
  }

  test("Prefix resolver baseResolver and prefix are mutable") {
    val base1 = new StubResolver()
    val base2 = new StubResolver()
    val prefixed = new FileHandleResolver.Prefix(base1, "v1/")
    assert(prefixed.resolve("f.txt").path.contains("v1/f.txt"))

    prefixed.prefix = "v2/"
    assert(prefixed.resolve("f.txt").path.contains("v2/f.txt"))

    prefixed.baseResolver = base2
    assert(prefixed.baseResolver eq base2)
  }

  test("ForResolution requires at least one descriptor") {
    val base = new StubResolver()
    intercept[IllegalArgumentException] {
      new FileHandleResolver.ForResolution(base, Array.empty[FileHandleResolver.Resolution])
    }
  }

  test("ForResolution.choose returns best match for screen size") {
    // NoopGraphics defaults: backBufferWidth=0, backBufferHeight=0
    // With 0x0, the first descriptor should be chosen
    val low = FileHandleResolver.Resolution(480, 320, "low")
    val high = FileHandleResolver.Resolution(1920, 1080, "high")
    val best = FileHandleResolver.ForResolution.choose(low, high)
    assertEquals(best.folder, "low")
  }

  test("ForResolution.choose with single descriptor returns it") {
    val only = FileHandleResolver.Resolution(1024, 768, "default")
    val best = FileHandleResolver.ForResolution.choose(only)
    assertEquals(best.folder, "default")
  }
}
