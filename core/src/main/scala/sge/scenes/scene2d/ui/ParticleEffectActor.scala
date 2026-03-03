/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/ui/ParticleEffectActor.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: Disposable -> AutoCloseable; (using Sge) added to act() and one constructor
 *   Idiom: split packages
 *   TODO: Java-style getters/setters — isResetOnStart/setResetOnStart, isAutoRemove/setAutoRemove, isRunning, getEffect
 *   Audited: 2026-03-03
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package scenes
package scene2d
package ui

import sge.files.FileHandle
import sge.graphics.g2d.{ Batch, ParticleEffect, TextureAtlas }

/** ParticleEffectActor holds an {@link ParticleEffect} to use in Scene2d applications. The particle effect is positioned at 0, 0 in the ParticleEffectActor. Its bounding box is not limited to the
  * size of this actor.
  */
class ParticleEffectActor(val particleEffect: ParticleEffect, private var resetOnStart: Boolean) extends Actor with AutoCloseable {

  protected var lastDelta:  Float   = 0
  protected var _isRunning: Boolean = false
  protected var ownsEffect: Boolean = false
  private var autoRemove:   Boolean = false

  def this(particleFile: FileHandle, atlas: TextureAtlas) = {
    this(new ParticleEffect(), true)
    particleEffect.load(particleFile, atlas)
    ownsEffect = true
    resetOnStart = false
  }

  def this(particleFile: FileHandle, imagesDir: FileHandle)(using Sge) = {
    this(new ParticleEffect(), true)
    particleEffect.load(particleFile, imagesDir)
    ownsEffect = true
    resetOnStart = false
  }

  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    particleEffect.setPosition(getX, getY)
    if (lastDelta > 0) {
      particleEffect.update(lastDelta)
      lastDelta = 0
    }
    if (_isRunning) {
      particleEffect.draw(batch)
      _isRunning = !particleEffect.isComplete()
    }
  }

  override def act(delta: Float)(using Sge): Unit = {
    super.act(delta)
    // don't do particleEffect.update() here - the correct position is set just while we
    // are in draw() method. We save the delta here to update in draw()
    lastDelta += delta

    if (autoRemove && particleEffect.isComplete()) {
      remove()
    }
  }

  def start(): Unit = {
    _isRunning = true
    if (resetOnStart) {
      particleEffect.reset(false)
    }
    particleEffect.start()
  }

  def isResetOnStart: Boolean = resetOnStart

  def setResetOnStart(resetOnStart: Boolean): ParticleEffectActor = {
    this.resetOnStart = resetOnStart
    this
  }

  def isAutoRemove: Boolean = autoRemove

  def setAutoRemove(autoRemove: Boolean): ParticleEffectActor = {
    this.autoRemove = autoRemove
    this
  }

  def isRunning: Boolean = _isRunning

  def getEffect: ParticleEffect = this.particleEffect

  override protected def scaleChanged(): Unit = {
    super.scaleChanged()
    particleEffect.scaleEffect(getScaleX, getScaleY, getScaleY)
  }

  def cancel(): Unit =
    _isRunning = true

  def allowCompletion(): Unit =
    particleEffect.allowCompletion()

  override def close(): Unit =
    if (ownsEffect) {
      particleEffect.close()
    }
}
