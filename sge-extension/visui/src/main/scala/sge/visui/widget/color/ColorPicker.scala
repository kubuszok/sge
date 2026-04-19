/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 168
 * Covenant-baseline-methods: ColorPicker,_closeAfterPickingFinished,_listener,allowAlphaEdit,allowAlphaEdit_,buttonBar,cancelButton,changed,close,closeAfterPickingFinished,closeAfterPickingFinished_,cpStyle,createButtons,createListeners,dispose,fadeOutDueToCanceled,getListener,getPicker,isDisposed,okButton,picker,restoreButton,restoreLastColor,setListener,setPickerColor,setStage,showHexFields,showHexFields_,this
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/color/ColorPicker.java
 * Covenant-verified: 2026-04-19
 */
package sge
package visui
package widget
package color

import scala.language.implicitConversions

import sge.graphics.Color
import sge.scenes.scene2d.{ Actor, Stage }
import sge.scenes.scene2d.utils.ChangeListener
import sge.utils.Nullable
import sge.visui.VisUI
import sge.visui.widget.{ ButtonBar, VisTable, VisTextButton, VisWindow }
import sge.visui.widget.color.internal.ColorPickerText.*

/** Color Picker dialog, allows user to select color. ColorPicker is relatively heavy dialog and should be reused whenever possible. This dialog must be disposed when no longer needed! ColorPicker
  * will be centered on screen after adding to Stage use [[setCenterOnAdd]] to change this.
  * @author
  *   Kotcrab
  * @see
  *   [[ColorPicker]]
  * @see
  *   [[BasicColorPicker]]
  * @see
  *   [[ExtendedColorPicker]]
  * @since 0.6.0
  */
class ColorPicker(styleName: String, title: Nullable[String], initListener: Nullable[ColorPickerListener])(using Sge)
    extends VisWindow(if (title.isDefined) title.get else "", VisUI.getSkin.get(styleName, classOf[ColorPickerStyle])) {

  private var picker: ExtendedColorPicker = scala.compiletime.uninitialized

  private var _listener: Nullable[ColorPickerListener] = initListener

  private var restoreButton: VisTextButton = scala.compiletime.uninitialized
  private var cancelButton:  VisTextButton = scala.compiletime.uninitialized
  private var okButton:      VisTextButton = scala.compiletime.uninitialized

  private var _closeAfterPickingFinished: Boolean = true
  private var fadeOutDueToCanceled:       Boolean = false

  {
    val cpStyle = style.asInstanceOf[ColorPickerStyle]

    if (title.isEmpty) titleLabel.setText(TITLE.get)

    isModal = true
    isMovable = true

    addCloseButton()
    closeOnEscape()

    picker = new ExtendedColorPicker(cpStyle.pickerStyle.get, initListener)

    add(Nullable[Actor](picker))
    row()
    add(Nullable[Actor](createButtons())).pad(3).right().expandX().colspan(3)

    pack()
    centerWindow()

    createListeners()
  }

  def this()(using Sge) = this("default", Nullable.empty, Nullable.empty)

  def this(title: String)(using Sge) = this("default", Nullable(title), Nullable.empty)

  def this(title: String, listener: ColorPickerListener)(using Sge) = this("default", Nullable(title), Nullable(listener))

  def this(listener: ColorPickerListener)(using Sge) = this("default", Nullable.empty, Nullable(listener))

  private def createButtons(): VisTable = {
    val buttonBar = new ButtonBar()
    buttonBar.ignoreSpacing = true
    restoreButton = new VisTextButton(RESTORE.get)
    buttonBar.setButton(ButtonBar.ButtonType.LEFT, restoreButton)
    okButton = new VisTextButton(OK.get)
    buttonBar.setButton(ButtonBar.ButtonType.OK, okButton)
    cancelButton = new VisTextButton(CANCEL.get)
    buttonBar.setButton(ButtonBar.ButtonType.CANCEL, cancelButton)
    buttonBar.createTable()
  }

  private def createListeners(): Unit = {
    restoreButton.addListener(new ChangeListener() {
      override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit =
        picker.restoreLastColor()
    })

    okButton.addListener(
      new ChangeListener() {
        override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit = {
          if (_listener.isDefined) _listener.get.finished(new Color(picker.pickerColor))
          setPickerColor(picker.pickerColor)
          if (_closeAfterPickingFinished) fadeOut()
        }
      }
    )

    cancelButton.addListener(
      new ChangeListener() {
        override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit = {
          fadeOutDueToCanceled = true
          close()
        }
      }
    )
  }

  override protected[sge] def setStage(stage: Nullable[Stage]): Unit = {
    super.setStage(stage)
    if (stage.isEmpty && fadeOutDueToCanceled) {
      fadeOutDueToCanceled = false
      setPickerColor(picker.oldColor)
    }
  }

  /** Controls whether to fade out color picker after users finished color picking and has pressed OK button. If this is set to false picker won't close after pressing OK button. Default is true. Note
    * that by default picker is a modal window so might also want to call `colorPicker.setModal(false)` to disable it.
    */
  def closeAfterPickingFinished_=(closeAfterPickingFinished: Boolean): Unit =
    _closeAfterPickingFinished = closeAfterPickingFinished

  def closeAfterPickingFinished: Boolean = _closeAfterPickingFinished

  override protected def close(): Unit = {
    if (_listener.isDefined) _listener.get.canceled(picker.oldColor)
    super.close()
  }

  /** Disposes the picker resources. */
  def dispose(): Unit =
    picker.close()

  /** @return internal dialog color picker */
  def getPicker: ExtendedColorPicker = picker

  // ColorPicker delegates

  def showHexFields: Boolean = picker.showHexFields

  def showHexFields_=(showHexFields: Boolean): Unit = picker.showHexFields = showHexFields

  def isDisposed: Boolean = picker.isDisposed

  def allowAlphaEdit_=(allowAlphaEdit: Boolean): Unit = picker.allowAlphaEdit = allowAlphaEdit

  def allowAlphaEdit: Boolean = picker.allowAlphaEdit

  def restoreLastColor(): Unit = picker.restoreLastColor()

  def setPickerColor(newColor: Color): Unit = picker.setPickerColor(newColor)

  def setListener(listener: Nullable[ColorPickerListener]): Unit = {
    _listener = listener
    picker.setListener(listener)
  }

  def getListener: Nullable[ColorPickerListener] = picker.getListener
}
