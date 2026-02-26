/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/emitters/RegularEmitter.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d
package particles
package emitters

import scala.util.boundary
import scala.util.boundary.break

import sge.graphics.g3d.particles.ParallelArray.FloatChannel
import sge.graphics.g3d.particles.ParticleChannels
import sge.graphics.g3d.particles.ParticleControllerComponent
import sge.graphics.g3d.particles.values.{ RangedNumericValue, ScaledNumericValue }

/** It's a generic use {@link Emitter} which fits most of the particles simulation scenarios.
  * @author
  *   Inferno
  */
class RegularEmitter extends Emitter {
  import RegularEmitter.EmissionMode

  var delayValue:      RangedNumericValue = new RangedNumericValue()
  var durationValue:   RangedNumericValue = new RangedNumericValue()
  var lifeOffsetValue: ScaledNumericValue = new ScaledNumericValue()
  var lifeValue:       ScaledNumericValue = new ScaledNumericValue()
  var emissionValue:   ScaledNumericValue = new ScaledNumericValue()

  protected var emission:       Int          = 0
  protected var emissionDiff:   Int          = 0
  protected var emissionDelta:  Int          = 0
  protected var lifeOffset:     Int          = 0
  protected var lifeOffsetDiff: Int          = 0
  protected var life:           Int          = 0
  protected var lifeDiff:       Int          = 0
  protected var duration:       Float        = 0f
  protected var delay:          Float        = 0f
  protected var durationTimer:  Float        = 0f
  protected var delayTimer:     Float        = 0f
  private var continuous:       Boolean      = true
  private var emissionMode:     EmissionMode = EmissionMode.Enabled

  private var lifeChannel: FloatChannel = scala.compiletime.uninitialized

  durationValue.setActive(true)
  emissionValue.setActive(true)
  lifeValue.setActive(true)

  def this(regularEmitter: RegularEmitter) = {
    this()
    set(regularEmitter)
  }

  override def allocateChannels(): Unit =
    lifeChannel = controller.particles.addChannel(ParticleChannels.Life)

  override def start(): Unit = {
    delay = if (delayValue.active) delayValue.newLowValue() else 0
    delayTimer = 0
    durationTimer = 0f

    duration = durationValue.newLowValue()
    percent = durationTimer / duration

    emission = emissionValue.newLowValue().toInt
    emissionDiff = emissionValue.newHighValue().toInt
    if (!emissionValue.isRelative()) emissionDiff -= emission

    life = lifeValue.newLowValue().toInt
    lifeDiff = lifeValue.newHighValue().toInt
    if (!lifeValue.isRelative()) lifeDiff -= life

    lifeOffset = if (lifeOffsetValue.active) lifeOffsetValue.newLowValue().toInt else 0
    lifeOffsetDiff = lifeOffsetValue.newHighValue().toInt
    if (!lifeOffsetValue.isRelative()) lifeOffsetDiff -= lifeOffset
  }

  override def init(): Unit = {
    super.init()
    emissionDelta = 0
    durationTimer = duration
  }

  override def activateParticles(startIndex: Int, count: Int): Unit = {
    val currentTotalLife = life + (lifeDiff * lifeValue.getScale(percent)).toInt
    var currentLife      = currentTotalLife
    var offsetTime       = (lifeOffset + lifeOffsetDiff * lifeOffsetValue.getScale(percent)).toInt
    if (offsetTime > 0) {
      if (offsetTime >= currentLife) offsetTime = currentLife - 1
      currentLife -= offsetTime
    }
    val lifePercent = 1 - currentLife / currentTotalLife.toFloat

    var i = startIndex * lifeChannel.strideSize
    val c = i + count * lifeChannel.strideSize
    while (i < c) {
      lifeChannel.floatData(i + ParticleChannels.CurrentLifeOffset) = currentLife.toFloat
      lifeChannel.floatData(i + ParticleChannels.TotalLifeOffset) = currentTotalLife.toFloat
      lifeChannel.floatData(i + ParticleChannels.LifePercentOffset) = lifePercent
      i += lifeChannel.strideSize
    }
  }

