package sge
package ecs

/** Red test — ISS-723 clause c12, wave 2026-07-18-G, territory G6.
  *
  * Pins the reflective-instantiation gap in the base [[Engine]].
  *
  * Ashley's base Engine.createComponent (com/badlogic/ashley/core/Engine.java:67-73) reflectively instantiates the component via `ClassReflection.newInstance(componentType)` and returns `null` only
  * when reflection actually fails. It is a WORKING factory on the base Engine — no registration required. The SGE port (sge/ecs/Engine.scala:94-98) instead throws `UnsupportedOperationException`
  * unconditionally, so the base Engine diverges from the original JVM contract.
  *
  * JVM has runtime reflection, so the faithful port must return a fresh instance here. This suite asserts createComponent returns an instance of the requested no-arg Component; it currently fails at
  * runtime with `UnsupportedOperationException`, which is the pinned gap.
  *
  * Scoped to JVM (src/test/scalajvm) on purpose: the reflective contract is JVM-specific. Scala.js and Scala Native have no runtime reflection and resolve the same gap via explicitly registered
  * factories (see [[PooledEngine.registerComponentFactory]]); the cross-platform design is left for the orchestrator to adjudicate before the implementer runs.
  *
  * Reuses the top-level no-arg component `EngineTestComponentD` declared in EngineSuite.scala.
  */
class EngineCreateComponentReflectionRedSuite extends munit.FunSuite {

  test("base Engine.createComponent reflectively instantiates a no-arg Component (ISS-723 c12)") {
    val engine    = new Engine
    val component = engine.createComponent(classOf[EngineTestComponentD])
    assert(component.isDefined, "expected base Engine.createComponent to return an instance via reflection")
    assert(component.get.isInstanceOf[EngineTestComponentD])
  }
}
