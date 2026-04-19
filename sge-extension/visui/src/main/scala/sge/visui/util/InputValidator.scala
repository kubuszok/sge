/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 25
 * Covenant-baseline-methods: InputValidator,validateInput
 * Covenant-source-reference: com/kotcrab/vis/ui/util/InputValidator.java
 * Covenant-verified: 2026-04-19
 */
package sge
package visui
package util

/** Interface implemented by classes that can validate whether user input is right or wrong.
  * @author
  *   Kotcrab
  */
trait InputValidator {

  /** Called when input must be validated.
    * @param input
    *   text that should be validated
    * @return
    *   true if input is valid, false otherwise
    */
  def validateInput(input: String): Boolean
}
