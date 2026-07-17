/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge

import sge.platform.WindowingOps

/** ISS-762: dead desktop config knobs — red tests (reproducer; wave 2026-07-17-C, territory I).
  *
  * [[DesktopApplicationConfig]] carries several knobs (DesktopApplicationConfig.scala:29-96) that nothing in the desktop backend reads. This suite red-tests the two adjudicated as honestly
  * implementable today:
  *
  *   - `transparentFramebuffer` (DesktopApplicationConfig.scala:64-65): the original sets the GLFW_TRANSPARENT_FRAMEBUFFER window hint before creating the window (`if (config.transparentFramebuffer)
  *     GLFW.glfwWindowHint(GLFW.GLFW_TRANSPARENT_FRAMEBUFFER, GLFW.GLFW_TRUE)`, Lwjgl3Application.java:505-507). `DesktopApplication.createGlfwWindow` (DesktopApplication.scala:385-399) never reads
  *     the knob, although `WindowingOps.setWindowHint` and the `GLFW_TRANSPARENT_FRAMEBUFFER` constant (WindowingOps.scala:383) already exist.
  *   - `DesktopWindowConfig.title` empty-string fallback (DesktopWindowConfig.scala:67-68 promises: "Window title. If empty, the application listener's class name is used."): the original applies
  *     `if (config.title == null) config.title = listener.getClass().getSimpleName()` in the constructor before any window is created (Lwjgl3Application.java:128-129; same fallback on the newWindow
  *     path, Lwjgl3Application.java:428). `DesktopApplication` passes `config.title` ("") straight to `WindowingOps.createWindow` (DesktopApplication.scala:397-399).
  *
  * The remaining dead knobs (glEmulation + glesContextMajor/MinorVersion, debug/debugStream/errorStream, maxNetThreads) are adjudicated as delete-candidates or orchestrator decisions in the
  * reproducer report — deliberately NOT red-tested here.
  *
  * These are headless structural/contract tests: the real `DesktopApplication` constructor is driven through `createGlfwWindow` + `setupWindow` against the recording [[Iss76xRecordingWindowingOps]]
  * stub and stopped at the `glFactory` seam (pattern: [[DesktopFullscreenIss759RedSuite]]), so no native library is touched.
  */
class DesktopConfigKnobsIss762RedSuite extends munit.FunSuite {

  /** Named (non-anonymous, non-local) listener class so `getClass.getSimpleName` is stable and non-empty — the original's title fallback uses exactly that name (Lwjgl3Application.java:129). */
  final private class Iss762FallbackTitleListener extends ApplicationListener {
    override def render(): Unit = {}
  }

  /** Drives the real `DesktopApplication` constructor through window creation against the recording ops, stopping at the `glFactory` seam. */
  private def driveAppCreation(config: DesktopApplicationConfig, listener: => ApplicationListener): Iss76xRecordingWindowingOps = {
    val ops = new Iss76xRecordingWindowingOps
    intercept[Iss76xStopBeforeGlInit] {
      new DesktopApplication(
        listenerFactory = listener,
        config = config,
        windowing = ops,
        audioOps = new Iss76xStubAudioOps,
        glOps = new Iss76xStubGlOps,
        glFactory = () => throw new Iss76xStopBeforeGlInit
      )
    }
    assert(ops.createWindowCalls.nonEmpty, "test drive broke: DesktopApplication never reached window creation")
    ops
  }

  test(
    "ISS-762: transparentFramebuffer=true sets the GLFW_TRANSPARENT_FRAMEBUFFER window hint at window creation (Lwjgl3Application.java:505-507)"
  ) {
    val config = new DesktopApplicationConfig
    config.disableAudio = true
    config.title = "iss762"
    config.transparentFramebuffer = true
    val ops = driveAppCreation(config, new Iss762FallbackTitleListener)
    assert(
      ops.windowHintCalls.contains((WindowingOps.GLFW_TRANSPARENT_FRAMEBUFFER, WindowingOps.GLFW_TRUE)),
      "config.transparentFramebuffer = true must be honored as setWindowHint(GLFW_TRANSPARENT_FRAMEBUFFER, GLFW_TRUE) " +
        "before the window is created (original: Lwjgl3Application.java:505-507); DesktopApplication.createGlfwWindow " +
        "(DesktopApplication.scala:385-399) never reads the knob. Hints seen: " +
        ops.windowHintCalls.toList.mkString("[", ", ", "]")
    )
  }

  test(
    "ISS-762: empty title falls back to the listener's class name (Lwjgl3Application.java:128-129; promised by DesktopWindowConfig.scala:67-68)"
  ) {
    val config = new DesktopApplicationConfig
    config.disableAudio = true
    // config.title is left at its "" default — DesktopWindowConfig.scala:67-68 documents:
    // "Window title. If empty, the application listener's class name is used."
    val ops          = driveAppCreation(config, new Iss762FallbackTitleListener)
    val windowHandle = ops.createdWindowHandles.head
    assertEquals(
      ops.effectiveTitleOf(windowHandle),
      "Iss762FallbackTitleListener",
      "an empty config.title must fall back to the listener's simple class name (original: " +
        "`if (config.title == null) config.title = listener.getClass().getSimpleName()` before createGlfwWindow, " +
        "Lwjgl3Application.java:128-129); DesktopApplication passes config.title ('') straight to " +
        "WindowingOps.createWindow (DesktopApplication.scala:397-399). The drive stops before the render loop, " +
        "so a faithful fix must apply the fallback by the end of window setup — as the creation title or a " +
        "setWindowTitle during setup (both count into effectiveTitleOf), like the original does before glfwCreateWindow"
    )
  }
}
