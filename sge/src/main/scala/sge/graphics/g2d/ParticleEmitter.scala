/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g2d/ParticleEmitter.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: allowCompletion() -> allowCompletionMethod() to avoid clash with allowCompletion field
 *   Convention: Java enums -> Scala 3 enums; boundary/break for early returns; Nullable for null safety
 *   Idiom: boundary/break, Nullable, split packages
 *   Fixes: Raw null on line 710 removed (dead code — sprites guaranteed non-empty by line 705)
 *   Fixes: Java-style getters/setters → Scala property accessors
 *   Fixes: Inner class redundant getters/setters removed (ParticleValue, NumericValue, RangedNumericValue, ScaledNumericValue, IndependentScaledNumericValue, GradientColorValue, SpawnShapeValue)
 *   Improvement: opaque Seconds for timing values in numeric value objects -- see docs/improvements/opaque-types.md
 *   Issue: test: needs .p fixture file to test ParticleEmitter.load end-to-end
 *   Audited: 2026-03-04
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g2d

import sge.graphics.GL20
import sge.math.MathUtils
import sge.math.collision.BoundingBox

import sge.utils.{ DynamicArray, Nullable, Seconds }

import scala.language.implicitConversions

import java.io.BufferedReader
import java.io.IOException
import java.io.Writer

class ParticleEmitter {
  import ParticleEmitter._

  private val delayValue        = RangedNumericValue()
  private val lifeOffsetValue   = IndependentScaledNumericValue()
  private val durationValue     = RangedNumericValue()
  private val lifeValue         = IndependentScaledNumericValue()
  private val emissionValue     = ScaledNumericValue()
  private val xScaleValue       = ScaledNumericValue()
  private val yScaleValue       = ScaledNumericValue()
  private val rotationValue     = ScaledNumericValue()
  private val velocityValue     = ScaledNumericValue()
  private val angleValue        = ScaledNumericValue()
  private val windValue         = ScaledNumericValue()
  private val gravityValue      = ScaledNumericValue()
  private val transparencyValue = ScaledNumericValue()
  private val tintValue         = GradientColorValue()
  private val xOffsetValue      = ScaledNumericValue()
  private val yOffsetValue      = ScaledNumericValue()
  private val spawnWidthValue   = ScaledNumericValue()
  private val spawnHeightValue  = ScaledNumericValue()
  private val spawnShapeValue   = SpawnShapeValue()

  private var _xSizeValues:  Array[RangedNumericValue] = scala.compiletime.uninitialized
  private var _ySizeValues:  Array[RangedNumericValue] = scala.compiletime.uninitialized
  private var _motionValues: Array[RangedNumericValue] = scala.compiletime.uninitialized

  private var accumulator:       Float                = 0f
  private var _sprites:          DynamicArray[Sprite] = scala.compiletime.uninitialized
  var spriteMode:                SpriteMode           = SpriteMode.single
  private var _particles:        Array[Particle]      = scala.compiletime.uninitialized
  var minParticleCount:          Int                  = 0
  private var _maxParticleCount: Int                  = 4
  private var _x:                Float                = 0f
  private var _y:                Float                = 0f
  var name:                      String               = scala.compiletime.uninitialized
  private var _imagePaths:       DynamicArray[String] = scala.compiletime.uninitialized
  private var _activeCount:      Int                  = 0
  private var _active:           Array[Boolean]       = scala.compiletime.uninitialized
  private var firstUpdate:       Boolean              = false
  private var flipX:             Boolean              = false
  private var flipY:             Boolean              = false
  private var updateFlags:       Int                  = 0
  var allowCompletion:           Boolean              = false
  private var _bounds:           BoundingBox          = scala.compiletime.uninitialized

  private var emission:        Int   = 0
  private var emissionDiff:    Int   = 0
  private var emissionDelta:   Float = 0f
  private var lifeOffset:      Int   = 0
  private var lifeOffsetDiff:  Int   = 0
  private var life:            Int   = 0
  private var lifeDiff:        Int   = 0
  private var spawnWidth:      Float = 0f
  private var spawnWidthDiff:  Float = 0f
  private var spawnHeight:     Float = 0f
  private var spawnHeightDiff: Float = 0f
  var duration:                Float = 1f
  var durationTimer:           Float = 0f
  private var delay:           Float = 0f
  private var delayTimer:      Float = 0f

  var attached:              Boolean = false
  var continuous:            Boolean = false
  var aligned:               Boolean = false
  var behind:                Boolean = false
  var additive:              Boolean = true
  var premultipliedAlpha:    Boolean = false
  var cleansUpBlendFunction: Boolean = true

  // Initialize in primary constructor
  initialize()

  def this(reader: BufferedReader) = {
    this()
    initialize()
    load(reader)
  }

  def this(emitter: ParticleEmitter) = {
    this()
    _sprites = DynamicArray.from(emitter._sprites)
    name = emitter.name
    _imagePaths = DynamicArray.from(emitter._imagePaths)
    maxParticleCount = emitter._maxParticleCount
    minParticleCount = emitter.minParticleCount
    delayValue.load(emitter.delayValue)
    durationValue.load(emitter.durationValue)
    emissionValue.load(emitter.emissionValue)
    lifeValue.load(emitter.lifeValue)
    lifeOffsetValue.load(emitter.lifeOffsetValue)
    xScaleValue.load(emitter.xScaleValue)
    yScaleValue.load(emitter.yScaleValue)
    rotationValue.load(emitter.rotationValue)
    velocityValue.load(emitter.velocityValue)
    angleValue.load(emitter.angleValue)
    windValue.load(emitter.windValue)
    gravityValue.load(emitter.gravityValue)
    transparencyValue.load(emitter.transparencyValue)
    tintValue.load(emitter.tintValue)
    xOffsetValue.load(emitter.xOffsetValue)
    yOffsetValue.load(emitter.yOffsetValue)
    spawnWidthValue.load(emitter.spawnWidthValue)
    spawnHeightValue.load(emitter.spawnHeightValue)
    spawnShapeValue.load(emitter.spawnShapeValue)
    attached = emitter.attached
    continuous = emitter.continuous
    aligned = emitter.aligned
    behind = emitter.behind
    additive = emitter.additive
    premultipliedAlpha = emitter.premultipliedAlpha
    cleansUpBlendFunction = emitter.cleansUpBlendFunction
    spriteMode = emitter.spriteMode
    setPosition(emitter._x, emitter._y)
  }

  private def initialize(): Unit = {
    _sprites = DynamicArray[Sprite]()
    _imagePaths = DynamicArray[String]()
    durationValue.alwaysActive = true
    emissionValue.alwaysActive = true
    lifeValue.alwaysActive = true
    xScaleValue.alwaysActive = true
    transparencyValue.alwaysActive = true
    spawnShapeValue.alwaysActive = true
    spawnWidthValue.alwaysActive = true
    spawnHeightValue.alwaysActive = true
  }

