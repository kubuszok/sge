package sge
package ecs

import scala.collection.mutable.ArrayBuffer

import sge.ecs.utils.ImmutableArray
import sge.utils.Nullable

// Top-level class with public no-arg constructor for createComponent test
class EngineTestComponentD extends Component

class EngineSuite extends munit.FunSuite {

  private val deltaTime: Float = 0.16f

  private class ComponentA extends Component
  private class ComponentB extends Component
  private class ComponentC extends Component

  private class EntityListenerMock extends EntityListener {
    var addedCount:   Int = 0
    var removedCount: Int = 0

    override def entityAdded(entity: Entity): Unit = {
      addedCount += 1
      assert(entity != null)
    }

    override def entityRemoved(entity: Entity): Unit = {
      removedCount += 1
      assert(entity != null)
    }
  }

  private class AddComponentBEntityListenerMock extends EntityListenerMock {
    override def entityAdded(entity: Entity): Unit = {
      super.entityAdded(entity)
      entity.add(new ComponentB)
    }
  }

  private class EntitySystemMock(val updates: ArrayBuffer[Int] = null, p: Int = 0) extends EntitySystem(p) {
    var updateCalls:  Int = 0
    var addedCalls:   Int = 0
    var removedCalls: Int = 0

    override def update(deltaTime: Float): Unit = {
      updateCalls += 1
      if (updates != null) {
        updates += priority
      }
    }

    override def addedToEngine(engine: Engine): Unit = {
      addedCalls += 1
      assert(engine != null)
    }

    override def removedFromEngine(engine: Engine): Unit = {
      removedCalls += 1
      assert(engine != null)
    }
  }

  private class EntitySystemMockA(updates: ArrayBuffer[Int] = null, p: Int = 0) extends EntitySystemMock(updates, p)
  private class EntitySystemMockB(updates: ArrayBuffer[Int] = null, p: Int = 0) extends EntitySystemMock(updates, p)

  private class CounterComponent extends Component {
    var counter: Int = 0
  }

  private class CounterSystem extends EntitySystem() {
    private var entities: ImmutableArray[Entity] = scala.compiletime.uninitialized

    override def addedToEngine(engine: Engine): Unit =
      entities = engine.getEntitiesFor(Family.all(classOf[CounterComponent]).get())

    override def update(deltaTime: Float): Unit = {
      var i = 0
      while (i < entities.size) {
        if (i % 2 == 0) {
          entities(i).getComponent(classOf[CounterComponent]).get.counter += 1
        } else {
          engine.get.removeEntity(entities(i))
        }
        i += 1
      }
    }
  }

  test("addEntity and removeEntity") {
    val engine    = new Engine
    val listenerA = new EntityListenerMock
    val listenerB = new EntityListenerMock

    engine.addEntityListener(listenerA)
    engine.addEntityListener(listenerB)

    val entity1 = new Entity
    engine.addEntity(entity1)

    assertEquals(listenerA.addedCount, 1)
    assertEquals(listenerB.addedCount, 1)

    engine.removeEntityListener(listenerB)

    val entity2 = new Entity
    engine.addEntity(entity2)

    assertEquals(listenerA.addedCount, 2)
    assertEquals(listenerB.addedCount, 1)

    engine.addEntityListener(listenerB)

    engine.removeAllEntities()

    assertEquals(listenerA.removedCount, 2)
    assertEquals(listenerB.removedCount, 2)
  }

  test("addSystem and removeSystem") {
    val engine  = new Engine
    val systemA = new EntitySystemMockA
    val systemB = new EntitySystemMockB

    assert(engine.getSystem(classOf[EntitySystemMockA]).isEmpty)
    assert(engine.getSystem(classOf[EntitySystemMockB]).isEmpty)

    engine.addSystem(systemA)
    engine.addSystem(systemB)

    assert(engine.getSystem(classOf[EntitySystemMockA]).isDefined)
    assert(engine.getSystem(classOf[EntitySystemMockB]).isDefined)
    assertEquals(systemA.addedCalls, 1)
    assertEquals(systemB.addedCalls, 1)

    engine.removeSystem(systemA)
    engine.removeSystem(systemB)

    assert(engine.getSystem(classOf[EntitySystemMockA]).isEmpty)
    assert(engine.getSystem(classOf[EntitySystemMockB]).isEmpty)
    assertEquals(systemA.removedCalls, 1)
    assertEquals(systemB.removedCalls, 1)

    engine.addSystem(systemA)
    engine.addSystem(systemB)
    engine.removeAllSystems()

    assert(engine.getSystem(classOf[EntitySystemMockA]).isEmpty)
    assert(engine.getSystem(classOf[EntitySystemMockB]).isEmpty)
    assertEquals(systemA.removedCalls, 2)
    assertEquals(systemB.removedCalls, 2)
  }

