/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 35
 * Covenant-baseline-methods: ColorPickerWidgetStyle,barSelector,cross,horizontalSelector,iconArrowRight,this,verticalSelector
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/color/ColorPickerWidgetStyle.java
 * Covenant-verified: 2026-04-19
 */
package sge
package visui
package widget
package color

import sge.scenes.scene2d.utils.Drawable
import sge.utils.Nullable

/** Style used by [[ExtendedColorPicker]] and [[BasicColorPicker]]. [[ColorPicker]] dialog is using [[ColorPickerStyle]].
  * @author
  *   Kotcrab
  */
class ColorPickerWidgetStyle() {
  var barSelector:        Nullable[Drawable] = Nullable.empty
  var cross:              Nullable[Drawable] = Nullable.empty
  var verticalSelector:   Nullable[Drawable] = Nullable.empty
  var horizontalSelector: Nullable[Drawable] = Nullable.empty
  var iconArrowRight:     Nullable[Drawable] = Nullable.empty

  def this(other: ColorPickerWidgetStyle) = {
    this()
    barSelector = other.barSelector
    cross = other.cross
    verticalSelector = other.verticalSelector
    horizontalSelector = other.horizontalSelector
    iconArrowRight = other.iconArrowRight
  }
}
