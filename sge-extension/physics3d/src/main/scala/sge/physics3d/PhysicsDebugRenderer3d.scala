/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (3D physics debug renderer)
 *   Convention: handle-based FFI, platform-agnostic
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 627
 * Covenant-baseline-methods: DebugBodyInfo3d,PhysicsDebugRenderer3d,angle,apex,c0,c1,c2,c3,c4,c5,c6,c7,circleSegments,close,count,cx,cy,cz,drawBox,drawCapsule,drawCircle,drawCircleAtY,drawCone,drawConvexHull,drawCylinder,drawHalfCircleArc,drawHeightfield,drawLine3,drawShape,drawSphere,drawTriMesh,dynamicColor,halfSegs,halfStep,i,j,kinematicColor,local,prevX,prevY,prevZ,render,rx,ry,rz,shapeRenderer,staticColor,step,t,transformPoint,triCount
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-17
 */
package sge
package physics3d

import sge.graphics.Color
import sge.graphics.glutils.ShapeRenderer
import sge.math.{ MathUtils, Matrix4 }

/** Debug renderer that draws 3D physics collider wireframes using a [[ShapeRenderer]].
  *
  * Usage:
  * {{{
  *   val debugRenderer = PhysicsDebugRenderer3d()(using sge)
  *   // in render loop:
  *   debugRenderer.render(world, camera.combined, bodies)
  *   // when done:
  *   debugRenderer.close()
  * }}}
  *
  * @param maxVertices
  *   maximum vertex count for the internal ShapeRenderer
  */
class PhysicsDebugRenderer3d(maxVertices: Int = 10000)(using Sge) extends AutoCloseable {

  private val shapeRenderer = ShapeRenderer(maxVertices)

  /** Color for dynamic body outlines. */
  var dynamicColor: Color = Color(0f, 1f, 0f, 1f)

  /** Color for static body outlines. */
  var staticColor: Color = Color(0.5f, 0.5f, 0.5f, 1f)

  /** Color for kinematic body outlines. */
  var kinematicColor: Color = Color(0f, 0.5f, 1f, 1f)

  /** Number of line segments used to approximate circles in sphere, cylinder, capsule, and cone wireframes. */
  var circleSegments: Int = 20

  /** Renders debug wireframe outlines for all bodies provided.
    *
    * @param world
    *   the physics world (unused directly, but kept for API symmetry with 2D)
    * @param projectionMatrix
    *   the camera projection matrix (typically `camera.combined` from a PerspectiveCamera)
    * @param bodies
    *   the bodies to render, each with position, rotation, and collider shapes
    */
  def render(world: PhysicsWorld3d, projectionMatrix: Matrix4, bodies: Iterable[DebugBodyInfo3d]): Unit = {
    shapeRenderer.setProjectionMatrix(projectionMatrix)
    shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
    bodies.foreach { info =>
      val color = info.bodyType match {
        case BodyType3d.Dynamic   => dynamicColor
        case BodyType3d.Static    => staticColor
        case BodyType3d.Kinematic => kinematicColor
      }
      shapeRenderer.setColor(color)
      val (px, py, pz)     = info.position
      val (qx, qy, qz, qw) = info.rotation
      info.shapes.foreach { shape =>
        drawShape(shape, px, py, pz, qx, qy, qz, qw)
      }
    }
    shapeRenderer.end()
  }

  // ─── Shape wireframe drawing ──────────────────────────────────────────