  test("getSystems") {
    val engine  = new Engine
    val systemA = new EntitySystemMockA
    val systemB = new EntitySystemMockB

    assertEquals(engine.getSystems.size, 0)

    engine.addSystem(systemA)
    engine.addSystem(systemB)

    assertEquals(engine.getSystems.size, 2)
  }

  test("add two systems of same class replaces first") {
    val engine  = new Engine
    val system1 = new EntitySystemMockA
    val system2 = new EntitySystemMockA

    assertEquals(engine.getSystems.size, 0)

    engine.addSystem(system1)
    assertEquals(engine.getSystems.size, 1)
    assert(engine.getSystem(classOf[EntitySystemMockA]).get eq system1)

    engine.addSystem(system2)
    assertEquals(engine.getSystems.size, 1)
    assert(engine.getSystem(classOf[EntitySystemMockA]).get eq system2)
  }

  test("update calls systems") {
    val engine  = new Engine
    val systemA = new EntitySystemMockA
    val systemB = new EntitySystemMockB

    engine.addSystem(systemA)
    engine.addSystem(systemB)

    val numUpdates = 10
    for (i <- 0 until numUpdates) {
      assertEquals(systemA.updateCalls, i)
      assertEquals(systemB.updateCalls, i)
      engine.update(deltaTime)
      assertEquals(systemA.updateCalls, i + 1)
      assertEquals(systemB.updateCalls, i + 1)
    }

    engine.removeSystem(systemB)

    for (i <- 0 until numUpdates) {
      assertEquals(systemA.updateCalls, i + numUpdates)
      assertEquals(systemB.updateCalls, numUpdates)
      engine.update(deltaTime)
      assertEquals(systemA.updateCalls, i + 1 + numUpdates)
      assertEquals(systemB.updateCalls, numUpdates)
    }
  }

  test("update calls systems in priority order") {
    val updates = ArrayBuffer[Int]()
    val engine  = new Engine
    val system1 = new EntitySystemMockA(updates, 2)
    val system2 = new EntitySystemMockB(updates, 1)

    engine.addSystem(system1)
    engine.addSystem(system2)

    engine.update(deltaTime)

    var previous = Int.MinValue
    for (value <- updates) {
      assert(value >= previous, s"Expected $value >= $previous")
      previous = value
    }
  }

  test("system engine reference lifecycle") {
    val engine = new Engine
    val system = new EntitySystemMock

    assert(system.engine.isEmpty)
    engine.addSystem(system)
    assert(system.engine.isDefined)
    assert(system.engine.get eq engine)
    engine.removeSystem(system)
    assert(system.engine.isEmpty)
  }

  test("ignoreSystem when processing is false") {
    val engine = new Engine
    val system = new EntitySystemMock

    engine.addSystem(system)

    val numUpdates = 10
    for (i <- 0 until numUpdates) {
      system.processing = i % 2 == 0
      engine.update(deltaTime)
      assertEquals(system.updateCalls, i / 2 + 1)
    }
  }

  test("getEntitiesFor family") {
    val engine         = new Engine
    val family         = Family.all(classOf[ComponentA], classOf[ComponentB]).get()
    val familyEntities = engine.getEntitiesFor(family)

    assertEquals(familyEntities.size, 0)

    val entity1 = new Entity
    val entity2 = new Entity
    val entity3 = new Entity
    val entity4 = new Entity

    entity1.add(new ComponentA).add(new ComponentB)
    entity2.add(new ComponentA).add(new ComponentC)
    entity3.add(new ComponentA).add(new ComponentB).add(new ComponentC)
    entity4.add(new ComponentA).add(new ComponentB).add(new ComponentC)

    engine.addEntity(entity1)
    engine.addEntity(entity2)
    engine.addEntity(entity3)
    engine.addEntity(entity4)

    assertEquals(familyEntities.size, 3)
    assert(familyEntities.contains(entity1))
    assert(familyEntities.contains(entity3))
    assert(familyEntities.contains(entity4))
    assert(!familyEntities.contains(entity2))
  }

