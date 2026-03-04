/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/utils/shapebuilders/EllipseShapeBuilder.java
 * Original authors: xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: Java static utility class -> Scala object; null params -> Nullable[T];
 *     GdxRuntimeException -> SgeError.InvalidInput
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d
package utils
package shapebuilders

import sge.graphics.GL20
import sge.graphics.g3d.utils.MeshPartBuilder
import sge.math.{ MathUtils, Vector3 }
import sge.utils.{ Nullable, SgeError }

/** Helper class with static methods to build ellipse shapes using {@link MeshPartBuilder}.
  * @author
  *   xoppa
  */
object EllipseShapeBuilder {
  import BaseShapeBuilder._

  /** Build a circle */
  def build(builder: MeshPartBuilder, radius: Float, divisions: Int, centerX: Float, centerY: Float, centerZ: Float, normalX: Float, normalY: Float, normalZ: Float): Unit =
    build(builder, radius, divisions, centerX, centerY, centerZ, normalX, normalY, normalZ, 0f, 360f)

  /** Build a circle */
  def build(builder: MeshPartBuilder, radius: Float, divisions: Int, center: Vector3, normal: Vector3): Unit =
    build(builder, radius, divisions, center.x, center.y, center.z, normal.x, normal.y, normal.z)

  /** Build a circle */
  def build(builder: MeshPartBuilder, radius: Float, divisions: Int, center: Vector3, normal: Vector3, tangent: Vector3, binormal: Vector3): Unit =
    build(
      builder,
      radius,
      divisions,
      center.x,
      center.y,
      center.z,
      normal.x,
      normal.y,
      normal.z,
      tangent.x,
      tangent.y,
      tangent.z,
      binormal.x,
      binormal.y,
      binormal.z
    )

  /** Build a circle */
  def build(
    builder:   MeshPartBuilder,
    radius:    Float,
    divisions: Int,
    centerX:   Float,
    centerY:   Float,
    centerZ:   Float,
    normalX:   Float,
    normalY:   Float,
    normalZ:   Float,
    tangentX:  Float,
    tangentY:  Float,
    tangentZ:  Float,
    binormalX: Float,
    binormalY: Float,
    binormalZ: Float
  ): Unit =
    build(
      builder,
      radius,
      divisions,
      centerX,
      centerY,
      centerZ,
      normalX,
      normalY,
      normalZ,
      tangentX,
      tangentY,
      tangentZ,
      binormalX,
      binormalY,
      binormalZ,
      0f,
      360f
    )

  /** Build a circle */
  def build(
    builder:   MeshPartBuilder,
    radius:    Float,
    divisions: Int,
    centerX:   Float,
    centerY:   Float,
    centerZ:   Float,
    normalX:   Float,
    normalY:   Float,
    normalZ:   Float,
    angleFrom: Float,
    angleTo:   Float
  ): Unit =
    build(builder, radius * 2f, radius * 2f, divisions, centerX, centerY, centerZ, normalX, normalY, normalZ, angleFrom, angleTo)

  /** Build a circle */
  def build(builder: MeshPartBuilder, radius: Float, divisions: Int, center: Vector3, normal: Vector3, angleFrom: Float, angleTo: Float): Unit =
    build(builder, radius, divisions, center.x, center.y, center.z, normal.x, normal.y, normal.z, angleFrom, angleTo)

  /** Build a circle */
  def build(builder: MeshPartBuilder, radius: Float, divisions: Int, center: Vector3, normal: Vector3, tangent: Vector3, binormal: Vector3, angleFrom: Float, angleTo: Float): Unit =
    build(
      builder,
      radius,
      divisions,
      center.x,
      center.y,
      center.z,
      normal.x,
      normal.y,
      normal.z,
      tangent.x,
      tangent.y,
      tangent.z,
      binormal.x,
      binormal.y,
      binormal.z,
      angleFrom,
      angleTo
    )

  /** Build a circle */
  def build(
    builder:   MeshPartBuilder,
    radius:    Float,
    divisions: Int,
    centerX:   Float,
    centerY:   Float,
    centerZ:   Float,
    normalX:   Float,
    normalY:   Float,
    normalZ:   Float,
    tangentX:  Float,
    tangentY:  Float,
    tangentZ:  Float,
    binormalX: Float,
    binormalY: Float,
    binormalZ: Float,
    angleFrom: Float,
    angleTo:   Float
  ): Unit =
    build(
      builder,
      radius * 2,
      radius * 2,
      0,
      0,
      divisions,
      centerX,
      centerY,
      centerZ,
      normalX,
      normalY,
      normalZ,
      tangentX,
      tangentY,
      tangentZ,
      binormalX,
      binormalY,
      binormalZ,
      angleFrom,
      angleTo
    )

  /** Build an ellipse */
  def build(builder: MeshPartBuilder, width: Float, height: Float, divisions: Int, centerX: Float, centerY: Float, centerZ: Float, normalX: Float, normalY: Float, normalZ: Float): Unit =
    build(builder, width, height, divisions, centerX, centerY, centerZ, normalX, normalY, normalZ, 0f, 360f)

