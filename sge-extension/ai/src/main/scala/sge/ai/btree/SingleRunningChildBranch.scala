/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/btree/SingleRunningChildBranch.java
 * Original authors: implicit-invocation, davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.btree` -> `sge.ai.btree`; `Array` -> `DynamicArray`; `MathUtils` -> `sge.math.MathUtils`
 *   Convention: split packages, Nullable instead of null
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 122
 * Covenant-baseline-methods: SingleRunningChildBranch,branch,cancelRunningChildren,childFail,childRunning,childSuccess,copyTo,createRandomChildren,currentChildIndex,i,randomChildren,reset,resetTask,rndChildren,run,runningChild,start
 * Covenant-source-reference: com/badlogic/gdx/ai/btree/SingleRunningChildBranch.java
 * Covenant-verified: 2026-04-19
 */
package sge
package ai
package btree

import sge.math.MathUtils
import sge.utils.DynamicArray
import sge.utils.Nullable

/** A `SingleRunningChildBranch` task is a branch task that supports only one running child at a time.
  *
  * @tparam E
  *   type of the blackboard object that tasks use to read or modify game state
  *
  * @author
  *   implicit-invocation, davebaol (original implementation)
  */
abstract class SingleRunningChildBranch[E](
  children: DynamicArray[Task[E]] = DynamicArray[Task[E]]()
) extends BranchTask[E](children) {

  /** The child in the running status or empty if no child is running. */
  protected var runningChild: Nullable[Task[E]] = Nullable.empty

  /** The index of the child currently processed. */
  protected var currentChildIndex: Int = 0

  /** Array of random children. If it's empty this task is deterministic. */
  protected var randomChildren: Nullable[Array[Task[E]]] = Nullable.empty

  override def childRunning(task: Task[E], reporter: Task[E]): Unit = {
    runningChild = Nullable(task)
    running() // Return a running status when a child says it's running
  }

  override def childSuccess(task: Task[E]): Unit =
    this.runningChild = Nullable.empty

  override def childFail(task: Task[E]): Unit =
    this.runningChild = Nullable.empty

  override def run(): Unit =
    runningChild.fold {
      if (currentChildIndex < children.size) {
        randomChildren.fold {
          runningChild = Nullable(children(currentChildIndex))
        } { rndChildren =>
          val last = children.size - 1
          if (currentChildIndex < last) {
            // Random swap
            val otherChildIndex = MathUtils.random(currentChildIndex, last)
            val tmp             = rndChildren(currentChildIndex)
            rndChildren(currentChildIndex) = rndChildren(otherChildIndex)
            rndChildren(otherChildIndex) = tmp
          }
          runningChild = Nullable(rndChildren(currentChildIndex))
        }
        val child = runningChild.get
        child.setControl(this)
        child.start()
        if (!child.checkGuard(this))
          child.fail()
        else
          run()
      } else {
        // Should never happen; this case must be handled by subclasses in childXXX methods
      }
    } { child =>
      child.run()
    }

  override def start(): Unit = {
    this.currentChildIndex = 0
    runningChild = Nullable.empty
  }

  override private[btree] def cancelRunningChildren(startIndex: Int): Unit = {
    super.cancelRunningChildren(startIndex)
    runningChild = Nullable.empty
  }

  override def resetTask(): Unit = {
    super.resetTask()
    this.currentChildIndex = 0
    this.runningChild = Nullable.empty
    this.randomChildren = Nullable.empty
  }

  override protected def copyTo(task: Task[E]): Task[E] = {
    val branch = task.asInstanceOf[SingleRunningChildBranch[E]]
    branch.randomChildren = Nullable.empty
    super.copyTo(task)
  }

  protected def createRandomChildren(): Array[Task[E]] = {
    val rndChildren = new Array[Task[E]](children.size)
    var i           = 0
    while (i < children.size) {
      rndChildren(i) = children(i)
      i += 1
    }
    rndChildren
  }

  override def reset(): Unit = {
    this.currentChildIndex = 0
    this.runningChild = Nullable.empty
    this.randomChildren = Nullable.empty
    super.reset()
  }
}
