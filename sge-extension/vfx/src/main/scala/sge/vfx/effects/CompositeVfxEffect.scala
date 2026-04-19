/*
 * Ported from gdx-vfx - https://github.com/crashinvaders/gdx-vfx
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 63
 * Covenant-baseline-methods: CompositeVfxEffect,close,i,managedEffects,rebind,register,resize,unregister,update
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package vfx
package effects

import scala.collection.mutable.ArrayBuffer

/** Base class for an effect that is a composition of some other [[VfxEffect]]s. The class manages contained effects and delegates the lifecycle methods to the instances (e.g. [[VfxEffect.resize]],
  * [[VfxEffect.rebind]], [[VfxEffect.update]], [[VfxEffect.close]]).
  *
  * To register an internal effect, call [[register]].
  */
abstract class CompositeVfxEffect extends AbstractVfxEffect {

  protected val managedEffects: ArrayBuffer[VfxEffect] = ArrayBuffer.empty

  override def resize(width: Int, height: Int): Unit = {
    var i = 0
    while (i < managedEffects.size) {
      managedEffects(i).resize(width, height)
      i += 1
    }
  }

  override def rebind(): Unit = {
    var i = 0
    while (i < managedEffects.size) {
      managedEffects(i).rebind()
      i += 1
    }
  }

  override def update(delta: Float): Unit = {
    var i = 0
    while (i < managedEffects.size) {
      managedEffects(i).update(delta)
      i += 1
    }
  }

  override def close(): Unit = {
    var i = 0
    while (i < managedEffects.size) {
      managedEffects(i).close()
      i += 1
    }
  }

  protected def register[T <: VfxEffect](effect: T): T = {
    managedEffects += effect
    effect
  }

  protected def unregister[T <: VfxEffect](effect: T): T = {
    managedEffects -= effect
    effect
  }
}
