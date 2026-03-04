/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/ParticleController.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - All public methods ported faithfully
 * - Fixes (2026-03-04): getBoundingBox() → def boundingBox (with _boundingBox backing field)
 * - Json.Serializable (write/read): not implemented (JSON serialization deferred)
 * - update() overload uses (using Sge) context parameter instead of Gdx.graphics.getDeltaTime()
 * - dispose() calls emitter.close() / influencer.close() (Disposable → AutoCloseable mapping)
 * - findInfluencer returns Nullable[K] instead of K|null
 * - boundingBox: Nullable[BoundingBox] instead of BoundingBox|null
 * - getBoundingBox javadoc says "copy of controller" but returns bounding box (matches Java)
 * - ClassReflection.isAssignableFrom → direct Class.isAssignableFrom
 * - DEFAULT_TIME_STEP: companion object protected val (Java protected static final)
 * - TODO: opaque Seconds for deltaTime, deltaTimeSqr, update(deltaTime), setTimeStep params -- see docs/improvements/opaque-types.md
 */
package sge
package graphics
package g3d
package particles

import scala.util.boundary
import scala.util.boundary.break

import sge.Sge
import sge.assets.AssetManager
import sge.graphics.g3d.particles.ParallelArray.FloatChannel
import sge.utils.DynamicArray
import sge.graphics.g3d.particles.emitters.Emitter
import sge.graphics.g3d.particles.influencers.Influencer
import sge.graphics.g3d.particles.renderers.ParticleControllerRenderer
import sge.math.{ Matrix4, Quaternion, Vector3 }
import sge.math.collision.BoundingBox
import sge.utils.Nullable

/** Base class of all the particle controllers. Encapsulate the generic structure of a controller and methods to update the particles simulation.
  * @author
  *   Inferno
  */
class ParticleController()(using Sge) extends ResourceData.Configurable {

  /** Name of the controller */
  var name: String = scala.compiletime.uninitialized

  /** Controls the emission of the particles */
  var emitter: Emitter = scala.compiletime.uninitialized

  /** Update the properties of the particles */
  var influencers: DynamicArray[Influencer] = DynamicArray[Influencer](3)

  /** Controls the graphical representation of the particles */
  var renderer: ParticleControllerRenderer[?, ?] = scala.compiletime.uninitialized

  /** Particles components */
  var particles:        ParallelArray    = scala.compiletime.uninitialized
  var particleChannels: ParticleChannels = scala.compiletime.uninitialized

  /** Current transform of the controller DO NOT CHANGE MANUALLY */
  var transform: Matrix4 = Matrix4()

  /** Transform flags */
  var scale: Vector3 = Vector3(1, 1, 1)

  /** Not used by the simulation, it should represent the bounding box containing all the particles */
  protected var _boundingBox: Nullable[BoundingBox] = Nullable.empty

  /** Time step, DO NOT CHANGE MANUALLY */
  var deltaTime:    Float = 0f
  var deltaTimeSqr: Float = 0f

  setTimeStep(ParticleController.DEFAULT_TIME_STEP)

  def this(name: String, emitter: Emitter, renderer: ParticleControllerRenderer[?, ?], influencers: Influencer*)(using Sge) = {
    this()
    this.name = name
    this.emitter = emitter
    this.renderer = renderer
    this.particleChannels = ParticleChannels()
    this.influencers = DynamicArray[Influencer](influencers.length)
    for (inf <- influencers)
      this.influencers.add(inf)
  }

  /** Sets the delta used to step the simulation */
  private def setTimeStep(timeStep: Float): Unit = {
    deltaTime = timeStep
    deltaTimeSqr = deltaTime * deltaTime
  }

  /** Sets the current transformation to the given one.
    * @param transform
    *   the new transform matrix
    */
  def setTransform(transform: Matrix4): Unit = {
    this.transform.set(transform)
    transform.getScale(scale)
  }

