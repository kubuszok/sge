/* Copyright 2025-2026 Mateusz Kubuszok / Licensed under Apache 2.0 */
package sge
package scenes
package scene2d
package actions

import lowlevel.Nullable
import sge.math.Interpolation
import sge.utils.Seconds

/** Characterization suite for the scenes/scene2d/actions core (ISS-725 — largest untested core subpackage). Every test pins observable behavior — lifecycle state transitions, timing math, pool
  * restoration, composition ordering — verified against the LibGDX originals under original-src/libgdx/gdx/src/com/badlogic/gdx/scenes/scene2d/actions/. No `x != null` theater.
  *
  * Covers: TemporalAction lifecycle, DelayAction, RepeatAction, ParallelAction/SequenceAction composition, and the Actions pooled factory.
  */
class ActionsCharacterizationTest extends munit.FunSuite {

  private def ctx(): Sge = SgeTestFixture.testSge()

  // Records TemporalAction lifecycle callbacks and every percent value passed to update().
  final private class RecordingTemporal extends TemporalAction {
    var beginCount:                       Int                                         = 0
    var endCount:                         Int                                         = 0
    val percents:                         scala.collection.mutable.ArrayBuffer[Float] = scala.collection.mutable.ArrayBuffer.empty
    override protected def begin():       Unit                                        = beginCount += 1
    override protected def end():         Unit                                        = endCount += 1
    protected def update(percent: Float): Unit                                        = percents += percent
    def lastPercent:                      Float                                       = percents.last
  }

  // A leaf Action that reports how many times it was acted / restarted and the last delta it saw.
  // `doneAfter` controls how many act() calls within a run return "not done" before returning true.
  final private class CountingAction(doneAfter: Int = 1) extends Action {
    var acted:               Int     = 0
    var restarts:            Int     = 0
    var lastDelta:           Float   = -1f
    private var runActs:     Int     = 0
    def act(delta: Seconds): Boolean = {
      acted += 1
      runActs += 1
      lastDelta = delta.toFloat
      runActs >= doneAfter
    }
    override def restart(): Unit = {
      restarts += 1
      runActs = 0
    }
  }

  // ===========================================================================
  // TemporalAction lifecycle
  // ===========================================================================

  test("TemporalAction begin() is called exactly once, on the first act") {
    val a = new RecordingTemporal
    a.duration = Seconds(1f)
    a.act(Seconds(0.25f))
    a.act(Seconds(0.25f))
    a.act(Seconds(0.25f))
    assertEquals(a.beginCount, 1)
  }

  test("TemporalAction completes and calls end() once when time reaches duration") {
    val a = new RecordingTemporal
    a.duration = Seconds(1f)

    assert(!a.act(Seconds(0.5f)), "not complete at 0.5 of 1.0")
    assert(!a.complete)
    assertEquals(a.endCount, 0)

    assert(a.act(Seconds(0.5f)), "complete once time == duration")
    assert(a.complete)
    assertEquals(a.endCount, 1)
    assertEquals(a.lastPercent, 1f, "final frame drives percent to exactly 1")
  }

  test("TemporalAction act() after completion short-circuits: no further update()/end()") {
    val a = new RecordingTemporal
    a.duration = Seconds(1f)
    a.act(Seconds(1f))
    val framesAtComplete = a.percents.size
    val endsAtComplete   = a.endCount

    assert(a.act(Seconds(1f)), "still reports done")
    assertEquals(a.percents.size, framesAtComplete, "no extra update() after completion")
    assertEquals(a.endCount, endsAtComplete, "end() not called again")
  }

  test("TemporalAction percent is time/duration before completion") {
    val a = new RecordingTemporal
    a.duration = Seconds(2f)
    a.act(Seconds(0.5f))
    assertEquals(a.lastPercent, 0.25f) // 0.5 / 2.0
    a.act(Seconds(0.5f))
    assertEquals(a.lastPercent, 0.5f) // 1.0 / 2.0
  }

  test("TemporalAction with zero duration completes on the first frame at percent 1") {
    val a = new RecordingTemporal // default duration is zero
    assert(a.act(Seconds.zero))
    assert(a.complete)
    assertEquals(a.lastPercent, 1f)
    assertEquals(a.endCount, 1)
  }

  test("TemporalAction reverse feeds update() with 1 - percent") {
    val a = new RecordingTemporal
    a.duration = Seconds(1f)
    a.reverse = true
    a.act(Seconds(0.25f))
    assertEquals(a.lastPercent, 0.75f) // 1 - 0.25
  }

