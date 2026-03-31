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

import java.math.{ BigDecimal, RoundingMode }

import sge.utils.Nullable
import sge.visui.util.{ FloatDigitsOnlyFilter, InputValidator, IntDigitsOnlyFilter, NumberDigitsTextFieldFilter, Validators }

/** Spinner models allowing to select float values. Uses float to store values, good for small numbers with low precision. If high precision is required or very big numbers are used then
  * [[FloatSpinnerModel]] should be used. If only ints are needed then [[IntSpinnerModel]] should be used.
  * @author
  *   Kotcrab
  * @see
  *   [[FloatSpinnerModel]]
  * @see
  *   [[IntSpinnerModel]]
  * @since 1.0.2
  */
class SimpleFloatSpinnerModel(
  initialValue:           Float,
  private var _min:       Float,
  private var _max:       Float,
  private var _step:      Float,
  private var _precision: Int
) extends AbstractSpinnerModel(false) {

  require(_min <= _max, "min can't be > max")
  require(_step > 0, "step must be > 0")
  require(_precision >= 0, "precision must be >= 0")

  private var current:         Float                       = initialValue
  private var textFieldFilter: NumberDigitsTextFieldFilter = scala.compiletime.uninitialized

  private val boundsValidator: InputValidator = (input: String) => checkInputBounds(input)

  def this(initialValue: Float, min: Float, max: Float) = this(initialValue, min, max, 1, 1)

  def this(initialValue: Float, min: Float, max: Float, step: Float) = this(initialValue, min, max, step, 1)

  override def bind(spinner: Spinner): Unit = {
    super.bind(spinner)
    setPrecisionInternal(_precision, notifySpinner = false)
    spinner.notifyValueChanged(true)
  }

  override def textChanged(): Unit = {
    val text = spinner.get.textField.text
    if (text == "") {
      current = _min
    } else if (checkInputBounds(text)) {
      current = java.lang.Float.parseFloat(text)
    }
  }

  override protected def incrementModel(): Boolean =
    if (current + _step > _max) {
      if (current == _max) {
        if (wrap) { current = _min; true }
        else false
      } else {
        current = _max
        true
      }
    } else {
      current += _step
      true
    }

  override protected def decrementModel(): Boolean =
    if (current - _step < _min) {
      if (current == _min) {
        if (wrap) { current = _max; true }
        else false
      } else {
        current = _min
        true
      }
    } else {
      current -= _step
      true
    }

  override def text: String =
    if (_precision >= 1) {
      // dealing with float rounding errors
      var bd = new BigDecimal(String.valueOf(current))
      bd = bd.setScale(_precision, RoundingMode.HALF_UP)
      String.valueOf(bd.floatValue())
    } else {
      String.valueOf(current.toInt)
    }

  def precision: Int = _precision

  /** Sets precision of this selector. Precision defines how many digits after decimal point can be entered. By default this is set to 0, meaning that only integers are allowed. Setting precision to 1
    * would allow 0.0, precision = 2 would allow 0.00 and etc.
    */
  def precision_=(precision: Int): Unit = setPrecisionInternal(precision, notifySpinner = true)

  private def setPrecisionInternal(precision: Int, notifySpinner: Boolean): Unit = {
    require(precision >= 0, "Precision can't be < 0")
    _precision = precision

    val valueText = spinner.get.textField
    valueText.getValidators.clear()
    valueText.addValidator(boundsValidator)
    if (precision == 0) {
      valueText.addValidator(Validators.INTEGERS)
      textFieldFilter = new IntDigitsOnlyFilter(true)
      valueText.setTextFieldFilter(Nullable(textFieldFilter))
    } else {
      valueText.addValidator(Validators.FLOATS)
      valueText.addValidator { (input: String) =>
        val dotIndex = input.indexOf('.')
        if (dotIndex == -1) true
        else input.length - input.indexOf('.') - 1 <= precision
      }
      textFieldFilter = new FloatDigitsOnlyFilter(true)
      valueText.setTextFieldFilter(Nullable(textFieldFilter))
    }

    textFieldFilter.useFieldCursorPosition = true
    if (_min >= 0) {
      textFieldFilter.acceptNegativeValues = false
    } else {
      textFieldFilter.acceptNegativeValues = true
    }

    if (notifySpinner) {
      spinner.get.notifyValueChanged(spinner.get.programmaticChangeEvents)
    }
  }

  def setValue(newValue: Float): Unit = setValue(newValue, spinner.get.programmaticChangeEvents)

  def setValue(newValue: Float, fireEvent: Boolean): Unit = {
    if (newValue > _max) {
      current = _max
    } else if (newValue < _min) {
      current = _min
    } else {
      current = newValue
    }
    spinner.get.notifyValueChanged(fireEvent)
  }

  def value: Float = current

  def min: Float = _min

  /** Sets min value, if current is lesser than min, the current value is set to min value */
  def min_=(min: Float): Unit = {
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

  def max: Float = _max

  /** Sets max value. If current is greater than max, the current value is set to max value. */
  def max_=(max: Float): Unit = {
    require(_min <= max, "min can't be > max")
    _max = max
    if (current > max) {
      current = max
      spinner.get.notifyValueChanged(spinner.get.programmaticChangeEvents)
    }
  }

  def step: Float = _step

  def step_=(step: Float): Unit = {
    require(step > 0, "step must be > 0")
    _step = step
  }

  private def checkInputBounds(input: String): Boolean =
    try {
      val x = java.lang.Float.parseFloat(input)
      x >= _min && x <= _max
    } catch {
      case _: NumberFormatException => false
    }
}
