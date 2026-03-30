/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/btree/leaf/Success.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.btree.leaf` -> `sge.ai.btree.leaf`
 *   Convention: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ai
package btree
package leaf

/** `Success` is a leaf that immediately succeeds.
  *
  * @tparam E
  *   type of the blackboard object that tasks use to read or modify game state
  *
  * @author
  *   davebaol (original implementation)
  */
class Success[E] extends LeafTask[E] {

  /** Executes this `Success` task.
    * @return
    *   SUCCEEDED.
    */
  override def execute(): Task.Status = Task.Status.SUCCEEDED

  override protected def copyTo(task: Task[E]): Task[E] = task

  override def newInstance(): Task[E] = new Success[E]()
}
