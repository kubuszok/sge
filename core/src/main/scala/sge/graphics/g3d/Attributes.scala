/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/Attributes.java
 * Original authors: (see AUTHORS file)
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d

import scala.util.boundary
import scala.util.boundary.break
import sge.utils.{ DynamicArray, Nullable }

class Attributes extends Iterable[Attribute] with Ordered[Attributes] {

  protected var mask:       Long                    = 0L
  protected val attributes: DynamicArray[Attribute] = DynamicArray[Attribute]()

  protected var sorted: Boolean = true

  /** Sort the attributes by their ID */
  final def sort(): Unit =
    if (!sorted) {
      attributes.sort()(using Attributes.attributeOrdering)
      sorted = true
    }

  /** @return Bitwise mask of the ID's of all the containing attributes */
  final def getMask: Long = mask

  /** Example usage: ((BlendingAttribute)material.get(BlendingAttribute.ID)).sourceFunction;
    * @return
    *   The attribute (which can safely be cast) if any, otherwise null
    */
  final def get(tpe: Long): Nullable[Attribute] = boundary {
    if (has(tpe)) {
      var i = 0
      while (i < attributes.size) {
        if (attributes(i).`type` == tpe) break(Nullable(attributes(i)))
        i += 1
      }
    }
    Nullable.empty
  }

  /** Example usage: ((BlendingAttribute)material.get(BlendingAttribute.ID)).sourceFunction;
    * @return
    *   The attribute if any, otherwise null
    */
  final def get[T <: Attribute](clazz: Class[T], tpe: Long): Nullable[T] =
    get(tpe).map(_.asInstanceOf[T])

  /** Get multiple attributes at once. Example: material.get(out, ColorAttribute.Diffuse | ColorAttribute.Specular | TextureAttribute.Diffuse);
    */
  final def get(out: DynamicArray[Attribute], tpe: Long): DynamicArray[Attribute] = {
    var i = 0
    while (i < attributes.size) {
      if ((attributes(i).`type` & tpe) != 0) out.add(attributes(i))
      i += 1
    }
    out
  }

  /** Removes all attributes */
  def clear(): Unit = {
    mask = 0
    attributes.clear()
  }

  /** @return The amount of attributes this material contains. */
  override def size: Int = attributes.size

  final private def enable(mask: Long): Unit =
    this.mask |= mask

  final private def disable(mask: Long): Unit =
    this.mask &= ~mask

  /** Add a attribute to this material. If the material already contains an attribute of the same type it is overwritten. */
  final def set(attribute: Attribute): Unit = {
    val idx = indexOf(attribute.`type`)
    if (idx < 0) {
      enable(attribute.`type`)
      attributes.add(attribute)
      sorted = false
    } else {
      attributes(idx) = attribute
    }
    sort() // FIXME: See #4186
  }

  /** Add multiple attributes to this material. If the material already contains an attribute of the same type it is overwritten.
    */
  final def set(attribute1: Attribute, attribute2: Attribute): Unit = {
    set(attribute1)
    set(attribute2)
  }

  /** Add multiple attributes to this material. If the material already contains an attribute of the same type it is overwritten.
    */
  final def set(attribute1: Attribute, attribute2: Attribute, attribute3: Attribute): Unit = {
    set(attribute1)
    set(attribute2)
    set(attribute3)
  }

  /** Add multiple attributes to this material. If the material already contains an attribute of the same type it is overwritten.
    */
  final def set(attribute1: Attribute, attribute2: Attribute, attribute3: Attribute, attribute4: Attribute): Unit = {
    set(attribute1)
    set(attribute2)
    set(attribute3)
    set(attribute4)
  }

  /** Add an array of attributes to this material. If the material already contains an attribute of the same type it is overwritten.
    */
  final def set(attributes: Attribute*): Unit =
    for (attr <- attributes)
      set(attr)

  /** Add an array of attributes to this material. If the material already contains an attribute of the same type it is overwritten.
    */
  final def set(attributes: Iterable[Attribute]): Unit =
    for (attr <- attributes)
      set(attr)

  /** Removes the attribute from the material, i.e.: material.remove(BlendingAttribute.ID); Can also be used to remove multiple attributes also, i.e. remove(AttributeA.ID | AttributeB.ID);
    */
  final def remove(mask: Long): Unit = {
    var i = attributes.size - 1
    while (i >= 0) {
      val tpe = attributes(i).`type`
      if ((mask & tpe) == tpe) {
        attributes.removeIndex(i)
        disable(tpe)
        sorted = false
      }
      i -= 1
    }
    sort() // FIXME: See #4186
  }

  /** @return
    *   True if this collection has the specified attribute, i.e. attributes.has(ColorAttribute.Diffuse); Or when multiple attribute types are specified, true if this collection has all specified
    *   attributes, i.e. attributes.has(out, ColorAttribute.Diffuse | ColorAttribute.Specular | TextureAttribute.Diffuse);
    */
  final def has(tpe: Long): Boolean =
    tpe != 0 && (this.mask & tpe) == tpe

  /** @return the index of the attribute with the specified type or negative if not available. */
  protected def indexOf(tpe: Long): Int = boundary {
    if (has(tpe)) {
      var i = 0
      while (i < attributes.size) {
        if (attributes(i).`type` == tpe) break(i)
        i += 1
      }
    }
    -1
  }

  /** Check if this collection has the same attributes as the other collection. If compareValues is true, it also compares the values of each attribute.
    * @param compareValues
    *   True to compare attribute values, false to only compare attribute types
    * @return
    *   True if this collection contains the same attributes (and optionally attribute values) as the other.
    */
  final def same(other: Attributes, compareValues: Boolean): Boolean = boundary {
    if (other eq this) break(true)
    if ((other == null) || (mask != other.mask)) break(false)
    if (!compareValues) break(true)
    sort()
    other.sort()
    var i = 0
    while (i < attributes.size) {
      if (attributes(i) != other.attributes(i)) break(false)
      i += 1
    }
    true
  }

  /** See [[same(Attributes, Boolean)]]
    * @return
    *   True if this collection contains the same attributes (but not values) as the other.
    */
  final def same(other: Attributes): Boolean = same(other, false)

  /** Used for iterating through the attributes */
  final override def iterator: Iterator[Attribute] = attributes.iterator

  /** @return
    *   A hash code based on only the attribute values, which might be different compared to [[hashCode]] because the latter might include other properties as well, i.e. the material id.
    */
  def attributesHash: Int = {
    sort()
    val n = attributes.size
    var result: Long = 71L + mask
    var m = 1
    var i = 0
    while (i < n) {
      m = (m * 7) & 0xffff
      result += mask * attributes(i).hashCode() * m
      i += 1
    }
    (result ^ (result >> 32)).toInt
  }

  override def hashCode(): Int = attributesHash

  override def equals(other: Any): Boolean = other match {
    case that: Attributes => same(that, true)
    case _ => false
  }

  override def compare(other: Attributes): Int = boundary {
    if (other eq this) break(0)
    if (mask != other.mask) break(if (mask < other.mask) -1 else 1)
    sort()
    other.sort()
    var i = 0
    while (i < attributes.size) {
      val c = attributes(i).compareTo(other.attributes(i))
      if (c != 0) break(if (c < 0) -1 else 1)
      i += 1
    }
    0
  }
}

object Attributes {

  /** Used for sorting attributes by type (not by value) */
  given attributeOrdering: Ordering[Attribute] = (arg0: Attribute, arg1: Attribute) => (arg0.`type` - arg1.`type`).toInt
}
