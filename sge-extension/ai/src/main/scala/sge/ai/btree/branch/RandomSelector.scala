/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/btree/branch/RandomSelector.java
 * Original authors: implicit-invocation
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.btree.branch` -> `sge.ai.btree.branch`; `Array` -> `DynamicArray`
 *   Convention: split packages, Nullable instead of null
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 39
 * Covenant-baseline-methods: RandomSelector,newInstance,start
 * Covenant-source-reference: com/badlogic/gdx/ai/btree/branch/RandomSelector.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 6726e345248ddcad7cec0737f6ad83e4e028266d
 */
package sge
package ai
package btree
package branch

import sge.utils.DynamicArray
import sge.utils.Nullable

/** A `RandomSelector` is a selector task's variant that runs its children in a random order.
  *
  * @tparam E
  *   type of the blackboard object that tasks use to read or modify game state
  *
  * @author
  *   implicit-invocation (original implementation)
  */
class RandomSelector[E](
  children: DynamicArray[Task[E]] = DynamicArray[Task[E]]()
) extends Selector[E](children) {

  override def start(): Unit = {
    super.start()
    if (randomChildren.isEmpty) randomChildren = Nullable(createRandomChildren())
  }

  override def newInstance(): Task[E] = new RandomSelector[E]()
}
