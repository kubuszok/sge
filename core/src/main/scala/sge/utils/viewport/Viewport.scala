/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/viewport/Viewport.java
 * Original authors: Daniel Holderbaum, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package utils
package viewport

import sge.graphics.Camera
import sge.math.Matrix4
import sge.math.Rectangle
import sge.math.Vector2
import sge.math.Vector3
import sge.math.collision.Ray
import sge.graphics.glutils.HdpiUtils
import sge.Sge

/** Manages a {@link Camera} and determines how world coordinates are mapped to and from the screen.
  * @author
  *   Daniel Holderbaum
  * @author
  *   Nathan Sweet
  */
abstract class Viewport(using sge: Sge) {
  private var camera:       Camera = scala.compiletime.uninitialized
  private var worldWidth:   Float  = scala.compiletime.uninitialized
  private var worldHeight:  Float  = scala.compiletime.uninitialized
  private var screenX:      Int    = scala.compiletime.uninitialized
  private var screenY:      Int    = scala.compiletime.uninitialized
  private var screenWidth:  Int    = scala.compiletime.uninitialized
  private var screenHeight: Int    = scala.compiletime.uninitialized

  private val tmp = new Vector3()

  /** Calls {@link #apply(boolean)} with false. */
  def apply(): Unit =
    apply(false)

  /** Applies the viewport to the camera and sets the glViewport.
    * @param centerCamera
    *   If true, the camera position is set to the center of the world.
    */
  def apply(centerCamera: Boolean): Unit = {
    HdpiUtils.glViewport(screenX, screenY, screenWidth, screenHeight)
    camera.viewportWidth = worldWidth
    camera.viewportHeight = worldHeight
    if (centerCamera) camera.position.set(worldWidth / 2, worldHeight / 2, 0)
    camera.update()
  }

  /** Calls {@link #update(int, int, boolean)} with false. */
  final def update(screenWidth: Int, screenHeight: Int): Unit =
    update(screenWidth, screenHeight, false)

  /** Configures this viewport's screen bounds using the specified screen size and calls {@link #apply(boolean)} . Typically called from {@link ApplicationListener#resize(int, int)} or
    * {@link Screen#resize(int, int)} . <p> The default implementation only calls {@link #apply(boolean)} .
    */
  def update(screenWidth: Int, screenHeight: Int, centerCamera: Boolean): Unit =
    apply(centerCamera)

  /** Transforms the specified touch coordinate to world coordinates. The x- and y-coordinate of vec are assumed to be in touch coordinates (origin is the top left corner, y * pointing down, x
    * pointing to the right)
    * @return
    *   The vector that was passed in, transformed to world coordinates.
    * @see
    *   Camera#unproject(Vector3)
    */
  def unproject(touchCoords: Vector2): Vector2 = {
    tmp.set(touchCoords.x, touchCoords.y, 1)
    camera.unproject(tmp, screenX.toFloat, screenY.toFloat, screenWidth.toFloat, screenHeight.toFloat)
    touchCoords.set(tmp.x, tmp.y)
    touchCoords
  }

  /** Transforms the specified world coordinate to screen coordinates.
    * @return
    *   The vector that was passed in, transformed to screen coordinates.
    * @see
    *   Camera#project(Vector3)
    */
  def project(worldCoords: Vector2): Vector2 = {
    tmp.set(worldCoords.x, worldCoords.y, 1)
    camera.project(tmp, screenX.toFloat, screenY.toFloat, screenWidth.toFloat, screenHeight.toFloat)
    worldCoords.set(tmp.x, tmp.y)
    worldCoords
  }

  /** Transforms the specified screen coordinate to world coordinates.
    * @return
    *   The vector that was passed in, transformed to world coordinates.
    * @see
    *   Camera#unproject(Vector3)
    */
  def unproject(screenCoords: Vector3): Vector3 = {
    camera.unproject(screenCoords, screenX.toFloat, screenY.toFloat, screenWidth.toFloat, screenHeight.toFloat)
    screenCoords
  }

  /** Transforms the specified world coordinate to screen coordinates.
    * @return
    *   The vector that was passed in, transformed to screen coordinates.
    * @see
    *   Camera#project(Vector3)
    */
  def project(worldCoords: Vector3): Vector3 = {
    camera.project(worldCoords, screenX.toFloat, screenY.toFloat, screenWidth.toFloat, screenHeight.toFloat)
    worldCoords
  }

  /** @see Camera#getPickRay(float, float, float, float, float, float) */
  def getPickRay(touchX: Float, touchY: Float): Ray =
    camera.getPickRay(touchX, touchY, this.screenX.toFloat, this.screenY.toFloat, screenWidth.toFloat, screenHeight.toFloat)

