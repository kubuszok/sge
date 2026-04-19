/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 104
 * Covenant-baseline-methods: Palette,_pickerHue,changeEvent,draw,getS,getV,newS,newV,selectorX,selectorY,setPickerHue,setShaderUniforms,setValue,sizes,style,touchDown,touchDragged,updateValueFromTouch,xV,yS
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/color/internal/Palette.java
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
import sge.visui.Sizes

/** Colors palette used to display colors using all possible values of saturation and value.
  * @author
  *   Kotcrab
  */
class Palette(commons: PickerCommons, private val maxValue: Int, listener: ChangeListener)(using Sge) extends ShaderImage(commons.paletteShader, commons.whiteTexture) {

  private val style: ColorPickerWidgetStyle = commons.style
  private val sizes: Sizes                  = commons.sizes

  private var xV: Int = 0
  private var yS: Int = 0

  private var selectorX: Float = 0f
  private var selectorY: Float = 0f

  private var _pickerHue: Float = 0f

  touchable = Touchable.enabled
  setValue(0, 0)
  addListener(listener)

  addListener(
    new InputListener() {
      override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Button): Boolean = {
        updateValueFromTouch(x, y)
        true
      }

      override def touchDragged(event: InputEvent, x: Float, y: Float, pointer: Int): Unit =
        updateValueFromTouch(x, y)
    }
  )

  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    super.draw(batch, parentAlpha)

    style.verticalSelector.get.draw(batch, x, y + selectorY - style.verticalSelector.get.minHeight / 2 + 0.1f, imageWidth, style.verticalSelector.get.minHeight)

    style.horizontalSelector.get.draw(batch, x + selectorX - style.horizontalSelector.get.minWidth / 2 + 0.1f, y, style.horizontalSelector.get.minWidth, imageHeight)

    style.cross.get.draw(
      batch,
      x + selectorX - style.cross.get.minWidth / 2 + 0.1f,
      y + selectorY - style.cross.get.minHeight / 2 + 0.1f,
      style.cross.get.minWidth,
      style.cross.get.minHeight
    )
  }

  override protected def setShaderUniforms(shader: ShaderProgram): Unit =
    shader.setUniformf("u_h", _pickerHue)

  def setPickerHue(pickerHue: Int): Unit =
    _pickerHue = pickerHue / 360.0f

  def setValue(s: Int, v: Int): Unit = {
    xV = v
    yS = s

    if (xV < 0) xV = 0
    if (xV > maxValue) xV = maxValue

    if (yS < 0) yS = 0
    if (yS > maxValue) yS = maxValue

    selectorX = (xV.toFloat / maxValue) * BasicColorPicker.PALETTE_SIZE * sizes.scaleFactor
    selectorY = (yS.toFloat / maxValue) * BasicColorPicker.PALETTE_SIZE * sizes.scaleFactor
  }

  private def updateValueFromTouch(touchX: Float, touchY: Float): Unit = {
    val newV = (touchX / BasicColorPicker.PALETTE_SIZE * maxValue / sizes.scaleFactor).toInt
    val newS = (touchY / BasicColorPicker.PALETTE_SIZE * maxValue / sizes.scaleFactor).toInt

    setValue(newS, newV)

    val changeEvent = Actor.POOLS.obtain[ChangeEvent]
    fire(changeEvent)
    Actor.POOLS.free(changeEvent)
  }

  def getV: Int = xV
  def getS: Int = yS
}
