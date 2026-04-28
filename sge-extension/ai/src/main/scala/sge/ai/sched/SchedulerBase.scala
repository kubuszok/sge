/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/sched/SchedulerBase.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.sched` -> `sge.ai.sched`; `Array` -> `DynamicArray`; `IntArray` -> `DynamicArray[Int]`
 *   Convention: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 100
 * Covenant-baseline-methods: SchedulableRecord,SchedulerBase,calculatePhase,dryRunFrames,frame,frequency,i,minValue,minValueAt,phase,phaseCounters,runList,schedulable,schedulableRecords
 * Covenant-source-reference: com/badlogic/gdx/ai/sched/SchedulerBase.java
 *   Renames: `com.badlogic.gdx.ai.sched` -> `sge.ai.sched`; `Array` -> `DynamicArray`; `IntArray` -> `DynamicArray[Int]`
 *   Convention: split packages
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 100
 * Covenant-baseline-methods: SchedulableRecord,SchedulerBase,calculatePhase,dryRunFrames,frame,frequency,i,minValue,minValueAt,phase,phaseCounters,runList,schedulable,schedulableRecords
 * Covenant-verified: 2026-04-19
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 6726e345248ddcad7cec0737f6ad83e4e028266d
 */
package sge
package ai
package sched

import sge.utils.DynamicArray
import sge.utils.MkArray

/** Base class for scheduler implementations.
  *
  * @tparam T
  *   the type of schedulable record
  * @param dryRunFrames
  *   number of frames simulated by the dry run to calculate the phase when adding a schedulable via [[addWithAutomaticPhasing]]
  *
  * @author
  *   davebaol (original implementation)
  */
abstract class SchedulerBase[T <: SchedulableRecord](
  protected val dryRunFrames: Int
)(using mk: MkArray[T])
    extends Scheduler {

  /** The list of the scheduled tasks. */
  protected val schedulableRecords: DynamicArray[T] = DynamicArray.createWithMk[T](mk, 16, true)

  protected val runList: DynamicArray[T] = DynamicArray.createWithMk[T](mk, 16, true)

  protected val phaseCounters: DynamicArray[Int] = DynamicArray[Int]()

  /** This method is invoked by [[addWithAutomaticPhasing]] and calculates the best phase based on the number of frames of the dry run. The optimal phase is guaranteed if the number of simulated
    * frames is at least as large as the size of the least common multiple (LCM, see [[ArithmeticUtils.lcmPositive]]) of all the frequency values used in the scheduler so far.
    * @param frequency
    *   the frequency of the schedulable task to add
    * @return
    *   the best phase based on the length of the dry run.
    */
  protected def calculatePhase(frequency: Int): Int = {
    if (frequency > phaseCounters.size) phaseCounters.ensureCapacity(frequency - phaseCounters.size)

    // Reset counters
    phaseCounters.setSize(frequency)
    var i = 0
    while (i < frequency) {
      phaseCounters(i) = 0
      i += 1
    }

    // Perform a dry run
    var frame = 0
    while (frame < dryRunFrames) {
      val slot = frame % frequency
      // Go through each task
      i = 0
      while (i < schedulableRecords.size) {
        val record = schedulableRecords(i)
        // If it is due, count it
        if ((frame - record.phase) % record.frequency == 0) phaseCounters(slot) = phaseCounters(slot) + 1
        i += 1
      }
      frame += 1
    }

    var minValue   = Int.MaxValue
    var minValueAt = -1
    i = 0
    while (i < frequency) {
      if (phaseCounters(i) < minValue) {
        minValue = phaseCounters(i)
        minValueAt = i
      }
      i += 1
    }

    // Return the phase
    minValueAt
  }
}

/** A scheduled task.
  *
  * @author
  *   davebaol (original implementation)
  */
class SchedulableRecord(
  val schedulable: Schedulable,
  val frequency:   Int,
  val phase:       Int
) {}