  private def drawShape(
    shape: Shape3d,
    px:    Float,
    py:    Float,
    pz:    Float,
    qx:    Float,
    qy:    Float,
    qz:    Float,
    qw:    Float
  ): Unit =
    shape match {
      case Shape3d.Sphere(radius) =>
        drawSphere(radius, px, py, pz, qx, qy, qz, qw)

      case Shape3d.Box(halfX, halfY, halfZ) =>
        drawBox(halfX, halfY, halfZ, px, py, pz, qx, qy, qz, qw)

      case Shape3d.Capsule(halfHeight, radius) =>
        drawCapsule(halfHeight, radius, px, py, pz, qx, qy, qz, qw)

      case Shape3d.Cylinder(halfHeight, radius) =>
        drawCylinder(halfHeight, radius, px, py, pz, qx, qy, qz, qw)

      case Shape3d.Cone(halfHeight, radius) =>
        drawCone(halfHeight, radius, px, py, pz, qx, qy, qz, qw)

      case Shape3d.ConvexHull(vertices) =>
        drawConvexHull(vertices, px, py, pz, qx, qy, qz, qw)

      case Shape3d.TriMesh(vertices, indices) =>
        drawTriMesh(vertices, indices, px, py, pz, qx, qy, qz, qw)

      case Shape3d.Heightfield(heights, nrows, ncols, scaleX, scaleY, scaleZ) =>
        drawHeightfield(heights, nrows, ncols, scaleX, scaleY, scaleZ, px, py, pz, qx, qy, qz, qw)
    }

  // ─── Sphere: 3 great circles (XY, XZ, YZ planes) ─────────────────────

  private def drawSphere(
    radius: Float,
    px:     Float,
    py:     Float,
    pz:     Float,
    qx:     Float,
    qy:     Float,
    qz:     Float,
    qw:     Float
  ): Unit = {
    val step = 2f * MathUtils.PI / circleSegments
    // XY plane circle (Z = 0)
    drawCircle(radius, step, 0, 1, 2, px, py, pz, qx, qy, qz, qw)
    // XZ plane circle (Y = 0)
    drawCircle(radius, step, 0, 2, 1, px, py, pz, qx, qy, qz, qw)
    // YZ plane circle (X = 0)
    drawCircle(radius, step, 1, 2, 0, px, py, pz, qx, qy, qz, qw)
  }

  /** Draws a circle of `radius` in a local plane defined by two axes.
    *
    * @param axis1
    *   first axis index of the circle plane (0=X, 1=Y, 2=Z)
    * @param axis2
    *   second axis index of the circle plane
    * @param axisConst
    *   the axis that is constant (0 for the plane)
    */
  private def drawCircle(
    radius:    Float,
    step:      Float,
    axis1:     Int,
    axis2:     Int,
    axisConst: Int,
    px:        Float,
    py:        Float,
    pz:        Float,
    qx:        Float,
    qy:        Float,
    qz:        Float,
    qw:        Float
  ): Unit = {
    val local = new Array[Float](3)
    var angle = 0f
    var prevX = 0f
    var prevY = 0f
    var prevZ = 0f
    var i     = 0
    while (i <= circleSegments) {
      local(0) = 0f
      local(1) = 0f
      local(2) = 0f
      local(axis1) = radius * MathUtils.cos(angle)
      local(axis2) = radius * MathUtils.sin(angle)
      local(axisConst) = 0f
      val (wx, wy, wz) = transformPoint(local(0), local(1), local(2), px, py, pz, qx, qy, qz, qw)
      if (i > 0) {
        shapeRenderer.line(prevX, prevY, prevZ, wx, wy, wz)
      }
      prevX = wx
      prevY = wy
      prevZ = wz
      angle += step
      i += 1
    }
  }

  // ─── Box: 12 edges of a cuboid ────────────────────────────────────────

