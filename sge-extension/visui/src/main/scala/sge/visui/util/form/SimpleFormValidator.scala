/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 299
 * Covenant-baseline-methods: ChangeSharedListener,CheckedButtonWrapper,EmptyInputValidator,FormValidatorStyle,SimpleFormValidator,_successMsg,_treatDisabledFieldsAsValid,add,addDisableTarget,buttons,changeListener,changed,checked,colorTransitionDuration,custom,disableTargets,errorLabelColor,errorMsgText,fields,floatNumber,formInvalid,i,integerNumber,isTreatDisabledFieldsAsValid,notEmpty,removeDisableTarget,result,setButtonStateInvalid,setMessageLabel,setSuccessMessage,setTreatDisabledFieldsAsValid,this,unchecked,updateWidgets,validLabelColor,validate,validator,valueGreaterThan,valueLesserThan,wrapper
 * Covenant-source-reference: com/kotcrab/vis/ui/util/form/SimpleFormValidator.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
 */
package sge
package visui
package util
package form

import scala.language.implicitConversions
import scala.util.boundary
import scala.util.boundary.break

import sge.graphics.Color
import sge.scenes.scene2d.Actor
import sge.scenes.scene2d.actions.Actions
import sge.scenes.scene2d.ui.{ Button, Label }
import sge.scenes.scene2d.utils.{ ChangeListener, Disableable }
import sge.utils.{ DynamicArray, Nullable, Seconds }
import sge.visui.VisUI
import sge.visui.util.Validators
import sge.visui.widget.{ VisCheckBox, VisValidatableTextField }

/** Utility class made for creating input forms that requires inputting various information and that information cannot be wrong. For example user registration form.
  *
  * SimpleFormValidator is GWT compatible and does not provide fileExists methods, if you are not using GWT use [[FormValidator]].
  * @author
  *   Kotcrab
  */
class SimpleFormValidator(targetToDisable: Nullable[Disableable], private var _messageLabel: Nullable[Label], formStyle: SimpleFormValidator.FormValidatorStyle) {

  private val changeListener:              ChangeListener                                         = new SimpleFormValidator.ChangeSharedListener(this)
  private val fields:                      DynamicArray[VisValidatableTextField]                  = DynamicArray[VisValidatableTextField]()
  private val buttons:                     DynamicArray[SimpleFormValidator.CheckedButtonWrapper] = DynamicArray[SimpleFormValidator.CheckedButtonWrapper]()
  private var _successMsg:                 Nullable[String]                                       = Nullable.empty
  private var formInvalid:                 Boolean                                                = false
  private var errorMsgText:                Nullable[String]                                       = Nullable.empty
  private val disableTargets:              DynamicArray[Disableable]                              = DynamicArray[Disableable]()
  private var _treatDisabledFieldsAsValid: Boolean                                                = true

  targetToDisable.foreach(disableTargets.add)

  /** @param targetToDisable target actor that will be disabled if form is invalid. May be null. */
  def this(targetToDisable: Disableable) = this(Nullable(targetToDisable), Nullable.empty, VisUI.getSkin.get[SimpleFormValidator.FormValidatorStyle])

  /** @param targetToDisable
    *   target actor that will be disabled if form is invalid. May be null.
    * @param messageLabel
    *   label that text will be changed if form is valid or invalid. May be null.
    */
  def this(targetToDisable: Disableable, messageLabel: Label) = this(Nullable(targetToDisable), Nullable(messageLabel), VisUI.getSkin.get[SimpleFormValidator.FormValidatorStyle])

  def this(targetToDisable: Disableable, messageLabel: Label, styleName: String) =
    this(Nullable(targetToDisable), Nullable(messageLabel), VisUI.getSkin.get[SimpleFormValidator.FormValidatorStyle](styleName))

  def this(targetToDisable: Disableable, messageLabel: Label, style: SimpleFormValidator.FormValidatorStyle) =
    this(Nullable(targetToDisable), Nullable(messageLabel), style)

