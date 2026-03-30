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
package spinner

import sge.utils.Nullable
import sge.visui.util.{ InputValidator, IntDigitsOnlyFilter, Validators }

/** Spinner models allowing to select int values.
  * @author
  *   Kotcrab
  * @see
  *   [[SimpleFloatSpinnerModel]]
  * @see
  *   [[FloatSpinnerModel]]
  * @since 1.0.2
  */
class IntSpinnerModel(initialValue: Int, private var _min: Int, private var _max: Int, private var _step: Int) extends AbstractSpinnerModel(false) {

  require(_min <= _max, "min can't be > max")
  require(_step > 0, "step must be > 0")

  private var current:         Int                 = initialValue
  private var textFieldFilter: IntDigitsOnlyFilter = scala.compiletime.uninitialized

  def this(initialValue: Int, min: Int, max: Int) = this(initialValue, min, max, 1)

  private val boundsValidator: InputValidator = (input: String) => checkInputBounds(input)

  override def bind(spinner: Spinner): Unit = {
    super.bind(spinner)

    val valueText = spinner.textField
    valueText.getValidators.clear()
    valueText.addValidator(boundsValidator)
    valueText.addValidator(Validators.INTEGERS)
    textFieldFilter = new IntDigitsOnlyFilter(true)
    valueText.setTextFieldFilter(Nullable(textFieldFilter))

    textFieldFilter.useFieldCursorPosition = true
    if (_min >= 0) {
      textFieldFilter.acceptNegativeValues = false
    } else {
      textFieldFilter.acceptNegativeValues = true
    }

    spinner.notifyValueChanged(true)
  }

  override def textChanged(): Unit = {
    val text = spinner.get.textField.text
    if (text == "") {
      current = _min
    } else if (checkInputBounds(text)) {
      current = Integer.parseInt(text)
    }
  }

  override protected def incrementModel(): Boolean = {
    if (current + _step > _max) {
      if (current == _max) {
        if (wrap) {
          current = _min
          return true
        }
        return false
      }
      current = _max
    } else {
      current += _step
    }
    true
  }

  override protected def decrementModel(): Boolean = {
    if (current - _step < _min) {
      if (current == _min) {
        if (wrap) {
          current = _max
          return true
        }
        return false
      }
      current = _min
    } else {
      current -= _step
    }
    true
  }

  override def text: String = current.toString

  def setValue(newValue: Int): Unit = setValue(newValue, spinner.get.programmaticChangeEvents)

  def setValue(newValue: Int, fireEvent: Boolean): Unit = {
    if (newValue > _max) {
      current = _max
    } else if (newValue < _min) {
      current = _min
    } else {
      current = newValue
    }
    spinner.get.notifyValueChanged(fireEvent)
  }

  def value: Int = current

  def min: Int = _min

  /** Sets min value. If current is lesser than min, the current value is set to min value. */
  def min_=(min: Int): Unit = {
    require(min <= _max, "min can't be > max")
    _min = min
    if (min >= 0) {
      textFieldFilter.acceptNegativeValues = false
    } else {
      textFieldFilter.acceptNegativeValues = true
    }
    if (current < min) {
      current = min
      spinner.get.notifyValueChanged(spinner.get.programmaticChangeEvents)
    }
  }

  def max: Int = _max

  /** Sets max value. If current is greater than max, the current value is set to max value. */
  def max_=(max: Int): Unit = {
    require(_min <= max, "min can't be > max")
    _max = max
    if (current > max) {
      current = max
      spinner.get.notifyValueChanged(spinner.get.programmaticChangeEvents)
    }
  }

  def step: Int = _step

  def step_=(step: Int): Unit = {
    require(step > 0, "step must be > 0")
    _step = step
  }

  private def checkInputBounds(input: String): Boolean =
    try {
      val x = Integer.parseInt(input).toFloat
      x >= _min && x <= _max
    } catch {
      case _: NumberFormatException => false
    }
}
