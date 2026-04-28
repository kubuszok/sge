/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/btree/decorator/Random.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.btree.decorator` -> `sge.ai.btree.decorator`; `MathUtils` -> `sge.math.MathUtils`
 *   Convention: split packages, Nullable instead of null
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 81
 * Covenant-baseline-methods: Random,childFail,childSuccess,copyTo,decide,newInstance,p,random,reset,run,start,successDistribution
 * Covenant-source-reference: com/badlogic/gdx/ai/btree/decorator/Random.java
 *   Renames: `com.badlogic.gdx.ai.btree.decorator` -> `sge.ai.btree.decorator`; `MathUtils` -> `sge.math.MathUtils`
 *   Convention: split packages, Nullable instead of null
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 81
 * Covenant-baseline-methods: Random,childFail,childSuccess,copyTo,decide,newInstance,p,random,reset,run,start,successDistribution
 * Covenant-verified: 2026-04-19
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 6726e345248ddcad7cec0737f6ad83e4e028266d
 */
package sge
package ai
package btree
package decorator

import sge.ai.utils.random.ConstantFloatDistribution
import sge.ai.utils.random.FloatDistribution
import sge.math.MathUtils
import sge.utils.Nullable

/** The `Random` decorator succeeds with the specified probability, regardless of whether the wrapped task fails or succeeds. Also, the wrapped task is optional, meaning that this decorator can act
  * like a leaf task.
  *
  * Notice that if success probability is 1 this task is equivalent to the decorator [[AlwaysSucceed]] and the leaf [[sge.ai.btree.leaf.Success]]. Similarly if success probability is 0 this task is
  * equivalent to the decorator [[AlwaysFail]] and the leaf [[sge.ai.btree.leaf.Failure]].
  *
  * @tparam E
  *   type of the blackboard object that tasks use to read or modify game state
  *
  * @author
  *   davebaol (original implementation)
  */
class Random[E](
  /** The random distribution that determines the success probability. Defaults to 0.5. */
  var successDistribution: FloatDistribution = ConstantFloatDistribution.ZeroPointFive,
  child:                   Nullable[Task[E]] = Nullable.empty
) extends Decorator[E](child) {

  private var p: Float = 0f

  /** Draws a value from the distribution that determines the success probability.
    *
    * This method is called when the task is entered.
    */
  override def start(): Unit =
    p = successDistribution.nextFloat()

  override def run(): Unit =
    if (child.isDefined)
      super.run()
    else
      decide()

  override def childFail(runningTask: Task[E]): Unit =
    decide()

  override def childSuccess(runningTask: Task[E]): Unit =
    decide()

  private def decide(): Unit =
    if (MathUtils.random() <= p)
      success()
    else
      fail()

  override protected def copyTo(task: Task[E]): Task[E] = {
    val random = task.asInstanceOf[Random[E]]
    random.successDistribution = successDistribution // no need to clone since it is immutable
    super.copyTo(task)
  }

  override def newInstance(): Task[E] = new Random[E](child = Nullable.empty[Task[E]])

  override def reset(): Unit = {
    this.p = 0f
    this.successDistribution = ConstantFloatDistribution.ZeroPointFive
    super.reset()
  }
}
