/* Copyright 2025-2026 Mateusz Kubuszok / Licensed under Apache 2.0 */
package sge
package scenes
package scene2d
package actions

import lowlevel.Nullable
import sge.graphics.Color
import sge.math.Interpolation
import sge.utils.{ Align, Seconds }

/** Behavioural, exact-value suite for the concrete leaf Action subclasses in scenes/scene2d/actions (ISS-725, territory J5).
  *
  * Complements ActionsCharacterizationTest (which pins TemporalAction lifecycle, DelayAction, RepeatAction, composition and pooling) by driving the *interpolation math* of the spatial / value actions
  * against a real Actor and asserting the exact computed field values: MoveTo/MoveBy, ScaleTo/ScaleBy, RotateTo/RotateBy, SizeTo/SizeBy, Alpha, Color, Float, Int, and the RelativeTemporalAction
  * per-frame-delta accumulation. Every assertion is a concrete number derived from the LibGDX originals under original-src/libgdx/.../scenes/scene2d/actions/. No `x != null` theater.
  */
class ActionsSpatialBehaviourSuite extends munit.FunSuite {

  private val eps = 1e-4f

  private def freshActor(): Actor = {
    given Sge = SgeTestFixture.testSge()
    Actor()
  }

  // Attaches an action to an actor (sets actor+target) and returns both for direct driving.
  private def bind[A <: Action](actor: Actor, action: A): A = {
    action.setActor(Nullable(actor))
    action
  }

  // ===========================================================================
  // MoveToAction
  // ===========================================================================

  test("MoveToAction lerps position: (0,0)->(10,20) over 1.0 at act(0.5) puts actor at (5,10)") {
    val actor = freshActor()
    actor.setPosition(0f, 0f)
    val a = bind(actor, new MoveToAction)
    a.duration = Seconds(1f)
    a.setPosition(10f, 20f)

    assert(!a.act(Seconds(0.5f)), "half-way is not complete")
    assertEqualsFloat(actor.x, 5f, eps)
    assertEqualsFloat(actor.y, 10f, eps)
  }

  test("MoveToAction final frame snaps exactly to the endpoint (percent==1 branch, no lerp rounding)") {
    val actor = freshActor()
    actor.setPosition(3f, 7f)
    val a = bind(actor, new MoveToAction)
    a.duration = Seconds(1f)
    a.setPosition(10f, 20f)

    assert(a.act(Seconds(1f)), "reaching duration completes")
    assertEqualsFloat(actor.x, 10f, eps)
    assertEqualsFloat(actor.y, 20f, eps)
  }

  test("MoveToAction applies interpolation before lerping: pow2In at act(0.5) -> percent 0.25 -> x=25") {
    val actor = freshActor()
    actor.setPosition(0f, 0f)
    val a = bind(actor, new MoveToAction)
    a.duration = Seconds(1f)
    a.interpolation = Nullable(Interpolation.pow2In) // f(0.5) = 0.25
    a.setPosition(100f, 0f)

    a.act(Seconds(0.5f))
    assertEqualsFloat(actor.x, 25f, eps) // 0 + 100 * 0.25
  }

  test("MoveToAction reverse drives percent 1->0: a reversed move completes back at the start (percent==0 branch)") {
    val actor = freshActor()
    actor.setPosition(0f, 0f)
    val a = bind(actor, new MoveToAction)
    a.duration = Seconds(1f)
    a.reverse = true
    a.setPosition(100f, 0f)

    a.act(Seconds(0.25f))
    assertEqualsFloat(actor.x, 75f, eps) // update(1 - 0.25) = 0.75 -> 75

    assert(a.act(Seconds(0.75f)), "completes at duration")
    assertEqualsFloat(actor.x, 0f, eps) // percent 1, reversed -> update(0) -> startX
  }

  test("MoveToAction honours Align.center: start/end queried and set at the actor centre") {
    val actor = freshActor()
    actor.setSize(40f, 20f)
    actor.setPosition(0f, 0f) // bottom-left origin
    val a = bind(actor, new MoveToAction)
    a.duration = Seconds(1f)
    a.setPosition(20f, 30f, Align.center)

    // begin(): startX = getX(center) = x + w/2 = 20 ; startY = y + h/2 = 10
    // percent 1 -> setPosition(endX=20, endY=30, center) -> x = 20 - w/2 = 0 ; y = 30 - h/2 = 20
    assert(a.act(Seconds(1f)))
    assertEqualsFloat(actor.x, 0f, eps)
    assertEqualsFloat(actor.y, 20f, eps)
  }

  // ===========================================================================
  // MoveByAction (RelativeTemporalAction)
  // ===========================================================================

