/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g2d/RepeatablePolygonSprite.java
 * Original authors: Avetis Zakharyan
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: Nullable[TextureRegion] for region; Nullable[Array[Float]] for nullable parts; DynamicArray instead of libGDX Array
 *   Idiom: boundary/break, Nullable, split packages
 *   Fixes: setPolygon now uses Intersector.intersectPolygons instead of simplified vertex containment
 *   Notes: Java density field omitted — dead code (private, method parameter shadows it, this.density never accessed)
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g2d

import sge.math.*
import sge.utils.{ DynamicArray, Nullable }

/** Renders polygon filled with a repeating TextureRegion with specified density Without causing an additional flush or render call
  *
  * @author
  *   Avetis Zakharyan
  */
class RepeatablePolygonSprite {

  private var region: Nullable[TextureRegion] = Nullable.empty

  private var dirty: Boolean = true

  private val parts: DynamicArray[Nullable[Array[Float]]] = DynamicArray[Nullable[Array[Float]]]()

  private val vertices: DynamicArray[Array[Float]] = DynamicArray[Array[Float]]()
  private val indices:  DynamicArray[Array[Short]] = DynamicArray[Array[Short]]()

  private var cols:       Int   = 0
  private var rows:       Int   = 0
  private var gridWidth:  Float = 0.0f
  private var gridHeight: Float = 0.0f

  var x:              Float   = 0.0f
  var y:              Float   = 0.0f
  private var color:  Color   = Color.WHITE
  private val offset: Vector2 = Vector2()

  /** Sets polygon with repeating texture region, the size of repeating grid is equal to region size
    * @param region
    *   \- region to repeat
    * @param vertices
    *   \- cw vertices of polygon
    */
  def setPolygon(region: TextureRegion, vertices: Array[Float]): Unit =
    setPolygon(region, vertices, -1)

  /** Sets polygon with repeating texture region, the size of repeating grid is equal to region size
    * @param region
    *   \- region to repeat
    * @param vertices
    *   \- cw vertices of polygon
    * @param density
    *   \- number of regions per polygon width bound
    */
  def setPolygon(region: TextureRegion, vertices: Array[Float], density: Float): Unit = {

    this.region = Nullable(region)

    val offsetVertices = offset(vertices)

    val polygon          = Polygon(offsetVertices)
    val tmpPoly          = Polygon()
    val intersectionPoly = Polygon()
    val triangulator     = EarClippingTriangulator()

    var idx: Int = 0

    val boundRect = polygon.boundingRectangle

    val finalDensity = if (density == -1) boundRect.width / region.regionWidth else density

    val regionAspectRatio = region.regionHeight.toFloat / region.regionWidth.toFloat
    cols = Math.ceil(finalDensity).toInt
    gridWidth = boundRect.width / finalDensity
    gridHeight = regionAspectRatio * gridWidth
    rows = Math.ceil(boundRect.height / gridHeight).toInt

    for (col <- 0 until cols)
      for (row <- 0 until rows) {
        var verts = Array.ofDim[Float](8)
        idx = 0
        verts(idx) = col * gridWidth
        idx += 1
        verts(idx) = row * gridHeight
        idx += 1
        verts(idx) = col * gridWidth
        idx += 1
        verts(idx) = (row + 1) * gridHeight
        idx += 1
        verts(idx) = (col + 1) * gridWidth
        idx += 1
        verts(idx) = (row + 1) * gridHeight
        idx += 1
        verts(idx) = (col + 1) * gridWidth
        idx += 1
        verts(idx) = row * gridHeight
        tmpPoly.setVertices(verts)

        Intersector.intersectPolygons(polygon, tmpPoly, Nullable(intersectionPoly))
        verts = intersectionPoly.vertices
        if (verts.length > 0) {
          parts.add(Nullable(snapToGrid(verts)))
          val triangleIndices = triangulator.computeTriangles(verts)
          indices.add(triangleIndices.toArray)
        } else {
          // adding null for key consistency, needed to get col/row from key
          // the other alternative is to make parts - IntMap<FloatArray>
          parts.add(Nullable.empty)
        }
      }

    buildVertices()
  }

