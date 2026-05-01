/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 292
 * Covenant-baseline-methods: BAR_HEIGHT,BAR_WIDTH,BasicColorPicker,FIELD_WIDTH,HEX_COLOR_LENGTH,HEX_COLOR_LENGTH_WITH_ALPHA,HEX_FIELD_WIDTH,PALETTE_SIZE,PickerChangeListener,VERTICAL_BAR_WIDTH,_allowAlphaEdit,_disposed,_showColorPreviews,_showHexFields,acceptChar,allowAlphaEdit,allowAlphaEdit_,c,ch,changed,clicked,close,colorBeforeReset,colorPreviewsTable,commons,createColorWidgets,createColorsPreviewTable,createHexTable,createUI,cs,currentColorImg,cv,draw,focusHexField,getListener,hexField,hexTable,hsv,isDisposed,listener,mainTable,newColorImg,oldColor,palette,pickerColor,pickerListener,rebuildMainTable,restoreLastColor,setListener,setPickerColor,showColorPreviews,showColorPreviews_,showHexFields,showHexFields_,sizes,style,table,this,updateLinkedWidget,updateUI,updateValuesFromCurrentColor,updateValuesFromHSVFields,verticalBar,wasPedantic
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/color/BasicColorPicker.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
 */
package sge
package visui
package widget
package color

import scala.language.implicitConversions

import sge.graphics.Color
import sge.graphics.g2d.Batch
import sge.graphics.glutils.ShaderProgram
import sge.scenes.scene2d.{ Actor, InputEvent }
import sge.scenes.scene2d.ui.{ Image, TextField }
import sge.scenes.scene2d.utils.{ ChangeListener, ClickListener }
import sge.utils.Nullable
import sge.visui.{ FocusManager, Sizes, VisUI }
import sge.visui.util.ColorUtils
import sge.visui.widget.{ VisLabel, VisTable, VisValidatableTextField }
import sge.visui.widget.color.internal.{ AlphaImage, ColorPickerText, Palette, PickerCommons, VerticalChannelBar }

/** Color Picker widget, allows user to select color. ColorPicker is relatively heavy widget and should be reused if possible. Unlike other widgets, this one must be disposed when no longer needed!
  *
  * Displays color pallet along with hue spectrum bar. Palette show all possible combination of saturation and value (SV components of HSV color system) for given hue, spectrum bar shows all possible
  * values of hue (H component). Displays preview of current and new color and hex field that can be disabled. See [[ExtendedColorPicker]] if you need more features.
  *
  * Alpha channel can be only set from hex field and it disabled by default, use [[allowAlphaEdit_=]] to enable.
  * @author
  *   Kotcrab
  * @see
  *   [[ColorPicker]]
  * @see
  *   [[BasicColorPicker]]
  * @see
  *   [[ExtendedColorPicker]]
  * @since 0.9.3
  */
class BasicColorPicker protected (initStyle: ColorPickerWidgetStyle, initListener: Nullable[ColorPickerListener], loadExtendedShaders: Boolean)(using Sge) extends VisTable() with AutoCloseable {

  protected var style:    ColorPickerWidgetStyle        = initStyle
  protected var sizes:    Sizes                         = VisUI.getSizes
  protected var listener: Nullable[ColorPickerListener] = initListener

  var oldColor:    Color = new Color(Color.BLACK)
  var pickerColor: Color = new Color(Color.BLACK)

  protected var commons: PickerCommons = new PickerCommons(style, sizes, loadExtendedShaders)

  protected var palette:     Palette            = scala.compiletime.uninitialized
  protected var verticalBar: VerticalChannelBar = scala.compiletime.uninitialized

  private var mainTable:          VisTable                = scala.compiletime.uninitialized
  private var colorPreviewsTable: VisTable                = scala.compiletime.uninitialized
  private var hexTable:           VisTable                = scala.compiletime.uninitialized
  private var hexField:           VisValidatableTextField = scala.compiletime.uninitialized

  private var currentColorImg: Image = scala.compiletime.uninitialized
  private var newColorImg:     Image = scala.compiletime.uninitialized

  private var _allowAlphaEdit:    Boolean = false
  private var _showHexFields:     Boolean = true
  private var _showColorPreviews: Boolean = true

  private var _disposed: Boolean = false

  {
    createColorWidgets()
    createUI()
    updateValuesFromCurrentColor()
    updateUI()
  }

