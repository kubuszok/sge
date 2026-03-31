/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package visui
package widget
package color

import sge.graphics.Color

/** Listener for [[ColorPicker]].
  * @author
  *   Kotcrab
  */
trait ColorPickerListener {

  /** Called when color selection was canceled by user (either by clicking cancel or closing the window). Note that this event can only occur when using [[ColorPicker]] dialog.
    */
  def canceled(oldColor: Color): Unit

  /** Called when currently selected color in picker has changed. This does not mean that user finished selecting color, if you are only interested in that event use [[finished]] or [[canceled]].
    */
  def changed(newColor: Color): Unit

  /** Called when selected color in picker were reset to previously select one.
    * @param previousColor
    *   color that was set before reset.
    * @param newColor
    *   new picker color.
    */
  def reset(previousColor: Color, newColor: Color): Unit

  /** Called when user has finished selecting new color. Note that this event can only occur when using [[ColorPicker]] dialog.
    */
  def finished(newColor: Color): Unit
}
