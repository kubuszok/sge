/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/viewport/ScalingViewport.java
 * Original authors: Daniel Holderbaum, Nathan Sweet
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

/** A viewport that scales the world using {@link Scaling} . <p> {@link Scaling#fit} keeps the aspect ratio by scaling the world up to fit the screen, adding black bars (letterboxing) for the
  * remaining space. <p> {@link Scaling#fill} keeps the aspect ratio by scaling the world up to take the whole screen (some of the world may be off screen). <p> {@link Scaling#stretch} does not keep
  * the aspect ratio, the world is scaled to take the whole screen. <p> {@link Scaling#none} keeps the aspect ratio by using a fixed size world (the world may not fill the screen or some of the world
  * may be off screen).
  * @author
  *   Daniel Holderbaum
  * @author
  *   Nathan Sweet
  */
class ScalingViewport(scaling: Scaling, worldWidth: Float, worldHeight: Float, camera: Camera)(using Sge) extends Viewport {
  private var _scaling: Scaling = scaling

  setWorldSize(worldWidth, worldHeight)
  setCamera(camera)

  /** Creates a new viewport using a new {@link OrthographicCamera}. */
  def this(scaling: Scaling, worldWidth: Float, worldHeight: Float)(using Sge) =
    this(scaling, worldWidth, worldHeight, new OrthographicCamera())

  override def update(screenWidth: Int, screenHeight: Int, centerCamera: Boolean): Unit = {
    val scaled         = _scaling.apply(getWorldWidth(), getWorldHeight(), screenWidth.toFloat, screenHeight.toFloat)
    val viewportWidth  = Math.round(scaled.x)
    val viewportHeight = Math.round(scaled.y)

    // Center.
    setScreenBounds((screenWidth - viewportWidth) / 2, (screenHeight - viewportHeight) / 2, viewportWidth, viewportHeight)

    apply(centerCamera)
  }

  def getScaling(): Scaling =
    _scaling

  def setScaling(scaling: Scaling): Unit =
    this._scaling = scaling
}
