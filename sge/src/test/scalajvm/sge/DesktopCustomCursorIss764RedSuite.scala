/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge

import sge.graphics.{ Cursor, Pixmap }

import scala.collection.mutable.ArrayBuffer

/** ISS-764: custom cursors are dead API — red tests (reproducer; wave 2026-07-17-C, territory I).
  *
  * `DesktopGraphics.newCursor(pixmap, xHotspot, yHotspot)` (DesktopGraphics.scala:505-507) unconditionally returns `Nullable.empty` ("Custom pixmap cursors require WindowingOps.createCursorFromImage
  * (deferred)"), although [[sge.platform.WindowingOps]] already carries cursor FFI for standard cursors (`createStandardCursor`/`setCursor`/`destroyCursor`, WindowingOps.scala:185-197) and
  * [[DesktopCursor]] exists. The original creates a native cursor from the pixmap image: `Lwjgl3Graphics.newCursor` returns `new Lwjgl3Cursor(window, pixmap, xHotspot, yHotspot)`
  * (Lwjgl3Graphics.java:548-551), whose constructor validates the pixmap and calls `glfwCreateCursor(GLFWImage{width, height, pixels}, xHotspot, yHotspot)` (Lwjgl3Cursor.java:42-78). The created
  * cursor is then installable via `Graphics.setCursor` -> `glfwSetCursor(windowHandle, cursor.glfwCursor)` (Lwjgl3Graphics.java:553-556).
  *
  * These are headless structural/contract tests over the recording [[Iss76xRecordingWindowingOps]] stub (pattern: [[DesktopFullscreenIss759RedSuite]], drive of test (b)). The create-cursor-from-image
  * seam does not exist on [[sge.platform.WindowingOps]] yet — see the anticipated-new-method note on `imageCursorCalls` in the fixture: assertions are formulated against the observable end state
  * (recorded image-cursor creation carrying pixmap dimensions + hotspot, non-empty returned Cursor, and setCursor forwarding the created handle), not one fixed FFI signature.
  */
class DesktopCustomCursorIss764RedSuite extends munit.FunSuite {

  /** A do-nothing listener; `render()` is the only abstract member and is never reached (no render loop runs in these tests). */
  private def listener: ApplicationListener = new ApplicationListener {
    override def render(): Unit = {}
  }

  /** Builds a headless [[DesktopWindow]] + [[DesktopGraphics]] over the recording ops. */
  private def makeGraphics(): (Iss76xRecordingWindowingOps, DesktopGraphics) = {
    val ops = new Iss76xRecordingWindowingOps
    // DesktopCursor reaches the windowing FFI through the PlatformOps globals; route them to the
    // same recording ops so the fix is observed regardless of which seam it uses.
    sge.platform.PlatformOps.windowing = ops
    sge.platform.PlatformOps.gl = new Iss76xStubGlOps
    val config = new DesktopApplicationConfig
    val window = new DesktopWindow(listener, ArrayBuffer.empty, config, new Iss76xStubDesktopApplicationBase(ops), ops, new Iss76xStubGlOps)
    window.create(900001L)
    (ops, window.graphics)
  }

  /** 8x8 RGBA8888 pixmap with hotspot-compatible bounds: satisfies every Lwjgl3Cursor precondition (RGBA8888 format, power-of-two width/height, hotspot within bounds — Lwjgl3Cursor.java:44-66), so a
    * faithful port has no legitimate reason to reject it.
    */
  private def makeCursorPixmap(): Pixmap = new Pixmap(8, 8, Pixmap.Format.RGBA8888)

  test(
    "ISS-764: newCursor creates a native cursor from the pixmap image with the hotspot (Lwjgl3Graphics.java:548-551, Lwjgl3Cursor.java:42-78)"
  ) {
    val (ops, graphics) = makeGraphics()
    val pixmap          = makeCursorPixmap()
    try {
      val cursor = graphics.newCursor(pixmap, Pixels(2), Pixels(3))
      assert(
        !cursor.isEmpty,
        "newCursor(8x8 RGBA8888 pixmap, hotspot 2,3) must return a non-empty Cursor " +
          "(original: new Lwjgl3Cursor(window, pixmap, xHotspot, yHotspot) -> glfwCreateCursor(GLFWImage, x, y), " +
          "Lwjgl3Graphics.java:548-551 + Lwjgl3Cursor.java:72-77); DesktopGraphics.newCursor " +
          "(DesktopGraphics.scala:505-507) unconditionally returns Nullable.empty"
      )
      assertEquals(
        ops.imageCursorCalls.toList,
        List((8, 8, 2, 3)),
        "creating the cursor must reach the windowing FFI as exactly one create-cursor-from-image call " +
          "carrying the pixmap dimensions and the hotspot (glfwCreateCursor(GLFWImage{width, height, pixels}, " +
          "xHotspot, yHotspot), Lwjgl3Cursor.java:72-76); see the anticipated-new-method note on " +
          "Iss76xRecordingWindowingOps.imageCursorCalls"
      )
    } finally
      pixmap.close()
  }

  test("ISS-764: setCursor forwards the custom cursor's native handle to ops.setCursor (Lwjgl3Graphics.java:553-556)") {
    val (ops, graphics) = makeGraphics()
    val pixmap          = makeCursorPixmap()
    try {
      val cursor = graphics
        .newCursor(pixmap, Pixels(2), Pixels(3))
        .fold[Cursor](
          fail(
            "newCursor returned Nullable.empty — custom cursors are dead API (DesktopGraphics.scala:505-507), " +
              "so there is no cursor to pass to setCursor (original returns a live Lwjgl3Cursor, Lwjgl3Graphics.java:548-551)"
          )
        )(identity)
      graphics.setCursor(cursor)
      assert(
        ops.setCursorCalls.nonEmpty,
        "Graphics.setCursor(customCursor) must forward to the windowing FFI (glfwSetCursor, Lwjgl3Graphics.java:553-556)"
      )
      val (windowHandle, cursorHandle) = ops.setCursorCalls.last
      assertEquals(windowHandle, 900001L, "setCursor must target this window's handle")
      assert(cursorHandle != 0L, "setCursor must carry a live (non-zero) native cursor handle")
      assertEquals(
        ops.createdImageCursorHandles.lastOption,
        Some(cursorHandle),
        "the handle passed to ops.setCursor must be the one returned by the create-cursor-from-image call " +
          "(original: glfwSetCursor(window, ((Lwjgl3Cursor)cursor).glfwCursor), Lwjgl3Graphics.java:553-556 " +
          "with glfwCursor from Lwjgl3Cursor.java:76)"
      )
    } finally
      pixmap.close()
  }
}
