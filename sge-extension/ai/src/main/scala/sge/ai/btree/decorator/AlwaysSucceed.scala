/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/btree/decorator/AlwaysSucceed.java
 * Original authors: implicit-invocation
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
 * Covenant-baseline-loc: 36
 * Covenant-baseline-methods: AlwaysSucceed,childFail,newInstance
 * Covenant-source-reference: auto
 * Covenant-verified: 2026-04-19
 */
package sge
package ai
package btree
package decorator

import sge.utils.Nullable

/** An `AlwaysSucceed` decorator will succeed no matter the wrapped task succeeds or fails.
  *
  * @tparam E
  *   type of the blackboard object that tasks use to read or modify game state
  *
  * @author
  *   implicit-invocation (original implementation)
  */
class AlwaysSucceed[E](
  child: Nullable[Task[E]] = Nullable.empty
) extends Decorator[E](child) {

  override def childFail(runningTask: Task[E]): Unit =
    childSuccess(runningTask)

  override def newInstance(): Task[E] = new AlwaysSucceed[E](Nullable.empty[Task[E]])
}
