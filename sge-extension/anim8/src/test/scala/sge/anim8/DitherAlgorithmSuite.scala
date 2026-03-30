package sge
package anim8

class DitherAlgorithmSuite extends munit.FunSuite {

  test("all 22 algorithms exist") {
    assertEquals(DitherAlgorithm.values.length, 22)
  }

  test("DitherAlgorithm.ALL contains all values") {
    assertEquals(DitherAlgorithm.ALL.length, DitherAlgorithm.values.length)
    DitherAlgorithm.values.foreach { alg =>
      assert(DitherAlgorithm.ALL.contains(alg), s"ALL missing $alg")
    }
  }

  test("each algorithm has a non-empty legibleName") {
    DitherAlgorithm.values.foreach { alg =>
      assert(alg.legibleName.nonEmpty, s"$alg has empty legibleName")
    }
  }

  test("toString returns legibleName") {
    DitherAlgorithm.values.foreach { alg =>
      assertEquals(alg.toString, alg.legibleName)
    }
  }

  test("WREN is the default dither with expected name") {
    assertEquals(DitherAlgorithm.WREN.legibleName, "Wren")
  }
}
