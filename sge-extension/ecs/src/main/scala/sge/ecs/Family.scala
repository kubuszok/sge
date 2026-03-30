/*
 * Ported from Ashley ECS - https://github.com/libgdx/ashley
 * Original source: com/badlogic/ashley/core/Family.java
 * Original authors: Stefan Bachmann
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.ashley.core` -> `sge.ecs`
 *   Convention: split packages
 *   Idiom: mutable.BitSet instead of Bits
 *   Idiom: mutable.HashMap instead of ObjectMap for cache
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ecs

import scala.collection.mutable

/** Represents a group of [[Component]]s. It is used to describe what [[Entity]] objects an [[EntitySystem]] should
  * process.
  *
  * Example: `Family.all(classOf[PositionComponent], classOf[VelocityComponent]).get()`
  *
  * Families cannot be instantiated directly but must be accessed via a builder (start with [[Family.all]],
  * [[Family.one]] or [[Family.exclude]]). This avoids duplicate families that describe the same components.
  *
  * @author
  *   Stefan Bachmann (original implementation)
  */
class Family private (
    private val all: mutable.BitSet,
    private val one: mutable.BitSet,
    private val exclude: mutable.BitSet,
    val index: Int
) {

  /** @return Whether the entity matches the family requirements or not */
  def matches(entity: Entity): Boolean = {
    val entityComponentBits = entity.getComponentBits

    if (!all.subsetOf(entityComponentBits)) {
      false
    } else if (one.nonEmpty && (one & entityComponentBits).isEmpty) {
      false
    } else if (exclude.nonEmpty && (exclude & entityComponentBits).nonEmpty) {
      false
    } else {
      true
    }
  }

  override def hashCode(): Int = index

  override def equals(obj: Any): Boolean = this eq obj.asInstanceOf[AnyRef]
}

object Family {

  private val families: mutable.HashMap[String, Family] = mutable.HashMap.empty
  private var familyIndex: Int = 0
  private val zeroBits: mutable.BitSet = mutable.BitSet()

  /** @param componentTypes entities will have to contain all of the specified components.
    * @return A Builder instance to get a family
    */
  def all(componentTypes: Class[? <: Component]*): Builder = {
    new Builder().all(componentTypes*)
  }

  /** @param componentTypes entities will have to contain at least one of the specified components.
    * @return A Builder instance to get a family
    */
  def one(componentTypes: Class[? <: Component]*): Builder = {
    new Builder().one(componentTypes*)
  }

  /** @param componentTypes entities cannot contain any of the specified components.
    * @return A Builder instance to get a family
    */
  def exclude(componentTypes: Class[? <: Component]*): Builder = {
    new Builder().exclude(componentTypes*)
  }

  class Builder {

    private var _all: mutable.BitSet = zeroBits
    private var _one: mutable.BitSet = zeroBits
    private var _exclude: mutable.BitSet = zeroBits

    /** @param componentTypes entities will have to contain all of the specified components.
      * @return This Builder instance for chaining
      */
    def all(componentTypes: Class[? <: Component]*): Builder = {
      _all = ComponentType.getBitsFor(componentTypes*)
      this
    }

    /** @param componentTypes entities will have to contain at least one of the specified components.
      * @return This Builder instance for chaining
      */
    def one(componentTypes: Class[? <: Component]*): Builder = {
      _one = ComponentType.getBitsFor(componentTypes*)
      this
    }

    /** @param componentTypes entities cannot contain any of the specified components.
      * @return This Builder instance for chaining
      */
    def exclude(componentTypes: Class[? <: Component]*): Builder = {
      _exclude = ComponentType.getBitsFor(componentTypes*)
      this
    }

    /** @return A family for the configured component types */
    def get(): Family = {
      val hash = getFamilyHash(_all, _one, _exclude)
      families.getOrElseUpdate(hash, {
        val f = new Family(_all, _one, _exclude, familyIndex)
        familyIndex += 1
        f
      })
    }
  }

  private def getFamilyHash(all: mutable.BitSet, one: mutable.BitSet, exclude: mutable.BitSet): String = {
    val sb = new StringBuilder
    if (all.nonEmpty) {
      sb.append("{all:").append(getBitsString(all)).append("}")
    }
    if (one.nonEmpty) {
      sb.append("{one:").append(getBitsString(one)).append("}")
    }
    if (exclude.nonEmpty) {
      sb.append("{exclude:").append(getBitsString(exclude)).append("}")
    }
    sb.toString()
  }

  private def getBitsString(bits: mutable.BitSet): String = {
    val numBits = if (bits.isEmpty) 0 else bits.last + 1
    val sb = new StringBuilder
    var i = 0
    while (i < numBits) {
      sb.append(if (bits.contains(i)) "1" else "0")
      i += 1
    }
    sb.toString()
  }
}
