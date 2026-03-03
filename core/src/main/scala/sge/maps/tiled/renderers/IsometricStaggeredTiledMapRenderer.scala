/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/tiled/renderers/IsometricStaggeredTiledMapRenderer.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - Audited 2026-03-03 against libGDX source: all methods match 1:1
 * - Java null checks on cell/tile replaced with Nullable.foreach chaining
 * - Java null==region early return replaced with Nullable isEmpty/else
 * - Java switch/break on rotations replaced with match/case
 * - renderImageLayer override: halfTileWidth X-offset logic faithfully ported
 * - Constructors: 4 Java ctors mapped to primary + 3 auxiliary
 */
package sge
package maps
package tiled
package renderers

import sge.Sge
import sge.graphics.g2d.{ Batch, SpriteBatch }
import sge.utils.Nullable

class IsometricStaggeredTiledMapRenderer(map: TiledMap, unitScale: Float, batch: Batch, ownsBatch: Boolean)(using Sge) extends BatchTiledMapRenderer(map, unitScale, batch, ownsBatch) {

  def this(map: TiledMap)(using Sge) = this(map, 1.0f, new SpriteBatch(), true)
  def this(map: TiledMap, batch:     Batch)(using Sge) = this(map, 1.0f, batch, false)
  def this(map: TiledMap, unitScale: Float)(using Sge) = this(map, unitScale, new SpriteBatch(), true)

  override def renderTileLayer(layer: TiledMapTileLayer): Unit = {
    val batchColor = batch.getColor()
    val color      = getTileLayerColor(layer, batchColor)

    val layerWidth  = layer.getWidth
    val layerHeight = layer.getHeight

    val layerOffsetX = layer.getRenderOffsetX * unitScale - viewBounds.x * (layer.getParallaxX - 1)
    // offset in tiled is y down, so we flip it
    val layerOffsetY = -layer.getRenderOffsetY * unitScale - viewBounds.y * (layer.getParallaxY - 1)

    val layerTileWidth  = layer.getTileWidth * unitScale
    val layerTileHeight = layer.getTileHeight * unitScale

    val layerTileWidth50  = layerTileWidth * 0.50f
    val layerTileHeight50 = layerTileHeight * 0.50f

    val minX = Math.max(0, ((viewBounds.x - layerTileWidth50 - layerOffsetX) / layerTileWidth).toInt)
    val maxX = Math.min(layerWidth, ((viewBounds.x + viewBounds.width + layerTileWidth + layerTileWidth50 - layerOffsetX) / layerTileWidth).toInt)

    val minY = Math.max(0, ((viewBounds.y - layerTileHeight - layerOffsetY) / layerTileHeight).toInt)
    val maxY = Math.min(layerHeight, ((viewBounds.y + viewBounds.height + layerTileHeight - layerOffsetY) / layerTileHeight50).toInt)

    var y = maxY - 1
    while (y >= minY) {
      val offsetX = if (y % 2 == 1) layerTileWidth50 else 0f
      var x       = maxX - 1
      while (x >= minX) {
        val cell = layer.getCell(x, y)
        cell.foreach { c =>
          val tile = c.getTile
          tile.foreach { t =>
            val flipX     = c.getFlipHorizontally
            val flipY     = c.getFlipVertically
            val rotations = c.getRotation
            val region    = t.getTextureRegion

            val x1 = x * layerTileWidth - offsetX + t.getOffsetX * unitScale + layerOffsetX
            val y1 = y * layerTileHeight50 + t.getOffsetY * unitScale + layerOffsetY
            val x2 = x1 + region.getRegionWidth() * unitScale
            val y2 = y1 + region.getRegionHeight() * unitScale

            vertices(Batch.X1) = x1; vertices(Batch.Y1) = y1; vertices(Batch.C1) = color
            vertices(Batch.U1) = region.getU(); vertices(Batch.V1) = region.getV2()
            vertices(Batch.X2) = x1; vertices(Batch.Y2) = y2; vertices(Batch.C2) = color
            vertices(Batch.U2) = region.getU(); vertices(Batch.V2) = region.getV()
            vertices(Batch.X3) = x2; vertices(Batch.Y3) = y2; vertices(Batch.C3) = color
            vertices(Batch.U3) = region.getU2(); vertices(Batch.V3) = region.getV()
            vertices(Batch.X4) = x2; vertices(Batch.Y4) = y1; vertices(Batch.C4) = color
            vertices(Batch.U4) = region.getU2(); vertices(Batch.V4) = region.getV2()

            if (flipX) {
              var temp = vertices(Batch.U1); vertices(Batch.U1) = vertices(Batch.U3); vertices(Batch.U3) = temp
              temp = vertices(Batch.U2); vertices(Batch.U2) = vertices(Batch.U4); vertices(Batch.U4) = temp
            }
            if (flipY) {
              var temp = vertices(Batch.V1); vertices(Batch.V1) = vertices(Batch.V3); vertices(Batch.V3) = temp
              temp = vertices(Batch.V2); vertices(Batch.V2) = vertices(Batch.V4); vertices(Batch.V4) = temp
            }
            if (rotations != 0) {
              rotations match {
                case TiledMapTileLayer.Cell.ROTATE_90 =>
                  val tempV = vertices(Batch.V1); vertices(Batch.V1) = vertices(Batch.V2); vertices(Batch.V2) = vertices(Batch.V3); vertices(Batch.V3) = vertices(Batch.V4); vertices(Batch.V4) = tempV
                  val tempU = vertices(Batch.U1); vertices(Batch.U1) = vertices(Batch.U2); vertices(Batch.U2) = vertices(Batch.U3); vertices(Batch.U3) = vertices(Batch.U4); vertices(Batch.U4) = tempU
                case TiledMapTileLayer.Cell.ROTATE_180 =>
                  var tempU = vertices(Batch.U1); vertices(Batch.U1) = vertices(Batch.U3); vertices(Batch.U3) = tempU
                  tempU = vertices(Batch.U2); vertices(Batch.U2) = vertices(Batch.U4); vertices(Batch.U4) = tempU
                  var tempV = vertices(Batch.V1); vertices(Batch.V1) = vertices(Batch.V3); vertices(Batch.V3) = tempV
                  tempV = vertices(Batch.V2); vertices(Batch.V2) = vertices(Batch.V4); vertices(Batch.V4) = tempV
                case TiledMapTileLayer.Cell.ROTATE_270 =>
                  val tempV = vertices(Batch.V1); vertices(Batch.V1) = vertices(Batch.V4); vertices(Batch.V4) = vertices(Batch.V3); vertices(Batch.V3) = vertices(Batch.V2); vertices(Batch.V2) = tempV
                  val tempU = vertices(Batch.U1); vertices(Batch.U1) = vertices(Batch.U4); vertices(Batch.U4) = vertices(Batch.U3); vertices(Batch.U3) = vertices(Batch.U2); vertices(Batch.U2) = tempU
                case _ => ()
              }
            }
            batch.draw(region.getTexture(), vertices, 0, BatchTiledMapRenderer.NUM_VERTICES)
          }
        }
        x -= 1
      }
      y -= 1
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

      /** Must offset imagelayer x position by half of tileWidth to match position */
      val tileWidth     = getMap.getProperties.get("tilewidth", classOf[Integer]).intValue()
      val halfTileWidth = (tileWidth * 0.5f) * unitScale

      val x  = layer.getX
      val y  = layer.getY
      val x1 = x * unitScale - viewBounds.x * (layer.getParallaxX - 1) - halfTileWidth
      val y1 = y * unitScale - viewBounds.y * (layer.getParallaxY - 1)
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
  }
}
