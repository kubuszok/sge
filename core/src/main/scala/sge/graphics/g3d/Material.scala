/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/Material.java
 * Original authors: (see AUTHORS file)
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   - All 8 constructors match Java source.
 *   - Static counter moved to companion object.
 *   - copy(), hashCode(), equals() all match.
 *   - id is constructor param (Java: public field set in constructors).
 *   - Audit: pass (2026-03-03)
 */
package sge
package graphics
package g3d

import sge.utils.DynamicArray

class Material(var id: String) extends Attributes {

  /** Create an empty material */
  def this() =
    this("mtl" + Material.nextCounter())

  /** Create a material with the specified attributes */
  def this(attributes: Attribute*) = {
    this()
    set(attributes*)
  }

  /** Create a material with the specified attributes */
  def this(id: String, attributes: Attribute*) = {
    this(id)
    set(attributes*)
  }

  /** Create a material with the specified attributes */
  def this(attributes: DynamicArray[Attribute]) = {
    this()
    for (attr <- attributes) set(attr)
  }

  /** Create a material with the specified attributes */
  def this(id: String, attributes: DynamicArray[Attribute]) = {
    this(id)
    for (attr <- attributes) set(attr)
  }

  /** Create a material which is an exact copy of the specified material */
  def this(id: String, copyFrom: Material) = {
    this(id)
    for (attr <- copyFrom)
      set(attr.copy())
  }

  /** Create a material which is an exact copy of the specified material */
  def this(copyFrom: Material) =
    this(copyFrom.id, copyFrom)

  /** Create a copy of this material */
  def copy(): Material = new Material(this)

  override def hashCode(): Int = super.hashCode() + 3 * id.hashCode()

  override def equals(other: Any): Boolean = other match {
    case that: Material => (that eq this) || (that.id == id && super.equals(that))
    case _ => false
  }
}

object Material {
  private var counter: Int = 0

  private def nextCounter(): Int = {
    counter += 1
    counter
  }
}
