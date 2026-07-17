/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/pfa/PathFinderQueue.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.pfa` -> `sge.ai.pfa`; `TimeUtils` -> `sge.utils.TimeUtils`;
 *     `CircularBuffer` -> `sge.ai.utils.CircularBuffer`; `Schedulable` -> `sge.ai.sched.Schedulable`;
 *     `Telegraph` -> `sge.ai.msg.Telegraph`; `Telegram` -> `sge.ai.msg.Telegram`;
 *     `GdxAI.getTimepiece()` -> constructor `timepiece` parameter (like `btree.leaf.Wait`)
 *   Convention: split packages; `null` -> `Nullable`; `return` -> `boundary`/`break`
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 104
 * Covenant-baseline-methods: PathFinderQueue,TIME_TOLERANCE,currentRequest,handleMessage,requestControl,requestQueue,run,size,timepiece
 * Covenant-source-reference: com/badlogic/gdx/ai/pfa/PathFinderQueue.java
 * Covenant-verified: 2026-06-13
 *
 * upstream-commit: 6726e345248ddcad7cec0737f6ad83e4e028266d
 */
package sge
package ai
package pfa

import sge.ai.msg.Telegram
import sge.ai.msg.Telegraph
import sge.ai.sched.Schedulable
import sge.ai.Timepiece
import sge.ai.utils.CircularBuffer
import lowlevel.Nullable
import sge.utils.TimeUtils

import scala.util.boundary, boundary.break

/** @tparam N
  *   Type of node
  *
  * @author
  *   davebaol (original implementation)
  */
class PathFinderQueue[N](
  pathFinder: PathFinder[N],
  /** The timepiece used for tracking AI time. */
  val timepiece: Timepiece
) extends Schedulable,
      Telegraph {

  private val requestQueue: CircularBuffer[PathFinderRequest[N]] = CircularBuffer[PathFinderRequest[N]](16)

  private var currentRequest: Nullable[PathFinderRequest[N]] = Nullable.empty

  private val requestControl: PathFinderRequestControl[N] = PathFinderRequestControl[N]()

  override def run(timeToRun: Long): Unit = {
    // Keep track of the current time
    requestControl.lastTime = TimeUtils.nanoTime().toLong
    requestControl.timeToRun = timeToRun

    requestControl.timeTolerance = PathFinderQueue.TIME_TOLERANCE
    requestControl.pathFinder = pathFinder
    requestControl.server = Nullable(this)
    requestControl.timepiece = Nullable(timepiece)

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
    // PathFinderQueue.java:74-75 — `PathFinderRequest<N> pfr = (PathFinderRequest<N>)telegram.extraInfo;`
    // then `pfr.client = telegram.sender;` dereferences extraInfo UNCONDITIONALLY. A telegram carrying no
    // request payload is a protocol error; the original fails fast (NPE on the deref). We surface the same
    // fail-fast with a named condition instead of silently swallowing the malformed telegram (ISS-730 c4
    // ruling, wave-F).
    //
    // Deviation: the c4 ruling nominated SgeError.InvalidInput, but the frozen red suite
    // (PathFinderQueueExtraInfoIss730RedSuite) pins `intercept[NullPointerException]`; SgeError extends
    // Exception (not NPE) and lives outside this territory. The faithful unconditional deref throws
    // NullPointerException, matching both upstream behavior and the red contract (whose own doc prescribes
    // `extraInfo.get`).
    if (msg.extraInfo.isEmpty)
      throw new NullPointerException(
        "payload-less telegram delivered to PathFinderQueue.handleMessage: extraInfo carried no PathFinderRequest"
      )
    val pfr = msg.extraInfo.get.asInstanceOf[PathFinderRequest[N]]
    pfr.client = msg.sender // set the client to be notified once the request has completed (empty when broadcast)
    pfr.status = PathFinderRequest.SEARCH_NEW // Reset status
    pfr.statusChanged = true // Status has just changed
    pfr.executionFrames = 0 // Reset execution frames counter
    requestQueue.store(pfr)
    true
  }

  def size: Int = requestQueue.size
}

object PathFinderQueue {
  val TIME_TOLERANCE: Long = 100L
}
