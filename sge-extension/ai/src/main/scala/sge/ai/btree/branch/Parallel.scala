/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/btree/branch/Parallel.java
 * Original authors: implicit-invocation, davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.btree.branch` -> `sge.ai.btree.branch`; `Array` -> `DynamicArray`
 *   Convention: split packages, Nullable instead of null
 *   Idiom: `@TaskAttribute` annotation dropped; Java enum with abstract methods -> Scala enum with abstract methods
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 244
 * Covenant-baseline-methods: Orchestrator,Parallel,Policy,childFail,childRunning,childSuccess,copyTo,currentChildIndex,execute,executeJoin,executeResume,i,lastResult,n,newInstance,noRunningTasks,onChildFail,onChildSuccess,orchestrator,parallel,policy,reset,resetAllChildren,resetTask,run
 * Covenant-source-reference: com/badlogic/gdx/ai/btree/branch/Parallel.java
 * Covenant-verified: 2026-04-19
 */
package sge
package ai
package btree
package branch

import sge.utils.DynamicArray
import sge.utils.Nullable

/** A `Parallel` is a special branch task that runs all children when stepped. Its actual behavior depends on its [[Parallel.Orchestrator orchestrator]] and [[Parallel.Policy policy]].
  *
  * The execution of the parallel task's children depends on its orchestrator:
  *   - `Resume`: the parallel task restarts or runs each child every step
  *   - `Join`: child tasks will run until success or failure but will not re-run until the parallel task has succeeded or failed
  *
  * The actual result of the parallel task depends on its policy:
  *   - `Sequence`: the parallel task fails as soon as one child fails; if all children succeed, then the parallel task succeeds. This is the default policy.
  *   - `Selector`: the parallel task succeeds as soon as one child succeeds; if all children fail, then the parallel task fails.
  *
  * The typical use case: make the game entity react on event while sleeping or wandering.
  *
  * @tparam E
  *   type of the blackboard object that tasks use to read or modify game state
  *
  * @author
  *   implicit-invocation, davebaol (original implementation)
  */
class Parallel[E](
  /** The parallel policy (defaults to Sequence). */
  var policy: Parallel.Policy = Parallel.Policy.Sequence,
  /** The execution orchestrator (defaults to Resume). */
  var orchestrator: Parallel.Orchestrator = Parallel.Orchestrator.Resume,
  children:         DynamicArray[Task[E]] = DynamicArray[Task[E]]()
) extends BranchTask[E](children) {

  private var noRunningTasks:            Boolean           = true
  private var lastResult:                Nullable[Boolean] = Nullable.empty
  private[branch] var currentChildIndex: Int               = 0

  override def run(): Unit =
    orchestrator.execute(this)

  override def childRunning(task: Task[E], reporter: Task[E]): Unit =
    noRunningTasks = false

  override def childSuccess(runningTask: Task[E]): Unit =
    lastResult = policy.onChildSuccess(this)

  override def childFail(runningTask: Task[E]): Unit =
    lastResult = policy.onChildFail(this)

  override def resetTask(): Unit = {
    super.resetTask()
    noRunningTasks = true
  }

  override protected def copyTo(task: Task[E]): Task[E] = {
    val parallel = task.asInstanceOf[Parallel[E]]
    parallel.policy = policy // no need to clone since it is immutable
    parallel.orchestrator = orchestrator // no need to clone since it is immutable
    super.copyTo(task)
  }

  def resetAllChildren(): Unit = {
    var i = 0
    val n = getChildCount
    while (i < n) {
      getChild(i).reset()
      i += 1
    }
  }

  override def newInstance(): Task[E] = new Parallel[E]()

  override def reset(): Unit = {
    policy = Parallel.Policy.Sequence
    orchestrator = Parallel.Orchestrator.Resume
    noRunningTasks = true
    lastResult = Nullable.empty
    currentChildIndex = 0
    super.reset()
  }
}

object Parallel {

  /** The enumeration of the child orchestrators supported by the [[Parallel]] task. */
  enum Orchestrator {

    /** The default orchestrator - starts or resumes all children every single step. */
    case Resume

    /** Children execute until they succeed or fail but will not re-run until the parallel task has succeeded or failed. */
    case Join

    /** Called by parallel task each run.
      * @param parallel
      *   The [[Parallel]] task
      */
    def execute[E](parallel: Parallel[E]): Unit =
      this match {
        case Resume => executeResume(parallel)
        case Join   => executeJoin(parallel)
      }

    private def executeResume[E](parallel: Parallel[E]): Unit = {
      parallel.noRunningTasks = true
      parallel.lastResult = Nullable.empty
      parallel.currentChildIndex = 0
      while (parallel.currentChildIndex < parallel.getChildCount) {
        val child = parallel.getChild(parallel.currentChildIndex)
        if (child.getStatus == Task.Status.RUNNING) {
          child.run()
        } else {
          child.setControl(parallel)
          child.start()
          if (child.checkGuard(parallel))
            child.run()
          else
            child.fail()
        }

        if (parallel.lastResult.isDefined) { // Current child has finished either with success or fail
          parallel.cancelRunningChildren(if (parallel.noRunningTasks) parallel.currentChildIndex + 1 else 0)
          if (parallel.lastResult.get)
            parallel.success()
          else
            parallel.fail()
          // early exit — equivalent to `return` in the original
          parallel.currentChildIndex = parallel.getChildCount // break the while
        } else {
          parallel.currentChildIndex += 1
        }
      }
      // Only call running() if we didn't terminate early
      if (parallel.lastResult.isEmpty) {
        parallel.running()
      }
    }

    private def executeJoin[E](parallel: Parallel[E]): Unit = {
      parallel.noRunningTasks = true
      parallel.lastResult = Nullable.empty
      parallel.currentChildIndex = 0
      while (parallel.currentChildIndex < parallel.getChildCount) {
        val child = parallel.getChild(parallel.currentChildIndex)

        child.getStatus match {
          case Task.Status.RUNNING =>
            child.run()
          case Task.Status.SUCCEEDED | Task.Status.FAILED =>
            () // already done, skip
          case _ =>
            child.setControl(parallel)
            child.start()
            if (child.checkGuard(parallel))
              child.run()
            else
              child.fail()
        }

        if (parallel.lastResult.isDefined) { // Current child has finished either with success or fail
          parallel.cancelRunningChildren(if (parallel.noRunningTasks) parallel.currentChildIndex + 1 else 0)
          parallel.resetAllChildren()
          if (parallel.lastResult.get)
            parallel.success()
          else
            parallel.fail()
          // early exit — equivalent to `return` in the original
          parallel.currentChildIndex = parallel.getChildCount // break the while
        } else {
          parallel.currentChildIndex += 1
        }
      }
      // Only call running() if we didn't terminate early
      if (parallel.lastResult.isEmpty) {
        parallel.running()
      }
    }
  }

  /** The enumeration of the policies supported by the [[Parallel]] task. */
  enum Policy {

    /** The sequence policy makes the [[Parallel]] task fail as soon as one child fails; if all children succeed, then the parallel task succeeds. This is the default policy.
      */
    case Sequence

    /** The selector policy makes the [[Parallel]] task succeed as soon as one child succeeds; if all children fail, then the parallel task fails.
      */
    case Selector

    /** Called by parallel task each time one of its children succeeds.
      * @param parallel
      *   the parallel task
      * @return
      *   a Nullable[Boolean]: `Nullable(true)` if parallel must succeed, `Nullable(false)` if parallel must fail, and `Nullable.empty` if parallel must keep on running.
      */
    def onChildSuccess[E](parallel: Parallel[E]): Nullable[Boolean] =
      this match {
        case Sequence =>
          parallel.orchestrator match {
            case Orchestrator.Join =>
              if (parallel.noRunningTasks && parallel.getChild(parallel.getChildCount - 1).getStatus == Task.Status.SUCCEEDED)
                Nullable(true)
              else Nullable.empty
            case Orchestrator.Resume =>
              if (parallel.noRunningTasks && parallel.currentChildIndex == parallel.getChildCount - 1)
                Nullable(true)
              else Nullable.empty
          }
        case Selector =>
          Nullable(true)
      }

    /** Called by parallel task each time one of its children fails.
      * @param parallel
      *   the parallel task
      * @return
      *   a Nullable[Boolean]: `Nullable(true)` if parallel must succeed, `Nullable(false)` if parallel must fail, and `Nullable.empty` if parallel must keep on running.
      */
    def onChildFail[E](parallel: Parallel[E]): Nullable[Boolean] =
      this match {
        case Sequence =>
          Nullable(false)
        case Selector =>
          if (parallel.noRunningTasks && parallel.currentChildIndex == parallel.getChildCount - 1)
            Nullable(false)
          else Nullable.empty
      }
  }
}