  /** Sets the current transformation. */
  def setTransform(x: Float, y: Float, z: Float, qx: Float, qy: Float, qz: Float, qw: Float, scale: Float): Unit = {
    transform.set(x, y, z, qx, qy, qz, qw, scale, scale, scale)
    this.scale.set(scale, scale, scale)
  }

  /** Post-multiplies the current transformation with a rotation matrix represented by the given quaternion. */
  def rotate(rotation: Quaternion): Unit =
    this.transform.rotate(rotation)

  /** Post-multiplies the current transformation with a rotation matrix by the given angle around the given axis.
    * @param axis
    *   the rotation axis
    * @param angle
    *   the rotation angle in degrees
    */
  def rotate(axis: Vector3, angle: Float): Unit =
    this.transform.rotate(axis, angle)

  /** Postmultiplies the current transformation with a translation matrix represented by the given translation. */
  def translate(translation: Vector3): Unit =
    this.transform.translate(translation)

  def setTranslation(translation: Vector3): Unit =
    this.transform.setTranslation(translation)

  /** Postmultiplies the current transformation with a scale matrix represented by the given scale on x,y,z. */
  def scale(scaleX: Float, scaleY: Float, scaleZ: Float): Unit = {
    this.transform.scale(scaleX, scaleY, scaleZ)
    this.transform.getScale(scale)
  }

  /** Postmultiplies the current transformation with a scale matrix represented by the given scale vector. */
  def scale(scale: Vector3): Unit =
    this.scale(scale.x, scale.y, scale.z)

  /** Postmultiplies the current transformation with the given matrix. */
  def mul(transform: Matrix4): Unit = {
    this.transform.mul(transform)
    this.transform.getScale(scale)
  }

  /** Set the given matrix to the current transformation matrix. */
  def getTransform(transform: Matrix4): Unit =
    transform.set(this.transform)

  def isComplete: Boolean =
    emitter.isComplete

  /** Initialize the controller. All the sub systems will be initialized and binded to the controller. Must be called before any other method.
    */
  def init(): Unit = {
    bind()
    if (Nullable(particles).isDefined) {
      end()
      particleChannels.resetIds()
    }
    allocateChannels(emitter.maxParticleCount)

    emitter.init()
    for (influencer <- influencers)
      influencer.init()
    renderer.init()
  }

  protected def allocateChannels(maxParticleCount: Int): Unit = {
    particles = ParallelArray(maxParticleCount)
    // Alloc additional channels
    emitter.allocateChannels()
    for (influencer <- influencers)
      influencer.allocateChannels()
    renderer.allocateChannels()
  }

  /** Bind the sub systems to the controller Called once during the init phase. */
  protected def bind(): Unit = {
    emitter.set(this)
    for (influencer <- influencers)
      influencer.set(this)
    renderer.set(this)
  }

  /** Start the simulation. */
  def start(): Unit = {
    emitter.start()
    for (influencer <- influencers)
      influencer.start()
  }

  /** Reset the simulation. */
  def reset(): Unit = {
    end()
    start()
  }

  /** End the simulation. */
  def end(): Unit = {
    for (influencer <- influencers)
      influencer.end()
    emitter.end()
  }

  /** Generally called by the Emitter. This method will notify all the sub systems that a given amount of particles has been activated.
    */
  def activateParticles(startIndex: Int, count: Int): Unit = {
    emitter.activateParticles(startIndex, count)
    for (influencer <- influencers)
      influencer.activateParticles(startIndex, count)
  }

  /** Generally called by the Emitter. This method will notify all the sub systems that a given amount of particles has been killed.
    */
  def killParticles(startIndex: Int, count: Int): Unit = {
    emitter.killParticles(startIndex, count)
    for (influencer <- influencers)
      influencer.killParticles(startIndex, count)
  }

  /** Updates the particles data */
  def update(): Unit =
    update(Sge().graphics.getDeltaTime())

  /** Updates the particles data */
  def update(deltaTime: Float): Unit = {
    setTimeStep(deltaTime)
    emitter.update()
    for (influencer <- influencers)
      influencer.update()
  }

