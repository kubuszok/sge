/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/btree/decorator/UntilFail.java
 * Original authors: implicit-invocation, davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.btree.decorator` -> `sge.ai.btree.decorator`
 *   Convention: split packages, Nullable instead of null
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ai
package btree
package decorator

import sge.utils.Nullable

/** The `UntilFail` decorator will repeat the wrapped task until that task fails, which makes the decorator succeed.
  *
  * Notice that a wrapped task that always succeeds without entering the running status will cause an infinite loop in the current frame.
  *
  * @tparam E
  *   type of the blackboard object that tasks use to read or modify game state
  *
  * @author
  *   implicit-invocation, davebaol (original implementation)
  */
class UntilFail[E](
  child: Nullable[Task[E]] = Nullable.empty
) extends LoopDecorator[E](child) {

  override def childSuccess(runningTask: Task[E]): Unit =
    loop = true

  override def childFail(runningTask: Task[E]): Unit = {
    success()
    loop = false
  }

  override def newInstance(): Task[E] = new UntilFail[E](Nullable.empty[Task[E]])
}
