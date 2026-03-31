package sge
package ai
package fsm

import sge.ai.msg.Telegram
import sge.utils.Nullable

class TrackingState extends State[String] {
  var events:                                                 List[String] = List.empty
  override def enter(entity:     String):                     Unit         = events = events :+ s"enter:$entity"
  override def update(entity:    String):                     Unit         = events = events :+ s"update:$entity"
  override def exit(entity:      String):                     Unit         = events = events :+ s"exit:$entity"
  override def onMessage(entity: String, telegram: Telegram): Boolean      = false
}

class MessageHandlingState(val handle: Boolean) extends State[String] {
  override def enter(entity:     String):                     Unit    = {}
  override def update(entity:    String):                     Unit    = {}
  override def exit(entity:      String):                     Unit    = {}
  override def onMessage(entity: String, telegram: Telegram): Boolean = handle
}

class StateMachineSuite extends munit.FunSuite {

  test("initial state enter is called via changeState") {
    val state = new TrackingState()
    val fsm   = new DefaultStateMachine[String, TrackingState](
      "hero",
      Nullable.empty[TrackingState],
      Nullable.empty[TrackingState]
    )
    fsm.changeState(state)
    assertEquals(state.events, List("enter:hero"))
  }

  test("update delegates to current state") {
    val state = new TrackingState()
    val fsm   = new DefaultStateMachine[String, TrackingState](
      "hero",
      Nullable.empty[TrackingState],
      Nullable.empty[TrackingState]
    )
    fsm.changeState(state)
    state.events = Nil // reset
    fsm.update()
    assertEquals(state.events, List("update:hero"))
  }

  test("changeState calls exit on old and enter on new") {
    val stateA = new TrackingState()
    val stateB = new TrackingState()
    val fsm    = new DefaultStateMachine[String, TrackingState](
      "hero",
      Nullable.empty[TrackingState],
      Nullable.empty[TrackingState]
    )
    fsm.changeState(stateA)
    stateA.events = Nil
    fsm.changeState(stateB)
    assertEquals(stateA.events, List("exit:hero"))
    assertEquals(stateB.events, List("enter:hero"))
  }

  test("revertToPreviousState works") {
    val stateA = new TrackingState()
    val stateB = new TrackingState()
    val fsm    = new DefaultStateMachine[String, TrackingState](
      "hero",
      Nullable.empty[TrackingState],
      Nullable.empty[TrackingState]
    )
    fsm.changeState(stateA)
    fsm.changeState(stateB)
    stateA.events = Nil
    stateB.events = Nil
    val reverted = fsm.revertToPreviousState()
    assert(reverted, "revertToPreviousState should return true")
    // exit B, enter A
    assertEquals(stateB.events, List("exit:hero"))
    assertEquals(stateA.events, List("enter:hero"))
    assert(fsm.isInState(stateA), "should be in stateA")
  }

  test("revertToPreviousState returns false when no previous") {
    val fsm = new DefaultStateMachine[String, TrackingState](
      "hero",
      Nullable.empty[TrackingState],
      Nullable.empty[TrackingState]
    )
    assert(!fsm.revertToPreviousState(), "should return false with no previous state")
  }

  test("handleMessage routes to current state then global state") {
    val currentState = new MessageHandlingState(false)
    val globalState  = new MessageHandlingState(true)
    val fsm          = new DefaultStateMachine[String, MessageHandlingState](
      "hero",
      initialState = Nullable(currentState),
      _globalState = Nullable(globalState)
    )
    val telegram = new Telegram()
    telegram.message = 99
    // Current state returns false, so global state handles it
    val handled = fsm.handleMessage(telegram)
    assert(handled, "global state should handle the message")
  }

  test("handleMessage: current state handles if it returns true") {
    val currentState = new MessageHandlingState(true)
    val globalState  = new MessageHandlingState(true)
    val fsm          = new DefaultStateMachine[String, MessageHandlingState](
      "hero",
      initialState = Nullable(currentState),
      _globalState = Nullable(globalState)
    )
    val telegram = new Telegram()
    telegram.message = 1
    assert(fsm.handleMessage(telegram), "current state should handle the message")
  }

  // ── StackStateMachine ──────────────────────────────────────────────────

  test("StackStateMachine: push and pop states") {
    val stateA = new TrackingState()
    val stateB = new TrackingState()
    val stateC = new TrackingState()
    val fsm    = new StackStateMachine[String, TrackingState](
      "hero",
      Nullable.empty[TrackingState],
      Nullable.empty[TrackingState]
    )

    fsm.changeState(stateA)
    fsm.changeState(stateB)
    fsm.changeState(stateC)

    assert(fsm.isInState(stateC), "should be in stateC")

    // Pop back to B
    stateC.events = Nil
    stateB.events = Nil
    assert(fsm.revertToPreviousState(), "should revert to B")
    assertEquals(stateC.events, List("exit:hero"))
    assertEquals(stateB.events, List("enter:hero"))
    assert(fsm.isInState(stateB), "should be in stateB")

    // Pop back to A
    stateB.events = Nil
    stateA.events = Nil
    assert(fsm.revertToPreviousState(), "should revert to A")
    assertEquals(stateB.events, List("exit:hero"))
    assertEquals(stateA.events, List("enter:hero"))
    assert(fsm.isInState(stateA), "should be in stateA")

    // No more previous states
    assert(!fsm.revertToPreviousState(), "no more previous states")
  }

  test("StackStateMachine: update delegates to current state") {
    val state = new TrackingState()
    val fsm   = new StackStateMachine[String, TrackingState](
      "hero",
      Nullable.empty[TrackingState],
      Nullable.empty[TrackingState]
    )
    fsm.changeState(state)
    state.events = Nil
    fsm.update()
    assertEquals(state.events, List("update:hero"))
  }
}
