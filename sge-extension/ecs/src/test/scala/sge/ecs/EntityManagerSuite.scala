package sge
package ecs

class EntityManagerSuite extends munit.FunSuite {

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

  test("addAndRemoveEntity via EntityManager") {
    val listener = new EntityListenerMock
    val manager  = new EntityManager(listener)

    val entity1 = new Entity
    manager.addEntity(entity1)
    assertEquals(listener.addedCount, 1)

    val entity2 = new Entity
    manager.addEntity(entity2)
    assertEquals(listener.addedCount, 2)

    manager.removeAllEntities()
    assertEquals(listener.removedCount, 2)
  }

  test("getEntities via EntityManager") {
    val numEntities = 10
    val listener    = new EntityListenerMock
    val manager     = new EntityManager(listener)

    val added = scala.collection.mutable.ArrayBuffer[Entity]()

    for (_ <- 0 until numEntities) {
      val entity = new Entity
      added += entity
      manager.addEntity(entity)
    }

    val engineEntities = manager.getEntities
    assertEquals(engineEntities.size, added.size)

    for (i <- 0 until numEntities)
      assert(engineEntities(i) eq added(i))

    manager.removeAllEntities()
    assertEquals(engineEntities.size, 0)
  }

  test("addEntityTwice immediate throws") {
    val listener = new EntityListenerMock
    val manager  = new EntityManager(listener)
    val entity   = new Entity
    manager.addEntity(entity)
    intercept[IllegalArgumentException] {
      manager.addEntity(entity)
    }
  }

  test("addEntityTwice with delayed=false throws") {
    val listener = new EntityListenerMock
    val manager  = new EntityManager(listener)
    val entity   = new Entity
    manager.addEntity(entity, delayed = false)
    intercept[IllegalArgumentException] {
      manager.addEntity(entity, delayed = false)
    }
  }

  test("addEntityTwiceDelayed throws on process") {
    val listener = new EntityListenerMock
    val manager  = new EntityManager(listener)

    val entity = new Entity
    manager.addEntity(entity, delayed = true)
    manager.addEntity(entity, delayed = true)
    intercept[IllegalArgumentException] {
      manager.processPendingOperations()
    }
  }

  test("delayedOperationsOrder: removeAll then add") {
    val listener = new EntityListenerMock
    val manager  = new EntityManager(listener)

    val entityA = new Entity
    val entityB = new Entity
    manager.addEntity(entityA)
    manager.addEntity(entityB)

    assertEquals(manager.getEntities.size, 2)

    val entityC = new Entity
    val entityD = new Entity
    manager.removeAllEntities(delayed = true)
    manager.addEntity(entityC, delayed = true)
    manager.addEntity(entityD, delayed = true)
    manager.processPendingOperations()

    assertEquals(manager.getEntities.size, 2)
    assert(manager.getEntities.contains(entityC))
    assert(manager.getEntities.contains(entityD))
  }

  test("removeAndAddEntityDelayed") {
    val listener = new EntityListenerMock
    val manager  = new EntityManager(listener)

    val entity = new Entity
    manager.addEntity(entity, delayed = false) // immediate
    assertEquals(manager.getEntities.size, 1)

    manager.removeEntity(entity, delayed = true) // delayed
    assertEquals(manager.getEntities.size, 1)

    manager.addEntity(entity, delayed = true) // delayed
    assertEquals(manager.getEntities.size, 1)

    manager.processPendingOperations()
    assertEquals(manager.getEntities.size, 1)
  }

  test("removeAllAndAddEntityDelayed") {
    val listener = new EntityListenerMock
    val manager  = new EntityManager(listener)

    val entity = new Entity
    manager.addEntity(entity, delayed = false) // immediate
    assertEquals(manager.getEntities.size, 1)

    manager.removeAllEntities(delayed = true) // delayed
    assertEquals(manager.getEntities.size, 1)

    manager.addEntity(entity, delayed = true) // delayed
    assertEquals(manager.getEntities.size, 1)

    manager.processPendingOperations()
    assertEquals(manager.getEntities.size, 1)
  }
}
