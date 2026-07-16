package sge
package ai
package pfa

import sge.ai.DefaultTimepiece
import sge.ai.msg.MessageDispatcher
import sge.ai.msg.Telegram
import sge.ai.msg.Telegraph
import sge.ai.pfa.indexed.IndexedAStarPathFinder
import sge.utils.Pool
import lowlevel.Nullable

/** Red test for ISS-729: the port silently drops the path-completed dispatch when the request has no explicit `client`.
  *
  * In the original gdx-ai the completion dispatch is unconditional whenever the request-control has a `server`:
  *   - com/badlogic/gdx/ai/pfa/PathFinderRequestControl.java:86-89 —
  *     {{{
  *     if (server != null) {
  *       MessageDispatcher dispatcher = request.dispatcher != null ? request.dispatcher : MessageManager.getInstance();
  *       dispatcher.dispatchMessage(server, request.client, request.responseMessageCode, request);
  *     }
  *     }}}
  *     `request.client` may be `null`; a `null` receiver means "broadcast to every listener registered for the response code" — standard
  *     `MessageDispatcher.dispatchMessage(sender, receiver=null, msg)` semantics (MessageDispatcher.java `discharge`: a null receiver dispatches to all registered listeners).
  *   - com/badlogic/gdx/ai/pfa/PathFinderQueue.java:75 — `pfr.client = telegram.sender;` is assigned unconditionally, so a request enqueued by a telegram with a `null` sender reaches completion with
  *     `client == null` and must still broadcast its result.
  *
  * The Scala port wraps the dispatch in `request.client.foreach { ... }` (PathFinderRequestControl.scala:94-106), so when `client` is empty NOTHING is dispatched and the path-completed telegram is
  * lost to broadcast listeners. `PathFinderQueue.handleMessage` compounds this by only assigning `client` inside `msg.sender.foreach` (PathFinderQueue.scala:88-90), leaving it empty for a null-sender
  * request.
  *
  * This suite drives a real A* request whose telegram carries NO sender (a broadcast request), registers a broadcast listener for the response code (not tied to any specific client), runs the queue
  * to completion, and asserts the broadcast listener received the path-completed telegram. It FAILS on the current port (nothing dispatched) and PASSES once the dispatch is made unconditional,
  * faithful to the original Java.
  */
class PathFinderBroadcastDispatchRedSuite extends munit.FunSuite {

  private val Iss729ResponseCode = 729

  /** Generous per-frame AI budget (1s in nanos) so a 5x5 A* search always completes within one frame. */
  private val FrameBudgetNanos: Long = 1_000_000_000L

  /** A broadcast listener: registered against the response CODE via `addListener`, not addressed as a receiver. */
  private class BroadcastListener extends Telegraph {
    var received: List[Int] = List.empty

    override def handleMessage(msg: Telegram): Boolean = {
      received = received :+ msg.message
      true
    }
  }

  /** Private dispatcher pool so this suite cannot leak telegrams into the global pool shared with other tests. */
  private def newDispatcher(): MessageDispatcher = {
    val pool = new Pool[Telegram] {
      override protected val max:             Int      = Int.MaxValue
      override protected val initialCapacity: Int      = 16
      override protected def newObject():     Telegram = new Telegram()
    }
    new MessageDispatcher(pool)
  }

  private def makeTelegram(sender: Nullable[Telegraph], receiver: Nullable[Telegraph], extraInfo: Nullable[Any]): Telegram = {
    val t = new Telegram()
    t.sender = sender
    t.receiver = receiver
    t.message = Iss729ResponseCode
    t.extraInfo = extraInfo
    t
  }

  /** One broadcast pathfinding scenario: 5x5 open grid, A* path finder, request from (0,0) to (4,4). */
  private class Fixture {
    given timepiece: DefaultTimepiece = new DefaultTimepiece()
    timepiece.update(1.0f)

    val graph: GridGraph = new GridGraph(5, 5)
    graph.buildConnections()

    val pathFinder: IndexedAStarPathFinder[GridNode] = new IndexedAStarPathFinder[GridNode](graph)

    val queue: PathFinderQueue[GridNode] = new PathFinderQueue[GridNode](pathFinder, timepiece)

    val dispatcher: MessageDispatcher = newDispatcher()
    val listener:   BroadcastListener = new BroadcastListener()

    // Register the listener for the response code as a BROADCAST listener (no specific receiver).
    // MessageDispatcher.java addListener — "Messages without an explicit receiver are broadcasted to its listeners".
    dispatcher.addListener(listener, Iss729ResponseCode)

