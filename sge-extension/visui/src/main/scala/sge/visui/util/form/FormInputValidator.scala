/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 62
 * Covenant-baseline-methods: FormInputValidator,_hideErrorOnEmptyInput,_result,errorMsg,errorMsg_,getErrorMsg,getLastResult,hideErrorOnEmptyInput,isHideErrorOnEmptyInput,setErrorMsg,setHideErrorOnEmptyInput,validate,validateInput
 * Covenant-source-reference: com/kotcrab/vis/ui/util/form/FormInputValidator.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
 */
package sge
package visui
package util
package form

import sge.visui.util.InputValidator

/** Base class for all validators used in [[SimpleFormValidator]]. Implementing custom [[FormInputValidator]] doesn't differ from creating standard [[InputValidator]]. You just need to supply error
  * message which will be displayed when form validation failed on this validator. Because implementing custom [[FormInputValidator]] does not require any more changes you can use [[ValidatorWrapper]]
  * for existing [[InputValidator]]s.
  * @author
  *   Kotcrab
  * @see
  *   [[InputValidator]]
  * @see
  *   [[ValidatorWrapper]]
  */
abstract class FormInputValidator(private var _errorMsg: String) extends InputValidator {
  private var _result:                Boolean = false
  private var _hideErrorOnEmptyInput: Boolean = false

  final override def validateInput(input: String): Boolean = {
    _result = validate(input)
    _result
  }

  /** Called by FormInputValidator when input should be validated, for proper validator behaviour this must be used instead of [[validateInput]]. Last result of this function will be stored because it
    * is required by FormValidator.
    * @param input
    *   that should be validated.
    * @return
    *   if input is valid, false otherwise.
    */
  protected def validate(input: String): Boolean

  /** When called, error message of this validator won't be displayed if input field is empty, however form still will be treated as invalid (confirm button won't be enabled). This is UX improvement
    * feature, simply don't display error before user typed in something.
    */
  def hideErrorOnEmptyInput(): FormInputValidator = {
    _hideErrorOnEmptyInput = true
    this
  }

  def setHideErrorOnEmptyInput(value: Boolean): Unit    = _hideErrorOnEmptyInput = value
  def isHideErrorOnEmptyInput:                  Boolean = _hideErrorOnEmptyInput

  def errorMsg:                String = _errorMsg
  def errorMsg_=(msg: String): Unit   = _errorMsg = msg

  // Alias to match Java getter pattern used by SimpleFormValidator
  def getErrorMsg:              String = _errorMsg
  def setErrorMsg(msg: String): Unit   = _errorMsg = msg

  private[form] def getLastResult: Boolean = _result
}
