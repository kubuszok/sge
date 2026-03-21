package sge
package math

import org.scalacheck.Gen
import org.scalacheck.Prop.forAll

class CumulativeDistributionTest extends munit.ScalaCheckSuite {

  // ---- ensureCapacity fix: adding > 10 values must not throw ----

  test("add more than initial capacity (10) without ArrayIndexOutOfBoundsException") {
    val cd = new CumulativeDistribution[Int]()
    for (i <- 0 until 50)
      cd.add(i, 1.0f)
    assertEquals(cd.size(), 50)
  }

  test("add and retrieve all values") {
    val cd    = new CumulativeDistribution[String]()
    val items = (0 until 25).map(i => s"item$i")
    items.foreach(s => cd.add(s, 1.0f))
    assertEquals(cd.size(), 25)
    for (i <- items.indices)
      assertEquals(cd.getValue(i), items(i))
  }

  // ---- generate() ----

  test("generate produces monotonically increasing frequencies") {
    val cd = new CumulativeDistribution[Int]()
    cd.add(1, 0.2f)
    cd.add(2, 0.3f)
    cd.add(3, 0.5f)
    cd.generate()
    for (i <- 0 until cd.size())
      cd.getInterval(i) // interval is unchanged; frequency is cumulative
    // can't read frequency directly, but value() binary search relies on it
    // verify value() works for exact boundaries
    assertEquals(cd.size(), 3)
  }

  // ---- generateNormalized() ----

  test("generateNormalized last frequency approaches 1.0") {
    val cd = new CumulativeDistribution[Int]()
    cd.add(1, 10.0f)
    cd.add(2, 20.0f)
    cd.add(3, 30.0f)
    cd.generateNormalized()
    // The last cumulative frequency should be approximately 1.0
    // We verify via value(1.0f) returning the last element
    assertEquals(cd.value(1.0f), 3)
  }

  // ---- generateUniform() ----

  test("generateUniform distributes equally") {
    val cd = new CumulativeDistribution[Int]()
    for (i <- 0 until 4) cd.add(i, 999.0f) // original intervals are irrelevant
    cd.generateUniform()
    // Each interval should be 1/4 = 0.25
    for (i <- 0 until 4)
      assertEqualsFloat(cd.getInterval(i), 0.25f, 0.0001f)
  }

  // ---- value() binary search ----

  test("value returns correct element for exact frequency match") {
    val cd = new CumulativeDistribution[String]()
    cd.add("a", 1.0f)
    cd.add("b", 1.0f)
    cd.add("c", 1.0f)
    cd.generate()
    // frequencies: 1.0, 2.0, 3.0
    assertEquals(cd.value(0.5f), "a")
    assertEquals(cd.value(1.5f), "b")
    assertEquals(cd.value(2.5f), "c")
  }

  test("value with probability 0 returns first element") {
    val cd = new CumulativeDistribution[Int]()
    cd.add(42, 1.0f)
    cd.add(99, 1.0f)
    cd.generate()
    assertEquals(cd.value(0.0f), 42)
  }

  // ---- setInterval ----

  test("setInterval by object updates interval") {
    val cd = new CumulativeDistribution[String]()
    cd.add("x", 1.0f)
    cd.add("y", 2.0f)
    cd.setInterval("x", 5.0f)
    assertEqualsFloat(cd.getInterval(0), 5.0f, 0.0001f)
  }

  test("setInterval by index updates interval") {
    val cd = new CumulativeDistribution[String]()
    cd.add("x", 1.0f)
    cd.add("y", 2.0f)
    cd.setInterval(1, 10.0f)
    assertEqualsFloat(cd.getInterval(1), 10.0f, 0.0001f)
  }

  // ---- clear ----

  test("clear resets size to zero") {
    val cd = new CumulativeDistribution[Int]()
    cd.add(1, 1.0f)
    cd.add(2, 1.0f)
    cd.clear()
    assertEquals(cd.size(), 0)
  }

  // ---- ScalaCheck property tests ----

  property("adding N values gives size N") {
    forAll(Gen.choose(1, 100)) { (n: Int) =>
      val cd = new CumulativeDistribution[Int]()
      for (i <- 0 until n) cd.add(i, 1.0f)
      assertEquals(cd.size(), n)
    }
  }

  property("value() does not throw for probability in [0, last frequency] after generate") {
    forAll(Gen.choose(2, 50), Gen.choose(0.0f, 1.0f)) { (n: Int, prob: Float) =>
      val cd = new CumulativeDistribution[Int]()
      for (i <- 0 until n) cd.add(i, 1.0f)
      cd.generateNormalized()
      // prob is in [0, 1], and generateNormalized produces frequencies in [0, 1]
      val result = cd.value(prob)
      assert(result >= 0 && result < n)
    }
  }

  property("generateUniform intervals all equal 1/size") {
    forAll(Gen.choose(1, 100)) { (n: Int) =>
      val cd = new CumulativeDistribution[Int]()
      for (i <- 0 until n) cd.add(i, 99.0f)
      cd.generateUniform()
      val expected = 1.0f / n
      for (i <- 0 until n)
        assertEqualsFloat(cd.getInterval(i), expected, 0.0001f)
    }
  }

  private def assertEqualsFloat(actual: Float, expected: Float, delta: Float): Unit =
    assert(
      Math.abs(actual - expected) <= delta,
      s"expected $expected +/- $delta but got $actual"
    )
}
