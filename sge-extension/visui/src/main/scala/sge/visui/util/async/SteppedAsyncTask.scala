/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 36
 * Covenant-baseline-methods: SteppedAsyncTask,nextStep,setTotalSteps,step,totalSteps
 * Covenant-source-reference: com/kotcrab/vis/ui/util/async/SteppedAsyncTask.java
 * Covenant-verified: 2026-04-19
 */
package sge
package visui
package util
package async

/** [[AsyncTask]] that performs fixed numbers of steps, provides convenient methods to calculate and update task progress.
  * @author
  *   Kotcrab
  */
abstract class SteppedAsyncTask(threadName: String)(using Sge) extends AsyncTask(threadName) {
  private var step:       Int = 0
  private var totalSteps: Int = 0

  /** Sets total numbers of steps this task will have to perform, usually called at the beginning of [[doInBackground]].
    * @see
    *   [[nextStep]]
    */
  protected def setTotalSteps(totalSteps: Int): Unit = {
    this.totalSteps = totalSteps
    this.step = 0
    setProgressPercent(0)
  }

  /** Advances task to next step and updates its percent progress. */
  protected def nextStep(): Unit = {
    step += 1
    setProgressPercent(step * 100 / totalSteps)
  }
}
