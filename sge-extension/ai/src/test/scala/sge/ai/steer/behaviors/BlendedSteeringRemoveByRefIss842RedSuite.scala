package sge
package ai
package steer
package behaviors

import sge.ai.{ DefaultTimepiece, Timepiece }
import sge.ai.steer.SimpleSteerable
import sge.math.Vector2

/** Red suite for ISS-842 (wave 2026-07-18-G, territory G2).
  *
  * `BlendedSteering.remove(BehaviorAndWeight)` must unregister the behavior by REFERENCE IDENTITY, not by value equality.
  *
  * Original contract:
  *   - com/badlogic/gdx/ai/steer/behaviors/BlendedSteering.java:83-86 —
  *     {{{
  *     public void remove (BehaviorAndWeight<T> item) {
  *       list.removeValue(item, true);   // identity == true
  *     }
  *     }}}
  *     libGDX `Array.removeValue(value, identity)` with `identity == true` removes the first element that is `== value` (reference identity), NOT the first `.equals(value)`.
  *
  * The port (BlendedSteering.scala:81) calls `list.removeValue(item)`, which `lowlevel.util.DynamicArray.removeValue` (DynamicArray.scala:187-197) implements with value equality (`mk.elemEquals`,
  * i.e. `.equals`). The identity-preserving sibling `removeValueByRef` (DynamicArray.scala:200-210) is the faithful mapping of `removeValue(value, true)`.
  *
  * Observable divergence: when two DISTINCT BehaviorAndWeight instances compare equal via `.equals`, removing the SECOND one by value equality deletes the FIRST one (index 0 is the first `.equals`
  * match), so the wrong behavior is unregistered. Mirrors MessageDispatcherRemoveByRefIss730RedSuite's proof shape.
  *
  * These assertions encode the ORIGINAL semantics and MUST NOT be weakened.
  */
class BlendedSteeringRemoveByRefIss842RedSuite extends munit.FunSuite {

  /** A BehaviorAndWeight whose instances are ALL `.equals` to one another (and share a hash) but remain distinct objects — the exact condition under which identity- vs equality-removal diverge. */
  final private class EqualBAW(behavior: SteeringBehavior[Vector2]) extends BlendedSteering.BehaviorAndWeight[Vector2](behavior, 1f) {
    override def equals(other: Any): Boolean = other.isInstanceOf[BlendedSteering.BehaviorAndWeight[?]]
    override def hashCode():         Int     = 0
  }

  test("ISS-842: remove unregisters the SPECIFIC instance by identity, leaving a value-equal sibling registered") {
    given tp: Timepiece = new DefaultTimepiece()

    val owner   = new SimpleSteerable(Vector2(0, 0))
    val blended = new BlendedSteering[Vector2](owner)

    val first  = new EqualBAW(new Wander[Vector2](owner))
    val second = new EqualBAW(new Wander[Vector2](owner))

    // Sanity: the two instances are value-equal but not the same reference.
    assert(first == second, "test fixture invariant: the two behaviors must be .equals-equal")
    assert(!(first eq second), "test fixture invariant: the two behaviors must be distinct instances")

    blended.add(first)
    blended.add(second)

    // Remove the SECOND registration. Identity semantics (removeValueByRef) drop `second`;
    // value semantics (removeValue) wrongly drop `first` (the first `.equals` match at index 0).
    blended.remove(second)

    assertEquals(blended.list.size, 1, "exactly one behavior must remain after removing one of two")
    assert(
      blended.get(0) eq first,
      "remove(second) must NOT unregister `first`; BlendedSteering.java:85 removes by identity (removeValue(item, true)), " +
        "so the first-registered instance survives; the port's value-equality removeValue deletes the wrong (first) instance"
    )
  }
}
