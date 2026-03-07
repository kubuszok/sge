/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/ParticleEffect.java
 * Original authors: inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - All public methods ported faithfully
 * - Fixes (2026-03-04): getControllers() removed → public var controllers;
 *   getBoundingBox() → def boundingBox (with _boundingBox backing field)
 * - Disposable → AutoCloseable; dispose() → close()
 * - update() overload uses (using Sge) context parameter instead of Gdx.graphics.getDeltaTime()
 * - findController returns Nullable[ParticleController] instead of ParticleController|null
 * - bounds: Nullable[BoundingBox] instead of BoundingBox|null
 * - setBatch uses boundary/break for inner break (Java labeled break)
 * - Java string equals() → Scala == (structural equality)
 * - Convention: opaque Seconds for update(deltaTime) param
 */
package sge
package graphics
package g3d
package particles

import scala.util.boundary
import scala.util.boundary.break

import sge.Sge
import sge.assets.AssetManager
import sge.graphics.g3d.particles.batches.ParticleBatch
import sge.utils.DynamicArray
import sge.math.{ Matrix4, Quaternion, Vector3 }
import sge.math.collision.BoundingBox
import sge.utils.{ Nullable, Seconds }

/** It's a set of particles controllers. It can be updated, rendered, transformed which means the changes will be applied on all the particles controllers.
  * @author
  *   inferno
  */
class ParticleEffect()(using Sge) extends AutoCloseable with ResourceData.Configurable {

  var controllers:          DynamicArray[ParticleController] = DynamicArray[ParticleController](3)
  private var _boundingBox: Nullable[BoundingBox]            = Nullable.empty

  def this(effect: ParticleEffect)(using Sge) = {
    this()
    controllers = DynamicArray[ParticleController](effect.controllers.size)
    var i = 0
    val n = effect.controllers.size
    while (i < n) {
      controllers.add(effect.controllers(i).copy())
      i += 1
    }
  }

  def this(emitters: ParticleController*)(using Sge) = {
    this()
    this.controllers = DynamicArray[ParticleController](emitters.length)
    for (e <- emitters) this.controllers.add(e)
  }

  def init(): Unit = {
    var i = 0
    val n = controllers.size
    while (i < n) {
      controllers(i).init()
      i += 1
    }
  }

  def start(): Unit = {
    var i = 0
    val n = controllers.size
    while (i < n) {
      controllers(i).start()
      i += 1
    }
  }

  def end(): Unit = {
    var i = 0
    val n = controllers.size
    while (i < n) {
      controllers(i).end()
      i += 1
    }
  }

  def reset(): Unit = {
    var i = 0
    val n = controllers.size
    while (i < n) {
      controllers(i).reset()
      i += 1
    }
  }

  def update(): Unit = {
    var i = 0
    val n = controllers.size
    while (i < n) {
      controllers(i).update()
      i += 1
    }
  }

  def update(deltaTime: Seconds): Unit = {
    var i = 0
    val n = controllers.size
    while (i < n) {
      controllers(i).update(deltaTime)
      i += 1
    }
  }

  def draw(): Unit = {
    var i = 0
    val n = controllers.size
    while (i < n) {
      controllers(i).draw()
      i += 1
    }
  }

  def isComplete: Boolean = boundary {
    var i = 0
    val n = controllers.size
    while (i < n) {
      if (!controllers(i).isComplete) break(false)
      i += 1
    }
    true
  }

  /** Sets the given transform matrix on each controller. */
  def setTransform(transform: Matrix4): Unit = {
    var i = 0
    val n = controllers.size
    while (i < n) {
      controllers(i).setTransform(transform)
      i += 1
    }
  }

  /** Applies the rotation to the current transformation matrix of each controller. */
  def rotate(rotation: Quaternion): Unit = {
    var i = 0
    val n = controllers.size
    while (i < n) {
      controllers(i).rotate(rotation)
      i += 1
    }
  }

  /** Applies the rotation by the given angle around the given axis to the current transformation matrix of each controller.
    * @param axis
    *   the rotation axis
    * @param angle
    *   the rotation angle in degrees
    */
  def rotate(axis: Vector3, angle: Float): Unit = {
    var i = 0
    val n = controllers.size
    while (i < n) {
      controllers(i).rotate(axis, angle)
      i += 1
    }
  }

  /** Applies the translation to the current transformation matrix of each controller. */
  def translate(translation: Vector3): Unit = {
    var i = 0
    val n = controllers.size
    while (i < n) {
      controllers(i).translate(translation)
      i += 1
    }
  }

  /** Applies the scale to the current transformation matrix of each controller. */
  def scale(scaleX: Float, scaleY: Float, scaleZ: Float): Unit = {
    var i = 0
    val n = controllers.size
    while (i < n) {
      controllers(i).scale(scaleX, scaleY, scaleZ)
      i += 1
    }
  }

  /** Applies the scale to the current transformation matrix of each controller. */
  def scale(scale: Vector3): Unit = {
    var i = 0
    val n = controllers.size
    while (i < n) {
      controllers(i).scale(scale.x, scale.y, scale.z)
      i += 1
    }
  }

  /** Returns the controller with the specified name, or null. */
  def findController(name: String): Nullable[ParticleController] = boundary {
    var i = 0
    val n = controllers.size
    while (i < n) {
      val emitter = controllers(i)
      if (emitter.name == name) break(Nullable(emitter))
      i += 1
    }
    Nullable.empty
  }

  override def close(): Unit = {
    var i = 0
    val n = controllers.size
    while (i < n) {
      controllers(i).dispose()
      i += 1
    }
  }

  /** @return the merged bounding box of all controllers. */
  def boundingBox: BoundingBox = {
    if (_boundingBox.isEmpty) _boundingBox = Nullable(BoundingBox())

    _boundingBox.foreach { b =>
      b.inf()
      for (emitter <- controllers)
        b.ext(emitter.boundingBox)
    }
    _boundingBox.getOrElse(BoundingBox())
  }

  /** Assign one batch, among those passed in, to each controller. The batch must be compatible with the controller to be assigned.
    */
  def setBatch(batches: DynamicArray[ParticleBatch[?]]): Unit =
    for (controller <- controllers)
      boundary {
        for (batch <- batches)
          if (controller.renderer.setBatch(batch)) break(())
      }

  /** @return a copy of this effect, should be used after the particle effect has been loaded. */
  def copy(): ParticleEffect =
    ParticleEffect(this)

  /** Saves all the assets required by all the controllers inside this effect. */
  override def save(assetManager: AssetManager, data: ResourceData[?]): Unit =
    for (controller <- controllers)
      controller.save(assetManager, data)

  /** Loads all the assets required by all the controllers inside this effect. */
  override def load(assetManager: AssetManager, data: ResourceData[?]): Unit =
    for (controller <- controllers)
      controller.load(assetManager, data)
}