  test("TemporalAction applies interpolation to percent") {
    val a = new RecordingTemporal
    a.duration = Seconds(1f)
    a.interpolation = Nullable(Interpolation.pow2In) // f(x) = x^2
    a.act(Seconds(0.5f))
    assertEquals(a.lastPercent, 0.25f) // pow2In(0.5) = 0.25
  }

  test("TemporalAction finish() skips to the end so the next act completes") {
    val a = new RecordingTemporal
    a.duration = Seconds(10f)
    a.finish() // sets time = duration
    assert(a.act(Seconds.zero), "already at duration -> completes on next act")
    assert(a.complete)
    assertEquals(a.lastPercent, 1f)
  }

  test("TemporalAction restart() re-runs begin() and clears completion") {
    val a = new RecordingTemporal
    a.duration = Seconds(1f)
    a.act(Seconds(1f))
    assert(a.complete)

    a.restart()
    assert(!a.complete, "restart clears complete")
    assertEquals(a.time.toFloat, 0f, "restart resets time")
    a.act(Seconds(0.25f))
    assertEquals(a.beginCount, 2, "restart makes begin() run again")
  }

  test("TemporalAction reset() clears reverse and interpolation") {
    val a = new RecordingTemporal
    a.reverse = true
    a.interpolation = Nullable(Interpolation.linear)
    a.reset()
    assert(!a.reverse)
    assert(a.interpolation.isEmpty)
  }

  // ===========================================================================
  // DelayAction
  // ===========================================================================

  test("DelayAction does not act the wrapped action until the delay elapses") {
    val inner = new CountingAction
    val delay = DelayAction(Seconds(1f))
    delay.action = Nullable(inner)

    assert(!delay.act(Seconds(0.5f)), "still delaying")
    assertEquals(inner.acted, 0, "wrapped action untouched during the delay")
  }

  test("DelayAction forwards only the overshoot delta on the frame the delay elapses") {
    val inner = new CountingAction
    val delay = DelayAction(Seconds(1f))
    delay.action = Nullable(inner)

    delay.act(Seconds(0.5f)) // time -> 0.5, still delaying
    delay.act(Seconds(0.7f)) // time -> 1.2, elapses; overshoot = 0.2
    assertEquals(inner.acted, 1)
    assertEqualsFloat(inner.lastDelta, 0.2f, 1e-5f)
  }

  test("DelayAction forwards the full delta once the delay has elapsed") {
    val inner = new CountingAction(doneAfter = 99) // never reports done, so delay keeps forwarding
    val delay = DelayAction(Seconds(1f))
    delay.action = Nullable(inner)

    delay.act(Seconds(1.2f)) // elapse; forwards 0.2
    delay.act(Seconds(0.3f)) // past delay; forwards full 0.3
    assertEquals(inner.acted, 2)
    assertEqualsFloat(inner.lastDelta, 0.3f, 1e-5f)
  }

  test("DelayAction finish() completes the delay and restart() resets its time") {
    val inner = new CountingAction
    val delay = DelayAction(Seconds(5f))
    delay.action = Nullable(inner)

    delay.finish()
    assert(delay.act(Seconds.zero), "finished delay runs the wrapped action immediately")
    assertEquals(inner.acted, 1)

    delay.restart()
    assertEquals(delay.time.toFloat, 0f)
  }

  // ===========================================================================
  // RepeatAction
  // ===========================================================================

  test("RepeatAction repeats the wrapped action `count` times, restarting between runs") {
    val inner  = new CountingAction // completes every act
    val repeat = new RepeatAction
    repeat.count = 3
    repeat.action = Nullable(inner)

    assert(!repeat.act(Seconds(1f)), "run 1 of 3")
    assert(!repeat.act(Seconds(1f)), "run 2 of 3")
    assert(repeat.act(Seconds(1f)), "run 3 of 3 completes")
    assertEquals(inner.acted, 3)
    assertEquals(inner.restarts, 2, "restarts between the 3 runs")
  }

  test("RepeatAction FOREVER never reports completion") {
    val inner  = new CountingAction
    val repeat = new RepeatAction
    repeat.count = RepeatAction.FOREVER
    repeat.action = Nullable(inner)

    (1 to 5).foreach(_ => assert(!repeat.act(Seconds(1f)), "FOREVER stays incomplete"))
    assertEquals(inner.acted, 5)
  }

  test("RepeatAction finish() stops repetition at the next completed run") {
    val inner  = new CountingAction
    val repeat = new RepeatAction
    repeat.count = RepeatAction.FOREVER
    repeat.action = Nullable(inner)

    repeat.act(Seconds(1f)) // one run, still forever
    repeat.finish()
    assert(repeat.act(Seconds(1f)), "finish() makes the next completed run terminate the repeat")
  }

