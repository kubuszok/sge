/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/btree/decorator/Invert.java
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
 * Covenant-baseline-loc: 39
 * Covenant-baseline-methods: Invert,childFail,childSuccess,newInstance
 * Covenant-source-reference: auto
 * Covenant-verified: 2026-04-19
 */
package sge
package ai
package btree
package decorator

import sge.utils.Nullable

/** An `Invert` decorator will succeed if the wrapped task fails and will fail if the wrapped task succeeds.
  *
  * @tparam E
  *   type of the blackboard object that tasks use to read or modify game state
  *
  * @author
  *   implicit-invocation (original implementation)
  */
class Invert[E](
  child: Nullable[Task[E]] = Nullable.empty
) extends Decorator[E](child) {

  override def childSuccess(runningTask: Task[E]): Unit =
    super.childFail(runningTask)

  override def childFail(runningTask: Task[E]): Unit =
    super.childSuccess(runningTask)

  override def newInstance(): Task[E] = new Invert[E](Nullable.empty[Task[E]])
}
