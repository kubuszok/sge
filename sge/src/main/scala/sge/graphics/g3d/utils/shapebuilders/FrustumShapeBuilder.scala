/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/utils/shapebuilders/FrustumShapeBuilder.java
 * Original authors: realitix
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: null → Nullable
 *   Renames: len → length
 *   Idiom: Java static class → Scala object
 *   Audited: 2026-03-04 — pass
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 147
 * Covenant-baseline-methods: FrustumShapeBuilder,build,centerNear,centerPoint,halfNearSize,middlePoint,planePoints
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/utils/shapebuilders/FrustumShapeBuilder.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: bac009762b9d593223a6a3a3ff05b12c97ff7e40
 */
package sge
package graphics
package g3d
package utils
package shapebuilders

import sge.graphics.{ Camera, Color }
import sge.graphics.g3d.utils.MeshPartBuilder
import sge.math.{ Frustum, Vector3 }

/** FrustumShapeBuilder builds camera or frustum.
  *
  * @author
  *   realitix
  */
object FrustumShapeBuilder {
  import BaseShapeBuilder._

  /** Build camera with default colors
    * @param builder
    *   MeshPartBuilder
    * @param camera
    *   Camera
    */
  def build(builder: MeshPartBuilder, camera: Camera): Unit =
    build(
      builder,
      camera,
      tmpColor0.set(1, 0.66f, 0, 1),
      tmpColor1.set(1, 0, 0, 1),
      tmpColor2.set(0, 0.66f, 1, 1),
      tmpColor3.set(1, 1, 1, 1),
      tmpColor4.set(0.2f, 0.2f, 0.2f, 1)
    )

  /** Build Camera with custom colors
    * @param builder
    * @param camera
    * @param frustumColor
    * @param coneColor
    * @param upColor
    * @param targetColor
    * @param crossColor
    */
  def build(builder: MeshPartBuilder, camera: Camera, frustumColor: Color, coneColor: Color, upColor: Color, targetColor: Color, crossColor: Color): Unit = {
    val planePoints = camera.frustum.planePoints

    // Frustum
    build(builder, camera.frustum, frustumColor, crossColor)

    // Cones (camera position to near plane)
    builder.line(planePoints(0), coneColor, camera.position, coneColor)
    builder.line(planePoints(1), coneColor, camera.position, coneColor)
    builder.line(planePoints(2), coneColor, camera.position, coneColor)
    builder.line(planePoints(3), coneColor, camera.position, coneColor)

    // Target line
    builder.line(camera.position, targetColor, centerPoint(planePoints(4), planePoints(5), planePoints(6)), targetColor)

    // Up triangle
    val halfNearSize = tmpV0.set(planePoints(1)).sub(planePoints(0)).scl(0.5f).length
    val centerNear   = centerPoint(planePoints(0), planePoints(1), planePoints(2))
    tmpV0.set(camera.up).scl(halfNearSize * 2)
    centerNear.add(tmpV0)

    builder.line(centerNear, upColor, planePoints(2), upColor)
    builder.line(planePoints(2), upColor, planePoints(3), upColor)
    builder.line(planePoints(3), upColor, centerNear, upColor)
  }

  /** Build Frustum with custom colors
    * @param builder
    * @param frustum
    * @param frustumColor
    * @param crossColor
    */
  def build(builder: MeshPartBuilder, frustum: Frustum, frustumColor: Color, crossColor: Color): Unit = {
    val planePoints = frustum.planePoints

    // Near
    builder.line(planePoints(0), frustumColor, planePoints(1), frustumColor)
    builder.line(planePoints(1), frustumColor, planePoints(2), frustumColor)
    builder.line(planePoints(2), frustumColor, planePoints(3), frustumColor)
    builder.line(planePoints(3), frustumColor, planePoints(0), frustumColor)

    // Far
    builder.line(planePoints(4), frustumColor, planePoints(5), frustumColor)
    builder.line(planePoints(5), frustumColor, planePoints(6), frustumColor)
    builder.line(planePoints(6), frustumColor, planePoints(7), frustumColor)
    builder.line(planePoints(7), frustumColor, planePoints(4), frustumColor)

    // Sides
    builder.line(planePoints(0), frustumColor, planePoints(4), frustumColor)
    builder.line(planePoints(1), frustumColor, planePoints(5), frustumColor)
    builder.line(planePoints(2), frustumColor, planePoints(6), frustumColor)
    builder.line(planePoints(3), frustumColor, planePoints(7), frustumColor)

    // Cross near
    builder.line(middlePoint(planePoints(1), planePoints(0)), crossColor, middlePoint(planePoints(3), planePoints(2)), crossColor)
    builder.line(middlePoint(planePoints(2), planePoints(1)), crossColor, middlePoint(planePoints(3), planePoints(0)), crossColor)

    // Cross far
    builder.line(middlePoint(planePoints(5), planePoints(4)), crossColor, middlePoint(planePoints(7), planePoints(6)), crossColor)
    builder.line(middlePoint(planePoints(6), planePoints(5)), crossColor, middlePoint(planePoints(7), planePoints(4)), crossColor)
  }

  /** Return middle point's segment
    * @param point0
    *   First segment's point
    * @param point1
    *   Second segment's point
    * @return
    *   the middle point
    */
  private def middlePoint(point0: Vector3, point1: Vector3): Vector3 = {
    tmpV0.set(point1).sub(point0).scl(0.5f)
    tmpV1.set(point0).add(tmpV0)
  }

  /** Return center point's rectangle
    * @param point0
    * @param point1
    * @param point2
    * @return
    *   the center point
    */
  private def centerPoint(point0: Vector3, point1: Vector3, point2: Vector3): Vector3 = {
    tmpV0.set(point1).sub(point0).scl(0.5f)
    tmpV1.set(point0).add(tmpV0)
    tmpV0.set(point2).sub(point1).scl(0.5f)
    tmpV1.add(tmpV0)
  }
}
