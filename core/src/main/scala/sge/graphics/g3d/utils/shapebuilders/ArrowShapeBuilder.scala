/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/utils/shapebuilders/ArrowShapeBuilder.java
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
import sge.math.{ Matrix4, Vector3 }
import sge.utils.Nullable

/** Helper class with static methods to build arrow shapes using {@link MeshPartBuilder}.
  * @author
  *   xoppa
  */
object ArrowShapeBuilder {
  import BaseShapeBuilder._

  /** Build an arrow
    * @param x1
    *   source x
    * @param y1
    *   source y
    * @param z1
    *   source z
    * @param x2
    *   destination x
    * @param y2
    *   destination y
    * @param z2
    *   destination z
    * @param capLength
    *   is the height of the cap in percentage, must be in (0,1)
    * @param stemThickness
    *   is the percentage of stem diameter compared to cap diameter, must be in (0,1]
    * @param divisions
    *   the amount of vertices used to generate the cap and stem ellipsoidal bases
    */
  def build(builder: MeshPartBuilder, x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float, capLength: Float, stemThickness: Float, divisions: Int): Unit = {
    val begin        = obtainV3().set(x1, y1, z1)
    val end          = obtainV3().set(x2, y2, z2)
    val length       = begin.distance(end)
    val coneHeight   = length * capLength
    val coneDiameter = 2 * (coneHeight * Math.sqrt(1f / 3)).toFloat
    val stemLength   = length - coneHeight
    val stemDiameter = coneDiameter * stemThickness

    val up      = obtainV3().set(end).sub(begin).nor()
    val forward = obtainV3().set(up).crs(Vector3.Z)
    if (forward.isZero) forward.set(Vector3.X)
    forward.crs(up).nor()
    val left      = obtainV3().set(up).crs(forward).nor()
    val direction = obtainV3().set(end).sub(begin).nor()

    // Matrices
    val userTransform = builder.getVertexTransform(obtainM4())
    val transform     = obtainM4()
    val values        = transform.values
    values(Matrix4.M00) = left.x
    values(Matrix4.M01) = up.x
    values(Matrix4.M02) = forward.x
    values(Matrix4.M10) = left.y
    values(Matrix4.M11) = up.y
    values(Matrix4.M12) = forward.y
    values(Matrix4.M20) = left.z
    values(Matrix4.M21) = up.z
    values(Matrix4.M22) = forward.z
    val temp = obtainM4()

    // Stem
    transform.setTranslation(obtainV3().set(direction).scl(stemLength / 2).add(x1, y1, z1))
    builder.setVertexTransform(Nullable(temp.set(transform).mul(userTransform)))
    CylinderShapeBuilder.build(builder, stemDiameter, stemLength, stemDiameter, divisions)

    // Cap
    transform.setTranslation(obtainV3().set(direction).scl(stemLength).add(x1, y1, z1))
    builder.setVertexTransform(Nullable(temp.set(transform).mul(userTransform)))
    ConeShapeBuilder.build(builder, coneDiameter, coneHeight, coneDiameter, divisions)

    builder.setVertexTransform(Nullable(userTransform))
    freeAll()
  }
}