  test("entity for family with removal") {
    val engine = new Engine
    val entity = new Entity
    entity.add(new ComponentA)
    engine.addEntity(entity)

    val entities = engine.getEntitiesFor(Family.all(classOf[ComponentA]).get())
    assertEquals(entities.size, 1)
    assert(entities.contains(entity))

    engine.removeEntity(entity)
    assertEquals(entities.size, 0)
    assert(!entities.contains(entity))
  }

  test("entities for family after adding components post-addEntity") {
    val engine         = new Engine
    val family         = Family.all(classOf[ComponentA], classOf[ComponentB]).get()
    val familyEntities = engine.getEntitiesFor(family)

    val entity1 = new Entity
    val entity2 = new Entity

    engine.addEntity(entity1)
    engine.addEntity(entity2)

    entity1.add(new ComponentA).add(new ComponentB)
    entity2.add(new ComponentA).add(new ComponentC)

    assertEquals(familyEntities.size, 1)
    assert(familyEntities.contains(entity1))
    assert(!familyEntities.contains(entity2))
  }

  test("entity component change updates family membership") {
    val engine         = new Engine
    val family         = Family.all(classOf[ComponentA], classOf[ComponentB]).get()
    val familyEntities = engine.getEntitiesFor(family)

    val entity1 = new Entity
    val entity2 = new Entity

    engine.addEntity(entity1)
    engine.addEntity(entity2)

    entity1.add(new ComponentA).add(new ComponentB)
    entity2.add(new ComponentA).add(new ComponentC)

    assertEquals(familyEntities.size, 1)

    entity1.remove(classOf[ComponentA])
    assertEquals(familyEntities.size, 0)

    entity2.add(new ComponentB)
    assertEquals(familyEntities.size, 1)
    assert(familyEntities.contains(entity2))
  }

  test("family filtering with removal and exclusion") {
    val engine            = new Engine
    val entitiesWithAOnly = engine.getEntitiesFor(
      Family.all(classOf[ComponentA]).exclude(classOf[ComponentB]).get()
    )
    val entitiesWithB = engine.getEntitiesFor(Family.all(classOf[ComponentB]).get())

    val entity1 = new Entity
    val entity2 = new Entity

    engine.addEntity(entity1)
    engine.addEntity(entity2)

    entity1.add(new ComponentA)
    entity2.add(new ComponentA).add(new ComponentB)

    assertEquals(entitiesWithAOnly.size, 1)
    assertEquals(entitiesWithB.size, 1)

    entity2.remove(classOf[ComponentB])

    assertEquals(entitiesWithAOnly.size, 2)
    assertEquals(entitiesWithB.size, 0)
  }

  test("entity removal while iterating in system") {
    val engine = new Engine
    engine.addSystem(new CounterSystem)

    for (_ <- 0 until 20) {
      val entity = new Entity
      entity.add(new CounterComponent)
      engine.addEntity(entity)
    }

    val entities = engine.getEntitiesFor(Family.all(classOf[CounterComponent]).get())

    for (i <- 0 until entities.size)
      assertEquals(entities(i).getComponent(classOf[CounterComponent]).get.counter, 0)

    engine.update(deltaTime)

    for (i <- 0 until entities.size)
      assertEquals(entities(i).getComponent(classOf[CounterComponent]).get.counter, 1)
  }

  test("family listener") {
    val engine = new Engine

    val listenerA = new EntityListenerMock
    val listenerB = new EntityListenerMock

    val familyA = Family.all(classOf[ComponentA]).get()
    val familyB = Family.all(classOf[ComponentB]).get()

    engine.addEntityListener(familyA, listenerA)
    engine.addEntityListener(familyB, listenerB)

    val entity1 = new Entity
    engine.addEntity(entity1)

    assertEquals(listenerA.addedCount, 0)
    assertEquals(listenerB.addedCount, 0)

    entity1.add(new ComponentA)
    assertEquals(listenerA.addedCount, 1)
    assertEquals(listenerB.addedCount, 0)

    val entity2 = new Entity
    engine.addEntity(entity2)
    entity2.add(new ComponentB)
    assertEquals(listenerA.addedCount, 1)
    assertEquals(listenerB.addedCount, 1)

    entity1.remove(classOf[ComponentA])
    assertEquals(listenerA.removedCount, 1)
    assertEquals(listenerB.removedCount, 0)

    engine.removeEntity(entity2)
    assertEquals(listenerA.removedCount, 1)
    assertEquals(listenerB.removedCount, 1)
  }

