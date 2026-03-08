/*
 * SGE Demos — interactive curve and geometry visualization playground.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package demos
package curves

import scala.compiletime.uninitialized

import _root_.sge.{Pixels, Sge}
import _root_.sge.graphics.Color
import _root_.sge.graphics.glutils.ShapeRenderer
import _root_.sge.math.{
  Bezier, Bresenham2, CatmullRomSpline, ConvexHull, DelaunayTriangulator,
  EarClippingTriangulator, GeometryUtils, Interpolation, MathUtils, Vector2
}
import _root_.sge.utils.{DynamicArray, ScreenUtils}
import _root_.sge.utils.viewport.FitViewport
import sge.demos.shared.DemoScene

/** Interactive math visualization playground.
  *
  * Left half: draggable Bezier + CatmullRom curves.
  * Right half: ConvexHull, Delaunay, EarClipping on random points.
  * Bottom strip: interpolation easing curve gallery.
  * Also shows a Bresenham2 pixel-perfect diagonal line.
  */
object CurvePlayground extends DemoScene {

  override def name: String = "Curve Playground"

  private val WorldW = 800f
  private val WorldH = 600f

  // --- State ---
  private var shapeRenderer: ShapeRenderer = uninitialized
  private var viewport: FitViewport        = uninitialized

  // Left half: control points for curves
  private val controlPoints: Array[Vector2] = Array(
    Vector2(60f, 350f), Vector2(140f, 520f),
    Vector2(260f, 200f), Vector2(360f, 450f)
  )
  private var draggingIndex: Int = -1
  private val touchWorld: Vector2 = Vector2(0f, 0f)

  // Curve objects
  private var bezier: Bezier[Vector2]               = uninitialized
  private var catmullRom: CatmullRomSpline[Vector2]  = uninitialized
  private val curveOut: Vector2 = Vector2(0f, 0f)

  // Right half: random scattered points for geometry tools
  private val NumGeoPoints = 8
  private var geoPoints: Array[Float] = uninitialized

  // Geometry tool instances
  private val convexHull: ConvexHull                       = ConvexHull()
  private val delaunay: DelaunayTriangulator                = DelaunayTriangulator()
  private val earClipper: EarClippingTriangulator           = EarClippingTriangulator()
  private val bresenham: Bresenham2                         = Bresenham2()

  // Interpolation gallery
  private val interpCurves: Array[(String, Interpolation, Color)] = Array(
    ("linear", Interpolation.linear, Color.WHITE),
    ("pow2", Interpolation.pow2, Color.RED),
    ("smooth", Interpolation.smooth, Color.GREEN),
    ("sine", Interpolation.sine, Color.CYAN),
    ("bounce", Interpolation.bounce, Color.YELLOW),
    ("elastic", Interpolation.elastic, Color(1f, 0.5f, 0f, 1f)),
    ("exp5", Interpolation.exp5, Color(0.5f, 0.5f, 1f, 1f)),
    ("swing", Interpolation.swing, Color(1f, 0.3f, 0.7f, 1f))
  )

  // --- Lifecycle ---

  override def init()(using Sge): Unit = {
    shapeRenderer = ShapeRenderer()
    viewport = FitViewport(WorldW, WorldH)

    bezier = Bezier[Vector2](controlPoints(0), controlPoints(1), controlPoints(2), controlPoints(3))
    catmullRom = CatmullRomSpline[Vector2]()
    catmullRom.set(controlPoints, false)

    geoPoints = new Array[Float](NumGeoPoints * 2)
    randomizeGeoPoints()
  }

  override def render(dt: Float)(using Sge): Unit = {
    handleInput()
    draw()
  }

  override def resize(width: Pixels, height: Pixels)(using Sge): Unit = {
    viewport.update(width, height, true)
  }

  override def dispose()(using Sge): Unit = {
    shapeRenderer.close()
  }

  // --- Input handling ---

