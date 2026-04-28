/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/btree/TaskCloner.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.btree` -> `sge.ai.btree`
 *   Convention: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 37
 * Covenant-baseline-methods: TaskCloner,cloneTask,freeTask
 * Covenant-source-reference: com/badlogic/gdx/ai/btree/TaskCloner.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 6726e345248ddcad7cec0737f6ad83e4e028266d
 */
package sge
package ai
package btree

/** A `TaskCloner` allows you to use third-party libraries like Kryo to clone behavior trees. See [[Task.taskCloner]].
  *
  * @author
  *   davebaol (original implementation)
  */
trait TaskCloner {

  /** Makes a deep copy of the given task.
    * @param task
    *   the task to clone
    * @return
    *   the cloned task
    */
  def cloneTask[T](task: Task[T]): Task[T]

  /** Free task previously created by this [[TaskCloner]].
    * @param task
    *   task to free
    */
  def freeTask[T](task: Task[T]): Unit
}