  test("MoveByAction accumulates per-frame deltas: two 0.5 frames of amount (10,20) move exactly (10,20)") {
    val actor = freshActor()
    actor.setPosition(1f, 2f)
    val a = bind(actor, new MoveByAction)
    a.duration = Seconds(1f)
    a.setAmount(10f, 20f)

    a.act(Seconds(0.5f)) // percentDelta 0.5 -> moveBy(5,10)
    assertEqualsFloat(actor.x, 6f, eps)
    assertEqualsFloat(actor.y, 12f, eps)

    assert(a.act(Seconds(0.5f))) // percentDelta 0.5 -> moveBy(5,10)
    assertEqualsFloat(actor.x, 11f, eps) // 1 + 10
    assertEqualsFloat(actor.y, 22f, eps) // 2 + 20
  }

  // ===========================================================================
  // ScaleToAction / ScaleByAction
  // ===========================================================================

  test("ScaleToAction lerps scale from the actor's current scale to the target") {
    val actor = freshActor()
    actor.setScale(1f, 1f)
    val a = bind(actor, new ScaleToAction)
    a.duration = Seconds(1f)
    a.setScale(3f, 5f)

    a.act(Seconds(0.5f))
    assertEqualsFloat(actor.scaleX, 2f, eps) // 1 + (3-1)*0.5
    assertEqualsFloat(actor.scaleY, 3f, eps) // 1 + (5-1)*0.5
  }

  test("ScaleByAction adds a relative scale delta over the run") {
    val actor = freshActor()
    actor.setScale(1f, 1f)
    val a = bind(actor, new ScaleByAction)
    a.duration = Seconds(1f)
    a.setAmount(1f, 2f)

    a.act(Seconds(0.5f)) // +(0.5,1)
    assertEqualsFloat(actor.scaleX, 1.5f, eps)
    assertEqualsFloat(actor.scaleY, 2f, eps)
    assert(a.act(Seconds(0.5f))) // +(0.5,1)
    assertEqualsFloat(actor.scaleX, 2f, eps) // 1 + total 1
    assertEqualsFloat(actor.scaleY, 3f, eps) // 1 + total 2
  }

  // ===========================================================================
  // RotateToAction / RotateByAction
  // ===========================================================================

  test("RotateToAction lerps rotation linearly by default") {
    val actor = freshActor()
    actor.setRotation(0f)
    val a = bind(actor, new RotateToAction())
    a.duration = Seconds(1f)
    a.rotation = 90f

    a.act(Seconds(0.5f))
    assertEqualsFloat(actor.rotation, 45f, eps)
  }

  test("RotateToAction useShortestDirection goes the short way: 10 -> 350 at half lands on 0") {
    val actor = freshActor()
    actor.setRotation(10f)
    val a = bind(actor, new RotateToAction(useShortestDirection = true))
    a.duration = Seconds(1f)
    a.rotation = 350f

    a.act(Seconds(0.5f))
    // lerpAngleDeg(10,350,0.5): delta -20 -> 10 - 10 = 0 (short way through 0, not forward through 180)
    assertEqualsFloat(actor.rotation, 0f, eps)
  }

  test("RotateByAction adds a relative rotation over the run") {
    val actor = freshActor()
    actor.setRotation(0f)
    val a = bind(actor, new RotateByAction)
    a.duration = Seconds(1f)
    a.amount = 90f

    a.act(Seconds(0.5f)) // rotateBy(45)
    assertEqualsFloat(actor.rotation, 45f, eps)
    assert(a.act(Seconds(0.5f))) // rotateBy(45)
    assertEqualsFloat(actor.rotation, 90f, eps)
  }

  // ===========================================================================
  // SizeToAction / SizeByAction
  // ===========================================================================

  test("SizeToAction lerps width/height from the actor's current size to the target") {
    val actor = freshActor()
    actor.setSize(10f, 20f)
    val a = bind(actor, new SizeToAction)
    a.duration = Seconds(1f)
    a.setSize(30f, 60f)

    a.act(Seconds(0.5f))
    assertEqualsFloat(actor.width, 20f, eps) // 10 + (30-10)*0.5
    assertEqualsFloat(actor.height, 40f, eps) // 20 + (60-20)*0.5
  }

  test("SizeByAction adds a relative size delta over the run") {
    val actor = freshActor()
    actor.setSize(10f, 20f)
    val a = bind(actor, new SizeByAction)
    a.duration = Seconds(1f)
    a.setAmount(10f, 40f)

    assert(a.act(Seconds(1f)))
    assertEqualsFloat(actor.width, 20f, eps) // 10 + 10
    assertEqualsFloat(actor.height, 60f, eps) // 20 + 40
  }

  // ===========================================================================
  // AlphaAction
  // ===========================================================================

  test("AlphaAction lerps the actor colour's alpha from its start value to the target") {
    val actor = freshActor() // default colour is (1,1,1,1)
    val a     = bind(actor, new AlphaAction)
    a.duration = Seconds(1f)
    a.alpha = 0f

    a.act(Seconds(0.5f))
    assertEqualsFloat(actor.color.a, 0.5f, eps) // 1 + (0-1)*0.5
    assert(a.act(Seconds(0.5f)))
    assertEqualsFloat(actor.color.a, 0f, eps) // percent 1 -> alpha
  }

