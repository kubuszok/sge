/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/ui/TooltipManager.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: Singleton pattern adapted with Nullable; Gdx.files check -> _defaults.isEmpty + _creatingDefaults guard
 *   Idiom: split packages
 *   Convention: opaque Seconds for initialTime, subsequentTime, resetTime
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 178
 * Covenant-baseline-methods: TooltipManager,actionTime,animations,edgeDistance,enabled,enter,files,hide,hideAction,hideAll,initialTime,instance,instant,maxWidth,offsetX,offsetY,resetTask,resetTime,run,showAction,showTask,showTooltip,shown,subsequentTime,time,touchDown
 * Covenant-source-reference: com/badlogic/gdx/scenes/scene2d/ui/TooltipManager.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 10478a75550b82e13cc2501eda20512a85f8c840
 */
package sge
package scenes
package scene2d
package ui

import sge.math.Interpolation
import sge.scenes.scene2d.actions.Actions
import sge.utils.{ DynamicArray, Nullable, Seconds, Timer }

/** Keeps track of an application's tooltips.
  * @author
  *   Nathan Sweet
  */
class TooltipManager()(using Sge) {

  /** Seconds from when an actor is hovered to when the tooltip is shown. Default is 2. Call {@link #hideAll()} after changing to reset internal state.
    */
  var initialTime: Seconds = Seconds(2)

  /** Once a tooltip is shown, this is used instead of {@link #initialTime}. Default is 0. */
  var subsequentTime: Seconds = Seconds.zero

  /** Seconds to use {@link #subsequentTime}. Default is 1.5. */
  var resetTime: Seconds = Seconds(1.5f)

  /** If false, tooltips will not be shown. Default is true. */
  var enabled: Boolean = true

  /** If false, tooltips will be shown without animations. Default is true. */
  var animations: Boolean = true

  /** The maximum width of a {@link TextTooltip}. The label will wrap if needed. Default is Integer.MAX_VALUE. */
  var maxWidth: Float = Int.MaxValue.toFloat

  /** The distance from the mouse position to offset the tooltip actor. Default is 15,19. */
  var offsetX: Float = 15
  var offsetY: Float = 19

  /** The distance from the tooltip actor position to the edge of the screen where the actor will be shown on the other side of the mouse cursor. Default is 7.
    */
  var edgeDistance: Float = 7

  val shown: DynamicArray[Tooltip[?]] = DynamicArray[Tooltip[?]]()

  var time: Seconds = initialTime

  val resetTask: Timer.Task = new Timer.Task() {
    def run(): Unit =
      time = initialTime
  }

  var showTooltip: Nullable[Tooltip[?]] = Nullable.empty

  val showTask: Timer.Task = new Timer.Task() {
    def run(): Unit =
      showTooltip.foreach { tooltip =>
        tooltip.targetActor.foreach { ta =>
          ta.stage.foreach { stage =>
            stage.addActor(tooltip.container)
            tooltip.container.toFront()
            shown.add(tooltip)

            tooltip.container.clearActions()
            showAction(tooltip)

            if (!tooltip.instant) {
              time = subsequentTime
              resetTask.cancel()
            }
          }
        }
      }
  }

  def touchDown(tooltip: Tooltip[?]): Unit = {
    showTask.cancel()
    if (tooltip.container.remove()) resetTask.cancel()
    resetTask.run()
    if (enabled || tooltip.always) {
      showTooltip = Nullable(tooltip)
      Timer.schedule(showTask, time)
    }
  }

  def enter(tooltip: Tooltip[?]): Unit = {
    showTooltip = Nullable(tooltip)
    showTask.cancel()
    if (enabled || tooltip.always) {
      if (time == Seconds.zero || tooltip.instant)
        showTask.run()
      else
        Timer.schedule(showTask, time)
    }
  }

  def hide(tooltip: Tooltip[?]): Unit = {
    showTooltip = Nullable.empty
    showTask.cancel()
    if (tooltip.container.hasParent) {
      shown.removeValue(tooltip)
      hideAction(tooltip)
      resetTask.cancel()
      Timer.schedule(resetTask, resetTime)
    }
  }

  /** Called when tooltip is shown. Default implementation sets actions to animate showing. */
  protected def showAction(tooltip: Tooltip[?]): Unit = {
    val actionTime = Seconds(if (animations) if (time > Seconds.zero) 0.5f else 0.15f else 0.1f)
    tooltip.container.transform = true
    tooltip.container.color.a = 0.2f
    tooltip.container.setScale(0.05f)
    tooltip.container.addAction(
      Actions.parallel(
        Actions.fadeIn(actionTime, Nullable(Interpolation.fade)),
        Actions.scaleTo(1, 1, actionTime, Nullable(Interpolation.fade))
      )
    )
  }

  /** Called when tooltip is hidden. Default implementation sets actions to animate hiding and to remove the actor from the stage when the actions are complete. A subclass must at least remove the
    * actor.
    */
  protected def hideAction(tooltip: Tooltip[?]): Unit =
    tooltip.container.addAction(
      Actions.sequence(
        Actions.parallel(
          Actions.alpha(0.2f, Seconds(0.2f), Nullable(Interpolation.fade)),
          Actions.scaleTo(0.05f, 0.05f, Seconds(0.2f), Nullable(Interpolation.fade))
        ),
        Actions.removeActor()
      )
    )

  def hideAll(): Unit = {
    resetTask.cancel()
    showTask.cancel()
    time = initialTime
    showTooltip = Nullable.empty

    shown.foreach(_.hide())
    shown.clear()
  }

  /** Shows all tooltips on hover without a delay for {@link #resetTime} seconds. */
  def instant(): Unit = {
    time = Seconds.zero
    showTask.run()
    showTask.cancel()
  }
}

object TooltipManager {
  private var instance: Nullable[TooltipManager] = Nullable.empty
  private var files:    Nullable[sge.Files]      = Nullable.empty

  def instance(using Sge): TooltipManager = {
    if (files.isEmpty || files.forall(_ ne Sge().files)) {
      files = Nullable(Sge().files)
      instance = Nullable(TooltipManager())
    }
    instance.getOrElse(throw new IllegalStateException("TooltipManager instance not initialized"))
  }
}
