/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package platform

import sge.graphics.Pixmap

import java.lang.foreign.{ MemorySegment, SymbolLookup }
import java.util.Optional

// RED (runtime) pin for ISS-833 (reproducer; wave 2026-07-18-J, territory J1), auditor V finding 1.
//
// WindowingOpsJvm.createCursor (WindowingOpsJvm.scala:601-618) reads the pixmap's pixel data with:
//     val pixels = pixmap.pixels
//     pixels.position(0)
// `pixmap.pixels` returns the Pixmap's OWN backing ByteBuffer (Gdx2DPixmap._pixelPtr, returned by
// identity — Gdx2DPixmap.scala:243), shared with the caller and with the pixmap's own state. Calling
// `position(0)` on it mutates that shared buffer, so a caller who had positioned the buffer elsewhere
// finds its position silently reset after createCursor — the residual JVM sibling of ISS-809, which
// fixed the SAME bug on the Native backend by draining a `duplicate()` view with an independent
// position/limit/mark (WindowingOpsNative.scala:494-495: `val pixels = pixmap.pixels.duplicate();
// pixels.position(0)`).
//
// This suite drives the REAL WindowingOpsJvm over an empty SymbolLookup (no GLFW symbols): the pixel
// read + the shared-buffer mutation both execute before the terminal `glfwCreateCursor` downcall
// handle fails to resolve, so the FFI throw is caught and the observable end state — the caller's
// buffer position — is asserted. RED on the current position(0)-on-shared-buffer impl (position
// observed as 0); GREEN once createCursor drains a `duplicate()` like the Native twin.
//
// WindowingOpsJvm is COVENANTED — this reproducer does NOT edit it; the fix is the implementer's.
class WindowingOpsCreateCursorBufferIss833RedSuite extends munit.FunSuite {

  /** A SymbolLookup that resolves no symbols, so createCursor's terminal glfwCreateCursor downcall handle fails to resolve (UnsatisfiedLinkError) — after the pixel read and the shared-buffer mutation
    * have already run. All method handles in WindowingOpsJvm are lazy, so construction over this lookup performs no native lookup.
    */
  private val emptyLookup: SymbolLookup = (_: String) => Optional.empty[MemorySegment]()

  test("createCursor must not disturb the caller's shared pixels buffer position (ISS-833; mirror ISS-809 duplicate())") {
    val pixmap = new Pixmap(8, 8, Pixmap.Format.RGBA8888)
    try {
      // A caller who left the shared buffer positioned somewhere non-zero.
      val startPosition = 7
      pixmap.pixels.position(startPosition)

      val ops = new WindowingOpsJvm(emptyLookup)
      // createCursor reads pixmap.pixels then position(0)s it BEFORE the glfwCreateCursor downcall,
      // which fails to resolve over emptyLookup — the mutation is what we observe, not the throw.
      try ops.createCursor(pixmap, 0, 0)
      catch { case _: Throwable => () }

      assertEquals(
        pixmap.pixels.position(),
        startPosition,
        "createCursor(pixmap, ...) must leave the caller's shared pixels buffer position untouched " +
          "(original: GLFW copies pixel data non-destructively; ISS-809 fixed the Native twin by draining " +
          "a duplicate() view — WindowingOpsNative.scala:494-495). The JVM impl still calls position(0) " +
          "on the shared buffer (WindowingOpsJvm.scala:607-608), resetting the caller's position to 0."
      )
    } finally
      pixmap.close()
  }
}
