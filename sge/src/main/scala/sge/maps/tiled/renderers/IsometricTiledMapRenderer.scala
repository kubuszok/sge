/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/tiled/renderers/IsometricTiledMapRenderer.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - Audited 2026-03-03 against libGDX source: all methods match 1:1
 * - isoTransform/invIsotransform init via init() method called from class body (matches Java constructor pattern)
 * - Java null checks on cell/tile replaced with Nullable.foreach chaining
 * - Java null==region early return replaced with Nullable isEmpty/else
 * - Java switch/break on rotations replaced with match/case
 * - renderImageLayer override: isometric Y-offset logic faithfully ported
 * - Private renderRepeatedImage helper extracted (same logic as Java inline block)
 * - Constructors: 4 Java ctors mapped to primary + 3 auxiliary
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 283
 * Covenant-baseline-methods: IsometricTiledMapRenderer,batchColor,bottomLeft,bottomRight,col1,col2,color,halfTileHeight,halfTileWidth,i,init,invIsotransform,isoTransform,layerOffsetX,layerOffsetY,region,renderImageLayer,renderRepeatedImage,renderTileLayer,repeatX,repeatY,row,row1,row2,screenPos,startX,startY,this,tileHeight,tileWidth,topLeft,topRight,translateScreenToIso,vertices
 * Covenant-source-reference: com/badlogic/gdx/maps/tiled/renderers/IsometricTiledMapRenderer.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 0dc27743a43739f14f7220b4ec7dcc3ada3c8b61
 */
package sge
package maps
package tiled
package renderers

import sge.Sge
import sge.graphics.g2d.{ Batch, SpriteBatch, TextureRegion }
import sge.math.{ Matrix4, Vector2, Vector3 }
import sge.utils.Nullable

class IsometricTiledMapRenderer(map: TiledMap, unitScale: Float, batch: Batch, ownsBatch: Boolean)(using Sge) extends BatchTiledMapRenderer(map, unitScale, batch, ownsBatch) {

  private var isoTransform:    Matrix4 = scala.compiletime.uninitialized
  private var invIsotransform: Matrix4 = scala.compiletime.uninitialized
  private val screenPos:       Vector3 = Vector3()

  private val topRight:    Vector2 = Vector2()
  private val bottomLeft:  Vector2 = Vector2()
  private val topLeft:     Vector2 = Vector2()
  private val bottomRight: Vector2 = Vector2()

  init()

  def this(map: TiledMap)(using Sge) = this(map, 1.0f, SpriteBatch(), true)
  def this(map: TiledMap, batch:     Batch)(using Sge) = this(map, 1.0f, batch, false)
  def this(map: TiledMap, unitScale: Float)(using Sge) = this(map, unitScale, SpriteBatch(), true)

  private def init(): Unit = {
    // create the isometric transform
    isoTransform = Matrix4()
    isoTransform.idt()

    // isoTransform.translate(0, 32, 0);
    isoTransform.scale((Math.sqrt(2.0) / 2.0).toFloat, (Math.sqrt(2.0) / 4.0).toFloat, 1.0f)
    isoTransform.rotate(0.0f, 0.0f, 1.0f, -45)

    // ... and the inverse matrix
    invIsotransform = Matrix4(isoTransform)
    invIsotransform.inv()
  }

  private def translateScreenToIso(vec: Vector2): Vector3 = {
    screenPos.set(vec.x, vec.y, 0)
    screenPos.mul(invIsotransform)
    screenPos
  }

