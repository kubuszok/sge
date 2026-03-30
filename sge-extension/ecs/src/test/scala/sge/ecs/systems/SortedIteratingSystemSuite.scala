package sge
package ecs
package systems

import scala.collection.mutable

class SortedIteratingSystemSuite extends munit.FunSuite {

  private val deltaTime: Float = 0.16f

  class OrderComponent(var name: String, var zLayer: Int) extends Component

  private class ComponentB extends Component
  private class ComponentC extends Component

  private class SpyComponent extends Component {
    var updates: Int = 0
  }

  private class IndexComponent extends Component {
    var index: Int = 0
  }

  private val orderMapper = ComponentMapper.getFor(classOf[OrderComponent])

  private val comparator: Ordering[Entity] = (a: Entity, b: Entity) => {
    val ac = orderMapper.get(a).get
    val bc = orderMapper.get(b).get
    Integer.compare(ac.zLayer, bc.zLayer)
  }

  private class SortedIteratingSystemMock(family: Family) extends SortedIteratingSystem(family, comparator) {
    val expectedNames: mutable.Queue[String] = mutable.Queue.empty
    var numStartProcessing: Int = 0
    var numEndProcessing: Int = 0

    override def update(deltaTime: Float): Unit = {
      super.update(deltaTime)
      assert(expectedNames.isEmpty, s"Not all expected names were consumed: $expectedNames")
    }

    override def startProcessing(): Unit = {
      numStartProcessing += 1
    }

    override protected def processEntity(entity: Entity, deltaTime: Float): Unit = {
      val component = orderMapper.get(entity)
      assert(component.isDefined)
      assert(expectedNames.nonEmpty, "Expected more entities to process")
      assertEquals(expectedNames.dequeue(), component.get.name)
    }

    override def endProcessing(): Unit = {
      numEndProcessing += 1
    }
  }

  private class IteratingRemovalSystem extends SortedIteratingSystem(
    Family.all(classOf[SpyComponent], classOf[IndexComponent]).get(),
    comparator
  ) {
    private val sm = ComponentMapper.getFor(classOf[SpyComponent])
    private val im = ComponentMapper.getFor(classOf[IndexComponent])
    private var localEngine: Engine = scala.compiletime.uninitialized

    override def addedToEngine(engine: Engine): Unit = {
      super.addedToEngine(engine)
      localEngine = engine
    }

    override protected def processEntity(entity: Entity, deltaTime: Float): Unit = {
      val index = im.get(entity).get.index
      if (index % 2 == 0) {
        localEngine.removeEntity(entity)
      } else {
        sm.get(entity).get.updates += 1
      }
    }
  }

