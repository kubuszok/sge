/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/btree/TaskCloneException.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.btree` -> `sge.ai.btree`
 *   Convention: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ai
package btree

/** A `TaskCloneException` is thrown when an exception occurs during task cloning. See [[Task.cloneTask]].
  *
  * @author
  *   davebaol (original implementation)
  */
class TaskCloneException(message: String, cause: Throwable) extends RuntimeException(message, cause) {

  /** Constructs a new `TaskCloneException` with no detail message. */
  def this() = this(null, null) // @nowarn — null required for Java interop (RuntimeException constructor)

  /** Constructs a new `TaskCloneException` with the specified detail message. */
  def this(message: String) = this(message, null) // @nowarn — null required for Java interop (RuntimeException constructor)

  /** Constructs a new `TaskCloneException` with the specified cause. */
  def this(cause: Throwable) = this(if (cause != null) cause.toString else null, cause) // @nowarn — null required for Java interop (RuntimeException constructor)
}
