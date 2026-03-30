/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/btree/branch/DynamicGuardSelector.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.btree.branch` -> `sge.ai.btree.branch`; `Array` -> `DynamicArray`
 *   Convention: split packages, Nullable instead of null
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ai
package btree
package branch

import sge.utils.DynamicArray
import sge.utils.Nullable

/** A `DynamicGuardSelector` is a branch task that executes the first child whose guard is evaluated to `true`. At every AI cycle, the children's guards are re-evaluated, so if the guard of the
  * running child is evaluated to `false`, it is cancelled, and the child with the highest priority starts running. The `DynamicGuardSelector` task finishes when no guard is evaluated to `true` (thus
  * failing) or when its active child finishes (returning the active child's termination status).
  *
  * @tparam E
  *   type of the blackboard object that tasks use to read or modify game state
  *
  * @author
  *   davebaol (original implementation)
  */
class DynamicGuardSelector[E](
  children: DynamicArray[Task[E]] = DynamicArray[Task[E]]()
) extends BranchTask[E](children) {

  /** The child in the running status or empty if no child is running. */
  protected var runningChild: Nullable[Task[E]] = Nullable.empty

  override def childRunning(task: Task[E], reporter: Task[E]): Unit = {
    runningChild = Nullable(task)
    running() // Return a running status when a child says it's running
  }

  override def childSuccess(task: Task[E]): Unit = {
    this.runningChild = Nullable.empty
    success()
  }

  override def childFail(task: Task[E]): Unit = {
    this.runningChild = Nullable.empty
    fail()
  }

  override def run(): Unit = {
    // Check guards
    var childToRun: Nullable[Task[E]] = Nullable.empty
    var i = 0
    val n = children.size
    while (i < n && childToRun.isEmpty) {
      val child = children(i)
      if (child.checkGuard(this)) {
        childToRun = Nullable(child)
      }
      i += 1
    }

    runningChild.foreach { rc =>
      if (childToRun.fold(true)(_ ne rc)) {
        rc.cancel()
        runningChild = Nullable.empty
      }
    }

    childToRun.fold {
      fail()
    } { ctr =>
      if (runningChild.isEmpty) {
        runningChild = Nullable(ctr)
        ctr.setControl(this)
        ctr.start()
      }
      runningChild.foreach(_.run())
    }
  }

  override def resetTask(): Unit = {
    super.resetTask()
    this.runningChild = Nullable.empty
  }

  override protected def copyTo(task: Task[E]): Task[E] = {
    val branch = task.asInstanceOf[DynamicGuardSelector[E]]
    branch.runningChild = Nullable.empty
    super.copyTo(task)
  }

  override def newInstance(): Task[E] = new DynamicGuardSelector[E]()

  override def reset(): Unit = {
    runningChild = Nullable.empty
    super.reset()
  }
}
