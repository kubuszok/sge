package sge
package ecs
package systems

class IntervalSystemSuite extends munit.FunSuite {

  private val deltaTime: Float = 0.1f

  private class IntervalSystemSpy extends IntervalSystem(deltaTime * 2.0f) {
    var numUpdates: Int = 0

    override protected def updateInterval(): Unit = {
      numUpdates += 1
    }
  }

  test("calls updateInterval at correct intervals") {
    val engine = new Engine
    val spy = new IntervalSystemSpy

    engine.addSystem(spy)

    for (i <- 1 to 10) {
      engine.update(deltaTime)
      assertEquals(spy.numUpdates, i / 2)
    }
  }

  test("interval accessor returns correct value") {
    val spy = new IntervalSystemSpy
    assertEquals(spy.interval, deltaTime * 2.0f, 0.0001f)
  }

  test("does not call updateInterval before interval elapsed") {
    val engine = new Engine
    val spy = new IntervalSystemSpy

    engine.addSystem(spy)

    engine.update(deltaTime * 0.5f)
    assertEquals(spy.numUpdates, 0)

    engine.update(deltaTime * 0.5f)
    assertEquals(spy.numUpdates, 0)

    engine.update(deltaTime)
    assertEquals(spy.numUpdates, 1)
  }

  test("multiple intervals fire when enough time accumulated") {
    val engine = new Engine
    val spy = new IntervalSystemSpy

    engine.addSystem(spy)

    // Pass enough time for 3 intervals at once
    engine.update(deltaTime * 2.0f * 3.0f)
    assertEquals(spy.numUpdates, 3)
  }
}