  def maxParticleCount: Int = _maxParticleCount

  def maxParticleCount_=(maxParticleCount: Int): Unit = {
    this._maxParticleCount = maxParticleCount
    _active = new Array[Boolean](maxParticleCount)
    _activeCount = 0
    _particles = new Array[Particle](maxParticleCount)
  }

  def addParticle(): Unit = scala.util.boundary {
    val activeCount = this._activeCount
    if (activeCount == _maxParticleCount) scala.util.boundary.break()
    val active = this._active
    for (i <- active.indices)
      if (!active(i)) {
        activateParticle(i)
        active(i) = true
        this._activeCount = activeCount + 1
        scala.util.boundary.break()
      }
  }

  def addParticles(count: Int): Unit = scala.util.boundary {
    val actualCount = Math.min(count, _maxParticleCount - _activeCount)
    if (actualCount == 0) scala.util.boundary.break(())
    val active = this._active
    var index  = 0
    val n      = active.length
    var i      = 0
    while (i < actualCount) {
      var found = false
      while (index < n && !found)
        if (!active(index)) {
          activateParticle(index)
          active(index) = true
          index += 1
          found = true
        } else {
          index += 1
        }
      if (!found) scala.util.boundary.break(())
      i += 1
    }
    this._activeCount += actualCount
  }

  def update(delta: Seconds): Unit = {
    accumulator += delta.toFloat * 1000
    if (accumulator >= 1) {
      val deltaMillis = accumulator.toInt
      accumulator -= deltaMillis

      if (delayTimer < delay) {
        delayTimer += deltaMillis
      } else {
        scala.util.boundary {
          if (firstUpdate) {
            firstUpdate = false
            addParticle()
          }

          if (durationTimer < duration)
            durationTimer += deltaMillis
          else {
            if (!continuous || allowCompletion)
              scala.util.boundary.break(())
            else
              restart()
          }

          emissionDelta += deltaMillis
          var emissionTime = emission + emissionDiff * emissionValue.getScale(durationTimer / duration)
          if (emissionTime > 0) {
            emissionTime = 1000 / emissionTime
            if (emissionDelta >= emissionTime) {
              var emitCount = (emissionDelta / emissionTime).toInt
              emitCount = Math.min(emitCount, _maxParticleCount - this._activeCount)
              emissionDelta -= emitCount * emissionTime
              emissionDelta %= emissionTime
              addParticles(emitCount)
            }
          }
          if (this._activeCount < minParticleCount) addParticles(minParticleCount - this._activeCount)
        }
      }

      val active      = this._active
      var activeCount = this._activeCount
      val particles   = this._particles
      for (i <- active.indices)
        if (active(i) && !updateParticle(particles(i), delta, deltaMillis)) {
          active(i) = false
          activeCount -= 1
        }
      this._activeCount = activeCount
    }
  }

