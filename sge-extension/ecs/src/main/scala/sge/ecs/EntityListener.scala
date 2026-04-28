/*
 * Ported from Ashley ECS - https://github.com/libgdx/ashley
 * Original source: com/badlogic/ashley/core/EntityListener.java
 * Original authors: David Saltares
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.ashley.core` -> `sge.ecs`
 *   Convention: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 34
 * Covenant-baseline-methods: EntityListener,entityAdded,entityRemoved
 * Covenant-source-reference: com/badlogic/ashley/core/EntityListener.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: d63d542228cd8c62cc2f7adf20055b0ac59a547e
 */
package sge
package ecs

/** Gets notified of [[Entity]] related events.
  *
  * @author
  *   David Saltares (original implementation)
  */
trait EntityListener {

  /** Called whenever an [[Entity]] is added to the engine or matches a specific [[Family]].
    * @param entity
    *   the entity that was added
    */
  def entityAdded(entity: Entity): Unit

  /** Called whenever an [[Entity]] is removed from the engine or no longer matches a specific [[Family]].
    * @param entity
    *   the entity that was removed
    */
  def entityRemoved(entity: Entity): Unit
}
