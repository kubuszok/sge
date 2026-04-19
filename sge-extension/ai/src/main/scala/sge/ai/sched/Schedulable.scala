/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/sched/Schedulable.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.sched` -> `sge.ai.sched`
 *   Convention: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 29
 * Covenant-baseline-methods: Schedulable,run
 * Covenant-source-reference: auto
 * Covenant-verified: 2026-04-19
 */
package sge
package ai
package sched

/** Anything that can be scheduled by a [[Scheduler]] must implement this trait.
  *
  * @author
  *   davebaol (original implementation)
  */
trait Schedulable {

  /** Method invoked by the [[Scheduler]] when this schedulable needs to be run.
    * @param nanoTimeToRun
    *   the maximum time in nanoseconds this scheduler should run on the current frame.
    */
  def run(nanoTimeToRun: Long): Unit
}
