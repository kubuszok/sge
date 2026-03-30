package sge
package ecs

class FamilySuite extends munit.FunSuite {

  private class ComponentA extends Component
  private class ComponentB extends Component
  private class ComponentC extends Component
  private class ComponentD extends Component
  private class ComponentE extends Component
  private class ComponentF extends Component

  test("valid families can be created") {
    assert(Family.all().get() != null)
    assert(Family.all(classOf[ComponentA]).get() != null)
    assert(Family.all(classOf[ComponentA], classOf[ComponentB]).get() != null)
    assert(Family.all(classOf[ComponentA], classOf[ComponentB], classOf[ComponentC]).get() != null)
  }

  test("same spec returns same Family instance (caching)") {
    val f1 = Family.all(classOf[ComponentA]).get()
    val f2 = Family.all(classOf[ComponentA]).get()
    assert(f1 eq f2)
    assertEquals(f1.index, f2.index)

    val f3 = Family.all(classOf[ComponentA], classOf[ComponentB]).get()
    val f4 = Family.all(classOf[ComponentA], classOf[ComponentB]).get()
    assert(f3 eq f4)
  }

  test("different specs return different families") {
    val f1 = Family.all(classOf[ComponentA]).get()
    val f2 = Family.all(classOf[ComponentB]).get()
    val f3 = Family.all(classOf[ComponentA], classOf[ComponentB]).get()

    assert(!(f1 eq f2))
    assert(!(f1 eq f3))
    assert(!(f2 eq f3))
    assertNotEquals(f1.index, f2.index)
    assertNotEquals(f1.index, f3.index)
  }

  test("Family.all matches entity with all components") {
    val family = Family.all(classOf[ComponentA], classOf[ComponentB]).get()

    val entity = new Entity
    entity.add(new ComponentA)
    entity.add(new ComponentB)

    assert(family.matches(entity))
  }

  test("Family.all matches entity with extra components") {
    val family = Family.all(classOf[ComponentA], classOf[ComponentB]).get()

    val entity = new Entity
    entity.add(new ComponentA)
    entity.add(new ComponentB)
    entity.add(new ComponentC)

    assert(family.matches(entity))
  }

  test("Family.all does not match entity missing a component") {
    val family = Family.all(classOf[ComponentA], classOf[ComponentC]).get()

    val entity = new Entity
    entity.add(new ComponentA)
    entity.add(new ComponentB)

    assert(!family.matches(entity))
  }

  test("entity match then mismatch on remove") {
    val family = Family.all(classOf[ComponentA], classOf[ComponentB]).get()

    val entity = new Entity
    entity.add(new ComponentA)
    entity.add(new ComponentB)
    assert(family.matches(entity))

    entity.remove(classOf[ComponentA])
    assert(!family.matches(entity))
  }

  test("entity mismatch then match on add") {
    val family = Family.all(classOf[ComponentA], classOf[ComponentB]).get()

    val entity = new Entity
    entity.add(new ComponentA)
    entity.add(new ComponentC)
    assert(!family.matches(entity))

    entity.add(new ComponentB)
    assert(family.matches(entity))
  }

  test("empty family matches any entity") {
    val family = Family.all().get()
    val entity = new Entity
    assert(family.matches(entity))
  }

  test("Family.one matches entity with any of the components") {
    val family = Family.one(classOf[ComponentA], classOf[ComponentB]).get()

    val entityA = new Entity
    entityA.add(new ComponentA)
    assert(family.matches(entityA))

    val entityB = new Entity
    entityB.add(new ComponentB)
    assert(family.matches(entityB))

    val entityC = new Entity
    entityC.add(new ComponentC)
    assert(!family.matches(entityC))
  }

  test("Family.exclude rejects entity with excluded component") {
    val family = Family.all(classOf[ComponentA]).exclude(classOf[ComponentB]).get()

    val entity1 = new Entity
    entity1.add(new ComponentA)
    assert(family.matches(entity1))

    val entity2 = new Entity
    entity2.add(new ComponentA)
    entity2.add(new ComponentB)
    assert(!family.matches(entity2))
  }

  test("complex family filtering") {
    val family = Family
      .all(classOf[ComponentA], classOf[ComponentB])
      .one(classOf[ComponentC], classOf[ComponentD])
      .exclude(classOf[ComponentE], classOf[ComponentF])
      .get()

    val entity = new Entity
    assert(!family.matches(entity))

    entity.add(new ComponentA)
    entity.add(new ComponentB)
    assert(!family.matches(entity))

    entity.add(new ComponentC)
    assert(family.matches(entity))

    entity.add(new ComponentE)
    assert(!family.matches(entity))

    entity.remove(classOf[ComponentE])
    assert(family.matches(entity))
  }

  test("family equality with all/one/exclude") {
    val f1 = Family.all(classOf[ComponentA]).one(classOf[ComponentB]).exclude(classOf[ComponentC]).get()
    val f2 = Family.all(classOf[ComponentA]).one(classOf[ComponentB]).exclude(classOf[ComponentC]).get()
    val f3 = Family.all(classOf[ComponentB]).one(classOf[ComponentC]).exclude(classOf[ComponentA]).get()

    assert(f1 eq f2)
    assert(!(f1 eq f3))
  }

  test("match with complex building") {
    val family = Family.all(classOf[ComponentB]).one(classOf[ComponentA]).exclude(classOf[ComponentC]).get()
    val entity = new Entity().add(new ComponentA)
    assert(!family.matches(entity))
    entity.add(new ComponentB)
    assert(family.matches(entity))
    entity.add(new ComponentC)
    assert(!family.matches(entity))
  }
}
