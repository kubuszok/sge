/*
 * Ported from Ashley ECS - https://github.com/libgdx/ashley
 * Original source: com/badlogic/ashley/core/ComponentMapper.java
 * Original authors: David Saltares
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.ashley.core` -> `sge.ecs`
 *   Convention: split packages
 *   Idiom: Nullable[A] return type for get
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 46
 * Covenant-baseline-methods: ComponentMapper,componentType,get,getFor,has
 * Covenant-source-reference: com/badlogic/ashley/core/ComponentMapper.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: d63d542228cd8c62cc2f7adf20055b0ac59a547e
 */
package sge
package ecs

import sge.utils.Nullable

/** Provides super fast [[Component]] retrieval from [[Entity]] objects.
  *
  * @tparam A
  *   the class type of the [[Component]].
  * @author
  *   David Saltares (original implementation)
  */
final class ComponentMapper[A <: Component] private (componentClass: Class[A]) {

  private val componentType: ComponentType = ComponentType.getFor(componentClass)

  /** @return The [[Component]] of the specified class belonging to entity, or Nullable.empty if not present. */
  def get(entity: Entity): Nullable[A] = entity.getComponent[A](componentType)

  /** @return Whether or not entity has the component of the specified class. */
  def has(entity: Entity): Boolean = entity.hasComponent(componentType)
}

object ComponentMapper {

  /** @param componentClass
    *   Component class to be retrieved by the mapper.
    * @return
    *   New instance that provides fast access to the [[Component]] of the specified class.
    */
  def getFor[A <: Component](componentClass: Class[A]): ComponentMapper[A] =
    new ComponentMapper[A](componentClass)
}
