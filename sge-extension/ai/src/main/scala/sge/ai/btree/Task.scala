/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/btree/Task.java
 * Original authors: implicit-invocation, davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.btree` -> `sge.ai.btree`
 *   Convention: split packages, Nullable instead of null, no return statements
 *   Idiom: `ClassReflection.newInstance` replaced by abstract `newInstance()` method
 *   Idiom: `@TaskConstraint` annotation dropped (parser uses registry instead)
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 332
 * Covenant-baseline-methods: Status,Task,addChild,addChildToTask,cancel,cancelRunningChildren,checkGuard,childFail,childRunning,childSuccess,cloneTask,control,copyTo,end,fail,getChild,getChildCount,getObject,getStatus,guard,i,index,n,newInstance,previousStatus,reset,resetTask,run,running,setControl,start,status,success,taskCloner,tree
 * Covenant-source-reference: com/badlogic/gdx/ai/btree/Task.java
 * Covenant-verified: 2026-04-19
 */
package sge
package ai
package btree

import sge.utils.Nullable
import sge.utils.Pool

/** This is the abstract base class of all behavior tree tasks. The `Task` of a behavior tree has a status, one control and a list of children.
  *
  * @tparam E
  *   type of the blackboard object that tasks use to read or modify game state
  *
  * @author
  *   implicit-invocation, davebaol (original implementation)
  */
abstract class Task[E] extends Pool.Poolable {

  /** The status of this task. */
  private[btree] var status: Task.Status = Task.Status.FRESH

  /** The parent of this task. */
  protected var control: Nullable[Task[E]] = Nullable.empty

  /** The behavior tree this task belongs to. */
  protected var tree: Nullable[BehaviorTree[E]] = Nullable.empty

  /** The guard of this task. */
  var guard: Nullable[Task[E]] = Nullable.empty

  // ── Child management ──────────────────────────────────────────────────

  /** This method will add a child to the list of this task's children.
    *
    * @param child
    *   the child task which will be added
    * @return
    *   the index where the child has been added.
    * @throws IllegalStateException
    *   if the child cannot be added for whatever reason.
    */
  final def addChild(child: Task[E]): Int = {
    val index = addChildToTask(child)
    tree.foreach { t =>
      t.notifyChildAdded(this, index)
    }
    index
  }

  /** This method will add a child to the list of this task's children.
    *
    * @param child
    *   the child task which will be added
    * @return
    *   the index where the child has been added.
    * @throws IllegalStateException
    *   if the child cannot be added for whatever reason.
    */
  protected def addChildToTask(child: Task[E]): Int

  /** Returns the number of children of this task. */
  def getChildCount: Int

  /** Returns the child at the given index. */
  def getChild(i: Int): Task[E]

  // ── Blackboard ────────────────────────────────────────────────────────

  /** Returns the blackboard object of the behavior tree this task belongs to.
    * @throws IllegalStateException
    *   if this task has never run
    */
  def getObject: E =
    tree.getOrElse(throw new IllegalStateException("This task has never run")).getObject

  // ── Status ────────────────────────────────────────────────────────────

  /** Returns the status of this task. */
  final def getStatus: Task.Status = status

  // ── Control ───────────────────────────────────────────────────────────

  /** This method will set a task as this task's control (parent).
    *
    * @param control
    *   the parent task
    */
  final def setControl(control: Task[E]): Unit = {
    this.control = Nullable(control)
    this.tree = control.tree
  }

  // ── Guard ─────────────────────────────────────────────────────────────

  /** Checks the guard of this task.
    * @param control
    *   the parent task
    * @return
    *   `true` if guard evaluation succeeds or there's no guard; `false` otherwise.
    * @throws IllegalStateException
    *   if guard evaluation returns any status other than SUCCEEDED and FAILED.
    */
  def checkGuard(control: Task[E]): Boolean =
    // No guard to check
    guard.fold(true) { g =>
      // Check the guard of the guard recursively
      if (!g.checkGuard(control)) {
        false
      } else {
        // Use the tree's guard evaluator task to check the guard of this task
        g.setControl(control.tree.getOrElse(throw new IllegalStateException("Tree not set")).guardEvaluator)
        g.start()
        g.run()
        g.getStatus match {
          case Task.Status.SUCCEEDED => true
          case Task.Status.FAILED    => false
          case other                 =>
            throw new IllegalStateException(
              s"Illegal guard status '$other'. Guards must either succeed or fail in one step."
            )
        }
      }
    }

  // ── Lifecycle ─────────────────────────────────────────────────────────

  /** This method will be called once before this task's first run. */
  def start(): Unit = {}

  /** This method will be called by `success()`, `fail()` or `cancel()`, meaning that this task's status has just been set to SUCCEEDED, FAILED or CANCELLED respectively.
    */
  def end(): Unit = {}

  /** This method contains the update logic of this task. The actual implementation MUST call `running()`, `success()` or `fail()` exactly once.
    */
  def run(): Unit

  // ── Status reporting ──────────────────────────────────────────────────

  /** This method will be called in `run()` to inform control that this task needs to run again. */
  final def running(): Unit = {
    val previousStatus = status
    status = Task.Status.RUNNING
    tree.foreach { t =>
      t.notifyStatusUpdated(this, previousStatus)
    }
    control.foreach(_.childRunning(this, this))
  }

  /** This method will be called in `run()` to inform control that this task has finished running with a success result. */
  final def success(): Unit = {
    val previousStatus = status
    status = Task.Status.SUCCEEDED
    tree.foreach { t =>
      t.notifyStatusUpdated(this, previousStatus)
    }
    end()
    control.foreach(_.childSuccess(this))
  }

  /** This method will be called in `run()` to inform control that this task has finished running with a failure result. */
  final def fail(): Unit = {
    val previousStatus = status
    status = Task.Status.FAILED
    tree.foreach { t =>
      t.notifyStatusUpdated(this, previousStatus)
    }
    end()
    control.foreach(_.childFail(this))
  }

  // ── Child callbacks ───────────────────────────────────────────────────

  /** This method will be called when one of the children of this task succeeds.
    *
    * @param task
    *   the task that succeeded
    */
  def childSuccess(task: Task[E]): Unit

  /** This method will be called when one of the children of this task fails.
    *
    * @param task
    *   the task that failed
    */
  def childFail(task: Task[E]): Unit

  /** This method will be called when one of the ancestors of this task needs to run again.
    *
    * @param runningTask
    *   the task that needs to run again
    * @param reporter
    *   the task that reports, usually one of this task's children
    */
  def childRunning(runningTask: Task[E], reporter: Task[E]): Unit

  // ── Cancellation ──────────────────────────────────────────────────────

  /** Terminates this task and all its running children. This method MUST be called only if this task is running. */
  final def cancel(): Unit = {
    cancelRunningChildren(0)
    val previousStatus = status
    status = Task.Status.CANCELLED
    tree.foreach { t =>
      t.notifyStatusUpdated(this, previousStatus)
    }
    end()
  }

  /** Terminates the running children of this task starting from the specified index up to the end.
    * @param startIndex
    *   the start index
    */
  private[btree] def cancelRunningChildren(startIndex: Int): Unit = {
    var i = startIndex
    val n = getChildCount
    while (i < n) {
      val child = getChild(i)
      if (child.status == Task.Status.RUNNING) child.cancel()
      i += 1
    }
  }

  // ── Reset ─────────────────────────────────────────────────────────────

  /** Resets this task to make it restart from scratch on next run. */
  def resetTask(): Unit = {
    if (status == Task.Status.RUNNING) cancel()
    var i = 0
    val n = getChildCount
    while (i < n) {
      getChild(i).resetTask()
      i += 1
    }
    status = Task.Status.FRESH
    tree = Nullable.empty
    control = Nullable.empty
  }

  // ── Cloning ───────────────────────────────────────────────────────────

  /** Creates a new empty instance of this task type. Each concrete task must implement this to provide its own constructor call.
    *
    * This replaces the original `ClassReflection.newInstance(this.getClass())` fallback in the Java version.
    */
  def newInstance(): Task[E]

  /** Clones this task to a new one. If you don't specify a clone strategy through [[Task.taskCloner]] the new task is instantiated via `newInstance()` and `copyTo(Task)` is invoked.
    * @return
    *   the cloned task
    * @throws TaskCloneException
    *   if the task cannot be successfully cloned.
    */
  def cloneTask(): Task[E] =
    Task.taskCloner.fold {
      try {
        val clone = copyTo(newInstance())
        clone.guard = guard.map(_.cloneTask())
        clone
      } catch {
        case e: TaskCloneException => throw e
        case t: Throwable          => throw new TaskCloneException(t)
      }
    } { cloner =>
      try
        cloner.cloneTask(this)
      catch {
        case t: Throwable => throw new TaskCloneException(t)
      }
    }

  /** Copies this task to the given task. This method is invoked by `cloneTask()` only if [[Task.taskCloner]] is empty which is its default value.
    * @param task
    *   the task to be filled
    * @return
    *   the given task for chaining
    * @throws TaskCloneException
    *   if the task cannot be successfully copied.
    */
  protected def copyTo(task: Task[E]): Task[E]

  override def reset(): Unit = {
    control = Nullable.empty
    guard = Nullable.empty
    status = Task.Status.FRESH
    tree = Nullable.empty
  }
}

object Task {

  /** The clone strategy (if any) that `cloneTask()` will use. Defaults to `Nullable.empty`, meaning that `copyTo(Task)` is used instead. In this case, properly overriding this method in each task is
    * developer's responsibility.
    */
  var taskCloner: Nullable[TaskCloner] = Nullable.empty

  /** The enumeration of the values that a task's status can have.
    *
    * @author
    *   davebaol (original implementation)
    */
  enum Status extends java.lang.Enum[Status] {

    /** Means that the task has never run or has been reset. */
    case FRESH

    /** Means that the task needs to run again. */
    case RUNNING

    /** Means that the task returned a failure result. */
    case FAILED

    /** Means that the task returned a success result. */
    case SUCCEEDED

    /** Means that the task has been terminated by an ancestor. */
    case CANCELLED
  }
}
