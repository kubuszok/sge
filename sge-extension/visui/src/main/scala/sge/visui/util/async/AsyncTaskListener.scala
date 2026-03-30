/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package visui
package util
package async

/** Allows to listen to events occurring in [[AsyncTask]].
  * @author
  *   Kotcrab
  */
trait AsyncTaskListener {

  /** Called when task status message has changed. */
  def messageChanged(message: String): Unit

  /** Called when task progress has changed. */
  def progressChanged(newProgressPercent: Int): Unit

  /** Called when task has finished executing. Finished will always called, even if some exception occurred during task execution.
    */
  def finished(): Unit

  /** Called when some error occurred during task execution. */
  def failed(message: String, exception: Exception): Unit
}