  /** Build an ellipse */
  def build(builder: MeshPartBuilder, width: Float, height: Float, divisions: Int, center: Vector3, normal: Vector3): Unit =
    build(builder, width, height, divisions, center.x, center.y, center.z, normal.x, normal.y, normal.z)

  /** Build an ellipse */
  def build(builder: MeshPartBuilder, width: Float, height: Float, divisions: Int, center: Vector3, normal: Vector3, tangent: Vector3, binormal: Vector3): Unit =
    build(
      builder,
      width,
      height,
      divisions,
      center.x,
      center.y,
      center.z,
      normal.x,
      normal.y,
      normal.z,
      tangent.x,
      tangent.y,
      tangent.z,
      binormal.x,
      binormal.y,
      binormal.z
    )

  /** Build an ellipse */
  def build(
    builder:   MeshPartBuilder,
    width:     Float,
    height:    Float,
    divisions: Int,
    centerX:   Float,
    centerY:   Float,
    centerZ:   Float,
    normalX:   Float,
    normalY:   Float,
    normalZ:   Float,
    tangentX:  Float,
    tangentY:  Float,
    tangentZ:  Float,
    binormalX: Float,
    binormalY: Float,
    binormalZ: Float
  ): Unit =
    build(
      builder,
      width,
      height,
      divisions,
      centerX,
      centerY,
      centerZ,
      normalX,
      normalY,
      normalZ,
      tangentX,
      tangentY,
      tangentZ,
      binormalX,
      binormalY,
      binormalZ,
      0f,
      360f
    )

  /** Build an ellipse */
  def build(
    builder:   MeshPartBuilder,
    width:     Float,
    height:    Float,
    divisions: Int,
    centerX:   Float,
    centerY:   Float,
    centerZ:   Float,
    normalX:   Float,
    normalY:   Float,
    normalZ:   Float,
    angleFrom: Float,
    angleTo:   Float
  ): Unit =
    build(builder, width, height, 0f, 0f, divisions, centerX, centerY, centerZ, normalX, normalY, normalZ, angleFrom, angleTo)

  /** Build an ellipse */
  def build(builder: MeshPartBuilder, width: Float, height: Float, divisions: Int, center: Vector3, normal: Vector3, angleFrom: Float, angleTo: Float): Unit =
    build(builder, width, height, 0f, 0f, divisions, center.x, center.y, center.z, normal.x, normal.y, normal.z, angleFrom, angleTo)

  /** Build an ellipse */
  def build(builder: MeshPartBuilder, width: Float, height: Float, divisions: Int, center: Vector3, normal: Vector3, tangent: Vector3, binormal: Vector3, angleFrom: Float, angleTo: Float): Unit =
    build(
      builder,
      width,
      height,
      0f,
      0f,
      divisions,
      center.x,
      center.y,
      center.z,
      normal.x,
      normal.y,
      normal.z,
      tangent.x,
      tangent.y,
      tangent.z,
      binormal.x,
      binormal.y,
      binormal.z,
      angleFrom,
      angleTo
    )

  /** Build an ellipse */
  def build(
    builder:   MeshPartBuilder,
    width:     Float,
    height:    Float,
    divisions: Int,
    centerX:   Float,
    centerY:   Float,
    centerZ:   Float,
    normalX:   Float,
    normalY:   Float,
    normalZ:   Float,
    tangentX:  Float,
    tangentY:  Float,
    tangentZ:  Float,
    binormalX: Float,
    binormalY: Float,
    binormalZ: Float,
    angleFrom: Float,
    angleTo:   Float
  ): Unit =
    build(
      builder,
      width,
      height,
      0f,
      0f,
      divisions,
      centerX,
      centerY,
      centerZ,
      normalX,
      normalY,
      normalZ,
      tangentX,
      tangentY,
      tangentZ,
      binormalX,
      binormalY,
      binormalZ,
      angleFrom,
      angleTo
    )

  /** Build an ellipse */
  def build(
    builder:     MeshPartBuilder,
    width:       Float,
    height:      Float,
    innerWidth:  Float,
    innerHeight: Float,
    divisions:   Int,
    centerX:     Float,
    centerY:     Float,
    centerZ:     Float,
    normalX:     Float,
    normalY:     Float,
    normalZ:     Float,
    angleFrom:   Float,
    angleTo:     Float
  ): Unit = {
    tmpV1.set(normalX, normalY, normalZ).crs(0, 0, 1)
    tmpV2.set(normalX, normalY, normalZ).crs(0, 1, 0)
    if (tmpV2.lengthSq > tmpV1.lengthSq) tmpV1.set(tmpV2)
    tmpV2.set(tmpV1.nor()).crs(normalX, normalY, normalZ).nor()
    build(
      builder,
      width,
      height,
      innerWidth,
      innerHeight,
      divisions,
      centerX,
      centerY,
      centerZ,
      normalX,
      normalY,
      normalZ,
      tmpV1.x,
      tmpV1.y,
      tmpV1.z,
      tmpV2.x,
      tmpV2.y,
      tmpV2.z,
      angleFrom,
      angleTo
    )
  }

