/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/btree/leaf/Wait.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.btree.leaf` -> `sge.ai.btree.leaf`; `GdxAI.getTimepiece()` -> constructor `timepiece` parameter
 *   Convention: split packages, explicit Timepiece dependency instead of global singleton
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ai
package btree
package leaf

import sge.ai.Timepiece
import sge.ai.utils.random.ConstantFloatDistribution
import sge.ai.utils.random.FloatDistribution

/** `Wait` is a leaf that keeps running for the specified amount of time then succeeds.
  *
  * @tparam E
  *   type of the blackboard object that tasks use to read or modify game state
  *
  * @author
  *   davebaol (original implementation)
  */
class Wait[E](
  /** The random distribution determining the number of seconds to wait for. */
  var seconds: FloatDistribution = ConstantFloatDistribution.Zero,
  /** The timepiece used for tracking AI time. */
  val timepiece: Timepiece
) extends LeafTask[E] {

  /** Creates a `Wait` task running for the specified number of seconds. */
  def this(seconds: Float, timepiece: Timepiece) = this(new ConstantFloatDistribution(seconds), timepiece)

  private var startTime: Float = 0f
  private var timeout:   Float = 0f

  /** Draws a value from the distribution that determines the seconds to wait for.
    *
    * This method is called when the task is entered. Also, this method internally calls `timepiece.time` to get the current AI time. This means that:
    *   - if you forget to update the timepiece this task will keep running indefinitely.
    *   - the timepiece should be updated before this task runs.
    */
  override def start(): Unit = {
    timeout = seconds.nextFloat()
    startTime = timepiece.time
  }

  /** Executes this `Wait` task.
    * @return
    *   SUCCEEDED if the specified timeout has expired; RUNNING otherwise.
    */
  override def execute(): Task.Status =
    if (timepiece.time - startTime < timeout) Task.Status.RUNNING
    else Task.Status.SUCCEEDED

  override protected def copyTo(task: Task[E]): Task[E] = {
    val w = task.asInstanceOf[Wait[E]]
    w.seconds = seconds
    task
  }

  override def newInstance(): Task[E] = new Wait[E](seconds, timepiece)

  override def reset(): Unit = {
    seconds = ConstantFloatDistribution.Zero
    startTime = 0f
    timeout = 0f
    super.reset()
  }
}
