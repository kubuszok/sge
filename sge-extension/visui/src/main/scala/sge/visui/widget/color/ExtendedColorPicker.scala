/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 240
 * Covenant-baseline-methods: AlphaChannelBarListener,ExtendedColorPicker,HsvChannelBarListener,RgbChannelBarListener,aBar,allowAlphaEdit_,b,bBar,ca,cb,cg,ch,cr,createColorWidgets,createUI,cs,cv,extendedTable,g,gBar,h,hBar,hsv,r,rBar,rgbListener,s,sBar,setShaderUniforms,svListener,this,updateFields,updateLinkedWidget,updateValuesFromCurrentColor,updateValuesFromHSVFields,updateValuesFromRGBFields,v,vBar
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/color/ExtendedColorPicker.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
 */
package sge
package visui
package widget
package color

import sge.graphics.glutils.ShaderProgram
import sge.math.MathUtils
import sge.utils.Nullable
import sge.visui.VisUI
import sge.visui.util.ColorUtils
import sge.visui.widget.VisTable
import sge.visui.widget.color.internal.{ ChannelBar, ColorChannelWidget, Palette, VerticalChannelBar }

/** Color Picker widget, allows user to select color. ColorPicker is relatively heavy widget and should be reused if possible. Unlike other widgets, this one must be disposed when no longer needed!
  *
  * Extends [[BasicColorPicker]] functionality and additionally provides separate bars for H, S, V color components. Additional 3 bars (R, G, B) for selecting colors using RGB systems and dedicated
  * bar to alpha channel. Alpha edition is enabled by default.
  *
  * Used directly by [[ColorPicker]] dialog.
  * @author
  *   Kotcrab
  * @see
  *   [[BasicColorPicker]]
  * @see
  *   [[ColorPicker]]
  * @since 0.9.3
  */
class ExtendedColorPicker(initStyle: ColorPickerWidgetStyle, initListener: Nullable[ColorPickerListener])(using Sge) extends BasicColorPicker(initStyle, initListener, true) {

  private var hBar: ColorChannelWidget = scala.compiletime.uninitialized
  private var sBar: ColorChannelWidget = scala.compiletime.uninitialized
  private var vBar: ColorChannelWidget = scala.compiletime.uninitialized

  private var rBar: ColorChannelWidget = scala.compiletime.uninitialized
  private var gBar: ColorChannelWidget = scala.compiletime.uninitialized
  private var bBar: ColorChannelWidget = scala.compiletime.uninitialized

  private var aBar: ColorChannelWidget = scala.compiletime.uninitialized

  allowAlphaEdit = true

  def this()(using Sge) = this(VisUI.getSkin.get(classOf[ColorPickerWidgetStyle]), Nullable.empty)

  def this(listener: ColorPickerListener)(using Sge) = this(VisUI.getSkin.get(classOf[ColorPickerWidgetStyle]), Nullable(listener))

  def this(styleName: String, listener: Nullable[ColorPickerListener])(using Sge) =
    this(VisUI.getSkin.get(styleName, classOf[ColorPickerWidgetStyle]), listener)

  override protected def createUI(): Unit = {
    super.createUI()

    val extendedTable = new VisTable(true) // displayed next to mainTable

    extendedTable.add(Nullable[sge.scenes.scene2d.Actor](hBar)).row()
    extendedTable.add(Nullable[sge.scenes.scene2d.Actor](sBar)).row()
    extendedTable.add(Nullable[sge.scenes.scene2d.Actor](vBar)).row()

    extendedTable.add()
    extendedTable.row()

    extendedTable.add(Nullable[sge.scenes.scene2d.Actor](rBar)).row()
    extendedTable.add(Nullable[sge.scenes.scene2d.Actor](gBar)).row()
    extendedTable.add(Nullable[sge.scenes.scene2d.Actor](bBar)).row()

    extendedTable.add()
    extendedTable.row()

    extendedTable.add(Nullable[sge.scenes.scene2d.Actor](aBar)).row()

    add(Nullable[sge.scenes.scene2d.Actor](extendedTable)).expand().left().top().pad(0, 9, 4, 4)
  }

  override protected def createColorWidgets(): Unit = {
    palette = new Palette(
      commons,
      100,
      new PickerChangeListener() {
        override protected def updateLinkedWidget(): Unit = {
          sBar.setValue(palette.getS)
          vBar.setValue(palette.getV)
        }
      }
    )

    verticalBar = new VerticalChannelBar(
      commons,
      360,
      new PickerChangeListener() {
        override protected def updateLinkedWidget(): Unit =
          hBar.setValue(verticalBar.getValue)
      }
    )

    val svListener = new HsvChannelBarListener() {
      override protected def updateLinkedWidget(): Unit =
        palette.setValue(sBar.getValue, vBar.getValue)
    }

    hBar = new ColorChannelWidget(
      commons,
      "H",
      ChannelBar.MODE_H,
      360,
      new HsvChannelBarListener() {
        override protected def updateLinkedWidget(): Unit =
          verticalBar.setValue(hBar.getValue)
      }
    )

    sBar = new ColorChannelWidget(commons, "S", ChannelBar.MODE_S, 100, svListener)
    vBar = new ColorChannelWidget(commons, "V", ChannelBar.MODE_V, 100, svListener)

    val rgbListener = new RgbChannelBarListener()
    rBar = new ColorChannelWidget(commons, "R", ChannelBar.MODE_R, 255, rgbListener)
    gBar = new ColorChannelWidget(commons, "G", ChannelBar.MODE_G, 255, rgbListener)
    bBar = new ColorChannelWidget(commons, "B", ChannelBar.MODE_B, 255, rgbListener)

    aBar = new ColorChannelWidget(commons, "A", ChannelBar.MODE_ALPHA, 255, new AlphaChannelBarListener())
  }

