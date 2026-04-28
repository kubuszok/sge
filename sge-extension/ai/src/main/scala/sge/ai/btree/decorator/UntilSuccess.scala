/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/btree/decorator/UntilSuccess.java
 * Original authors: implicit-invocation, davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.btree.decorator` -> `sge.ai.btree.decorator`
 *   Convention: split packages, Nullable instead of null
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 43
 * Covenant-baseline-methods: UntilSuccess,childFail,childSuccess,newInstance
 * Covenant-source-reference: com/badlogic/gdx/ai/btree/decorator/UntilSuccess.java
 *   Renames: `com.badlogic.gdx.ai.btree.decorator` -> `sge.ai.btree.decorator`
 *   Convention: split packages, Nullable instead of null
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 43
 * Covenant-baseline-methods: UntilSuccess,childFail,childSuccess,newInstance
 * Covenant-verified: 2026-04-19
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 6726e345248ddcad7cec0737f6ad83e4e028266d
 */
package sge
package ai
package btree
package decorator

import sge.utils.Nullable

/** The `UntilSuccess` decorator will repeat the wrapped task until that task succeeds, which makes the decorator succeed.
  *
  * Notice that a wrapped task that always fails without entering the running status will cause an infinite loop in the current frame.
  *
  * @tparam E
  *   type of the blackboard object that tasks use to read or modify game state
  *
  * @author
  *   implicit-invocation, davebaol (original implementation)
  */
class UntilSuccess[E](
  child: Nullable[Task[E]] = Nullable.empty
) extends LoopDecorator[E](child) {

  override def childSuccess(runningTask: Task[E]): Unit = {
    success()
    loop = false
  }

  override def childFail(runningTask: Task[E]): Unit =
    loop = true

  override def newInstance(): Task[E] = new UntilSuccess[E](Nullable.empty[Task[E]])
}
