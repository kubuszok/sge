/*
 * Ported from gdx-vfx - https://github.com/crashinvaders/gdx-vfx
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package vfx
package effects

import sge.graphics.Color
import sge.graphics.glutils.ShaderProgram
import sge.math.{ Matrix3, Matrix4, Vector2, Vector3 }
import sge.vfx.VfxRenderContext
import sge.vfx.framebuffer.VfxFrameBuffer

/** Base class for any shader based single-pass filter. */
abstract class ShaderVfxEffect(protected val program: ShaderProgram)(using Sge) extends AbstractVfxEffect {

  /** Unbinds the current shader program (equivalent to LibGDX ShaderProgram.end()). */
  private def unbindProgram(): Unit =
    Sge().graphics.gl20.glUseProgram(0)

  override def close(): Unit =
    program.close()

  override def resize(width: Int, height: Int): Unit = {
    // Do nothing by default.
  }

  override def rebind(): Unit = {
    // Do nothing by default.
  }

  override def update(delta: Float): Unit = {
    // Do nothing by default.
  }

  protected def renderShader(context: VfxRenderContext, dst: VfxFrameBuffer): Unit = {
    val manualBufferBind = !dst.isDrawing
    if (manualBufferBind) { dst.begin() }

    program.bind()
    context.viewportMesh.render(program)
    unbindProgram()

    if (manualBufferBind) { dst.end() }
  }

  /** Updates shader's uniform of float type.
    *
    * <b>NOTE:</b> This is a utility method that will bind/unbind the shader program internally on every call. If you need to update multiple uniforms, please consider calling methods directly from
    * [[ShaderProgram]].
    */
  protected def setUniform(uniformName: String, value: Float): Unit = {
    program.bind()
    program.setUniformf(uniformName, value)
    unbindProgram()
  }

  /** Updates shader's uniform of int type. */
  protected def setUniform(uniformName: String, value: Int): Unit = {
    program.bind()
    program.setUniformi(uniformName, value)
    unbindProgram()
  }

  /** Updates shader's uniform of vec2 type. */
  protected def setUniform(uniformName: String, value: Vector2): Unit = {
    program.bind()
    program.setUniformf(uniformName, value)
    unbindProgram()
  }

  /** Updates shader's uniform of vec3 type. */
  protected def setUniform(uniformName: String, value: Vector3): Unit = {
    program.bind()
    program.setUniformf(uniformName, value)
    unbindProgram()
  }

  /** Updates shader's uniform of vec4 type. */
  protected def setUniform(uniformName: String, value: Color): Unit = {
    program.bind()
    program.setUniformf(uniformName, value)
    unbindProgram()
  }

  /** Updates shader's uniform of mat3 type. */
  protected def setUniform(uniformName: String, value: Matrix3): Unit = {
    program.bind()
    program.setUniformMatrix(uniformName, value)
    unbindProgram()
  }

  /** Updates shader's uniform of mat4 type. */
  protected def setUniform(uniformName: String, value: Matrix4): Unit = {
    program.bind()
    program.setUniformMatrix(uniformName, value)
    unbindProgram()
  }

  /** Updates shader's uniform array.
    * @param elementSize
    *   Defines the type of the uniform array: float[], vec2[], vec3[] or vec4[]. Expected value is within the range of [1..4] (inclusively).
    */
  protected def setUniform(uniformName: String, elementSize: Int, values: Array[Float], offset: Int, length: Int): Unit = {
    program.bind()
    elementSize match {
      case 1 => program.setUniform1fv(uniformName, values, offset, length)
      case 2 => program.setUniform2fv(uniformName, values, offset, length)
      case 3 => program.setUniform3fv(uniformName, values, offset, length)
      case 4 => program.setUniform4fv(uniformName, values, offset, length)
      case _ => throw IllegalArgumentException("elementSize has illegal value: " + elementSize + ". Possible values are 1..4")
    }
    unbindProgram()
  }
}

object ShaderVfxEffect {
  val TEXTURE_HANDLE0: Int = 0
  val TEXTURE_HANDLE1: Int = 1
  val TEXTURE_HANDLE2: Int = 2
  val TEXTURE_HANDLE3: Int = 3
  val TEXTURE_HANDLE4: Int = 4
  val TEXTURE_HANDLE5: Int = 5
  val TEXTURE_HANDLE6: Int = 6
  val TEXTURE_HANDLE7: Int = 7
}
