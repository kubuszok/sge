package sge
package math

import sge.utils.DynamicArray

class ConvexHullTest extends munit.FunSuite {

  /** Checks that the array contents match the witness in the same cyclic order (but potentially starting at a different offset). The first two elements of the output array should correspond to the
    * last two elements, otherwise the last two elements are ignored.
    */
  private def assertArraySimilar(
    array:         DynamicArray[Float],
    witness:       Array[Float],
    witnessOffset: Int,
    witnessCount:  Int
  ): Unit = {
    val witnessLength = witnessCount + witnessOffset
    assert(witnessCount + witnessOffset <= witness.length)
    assertEquals(array.size, witnessCount + 2)
    assertEqualsDouble(array.items(0).toDouble, array.items(array.size - 2).toDouble, 0.0)
    assertEqualsDouble(array.items(1).toDouble, array.items(array.size - 1).toDouble, 0.0)

    var found = false
    for (offset <- 0 until witnessLength if !found) {
      var contentMatches = true
      for (i <- 0 until witnessLength if contentMatches) {
        val j = ((offset + i) % witnessCount) + witnessOffset
        if (array(i) != witness(j)) {
          contentMatches = false
        }
      }
      if (contentMatches) {
        found = true
      }
    }

    assert(found, s"Array items ${array.toString()} does not match witness array ${witness.mkString("[", ", ", "]")}")
  }

  test("computePolygon") {
    val convexHull              = new ConvexHull()
    val rawPolygon              = Array(0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f)
    val polygonCounterclockwise = Array(1f, 0f, 1f, 1f, 0f, 1f, 0f, 0f)
    assertArraySimilar(convexHull.computePolygon(rawPolygon, 0, 8, false), polygonCounterclockwise, 0, 8)
    assertArraySimilar(convexHull.computePolygon(rawPolygon, false), polygonCounterclockwise, 0, 8)
    assertArraySimilar(convexHull.computePolygon(rawPolygon, 2, 6, false), polygonCounterclockwise, 0, 6)
    assertArraySimilar(convexHull.computePolygon(rawPolygon, 0, 6, false), polygonCounterclockwise, 2, 6)

    assertArraySimilar(new ConvexHull().computePolygon(rawPolygon, 0, 8, false), polygonCounterclockwise, 0, 8)
    assertArraySimilar(new ConvexHull().computePolygon(rawPolygon, false), polygonCounterclockwise, 0, 8)
    assertArraySimilar(new ConvexHull().computePolygon(rawPolygon, 2, 6, false), polygonCounterclockwise, 0, 6)
    assertArraySimilar(new ConvexHull().computePolygon(rawPolygon, 0, 6, false), polygonCounterclockwise, 2, 6)
  }
}
