/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/utils/shapebuilders/SphereShapeBuilder.java
 * Original authors: xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: Java static utility class -> Scala object; short[] indices ->
 *     DynamicArray[Short]; null params -> Nullable[T]
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 127
 * Covenant-baseline-methods: SphereShapeBuilder,angleU,angleV,auo,avo,build,closedVFrom,closedVTo,curr1,hd,hh,hw,normalTransform,s,stepU,stepV,tempOffset,tmpIndices,u,us,v,vs
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/utils/shapebuilders/SphereShapeBuilder.java
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
import sge.math.{ MathUtils, Matrix3, Matrix4 }
import sge.utils.{ DynamicArray, Nullable }

/** Helper class with static methods to build sphere shapes using {@link MeshPartBuilder}.
  * @author
  *   xoppa
  */
@scala.annotation.nowarn("msg=deprecated")
object SphereShapeBuilder {
  import BaseShapeBuilder._

  private val tmpIndices:      DynamicArray[Short] = DynamicArray[Short]()
  private val normalTransform: Matrix3             = Matrix3()

  def build(builder: MeshPartBuilder, width: Float, height: Float, depth: Float, divisionsU: Int, divisionsV: Int): Unit =
    build(builder, width, height, depth, divisionsU, divisionsV, 0, 360, 0, 180)

  /** @deprecated
    *   use {@link MeshPartBuilder#setVertexTransform(Matrix4)} instead of using the method signature taking a matrix.
    */
  @deprecated("use MeshPartBuilder.setVertexTransform instead", "")
  def build(builder: MeshPartBuilder, transform: Matrix4, width: Float, height: Float, depth: Float, divisionsU: Int, divisionsV: Int): Unit =
    build(builder, transform, width, height, depth, divisionsU, divisionsV, 0, 360, 0, 180)

  def build(builder: MeshPartBuilder, width: Float, height: Float, depth: Float, divisionsU: Int, divisionsV: Int, angleUFrom: Float, angleUTo: Float, angleVFrom: Float, angleVTo: Float): Unit =
    build(builder, matTmp1.idt(), width, height, depth, divisionsU, divisionsV, angleUFrom, angleUTo, angleVFrom, angleVTo)

  /** @deprecated
    *   use {@link MeshPartBuilder#setVertexTransform(Matrix4)} instead of using the method signature taking a matrix.
    */
  @deprecated("use MeshPartBuilder.setVertexTransform instead", "")
  def build(
    builder:    MeshPartBuilder,
    transform:  Matrix4,
    width:      Float,
    height:     Float,
    depth:      Float,
    divisionsU: Int,
    divisionsV: Int,
    angleUFrom: Float,
    angleUTo:   Float,
    angleVFrom: Float,
    angleVTo:   Float
  ): Unit = {
    val closedVFrom = MathUtils.isEqual(angleVFrom, 0f)
    val closedVTo   = MathUtils.isEqual(angleVTo, 180f)
    val hw          = width * 0.5f
    val hh          = height * 0.5f
    val hd          = depth * 0.5f
    val auo         = MathUtils.degreesToRadians * angleUFrom
    val stepU       = (MathUtils.degreesToRadians * (angleUTo - angleUFrom)) / divisionsU
    val avo         = MathUtils.degreesToRadians * angleVFrom
    val stepV       = (MathUtils.degreesToRadians * (angleVTo - angleVFrom)) / divisionsV
    val us          = 1f / divisionsU
    val vs          = 1f / divisionsV
    var u           = 0f
    var v           = 0f
    var angleU      = 0f
    var angleV      = 0f
    val curr1       = vertTmp3.set(Nullable.empty, Nullable.empty, Nullable.empty, Nullable.empty)
    curr1.hasUV = true
    curr1.hasPosition = true
    curr1.hasNormal = true

    normalTransform.set(transform)

    val s = divisionsU + 3
    tmpIndices.clear()
    tmpIndices.ensureCapacity(divisionsU * 2)
    tmpIndices.setSize(s)
    var tempOffset = 0

    builder.ensureVertices((divisionsV + 1) * (divisionsU + 1))
    builder.ensureRectangleIndices(divisionsU)
    for (iv <- 0 to divisionsV) {
      angleV = avo + stepV * iv
      v = vs * iv
      val t = MathUtils.sin(angleV)
      val h = MathUtils.cos(angleV) * hh
      for (iu <- 0 to divisionsU) {
        angleU = auo + stepU * iu
        if ((iv == 0 && closedVFrom) || (iv == divisionsV && closedVTo)) {
          u = 1f - us * (iu - .5f)
        } else {
          u = 1f - us * iu
        }
        curr1.position.set(MathUtils.cos(angleU) * hw * t, h, MathUtils.sin(angleU) * hd * t)
        curr1.normal.set(curr1.position).mul(normalTransform).nor()
        curr1.position.mul(transform)
        curr1.uv.set(u, v)
        tmpIndices(tempOffset) = builder.vertex(curr1)
        val o = tempOffset + s
        if ((iv > 0) && (iu > 0)) { // FIXME don't duplicate lines and points
          if (iv == 1 && closedVFrom) {
            builder.triangle(tmpIndices(tempOffset), tmpIndices((o - 1) % s), tmpIndices((o - (divisionsU + 1)) % s))
          } else if (iv == divisionsV && closedVTo) {
            builder.triangle(tmpIndices(tempOffset), tmpIndices((o - (divisionsU + 2)) % s), tmpIndices((o - (divisionsU + 1)) % s))
          } else {
            builder.rect(tmpIndices(tempOffset), tmpIndices((o - 1) % s), tmpIndices((o - (divisionsU + 2)) % s), tmpIndices((o - (divisionsU + 1)) % s))
          }
        }
        tempOffset = (tempOffset + 1) % tmpIndices.size
      }
    }
  }
}
