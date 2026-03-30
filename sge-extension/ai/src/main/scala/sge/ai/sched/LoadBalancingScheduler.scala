/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/sched/LoadBalancingScheduler.java
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

/** A `LoadBalancingScheduler` understands the time it has to run and distributes this time among the tasks that need to be run. This scheduler splits the time it is given according to the number of
  * tasks that must be run on this frame. To adjust for small errors in the running time of tasks, this scheduler recalculates the time it has left after each task is run. This way an overrunning task
  * will reduce the time that is given to others run in the same frame.
  *
  * The scheduler takes tasks, each one having a frequency and a phase that determine when it should be run.
  *   - '''Frequency:''' On each time frame, the scheduler is called to manage the whole AI budget. It decides which tasks need to be run and calls them. This is done by keeping count of the number of
  *     frames passed. This is incremented each time the scheduler is called. It is easy to test if each task should be run by checking if the frame count is evenly divisible by the frequency. On its
  *     own, this approach suffers from clumping: some frames with no tasks being run, and other frames with several tasks sharing the budget. Picking frequencies that are relatively prime makes the
  *     clash points less frequent but doesn't eliminate them. To solve the problem, we use the phase.
  *   - '''Phase:''' The phase doesn't change the frequency but offsets when the task will be called. However, calculating good phase values to avoid spikes can be difficult. It is not intuitively
  *     clear whether a particular set of frequency and phase values will lead to a regular spike or not. That's why this scheduler supports automatic phasing. When a new task is added to the
  *     scheduler, with a frequency of `f`, we perform a dry run of the scheduler for a fixed number of frames into the future. Rather than executing tasks in this dry run, we simply count how many
  *     would be executed. We find the frame with the least number of running tasks. The phase value for the task is set to the number of frames ahead at which this minimum occurs.
  *
  * @param dryRunFrames
  *   number of frames simulated by the dry run to calculate the phase when adding a schedulable via [[addWithAutomaticPhasing]]
  *
  * @author
  *   davebaol (original implementation)
  */
class LoadBalancingScheduler(dryRunFrames: Int) extends SchedulerBase[SchedulableRecord](dryRunFrames) {

  /** The current frame number */
  protected var frame: Int = 0

  /** Adds the `schedulable` to the list using the given `frequency` and a phase calculated by a dry run of the scheduler.
    * @param schedulable
    *   the task to schedule
    * @param frequency
    *   the frequency
    */
  override def addWithAutomaticPhasing(schedulable: Schedulable, frequency: Int): Unit =
    // Calculate the phase and add the schedulable to the list
    add(schedulable, frequency, calculatePhase(frequency))

  override def add(schedulable: Schedulable, frequency: Int, phase: Int): Unit =
    // Compile the record and add it to the list
    schedulableRecords.add(SchedulableRecord(schedulable, frequency, phase))

  /** Executes scheduled tasks based on their frequency and phase. This method must be called once per frame.
    * @param nanoTimeToRun
    *   the maximum time in nanoseconds this scheduler should run on the current frame.
    */
  override def run(nanoTimeToRun: Long): Unit = {
    // Increment the frame number
    frame += 1

    // Clear the list of tasks to run
    runList.clear()

    // Go through each task
    var i = 0
    while (i < schedulableRecords.size) {
      val record = schedulableRecords(i)
      // If it is due, schedule it
      if ((frame + record.phase) % record.frequency == 0) runList.add(record)
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
      val availableTime = timeToRun / (numToRun - i)

      // Run the schedulable object
      runList(i).schedulable.run(availableTime)

      // Store the current time
      lastTime = currentTime
      i += 1
    }
  }
}
