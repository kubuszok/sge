/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Integration test for the Panama FFM desktop backend pipeline.
 * Uses GLFW_PLATFORM_NULL for headless testing (no display required).
 * Tests are skipped if GLFW shared library is not found on java.library.path.
 * Install GLFW 3.4+: brew install glfw (macOS) / apt install libglfw3 (Linux)
 */
package sge
package platform

import munit.FunSuite

class DesktopFfiIntegrationTest extends FunSuite {

  private var windowing: WindowingOps = scala.compiletime.uninitialized
  private var available: Boolean      = false

  override def beforeAll(): Unit =
    try {
      windowing = WindowingOpsJvm()
      // Use null/headless platform to avoid macOS Cocoa main-thread requirement
      windowing.setInitHint(WindowingOps.GLFW_PLATFORM, WindowingOps.GLFW_PLATFORM_NULL)
      available = windowing.init()
      if (!available) {
        System.err.println("[DesktopFfiIntegrationTest] glfwInit returned false — skipping")
      }
    } catch {
      case _: UnsatisfiedLinkError =>
        System.err.println(
          "[DesktopFfiIntegrationTest] GLFW not found on java.library.path — skipping"
        )
      case e: Throwable =>
        System.err.println(
          s"[DesktopFfiIntegrationTest] Failed: ${e.getClass.getName}: ${e.getMessage}"
        )
    }

  override def afterAll(): Unit =
    if (available) {
      windowing.terminate()
    }

  private def requireGlfw(): Unit =
    assume(available, "GLFW not available — install GLFW 3.4+: brew install glfw")

  /** Helper: create a hidden window, run body, then destroy it. */
  private def withWindow(width: Int, height: Int, title: String)(body: Long => Unit): Unit = {
    windowing.defaultWindowHints()
    windowing.setWindowHint(WindowingOps.GLFW_VISIBLE, WindowingOps.GLFW_FALSE)
    // Null platform has no GL context
    windowing.setWindowHint(WindowingOps.GLFW_CLIENT_API, WindowingOps.GLFW_NO_API)
    val handle = windowing.createWindow(width, height, title)
    assert(handle != 0L, s"glfwCreateWindow('$title') returned null")
    try body(handle)
    finally windowing.destroyWindow(handle)
  }

  // ─── Windowing ────────────────────────────────────────────────────────

  test("GLFW init succeeds (null platform)") {
    requireGlfw()
    // glfwInit already called in beforeAll; calling again is safe (idempotent)
    assert(windowing.init(), "glfwInit() should return true")
  }

  test("GLFW create and destroy window") {
    requireGlfw()
    withWindow(320, 240, "SGE Integration Test") { handle =>
      val (w, h) = windowing.getWindowSize(handle)
      assert(w > 0 && h > 0, s"window size should be positive, got $w x $h")
    }
  }

  test("GLFW window properties round-trip") {
    requireGlfw()
    windowing.defaultWindowHints()
    withWindow(400, 300, "Props Test") { handle =>
      windowing.setWindowTitle(handle, "Updated Title")
      windowing.setWindowSize(handle, 500, 400)
      val (w, h) = windowing.getWindowSize(handle)
      assertEquals(w, 500)
      assertEquals(h, 400)
    }
  }

  test("GLFW poll events without crash") {
    requireGlfw()
    withWindow(320, 240, "Poll Test") { handle =>
      var i = 0
      while (i < 10 && !windowing.windowShouldClose(handle)) {
        windowing.pollEvents()
        i += 1
      }
    }
  }

  test("GLFW time returns non-negative value") {
    requireGlfw()
    val t = windowing.time
    assert(t >= 0.0, s"glfwGetTime should be non-negative, got $t")
  }

  test("GLFW callbacks can be set and cleared") {
    requireGlfw()
    withWindow(320, 240, "Callback Test") { handle =>
      // Set callbacks (just verify no crash)
      windowing.setFramebufferSizeCallback(handle, (_, _, _) => ())
      windowing.setWindowFocusCallback(handle, (_, _) => ())
      windowing.setWindowCloseCallback(handle, _ => ())
      windowing.setKeyCallback(handle, (_, _, _, _, _) => ())
      windowing.setCharCallback(handle, (_, _) => ())
      windowing.setMouseButtonCallback(handle, (_, _, _, _) => ())
      windowing.setCursorPosCallback(handle, (_, _, _) => ())
      windowing.setScrollCallback(handle, (_, _, _) => ())
      windowing.setDropCallback(handle, (_, _) => ())

      // Clear callbacks
      windowing.setFramebufferSizeCallback(handle, null)
      windowing.setWindowFocusCallback(handle, null)
      windowing.setWindowCloseCallback(handle, null)
      windowing.setKeyCallback(handle, null)
    }
  }
}