  private def handleInput()(using Sge): Unit = {
    val input = Sge().input

    // Randomize right-half points on R
    if (input.isKeyJustPressed(Input.Keys.R)) {
      randomizeGeoPoints()
    }

    // Drag control points
    if (input.isTouched()) {
      touchWorld.set(input.getX().toFloat, input.getY().toFloat)
      viewport.unproject(touchWorld)

      if (draggingIndex < 0) {
        // Find nearest control point within grab radius
        var bestDist = 30f * 30f
        var bestIdx  = -1
        var i = 0
        while (i < controlPoints.length) {
          val dx = touchWorld.x - controlPoints(i).x
          val dy = touchWorld.y - controlPoints(i).y
          val d2 = dx * dx + dy * dy
          if (d2 < bestDist) {
            bestDist = d2
            bestIdx = i
          }
          i += 1
        }
        draggingIndex = bestIdx
      }

      if (draggingIndex >= 0) {
        controlPoints(draggingIndex).set(touchWorld.x, touchWorld.y)
        // Rebuild curves after moving a point
        bezier.set(controlPoints(0), controlPoints(1), controlPoints(2), controlPoints(3))
        catmullRom.set(controlPoints, false)
      }
    } else {
      draggingIndex = -1
    }
  }

  // --- Drawing ---

  private def draw()(using Sge): Unit = {
    ScreenUtils.clear(0.1f, 0.1f, 0.12f, 1f)
    viewport.apply()
    val cam = viewport.camera
    shapeRenderer.setProjectionMatrix(cam.combined)

    drawCurves()
    drawGeometry()
    drawInterpolationGallery()
    drawBresenhamLine()
  }

  private def drawCurves()(using Sge): Unit = {
    val samples = 50

    // Control point connections (thin lines)
    shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
    shapeRenderer.setColor(Color(0.4f, 0.4f, 0.4f, 1f))
    var i = 0
    while (i < controlPoints.length - 1) {
      val a = controlPoints(i)
      val b = controlPoints(i + 1)
      shapeRenderer.line(a.x, a.y, b.x, b.y)
      i += 1
    }

    // Bezier curve (cyan)
    shapeRenderer.setColor(Color.CYAN)
    i = 0
    while (i < samples) {
      val t0 = i.toFloat / samples
      val t1 = (i + 1).toFloat / samples
      bezier.valueAt(curveOut, t0)
      val x0 = curveOut.x; val y0 = curveOut.y
      bezier.valueAt(curveOut, t1)
      shapeRenderer.line(x0, y0, curveOut.x, curveOut.y)
      i += 1
    }

    // CatmullRom spline (yellow)
    shapeRenderer.setColor(Color.YELLOW)
    i = 0
    while (i < samples) {
      val t0 = i.toFloat / samples
      val t1 = (i + 1).toFloat / samples
      catmullRom.valueAt(curveOut, t0)
      val x0 = curveOut.x; val y0 = curveOut.y
      catmullRom.valueAt(curveOut, t1)
      shapeRenderer.line(x0, y0, curveOut.x, curveOut.y)
      i += 1
    }
    shapeRenderer.end()

    // Control points as filled circles
    shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
    shapeRenderer.setColor(Color.WHITE)
    controlPoints.foreach { p =>
      shapeRenderer.circle(p.x, p.y, 6f)
    }
    shapeRenderer.end()
  }

