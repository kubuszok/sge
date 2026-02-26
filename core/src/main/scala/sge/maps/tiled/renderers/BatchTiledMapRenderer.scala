/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/tiled/renderers/BatchTiledMapRenderer.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package maps
package tiled
package renderers

import sge.Sge
import sge.graphics.{ Color, OrthographicCamera }
import sge.graphics.g2d.{ Batch, SpriteBatch, TextureRegion }
import sge.maps.{ MapGroupLayer, MapLayer, MapLayers, MapObject }
import sge.maps.tiled.tiles.AnimatedTiledMapTile
import sge.math.{ Matrix4, Rectangle }

abstract class BatchTiledMapRenderer(
  protected var map:       TiledMap,
  protected var unitScale: Float,
  protected var batch:     Batch,
  protected var ownsBatch: Boolean
)(using sge: Sge)
    extends TiledMapRenderer
    with AutoCloseable {

  protected val viewBounds:          Rectangle    = new Rectangle()
  protected val imageBounds:         Rectangle    = new Rectangle()
  protected val repeatedImageBounds: Rectangle    = new Rectangle()
  protected val vertices:            Array[Float] = new Array[Float](BatchTiledMapRenderer.NUM_VERTICES)

  def this(map: TiledMap)(using sge: Sge) =
    this(map, 1.0f, new SpriteBatch(), true)

  def this(map: TiledMap, unitScale: Float)(using sge: Sge) =
    this(map, unitScale, new SpriteBatch(), true)

  def this(map: TiledMap, batch: Batch)(using sge: Sge) =
    this(map, 1.0f, batch, false)

  def getMap: TiledMap = map

  def setMap(map: TiledMap): Unit =
    this.map = map

  def getUnitScale: Float = unitScale

  def getBatch: Batch = batch

  def getViewBounds: Rectangle = viewBounds

  override def setView(camera: OrthographicCamera): Unit = {
    batch.setProjectionMatrix(camera.combined)
    val width  = camera.viewportWidth * camera.zoom
    val height = camera.viewportHeight * camera.zoom
    val w      = width * Math.abs(camera.up.y) + height * Math.abs(camera.up.x)
    val h      = height * Math.abs(camera.up.y) + width * Math.abs(camera.up.x)
    viewBounds.set(camera.position.x - w / 2, camera.position.y - h / 2, w, h)
  }

  override def setView(projection: Matrix4, x: Float, y: Float, width: Float, height: Float): Unit = {
    batch.setProjectionMatrix(projection)
    viewBounds.set(x, y, width, height)
  }

  override def render(): Unit = {
    beginRender()
    map.getLayers.foreach { layer =>
      renderMapLayer(layer)
    }
    endRender()
  }

  override def render(layers: Array[Int]): Unit = {
    beginRender()
    layers.foreach { layerIdx =>
      val layer = map.getLayers.get(layerIdx)
      renderMapLayer(layer)
    }
    endRender()
  }

  def renderMapLayer(layer: MapLayer): Unit =
    if (!layer.isVisible) ()
    else
      layer match {
        case groupLayer: MapGroupLayer =>
          val childLayers = groupLayer.getLayers
          var i           = 0
          while (i < childLayers.size) {
            val childLayer = childLayers.get(i)
            if (childLayer.isVisible) {
              renderMapLayer(childLayer)
            }
            i += 1
          }
        case tileLayer: TiledMapTileLayer =>
          renderTileLayer(tileLayer)
        case imageLayer: TiledMapImageLayer =>
          renderImageLayer(imageLayer)
        case _ =>
          renderObjects(layer)
      }

  override def renderObjects(layer: MapLayer): Unit =
    layer.getObjects.foreach { obj =>
      renderObject(obj)
    }

  override def renderObject(obj: MapObject): Unit = {}

  override def renderImageLayer(layer: TiledMapImageLayer): Unit = {
    val batchColor = batch.getColor()

    val color = getImageLayerColor(layer, batchColor)

    val vertices = this.vertices

    val region = layer.getTextureRegion

    if (region == null) {
      ()
    } else {
      val x  = layer.getX
      val y  = layer.getY
      val x1 = x * unitScale - viewBounds.x * (layer.getParallaxX - 1)
      val y1 = y * unitScale - viewBounds.y * (layer.getParallaxY - 1)
      val x2 = x1 + region.getRegionWidth() * unitScale
      val y2 = y1 + region.getRegionHeight() * unitScale

      imageBounds.set(x1, y1, x2 - x1, y2 - y1)

      if (!layer.isRepeatX && !layer.isRepeatY) {
        if (viewBounds.contains(imageBounds) || viewBounds.overlaps(imageBounds)) {
          val u1 = region.getU()
          val v1 = region.getV2()
          val u2 = region.getU2()
          val v2 = region.getV()

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

          batch.draw(region.getTexture(), vertices, 0, BatchTiledMapRenderer.NUM_VERTICES)
        }
      } else {
        // Determine number of times to repeat image across X and Y, + 4 for padding to avoid pop in/out
        val repeatX = if (layer.isRepeatX) Math.ceil((viewBounds.width / imageBounds.width) + 4).toInt else 0
        val repeatY = if (layer.isRepeatY) Math.ceil((viewBounds.height / imageBounds.height) + 4).toInt else 0

        // Calculate the offset of the first image to align with the camera
        var startX = viewBounds.x
        var startY = viewBounds.y
        startX = startX - (startX % imageBounds.width)
        startY = startY - (startY % imageBounds.height)

        var i = 0
        while (i <= repeatX) {
          var j = 0
          while (j <= repeatY) {
            var rx1 = x1
            var ry1 = y1
            var rx2 = x2
            var ry2 = y2

            // Use (i -2)/(j-2) to begin placing our repeating images outside the camera.
            // In case the image is offset, we must negate this using + (x1% imageBounds.width)
            // It's a way to get the remainder of how many images would fit between its starting position and 0
            if (layer.isRepeatX) {
              rx1 = startX + ((i - 2) * imageBounds.width) + (x1 % imageBounds.width)
              rx2 = rx1 + imageBounds.width
            }

            if (layer.isRepeatY) {
              ry1 = startY + ((j - 2) * imageBounds.height) + (y1 % imageBounds.height)
              ry2 = ry1 + imageBounds.height
            }

            repeatedImageBounds.set(rx1, ry1, rx2 - rx1, ry2 - ry1)

            if (viewBounds.contains(repeatedImageBounds) || viewBounds.overlaps(repeatedImageBounds)) {
              val ru1 = region.getU()
              val rv1 = region.getV2()
              val ru2 = region.getU2()
              val rv2 = region.getV()

              vertices(Batch.X1) = rx1
              vertices(Batch.Y1) = ry1
              vertices(Batch.C1) = color
              vertices(Batch.U1) = ru1
              vertices(Batch.V1) = rv1

              vertices(Batch.X2) = rx1
              vertices(Batch.Y2) = ry2
              vertices(Batch.C2) = color
              vertices(Batch.U2) = ru1
              vertices(Batch.V2) = rv2

              vertices(Batch.X3) = rx2
              vertices(Batch.Y3) = ry2
              vertices(Batch.C3) = color
              vertices(Batch.U3) = ru2
              vertices(Batch.V3) = rv2

              vertices(Batch.X4) = rx2
              vertices(Batch.Y4) = ry1
              vertices(Batch.C4) = color
              vertices(Batch.U4) = ru2
              vertices(Batch.V4) = rv1

              batch.draw(region.getTexture(), vertices, 0, BatchTiledMapRenderer.NUM_VERTICES)
            }
            j += 1
          }
          i += 1
        }
      }
    }
  }

  /** Calculates the float color for rendering an image layer, taking into account the layer's tint color, opacity, and whether the image format supports transparency then multiplying is against the
    * batchColor
    *
    * @param layer
    *   The layer to render.
    * @param batchColor
    *   The current color of the batch.
    * @return
    *   The float color value to use for rendering.
    */
  protected def getImageLayerColor(layer: TiledMapImageLayer, batchColor: Color): Float = {
    val combinedTint = layer.getCombinedTintColor

    // Check if layer supports transparency
    val supportsTransparency = layer.supportsTransparency

    // If the Image Layer supports transparency we do not want to modify the combined tint during rendering
    // and if the Image Layer does not support transparency, we want to multiply the combined tint values, by its alpha
    val alphaMultiplier = if (supportsTransparency) 1f else combinedTint.a
    // Only modify opacity by combinedTint.b if Image Layer supports transparency
    val opacityMultiplier = if (supportsTransparency) combinedTint.a else 1f

    // For image layer rendering multiply all by alpha
    // except for opacity when image layer does not support transparency
    Color.toFloatBits(
      batchColor.r * (combinedTint.r * alphaMultiplier),
      batchColor.g * (combinedTint.g * alphaMultiplier),
      batchColor.b * (combinedTint.b * alphaMultiplier),
      batchColor.a * (layer.getOpacity * opacityMultiplier)
    )
  }

  /** Calculates the float color for rendering a tile layer, taking into account the layer's tint color and opacity, then multiplying is against the batchColor
    *
    * @param layer
    * @param batchColor
    */
  protected def getTileLayerColor(layer: TiledMapTileLayer, batchColor: Color): Float =
    Color.toFloatBits(
      batchColor.r * layer.getCombinedTintColor.r,
      batchColor.g * layer.getCombinedTintColor.g,
      batchColor.b * layer.getCombinedTintColor.b,
      batchColor.a * layer.getCombinedTintColor.a * layer.getOpacity
    )

  /** Called before the rendering of all layers starts. */
  protected def beginRender(): Unit = {
    AnimatedTiledMapTile.updateAnimationBaseTime()
    batch.begin()
  }

  /** Called after the rendering of all layers ended. */
  protected def endRender(): Unit =
    batch.end()

  override def close(): Unit =
    if (ownsBatch) {
      batch.close()
    }
}

object BatchTiledMapRenderer {
  final protected[renderers] val NUM_VERTICES = 20
}
