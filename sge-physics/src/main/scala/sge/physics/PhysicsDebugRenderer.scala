/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (2D physics API backed by Rapier2D)
 *   Convention: handle-based FFI, platform-agnostic trait
 *   Audited: 2026-03-08
 */
package sge
package physics

import sge.graphics.Color
import sge.graphics.glutils.ShapeRenderer
import sge.math.{ MathUtils, Matrix4 }

/** Debug renderer that draws physics collider outlines using a [[ShapeRenderer]].
  *
  * Usage:
  * {{{
  *   val debugRenderer = PhysicsDebugRenderer()(using sge)
  *   // in render loop:
  *   debugRenderer.render(world, camera.combined)
  *   // when done:
  *   debugRenderer.close()
  * }}}
  *
  * @param maxVertices
  *   maximum vertex count for the internal ShapeRenderer
  */
class PhysicsDebugRenderer(maxVertices: Int = 5000)(using Sge) extends AutoCloseable {

  private val shapeRenderer = ShapeRenderer(maxVertices)

  /** Color for dynamic body outlines. */
  var dynamicColor: Color = Color(0f, 1f, 0f, 1f)

  /** Color for static body outlines. */
  var staticColor: Color = Color(0.5f, 0.5f, 0.5f, 1f)

  /** Color for kinematic body outlines. */
  var kinematicColor: Color = Color(0f, 0.5f, 1f, 1f)

  /** Number of line segments used to approximate a circle. */
  var circleSegments: Int = 20

  /** Renders debug outlines for all bodies in the given physics world.
    *
    * @param world
    *   the physics world to render
    * @param projectionMatrix
    *   the camera projection matrix (typically `camera.combined`)
    * @param bodies
    *   the bodies to render, each paired with its collider shapes
    */
  def render(world: PhysicsWorld, projectionMatrix: Matrix4, bodies: Iterable[DebugBodyInfo]): Unit = {
    shapeRenderer.setProjectionMatrix(projectionMatrix)
    shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
    bodies.foreach { info =>
      val color = info.bodyType match {
        case BodyType.Dynamic   => dynamicColor
        case BodyType.Static    => staticColor
        case BodyType.Kinematic => kinematicColor
      }
      shapeRenderer.setColor(color)
      val (px, py) = info.position
      val a        = info.angle
      val cosA     = MathUtils.cos(a)
      val sinA     = MathUtils.sin(a)
      info.shapes.foreach { shape =>
        drawShape(shape, px, py, cosA, sinA)
      }
    }
    shapeRenderer.end()
  }

  private def drawShape(shape: Shape, px: Float, py: Float, cosA: Float, sinA: Float): Unit = {
    shape match {
      case Shape.Circle(radius) =>
        shapeRenderer.circle(px, py, radius, circleSegments)

      case Shape.Box(hw, hh) =>
        // Rotate the four corners of the box
        val x0 = -hw; val y0 = -hh
        val x1 = hw;  val y1 = -hh
        val x2 = hw;  val y2 = hh
        val x3 = -hw; val y3 = hh
        val rx0 = px + x0 * cosA - y0 * sinA; val ry0 = py + x0 * sinA + y0 * cosA
        val rx1 = px + x1 * cosA - y1 * sinA; val ry1 = py + x1 * sinA + y1 * cosA
        val rx2 = px + x2 * cosA - y2 * sinA; val ry2 = py + x2 * sinA + y2 * cosA
        val rx3 = px + x3 * cosA - y3 * sinA; val ry3 = py + x3 * sinA + y3 * cosA
        shapeRenderer.line(rx0, ry0, rx1, ry1)
        shapeRenderer.line(rx1, ry1, rx2, ry2)
        shapeRenderer.line(rx2, ry2, rx3, ry3)
        shapeRenderer.line(rx3, ry3, rx0, ry0)

      case Shape.Capsule(halfHeight, radius) =>
        // Draw two semicircles and two connecting lines
        val top    = halfHeight
        val bottom = -halfHeight
        // Left and right lines
        val lx = -radius; val rx = radius
        val tlx = px + lx * cosA - top * sinA;    val tly = py + lx * sinA + top * cosA
        val trx = px + rx * cosA - top * sinA;    val try_ = py + rx * sinA + top * cosA
        val blx = px + lx * cosA - bottom * sinA; val bly = py + lx * sinA + bottom * cosA
        val brx = px + rx * cosA - bottom * sinA; val bry = py + rx * sinA + bottom * cosA
        shapeRenderer.line(tlx, tly, blx, bly)
        shapeRenderer.line(trx, try_, brx, bry)
        // Top and bottom semicircular caps (approximated as circles)
        val topCenterX = px - top * sinA;    val topCenterY = py + top * cosA
        val botCenterX = px - bottom * sinA; val botCenterY = py + bottom * cosA
        shapeRenderer.circle(topCenterX, topCenterY, radius, circleSegments)
        shapeRenderer.circle(botCenterX, botCenterY, radius, circleSegments)

      case Shape.Polygon(vertices) =>
        val count = vertices.length / 2
        if (count >= 2) {
          var i = 0
          while (i < count) {
            val j  = (i + 1) % count
            val vx = vertices(i * 2)
            val vy = vertices(i * 2 + 1)
            val wx = vertices(j * 2)
            val wy = vertices(j * 2 + 1)
            val ax = px + vx * cosA - vy * sinA
            val ay = py + vx * sinA + vy * cosA
            val bx = px + wx * cosA - wy * sinA
            val by = py + wx * sinA + wy * cosA
            shapeRenderer.line(ax, ay, bx, by)
            i += 1
          }
        }
    }
  }

  /** Releases the internal [[ShapeRenderer]]. */
  override def close(): Unit =
    shapeRenderer.close()
}

/** Debug info for a single body, used by [[PhysicsDebugRenderer.render]].
  *
  * @param bodyType
  *   the body's type
  * @param position
  *   the body's world position (x, y)
  * @param angle
  *   the body's rotation angle in radians
  * @param shapes
  *   the collision shapes attached to this body
  */
final case class DebugBodyInfo(
  bodyType: BodyType,
  position: (Float, Float),
  angle:    Float,
  shapes:   Seq[Shape]
)