  def draw(batch: Batch): Unit = {
    if (premultipliedAlpha) {
      batch.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA)
    } else if (additive) {
      batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE)
    } else {
      batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
    }
    val particles = this._particles
    val active    = this._active

    for (i <- active.indices)
      if (active(i)) particles(i).draw(batch)

    if (cleansUpBlendFunction && (additive || premultipliedAlpha))
      batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
  }

  /** Updates and draws the particles. This is slightly more efficient than calling {@link #update(float)} and {@link #draw(Batch)} separately.
    */
  def draw(batch: Batch, delta: Seconds): Unit = scala.util.boundary {
    accumulator += delta.toFloat * 1000
    if (accumulator < 1) {
      draw(batch)
      scala.util.boundary.break(())
    }
    val deltaMillis = accumulator.toInt
    accumulator -= deltaMillis

    if (premultipliedAlpha) {
      batch.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA)
    } else if (additive) {
      batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE)
    } else {
      batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
    }

    val particles   = this._particles
    val active      = this._active
    var activeCount = this._activeCount
    for (i <- active.indices)
      if (active(i)) {
        val particle = particles(i)
        if (updateParticle(particle, delta, deltaMillis))
          particle.draw(batch)
        else {
          active(i) = false
          activeCount -= 1
        }
      }
    this._activeCount = activeCount

    if (cleansUpBlendFunction && (additive || premultipliedAlpha))
      batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

    if (delayTimer < delay) {
      delayTimer += deltaMillis
      scala.util.boundary.break(())
    }

    if (firstUpdate) {
      firstUpdate = false
      addParticle()
    }

    if (durationTimer < duration)
      durationTimer += deltaMillis
    else {
      if (!continuous || allowCompletion) scala.util.boundary.break(())
      restart()
    }

    emissionDelta += deltaMillis
    var emissionTime = emission + emissionDiff * emissionValue.getScale(durationTimer / duration)
    if (emissionTime > 0) {
      emissionTime = 1000 / emissionTime
      if (emissionDelta >= emissionTime) {
        var emitCount = (emissionDelta / emissionTime).toInt
        emitCount = Math.min(emitCount, _maxParticleCount - activeCount)
        emissionDelta -= emitCount * emissionTime
        emissionDelta %= emissionTime
        addParticles(emitCount)
      }
    }
    if (activeCount < minParticleCount) addParticles(minParticleCount - activeCount)
  }

  def start(): Unit = {
    firstUpdate = true
    allowCompletion = false
    restart()
  }

  def reset(): Unit =
    reset(startEmitter = true)

  def reset(startEmitter: Boolean): Unit = {
    emissionDelta = 0
    durationTimer = duration
    val active = this._active
    for (i <- active.indices)
      active(i) = false
    _activeCount = 0
    if (startEmitter) start()
  }

  private def restart(): Unit = {
    delay = if (delayValue.isActive) delayValue.newLowValue() else 0
    delayTimer = 0

    durationTimer -= duration
    duration = durationValue.newLowValue()

    emission = emissionValue.newLowValue().toInt
    emissionDiff = emissionValue.newHighValue().toInt
    if (!emissionValue.relative) emissionDiff -= emission

    if (!lifeValue.independent) generateLifeValues()

    if (!lifeOffsetValue.independent) generateLifeOffsetValues()

    spawnWidth = spawnWidthValue.newLowValue()
    spawnWidthDiff = spawnWidthValue.newHighValue()
    if (!spawnWidthValue.relative) spawnWidthDiff -= spawnWidth

    spawnHeight = spawnHeightValue.newLowValue()
    spawnHeightDiff = spawnHeightValue.newHighValue()
    if (!spawnHeightValue.relative) spawnHeightDiff -= spawnHeight

    updateFlags = 0
    if (angleValue.isActive && angleValue.timeline.length > 1) updateFlags |= UPDATE_ANGLE
    if (velocityValue.isActive) updateFlags |= UPDATE_VELOCITY
    if (xScaleValue.timeline.length > 1) updateFlags |= UPDATE_SCALE
    if (yScaleValue.isActive && yScaleValue.timeline.length > 1) updateFlags |= UPDATE_SCALE
    if (rotationValue.isActive && rotationValue.timeline.length > 1) updateFlags |= UPDATE_ROTATION
    if (windValue.isActive) updateFlags |= UPDATE_WIND
    if (gravityValue.isActive) updateFlags |= UPDATE_GRAVITY
    if (tintValue.timeline.length > 1) updateFlags |= UPDATE_TINT
    if (spriteMode == SpriteMode.animated) updateFlags |= UPDATE_SPRITE
  }

  protected def newParticle(sprite: Sprite): Particle =
    Particle(sprite)

  protected def particles: Array[Particle] =
    _particles

  private def activateParticle(index: Int): Unit = {
    var sprite: Nullable[Sprite] = Nullable.empty
    spriteMode match {
      case SpriteMode.single | SpriteMode.animated =>
        if (_sprites.nonEmpty) sprite = Nullable(_sprites.first)
      case SpriteMode.random =>
        if (_sprites.nonEmpty) sprite = Nullable(_sprites(scala.util.Random.nextInt(_sprites.size)))
    }

    var particle  = _particles(index)
    val spriteVal = sprite.getOrElse(throw new IllegalStateException("No sprite available for particle activation"))
    if (Nullable(particle).isEmpty) {
      _particles(index) = newParticle(spriteVal)
      particle = _particles(index)
      particle.flip(flipX, flipY)
    } else {
      particle.set(spriteVal)
    }

    val percent     = durationTimer / duration.toFloat
    val updateFlags = this.updateFlags

    if (lifeValue.independent) generateLifeValues()

    if (lifeOffsetValue.independent) generateLifeOffsetValues()

    particle.currentLife = life + (lifeDiff * lifeValue.getScale(percent)).toInt
    particle.life = particle.currentLife

    if (velocityValue.isActive) {
      particle.velocity = velocityValue.newLowValue()
      particle.velocityDiff = velocityValue.newHighValue()
      if (!velocityValue.relative) particle.velocityDiff -= particle.velocity
    }

    particle.angle = angleValue.newLowValue()
    particle.angleDiff = angleValue.newHighValue()
    if (!angleValue.relative) particle.angleDiff -= particle.angle
    var angle = 0f
    if ((updateFlags & UPDATE_ANGLE) == 0) {
      angle = particle.angle + particle.angleDiff * angleValue.getScale(0)
      particle.angle = angle
      particle.angleCos = MathUtils.cosDeg(angle)
      particle.angleSin = MathUtils.sinDeg(angle)
    }

    val spriteWidth  = sprite.map(_.width).getOrElse(1f)
    val spriteHeight = sprite.map(_.height).getOrElse(1f)

    particle.xScale = xScaleValue.newLowValue() / spriteWidth
    particle.xScaleDiff = xScaleValue.newHighValue() / spriteWidth
    if (!xScaleValue.relative) particle.xScaleDiff -= particle.xScale

    if (yScaleValue.isActive) {
      particle.yScale = yScaleValue.newLowValue() / spriteHeight
      particle.yScaleDiff = yScaleValue.newHighValue() / spriteHeight
      if (!yScaleValue.relative) particle.yScaleDiff -= particle.yScale
      particle.setScale(
        particle.xScale + particle.xScaleDiff * xScaleValue.getScale(0),
        particle.yScale + particle.yScaleDiff * yScaleValue.getScale(0)
      )
    } else {
      particle.setScale(particle.xScale + particle.xScaleDiff * xScaleValue.getScale(0))
    }

    if (rotationValue.isActive) {
      particle.particleRotation = rotationValue.newLowValue()
      particle.rotationDiff = rotationValue.newHighValue()
      if (!rotationValue.relative) particle.rotationDiff -= particle.particleRotation
      val rotation      = particle.particleRotation + particle.rotationDiff * rotationValue.getScale(0)
      val finalRotation = if (aligned) rotation + angle else rotation
      particle.rotation = finalRotation
    }

    if (windValue.isActive) {
      particle.wind = windValue.newLowValue()
      particle.windDiff = windValue.newHighValue()
      if (!windValue.relative) particle.windDiff -= particle.wind
    }

    if (gravityValue.isActive) {
      particle.gravity = gravityValue.newLowValue()
      particle.gravityDiff = gravityValue.newHighValue()
      if (!gravityValue.relative) particle.gravityDiff -= particle.gravity
    }

    var color = particle.tint
    if (Nullable(color).isEmpty) {
      color = new Array[Float](3)
      particle.tint = color
    }
    val temp = tintValue.getColor(0)
    color(0) = temp(0)
    color(1) = temp(1)
    color(2) = temp(2)

    particle.transparency = transparencyValue.newLowValue()
    particle.transparencyDiff = transparencyValue.newHighValue() - particle.transparency

    // Spawn.
    var x = this._x
    if (xOffsetValue.isActive) x += xOffsetValue.newLowValue()
    var y = this._y
    if (yOffsetValue.isActive) y += yOffsetValue.newLowValue()

    spawnShapeValue.shape match {
      case SpawnShape.square =>
        val width  = spawnWidth + (spawnWidthDiff * spawnWidthValue.getScale(percent))
        val height = spawnHeight + (spawnHeightDiff * spawnHeightValue.getScale(percent))
        x += MathUtils.random(width) - width * 0.5f
        y += MathUtils.random(height) - height * 0.5f
      case SpawnShape.ellipse =>
        val width   = spawnWidth + (spawnWidthDiff * spawnWidthValue.getScale(percent))
        val height  = spawnHeight + (spawnHeightDiff * spawnHeightValue.getScale(percent))
        val radiusX = width * 0.5f
        val radiusY = height * 0.5f
        if (radiusX != 0 && radiusY != 0) {
          val scaleY = radiusX / radiusY
          if (spawnShapeValue.edges) {
            val spawnAngle = spawnShapeValue.side match {
              case SpawnEllipseSide.top    => -MathUtils.random(179f)
              case SpawnEllipseSide.bottom => MathUtils.random(179f)
              case _                       => MathUtils.random(360f)
            }
            val cosDeg = MathUtils.cosDeg(spawnAngle)
            val sinDeg = MathUtils.sinDeg(spawnAngle)
            x += cosDeg * radiusX
            y += sinDeg * radiusX / scaleY
            if ((updateFlags & UPDATE_ANGLE) == 0) {
              particle.angle = spawnAngle
              particle.angleCos = cosDeg
              particle.angleSin = sinDeg
            }
          } else {
            val radius2 = radiusX * radiusX
            scala.util.boundary {
              while (true) {
                val px = MathUtils.random(width) - radiusX
                val py = MathUtils.random(width) - radiusX
                if (px * px + py * py <= radius2) {
                  x += px
                  y += py / scaleY
                  scala.util.boundary.break(())
                }
              }
            }
          }
        }
      case SpawnShape.line =>
        val width  = spawnWidth + (spawnWidthDiff * spawnWidthValue.getScale(percent))
        val height = spawnHeight + (spawnHeightDiff * spawnHeightValue.getScale(percent))
        if (width != 0) {
          val lineX = width * MathUtils.random()
          x += lineX
          y += lineX * (height / width)
        } else {
          y += height * MathUtils.random()
        }
      case _ => // point or other shapes - no additional offset
    }

    particle.setBounds(x - spriteWidth * 0.5f, y - spriteHeight * 0.5f, spriteWidth, spriteHeight)

    val offsetTime = (lifeOffset + lifeOffsetDiff * lifeOffsetValue.getScale(percent)).toInt
    if (offsetTime > 0) {
      val adjustedOffsetTime = if (offsetTime >= particle.currentLife) particle.currentLife - 1 else offsetTime
      updateParticle(particle, Seconds(adjustedOffsetTime / 1000f), adjustedOffsetTime)
    }
  }

  private def updateParticle(particle: Particle, delta: Seconds, deltaMillis: Int): Boolean = {
    val life = particle.currentLife - deltaMillis
    if (life <= 0) false
    else {
      particle.currentLife = life

      val percent     = 1 - particle.currentLife / particle.life.toFloat
      val updateFlags = this.updateFlags

      if ((updateFlags & UPDATE_SCALE) != 0) {
        if (yScaleValue.isActive) {
          particle.setScale(
            particle.xScale + particle.xScaleDiff * xScaleValue.getScale(percent),
            particle.yScale + particle.yScaleDiff * yScaleValue.getScale(percent)
          )
        } else {
          particle.setScale(particle.xScale + particle.xScaleDiff * xScaleValue.getScale(percent))
        }
      }

      if ((updateFlags & UPDATE_VELOCITY) != 0) {
        val velocity = (particle.velocity + particle.velocityDiff * velocityValue.getScale(percent)) * delta.toFloat

        val (velocityX, velocityY) = if ((updateFlags & UPDATE_ANGLE) != 0) {
          val angle = particle.angle + particle.angleDiff * angleValue.getScale(percent)
          val vx    = velocity * MathUtils.cosDeg(angle)
          val vy    = velocity * MathUtils.sinDeg(angle)
          if ((updateFlags & UPDATE_ROTATION) != 0) {
            val rotation      = particle.particleRotation + particle.rotationDiff * rotationValue.getScale(percent)
            val finalRotation = if (aligned) rotation + angle else rotation
            particle.rotation = finalRotation
          }
          (vx, vy)
        } else {
          val vx = velocity * particle.angleCos
          val vy = velocity * particle.angleSin
          if (aligned || (updateFlags & UPDATE_ROTATION) != 0) {
            val rotation      = particle.particleRotation + particle.rotationDiff * rotationValue.getScale(percent)
            val finalRotation = if (aligned) rotation + particle.angle else rotation
            particle.rotation = finalRotation
          }
          (vx, vy)
        }

        val finalVelocityX =
          if ((updateFlags & UPDATE_WIND) != 0)
            velocityX + (particle.wind + particle.windDiff * windValue.getScale(percent)) * delta.toFloat
          else velocityX

        val finalVelocityY =
          if ((updateFlags & UPDATE_GRAVITY) != 0)
            velocityY + (particle.gravity + particle.gravityDiff * gravityValue.getScale(percent)) * delta.toFloat
          else velocityY

        particle.translate(finalVelocityX, finalVelocityY)
      } else {
        if ((updateFlags & UPDATE_ROTATION) != 0)
          particle.rotation = particle.particleRotation + particle.rotationDiff * rotationValue.getScale(percent)
      }

      val color =
        if ((updateFlags & UPDATE_TINT) != 0)
          tintValue.getColor(percent)
        else
          particle.tint

      if (premultipliedAlpha) {
        val alphaMultiplier = if (additive) 0f else 1f
        val a               = particle.transparency + particle.transparencyDiff * transparencyValue.getScale(percent)
        particle.setColor(color(0) * a, color(1) * a, color(2) * a, a * alphaMultiplier)
      } else {
        particle.setColor(color(0), color(1), color(2), particle.transparency + particle.transparencyDiff * transparencyValue.getScale(percent))
      }

      if ((updateFlags & UPDATE_SPRITE) != 0) {
        val frame = Math.min((percent * _sprites.size).toInt, _sprites.size - 1)
        if (particle.frame != frame) {
          val sprite           = _sprites(frame)
          val prevSpriteWidth  = particle.width
          val prevSpriteHeight = particle.height
          particle.setRegion(sprite)
          particle.setSize(sprite.width, sprite.height)
          particle.setOrigin(sprite.originX, sprite.originY)
          particle.translate((prevSpriteWidth - sprite.width) * 0.5f, (prevSpriteHeight - sprite.height) * 0.5f)
          particle.frame = frame
        }
      }

      true
    }
  }

  private def generateLifeValues(): Unit = {
    life = lifeValue.newLowValue().toInt
    lifeDiff = lifeValue.newHighValue().toInt
    if (!lifeValue.relative) lifeDiff -= life
  }

  private def generateLifeOffsetValues(): Unit = {
    lifeOffset = if (lifeOffsetValue.isActive) lifeOffsetValue.newLowValue().toInt else 0
    lifeOffsetDiff = lifeOffsetValue.newHighValue().toInt
    if (!lifeOffsetValue.relative) lifeOffsetDiff -= lifeOffset
  }

  def setPosition(x: Float, y: Float): Unit = {
    if (attached) {
      val xAmount = x - this._x
      val yAmount = y - this._y
      val active  = this._active
      for (i <- active.indices)
        if (active(i)) _particles(i).translate(xAmount, yAmount)
    }
    this._x = x
    this._y = y
  }

  def setSprites(sprites: Array[Sprite]): Unit = scala.util.boundary {
    this._sprites = DynamicArray.from(sprites)
    if (sprites.size == 0) scala.util.boundary.break()
    for (i <- _particles.indices) {
      val particle = _particles(i)
      if (Nullable(particle).isEmpty) scala.util.boundary.break()
      val sprite: Nullable[Sprite] = spriteMode match {
        case SpriteMode.single =>
          if (sprites.nonEmpty) Nullable(sprites(0)) else Nullable.empty
        case SpriteMode.random =>
          if (sprites.nonEmpty) Nullable(sprites(scala.util.Random.nextInt(sprites.size))) else Nullable.empty
        case SpriteMode.animated =>
          val percent    = 1 - particle.currentLife / particle.life.toFloat
          val frameIndex = Math.min((percent * sprites.size).toInt, sprites.size - 1)
          particle.frame = frameIndex
          if (sprites.nonEmpty) Nullable(sprites(frameIndex)) else Nullable.empty
      }
      sprite.foreach { s =>
        particle.setRegion(s)
        particle.setOrigin(s.originX, s.originY)
      }
    }
  }

  /** Allocates max particles emitter can hold. Usually called early on to avoid allocation on updates. {@link #setSprites(Array)} must have been set before calling this method
    */
  def preAllocateParticles(): Unit = {
    if (_sprites.isEmpty)
      throw new IllegalStateException("ParticleEmitter.setSprites() must have been called before preAllocateParticles()")
    for (index <- _particles.indices) {
      var particle = _particles(index)
      if (Nullable(particle).isEmpty) {
        _particles(index) = newParticle(_sprites.first)
        particle = _particles(index)
        particle.flip(flipX, flipY)
      }
    }
  }

  /** Ignores the {@link #setContinuous(boolean) continuous} setting until the emitter is started again. This allows the emitter to stop smoothly.
    */
  def allowCompletionMethod(): Unit = {
    // renamed from allowCompletion to allowCompletionMethod to avoid conflict with the method in ParticleEffect
    allowCompletion = true
    durationTimer = duration
  }

  // --- Property accessors (replacing Java-style getters/setters) ---

  def sprites: Array[Sprite] =
    _sprites.toArray

  def x: Float = _x
  def y: Float = _y

  def activeCount: Int = _activeCount

  def isComplete: Boolean =
    if (continuous && !allowCompletion) false
    else if (delayTimer < delay) false
    else durationTimer >= duration && _activeCount == 0

  def percentComplete: Float =
    if (delayTimer < delay) 0
    else Math.min(1, durationTimer / duration)

  def imagePaths: Array[String] =
    Nullable(_imagePaths).map(_.toArray).getOrElse(Array.empty[String])

  def imagePaths_=(imagePaths: Array[String]): Unit =
    this._imagePaths = DynamicArray.from(imagePaths)

  /** Returns the bounding box for all active particles. z axis will always be zero. */
  def boundingBox: BoundingBox = {
    if (Nullable(_bounds).isEmpty) _bounds = BoundingBox()

    val particles   = this._particles
    val active      = this._active
    val boundingBox = this._bounds

    boundingBox.inf()
    for (i <- active.indices)
      if (active(i)) {
        val r = particles(i).boundingRectangle
        boundingBox.ext(r.x, r.y, 0)
        boundingBox.ext(r.x + r.width, r.y + r.height, 0)
      }

    boundingBox
  }

  // --- Value object getters ---

  def lifeVal: ScaledNumericValue =
    lifeValue

  def xScale: ScaledNumericValue =
    xScaleValue

  def yScale: ScaledNumericValue =
    yScaleValue

  def rotation: ScaledNumericValue =
    rotationValue

  def tint: GradientColorValue =
    tintValue

  def velocity: ScaledNumericValue =
    velocityValue

  def wind: ScaledNumericValue =
    windValue

  def gravity: ScaledNumericValue =
    gravityValue

  def angle: ScaledNumericValue =
    angleValue

  def emissionVal: ScaledNumericValue =
    emissionValue

  def transparency: ScaledNumericValue =
    transparencyValue

  def durationVal: RangedNumericValue =
    durationValue

  def delayVal: RangedNumericValue =
    delayValue

  def lifeOffsetVal: ScaledNumericValue =
    lifeOffsetValue

  def xOffset: RangedNumericValue =
    xOffsetValue

  def yOffset: RangedNumericValue =
    yOffsetValue

  def spawnWidthVal: ScaledNumericValue =
    spawnWidthValue

  def spawnHeightVal: ScaledNumericValue =
    spawnHeightValue

  def spawnShape: SpawnShapeValue =
    spawnShapeValue

  // --- Protected accessors ---

  protected def xSizeValues: Array[RangedNumericValue] = {
    if (Nullable(_xSizeValues).isEmpty) {
      _xSizeValues = new Array[RangedNumericValue](3)
      _xSizeValues(0) = xScaleValue
      _xSizeValues(1) = spawnWidthValue
      _xSizeValues(2) = xOffsetValue
    }
    _xSizeValues
  }

  protected def ySizeValues: Array[RangedNumericValue] = {
    if (Nullable(_ySizeValues).isEmpty) {
      _ySizeValues = new Array[RangedNumericValue](3)
      _ySizeValues(0) = yScaleValue
      _ySizeValues(1) = spawnHeightValue
      _ySizeValues(2) = yOffsetValue
    }
    _ySizeValues
  }

  protected def motionValues: Array[RangedNumericValue] = {
    if (Nullable(_motionValues).isEmpty) {
      _motionValues = new Array[RangedNumericValue](3)
      _motionValues(0) = velocityValue
      _motionValues(1) = windValue
      _motionValues(2) = gravityValue
    }
    _motionValues
  }

  // --- Flip and scale methods ---

  def setFlip(flipX: Boolean, flipY: Boolean): Unit = {
    this.flipX = flipX
    this.flipY = flipY
    Nullable(_particles).foreach { ps =>
      for (i <- ps.indices) {
        val particle = ps(i)
        Nullable(particle).foreach(_.flip(flipX, flipY))
      }
    }
  }

  def flipYValues(): Unit = {
    angleValue.setHigh(-angleValue.highMin, -angleValue.highMax)
    angleValue.setLow(-angleValue.lowMin, -angleValue.lowMax)

    gravityValue.setHigh(-gravityValue.highMin, -gravityValue.highMax)
    gravityValue.setLow(-gravityValue.lowMin, -gravityValue.lowMax)

    windValue.setHigh(-windValue.highMin, -windValue.highMax)
    windValue.setLow(-windValue.lowMin, -windValue.lowMax)

    rotationValue.setHigh(-rotationValue.highMin, -rotationValue.highMax)
    rotationValue.setLow(-rotationValue.lowMin, -rotationValue.lowMax)

    yOffsetValue.setLow(-yOffsetValue.lowMin, -yOffsetValue.lowMax)
  }

  /** Permanently scales the size of the emitter by scaling all its ranged values related to size. */
  def scaleSize(scale: Float): Unit =
    if (scale != 1f) {
      scaleSize(scale, scale)
    }

  /** Permanently scales the size of the emitter by scaling all its ranged values related to size. */
  def scaleSize(scaleX: Float, scaleY: Float): Unit =
    if (scaleX != 1f || scaleY != 1f) {
      for (value <- xSizeValues)
        value.scale(scaleX)
      for (value <- ySizeValues)
        value.scale(scaleY)
    }

  /** Permanently scales the speed of the emitter by scaling all its ranged values related to motion. */
  def scaleMotion(scale: Float): Unit =
    if (scale != 1f) {
      for (value <- motionValues)
        value.scale(scale)
    }

  /** Sets all size-related ranged values to match those of the template emitter. */
  def matchSize(template: ParticleEmitter): Unit = {
    matchXSize(template)
    matchYSize(template)
  }

  /** Sets all horizontal size-related ranged values to match those of the template emitter. */
  def matchXSize(template: ParticleEmitter): Unit = {
    val values         = xSizeValues
    val templateValues = template.xSizeValues
    for (i <- values.indices)
      copyValue(values(i), templateValues(i))
  }

  /** Sets all vertical size-related ranged values to match those of the template emitter. */
  def matchYSize(template: ParticleEmitter): Unit = {
    val values         = ySizeValues
    val templateValues = template.ySizeValues
    for (i <- values.indices)
      copyValue(values(i), templateValues(i))
  }

  /** Sets all motion-related ranged values to match those of the template emitter. */
  def matchMotion(template: ParticleEmitter): Unit = {
    val values         = motionValues
    val templateValues = template.motionValues
    for (i <- values.indices)
      copyValue(values(i), templateValues(i))
  }

  private def copyValue(target: RangedNumericValue, source: RangedNumericValue): Unit =
    target.set(source)

  def save(output: Writer): Unit = {
    output.write(name + "\n")
    output.write("- Delay -\n")
    delayValue.save(output)
    output.write("- Duration - \n")
    durationValue.save(output)
    output.write("- Count - \n")
    output.write("min: " + minParticleCount + "\n")
    output.write("max: " + _maxParticleCount + "\n")
    output.write("- Emission - \n")
    emissionValue.save(output)
    output.write("- Life - \n")
    lifeValue.save(output)
    output.write("- Life Offset - \n")
    lifeOffsetValue.save(output)
    output.write("- X Offset - \n")
    xOffsetValue.save(output)
    output.write("- Y Offset - \n")
    yOffsetValue.save(output)
    output.write("- Spawn Shape - \n")
    spawnShapeValue.save(output)
    output.write("- Spawn Width - \n")
    spawnWidthValue.save(output)
    output.write("- Spawn Height - \n")
    spawnHeightValue.save(output)
    output.write("- X Scale - \n")
    xScaleValue.save(output)
    output.write("- Y Scale - \n")
    yScaleValue.save(output)
    output.write("- Velocity - \n")
    velocityValue.save(output)
    output.write("- Angle - \n")
    angleValue.save(output)
    output.write("- Rotation - \n")
    rotationValue.save(output)
    output.write("- Wind - \n")
    windValue.save(output)
    output.write("- Gravity - \n")
    gravityValue.save(output)
    output.write("- Tint - \n")
    tintValue.save(output)
    output.write("- Transparency - \n")
    transparencyValue.save(output)
    output.write("- Options - \n")
    output.write("attached: " + attached + "\n")
    output.write("continuous: " + continuous + "\n")
    output.write("aligned: " + aligned + "\n")
    output.write("additive: " + additive + "\n")
    output.write("behind: " + behind + "\n")
    output.write("premultipliedAlpha: " + premultipliedAlpha + "\n")
    output.write("spriteMode: " + spriteMode.toString + "\n")
    output.write("- Image Paths -\n")
    for (imagePath <- _imagePaths)
      output.write(imagePath + "\n")
    output.write("\n")
  }

  def load(reader: BufferedReader): Unit =
    try {
      name = ParticleEmitter.readString(reader, "name")
      reader.readLine()
      delayValue.load(reader)
      reader.readLine()
      durationValue.load(reader)
      reader.readLine()
      minParticleCount = ParticleEmitter.readInt(reader, "minParticleCount")
      maxParticleCount = ParticleEmitter.readInt(reader, "maxParticleCount")
      reader.readLine()
      emissionValue.load(reader)
      reader.readLine()
      lifeValue.load(reader)
      reader.readLine()
      lifeOffsetValue.load(reader)
      reader.readLine()
      xOffsetValue.load(reader)
      reader.readLine()
      yOffsetValue.load(reader)
      reader.readLine()
      spawnShapeValue.load(reader)
      reader.readLine()
      spawnWidthValue.load(reader)
      reader.readLine()
      spawnHeightValue.load(reader)
      var line = reader.readLine()
      if (line.trim.equals("- Scale -")) {
        xScaleValue.load(reader)
        yScaleValue.active = false
      } else {
        xScaleValue.load(reader)
        reader.readLine()
        yScaleValue.load(reader)
      }
      reader.readLine()
      velocityValue.load(reader)
      reader.readLine()
      angleValue.load(reader)
      reader.readLine()
      rotationValue.load(reader)
      reader.readLine()
      windValue.load(reader)
      reader.readLine()
      gravityValue.load(reader)
      reader.readLine()
      tintValue.load(reader)
      reader.readLine()
      transparencyValue.load(reader)
      reader.readLine()
      attached = ParticleEmitter.readBoolean(reader, "attached")
      continuous = ParticleEmitter.readBoolean(reader, "continuous")
      aligned = ParticleEmitter.readBoolean(reader, "aligned")
      additive = ParticleEmitter.readBoolean(reader, "additive")
      behind = ParticleEmitter.readBoolean(reader, "behind")

      // Backwards compatibility
      line = reader.readLine()
      if (line.startsWith("premultipliedAlpha")) {
        premultipliedAlpha = ParticleEmitter.readBoolean(line)
        line = reader.readLine()
      }
      if (line.startsWith("spriteMode")) {
        spriteMode = SpriteMode.valueOf(ParticleEmitter.readString(line))
        line = reader.readLine()
      }

      val imagePaths  = DynamicArray[String]()
      var currentLine = line
      while (Nullable(currentLine).isDefined && currentLine.nonEmpty) {
        imagePaths.add(currentLine)
        currentLine = reader.readLine()
      }
      this.imagePaths = imagePaths.toArray
    } catch {
      case ex: RuntimeException =>
        if (Nullable(name).isEmpty) throw ex
        throw new RuntimeException("Error parsing emitter: " + name, ex)
    }
}

