/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/TextraListBox.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: Widget → standalone class, Cullable → trait field,
 *     Array<T> → ArrayBuffer[T], ArraySelection → deferred,
 *     Rectangle → deferred, InputListener → deferred,
 *     ChangeEvent → deferred
 *   Convention: List box selection and item management preserved in API;
 *     actual rendering and input handling deferred until scene2d wiring.
 */
package sge
package textra

import scala.collection.mutable.ArrayBuffer
import sge.utils.Nullable

/** A TextraListBox displays TextraLabels and highlights the currently selected item.
  *
  * A ChangeEvent is fired when the list selection changes. The preferred size of the list box is determined by the text bounds of the items and the size of the ListStyle.selection.
  */
class TextraListBox(style: Styles.ListStyle) {

  protected var _style:       Styles.ListStyle         = style
  protected val items:        ArrayBuffer[TextraLabel] = ArrayBuffer.empty
  private var _selectedIndex: Int                      = -1
  private var alignment:      Int                      = 8 // Align.left
  var pressedIndex:           Int                      = -1
  var overIndex:              Int                      = -1

  /** When true, typing a character while focused will jump to the first item starting with that character. */
  var typeToSelect: Boolean = false

  def setStyle(style: Styles.ListStyle): Unit =
    this._style = style

  def getStyle: Styles.ListStyle = _style

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
    this.alignment = alignment

  def getAlignment: Int = alignment
}
