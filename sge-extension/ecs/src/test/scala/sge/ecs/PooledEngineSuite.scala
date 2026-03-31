package sge
package ecs

import scala.collection.mutable.ArrayBuffer

import sge.ecs.signals.{ Listener, Signal }
import sge.ecs.utils.ImmutableArray
import sge.utils.{ Nullable, Pool }

// Top-level classes with public no-arg constructors for reflection-based createComponent
class PooledPositionComponent extends Component {
  var x: Float = 0.0f
  var y: Float = 0.0f
}

class PooledComponentA extends Component

class PoolableComponent extends Component with Pool.Poolable {
  var wasReset:         Boolean = true
  override def reset(): Unit    =
    wasReset = true
}

class PooledComponentSpy extends Component with Pool.Poolable {
  var recycled:         Boolean = false
  override def reset(): Unit    =
    recycled = true
}

class PooledEngineSuite extends munit.FunSuite {

  private val deltaTime: Float = 0.16f

  /** Creates a PooledEngine with all test component factories pre-registered. */
  private def newPooledEngine(
    entityPoolInitialSize:    Int = 10,
    entityPoolMaxSize:        Int = 100,
    componentPoolInitialSize: Int = 10,
    componentPoolMaxSize:     Int = 100
  ): PooledEngine = {
    val e = new PooledEngine(entityPoolInitialSize, entityPoolMaxSize, componentPoolInitialSize, componentPoolMaxSize)
    e.registerComponentFactory(classOf[PooledPositionComponent], () => new PooledPositionComponent)
    e.registerComponentFactory(classOf[PooledComponentA], () => new PooledComponentA)
    e.registerComponentFactory(classOf[PoolableComponent], () => new PoolableComponent)
    e.registerComponentFactory(classOf[PooledComponentSpy], () => new PooledComponentSpy)
    e
  }

  private class MyPositionListener extends EntityListener {
    val positionMapper: ComponentMapper[PooledPositionComponent] = ComponentMapper.getFor(classOf[PooledPositionComponent])
    var counter:        Int                                      = 0

    override def entityAdded(entity: Entity): Unit = {}

    override def entityRemoved(entity: Entity): Unit = {
      val position = positionMapper.get(entity)
      assert(position.isDefined, "Position should still be accessible during removal")
    }
  }

  private class CombinedSystem extends EntitySystem() {
    var allEntities:     ImmutableArray[Entity] = scala.compiletime.uninitialized
    private var counter: Int                    = 0

    override def addedToEngine(engine: Engine): Unit =
      allEntities = engine.getEntitiesFor(Family.all(classOf[PooledPositionComponent]).get())

    override def update(deltaTime: Float): Unit = {
      if (counter >= 6 && counter <= 8) {
        engine.get.removeEntity(allEntities(2))
      }
      counter += 1
    }
  }

  private class ComponentCounterListener extends Listener[Entity] {
    var totalCalls:                                            Int  = 0
    override def receive(signal: Signal[Entity], obj: Entity): Unit =
      totalCalls += 1
  }

  test("createEntity returns pooled entity") {
    val engine = newPooledEngine()
    val entity = engine.createEntity()
    assert(entity != null)
  }

  test("createComponent creates component via factory") {
    val engine = newPooledEngine()
    val comp   = engine.createComponent(classOf[PooledComponentA])
    assert(comp.isDefined)
  }

  test("entity removal listener order") {
    val engine         = newPooledEngine()
    val combinedSystem = new CombinedSystem

    engine.addSystem(combinedSystem)
    engine.addEntityListener(Family.all(classOf[PooledPositionComponent]).get(), new MyPositionListener)

    for (_ <- 0 until 10) {
      val entity = engine.createEntity()
      entity.add(engine.createComponent(classOf[PooledPositionComponent]).get)
      engine.addEntity(entity)
    }

    assertEquals(combinedSystem.allEntities.size, 10)

    for (_ <- 0 until 10)
      engine.update(deltaTime)

    engine.removeAllEntities()
  }

  test("recycleEntity: removed entities are reused") {
    val numEntities = 5
    val engine      = newPooledEngine(entityPoolInitialSize = numEntities, entityPoolMaxSize = 100, componentPoolInitialSize = 0, componentPoolMaxSize = 100)
    val entities    = ArrayBuffer[Entity]()

    for (_ <- 0 until numEntities) {
      val entity = engine.createEntity()
      assert(!entity.removing)
      assertEquals(entity.flags, 0)
      engine.addEntity(entity)
      entities += entity
      entity.flags = 1
    }

    for (entity <- entities) {
      engine.removeEntity(entity)
      assertEquals(entity.flags, 0)
      assert(!entity.removing)
    }

    for (_ <- 0 until numEntities) {
      val entity = engine.createEntity()
      assertEquals(entity.flags, 0)
      assert(!entity.removing)
      assert(entities.contains(entity))
    }
  }

