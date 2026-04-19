/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/btree/decorator/Repeat.java
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
 * Covenant-baseline-loc: 74
 * Covenant-baseline-methods: Repeat,childFail,childSuccess,condition,copyTo,count,newInstance,repeat,reset,start,times
 * Covenant-source-reference: auto
 * Covenant-verified: 2026-04-19
 */
package sge
package ai
package btree
package decorator

import sge.ai.utils.random.ConstantIntegerDistribution
import sge.ai.utils.random.IntegerDistribution
import sge.utils.Nullable

/** A `Repeat` decorator will repeat the wrapped task a certain number of times, possibly infinite. This task always succeeds when reaches the specified number of repetitions.
  *
  * @tparam E
  *   type of the blackboard object that tasks use to read or modify game state
  *
  * @author
  *   implicit-invocation (original implementation)
  */
class Repeat[E](
  /** The integer distribution that determines how many times the wrapped task must be repeated. Any negative value means forever.
    */
  var times: IntegerDistribution = ConstantIntegerDistribution.NegativeOne,
  child:     Nullable[Task[E]] = Nullable.empty
) extends LoopDecorator[E](child) {

  private var count: Int = 0

  /** Draws a value from the distribution that determines how many times the wrapped task must be repeated. Any negative value means forever.
    *
    * This method is called when the task is entered.
    */
  override def start(): Unit =
    count = times.nextInt()

  override def condition(): Boolean = loop && count != 0

  override def childSuccess(runningTask: Task[E]): Unit = {
    if (count > 0) count -= 1
    if (count == 0) {
      super.childSuccess(runningTask)
      loop = false
    } else {
      loop = true
    }
  }

  override def childFail(runningTask: Task[E]): Unit =
    childSuccess(runningTask)

  override protected def copyTo(task: Task[E]): Task[E] = {
    val repeat = task.asInstanceOf[Repeat[E]]
    repeat.times = times // no need to clone since it is immutable
    super.copyTo(task)
  }

  override def newInstance(): Task[E] = new Repeat[E](child = Nullable.empty[Task[E]])

  override def reset(): Unit = {
    count = 0
    times = ConstantIntegerDistribution.NegativeOne
    super.reset()
  }
}
