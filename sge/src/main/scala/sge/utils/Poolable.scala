/*
 * Migration notes:
 *   SGE-original file, no LibGDX counterpart (extracted from inner interface in Pool.java)
 *   Convention: type class pattern decouples reset behavior from pooled type; `fromTrait` given auto-derives from `Pool.Poolable`
 *   Idiom: split packages
 *   Convention: Poolable[A] type class with noop fallback and fromTrait bridge for Pool.Poolable subtypes
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 36
 * Covenant-baseline-methods: LowPriority,NoopInstance,Poolable,noop,reset
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package utils

/** Type class that decouples reset behavior from the pooled type. Users can make any type poolable without modifying it.
  */
trait Poolable[A] {
  def reset(a: A): Unit
}
object Poolable extends Poolable.LowPriority {
  // Bridge: any type implementing Pool.Poolable auto-derives the type class (higher priority than noop)
  given fromTrait[A <: Pool.Poolable]: Poolable[A] with {
    def reset(a: A): Unit = a.reset()
  }

  /** No-op Poolable for types that don't need reset. */
  def noop[A]: Poolable[A] = NoopInstance.asInstanceOf[Poolable[A]]

  private object NoopInstance extends Poolable[Any] {
    def reset(a: Any): Unit = ()
  }

  private[utils] trait LowPriority {
    // Fallback: any type gets noop reset when no more specific given is available
    given noopPoolable[A]: Poolable[A] = Poolable.noop[A]
  }
}
