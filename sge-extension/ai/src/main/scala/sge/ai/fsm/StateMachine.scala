/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/fsm/StateMachine.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.fsm` -> `sge.ai.fsm`
 *   Convention: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 92
 * Covenant-baseline-methods: StateMachine,changeState,getCurrentState,getGlobalState,getPreviousState,handleMessage,isInState,revertToPreviousState,setGlobalState,setInitialState,update
 * Covenant-source-reference: com/badlogic/gdx/ai/fsm/StateMachine.java
 *   Renames: `com.badlogic.gdx.ai.fsm` -> `sge.ai.fsm`
 *   Convention: split packages
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 92
 * Covenant-baseline-methods: StateMachine,changeState,getCurrentState,getGlobalState,getPreviousState,handleMessage,isInState,revertToPreviousState,setGlobalState,setInitialState,update
 * Covenant-verified: 2026-04-19
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 6726e345248ddcad7cec0737f6ad83e4e028266d
 */
package sge
package ai
package fsm

import sge.ai.msg.Telegram
import sge.ai.msg.Telegraph
import sge.utils.Nullable

/** A state machine manages the state transitions of its entity. Additionally, the state machine may be delegated by the entity to handle its messages.
  *
  * @tparam E
  *   the type of the entity owning this state machine
  * @tparam S
  *   the type of the states of this state machine
  * @author
  *   davebaol (original implementation)
  */
trait StateMachine[E, S <: State[E]] extends Telegraph {

  /** Updates the state machine.
    *
    * Implementation classes should invoke first the `update` method of the global state (if any) then the `update` method of the current state.
    */
  def update(): Unit

  /** Performs a transition to the specified state.
    * @param newState
    *   the state to transition to
    */
  def changeState(newState: S): Unit

  /** Changes the state back to the previous state.
    * @return
    *   `true` in case there was a previous state that we were able to revert to. In case there is no previous state, no state change occurs and `false` will be returned.
    */
  def revertToPreviousState(): Boolean

  /** Sets the initial state of this state machine.
    * @param state
    *   the initial state.
    */
  def setInitialState(state: Nullable[S]): Unit

  /** Sets the global state of this state machine.
    * @param state
    *   the global state.
    */
  def setGlobalState(state: Nullable[S]): Unit

  /** Returns the current state of this state machine. */
  def getCurrentState: S

  /** Returns the global state of this state machine.
    *
    * Implementation classes should invoke the `update` method of the global state every time the FSM is updated. Also, they should never invoke its `enter` and `exit` method.
    */
  def getGlobalState: S

  /** Returns the last state of this state machine. */
  def getPreviousState: S

  /** Indicates whether the state machine is in the given state.
    * @param state
    *   the state to be compared with the current state
    * @return
    *   `true` if the current state's type is equal to the type of the class passed as a parameter.
    */
  def isInState(state: S): Boolean

  /** Handles received telegrams.
    *
    * Implementation classes should first route the telegram to the current state. If the current state does not deal with the message, it should be routed to the global state.
    *
    * @param telegram
    *   the received telegram
    * @return
    *   `true` if telegram has been successfully handled; `false` otherwise.
    */
  override def handleMessage(telegram: Telegram): Boolean
}
