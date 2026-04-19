/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/ui/ParticleEffectActor.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: Disposable -> AutoCloseable; (using Sge) added to act() and one constructor
 *   Idiom: split packages
 *   Fixes: Java-style getters/setters → Scala property accessors (resetOnStart, autoRemove, running, effect→particleEffect)
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 94
 * Covenant-baseline-methods: ParticleEffectActor,act,allowCompletion,autoRemove,cancel,close,draw,lastDelta,ownsEffect,running,scaleChanged,start,this
 * Covenant-source-reference: com/badlogic/gdx/scenes/scene2d/ui/ParticleEffectActor.java
 * Covenant-verified: 2026-04-19
 */
package sge
package scenes
package scene2d
package ui

import sge.files.FileHandle
import sge.graphics.g2d.{ Batch, ParticleEffect, TextureAtlas }
import sge.utils.Seconds

/** ParticleEffectActor holds an {@link ParticleEffect} to use in Scene2d applications. The particle effect is positioned at 0, 0 in the ParticleEffectActor. Its bounding box is not limited to the
  * size of this actor.
  */
class ParticleEffectActor(val particleEffect: ParticleEffect, var resetOnStart: Boolean)(using Sge) extends Actor() with AutoCloseable {

  protected var lastDelta:  Seconds = Seconds.zero
  var running:              Boolean = false
  protected var ownsEffect: Boolean = false
  var autoRemove:           Boolean = false

  def this(particleFile: FileHandle, atlas: TextureAtlas)(using Sge) = {
    this(ParticleEffect(), true)
    particleEffect.load(particleFile, atlas)
    ownsEffect = true
    resetOnStart = false
  }

  def this(particleFile: FileHandle, imagesDir: FileHandle)(using Sge) = {
    this(ParticleEffect(), true)
    particleEffect.load(particleFile, imagesDir)
    ownsEffect = true
    resetOnStart = false
  }

  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    particleEffect.setPosition(x, y)
    if (lastDelta > Seconds.zero) {
      particleEffect.update(lastDelta)
      lastDelta = Seconds.zero
    }
    if (running) {
      particleEffect.draw(batch)
      running = !particleEffect.isComplete()
    }
  }

  override def act(delta: Seconds): Unit = {
    super.act(delta)
    // don't do particleEffect.update() here - the correct position is set just while we
    // are in draw() method. We save the delta here to update in draw()
    lastDelta = lastDelta + delta

    if (autoRemove && particleEffect.isComplete()) {
      remove()
    }
  }

  def start(): Unit = {
    running = true
    if (resetOnStart) {
      particleEffect.reset(false)
    }
    particleEffect.start()
  }

  override protected def scaleChanged(): Unit = {
    super.scaleChanged()
    particleEffect.scaleEffect(scaleX, scaleY, scaleY)
  }

  def cancel(): Unit =
    running = true

  def allowCompletion(): Unit =
    particleEffect.allowCompletion()

  override def close(): Unit =
    if (ownsEffect) {
      particleEffect.close()
    }
}
