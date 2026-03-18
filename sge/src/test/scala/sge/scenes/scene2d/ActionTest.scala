/* Copyright 2025-2026 Mateusz Kubuszok / Licensed under Apache 2.0 */
package sge
package scenes
package scene2d

import sge.scenes.scene2d.actions.{ Actions, MoveByAction }
import sge.utils.Seconds

/** Tests for Scene2D Action lifecycle: add, act, remove, sequence, parallel. */
class ActionTest extends munit.FunSuite {

  private def ctx(): Sge = SgeTestFixture.testSge()

  // ---------------------------------------------------------------------------
  // Basic action lifecycle
  // ---------------------------------------------------------------------------

  test("addAction increases action count") {
    given Sge = ctx()
    val actor = Actor()
    assert(!actor.hasActions)

    val action = Actions.moveBy(10f, 0f, Seconds(1f))
    actor.addAction(action)
    assert(actor.hasActions)
    assertEquals(actor.actions.size, 1)
  }

  test("act removes completed actions") {
    given Sge = ctx()
    val actor = Actor()
    actor.addAction(Actions.moveBy(10f, 0f, Seconds(0.5f)))

    // Advance past duration
    actor.act(1f)
    assert(!actor.hasActions)
  }

  test("clearActions removes all actions") {
    given Sge = ctx()
    val actor = Actor()
    actor.addAction(Actions.moveBy(10f, 0f, Seconds(1f)))
    actor.addAction(Actions.moveBy(0f, 10f, Seconds(1f)))
    assertEquals(actor.actions.size, 2)

    actor.clearActions()
    assertEquals(actor.actions.size, 0)
  }

  // ---------------------------------------------------------------------------
  // Movement actions
  // ---------------------------------------------------------------------------

  test("moveBy moves actor by specified amount") {
    given Sge = ctx()
    val actor = Actor()
    actor.setPosition(100f, 200f)

    // Create directly to bypass pool
    val action = MoveByAction()
    action.setAmount(50f, -30f)
    action.duration = Seconds(1f)

    actor.addAction(action)
    assert(actor.hasActions, "action should be added")
    assert(action.target.isDefined, "target should be set after addAction")

    actor.act(1f) // Advance full duration

    assertEqualsFloat(actor.x, 150f, 0.01f)
    assertEqualsFloat(actor.y, 170f, 0.01f)
  }

  test("moveBy partial progress") {
    given Sge = ctx()
    val actor = Actor()
    actor.setPosition(0f, 0f)

    actor.addAction(Actions.moveBy(100f, 0f, Seconds(1f)))
    actor.act(0.5f) // Half duration

    assertEqualsFloat(actor.x, 50f, 1f) // Linear interpolation by default
  }

  test("moveTo moves actor to absolute position") {
    given Sge = ctx()
    val actor = Actor()
    actor.setPosition(0f, 0f)

    actor.addAction(Actions.moveTo(200f, 300f, Seconds(1f)))
    actor.act(1f)

    assertEqualsFloat(actor.x, 200f, 0.01f)
    assertEqualsFloat(actor.y, 300f, 0.01f)
  }

  // ---------------------------------------------------------------------------
  // Visibility actions
  // ---------------------------------------------------------------------------

  test("hide action sets visible to false") {
    given Sge = ctx()
    val actor = Actor()
    assert(actor.visible)

    actor.addAction(Actions.hide())
    actor.act(0f)
    assert(!actor.visible)
  }

  test("show action sets visible to true") {
    given Sge = ctx()
    val actor = Actor()
    actor.visible = false

    actor.addAction(Actions.show())
    actor.act(0f)
    assert(actor.visible)
  }

  // ---------------------------------------------------------------------------
  // Scale actions
  // ---------------------------------------------------------------------------

  test("scaleTo changes actor scale") {
    given Sge = ctx()
    val actor = Actor()

    actor.addAction(Actions.scaleTo(2f, 3f, Seconds(1f)))
    actor.act(1f)

    assertEqualsFloat(actor.scaleX, 2f, 0.01f)
    assertEqualsFloat(actor.scaleY, 3f, 0.01f)
  }

