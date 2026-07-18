/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * ISS-865: cross-platform (JS) behavioural coverage for the asset-loading /
 * resource-read path of the files subsystem.
 *
 * On Scala.js resources are served synchronously by [[BrowserAssetLoader]]
 * (backed by build-time-embedded resources with a Node fs fallback for
 * in-repo dev/test) and surfaced through [[BrowserFileHandle]]. There was no
 * JS test pinning the read/exists/list contract; the JVM twin lives in
 * sge/src/test/scalajvm/sge/files/. These tests exercise the REAL runtime
 * classes (no synthetic handles) with deterministic, GL-free assertions.
 *
 * Could-not-test note: a POSITIVE embedded-resource read is not asserted here.
 * BrowserAssetLoader resolves through multiarch.resources.PlatformResources,
 * whose embedded-map registration (sge.platform.GeneratedEmbeddedResources) is
 * wired from BrowserApplication to defeat Scala.js DCE; a bare unit-test binary
 * that never touches BrowserApplication cannot rely on that registration being
 * present, and the Node fs fallback's resolution root is the test runner's cwd
 * rather than the module resource root. Pinning a positive read therefore needs
 * the packaging pipeline (covered by sge-build's browser packaging sbt-test),
 * not this unit binary. The not-found path below is fully deterministic.
 */
package sge
package files

import munit.FunSuite
import sge.utils.SgeError

class BrowserAssetLoadingIss865Suite extends FunSuite {

  private val loader = new BrowserAssetLoader
  private val files  = new BrowserFiles(loader)

  private val missing = "iss865/definitely/not/here.bin"

  /** A BrowserFileHandle typed as its concrete class on purpose: read/exists are called below, and a monomorphic BrowserFileHandle receiver keeps the Scala.js linker from pulling the base
    * FileHandle.read()/exists() bodies (which reference java.io.FileInputStream / Class.getResourceAsStream, absent on JS) into the test binary.
    */
  private def handle(path: String): BrowserFileHandle =
    new BrowserFileHandle(loader, path, FileType.Internal)

  // ── BrowserAssetLoader not-found contract ───────────────────────────────

  test("ISS-865 JS: contains is false for an absent asset") {
    assertEquals(loader.contains(missing), false)
  }

  test("ISS-865 JS: readBytes is None for an absent asset") {
    assertEquals(loader.readBytes(missing), None)
  }

  test("ISS-865 JS: readText is None for an absent asset") {
    assertEquals(loader.readText(missing), None)
  }

  test("ISS-865 JS: length is 0 for an absent asset") {
    assertEquals(loader.length(missing), 0L)
  }

  test("ISS-865 JS: isDirectory is false for an absent path") {
    assertEquals(loader.isDirectory(missing), false)
  }

  test("ISS-865 JS: list of an absent directory is empty") {
    assertEquals(loader.list(missing).toList, Nil)
  }

  // ── BrowserFileHandle read/exists contract ──────────────────────────────

  test("ISS-865 JS: internal handle for an absent asset does not exist") {
    assertEquals(handle(missing).exists(), false)
  }

  test("ISS-865 JS: readString on an absent asset throws FileReadError") {
    val err = intercept[SgeError.FileReadError](handle(missing).readString())
    assert(err.getMessage.contains(missing), s"error should name the missing path, got: ${err.getMessage}")
  }

  test("ISS-865 JS: readBytes on an absent asset throws FileReadError") {
    intercept[SgeError.FileReadError](handle(missing).readBytes())
  }

  test("ISS-865 JS: read() on an absent asset throws FileReadError") {
    intercept[SgeError.FileReadError](handle(missing).read())
  }

  test("ISS-865 JS: length on an absent internal handle is 0") {
    assertEquals(handle(missing).length(), 0L)
  }

  // ── BrowserFiles unsupported file types (browser has no filesystem) ──────

  test("ISS-865 JS: external files are an unsupported capability") {
    intercept[SgeError.Unsupported](files.external("save.dat"))
  }

  test("ISS-865 JS: absolute files are an unsupported capability") {
    intercept[SgeError.Unsupported](files.absolute("/etc/hosts"))
  }

  test("ISS-865 JS: local files are an unsupported capability") {
    intercept[SgeError.Unsupported](files.local("save.dat"))
  }

  test("ISS-865 JS: classpath and internal handles carry their FileType") {
    assertEquals(files.classpath("a.txt").fileType, FileType.Classpath)
    assertEquals(files.internal("a.txt").fileType, FileType.Internal)
  }
}
