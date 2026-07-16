/*
 * SGE Gauntlet — ext/ai: DefaultStateMachine transition sequence.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gauntlet
package probes

import sge.ai.fsm.{ DefaultStateMachine, State }
import sge.ai.msg.Telegram
import lowlevel.Nullable

import scala.collection.mutable.ListBuffer

/** Drives a DefaultStateMachine through update / changeState / revertToPreviousState and asserts the exact enter/update/exit call sequence. */
object AiStateMachineProbe extends FeatureProbe {

  final private class Bot {
    val journal: ListBuffer[String] = ListBuffer.empty[String]
  }

  sealed abstract private class BotState(name: String) extends State[Bot] {
    override def enter(entity:     Bot):                     Unit    = entity.journal += s"$name-enter"
    override def update(entity:    Bot):                     Unit    = entity.journal += s"$name-update"
    override def exit(entity:      Bot):                     Unit    = entity.journal += s"$name-exit"
    override def onMessage(entity: Bot, telegram: Telegram): Boolean = false
  }

  private object IdleState extends BotState("idle")
  private object ActiveState extends BotState("active")

  override def id: String = "ext/ai-statemachine"

  override def area: String = "ext/ai"

  override def requiresGpu: Boolean = false

  override def frames: Int = 1

  private val checks = ListBuffer.empty[Check]

  override def init(ctx: ProbeContext): Unit =
    checks.clear()

  override def render(ctx: ProbeContext, frame: Int): Unit = {
    val bot = new Bot
    val initial: State[Bot] = IdleState
    // Nullable's Impl is invariant, so the ctor's `Nullable.empty` defaults do not
    // infer for a concrete S — pass all three arguments explicitly (like the ai suite does).
    val fsm = new DefaultStateMachine[Bot, State[Bot]](bot, Nullable(initial), Nullable.empty[State[Bot]])

    checks += Check.eq("initially-in-idle", true, fsm.isInState(IdleState))

    fsm.update()
    fsm.changeState(ActiveState)
    fsm.update()
    val revertResult = fsm.revertToPreviousState()

    checks += Check.eq("revert-returns-true", true, revertResult)
    checks += Check.eq("back-in-idle-after-revert", true, fsm.isInState(IdleState))
    checks += Check.eq(
      "transition-sequence",
      List("idle-update", "idle-exit", "active-enter", "active-update", "active-exit", "idle-enter"),
      bot.journal.toList
    )
  }

  override def verify(ctx: ProbeContext): List[Check] =
    checks.toList
}
