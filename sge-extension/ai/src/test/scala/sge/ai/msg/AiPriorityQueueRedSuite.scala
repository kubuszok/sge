package sge
package ai
package msg

import sge.utils.Pool

/** Comparable wrapper used as the queue element type (PriorityQueue requires E <: Comparable[E]). */
final case class Iss502Item(value: Int) extends Comparable[Iss502Item] {
  override def compareTo(other: Iss502Item): Int = Integer.compare(value, other.value)
}

/** Telegraph recording every received message code in arrival order. */
class Iss502RecordingTelegraph extends Telegraph {
  var received: List[Int] = List.empty

  override def handleMessage(msg: Telegram): Boolean = {
    received = received :+ msg.message
    true
  }
}

/** Red tests for ISS-502: PriorityQueue.siftDown dropped Java's unconditional post-loop `queue[k] = x` write-back (original: com/badlogic/gdx/ai/msg/PriorityQueue.java, siftDown) for a three-way
  * conditional that is wrong in every path. Expected values below are pinned to the Java siftUp/siftDown semantics.
  */
class AiPriorityQueueRedSuite extends munit.FunSuite {

  private def drain(q: PriorityQueue[Iss502Item]): List[Int] = {
    val builder  = List.newBuilder[Int]
    var continue = true
    while (continue) {
      val polled = q.poll()
      if (polled.isEmpty) continue = false
      else builder += polled.get.value
    }
    builder.result()
  }

  test("ISS-502 two-element heap polls survivor: {1,2} polled yields 1 then 2") {
    val q = new PriorityQueue[Iss502Item]()
    assert(q.add(Iss502Item(1)))
    assert(q.add(Iss502Item(2)))
    // Java siftDown writes x back unconditionally after the loop, so the
    // survivor 2 becomes the new head; the broken port leaves the stale 1.
    assertEquals(drain(q), List(1, 2))
  }

  test("ISS-502 ten shuffled elements poll strictly ascending 0..9 with no duplicates") {
    val q = new PriorityQueue[Iss502Item]()
    List(7, 3, 9, 1, 5, 0, 8, 2, 6, 4).foreach(v => assert(q.add(Iss502Item(v))))
    assertEquals(drain(q), List(0, 1, 2, 3, 4, 5, 6, 7, 8, 9))
  }

  test("ISS-502 uniqueness mode rejects duplicates and still polls in order") {
    val q = new PriorityQueue[Iss502Item]()
    q.uniqueness = true
    assert(q.add(Iss502Item(1)))
    assert(!q.add(Iss502Item(1)), "duplicate must be rejected while present")
    assert(q.add(Iss502Item(2)))
    // Polling removes the element from the uniqueness set as well, and the
    // heap must still yield ascending order (Java semantics).
    assertEquals(drain(q), List(1, 2))
    // After polling, re-adding a previously present element must succeed.
    assert(q.add(Iss502Item(1)), "element polled out must be addable again")
  }

  test("ISS-502 dispatcher delivers each delayed telegram exactly once in timestamp order") {
    val tp          = new DefaultTimepiece()
    given Timepiece = tp
    // Private pool so the broken queue's stale/double-freed telegrams cannot
    // leak into the global pool shared with other tests.
    val pool = new Pool[Telegram] {
      override protected val max:             Int      = Int.MaxValue
      override protected val initialCapacity: Int      = 16
      override protected def newObject():     Telegram = new Telegram()
    }
    val dispatcher = new MessageDispatcher(pool)
    val listener   = new Iss502RecordingTelegraph()
    dispatcher.addListener(listener, 1)
    dispatcher.addListener(listener, 2)
    dispatcher.addListener(listener, 3)

    dispatcher.dispatchMessage(msg = 1, delay = 1.0f)
    dispatcher.dispatchMessage(msg = 2, delay = 2.0f)
    dispatcher.dispatchMessage(msg = 3, delay = 3.0f)

    // Advance past all timestamps; every telegram must be delivered exactly
    // once, in timestamp order. The broken siftDown leaves stale pooled
    // telegrams at the heap root, double-dispatching them and dropping others.
    tp.update(5f)
    dispatcher.update()
    assertEquals(listener.received, List(1, 2, 3))
  }
}
