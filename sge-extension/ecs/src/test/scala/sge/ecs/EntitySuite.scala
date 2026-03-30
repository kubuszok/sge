package sge
package ecs

import sge.ecs.signals.{Listener, Signal}
import sge.utils.Nullable

class EntitySuite extends munit.FunSuite {

  private class ComponentA extends Component
  private class ComponentB extends Component

  private class EntityListenerMock extends Listener[Entity] {
    var counter: Int = 0

    override def receive(signal: Signal[Entity], obj: Entity): Unit = {
      counter += 1
      assert(signal != null)
      assert(obj != null)
    }
  }

  private val am = ComponentMapper.getFor(classOf[ComponentA])
  private val bm = ComponentMapper.getFor(classOf[ComponentB])

  test("addAndReturn returns the component") {
    val entity = new Entity
    val compA = new ComponentA
    val compB = new ComponentB

    assert(entity.addAndReturn(compA) eq compA)
    assert(entity.addAndReturn(compB) eq compB)
    assertEquals(entity.getComponents.size, 2)
  }

  test("no components initially") {
    val entity = new Entity
    assertEquals(entity.getComponents.size, 0)
    assert(entity.getComponentBits.isEmpty)
    assert(am.get(entity).isEmpty)
    assert(bm.get(entity).isEmpty)
    assert(!am.has(entity))
    assert(!bm.has(entity))
  }

  test("add and remove component") {
    val entity = new Entity

    entity.add(new ComponentA)
    assertEquals(entity.getComponents.size, 1)

    val componentBits = entity.getComponentBits
    val indexA = ComponentType.getIndexFor(classOf[ComponentA])

    assert(componentBits.contains(indexA))
    assert(am.get(entity).isDefined)
    assert(bm.get(entity).isEmpty)
    assert(am.has(entity))
    assert(!bm.has(entity))

    entity.remove(classOf[ComponentA])

    assertEquals(entity.getComponents.size, 0)
    assert(!componentBits.contains(indexA))
    assert(am.get(entity).isEmpty)
    assert(!am.has(entity))
  }

  test("add replaces existing same-type component") {
    val entity = new Entity
    val a1 = new ComponentA
    val a2 = new ComponentA

    entity.add(a1)
    entity.add(a2)

    assertEquals(entity.getComponents.size, 1)
    assert(am.has(entity))
    assert(am.get(entity).get eq a2)
  }

  test("add and removeAll") {
    val entity = new Entity

    entity.add(new ComponentA)
    entity.add(new ComponentB)
    assertEquals(entity.getComponents.size, 2)

    entity.removeAll()
    assertEquals(entity.getComponents.size, 0)
    assert(am.get(entity).isEmpty)
    assert(bm.get(entity).isEmpty)
  }

  test("componentAdded signal fires") {
    val addedListener = new EntityListenerMock
    val removedListener = new EntityListenerMock

    val entity = new Entity
    entity.componentAdded.add(addedListener)
    entity.componentRemoved.add(removedListener)

    assertEquals(addedListener.counter, 0)
    assertEquals(removedListener.counter, 0)

    entity.add(new ComponentA)
    assertEquals(addedListener.counter, 1)
    assertEquals(removedListener.counter, 0)

    entity.remove(classOf[ComponentA])
    assertEquals(addedListener.counter, 1)
    assertEquals(removedListener.counter, 1)

    entity.add(new ComponentB)
    assertEquals(addedListener.counter, 2)

    entity.remove(classOf[ComponentB])
    assertEquals(removedListener.counter, 2)
  }

  test("getComponent by class") {
    val compA = new ComponentA
    val compB = new ComponentB

    val entity = new Entity
    entity.add(compA).add(compB)

    val retA = entity.getComponent(classOf[ComponentA])
    val retB = entity.getComponent(classOf[ComponentB])

    assert(retA.isDefined)
    assert(retB.isDefined)
    assert(retA.get eq compA)
    assert(retB.get eq compB)
  }

  test("getComponent returns empty for absent component") {
    val entity = new Entity
    val result = entity.getComponent(classOf[ComponentA])
    assert(result.isEmpty)
  }

  test("hasComponent via ComponentType") {
    val entity = new Entity
    val ct = ComponentType.getFor(classOf[ComponentA])

    assert(!entity.hasComponent(ct))
    entity.add(new ComponentA)
    assert(entity.hasComponent(ct))
  }

  test("flags field") {
    val entity = new Entity
    assertEquals(entity.flags, 0)
    entity.flags = 42
    assertEquals(entity.flags, 42)
  }

  test("chaining add returns entity") {
    val entity = new Entity
    val result = entity.add(new ComponentA).add(new ComponentB)
    assert(result eq entity)
    assertEquals(entity.getComponents.size, 2)
  }

  test("remove non-existing component returns empty") {
    val entity = new Entity
    val result = entity.remove(classOf[ComponentA])
    assert(result.isEmpty)
  }
}
