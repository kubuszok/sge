package sge
package colorful

class TrigToolsSuite extends munit.FunSuite {

  private val Epsilon = 0.001f

  @scala.annotation.nowarn("msg=unused private member")
  private def assertApprox(actual: Float, expected: Float, tolerance: Float = Epsilon, clue: String = "")(using
    munit.Location
  ): Unit = {
    val msg = if (clue.nonEmpty) s"$clue: " else ""
    assert(
      Math.abs(actual - expected) <= tolerance,
      s"${msg}expected ~$expected but got $actual (diff=${Math.abs(actual - expected)})"
    )
  }

  test("sin at known angles") {
    assertApprox(TrigTools.sin(0f), 0f, clue = "sin(0)")
    assertApprox(TrigTools.sin(TrigTools.HALF_PI), 1f, clue = "sin(PI/2)")
    assertApprox(TrigTools.sin(TrigTools.PI), 0f, clue = "sin(PI)")
    assertApprox(TrigTools.sin(TrigTools.PI * 1.5f), -1f, clue = "sin(3PI/2)")
  }

  test("cos at known angles") {
    assertApprox(TrigTools.cos(0f), 1f, clue = "cos(0)")
    assertApprox(TrigTools.cos(TrigTools.HALF_PI), 0f, clue = "cos(PI/2)")
    assertApprox(TrigTools.cos(TrigTools.PI), -1f, clue = "cos(PI)")
  }

  test("sin squared plus cos squared equals 1") {
    val angles = Seq(0.1f, 0.5f, 1.0f, 2.0f, 3.0f, 4.5f)
    angles.foreach { angle =>
      val s = TrigTools.sin(angle)
      val c = TrigTools.cos(angle)
      assertApprox(s * s + c * c, 1f, tolerance = 0.002f, clue = s"angle=$angle")
    }
  }

  test("atan2Turns at known positions") {
    // atan2(0, 1) = 0 turns (positive x-axis)
    assertApprox(TrigTools.atan2Turns(0f, 1f), 0f, clue = "atan2(0,1)")
    // atan2(1, 0) = 0.25 turns (positive y-axis, 90 degrees)
    assertApprox(TrigTools.atan2Turns(1f, 0f), 0.25f, clue = "atan2(1,0)")
    // atan2(0, -1) = 0.5 turns (negative x-axis, 180 degrees)
    assertApprox(TrigTools.atan2Turns(0f, -1f), 0.5f, clue = "atan2(0,-1)")
    // atan2(-1, 0) = 0.75 turns (negative y-axis, 270 degrees)
    assertApprox(TrigTools.atan2Turns(-1f, 0f), 0.75f, clue = "atan2(-1,0)")
  }

  test("sinPrecise and cosPrecise match Math.sin and Math.cos closely") {
    val angle = 1.234f
    assertApprox(TrigTools.sinPrecise(angle), Math.sin(angle).toFloat, tolerance = 1e-5f, clue = "sinPrecise")
    assertApprox(TrigTools.cosPrecise(angle), Math.cos(angle).toFloat, tolerance = 1e-5f, clue = "cosPrecise")
  }
}
