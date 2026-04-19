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
 * - Java return-in-loop (currentFrameIndex) replaced with boundary/break
 * - Java Array<StaticTiledMapTile> replaced with DynamicArray[StaticTiledMapTile]
 * - Java IntArray replaced with Array[Int]
 * - Java null-initialized properties/objects replaced with Nullable.empty
 * - Private primary constructor + 2 public auxiliary constructors (matching Java's 2 public ctors)
 * - Companion object holds lastTiledMapRenderTime, initialTimeOffset, updateAnimationBaseTime (match Java static)
 * - Convention: opaque Millis for lastTiledMapRenderTime, initialTimeOffset
 *   Renames: getId/setId → var id, getBlendMode/setBlendMode → var blendMode,
 *     getTextureRegion/setTextureRegion → var textureRegion (throws),
 *     getOffsetX/setOffsetX → var offsetX/offsetY (throws),
 *     getProperties → def properties, getObjects → def objects,
 *     getCurrentFrameIndex → currentFrameIndex, getCurrentFrame → currentFrame,
 *     getAnimationIntervals/setAnimationIntervals → var animationIntervals,
 *     getFrameTiles → def frameTiles
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 171
 * Covenant-baseline-methods: AnimatedTiledMapTile,_animationIntervals,_blendMode,_frameTiles,_id,_objects,_properties,animationIntervals,animationIntervals_,blendMode,blendMode_,currentFrame,currentFrameIndex,currentTime,frameTiles,i,id,id_,initialTimeOffset,lastTiledMapRenderTime,loopDuration,objects,offsetX,offsetX_,offsetY,offsetY_,properties,textureRegion,textureRegion_,this,updateAnimationBaseTime
 * Covenant-source-reference: com/badlogic/gdx/maps/tiled/tiles/AnimatedTiledMapTile.java
 * Covenant-verified: 2026-04-19
 */
package sge
package maps
package tiled
package tiles

import scala.util.boundary
import scala.util.boundary.break
import sge.graphics.g2d.TextureRegion
import sge.maps.{ MapObjects, MapProperties }
import sge.utils.{ DynamicArray, Millis, Nullable, SgeError, TimeUtils }

/** @brief Represents a changing {@link TiledMapTile}. */
class AnimatedTiledMapTile private (
  private val _frameTiles:         Array[StaticTiledMapTile],
  private var _animationIntervals: Array[Int],
  private var loopDuration:        Int
) extends TiledMapTile {

  private var _id: Int = 0

  private var _blendMode: TiledMapTile.BlendMode = TiledMapTile.BlendMode.ALPHA

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

  override def id: Int = _id

  override def id_=(id: Int): Unit =
    this._id = id

  override def blendMode: TiledMapTile.BlendMode = _blendMode

  override def blendMode_=(blendMode: TiledMapTile.BlendMode): Unit =
    this._blendMode = blendMode

  def currentFrameIndex: Int = boundary {
    var currentTime = (AnimatedTiledMapTile.lastTiledMapRenderTime % Millis(loopDuration.toLong)).toInt

    var i = 0
    while (i < _animationIntervals.length) {
      val animationInterval = _animationIntervals(i)
      if (currentTime <= animationInterval) break(i)
      currentTime -= animationInterval
      i += 1
    }

    throw SgeError.GraphicsError("Could not determine current animation frame in AnimatedTiledMapTile.  This should never happen.")
  }

  def currentFrame: TiledMapTile =
    _frameTiles(currentFrameIndex)

  override def textureRegion: TextureRegion =
    currentFrame.textureRegion

  override def textureRegion_=(textureRegion: TextureRegion): Unit =
    throw SgeError.GraphicsError("Cannot set the texture region of AnimatedTiledMapTile.")

  override def offsetX: Float =
    currentFrame.offsetX

  override def offsetX_=(offsetX: Float): Unit =
    throw SgeError.GraphicsError("Cannot set offset of AnimatedTiledMapTile.")

  override def offsetY: Float =
    currentFrame.offsetY

  override def offsetY_=(offsetY: Float): Unit =
    throw SgeError.GraphicsError("Cannot set offset of AnimatedTiledMapTile.")

  def animationIntervals: Array[Int] = _animationIntervals

  def animationIntervals_=(intervals: Array[Int]): Unit =
    if (intervals.length == _animationIntervals.length) {
      this._animationIntervals = intervals

      loopDuration = 0
      var i = 0
      while (i < intervals.length) {
        loopDuration += intervals(i)
        i += 1
      }
    } else {
      throw SgeError.GraphicsError(
        "Cannot set " + intervals.length + " frame intervals. The given int[] must have a size of "
          + _animationIntervals.length + "."
      )
    }

  override def properties: MapProperties = {
    if (_properties.isEmpty) {
      _properties = Nullable(MapProperties())
    }
    _properties.getOrElse(MapProperties())
  }

  override def objects: MapObjects = {
    if (_objects.isEmpty) {
      _objects = Nullable(MapObjects())
    }
    _objects.getOrElse(MapObjects())
  }

  def frameTiles: Array[StaticTiledMapTile] = _frameTiles
}

object AnimatedTiledMapTile {
  private var lastTiledMapRenderTime: Millis = Millis.zero
  private val initialTimeOffset:      Millis = TimeUtils.millis()

  /** Function is called by BatchTiledMapRenderer render(), lastTiledMapRenderTime is used to keep all of the tiles in lock-step animation and avoids having to call TimeUtils.millis() in
    * getTextureRegion()
    */
  def updateAnimationBaseTime(): Unit =
    lastTiledMapRenderTime = TimeUtils.millis() - initialTimeOffset
}
