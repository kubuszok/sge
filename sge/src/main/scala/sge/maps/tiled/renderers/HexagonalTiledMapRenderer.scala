/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/tiled/renderers/HexagonalTiledMapRenderer.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - Audited 2026-03-03 against libGDX source: all methods match 1:1
 * - initHex: Java null checks on map properties replaced with Nullable.foreach/fold
 * - renderCell: Java null checks on cell/tile replaced with Nullable.foreach chaining
 * - renderCell uses !isInstanceOf[AnimatedTiledMapTile] guard (matches Java instanceof return)
 * - renderCell only handles rotations==2 (ROTATE_180), matching Java source
 * - Java switch/break on rotations NOT present in renderCell (only ==2 check), matching Java
 * - renderImageLayer: hex Y-offset logic faithfully ported
 * - Renames: isStaggerAxisX/setStaggerAxisX -> var staggerAxisX,
 *           isStaggerIndexEven/setStaggerIndexEven -> var staggerIndexEven,
 *           getHexSideLength/setHexSideLength -> var hexSideLength
 * - Constructors: 4 Java ctors mapped to primary + 3 auxiliary
 * - Audited: 2026-03-04
 */
package sge
package maps
package tiled
package renderers

import sge.Sge
import sge.graphics.g2d.{ Batch, SpriteBatch }
import sge.maps.tiled.tiles.AnimatedTiledMapTile
import sge.utils.Nullable

class HexagonalTiledMapRenderer(map: TiledMap, unitScale: Float, batch: Batch, ownsBatch: Boolean)(using Sge) extends BatchTiledMapRenderer(map, unitScale, batch, ownsBatch) {

  /** true for X-Axis, false for Y-Axis */
  var staggerAxisX: Boolean = true

  /** true for even StaggerIndex, false for odd */
  var staggerIndexEven: Boolean = false

  /** the parameter defining the shape of the hexagon from tiled. more specifically it represents the length of the sides that are parallel to the stagger axis. e.g. with respect to the stagger axis a
    * value of 0 results in a rhombus shape, while a value equal to the tile length/height represents a square shape and a value of 0.5 represents a regular hexagon if tile length equals tile height
    */
  var hexSideLength: Float = 0f

  initHex(map)

  def this(map: TiledMap)(using Sge) = this(map, 1.0f, SpriteBatch(), true)
  def this(map: TiledMap, unitScale: Float)(using Sge) = this(map, unitScale, SpriteBatch(), true)
  def this(map: TiledMap, batch:     Batch)(using Sge) = this(map, 1.0f, batch, false)

  private def initHex(map: TiledMap): Unit = {
    map.properties.getAs[String]("staggeraxis").foreach { axis =>
      staggerAxisX = axis == "x"
    }

    map.properties.getAs[String]("staggerindex").foreach { index =>
      staggerIndexEven = index == "even"
    }

    // due to y-axis being different we need to change stagger index in even map height situations as else it would render
    // differently.
    if (!staggerAxisX && map.properties.getAs[Integer]("height").get.intValue() % 2 == 0) {
      staggerIndexEven = !staggerIndexEven
    }

    map.properties
      .getAs[Integer]("hexsidelength")
      .fold {
        if (staggerAxisX) {
          map.properties
            .getAs[Integer]("tilewidth")
            .fold {
              if (map.layers.size > 0) {
                val tmtl = map.layers.get(0).asInstanceOf[TiledMapTileLayer]
                hexSideLength = 0.5f * tmtl.getTileWidth
              } else {
                hexSideLength = 0f
              }
            } { tw =>
              hexSideLength = 0.5f * tw.intValue()
            }
        } else {
          map.properties
            .getAs[Integer]("tileheight")
            .fold {
              if (map.layers.size > 0) {
                val tmtl = map.layers.get(0).asInstanceOf[TiledMapTileLayer]
                hexSideLength = 0.5f * tmtl.getTileHeight
              } else {
                hexSideLength = 0f
              }
            } { th =>
              hexSideLength = 0.5f * th.intValue()
            }
        }
      } { length =>
        hexSideLength = length.intValue().toFloat
      }
  }

