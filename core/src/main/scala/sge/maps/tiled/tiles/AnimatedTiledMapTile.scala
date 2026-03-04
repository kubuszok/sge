/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/tiled/tiles/AnimatedTiledMapTile.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - Audited 2026-03-03 against libGDX source: all methods, fields, and constructors match 1:1
 * - GdxRuntimeException replaced with SgeError.GraphicsError
 * - Java return-in-loop (getCurrentFrameIndex) replaced with boundary/break
 * - Java Array<StaticTiledMapTile> replaced with DynamicArray[StaticTiledMapTile]
 * - Java IntArray replaced with Array[Int]
 * - Java null-initialized properties/objects replaced with Nullable.empty
 * - Private primary constructor + 2 public auxiliary constructors (matching Java's 2 public ctors)
 * - Companion object holds lastTiledMapRenderTime, initialTimeOffset, updateAnimationBaseTime (match Java static)
 * - TODO: opaque Millis for animationIntervals, loopDuration, lastTiledMapRenderTime -- see docs/improvements/opaque-types.md
 */
package sge
package maps
package tiled
package tiles

import scala.util.boundary
import scala.util.boundary.break
import sge.graphics.g2d.TextureRegion
import sge.maps.{ MapObjects, MapProperties }
import sge.utils.{ DynamicArray, Nullable, SgeError, TimeUtils }

/** @brief Represents a changing {@link TiledMapTile}. */
class AnimatedTiledMapTile private (
  private val frameTiles:         Array[StaticTiledMapTile],
  private var animationIntervals: Array[Int],
  private var loopDuration:       Int
) extends TiledMapTile {

  private var id: Int = 0

  private var blendMode: TiledMapTile.BlendMode = TiledMapTile.BlendMode.ALPHA

  private var _properties: Nullable[MapProperties] = Nullable.empty

  private var _objects: Nullable[MapObjects] = Nullable.empty

  /** Creates an animated tile with the given animation interval and frame tiles.
    *
    * @param interval
    *   The interval between each individual frame tile.
    * @param frameTiles
    *   An array of {@link StaticTiledMapTile}s that make up the animation.
    */
  def this(interval: Float, frameTiles: DynamicArray[StaticTiledMapTile]) =
    this(
      frameTiles.toArray,
      Array.fill(frameTiles.size)((interval * 1000f).toInt),
      frameTiles.size * (interval * 1000f).toInt
    )

  /** Creates an animated tile with the given animation intervals and frame tiles.
    *
    * @param intervals
    *   The intervals between each individual frame tile in milliseconds.
    * @param frameTiles
    *   An array of {@link StaticTiledMapTile}s that make up the animation.
    */
  def this(intervals: Array[Int], frameTiles: DynamicArray[StaticTiledMapTile]) =
    this(
      frameTiles.toArray,
      intervals.clone(),
      intervals.sum
    )

  override def getId: Int = id

  override def setId(id: Int): Unit =
    this.id = id

  override def getBlendMode: TiledMapTile.BlendMode = blendMode

  override def setBlendMode(blendMode: TiledMapTile.BlendMode): Unit =
    this.blendMode = blendMode

  def getCurrentFrameIndex: Int = boundary {
    var currentTime = (AnimatedTiledMapTile.lastTiledMapRenderTime % loopDuration).toInt

    var i = 0
    while (i < animationIntervals.length) {
      val animationInterval = animationIntervals(i)
      if (currentTime <= animationInterval) break(i)
      currentTime -= animationInterval
      i += 1
    }

    throw SgeError.GraphicsError("Could not determine current animation frame in AnimatedTiledMapTile.  This should never happen.")
  }

  def getCurrentFrame: TiledMapTile =
    frameTiles(getCurrentFrameIndex)

  override def getTextureRegion: TextureRegion =
    getCurrentFrame.getTextureRegion

  override def setTextureRegion(textureRegion: TextureRegion): Unit =
    throw SgeError.GraphicsError("Cannot set the texture region of AnimatedTiledMapTile.")

  override def getOffsetX: Float =
    getCurrentFrame.getOffsetX

  override def setOffsetX(offsetX: Float): Unit =
    throw SgeError.GraphicsError("Cannot set offset of AnimatedTiledMapTile.")

  override def getOffsetY: Float =
    getCurrentFrame.getOffsetY

  override def setOffsetY(offsetY: Float): Unit =
    throw SgeError.GraphicsError("Cannot set offset of AnimatedTiledMapTile.")

  def getAnimationIntervals: Array[Int] = animationIntervals

  def setAnimationIntervals(intervals: Array[Int]): Unit =
    if (intervals.length == animationIntervals.length) {
      this.animationIntervals = intervals

      loopDuration = 0
      var i = 0
      while (i < intervals.length) {
        loopDuration += intervals(i)
        i += 1
      }
    } else {
      throw SgeError.GraphicsError(
        "Cannot set " + intervals.length + " frame intervals. The given int[] must have a size of "
          + animationIntervals.length + "."
      )
    }

  override def getProperties: MapProperties = {
    if (_properties.isEmpty) {
      _properties = Nullable(MapProperties())
    }
    _properties.getOrElse(MapProperties())
  }

  override def getObjects: MapObjects = {
    if (_objects.isEmpty) {
      _objects = Nullable(MapObjects())
    }
    _objects.getOrElse(MapObjects())
  }

  def getFrameTiles: Array[StaticTiledMapTile] = frameTiles
}

object AnimatedTiledMapTile {
  private var lastTiledMapRenderTime: Long = 0
  private val initialTimeOffset:      Long = TimeUtils.millis()

  /** Function is called by BatchTiledMapRenderer render(), lastTiledMapRenderTime is used to keep all of the tiles in lock-step animation and avoids having to call TimeUtils.millis() in
    * getTextureRegion()
    */
  def updateAnimationBaseTime(): Unit =
    lastTiledMapRenderTime = TimeUtils.millis() - initialTimeOffset
}