  def this()(using Sge) = this(VisUI.getSkin.get(classOf[ColorPickerWidgetStyle]), Nullable.empty, false)

  def this(listener: ColorPickerListener)(using Sge) = this(VisUI.getSkin.get(classOf[ColorPickerWidgetStyle]), Nullable(listener), false)

  def this(styleName: String, listener: Nullable[ColorPickerListener])(using Sge) =
    this(VisUI.getSkin.get(styleName, classOf[ColorPickerWidgetStyle]), listener, false)

  def this(style: ColorPickerWidgetStyle, listener: Nullable[ColorPickerListener])(using Sge) =
    this(style, listener, false)

  protected def createUI(): Unit = {
    mainTable = new VisTable(true)
    colorPreviewsTable = createColorsPreviewTable()
    hexTable = createHexTable()
    rebuildMainTable()
    add(Nullable(mainTable)).top()
  }

  private def rebuildMainTable(): Unit = {
    mainTable.clearChildren()
    mainTable.add(Nullable(palette)).size(BasicColorPicker.PALETTE_SIZE * sizes.scaleFactor)
    mainTable.add(Nullable(verticalBar)).size(BasicColorPicker.VERTICAL_BAR_WIDTH * sizes.scaleFactor, BasicColorPicker.PALETTE_SIZE * sizes.scaleFactor).top()

    if (_showColorPreviews) {
      mainTable.row()
      mainTable.add(Nullable(colorPreviewsTable)).colspan(2).expandX().fillX()
    }

    if (_showHexFields) {
      mainTable.row()
      mainTable.add(Nullable(hexTable)).colspan(2).expandX().left()
    }
  }

  private def createColorsPreviewTable(): VisTable = {
    val table = new VisTable(false)
    currentColorImg = new AlphaImage(commons, 5 * sizes.scaleFactor)
    table.add(Nullable(currentColorImg)).height(25 * sizes.scaleFactor).width(80 * sizes.scaleFactor).expandX().fillX()
    table.add(Nullable(new Image(style.iconArrowRight.get))).pad(0, 2, 0, 2)
    newColorImg = new AlphaImage(commons, 5 * sizes.scaleFactor)
    table.add(Nullable(newColorImg)).height(25 * sizes.scaleFactor).width(80 * sizes.scaleFactor).expandX().fillX()

    currentColorImg.color.set(pickerColor)
    newColorImg.color.set(pickerColor)

    currentColorImg.addListener(new ClickListener() {
      override def clicked(event: InputEvent, x: Float, y: Float): Unit =
        restoreLastColor()
    })

    table
  }

  private def createHexTable(): VisTable = {
    val table = new VisTable(true)
    hexField = new VisValidatableTextField("00000000")
    table.add(Nullable(new VisLabel(ColorPickerText.HEX.get)))
    table.add(Nullable(hexField)).width(BasicColorPicker.HEX_FIELD_WIDTH * sizes.scaleFactor)
    table.row()

    hexField.setMaxLength(BasicColorPicker.HEX_COLOR_LENGTH)
    hexField.setProgrammaticChangeEvents(false)
    hexField.setTextFieldFilter(
      Nullable(
        new TextField.TextFieldFilter() {
          override def acceptChar(textField: TextField, c: Char): Boolean =
            Character.isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')
        }
      )
    )

    hexField.addListener(
      new ChangeListener() {
        override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit = {
          val expectedLen = if (_allowAlphaEdit) BasicColorPicker.HEX_COLOR_LENGTH_WITH_ALPHA else BasicColorPicker.HEX_COLOR_LENGTH
          if (hexField.text.length == expectedLen) {
            setPickerColor(Color.valueOf(hexField.text), updateCurrentColor = false)
          }
        }
      }
    )

    table
  }

  def focusHexField(): Unit =
    if (showHexFields) {
      FocusManager.switchFocus(stage, hexField)
      stage.foreach(_.setKeyboardFocus(Nullable(hexField)))
    }

  protected def createColorWidgets(): Unit = {
    val pickerListener = new PickerChangeListener()
    palette = new Palette(commons, 100, pickerListener)
    verticalBar = new VerticalChannelBar(commons, 360, pickerListener)
  }