  override def allowAlphaEdit_=(allowAlphaEdit: Boolean): Unit = {
    aBar.visible = allowAlphaEdit
    super.allowAlphaEdit_=(allowAlphaEdit)
  }

  override protected def updateValuesFromCurrentColor(): Unit = {
    val hsv = ColorUtils.RGBtoHSV(pickerColor)
    val ch  = hsv(0)
    val cs  = hsv(1)
    val cv  = hsv(2)

    val cr = MathUtils.round(pickerColor.r * 255.0f)
    val cg = MathUtils.round(pickerColor.g * 255.0f)
    val cb = MathUtils.round(pickerColor.b * 255.0f)
    val ca = MathUtils.round(pickerColor.a * 255.0f)

    hBar.setValue(ch)
    sBar.setValue(cs)
    vBar.setValue(cv)

    rBar.setValue(cr)
    gBar.setValue(cg)
    bBar.setValue(cb)

    aBar.setValue(ca)

    verticalBar.setValue(hBar.getValue)
    palette.setValue(sBar.getValue, vBar.getValue)
  }

  /** Updates picker from H, S and V bars */
  override protected def updateValuesFromHSVFields(): Unit = {
    val hsv = ColorUtils.RGBtoHSV(pickerColor)
    var h   = hsv(0)
    var s   = hsv(1)
    var v   = hsv(2)

    if (hBar.isInputValid) h = hBar.getValue
    if (sBar.isInputValid) s = sBar.getValue
    if (vBar.isInputValid) v = vBar.getValue

    pickerColor = ColorUtils.HSVtoRGB(h.toFloat, s.toFloat, v.toFloat, pickerColor.a)

    val cr = MathUtils.round(pickerColor.r * 255.0f)
    val cg = MathUtils.round(pickerColor.g * 255.0f)
    val cb = MathUtils.round(pickerColor.b * 255.0f)

    rBar.setValue(cr)
    gBar.setValue(cg)
    bBar.setValue(cb)
  }

  /** Updates picker from R, G and B bars */
  private def updateValuesFromRGBFields(): Unit = {
    var r = MathUtils.round(pickerColor.r * 255.0f)
    var g = MathUtils.round(pickerColor.g * 255.0f)
    var b = MathUtils.round(pickerColor.b * 255.0f)

    if (rBar.isInputValid) r = rBar.getValue
    if (gBar.isInputValid) g = gBar.getValue
    if (bBar.isInputValid) b = bBar.getValue

    pickerColor.set(r / 255.0f, g / 255.0f, b / 255.0f, pickerColor.a)

    val hsv = ColorUtils.RGBtoHSV(pickerColor)
    val ch  = hsv(0)
    val cs  = hsv(1)
    val cv  = hsv(2)

    hBar.setValue(ch)
    sBar.setValue(cs)
    vBar.setValue(cv)

    verticalBar.setValue(hBar.getValue)
    palette.setValue(sBar.getValue, vBar.getValue)
  }

  private class RgbChannelBarListener extends ChannelBar.ChannelBarListener {
    override def updateFields(): Unit = {
      updateValuesFromRGBFields()
      updateUI()
    }

    override def setShaderUniforms(shader: ShaderProgram): Unit = {
      shader.setUniformf("u_r", pickerColor.r)
      shader.setUniformf("u_g", pickerColor.g)
      shader.setUniformf("u_b", pickerColor.b)
    }
  }

  private class AlphaChannelBarListener extends RgbChannelBarListener {
    override def updateFields(): Unit = {
      if (aBar.isInputValid) pickerColor.a = aBar.getValue / 255.0f
      updateUI()
    }
  }

  abstract private class HsvChannelBarListener extends ChannelBar.ChannelBarListener {
    override def updateFields(): Unit = {
      updateLinkedWidget()
      updateValuesFromHSVFields()
      updateUI()
    }

    override def setShaderUniforms(shader: ShaderProgram): Unit = {
      shader.setUniformf("u_h", hBar.getValue / 360.0f)
      shader.setUniformf("u_s", sBar.getValue / 100.0f)
      shader.setUniformf("u_v", vBar.getValue / 100.0f)
    }

    protected def updateLinkedWidget(): Unit
  }
}
