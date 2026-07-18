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

import java.lang.reflect.InvocationTargetException

import lowlevel.Nullable

/** JVM reflective fallback for [[Engine.createComponent]] on component types without a registered factory.
  *
  * Faithful to Ashley's base Engine.createComponent (com/badlogic/ashley/core/Engine.java:67-73), which calls `ClassReflection.newInstance(componentType)` and returns `null` only when reflection
  * actually fails (`catch (ReflectionException e) { return null; }`). Here the component is instantiated via its visible no-arg constructor.
  *
  * Exception net is narrowed to strict parity with gdx `ClassReflection.newInstance` (gdx-utils reflect/ClassReflection.java:91-99), which wraps EXACTLY `InstantiationException`,
  * `IllegalAccessException`, and (via `getConstructor`) `NoSuchMethodException` into `ReflectionException` — Engine.java:70-72 then maps that to `null`, our empty/null-equivalent. A constructor that
  * throws surfaces as `InvocationTargetException`; ashley's underlying `Class.newInstance` propagates the constructor-thrown exception unwrapped, so we rethrow its cause rather than swallow it.
  */
private[ecs] object EnginePlatform {

  def createComponentReflectively[T <: Component](componentType: Class[T]): Nullable[T] =
    try Nullable(componentType.getConstructor().newInstance())
    catch {
      // gdx ClassReflection.java:91-99 wraps exactly these three -> ReflectionException -> Engine.java:70-72 catch -> null
      case _: NoSuchMethodException | _: InstantiationException | _: IllegalAccessException => Nullable.empty[T]
      // ashley Class.newInstance propagates a constructor's own exception unwrapped; do not swallow it
      case e: InvocationTargetException => throw e.getCause
    }
}
