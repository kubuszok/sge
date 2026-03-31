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

import sge.scenes.scene2d.ui.Window.WindowStyle
import sge.utils.Nullable

/** @author Kotcrab */
class ColorPickerStyle() extends WindowStyle {
  var pickerStyle: Nullable[ColorPickerWidgetStyle] = Nullable.empty

  def this(style: ColorPickerStyle) = {
    this()
    this.titleFont = style.titleFont
    this.titleFontColor = style.titleFontColor.map(c => sge.graphics.Color(c))
    this.background = style.background
    this.stageBackground = style.stageBackground
    if (style.pickerStyle.isDefined) pickerStyle = Nullable(new ColorPickerWidgetStyle(style.pickerStyle.get))
  }
}