  // ===========================================================================
  // ParallelAction / SequenceAction composition
  // ===========================================================================

  test("ParallelAction advances all children each frame and completes only when all are done") {
    given Sge = ctx()
    val actor = Actor()
    val a     = new RecordingTemporal
    a.duration = Seconds(1f)
    val b = new RecordingTemporal
    b.duration = Seconds(2f)

    val p = new ParallelAction
    p.addAction(a)
    p.addAction(b)
    p.setActor(Nullable(actor)) // propagates the actor to both children

    assert(!p.act(Seconds(1f)), "a done, b not -> parallel not complete")
    assertEquals(a.beginCount, 1)
    assertEquals(b.beginCount, 1, "both children begin on the same frame (concurrent)")
    assert(a.complete)
    assert(!b.complete)

    assert(p.act(Seconds(1f)), "b reaches its duration -> parallel complete")
    assert(b.complete)
  }

  test("ParallelAction act() short-circuits once complete without re-acting children") {
    given Sge = ctx()
    val actor = Actor()
    val a     = new RecordingTemporal
    a.duration = Seconds(1f)

    val p = new ParallelAction
    p.addAction(a)
    p.setActor(Nullable(actor))

    assert(p.act(Seconds(1f)), "single child completes -> parallel complete")
    val framesAtComplete = a.percents.size
    assert(p.act(Seconds(1f)), "still complete")
    assertEquals(a.percents.size, framesAtComplete, "child not acted again after parallel completes")
  }

  test("SequenceAction runs children strictly in order") {
    given Sge = ctx()
    val actor = Actor()
    val first = new RecordingTemporal
    first.duration = Seconds(1f)
    val second = new RecordingTemporal
    second.duration = Seconds(1f)

    val s = new SequenceAction
    s.addAction(first)
    s.addAction(second)
    s.setActor(Nullable(actor))

    assert(!s.act(Seconds(1f)), "first completes, sequence advances but is not done")
    assertEquals(first.beginCount, 1)
    assertEquals(second.beginCount, 0, "second must not start until the first finishes")

    assert(s.act(Seconds(1f)), "second completes -> sequence complete")
    assertEquals(second.beginCount, 1)
  }

  test("SequenceAction with no children is immediately complete") {
    val s = new SequenceAction
    assert(s.act(Seconds(1f)))
  }

  test("ParallelAction.addAction propagates the already-set actor to the new child") {
    given Sge = ctx()
    val actor = Actor()
    val p     = new ParallelAction
    p.setActor(Nullable(actor))
    val child = new RecordingTemporal
    p.addAction(child)
    assert(child.actor.isDefined, "child added after setActor inherits the parallel's actor")
    assert(child.actor.exists(_ eq actor))
  }

  // ===========================================================================
  // Actions pooled factory
  // ===========================================================================

  test("Actions.action assigns the originating pool to the obtained action") {
    val a = Actions.action[DelayAction]
    assert(a.pool.isDefined, "a pooled action carries a reference to its pool")
  }

  test("Actions.moveBy configures amount and duration on the pooled action") {
    val a = Actions.moveBy(30f, -40f, Seconds(2f))
    assertEquals(a.duration.toFloat, 2f)
    // MoveByAction stores the requested delta; drive one full frame on an actor to observe it.
    given Sge = ctx()
    val actor = Actor()
    actor.setPosition(0f, 0f)
    actor.addAction(a)
    actor.act(Seconds(2f))
    assertEqualsFloat(actor.x, 30f, 1e-4f)
    assertEqualsFloat(actor.y, -40f, 1e-4f)
  }

  test("Actions pool restores freed actions: setActor(empty) returns the instance and resets it") {
    val a1 = Actions.action[DelayAction]
    a1.duration = Seconds(9f)
    a1.action = Nullable(new CountingAction)
    a1.setActor(Nullable.empty) // no actor + pooled -> freed back to the pool (reset() runs)

    val a2 = Actions.action[DelayAction]
    assert(a2 eq a1, "the pool hands back the just-freed instance (LIFO reuse)")
    assert(a2.action.isEmpty, "reset() cleared the wrapped action")
    assertEquals(a2.time.toFloat, 0f, "reset()/restart() zeroed the delay time")
  }

  test("Actions.sequence and Actions.parallel varargs collect all children") {
    val moves = (1 to 4).map(i => Actions.moveBy(i.toFloat, 0f, Seconds(1f)))
    val seq   = Actions.sequence(moves*)
    assertEquals(seq.actions.size, 4)
    val par = Actions.parallel(moves*)
    assertEquals(par.actions.size, 4)
  }
}
