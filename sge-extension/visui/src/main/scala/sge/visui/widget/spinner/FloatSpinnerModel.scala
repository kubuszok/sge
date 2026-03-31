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

/** Spinner model allowing to select float values. Uses [[BigDecimal]] to support high precision and support large numbers. Consider using [[SimpleFloatSpinnerModel]] when such high precision is not
  * needed as it will be faster and simpler to use.
  * @author
  *   Kotcrab
  * @see
  *   [[FloatSpinnerModel]]
  * @see
  *   [[IntSpinnerModel]]
  * @since 1.0.2
  */
class FloatSpinnerModel(
  initialValue:       BigDecimal,
  private var _min:   BigDecimal,
  private var _max:   BigDecimal,
  private var _step:  BigDecimal,
  private var _scale: Int
) extends AbstractSpinnerModel(false) {

  require(_min.compareTo(_max) <= 0, "min can't be > max")
  require(_step.compareTo(BigDecimal.ZERO) > 0, "step must be > 0")
  require(_scale >= 0, "scale must be >= 0")

  private var current:         BigDecimal                  = initialValue
  private var textFieldFilter: NumberDigitsTextFieldFilter = scala.compiletime.uninitialized

  private val boundsValidator: InputValidator = (input: String) => checkInputBounds(input)

  def this(initialValue: String, min: String, max: String) =
    this(new BigDecimal(initialValue), new BigDecimal(min), new BigDecimal(max), new BigDecimal("1"), 1)

  def this(initialValue: String, min: String, max: String, step: String) =
    this(new BigDecimal(initialValue), new BigDecimal(min), new BigDecimal(max), new BigDecimal(step), 1)

  def this(initialValue: String, min: String, max: String, step: String, scale: Int) =
    this(new BigDecimal(initialValue), new BigDecimal(min), new BigDecimal(max), new BigDecimal(step), scale)

  override def bind(spinner: Spinner): Unit = {
    super.bind(spinner)
    setScaleInternal(_scale, notifySpinner = false)
    spinner.notifyValueChanged(true)
  }

  override def textChanged(): Unit = {
    val text = spinner.get.textField.text
    if (text == "") {
      current = _min.setScale(_scale, RoundingMode.HALF_UP)
    } else if (checkInputBounds(text)) {
      current = new BigDecimal(text)
    }
  }

  override protected def incrementModel(): Boolean =
    if (current.add(_step).compareTo(_max) > 0) {
      if (current.compareTo(_max) == 0) {
        if (wrap) { current = _min.setScale(_scale, RoundingMode.HALF_UP); true }
        else false
      } else {
        current = _max.setScale(_scale, RoundingMode.HALF_UP)
        true
      }
    } else {
      current = current.add(_step)
      true
    }

  override protected def decrementModel(): Boolean =
    if (current.subtract(_step).compareTo(_min) < 0) {
      if (current.compareTo(_min) == 0) {
        if (wrap) { current = _max.setScale(_scale, RoundingMode.HALF_UP); true }
        else false
      } else {
        current = _min.setScale(_scale, RoundingMode.HALF_UP)
        true
      }
    } else {
      current = current.subtract(_step)
      true
    }

  override def text: String = current.toPlainString

  def scale: Int = _scale

  /** Sets scale of this selector. Scale defines how many digits after decimal point can be entered. By default this is set to 0, meaning that only integers are allowed. Setting scale to 1 would allow
    * 0.0, scale = 2 would allow 0.00 and etc.
    */
  def scale_=(scale: Int): Unit = setScaleInternal(scale, notifySpinner = true)

  private def setScaleInternal(scale: Int, notifySpinner: Boolean): Unit = {
    require(scale >= 0, "Scale can't be < 0")
    _scale = scale
    current = current.setScale(scale, RoundingMode.HALF_UP)

    val valueText = spinner.get.textField
    valueText.getValidators.clear()
    valueText.addValidator(boundsValidator)
    if (scale == 0) {
      valueText.addValidator(Validators.INTEGERS)
      textFieldFilter = new IntDigitsOnlyFilter(true)
      valueText.setTextFieldFilter(Nullable(textFieldFilter))
    } else {
      valueText.addValidator(Validators.FLOATS)
      valueText.addValidator { (input: String) =>
        val dotIndex = input.indexOf('.')
        if (dotIndex == -1) true
        else input.length - input.indexOf('.') - 1 <= scale
      }
      textFieldFilter = new FloatDigitsOnlyFilter(true)
      valueText.setTextFieldFilter(Nullable(textFieldFilter))
    }

    textFieldFilter.useFieldCursorPosition = true
    if (_min.compareTo(BigDecimal.ZERO) >= 0) {
      textFieldFilter.acceptNegativeValues = false
    } else {
      textFieldFilter.acceptNegativeValues = true
    }

    if (notifySpinner) {
      spinner.get.notifyValueChanged(spinner.get.programmaticChangeEvents)
    }
  }

  def setValue(newValue: BigDecimal): Unit = setValue(newValue, spinner.get.programmaticChangeEvents)

  def setValue(newValue: BigDecimal, fireEvent: Boolean): Unit = {
    if (newValue.compareTo(_max) > 0) {
      current = _max.setScale(_scale, RoundingMode.HALF_UP)
    } else if (newValue.compareTo(_min) < 0) {
      current = _min.setScale(_scale, RoundingMode.HALF_UP)
    } else {
      current = newValue.setScale(_scale, RoundingMode.HALF_UP)
    }
    spinner.get.notifyValueChanged(fireEvent)
  }

  def value: BigDecimal = current

  def min: BigDecimal = _min

  /** Sets min value. If current is lesser than min, the current value is set to min value */
  def min_=(min: BigDecimal): Unit = {
    require(min.compareTo(_max) <= 0, "min can't be > max")
    _min = min
    if (min.compareTo(BigDecimal.ZERO) >= 0) {
      textFieldFilter.acceptNegativeValues = false
    } else {
      textFieldFilter.acceptNegativeValues = true
    }
    if (current.compareTo(min) < 0) {
      current = min.setScale(_scale, RoundingMode.HALF_UP)
      spinner.get.notifyValueChanged(spinner.get.programmaticChangeEvents)
    }
  }

  def max: BigDecimal = _max

  /** Sets max value. If current is greater than max, the current value is set to max value. */
  def max_=(max: BigDecimal): Unit = {
    require(_min.compareTo(max) <= 0, "min can't be > max")
    _max = max
    if (current.compareTo(max) > 0) {
      current = max.setScale(_scale, RoundingMode.HALF_UP)
      spinner.get.notifyValueChanged(spinner.get.programmaticChangeEvents)
    }
  }

  def step: BigDecimal = _step

  def step_=(step: BigDecimal): Unit = {
    require(step.compareTo(BigDecimal.ZERO) > 0, "step must be > 0")
    _step = step
  }

  private def checkInputBounds(input: String): Boolean =
    try {
      val x = new BigDecimal(input)
      x.compareTo(_min) >= 0 && x.compareTo(_max) <= 0
    } catch {
      case _: NumberFormatException => false
    }
}
