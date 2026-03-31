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

import sge.math.MathUtils
import sge.utils.Nullable
import sge.visui.util.InputValidator

import scala.collection.mutable.ArrayBuffer

/** [[Spinner]] model allowing to browse through items from object [[GdxArray]].
  *
  * Note that this (by default) uses item's toString() method to get string representation of objects used to validate that user has entered valid value which due to [[VisValidatableTextField]] nature
  * has to be done for every entered letter. Item's toString() should cache its result internally to optimize this check. To customize how string representation is obtained override [[itemToString]].
  * @author
  *   Kotcrab
  * @since 1.0.2
  */
class ArraySpinnerModel[T] extends AbstractSpinnerModel(false) {

  private val items:        ArrayBuffer[T] = new ArrayBuffer[T]()
  private var current:      Nullable[T]    = Nullable.empty
  private var currentIndex: Int            = -1

  /** Creates empty instance with no items set. Note that spinner with empty array model will be always treated as in invalid state.
    */
  def this(dummy: Unit) = // overload disambiguator
    this()

  /** Creates new instance of [[ArraySpinnerModel]] using provided items.
    * @param items
    *   array containing items for the model. It is copied to new array in order to prevent accidental modification. Array may be empty however in such case spinner will be always in invalid input
    *   state.
    */
  def this(items: ArrayBuffer[T]) = {
    this()
    this.items.addAll(items)
  }

  override def bind(spinner: Spinner): Unit = {
    super.bind(spinner)
    updateCurrentItem(0)
    spinner.textField.addValidator((input: String) => getItemIndexForText(input) != -1)
    spinner.notifyValueChanged(true)
  }

  /** Creates string representation displayed in [[Spinner]] for given object. By default toString() is used.
    * @param item
    *   that string representation should be created. It is necessary to check if item is null!
    * @return
    *   string representation of item
    */
  protected def itemToString(item: Nullable[T]): String =
    if (item.isEmpty) "" else item.get.toString

  private def getItemIndexForText(text: String): Int = {
    var i     = 0
    var found = -1
    while (i < items.size && found == -1) {
      val item = items(i)
      if (itemToString(Nullable(item)) == text) found = i
      i += 1
    }
    found
  }

  override def textChanged(): Unit = {
    val text  = spinner.get.textField.text
    val index = getItemIndexForText(text)
    if (index != -1) updateCurrentItem(index)
  }

  override protected def incrementModel(): Boolean =
    if (currentIndex + 1 >= items.size) {
      if (wrap) { updateCurrentItem(0); true }
      else false
    } else {
      updateCurrentItem(currentIndex + 1)
      true
    }

  override protected def decrementModel(): Boolean =
    if (currentIndex - 1 < 0) {
      if (wrap) { updateCurrentItem(items.size - 1); true }
      else false
    } else {
      updateCurrentItem(currentIndex - 1)
      true
    }

  override def text: String = itemToString(current)

  /** Notifies model that items has changed and view must be refreshed. This will trigger a change event. */
  def invalidateDataSet(): Unit = {
    updateCurrentItem(MathUtils.clamp(currentIndex, 0, items.size - 1))
    spinner.get.notifyValueChanged(true)
  }

  /** @return array containing model items. If you modify returned array you must call [[invalidateDataSet]]. */
  def getItems: ArrayBuffer[T] = items

  /** Changes items of this model. Current index is not preserved. This will trigger a change event. */
  def setItems(newItems: ArrayBuffer[T]): Unit = {
    items.clear()
    items.addAll(newItems)
    currentIndex = 0
    invalidateDataSet()
  }

  /** @return current item index or -1 if items array is empty */
  def getCurrentIndex: Int = currentIndex

  /** @return current item or null if items array is empty */
  def getCurrent: Nullable[T] = current

  /** Sets current item. If array is empty then current value will be set to null. */
  def setCurrent(newIndex: Int): Unit = setCurrent(newIndex, spinner.get.programmaticChangeEvents)

  /** Sets current item. If array is empty then current value will be set to null. */
  def setCurrent(newIndex: Int, fireEvent: Boolean): Unit = {
    updateCurrentItem(newIndex)
    spinner.get.notifyValueChanged(fireEvent)
  }

  /** @param item if does not exist in items array, model item will be set to first item. */
  def setCurrent(item: T): Unit = setCurrent(item, spinner.get.programmaticChangeEvents)

  /** @param item if does not exist in items array, model item will be set to first item. */
  def setCurrent(item: T, fireEvent: Boolean): Unit = {
    val index = items.indexOf(item)
    if (index == -1) {
      setCurrent(0, fireEvent)
    } else {
      setCurrent(index, fireEvent)
    }
  }

  private def updateCurrentItem(newIndex: Int): Unit =
    if (items.size == 0) {
      current = Nullable.empty
      currentIndex = -1
    } else {
      currentIndex = newIndex
      current = Nullable(items(newIndex))
    }
}
