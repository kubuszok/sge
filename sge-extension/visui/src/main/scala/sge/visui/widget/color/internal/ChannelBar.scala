/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 99
 * Covenant-baseline-methods: ChannelBar,ChannelBarListener,MODE_ALPHA,MODE_B,MODE_G,MODE_H,MODE_R,MODE_S,MODE_V,_channelBarListener,_value,changeEvent,channelBarListener,channelBarListener_,draw,getValue,newValue,selectorX,setShaderUniforms,setValue,sizes,style,touchDown,touchDragged,updateFields,updateValueFromTouch
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/color/internal/ChannelBar.java
 * Covenant-verified: 2026-04-19
 */
package sge
package visui
package widget
package color
package internal

import sge.Input.Button
import sge.graphics.g2d.Batch
import sge.graphics.glutils.ShaderProgram
import sge.scenes.scene2d.{ Actor, InputEvent, InputListener, Touchable }
import sge.scenes.scene2d.utils.ChangeListener
import sge.scenes.scene2d.utils.ChangeListener.ChangeEvent
import sge.utils.Nullable
import sge.visui.Sizes

/** Used to display channel color bars in color picker.
  * @author
  *   Kotcrab
  */
class ChannelBar(commons: PickerCommons, private val mode: Int, private val maxValue: Int, changeListener: ChangeListener)(using Sge)
    extends ShaderImage(commons.getBarShader(mode), commons.whiteTexture) {

  protected val style: ColorPickerWidgetStyle = commons.style
  private val sizes:   Sizes                  = commons.sizes

  private var _value:    Int   = 0
  private var selectorX: Float = 0f

  private var _channelBarListener: Nullable[ChannelBar.ChannelBarListener] = Nullable.empty

  touchable = Touchable.enabled
  setValue(_value)
  addListener(changeListener)

  addListener(
    new InputListener() {
      override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Button): Boolean = {
        updateValueFromTouch(x)
        true
      }

      override def touchDragged(event: InputEvent, x: Float, y: Float, pointer: Int): Unit =
        updateValueFromTouch(x)
    }
  )

  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    super.draw(batch, parentAlpha)
    style.barSelector.get.draw(batch, x + selectorX - style.barSelector.get.minWidth / 2, y - 1, style.barSelector.get.minWidth, style.barSelector.get.minHeight)
  }

  def setValue(newValue: Int): Unit = {
    _value = newValue
    if (_value < 0) _value = 0
    if (_value > maxValue) _value = maxValue
    selectorX = (_value.toFloat / maxValue) * BasicColorPicker.BAR_WIDTH * sizes.scaleFactor
  }

  def getValue: Int = _value

  private def updateValueFromTouch(x: Float): Unit = {
    val newValue = (x / BasicColorPicker.BAR_WIDTH * maxValue / sizes.scaleFactor).toInt
    setValue(newValue)

    val changeEvent = Actor.POOLS.obtain[ChangeEvent]
    fire(changeEvent)
    Actor.POOLS.free(changeEvent)
  }

  override protected def setShaderUniforms(shader: ShaderProgram): Unit = {
    shader.setUniformi("u_mode", mode)
    if (_channelBarListener.isDefined) _channelBarListener.get.setShaderUniforms(shader)
  }

  def channelBarListener:                                            Nullable[ChannelBar.ChannelBarListener] = _channelBarListener
  def channelBarListener_=(listener: ChannelBar.ChannelBarListener): Unit                                    = _channelBarListener = Nullable(listener)
}

object ChannelBar {
  val MODE_ALPHA: Int = 0
  val MODE_R:     Int = 1
  val MODE_G:     Int = 2
  val MODE_B:     Int = 3
  val MODE_H:     Int = 4
  val MODE_S:     Int = 5
  val MODE_V:     Int = 6

  trait ChannelBarListener {
    def updateFields():                           Unit
    def setShaderUniforms(shader: ShaderProgram): Unit
  }
}
