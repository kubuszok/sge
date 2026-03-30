/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/btree/branch/Sequence.java
 * Original authors: implicit-invocation
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.btree.branch` -> `sge.ai.btree.branch`; `Array` -> `DynamicArray`
 *   Convention: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ai
package btree
package branch

import sge.utils.DynamicArray

/** A `Sequence` is a branch task that runs every children until one of them fails. If a child task succeeds, the selector will start and run the next child task.
  *
  * @tparam E
  *   type of the blackboard object that tasks use to read or modify game state
  *
  * @author
  *   implicit-invocation (original implementation)
  */
class Sequence[E](
  children: DynamicArray[Task[E]] = DynamicArray[Task[E]]()
) extends SingleRunningChildBranch[E](children) {

  override def childSuccess(runningTask: Task[E]): Unit = {
    super.childSuccess(runningTask)
    currentChildIndex += 1
    if (currentChildIndex < children.size) {
      run() // Run next child
    } else {
      success() // All children processed, return success status
    }
  }

  override def childFail(runningTask: Task[E]): Unit = {
    super.childFail(runningTask)
    fail() // Return failure status when a child says it failed
  }

  override def newInstance(): Task[E] = new Sequence[E]()
}