  private def drawBox(
    hx: Float,
    hy: Float,
    hz: Float,
    px: Float,
    py: Float,
    pz: Float,
    qx: Float,
    qy: Float,
    qz: Float,
    qw: Float
  ): Unit = {
    // 8 corners of the box in local space
    val c0 = transformPoint(-hx, -hy, -hz, px, py, pz, qx, qy, qz, qw)
    val c1 = transformPoint(hx, -hy, -hz, px, py, pz, qx, qy, qz, qw)
    val c2 = transformPoint(hx, hy, -hz, px, py, pz, qx, qy, qz, qw)
    val c3 = transformPoint(-hx, hy, -hz, px, py, pz, qx, qy, qz, qw)
    val c4 = transformPoint(-hx, -hy, hz, px, py, pz, qx, qy, qz, qw)
    val c5 = transformPoint(hx, -hy, hz, px, py, pz, qx, qy, qz, qw)
    val c6 = transformPoint(hx, hy, hz, px, py, pz, qx, qy, qz, qw)
    val c7 = transformPoint(-hx, hy, hz, px, py, pz, qx, qy, qz, qw)

    // Bottom face (y = -hy)
    drawLine3(c0, c1); drawLine3(c1, c5); drawLine3(c5, c4); drawLine3(c4, c0)
    // Top face (y = +hy)
    drawLine3(c3, c2); drawLine3(c2, c6); drawLine3(c6, c7); drawLine3(c7, c3)
    // Vertical edges
    drawLine3(c0, c3); drawLine3(c1, c2); drawLine3(c5, c6); drawLine3(c4, c7)
  }

  /** Helper to draw a line from two (x, y, z) tuples. */
  private inline def drawLine3(a: (Float, Float, Float), b: (Float, Float, Float)): Unit =
    shapeRenderer.line(a._1, a._2, a._3, b._1, b._2, b._3)

  // ─── Capsule: cylinder + 2 hemisphere circles ─────────────────────────

  private def drawCapsule(
    halfHeight: Float,
    radius:     Float,
    px:         Float,
    py:         Float,
    pz:         Float,
    qx:         Float,
    qy:         Float,
    qz:         Float,
    qw:         Float
  ): Unit = {
    val step = 2f * MathUtils.PI / circleSegments
    // Draw the cylindrical section: two circles at y = +halfHeight and y = -halfHeight
    drawCircleAtY(radius, halfHeight, step, px, py, pz, qx, qy, qz, qw)
    drawCircleAtY(radius, -halfHeight, step, px, py, pz, qx, qy, qz, qw)

    // 4 vertical lines connecting top and bottom circles
    var j = 0
    while (j < 4) {
      val angle = j * MathUtils.PI * 0.5f
      val lx    = radius * MathUtils.cos(angle)
      val lz    = radius * MathUtils.sin(angle)
      val top   = transformPoint(lx, halfHeight, lz, px, py, pz, qx, qy, qz, qw)
      val bot   = transformPoint(lx, -halfHeight, lz, px, py, pz, qx, qy, qz, qw)
      drawLine3(top, bot)
      j += 1
    }

    // Hemisphere caps: half-circle arcs in XY and ZY planes at each end
    drawHalfCircleArc(radius, halfHeight, axis1 = 0, sign = 1f, px, py, pz, qx, qy, qz, qw)
    drawHalfCircleArc(radius, halfHeight, axis1 = 2, sign = 1f, px, py, pz, qx, qy, qz, qw)
    drawHalfCircleArc(radius, -halfHeight, axis1 = 0, sign = -1f, px, py, pz, qx, qy, qz, qw)
    drawHalfCircleArc(radius, -halfHeight, axis1 = 2, sign = -1f, px, py, pz, qx, qy, qz, qw)
  }

