/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge

import lowlevel.Nullable
import sge.utils.SgeError

/** ISS-688: DesktopApplication.applicationListener fail-fast instead of windows.head NoSuchElementException.
  *
  * `DesktopApplication.applicationListener` previously read `windows.head` directly when no window was current; with an empty `windows` list this threw a `NoSuchElementException` (an exception far
  * from cause) in an otherwise null-free API. The fix routes the accessor through `DesktopApplication.listenerOrFail`, which fails fast with a clearly-messaged [[SgeError.GraphicsError]] naming the
  * missing-window condition and the affected accessor.
  *
  * The resolver is pure and `private[sge]`, so it is unit-testable without constructing a real [[DesktopApplication]] (whose constructor blocks in `loop()` and needs a live GLFW window) or any
  * [[DesktopWindow]] (which would require null collaborators). The genuinely-empty case is the bug, so empty collections suffice.
  */
class DesktopApplicationIss688Suite extends munit.FunSuite {

  test("ISS-688 listenerOrFail throws SgeError.GraphicsError when there is no current window and no windows at all") {
    val ex = intercept[SgeError.GraphicsError] {
      DesktopApplication.listenerOrFail(
        Nullable.empty[DesktopWindow],
        scala.collection.mutable.ArrayBuffer.empty[DesktopWindow]
      )
    }
    assert(ex.getMessage.toLowerCase.contains("window"), s"message should name the missing window: ${ex.getMessage}")
    assert(
      ex.getMessage.toLowerCase.contains("applicationlistener"),
      s"message should name the unavailable applicationListener: ${ex.getMessage}"
    )
  }
}
