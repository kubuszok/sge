/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/btree/BehaviorTree.java
 * Original authors: implicit-invocation, davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.btree` -> `sge.ai.btree`; `Array` -> `DynamicArray`
 *   Convention: split packages, Nullable instead of null
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ai
package btree

import sge.utils.DynamicArray
import sge.utils.Nullable

/** The behavior tree itself.
  *
  * @tparam E
  *   type of the blackboard object that tasks use to read or modify game state
  *
  * @author
  *   implicit-invocation, davebaol (original implementation)
  */
class BehaviorTree[E](
  private var rootTask: Nullable[Task[E]] = Nullable.empty,
  private var obj:      Nullable[E] = Nullable.empty
) extends Task[E] {

  this.tree = Nullable(this)
  private[btree] val guardEvaluator: GuardEvaluator[E] = new GuardEvaluator[E](this)

  private var _listeners: Nullable[DynamicArray[BehaviorTree.Listener[E]]] = Nullable.empty

  /** Returns the blackboard object of this behavior tree. */
  override def getObject: E =
    obj.getOrElse(throw new IllegalStateException("Blackboard object not set"))

  /** Sets the blackboard object of this behavior tree.
    *
    * @param obj
    *   the new blackboard
    */
  def setObject(obj: E): Unit =
    this.obj = Nullable(obj)

  /** This method will add a child, namely the root, to this behavior tree.
    *
    * @param child
    *   the root task to add
    * @return
    *   the index where the root task has been added (always 0).
    * @throws IllegalStateException
    *   if the root task is already set.
    */
  override protected def addChildToTask(child: Task[E]): Int = {
    if (this.rootTask.isDefined) throw new IllegalStateException("A behavior tree cannot have more than one root task")
    this.rootTask = Nullable(child)
    0
  }

  override def getChildCount: Int = if (rootTask.isDefined) 1 else 0

  override def getChild(i: Int): Task[E] =
    if (i == 0) rootTask.getOrElse(throw new IndexOutOfBoundsException("index can't be >= size: 0 >= 0"))
    else throw new IndexOutOfBoundsException(s"index can't be >= size: $i >= $getChildCount")

  override def childRunning(runningTask: Task[E], reporter: Task[E]): Unit =
    running()

  override def childFail(runningTask: Task[E]): Unit =
    fail()

  override def childSuccess(runningTask: Task[E]): Unit =
    success()

  /** This method should be called when game entity needs to make decisions: call this in game loop or after a fixed time slice if the game is real-time, or on entity's turn if the game is turn-based.
    */
  def step(): Unit = {
    val root = rootTask.getOrElse(throw new IllegalStateException("Root task not set"))
    if (root.status == Task.Status.RUNNING) {
      root.run()
    } else {
      root.setControl(this)
      root.start()
      if (root.checkGuard(this))
        root.run()
      else
        root.fail()
    }
  }

  override def run(): Unit = {}

  override def resetTask(): Unit = {
    super.resetTask()
    tree = Nullable(this)
  }

  override protected def copyTo(task: Task[E]): Task[E] = {
    val bt = task.asInstanceOf[BehaviorTree[E]]
    bt.rootTask = rootTask.map(_.cloneTask())
    task
  }

  override def newInstance(): Task[E] = new BehaviorTree[E](Nullable.empty[Task[E]], Nullable.empty[E])

  def addListener(listener: BehaviorTree.Listener[E]): Unit = {
    val ls = _listeners.getOrElse {
      val arr = DynamicArray[BehaviorTree.Listener[E]]()
      _listeners = Nullable(arr)
      arr
    }
    ls.add(listener)
  }

  def removeListener(listener: BehaviorTree.Listener[E]): Unit =
    Nullable.foreach(_listeners)(_.removeValue(listener))

  def removeListeners(): Unit =
    Nullable.foreach(_listeners)(_.clear())

  def notifyStatusUpdated(task: Task[E], previousStatus: Task.Status): Unit =
    Nullable.foreach(_listeners) { ls =>
      var i = 0
      while (i < ls.size) {
        ls(i).statusUpdated(task, previousStatus)
        i += 1
      }
    }

  def notifyChildAdded(task: Task[E], index: Int): Unit =
    Nullable.foreach(_listeners) { ls =>
      var i = 0
      while (i < ls.size) {
        ls(i).childAdded(task, index)
        i += 1
      }
    }

  override def reset(): Unit = {
    removeListeners()
    this.rootTask = Nullable.empty
    this.obj = Nullable.empty
    this._listeners = Nullable.empty
    super.reset()
  }
}

object BehaviorTree {

  /** The listener interface for receiving task events. The class that is interested in processing a task event implements this interface, and the object created with that class is registered with a
    * behavior tree, using the [[BehaviorTree.addListener]] method. When a task event occurs, the corresponding method is invoked.
    *
    * @tparam E
    *   type of the blackboard object that tasks use to read or modify game state
    *
    * @author
    *   davebaol (original implementation)
    */
  trait Listener[E] {

    /** This method is invoked when the task status is set. This does not necessarily mean that the status has changed.
      * @param task
      *   the task whose status has been set
      * @param previousStatus
      *   the task's status before the update
      */
    def statusUpdated(task: Task[E], previousStatus: Task.Status): Unit

    /** This method is invoked when a child task is added to the children of a parent task.
      * @param task
      *   the parent task of the newly added child
      * @param index
      *   the index where the child has been added
      */
    def childAdded(task: Task[E], index: Int): Unit
  }
}

/** Internal task used for guard evaluation. It acts as a dummy control for guards during their evaluation run.
  *
  * @author
  *   davebaol (original implementation)
  */
private[btree] class GuardEvaluator[E](bt: BehaviorTree[E]) extends Task[E] {
  this.tree = Nullable(bt)

  override protected def addChildToTask(child: Task[E]):                    Int     = 0
  override def getChildCount:                                               Int     = 0
  override def getChild(i:                     Int):                        Task[E] = throw new IndexOutOfBoundsException("GuardEvaluator has no children")
  override def run():                                                       Unit    = {}
  override def childSuccess(task:              Task[E]):                    Unit    = {}
  override def childFail(task:                 Task[E]):                    Unit    = {}
  override def childRunning(runningTask:       Task[E], reporter: Task[E]): Unit    = {}
  override protected def copyTo(task:          Task[E]):                    Task[E] = task
  override def newInstance():                                               Task[E] = new GuardEvaluator[E](bt)
}
