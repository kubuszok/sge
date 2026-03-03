/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/emitters/Emitter.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - All public methods ported faithfully
 * - Json.Serializable (write/read): not implemented (JSON serialization deferred)
 * - Extends ParticleControllerComponent (which maps Disposable → AutoCloseable)
 */
package sge
package graphics
package g3d
package particles
package emitters

/** An {@link Emitter} is a {@link ParticleControllerComponent} which will handle the particles emission. It must update the {@link Emitter#percent} to reflect the current percentage of the current
  * emission cycle. It should consider {@link Emitter#minParticleCount} and {@link Emitter#maxParticleCount} to rule particle emission. It should notify the particle controller when particles are
  * activated, killed, or when an emission cycle begins.
  * @author
  *   Inferno
  */
abstract class Emitter extends ParticleControllerComponent {

  /** The min/max quantity of particles */
  var minParticleCount: Int = 0
  var maxParticleCount: Int = 4

  /** Current state of the emission, should be currentTime/ duration Must be updated on each update */
  var percent: Float = 0f

  def this(regularEmitter: Emitter) = {
    this()
    set(regularEmitter)
  }

  override def init(): Unit =
    controller.particles.size = 0

  override def end(): Unit =
    controller.particles.size = 0

  def isComplete(): Boolean =
    percent >= 1.0f

  def getMinParticleCount(): Int =
    minParticleCount

  def setMinParticleCount(minParticleCount: Int): Unit =
    this.minParticleCount = minParticleCount

  def getMaxParticleCount(): Int =
    maxParticleCount

  def setMaxParticleCount(maxParticleCount: Int): Unit =
    this.maxParticleCount = maxParticleCount

  def setParticleCount(aMin: Int, aMax: Int): Unit = {
    setMinParticleCount(aMin)
    setMaxParticleCount(aMax)
  }

  def set(emitter: Emitter): Unit = {
    minParticleCount = emitter.minParticleCount
    maxParticleCount = emitter.maxParticleCount
  }
}
