/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/btree/decorator/AlwaysFail.java
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
 * Covenant-baseline-methods: AlwaysFail,childSuccess,newInstance
 * Covenant-source-reference: auto
 * Covenant-verified: 2026-04-19
 */
package sge
package ai
package btree
package decorator

import sge.utils.Nullable

/** An `AlwaysFail` decorator will fail no matter the wrapped task fails or succeeds.
  *
  * @tparam E
  *   type of the blackboard object that tasks use to read or modify game state
  *
  * @author
  *   implicit-invocation (original implementation)
  */
class AlwaysFail[E](
  child: Nullable[Task[E]] = Nullable.empty
) extends Decorator[E](child) {

  override def childSuccess(runningTask: Task[E]): Unit =
    childFail(runningTask)

  override def newInstance(): Task[E] = new AlwaysFail[E](Nullable.empty[Task[E]])
}
