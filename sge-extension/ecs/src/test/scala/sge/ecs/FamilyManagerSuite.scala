package sge
package ecs

import scala.collection.mutable.ArrayBuffer

class FamilyManagerSuite extends munit.FunSuite {

  private class ComponentA extends Component
  private class ComponentB extends Component
  private class EntityListenerMock extends EntityListener {
    var addedCount:   Int = 0
    var removedCount: Int = 0
    val addedEntities:   ArrayBuffer[Entity] = ArrayBuffer.empty
    val removedEntities: ArrayBuffer[Entity] = ArrayBuffer.empty

    override def entityAdded(entity: Entity): Unit = {
      addedCount += 1
      addedEntities += entity
    }

    override def entityRemoved(entity: Entity): Unit = {
      removedCount += 1
      removedEntities += entity
    }
  }

  /** Helper to set up a FamilyManager with entities that can trigger membership updates. */
  private def createManagerWithEntities(count: Int): (FamilyManager, ArrayBuffer[Entity]) = {
    val entities = ArrayBuffer.empty[Entity]
    for (_ <- 0 until count)
      entities += new Entity
    val immutable = new utils.ImmutableArray[Entity](entities)
    val manager   = new FamilyManager(immutable)
    (manager, entities)
  }

  test("getEntitiesFor returns empty for new family") {
    val (manager, _) = createManagerWithEntities(0)
    val family       = Family.all(classOf[ComponentA]).get()
    val result       = manager.getEntitiesFor(family)
    assertEquals(result.size, 0)
  }

  test("getEntitiesFor returns same instance for same family") {
    val (manager, _) = createManagerWithEntities(0)
    val family       = Family.all(classOf[ComponentA]).get()
    val r1           = manager.getEntitiesFor(family)
    val r2           = manager.getEntitiesFor(family)
    assert(r1 eq r2)
  }

  test("updateFamilyMembership adds entity to matching family") {
    val entities  = ArrayBuffer.empty[Entity]
    val entity    = new Entity
    entities += entity
    val immutable = new utils.ImmutableArray[Entity](entities)
    val manager   = new FamilyManager(immutable)

    val family   = Family.all(classOf[ComponentA]).get()
    val listener = new EntityListenerMock
    manager.addEntityListener(family, 0, listener)

    // Add component that matches the family
    entity.addInternal(new ComponentA)
    manager.updateFamilyMembership(entity)

    assertEquals(listener.addedCount, 1)
    val familyEntities = manager.getEntitiesFor(family)
    assertEquals(familyEntities.size, 1)
    assert(familyEntities.contains(entity))
  }

  test("updateFamilyMembership removes entity from family when component removed") {
    val entities  = ArrayBuffer.empty[Entity]
    val entity    = new Entity
    entities += entity
    val immutable = new utils.ImmutableArray[Entity](entities)
    val manager   = new FamilyManager(immutable)

    val family   = Family.all(classOf[ComponentA]).get()
    val listener = new EntityListenerMock
    manager.addEntityListener(family, 0, listener)

    // Add component, update membership
    entity.addInternal(new ComponentA)
    manager.updateFamilyMembership(entity)
    assertEquals(listener.addedCount, 1)

    // Remove component, update membership
    entity.removeInternal(classOf[ComponentA])
    manager.updateFamilyMembership(entity)
    assertEquals(listener.removedCount, 1)

    val familyEntities = manager.getEntitiesFor(family)
    assertEquals(familyEntities.size, 0)
  }

  test("addEntityListener with priority ordering") {
    val entities  = ArrayBuffer.empty[Entity]
    val entity    = new Entity
    entities += entity
    val immutable = new utils.ImmutableArray[Entity](entities)
    val manager   = new FamilyManager(immutable)

    val family    = Family.all(classOf[ComponentA]).get()
    val callOrder = ArrayBuffer[String]()

    val listenerLow = new EntityListener {
      override def entityAdded(entity: Entity): Unit   = callOrder += "low-added"
      override def entityRemoved(entity: Entity): Unit = callOrder += "low-removed"
    }
    val listenerHigh = new EntityListener {
      override def entityAdded(entity: Entity): Unit   = callOrder += "high-added"
      override def entityRemoved(entity: Entity): Unit = callOrder += "high-removed"
    }

    // Add high priority (5) first, then low priority (-1)
    manager.addEntityListener(family, 5, listenerHigh)
    manager.addEntityListener(family, -1, listenerLow)

    entity.addInternal(new ComponentA)
    manager.updateFamilyMembership(entity)

    // Low priority (-1) should be called before high priority (5)
    assertEquals(callOrder.toList, List("low-added", "high-added"))
  }

