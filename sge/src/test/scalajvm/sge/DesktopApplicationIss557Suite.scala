/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge

import lowlevel.Nullable
import sge.utils.SgeError

/** ISS-557 (clause 1): DesktopApplication graphics/input fail-fast instead of orNull.
  *
  * `DesktopApplication.graphics`/`input` previously returned a bare `null` (via `orNull`) when no window was current, producing an NPE far from cause in an otherwise null-free API. The fix routes
  * both accessors through `DesktopApplication.currentOrFail`, which fails fast with a clearly-messaged [[SgeError.GraphicsError]] naming the missing-current-window condition and the affected
  * accessor.
  *
  * The resolver is pure and `private[sge]`, so it is unit-testable without constructing a real [[DesktopApplication]] (whose constructor blocks in `loop()` and needs a live GLFW window).
  */
class DesktopApplicationIss557Suite extends munit.FunSuite {

  test("ISS-557 currentOrFail throws SgeError.GraphicsError for graphics when no window is current") {
    val ex = intercept[SgeError.GraphicsError] {
      DesktopApplication.currentOrFail(Nullable.empty[DesktopWindow])(_.graphics, "graphics")
    }
    assert(ex.getMessage.contains("graphics"), s"message should name the accessor: ${ex.getMessage}")
    assert(ex.getMessage.toLowerCase.contains("window"), s"message should name the missing window: ${ex.getMessage}")
  }

  test("ISS-557 currentOrFail throws SgeError.GraphicsError for input when no window is current") {
    val ex = intercept[SgeError.GraphicsError] {
      DesktopApplication.currentOrFail(Nullable.empty[DesktopWindow])(_.input, "input")
    }
    assert(ex.getMessage.contains("input"), s"message should name the accessor: ${ex.getMessage}")
    assert(ex.getMessage.toLowerCase.contains("window"), s"message should name the missing window: ${ex.getMessage}")
  }
}