  /** Build an ellipse */
  def build(
    builder:     MeshPartBuilder,
    width:       Float,
    height:      Float,
    innerWidth:  Float,
    innerHeight: Float,
    divisions:   Int,
    centerX:     Float,
    centerY:     Float,
    centerZ:     Float,
    normalX:     Float,
    normalY:     Float,
    normalZ:     Float
  ): Unit =
    build(builder, width, height, innerWidth, innerHeight, divisions, centerX, centerY, centerZ, normalX, normalY, normalZ, 0f, 360f)

  /** Build an ellipse */
  def build(builder: MeshPartBuilder, width: Float, height: Float, innerWidth: Float, innerHeight: Float, divisions: Int, center: Vector3, normal: Vector3): Unit =
    build(builder, width, height, innerWidth, innerHeight, divisions, center.x, center.y, center.z, normal.x, normal.y, normal.z, 0f, 360f)

  /** Build an ellipse */
  def build(
    builder:     MeshPartBuilder,
    width:       Float,
    height:      Float,
    innerWidth:  Float,
    innerHeight: Float,
    divisions:   Int,
    centerX:     Float,
    centerY:     Float,
    centerZ:     Float,
    normalX:     Float,
    normalY:     Float,
    normalZ:     Float,
    tangentX:    Float,
    tangentY:    Float,
    tangentZ:    Float,
    binormalX:   Float,
    binormalY:   Float,
    binormalZ:   Float,
    angleFrom:   Float,
    angleTo:     Float
  ): Unit = {
    if (innerWidth <= 0 || innerHeight <= 0) {
      builder.ensureVertices(divisions + 2)
      builder.ensureTriangleIndices(divisions)
    } else if (innerWidth == width && innerHeight == height) {
      builder.ensureVertices(divisions + 1)
      builder.ensureIndices(divisions + 1)
      if (builder.getPrimitiveType() != GL20.GL_LINES) throw SgeError.InvalidInput("Incorrect primitive type : expect GL_LINES because innerWidth == width && innerHeight == height")
    } else {
      builder.ensureVertices((divisions + 1) * 2)
      builder.ensureRectangleIndices(divisions + 1)
    }

    val ao     = MathUtils.degreesToRadians * angleFrom
    val step   = (MathUtils.degreesToRadians * (angleTo - angleFrom)) / divisions
    val sxEx   = tmpV1.set(tangentX, tangentY, tangentZ).scl(width * 0.5f)
    val syEx   = tmpV2.set(binormalX, binormalY, binormalZ).scl(height * 0.5f)
    val sxIn   = tmpV3.set(tangentX, tangentY, tangentZ).scl(innerWidth * 0.5f)
    val syIn   = tmpV4.set(binormalX, binormalY, binormalZ).scl(innerHeight * 0.5f)
    val currIn = vertTmp3.set(Nullable.empty, Nullable.empty, Nullable.empty, Nullable.empty)
    currIn.hasUV = true
    currIn.hasPosition = true
    currIn.hasNormal = true
    currIn.uv.set(.5f, .5f)
    currIn.position.set(centerX, centerY, centerZ)
    currIn.normal.set(normalX, normalY, normalZ)
    val currEx = vertTmp4.set(Nullable.empty, Nullable.empty, Nullable.empty, Nullable.empty)
    currEx.hasUV = true
    currEx.hasPosition = true
    currEx.hasNormal = true
    currEx.uv.set(.5f, .5f)
    currEx.position.set(centerX, centerY, centerZ)
    currEx.normal.set(normalX, normalY, normalZ)
    val center = builder.vertex(currEx)
    var angle  = 0f
    val us     = 0.5f * (innerWidth / width)
    val vs     = 0.5f * (innerHeight / height)
    var i1: Short = 0
    var i2: Short = 0
    var i3: Short = 0
    var i4: Short = 0
    for (i <- 0 to divisions) {
      angle = ao + step * i
      val x = MathUtils.cos(angle)
      val y = MathUtils.sin(angle)
      currEx.position.set(centerX, centerY, centerZ).add(sxEx.x * x + syEx.x * y, sxEx.y * x + syEx.y * y, sxEx.z * x + syEx.z * y)
      currEx.uv.set(.5f + .5f * x, .5f + .5f * y)
      i1 = builder.vertex(currEx)

      if (innerWidth <= 0f || innerHeight <= 0f) {
        if (i != 0) builder.triangle(i1, i2, center)
        i2 = i1
      } else if (innerWidth == width && innerHeight == height) {
        if (i != 0) builder.line(i1, i2)
        i2 = i1
      } else {
        currIn.position.set(centerX, centerY, centerZ).add(sxIn.x * x + syIn.x * y, sxIn.y * x + syIn.y * y, sxIn.z * x + syIn.z * y)
        currIn.uv.set(.5f + us * x, .5f + vs * y)
        i2 = i1
        i1 = builder.vertex(currIn)

        if (i != 0) builder.rect(i1, i2, i4, i3)
        i4 = i2
        i3 = i1
      }
    }
  }
}