object ParticleEmitter {
  private val UPDATE_SCALE    = 1 << 0
  private val UPDATE_ANGLE    = 1 << 1
  private val UPDATE_ROTATION = 1 << 2
  private val UPDATE_VELOCITY = 1 << 3
  private val UPDATE_WIND     = 1 << 4
  private val UPDATE_GRAVITY  = 1 << 5
  private val UPDATE_TINT     = 1 << 6
  private val UPDATE_SPRITE   = 1 << 7

  def readString(line: String): String =
    line.substring(line.indexOf(":") + 1).trim

  def readString(reader: BufferedReader, name: String): String = {
    val line = reader.readLine()
    if (Nullable(line).isEmpty) throw new IOException("Missing value: " + name)
    readString(line)
  }

  def readBoolean(line: String): Boolean =
    java.lang.Boolean.parseBoolean(readString(line))

  def readBoolean(reader: BufferedReader, name: String): Boolean =
    java.lang.Boolean.parseBoolean(readString(reader, name))

  def readInt(reader: BufferedReader, name: String): Int =
    java.lang.Integer.parseInt(readString(reader, name))

  def readFloat(reader: BufferedReader, name: String): Float =
    java.lang.Float.parseFloat(readString(reader, name))

  class Particle(sprite: Sprite) extends Sprite(sprite) {
    var life:             Int          = 0
    var currentLife:      Int          = 0
    var xScale:           Float        = 0f
    var xScaleDiff:       Float        = 0f
    var yScale:           Float        = 0f
    var yScaleDiff:       Float        = 0f
    var particleRotation: Float        = 0f
    var rotationDiff:     Float        = 0f
    var velocity:         Float        = 0f
    var velocityDiff:     Float        = 0f
    var angle:            Float        = 0f
    var angleDiff:        Float        = 0f
    var angleCos:         Float        = 0f
    var angleSin:         Float        = 0f
    var transparency:     Float        = 0f
    var transparencyDiff: Float        = 0f
    var wind:             Float        = 0f
    var windDiff:         Float        = 0f
    var gravity:          Float        = 0f
    var gravityDiff:      Float        = 0f
    var tint:             Array[Float] = scala.compiletime.uninitialized
    var frame:            Int          = 0
  }