  test("addEntityListener receives add/remove events") {
    val engine   = new Engine
    val listener = new EntityListenerMock

    engine.addEntityListener(listener)

    val e1 = new Entity
    val e2 = new Entity
    engine.addEntity(e1)
    engine.addEntity(e2)

    assertEquals(listener.addedCount, 2)

    engine.removeEntity(e1)
    assertEquals(listener.removedCount, 1)

    engine.removeAllEntities()
    assertEquals(listener.removedCount, 2)
  }

  test("removeAllEntities clears everything") {
    val engine = new Engine

    for (_ <- 0 until 10)
      engine.addEntity(new Entity)

    assertEquals(engine.getEntities.size, 10)

    engine.removeAllEntities()
    assertEquals(engine.getEntities.size, 0)
  }

  test("getEntities returns live list") {
    val numEntities = 10
    val engine      = new Engine
    val added       = ArrayBuffer[Entity]()

    for (_ <- 0 until numEntities) {
      val entity = new Entity
      added += entity
      engine.addEntity(entity)
    }

    val engineEntities = engine.getEntities
    assertEquals(engineEntities.size, added.size)

    for (i <- 0 until numEntities)
      assert(engineEntities(i) eq added(i))

    engine.removeAllEntities()
    assertEquals(engineEntities.size, 0)
  }

  test("addEntity twice throws") {
    val engine = new Engine
    val entity = new Entity
    engine.addEntity(entity)
    intercept[IllegalArgumentException] {
      engine.addEntity(entity)
    }
  }

  test("nested update throws IllegalStateException") {
    val engine = new Engine

    val outerEngine = engine
    outerEngine.addSystem(
      new EntitySystem() {
        private var duringCallback = false

        override def update(deltaTime: Float): Unit =
          if (!duringCallback) {
            duringCallback = true
            outerEngine.update(deltaTime)
            duringCallback = false
          }
      }
    )

    intercept[IllegalStateException] {
      engine.update(deltaTime)
    }
  }

  test("system update that throws does not leave engine in updating state") {
    val engine = new Engine

    val system = new EntitySystem() {
      override def update(deltaTime: Float): Unit =
        throw new RuntimeException("throwing")
    }

    engine.addSystem(system)

    intercept[RuntimeException] {
      engine.update(0.0f)
    }

    engine.removeSystem(system)

    // Should not throw -- engine should no longer be in "updating" state
    engine.update(0.0f)
  }

  test("createEntity returns new entity") {
    val engine = new Engine
    val entity = engine.createEntity()
    assert(entity != null)
  }

  test("createComponent throws by default in base Engine") {
    val engine = new Engine
    intercept[UnsupportedOperationException] {
      engine.createComponent(classOf[EngineTestComponentD])
    }
  }

  test("createComponent works via PooledEngine with factory") {
    val engine = new PooledEngine
    engine.registerComponentFactory(classOf[EngineTestComponentD], () => new EngineTestComponentD)
    val comp = engine.createComponent(classOf[EngineTestComponentD])
    assert(comp.isDefined)
  }

  test("deferred operations during update") {
    val eng = new Engine

    // System that adds and removes entities during update
    eng.addSystem(new EntitySystem() {
      override def update(deltaTime: Float): Unit = {
        val e = new Entity
        e.add(new ComponentA)
        eng.addEntity(e)
      }
    })

    eng.update(deltaTime)
    // The deferred add should have been processed
    assertEquals(eng.getEntities.size, 1)
  }

  test("addComponent inside listener") {
    val engine = new Engine

    val listenerA = new AddComponentBEntityListenerMock
    val listenerB = new EntityListenerMock

    engine.addEntityListener(Family.all(classOf[ComponentA]).get(), listenerA)
    engine.addEntityListener(Family.all(classOf[ComponentB]).get(), listenerB)

    val entity1 = new Entity
    entity1.add(new ComponentA)
    engine.addEntity(entity1)

    assertEquals(listenerA.addedCount, 1)
    assert(entity1.getComponent(classOf[ComponentB]).isDefined)
    assertEquals(listenerB.addedCount, 1)
  }

  test("create many entities without stack overflow") {
    val engine = new Engine
    engine.addSystem(new CounterSystem)

    for (_ <- 0 until 15000) {
      val e = new Entity
      e.add(new CounterComponent)
      engine.addEntity(e)
    }

    engine.update(0)
  }

