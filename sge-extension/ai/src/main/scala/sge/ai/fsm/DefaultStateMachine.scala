/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/fsm/DefaultStateMachine.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.fsm` -> `sge.ai.fsm`
 *   Convention: split packages; `null` -> `Nullable`; `getOwner`/`setOwner` -> `var owner`
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 132
 * Covenant-baseline-methods: DefaultStateMachine,changeState,currentState,getCurrentState,getGlobalState,getPreviousState,globalState,handleMessage,isInState,owner,previousState,revertToPreviousState,setGlobalState,setInitialState,update
 * Covenant-source-reference: auto
 * Covenant-verified: 2026-04-19
 */
package sge
package ai
package fsm

import sge.ai.msg.Telegram
import sge.utils.Nullable

/** Default implementation of the [[StateMachine]] trait.
  *
  * @tparam E
  *   the type of the entity owning this state machine
  * @tparam S
  *   the type of the states of this state machine
  * @param owner
  *   the owner of the state machine
  * @param initialState
  *   the initial state
  * @param _globalState
  *   the global state
  * @author
  *   davebaol (original implementation)
  */
class DefaultStateMachine[E, S <: State[E]](
  /** The entity that owns this state machine. */
  var owner:    E,
  initialState: Nullable[S] = Nullable.empty,
  _globalState: Nullable[S] = Nullable.empty
) extends StateMachine[E, S] {

  /** The current state the owner is in. */
  protected var currentState: Nullable[S] = Nullable.empty

  /** The last state the owner was in. */
  protected var previousState: Nullable[S] = Nullable.empty

  /** The global state of the owner. Its logic is called every time the FSM is updated. */
  protected var globalState: Nullable[S] = Nullable.empty

  // Initialize via the setter methods to match the original constructor behavior
  setInitialState(initialState)
  setGlobalState(_globalState)

  override def setInitialState(state: Nullable[S]): Unit = {
    previousState = Nullable.empty
    currentState = state
  }

  override def setGlobalState(state: Nullable[S]): Unit =
    globalState = state

  override def getCurrentState: S = currentState.get

  override def getGlobalState: S = globalState.get

  override def getPreviousState: S = previousState.get

  /** Updates the state machine by invoking first the `update` method of the global state (if any) then the `update` method of the current state.
    */
  override def update(): Unit = {
    // Execute the global state (if any)
    globalState.foreach(_.update(owner))

    // Execute the current state (if any)
    currentState.foreach(_.update(owner))
  }

  override def changeState(newState: S): Unit = {
    // Keep a record of the previous state
    previousState = currentState

    // Call the exit method of the existing state
    currentState.foreach(_.exit(owner))

    // Change state to the new state
    currentState = Nullable(newState)

    // Call the entry method of the new state
    currentState.foreach(_.enter(owner))
  }

  override def revertToPreviousState(): Boolean =
    if (previousState.isEmpty) {
      false
    } else {
      changeState(previousState.get)
      true
    }

  /** Indicates whether the state machine is in the given state.
    *
    * This implementation assumes states are singletons (typically an enum) so they are compared with reference equality instead of the `equals` method.
    *
    * @param state
    *   the state to be compared with the current state
    * @return
    *   `true` if the current state and the given state are the same object.
    */
  override def isInState(state: S): Boolean =
    currentState.exists(_.asInstanceOf[AnyRef] eq state.asInstanceOf[AnyRef])

  /** Handles received telegrams. The telegram is first routed to the current state. If the current state does not deal with the message, it's routed to the global state's message handler.
    *
    * @param telegram
    *   the received telegram
    * @return
    *   `true` if telegram has been successfully handled; `false` otherwise.
    */
  override def handleMessage(telegram: Telegram): Boolean =
    // First see if the current state is valid and that it can handle the message
    if (currentState.exists(_.onMessage(owner, telegram))) {
      true
    }
    // If not, and if a global state has been implemented, send
    // the message to the global state
    else if (globalState.exists(_.onMessage(owner, telegram))) {
      true
    } else {
      false
    }
}
