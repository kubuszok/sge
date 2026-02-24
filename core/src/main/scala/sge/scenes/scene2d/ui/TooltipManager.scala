/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/ui/TooltipManager.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package scenes
package scene2d
package ui

import sge.utils.Nullable

// TODO: uncomment when Tooltip, Container, and Actions are fully integrated
// import sge.math.Interpolation
// import sge.scenes.scene2d.actions.Actions._
// import sge.utils.Timer

/** Keeps track of an application's tooltips.
  * @author
  *   Nathan Sweet
  */
class TooltipManager()(using sge: Sge) {
  import TooltipManager._

  /** Seconds from when an actor is hovered to when the tooltip is shown. Default is 2. Call {@link #hideAll()} after changing to reset internal state.
    */
  var initialTime: Float = 2

  /** Once a tooltip is shown, this is used instead of {@link #initialTime}. Default is 0. */
  var subsequentTime: Float = 0

  /** Seconds to use {@link #subsequentTime}. Default is 1.5. */
  var resetTime: Float = 1.5f

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

  // TODO: uncomment when Tooltip is ported
  // val shown: ArrayBuffer[Tooltip[?]] = ArrayBuffer.empty

  var time: Float = initialTime

  // TODO: uncomment when Timer.Task is integrated
  // val resetTask: Timer.Task = new Timer.Task() {
  //   def run(): Unit = {
  //     time = initialTime
  //   }
  // }

  // TODO: uncomment when Tooltip is ported
  // var showTooltip: Nullable[Tooltip[?]] = Nullable.empty

  // TODO: uncomment when Timer.Task is integrated
  // val showTask: Timer.Task = new Timer.Task() {
  //   def run(): Unit = {
  //     // ... showTooltip logic
  //   }
  // }

  // TODO: uncomment when Tooltip is ported
  // def touchDown(tooltip: Tooltip[?]): Unit = { ... }

  // TODO: uncomment when Tooltip is ported
  // def enter(tooltip: Tooltip[?]): Unit = { ... }

  // TODO: uncomment when Tooltip is ported
  // def hide(tooltip: Tooltip[?]): Unit = { ... }

  // /** Called when tooltip is shown. Default implementation sets actions to animate showing. */
  // TODO: uncomment when Tooltip, Container, and Actions are ported
  // protected def showAction(tooltip: Tooltip[?]): Unit = { ... }

  // /** Called when tooltip is hidden. Default implementation sets actions to animate hiding and to remove the actor from the stage
  //   * when the actions are complete. A subclass must at least remove the actor. */
  // TODO: uncomment when Tooltip, Container, and Actions are ported
  // protected def hideAction(tooltip: Tooltip[?]): Unit = { ... }

  // TODO: uncomment when Tooltip is ported
  // def hideAll(): Unit = { ... }

  // /** Shows all tooltips on hover without a delay for {@link #resetTime} seconds. */
  // TODO: uncomment when Timer.Task is integrated
  // def instant(): Unit = { ... }
}

object TooltipManager {
  private var instance: Nullable[TooltipManager] = Nullable.empty
  private var files:    Nullable[sge.Files]      = Nullable.empty

  def getInstance()(using sge: Sge): TooltipManager = {
    if (files.isEmpty || files.fold(true)(_ ne sge.files)) {
      files = Nullable(sge.files)
      instance = Nullable(new TooltipManager())
    }
    instance.getOrElse(throw new IllegalStateException("TooltipManager instance not initialized"))
  }
}
