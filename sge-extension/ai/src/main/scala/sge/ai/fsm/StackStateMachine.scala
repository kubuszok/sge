/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/fsm/StackStateMachine.java
 * Original authors: Daniel Holderbaum
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.fsm` -> `sge.ai.fsm`; `com.badlogic.gdx.utils.Array` -> `sge.utils.DynamicArray`
 *   Convention: split packages; `null` -> `Nullable`
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 91
 * Covenant-baseline-methods: StackStateMachine,changeState,getCurrentState,getPreviousState,revertToPreviousState,setInitialState,stateStack
 * Covenant-source-reference: auto
 * Covenant-verified: 2026-04-19
 */
package sge
package ai
package fsm

import sge.utils.DynamicArray
import sge.utils.Nullable

/** A [[StateMachine]] implementation that keeps track of all previous [[State]]s via a stack. This makes sense for example in case of a hierarchical menu structure where each menu screen is one state
  * and one wants to navigate back to the main menu anytime, via [[revertToPreviousState]].
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
  *   Daniel Holderbaum (original implementation)
  */
class StackStateMachine[E, S <: State[E]: scala.reflect.ClassTag](
  owner:        E,
  initialState: Nullable[S] = Nullable.empty,
  _globalState: Nullable[S] = Nullable.empty
) extends DefaultStateMachine[E, S](owner, initialState, _globalState) {

  private var stateStack: DynamicArray[S] = DynamicArray[S]()

  override def setInitialState(state: Nullable[S]): Unit = {
    stateStack = DynamicArray[S]()
    stateStack.clear()
    currentState = state
  }

  override def getCurrentState: S = currentState.get

  /** Returns the last state of this state machine. That is the high-most state on the internal stack of previous states.
    */
  override def getPreviousState: S =
    if (stateStack.size == 0) {
      throw new NullPointerException("No previous state on the stack")
    } else {
      stateStack.peek
    }

  override def changeState(newState: S): Unit =
    changeState(newState, pushCurrentStateToStack = true)

  /** Changes the state back to the previous state. That is the high-most state on the internal stack of previous states.
    * @return
    *   `true` in case there was a previous state that we were able to revert to. In case there is no previous state, no state change occurs and `false` will be returned.
    */
  override def revertToPreviousState(): Boolean =
    if (stateStack.size == 0) {
      false
    } else {
      val previous = stateStack.pop()
      changeState(previous, pushCurrentStateToStack = false)
      true
    }

  private def changeState(newState: S, pushCurrentStateToStack: Boolean): Unit = {
    if (pushCurrentStateToStack) {
      currentState.foreach(stateStack.add)
    }

    // Call the exit method of the existing state
    currentState.foreach(_.exit(owner))

    // Change state to the new state
    currentState = Nullable(newState)

    // Call the entry method of the new state
    currentState.foreach(_.enter(owner))
  }
}