  class ParticleValue {
    var active:       Boolean = false
    var alwaysActive: Boolean = false

    def isActive: Boolean = alwaysActive || active

    def save(output: Writer): Unit =
      if (!alwaysActive)
        output.write("active: " + active + "\n")
      else
        active = true

    def load(reader: BufferedReader): Unit =
      if (!alwaysActive)
        active = readBoolean(reader, "active")
      else
        active = true

    def load(value: ParticleValue): Unit = {
      active = value.active
      alwaysActive = value.alwaysActive
    }
  }

  class NumericValue extends ParticleValue {
    var value: Float = 0f

    override def save(output: Writer): Unit = {
      super.save(output)
      if (active) output.write("value: " + value + "\n")
    }

    override def load(reader: BufferedReader): Unit = {
      super.load(reader)
      if (active) {
        value = readFloat(reader, "value")
      }
    }

    def load(value: NumericValue): Unit = {
      super.load(value)
      this.value = value.value
    }
  }

  class RangedNumericValue extends ParticleValue {
    var lowMin: Float = 0f
    var lowMax: Float = 0f

    def newLowValue(): Float =
      lowMin + (lowMax - lowMin) * scala.util.Random.nextFloat()

    def setLow(value: Float): Unit = {
      lowMin = value
      lowMax = value
    }

