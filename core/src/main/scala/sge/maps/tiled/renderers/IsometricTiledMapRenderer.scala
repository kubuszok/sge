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
  private val screenPos:       Vector3 = new Vector3()

  private val topRight:    Vector2 = new Vector2()
  private val bottomLeft:  Vector2 = new Vector2()
  private val topLeft:     Vector2 = new Vector2()
  private val bottomRight: Vector2 = new Vector2()

  init()

  def this(map: TiledMap)(using Sge) = this(map, 1.0f, new SpriteBatch(), true)
  def this(map: TiledMap, batch:     Batch)(using Sge) = this(map, 1.0f, batch, false)
  def this(map: TiledMap, unitScale: Float)(using Sge) = this(map, unitScale, new SpriteBatch(), true)

  private def init(): Unit = {
    // create the isometric transform
    isoTransform = new Matrix4()
    isoTransform.idt()

    // isoTransform.translate(0, 32, 0);
    isoTransform.scale((Math.sqrt(2.0) / 2.0).toFloat, (Math.sqrt(2.0) / 4.0).toFloat, 1.0f)
    isoTransform.rotate(0.0f, 0.0f, 1.0f, -45)

    // ... and the inverse matrix
    invIsotransform = new Matrix4(isoTransform)
    invIsotransform.inv()
  }

  private def translateScreenToIso(vec: Vector2): Vector3 = {
    screenPos.set(vec.x, vec.y, 0)
    screenPos.mul(invIsotransform)
    screenPos
  }

  override def renderTileLayer(layer: TiledMapTileLayer): Unit = {
    val batchColor = batch.getColor()
    val color      = getTileLayerColor(layer, batchColor)

    val tileWidth  = layer.getTileWidth * unitScale
    val tileHeight = layer.getTileHeight * unitScale

    val layerOffsetX = layer.getRenderOffsetX * unitScale - viewBounds.x * (layer.getParallaxX - 1)
    // offset in tiled is y down, so we flip it
    val layerOffsetY = -layer.getRenderOffsetY * unitScale - viewBounds.y * (layer.getParallaxY - 1)

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
          val tile = c.getTile
          tile.foreach { t =>
            val flipX     = c.getFlipHorizontally
            val flipY     = c.getFlipVertically
            val rotations = c.getRotation

            val region = t.getTextureRegion

            val x1 = x + t.getOffsetX * unitScale + layerOffsetX
            val y1 = y + t.getOffsetY * unitScale + layerOffsetY
            val x2 = x1 + region.getRegionWidth() * unitScale
            val y2 = y1 + region.getRegionHeight() * unitScale

            vertices(Batch.X1) = x1
            vertices(Batch.Y1) = y1
            vertices(Batch.C1) = color
            vertices(Batch.U1) = region.getU()
            vertices(Batch.V1) = region.getV2()

            vertices(Batch.X2) = x1
            vertices(Batch.Y2) = y2
            vertices(Batch.C2) = color
            vertices(Batch.U2) = region.getU()
            vertices(Batch.V2) = region.getV()

            vertices(Batch.X3) = x2
            vertices(Batch.Y3) = y2
            vertices(Batch.C3) = color
            vertices(Batch.U3) = region.getU2()
            vertices(Batch.V3) = region.getV()

            vertices(Batch.X4) = x2
            vertices(Batch.Y4) = y1
            vertices(Batch.C4) = color
            vertices(Batch.U4) = region.getU2()
            vertices(Batch.V4) = region.getV2()

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
            batch.draw(region.getTexture(), vertices, 0, BatchTiledMapRenderer.NUM_VERTICES)
          }
        }
        col += 1
      }
      row -= 1
    }
  }

  override def renderImageLayer(layer: TiledMapImageLayer): Unit = {
    val batchColor = batch.getColor()
    val color      = getImageLayerColor(layer, batchColor)
    val vertices   = this.vertices
    val region     = layer.getTextureRegion

    if (Nullable(region).isEmpty) {
      ()
    } else {

      /** Because of the way libGDX handles the isometric coordinates. The leftmost tile of the map begins rendering at world position 0,0, while in Tiled the y position is actually totalHeight/2 ex.
        * Map 800px in height, leftmost tile edge starts rendering at 0,400 in tiled To compensate for that we need to subtract half the map's height in pixels then add half of the tile's height in
        * order to position it properly in order to get a 1 to 1 rendering as to where the imagelayer renders in tiled.
        */
      val tileHeight      = getMap.getProperties.get("tileheight", classOf[Integer]).intValue()
      val mapHeight       = getMap.getProperties.get("height", classOf[Integer]).intValue()
      val mapHeightPixels = (mapHeight * tileHeight) * unitScale
      val halfTileHeight  = (tileHeight * 0.5f) * unitScale

      val x  = layer.getX
      val y  = layer.getY
      val x1 = x * unitScale - viewBounds.x * (layer.getParallaxX - 1)
      val y1 = y * unitScale - viewBounds.y * (layer.getParallaxY - 1) - (mapHeightPixels * 0.5f) + halfTileHeight
      val x2 = x1 + region.getRegionWidth() * unitScale
      val y2 = y1 + region.getRegionHeight() * unitScale

      imageBounds.set(x1, y1, x2 - x1, y2 - y1)

      if (!layer.isRepeatX && !layer.isRepeatY) {
        if (viewBounds.contains(imageBounds) || viewBounds.overlaps(imageBounds)) {
          vertices(Batch.X1) = x1; vertices(Batch.Y1) = y1; vertices(Batch.C1) = color
          vertices(Batch.U1) = region.getU(); vertices(Batch.V1) = region.getV2()
          vertices(Batch.X2) = x1; vertices(Batch.Y2) = y2; vertices(Batch.C2) = color
          vertices(Batch.U2) = region.getU(); vertices(Batch.V2) = region.getV()
          vertices(Batch.X3) = x2; vertices(Batch.Y3) = y2; vertices(Batch.C3) = color
          vertices(Batch.U3) = region.getU2(); vertices(Batch.V3) = region.getV()
          vertices(Batch.X4) = x2; vertices(Batch.Y4) = y1; vertices(Batch.C4) = color
          vertices(Batch.U4) = region.getU2(); vertices(Batch.V4) = region.getV2()
          batch.draw(region.getTexture(), vertices, 0, BatchTiledMapRenderer.NUM_VERTICES)
        }
      } else {
        renderRepeatedImage(layer, region, vertices, color, x1, y1, x2, y2)
      }
    }
  }

  private def renderRepeatedImage(layer: TiledMapImageLayer, region: TextureRegion, vertices: Array[Float], color: Float, x1: Float, y1: Float, x2: Float, y2: Float): Unit = {
    val repeatX = if (layer.isRepeatX) Math.ceil((viewBounds.width / imageBounds.width) + 4).toInt else 0
    val repeatY = if (layer.isRepeatY) Math.ceil((viewBounds.height / imageBounds.height) + 4).toInt else 0
    var startX  = viewBounds.x; startX = startX - (startX % imageBounds.width)
    var startY  = viewBounds.y; startY = startY - (startY % imageBounds.height)
    var i       = 0
    while (i <= repeatX) {
      var j = 0
      while (j <= repeatY) {
        var rx1 = x1; var ry1 = y1; var rx2 = x2; var ry2 = y2
        if (layer.isRepeatX) { rx1 = startX + ((i - 2) * imageBounds.width) + (x1 % imageBounds.width); rx2 = rx1 + imageBounds.width }
        if (layer.isRepeatY) { ry1 = startY + ((j - 2) * imageBounds.height) + (y1 % imageBounds.height); ry2 = ry1 + imageBounds.height }
        repeatedImageBounds.set(rx1, ry1, rx2 - rx1, ry2 - ry1)
        if (viewBounds.contains(repeatedImageBounds) || viewBounds.overlaps(repeatedImageBounds)) {
          vertices(Batch.X1) = rx1; vertices(Batch.Y1) = ry1; vertices(Batch.C1) = color
          vertices(Batch.U1) = region.getU(); vertices(Batch.V1) = region.getV2()
          vertices(Batch.X2) = rx1; vertices(Batch.Y2) = ry2; vertices(Batch.C2) = color
          vertices(Batch.U2) = region.getU(); vertices(Batch.V2) = region.getV()
          vertices(Batch.X3) = rx2; vertices(Batch.Y3) = ry2; vertices(Batch.C3) = color
          vertices(Batch.U3) = region.getU2(); vertices(Batch.V3) = region.getV()
          vertices(Batch.X4) = rx2; vertices(Batch.Y4) = ry1; vertices(Batch.C4) = color
          vertices(Batch.U4) = region.getU2(); vertices(Batch.V4) = region.getV2()
          batch.draw(region.getTexture(), vertices, 0, BatchTiledMapRenderer.NUM_VERTICES)
        }
        j += 1
      }
      i += 1
    }
  }
}
