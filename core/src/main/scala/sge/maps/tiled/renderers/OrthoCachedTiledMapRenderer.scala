/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/tiled/renderers/OrthoCachedTiledMapRenderer.java
 * Original authors: Justin Shapcott, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package maps
package tiled
package renderers

import sge.Sge
import sge.graphics.{ Color, GL20, OrthographicCamera }
import sge.graphics.g2d.{ Batch, SpriteCache }
import sge.maps.{ MapLayer, MapObject }
import sge.math.{ Matrix4, Rectangle }
import sge.utils.Nullable

/** Renders ortho tiles by caching geometry on the GPU. How much is cached is controlled by {@link #setOverCache(float)}. When the view reaches the edge of the cached tiles, the cache is rebuilt at
  * the new view position. <p> This class may have poor performance when tiles are often changed dynamically, since the cache must be rebuilt after each change.
  * @author
  *   Justin Shapcott
  * @author
  *   Nathan Sweet
  */
class OrthoCachedTiledMapRenderer(
  protected val map:       TiledMap,
  protected var unitScale: Float,
  cacheSize:               Int
)(using Sge)
    extends TiledMapRenderer
    with AutoCloseable {

  protected val spriteCache: SpriteCache  = new SpriteCache(cacheSize, true)
  protected val vertices:    Array[Float] = new Array[Float](OrthoCachedTiledMapRenderer.NUM_VERTICES)
  protected var blending:    Boolean      = false

  protected val viewBounds:  Rectangle = new Rectangle()
  protected val cacheBounds: Rectangle = new Rectangle()
  protected val imageBounds: Rectangle = new Rectangle()

  protected var overCache:     Float   = 0.50f
  protected var maxTileWidth:  Float   = 0f
  protected var maxTileHeight: Float   = 0f
  protected var cached:        Boolean = false
  protected var count:         Int     = 0
  protected var canCacheMoreN: Boolean = false
  protected var canCacheMoreE: Boolean = false
  protected var canCacheMoreW: Boolean = false
  protected var canCacheMoreS: Boolean = false

  /** Creates a renderer with a unit scale of 1 and cache size of 2000. */
  def this(map: TiledMap)(using Sge) = this(map, 1f, 2000)

  /** Creates a renderer with a cache size of 2000. */
  def this(map: TiledMap, unitScale: Float)(using Sge) = this(map, unitScale, 2000)

  override def setView(camera: OrthographicCamera): Unit = {
    spriteCache.setProjectionMatrix(camera.combined)
    val width  = camera.viewportWidth * camera.zoom + maxTileWidth * 2 * unitScale
    val height = camera.viewportHeight * camera.zoom + maxTileHeight * 2 * unitScale
    viewBounds.set(camera.position.x - width / 2, camera.position.y - height / 2, width, height)

    if (
      (canCacheMoreW && viewBounds.x < cacheBounds.x - OrthoCachedTiledMapRenderer.tolerance) ||
      (canCacheMoreS && viewBounds.y < cacheBounds.y - OrthoCachedTiledMapRenderer.tolerance) ||
      (canCacheMoreE && viewBounds.x + viewBounds.width > cacheBounds.x + cacheBounds.width + OrthoCachedTiledMapRenderer.tolerance) ||
      (canCacheMoreN && viewBounds.y + viewBounds.height > cacheBounds.y + cacheBounds.height + OrthoCachedTiledMapRenderer.tolerance)
    ) {
      cached = false
    }
  }

  override def setView(projection: Matrix4, x: Float, y: Float, width: Float, height: Float): Unit = {
    spriteCache.setProjectionMatrix(projection)
    val ax = x - maxTileWidth * unitScale
    val ay = y - maxTileHeight * unitScale
    val aw = width + maxTileWidth * 2 * unitScale
    val ah = height + maxTileHeight * 2 * unitScale
    viewBounds.set(ax, ay, aw, ah)

    if (
      (canCacheMoreW && viewBounds.x < cacheBounds.x - OrthoCachedTiledMapRenderer.tolerance) ||
      (canCacheMoreS && viewBounds.y < cacheBounds.y - OrthoCachedTiledMapRenderer.tolerance) ||
      (canCacheMoreE && viewBounds.x + viewBounds.width > cacheBounds.x + cacheBounds.width + OrthoCachedTiledMapRenderer.tolerance) ||
      (canCacheMoreN && viewBounds.y + viewBounds.height > cacheBounds.y + cacheBounds.height + OrthoCachedTiledMapRenderer.tolerance)
    ) {
      cached = false
    }
  }

  override def render(): Unit = {
    if (!cached) {
      cached = true
      count = 0
      spriteCache.clear()

      val extraWidth  = viewBounds.width * overCache
      val extraHeight = viewBounds.height * overCache
      cacheBounds.x = viewBounds.x - extraWidth
      cacheBounds.y = viewBounds.y - extraHeight
      cacheBounds.width = viewBounds.width + extraWidth * 2
      cacheBounds.height = viewBounds.height + extraHeight * 2

      map.getLayers.foreach { layer =>
        spriteCache.beginCache()
        layer match {
          case tileLayer: TiledMapTileLayer =>
            renderTileLayer(tileLayer)
          case imageLayer: TiledMapImageLayer =>
            renderImageLayer(imageLayer)
          case _ => ()
        }
        spriteCache.endCache()
      }
    }

    if (blending) {
      Sge().graphics.gl.glEnable(GL20.GL_BLEND)
      Sge().graphics.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
    }
    spriteCache.begin()
    val mapLayers = map.getLayers
    var i         = 0
    val j         = mapLayers.getCount
    while (i < j) {
      val layer = mapLayers.get(i)
      if (layer.isVisible) {
        spriteCache.draw(i)
        renderObjects(layer)
      }
      i += 1
    }
    spriteCache.end()
    if (blending) Sge().graphics.gl.glDisable(GL20.GL_BLEND)
  }

  override def render(layers: Array[Int]): Unit = {
    if (!cached) {
      cached = true
      count = 0
      spriteCache.clear()

      val extraWidth  = viewBounds.width * overCache
      val extraHeight = viewBounds.height * overCache
      cacheBounds.x = viewBounds.x - extraWidth
      cacheBounds.y = viewBounds.y - extraHeight
      cacheBounds.width = viewBounds.width + extraWidth * 2
      cacheBounds.height = viewBounds.height + extraHeight * 2

      map.getLayers.foreach { layer =>
        spriteCache.beginCache()
        layer match {
          case tileLayer: TiledMapTileLayer =>
            renderTileLayer(tileLayer)
          case imageLayer: TiledMapImageLayer =>
            renderImageLayer(imageLayer)
          case _ => ()
        }
        spriteCache.endCache()
      }
    }

    if (blending) {
      Sge().graphics.gl.glEnable(GL20.GL_BLEND)
      Sge().graphics.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
    }
    spriteCache.begin()
    val mapLayers = map.getLayers
    layers.foreach { i =>
      val layer = mapLayers.get(i)
      if (layer.isVisible) {
        spriteCache.draw(i)
        renderObjects(layer)
      }
    }
    spriteCache.end()
    if (blending) Sge().graphics.gl.glDisable(GL20.GL_BLEND)
  }

  override def renderObjects(layer: MapLayer): Unit =
    layer.getObjects.foreach { obj =>
      renderObject(obj)
    }

  override def renderObject(obj: MapObject): Unit = {}

  override def renderTileLayer(layer: TiledMapTileLayer): Unit = {
    val color = Color.toFloatBits(
      layer.getCombinedTintColor.r,
      layer.getCombinedTintColor.g,
      layer.getCombinedTintColor.b,
      layer.getOpacity * layer.getCombinedTintColor.a
    )

    val layerWidth  = layer.getWidth
    val layerHeight = layer.getHeight

    val layerTileWidth  = layer.getTileWidth * unitScale
    val layerTileHeight = layer.getTileHeight * unitScale

    val layerOffsetX = layer.getRenderOffsetX * unitScale - viewBounds.x * (layer.getParallaxX - 1)
    // offset in tiled is y down, so we flip it
    val layerOffsetY = -layer.getRenderOffsetY * unitScale - viewBounds.y * (layer.getParallaxY - 1)

    val col1 = Math.max(0, ((cacheBounds.x - layerOffsetX) / layerTileWidth).toInt)
    val col2 = Math.min(layerWidth, ((cacheBounds.x + cacheBounds.width + layerTileWidth - layerOffsetX) / layerTileWidth).toInt)

    val row1 = Math.max(0, ((cacheBounds.y - layerOffsetY) / layerTileHeight).toInt)
    val row2 = Math.min(layerHeight, ((cacheBounds.y + cacheBounds.height + layerTileHeight - layerOffsetY) / layerTileHeight).toInt)

    canCacheMoreN = row2 < layerHeight
    canCacheMoreE = col2 < layerWidth
    canCacheMoreW = col1 > 0
    canCacheMoreS = row1 > 0

    val vertices = this.vertices
    var row      = row2
    while (row >= row1) {
      var col = col1
      while (col < col2) {
        val cell = layer.getCell(col, row)
        cell.foreach { c =>
          val tile = c.getTile
          tile.foreach { t =>
            count += 1
            val flipX     = c.getFlipHorizontally
            val flipY     = c.getFlipVertically
            val rotations = c.getRotation

            val region  = t.getTextureRegion
            val texture = region.getTexture()

            val x1 = col * layerTileWidth + t.getOffsetX * unitScale + layerOffsetX
            val y1 = row * layerTileHeight + t.getOffsetY * unitScale + layerOffsetY
            val x2 = x1 + region.getRegionWidth() * unitScale
            val y2 = y1 + region.getRegionHeight() * unitScale

            val adjustX = 0.5f / texture.getWidth
            val adjustY = 0.5f / texture.getHeight
            val u1      = region.getU() + adjustX
            val v1      = region.getV2() - adjustY
            val u2      = region.getU2() - adjustX
            val v2      = region.getV() + adjustY

            vertices(Batch.X1) = x1; vertices(Batch.Y1) = y1; vertices(Batch.C1) = color
            vertices(Batch.U1) = u1; vertices(Batch.V1) = v1
            vertices(Batch.X2) = x1; vertices(Batch.Y2) = y2; vertices(Batch.C2) = color
            vertices(Batch.U2) = u1; vertices(Batch.V2) = v2
            vertices(Batch.X3) = x2; vertices(Batch.Y3) = y2; vertices(Batch.C3) = color
            vertices(Batch.U3) = u2; vertices(Batch.V3) = v2
            vertices(Batch.X4) = x2; vertices(Batch.Y4) = y1; vertices(Batch.C4) = color
            vertices(Batch.U4) = u2; vertices(Batch.V4) = v1

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
            spriteCache.add(texture, vertices, 0, OrthoCachedTiledMapRenderer.NUM_VERTICES)
          }
        }
        col += 1
      }
      row -= 1
    }
  }

  override def renderImageLayer(layer: TiledMapImageLayer): Unit = {
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
    val color = Color.toFloatBits(
      combinedTint.r * alphaMultiplier,
      combinedTint.g * alphaMultiplier,
      combinedTint.b * alphaMultiplier,
      layer.getOpacity * opacityMultiplier
    )

    val vertices = this.vertices

    val region = layer.getTextureRegion

    if (Nullable(region).isEmpty) {
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
        vertices(Batch.X1) = x1; vertices(Batch.Y1) = y1; vertices(Batch.C1) = color
        vertices(Batch.U1) = region.getU(); vertices(Batch.V1) = region.getV2()
        vertices(Batch.X2) = x1; vertices(Batch.Y2) = y2; vertices(Batch.C2) = color
        vertices(Batch.U2) = region.getU(); vertices(Batch.V2) = region.getV()
        vertices(Batch.X3) = x2; vertices(Batch.Y3) = y2; vertices(Batch.C3) = color
        vertices(Batch.U3) = region.getU2(); vertices(Batch.V3) = region.getV()
        vertices(Batch.X4) = x2; vertices(Batch.Y4) = y1; vertices(Batch.C4) = color
        vertices(Batch.U4) = region.getU2(); vertices(Batch.V4) = region.getV2()
        spriteCache.add(region.getTexture(), vertices, 0, OrthoCachedTiledMapRenderer.NUM_VERTICES)
      } else {
        // Determine number of times to repeat image across X and Y, + 4 for padding to avoid pop in/out
        val repeatX = if (layer.isRepeatX) Math.ceil((cacheBounds.width / imageBounds.width) + 4).toInt else 0
        val repeatY = if (layer.isRepeatY) Math.ceil((cacheBounds.height / imageBounds.height) + 4).toInt else 0

        // Calculate the offset of the first image to align with the camera
        var startX = cacheBounds.x; startX = startX - (startX % imageBounds.width)
        var startY = cacheBounds.y; startY = startY - (startY % imageBounds.height)

        var i = 0
        while (i <= repeatX) {
          var j = 0
          while (j <= repeatY) {
            var rx1 = x1; var ry1 = y1; var rx2 = x2; var ry2 = y2
            if (layer.isRepeatX) { rx1 = startX + ((i - 2) * imageBounds.width) + (x1 % imageBounds.width); rx2 = rx1 + imageBounds.width }
            if (layer.isRepeatY) { ry1 = startY + ((j - 2) * imageBounds.height) + (y1 % imageBounds.height); ry2 = ry1 + imageBounds.height }
            vertices(Batch.X1) = rx1; vertices(Batch.Y1) = ry1; vertices(Batch.C1) = color
            vertices(Batch.U1) = region.getU(); vertices(Batch.V1) = region.getV2()
            vertices(Batch.X2) = rx1; vertices(Batch.Y2) = ry2; vertices(Batch.C2) = color
            vertices(Batch.U2) = region.getU(); vertices(Batch.V2) = region.getV()
            vertices(Batch.X3) = rx2; vertices(Batch.Y3) = ry2; vertices(Batch.C3) = color
            vertices(Batch.U3) = region.getU2(); vertices(Batch.V3) = region.getV()
            vertices(Batch.X4) = rx2; vertices(Batch.Y4) = ry1; vertices(Batch.C4) = color
            vertices(Batch.U4) = region.getU2(); vertices(Batch.V4) = region.getV2()
            spriteCache.add(region.getTexture(), vertices, 0, OrthoCachedTiledMapRenderer.NUM_VERTICES)
            j += 1
          }
          i += 1
        }
      }
    }
  }

  /** Causes the cache to be rebuilt the next time it is rendered. */
  def invalidateCache(): Unit =
    cached = false

  /** Returns true if tiles are currently cached. */
  def isCached: Boolean = cached

  /** Sets the percentage of the view that is cached in each direction. Default is 0.5. <p> Eg, 0.75 will cache 75% of the width of the view to the left and right of the view, and 75% of the height of
    * the view above and below the view.
    */
  def setOverCache(overCache: Float): Unit =
    this.overCache = overCache

  /** Expands the view size in each direction, ensuring that tiles of this size or smaller are never culled from the visible portion of the view. Default is 0,0. <p> The amount of tiles cached is
    * computed using <code>(view size + max tile size) * overCache</code>, meaning the max tile size increases the amount cached and possibly {@link #setOverCache(float)} can be reduced. <p> If the
    * view size and {@link #setOverCache(float)} are configured so the size of the cached tiles is always larger than the largest tile size, this setting is not needed.
    */
  def setMaxTileSize(maxPixelWidth: Float, maxPixelHeight: Float): Unit = {
    this.maxTileWidth = maxPixelWidth
    this.maxTileHeight = maxPixelHeight
  }

  def setBlending(blending: Boolean): Unit =
    this.blending = blending

  def getSpriteCache: SpriteCache = spriteCache

  override def close(): Unit =
    spriteCache.close()
}

object OrthoCachedTiledMapRenderer {
  final private val tolerance                 = 0.00001f
  final protected[renderers] val NUM_VERTICES = 20
}
