/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * ISS-865: cross-platform (JS) behavioural coverage for the asset-loading
 * resolver layer. The JVM twin (sge/src/test/scalajvm/sge/assets/loaders/
 * FileHandleResolverTest.scala) is JVM-only; its own StubResolver instantiates
 * a bare java.io.File-backed FileHandle, which the Scala.js linker rejects
 * (FileHandle.exists() → getResourceAsStream). Here the stub returns a real
 * [[sge.files.BrowserFileHandle]] instead, so the Prefix path-rewrite algebra
 * and the resolution chooser are pinned on JS without touching any GL-,
 * filesystem- or classpath-dependent code.
 */
package sge
package assets
package loaders

import munit.FunSuite
import sge.files.{ BrowserAssetLoader, BrowserFiles, FileHandle }

class FileHandleResolverIss865JsSuite extends FunSuite {

  given Sge = SgeTestFixture.testSge()

  private val files = new BrowserFiles(new BrowserAssetLoader)

  /** Returns a BrowserFileHandle (the real JS runtime handle) — no FS access. */
  private class StubResolver extends FileHandleResolver {
    override def resolve(fileName: String): FileHandle = files.internal(fileName)
  }

  test("ISS-865 JS: Prefix resolver prepends the prefix to the filename") {
    val prefixed = new FileHandleResolver.Prefix(new StubResolver, "assets/")
    assertEquals(prefixed.resolve("textures/wall.png").path, "assets/textures/wall.png")
  }

  test("ISS-865 JS: Prefix resolver with an empty prefix passes through") {
    val prefixed = new FileHandleResolver.Prefix(new StubResolver, "")
    assertEquals(prefixed.resolve("textures/wall.png").path, "textures/wall.png")
  }

  test("ISS-865 JS: Prefix resolver base + prefix are mutable") {
    val prefixed = new FileHandleResolver.Prefix(new StubResolver, "v1/")
    assertEquals(prefixed.resolve("f.txt").path, "v1/f.txt")
    prefixed.prefix = "v2/"
    assertEquals(prefixed.resolve("f.txt").path, "v2/f.txt")
  }

  test("ISS-865 JS: ForResolution.choose picks the first descriptor at 0x0 back buffer") {
    val low  = FileHandleResolver.Resolution(480, 320, "low")
    val high = FileHandleResolver.Resolution(1920, 1080, "high")
    assertEquals(FileHandleResolver.ForResolution.choose(low, high).folder, "low")
  }

  test("ISS-865 JS: ForResolution.choose with a single descriptor returns it") {
    val only = FileHandleResolver.Resolution(1024, 768, "default")
    assertEquals(FileHandleResolver.ForResolution.choose(only).folder, "default")
  }
}