  /** @see ScissorStack#calculateScissors(Camera, float, float, float, float, Matrix4, Rectangle, Rectangle) */
  // TODO: Convert to proper Scala when ScissorStack equivalent is available
  // def calculateScissors(batchTransform: Matrix4, area: Rectangle, scissor: Rectangle): Unit = {
  //   ScissorStack.calculateScissors(camera, screenX, screenY, screenWidth, screenHeight, batchTransform, area, scissor)
  // }

  /** Transforms a point to real screen coordinates (as opposed to OpenGL ES window coordinates), where the origin is in the top left and the the y-axis is pointing downwards.
    */
  // TODO: Convert to proper Scala syntax
  def toScreenCoordinates(worldCoords: Vector2, transformMatrix: Matrix4): Vector2 = {
    tmp.set(worldCoords.x, worldCoords.y, 0)
    tmp.mul(transformMatrix)
    camera.project(tmp, screenX.toFloat, screenY.toFloat, screenWidth.toFloat, screenHeight.toFloat)
    tmp.y = sge.graphics.getHeight() - tmp.y
    worldCoords.x = tmp.x
    worldCoords.y = tmp.y
    worldCoords
  }

  def getCamera(): Camera =
    camera

  def setCamera(camera: Camera): Unit =
    this.camera = camera

  def getWorldWidth(): Float =
    worldWidth

  /** The virtual width of this viewport in world coordinates. This width is scaled to the viewport's screen width. */
  def setWorldWidth(worldWidth: Float): Unit =
    this.worldWidth = worldWidth

  def getWorldHeight(): Float =
    worldHeight

  /** The virtual height of this viewport in world coordinates. This height is scaled to the viewport's screen height. */
  def setWorldHeight(worldHeight: Float): Unit =
    this.worldHeight = worldHeight

  def setWorldSize(worldWidth: Float, worldHeight: Float): Unit = {
    this.worldWidth = worldWidth
    this.worldHeight = worldHeight
  }

  def getScreenX(): Int =
    screenX

  /** Sets the viewport's offset from the left edge of the screen. This is typically set by {@link #update(int, int, boolean)} .
    */
  def setScreenX(screenX: Int): Unit =
    this.screenX = screenX

  def getScreenY(): Int =
    screenY

  /** Sets the viewport's offset from the bottom edge of the screen. This is typically set by {@link #update(int, int, boolean)} .
    */
  def setScreenY(screenY: Int): Unit =
    this.screenY = screenY

  def getScreenWidth(): Int =
    screenWidth

  /** Sets the viewport's width in screen coordinates. This is typically set by {@link #update(int, int, boolean)}. */
  def setScreenWidth(screenWidth: Int): Unit =
    this.screenWidth = screenWidth

  def getScreenHeight(): Int =
    screenHeight

  /** Sets the viewport's height in screen coordinates. This is typically set by {@link #update(int, int, boolean)}. */
  def setScreenHeight(screenHeight: Int): Unit =
    this.screenHeight = screenHeight

  /** Sets the viewport's position in screen coordinates. This is typically set by {@link #update(int, int, boolean)}. */
  def setScreenPosition(screenX: Int, screenY: Int): Unit = {
    this.screenX = screenX
    this.screenY = screenY
  }

  /** Sets the viewport's size in screen coordinates. This is typically set by {@link #update(int, int, boolean)}. */
  def setScreenSize(screenWidth: Int, screenHeight: Int): Unit = {
    this.screenWidth = screenWidth
    this.screenHeight = screenHeight
  }

  /** Sets the viewport's bounds in screen coordinates. This is typically set by {@link #update(int, int, boolean)}. */
  def setScreenBounds(screenX: Int, screenY: Int, screenWidth: Int, screenHeight: Int): Unit = {
    this.screenX = screenX
    this.screenY = screenY
    this.screenWidth = screenWidth
    this.screenHeight = screenHeight
  }

  /** Returns the left gutter (black bar) width in screen coordinates. */
  def getLeftGutterWidth(): Int =
    screenX

  /** Returns the right gutter (black bar) x in screen coordinates. */
  def getRightGutterX(): Int =
    screenX + screenWidth

  /** Returns the right gutter (black bar) width in screen coordinates. */
  def getRightGutterWidth(): Int =
    sge.graphics.getWidth() - (screenX + screenWidth)

  /** Returns the bottom gutter (black bar) height in screen coordinates. */
  def getBottomGutterHeight(): Int =
    screenY

  /** Returns the top gutter (black bar) y in screen coordinates. */
  def getTopGutterY(): Int =
    screenY + screenHeight

  /** Returns the top gutter (black bar) height in screen coordinates. */
  def getTopGutterHeight(): Int =
    sge.graphics.getHeight() - (screenY + screenHeight)
}