    def setLow(min: Float, max: Float): Unit = {
      lowMin = min
      lowMax = max
    }

    def scale(scale: Float): Unit = {
      lowMin *= scale
      lowMax *= scale
    }

    def set(value: RangedNumericValue): Unit = {
      this.lowMin = value.lowMin
      this.lowMax = value.lowMax
    }

    override def save(output: Writer): Unit = {
      super.save(output)
      if (active) {
        output.write("lowMin: " + lowMin + "\n")
        output.write("lowMax: " + lowMax + "\n")
      }
    }

    override def load(reader: BufferedReader): Unit = {
      super.load(reader)
      if (active) {
        lowMin = readFloat(reader, "lowMin")
        lowMax = readFloat(reader, "lowMax")
      }
    }

    def load(value: RangedNumericValue): Unit = {
      super.load(value)
      lowMax = value.lowMax
      lowMin = value.lowMin
    }
  }

  class ScaledNumericValue extends RangedNumericValue {
    var scaling:  Array[Float] = Array(1f)
    var timeline: Array[Float] = Array(0f)
    var highMin:  Float        = 0f
    var highMax:  Float        = 0f
    var relative: Boolean      = false

    def newHighValue(): Float =
      highMin + (highMax - highMin) * scala.util.Random.nextFloat()