  test("remove entity twice does not crash") {
    val engine = newPooledEngine()

    for (_ <- 0 until 10) {
      val entities = ArrayBuffer[Entity]()

      for (_ <- 0 until 10) {
        val entity = engine.createEntity()
        engine.addEntity(entity)
        assertEquals(entity.flags, 0)
        entity.flags = 1
        entities += entity
      }

      for (entity <- entities) {
        engine.removeEntity(entity)
        engine.removeEntity(entity)
      }
    }
  }

  test("recycleComponent: components are pooled and reset") {
    val maxEntities   = 10
    val maxComponents = 10
    val engine        = newPooledEngine(
      entityPoolInitialSize = maxEntities,
      entityPoolMaxSize = maxEntities,
      componentPoolInitialSize = maxComponents,
      componentPoolMaxSize = maxComponents
    )

    for (_ <- 0 until maxComponents) {
      val e = engine.createEntity()
      val c = engine.createComponent(classOf[PooledComponentSpy]).get
      assertEquals(c.recycled, false)
      e.add(c)
      engine.addEntity(e)
    }

    engine.removeAllEntities()

    for (_ <- 0 until maxComponents) {
      val e = engine.createEntity()
      val c = engine.createComponent(classOf[PooledComponentSpy]).get
      assertEquals(c.recycled, true)
      e.add(c)
    }

    engine.removeAllEntities()
  }

  test("clearPools") {
    val engine = newPooledEngine()

    for (_ <- 0 until 5) {
      val entity = engine.createEntity()
      engine.addEntity(entity)
    }

    engine.removeAllEntities()
    engine.clearPools()

    // After clearing, new entities should be fresh (not recycled from pool)
    // This is a smoke test -- just verify it doesn't crash
    val entity = engine.createEntity()
    assert(entity != null)
  }

  test("resetEntity correctly: flags, components, familyBits cleared") {
    val engine          = newPooledEngine()
    val addedListener   = new ComponentCounterListener
    val removedListener = new ComponentCounterListener

    val familyEntities = engine.getEntitiesFor(Family.all(classOf[PooledPositionComponent]).get())

    val totalEntities = 10
    val entities      = new Array[Entity](totalEntities)

    for (i <- 0 until totalEntities) {
      entities(i) = engine.createEntity()
      entities(i).flags = 5
      entities(i).componentAdded.add(addedListener)
      entities(i).componentRemoved.add(removedListener)
      entities(i).add(engine.createComponent(classOf[PooledPositionComponent]).get)
      engine.addEntity(entities(i))

      assertEquals(entities(i).getComponents.size, 1)
      assert(familyEntities.contains(entities(i)))
    }

    assertEquals(addedListener.totalCalls, totalEntities)
    assertEquals(removedListener.totalCalls, 0)

    engine.removeAllEntities()

    assertEquals(addedListener.totalCalls, totalEntities)
    assertEquals(removedListener.totalCalls, totalEntities)

    for (i <- 0 until totalEntities) {
      assert(!entities(i).removing)
      assertEquals(entities(i).flags, 0)
      assertEquals(entities(i).getComponents.size, 0)
      assert(!familyEntities.contains(entities(i)))

      // After reset, listeners should have been cleared -- dispatch should not increment counters
      entities(i).componentAdded.dispatch(entities(i))
      entities(i).componentRemoved.dispatch(entities(i))
    }

    // Counters should not have changed from dispatches on reset entities
    assertEquals(addedListener.totalCalls, totalEntities)
    assertEquals(removedListener.totalCalls, totalEntities)
  }

  test("addSameComponent should reset and return old component to pool") {
    val engine         = newPooledEngine()
    val poolableMapper = ComponentMapper.getFor(classOf[PoolableComponent])

    val component1 = engine.createComponent(classOf[PoolableComponent]).get
    component1.wasReset = false
    val component2 = engine.createComponent(classOf[PoolableComponent]).get
    component2.wasReset = false

    val entity = engine.createEntity()
    entity.add(component1)
    entity.add(component2)

    assertEquals(entity.getComponents.size, 1)
    assert(poolableMapper.has(entity))
    assert(!(poolableMapper.get(entity).get eq component1))
    assert(poolableMapper.get(entity).get eq component2)

    // component1 should have been reset when returned to pool
    assert(component1.wasReset)
  }

  test("removeComponent returns it to the pool exactly once") {
    val engine = newPooledEngine()

    val removedComponent = engine.createComponent(classOf[PoolableComponent]).get

    val entity = engine.createEntity()
    entity.add(removedComponent)
    entity.remove(classOf[PoolableComponent])

    val newComponent1 = engine.createComponent(classOf[PoolableComponent]).get
    val newComponent2 = engine.createComponent(classOf[PoolableComponent]).get

    // The removed component should be reused as one of these, but both should be distinct
    assert(!(newComponent1 eq newComponent2))
  }
}
