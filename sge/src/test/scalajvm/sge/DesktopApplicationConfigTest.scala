/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge

import sge.files.FileType
import sge.graphics.glutils.HdpiMode

class DesktopApplicationConfigTest extends munit.FunSuite {

  // ---- defaults ----

  test("window defaults are sensible") {
    val config = DesktopApplicationConfig()
    assertEquals(config.windowWidth, 640)
    assertEquals(config.windowHeight, 480)
    assertEquals(config.windowX, -1)
    assertEquals(config.windowY, -1)
    assert(config.windowResizable)
    assert(config.windowDecorated)
    assert(!config.windowMaximized)
    assert(config.vSyncEnabled)
    assert(config.initialVisible)
  }

  test("application defaults are sensible") {
    val config = DesktopApplicationConfig()
    assert(!config.disableAudio)
    assertEquals(config.maxNetThreads, Int.MaxValue)
    assertEquals(config.audioDeviceSimultaneousSources, 16)
    assertEquals(config.audioDeviceBufferSize, 512)
    assertEquals(config.audioDeviceBufferCount, 9)
    assertEquals(config.glEmulation, DesktopApplicationConfig.GLEmulation.ANGLE_GLES20)
    assertEquals(config.glesContextMajorVersion, 3)
    assertEquals(config.glesContextMinorVersion, 2)
    assertEquals(config.r, 8)
    assertEquals(config.depth, 16)
    assertEquals(config.stencil, 0)
    assertEquals(config.samples, 0)
    assert(!config.transparentFramebuffer)
    assertEquals(config.idleFPS, 60)
    assertEquals(config.foregroundFPS, 0)
    assert(config.pauseWhenMinimized)
    assert(!config.pauseWhenLostFocus)
    assertEquals(config.preferencesDirectory, ".prefs/")
    assertEquals(config.preferencesFileType, FileType.External)
    assertEquals(config.hdpiMode, HdpiMode.Logical)
    assert(!config.debug)
  }

  // ---- convenience methods ----

  test("setWindowedMode sets width and height") {
    val config = DesktopApplicationConfig()
    config.setWindowedMode(1920, 1080)
    assertEquals(config.windowWidth, 1920)
    assertEquals(config.windowHeight, 1080)
  }

  test("setWindowPosition sets x and y") {
    val config = DesktopApplicationConfig()
    config.setWindowPosition(100, 200)
    assertEquals(config.windowX, 100)
    assertEquals(config.windowY, 200)
  }

  test("setWindowSizeLimits sets all four limits") {
    val config = DesktopApplicationConfig()
    config.setWindowSizeLimits(320, 240, 3840, 2160)
    assertEquals(config.windowMinWidth, 320)
    assertEquals(config.windowMinHeight, 240)
    assertEquals(config.windowMaxWidth, 3840)
    assertEquals(config.windowMaxHeight, 2160)
  }

  test("setAudioConfig sets all three audio fields") {
    val config = DesktopApplicationConfig()
    config.setAudioConfig(32, 1024, 4)
    assertEquals(config.audioDeviceSimultaneousSources, 32)
    assertEquals(config.audioDeviceBufferSize, 1024)
    assertEquals(config.audioDeviceBufferCount, 4)
  }

  test("setOpenGLEmulation sets GL version fields") {
    val config = DesktopApplicationConfig()
    config.setOpenGLEmulation(DesktopApplicationConfig.GLEmulation.GL30, 4, 5)
    assertEquals(config.glEmulation, DesktopApplicationConfig.GLEmulation.GL30)
    assertEquals(config.glesContextMajorVersion, 4)
    assertEquals(config.glesContextMinorVersion, 5)
  }

  test("setBackBufferConfig sets all framebuffer fields") {
    val config = DesktopApplicationConfig()
    config.setBackBufferConfig(5, 6, 5, 0, 24, 8, 4)
    assertEquals(config.r, 5)
    assertEquals(config.g, 6)
    assertEquals(config.b, 5)
    assertEquals(config.a, 0)
    assertEquals(config.depth, 24)
    assertEquals(config.stencil, 8)
    assertEquals(config.samples, 4)
  }

  test("setPreferencesConfig sets directory and file type") {
    val config = DesktopApplicationConfig()
    config.setPreferencesConfig("myprefs/", FileType.Local)
    assertEquals(config.preferencesDirectory, "myprefs/")
    assertEquals(config.preferencesFileType, FileType.Local)
  }

  test("enableGLDebugOutput sets debug and stream") {
    val config = DesktopApplicationConfig()
    config.enableGLDebugOutput(true, System.out)
    assert(config.debug)
    assert(config.debugStream eq System.out)
  }

  // ---- copy ----

  test("copy creates independent copy") {
    val original = DesktopApplicationConfig()
    original.setWindowedMode(800, 600)
    original.title = "Test"
    original.disableAudio = true

    val copied = DesktopApplicationConfig.copy(original)
    assertEquals(copied.windowWidth, 800)
    assertEquals(copied.windowHeight, 600)
    assertEquals(copied.title, "Test")
    assert(copied.disableAudio)

    // Modifying copy does not affect original
    copied.windowWidth = 1024
    assertEquals(original.windowWidth, 800)
  }

  // ---- window config inheritance ----

  test("DesktopApplicationConfig extends DesktopWindowConfig") {
    val config: DesktopWindowConfig = DesktopApplicationConfig()
    assert(config.isInstanceOf[DesktopWindowConfig])
  }

  // ---- GLEmulation enum ----

  test("GLEmulation enum has expected values") {
    val values = DesktopApplicationConfig.GLEmulation.values
    assertEquals(values.length, 5)
    assertEquals(values(0), DesktopApplicationConfig.GLEmulation.ANGLE_GLES20)
    assertEquals(values(1), DesktopApplicationConfig.GLEmulation.GL20)
  }

  // ---- DesktopMonitor ----

  test("DesktopMonitor stores handle and converts to core Monitor") {
    val dm = DesktopMonitor(42L, 100, 200, "Primary")
    assertEquals(dm.monitorHandle, 42L)
    assertEquals(dm.virtualX, 100)
    assertEquals(dm.virtualY, 200)
    assertEquals(dm.name, "Primary")

    val core = dm.toMonitor
    assertEquals(core.virtualX, 100)
    assertEquals(core.virtualY, 200)
    assertEquals(core.name, "Primary")
  }

  // ---- DesktopDisplayMode ----

  test("DesktopDisplayMode stores handle and converts to core DisplayMode") {
    val ddm = DesktopDisplayMode(99L, 1920, 1080, 60, 24)
    assertEquals(ddm.monitorHandle, 99L)
    assertEquals(ddm.width, 1920)
    assertEquals(ddm.height, 1080)
    assertEquals(ddm.refreshRate, 60)
    assertEquals(ddm.bitsPerPixel, 24)

    val core = ddm.toDisplayMode
    assertEquals(core.width, 1920)
    assertEquals(core.height, 1080)
    assertEquals(core.refreshRate, 60)
    assertEquals(core.bitsPerPixel, 24)
  }
}
