/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/btree/LeafTask.java
 * Original authors: implicit-invocation, davebaol
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

/** A `LeafTask` is a terminal task of a behavior tree, contains action or condition logic, can not have any child.
  *
  * @tparam E
  *   type of the blackboard object that tasks use to read or modify game state
  *
  * @author
  *   implicit-invocation, davebaol (original implementation)
  */
abstract class LeafTask[E] extends Task[E] {

  /** This method contains the update logic of this leaf task. The actual implementation MUST return one of RUNNING, SUCCEEDED or FAILED. Other return values will cause an `IllegalStateException`.
    * @return
    *   the status of this leaf task
    */
  def execute(): Task.Status

  /** This method contains the update logic of this task. The implementation delegates to the `execute()` method. */
  final override def run(): Unit = {
    val result = execute()
    result match {
      case Task.Status.SUCCEEDED => success()
      case Task.Status.FAILED    => fail()
      case Task.Status.RUNNING   => running()
      case other                 =>
        throw new IllegalStateException(s"Invalid status '$other' returned by the execute method")
    }
  }

  /** Always throws `IllegalStateException` because a leaf task cannot have any children. */
  override protected def addChildToTask(child: Task[E]): Int =
    throw new IllegalStateException("A leaf task cannot have any children")

  override def getChildCount: Int = 0

  override def getChild(i: Int): Task[E] =
    throw new IndexOutOfBoundsException("A leaf task can not have any child")

  final override def childRunning(runningTask: Task[E], reporter: Task[E]): Unit = {}
  final override def childFail(runningTask:    Task[E]):                    Unit = {}
  final override def childSuccess(runningTask: Task[E]):                    Unit = {}
}
