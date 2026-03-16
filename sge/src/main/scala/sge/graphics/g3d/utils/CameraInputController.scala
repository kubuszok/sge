/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/utils/CameraInputController.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   - extends GestureDetector: uses named parameter `listener = gestureListener`
 *   - CameraGestureListener: static protected class -> companion object protected class
 *   - Gdx.graphics -> Sge().graphics (using Sge)
 *   - MathUtils.isPowerOfTwo: same name, ported
 *   - All fields, overrides (touchDown, touchUp, touchDragged, scrolled, keyDown, keyUp) ported
 *   - process(), zoom(), pinchZoom(), update(), setInvertedControls() ported
 *   - Convention: Int key/button params → opaque Key/Button types
 *   - Audit: pass (2026-03-03)
 */
package sge
package graphics
package g3d
package utils

import sge.Input.{ Button, Key }
import sge.graphics.Camera
import sge.input.GestureDetector
import sge.math.{ MathUtils, Vector2, Vector3 }

class CameraInputController protected (
  val gestureListener: CameraInputController.CameraGestureListener,
  var camera:          Camera
)(using Sge)
    extends GestureDetector(listener = gestureListener) {

  gestureListener.controller = this

  /** The button for rotating the camera. */
  var rotateButton: Button = Input.Buttons.LEFT

  /** The angle to rotate when moved the full width or height of the screen. */
  var rotateAngle: Float = 360f

  /** The button for translating the camera along the up/right plane */
  var translateButton: Button = Input.Buttons.RIGHT

  /** The units to translate the camera when moved the full width or height of the screen. */
  var translateUnits: Float = 10f // FIXME auto calculate this based on the target
  /** The button for translating the camera along the direction axis */
  var forwardButton: Button = Input.Buttons.MIDDLE

  /** The key which must be pressed to activate rotate, translate and forward or 0 to always activate. */
  var activateKey: Key = Key(0)

  /** Indicates if the activateKey is currently being pressed. */
  protected var activatePressed: Boolean = false

  /** Whether scrolling requires the activeKey to be pressed (false) or always allow scrolling (true). */
  var alwaysScroll: Boolean = true

  /** The weight for each scrolled amount. */
  var scrollFactor: Float = -0.1f

  /** World units per screen size */
  var pinchZoomFactor: Float = 10f

  /** Whether to update the camera after it has been changed. */
  var autoUpdate: Boolean = true

  /** The target to rotate around. */
  var target: Vector3 = Vector3()

  /** Whether to update the target on translation */
  var translateTarget: Boolean = true

  /** Whether to update the target on forward */
  var forwardTarget: Boolean = true

  /** Whether to update the target on scroll */
  var scrollTarget:                 Boolean = false
  var forwardKey:                   Key     = Input.Keys.W
  protected var forwardPressed:     Boolean = false
  var backwardKey:                  Key     = Input.Keys.S
  protected var backwardPressed:    Boolean = false
  var rotateRightKey:               Key     = Input.Keys.A
  protected var rotateRightPressed: Boolean = false
  var rotateLeftKey:                Key     = Input.Keys.D
  protected var rotateLeftPressed:  Boolean = false
  protected var controlsInverted:   Boolean = false

  /** The current (first) button being pressed. */
  protected var button: Button = Button(-1)

  private var startX: Float   = 0f
  private var startY: Float   = 0f
  private val tmpV1:  Vector3 = Vector3()
  private val tmpV2:  Vector3 = Vector3()

  def this(camera: Camera)(using Sge) = {
    this(CameraInputController.CameraGestureListener(), camera)
  }

  def update(): Unit =
    if (rotateRightPressed || rotateLeftPressed || forwardPressed || backwardPressed) {
      val delta = Sge().graphics.getDeltaTime()
      if (rotateRightPressed) camera.rotate(camera.up, -delta * rotateAngle)
      if (rotateLeftPressed) camera.rotate(camera.up, delta * rotateAngle)
      if (forwardPressed) {
        camera.translate(tmpV1.set(camera.direction).scl(delta * translateUnits))
        if (forwardTarget) target.add(tmpV1)
      }
      if (backwardPressed) {
        camera.translate(tmpV1.set(camera.direction).scl(-delta * translateUnits))
        if (forwardTarget) target.add(tmpV1)
      }
      if (autoUpdate) camera.update()
    }

  private var touched:    Int     = 0
  private var multiTouch: Boolean = false

  override def touchDown(screenX: Pixels, screenY: Pixels, pointer: Int, button: Button): Boolean = {
    touched |= (1 << pointer)
    multiTouch = !MathUtils.isPowerOfTwo(touched)
    if (multiTouch)
      this.button = Button(-1)
    else if (this.button.toInt < 0 && (activateKey == Key(0) || activatePressed)) {
      startX = screenX.toFloat
      startY = screenY.toFloat
      this.button = button
    }
    super.touchDown(screenX, screenY, pointer, button) || (activateKey == Key(0) || activatePressed)
  }

  override def touchUp(screenX: Pixels, screenY: Pixels, pointer: Int, button: Button): Boolean = {
    touched &= -1 ^ (1 << pointer)
    multiTouch = !MathUtils.isPowerOfTwo(touched)
    if (button == this.button) this.button = Button(-1)
    super.touchUp(screenX, screenY, pointer, button) || activatePressed
  }

  /** Sets the CameraInputControllers' control inversion.
    * @param invertControls
    *   Whether or not to invert the controls
    */
  def setInvertedControls(invertControls: Boolean): Unit = {
    if (this.controlsInverted != invertControls) {
      // Flip the rotation angle
      this.rotateAngle = -this.rotateAngle
    }
    this.controlsInverted = invertControls
  }

  protected def process(deltaX: Float, deltaY: Float, button: Button): Boolean = {
    if (button == rotateButton) {
      tmpV1.set(camera.direction).crs(camera.up).y = 0f
      camera.rotateAround(target, tmpV1.nor(), deltaY * rotateAngle)
      camera.rotateAround(target, Vector3.Y, deltaX * -rotateAngle)
    } else if (button == translateButton) {
      camera.translate(tmpV1.set(camera.direction).crs(camera.up).nor().scl(-deltaX * translateUnits))
      camera.translate(tmpV2.set(camera.up).scl(-deltaY * translateUnits))
      if (translateTarget) target.add(tmpV1).add(tmpV2)
    } else if (button == forwardButton) {
      camera.translate(tmpV1.set(camera.direction).scl(deltaY * translateUnits))
      if (forwardTarget) target.add(tmpV1)
    }
    if (autoUpdate) camera.update()
    true
  }

  override def touchDragged(screenX: Pixels, screenY: Pixels, pointer: Int): Boolean = {
    val result = super.touchDragged(screenX, screenY, pointer)
    if (result || this.button.toInt < 0) result
    else {
      val deltaX = (screenX.toFloat - startX) / Sge().graphics.getWidth().toFloat
      val deltaY = (startY - screenY.toFloat) / Sge().graphics.getHeight().toFloat
      startX = screenX.toFloat
      startY = screenY.toFloat
      process(deltaX, deltaY, button)
    }
  }

  override def scrolled(amountX: Float, amountY: Float): Boolean =
    zoom(amountY * scrollFactor * translateUnits)

  def zoom(amount: Float): Boolean =
    if (!alwaysScroll && activateKey != Key(0) && !activatePressed) false
    else {
      camera.translate(tmpV1.set(camera.direction).scl(amount))
      if (scrollTarget) target.add(tmpV1)
      if (autoUpdate) camera.update()
      true
    }

  protected def pinchZoom(amount: Float): Boolean =
    zoom(pinchZoomFactor * amount)

  override def keyDown(keycode: Key): Boolean = {
    if (keycode == activateKey) activatePressed = true
    if (keycode == forwardKey)
      forwardPressed = true
    else if (keycode == backwardKey)
      backwardPressed = true
    else if (keycode == rotateRightKey)
      rotateRightPressed = true
    else if (keycode == rotateLeftKey) rotateLeftPressed = true
    false
  }

  override def keyUp(keycode: Key): Boolean = {
    if (keycode == activateKey) {
      activatePressed = false
      button = Button(-1)
    }
    if (keycode == forwardKey)
      forwardPressed = false
    else if (keycode == backwardKey)
      backwardPressed = false
    else if (keycode == rotateRightKey)
      rotateRightPressed = false
    else if (keycode == rotateLeftKey) rotateLeftPressed = false
    false
  }
}

object CameraInputController {

  protected class CameraGestureListener(using Sge) extends GestureDetector.GestureAdapter {
    var controller:           CameraInputController = scala.compiletime.uninitialized
    private var previousZoom: Float                 = 0f

    override def touchDown(x: Float, y: Float, pointer: Int, button: Button): Boolean = {
      previousZoom = 0
      false
    }

    override def tap(x: Float, y: Float, count: Int, button: Button): Boolean = false

    override def longPress(x: Float, y: Float): Boolean = false

    override def fling(velocityX: Float, velocityY: Float, button: Button): Boolean = false

    override def pan(x: Float, y: Float, deltaX: Float, deltaY: Float): Boolean = false

    override def zoom(initialDistance: Float, distance: Float): Boolean = {
      val newZoom = distance - initialDistance
      val amount  = newZoom - previousZoom
      previousZoom = newZoom
      val w = Sge().graphics.getWidth().toFloat
      val h = Sge().graphics.getHeight().toFloat
      controller.pinchZoom(amount / (if (w > h) h else w))
    }

    override def pinch(initialPointer1: Vector2, initialPointer2: Vector2, pointer1: Vector2, pointer2: Vector2): Boolean = false
  }
}
