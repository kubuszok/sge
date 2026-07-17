/*
 * Ported from Ashley ECS - https://github.com/libgdx/ashley
 * Original source: com/badlogic/ashley/core/Engine.java (createComponent, lines 67-73)
 * Original authors: Stefan Bachmann
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   SGE-original platform seam for Engine.createComponent's reflective fallback
 *   Renames: `com.badlogic.ashley.core` -> `sge.ecs`
 *   Convention: split packages; JVM-only source dir (src/main/scalajvm)
 *   Idiom: Nullable[A] for the null-on-failure return
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ecs

import scala.util.control.NonFatal

import lowlevel.Nullable

/** JVM reflective fallback for [[Engine.createComponent]] on component types without a registered factory.
  *
  * Faithful to Ashley's base Engine.createComponent (com/badlogic/ashley/core/Engine.java:67-73), which calls `ClassReflection.newInstance(componentType)` and returns `null` only when reflection
  * actually fails (`catch (ReflectionException e) { return null; }`). Here the component is instantiated via its visible no-arg constructor, and any reflection failure yields the
  * empty/null-equivalent.
  */
private[ecs] object EnginePlatform {

  def createComponentReflectively[T <: Component](componentType: Class[T]): Nullable[T] =
    try Nullable(componentType.getConstructor().newInstance())
    catch { case NonFatal(_) => Nullable.empty[T] } // Engine.java:70-72 — catch ReflectionException -> null
}
