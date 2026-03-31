package sge
package ecs
package signals

class SignalSuite extends munit.FunSuite {

  private class Dummy

  private class ListenerMock extends Listener[Dummy] {
    var count: Int = 0

    override def receive(signal: Signal[Dummy], obj: Dummy): Unit = {
      count += 1
      assert(signal != null)
      assert(obj != null)
    }
  }

  private class RemoveWhileDispatchListener extends Listener[Dummy] {
    var count: Int = 0

    override def receive(signal: Signal[Dummy], obj: Dummy): Unit = {
      count += 1
      signal.remove(this)
    }
  }

  test("add listener and dispatch") {
    val dummy    = new Dummy
    val signal   = new Signal[Dummy]
    val listener = new ListenerMock

    signal.add(listener)

    for (i <- 0 until 10) {
      assertEquals(listener.count, i)
      signal.dispatch(dummy)
      assertEquals(listener.count, i + 1)
    }
  }

  test("multiple listeners all receive") {
    val dummy     = new Dummy
    val signal    = new Signal[Dummy]
    val listeners = (0 until 10).map(_ => new ListenerMock).toArray

    listeners.foreach(signal.add)

    val numDispatches = 10
    for (i <- 0 until numDispatches) {
      listeners.foreach(l => assertEquals(l.count, i))
      signal.dispatch(dummy)
      listeners.foreach(l => assertEquals(l.count, i + 1))
    }
  }

  test("add, dispatch, and remove listener") {
    val dummy     = new Dummy
    val signal    = new Signal[Dummy]
    val listenerA = new ListenerMock
    val listenerB = new ListenerMock

    signal.add(listenerA)
    signal.add(listenerB)

    val numDispatches = 5
    for (i <- 0 until numDispatches) {
      assertEquals(listenerA.count, i)
      assertEquals(listenerB.count, i)
      signal.dispatch(dummy)
      assertEquals(listenerA.count, i + 1)
      assertEquals(listenerB.count, i + 1)
    }

    signal.remove(listenerB)

    for (i <- 0 until numDispatches) {
      assertEquals(listenerA.count, i + numDispatches)
      assertEquals(listenerB.count, numDispatches)
      signal.dispatch(dummy)
      assertEquals(listenerA.count, i + 1 + numDispatches)
      assertEquals(listenerB.count, numDispatches)
    }
  }

  test("remove during dispatch (snapshot safety)") {
    val dummy     = new Dummy
    val signal    = new Signal[Dummy]
    val listenerA = new RemoveWhileDispatchListener
    val listenerB = new ListenerMock

    signal.add(listenerA)
    signal.add(listenerB)

    signal.dispatch(dummy)

    assertEquals(listenerA.count, 1)
    assertEquals(listenerB.count, 1)
  }

  test("removeAllListeners") {
    val dummy     = new Dummy
    val signal    = new Signal[Dummy]
    val listenerA = new ListenerMock
    val listenerB = new ListenerMock

    signal.add(listenerA)
    signal.add(listenerB)

    signal.dispatch(dummy)
    assertEquals(listenerA.count, 1)
    assertEquals(listenerB.count, 1)

    signal.removeAllListeners()

    signal.dispatch(dummy)
    assertEquals(listenerA.count, 1)
    assertEquals(listenerB.count, 1)
  }
}
