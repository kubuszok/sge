/*
 * Compatibility extensions for the generated Attributes API.
 *
 * The old hand-ported Attributes had `getAs[T](type): Nullable[T]` using ClassTag.
 * The generated version only has `get(type): Nullable[Attribute]`.
 * This extension bridges the gap for the gltf module which calls getAs extensively.
 */
package sge.gltf

import lowlevel.Nullable

extension (attrs: sge.graphics.g3d.Attributes) {
  def getAs[T <: sge.graphics.g3d.Attribute](tpe: Long): Nullable[T] =
    attrs.get(tpe).asInstanceOf[Nullable[T]]
}

extension (env: sge.graphics.g3d.Environment) {
  def getAs[T <: sge.graphics.g3d.Attribute](tpe: Long): Nullable[T] =
    env.get(tpe).asInstanceOf[Nullable[T]]
}

// ShaderProgram.setUniformf/setUniformi/setUniformMatrix take Int locations,
// but gltf code uses UniformLocation (opaque Int). These overloads bridge the gap.
extension (p: sge.graphics.glutils.ShaderProgram) {
  def setUniformf(location: sge.graphics.UniformLocation, value: Float): Unit =
    p.setUniformf(location.toInt, value)
  def setUniformf(location: sge.graphics.UniformLocation, v1: Float, v2: Float): Unit =
    p.setUniformf(location.toInt, v1, v2)
  def setUniformf(location: sge.graphics.UniformLocation, v1: Float, v2: Float, v3: Float): Unit =
    p.setUniformf(location.toInt, v1, v2, v3)
  def setUniformf(location: sge.graphics.UniformLocation, v1: Float, v2: Float, v3: Float, v4: Float): Unit =
    p.setUniformf(location.toInt, v1, v2, v3, v4)
  def setUniformi(location: sge.graphics.UniformLocation, value: Int): Unit =
    p.setUniformi(location.toInt, value)
  def setUniformMatrix(location: sge.graphics.UniformLocation, matrix: sge.math.Matrix4): Unit =
    p.setUniformMatrix(location.toInt, matrix, false)
  def setUniformMatrix(location: sge.graphics.UniformLocation, matrix: sge.math.Matrix3): Unit =
    p.setUniformMatrix(location.toInt, matrix, false)
  def setUniformMatrix4fv(location: sge.graphics.UniformLocation, values: Array[Float], offset: Int, length: Int): Unit =
    p.setUniformMatrix4fv(location.toInt, values, offset, length)
  def setUniform2fv(location: sge.graphics.UniformLocation, values: Array[Float], offset: Int, length: Int): Unit =
    p.setUniform2fv(location.toInt, values, offset, length)
}