  override def renderTileLayer(layer: TiledMapTileLayer): Unit = {
    val batchColor = batch.color
    val color      = getTileLayerColor(layer, batchColor)

    val tileWidth  = layer.tileWidth * unitScale
    val tileHeight = layer.tileHeight * unitScale

    val layerOffsetX = layer.renderOffsetX * unitScale - viewBounds.x * (layer.parallaxX - 1)
    // offset in tiled is y down, so we flip it
    val layerOffsetY = -layer.renderOffsetY * unitScale - viewBounds.y * (layer.parallaxY - 1)

    val halfTileWidth  = tileWidth * 0.5f
    val halfTileHeight = tileHeight * 0.5f

    // setting up the screen points
    // COL1
    topRight.set(viewBounds.x + viewBounds.width - layerOffsetX, viewBounds.y - layerOffsetY)
    // COL2
    bottomLeft.set(viewBounds.x - layerOffsetX, viewBounds.y + viewBounds.height - layerOffsetY)
    // ROW1
    topLeft.set(viewBounds.x - layerOffsetX, viewBounds.y - layerOffsetY)
    // ROW2
    bottomRight.set(viewBounds.x + viewBounds.width - layerOffsetX, viewBounds.y + viewBounds.height - layerOffsetY)

    // transforming screen coordinates to iso coordinates
    val row1 = (translateScreenToIso(topLeft).y / tileWidth).toInt - 2
    val row2 = (translateScreenToIso(bottomRight).y / tileWidth).toInt + 2

    val col1 = (translateScreenToIso(bottomLeft).x / tileWidth).toInt - 2
    val col2 = (translateScreenToIso(topRight).x / tileWidth).toInt + 2

    var row = row2
    while (row >= row1) {
      var col = col1
      while (col <= col2) {
        val x = (col * halfTileWidth) + (row * halfTileWidth)
        val y = (row * halfTileHeight) - (col * halfTileHeight)

        val cell = layer.getCell(col, row)
        cell.foreach { c =>
          val tile = c.tile
          tile.foreach { t =>
            val flipX     = c.flipHorizontally
            val flipY     = c.flipVertically
            val rotations = c.rotation

            val region = t.textureRegion

            val x1 = x + t.offsetX * unitScale + layerOffsetX
            val y1 = y + t.offsetY * unitScale + layerOffsetY
            val x2 = x1 + region.regionWidth * unitScale
            val y2 = y1 + region.regionHeight * unitScale

            vertices(Batch.X1) = x1
            vertices(Batch.Y1) = y1
            vertices(Batch.C1) = color
            vertices(Batch.U1) = region.u
            vertices(Batch.V1) = region.v2

            vertices(Batch.X2) = x1
            vertices(Batch.Y2) = y2
            vertices(Batch.C2) = color
            vertices(Batch.U2) = region.u
            vertices(Batch.V2) = region.v

            vertices(Batch.X3) = x2
            vertices(Batch.Y3) = y2
            vertices(Batch.C3) = color
            vertices(Batch.U3) = region.u2
            vertices(Batch.V3) = region.v

            vertices(Batch.X4) = x2
            vertices(Batch.Y4) = y1
            vertices(Batch.C4) = color
            vertices(Batch.U4) = region.u2
            vertices(Batch.V4) = region.v2

            if (flipX) {
              var temp = vertices(Batch.U1)
              vertices(Batch.U1) = vertices(Batch.U3)
              vertices(Batch.U3) = temp
              temp = vertices(Batch.U2)
              vertices(Batch.U2) = vertices(Batch.U4)
              vertices(Batch.U4) = temp
            }
            if (flipY) {
              var temp = vertices(Batch.V1)
              vertices(Batch.V1) = vertices(Batch.V3)
              vertices(Batch.V3) = temp
              temp = vertices(Batch.V2)
              vertices(Batch.V2) = vertices(Batch.V4)
              vertices(Batch.V4) = temp
            }
            if (rotations != 0) {
              rotations match {
                case TiledMapTileLayer.Cell.ROTATE_90 =>
                  val tempV = vertices(Batch.V1)
                  vertices(Batch.V1) = vertices(Batch.V2)
                  vertices(Batch.V2) = vertices(Batch.V3)
                  vertices(Batch.V3) = vertices(Batch.V4)
                  vertices(Batch.V4) = tempV
                  val tempU = vertices(Batch.U1)
                  vertices(Batch.U1) = vertices(Batch.U2)
                  vertices(Batch.U2) = vertices(Batch.U3)
                  vertices(Batch.U3) = vertices(Batch.U4)
                  vertices(Batch.U4) = tempU
                case TiledMapTileLayer.Cell.ROTATE_180 =>
                  var tempU = vertices(Batch.U1)
                  vertices(Batch.U1) = vertices(Batch.U3)
                  vertices(Batch.U3) = tempU
                  tempU = vertices(Batch.U2)
                  vertices(Batch.U2) = vertices(Batch.U4)
                  vertices(Batch.U4) = tempU
                  var tempV = vertices(Batch.V1)
                  vertices(Batch.V1) = vertices(Batch.V3)
                  vertices(Batch.V3) = tempV
                  tempV = vertices(Batch.V2)
                  vertices(Batch.V2) = vertices(Batch.V4)
                  vertices(Batch.V4) = tempV
                case TiledMapTileLayer.Cell.ROTATE_270 =>
                  val tempV = vertices(Batch.V1)
                  vertices(Batch.V1) = vertices(Batch.V4)
                  vertices(Batch.V4) = vertices(Batch.V3)
                  vertices(Batch.V3) = vertices(Batch.V2)
                  vertices(Batch.V2) = tempV
                  val tempU = vertices(Batch.U1)
                  vertices(Batch.U1) = vertices(Batch.U4)
                  vertices(Batch.U4) = vertices(Batch.U3)
                  vertices(Batch.U3) = vertices(Batch.U2)
                  vertices(Batch.U2) = tempU
                case _ => ()
              }
            }
            batch.draw(region.texture, vertices, 0, BatchTiledMapRenderer.NUM_VERTICES)
          }
        }
        col += 1
      }
      row -= 1
    }
  }

