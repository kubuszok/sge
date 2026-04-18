package sge
package ai

class DefaultTimepieceSuite extends munit.FunSuite {

  private val Eps = 1e-6f

  // ── maxDeltaTime defaults ─────────────────────────────────────────────

  test("default maxDeltaTime is Float.PositiveInfinity") {
    val tp = new DefaultTimepiece()
    assertEquals(tp.maxDeltaTime, Float.PositiveInfinity)
  }

  test("custom maxDeltaTime via constructor") {
    val tp = new DefaultTimepiece(maxDeltaTime = 0.25f)
    assertEqualsFloat(tp.maxDeltaTime, 0.25f, Eps)
  }

  // ── clamping behavior ─────────────────────────────────────────────────

  test("delta time is clamped when exceeding maxDeltaTime") {
    val tp = new DefaultTimepiece(maxDeltaTime = 0.1f)
    tp.update(0.5f)
    assertEqualsFloat(tp.deltaTime, 0.1f, Eps)
  }

  test("delta time is NOT clamped when below maxDeltaTime") {
    val tp = new DefaultTimepiece(maxDeltaTime = 1.0f)
    tp.update(0.016f)
    assertEqualsFloat(tp.deltaTime, 0.016f, Eps)
  }

  test("delta time equals maxDeltaTime when exactly at limit") {
    val tp = new DefaultTimepiece(maxDeltaTime = 0.1f)
    tp.update(0.1f)
    assertEqualsFloat(tp.deltaTime, 0.1f, Eps)
  }

  // ── time accumulation uses clamped delta ──────────────────────────────

  test("time accumulates with clamped delta, not raw delta") {
    val tp = new DefaultTimepiece(maxDeltaTime = 0.1f)
    tp.update(0.5f) // clamped to 0.1
    tp.update(0.3f) // clamped to 0.1
    assertEqualsFloat(tp.time, 0.2f, Eps)
  }

  test("time accumulates normally without clamping") {
    val tp = new DefaultTimepiece()
    tp.update(0.016f)
    tp.update(0.016f)
    tp.update(0.016f)
    assertEqualsFloat(tp.time, 0.048f, Eps)
  }

  // ── initial state ─────────────────────────────────────────────────────

  test("initial time and deltaTime are zero") {
    val tp = new DefaultTimepiece()
    assertEqualsFloat(tp.time, 0f, Eps)
    assertEqualsFloat(tp.deltaTime, 0f, Eps)
  }
}
