/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/reflect/ReflectionException.java
 * Original authors: nexsoftware
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Idiom: java's four constructors each call a different `Exception` constructor; scala has one
 *     primary constructor, so `Exception(String)` is the primary (a missing message defaults to
 *     null, as `Exception()` leaves it) and the two cause-taking constructors reproduce
 *     `Throwable(Throwable)` / `Throwable(String, Throwable)` through `initCause`, including
 *     `Throwable(Throwable)`'s message of `cause.toString`.
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 29
 * Covenant-baseline-methods: ReflectionException,this
 * Covenant-source-reference: com/badlogic/gdx/utils/reflect/ReflectionException.java
 * Covenant-verified: 2026-09-22
 */
package sge.utils.reflect

/** INJECTED SCALA (Substitutions.inject) — see [[ClassReflection]] for why the `reflect` package
  * is substituted rather than ported. */
class ReflectionException(message: String = null) extends Exception(message) {
  def this(cause: Throwable) = {
    this(if (cause == null) null else cause.toString)
    initCause(cause)
  }
  def this(message: String, cause: Throwable) = {
    this(message)
    initCause(cause)
  }
}
