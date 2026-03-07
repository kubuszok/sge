/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/values/ParticleValue.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes (2026-03-03):
 * - Json.Serializable write/read methods intentionally omitted (serialization handled separately)
 * - All public methods ported: active (property), load, copy constructor
 * - Fixes (2026-03-04): isActive()/setActive() → virtual property active/active_=
 *   with backing field _active (PrimitiveSpawnShapeValue overrides active_=)
 * - Status: pass
 */

package sge
package graphics
package g3d
package particles
package values

/** It's a class which represents a value bound to the particles. Generally used by a particle controller component to find the current value of a particle property during the simulation.
  * @author
  *   Inferno
  */
class ParticleValue {
  protected var _active: Boolean = false

  def this(value: ParticleValue) = {
    this()
    this._active = value._active
  }

  def active: Boolean = _active

  def active_=(value: Boolean): Unit =
    _active = value

  def load(value: ParticleValue): Unit =
    _active = value._active
}
