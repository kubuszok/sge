/*
 * Migration notes:
 *   SGE-original file, no LibGDX counterpart (extracted from inner interface in Pool.java)
 *   Convention: type class pattern decouples reset behavior from pooled type; `fromTrait` given auto-derives from `Pool.Poolable`
 *   Idiom: split packages
 *   TODO: Poolable[A] type class should replace Pool.Poolable interface; define in companion objects
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package utils

/** Type class that decouples reset behavior from the pooled type. Users can make any type poolable without modifying it.
  */
trait Poolable[A] {
  def reset(a: A): Unit
}
object Poolable {
  // Bridge: any type implementing Pool.Poolable auto-derives the type class
  given fromTrait[A <: Pool.Poolable]: Poolable[A] with {
    def reset(a: A): Unit = a.reset()
  }
}
