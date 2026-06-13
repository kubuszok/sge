package sge
package ai
package pfa

import sge.ai.DefaultTimepiece
import sge.ai.msg.MessageDispatcher
import sge.ai.msg.Telegram
import sge.ai.msg.Telegraph
import sge.ai.pfa.indexed.IndexedAStarPathFinder
import sge.ai.sched.LoadBalancingScheduler
import sge.ai.sched.Schedulable
import sge.utils.Pool
import lowlevel.Nullable

/** Red tests for ISS-530: `PathFinderQueue` no longer implements `Schedulable` because `run` grew a `(using Timepiece)` parameter, so the canonical gdx-ai composition — a path finder queue added to a
  * scheduler that slices the per-frame AI budget — cannot be assembled.
  *
  * Composition contract in the original source:
  *   - com/badlogic/gdx/ai/pfa/PathFinderQueue.java:28 — `public class PathFinderQueue<N> implements Schedulable, Telegraph`, with `run(long timeToRun)` at lines 47-69.
  *   - com/badlogic/gdx/ai/sched/Schedulable.java:22-26 — `run(long nanoTimeToRun)` invoked by the scheduler.
  *   - com/badlogic/gdx/ai/sched/LoadBalancingScheduler.java:68-77 — `addWithAutomaticPhasing(Schedulable, int)` / `add(Schedulable, int, int)`; line 110 — `record.schedulable.run(availableTime)`.
  *
  * Fix direction pinned by these tests: store the `Timepiece` at construction, exactly like `sge.ai.btree.leaf.Wait` stores its timepiece as a constructor `val timepiece: Timepiece`
  * (Wait.scala:39-44), so `run(timeToRun: Long)` regains the `Schedulable` signature.
  */
class PathFinderQueueSchedulableRedSuite extends munit.FunSuite {

  private val Iss530ResponseCode = 530

  /** Generous per-frame AI budget (1s in nanos) so a 5x5 A* search always completes within one frame. */
  private val FrameBudgetNanos: Long = 1_000_000_000L

  /** Telegraph recording every received response message code in arrival order. */
  private class Iss530Client extends Telegraph {
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
    t.message = Iss530ResponseCode
    t.extraInfo = extraInfo
    t
  }

  /** One pathfinding scenario: 5x5 grid (fixture shared with AStarSuite), A* path finder, request from (0,0) to (4,4) responding through a private dispatcher. */
  private class Iss530Fixture {
    val timepiece: DefaultTimepiece = new DefaultTimepiece()
    timepiece.update(1.0f)

    val graph: GridGraph = new GridGraph(5, 5)
    graph.buildConnections()

    val pathFinder: IndexedAStarPathFinder[GridNode] = new IndexedAStarPathFinder[GridNode](graph)

    // Timepiece stored at construction, like btree.leaf.Wait does (Wait.scala:39-44).
    val queue: PathFinderQueue[GridNode] = new PathFinderQueue[GridNode](pathFinder, timepiece)

    val dispatcher: MessageDispatcher = newDispatcher()
    val client:     Iss530Client      = new Iss530Client()

    val request: PathFinderRequest[GridNode] =
      new PathFinderRequest[GridNode](graph.node(0, 0), graph.node(4, 4), new ManhattanHeuristic(), DefaultGraphPath[GridNode](), dispatcher)
    request.responseMessageCode = Iss530ResponseCode

    /** Enqueues the request the canonical way: a telegram handled by the queue (PathFinderQueue.java:71-81). */
    def enqueue(): Unit =
      queue.handleMessage(makeTelegram(sender = Nullable(client), receiver = Nullable(queue), extraInfo = Nullable(request)))

    def assertRequestServed(): Unit = {
      assertEquals(request.status, PathFinderRequest.SEARCH_FINALIZED, "request should have been finalized")
      assert(request.pathFound, "a path from (0,0) to (4,4) on an open 5x5 grid must be found")
      assertEquals(client.received, List(Iss530ResponseCode), "client should have been notified exactly once with the response code")
      assertEquals(queue.size, 0, "served request should have been drained from the queue")
    }
  }

  test("ISS-530 green control: queue.run services a queued A* request to completion") {
    val f = new Iss530Fixture()
    f.enqueue()
    assertEquals(f.queue.size, 1)

    // PathFinderQueue.java:47-69 — run drains the queue within the given time budget.
    f.queue.run(FrameBudgetNanos)

    f.assertRequestServed()
  }

  test("ISS-530 red: PathFinderQueue is a Schedulable that LoadBalancingScheduler.add accepts and runs") {
    val f = new Iss530Fixture()

    // PathFinderQueue.java:28 — implements Schedulable, Telegraph.
    val schedulable: Schedulable = f.queue

    val scheduler = new LoadBalancingScheduler(100)
    // LoadBalancingScheduler.java:74 — add(Schedulable schedulable, int frequency, int phase).
    scheduler.add(schedulable, 1, 0)

    f.enqueue()
    assertEquals(f.queue.size, 1)

    // LoadBalancingScheduler.java:82-115 — one frame; line 110 calls record.schedulable.run(availableTime).
    scheduler.run(FrameBudgetNanos)

    f.assertRequestServed()
  }

  test("ISS-530 red: LoadBalancingScheduler.addWithAutomaticPhasing accepts a PathFinderQueue") {
    val f = new Iss530Fixture()

    val scheduler = new LoadBalancingScheduler(100)
    // LoadBalancingScheduler.java:68-71 — addWithAutomaticPhasing(Schedulable schedulable, int frequency).
    scheduler.addWithAutomaticPhasing(f.queue, 1)

    f.enqueue()

    // Frequency 1 means the queue is due on every frame regardless of the calculated phase.
    scheduler.run(FrameBudgetNanos)

    f.assertRequestServed()
  }
}