    val request: PathFinderRequest[GridNode] =
      new PathFinderRequest[GridNode](graph.node(0, 0), graph.node(4, 4), new ManhattanHeuristic(), DefaultGraphPath[GridNode](), dispatcher)
    request.responseMessageCode = Iss729ResponseCode

    /** Enqueues the request with NO sender — a broadcast request. PathFinderQueue.java:75 assigns `client = sender` unconditionally, so `client` becomes null and completion must broadcast.
      */
    def enqueueBroadcast(): Unit =
      queue.handleMessage(makeTelegram(sender = Nullable.empty, receiver = Nullable(queue), extraInfo = Nullable(request)))

    /** Enqueues the request DIRECTED to a specific client. PathFinderQueue.java:75 assigns `client = sender`, so completion dispatches straight to that client (not a broadcast).
      */
    def enqueueDirected(client: Telegraph): Unit =
      queue.handleMessage(makeTelegram(sender = Nullable(client), receiver = Nullable(queue), extraInfo = Nullable(request)))
  }

  test("ISS-729 green control: a null-receiver dispatch reaches a registered broadcast listener") {
    val f = new Fixture()
    import f.timepiece

    // Prove the broadcast infrastructure works in isolation: dispatchMessage with receiver=empty (a broadcast)
    // is delivered to every listener registered for the code (MessageDispatcher.discharge broadcast branch).
    f.dispatcher.dispatchMessage(
      msg = Iss729ResponseCode,
      sender = Nullable(f.queue),
      receiver = Nullable.empty,
      extraInfo = Nullable(f.request)
    )

    assertEquals(f.listener.received, List(Iss729ResponseCode), "broadcast listener must receive a null-receiver telegram")
  }

  test("ISS-729 red: path-completed telegram for a client-less request must broadcast to registered listeners") {
    val f = new Fixture()

    f.enqueueBroadcast()
    assertEquals(f.queue.size, 1)

    // PathFinderQueue.java:47-69 — run drains the queue; PathFinderRequestControl.java:86-89 dispatches the
    // completion telegram unconditionally (receiver = request.client, which is null here => broadcast).
    f.queue.run(FrameBudgetNanos)

    assertEquals(f.request.status, PathFinderRequest.SEARCH_FINALIZED, "request should have been finalized")
    assert(f.request.pathFound, "a path from (0,0) to (4,4) on an open 5x5 grid must be found")
    assertEquals(f.queue.size, 0, "served request should have been drained from the queue")

    // The core assertion: the client-less (broadcast) request's completion telegram must reach the broadcast
    // listener. The port drops it (request.client.foreach skips the empty client), so this FAILS on current code.
    assertEquals(
      f.listener.received,
      List(Iss729ResponseCode),
      "PathFinderRequestControl.java:86-89 broadcasts the path-completed telegram even when client is null; " +
        "the port drops it via request.client.foreach, losing the broadcast"
    )
  }

  test(
    "ISS-729 red: re-enqueuing a served request without a sender must reset the stale directed client so completion broadcasts"
  ) {
    val f              = new Fixture()
    val directedClient = new BroadcastListener()

    // 1. First serve the request DIRECTED to a specific client (the telegram carries that client as sender).
    //    PathFinderQueue.java:75 sets request.client = sender; completion dispatches straight to it.
    f.enqueueDirected(directedClient)
    f.queue.run(FrameBudgetNanos)
    assertEquals(directedClient.received, List(Iss729ResponseCode), "first (directed) run must notify the directed client")
    assertEquals(f.listener.received, List.empty[Int], "a directed dispatch must NOT reach the broadcast listener")

    // 2. Re-enqueue the SAME request object with NO sender — a broadcast request.
    //    PathFinderQueue.java:75 reassigns client = sender (empty here), clearing the stale directed client.
    f.enqueueBroadcast()
    assertEquals(f.queue.size, 1)
    f.queue.run(FrameBudgetNanos)

    assertEquals(f.request.status, PathFinderRequest.SEARCH_FINALIZED, "re-served request should be finalized")

    // 3. The completion broadcasts to the registered listener without re-notifying the previously-directed client.
    //    If PathFinderQueue regresses to `msg.sender.foreach { ... }`, the empty sender skips the assignment,
    //    request.client stays the old directed client, the telegram is directed there again, and the broadcast
    //    listener obtains Nil.
    assertEquals(
      f.listener.received,
      List(Iss729ResponseCode),
      "PathFinderQueue.java:75 resets client to the (empty) sender on re-enqueue; a stale client would misroute the broadcast away from the broadcast listener"
    )
    assertEquals(
      directedClient.received,
      List(Iss729ResponseCode),
      "the stale directed client must NOT receive a second, misrouted telegram after a client-less re-enqueue"
    )
  }
}
