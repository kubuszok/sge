/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/utils/shapebuilders/CylinderShapeBuilder.java
 * Original authors: xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: Java static utility class -> Scala object; null params -> Nullable[T]
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 87
 * Covenant-baseline-methods: CylinderShapeBuilder,angle,ao,build,curr1,curr2,hd,hh,hw,i1,i2,i3,i4,step,u,us
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/utils/shapebuilders/CylinderShapeBuilder.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
 */
package sge
package graphics
package g3d
package utils
package shapebuilders

import sge.graphics.g3d.utils.MeshPartBuilder
import sge.math.MathUtils
import sge.utils.Nullable

/** Helper class with static methods to build cylinders shapes using {@link MeshPartBuilder}.
  * @author
  *   xoppa
  */
object CylinderShapeBuilder {
  import BaseShapeBuilder._

  /** Build a cylinder */
  def build(builder: MeshPartBuilder, width: Float, height: Float, depth: Float, divisions: Int): Unit =
    build(builder, width, height, depth, divisions, 0, 360)

  /** Build a cylinder */
  def build(builder: MeshPartBuilder, width: Float, height: Float, depth: Float, divisions: Int, angleFrom: Float, angleTo: Float): Unit =
    build(builder, width, height, depth, divisions, angleFrom, angleTo, true)

  /** Build a cylinder */
  def build(builder: MeshPartBuilder, width: Float, height: Float, depth: Float, divisions: Int, angleFrom: Float, angleTo: Float, close: Boolean): Unit = {
    // FIXME create better cylinder method (- axis on which to create the cylinder (matrix?))
    val hw    = width * 0.5f
    val hh    = height * 0.5f
    val hd    = depth * 0.5f
    val ao    = MathUtils.degreesToRadians * angleFrom
    val step  = (MathUtils.degreesToRadians * (angleTo - angleFrom)) / divisions
    val us    = 1f / divisions
    var u     = 0f
    var angle = 0f
    val curr1 = vertTmp3.set(Nullable.empty, Nullable.empty, Nullable.empty, Nullable.empty)
    curr1.hasUV = true
    curr1.hasPosition = true
    curr1.hasNormal = true
    val curr2 = vertTmp4.set(Nullable.empty, Nullable.empty, Nullable.empty, Nullable.empty)
    curr2.hasUV = true
    curr2.hasPosition = true
    curr2.hasNormal = true
    var i1: Short = 0
    var i2: Short = 0
    var i3: Short = 0
    var i4: Short = 0

    builder.ensureVertices(2 * (divisions + 1))
    builder.ensureRectangleIndices(divisions)
    for (i <- 0 to divisions) {
      angle = ao + step * i
      u = 1f - us * i
      curr1.position.set(MathUtils.cos(angle) * hw, 0f, MathUtils.sin(angle) * hd)
      curr1.normal.set(curr1.position).nor()
      curr1.position.y = -hh
      curr1.uv.set(u, 1)
      curr2.position.set(curr1.position)
      curr2.normal.set(curr1.normal)
      curr2.position.y = hh
      curr2.uv.set(u, 0)
      i2 = builder.vertex(curr1)
      i1 = builder.vertex(curr2)
      if (i != 0) builder.rect(i3, i1, i2, i4) // FIXME don't duplicate lines and points
      i4 = i2
      i3 = i1
    }
    if (close) {
      EllipseShapeBuilder.build(builder, width, depth, 0, 0, divisions, 0, hh, 0, 0, 1, 0, 1, 0, 0, 0, 0, 1, angleFrom, angleTo)
      EllipseShapeBuilder.build(builder, width, depth, 0, 0, divisions, 0, -hh, 0, 0, -1, 0, -1, 0, 0, 0, 0, 1, 180f - angleTo, 180f - angleFrom)
    }
  }
}
