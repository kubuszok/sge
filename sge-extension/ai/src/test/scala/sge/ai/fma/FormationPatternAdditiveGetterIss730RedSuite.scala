package sge
package ai
package fma

import sge.ai.utils.Location
import sge.math.Vector

/** Red suite for ISS-730 clause c7 (wave 2026-07-17-F, territory Z).
  *
  * COMPILE-SHAPE red. The `FormationPattern` trait declares an ADDITIVE abstract getter `numberOfSlots: Int` (FormationPattern.scala:43) that the original interface never required.
  *
  * Original interface (com/badlogic/gdx/ai/fma/FormationPattern.java:31-44) declares EXACTLY three members:
  *   - `void setNumberOfSlots (int numberOfSlots)` (a setter — NO getter)
  *   - `Location<T> calculateSlotLocation (Location<T> outLocation, int slotNumber)`
  *   - `boolean supportsSlots (int slotCount)`
  *
  * There is no `getNumberOfSlots()` on the interface, and the concrete `DefensiveCircleFormationPattern` (patterns/DefensiveCircleFormationPattern.java:33,45) keeps `numberOfSlots` as a
  * package-private FIELD read only internally — it never exposes a public getter either.
  *
  * ADJUDICATION (reproducer verdict): the getter is additive public API surface, not a sanctioned improvement. It is read by NO caller in the port (only the setter is used, Formation.scala:80:
  * `pattern.numberOfSlots = ...`). It is not language-forced: a setter-only trait member `def numberOfSlots_=(v: Int): Unit` compiles and is callable explicitly as `pattern.numberOfSlots_=(n)`; only
  * the `x = value` assignment SUGAR at the single call site requires the paired getter (verified on Scala 3.8.4). The faithful fix is to drop the getter from the trait (and the concrete overrides)
  * and call the setter explicitly at Formation.scala:80 — restoring the original setter-only interface contract.
  *
  * This suite defines a `FormationPattern` implementation that provides EXACTLY the original interface's members. It FAILS TO COMPILE while the port declares the extra abstract `numberOfSlots: Int`
  * getter (the class is left abstract for an unimplemented member), and COMPILES once the additive getter is removed.
  *
  * This encodes the ORIGINAL interface shape and MUST NOT be weakened.
  */
class FormationPatternAdditiveGetterIss730RedSuite extends munit.FunSuite {

  /** Faithful-shape implementation: the original interface's three members only — the setter, `calculateSlotLocation`, and `supportsSlots`. No `numberOfSlots` getter, because the original
    * `FormationPattern` interface declares none.
    */
  final private class OriginalShapePattern[T <: Vector[T]] extends FormationPattern[T] {
    override def numberOfSlots_=(numberOfSlots:     Int):                          Unit        = ()
    override def calculateSlotLocation(outLocation: Location[T], slotNumber: Int): Location[T] = outLocation
    override def supportsSlots(slotCount:           Int):                          Boolean     = true
  }

  test(
    "ISS-730 c7: FormationPattern's member set must match the original setter-only interface (no additive numberOfSlots getter)"
  ) {
    // Instantiating the faithful-shape implementation is only possible once the
    // additive `numberOfSlots: Int` getter is removed from the trait.
    val pattern = new OriginalShapePattern[Nothing]()
    pattern.numberOfSlots_=(3)
    assert(pattern.supportsSlots(3))
  }
}
