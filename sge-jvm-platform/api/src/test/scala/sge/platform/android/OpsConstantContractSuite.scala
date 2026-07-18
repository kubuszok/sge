// SGE — Cross-module ordinal-constant contract tests
//
// Covers ISS-723 clause c14 (sge-jvm-platform had ZERO test directories).
// Wave 2026-07-18-I, territory I4. CursorOps and KeyboardHeightObserverOps
// publish integer constants whose VALUES are load-bearing contracts:
//   - CursorOps.* must equal sge Cursor.SystemCursor ordinals (the android
//     impl indexes pointer icons by these numbers).
//   - KeyboardHeightObserverOps.ORIENTATION_* must equal Android's
//     Configuration.ORIENTATION_PORTRAIT/LANDSCAPE.
// A renumbering would silently desync the two modules; these tests pin the
// exact contract so such a mutation reddens here.

package sge
package platform
package android

final class OpsConstantContractSuite extends munit.FunSuite {

  test("CursorOps constants match Cursor.SystemCursor ordinals 0..10") {
    assertEquals(CursorOps.Arrow, 0)
    assertEquals(CursorOps.Ibeam, 1)
    assertEquals(CursorOps.Crosshair, 2)
    assertEquals(CursorOps.Hand, 3)
    assertEquals(CursorOps.HorizontalResize, 4)
    assertEquals(CursorOps.VerticalResize, 5)
    assertEquals(CursorOps.NWSEResize, 6)
    assertEquals(CursorOps.NESWResize, 7)
    assertEquals(CursorOps.AllResize, 8)
    assertEquals(CursorOps.NotAllowed, 9)
    assertEquals(CursorOps.None, 10)
  }

  test("CursorOps ordinals are the dense, distinct sequence 0..10") {
    val all = List(
      CursorOps.Arrow,
      CursorOps.Ibeam,
      CursorOps.Crosshair,
      CursorOps.Hand,
      CursorOps.HorizontalResize,
      CursorOps.VerticalResize,
      CursorOps.NWSEResize,
      CursorOps.NESWResize,
      CursorOps.AllResize,
      CursorOps.NotAllowed,
      CursorOps.None
    )
    assertEquals(all, (0 to 10).toList)
    assertEquals(all.distinct.size, 11)
  }

  test("KeyboardHeightObserverOps orientation constants match Android Configuration") {
    assertEquals(KeyboardHeightObserverOps.ORIENTATION_PORTRAIT, 1)
    assertEquals(KeyboardHeightObserverOps.ORIENTATION_LANDSCAPE, 2)
    assertNotEquals(
      KeyboardHeightObserverOps.ORIENTATION_PORTRAIT,
      KeyboardHeightObserverOps.ORIENTATION_LANDSCAPE
    )
  }
}
