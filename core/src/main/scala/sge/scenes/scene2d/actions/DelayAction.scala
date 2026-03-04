/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/actions/DelayAction.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: no return; split packages; braces on class
 *   Renames: implements FinishableAction -> with FinishableAction
 *   Idiom: action null-check + early return -> action.fold(true)(_.act(delta))
 *   TODO: Java-style getters/setters -- getTime/setTime, getDuration/setDuration
 *   TODO: opaque Seconds for duration/time params -- see docs/improvements/opaque-types.md
 *   Audited: 2026-03-03
 */
package sge
package scenes
package scene2d
package actions

/** Delays execution of an action or inserts a pause in a {@link SequenceAction}.
  * @author
  *   Nathan Sweet
  */
class DelayAction(private var duration: Float = 0) extends DelegateAction with FinishableAction {
  private var time: Float = 0

  override protected def delegate(delta: Float): Boolean =
    if (time < duration) {
      time += delta
      if (time < duration) false
      else {
        val actionDelta = time - duration
        time = duration
        action.forall(_.act(actionDelta))
      }
    } else {
      action.forall(_.act(delta))
    }

  def finish(): Unit = time = duration

  override def restart(): Unit = {
    super.restart()
    time = 0
  }

  def getTime: Float = time

  def setTime(time: Float): Unit = this.time = time

  def getDuration: Float = duration

  def setDuration(duration: Float): Unit = this.duration = duration
}