  /** Validates if field is not empty */
  def notEmpty(field: VisValidatableTextField, errorMsg: String): FormInputValidator = {
    val validator = new SimpleFormValidator.EmptyInputValidator(errorMsg)
    field.addValidator(validator)
    add(field)
    validator
  }

  /** Validates if entered text is integer number */
  def integerNumber(field: VisValidatableTextField, errorMsg: String): FormInputValidator = {
    val wrapper = new ValidatorWrapper(errorMsg, Validators.INTEGERS)
    field.addValidator(wrapper)
    add(field)
    wrapper
  }

  /** Validates if entered text is float number */
  def floatNumber(field: VisValidatableTextField, errorMsg: String): FormInputValidator = {
    val wrapper = new ValidatorWrapper(errorMsg, Validators.FLOATS)
    field.addValidator(wrapper)
    add(field)
    wrapper
  }

  def valueGreaterThan(field: VisValidatableTextField, errorMsg: String, value: Float): FormInputValidator =
    valueGreaterThan(field, errorMsg, value, validIfEqualsValue = false)

  def valueLesserThan(field: VisValidatableTextField, errorMsg: String, value: Float): FormInputValidator =
    valueLesserThan(field, errorMsg, value, validIfEqualsValue = false)

  def valueGreaterThan(field: VisValidatableTextField, errorMsg: String, value: Float, validIfEqualsValue: Boolean): FormInputValidator = {
    val wrapper = new ValidatorWrapper(errorMsg, new Validators.GreaterThanValidator(value, validIfEqualsValue))
    field.addValidator(wrapper)
    add(field)
    wrapper
  }

  def valueLesserThan(field: VisValidatableTextField, errorMsg: String, value: Float, validIfEqualsValue: Boolean): FormInputValidator = {
    val wrapper = new ValidatorWrapper(errorMsg, new Validators.LesserThanValidator(value, validIfEqualsValue))
    field.addValidator(wrapper)
    add(field)
    wrapper
  }

  /** Allows to add custom validator to field */
  def custom(field: VisValidatableTextField, customValidator: FormInputValidator): FormInputValidator = {
    field.addValidator(customValidator)
    add(field)
    customValidator
  }

  /** Validates if given button (usually checkbox) is checked. Use VisCheckBox to additionally support error border around it. */
  def checked(button: Button, errorMsg: String): Unit = {
    buttons.add(new SimpleFormValidator.CheckedButtonWrapper(button, mustBeChecked = true, errorMsg))
    button.addListener(changeListener)
    validate()
  }

  /** Validates if given button (usually checkbox) is unchecked. Use VisCheckBox to additionally support error border around it. */
  def unchecked(button: Button, errorMsg: String): Unit = {
    buttons.add(new SimpleFormValidator.CheckedButtonWrapper(button, mustBeChecked = false, errorMsg))
    button.addListener(changeListener)
    validate()
  }

  /** Adds field to this form without attaching any [[FormInputValidator]] to it. This can be used when field already has added all required validators.
    */
  def add(field: VisValidatableTextField): Unit = {
    if (!fields.contains(field)) fields.add(field)
    field.addListener(changeListener)
    validate()
  }

  def addDisableTarget(disableable: Disableable): Unit = {
    disableTargets.add(disableable)
    updateWidgets()
  }

  def removeDisableTarget(disableable: Disableable): Boolean = {
    val result = disableTargets.removeValue(disableable)
    updateWidgets()
    result
  }

  def setMessageLabel(messageLabel: Label): Unit = {
    _messageLabel = Nullable(messageLabel)
    updateWidgets()
  }

  def setSuccessMessage(successMsg: String): Unit = {
    _successMsg = Nullable(successMsg)
    updateWidgets()
  }

  def isTreatDisabledFieldsAsValid: Boolean = _treatDisabledFieldsAsValid