  test("removeAllEntities for specific family") {
    val engine  = new Engine
    val familyA = Family.all(classOf[ComponentA]).get()

    val entity1 = new Entity
    entity1.add(new ComponentA)
    engine.addEntity(entity1)

    val entity2 = new Entity
    entity2.add(new ComponentB)
    engine.addEntity(entity2)

    assertEquals(engine.getEntities.size, 2)

    engine.removeAllEntities(familyA)

    // Only entity with ComponentA removed
    assertEquals(engine.getEntities.size, 1)
  }

  // ── EntityListenerTests: priority ordering ────────────────────────────

  test("entityListenerPriority: listeners called in priority order") {
    val callOrder = ArrayBuffer[String]()

    val a = new EntityListener {
      override def entityAdded(entity:   Entity): Unit = callOrder += "a-added"
      override def entityRemoved(entity: Entity): Unit = callOrder += "a-removed"
    }
    val b = new EntityListener {
      override def entityAdded(entity:   Entity): Unit = callOrder += "b-added"
      override def entityRemoved(entity: Entity): Unit = callOrder += "b-removed"
    }
    val c = new EntityListener {
      override def entityAdded(entity:   Entity): Unit = callOrder += "c-added"
      override def entityRemoved(entity: Entity): Unit = callOrder += "c-removed"
    }

    val entity = new Entity
    val engine = new Engine

    // b has priority -3, c has default priority 0, a has priority -4
    engine.addEntityListener(-3, b)
    engine.addEntityListener(c)
    engine.addEntityListener(-4, a)
    assert(callOrder.isEmpty)

    // Add: expect order a(-4), b(-3), c(0)
    engine.addEntity(entity)
    assertEquals(callOrder.toList, List("a-added", "b-added", "c-added"))
    callOrder.clear()

    // Remove: expect same order
    engine.removeEntity(entity)
    assertEquals(callOrder.toList, List("a-removed", "b-removed", "c-removed"))
    callOrder.clear()

    // Remove b, add entity again: expect a, c
    engine.removeEntityListener(b)
    engine.addEntity(entity)
    assertEquals(callOrder.toList, List("a-added", "c-added"))
    callOrder.clear()

    // Re-add b with priority 4 (after c), remove entity: expect a(-4), c(0), b(4)
    engine.addEntityListener(4, b)
    engine.removeEntity(entity)
    assertEquals(callOrder.toList, List("a-removed", "c-removed", "b-removed"))
  }

  test("familyListenerPriority: family-scoped listeners called in priority order") {
    val callOrder = ArrayBuffer[String]()

    val a = new EntityListener {
      override def entityAdded(entity:   Entity): Unit = callOrder += "a-added"
      override def entityRemoved(entity: Entity): Unit = callOrder += "a-removed"
    }
    val b = new EntityListener {
      override def entityAdded(entity:   Entity): Unit = callOrder += "b-added"
      override def entityRemoved(entity: Entity): Unit = callOrder += "b-removed"
    }

    val engine = new Engine
    // b listens for ComponentB family with priority -2
    engine.addEntityListener(Family.all(classOf[ComponentB]).get(), -2, b)
    // a listens for ComponentA family with priority -3
    engine.addEntityListener(Family.all(classOf[ComponentA]).get(), -3, a)
    assert(callOrder.isEmpty)

    val entity = new Entity
    entity.add(new ComponentA)
    entity.add(new ComponentB)

    engine.addEntity(entity)
    assertEquals(callOrder.toList, List("a-added", "b-added"))
    callOrder.clear()

    entity.remove(classOf[ComponentB])
    assertEquals(callOrder.toList, List("b-removed"))
    callOrder.clear()

    entity.remove(classOf[ComponentA])
    assertEquals(callOrder.toList, List("a-removed"))
    callOrder.clear()

    entity.add(new ComponentA)
    assertEquals(callOrder.toList, List("a-added"))
    callOrder.clear()

    entity.add(new ComponentB)
    assertEquals(callOrder.toList, List("b-added"))
  }

