package sge
package ecs

class ComponentTypeSuite extends munit.FunSuite {

  private class ComponentA extends Component
  private class ComponentB extends Component

  test("valid ComponentType") {
    assert(ComponentType.getFor(classOf[ComponentA]) != null)
    assert(ComponentType.getFor(classOf[ComponentB]) != null)
  }

  test("same class returns same ComponentType") {
    val ct1 = ComponentType.getFor(classOf[ComponentA])
    val ct2 = ComponentType.getFor(classOf[ComponentA])

    assertEquals(ct1, ct2)
    assertEquals(ct2, ct1)
    assertEquals(ct1.index, ct2.index)
    assertEquals(ct1.index, ComponentType.getIndexFor(classOf[ComponentA]))
  }

  test("different class returns different ComponentType") {
    val ct1 = ComponentType.getFor(classOf[ComponentA])
    val ct2 = ComponentType.getFor(classOf[ComponentB])

    assertNotEquals(ct1, ct2)
    assertNotEquals(ct2, ct1)
    assertNotEquals(ct1.index, ct2.index)
  }

  test("getBitsFor sets correct bits") {
    val bits   = ComponentType.getBitsFor(classOf[ComponentA], classOf[ComponentB])
    val indexA = ComponentType.getIndexFor(classOf[ComponentA])
    val indexB = ComponentType.getIndexFor(classOf[ComponentB])

    assert(bits.contains(indexA))
    assert(bits.contains(indexB))
  }

  test("unique index per component class") {
    val indexA = ComponentType.getIndexFor(classOf[ComponentA])
    val indexB = ComponentType.getIndexFor(classOf[ComponentB])
    assertNotEquals(indexA, indexB)
  }
}