  def setTreatDisabledFieldsAsValid(value: Boolean): Unit = {
    _treatDisabledFieldsAsValid = value
    validate()
  }

  def validate(): Unit = {
    formInvalid = false
    errorMsgText = Nullable.empty

    var i = 0
    while (i < buttons.size) {
      val wrapper = buttons(i)
      wrapper.setButtonStateInvalid(wrapper.button.checked != wrapper.mustBeChecked)
      i += 1
    }

    boundary {
      i = 0
      while (i < buttons.size) {
        val wrapper = buttons(i)
        if (!(_treatDisabledFieldsAsValid && wrapper.button.disabled)) {
          if (wrapper.button.checked != wrapper.mustBeChecked) {
            errorMsgText = Nullable(wrapper.errorMsg)
            formInvalid = true
            break(())
          }
        }
        i += 1
      }
    }

    i = 0
    while (i < fields.size) {
      fields(i).validateInput()
      i += 1
    }

    boundary {
      i = 0
      while (i < fields.size) {
        val field = fields(i)
        if (!(_treatDisabledFieldsAsValid && field.disabled)) {
          if (!field.isInputValid) {
            val validators = field.getValidators
            var j          = 0
            while (j < validators.size) {
              validators(j) match {
                case validator: FormInputValidator =>
                  if (!validator.getLastResult) {
                    if (!(validator.isHideErrorOnEmptyInput && field.text.isEmpty)) {
                      errorMsgText = Nullable(validator.getErrorMsg)
                    }
                    formInvalid = true
                    break(())
                  }
                case _ =>
                  throw new IllegalStateException(
                    "Fields validated by FormValidator cannot have validators not added using FormValidator methods. " +
                      "Are you adding validators to field manually?"
                  )
              }
              j += 1
            }
            break(())
          }
        }
        i += 1
      }
    }

    updateWidgets()
  }

  private def updateWidgets(): Unit = {
    var i = 0
    while (i < disableTargets.size) {
      disableTargets(i).disabled = formInvalid
      i += 1
    }

    _messageLabel.foreach { label =>
      if (errorMsgText.isDefined) {
        label.setText(errorMsgText.get)
      } else {
        label.setText(_successMsg.getOrElse(""))
      }

      val targetColor: Nullable[Color] = if (errorMsgText.isDefined) Nullable(formStyle.errorLabelColor) else Nullable(formStyle.validLabelColor)
      targetColor.foreach { tc =>
        if (formStyle.colorTransitionDuration != 0) {
          label.addAction(Actions.color(tc, Seconds(formStyle.colorTransitionDuration)))
        } else {
          label.color.set(tc)
        }
      }
    }
  }
}

object SimpleFormValidator {

  private class ChangeSharedListener(form: SimpleFormValidator) extends ChangeListener {
    override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit =
      form.validate()
  }

  private class CheckedButtonWrapper(val button: Button, val mustBeChecked: Boolean, val errorMsg: String) {
    def setButtonStateInvalid(state: Boolean): Unit =
      button match {
        case cb: VisCheckBox => cb.setStateInvalid(state)
        case _ => ()
      }
  }

  class EmptyInputValidator(errorMsg: String) extends FormInputValidator(errorMsg) {
    override def validate(input: String): Boolean = input.nonEmpty
  }

  class FormValidatorStyle {

    /** Optional */
    var errorLabelColor: Color = scala.compiletime.uninitialized

    /** Optional */
    var validLabelColor: Color = scala.compiletime.uninitialized

    var colorTransitionDuration: Float = 0f

    def this(errorLabelColor: Color, validLabelColor: Color) = {
      this()
      this.errorLabelColor = errorLabelColor
      this.validLabelColor = validLabelColor
    }

    def this(style: FormValidatorStyle) = {
      this()
      this.errorLabelColor = style.errorLabelColor
      this.validLabelColor = style.validLabelColor
      this.colorTransitionDuration = style.colorTransitionDuration
    }
  }
}