  /** This is a garbage, due to Intersector returning values slightly different then the grid values Snapping exactly to grid is important, so that during bulidVertices method, it can be figured out
    * if points is on the wall of it's own grid box or not, to set u/v properly. Any other implementations are welcome
    */
  private def snapToGrid(vertices: Array[Float]): Array[Float] = {
    var i = 0
    while (i < vertices.length) {
      val numX = (vertices(i) / gridWidth) % 1
      val numY = (vertices(i + 1) / gridHeight) % 1
      if (numX > 0.99f || numX < 0.01f) {
        vertices(i) = gridWidth * Math.round(vertices(i) / gridWidth)
      }
      if (numY > 0.99f || numY < 0.01f) {
        vertices(i + 1) = gridHeight * Math.round(vertices(i + 1) / gridHeight)
      }
      i += 2
    }

    vertices
  }

  /** Offsets polygon to 0 coordinate for ease of calculations, later offset is put back on final render
    * @param vertices
    * @return
    *   offsetted vertices
    */
  private def offset(vertices: Array[Float]): Array[Float] = {
    offset.set(vertices(0), vertices(1))
    var i = 0
    while (i < vertices.length - 1) {
      if (offset.x > vertices(i)) {
        offset.x = vertices(i)
      }
      if (offset.y > vertices(i + 1)) {
        offset.y = vertices(i + 1)
      }
      i += 2
    }
    i = 0
    while (i < vertices.length) {
      vertices(i) -= offset.x
      vertices(i + 1) -= offset.y
      i += 2
    }

    vertices
  }

  /** Builds final vertices with vertex attributes like coordinates, color and region u/v */
  private def buildVertices(): Unit = {
    vertices.clear()
    var i = 0
    while (i < parts.size) {
      val vertsNullable = parts(i)
      vertsNullable.fold {
        // Skip null parts
      } { verts =>
        val fullVerts = Array.ofDim[Float](5 * verts.length / 2)
        var idx       = 0

        val col = i / rows
        val row = i % rows

        var j = 0
        while (j < verts.length) {
          fullVerts(idx) = verts(j) + offset.x + x
          idx += 1
          fullVerts(idx) = verts(j + 1) + offset.y + y
          idx += 1

          fullVerts(idx) = color.toFloatBits()
          idx += 1

          var u = (verts(j) % gridWidth) / gridWidth
          var v = (verts(j + 1) % gridHeight) / gridHeight
          if (verts(j) == col * gridWidth) u = 0f
          if (verts(j) == (col + 1) * gridWidth) u = 1f
          if (verts(j + 1) == row * gridHeight) v = 0f
          if (verts(j + 1) == (row + 1) * gridHeight) v = 1f
          val reg = region.getOrElse(throw new IllegalStateException("region not set"))
          u = reg.u + (reg.u2 - reg.u) * u
          v = reg.v + (reg.v2 - reg.v) * v
          fullVerts(idx) = u
          idx += 1
          fullVerts(idx) = v
          idx += 1
          j += 2
        }
        vertices.add(fullVerts)
      }
      i += 1
    }
    dirty = false
  }

  def draw(batch: PolygonSpriteBatch): Unit = {
    if (dirty) {
      buildVertices()
    }
    var i = 0
    while (i < vertices.size) {
      val reg = region.getOrElse(throw new IllegalStateException("region not set"))
      batch.draw(reg.texture, vertices(i), 0, vertices(i).length, indices(i), 0, indices(i).length)
      i += 1
    }
  }

  /** @param color - Tint color to be applied to entire polygon */
  def setColor(color: Color): Unit = {
    this.color = color
    dirty = true
  }

  def setPosition(x: Float, y: Float): Unit = {
    this.x = x
    this.y = y
    dirty = true
  }
}
