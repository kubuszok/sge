package sge
package math

class WindowedMeanTest extends munit.FunSuite {

  private val eps = 0.0001

  test("hasEnoughData false initially") {
    val wm = new WindowedMean(3)
    assert(!wm.hasEnoughData())
  }

  test("hasEnoughData true after filling window") {
    val wm = new WindowedMean(3)
    wm.addValue(1f)
    wm.addValue(2f)
    wm.addValue(3f)
    assert(wm.hasEnoughData())
  }

  test("mean of uniform values") {
    val wm = new WindowedMean(4)
    wm.addValue(5f)
    wm.addValue(5f)
    wm.addValue(5f)
    wm.addValue(5f)
    assertEqualsDouble(wm.mean.toDouble, 5.0, eps)
  }

  test("mean of varied values") {
    val wm = new WindowedMean(4)
    wm.addValue(1f)
    wm.addValue(2f)
    wm.addValue(3f)
    wm.addValue(4f)
    assertEqualsDouble(wm.mean.toDouble, 2.5, eps)
  }

  test("mean returns 0 when not enough data") {
    val wm = new WindowedMean(5)
    wm.addValue(100f)
    assertEqualsDouble(wm.mean.toDouble, 0.0, eps)
  }

  test("oldest value after wrapping") {
    val wm = new WindowedMean(3)
    wm.addValue(1f)
    wm.addValue(2f)
    wm.addValue(3f)
    // Window is full: [1, 2, 3], oldest is 1
    assertEqualsDouble(wm.oldest.toDouble, 1.0, eps)
    // Add one more, wraps: [4, 2, 3], oldest is 2
    wm.addValue(4f)
    assertEqualsDouble(wm.oldest.toDouble, 2.0, eps)
  }

  test("latest value") {
    val wm = new WindowedMean(3)
    wm.addValue(1f)
    assertEqualsDouble(wm.latest.toDouble, 1.0, eps)
    wm.addValue(2f)
    assertEqualsDouble(wm.latest.toDouble, 2.0, eps)
    wm.addValue(3f)
    assertEqualsDouble(wm.latest.toDouble, 3.0, eps)
  }

  test("standardDeviation of uniform values is 0") {
    val wm = new WindowedMean(3)
    wm.addValue(5f)
    wm.addValue(5f)
    wm.addValue(5f)
    assertEqualsDouble(wm.standardDeviation().toDouble, 0.0, eps)
  }

  test("standardDeviation computed correctly") {
    val wm = new WindowedMean(4)
    wm.addValue(2f)
    wm.addValue(4f)
    wm.addValue(4f)
    wm.addValue(4f)
    // Mean = 3.5, variance = ((2-3.5)^2 + (4-3.5)^2 + (4-3.5)^2 + (4-3.5)^2) / 4 = (2.25 + 0.25 + 0.25 + 0.25) / 4 = 0.75
    // StdDev = sqrt(0.75) ~ 0.866
    assertEqualsDouble(wm.standardDeviation().toDouble, Math.sqrt(0.75), eps)
  }

  test("standardDeviation returns 0 when not enough data") {
    val wm = new WindowedMean(5)
    wm.addValue(1f)
    assertEqualsDouble(wm.standardDeviation().toDouble, 0.0, eps)
  }

  test("lowest and highest") {
    val wm = new WindowedMean(4)
    wm.addValue(3f)
    wm.addValue(1f)
    wm.addValue(4f)
    wm.addValue(2f)
    assertEqualsDouble(wm.lowest.toDouble, 1.0, eps)
    assertEqualsDouble(wm.highest.toDouble, 4.0, eps)
  }

  test("valueCount tracks added values") {
    val wm = new WindowedMean(5)
    assertEquals(wm.valueCount, 0)
    wm.addValue(1f)
    assertEquals(wm.valueCount, 1)
    wm.addValue(2f)
    assertEquals(wm.valueCount, 2)
  }

  test("valueCount caps at window size") {
    val wm = new WindowedMean(3)
    wm.addValue(1f)
    wm.addValue(2f)
    wm.addValue(3f)
    wm.addValue(4f)
    assertEquals(wm.valueCount, 3)
  }

  test("clear resets state") {
    val wm = new WindowedMean(3)
    wm.addValue(1f)
    wm.addValue(2f)
    wm.addValue(3f)
    wm.clear()
    assert(!wm.hasEnoughData())
    assertEquals(wm.valueCount, 0)
  }

  test("windowValues before window full") {
    val wm = new WindowedMean(5)
    wm.addValue(10f)
    wm.addValue(20f)
    val wv = wm.windowValues
    assertEquals(wv.length, 2)
    assertEqualsDouble(wv(0).toDouble, 10.0, eps)
    assertEqualsDouble(wv(1).toDouble, 20.0, eps)
  }

  test("windowValues after wrapping") {
    val wm = new WindowedMean(3)
    wm.addValue(1f)
    wm.addValue(2f)
    wm.addValue(3f)
    wm.addValue(4f) // wraps: buffer = [4, 2, 3], oldest = index 1
    val wv = wm.windowValues
    assertEquals(wv.length, 3)
    // Should be in order: oldest to latest
    assertEqualsDouble(wv(0).toDouble, 2.0, eps)
    assertEqualsDouble(wv(1).toDouble, 3.0, eps)
    assertEqualsDouble(wv(2).toDouble, 4.0, eps)
  }

  test("windowSize accessor") {
    val wm = new WindowedMean(7)
    assertEquals(wm.windowSize, 7)
  }
}
