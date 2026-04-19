/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/btree/Decorator.java
 * Original authors: implicit-invocation, davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.btree` -> `sge.ai.btree`
 *   Convention: split packages, Nullable instead of null
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 79
 * Covenant-baseline-methods: Decorator,addChildToTask,c,child,childFail,childRunning,childSuccess,copyTo,getChild,getChildCount,reset,run
 * Covenant-source-reference: com/badlogic/gdx/ai/btree/Decorator.java
 * Covenant-verified: 2026-04-19
 */
package sge
package ai
package btree

import sge.utils.Nullable

/** A `Decorator` is a wrapper that provides custom behavior for its child. The child can be of any kind (branch task, leaf task, or another decorator).
  *
  * @tparam E
  *   type of the blackboard object that tasks use to read or modify game state
  *
  * @author
  *   implicit-invocation, davebaol (original implementation)
  */
abstract class Decorator[E](
  /** The child task wrapped by this decorator. */
  protected var child: Nullable[Task[E]] = Nullable.empty
) extends Task[E] {

  override protected def addChildToTask(child: Task[E]): Int = {
    if (this.child.isDefined) throw new IllegalStateException("A decorator task cannot have more than one child")
    this.child = Nullable(child)
    0
  }

  override def getChildCount: Int = if (child.isDefined) 1 else 0

  override def getChild(i: Int): Task[E] =
    if (i == 0) child.getOrElse(throw new IndexOutOfBoundsException("index can't be >= size: 0 >= 0"))
    else throw new IndexOutOfBoundsException(s"index can't be >= size: $i >= $getChildCount")

  override def run(): Unit = {
    val c = child.get
    if (c.status == Task.Status.RUNNING) {
      c.run()
    } else {
      c.setControl(this)
      c.start()
      if (c.checkGuard(this))
        c.run()
      else
        c.fail()
    }
  }

  override def childRunning(runningTask: Task[E], reporter: Task[E]): Unit =
    running()

  override def childFail(runningTask: Task[E]): Unit =
    fail()

  override def childSuccess(runningTask: Task[E]): Unit =
    success()

  override protected def copyTo(task: Task[E]): Task[E] = {
    child.foreach { c =>
      val decorator = task.asInstanceOf[Decorator[E]]
      decorator.child = Nullable(c.cloneTask())
    }
    task
  }

  override def reset(): Unit = {
    child = Nullable.empty
    super.reset()
  }
}
