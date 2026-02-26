/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/values/ParticleValue.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
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
  var active: Boolean = false

  def this(value: ParticleValue) = {
    this()
    this.active = value.active
  }

  def isActive(): Boolean =
    active

  def setActive(active: Boolean): Unit =
    this.active = active

  def load(value: ParticleValue): Unit =
    active = value.active
}
