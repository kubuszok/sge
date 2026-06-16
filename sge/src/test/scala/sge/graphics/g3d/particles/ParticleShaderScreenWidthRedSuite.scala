/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red test for ISS-549 (ParticleShader.Setters.screenWidth uses camera
 * viewport width instead of the framebuffer pixel width).
 *
 * The expected behaviour is taken verbatim from the original
 * com/badlogic/gdx/graphics/g3d/particles/ParticleShader.java
 * (original-src/libgdx), Setters.screenWidth.set (lines 168-171):
 *
 *   public void set (BaseShader shader, int inputID, Renderable renderable, Attributes combinedAttributes) {
 *     shader.set(inputID, (float)Gdx.graphics.getWidth());
 *   }
 *
 * i.e. the u_screenWidth uniform is fed the FRAMEBUFFER PIXEL width
 * (Gdx.graphics.getWidth()), not the camera viewport width. The GLSL derives
 * gl_PointSize from u_screenWidth, so point-sprite particles come out the wrong
 * size whenever world/viewport units differ from pixels.
 *
 * The SGE port (ParticleShader.scala Setters.screenWidth, ~lines 452-458)
 * instead passes `cam.viewportWidth.toFloat` (WORLD units). This test pins the
 * correct value: it must be `Sge().graphics.width.toFloat`.
 *
 * Headless fixture: NoopGraphics gives a controllable framebuffer width
 * (graphics.width = Pixels(noopWidth), ISS-671 made NoopGraphics usable
 * headless). The screenWidth setter calls `shader.setFloat(inputID, value)`,
 * which routes through `program.setUniformf(location, value)`; we capture that
 * value with a recording ShaderProgram subclass. NoopGL20 reports a found
 * uniform location (0 != UniformLocation.notFound = -1), so init() wires the
 * uniform through. The camera viewport width is set to a DIFFERENT value (10)
 * than the framebuffer width (800) so the buggy and correct values are
 * distinguishable.
 */
package sge
package graphics
package g3d
package particles

import sge.graphics.{ GL20, OrthographicCamera, UniformLocation }
import sge.graphics.g3d.{ Renderable, Shader }
import sge.graphics.g3d.shaders.BaseShader
import sge.graphics.glutils.ShaderProgram
import sge.noop.{ NoopGL20, NoopGraphics }
import lowlevel.Nullable

class ParticleShaderScreenWidthRedSuite extends munit.FunSuite {

  private val framebufferWidth  = 800
  private val framebufferHeight = 600
  private val viewportWidth     = 10f
  private val viewportHeight    = 8f

  private def makeSge(): Sge = {
    val graphics = new NoopGraphics(framebufferWidth, framebufferHeight) {
      override def gl20: GL20 = NoopGL20
    }
    SgeTestFixture.testSge(graphics = graphics)
  }

  /** ShaderProgram that reports compiled (so BaseShader.init accepts it) and records the float pushed to setUniformf. */
  final private class RecordingShaderProgram(using Sge) extends ShaderProgram("", "") {
    var captured: Nullable[Float] = Nullable.empty

    override def compiled: Boolean = true

    override def setUniformf(location: UniformLocation, value: Float): Unit =
      captured = Nullable(value)
  }

  /** Minimal BaseShader exposing only what the screenWidth setter needs. */
  final private class RecordingBaseShader(using Sge) extends BaseShader {
    def init():                          Unit    = ()
    def compareTo(other:    Shader):     Int     = 0
    def canRender(instance: Renderable): Boolean = true
  }

  test("screenWidth setter feeds u_screenWidth the framebuffer pixel width, not the camera viewport width") {
    given Sge = makeSge()

    val camera = OrthographicCamera(WorldUnits(viewportWidth), WorldUnits(viewportHeight))

    val program = new RecordingShaderProgram
    val shader  = new RecordingBaseShader
    val inputID = shader.register(ParticleShader.Inputs.screenWidth, Nullable(ParticleShader.Setters.screenWidth))
    shader.init(program, null.asInstanceOf[Renderable])
    shader.camera = Nullable(camera)

    ParticleShader.Setters.screenWidth.set(
      shader,
      inputID,
      null.asInstanceOf[Renderable],
      null.asInstanceOf[graphics.g3d.Attributes]
    )

    val captured = program.captured.getOrElse(
      fail("screenWidth setter never pushed a value to the uniform")
    )

    // sanity: the two candidate values really differ, so the assertion is meaningful
    assertEquals(summon[Sge].graphics.width.toFloat, framebufferWidth.toFloat)
    assert(
      camera.viewportWidth.toFloat != summon[Sge].graphics.width.toFloat,
      "fixture must make viewport width differ from framebuffer width"
    )

    assertEquals(
      captured,
      framebufferWidth.toFloat,
      s"u_screenWidth must be the framebuffer pixel width (${framebufferWidth.toFloat}), " +
        s"but the setter pushed $captured (camera viewport width is ${camera.viewportWidth.toFloat})"
    )
  }
}
