/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/btree/BranchTask.java
 * Original authors: implicit-invocation, davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.btree` -> `sge.ai.btree`; `Array` -> `DynamicArray`
 *   Convention: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ai
package btree

import sge.utils.DynamicArray

/** A branch task defines a behavior tree branch, contains logic of starting or running sub-branches and leaves.
  *
  * @tparam E
  *   type of the blackboard object that tasks use to read or modify game state
  *
  * @author
  *   implicit-invocation, davebaol (original implementation)
  */
abstract class BranchTask[E](
  /** The children of this branch task. */
  protected val children: DynamicArray[Task[E]] = DynamicArray[Task[E]]()
) extends Task[E] {

  override protected def addChildToTask(child: Task[E]): Int = {
    children.add(child)
    children.size - 1
  }

  override def getChildCount: Int = children.size

  override def getChild(i: Int): Task[E] = children(i)

  override protected def copyTo(task: Task[E]): Task[E] = {
    val branch = task.asInstanceOf[BranchTask[E]]
    var i      = 0
    while (i < children.size) {
      branch.children.add(children(i).cloneTask())
      i += 1
    }
    task
  }

  override def reset(): Unit = {
    children.clear()
    super.reset()
  }
}
