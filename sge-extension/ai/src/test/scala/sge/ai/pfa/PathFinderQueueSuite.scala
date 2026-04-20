package sge
package ai
package pfa

import sge.ai.DefaultTimepiece
import sge.ai.msg.Telegram
import sge.ai.msg.Telegraph
import sge.utils.Nullable

class PathFinderQueueSuite extends munit.FunSuite {

  /** A trivial PathFinder that always succeeds immediately. */
  private class ImmediatePathFinder extends PathFinder[Int] {
    var searchCalled: Boolean = false

    override def search(request: PathFinderRequest[Int], timeToRun: Long): Boolean = {
      searchCalled = true
      request.pathFound = true
      true
    }

    override def searchNodePath(startNode: Int, endNode: Int, heuristic: Heuristic[Int], outPath: GraphPath[Int]): Boolean = {
      searchCalled = true
      true
    }

    override def searchConnectionPath(startNode: Int, endNode: Int, heuristic: Heuristic[Int], outPath: GraphPath[Connection[Int]]): Boolean = {
      searchCalled = true
      true
    }
  }

  /** A simple Telegraph for receiving messages. */
  private class SimpleClient extends Telegraph {
    var messagesReceived:                      Int     = 0
    override def handleMessage(msg: Telegram): Boolean = {
      messagesReceived += 1
      true
    }
  }

  private def makeTelegram(
    sender:    Nullable[Telegraph] = Nullable.empty,
    receiver:  Nullable[Telegraph] = Nullable.empty,
    msg:       Int = 0,
    extraInfo: Nullable[Any] = Nullable.empty
  ): Telegram = {
    val t = new Telegram()
    t.sender = sender
    t.receiver = receiver
    t.message = msg
    t.extraInfo = extraInfo
    t
  }

  // ── Basic queue management ───────────────────────────────────────────

  test("new queue has size 0") {
    val pf    = new ImmediatePathFinder()
    val queue = new PathFinderQueue[Int](pf)
    assertEquals(queue.size, 0)
  }

  test("handleMessage adds request to queue") {
    val pf    = new ImmediatePathFinder()
    val queue = new PathFinderQueue[Int](pf)

    val request = new PathFinderRequest[Int]()
    request.startNode = 0
    request.endNode = 5

    val client   = new SimpleClient()
    val telegram = makeTelegram(
      sender = Nullable(client),
      receiver = Nullable(queue),
      extraInfo = Nullable(request)
    )

    queue.handleMessage(telegram)
    assertEquals(queue.size, 1)
  }

  test("handleMessage resets request status to SEARCH_NEW") {
    val pf    = new ImmediatePathFinder()
    val queue = new PathFinderQueue[Int](pf)

    val request = new PathFinderRequest[Int]()
    request.startNode = 0
    request.endNode = 5
    request.status = PathFinderRequest.SEARCH_DONE

    val client   = new SimpleClient()
    val telegram = makeTelegram(
      sender = Nullable(client),
      receiver = Nullable(queue),
      extraInfo = Nullable(request)
    )

    queue.handleMessage(telegram)
    assertEquals(request.status, PathFinderRequest.SEARCH_NEW)
    assert(request.statusChanged, "statusChanged should be true")
    assertEquals(request.executionFrames, 0)
  }

  test("handleMessage sets client from telegram sender") {
    val pf    = new ImmediatePathFinder()
    val queue = new PathFinderQueue[Int](pf)

    val request = new PathFinderRequest[Int]()
    request.startNode = 0
    request.endNode = 5

    val client   = new SimpleClient()
    val telegram = makeTelegram(
      sender = Nullable(client),
      receiver = Nullable(queue),
      extraInfo = Nullable(request)
    )

    queue.handleMessage(telegram)
    assert(request.client.isDefined, "client should be set from sender")
  }

  test("handleMessage returns true") {
    val pf    = new ImmediatePathFinder()
    val queue = new PathFinderQueue[Int](pf)

    val telegram = makeTelegram()
    val result   = queue.handleMessage(telegram)
    assert(result, "handleMessage should return true")
  }

  // ── Running the queue ────────────────────────────────────────────────

  test("run processes queued requests") {
    given tp: DefaultTimepiece = new DefaultTimepiece()
    tp.update(1.0f)

    val pf    = new ImmediatePathFinder()
    val queue = new PathFinderQueue[Int](pf)

    val request = new PathFinderRequest[Int]()
    request.startNode = 0
    request.endNode = 5

    val client   = new SimpleClient()
    val telegram = makeTelegram(
      sender = Nullable(client),
      receiver = Nullable(queue),
      extraInfo = Nullable(request)
    )

    queue.handleMessage(telegram)
    assertEquals(queue.size, 1)

    // Run with ample time
    queue.run(Long.MaxValue / 2)

    // The immediate path finder should have been called
    assert(pf.searchCalled, "path finder search should have been called")
  }

  test("multiple requests are queued in order") {
    val pf    = new ImmediatePathFinder()
    val queue = new PathFinderQueue[Int](pf)

    val client = new SimpleClient()

    for (i <- 0 until 3) {
      val request = new PathFinderRequest[Int]()
      request.startNode = i
      request.endNode = i + 10

      val telegram = makeTelegram(
        sender = Nullable(client),
        receiver = Nullable(queue),
        extraInfo = Nullable(request)
      )
      queue.handleMessage(telegram)
    }

    assertEquals(queue.size, 3)
  }

  test("run with no requests does not crash") {
    given tp: DefaultTimepiece = new DefaultTimepiece()
    tp.update(1.0f)

    val pf    = new ImmediatePathFinder()
    val queue = new PathFinderQueue[Int](pf)

    queue.run(1000000L)
    assert(!pf.searchCalled, "search should not be called when queue is empty")
  }

  test("telegram without extraInfo does not add to queue") {
    val pf    = new ImmediatePathFinder()
    val queue = new PathFinderQueue[Int](pf)

    val telegram = makeTelegram()
    queue.handleMessage(telegram)
    assertEquals(queue.size, 0)
  }
}
