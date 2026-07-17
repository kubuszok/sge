package sge
package ai
package fsm

import lowlevel.Nullable

/** Coverage suite for ISS-843 (wave 2026-07-18-G, territory G2).
  *
  * The wave-F mutation `getPreviousState := currentState` (a field-swap in DefaultStateMachine's accessors) survived the entire pre-existing ai suite because no test asserted the STATEFUL VALUES
  * returned by `getCurrentState` / `getPreviousState` / `getGlobalState` — the existing StateMachineSuite only observes State enter/exit/update event ordering, never the identity of the states the
  * getters return.
  *
  * This suite pins those accessors against DefaultStateMachine.java:90-103 (each getter returns its own field verbatim) and DefaultStateMachine.java:105-116 (`changeState` records `previousState =
  * currentState` before transitioning) plus the `revertToPreviousState` round-trip (DefaultStateMachine.java:118-124).
  *
  * This is GREEN coverage: current behavior is correct, so these assertions pass today. Identity (`eq`) assertions per the Nullable[S] accessor signatures landed in wave-F commit 40c4aca7. Verified
  * to go RED under a local re-application of the `getPreviousState := currentState` field-swap mutation.
  */
class StateMachineStateAccessorsIss843CoverageSuite extends munit.FunSuite {

  test("ISS-843: getCurrentState/getPreviousState/getGlobalState return the EXACT expected instance at each step") {
    val stateA = new TrackingState()
    val stateB = new TrackingState()
    val global = new TrackingState()

    val fsm = new DefaultStateMachine[String, TrackingState](
      "hero",
      Nullable.empty[TrackingState],
      Nullable.empty[TrackingState]
    )

    fsm.setGlobalState(Nullable(global))

    // Before any transition: no current, no previous, global set.
    assert(fsm.getCurrentState.isEmpty, "getCurrentState must be empty before any changeState")
    assert(fsm.getPreviousState.isEmpty, "getPreviousState must be empty before any changeState")
    assert(fsm.getGlobalState.get eq global, "getGlobalState must return exactly the state passed to setGlobalState")

    // First transition: current == A, previous stays empty (it was empty before).
    fsm.changeState(stateA)
    assert(fsm.getCurrentState.get eq stateA, "getCurrentState must return exactly stateA after changeState(stateA)")
    assert(fsm.getPreviousState.isEmpty, "getPreviousState must remain empty: the state before stateA was empty")
    assert(fsm.getGlobalState.get eq global, "getGlobalState must be unaffected by changeState")

    // Second transition: current == B, previous == A.
    fsm.changeState(stateB)
    assert(fsm.getCurrentState.get eq stateB, "getCurrentState must return exactly stateB after changeState(stateB)")
    assert(fsm.getPreviousState.get eq stateA, "getPreviousState must return exactly stateA (the state current held before)")
    assert(fsm.getGlobalState.get eq global, "getGlobalState must be unaffected by changeState")

    // Revert round-trip: current <- previous (A), previous <- current-before-revert (B).
    val reverted = fsm.revertToPreviousState()
    assert(reverted, "revertToPreviousState must return true when a previous state exists")
    assert(fsm.getCurrentState.get eq stateA, "after revert, getCurrentState must return exactly stateA")
    assert(fsm.getPreviousState.get eq stateB, "after revert, getPreviousState must return exactly stateB")
    assert(fsm.getGlobalState.get eq global, "getGlobalState must be unaffected by revert")
  }
}
