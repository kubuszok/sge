package sge
package ai
package btree

import lowlevel.Nullable

/** Red suite for ISS-842 (wave 2026-07-18-G, territory G2).
  *
  * `BehaviorTree.removeListener` must unregister the listener by REFERENCE IDENTITY, not by value equality.
  *
  * Original contract:
  *   - com/badlogic/gdx/ai/btree/BehaviorTree.java:153-155 —
  *     {{{
  *     public void removeListener (Listener<E> listener) {
  *       if (listeners != null) listeners.removeValue(listener, true);   // identity == true
  *     }
  *     }}}
  *     libGDX `Array.removeValue(value, identity)` with `identity == true` removes the first element that is `== value` (reference identity), NOT the first `.equals(value)`.
  *
  * The port (BehaviorTree.scala:130) calls `_.removeValue(listener)`, which `lowlevel.util.DynamicArray.removeValue` (DynamicArray.scala:187-197) implements with value equality (`mk.elemEquals`, i.e.
  * `.equals`). The identity-preserving sibling `removeValueByRef` (DynamicArray.scala:200-210) is the faithful mapping of `removeValue(value, true)`.
  *
  * Observable divergence: when two DISTINCT Listener instances compare equal via `.equals`, removing the SECOND one by value equality deletes the FIRST one (index 0 is the first `.equals` match), so
  * the wrong listener is unregistered. Mirrors MessageDispatcherRemoveByRefIss730RedSuite's proof shape.
  *
  * These assertions encode the ORIGINAL semantics and MUST NOT be weakened.
  */
class BehaviorTreeRemoveByRefIss842RedSuite extends munit.FunSuite {

  /** A Listener whose instances are ALL `.equals` to one another (and share a hash) but remain distinct objects — the exact condition under which identity- vs equality-removal diverge. Each instance
    * counts its own notifications so registration can be observed per-instance.
    */
  final private class EqualListener extends BehaviorTree.Listener[String] {
    var notified: Int = 0

    override def statusUpdated(task: Task[String], previousStatus: Task.Status): Unit = notified += 1
    override def childAdded(task:    Task[String], index:          Int):         Unit = notified += 1

    override def equals(other: Any): Boolean = other.isInstanceOf[EqualListener]
    override def hashCode():         Int     = 0
  }

  test("ISS-842: removeListener unregisters the SPECIFIC instance by identity, leaving a value-equal sibling registered") {
    val bt     = new BehaviorTree[String](Nullable.empty[Task[String]], Nullable("bb"))
    val first  = new EqualListener()
    val second = new EqualListener()

    // Sanity: the two instances are value-equal but not the same reference.
    assert(first == second, "test fixture invariant: the two listeners must be .equals-equal")
    assert(!(first eq second), "test fixture invariant: the two listeners must be distinct instances")

    bt.addListener(first)
    bt.addListener(second)

    // Remove the SECOND registration. Identity semantics (removeValueByRef) drop `second`;
    // value semantics (removeValue) wrongly drop `first` (the first `.equals` match at index 0).
    bt.removeListener(second)

    // Notify whoever is still registered.
    bt.notifyChildAdded(bt, 0)

    assertEquals(
      first.notified,
      1,
      "removeListener(second) must NOT unregister `first`; BehaviorTree.java:154 removes by identity (removeValue(listener, true)), " +
        "so the first-registered instance stays registered and receives the notification"
    )
    assertEquals(
      second.notified,
      0,
      "removeListener(second) must unregister exactly `second`; the port's value-equality removeValue deletes the wrong (first) instance"
    )
  }
}