  // ---------------------------------------------------------------------------
  // Rotation actions
  // ---------------------------------------------------------------------------

  test("rotateTo changes actor rotation") {
    given Sge = ctx()
    val actor = Actor()

    actor.addAction(Actions.rotateTo(90f, Seconds(1f)))
    actor.act(1f)

    assertEqualsFloat(actor.rotation, 90f, 0.01f)
  }

  test("rotateBy adds to current rotation") {
    given Sge = ctx()
    val actor = Actor()
    actor.rotation = 45f

    actor.addAction(Actions.rotateBy(90f, Seconds(1f)))
    actor.act(1f)

    assertEqualsFloat(actor.rotation, 135f, 0.01f)
  }

  // ---------------------------------------------------------------------------
  // Composition: sequence
  // ---------------------------------------------------------------------------

  test("sequence runs actions in order") {
    given Sge = ctx()
    val actor = Actor()
    actor.setPosition(0f, 0f)

    val action = Actions.sequence(
      Actions.moveBy(100f, 0f, Seconds(1f)),
      Actions.moveBy(0f, 100f, Seconds(1f))
    )
    actor.addAction(action)

    // First action only
    actor.act(1f)
    assertEqualsFloat(actor.x, 100f, 0.5f)
    assertEqualsFloat(actor.y, 0f, 0.5f)

    // Second action
    actor.act(1f)
    assertEqualsFloat(actor.x, 100f, 0.5f)
    assertEqualsFloat(actor.y, 100f, 0.5f)
  }

  // ---------------------------------------------------------------------------
  // Composition: parallel
  // ---------------------------------------------------------------------------

  test("parallel runs actions concurrently") {
    given Sge = ctx()
    val actor = Actor()
    actor.setPosition(0f, 0f)

    val action = Actions.parallel(
      Actions.moveBy(100f, 0f, Seconds(1f)),
      Actions.moveBy(0f, 100f, Seconds(1f))
    )
    actor.addAction(action)
    actor.act(1f)

    assertEqualsFloat(actor.x, 100f, 0.5f)
    assertEqualsFloat(actor.y, 100f, 0.5f)
  }

  // ---------------------------------------------------------------------------
  // Delay
  // ---------------------------------------------------------------------------

  test("delay postpones wrapped action") {
    given Sge = ctx()
    val actor = Actor()
    actor.setPosition(0f, 0f)

    val action = Actions.delay(Seconds(1f), Actions.moveBy(100f, 0f, Seconds(0f)))
    actor.addAction(action)

    actor.act(0.5f)
    assertEqualsFloat(actor.x, 0f, 0.01f) // Not yet

    actor.act(0.6f)
    assertEqualsFloat(actor.x, 100f, 0.5f) // Now moved
  }

  // ---------------------------------------------------------------------------
  // Runnable action
  // ---------------------------------------------------------------------------

  test("run action executes runnable") {
    given Sge  = ctx()
    val actor  = Actor()
    var called = false

    actor.addAction(Actions.run(() => called = true))
    actor.act(0f)
    assert(called)
  }

  // ---------------------------------------------------------------------------
  // Remove action
  // ---------------------------------------------------------------------------

  test("removeActor action removes actor from parent") {
    given Sge = ctx()
    val group = Group()
    val actor = Actor()
    group.addActor(actor)

    actor.addAction(Actions.removeActor())
    actor.act(0f)
    assert(actor.parent.isEmpty)
    assertEquals(group.children.size, 0)
  }

  // ---------------------------------------------------------------------------
  // Helper
  // ---------------------------------------------------------------------------

  private def assertEqualsFloat(actual: Float, expected: Float, tolerance: Float)(implicit loc: munit.Location): Unit =
    assert(
      Math.abs(actual - expected) <= tolerance,
      s"expected $expected ± $tolerance, got $actual"
    )
}
