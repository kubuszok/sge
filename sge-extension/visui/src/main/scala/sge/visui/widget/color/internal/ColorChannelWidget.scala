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
package internal

import sge.scenes.scene2d.Actor
import sge.scenes.scene2d.utils.ChangeListener
import sge.utils.Nullable
import sge.visui.Sizes
import sge.visui.widget.{ VisLabel, VisTable }

/** Used to display one color channel (hue, saturation etc.) with label, ColorInputField and ChannelBar.
  * @author
  *   Kotcrab
  */
class ColorChannelWidget(commons: PickerCommons, label: String, private val mode: Int, private val maxValue: Int, listener: ChannelBar.ChannelBarListener)(using Sge)
    extends VisTable(true) {

  @scala.annotation.nowarn("msg=unused")
  private val style: ColorPickerWidgetStyle = commons.style
  private val sizes: Sizes                  = commons.sizes

  private var bar:        ChannelBar      = scala.compiletime.uninitialized
  private var barListener: ChangeListener  = scala.compiletime.uninitialized
  private var inputField: ColorInputField = scala.compiletime.uninitialized

  private var _value: Int = 0

  {
    barListener = new ChangeListener() {
      override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit = {
        _value = bar.getValue
        listener.updateFields()
        inputField.setValue(_value)
      }
    }

    add(Nullable[Actor](new VisLabel(label))).width(10 * sizes.scaleFactor).center()
    inputField = new ColorInputField(maxValue, new ColorInputField.ColorInputFieldListener {
      override def changed(newValue: Int): Unit = {
        _value = newValue
        listener.updateFields()
        bar.setValue(newValue)
      }
    })
    add(Nullable[Actor](inputField)).width(BasicColorPicker.FIELD_WIDTH * sizes.scaleFactor)
    bar = createBarImage()
    add(Nullable[Actor](bar)).size(BasicColorPicker.BAR_WIDTH * sizes.scaleFactor, BasicColorPicker.BAR_HEIGHT * sizes.scaleFactor)
    bar.channelBarListener = listener

    inputField.setValue(0)
  }

  def getValue: Int = _value

  def setValue(value: Int): Unit = {
    _value = value
    inputField.setValue(value)
    bar.setValue(value)
  }

  private def createBarImage(): ChannelBar = {
    if (mode == ChannelBar.MODE_ALPHA) new AlphaChannelBar(commons, mode, maxValue, barListener)
    else new ChannelBar(commons, mode, maxValue, barListener)
  }

  def getBar: ChannelBar = bar

  def isInputValid: Boolean = inputField.isInputValid
}
