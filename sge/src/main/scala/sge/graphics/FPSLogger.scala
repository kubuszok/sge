/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/FPSLogger.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: Gdx.app.log -> utils.Log.info; Gdx.graphics -> Sge().graphics
 *   Convention: anonymous (using Sge) + Sge() accessor; logging via scribe
 *   Idiom: split packages
 *   Audited: 2026-03-04
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics

import sge.utils.{ Nanos, TimeUtils }

/** A simple helper class to log the frames per seconds achieved. Just invoke the {@link #log()} method in your rendering method. The output will be logged once per second.
  *
  * @author
  *   mzechner (original implementation)
  */
class FPSLogger(bound: Int = Int.MaxValue)(using Sge) {
  private var startTime: Nanos = TimeUtils.nanoTime()
  private var _bound:    Int   = bound

  def setBound(bound: Int): Unit = {
    this._bound = bound
    startTime = TimeUtils.nanoTime()
  }

  /** Logs the current frames per seconds to the console. */
  def log(): Unit = {
    val nanoTime = TimeUtils.nanoTime()
    if (nanoTime - startTime > Nanos(1000000000L)) { /* 1,000,000,000ns == one second */
      val fps = Sge().graphics.getFramesPerSecond()
      if (fps < _bound) {
        utils.Log.info(s"fps: $fps")
        startTime = nanoTime
      }
    }
  }
}
