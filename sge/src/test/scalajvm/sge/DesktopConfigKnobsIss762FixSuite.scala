/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge

import java.io.{ ByteArrayOutputStream, PrintStream }
import java.nio.charset.StandardCharsets

/** ISS-762 fix-side tests for the two config knobs adjudicated as *implement* rather than *delete* but not covered by the red suite: the GLFW error callback wired to `config.errorStream`
  * (Lwjgl3Application.java:84) and the `debug=true` fail-fast (Lwjgl3Application.java:594-597). Complements [[DesktopConfigKnobsIss762RedSuite]] (transparentFramebuffer + empty-title fallback), whose
  * assertions stay untouched.
  *
  * Headless: the real `DesktopApplication` constructor is driven through window creation against the recording [[Iss76xRecordingWindowingOps]] stub, stopped at the `glFactory` seam.
  */
class DesktopConfigKnobsIss762FixSuite extends munit.FunSuite {

  private def listener: ApplicationListener = new ApplicationListener {
    override def render(): Unit = {}
  }

  /** Drives the real `DesktopApplication` constructor through window creation against the recording ops, stopping at the `glFactory` seam. */
  private def driveAppCreation(config: DesktopApplicationConfig): Iss76xRecordingWindowingOps = {
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
    ops
  }

  test(
    "ISS-762: a GLFW error callback is installed during init and prints 'GLFW error <code>: <description>' to config.errorStream (Lwjgl3Application.java:84)"
  ) {
    val captured = new ByteArrayOutputStream()
    val config   = new DesktopApplicationConfig
    config.disableAudio = true
    config.title = "iss762-errorstream"
    config.errorStream = new PrintStream(captured, true, StandardCharsets.UTF_8)

    val ops = driveAppCreation(config)
    val cb = ops.errorCallback.getOrElse(
      fail(
        "DesktopApplication must install a GLFW error callback during init via WindowingOps.setErrorCallback " +
          "(original: GLFWErrorCallback.createPrint(config.errorStream).set(), Lwjgl3Application.java:84)"
      )
    )
    cb(0x10008, "a fabricated GLFW failure")
    val out = captured.toString(StandardCharsets.UTF_8)
    assertEquals(
      out.trim,
      s"GLFW error ${0x10008}: a fabricated GLFW failure",
      "the installed error callback must print 'GLFW error <code>: <description>' to config.errorStream"
    )
  }

  test(
    "ISS-762: debug=true fails fast at startup with an ANGLE-citing IllegalStateException (Lwjgl3Application.java:594-597)"
  ) {
    val config = new DesktopApplicationConfig
    config.disableAudio = true
    config.debug = true

    val ex = intercept[IllegalStateException] {
      val ops = new Iss76xRecordingWindowingOps
      new DesktopApplication(
        listenerFactory = listener,
        config = config,
        windowing = ops,
        audioOps = new Iss76xStubAudioOps,
        glOps = new Iss76xStubGlOps,
        glFactory = () => throw new Iss76xStopBeforeGlInit
      )
    }
    assert(
      ex.getMessage.contains("ANGLE"),
      s"debug=true must fail fast citing SGE's ANGLE-only backend; got: ${ex.getMessage}"
    )
  }
}
