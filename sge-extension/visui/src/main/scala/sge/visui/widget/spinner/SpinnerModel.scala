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

/** Classes implementing this trait represent model that can be used with [[Spinner]]. Model defines what is scrolled in spinner (eg. int numbers, floats or some arbitrary strings), set-ups input
  * validation and updates its value if user changed text in spinner value text field.
  *
  * Classes wanting to implement this trait should inherit from [[AbstractSpinnerModel]] to simplify event handling.
  * @author
  *   Kotcrab
  * @see
  *   [[AbstractSpinnerModel]]
  * @see
  *   [[IntSpinnerModel]]
  * @see
  *   [[FloatSpinnerModel]]
  * @see
  *   [[SimpleFloatSpinnerModel]]
  * @see
  *   [[ArraySpinnerModel]]
  * @since 1.0.2
  */
trait SpinnerModel {

  /** Called when model is assigned to [[Spinner]]. When this is called Spinner has been initialised so it's safe to do operation on it such as adding custom validators to text field.
    *
    * If this model can't be reused then in this function it should verify that it is not being bound for the second time.
    *
    * After model has finished its setup it should call [[Spinner.notifyValueChanged]] with true to perform first update and set initial spinner value.
    * @param spinner
    *   that this model was assigned to
    */
  def bind(spinner: Spinner): Unit

  /** Called when spinner text has changed. Usually this is the moment when model has to update its current value variable. If input is invalid when this is called then it should simply be ignored. If
    * field loses focus while it is in invalid state then last valid value will be automatically restored. This should NOT call [[Spinner.notifyValueChanged]].
    */
  def textChanged(): Unit

  /** Steps model up by one. Depending of the implementation this could move model to next item or increment its value by arbitrary amount. Implementation class MUST call
    * [[Spinner.notifyValueChanged]] with fireEvent param set to [[Spinner.programmaticChangeEvents]]
    *
    * @return
    *   true when value was changed, false otherwise
    */
  def increment(): Boolean

  /** Steps model up by one. Depending of the implementation this could move model to next item or increment its value by arbitrary amount. Implementation class MUST call
    * [[Spinner.notifyValueChanged]] using fireEvent param as argument.
    *
    * @return
    *   true when value was changed, false otherwise
    */
  def increment(fireEvent: Boolean): Boolean

  /** Steps model down by one. Depending of the implementation this could move model to previous item or decrement its value by arbitrary amount. Implementation class MUST call
    * [[Spinner.notifyValueChanged]] with fireEvent param set to [[Spinner.programmaticChangeEvents]]
    *
    * @return
    *   true when value was changed, false otherwise
    */
  def decrement(): Boolean

  /** Steps model down by one. Depending of the implementation this could move model to previous item or decrement its value by arbitrary amount. Implementation class MUST call
    * [[Spinner.notifyValueChanged]] using fireEvent param as argument.
    *
    * @return
    *   true when value was changed, false otherwise
    */
  def decrement(fireEvent: Boolean): Boolean

  /** Allows to enable model wrapping: if last element of model is reached and [[decrement]] was called then it will be looped to first element. Same applies for last element and [[increment]]
    * @param wrap
    *   whether to wrap this model or not
    */
  def wrap_=(wrap: Boolean): Unit

  /** @return true if model wrapping is enabled, false otherwise. See [[wrap_=]] */
  def wrap: Boolean

  /** @return text representation of current model value */
  def text: String
}
