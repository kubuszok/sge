/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/utils/shapebuilders/CapsuleShapeBuilder.java
 * Original authors: xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d
package utils
package shapebuilders

import sge.graphics.g3d.utils.MeshPartBuilder
import sge.utils.SgeError

/** Helper class with static methods to build capsule shapes using {@link MeshPartBuilder}.
  * @author
  *   xoppa
  */
@scala.annotation.nowarn("msg=deprecated")
object CapsuleShapeBuilder {
  import BaseShapeBuilder._

  def build(builder: MeshPartBuilder, radius: Float, height: Float, divisions: Int): Unit = {
    if (height < 2f * radius) throw SgeError.InvalidInput("Height must be at least twice the radius")
    val d = 2f * radius
    CylinderShapeBuilder.build(builder, d, height - d, d, divisions, 0, 360, false)
    SphereShapeBuilder.build(builder, matTmp1.setToTranslation(0, .5f * (height - d), 0), d, d, d, divisions, divisions, 0, 360, 0, 90)
    SphereShapeBuilder.build(builder, matTmp1.setToTranslation(0, -.5f * (height - d), 0), d, d, d, divisions, divisions, 0, 360, 90, 180)
  }
}
