/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/pfa/PathFinderQueue.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.pfa` -> `sge.ai.pfa`; `TimeUtils` -> `sge.utils.TimeUtils`;
 *     `CircularBuffer` -> `sge.ai.utils.CircularBuffer`; `Schedulable` removed (not ported);
 *     `Telegraph` -> `sge.ai.msg.Telegraph`; `Telegram` -> `sge.ai.msg.Telegram`
 *   Convention: split packages; `null` -> `Nullable`; `return` -> `boundary`/`break`
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 88
 * Covenant-baseline-methods: PathFinderQueue,TIME_TOLERANCE,currentRequest,handleMessage,requestControl,requestQueue,run,size
 * Covenant-source-reference: com/badlogic/gdx/ai/pfa/PathFinderQueue.java
 *   Renames: `com.badlogic.gdx.ai.pfa` -> `sge.ai.pfa`; `TimeUtils` -> `sge.utils.TimeUtils`;
 *   Convention: split packages; `null` -> `Nullable`; `return` -> `boundary`/`break`
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 88
 * Covenant-baseline-methods: PathFinderQueue,TIME_TOLERANCE,currentRequest,handleMessage,requestControl,requestQueue,run,size
 * Covenant-verified: 2026-04-19
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 6726e345248ddcad7cec0737f6ad83e4e028266d
 */
package sge
package ai
package pfa

import sge.ai.msg.Telegram
import sge.ai.msg.Telegraph
import sge.ai.Timepiece
import sge.ai.utils.CircularBuffer
import sge.utils.Nullable
import sge.utils.TimeUtils

import scala.util.boundary, boundary.break

/** @tparam N
  *   Type of node
  *
  * @author
  *   davebaol (original implementation)
  */
class PathFinderQueue[N](pathFinder: PathFinder[N]) extends Telegraph {

  private val requestQueue: CircularBuffer[PathFinderRequest[N]] = CircularBuffer[PathFinderRequest[N]](16)

  private var currentRequest: Nullable[PathFinderRequest[N]] = Nullable.empty

  private val requestControl: PathFinderRequestControl[N] = PathFinderRequestControl[N]()

  def run(timeToRun: Long)(using tp: Timepiece): Unit = {
    // Keep track of the current time
    requestControl.lastTime = TimeUtils.nanoTime().toLong
    requestControl.timeToRun = timeToRun

    requestControl.timeTolerance = PathFinderQueue.TIME_TOLERANCE
    requestControl.pathFinder = pathFinder
    requestControl.server = Nullable(this)
    requestControl.timepiece = Nullable(tp)

    // If no search in progress, take the next from the queue
    if (currentRequest.isEmpty) currentRequest = requestQueue.read()

    boundary {
      while (currentRequest.isDefined) {
        val req = currentRequest.get

        val finished = requestControl.execute(req)

        if (!finished) break(())

        // Read next request from the queue
        currentRequest = requestQueue.read()
      }
    }
  }

  override def handleMessage(msg: Telegram): Boolean = {
    msg.extraInfo.foreach { info =>
      val pfr = info.asInstanceOf[PathFinderRequest[N]]
      msg.sender.foreach { sender =>
        pfr.client = Nullable(sender) // set the client to be notified once the request has completed
      }
      pfr.status = PathFinderRequest.SEARCH_NEW // Reset status
      pfr.statusChanged = true // Status has just changed
      pfr.executionFrames = 0 // Reset execution frames counter
      requestQueue.store(pfr)
    }
    true
  }

  def size: Int = requestQueue.size
}

object PathFinderQueue {
  val TIME_TOLERANCE: Long = 100L
}
