/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/pfa/PathFinderRequest.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.pfa` -> `sge.ai.pfa`; `MessageManager.getInstance()` -> `MessageManager`
 *   Convention: split packages; `null` -> `Nullable`
 *   Idiom: `= _` -> `scala.compiletime.uninitialized`
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 102
 * Covenant-baseline-methods: PathFinderRequest,SEARCH_DONE,SEARCH_FINALIZED,SEARCH_INITIALIZED,SEARCH_NEW,changeStatus,client,dispatcher,endNode,executionFrames,finalizeSearch,heuristic,initializeSearch,pathFound,responseMessageCode,resultPath,search,startNode,status,statusChanged,this
 * Covenant-source-reference: com/badlogic/gdx/ai/pfa/PathFinderRequest.java
 *   Renames: `com.badlogic.gdx.ai.pfa` -> `sge.ai.pfa`; `MessageManager.getInstance()` -> `MessageManager`
 *   Convention: split packages; `null` -> `Nullable`
 *   Idiom: `= _` -> `scala.compiletime.uninitialized`
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 102
 * Covenant-baseline-methods: PathFinderRequest,SEARCH_DONE,SEARCH_FINALIZED,SEARCH_INITIALIZED,SEARCH_NEW,changeStatus,client,dispatcher,endNode,executionFrames,finalizeSearch,heuristic,initializeSearch,pathFound,responseMessageCode,resultPath,search,startNode,status,statusChanged,this
 * Covenant-verified: 2026-04-19
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 6726e345248ddcad7cec0737f6ad83e4e028266d
 */
package sge
package ai
package pfa

import sge.ai.msg.MessageDispatcher
import sge.ai.msg.MessageManager
import sge.ai.msg.Telegraph
import sge.utils.Nullable

/** A request for interruptible pathfinding that should be sent to a [[PathFinderQueue]] through a [[sge.ai.msg.Telegram Telegram]].
  *
  * @tparam N
  *   Type of node
  *
  * @author
  *   davebaol (original implementation)
  */
class PathFinderRequest[N] {

  var startNode:           N                           = scala.compiletime.uninitialized
  var endNode:             N                           = scala.compiletime.uninitialized
  var heuristic:           Heuristic[N]                = scala.compiletime.uninitialized
  var resultPath:          GraphPath[N]                = scala.compiletime.uninitialized
  var executionFrames:     Int                         = 0
  var pathFound:           Boolean                     = false
  var status:              Int                         = PathFinderRequest.SEARCH_NEW
  var statusChanged:       Boolean                     = false
  var client:              Nullable[Telegraph]         = Nullable.empty
  var responseMessageCode: Int                         = 0
  var dispatcher:          Nullable[MessageDispatcher] = Nullable.empty

  /** Creates a `PathFinderRequest` with the given arguments. */
  def this(startNode: N, endNode: N, heuristic: Heuristic[N], resultPath: GraphPath[N], dispatcher: MessageDispatcher) = {
    this()
    this.startNode = startNode
    this.endNode = endNode
    this.heuristic = heuristic
    this.resultPath = resultPath
    this.dispatcher = Nullable(dispatcher)

    this.executionFrames = 0
    this.pathFound = false
    this.status = PathFinderRequest.SEARCH_NEW
    this.statusChanged = false
  }

  /** Creates a `PathFinderRequest` with the given arguments that uses the singleton message dispatcher provided by [[MessageManager]].
    */
  def this(startNode: N, endNode: N, heuristic: Heuristic[N], resultPath: GraphPath[N]) =
    this(startNode, endNode, heuristic, resultPath, MessageManager)

  def changeStatus(newStatus: Int): Unit = {
    this.status = newStatus
    this.statusChanged = true
  }

  /** Interruptible method called by the [[PathFinderRequestControl]] as soon as this request starts to be served.
    * @param timeToRun
    *   the time in nanoseconds that this call can use on the current frame
    * @return
    *   `true` if initialization has completed; `false` if more time is needed to complete.
    */
  def initializeSearch(timeToRun: Long): Boolean = true

  /** @param pathFinder
    *   the path finder
    * @param timeToRun
    *   the time in nanoseconds that this call can use on the current frame
    * @return
    *   `true` if the search has completed; `false` if more time is needed to complete.
    */
  def search(pathFinder: PathFinder[N], timeToRun: Long): Boolean =
    pathFinder.search(this, timeToRun)

  /** Interruptible method called by [[PathFinderQueue]] when the path finder has completed the search. You have to check the [[pathFound]] field of this request to know if a path has been found.
    * @param timeToRun
    *   the time in nanoseconds that this call can use on the current frame
    * @return
    *   `true` if finalization has completed; `false` if more time is needed to complete.
    */
  def finalizeSearch(timeToRun: Long): Boolean = true
}

object PathFinderRequest {
  val SEARCH_NEW:         Int = 0
  val SEARCH_INITIALIZED: Int = 1
  val SEARCH_DONE:        Int = 2
  val SEARCH_FINALIZED:   Int = 3
}
