/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/fsm/State.java
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
 * Covenant-baseline-loc: 55
 * Covenant-baseline-methods: State,enter,exit,onMessage,update
 * Covenant-source-reference: com/badlogic/gdx/ai/fsm/State.java
 *   Renames: `com.badlogic.gdx.ai.fsm` -> `sge.ai.fsm`
 *   Convention: split packages
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 55
 * Covenant-baseline-methods: State,enter,exit,onMessage,update
 * Covenant-verified: 2026-04-19
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 6726e345248ddcad7cec0737f6ad83e4e028266d
 */
package sge
package ai
package fsm

import sge.ai.msg.Telegram

/** The state of a state machine defines the logic of the entities that enter, exit and last this state. Additionally, a state may be delegated by an entity to handle its messages.
  *
  * @tparam E
  *   the type of the entity handled by this state machine
  * @author
  *   davebaol (original implementation)
  */
trait State[E] {

  /** This method will execute when the state is entered.
    * @param entity
    *   the entity entering the state
    */
  def enter(entity: E): Unit

  /** This is the state's normal update function.
    * @param entity
    *   the entity lasting the state
    */
  def update(entity: E): Unit

  /** This method will execute when the state is exited.
    * @param entity
    *   the entity exiting the state
    */
  def exit(entity: E): Unit

  /** This method executes if the `entity` receives a `telegram` from the message dispatcher while it is in this state.
    * @param entity
    *   the entity that received the message
    * @param telegram
    *   the message sent to the entity
    * @return
    *   `true` if the message has been successfully handled; `false` otherwise.
    */
  def onMessage(entity: E, telegram: Telegram): Boolean
}
