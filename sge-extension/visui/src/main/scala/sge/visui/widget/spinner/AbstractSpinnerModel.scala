/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 78
 * Covenant-baseline-methods: AbstractSpinnerModel,_wrap,allowRebind,allowRebind_,bind,decrement,decrementModel,increment,incrementModel,spinner,valueChanged,wrap,wrap_
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/spinner/AbstractSpinnerModel.java
 * Covenant-verified: 2026-04-19
 */
package sge
package visui
package widget
package spinner

import sge.utils.Nullable

/** Basic implementation of [[SpinnerModel]] simplifying event handling for custom models.
  * @author
  *   Kotcrab
  * @see
  *   [[SpinnerModel]]
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
abstract class AbstractSpinnerModel(private var _allowRebind: Boolean) extends SpinnerModel {
  protected var spinner: Nullable[Spinner] = Nullable.empty

  private var _wrap: Boolean = false

  override def bind(spinner: Spinner): Unit = {
    if (this.spinner.isDefined && !_allowRebind) {
      throw new IllegalStateException("this spinner model can't be reused")
    }
    this.spinner = Nullable(spinner)
  }

  /** Step model up by one. Event and spinner update will be handled by [[AbstractSpinnerModel]].
    * @return
    *   true if value was changed, false otherwise.
    */
  protected def incrementModel(): Boolean

  /** Step model down by one. Event and spinner update will be handled by [[AbstractSpinnerModel]].
    * @return
    *   true if value was changed, false otherwise.
    */
  protected def decrementModel(): Boolean

  final override def increment(): Boolean = increment(spinner.get.programmaticChangeEvents)

  final override def increment(fireEvent: Boolean): Boolean = {
    val valueChanged = incrementModel()
    if (valueChanged) spinner.get.notifyValueChanged(fireEvent)
    valueChanged
  }

  final override def decrement(): Boolean = decrement(spinner.get.programmaticChangeEvents)

  final override def decrement(fireEvent: Boolean): Boolean = {
    val valueChanged = decrementModel()
    if (valueChanged) spinner.get.notifyValueChanged(fireEvent)
    valueChanged
  }

  override def wrap: Boolean = _wrap

  override def wrap_=(wrap: Boolean): Unit = _wrap = wrap

  /** @return true if this model can be reused with different spinner, false otherwise */
  def allowRebind: Boolean = _allowRebind

  protected def allowRebind_=(allowRebind: Boolean): Unit = _allowRebind = allowRebind
}