  test("removeEntityListener stops notifications") {
    val entities  = ArrayBuffer.empty[Entity]
    val entity    = new Entity
    entities += entity
    val immutable = new utils.ImmutableArray[Entity](entities)
    val manager   = new FamilyManager(immutable)

    val family   = Family.all(classOf[ComponentA]).get()
    val listener = new EntityListenerMock
    manager.addEntityListener(family, 0, listener)

    // First update triggers listener
    entity.addInternal(new ComponentA)
    manager.updateFamilyMembership(entity)
    assertEquals(listener.addedCount, 1)

    // Remove listener
    manager.removeEntityListener(listener)

    // Remove component and re-add; listener should not be called
    entity.removeInternal(classOf[ComponentA])
    manager.updateFamilyMembership(entity)
    assertEquals(listener.removedCount, 0)
  }

  test("multiple families track independently") {
    val entities  = ArrayBuffer.empty[Entity]
    val entity    = new Entity
    entities += entity
    val immutable = new utils.ImmutableArray[Entity](entities)
    val manager   = new FamilyManager(immutable)

    val familyA    = Family.all(classOf[ComponentA]).get()
    val familyB    = Family.all(classOf[ComponentB]).get()
    val listenerA  = new EntityListenerMock
    val listenerB  = new EntityListenerMock

    manager.addEntityListener(familyA, 0, listenerA)
    manager.addEntityListener(familyB, 0, listenerB)

    entity.addInternal(new ComponentA)
    manager.updateFamilyMembership(entity)

    assertEquals(listenerA.addedCount, 1)
    assertEquals(listenerB.addedCount, 0)

    entity.addInternal(new ComponentB)
    manager.updateFamilyMembership(entity)

    assertEquals(listenerA.addedCount, 1) // no change for A
    assertEquals(listenerB.addedCount, 1) // B matches now
  }

  test("notifying flag is set during updateFamilyMembership") {
    val entities  = ArrayBuffer.empty[Entity]
    val entity    = new Entity
    entities += entity
    val immutable = new utils.ImmutableArray[Entity](entities)
    val manager   = new FamilyManager(immutable)

    val family = Family.all(classOf[ComponentA]).get()
    var wasNotifying = false
    manager.addEntityListener(family, 0, new EntityListener {
      override def entityAdded(entity: Entity): Unit =
        wasNotifying = manager.notifying
      override def entityRemoved(entity: Entity): Unit = {}
    })

    entity.addInternal(new ComponentA)
    manager.updateFamilyMembership(entity)
    assert(wasNotifying, "Expected notifying=true during listener callback")
    assert(!manager.notifying, "Expected notifying=false after updateFamilyMembership completes")
  }

  test("notifying flag is reset even if listener throws") {
    val entities  = ArrayBuffer.empty[Entity]
    val entity    = new Entity
    entities += entity
    val immutable = new utils.ImmutableArray[Entity](entities)
    val manager   = new FamilyManager(immutable)

    val family = Family.all(classOf[ComponentA]).get()
    manager.addEntityListener(family, 0, new EntityListener {
      override def entityAdded(entity: Entity): Unit =
        throw new RuntimeException("listener error")
      override def entityRemoved(entity: Entity): Unit = {}
    })

    entity.addInternal(new ComponentA)
    intercept[RuntimeException] {
      manager.updateFamilyMembership(entity)
    }
    assert(!manager.notifying, "notifying should be reset after exception")
  }

  test("entity removing flag prevents family match") {
    val entities  = ArrayBuffer.empty[Entity]
    val entity    = new Entity
    entities += entity
    val immutable = new utils.ImmutableArray[Entity](entities)
    val manager   = new FamilyManager(immutable)

    val family   = Family.all(classOf[ComponentA]).get()
    val listener = new EntityListenerMock
    manager.addEntityListener(family, 0, listener)

    entity.addInternal(new ComponentA)
    entity.removing = true
    manager.updateFamilyMembership(entity)

    // Entity should not be added to family because removing flag is true
    assertEquals(listener.addedCount, 0)
    entity.removing = false
  }

  test("registerFamily populates existing matching entities") {
    val entities  = ArrayBuffer.empty[Entity]
    val entity1   = new Entity
    val entity2   = new Entity
    entity1.addInternal(new ComponentA)
    entity2.addInternal(new ComponentB)
    entities += entity1
    entities += entity2
    val immutable = new utils.ImmutableArray[Entity](entities)
    val manager   = new FamilyManager(immutable)

    // Register a listener for familyA -- getEntitiesFor triggers registerFamily
    val familyA  = Family.all(classOf[ComponentA]).get()
    val listener = new EntityListenerMock
    manager.addEntityListener(familyA, 0, listener)

    val result = manager.getEntitiesFor(familyA)
    assertEquals(result.size, 1)
    assert(result.contains(entity1))
    // entity2 does not have ComponentA so should not be included
    assert(!result.contains(entity2))
  }
}
