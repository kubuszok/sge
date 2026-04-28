/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/viewport/Viewport.java
 * Original authors: Daniel Holderbaum, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: Gdx.graphics -> Sge().graphics
 *   Convention: int-to-float widening done explicitly via .toFloat
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * --- AUDIT (2026-03-03) ---
 * API-complete: YES — all methods and fields ported
 * Behavioural parity: YES — logic identical
 * Conventions: OK — no return, no null, split packages, braces on multiline defs
 * Notes:
 *   - toScreenCoordinates uses worldCoords.set(tmp.x, tmp.y) instead of direct field
 *     assignment; functionally equivalent
 *   - Java int-to-float widening done explicitly via .toFloat (correct)
 *   - Sge() accessor used in place of Gdx.graphics (correct)
 *   Idiom: Java-style getters/setters converted to public vars (camera, worldWidth/Height, screenX/Y/Width/Height)
 * Idiom: opaque Pixels for update(screenWidth, screenHeight), screenX/Y/Width/Height
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 200
 * Covenant-baseline-methods: Viewport,apply,bottomGutterHeight,calculateScissors,camera,getPickRay,leftGutterWidth,project,rightGutterWidth,rightGutterX,screenHeight,screenWidth,screenX,screenY,setScreenBounds,setScreenPosition,setScreenSize,setWorldSize,tmp,toScreenCoordinates,topGutterHeight,topGutterY,unproject,update,worldHeight,worldWidth
 * Covenant-source-reference: com/badlogic/gdx/utils/viewport/Viewport.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 282f266f14146cbde64d9b1c6732fe3240739bcb
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
import sge.scenes.scene2d.utils.ScissorStack
import sge.Sge

/** Manages a {@link Camera} and determines how world coordinates are mapped to and from the screen.
  * @author
  *   Daniel Holderbaum
  * @author
  *   Nathan Sweet
  */
abstract class Viewport(using Sge) {
  var camera:       Camera     = scala.compiletime.uninitialized
  var worldWidth:   WorldUnits = scala.compiletime.uninitialized
  var worldHeight:  WorldUnits = scala.compiletime.uninitialized
  var screenX:      Pixels     = Pixels.zero
  var screenY:      Pixels     = Pixels.zero
  var screenWidth:  Pixels     = Pixels.zero
  var screenHeight: Pixels     = Pixels.zero

  private val tmp = Vector3()

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
    if (centerCamera) camera.position.set((worldWidth / 2f).toFloat, (worldHeight / 2f).toFloat, 0)
    camera.update()
  }

  /** Calls {@link #update(int, int, boolean)} with false. */
  final def update(screenWidth: Pixels, screenHeight: Pixels): Unit =
    update(screenWidth, screenHeight, false)

  /** Configures this viewport's screen bounds using the specified screen size and calls {@link #apply(boolean)} . Typically called from {@link ApplicationListener#resize(int, int)} or
    * {@link Screen#resize(int, int)} . <p> The default implementation only calls {@link #apply(boolean)} .
    */
  def update(screenWidth: Pixels, screenHeight: Pixels, centerCamera: Boolean): Unit =
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
  def calculateScissors(batchTransform: Matrix4, area: Rectangle, scissor: Rectangle): Unit =
    ScissorStack.calculateScissors(camera, screenX.toFloat, screenY.toFloat, screenWidth.toFloat, screenHeight.toFloat, batchTransform, area, scissor)

  /** Transforms a point to real screen coordinates (as opposed to OpenGL ES window coordinates), where the origin is in the top left and the the y-axis is pointing downwards.
    */
  def toScreenCoordinates(worldCoords: Vector2, transformMatrix: Matrix4): Vector2 = {
    tmp.set(worldCoords.x, worldCoords.y, 0)
    tmp.mul(transformMatrix)
    camera.project(tmp, screenX.toFloat, screenY.toFloat, screenWidth.toFloat, screenHeight.toFloat)
    tmp.y = Sge().graphics.height.toFloat - tmp.y
    worldCoords.set(tmp.x, tmp.y)
    worldCoords
  }

  def setWorldSize(worldWidth: WorldUnits, worldHeight: WorldUnits): Unit = {
    this.worldWidth = worldWidth
    this.worldHeight = worldHeight
  }

  /** Sets the viewport's position in screen coordinates. This is typically set by {@link #update(int, int, boolean)}. */
  def setScreenPosition(screenX: Pixels, screenY: Pixels): Unit = {
    this.screenX = screenX
    this.screenY = screenY
  }

  /** Sets the viewport's size in screen coordinates. This is typically set by {@link #update(int, int, boolean)}. */
  def setScreenSize(screenWidth: Pixels, screenHeight: Pixels): Unit = {
    this.screenWidth = screenWidth
    this.screenHeight = screenHeight
  }

  /** Sets the viewport's bounds in screen coordinates. This is typically set by {@link #update(int, int, boolean)}. */
  def setScreenBounds(screenX: Pixels, screenY: Pixels, screenWidth: Pixels, screenHeight: Pixels): Unit = {
    this.screenX = screenX
    this.screenY = screenY
    this.screenWidth = screenWidth
    this.screenHeight = screenHeight
  }

  /** Returns the left gutter (black bar) width in screen coordinates. */
  def leftGutterWidth: Pixels =
    screenX

  /** Returns the right gutter (black bar) x in screen coordinates. */
  def rightGutterX: Pixels =
    screenX + screenWidth

  /** Returns the right gutter (black bar) width in screen coordinates. */
  def rightGutterWidth: Pixels =
    Sge().graphics.width - (screenX + screenWidth)

  /** Returns the bottom gutter (black bar) height in screen coordinates. */
  def bottomGutterHeight: Pixels =
    screenY

  /** Returns the top gutter (black bar) y in screen coordinates. */
  def topGutterY: Pixels =
    screenY + screenHeight

  /** Returns the top gutter (black bar) height in screen coordinates. */
  def topGutterHeight: Pixels =
    Sge().graphics.height - (screenY + screenHeight)
}