  override def update(): Unit = {
    val deltaMillis = controller.deltaTime * 1000

    if (delayTimer < delay) {
      delayTimer += deltaMillis
    } else {
      var emit = emissionMode != EmissionMode.Disabled
      // End check
      if (durationTimer < duration) {
        durationTimer += deltaMillis
        percent = durationTimer / duration
      } else {
        if (continuous && emit && emissionMode == EmissionMode.Enabled)
          controller.start()
        else
          emit = false
      }

      if (emit) {
        // Emit particles
        emissionDelta += deltaMillis.toInt
        val emissionTime = emission + emissionDiff * emissionValue.getScale(percent)
        if (emissionTime > 0) {
          val emissionPeriod = 1000 / emissionTime
          if (emissionDelta >= emissionPeriod) {
            var emitCount = (emissionDelta / emissionPeriod).toInt
            emitCount = Math.min(emitCount, maxParticleCount - controller.particles.size)
            emissionDelta -= (emitCount * emissionPeriod).toInt
            emissionDelta = (emissionDelta % emissionPeriod).toInt
            addParticles(emitCount)
          }
        }
        if (controller.particles.size < minParticleCount) addParticles(minParticleCount - controller.particles.size)
      }
    }

    // Update particles
    val activeParticles = controller.particles.size
    var i               = 0
    var k               = 0
    while (i < controller.particles.size) {
      lifeChannel.floatData(k + ParticleChannels.CurrentLifeOffset) -= deltaMillis
      if (lifeChannel.floatData(k + ParticleChannels.CurrentLifeOffset) <= 0) {
        controller.particles.removeElement(i)
        // don't increment i, the swapped element is now at position i
      } else {
        lifeChannel.floatData(k + ParticleChannels.LifePercentOffset) = 1 -
          lifeChannel.floatData(k + ParticleChannels.CurrentLifeOffset) /
          lifeChannel.floatData(k + ParticleChannels.TotalLifeOffset)
        i += 1
        k += lifeChannel.strideSize
      }
    }

    if (controller.particles.size < activeParticles) {
      controller.killParticles(controller.particles.size, activeParticles - controller.particles.size)
    }
  }

  private def addParticles(count: Int): Unit = {
    val actualCount = Math.min(count, maxParticleCount - controller.particles.size)
    if (actualCount <= 0) {
      // nothing to add
    } else {
      controller.activateParticles(controller.particles.size, actualCount)
      controller.particles.size += actualCount
    }
  }

  def getLife():       ScaledNumericValue = lifeValue
  def getEmission():   ScaledNumericValue = emissionValue
  def getDuration():   RangedNumericValue = durationValue
  def getDelay():      RangedNumericValue = delayValue
  def getLifeOffset(): ScaledNumericValue = lifeOffsetValue

  def isContinuous():                     Boolean = continuous
  def setContinuous(continuous: Boolean): Unit    = this.continuous = continuous

  /** Gets current emission mode.
    * @return
    *   Current emission mode.
    */
  def getEmissionMode(): EmissionMode = emissionMode

  /** Sets emission mode. Emission mode does not affect already emitted particles.
    * @param emissionMode
    *   Emission mode to set.
    */
  def setEmissionMode(emissionMode: EmissionMode): Unit = this.emissionMode = emissionMode

  override def isComplete(): Boolean =
    if (delayTimer < delay) false
    else durationTimer >= duration && controller.particles.size == 0

  def getPercentComplete(): Float =
    if (delayTimer < delay) 0
    else Math.min(1, durationTimer / duration)

  def set(emitter: RegularEmitter): Unit = {
    super.set(emitter)
    delayValue.load(emitter.delayValue)
    durationValue.load(emitter.durationValue)
    lifeOffsetValue.load(emitter.lifeOffsetValue)
    lifeValue.load(emitter.lifeValue)
    emissionValue.load(emitter.emissionValue)
    emission = emitter.emission
    emissionDiff = emitter.emissionDiff
    emissionDelta = emitter.emissionDelta
    lifeOffset = emitter.lifeOffset
    lifeOffsetDiff = emitter.lifeOffsetDiff
    life = emitter.life
    lifeDiff = emitter.lifeDiff
    duration = emitter.duration
    delay = emitter.delay
    durationTimer = emitter.durationTimer
    delayTimer = emitter.delayTimer
    continuous = emitter.continuous
  }

  override def copy(): ParticleControllerComponent =
    new RegularEmitter(this)
}

object RegularEmitter {

  /** Possible emission modes. Emission mode does not affect already emitted particles. */
  enum EmissionMode {

    /** New particles can be emitted. */
    case Enabled

    /** Only valid for continuous emitters. It will only emit particles until the end of the effect duration. After that emission cycle will not be restarted.
      */
    case EnabledUntilCycleEnd

    /** Prevents new particle emission. */
    case Disabled
  }
}