  override def renderTileLayer(layer: TiledMapTileLayer): Unit = {
    val batchColor = batch.color
    val color      = getTileLayerColor(layer, batchColor)

    val layerWidth  = layer.getWidth
    val layerHeight = layer.getHeight

    val layerTileWidth  = layer.getTileWidth * unitScale
    val layerTileHeight = layer.getTileHeight * unitScale

    val layerOffsetX = layer.getRenderOffsetX * unitScale - viewBounds.x * (layer.parallaxX - 1)
    // offset in tiled is y down, so we flip it
    val layerOffsetY = -layer.getRenderOffsetY * unitScale - viewBounds.y * (layer.parallaxY - 1)

    val layerHexLength = hexSideLength * unitScale

    if (staggerAxisX) {
      val tileWidthLowerCorner = (layerTileWidth - layerHexLength) / 2
      val tileWidthUpperCorner = (layerTileWidth + layerHexLength) / 2
      val layerTileHeight50    = layerTileHeight * 0.50f

      val row1 = Math.max(0, ((viewBounds.y - layerTileHeight50 - layerOffsetY) / layerTileHeight).toInt)
      val row2 = Math.min(layerHeight, ((viewBounds.y + viewBounds.height + layerTileHeight - layerOffsetY) / layerTileHeight).toInt)

      val col1 = Math.max(0, ((viewBounds.x - tileWidthLowerCorner - layerOffsetX) / tileWidthUpperCorner).toInt)
      val col2 = Math.min(layerWidth, ((viewBounds.x + viewBounds.width + tileWidthUpperCorner - layerOffsetX) / tileWidthUpperCorner).toInt)

      // depending on the stagger index either draw all even before the odd or vice versa
      val colA = if (staggerIndexEven == (col1 % 2 == 0)) col1 + 1 else col1
      val colB = if (staggerIndexEven == (col1 % 2 == 0)) col1 else col1 + 1

      var row = row2 - 1
      while (row >= row1) {
        var col = colA
        while (col < col2) {
          renderCell(
            layer.getCell(col, row),
            tileWidthUpperCorner * col + layerOffsetX,
            layerTileHeight50 + (layerTileHeight * row) + layerOffsetY,
            color
          )
          col += 2
        }
        col = colB
        while (col < col2) {
          renderCell(layer.getCell(col, row), tileWidthUpperCorner * col + layerOffsetX, layerTileHeight * row + layerOffsetY, color)
          col += 2
        }
        row -= 1
      }
    } else {
      val tileHeightLowerCorner = (layerTileHeight - layerHexLength) / 2
      val tileHeightUpperCorner = (layerTileHeight + layerHexLength) / 2
      val layerTileWidth50      = layerTileWidth * 0.50f

      val row1 = Math.max(0, ((viewBounds.y - tileHeightLowerCorner - layerOffsetY) / tileHeightUpperCorner).toInt)
      val row2 = Math.min(layerHeight, ((viewBounds.y + viewBounds.height + tileHeightUpperCorner - layerOffsetY) / tileHeightUpperCorner).toInt)

      val col1 = Math.max(0, ((viewBounds.x - layerTileWidth50 - layerOffsetX) / layerTileWidth).toInt)
      val col2 = Math.min(layerWidth, ((viewBounds.x + viewBounds.width + layerTileWidth - layerOffsetX) / layerTileWidth).toInt)

      var row = row2 - 1
      while (row >= row1) {
        // depending on the stagger index either shift for even or uneven indexes
        val shiftX = if ((row % 2 == 0) == staggerIndexEven) layerTileWidth50 else 0f
        var col    = col1
        while (col < col2) {
          renderCell(layer.getCell(col, row), layerTileWidth * col + shiftX + layerOffsetX, tileHeightUpperCorner * row + layerOffsetY, color)
          col += 1
        }
        row -= 1
      }
    }
  }

