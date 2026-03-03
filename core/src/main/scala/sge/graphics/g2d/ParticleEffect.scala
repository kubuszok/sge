/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g2d/ParticleEffect.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: dispose() -> close(); Disposable -> AutoCloseable; findEmitter returns Nullable
 *   Convention: Nullable for null safety; using Sge context parameter; boundary/break for early returns
 *   Idiom: boundary/break, Nullable, split packages
 *   Audited: 2026-03-03
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g2d

import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.io.Writer

import sge.files.FileHandle
import sge.graphics.Texture
import sge.math.collision.BoundingBox
import sge.graphics.g2d.Sprite
import sge.graphics.g2d.Batch
import sge.utils.{ DynamicArray, Nullable, SgeError, StreamUtils }

import scala.language.implicitConversions

/** See <a href= "https://web.archive.org/web/20200427191041/http://www.badlogicgames.com/wordpress/?p=12555">http://www.badlogicgames.com/wordpress/?p=12555</a>
  * @author
  *   mzechner
  */
class ParticleEffect extends AutoCloseable {
  private val emitters:    DynamicArray[ParticleEmitter] = DynamicArray[ParticleEmitter]()
  private var bounds:      BoundingBox                   = scala.compiletime.uninitialized
  private var ownsTexture: Boolean                       = scala.compiletime.uninitialized
  /*protected*/
  var xSizeScale: Float = 1f
  /*protected*/
  var ySizeScale: Float = 1f
  /*protected*/
  var motionScale: Float = 1f

  def this(effect: ParticleEffect) = {
    this()
    emitters.clear()
    var i = 0
    while (i < effect.emitters.size) {
      emitters.add(newEmitter(effect.emitters(i)))
      i += 1
    }
  }

  def start(): Unit =
    for (i <- 0 until emitters.size)
      emitters(i).start()

  /** Resets the effect so it can be started again like a new effect. Any changes to scale are reverted. See {@link #reset(boolean)} .
    */
  def reset(): Unit =
    reset(resetScaling = true, start = true)

  /** Resets the effect so it can be started again like a new effect.
    * @param resetScaling
    *   Whether to restore the original size and motion parameters if they were scaled. Repeated scaling and resetting may introduce error.
    */
  def reset(resetScaling: Boolean): Unit =
    reset(resetScaling, start = true)

  /** Resets the effect so it can be started again like a new effect.
    * @param resetScaling
    *   Whether to restore the original size and motion parameters if they were scaled. Repeated scaling and resetting may introduce error.
    * @param start
    *   Whether to start the effect after resetting.
    */
  def reset(resetScaling: Boolean, start: Boolean): Unit = {
    for (i <- 0 until emitters.size)
      emitters(i).reset(start)
    if (resetScaling && (xSizeScale != 1f || ySizeScale != 1f || motionScale != 1f)) {
      scaleEffect(1f / xSizeScale, 1f / ySizeScale, 1f / motionScale)
      xSizeScale = 1f
      ySizeScale = 1f
      motionScale = 1f
    }
  }

  def update(delta: Float): Unit =
    for (i <- 0 until emitters.size)
      emitters(i).update(delta)

  def draw(spriteBatch: Batch): Unit =
    for (i <- 0 until emitters.size)
      emitters(i).draw(spriteBatch)

  def draw(spriteBatch: Batch, delta: Float): Unit =
    for (i <- 0 until emitters.size)
      emitters(i).draw(spriteBatch, delta)

  def allowCompletion(): Unit =
    for (i <- 0 until emitters.size)
      emitters(i).allowCompletionMethod()

  def isComplete(): Boolean = scala.util.boundary {
    for (i <- 0 until emitters.size) {
      val emitter = emitters(i)
      if (!emitter.isComplete()) scala.util.boundary.break(false)
    }
    true
  }

  def setDuration(duration: Int): Unit =
    for (i <- 0 until emitters.size) {
      val emitter = emitters(i)
      emitter.setContinuous(false)
      emitter.duration = duration.toFloat
      emitter.durationTimer = 0
    }

  def setPosition(x: Float, y: Float): Unit =
    for (i <- 0 until emitters.size)
      emitters(i).setPosition(x, y)

  def setFlip(flipX: Boolean, flipY: Boolean): Unit =
    for (i <- 0 until emitters.size)
      emitters(i).setFlip(flipX, flipY)

  def flipY(): Unit =
    for (i <- 0 until emitters.size)
      emitters(i).flipYValues()

  def getEmitters(): DynamicArray[ParticleEmitter] =
    emitters

  /** Returns the emitter with the specified name, or empty. */
  def findEmitter(name: String): Nullable[ParticleEmitter] = scala.util.boundary {
    for (i <- 0 until emitters.size) {
      val emitter = emitters(i)
      if (emitter.getName().equals(name)) scala.util.boundary.break(Nullable(emitter))
    }
    Nullable.empty
  }

  /** Allocates all emitters particles. See {@link com.badlogic.gdx.graphics.g2d.ParticleEmitter#preAllocateParticles()} */
  def preAllocateParticles(): Unit =
    for (emitter <- emitters)
      emitter.preAllocateParticles()

  def save(output: Writer): Unit = {
    var index = 0
    for (i <- 0 until emitters.size) {
      val emitter = emitters(i)
      if (index > 0) output.write("\n")
      emitter.save(output)
      index += 1
    }
  }

  def load(effectFile: FileHandle, imagesDir: FileHandle)(using Sge): Unit = {
    loadEmitters(effectFile)
    loadEmitterImages(imagesDir)
  }

  def load(effectFile: FileHandle, atlas: TextureAtlas): Unit =
    load(effectFile, atlas, Nullable.empty)

