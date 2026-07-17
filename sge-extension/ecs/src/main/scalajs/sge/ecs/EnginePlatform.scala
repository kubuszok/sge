/*
 * Ported from Ashley ECS - https://github.com/libgdx/ashley
 * Original source: com/badlogic/ashley/core/Engine.java (createComponent, lines 67-73)
 * Original authors: Stefan Bachmann
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   SGE-original platform seam for Engine.createComponent's reflective fallback
 *   Renames: `com.badlogic.ashley.core` -> `sge.ecs`
 *   Convention: split packages; Scala.js source dir (src/main/scalajs)
 *   Idiom: Nullable[A] for the null-on-failure return
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ecs

import lowlevel.Nullable

/** Scala.js fallback for [[Engine.createComponent]] on component types without a registered factory.
  *
  * Scala.js has no runtime reflection, so an unregistered component type resolves to the empty/null-equivalent — the same contract as Ashley's base Engine.createComponent when reflection fails
  * (com/badlogic/ashley/core/Engine.java:67-73: `catch (ReflectionException e) { return null; }`). Register a factory via [[Engine.registerComponentFactory]] to create components on this platform.
  */
private[ecs] object EnginePlatform {

  def createComponentReflectively[T <: Component](componentType: Class[T]): Nullable[T] =
    Nullable.empty[T] // Engine.java:70-72 — no JS reflection; unregistered resolves to null-equivalent
}