  /** Draws a half-circle arc for a hemisphere cap at the given Y offset.
    *
    * @param axis1
    *   0 for XY plane, 2 for ZY plane
    * @param sign
    *   +1 for top hemisphere (arcs upward), -1 for bottom (arcs downward)
    */
  private def drawHalfCircleArc(
    radius: Float,
    yOff:   Float,
    axis1:  Int,
    sign:   Float,
    px:     Float,
    py:     Float,
    pz:     Float,
    qx:     Float,
    qy:     Float,
    qz:     Float,
    qw:     Float
  ): Unit = {
    val halfSegs = circleSegments / 2
    val halfStep = MathUtils.PI / halfSegs
    var prevX    = 0f
    var prevY    = 0f
    var prevZ    = 0f
    var i        = 0
    while (i <= halfSegs) {
      val angle = -MathUtils.PI * 0.5f + i * halfStep
      val horiz = radius * MathUtils.cos(angle)
      val vert  = sign * radius * MathUtils.sin(angle)
      val local = new Array[Float](3)
      local(0) = 0f
      local(1) = yOff + vert
      local(2) = 0f
      local(axis1) = horiz
      val (wx, wy, wz) = transformPoint(local(0), local(1), local(2), px, py, pz, qx, qy, qz, qw)
      if (i > 0) {
        shapeRenderer.line(prevX, prevY, prevZ, wx, wy, wz)
      }
      prevX = wx
      prevY = wy
      prevZ = wz
      i += 1
    }
  }

  // ─── Cylinder: 2 circles + 4 vertical lines ──────────────────────────

  private def drawCylinder(
    halfHeight: Float,
    radius:     Float,
    px:         Float,
    py:         Float,
    pz:         Float,
    qx:         Float,
    qy:         Float,
    qz:         Float,
    qw:         Float
  ): Unit = {
    val step = 2f * MathUtils.PI / circleSegments
    // Top and bottom circles
    drawCircleAtY(radius, halfHeight, step, px, py, pz, qx, qy, qz, qw)
    drawCircleAtY(radius, -halfHeight, step, px, py, pz, qx, qy, qz, qw)

    // 4 vertical lines
    var j = 0
    while (j < 4) {
      val angle = j * MathUtils.PI * 0.5f
      val lx    = radius * MathUtils.cos(angle)
      val lz    = radius * MathUtils.sin(angle)
      val top   = transformPoint(lx, halfHeight, lz, px, py, pz, qx, qy, qz, qw)
      val bot   = transformPoint(lx, -halfHeight, lz, px, py, pz, qx, qy, qz, qw)
      drawLine3(top, bot)
      j += 1
    }
  }

  // ─── Cone: bottom circle + 4 lines from base to apex ─────────────────

  private def drawCone(
    halfHeight: Float,
    radius:     Float,
    px:         Float,
    py:         Float,
    pz:         Float,
    qx:         Float,
    qy:         Float,
    qz:         Float,
    qw:         Float
  ): Unit = {
    val step = 2f * MathUtils.PI / circleSegments
    // Bottom circle at y = -halfHeight
    drawCircleAtY(radius, -halfHeight, step, px, py, pz, qx, qy, qz, qw)

    // Apex at y = +halfHeight
    val apex = transformPoint(0f, halfHeight, 0f, px, py, pz, qx, qy, qz, qw)

    // 4 lines from base to apex
    var j = 0
    while (j < 4) {
      val angle     = j * MathUtils.PI * 0.5f
      val lx        = radius * MathUtils.cos(angle)
      val lz        = radius * MathUtils.sin(angle)
      val basePoint = transformPoint(lx, -halfHeight, lz, px, py, pz, qx, qy, qz, qw)
      drawLine3(basePoint, apex)
      j += 1
    }
  }

  // ─── ConvexHull: draw all vertex pair edges (point cloud visualization) ───

