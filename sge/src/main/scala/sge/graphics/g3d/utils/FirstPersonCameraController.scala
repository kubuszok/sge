/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/utils/FirstPersonCameraController.java
 * Original authors: badlogic
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   - extends InputAdapter -> extends InputProcessor (trait in SGE)
 *   - IntIntMap -> scala.collection.mutable.Map[Key, Key]
 *   - Gdx.input/Gdx.graphics -> Sge().input/Sge().graphics (using Sge)
 *   - direction.rotate(up, angle) -> direction.rotateAroundDeg(up, angle) (renamed in SGE Vector3)
 *   - keys.containsKey -> keys.contains (Scala Map API)
 *   - All methods (keyDown, keyUp, touchDragged, update, setVelocity, setDegreesPerPixel) ported
 *   - Convention: Int key params → opaque Key type
 *   - Audit: pass (2026-03-03)
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 115
 * Covenant-baseline-methods: FirstPersonCameraController,autoUpdate,backwardKey,camera,degreesPerPixel,deltaX,deltaY,downKey,dt,forwardKey,keyDown,keyUp,keys,setDegreesPerPixel,setVelocity,strafeLeftKey,strafeRightKey,tmp,touchDragged,upKey,update,velocity
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/utils/FirstPersonCameraController.java
 * Covenant-verified: 2026-04-19
 */
package sge
package graphics
package g3d
package utils

import scala.collection.mutable

import sge.Input.Key
import sge.graphics.Camera
import sge.math.Vector3
import sge.utils.Seconds

/** Takes a {@link Camera} instance and controls it via w,a,s,d and mouse panning.
  * @author
  *   badlogic
  */
class FirstPersonCameraController(
  protected val camera: Camera
)(using Sge)
    extends InputProcessor {

  protected val keys:            mutable.Map[Key, Key] = mutable.Map.empty
  var strafeLeftKey:             Key                   = Input.Keys.A
  var strafeRightKey:            Key                   = Input.Keys.D
  var forwardKey:                Key                   = Input.Keys.W
  var backwardKey:               Key                   = Input.Keys.S
  var upKey:                     Key                   = Input.Keys.Q
  var downKey:                   Key                   = Input.Keys.E
  var autoUpdate:                Boolean               = true
  protected var velocity:        Float                 = 5f
  protected var degreesPerPixel: Float                 = 0.5f
  protected val tmp:             Vector3               = Vector3()

  override def keyDown(keycode: Key): Boolean = {
    keys.put(keycode, keycode)
    true
  }

  override def keyUp(keycode: Key): Boolean = {
    keys.remove(keycode)
    true
  }

  /** Sets the velocity in units per second for moving forward, backward and strafing left/right.
    * @param velocity
    *   the velocity in units per second
    */
  def setVelocity(velocity: Float): Unit =
    this.velocity = velocity

  /** Sets how many degrees to rotate per pixel the mouse moved.
    * @param degreesPerPixel
    */
  def setDegreesPerPixel(degreesPerPixel: Float): Unit =
    this.degreesPerPixel = degreesPerPixel

  override def touchDragged(screenX: Pixels, screenY: Pixels, pointer: Int): Boolean = {
    val deltaX = -Sge().input.deltaX.toFloat * degreesPerPixel
    val deltaY = -Sge().input.deltaY.toFloat * degreesPerPixel
    camera.direction.rotateAroundDeg(camera.up, deltaX)
    tmp.set(camera.direction).crs(camera.up).nor()
    camera.direction.rotateAroundDeg(tmp, deltaY)
    true
  }

  def update(): Unit =
    update(Sge().graphics.deltaTime)

  def update(deltaTime: Seconds): Unit = {
    val dt = deltaTime.toFloat
    if (keys.contains(forwardKey)) {
      tmp.set(camera.direction).nor().scl(dt * velocity)
      camera.position.add(tmp)
    }
    if (keys.contains(backwardKey)) {
      tmp.set(camera.direction).nor().scl(-dt * velocity)
      camera.position.add(tmp)
    }
    if (keys.contains(strafeLeftKey)) {
      tmp.set(camera.direction).crs(camera.up).nor().scl(-dt * velocity)
      camera.position.add(tmp)
    }
    if (keys.contains(strafeRightKey)) {
      tmp.set(camera.direction).crs(camera.up).nor().scl(dt * velocity)
      camera.position.add(tmp)
    }
    if (keys.contains(upKey)) {
      tmp.set(camera.up).nor().scl(dt * velocity)
      camera.position.add(tmp)
    }
    if (keys.contains(downKey)) {
      tmp.set(camera.up).nor().scl(-dt * velocity)
      camera.position.add(tmp)
    }
    if (autoUpdate) camera.update(true)
  }
}
