/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/attributes/PBRMatrixAttribute.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 */
package sge
package gltf
package scene3d
package attributes

import sge.graphics.g3d.Attribute
import sge.math.{ Matrix4, Vector3 }

class PBRMatrixAttribute(
  `type`: Long
) extends Attribute(`type`) {

  val matrix: Matrix4 = Matrix4()

  private def set(matrix: Matrix4): PBRMatrixAttribute = {
    this.matrix.set(matrix)
    this
  }

  def set(azymuthAngleDegree: Float): PBRMatrixAttribute = {
    this.matrix.setToRotation(Vector3.Y, azymuthAngleDegree)
    this
  }

  override def copy(): Attribute =
    PBRMatrixAttribute(`type`).set(matrix)

  override def compare(that: Attribute): Int =
    (`type` - that.`type`).toInt
}

object PBRMatrixAttribute {

  val EnvRotationAlias: String = "envRotation"
  val EnvRotation:      Long   = Attribute.register(EnvRotationAlias)

  def createEnvRotation(azymuthAngleDegree: Float): PBRMatrixAttribute =
    PBRMatrixAttribute(EnvRotation).set(azymuthAngleDegree)

  def createEnvRotation(matrix: Matrix4): PBRMatrixAttribute = {
    val attr = PBRMatrixAttribute(EnvRotation)
    attr.matrix.set(matrix)
    attr
  }
}
