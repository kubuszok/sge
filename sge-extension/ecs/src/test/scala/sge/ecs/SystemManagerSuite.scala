package sge
package ecs

import scala.collection.mutable.ArrayBuffer

class SystemManagerSuite extends munit.FunSuite {

  private class SystemListenerMock extends SystemManager.SystemListener {
    val addedSystems:   ArrayBuffer[EntitySystem] = ArrayBuffer.empty
    val removedSystems: ArrayBuffer[EntitySystem] = ArrayBuffer.empty

    override def systemAdded(system: EntitySystem): Unit =
      addedSystems += system

    override def systemRemoved(system: EntitySystem): Unit =
      removedSystems += system
  }

  private class SystemA(p: Int = 0) extends EntitySystem(p)
  private class SystemB(p: Int = 0) extends EntitySystem(p)
  private class SystemC(p: Int = 0) extends EntitySystem(p)

  test("addSystem notifies listener") {
    val listener = new SystemListenerMock
    val manager  = new SystemManager(listener)
    val system   = new SystemA

    manager.addSystem(system)
    assertEquals(listener.addedSystems.size, 1)
    assert(listener.addedSystems.head eq system)
  }

  test("removeSystem notifies listener") {
    val listener = new SystemListenerMock
    val manager  = new SystemManager(listener)
    val system   = new SystemA

    manager.addSystem(system)
    manager.removeSystem(system)
    assertEquals(listener.removedSystems.size, 1)
    assert(listener.removedSystems.head eq system)
  }

  test("removeSystem not in manager does nothing") {
    val listener = new SystemListenerMock
    val manager  = new SystemManager(listener)
    val system   = new SystemA

    manager.removeSystem(system)
    assert(listener.removedSystems.isEmpty)
  }

  test("getSystems returns all added systems") {
    val listener = new SystemListenerMock
    val manager  = new SystemManager(listener)
    val a        = new SystemA
    val b        = new SystemB

    assertEquals(manager.getSystems.size, 0)

    manager.addSystem(a)
    manager.addSystem(b)
    assertEquals(manager.getSystems.size, 2)
  }

  test("getSystem returns system by class") {
    val listener = new SystemListenerMock
    val manager  = new SystemManager(listener)
    val a        = new SystemA
    val b        = new SystemB

    manager.addSystem(a)
    manager.addSystem(b)

    assert(manager.getSystem(classOf[SystemA]).isDefined)
    assert(manager.getSystem(classOf[SystemA]).get eq a)
    assert(manager.getSystem(classOf[SystemB]).isDefined)
    assert(manager.getSystem(classOf[SystemB]).get eq b)
    assert(manager.getSystem(classOf[SystemC]).isEmpty)
  }

  test("addSystem replaces existing system of same class") {
    val listener = new SystemListenerMock
    val manager  = new SystemManager(listener)
    val a1       = new SystemA
    val a2       = new SystemA

    manager.addSystem(a1)
    assertEquals(manager.getSystems.size, 1)
    assert(manager.getSystem(classOf[SystemA]).get eq a1)

    manager.addSystem(a2)
    assertEquals(manager.getSystems.size, 1)
    assert(manager.getSystem(classOf[SystemA]).get eq a2)

    // Old system should have been removed and new one added
    assertEquals(listener.removedSystems.size, 1)
    assert(listener.removedSystems.head eq a1)
    assertEquals(listener.addedSystems.size, 2)
    assert(listener.addedSystems(1) eq a2)
  }

  test("systems are sorted by priority") {
    val listener = new SystemListenerMock
    val manager  = new SystemManager(listener)

    val s1 = new SystemA(10)
    val s2 = new SystemB(5)
    val s3 = new SystemC(1)

    manager.addSystem(s1)
    manager.addSystem(s2)
    manager.addSystem(s3)

    val systems = manager.getSystems
    assertEquals(systems.size, 3)

    // Should be sorted by priority: 1, 5, 10
    assertEquals(systems(0).priority, 1)
    assertEquals(systems(1).priority, 5)
    assertEquals(systems(2).priority, 10)
  }

  test("removeAllSystems removes everything") {
    val listener = new SystemListenerMock
    val manager  = new SystemManager(listener)

    manager.addSystem(new SystemA)
    manager.addSystem(new SystemB)
    manager.addSystem(new SystemC)

    assertEquals(manager.getSystems.size, 3)

    manager.removeAllSystems()
    assertEquals(manager.getSystems.size, 0)
    assertEquals(listener.removedSystems.size, 3)
  }

  test("removeAllSystems on empty manager is no-op") {
    val listener = new SystemListenerMock
    val manager  = new SystemManager(listener)

    manager.removeAllSystems()
    assertEquals(manager.getSystems.size, 0)
    assert(listener.removedSystems.isEmpty)
  }

  test("priority ordering is maintained after multiple add/remove") {
    val listener = new SystemListenerMock
    val manager  = new SystemManager(listener)

    val s1 = new SystemA(3)
    val s2 = new SystemB(1)
    val s3 = new SystemC(2)

    manager.addSystem(s1)
    manager.addSystem(s2)
    manager.addSystem(s3)

    // Order: B(1), C(2), A(3)
    assertEquals(manager.getSystems(0).priority, 1)
    assertEquals(manager.getSystems(1).priority, 2)
    assertEquals(manager.getSystems(2).priority, 3)

    // Remove middle priority
    manager.removeSystem(s3)
    assertEquals(manager.getSystems.size, 2)
    assertEquals(manager.getSystems(0).priority, 1)
    assertEquals(manager.getSystems(1).priority, 3)

    // Add new system with middle priority
    val s4 = new SystemC(2)
    manager.addSystem(s4)
    assertEquals(manager.getSystems.size, 3)
    assertEquals(manager.getSystems(0).priority, 1)
    assertEquals(manager.getSystems(1).priority, 2)
    assertEquals(manager.getSystems(2).priority, 3)
  }
}
