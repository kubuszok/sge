/*
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
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