  override def renderImageLayer(layer: TiledMapImageLayer): Unit = {
    val batchColor = batch.color
    val color      = getImageLayerColor(layer, batchColor)
    val vertices   = this.vertices
    val region     = layer.region

    if (Nullable(region).isEmpty) {
      ()
    } else {

      /** Because of the way libGDX handles the isometric coordinates. The leftmost tile of the map begins rendering at world position 0,0, while in Tiled the y position is actually totalHeight/2 ex.
        * Map 800px in height, leftmost tile edge starts rendering at 0,400 in tiled To compensate for that we need to subtract half the map's height in pixels then add half of the tile's height in
        * order to position it properly in order to get a 1 to 1 rendering as to where the imagelayer renders in tiled.
        */
      val tileHeight      = map.properties.getAs[Integer]("tileheight").get.intValue()
      val mapHeight       = map.properties.getAs[Integer]("height").get.intValue()
      val mapHeightPixels = (mapHeight * tileHeight) * unitScale
      val halfTileHeight  = (tileHeight * 0.5f) * unitScale

      val x  = layer.x
      val y  = layer.y
      val x1 = x * unitScale - viewBounds.x * (layer.parallaxX - 1)
      val y1 = y * unitScale - viewBounds.y * (layer.parallaxY - 1) - (mapHeightPixels * 0.5f) + halfTileHeight
      val x2 = x1 + region.regionWidth * unitScale
      val y2 = y1 + region.regionHeight * unitScale

      imageBounds.set(x1, y1, x2 - x1, y2 - y1)

      if (!layer.repeatX && !layer.repeatY) {
        if (viewBounds.contains(imageBounds) || viewBounds.overlaps(imageBounds)) {
          vertices(Batch.X1) = x1; vertices(Batch.Y1) = y1; vertices(Batch.C1) = color
          vertices(Batch.U1) = region.u; vertices(Batch.V1) = region.v2
          vertices(Batch.X2) = x1; vertices(Batch.Y2) = y2; vertices(Batch.C2) = color
          vertices(Batch.U2) = region.u; vertices(Batch.V2) = region.v
          vertices(Batch.X3) = x2; vertices(Batch.Y3) = y2; vertices(Batch.C3) = color
          vertices(Batch.U3) = region.u2; vertices(Batch.V3) = region.v
          vertices(Batch.X4) = x2; vertices(Batch.Y4) = y1; vertices(Batch.C4) = color
          vertices(Batch.U4) = region.u2; vertices(Batch.V4) = region.v2
          batch.draw(region.texture, vertices, 0, BatchTiledMapRenderer.NUM_VERTICES)
        }
      } else {
        renderRepeatedImage(layer, region, vertices, color, x1, y1, x2, y2)
      }
    }
  }

  private def renderRepeatedImage(layer: TiledMapImageLayer, region: TextureRegion, vertices: Array[Float], color: Float, x1: Float, y1: Float, x2: Float, y2: Float): Unit = {
    val repeatX = if (layer.repeatX) Math.ceil((viewBounds.width / imageBounds.width) + 4).toInt else 0
    val repeatY = if (layer.repeatY) Math.ceil((viewBounds.height / imageBounds.height) + 4).toInt else 0
    var startX  = viewBounds.x; startX = startX - (startX % imageBounds.width)
    var startY  = viewBounds.y; startY = startY - (startY % imageBounds.height)
    var i       = 0
    while (i <= repeatX) {
      var j = 0
      while (j <= repeatY) {
        var rx1 = x1; var ry1 = y1; var rx2 = x2; var ry2 = y2
        if (layer.repeatX) { rx1 = startX + ((i - 2) * imageBounds.width) + (x1 % imageBounds.width); rx2 = rx1 + imageBounds.width }
        if (layer.repeatY) { ry1 = startY + ((j - 2) * imageBounds.height) + (y1 % imageBounds.height); ry2 = ry1 + imageBounds.height }
        repeatedImageBounds.set(rx1, ry1, rx2 - rx1, ry2 - ry1)
        if (viewBounds.contains(repeatedImageBounds) || viewBounds.overlaps(repeatedImageBounds)) {
          vertices(Batch.X1) = rx1; vertices(Batch.Y1) = ry1; vertices(Batch.C1) = color
          vertices(Batch.U1) = region.u; vertices(Batch.V1) = region.v2
          vertices(Batch.X2) = rx1; vertices(Batch.Y2) = ry2; vertices(Batch.C2) = color
          vertices(Batch.U2) = region.u; vertices(Batch.V2) = region.v
          vertices(Batch.X3) = rx2; vertices(Batch.Y3) = ry2; vertices(Batch.C3) = color
          vertices(Batch.U3) = region.u2; vertices(Batch.V3) = region.v
          vertices(Batch.X4) = rx2; vertices(Batch.Y4) = ry1; vertices(Batch.C4) = color
          vertices(Batch.U4) = region.u2; vertices(Batch.V4) = region.v2
          batch.draw(region.texture, vertices, 0, BatchTiledMapRenderer.NUM_VERTICES)
        }
        j += 1
      }
      i += 1
    }
  }
}
