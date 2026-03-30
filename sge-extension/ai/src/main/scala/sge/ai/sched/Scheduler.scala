/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/sched/Scheduler.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.sched` -> `sge.ai.sched`
 *   Convention: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ai
package sched

/** A `Scheduler` works by assigning a pot of execution time among a variety of tasks, based on which ones need the time.
  *
  * Different AI tasks can and should be run at different frequencies. You can simply schedule some tasks to run every few frames and other tasks to run more frequently, slicing up the overall AI and
  * distributing it over time. It is a powerful technique for making sure that the game doesn't take too much AI time overall.
  *
  * The tasks that get called are passed timing information so they can decide when to stop running and return. However, note that there is nothing to stop a task from running for as long as it wants.
  * The scheduler trusts that they will be well behaved.
  *
  * Notes:
  *   - '''Hierarchical Scheduling''': `Scheduler` extends the [[Schedulable]] trait, allowing a scheduling system to be run as a task by another scheduler. This technique is known as hierarchical
  *     scheduling. Also, it's worth noting that with a hierarchical approach, there's no reason why the schedulers at different levels should be of the same kind. For instance, it is possible to use
  *     a frequency-based scheduler for the whole game and priority-based schedulers for individual characters.
  *   - '''Level of Detail''': On its own there is nothing that hierarchical scheduling provides that a single scheduler cannot handle. It comes into its own when used in combination with level of
  *     detail (LOD) systems. Level of detail systems are behavior selectors; they choose only one behavior to run. In a hierarchical structure this means that schedulers running the whole game don't
  *     need to know which behavior each character is running. A flat structure would mean removing and registering behaviors with the main scheduler each time.
  *
  * @author
  *   davebaol (original implementation)
  */
trait Scheduler extends Schedulable {

  /** Adds the `schedulable` to the list using the given `frequency` and a phase calculated by this scheduler.
    * @param schedulable
    *   the task to schedule
    * @param frequency
    *   the frequency
    */
  def addWithAutomaticPhasing(schedulable: Schedulable, frequency: Int): Unit

  /** Adds the `schedulable` to the list using the given `frequency` and `phase`
    * @param schedulable
    *   the task to schedule
    * @param frequency
    *   the frequency
    * @param phase
    *   the phase
    */
  def add(schedulable: Schedulable, frequency: Int, phase: Int): Unit
}
