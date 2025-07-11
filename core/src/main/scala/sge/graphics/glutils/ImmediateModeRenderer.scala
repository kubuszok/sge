package sge
package graphics
package glutils

import sge.graphics.Color
import sge.math.Matrix4

trait ImmediateModeRenderer {
  def begin(projModelView: Matrix4, primitiveType: Int): Unit

  def flush(): Unit

  def color(color: Color): Unit

  def color(r: Float, g: Float, b: Float, a: Float): Unit

  def color(colorBits: Float): Unit

  def texCoord(u: Float, v: Float): Unit

  def normal(x: Float, y: Float, z: Float): Unit

  def vertex(x: Float, y: Float, z: Float): Unit

  def end(): Unit

  def getNumVertices(): Int

  def getMaxVertices(): Int

  def dispose(): Unit
}