  private class IteratingComponentRemovalSystem extends SortedIteratingSystem(
    Family.all(classOf[SpyComponent], classOf[IndexComponent]).get(),
    comparator
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

  private def createOrderEntity(name: String, zLayer: Int): Entity = {
    val entity = new Entity
    entity.add(new OrderComponent(name, zLayer))
    entity
  }

  test("processes entities with correct family") {
    val engine = new Engine
    val family = Family.all(classOf[OrderComponent], classOf[ComponentB]).get()
    val system = new SortedIteratingSystemMock(family)
    val e = new Entity

    engine.addSystem(system)
    engine.addEntity(e)

    // Only OrderComponent -- should not process
    e.add(new OrderComponent("A", 0))
    engine.update(deltaTime)

    // OrderComponent and ComponentB -- should process
    e.add(new ComponentB)
    system.expectedNames.enqueue("A")
    engine.update(deltaTime)

    // OrderComponent, ComponentB, ComponentC -- still processes
    e.add(new ComponentC)
    system.expectedNames.enqueue("A")
    engine.update(deltaTime)

    // Remove OrderComponent -- should not process
    e.remove(classOf[OrderComponent])
    engine.update(deltaTime)
  }

  test("processes entities in sorted order") {
    val engine = new Engine
    val family = Family.all(classOf[OrderComponent]).get()
    val system = new SortedIteratingSystemMock(family)
    engine.addSystem(system)

    val a = createOrderEntity("A", 0)
    val b = createOrderEntity("B", 1)
    val c = createOrderEntity("C", 3)
    val d = createOrderEntity("D", 2)

    engine.addEntity(a)
    engine.addEntity(b)
    engine.addEntity(c)
    system.expectedNames.enqueue("A")
    system.expectedNames.enqueue("B")
    system.expectedNames.enqueue("C")
    engine.update(0)

    engine.addEntity(d)
    system.expectedNames.enqueue("A")
    system.expectedNames.enqueue("B")
    system.expectedNames.enqueue("D")
    system.expectedNames.enqueue("C")
    engine.update(0)
  }

  test("forceSort re-sorts with updated values") {
    val engine = new Engine
    val family = Family.all(classOf[OrderComponent]).get()
    val system = new SortedIteratingSystemMock(family)
    engine.addSystem(system)

    val a = createOrderEntity("A", 0)
    val b = createOrderEntity("B", 1)
    val c = createOrderEntity("C", 3)
    val d = createOrderEntity("D", 2)

    engine.addEntity(a)
    engine.addEntity(b)
    engine.addEntity(c)
    engine.addEntity(d)

    // Initial order
    system.expectedNames.enqueue("A")
    system.expectedNames.enqueue("B")
    system.expectedNames.enqueue("D")
    system.expectedNames.enqueue("C")
    engine.update(0)

    // Reverse the z-layers
    orderMapper.get(a).get.zLayer = 3
    orderMapper.get(b).get.zLayer = 2
    orderMapper.get(c).get.zLayer = 1
    orderMapper.get(d).get.zLayer = 0
    system.forceSort()

    system.expectedNames.enqueue("D")
    system.expectedNames.enqueue("C")
    system.expectedNames.enqueue("B")
    system.expectedNames.enqueue("A")
    engine.update(0)
  }

  test("entity removal while iterating") {
    val engine = new Engine
    val entities = engine.getEntitiesFor(Family.all(classOf[SpyComponent], classOf[IndexComponent]).get())
    val sm = ComponentMapper.getFor(classOf[SpyComponent])

    engine.addSystem(new IteratingRemovalSystem)

    val numEntities = 10
    for (i <- 0 until numEntities) {
      val e = new Entity
      e.add(new SpyComponent)
      e.add(new OrderComponent("" + i, i))
      val ic = new IndexComponent
      ic.index = i + 1
      e.add(ic)
      engine.addEntity(e)
    }

    engine.update(deltaTime)
    assertEquals(entities.size, numEntities / 2)

    for (i <- 0 until entities.size) {
      assertEquals(sm.get(entities(i)).get.updates, 1)
    }
  }

  test("component removal while iterating") {
    val engine = new Engine
    val entities = engine.getEntitiesFor(Family.all(classOf[SpyComponent], classOf[IndexComponent]).get())
    val sm = ComponentMapper.getFor(classOf[SpyComponent])

    engine.addSystem(new IteratingComponentRemovalSystem)

    val numEntities = 10
    for (i <- 0 until numEntities) {
      val e = new Entity
      e.add(new SpyComponent)
      e.add(new OrderComponent("" + i, i))
      val ic = new IndexComponent
      ic.index = i + 1
      e.add(ic)
      engine.addEntity(e)
    }

    engine.update(deltaTime)
    assertEquals(entities.size, numEntities / 2)

    for (i <- 0 until entities.size) {
      assertEquals(sm.get(entities(i)).get.updates, 1)
    }
  }

  test("startProcessing and endProcessing called") {
    val engine = new Engine
    val system = new SortedIteratingSystemMock(Family.all().get())

    engine.addSystem(system)

    engine.update(deltaTime)
    assertEquals(system.numStartProcessing, 1)
    assertEquals(system.numEndProcessing, 1)

    engine.update(deltaTime)
    assertEquals(system.numStartProcessing, 2)
    assertEquals(system.numEndProcessing, 2)
  }
}