  /** Updates the renderer used by this controller, usually this means the particles will be draw inside a batch. */
  def draw(): Unit =
    if (particles.size > 0) {
      renderer.update()
    }

  /** @return a copy of this controller */
  def copy(): ParticleController = {
    val copiedEmitter     = this.emitter.copy().asInstanceOf[Emitter]
    val copiedRenderer    = this.renderer.copy().asInstanceOf[ParticleControllerRenderer[?, ?]]
    val copiedInfluencers = DynamicArray[Influencer](this.influencers.size)
    var ci                = 0
    while (ci < this.influencers.size) {
      copiedInfluencers.add(this.influencers(ci).copy().asInstanceOf[Influencer])
      ci += 1
    }
    ParticleController(new String(this.name), copiedEmitter, copiedRenderer, copiedInfluencers.toArray.toSeq*)
  }

  def dispose(): Unit = {
    emitter.close()
    for (influencer <- influencers)
      influencer.close()
  }

  /** @return the bounding box of this controller, should be used after the particle effect has been loaded. */
  def boundingBox: BoundingBox = {
    if (_boundingBox.isEmpty) _boundingBox = Nullable(BoundingBox())
    calculateBoundingBox()
    _boundingBox.getOrElse(throw new IllegalStateException("boundingBox"))
  }

  /** Updates the bounding box using the position channel. */
  protected def calculateBoundingBox(): Unit = {
    val bb = _boundingBox.getOrElse(throw new IllegalStateException("boundingBox"))
    bb.clr()
    val positionChannel: FloatChannel =
      particles.getChannel[FloatChannel](ParticleChannels.Position).getOrElse(throw new IllegalStateException("positionChannel"))
    var pos = 0
    val c   = positionChannel.strideSize * particles.size
    while (pos < c) {
      bb.ext(
        positionChannel.floatData(pos + ParticleChannels.XOffset),
        positionChannel.floatData(pos + ParticleChannels.YOffset),
        positionChannel.floatData(pos + ParticleChannels.ZOffset)
      )
      pos += positionChannel.strideSize
    }
  }

  /** @return the index of the Influencer of the given type. */
  private def findIndex[K <: Influencer](using tag: scala.reflect.ClassTag[K]): Int = boundary {
    var i = 0
    while (i < influencers.size) {
      val influencer = influencers(i)
      if (tag.runtimeClass.isAssignableFrom(influencer.getClass)) {
        break(i)
      }
      i += 1
    }
    -1
  }

  /** @return the influencer having the given type. */
  def findInfluencer[K <: Influencer](using tag: scala.reflect.ClassTag[K]): Nullable[K] = {
    val index = findIndex[K]
    if (index > -1) Nullable(influencers(index).asInstanceOf[K])
    else Nullable.empty
  }

  /** Removes the Influencer of the given type. */
  def removeInfluencer[K <: Influencer](using tag: scala.reflect.ClassTag[K]): Unit = {
    val index = findIndex[K]
    if (index > -1) influencers.removeIndex(index)
  }

  /** Replaces the Influencer of the given type with the one passed as parameter. */
  def replaceInfluencer[K <: Influencer](newInfluencer: K)(using tag: scala.reflect.ClassTag[K]): Boolean = {
    val index = findIndex[K]
    if (index > -1) {
      influencers.insert(index, newInfluencer)
      influencers.removeIndex(index + 1)
      true
    } else false
  }

  override def save(manager: AssetManager, data: ResourceData[?]): Unit = {
    emitter.save(manager, data)
    for (influencer <- influencers)
      influencer.save(manager, data)
    renderer.save(manager, data)
  }

  override def load(manager: AssetManager, data: ResourceData[?]): Unit = {
    emitter.load(manager, data)
    for (influencer <- influencers)
      influencer.load(manager, data)
    renderer.load(manager, data)
  }
}

object ParticleController {

  /** the default time step used to update the simulation */
  protected val DEFAULT_TIME_STEP: Float = 1f / 60
}
