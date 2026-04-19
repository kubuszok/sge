/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g2d/ParticleEffectPool.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: PooledEffect moved to companion object (was inner class in Java); Pool type parameter made explicit
 *   Idiom: boundary/break, Nullable, split packages
 *   Convention: PooledEffect.reset() delegates to super.reset() matching Java (no reset() override in Java PooledEffect)
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 70
 * Covenant-baseline-methods: ParticleEffectPool,PooledEffect,free,newObject,pooledEffect,reset
 * Covenant-source-reference: com/badlogic/gdx/graphics/g2d/ParticleEffectPool.java
 * Covenant-verified: 2026-04-19
 */
package sge
package graphics
package g2d

import sge.Sge
import sge.utils.Pool

/** Pool for {@link ParticleEffect} instances. This is a convenient class that prevents {@link ParticleEffect} instances from being freed while they are still in use.
  *
  * @author
  *   Nathan Sweet
  */
class ParticleEffectPool(effectTemplate: ParticleEffect, override protected val initialCapacity: Int = 16, override protected val max: Int = Int.MaxValue)(using Sge)
    extends Pool[ParticleEffectPool.PooledEffect] {

  protected def newObject(): ParticleEffectPool.PooledEffect = {
    val pooledEffect = ParticleEffectPool.PooledEffect(effectTemplate, this)
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
      val emitters         = effect.emitters
      val templateEmitters = this.effectTemplate.emitters
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
  class PooledEffect(effect: ParticleEffect, pool: Pool[ParticleEffectPool.PooledEffect])(using Sge) extends ParticleEffect(effect) {
    override def reset(): Unit =
      super.reset()

    def free(): Unit =
      pool.free(this)
  }
}
