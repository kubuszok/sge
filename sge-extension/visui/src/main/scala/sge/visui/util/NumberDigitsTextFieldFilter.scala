/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 41
 * Covenant-baseline-methods: NumberDigitsTextFieldFilter,_useFieldCursorPosition,acceptNegativeValues,acceptNegativeValues_,setAcceptNegativeValues,setUseFieldCursorPosition,useFieldCursorPosition,useFieldCursorPosition_
 * Covenant-source-reference: com/kotcrab/vis/ui/util/NumberDigitsTextFieldFilter.java
 * Covenant-verified: 2026-04-19
 */
package sge
package visui
package util

import sge.scenes.scene2d.ui.TextField.TextFieldFilter

/** Base class for number digits text field filters. Filters extending this class must handle disabling entering negative number values and using cursor position to prevent typing minus in wrong
  * place.
  * @author
  *   Kotcrab
  * @see
  *   [[IntDigitsOnlyFilter]]
  * @see
  *   [[FloatDigitsOnlyFilter]]
  */
abstract class NumberDigitsTextFieldFilter(private var _acceptNegativeValues: Boolean) extends TextFieldFilter {

  private var _useFieldCursorPosition: Boolean = false

  def acceptNegativeValues:                   Boolean = _acceptNegativeValues
  def acceptNegativeValues_=(value: Boolean): Unit    = _acceptNegativeValues = value
  @deprecated("Use acceptNegativeValues_=", "")
  def setAcceptNegativeValues(value: Boolean): Unit = _acceptNegativeValues = value

  def useFieldCursorPosition: Boolean = _useFieldCursorPosition

  /** @param useFieldCursorPosition
    *   if true this filter will use current field cursor position to prevent typing minus sign in wrong place. This is disabled by default. If you enable this feature you must ensure that field
    *   cursor position is set to 0 when you change text programmatically. Non zero cursor position can happen when you are changing text when field still has user focus.
    */
  def useFieldCursorPosition_=(value: Boolean): Unit = _useFieldCursorPosition = value
  @deprecated("Use useFieldCursorPosition_=", "")
  def setUseFieldCursorPosition(value: Boolean): Unit = _useFieldCursorPosition = value
}
