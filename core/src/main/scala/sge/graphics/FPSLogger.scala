/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/FPSLogger.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Idiom: split packages
 *   Issues: log() uses placeholder `val fps = 60` instead of Sge().graphics.getFramesPerSecond(); named (using sde: Sge) instead of anonymous (using Sge)
 *   TODO: named context parameter (implicit/using sge/sde: Sge) → anonymous (using Sge) + Sge() accessor
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics

import sge.utils.TimeUtils

/** A simple helper class to log the frames per seconds achieved. Just invoke the {@link #log()} method in your rendering method. The output will be logged once per second.
  *
  * @author
  *   mzechner (original implementation)
  */
class FPSLogger(bound: Int = Int.MaxValue)(using sde: Sge) {
  private var startTime: Long = TimeUtils.nanoTime()
  private var _bound:    Int  = bound

  def setBound(bound: Int): Unit = {
    this._bound = bound
    startTime = TimeUtils.nanoTime()
  }

  /** Logs the current frames per seconds to the console. */
  def log(): Unit = {
    val nanoTime = TimeUtils.nanoTime()
    if (nanoTime - startTime > 1000000000L) { /* 1,000,000,000ns == one second */
      // TODO: Need to implement getFramesPerSecond in Graphics trait
      val fps = 60 // Placeholder until Graphics trait has getFramesPerSecond method
      if (fps < _bound) {
        sde.application.log("FPSLogger", s"fps: $fps")
        startTime = nanoTime
      }
    }
  }
}
