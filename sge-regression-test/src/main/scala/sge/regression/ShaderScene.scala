/*
 * SGE Regression Test — shader compilation and uniform binding.
 *
 * Tests: ShaderProgram compilation, uniform location, glGetError.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package regression

import sge.graphics.glutils.ShaderProgram
import sge.utils.ScreenUtils

/** Compiles a minimal vertex + fragment shader, binds a uniform matrix, and verifies no GL errors.
  *
  * Based on the `SmokeListener.checkGL3D()` pattern from `sge-android-smoke`.
  */
object ShaderScene extends RegressionScene {

  override val name: String = "Shader"

  private var shader: ShaderProgram = scala.compiletime.uninitialized
  private var ok:     Boolean       = false

  override def init()(using Sge): Unit = {
    try {
      val vs =
        """#version 100
          |uniform mat4 u_projTrans;
          |attribute vec4 a_position;
          |void main() { gl_Position = u_projTrans * a_position; }""".stripMargin
      val fs =
        """#version 100
          |precision mediump float;
          |void main() { gl_FragColor = vec4(0.0, 1.0, 0.0, 1.0); }""".stripMargin

      shader = new ShaderProgram(vs, fs)
      SmokeResult.logCheck("SHADER_COMPILE", shader.compiled, if (shader.compiled) "OK" else shader.getLog())

      if (shader.compiled) {
        shader.bind()
        val loc = shader.fetchUniformLocation("u_projTrans", false)
        SmokeResult.logCheck("SHADER_UNIFORM", loc >= 0, s"location=$loc")

        if (loc >= 0) {
          shader.setUniformMatrix("u_projTrans", new math.Matrix4())
          val err = Sge().graphics.getGL20().glGetError()
          SmokeResult.logCheck("SHADER_GLERROR", err == 0, if (err == 0) "no error" else s"0x${err.toHexString}")
        }
      }
      ok = shader.compiled
    } catch {
      case e: Exception =>
        SmokeResult.logCheck("SHADER", false, s"Exception: ${e.getMessage}")
    }
  }

  override def render(elapsed: Float)(using Sge): Unit =
    if (ok) ScreenUtils.clear(0f, 0.4f, 0.2f, 1f)
    else ScreenUtils.clear(0.5f, 0f, 0f, 1f)

  override def dispose()(using Sge): Unit =
    if (shader != null) shader.close() // scalafix:ok
}
