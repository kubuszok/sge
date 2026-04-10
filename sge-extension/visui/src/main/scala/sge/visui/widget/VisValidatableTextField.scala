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

import scala.language.implicitConversions

import sge.scenes.scene2d.Actor
import sge.scenes.scene2d.utils.FocusListener
import sge.utils.Nullable
import sge.visui.VisUI
import sge.visui.util.InputValidator

import scala.collection.mutable.ArrayBuffer

/** Text field whose input can be validated by custom input validators.
  * @author
  *   Kotcrab
  * @see
  *   [[InputValidator]]
  * @see
  *   [[sge.visui.util.Validators]]
  */
class VisValidatableTextField(initialText: Nullable[String], visStyle: VisTextField.VisTextFieldStyle)(using Sge) extends VisTextField(initialText, visStyle) {

  private val validators:           ArrayBuffer[InputValidator]      = ArrayBuffer.empty
  private var validationEnabled:    Boolean                          = true
  private var restoreFocusListener: Nullable[LastValidFocusListener] = Nullable.empty
  private var _restoreLastValid:    Boolean                          = false
  private var lastValid:            String                           = ""

  setProgrammaticChangeEvents(true)
  setIgnoreEqualsTextChange(false)

  override protected def beforeChangeEventFired(): Unit = validateInput()

  def this()(using Sge) = this(Nullable(""), VisUI.getSkin.get[VisTextField.VisTextFieldStyle])
  def this(text: String)(using Sge) = this(Nullable(text), VisUI.getSkin.get[VisTextField.VisTextFieldStyle])
  def this(text: String, styleName: String)(using Sge) = this(Nullable(text), VisUI.getSkin.get[VisTextField.VisTextFieldStyle](styleName))
  def this(text: String, style:     VisTextField.VisTextFieldStyle)(using Sge) = this(Nullable(text), style)

  def this(validator: InputValidator)(using Sge) = {
    this(Nullable(""), VisUI.getSkin.get[VisTextField.VisTextFieldStyle])
    addValidator(validator)
  }

  def this(validators: InputValidator*)(using Sge) = {
    this(Nullable(""), VisUI.getSkin.get[VisTextField.VisTextFieldStyle])
    validators.foreach(addValidator)
  }

  def validateInput(): Unit =
    if (validationEnabled) {
      var valid = true
      var i     = 0
      while (i < validators.length && valid) {
        if (!validators(i).validateInput(text)) {
          setInputValid(false)
          valid = false
        }
        i += 1
      }
      if (valid) setInputValid(true)
    } else {
      // validation not enabled
      setInputValid(true)
    }

  def addValidator(validator: InputValidator): Unit = {
    validators += validator
    validateInput()
  }

  def getValidators: ArrayBuffer[InputValidator] = validators

  def isValidationEnabled: Boolean = validationEnabled

  /** Enables or disables validation, after changing this setting validateInput() is called, if validationEnabled == false then field is marked as valid otherwise standard validation is performed.
    */
  def setValidationEnabled(enabled: Boolean): Unit = {
    validationEnabled = enabled
    validateInput()
  }

  def restoreLastValid: Boolean = _restoreLastValid

  /** If true this field will automatically restore last valid text if it loses keyboard focus during text edition. This can't be called while field is selected, doing so will result in
    * IllegalStateException. Default is false.
    */
  def setRestoreLastValid(restore: Boolean): Unit = {
    _restoreLastValid = restore
    if (restore) {
      if (restoreFocusListener.isEmpty) restoreFocusListener = Nullable(new LastValidFocusListener)
      restoreFocusListener.foreach(addListener)
    } else {
      restoreFocusListener.foreach(removeListener)
    }
  }

  def restoreLastValidText(): Unit = {
    if (!_restoreLastValid)
      throw new IllegalStateException("Restore last valid is not enabled, see #setRestoreLastValid(boolean)")
    setText(lastValid)
    setInputValid(true)
  }

  private class LastValidFocusListener extends FocusListener {
    override def keyboardFocusChanged(event: FocusListener.FocusEvent, actor: Actor, focused: Boolean): Unit = {
      if (focused && _restoreLastValid) {
        lastValid = VisValidatableTextField.this.text
      }
      if (!focused && !isInputValid && _restoreLastValid) {
        restoreLastValidText()
      }
    }
  }
}