  test("AlphaAction targets an explicitly supplied colour instead of the actor's") {
    val actor = freshActor()
    val other = Color(1f, 1f, 1f, 1f)
    val a     = bind(actor, new AlphaAction)
    a.duration = Seconds(1f)
    a.alpha = 0f
    a.color = Nullable(other)

    a.act(Seconds(0.5f))
    assertEqualsFloat(other.a, 0.5f, eps)
    assertEqualsFloat(actor.color.a, 1f, eps) // actor colour untouched
  }

  // ===========================================================================
  // ColorAction
  // ===========================================================================

  test("ColorAction lerps every RGBA channel from the actor colour to the end colour") {
    val actor = freshActor() // (1,1,1,1)
    val a     = bind(actor, new ColorAction)
    a.duration = Seconds(1f)
    a.setEndColor(Color(0f, 0.5f, 1f, 0f))

    a.act(Seconds(0.5f))
    assertEqualsFloat(actor.color.r, 0.5f, eps) // 1 + (0-1)*0.5
    assertEqualsFloat(actor.color.g, 0.75f, eps) // 1 + (0.5-1)*0.5
    assertEqualsFloat(actor.color.b, 1f, eps) // 1 + (1-1)*0.5
    assertEqualsFloat(actor.color.a, 0.5f, eps) // 1 + (0-1)*0.5
  }

  test("ColorAction final frame snaps every channel to the end colour exactly") {
    val actor = freshActor()
    val a     = bind(actor, new ColorAction)
    a.duration = Seconds(1f)
    a.setEndColor(Color(0.2f, 0.4f, 0.6f, 0.8f))

    assert(a.act(Seconds(1f)))
    assertEqualsFloat(actor.color.r, 0.2f, eps)
    assertEqualsFloat(actor.color.g, 0.4f, eps)
    assertEqualsFloat(actor.color.b, 0.6f, eps)
    assertEqualsFloat(actor.color.a, 0.8f, eps)
  }

  // ===========================================================================
  // FloatAction / IntAction
  // ===========================================================================

  test("FloatAction transitions value from start to end and snaps to end at percent 1") {
    val a = new FloatAction(2f, 10f)
    a.duration = Seconds(1f)
    a.setActor(Nullable.empty) // no actor needed; value is self-contained

    a.act(Seconds(0.5f))
    assertEqualsFloat(a.value, 6f, eps) // 2 + (10-2)*0.5
    assert(a.act(Seconds(0.5f)))
    assertEqualsFloat(a.value, 10f, eps) // percent 1 -> end
  }

  test("FloatAction begin() seeds value with start; getEnd/setEnd expose the end field") {
    val a = new FloatAction(3f, 9f)
    assertEqualsFloat(a.getEnd, 9f, eps)
    a.setEnd(12f)
    assertEqualsFloat(a.getEnd, 12f, eps)
    a.duration = Seconds(2f)
    a.act(Seconds(0f)) // first frame: begin() sets value=start before the (zero) update
    assertEqualsFloat(a.value, 3f, eps)
  }

  test("FloatAction applies interpolation to the transition") {
    val a = new FloatAction(0f, 100f, Seconds(1f), Nullable(Interpolation.pow2In))
    a.act(Seconds(0.5f)) // pow2In(0.5)=0.25 -> 0 + 100*0.25
    assertEqualsFloat(a.value, 25f, eps)
  }

  test("IntAction truncates toward zero: 0->10 over 3.0 at act(1.0) yields floor(3.333)=3") {
    val a = new IntAction(0, 10)
    a.duration = Seconds(3f)
    a.act(Seconds(1f)) // percent 1/3 -> (0 + 10*0.3333).toInt = 3
    assertEquals(a.value, 3)
  }

  test("IntAction snaps to the exact end at percent 1") {
    val a = new IntAction(4, 42)
    a.duration = Seconds(1f)
    assert(a.act(Seconds(1f)))
    assertEquals(a.value, 42)
  }

  // ===========================================================================
  // RelativeTemporalAction contract (via MoveByAction restart)
  // ===========================================================================

  test("RelativeTemporalAction restart re-seeds lastPercent so a re-run applies the full amount again") {
    val actor = freshActor()
    actor.setPosition(0f, 0f)
    val a = bind(actor, new MoveByAction)
    a.duration = Seconds(1f)
    a.setAmount(10f, 0f)

    assert(a.act(Seconds(1f))) // full run -> +10
    assertEqualsFloat(actor.x, 10f, eps)

    a.restart() // begin() must reset lastPercent to 0
    assert(a.act(Seconds(1f))) // second full run -> another +10, not a no-op
    assertEqualsFloat(actor.x, 20f, eps)
  }
}