  private def drawConvexHull(
    vertices: Array[Float],
    px:       Float,
    py:       Float,
    pz:       Float,
    qx:       Float,
    qy:       Float,
    qz:       Float,
    qw:       Float
  ): Unit = {
    val count = vertices.length / 3
    if (count < 2) {
      // Single point or empty — draw as cross marker if there's at least one point
      if (count == 1) {
        val p = transformPoint(vertices(0), vertices(1), vertices(2), px, py, pz, qx, qy, qz, qw)
        val s = 0.05f
        shapeRenderer.line(p._1 - s, p._2, p._3, p._1 + s, p._2, p._3)
        shapeRenderer.line(p._1, p._2 - s, p._3, p._1, p._2 + s, p._3)
        shapeRenderer.line(p._1, p._2, p._3 - s, p._1, p._2, p._3 + s)
      }
    } else {
      // Without knowing the actual hull topology, we connect each vertex to its
      // nearest neighbors. Draw edges between consecutive vertices in the array —
      // this is a useful visual even though it may not match the true hull wireframe.
      val transformed = new Array[Float](count * 3)
      var i           = 0
      while (i < count) {
        val off          = i * 3
        val (wx, wy, wz) = transformPoint(vertices(off), vertices(off + 1), vertices(off + 2), px, py, pz, qx, qy, qz, qw)
        transformed(off) = wx
        transformed(off + 1) = wy
        transformed(off + 2) = wz
        i += 1
      }
      // Draw edges between consecutive vertices (closed polygon)
      i = 0
      while (i < count) {
        val j    = (i + 1) % count
        val off1 = i * 3
        val off2 = j * 3
        shapeRenderer.line(
          transformed(off1),
          transformed(off1 + 1),
          transformed(off1 + 2),
          transformed(off2),
          transformed(off2 + 1),
          transformed(off2 + 2)
        )
        i += 1
      }
    }
  }

  // ─── TriMesh: draw all triangle edges ─────────────────────────────────

  private def drawTriMesh(
    vertices: Array[Float],
    indices:  Array[Int],
    px:       Float,
    py:       Float,
    pz:       Float,
    qx:       Float,
    qy:       Float,
    qz:       Float,
    qw:       Float
  ): Unit = {
    val triCount = indices.length / 3
    var t        = 0
    while (t < triCount) {
      val i0 = indices(t * 3)
      val i1 = indices(t * 3 + 1)
      val i2 = indices(t * 3 + 2)

      val v0 = transformPoint(vertices(i0 * 3), vertices(i0 * 3 + 1), vertices(i0 * 3 + 2), px, py, pz, qx, qy, qz, qw)
      val v1 = transformPoint(vertices(i1 * 3), vertices(i1 * 3 + 1), vertices(i1 * 3 + 2), px, py, pz, qx, qy, qz, qw)
      val v2 = transformPoint(vertices(i2 * 3), vertices(i2 * 3 + 1), vertices(i2 * 3 + 2), px, py, pz, qx, qy, qz, qw)

      drawLine3(v0, v1)
      drawLine3(v1, v2)
      drawLine3(v2, v0)
      t += 1
    }
  }

  // ─── Heightfield: grid of connected quads ─────────────────────────────

  private def drawHeightfield(
    heights: Array[Float],
    nrows:   Int,
    ncols:   Int,
    scaleX:  Float,
    scaleY:  Float,
    scaleZ:  Float,
    px:      Float,
    py:      Float,
    pz:      Float,
    qx:      Float,
    qy:      Float,
    qz:      Float,
    qw:      Float
  ): Unit =
    if (nrows >= 2 && ncols >= 2) {
      // The heightfield is centered at origin. Grid spans:
      //   x: [-scaleX/2, +scaleX/2], z: [-scaleZ/2, +scaleZ/2]
      val dx = scaleX / (ncols - 1)
      val dz = scaleZ / (nrows - 1)
      val ox = -scaleX * 0.5f
      val oz = -scaleZ * 0.5f

      // Transform all grid points
      val points = new Array[Float](nrows * ncols * 3)
      var row    = 0
      while (row < nrows) {
        var col = 0
        while (col < ncols) {
          val idx          = row * ncols + col
          val lx           = ox + col * dx
          val ly           = heights(idx) * scaleY
          val lz           = oz + row * dz
          val (wx, wy, wz) = transformPoint(lx, ly, lz, px, py, pz, qx, qy, qz, qw)
          val off          = idx * 3
          points(off) = wx
          points(off + 1) = wy
          points(off + 2) = wz
          col += 1
        }
        row += 1
      }

      // Draw horizontal lines (along columns within each row)
      row = 0
      while (row < nrows) {
        var col = 0
        while (col < ncols - 1) {
          val off1 = (row * ncols + col) * 3
          val off2 = (row * ncols + col + 1) * 3
          shapeRenderer.line(
            points(off1),
            points(off1 + 1),
            points(off1 + 2),
            points(off2),
            points(off2 + 1),
            points(off2 + 2)
          )
          col += 1
        }
        row += 1
      }

      // Draw vertical lines (along rows within each column)
      var col = 0
      while (col < ncols) {
        row = 0
        while (row < nrows - 1) {
          val off1 = (row * ncols + col) * 3
          val off2 = ((row + 1) * ncols + col) * 3
          shapeRenderer.line(
            points(off1),
            points(off1 + 1),
            points(off1 + 2),
            points(off2),
            points(off2 + 1),
            points(off2 + 2)
          )
          row += 1
        }
        col += 1
      }
    }