    def setHigh(value: Float): Unit = {
      highMin = value
      highMax = value
    }

    def setHigh(min: Float, max: Float): Unit = {
      highMin = min
      highMax = max
    }

    override def scale(scale: Float): Unit = {
      super.scale(scale)
      highMin *= scale
      highMax *= scale
    }

    def getScale(percent: Float): Float = scala.util.boundary {
      val timeline = this.timeline
      val n        = timeline.length
      if (n <= 1) scala.util.boundary.break(scaling(0))

      for (i <- 1 until n) {
        val t = timeline(i)
        if (t > percent) {
          val scaling    = this.scaling
          val startIndex = i - 1
          val startValue = scaling(startIndex)
          val startTime  = timeline(startIndex)
          scala.util.boundary.break(startValue + (scaling(i) - startValue) * ((percent - startTime) / (t - startTime)))
        }
      }
      scaling(n - 1)
    }

    override def set(value: RangedNumericValue): Unit =
      value match {
        case scaledValue: ScaledNumericValue =>
          super.set(scaledValue)
          this.highMin = scaledValue.highMin
          this.highMax = scaledValue.highMax
          this.scaling = scaledValue.scaling.clone()
          this.timeline = scaledValue.timeline.clone()
          this.relative = scaledValue.relative
        case _ =>
          super.set(value)
      }

    override def save(output: Writer): Unit = {
      super.save(output)
      if (active) {
        output.write("highMin: " + highMin + "\n")
        output.write("highMax: " + highMax + "\n")
        output.write("relative: " + relative + "\n")
        output.write("scalingCount: " + scaling.length + "\n")
        for (i <- scaling.indices)
          output.write("scaling" + i + ": " + scaling(i) + "\n")
        output.write("timelineCount: " + timeline.length + "\n")
        for (i <- timeline.indices)
          output.write("timeline" + i + ": " + timeline(i) + "\n")
      }
    }