  /** render a single cell */
  private def renderCell(cell: Nullable[TiledMapTileLayer.Cell], x: Float, y: Float, color: Float): Unit =
    cell.foreach { c =>
      val tile = c.tile
      tile.foreach { t =>
        if (!t.isInstanceOf[AnimatedTiledMapTile]) {
          val flipX     = c.flipHorizontally
          val flipY     = c.flipVertically
          val rotations = c.rotation

          val region = t.getTextureRegion

          val x1 = x + t.getOffsetX * unitScale
          val y1 = y + t.getOffsetY * unitScale
          val x2 = x1 + region.regionWidth * unitScale
          val y2 = y1 + region.regionHeight * unitScale

          vertices(Batch.X1) = x1; vertices(Batch.Y1) = y1; vertices(Batch.C1) = color
          vertices(Batch.U1) = region.u; vertices(Batch.V1) = region.v2
          vertices(Batch.X2) = x1; vertices(Batch.Y2) = y2; vertices(Batch.C2) = color
          vertices(Batch.U2) = region.u; vertices(Batch.V2) = region.v
          vertices(Batch.X3) = x2; vertices(Batch.Y3) = y2; vertices(Batch.C3) = color
          vertices(Batch.U3) = region.u2; vertices(Batch.V3) = region.v
          vertices(Batch.X4) = x2; vertices(Batch.Y4) = y1; vertices(Batch.C4) = color
          vertices(Batch.U4) = region.u2; vertices(Batch.V4) = region.v2

          if (flipX) {
            var temp = vertices(Batch.U1); vertices(Batch.U1) = vertices(Batch.U3); vertices(Batch.U3) = temp
            temp = vertices(Batch.U2); vertices(Batch.U2) = vertices(Batch.U4); vertices(Batch.U4) = temp
          }
          if (flipY) {
            var temp = vertices(Batch.V1); vertices(Batch.V1) = vertices(Batch.V3); vertices(Batch.V3) = temp
            temp = vertices(Batch.V2); vertices(Batch.V2) = vertices(Batch.V4); vertices(Batch.V4) = temp
          }
          if (rotations == 2) {
            var tempU = vertices(Batch.U1); vertices(Batch.U1) = vertices(Batch.U3); vertices(Batch.U3) = tempU
            tempU = vertices(Batch.U2); vertices(Batch.U2) = vertices(Batch.U4); vertices(Batch.U4) = tempU
            var tempV = vertices(Batch.V1); vertices(Batch.V1) = vertices(Batch.V3); vertices(Batch.V3) = tempV
            tempV = vertices(Batch.V2); vertices(Batch.V2) = vertices(Batch.V4); vertices(Batch.V4) = tempV
          }
          batch.draw(region.texture, vertices, 0, BatchTiledMapRenderer.NUM_VERTICES)
        }
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
      val tileHeight     = getMap.properties.getAs[Integer]("tileheight").get.intValue()
      val mapHeight      = getMap.properties.getAs[Integer]("height").get.intValue()
      val layerHexLength = hexSideLength
      // Map height if it were tiles
      val totalHeightPixels = (mapHeight * tileHeight) * unitScale
      // To determine size of Hex map height we use (mapHeight * tileHeight(3/4)) + (layerHexLength * 0.5f)
      val hexMapHeightPixels = ((mapHeight * tileHeight * (3f / 4f)) + (layerHexLength * 0.5f)) * unitScale

      val layerTileHeight = tileHeight * unitScale
      val halfTileHeight  = layerTileHeight * 0.5f

      val imageLayerYOffset = if (staggerAxisX) {

        /** If X axis staggered, must offset imagelayer y position by adding half of tileHeight to match position */
        halfTileHeight
      } else {

        /** ImageLayer's y position seems to be placed at an offset determined the total height if this were a normal tile map minus the height as calculated for a hexmap. We get this number and use
          * it to counter offset our Y. Then we will have our imagelayer matching its position in Tiled.
          */
        -(totalHeightPixels - hexMapHeightPixels)
      }

      val x  = layer.x
      val y  = layer.y
      val x1 = x * unitScale - viewBounds.x * (layer.parallaxX - 1)
      val y1 = y * unitScale - viewBounds.y * (layer.parallaxY - 1) + imageLayerYOffset
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
  }

}
