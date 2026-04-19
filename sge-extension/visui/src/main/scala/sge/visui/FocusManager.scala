/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 68
 * Covenant-baseline-methods: FocusManager,focusedWidget,getFocusedWidget,resetFocus,switchFocus
 * Covenant-source-reference: com/kotcrab/vis/ui/FocusManager.java
 * Covenant-verified: 2026-04-19
 */
package sge
package visui

import sge.scenes.scene2d.{ Actor, Stage }
import sge.utils.Nullable

/** Manages focus of VisUI components. This is different from scene2d.ui focus management. In scene2d widgets can only acquire keyboard and scroll focus. VisUI focus manager allows any widget to
  * acquire general user focus, this is used mainly to manage rendering focus borders around widgets. Generally there is no need to call these methods manually.
  * @author
  *   Kotcrab
  * @see
  *   [[Focusable]]
  */
object FocusManager {
  private var focusedWidget: Nullable[Focusable] = Nullable.empty

  /** Takes focus from current focused widget (if any), and sets focus to provided widget.
    * @param stage
    *   if passed stage is not null then stage keyboard focus will be set to null
    * @param widget
    *   that will acquire focus
    */
  def switchFocus(stage: Nullable[Stage], widget: Focusable): Unit =
    if (focusedWidget.exists(_ eq widget)) {
      // already focused
    } else {
      focusedWidget.foreach(_.focusLost())
      focusedWidget = Nullable.empty
      stage.foreach(_.setKeyboardFocus(Nullable.empty))
      focusedWidget = Nullable(widget)
      widget.focusGained()
    }

  /** Takes focus from current focused widget (if any), and sets current focused widget to null. If widget owns keyboard focus [[resetFocus(Stage, Actor)]] should be always preferred.
    * @param stage
    *   if passed stage is not null then stage keyboard focus will be set to null
    */
  def resetFocus(stage: Nullable[Stage]): Unit = {
    focusedWidget.foreach(_.focusLost())
    stage.foreach(_.setKeyboardFocus(Nullable.empty))
    focusedWidget = Nullable.empty
  }

  /** Takes focus from current focused widget (if any), and sets current focused widget to null.
    * @param stage
    *   if passed stage is not null then stage keyboard focus will be set to null only if current focus owner is passed actor
    * @param caller
    *   the actor to compare keyboard focus against
    */
  def resetFocus(stage: Nullable[Stage], caller: Actor): Unit = {
    focusedWidget.foreach(_.focusLost())
    stage.foreach { s =>
      s.keyboardFocus.foreach { kf =>
        if (kf eq caller) s.setKeyboardFocus(Nullable.empty)
      }
    }
    focusedWidget = Nullable.empty
  }

  def getFocusedWidget: Nullable[Focusable] = focusedWidget
}