  test("componentHandlingInListeners: add/remove components inside entity listeners") {
    val engine = new Engine

    var addingComponentACalled   = false
    var removingComponentACalled = false
    var addingComponentBCalled   = false
    var removingComponentBCalled = false

    engine.addEntityListener(
      new EntityListener {
        override def entityAdded(entity: Entity): Unit = {
          addingComponentACalled = true
          entity.add(new ComponentA)
        }
        override def entityRemoved(entity: Entity): Unit = {
          removingComponentACalled = true
          entity.remove(classOf[ComponentA])
        }
      }
    )

    engine.addEntityListener(
      new EntityListener {
        override def entityAdded(entity: Entity): Unit = {
          addingComponentBCalled = true
          entity.add(new ComponentB)
        }
        override def entityRemoved(entity: Entity): Unit = {
          removingComponentBCalled = true
          entity.remove(classOf[ComponentB])
        }
      }
    )

    engine.update(0)
    val e = new Entity
    engine.addEntity(e)
    engine.update(0)
    engine.removeEntity(e)
    engine.update(0)

    assert(addingComponentACalled)
    assert(removingComponentACalled)
    assert(addingComponentBCalled)
    assert(removingComponentBCalled)
  }

  // ── EntityListenerTests: family-scoped add/remove inside listeners ────

  test("addEntityListenerFamilyRemove: add entity inside family listener on remove") {
    val engine = new Engine

    val e = new Entity
    e.add(new ComponentA)
    engine.addEntity(e)

    val family = Family.all(classOf[ComponentA]).get()
    engine.addEntityListener(
      family,
      new EntityListener {
        override def entityRemoved(entity: Entity): Unit =
          engine.addEntity(new Entity)
        override def entityAdded(entity: Entity): Unit = {}
      }
    )

    engine.removeEntity(e)
    // Should not throw - deferred add inside listener callback
  }

  test("addEntityListenerFamilyAdd: add entity inside family listener on add") {
    val engine = new Engine

    val e = new Entity
    e.add(new ComponentA)

    val family = Family.all(classOf[ComponentA]).get()
    engine.addEntityListener(
      family,
      new EntityListener {
        override def entityRemoved(entity: Entity): Unit = {}
        override def entityAdded(entity: Entity):   Unit =
          engine.addEntity(new Entity)
      }
    )

    engine.addEntity(e)
    // Should not throw
  }

  test("addEntityListenerNoFamilyRemove: add entity inside no-family listener on remove") {
    val engine = new Engine

    val e = new Entity
    e.add(new ComponentA)
    engine.addEntity(e)

    val family = Family.all(classOf[ComponentA]).get()
    engine.addEntityListener(
      new EntityListener {
        override def entityRemoved(entity: Entity): Unit =
          if (family.matches(entity)) engine.addEntity(new Entity)
        override def entityAdded(entity: Entity): Unit = {}
      }
    )

    engine.removeEntity(e)
    // Should not throw
  }

  test("addEntityListenerNoFamilyAdd: add entity inside no-family listener on add") {
    val engine = new Engine

    val e = new Entity
    e.add(new ComponentA)

    val family = Family.all(classOf[ComponentA]).get()
    engine.addEntityListener(
      new EntityListener {
        override def entityRemoved(entity: Entity): Unit = {}
        override def entityAdded(entity: Entity):   Unit =
          if (family.matches(entity)) engine.addEntity(new Entity)
      }
    )

    engine.addEntity(e)
    // Should not throw
  }

  // ── EngineTests: removeEntityBeforeAddingAndWhileEngineIsUpdating ─────

  test("removeEntityBeforeAddingAndWhileEngineIsUpdating") {
    // Test for issue #306 in original Ashley
    val eng = new Engine
    eng.addSystem(
      new EntitySystem() {
        override def update(deltaTime: Float): Unit = {
          val entity = new Entity
          eng.removeEntity(entity)
          eng.addEntity(entity)
          eng.removeEntity(entity)
        }
      }
    )
    eng.update(deltaTime)
    assertEquals(eng.getEntities.size, 0)
  }

  // ── FamilyManagerTests: entityListenerThrows ──────────────────────────

  test("entityListenerThrows resets notifying flag") {
    val engine = new Engine

    engine.addEntityListener(
      Family.all().get(),
      new EntityListener {
        override def entityAdded(entity: Entity): Unit =
          throw new RuntimeException("throwing")
        override def entityRemoved(entity: Entity): Unit =
          throw new RuntimeException("throwing")
      }
    )

    val entity = new Entity
    intercept[RuntimeException] {
      engine.addEntity(entity)
    }
    // After the exception, the engine should recover and be usable
    // (notifying flag should have been reset)
  }
}