  private def drawGeometry()(using Sge): Unit = {
    // Right half offset
    val ox = 420f
    val oy = 150f

    // Compute convex hull
    val hullPoly = convexHull.computePolygon(geoPoints, false)
    val hullArr = new Array[Float](hullPoly.size)
    var i = 0
    while (i < hullPoly.size) {
      hullArr(i) = hullPoly(i)
      i += 1
    }

    shapeRenderer.begin(ShapeRenderer.ShapeType.Line)

    // Convex hull outline (green)
    shapeRenderer.setColor(Color.GREEN)
    i = 0
    while (i < hullArr.length - 2) {
      shapeRenderer.line(
        hullArr(i) + ox, hullArr(i + 1) + oy,
        hullArr(i + 2) + ox, hullArr(i + 3) + oy
      )
      i += 2
    }

    // Delaunay triangulation (blue thin triangles)
    val triIndices = delaunay.computeTriangles(geoPoints, false)
    shapeRenderer.setColor(Color.BLUE)
    i = 0
    while (i < triIndices.size - 2) {
      val i0 = triIndices(i).toInt * 2
      val i1 = triIndices(i + 1).toInt * 2
      val i2 = triIndices(i + 2).toInt * 2
      if (i0 >= 0 && i1 >= 0 && i2 >= 0 &&
          i0 + 1 < geoPoints.length && i1 + 1 < geoPoints.length && i2 + 1 < geoPoints.length) {
        shapeRenderer.line(geoPoints(i0) + ox, geoPoints(i0 + 1) + oy, geoPoints(i1) + ox, geoPoints(i1 + 1) + oy)
        shapeRenderer.line(geoPoints(i1) + ox, geoPoints(i1 + 1) + oy, geoPoints(i2) + ox, geoPoints(i2 + 1) + oy)
        shapeRenderer.line(geoPoints(i2) + ox, geoPoints(i2 + 1) + oy, geoPoints(i0) + ox, geoPoints(i0 + 1) + oy)
      }
      i += 3
    }

    // EarClipping on the convex hull polygon (red, offset slightly)
    if (hullArr.length >= 6) {
      val earIndices = earClipper.computeTriangles(hullArr)
      shapeRenderer.setColor(Color.RED)
      val earOx = ox + 2f
      val earOy = oy + 2f
      i = 0
      while (i < earIndices.size - 2) {
        val i0 = earIndices(i).toInt * 2
        val i1 = earIndices(i + 1).toInt * 2
        val i2 = earIndices(i + 2).toInt * 2
        if (i0 + 1 < hullArr.length && i1 + 1 < hullArr.length && i2 + 1 < hullArr.length) {
          shapeRenderer.line(hullArr(i0) + earOx, hullArr(i0 + 1) + earOy, hullArr(i1) + earOx, hullArr(i1 + 1) + earOy)
          shapeRenderer.line(hullArr(i1) + earOx, hullArr(i1 + 1) + earOy, hullArr(i2) + earOx, hullArr(i2 + 1) + earOy)
          shapeRenderer.line(hullArr(i2) + earOx, hullArr(i2 + 1) + earOy, hullArr(i0) + earOx, hullArr(i0 + 1) + earOy)
        }
        i += 3
      }
    }

    shapeRenderer.end()

    // Draw the geo points as filled white dots
    shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
    shapeRenderer.setColor(Color.WHITE)
    i = 0
    while (i < geoPoints.length - 1) {
      shapeRenderer.circle(geoPoints(i) + ox, geoPoints(i + 1) + oy, 3f)
      i += 2
    }

    // Display polygon area as a small bar
    if (hullArr.length >= 6) {
      val area = scala.math.abs(GeometryUtils.polygonArea(hullArr, 0, hullArr.length))
      val barW = MathUtils.clamp(area / 200f, 5f, 350f)
      shapeRenderer.setColor(Color(0.3f, 0.8f, 0.3f, 1f))
      shapeRenderer.rectangle(ox, oy - 30f, barW, 8f)
    }
    shapeRenderer.end()
  }

  private def drawInterpolationGallery()(using Sge): Unit = {
    val graphW   = 80f
    val graphH   = 60f
    val spacing  = 10f
    val startX   = 15f
    val startY   = 10f
    val samples  = 50

    interpCurves.indices.foreach { idx =>
      val (_, interp, color) = interpCurves(idx)
      val gx = startX + idx * (graphW + spacing)
      val gy = startY

      // Graph background outline
      shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
      shapeRenderer.setColor(Color(0.3f, 0.3f, 0.3f, 1f))
      shapeRenderer.rectangle(gx, gy, graphW, graphH)

      // Curve
      shapeRenderer.setColor(color)
      var i = 0
      while (i < samples) {
        val t0 = i.toFloat / samples
        val t1 = (i + 1).toFloat / samples
        val v0 = interp.apply(t0)
        val v1 = interp.apply(t1)
        shapeRenderer.line(
          gx + t0 * graphW, gy + v0 * graphH,
          gx + t1 * graphW, gy + v1 * graphH
        )
        i += 1
      }
      shapeRenderer.end()

      // Color identifier dot
      shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
      shapeRenderer.setColor(color)
      shapeRenderer.circle(gx + graphW / 2f, gy + graphH + 8f, 3f)
      shapeRenderer.end()
    }
  }

  private def drawBresenhamLine()(using Sge): Unit = {
    // Draw a pixel-perfect diagonal line in the upper-right area
    val points = bresenham.line(700, 550, 780, 590)
    shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
    shapeRenderer.setColor(Color(1f, 0.6f, 0.2f, 1f))
    points.foreach { p =>
      shapeRenderer.rectangle(p.x.toFloat, p.y.toFloat, 2f, 2f)
    }
    shapeRenderer.end()
  }

  // --- Helpers ---

  private def randomizeGeoPoints(): Unit = {
    var i = 0
    while (i < geoPoints.length - 1) {
      geoPoints(i) = MathUtils.random(20f, 320f)
      geoPoints(i + 1) = MathUtils.random(20f, 300f)
      i += 2
    }
  }
}
