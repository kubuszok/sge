/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/pfa/PathFinderRequestControl.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.pfa` -> `sge.ai.pfa`; `TimeUtils` -> `sge.utils.TimeUtils`;
 *     `MessageManager.getInstance()` -> `MessageManager`; `GdxAI.getLogger()` -> logging removed
 *   Convention: split packages; `null` -> `Nullable`; `return` -> `boundary`/`break`
 *   Idiom: `= _` -> `scala.compiletime.uninitialized`
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 113
 * Covenant-baseline-methods: PathFinderRequestControl,execute,lastTime,pathFinder,server,timeToRun,timeTolerance,timepiece
 * Covenant-source-reference: com/badlogic/gdx/ai/pfa/PathFinderRequestControl.java
 *   Renames: `com.badlogic.gdx.ai.pfa` -> `sge.ai.pfa`; `TimeUtils` -> `sge.utils.TimeUtils`;
 *   Convention: split packages; `null` -> `Nullable`; `return` -> `boundary`/`break`
 *   Idiom: `= _` -> `scala.compiletime.uninitialized`
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 113
 * Covenant-baseline-methods: PathFinderRequestControl,execute,lastTime,pathFinder,server,timeToRun,timeTolerance,timepiece
 * Covenant-verified: 2026-04-19
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 6726e345248ddcad7cec0737f6ad83e4e028266d
 */
package sge
package ai
package pfa

import sge.ai.msg.MessageManager
import sge.ai.msg.Telegraph
import sge.ai.Timepiece
import sge.utils.Nullable
import sge.utils.TimeUtils

import scala.util.boundary, boundary.break

/** A `PathFinderRequestControl` manages execution and resume of any interruptible [[PathFinderRequest]].
  *
  * @tparam N
  *   Type of node
  *
  * @author
  *   davebaol (original implementation)
  */
class PathFinderRequestControl[N] {

  var server:        Nullable[Telegraph] = Nullable.empty
  var pathFinder:    PathFinder[N]       = scala.compiletime.uninitialized
  var lastTime:      Long                = 0L
  var timeToRun:     Long                = 0L
  var timeTolerance: Long                = 0L
  var timepiece:     Nullable[Timepiece] = Nullable.empty

  /** Executes the given pathfinding request resuming it if needed.
    * @param request
    *   the pathfinding request
    * @return
    *   `true` if this operation has completed; `false` if more time is needed to complete.
    */
  def execute(request: PathFinderRequest[N]): Boolean = {

    request.executionFrames += 1

    boundary {
      while (true) {
        // Should perform search begin?
        if (request.status == PathFinderRequest.SEARCH_NEW) {
          val currentTime = TimeUtils.nanoTime().toLong
          timeToRun -= currentTime - lastTime
          if (timeToRun <= timeTolerance) break(false)
          if (!request.initializeSearch(timeToRun)) break(false)
          request.changeStatus(PathFinderRequest.SEARCH_INITIALIZED)
          lastTime = currentTime
        }

        // Should perform search path?
        if (request.status == PathFinderRequest.SEARCH_INITIALIZED) {
          val currentTime = TimeUtils.nanoTime().toLong
          timeToRun -= currentTime - lastTime
          if (timeToRun <= timeTolerance) break(false)
          if (!request.search(pathFinder, timeToRun)) break(false)
          request.changeStatus(PathFinderRequest.SEARCH_DONE)
          lastTime = currentTime
        }

        // Should perform search end?
        if (request.status == PathFinderRequest.SEARCH_DONE) {
          val currentTime = TimeUtils.nanoTime().toLong
          timeToRun -= currentTime - lastTime
          if (timeToRun <= timeTolerance) break(false)
          if (!request.finalizeSearch(timeToRun)) break(false)
          request.changeStatus(PathFinderRequest.SEARCH_FINALIZED)

          // Search finished, send result to the client
          server.foreach { srv =>
            timepiece.foreach { implicit tp: Timepiece =>
              val disp = request.dispatcher.getOrElse(MessageManager)
              request.client.foreach { client =>
                disp.dispatchMessage(
                  msg = request.responseMessageCode,
                  sender = Nullable(srv),
                  receiver = Nullable(client),
                  extraInfo = Nullable(request)
                )(using tp)
              }
            }
          }

          lastTime = currentTime

          if (request.statusChanged && request.status == PathFinderRequest.SEARCH_NEW) {
            // search renew - continue the loop
          } else {
            break(true)
          }
        } else {
          break(true)
        }
      }
      true // unreachable, but needed for type
    }
  }
}
