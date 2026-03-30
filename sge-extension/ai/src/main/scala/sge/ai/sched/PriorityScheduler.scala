/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/sched/PriorityScheduler.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.sched` -> `sge.ai.sched`; `TimeUtils` -> `sge.utils.TimeUtils`
 *   Convention: split packages; `Nanos` opaque type for time values
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ai
package sched

import sge.utils.TimeUtils

/** A `PriorityScheduler` works like a [[LoadBalancingScheduler]] but allows different tasks to get a different share of the available time by assigning a priority to each task. The higher the
  * priority the longer the amount of the available time dedicated to the corresponding task.
  *
  * @param dryRunFrames
  *   number of frames simulated by the dry run to calculate the phase when adding a schedulable via [[addWithAutomaticPhasing(Schedulable,Int)]] and [[addWithAutomaticPhasing(Schedulable,Int,Float)]]
  *
  * @author
  *   davebaol (original implementation)
  */
class PriorityScheduler(dryRunFrames: Int) extends SchedulerBase[PrioritySchedulableRecord](dryRunFrames) {

  /** The current frame number */
  protected var frame: Int = 0

  /** Executes scheduled tasks based on their frequency and phase. This method must be called once per frame.
    * @param nanoTimeToRun
    *   the maximum time in nanoseconds this scheduler should run on the current frame.
    */
  override def run(nanoTimeToRun: Long): Unit = {
    // Increment the frame number
    frame += 1

    // Clear the list of tasks to run and their total priority
    runList.clear()
    var totalPriority = 0f

    // Go through each task
    var i = 0
    while (i < schedulableRecords.size) {
      val record = schedulableRecords(i)
      // If it is due, schedule it
      if ((frame + record.phase) % record.frequency == 0) {
        runList.add(record)
        totalPriority += record.priority
      }
      i += 1
    }

    // Keep track of the current time
    var lastTime  = TimeUtils.nanoTime().toLong
    var timeToRun = nanoTimeToRun

    // Find the number of tasks we need to run
    val numToRun = runList.size

    // Go through the tasks to run
    i = 0
    while (i < numToRun) {
      // Find the available time
      val currentTime = TimeUtils.nanoTime().toLong
      timeToRun -= currentTime - lastTime
      val record        = runList(i)
      val availableTime = (timeToRun * record.priority / totalPriority).toLong

      // Run the schedulable object
      record.schedulable.run(availableTime)

      // Store the current time
      lastTime = currentTime
      i += 1
    }
  }

  /** Adds the `schedulable` to the list using the given `frequency`, priority 1 and a phase calculated by a dry run of the scheduler.
    * @param schedulable
    *   the task to schedule
    * @param frequency
    *   the frequency
    */
  override def addWithAutomaticPhasing(schedulable: Schedulable, frequency: Int): Unit =
    addWithAutomaticPhasing(schedulable, frequency, 1f)

  /** Adds the `schedulable` to the list using the given `frequency` and `priority` while the phase is calculated by a dry run of the scheduler.
    * @param schedulable
    *   the task to schedule
    * @param frequency
    *   the frequency
    * @param priority
    *   the priority
    */
  def addWithAutomaticPhasing(schedulable: Schedulable, frequency: Int, priority: Float): Unit =
    // Calculate the phase and add the schedulable to the list
    add(schedulable, frequency, calculatePhase(frequency), priority)

  /** Adds the `schedulable` to the list using the given `frequency` and `phase` with priority 1.
    * @param schedulable
    *   the task to schedule
    * @param frequency
    *   the frequency
    * @param phase
    *   the phase
    */
  override def add(schedulable: Schedulable, frequency: Int, phase: Int): Unit =
    add(schedulable, frequency, phase, 1f)

  /** Adds the `schedulable` to the list using the given `frequency`, `phase` and priority.
    * @param schedulable
    *   the task to schedule
    * @param frequency
    *   the frequency
    * @param phase
    *   the phase
    * @param priority
    *   the priority
    */
  def add(schedulable: Schedulable, frequency: Int, phase: Int, priority: Float): Unit =
    // Compile the record and add it to the list
    schedulableRecords.add(PrioritySchedulableRecord(schedulable, frequency, phase, priority))
}

/** A scheduled task with priority.
  *
  * @author
  *   davebaol (original implementation)
  */
class PrioritySchedulableRecord(
  schedulable:  Schedulable,
  frequency:    Int,
  phase:        Int,
  val priority: Float
) extends SchedulableRecord(schedulable, frequency, phase) {}
