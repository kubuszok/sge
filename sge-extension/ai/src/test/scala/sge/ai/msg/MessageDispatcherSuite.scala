package sge
package ai
package msg

import sge.utils.Nullable

class TestTelegraph extends Telegraph {
  var received:                              List[Int] = List.empty
  override def handleMessage(msg: Telegram): Boolean   = {
    received = received :+ msg.message
    true
  }
}

class MessageDispatcherSuite extends munit.FunSuite {

  private def makeTimepiece(initialTime: Float = 0f): DefaultTimepiece = {
    val tp = new DefaultTimepiece()
    if (initialTime > 0f) tp.update(initialTime)
    tp
  }

  test("immediate dispatch to registered listener") {
    given tp: Timepiece = makeTimepiece()
    val dispatcher = new MessageDispatcher()
    val listener   = new TestTelegraph()
    dispatcher.addListener(listener, 1)
    dispatcher.dispatchMessage(msg = 1)
    assertEquals(listener.received, List(1))
  }

  test("broadcast to multiple listeners") {
    given tp: Timepiece = makeTimepiece()
    val dispatcher = new MessageDispatcher()
    val l1         = new TestTelegraph()
    val l2         = new TestTelegraph()
    dispatcher.addListener(l1, 42)
    dispatcher.addListener(l2, 42)
    dispatcher.dispatchMessage(msg = 42)
    assertEquals(l1.received, List(42))
    assertEquals(l2.received, List(42))
  }

  test("delayed dispatch with timepiece update") {
    val tp          = makeTimepiece()
    given Timepiece = tp
    val dispatcher  = new MessageDispatcher()
    val listener    = new TestTelegraph()
    dispatcher.addListener(listener, 1)

    // Dispatch with 1 second delay
    dispatcher.dispatchMessage(msg = 1, delay = 1.0f)
    // Not yet delivered
    assertEquals(listener.received, Nil)

    // Advance time to 0.5 - still not delivered
    tp.update(0.5f)
    dispatcher.update()
    assertEquals(listener.received, Nil)

    // Advance time to 1.0 - now delivered
    tp.update(0.5f)
    dispatcher.update()
    assertEquals(listener.received, List(1))
  }

  test("return receipt") {
    given tp: Timepiece = makeTimepiece()
    val dispatcher = new MessageDispatcher()
    val sender     = new TestTelegraph()
    val receiver   = new TestTelegraph()
    dispatcher.addListener(receiver, 10)

    dispatcher.dispatchMessage(
      msg = 10,
      sender = Nullable(sender),
      receiver = Nullable(receiver),
      needsReturnReceipt = true
    )

    // Receiver gets the message
    assertEquals(receiver.received, List(10))
    // Sender gets the return receipt (also message code 10)
    assertEquals(sender.received, List(10))
  }

  test("unregister listener stops receiving") {
    given tp: Timepiece = makeTimepiece()
    val dispatcher = new MessageDispatcher()
    val listener   = new TestTelegraph()
    dispatcher.addListener(listener, 1)
    dispatcher.dispatchMessage(msg = 1)
    assertEquals(listener.received, List(1))

    dispatcher.removeListener(listener, 1)
    dispatcher.dispatchMessage(msg = 1)
    // Should still be just the one message
    assertEquals(listener.received, List(1))
  }

  test("clear removes all listeners and queue") {
    val tp          = makeTimepiece()
    given Timepiece = tp
    val dispatcher  = new MessageDispatcher()
    val listener    = new TestTelegraph()
    dispatcher.addListener(listener, 1)
    dispatcher.dispatchMessage(msg = 1, delay = 5.0f)
    dispatcher.clear()

    // Advance time past the delay
    tp.update(10f)
    dispatcher.update()
    // Nothing delivered - listener was cleared
    assertEquals(listener.received, Nil)
  }

  test("return receipt requires sender") {
    given tp: Timepiece = makeTimepiece()
    val dispatcher = new MessageDispatcher()
    intercept[IllegalArgumentException] {
      dispatcher.dispatchMessage(msg = 1, needsReturnReceipt = true)
    }
  }
}
