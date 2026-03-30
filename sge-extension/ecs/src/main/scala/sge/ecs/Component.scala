/*
 * Ported from Ashley ECS - https://github.com/libgdx/ashley
 * Original source: com/badlogic/ashley/core/Component.java
 * Original authors: Stefan Bachmann
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.ashley.core` -> `sge.ecs`
 *   Convention: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ecs

/** Marker trait for all components. Components are data holders processed by [[EntitySystem]]s.
  *
  * @author
  *   Stefan Bachmann (original implementation)
  */
trait Component
