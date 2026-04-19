/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/tiled/renderers/BatchTiledMapRenderer.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - Audited 2026-03-03 against libGDX source: all public/protected methods, fields, and constants match 1:1
 * - Disposable.dispose() mapped to AutoCloseable.close()
 * - null checks on region replaced with Nullable[A] idiom
 * - Java return-early replaced with if/else control flow
 * - Constructors: 4 Java ctors mapped to primary + 3 auxiliary (matching signatures)
 * - Added getImageLayerColor/getTileLayerColor helper methods (match Java source)
 * - renderMapLayer uses Scala pattern match instead of Java instanceof chain
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 317
 * Covenant-baseline-methods: BatchTiledMapRenderer,NUM_VERTICES,_batch,_map,_unitScale,_viewBounds,alphaMultiplier,batch,batchColor,beginRender,close,color,combinedTint,endRender,getImageLayerColor,getTileLayerColor,h,height,imageBounds,map,map_,opacityMultiplier,ownsBatch,region,render,renderImageLayer,renderMapLayer,renderObject,renderObjects,repeatedImageBounds,setView,supportsTransparency,this,unitScale,vertices,viewBounds,w,width
 * Covenant-source-reference: com/badlogic/gdx/maps/tiled/renderers/BatchTiledMapRenderer.java
 * Covenant-verified: 2026-04-19
 */
package sge
package maps
package tiled
package renderers

import sge.Sge
import sge.graphics.{ Color, OrthographicCamera }
import sge.graphics.g2d.{ Batch, SpriteBatch }
import sge.maps.{ MapGroupLayer, MapLayer, MapObject }
import sge.maps.tiled.tiles.AnimatedTiledMapTile
import sge.math.{ Matrix4, Rectangle }
import sge.utils.Nullable

abstract class BatchTiledMapRenderer(
  protected var _map:       TiledMap,
  protected var _unitScale: Float,
  protected var _batch:     Batch,
  protected var ownsBatch:  Boolean
)(using Sge)
    extends TiledMapRenderer
    with AutoCloseable {

  protected val _viewBounds:         Rectangle    = Rectangle()
  protected val imageBounds:         Rectangle    = Rectangle()
  protected val repeatedImageBounds: Rectangle    = Rectangle()
  protected val vertices:            Array[Float] = new Array[Float](BatchTiledMapRenderer.NUM_VERTICES)

  def this(map: TiledMap)(using Sge) =
    this(map, 1.0f, SpriteBatch(), true)

  def this(map: TiledMap, unitScale: Float)(using Sge) =
    this(map, unitScale, SpriteBatch(), true)

  def this(map: TiledMap, batch: Batch)(using Sge) =
    this(map, 1.0f, batch, false)

  def map: TiledMap = _map

  def map_=(map: TiledMap): Unit =
    this._map = map

  def unitScale: Float = _unitScale

  def batch: Batch = _batch

  def viewBounds: Rectangle = _viewBounds

  override def setView(camera: OrthographicCamera): Unit = {
    batch.projectionMatrix = camera.combined
    val width  = camera.viewportWidth.toFloat * camera.zoom
    val height = camera.viewportHeight.toFloat * camera.zoom
    val w      = width * Math.abs(camera.up.y) + height * Math.abs(camera.up.x)
    val h      = height * Math.abs(camera.up.y) + width * Math.abs(camera.up.x)
    viewBounds.set(camera.position.x - w / 2, camera.position.y - h / 2, w, h)
  }

  override def setView(projection: Matrix4, x: Float, y: Float, width: Float, height: Float): Unit = {
    batch.projectionMatrix = projection
    viewBounds.set(x, y, width, height)
  }

  override def render(): Unit = {
    beginRender()
    map.layers.foreach { layer =>
      renderMapLayer(layer)
    }
    endRender()
  }

  override def render(layers: Array[Int]): Unit = {
    beginRender()
    layers.foreach { layerIdx =>
      val layer = map.layers.get(layerIdx)
      renderMapLayer(layer)
    }
    endRender()
  }

  def renderMapLayer(layer: MapLayer): Unit =
    if (!layer.visible) ()
    else
      layer match {
        case groupLayer: MapGroupLayer =>
          val childLayers = groupLayer.layers
          var i           = 0
          while (i < childLayers.size) {
            val childLayer = childLayers.get(i)
            if (childLayer.visible) {
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
    layer.objects.foreach { obj =>
      renderObject(obj)
    }

  override def renderObject(obj: MapObject): Unit = {}

  override def renderImageLayer(layer: TiledMapImageLayer): Unit = {
    val batchColor = batch.color

    val color = getImageLayerColor(layer, batchColor)

    val vertices = this.vertices

    val region = layer.region

    if (Nullable(region).isEmpty) {
      ()
    } else {
      val x  = layer.x
      val y  = layer.y
      val x1 = x * unitScale - viewBounds.x * (layer.parallaxX - 1)
      val y1 = y * unitScale - viewBounds.y * (layer.parallaxY - 1)
      val x2 = x1 + region.regionWidth * unitScale
      val y2 = y1 + region.regionHeight * unitScale

      imageBounds.set(x1, y1, x2 - x1, y2 - y1)

      if (!layer.repeatX && !layer.repeatY) {
        if (viewBounds.contains(imageBounds) || viewBounds.overlaps(imageBounds)) {
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

          batch.draw(region.texture, vertices, 0, BatchTiledMapRenderer.NUM_VERTICES)
        }
      } else {
        // Determine number of times to repeat image across X and Y, + 4 for padding to avoid pop in/out
        val repeatX = if (layer.repeatX) Math.ceil((viewBounds.width / imageBounds.width) + 4).toInt else 0
        val repeatY = if (layer.repeatY) Math.ceil((viewBounds.height / imageBounds.height) + 4).toInt else 0

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
            if (layer.repeatX) {
              rx1 = startX + ((i - 2) * imageBounds.width) + (x1 % imageBounds.width)
              rx2 = rx1 + imageBounds.width
            }

            if (layer.repeatY) {
              ry1 = startY + ((j - 2) * imageBounds.height) + (y1 % imageBounds.height)
              ry2 = ry1 + imageBounds.height
            }

            repeatedImageBounds.set(rx1, ry1, rx2 - rx1, ry2 - ry1)

            if (viewBounds.contains(repeatedImageBounds) || viewBounds.overlaps(repeatedImageBounds)) {
              val ru1 = region.u
              val rv1 = region.v2
              val ru2 = region.u2
              val rv2 = region.v

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

              batch.draw(region.texture, vertices, 0, BatchTiledMapRenderer.NUM_VERTICES)
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
    val combinedTint = layer.combinedTintColor

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
      batchColor.a * (layer.opacity * opacityMultiplier)
    )
  }

  /** Calculates the float color for rendering a tile layer, taking into account the layer's tint color and opacity, then multiplying is against the batchColor
    *
    * @param layer
    * @param batchColor
    */
  protected def getTileLayerColor(layer: TiledMapTileLayer, batchColor: Color): Float =
    Color.toFloatBits(
      batchColor.r * layer.combinedTintColor.r,
      batchColor.g * layer.combinedTintColor.g,
      batchColor.b * layer.combinedTintColor.b,
      batchColor.a * layer.combinedTintColor.a * layer.opacity
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