    override def load(reader: BufferedReader): Unit = {
      super.load(reader)
      if (active) {
        highMin = readFloat(reader, "highMin")
        highMax = readFloat(reader, "highMax")
        relative = readBoolean(reader, "relative")
        scaling = new Array[Float](readInt(reader, "scalingCount"))
        for (i <- scaling.indices)
          scaling(i) = readFloat(reader, "scaling" + i)
        timeline = new Array[Float](readInt(reader, "timelineCount"))
        for (i <- timeline.indices)
          timeline(i) = readFloat(reader, "timeline" + i)
      }
    }

    def load(value: ScaledNumericValue): Unit = {
      super.load(value)
      highMax = value.highMax
      highMin = value.highMin
      scaling = new Array[Float](value.scaling.length)
      System.arraycopy(value.scaling, 0, scaling, 0, scaling.length)
      timeline = new Array[Float](value.timeline.length)
      System.arraycopy(value.timeline, 0, timeline, 0, timeline.length)
      relative = value.relative
    }
  }

  class IndependentScaledNumericValue extends ScaledNumericValue {
    var independent: Boolean = false

    override def set(value: RangedNumericValue): Unit =
      value match {
        case independentValue: IndependentScaledNumericValue =>
          set(independentValue)
        case _ =>
          super.set(value)
      }

    def set(value: ScaledNumericValue): Unit =
      value match {
        case independentValue: IndependentScaledNumericValue =>
          set(independentValue)
        case _ =>
          super.set(value)
      }

    def set(value: IndependentScaledNumericValue): Unit = {
      super.set(value)
      independent = value.independent
    }

    override def save(output: Writer): Unit = {
      super.save(output)
      output.write("independent: " + independent + "\n")
    }

    override def load(reader: BufferedReader): Unit = {
      super.load(reader)
      // For backwards compatibility, independent property may not be defined
      if (reader.markSupported()) reader.mark(100)
      val line = reader.readLine()
      if (Nullable(line).isEmpty) throw new IOException("Missing value: independent")
      if (line.contains("independent"))
        independent = java.lang.Boolean.parseBoolean(readString(line))
      else if (reader.markSupported())
        reader.reset()
      else {
        // @see java.io.BufferedReader#markSupported may return false in some platforms (such as GWT),
        // in that case backwards compatibility is not possible
        val errorMessage = "The loaded particle effect descriptor file uses an old invalid format. " +
          "Please download the latest version of the Particle Editor tool and recreate the file by" +
          " loading and saving it again."
        // Gdx.app.error("ParticleEmitter", errorMessage) // Commented out since Gdx is not available
        throw new IOException(errorMessage)
      }
    }

    def load(value: IndependentScaledNumericValue): Unit = {
      super.load(value)
      independent = value.independent
    }
  }

  class GradientColorValue extends ParticleValue {
    import GradientColorValue._

    var colors:   Array[Float] = Array(1f, 1f, 1f)
    var timeline: Array[Float] = Array(0f)

    alwaysActive = true

    def getColor(percent: Float): Array[Float] = scala.util.boundary {
      var startIndex = 0
      var endIndex   = -1
      val timeline   = this.timeline
      val n          = timeline.length
      for (i <- 1 until n) {
        val t = timeline(i)
        if (t > percent) {
          endIndex = i
          // Break out of loop
          scala.util.boundary.break(calculateColor(startIndex, endIndex, percent))
        }
        startIndex = i
      }
      calculateColor(startIndex, endIndex, percent)
    }

    private def calculateColor(startIndex: Int, endIndex: Int, percent: Float): Array[Float] = {
      val timeline  = this.timeline
      val startTime = timeline(startIndex)
      val startIdx  = startIndex * 3
      val r1        = colors(startIdx)
      val g1        = colors(startIdx + 1)
      val b1        = colors(startIdx + 2)
      if (endIndex == -1) {
        temp(0) = r1
        temp(1) = g1
        temp(2) = b1
      } else {
        val factor = (percent - startTime) / (timeline(endIndex) - startTime)
        val endIdx = endIndex * 3
        temp(0) = r1 + (colors(endIdx) - r1) * factor
        temp(1) = g1 + (colors(endIdx + 1) - g1) * factor
        temp(2) = b1 + (colors(endIdx + 2) - b1) * factor
      }
      temp
    }

    override def save(output: Writer): Unit = {
      super.save(output)
      if (active) {
        output.write("colorsCount: " + colors.length + "\n")
        for (i <- colors.indices)
          output.write("colors" + i + ": " + colors(i) + "\n")
        output.write("timelineCount: " + timeline.length + "\n")
        for (i <- timeline.indices)
          output.write("timeline" + i + ": " + timeline(i) + "\n")
      }
    }

    override def load(reader: BufferedReader): Unit = {
      super.load(reader)
      if (active) {
        colors = new Array[Float](readInt(reader, "colorsCount"))
        for (i <- colors.indices)
          colors(i) = readFloat(reader, "colors" + i)
        timeline = new Array[Float](readInt(reader, "timelineCount"))
        for (i <- timeline.indices)
          timeline(i) = readFloat(reader, "timeline" + i)
      }
    }

    def load(value: GradientColorValue): Unit = {
      super.load(value)
      colors = new Array[Float](value.colors.length)
      System.arraycopy(value.colors, 0, colors, 0, colors.length)
      timeline = new Array[Float](value.timeline.length)
      System.arraycopy(value.timeline, 0, timeline, 0, timeline.length)
    }
  }

  object GradientColorValue {
    private val temp = new Array[Float](4)
  }

  class SpawnShapeValue extends ParticleValue {
    var shape: SpawnShape       = SpawnShape.point
    var edges: Boolean          = false
    var side:  SpawnEllipseSide = SpawnEllipseSide.both

    override def save(output: Writer): Unit = {
      super.save(output)
      if (active) {
        output.write("shape: " + shape + "\n")
        if (shape == SpawnShape.ellipse) {
          output.write("edges: " + edges + "\n")
          output.write("side: " + side + "\n")
        }
      }
    }

    override def load(reader: BufferedReader): Unit = {
      super.load(reader)
      if (active) {
        shape = SpawnShape.valueOf(readString(reader, "shape"))
        if (shape == SpawnShape.ellipse) {
          edges = readBoolean(reader, "edges")
          side = SpawnEllipseSide.valueOf(readString(reader, "side"))
        }
      }
    }

    def load(value: SpawnShapeValue): Unit = {
      super.load(value)
      shape = value.shape
      edges = value.edges
      side = value.side
    }
  }

  enum SpawnShape {
    case point, line, square, ellipse
  }

  enum SpawnEllipseSide {
    case both, top, bottom
  }

  enum SpriteMode {
    case single, random, animated
  }
}