  def load(effectFile: FileHandle, atlas: TextureAtlas, atlasPrefix: Nullable[String]): Unit = {
    loadEmitters(effectFile)
    loadEmitterImages(atlas, atlasPrefix)
  }

  def loadEmitters(effectFile: FileHandle): Unit = {
    val input = effectFile.read()
    emitters.clear()
    val reader = new BufferedReader(new InputStreamReader(input), 512)
    try {
      var shouldContinue = true
      while (shouldContinue) {
        val emitter = newEmitter(reader)
        emitters.add(emitter)
        if (Nullable(reader.readLine()).isEmpty) shouldContinue = false
      }
    } catch {
      case ex: IOException =>
        throw SgeError.GraphicsError("Error loading effect: " + effectFile, Some(ex))
    } finally
      StreamUtils.closeQuietly(reader)
  }

  def loadEmitterImages(atlas: TextureAtlas): Unit =
    loadEmitterImages(atlas, Nullable.empty)

  def loadEmitterImages(atlas: TextureAtlas, atlasPrefix: Nullable[String]): Unit =
    for (i <- 0 until emitters.size) {
      val emitter = emitters(i)
      if (emitter.getImagePaths().size == 0) { /* continue */ }
      else {
        val sprites = DynamicArray[Sprite]()
        for (imagePath <- emitter.getImagePaths()) {
          var imageName    = new File(imagePath.replace('\\', '/')).getName()
          val lastDotIndex = imageName.lastIndexOf('.')
          if (lastDotIndex != -1) imageName = imageName.substring(0, lastDotIndex)
          atlasPrefix.foreach(prefix => imageName = prefix + imageName)
          val sprite = atlas.createSprite(imageName)
          sprite.fold(throw new IllegalArgumentException("Atlas is missing region: " + imageName))(sprites.add)
        }
        emitter.setSprites(sprites.toArray)
      }
    }

  def loadEmitterImages(imagesDir: FileHandle)(using Sge): Unit = {
    ownsTexture = true
    val loadedSprites = scala.collection.mutable.Map[String, Sprite]()
    for (i <- 0 until emitters.size) {
      val emitter = emitters(i)
      if (emitter.getImagePaths().size != 0) {
        val sprites = DynamicArray[Sprite]()
        for (imagePath <- emitter.getImagePaths()) {
          val imageName = new File(imagePath.replace('\\', '/')).getName()
          val sprite    = loadedSprites.getOrElseUpdate(imageName, new Sprite(loadTexture(imagesDir.child(imageName))))
          sprites.add(sprite)
        }
        emitter.setSprites(sprites.toArray)
      }
    }
  }

  protected def newEmitter(reader: BufferedReader): ParticleEmitter =
    new ParticleEmitter(reader)

  protected def newEmitter(emitter: ParticleEmitter): ParticleEmitter =
    new ParticleEmitter(emitter)

  protected def loadTexture(file: FileHandle)(using Sge): Texture =
    new Texture(file, false)

  /** Disposes the texture for each sprite for each ParticleEmitter. */
  def close(): Unit =
    if (ownsTexture) {
      for (i <- 0 until emitters.size) {
        val emitter = emitters(i)
        for (sprite <- emitter.getSprites())
          sprite.getTexture().close()
      }
    }

  /** Returns the bounding box for all active particles. z axis will always be zero. */
  def getBoundingBox(): BoundingBox = {
    if (Nullable(bounds).isEmpty) bounds = new BoundingBox()

    bounds.inf()
    for (emitter <- this.emitters)
      bounds.ext(emitter.getBoundingBox())
    bounds
  }

  /** Permanently scales all the size and motion parameters of all the emitters in this effect. If this effect originated from a {@link ParticleEffectPool} , the scale will be reset when it is
    * returned to the pool.
    */
  def scaleEffect(scaleFactor: Float): Unit =
    scaleEffect(scaleFactor, scaleFactor, scaleFactor)

  /** Permanently scales all the size and motion parameters of all the emitters in this effect. If this effect originated from a {@link ParticleEffectPool} , the scale will be reset when it is
    * returned to the pool.
    */
  def scaleEffect(scaleFactor: Float, motionScaleFactor: Float): Unit =
    scaleEffect(scaleFactor, scaleFactor, motionScaleFactor)

  /** Permanently scales all the size and motion parameters of all the emitters in this effect. If this effect originated from a {@link ParticleEffectPool} , the scale will be reset when it is
    * returned to the pool.
    */
  def scaleEffect(xSizeScaleFactor: Float, ySizeScaleFactor: Float, motionScaleFactor: Float): Unit = {
    xSizeScale *= xSizeScaleFactor
    ySizeScale *= ySizeScaleFactor
    motionScale *= motionScaleFactor
    for (particleEmitter <- emitters) {
      particleEmitter.scaleSize(xSizeScaleFactor, ySizeScaleFactor)
      particleEmitter.scaleMotion(motionScaleFactor)
    }
  }

  /** Sets the {@link com.badlogic.gdx.graphics.g2d.ParticleEmitter#setCleansUpBlendFunction(boolean) cleansUpBlendFunction} parameter on all
    * {@link com.badlogic.gdx.graphics.g2d.ParticleEmitter ParticleEmitters} currently in this ParticleEffect. <p> IMPORTANT: If set to false and if the next object to use this Batch expects alpha
    * blending, you are responsible for setting the Batch's blend function to (GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA) before that next object is drawn.
    * @param cleanUpBlendFunction
    */
  def setEmittersCleanUpBlendFunction(cleanUpBlendFunction: Boolean): Unit =
    for (i <- 0 until emitters.size)
      emitters(i).setCleansUpBlendFunction(cleanUpBlendFunction)
}
