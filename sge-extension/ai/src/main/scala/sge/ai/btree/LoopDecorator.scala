/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/btree/LoopDecorator.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.btree` -> `sge.ai.btree`
 *   Convention: split packages, Nullable instead of null
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ai
package btree

import sge.utils.Nullable

/** `LoopDecorator` is an abstract class providing basic functionalities for concrete looping decorators.
  *
  * @tparam E
  *   type of the blackboard object that tasks use to read or modify game state
  *
  * @author
  *   davebaol (original implementation)
  */
abstract class LoopDecorator[E](
  child: Nullable[Task[E]] = Nullable.empty
) extends Decorator[E](child) {

  /** Whether the `run()` method must keep looping or not. */
  protected var loop: Boolean = false

  /** Whether the `run()` method must keep looping or not.
    * @return
    *   `true` if it must keep looping; `false` otherwise.
    */
  def condition(): Boolean = loop

  override def run(): Unit = {
    loop = true
    while (condition()) {
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
  }

  override def childRunning(runningTask: Task[E], reporter: Task[E]): Unit = {
    super.childRunning(runningTask, reporter)
    loop = false
  }

  override def reset(): Unit = {
    loop = false
    super.reset()
  }
}
