package sge
package math

class FloatCounterTest extends munit.FunSuite {

  private val eps = 0.0001

  test("initial state after construction") {
    val fc = new FloatCounter(0)
    assertEquals(fc.count, 0)
    assertEqualsDouble(fc.total.toDouble, 0.0, eps)
    assertEqualsDouble(fc.min.toDouble, Float.MaxValue.toDouble, eps)
    assertEqualsDouble(fc.max.toDouble, (-Float.MaxValue).toDouble, eps)
  }

  test("put updates count and total") {
    val fc = new FloatCounter(0)
    fc.put(5f)
    assertEquals(fc.count, 1)
    assertEqualsDouble(fc.total.toDouble, 5.0, eps)
    assertEqualsDouble(fc.latest.toDouble, 5.0, eps)
  }

  test("average computed correctly") {
    val fc = new FloatCounter(0)
    fc.put(2f)
    fc.put(4f)
    fc.put(6f)
    assertEqualsDouble(fc.average.toDouble, 4.0, eps)
  }

  test("min and max tracked without windowed mean") {
    val fc = new FloatCounter(0)
    fc.put(5f)
    fc.put(2f)
    fc.put(8f)
    fc.put(1f)
    assertEqualsDouble(fc.min.toDouble, 1.0, eps)
    assertEqualsDouble(fc.max.toDouble, 8.0, eps)
  }

  test("windowed mean is populated when windowSize > 1") {
    val fc = new FloatCounter(3)
    assert(fc.mean.isDefined)
    fc.put(1f)
    fc.put(2f)
    fc.put(3f)
    // After 3 values with window 3, the windowed mean should have enough data
    assert(fc.mean.get.hasEnoughData())
    assertEqualsDouble(fc.value.toDouble, 2.0, eps)
  }

  test("no windowed mean when windowSize <= 1") {
    val fc = new FloatCounter(1)
    assert(fc.mean.isEmpty)
    fc.put(42f)
    assertEqualsDouble(fc.value.toDouble, 42.0, eps)
  }

  test("reset clears all values") {
    val fc = new FloatCounter(3)
    fc.put(5f)
    fc.put(10f)
    fc.reset()
    assertEquals(fc.count, 0)
    assertEqualsDouble(fc.total.toDouble, 0.0, eps)
    assertEqualsDouble(fc.latest.toDouble, 0.0, eps)
    assertEqualsDouble(fc.min.toDouble, Float.MaxValue.toDouble, eps)
  }

  test("latest tracks most recent value") {
    val fc = new FloatCounter(0)
    fc.put(1f)
    fc.put(2f)
    fc.put(3f)
    assertEqualsDouble(fc.latest.toDouble, 3.0, eps)
  }
}
