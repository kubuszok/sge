/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package visui
package util
package form

import sge.visui.util.InputValidator

/** Allows standard [[InputValidator]] to be used with [[SimpleFormValidator#custom]]. Wraps standard input validator and adds error message.
  * @author
  *   Kotcrab
  */
class ValidatorWrapper(errorMsg: String, validator: InputValidator) extends FormInputValidator(errorMsg) {
  override protected def validate(input: String): Boolean =
    validator.validateInput(input)
}
