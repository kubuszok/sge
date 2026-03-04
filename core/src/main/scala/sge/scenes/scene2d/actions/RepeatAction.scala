/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/RepeatAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: no return; split packages; braces on class
 *   Renames: static FOREVER -> companion object val; implements FinishableAction -> with
 *   Idiom: action.act(delta) with multiple returns -> action.fold(true) with nested if/else
 *   TODO: Java-style getters/setters -- getCount/setCount
 *   Audited: 2026-03-03
 */
package sge
package scenes
package scene2d
package actions

/** Repeats an action a number of times or forever.
  * @author
  *   Nathan Sweet
  */
class RepeatAction extends DelegateAction with FinishableAction {
  private var repeatCount:   Int     = 0
  private var executedCount: Int     = 0
  private var finished:      Boolean = false

  override protected def delegate(delta: Float): Boolean =
    if (executedCount == repeatCount) true
    else {
      action.fold(true) { a =>
        if (a.act(delta)) {
          if (finished) true
          else {
            if (repeatCount > 0) executedCount += 1
            if (executedCount == repeatCount) true
            else {
              a.restart()
              false
            }
          }
        } else false
      }
    }

  def finish(): Unit = finished = true

  override def restart(): Unit = {
    super.restart()
    executedCount = 0
    finished = false
  }

  def setCount(count: Int): Unit = this.repeatCount = count

  def getCount: Int = repeatCount
}

object RepeatAction {
  val FOREVER: Int = -1
}
