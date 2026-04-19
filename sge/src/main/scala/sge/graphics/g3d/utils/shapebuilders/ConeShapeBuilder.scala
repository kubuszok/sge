/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/utils/shapebuilders/ConeShapeBuilder.java
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
 * Covenant-baseline-loc: 71
 * Covenant-baseline-methods: ConeShapeBuilder,angle,ao,base,build,curr1,curr2,hd,hh,hw,i1,i2,step,u,us
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/utils/shapebuilders/ConeShapeBuilder.java
 * Covenant-verified: 2026-04-19
 */
package sge
package graphics
package g3d
package utils
package shapebuilders

import sge.graphics.g3d.utils.MeshPartBuilder
import sge.math.MathUtils
import sge.utils.Nullable

/** Helper class with static methods to build cone shapes using {@link MeshPartBuilder}.
  * @author
  *   xoppa
  */
object ConeShapeBuilder {
  import BaseShapeBuilder._

  def build(builder: MeshPartBuilder, width: Float, height: Float, depth: Float, divisions: Int): Unit =
    build(builder, width, height, depth, divisions, 0, 360)

  def build(builder: MeshPartBuilder, width: Float, height: Float, depth: Float, divisions: Int, angleFrom: Float, angleTo: Float): Unit =
    build(builder, width, height, depth, divisions, angleFrom, angleTo, true)

  def build(builder: MeshPartBuilder, width: Float, height: Float, depth: Float, divisions: Int, angleFrom: Float, angleTo: Float, close: Boolean): Unit = {
    // FIXME create better cylinder method (- axis on which to create the cone (matrix?))
    builder.ensureVertices(divisions + 2)
    builder.ensureTriangleIndices(divisions)

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
    val curr2 = vertTmp4.set(Nullable.empty, Nullable.empty, Nullable.empty, Nullable.empty).setPos(0, hh, 0).setNor(0, 1, 0).setUV(0.5f, 0)
    val base  = builder.vertex(curr2)
    var i1: Short = 0
    var i2: Short = 0
    for (i <- 0 to divisions) {
      angle = ao + step * i
      u = 1f - us * i
      curr1.position.set(MathUtils.cos(angle) * hw, 0f, MathUtils.sin(angle) * hd)
      curr1.normal.set(curr1.position).nor()
      curr1.position.y = -hh
      curr1.uv.set(u, 1)
      i1 = builder.vertex(curr1)
      if (i != 0) builder.triangle(base, i1, i2) // FIXME don't duplicate lines and points
      i2 = i1
    }
    if (close) EllipseShapeBuilder.build(builder, width, depth, 0, 0, divisions, 0, -hh, 0, 0, -1, 0, -1, 0, 0, 0, 0, 1, 180f - angleTo, 180f - angleFrom)
  }
}
