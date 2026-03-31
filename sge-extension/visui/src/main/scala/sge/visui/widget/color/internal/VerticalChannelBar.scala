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

import sge.Input.Button
import sge.graphics.g2d.Batch
import sge.scenes.scene2d.{ Actor, InputEvent, InputListener, Touchable }
import sge.scenes.scene2d.utils.ChangeListener
import sge.scenes.scene2d.utils.ChangeListener.ChangeEvent
import sge.visui.Sizes

/** Vertical channel bar is used to display vertical hue bar.
  * @author
  *   Kotcrab
  */
class VerticalChannelBar(commons: PickerCommons, private val maxValue: Int, listener: ChangeListener)(using Sge) extends ShaderImage(commons.verticalChannelShader, commons.whiteTexture) {

  private val style: ColorPickerWidgetStyle = commons.style
  private val sizes: Sizes                  = commons.sizes

  private var selectorY: Float = 0f
  private var _value:    Int   = 0

  touchable = Touchable.enabled
  setValue(0)
  addListener(listener)

  addListener(
    new InputListener() {
      override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Button): Boolean = {
        updateValueFromTouch(y)
        true
      }

      override def touchDragged(event: InputEvent, x: Float, y: Float, pointer: Int): Unit =
        updateValueFromTouch(y)
    }
  )

  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    super.draw(batch, parentAlpha)
    style.verticalSelector.get.draw(batch, x, y + imageY + selectorY - 2.5f, imageWidth, style.verticalSelector.get.minHeight)
  }

  def setValue(newValue: Int): Unit = {
    _value = newValue
    if (_value < 0) _value = 0
    if (_value > maxValue) _value = maxValue
    selectorY = (_value.toFloat / maxValue) * BasicColorPicker.PALETTE_SIZE * sizes.scaleFactor
  }

  private def updateValueFromTouch(y: Float): Unit = {
    val newValue = (y / BasicColorPicker.PALETTE_SIZE * maxValue / sizes.scaleFactor).toInt
    setValue(newValue)

    val changeEvent = Actor.POOLS.obtain[ChangeEvent]
    fire(changeEvent)
    Actor.POOLS.free(changeEvent)
  }

  def getValue: Int = _value
}
