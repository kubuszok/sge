package sge
package ecs
package systems

class IntervalIteratingSystemSuite extends munit.FunSuite {

  private val deltaTime: Float = 0.1f

  private class IntervalComponentSpy extends Component {
    var numUpdates: Int = 0
  }

  private class IntervalIteratingSystemSpy extends IntervalIteratingSystem(
    Family.all(classOf[IntervalComponentSpy]).get(),
    deltaTime * 2.0f
  ) {
    private val im = ComponentMapper.getFor(classOf[IntervalComponentSpy])
    var numStartProcessing: Int = 0
    var numEndProcessing: Int = 0

    override def startProcessing(): Unit = {
      numStartProcessing += 1
    }

    override protected def processEntity(entity: Entity): Unit = {
      im.get(entity).get.numUpdates += 1
    }

    override def endProcessing(): Unit = {
      numEndProcessing += 1
    }
  }

  test("processes entities at interval") {
    val engine = new Engine
    val spy = new IntervalIteratingSystemSpy
    val entities = engine.getEntitiesFor(Family.all(classOf[IntervalComponentSpy]).get())
    val im = ComponentMapper.getFor(classOf[IntervalComponentSpy])

    engine.addSystem(spy)

    for (_ <- 0 until 10) {
      val entity = new Entity
      entity.add(new IntervalComponentSpy)
      engine.addEntity(entity)
    }

    for (i <- 1 to 10) {
      engine.update(deltaTime)

      for (j <- 0 until entities.size) {
        assertEquals(im.get(entities(j)).get.numUpdates, i / 2)
      }
    }
  }

  test("startProcessing and endProcessing called at intervals") {
    val engine = new Engine
    val system = new IntervalIteratingSystemSpy

    engine.addSystem(system)

    engine.update(deltaTime)
    assertEquals(system.numStartProcessing, 0)
    assertEquals(system.numEndProcessing, 0)

    engine.update(deltaTime)
    assertEquals(system.numStartProcessing, 1)
    assertEquals(system.numEndProcessing, 1)
  }
}
