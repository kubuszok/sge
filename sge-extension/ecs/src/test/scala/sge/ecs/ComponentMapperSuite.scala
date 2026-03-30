package sge
package ecs

import sge.utils.Nullable

class ComponentMapperSuite extends munit.FunSuite {

  private class ComponentA extends Component
  private class ComponentB extends Component

  test("get returns component when present") {
    val mapper = ComponentMapper.getFor(classOf[ComponentA])
    val entity = new Entity
    val comp = new ComponentA
    entity.add(comp)

    val result = mapper.get(entity)
    assert(result.isDefined)
    assert(result.get eq comp)
  }

  test("get returns empty when absent") {
    val mapper = ComponentMapper.getFor(classOf[ComponentA])
    val entity = new Entity

    val result = mapper.get(entity)
    assert(result.isEmpty)
  }

  test("has returns true when present") {
    val mapper = ComponentMapper.getFor(classOf[ComponentA])
    val entity = new Entity
    entity.add(new ComponentA)

    assert(mapper.has(entity))
  }

  test("has returns false when absent") {
    val mapper = ComponentMapper.getFor(classOf[ComponentA])
    val entity = new Entity

    assert(!mapper.has(entity))
  }

  test("multiple mappers for different types") {
    val mapperA = ComponentMapper.getFor(classOf[ComponentA])
    val mapperB = ComponentMapper.getFor(classOf[ComponentB])

    val entity = new Entity
    entity.add(new ComponentA)

    assert(mapperA.has(entity))
    assert(!mapperB.has(entity))

    entity.add(new ComponentB)
    assert(mapperA.has(entity))
    assert(mapperB.has(entity))
  }
}
