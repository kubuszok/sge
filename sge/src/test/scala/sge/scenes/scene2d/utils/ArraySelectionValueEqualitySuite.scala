/* Copyright 2025-2026 Mateusz Kubuszok / Licensed under Apache 2.0 */
package sge
package scenes
package scene2d
package utils

import lowlevel.util.DynamicArray

/** ISS-774 adjudication: the issue claims ArraySelection uses reference identity at :49, :55, :101 where the original uses value equality (`array.indexOf(x, false)` / `array.contains(x, false)`).
  *
  * Evidence in source: the port calls `array.indexOf(rs)` (:49), `array.indexOf(item)` (:55) and `array.contains(s)` (:101). `DynamicArray.indexOf` / `.contains` use `mk.elemEquals`, and the AnyRef
  * `MkArray.OfRefs.elemEquals(a, b) = a == b` — i.e. VALUE equality (`.equals`), which already matches Java's `indexOf(x, false)` / `contains(x, false)`. (The identity variants are the separate
  * `indexOfByRef` / `containsByRef`, which ArraySelection does not use.)
  *
  * These tests pin the ORIGINAL (value-equality) behavior at all three sites using value-equal but identity-distinct instances. If they PASS, the port already matches the original and ISS-774 is
  * STALE. If a future refactor swaps in the *ByRef variants, they turn red.
  */
class ArraySelectionValueEqualitySuite extends munit.FunSuite {

  /** Value-equal-but-distinct instances: `Box(1) == Box(1)`, `Box(1) ne Box(1)`. */
  final private case class Box(v: Int)

  /** Input reporting SHIFT as held so UIUtils.shift() is true on every platform. */
  final private class ShiftHeldInput extends Input {
    private val delegate = new sge.noop.NoopInput
    export delegate.{ isKeyPressed as _, * }
    override def isKeyPressed(key: Input.Key): Boolean =
      key == Input.Keys.SHIFT_LEFT || key == Input.Keys.SHIFT_RIGHT
  }

  private def arrayOf(boxes: Box*): DynamicArray[Box] = {
    val a = DynamicArray[Box]()
    boxes.foreach(a.add)
    a
  }

  // ---------------------------------------------------------------------------
  // Site :101 — validate() uses array.contains(s) (value equality)
  // ---------------------------------------------------------------------------

  test("ISS-774 validate keeps a selected item that is value-equal (not identical) to an array element") {
    given Sge = SgeTestFixture.testSge()
    val array = arrayOf(Box(1))
    val sel   = ArraySelection[Box](array)
    sel.programmaticChangeEvents = false

    val selectedButDistinct = Box(1) // == array(0), ne array(0)
    sel.add(selectedButDistinct)
    assertEquals(sel.size, 1)

    // Java: array.contains(selected, false) -> value equality -> true -> item retained.
    sel.validate()
    assertEquals(
      sel.size,
      1,
      "ISS-774: validate() must retain a value-equal item (Java `array.contains(x, false)`); reference identity would drop it"
    )
  }

  // ---------------------------------------------------------------------------
  // Sites :49 / :55 — range choose uses array.indexOf(rangeStart) / array.indexOf(item)
  // ---------------------------------------------------------------------------

  test("ISS-774 shift-range selection resolves range endpoints by value equality") {
    given Sge = SgeTestFixture.testSge(input = new ShiftHeldInput)
    val array = arrayOf(Box(10), Box(20), Box(30))
    val sel   = ArraySelection[Box](array)
    sel.programmaticChangeEvents = false
    sel.multiple = true // rangeSelect defaults to true

    // Anchor: selection empty, so the shift branch is skipped; sets rangeStart to a distinct instance.
    sel.choose(Box(10)) // == array(0), ne array(0)
    assertEquals(sel.size, 1)

    // Range end: shift held, selection non-empty. Java resolves rangeStartIndex via
    // array.indexOf(rangeStart, false) = 0 and end via array.indexOf(item, false) = 2, selecting [0..2].
    sel.choose(Box(30)) // == array(2), ne array(2)
    assertEquals(
      sel.size,
      3,
      "ISS-774: range endpoints resolve by value equality (Java `indexOf(x, false)`), selecting all 3 items; reference identity would fail to find the anchor and select only 1"
    )
  }
}
