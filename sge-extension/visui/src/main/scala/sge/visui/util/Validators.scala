/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 67
 * Covenant-baseline-methods: FLOATS,FloatValidator,GreaterThanValidator,INTEGERS,IntegerValidator,LesserThanValidator,Validators,validateInput
 * Covenant-source-reference: com/kotcrab/vis/ui/util/Validators.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
 */
package sge
package visui
package util

/** Provides premade validators that can be used with for example with [[sge.visui.widget.VisValidatableTextField]] or dialog input dialogs.
  * @author
  *   Kotcrab
  */
object Validators {

  /** Shared static instance of [[IntegerValidator]]. Can be safely reused. */
  val INTEGERS: IntegerValidator = IntegerValidator()

  /** Shared static instance of [[FloatValidator]]. Can be safely reused. */
  val FLOATS: FloatValidator = FloatValidator()

  /** Validates whether input is an integer number. You should use shared instance [[Validators.INTEGERS]]. */
  class IntegerValidator extends InputValidator {
    override def validateInput(input: String): Boolean =
      try {
        Integer.parseInt(input)
        true
      } catch {
        case _: NumberFormatException => false
      }
  }

  /** Validates whether input is a float number. You should use shared instance [[Validators.FLOATS]]. */
  class FloatValidator extends InputValidator {
    override def validateInput(input: String): Boolean =
      try {
        java.lang.Float.parseFloat(input)
        true
      } catch {
        case _: NumberFormatException => false
      }
  }

  /** Validates whether input is lesser (alternatively lesser or equal) than provided number. */
  class LesserThanValidator(var lesserThan: Float, var useEquals: Boolean = false) extends InputValidator {
    override def validateInput(input: String): Boolean =
      try {
        val value = java.lang.Float.parseFloat(input)
        if (useEquals) value <= lesserThan else value < lesserThan
      } catch {
        case _: NumberFormatException => false
      }
  }

  /** Validates whether input is greater (alternatively greater or equal) than provided number. */
  class GreaterThanValidator(var greaterThan: Float, var useEquals: Boolean = false) extends InputValidator {
    override def validateInput(input: String): Boolean =
      try {
        val value = java.lang.Float.parseFloat(input)
        if (useEquals) value >= greaterThan else value > greaterThan
      } catch {
        case _: NumberFormatException => false
      }
  }
}
