/* Copyright 2025-2026 Mateusz Kubuszok / Licensed under Apache 2.0 */
package sge
package scenes
package scene2d
package utils

import lowlevel.Nullable

/** Red test for ISS-594: Selection.set uses Scala value equality where Java uses reference identity.
  *
  * Original (com/badlogic/gdx/scenes/scene2d/utils/Selection.java:105):
  * {{{
  *   public void set (T item) {
  *     if (item == null) throw ...;
  *     if (selected.size == 1 && selected.first() == item) return;   // reference identity (==)
  *     snapshot();
  *     selected.clear(8);
  *     selected.add(item);
  *     ...
  *     lastSelected = item;
  *     ...
  *   }
  * }}}
  *
  * The Java early-return short-circuits ONLY when the single selected element is the very same object instance as `item` (`selected.first() == item`, reference identity). When a caller passes a
  * value-equal but non-identical instance (e.g. a freshly built String or an updated POJO that `.equals` the old one), Java proceeds: it clears the selection, adds the NEW instance, and updates
  * lastSelected to the new instance.
  *
  * The port (Selection.scala:123) writes `selected.head == item`, which is Scala value equality, so it short-circuits (no-ops) for a value-equal-but-distinct instance — the selection keeps the OLD
  * instance and lastSelected is never updated. This pins the Java behavior; it fails on the port until the comparison is changed to `eq` (AnyRef identity).
  */
class SelectionSetIdentityRedSuite extends munit.FunSuite {

  /** Value-equal-but-distinct instances: `Box(1) == Box(1)` is true, `Box(1) eq Box(1)` is false. */
  final private case class Box(v: Int)

  private def ctx(): Sge = SgeTestFixture.testSge()

  test("ISS-594 set(value-equal-but-distinct) replaces the stored instance (Java identity semantics)") {
    given Sge = ctx()
    val sel   = Selection[Box]()
    sel.programmaticChangeEvents = false

    val a = Box(1)
    val b = Box(1) // a == b (value), a ne b (identity)
    assert(a == b, "sanity: the two boxes are value-equal")
    assert(!(a eq b), "sanity: the two boxes are distinct instances")

    sel.set(a)
    assertEquals(sel.size, 1)
    assert(sel.selected.head eq a, "sanity: after set(a) the sole element is the `a` instance")

    // Java: selected.first() == b is false (identity), so it clears + adds b.
    sel.set(b)
    assertEquals(sel.size, 1)
    assert(
      sel.selected.head eq b,
      "ISS-594: Java uses reference identity in set(); a value-equal-but-distinct item must replace the stored instance"
    )
  }

  test("ISS-594 set(value-equal-but-distinct) updates lastSelected to the new instance") {
    given Sge = ctx()
    val sel   = Selection[Box]()
    sel.programmaticChangeEvents = false

    val a = Box(7)
    val b = Box(7)
    sel.set(a)
    assert(sel.lastSelected.exists(_ eq a), "sanity: lastSelected is `a` after set(a)")

    sel.set(b)
    assert(
      sel.lastSelected.exists(_ eq b),
      "ISS-594: set() must update lastSelected to the new (identity-distinct) instance, as Java does"
    )
  }
}
