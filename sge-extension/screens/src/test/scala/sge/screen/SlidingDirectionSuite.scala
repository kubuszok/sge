package sge
package screen

import sge.screen.transition.impl.SlidingDirection

class SlidingDirectionSuite extends munit.FunSuite {

  test("all 4 directions exist") {
    val directions = SlidingDirection.values
    assertEquals(directions.length, 4)
  }

  test("UP has correct factors") {
    assertEquals(SlidingDirection.UP.xPosFactor, 0)
    assertEquals(SlidingDirection.UP.yPosFactor, 1)
  }

  test("DOWN has correct factors") {
    assertEquals(SlidingDirection.DOWN.xPosFactor, 0)
    assertEquals(SlidingDirection.DOWN.yPosFactor, -1)
  }

  test("LEFT has correct factors") {
    assertEquals(SlidingDirection.LEFT.xPosFactor, -1)
    assertEquals(SlidingDirection.LEFT.yPosFactor, 0)
  }

  test("RIGHT has correct factors") {
    assertEquals(SlidingDirection.RIGHT.xPosFactor, 1)
    assertEquals(SlidingDirection.RIGHT.yPosFactor, 0)
  }
}
