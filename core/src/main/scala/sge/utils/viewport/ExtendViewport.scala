/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/viewport/ExtendViewport.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package utils
package viewport

import sge.graphics.Camera
import sge.graphics.OrthographicCamera
import sge.utils.Scaling

/** A viewport that keeps the world aspect ratio by both scaling and extending the world. By default, the world is first scaled to fit within the viewport using {@link Scaling#fit} , then the shorter
  * dimension is lengthened to fill the viewport. Other scaling, such as {@link Scaling#contain} , may lengthen the world in both directions. A maximum size can be specified to limit how much the
  * world is extended and black bars (letterboxing) are used for any remaining space.
  * @author
  *   Nathan Sweet
  */
class ExtendViewport(minWorldWidth: Float, minWorldHeight: Float, maxWorldWidth: Float, maxWorldHeight: Float, camera: Camera)(using Sge) extends Viewport {
  private var _minWorldWidth:  Float   = minWorldWidth
  private var _minWorldHeight: Float   = minWorldHeight
  private var _maxWorldWidth:  Float   = maxWorldWidth
  private var _maxWorldHeight: Float   = maxWorldHeight
  private var scaling:         Scaling = Scaling.fit

  setCamera(camera)

  /** Creates a new viewport using a new {@link OrthographicCamera} with no maximum world size. */
  def this(minWorldWidth: Float, minWorldHeight: Float)(using Sge) =
    this(minWorldWidth, minWorldHeight, 0, 0, new OrthographicCamera())

  /** Creates a new viewport with no maximum world size. */
  def this(minWorldWidth: Float, minWorldHeight: Float, camera: Camera)(using Sge) =
    this(minWorldWidth, minWorldHeight, 0, 0, camera)

  /** Creates a new viewport using a new {@link OrthographicCamera} and a maximum world size.
    * @see
    *   ExtendViewport#ExtendViewport(float, float, float, float, Camera)
    */
  def this(minWorldWidth: Float, minWorldHeight: Float, maxWorldWidth: Float, maxWorldHeight: Float)(using Sge) =
    this(minWorldWidth, minWorldHeight, maxWorldWidth, maxWorldHeight, new OrthographicCamera())

  override def update(screenWidth: Int, screenHeight: Int, centerCamera: Boolean): Unit = {
    // Fit min size to the screen.
    var worldWidth  = _minWorldWidth
    var worldHeight = _minWorldHeight
    val scaled      = scaling.apply(worldWidth, worldHeight, screenWidth.toFloat, screenHeight.toFloat)

    // Extend, possibly in both directions depending on the scaling.
    var viewportWidth  = Math.round(scaled.x)
    var viewportHeight = Math.round(scaled.y)
    if (viewportWidth < screenWidth) {
      val toViewportSpace = viewportHeight / worldHeight
      val toWorldSpace    = worldHeight / viewportHeight
      var lengthen        = (screenWidth - viewportWidth) * toWorldSpace
      if (_maxWorldWidth > 0) lengthen = Math.min(lengthen, _maxWorldWidth - _minWorldWidth)
      worldWidth += lengthen
      viewportWidth += Math.round(lengthen * toViewportSpace)
    }
    if (viewportHeight < screenHeight) {
      val toViewportSpace = viewportWidth / worldWidth
      val toWorldSpace    = worldWidth / viewportWidth
      var lengthen        = (screenHeight - viewportHeight) * toWorldSpace
      if (_maxWorldHeight > 0) lengthen = Math.min(lengthen, _maxWorldHeight - _minWorldHeight)
      worldHeight += lengthen
      viewportHeight += Math.round(lengthen * toViewportSpace)
    }

    setWorldSize(worldWidth, worldHeight)

    // Center.
    setScreenBounds((screenWidth - viewportWidth) / 2, (screenHeight - viewportHeight) / 2, viewportWidth, viewportHeight)

    apply(centerCamera)
  }

  def getMinWorldWidth(): Float =
    _minWorldWidth

  def setMinWorldWidth(minWorldWidth: Float): Unit =
    this._minWorldWidth = minWorldWidth

  def getMinWorldHeight(): Float =
    _minWorldHeight

  def setMinWorldHeight(minWorldHeight: Float): Unit =
    this._minWorldHeight = minWorldHeight

  def getMaxWorldWidth(): Float =
    _maxWorldWidth

  def setMaxWorldWidth(maxWorldWidth: Float): Unit =
    this._maxWorldWidth = maxWorldWidth

  def getMaxWorldHeight(): Float =
    _maxWorldHeight

  def setMaxWorldHeight(maxWorldHeight: Float): Unit =
    this._maxWorldHeight = maxWorldHeight

  def setScaling(scaling: Scaling): Unit =
    this.scaling = scaling
}