  // ─── Shared helpers ───────────────────────────────────────────────────

  /** Draws a horizontal circle (in the XZ plane) at the given local Y offset, transformed by the body's position and rotation.
    */
  private def drawCircleAtY(
    radius: Float,
    yOff:   Float,
    step:   Float,
    px:     Float,
    py:     Float,
    pz:     Float,
    qx:     Float,
    qy:     Float,
    qz:     Float,
    qw:     Float
  ): Unit = {
    var prevX = 0f
    var prevY = 0f
    var prevZ = 0f
    var angle = 0f
    var i     = 0
    while (i <= circleSegments) {
      val lx           = radius * MathUtils.cos(angle)
      val lz           = radius * MathUtils.sin(angle)
      val (wx, wy, wz) = transformPoint(lx, yOff, lz, px, py, pz, qx, qy, qz, qw)
      if (i > 0) {
        shapeRenderer.line(prevX, prevY, prevZ, wx, wy, wz)
      }
      prevX = wx
      prevY = wy
      prevZ = wz
      angle += step
      i += 1
    }
  }

  /** Transforms a local-space point by the body's position and quaternion rotation.
    *
    * Uses the optimized quaternion rotation formula: v' = v + 2 * cross(q.xyz, cross(q.xyz, v) + qw * v)
    */
  private def transformPoint(
    lx: Float,
    ly: Float,
    lz: Float,
    px: Float,
    py: Float,
    pz: Float,
    qx: Float,
    qy: Float,
    qz: Float,
    qw: Float
  ): (Float, Float, Float) = {
    // Quaternion rotation: v' = q * v * q^-1
    // Optimized: v' = v + 2 * cross(q.xyz, cross(q.xyz, v) + qw * v)
    val cx = qy * lz - qz * ly + qw * lx
    val cy = qz * lx - qx * lz + qw * ly
    val cz = qx * ly - qy * lx + qw * lz
    val rx = lx + 2f * (qy * cz - qz * cy)
    val ry = ly + 2f * (qz * cx - qx * cz)
    val rz = lz + 2f * (qx * cy - qy * cx)
    (px + rx, py + ry, pz + rz)
  }

  /** Releases the internal [[ShapeRenderer]]. */
  override def close(): Unit =
    shapeRenderer.close()
}

/** Debug info for a single 3D body, used by [[PhysicsDebugRenderer3d.render]].
  *
  * @param bodyType
  *   the body's type
  * @param position
  *   the body's world position (x, y, z)
  * @param rotation
  *   the body's rotation as a quaternion (qx, qy, qz, qw)
  * @param shapes
  *   the collision shapes attached to this body
  */
final case class DebugBodyInfo3d(
  bodyType: BodyType3d,
  position: (Float, Float, Float),
  rotation: (Float, Float, Float, Float),
  shapes:   Seq[Shape3d]
)
