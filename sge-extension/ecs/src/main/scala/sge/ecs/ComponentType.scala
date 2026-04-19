/*
 * Ported from Ashley ECS - https://github.com/libgdx/ashley
 * Original source: com/badlogic/ashley/core/ComponentType.java
 * Original authors: Stefan Bachmann
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.ashley.core` -> `sge.ecs`
 *   Convention: split packages
 *   Idiom: mutable.HashMap instead of ObjectMap for Class keys
 *   Idiom: mutable.BitSet instead of Bits
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 77
 * Covenant-baseline-methods: ComponentType,assignedComponentTypes,bits,equals,getBitsFor,getFor,getIndexFor,hashCode,typeIndex
 * Covenant-source-reference: com/badlogic/ashley/core/ComponentType.java
 * Covenant-verified: 2026-04-19
 */
package sge
package ecs

import scala.collection.mutable

/** Uniquely identifies a [[Component]] sub-class. It assigns them an index which is used internally for fast comparison and retrieval. See [[Family]] and [[Entity]].
  *
  * ComponentTypes cannot be instantiated directly. They can only be accessed via [[ComponentType.getFor]]. Each component class will always return the same instance of ComponentType.
  *
  * @author
  *   Stefan Bachmann (original implementation)
  */
final class ComponentType private (val index: Int) {

  override def hashCode(): Int = index

  override def equals(obj: Any): Boolean =
    obj match {
      case other: ComponentType => index == other.index
      case _ => false
    }
}

object ComponentType {

  private val assignedComponentTypes: mutable.HashMap[Class[? <: Component], ComponentType] = mutable.HashMap.empty
  private var typeIndex:              Int                                                   = 0

  /** @param componentType
    *   The [[Component]] class
    * @return
    *   A ComponentType matching the Component Class
    */
  def getFor(componentType: Class[? <: Component]): ComponentType =
    assignedComponentTypes.getOrElseUpdate(componentType, {
                                             val ct = new ComponentType(typeIndex)
                                             typeIndex += 1
                                             ct
                                           }
    )

  /** Quick helper method. The same could be done via [[getFor]].
    * @param componentType
    *   The [[Component]] class
    * @return
    *   The index for the specified [[Component]] Class
    */
  def getIndexFor(componentType: Class[? <: Component]): Int =
    getFor(componentType).index

  /** @param componentTypes
    *   list of [[Component]] classes
    * @return
    *   BitSet representing the collection of components for quick comparison and matching. See [[Family]].
    */
  def getBitsFor(componentTypes: Class[? <: Component]*): mutable.BitSet = {
    val bits = mutable.BitSet()
    componentTypes.foreach { ct =>
      bits += getIndexFor(ct)
    }
    bits
  }
}
