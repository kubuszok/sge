package sge
package ecs
package systems

class IteratingSystemSuite extends munit.FunSuite {

  private val deltaTime: Float = 0.16f

  private class ComponentA extends Component
  private class ComponentB extends Component
  private class ComponentC extends Component

  private class SpyComponent extends Component {
    var updates: Int = 0
  }

  private class IndexComponent extends Component {
    var index: Int = 0
  }

  private class IteratingSystemMock(family: Family) extends IteratingSystem(family) {
    var numUpdates:         Int = 0
    var numStartProcessing: Int = 0
    var numEndProcessing:   Int = 0

    override def startProcessing(): Unit =
      numStartProcessing += 1

    override protected def processEntity(entity: Entity, deltaTime: Float): Unit =
      numUpdates += 1

    override def endProcessing(): Unit =
      numEndProcessing += 1
  }

  private class IteratingRemovalSystem
      extends IteratingSystem(
        Family.all(classOf[SpyComponent], classOf[IndexComponent]).get()
      ) {
    private val sm = ComponentMapper.getFor(classOf[SpyComponent])
    private val im = ComponentMapper.getFor(classOf[IndexComponent])

    override protected def processEntity(entity: Entity, deltaTime: Float): Unit = {
      val index = im.get(entity).get.index
      if (index % 2 == 0) {
        engine.get.removeEntity(entity)
      } else {
        sm.get(entity).get.updates += 1
      }
    }
  }

  private class IteratingComponentRemovalSystem
      extends IteratingSystem(
        Family.all(classOf[SpyComponent], classOf[IndexComponent]).get()
      ) {
    private val sm = ComponentMapper.getFor(classOf[SpyComponent])
    private val im = ComponentMapper.getFor(classOf[IndexComponent])

    override protected def processEntity(entity: Entity, deltaTime: Float): Unit = {
      val index = im.get(entity).get.index
      if (index % 2 == 0) {
        entity.remove(classOf[SpyComponent])
        entity.remove(classOf[IndexComponent])
      } else {
        sm.get(entity).get.updates += 1
      }
    }
  }

  test("processes entities with correct family") {
    val engine = new Engine
    val family = Family.all(classOf[ComponentA], classOf[ComponentB]).get()
    val system = new IteratingSystemMock(family)
    val e      = new Entity

    engine.addSystem(system)
    engine.addEntity(e)

    // Entity has only ComponentA -- should not match
    e.add(new ComponentA)
    engine.update(deltaTime)
    assertEquals(system.numUpdates, 0)

    // Entity has ComponentA and ComponentB -- should match
    system.numUpdates = 0
    e.add(new ComponentB)
    engine.update(deltaTime)
    assertEquals(system.numUpdates, 1)

    // Entity has ComponentA, B, C -- still matches
    system.numUpdates = 0
    e.add(new ComponentC)
    engine.update(deltaTime)
    assertEquals(system.numUpdates, 1)

    // Remove ComponentA -- should not match
    system.numUpdates = 0
    e.remove(classOf[ComponentA])
    engine.update(deltaTime)
    assertEquals(system.numUpdates, 0)
  }

  test("entity removal while iterating") {
    val engine   = new Engine
    val entities = engine.getEntitiesFor(Family.all(classOf[SpyComponent], classOf[IndexComponent]).get())
    val sm       = ComponentMapper.getFor(classOf[SpyComponent])

    engine.addSystem(new IteratingRemovalSystem)

    val numEntities = 10
    for (i <- 0 until numEntities) {
      val e = new Entity
      e.add(new SpyComponent)
      val ic = new IndexComponent
      ic.index = i + 1
      e.add(ic)
      engine.addEntity(e)
    }

    engine.update(deltaTime)

    assertEquals(entities.size, numEntities / 2)

    for (i <- 0 until entities.size)
      assertEquals(sm.get(entities(i)).get.updates, 1)
  }

  test("component removal while iterating") {
    val engine   = new Engine
    val entities = engine.getEntitiesFor(Family.all(classOf[SpyComponent], classOf[IndexComponent]).get())
    val sm       = ComponentMapper.getFor(classOf[SpyComponent])

    engine.addSystem(new IteratingComponentRemovalSystem)

    val numEntities = 10
    for (i <- 0 until numEntities) {
      val e = new Entity
      e.add(new SpyComponent)
      val ic = new IndexComponent
      ic.index = i + 1
      e.add(ic)
      engine.addEntity(e)
    }

    engine.update(deltaTime)

    assertEquals(entities.size, numEntities / 2)

    for (i <- 0 until entities.size)
      assertEquals(sm.get(entities(i)).get.updates, 1)
  }

  test("startProcessing and endProcessing called") {
    val engine = new Engine
    val system = new IteratingSystemMock(Family.all().get())

    engine.addSystem(system)

    engine.update(deltaTime)
    assertEquals(system.numStartProcessing, 1)
    assertEquals(system.numEndProcessing, 1)

    engine.update(deltaTime)
    assertEquals(system.numStartProcessing, 2)
    assertEquals(system.numEndProcessing, 2)
  }
}
