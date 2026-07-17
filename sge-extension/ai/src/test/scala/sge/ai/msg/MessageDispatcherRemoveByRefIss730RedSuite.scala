package sge
package ai
package msg

import sge.utils.Pool

/** Red suite for ISS-730 clause c1 (wave 2026-07-17-F, territory Z).
  *
  * `MessageDispatcher.removeListener` must unregister the listener by REFERENCE IDENTITY, not by value equality.
  *
  * Original contract:
  *   - com/badlogic/gdx/ai/msg/MessageDispatcher.java:135-140 —
  *     {{{
  *     public void removeListener (Telegraph listener, int msg) {
  *       Array<Telegraph> listeners = msgListeners.get(msg);
  *       if (listeners != null) {
  *         listeners.removeValue(listener, true);   // identity == true
  *       }
  *     }
  *     }}}
  *     libGDX `Array.removeValue(value, identity)` with `identity == true` removes the first element that is `== value` (reference identity), NOT the first `.equals(value)`.
  *
  * The port (MessageDispatcher.scala:120-123) calls `listeners.removeValue(listener)`, which `lowlevel.util.DynamicArray.removeValue` (DynamicArray.scala:186-197) implements with value equality
  * (`mk.elemEquals`, i.e. `.equals`). The identity-preserving sibling `removeValueByRef` (DynamicArray.scala:199-210) is the faithful mapping of `removeValue(value, true)`.
  *
  * Observable divergence: when two DISTINCT Telegraph instances compare equal via `.equals`, removing the SECOND one by value equality deletes the FIRST one (index 0 is the first `.equals` match), so
  * the wrong listener is unregistered.
  *
  * These assertions encode the ORIGINAL semantics and MUST NOT be weakened.
  */
class MessageDispatcherRemoveByRefIss730RedSuite extends munit.FunSuite {

  private val MsgCode = 7301

  /** A Telegraph whose instances are ALL `.equals` to one another (and share a hash) but remain distinct objects — the exact condition under which identity- vs equality-removal diverge. Each instance
    * counts its own deliveries so registration can be observed per-instance.
    */
  final private class EqualTelegraph extends Telegraph {
    var received: Int = 0

    override def handleMessage(msg: Telegram): Boolean = {
      received += 1
      true
    }

    override def equals(other: Any): Boolean = other.isInstanceOf[EqualTelegraph]
    override def hashCode():         Int     = 0
  }

  /** Private Telegram pool so this suite never leaks into the global shared pool. */
  private def newDispatcher(): MessageDispatcher = {
    val pool = new Pool[Telegram] {
      override protected val max:             Int      = Int.MaxValue
      override protected val initialCapacity: Int      = 16
      override protected def newObject():     Telegram = new Telegram()
    }
    new MessageDispatcher(pool)
  }

  test("ISS-730 c1: removeListener unregisters the SPECIFIC instance by identity, leaving a value-equal sibling registered") {
    given tp: Timepiece = new DefaultTimepiece()

    val dispatcher = newDispatcher()
    val first      = new EqualTelegraph()
    val second     = new EqualTelegraph()

    // Sanity: the two instances are value-equal but not the same reference.
    assert(first == second, "test fixture invariant: the two telegraphs must be .equals-equal")
    assert(!(first eq second), "test fixture invariant: the two telegraphs must be distinct instances")

    dispatcher.addListener(first, MsgCode)
    dispatcher.addListener(second, MsgCode)

    // Remove the SECOND registration. Identity semantics (removeValueByRef) drop
    // `second`; value semantics (removeValue) wrongly drop `first` (the first
    // `.equals` match at index 0).
    dispatcher.removeListener(second, MsgCode)

    // Broadcast to whoever is still registered for MsgCode.
    dispatcher.dispatchMessage(msg = MsgCode)

    assertEquals(
      first.received,
      1,
      "removeListener(second) must NOT unregister `first`; MessageDispatcher.java:138 removes by identity (removeValue(listener, true)), " +
        "so the first-registered instance stays registered and receives the broadcast"
    )
    assertEquals(
      second.received,
      0,
      "removeListener(second) must unregister exactly `second`; the port's value-equality removeValue deletes the wrong (first) instance"
    )
  }
}