  protected def updateUI(): Unit = {
    palette.setPickerHue(verticalBar.getValue)
    newColorImg.color.set(pickerColor)
    hexField.setText(pickerColor.toString.toUpperCase)
    hexField.setCursorPosition(hexField.getMaxLength)
    if (listener.isDefined) listener.get.changed(pickerColor)
  }

  /** Updates picker ui from current color */
  protected def updateValuesFromCurrentColor(): Unit = {
    val hsv = ColorUtils.RGBtoHSV(pickerColor)
    val ch  = hsv(0)
    val cs  = hsv(1)
    val cv  = hsv(2)

    verticalBar.setValue(ch)
    palette.setValue(cs, cv)
  }

  protected def updateValuesFromHSVFields(): Unit =
    pickerColor = ColorUtils.HSVtoRGB(verticalBar.getValue.toFloat, palette.getS.toFloat, palette.getV.toFloat, pickerColor.a)

  def restoreLastColor(): Unit = {
    val colorBeforeReset = new Color(pickerColor)
    setPickerColor(oldColor)
    if (listener.isDefined) listener.get.reset(colorBeforeReset, pickerColor)
  }

  /** Sets current selected color in picker. */
  def setPickerColor(newColor: Color): Unit = {
    val c = new Color(newColor)
    if (!_allowAlphaEdit) c.a = 1
    setPickerColor(c, updateCurrentColor = true)
  }

  protected def setPickerColor(newColor: Color, updateCurrentColor: Boolean): Unit = {
    if (updateCurrentColor) {
      currentColorImg.color.set(new Color(newColor))
      oldColor = new Color(newColor)
    }
    pickerColor = new Color(newColor)
    updateValuesFromCurrentColor()
    updateUI()
  }

  def getListener: Nullable[ColorPickerListener] = listener

  def setListener(listener: Nullable[ColorPickerListener]): Unit =
    this.listener = listener

  /** @param allowAlphaEdit
    *   if false this picker will have disabled editing color alpha channel. If current picker color has alpha it will be reset to 1. If true alpha editing will be re-enabled. For better UX this
    *   should not be called while ColorPicker is visible.
    */
  def allowAlphaEdit_=(allowAlphaEdit: Boolean): Unit = {
    _allowAlphaEdit = allowAlphaEdit
    hexField.setMaxLength(if (allowAlphaEdit) BasicColorPicker.HEX_COLOR_LENGTH_WITH_ALPHA else BasicColorPicker.HEX_COLOR_LENGTH)
    if (!allowAlphaEdit) {
      setPickerColor(new Color(pickerColor))
    }
  }

  def allowAlphaEdit: Boolean = _allowAlphaEdit

  def showHexFields_=(showHexFields: Boolean): Unit = {
    _showHexFields = showHexFields
    hexTable.visible = showHexFields
    rebuildMainTable()
  }

  def showHexFields: Boolean = _showHexFields

  def showColorPreviews_=(showColorPreviews: Boolean): Unit = {
    _showColorPreviews = showColorPreviews
    colorPreviewsTable.visible = showColorPreviews
    rebuildMainTable()
  }

  def showColorPreviews: Boolean = _showColorPreviews

  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    val wasPedantic = ShaderProgram.pedantic
    ShaderProgram.pedantic = false
    super.draw(batch, parentAlpha)
    ShaderProgram.pedantic = wasPedantic
  }

  def isDisposed: Boolean = _disposed

  override def close(): Unit = {
    if (_disposed) throw new IllegalStateException("ColorPicker can't be disposed twice!")
    commons.close()
    _disposed = true
  }

  /** Internal default picker listener used to get events from color widgets */
  class PickerChangeListener extends ChangeListener {
    protected def updateLinkedWidget(): Unit = ()

    override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit = {
      updateLinkedWidget()
      updateValuesFromHSVFields()
      updateUI()
    }
  }
}

object BasicColorPicker {
  val FIELD_WIDTH:  Int = 50
  val PALETTE_SIZE: Int = 160
  val BAR_WIDTH:    Int = 130
  val BAR_HEIGHT:   Int = 12

  private[color] val VERTICAL_BAR_WIDTH:          Float = 15f
  private[color] val HEX_FIELD_WIDTH:             Int   = 95
  private[color] val HEX_COLOR_LENGTH:            Int   = 6
  private[color] val HEX_COLOR_LENGTH_WITH_ALPHA: Int   = 8
}
