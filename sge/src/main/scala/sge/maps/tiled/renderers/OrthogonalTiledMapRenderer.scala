/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/tiled/renderers/OrthogonalTiledMapRenderer.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - Audited 2026-03-03 against libGDX source: renderTileLayer matches 1:1
 * - Java null checks on cell/tile replaced with Nullable.foreach chaining
 * - Java continue replaced with if/else in inner loop
 * - Java switch/break on rotations replaced with match/case
 * - Does not override renderImageLayer (inherits from BatchTiledMapRenderer, matching Java)
 * - Constructors: 4 Java ctors mapped to primary + 3 auxiliary
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 176
 * Covenant-baseline-methods: OrthogonalTiledMapRenderer,batchColor,col1,col2,color,layerHeight,layerOffsetX,layerOffsetY,layerTileHeight,layerTileWidth,layerWidth,renderTileLayer,row,row1,row2,this,vertices,xStart,y
 * Covenant-source-reference: com/badlogic/gdx/maps/tiled/renderers/OrthogonalTiledMapRenderer.java
 * Covenant-verified: 2026-04-19
 */
package sge
package maps
package tiled
package renderers

import sge.Sge
import sge.graphics.g2d.{ Batch, SpriteBatch }

class OrthogonalTiledMapRenderer(map: TiledMap, unitScale: Float, batch: Batch, ownsBatch: Boolean)(using Sge) extends BatchTiledMapRenderer(map, unitScale, batch, ownsBatch) {

  def this(map: TiledMap)(using Sge) = this(map, 1.0f, SpriteBatch(), true)
  def this(map: TiledMap, batch:     Batch)(using Sge) = this(map, 1.0f, batch, false)
  def this(map: TiledMap, unitScale: Float)(using Sge) = this(map, unitScale, SpriteBatch(), true)

  override def renderTileLayer(layer: TiledMapTileLayer): Unit = {
    val batchColor = batch.color
    val color      = getTileLayerColor(layer, batchColor)

    val layerWidth  = layer.width
    val layerHeight = layer.height

    val layerTileWidth  = layer.tileWidth * unitScale
    val layerTileHeight = layer.tileHeight * unitScale

    val layerOffsetX = layer.renderOffsetX * unitScale - viewBounds.x * (layer.parallaxX - 1)
    // offset in tiled is y down, so we flip it
    val layerOffsetY = -layer.renderOffsetY * unitScale - viewBounds.y * (layer.parallaxY - 1)

    val col1 = Math.max(0, ((viewBounds.x - layerOffsetX) / layerTileWidth).toInt)
    val col2 = Math.min(layerWidth, ((viewBounds.x + viewBounds.width + layerTileWidth - layerOffsetX) / layerTileWidth).toInt)

    val row1 = Math.max(0, ((viewBounds.y - layerOffsetY) / layerTileHeight).toInt)
    val row2 = Math.min(layerHeight, ((viewBounds.y + viewBounds.height + layerTileHeight - layerOffsetY) / layerTileHeight).toInt)

    var y        = row2 * layerTileHeight + layerOffsetY
    val xStart   = col1 * layerTileWidth + layerOffsetX
    val vertices = this.vertices

    var row = row2
    while (row >= row1) {
      var x   = xStart
      var col = col1
      while (col < col2) {
        val cell = layer.getCell(col, row)
        if (cell.isEmpty) {
          x += layerTileWidth
        } else {
          cell.foreach { c =>
            val tile = c.tile
            tile.foreach { t =>
              val flipX     = c.flipHorizontally
              val flipY     = c.flipVertically
              val rotations = c.rotation

              val region = t.textureRegion

              val x1 = x + t.offsetX * unitScale
              val y1 = y + t.offsetY * unitScale
              val x2 = x1 + region.regionWidth * unitScale
              val y2 = y1 + region.regionHeight * unitScale

              val u1 = region.u
              val v1 = region.v2
              val u2 = region.u2
              val v2 = region.v

              vertices(Batch.X1) = x1
              vertices(Batch.Y1) = y1
              vertices(Batch.C1) = color
              vertices(Batch.U1) = u1
              vertices(Batch.V1) = v1

              vertices(Batch.X2) = x1
              vertices(Batch.Y2) = y2
              vertices(Batch.C2) = color
              vertices(Batch.U2) = u1
              vertices(Batch.V2) = v2

              vertices(Batch.X3) = x2
              vertices(Batch.Y3) = y2
              vertices(Batch.C3) = color
              vertices(Batch.U3) = u2
              vertices(Batch.V3) = v2

              vertices(Batch.X4) = x2
              vertices(Batch.Y4) = y1
              vertices(Batch.C4) = color
              vertices(Batch.U4) = u2
              vertices(Batch.V4) = v1

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
          x += layerTileWidth
        }
        col += 1
      }
      y -= layerTileHeight
      row -= 1
    }
  }
}
