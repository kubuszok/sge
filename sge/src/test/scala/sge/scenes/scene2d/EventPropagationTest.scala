/* Copyright 2025-2026 Mateusz Kubuszok / Licensed under Apache 2.0 */
package sge
package scenes
package scene2d

import sge.utils.Nullable

/** Tests for Scene2D event propagation: capture → target → bubble flow, stop/cancel/handled semantics. */
class EventPropagationTest extends munit.FunSuite {

  private def ctx(): Sge = SgeTestFixture.testSge()

  // ---------------------------------------------------------------------------
  // Basic event delivery
  // ---------------------------------------------------------------------------

  test("fire delivers event to target listener") {
    given Sge    = ctx()
    val actor    = Actor()
    var received = false
    actor.addListener(new EventListener {
      def handle(event: Event): Boolean = { received = true; true }
    })

    val event = Event()
    actor.fire(event)
    assert(received)
  }

  test("fire sets event target to the firing actor") {
    given Sge = ctx()
    val actor = Actor()
    var eventTarget: Nullable[Actor] = Nullable.empty
    actor.addListener(new EventListener {
      def handle(event: Event): Boolean = { eventTarget = event.target; false }
    })

    actor.fire(Event())
    assert(eventTarget.exists(_ eq actor))
  }

  test("fire returns true when event is cancelled") {
    given Sge = ctx()
    val actor = Actor()
    actor.addListener(new EventListener {
      def handle(event: Event): Boolean = { event.cancel(); true }
    })

    val result = actor.fire(Event())
    assert(result)
  }

  test("fire returns false when event is not cancelled") {
    given Sge = ctx()
    val actor = Actor()
    actor.addListener(new EventListener {
      def handle(event: Event): Boolean = true
    })

    val result = actor.fire(Event())
    assert(!result)
  }

  // ---------------------------------------------------------------------------
  // Capture phase (root → target)
  // ---------------------------------------------------------------------------

  test("capture listeners called before regular listeners") {
    given Sge = ctx()
    val group = Group()
    val actor = Actor()
    group.addActor(actor)

    val order = scala.collection.mutable.ArrayBuffer[String]()
    group.addCaptureListener(new EventListener {
      def handle(event: Event): Boolean = { order += "group-capture"; false }
    })
    group.addListener(new EventListener {
      def handle(event: Event): Boolean = { order += "group-bubble"; false }
    })
    actor.addListener(new EventListener {
      def handle(event: Event): Boolean = { order += "actor-target"; false }
    })

    actor.fire(Event())
    assertEquals(order.toList, List("group-capture", "actor-target", "group-bubble"))
  }

  test("capture phase walks from root to target") {
    given Sge = ctx()
    val root  = Group()
    val mid   = Group()
    val leaf  = Actor()
    root.addActor(mid)
    mid.addActor(leaf)

    val order = scala.collection.mutable.ArrayBuffer[String]()
    root.addCaptureListener(new EventListener {
      def handle(event: Event): Boolean = { order += "root"; false }
    })
    mid.addCaptureListener(new EventListener {
      def handle(event: Event): Boolean = { order += "mid"; false }
    })
    leaf.addCaptureListener(new EventListener {
      def handle(event: Event): Boolean = { order += "leaf"; false }
    })

    leaf.fire(Event())
    assertEquals(order.toList, List("root", "mid", "leaf"))
  }

  // ---------------------------------------------------------------------------
  // Bubble phase (target → root)
  // ---------------------------------------------------------------------------

  test("bubble phase walks from target to root") {
    given Sge = ctx()
    val root  = Group()
    val mid   = Group()
    val leaf  = Actor()
    root.addActor(mid)
    mid.addActor(leaf)

    val order = scala.collection.mutable.ArrayBuffer[String]()
    leaf.addListener(new EventListener {
      def handle(event: Event): Boolean = { order += "leaf"; false }
    })
    mid.addListener(new EventListener {
      def handle(event: Event): Boolean = { order += "mid"; false }
    })
    root.addListener(new EventListener {
      def handle(event: Event): Boolean = { order += "root"; false }
    })

    leaf.fire(Event())
    assertEquals(order.toList, List("leaf", "mid", "root"))
  }

  // ---------------------------------------------------------------------------
  // Stop / cancel semantics
  // ---------------------------------------------------------------------------

  test("stop prevents further propagation") {
    given Sge = ctx()
    val root  = Group()
    val actor = Actor()
    root.addActor(actor)

    val order = scala.collection.mutable.ArrayBuffer[String]()
    actor.addListener(new EventListener {
      def handle(event: Event): Boolean = { order += "actor"; event.stop(); false }
    })
    root.addListener(new EventListener {
      def handle(event: Event): Boolean = { order += "root"; false }
    })

    actor.fire(Event())
    // After stop, root's bubble listener should NOT be called
    assertEquals(order.toList, List("actor"))
  }

  test("cancel stops propagation and marks cancelled") {
    given Sge = ctx()
    val root  = Group()
    val actor = Actor()
    root.addActor(actor)

    root.addCaptureListener(new EventListener {
      def handle(event: Event): Boolean = { event.cancel(); true }
    })
    var reached = false
    actor.addListener(new EventListener {
      def handle(event: Event): Boolean = { reached = true; false }
    })

    val event     = Event()
    val cancelled = actor.fire(event)
    assert(cancelled)
    assert(event.isCancelled)
    // After cancel in capture, target listener should NOT be called
    assert(!reached)
  }

  test("handled does not stop propagation") {
    given Sge = ctx()
    val root  = Group()
    val actor = Actor()
    root.addActor(actor)

    val order = scala.collection.mutable.ArrayBuffer[String]()
    actor.addListener(new EventListener {
      def handle(event: Event): Boolean = { order += "actor"; event.handle(); true }
    })
    root.addListener(new EventListener {
      def handle(event: Event): Boolean = { order += "root"; false }
    })

    val event = Event()
    actor.fire(event)
    assert(event.isHandled)
    // handled does NOT stop bubbling
    assertEquals(order.toList, List("actor", "root"))
  }

  // ---------------------------------------------------------------------------
  // Multiple listeners on same actor
  // ---------------------------------------------------------------------------

  test("multiple listeners on same actor all receive event") {
    given Sge = ctx()
    val actor = Actor()
    var count = 0
    for (_ <- 0 until 3)
      actor.addListener(new EventListener {
        def handle(event: Event): Boolean = { count += 1; false }
      })

    actor.fire(Event())
    assertEquals(count, 3)
  }

  // ---------------------------------------------------------------------------
  // No-parent actor: no capture/bubble, just target
  // ---------------------------------------------------------------------------

  test("orphan actor receives event without capture/bubble") {
    given Sge    = ctx()
    val actor    = Actor()
    var received = false
    actor.addListener(new EventListener {
      def handle(event: Event): Boolean = { received = true; false }
    })

    actor.fire(Event())
    assert(received)
  }
}
