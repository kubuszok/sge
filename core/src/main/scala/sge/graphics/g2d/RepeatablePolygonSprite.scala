package sge
package graphics
package g2d

import sge.math.*
import sge.utils.Nullable
import scala.collection.mutable.ArrayBuffer

/** Renders polygon filled with a repeating TextureRegion with specified density Without causing an additional flush or render call
  *
  * @author
  *   Avetis Zakharyan
  */
class RepeatablePolygonSprite {

  private var region:  Nullable[TextureRegion] = Nullable.empty
  private var density: Float                   = 0.0f

  private var dirty: Boolean = true

  private val parts: ArrayBuffer[Nullable[Array[Float]]] = ArrayBuffer.empty[Nullable[Array[Float]]]

  private val vertices: ArrayBuffer[Array[Float]] = ArrayBuffer.empty[Array[Float]]
  private val indices:  ArrayBuffer[Array[Short]] = ArrayBuffer.empty[Array[Short]]

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

    val boundRect = polygon.getBoundingRectangle

    val finalDensity = if (density == -1) boundRect.getWidth() / region.getRegionWidth() else density

    val regionAspectRatio = region.getRegionHeight().toFloat / region.getRegionWidth().toFloat
    cols = Math.ceil(finalDensity).toInt
    gridWidth = boundRect.getWidth() / finalDensity
    gridHeight = regionAspectRatio * gridWidth
    rows = Math.ceil(boundRect.getHeight() / gridHeight).toInt

    for (col <- 0 until cols)
      for (row <- 0 until rows) {
        val verts = Array.ofDim[Float](8)
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

        // Simple intersection check - if polygon contains any vertex of tmpPoly, consider it intersecting
        val tmpVertices     = tmpPoly.getTransformedVertices
        var hasIntersection = false
        var i               = 0
        while (i < tmpVertices.length && !hasIntersection) {
          if (polygon.contains(tmpVertices(i), tmpVertices(i + 1))) {
            hasIntersection = true
          }
          i += 2
        }

        if (hasIntersection) {
          // For simplicity, use the tmpPoly vertices as intersection result
          val intersectionVerts = tmpVertices.clone()
          parts += Nullable(snapToGrid(intersectionVerts))
          val triangleIndices = triangulator.computeTriangles(intersectionVerts)
          indices += triangleIndices.toArray
        } else {
          // adding null for key consistency, needed to get col/row from key
          // the other alternative is to make parts - IntMap<FloatArray>
          parts += Nullable.empty
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
    for (i <- parts.indices) {
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
          u = region.orNull.getU() + (region.orNull.getU2() - region.orNull.getU()) * u
          v = region.orNull.getV() + (region.orNull.getV2() - region.orNull.getV()) * v
          fullVerts(idx) = u
          idx += 1
          fullVerts(idx) = v
          idx += 1
          j += 2
        }
        vertices += fullVerts
      }
    }
    dirty = false
  }

  def draw(batch: PolygonSpriteBatch): Unit = {
    if (dirty) {
      buildVertices()
    }
    for (i <- vertices.indices)
      batch.draw(region.orNull.getTexture(), vertices(i), 0, vertices(i).length, indices(i), 0, indices(i).length)
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
