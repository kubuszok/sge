/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/TextraSelectBox.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: Widget → standalone class, Disableable → trait field,
 *     Array<T> → ArrayBuffer[T], ArraySelection → deferred,
 *     ScrollPane → deferred, ClickListener → deferred,
 *     Vector2 → deferred, ChangeEvent → deferred
 *   Convention: Select box drop-down behavior preserved in API;
 *     actual rendering and input handling deferred until scene2d wiring.
 */
package sge
package textra

import scala.collection.mutable.ArrayBuffer
import sge.utils.Nullable

/** A select box (aka a drop-down list) allows a user to choose one of a number of values from a list. When inactive, the selected value is displayed. When activated, it shows the list of values that
  * may be selected.
  *
  * ChangeEvent is fired when the select box selection changes.
  */
class TextraSelectBox(style: Styles.SelectBoxStyle) {

  private var _style:         Styles.SelectBoxStyle    = style
  val items:                  ArrayBuffer[TextraLabel] = ArrayBuffer.empty
  private var _selectedIndex: Int                      = -1
  var disabled:               Boolean                  = false
  private var _alignment:     Int                      = 8 // Align.left
  var selectedPrefWidth:      Boolean                  = false
  var maxListCount:           Int                      = 0

  def setStyle(style: Styles.SelectBoxStyle): Unit =
    this._style = style

  def getStyle: Styles.SelectBoxStyle = _style

  def setMaxListCount(maxListCount: Int): Unit =
    this.maxListCount = maxListCount

  def getMaxListCount: Int = maxListCount

  def getItems: ArrayBuffer[TextraLabel] = items

  def setItems(newItems: ArrayBuffer[TextraLabel]): Unit = {
    items.clear()
    items ++= newItems
    if (items.nonEmpty && _selectedIndex < 0) _selectedIndex = 0
    if (_selectedIndex >= items.size) _selectedIndex = items.size - 1
  }

  def getSelectedIndex: Int = _selectedIndex

  def setSelectedIndex(index: Int): Unit =
    if (index >= -1 && index < items.size) {
      _selectedIndex = index
    }

  def getSelected: Nullable[TextraLabel] =
    if (_selectedIndex >= 0 && _selectedIndex < items.size) Nullable(items(_selectedIndex))
    else Nullable.empty

  def clearItems(): Unit = {
    items.clear()
    _selectedIndex = -1
  }

  def setAlignment(alignment: Int): Unit =
    this._alignment = alignment

  def getAlignment: Int = _alignment

  def isDisabled: Boolean = disabled

  def setDisabled(disabled: Boolean): Unit =
    this.disabled = disabled

  def showScrollPane(): Unit = {
    // Deferred: requires scene2d Stage integration
  }

  def hideScrollPane(): Unit = {
    // Deferred: requires scene2d Stage integration
  }
}
