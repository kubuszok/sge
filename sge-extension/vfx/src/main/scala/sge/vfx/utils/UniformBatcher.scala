/*
 * Ported from gdx-vfx - https://github.com/crashinvaders/gdx-vfx
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package vfx
package utils

import sge.graphics.glutils.ShaderProgram
import sge.math.{ Matrix3, Matrix4, Vector2, Vector3 }
import sge.utils.Nullable

class UniformBatcher(using Sge) {

  private var program:        Nullable[ShaderProgram] = Nullable.empty
  private var activateShader: Boolean                 = false

  def begin(prog: ShaderProgram, activate: Boolean): UniformBatcher = {
    this.program = Nullable(prog)
    this.activateShader = activate

    if (activate) {
      prog.bind()
    }

    this
  }

  /** Should be called after set* method calls. */
  def end(): Unit =
    if (activateShader) {
      Sge().graphics.gl20.glUseProgram(0)
    }

  /** Updates shader's uniform of float type. */
  def set(uniformName: String, value: Float): UniformBatcher = {
    program.get.setUniformf(uniformName, value)
    this
  }

  /** Updates shader's uniform of int type. */
  def set(uniformName: String, value: Int): UniformBatcher = {
    program.get.setUniformi(uniformName, value)
    this
  }

  /** Updates shader's uniform of vec2 type. */
  def set(uniformName: String, value: Vector2): UniformBatcher = {
    program.get.setUniformf(uniformName, value)
    this
  }

  /** Updates shader's uniform of vec3 type. */
  def set(uniformName: String, value: Vector3): UniformBatcher = {
    program.get.setUniformf(uniformName, value)
    this
  }

  /** Updates shader's uniform of mat3 type. */
  def set(uniformName: String, value: Matrix3): UniformBatcher = {
    program.get.setUniformMatrix(uniformName, value)
    this
  }

  /** Updates shader's uniform of mat4 type. */
  def set(uniformName: String, value: Matrix4): UniformBatcher = {
    program.get.setUniformMatrix(uniformName, value)
    this
  }

  /** Updates shader's uniform array.
    * @param elementSize
    *   could be 1..4 and defines type of the uniform array: float[], vec2[], vec3[] or vec4[].
    */
  def set(uniformName: String, elementSize: Int, values: Array[Float], offset: Int, length: Int): UniformBatcher = {
    elementSize match {
      case 1 => program.get.setUniform1fv(uniformName, values, offset, length)
      case 2 => program.get.setUniform2fv(uniformName, values, offset, length)
      case 3 => program.get.setUniform3fv(uniformName, values, offset, length)
      case 4 => program.get.setUniform4fv(uniformName, values, offset, length)
      case _ => throw IllegalArgumentException("elementSize has illegal value: " + elementSize + ". Possible values are 1..4")
    }
    this
  }
}
