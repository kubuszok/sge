/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g2d/ParticleEffectPool.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g2d

import sge.utils.Pool
import scala.collection.mutable.ArrayBuffer

/** Pool for {@link ParticleEffect} instances. This is a convenient class that prevents {@link ParticleEffect} instances from being freed while they are still in use.
  *
  * @author
  *   Nathan Sweet
  */
class ParticleEffectPool(effectTemplate: ParticleEffect, override protected val initialCapacity: Int = 16, override protected val max: Int = Int.MaxValue)
    extends Pool[ParticleEffectPool.PooledEffect] {

  protected def newObject(): ParticleEffectPool.PooledEffect = {
    val pooledEffect = new ParticleEffectPool.PooledEffect(effectTemplate, this)
    pooledEffect.start()
    pooledEffect
  }

  override def free(effect: ParticleEffectPool.PooledEffect): Unit = {
    super.free(effect)

    effect.reset(false) // copy parameters exactly to avoid introducing error
    if (
      effect.xSizeScale != this.effectTemplate.xSizeScale || effect.ySizeScale != this.effectTemplate.ySizeScale
      || effect.motionScale != this.effectTemplate.motionScale
    ) {
      val emitters         = effect.getEmitters()
      val templateEmitters = this.effectTemplate.getEmitters()
      for (i <- 0 until emitters.size) {
        val emitter         = emitters(i)
        val templateEmitter = templateEmitters(i)
        emitter.matchSize(templateEmitter)
        emitter.matchMotion(templateEmitter)
      }
      effect.xSizeScale = this.effectTemplate.xSizeScale
      effect.ySizeScale = this.effectTemplate.ySizeScale
      effect.motionScale = this.effectTemplate.motionScale
    }
  }
}

object ParticleEffectPool {

  /** A ParticleEffect obtained from a {@link ParticleEffectPool} and that will be automatically freed when this effect has finished.
    */
  class PooledEffect(effect: ParticleEffect, pool: Pool[ParticleEffectPool.PooledEffect]) extends ParticleEffect(effect) {
    override def reset(): Unit =
      this.reset(resetScaling = true, start = false)

    def free(): Unit =
      pool.free(this)
  }
}
