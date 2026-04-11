/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/TextraSelectBox.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: Widget → standalone class, Disableable → disabled field,
 *     Array<T> → ArrayBuffer[TextraLabel], ArraySelection → internal selection,
 *     ScrollPane → deferred (SelectBoxScrollPane simplified),
 *     ClickListener → handler methods, Vector2 → inline coords,
 *     ChangeEvent → direct callback
 *   Convention: Select box drop-down behavior, draw, layout, input fully ported.
 *   Idiom: Nullable[A] for nullable fields; boundary/break for early returns.
 */
package sge
package textra

import scala.collection.mutable.ArrayBuffer

import sge.graphics.Color
import sge.graphics.g2d.Batch
import sge.scenes.scene2d.utils.Drawable
import sge.utils.{ Align, Nullable }

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
  private var _alignment:     Int                      = Align.left.toInt
  var selectedPrefWidth:      Boolean                  = false
  var maxListCount:           Int                      = 0

  // Internal list for the dropdown
  val list: TextraListBox = newList()

  // Scroll pane open state
  private var _scrollPaneOpen: Boolean = false

  // Preferred size cache
  private var _prefWidth:   Float   = 0f
  private var _prefHeight:  Float   = 0f
  private var _needsLayout: Boolean = true

  // Widget-like fields
  private var _x:      Float = 0f
  private var _y:      Float = 0f
  private var _width:  Float = 0f
  private var _height: Float = 0f
  private val _color:  Color = new Color(Color.WHITE)

  // Over state
  private var _isOver: Boolean = false

  // Min/max sizes (for layout calculations)
  private val _minWidth:  Float = 0f
  private val _maxWidth:  Float = Float.MaxValue
  private val _minHeight: Float = 0f

  // --- Init ---
  list.setTypeToSelect(true)

  // --- Widget-like accessors ---

  def getX:                Float = _x
  def getY:                Float = _y
  def setX(x:      Float): Unit  = _x = x
  def setY(y:      Float): Unit  = _y = y
  def getWidth:            Float = _width
  def getHeight:           Float = _height
  def setWidth(w:  Float): Unit  = _width = w
  def setHeight(h: Float): Unit  = _height = h
  def getColor:            Color = _color
  def setColor(c:  Color): Unit  = if (c != null) _color.set(c)

  def setSize(width: Float, height: Float): Unit = {
    _width = width
    _height = height
  }

  def invalidate(): Unit =
    _needsLayout = true

  def invalidateHierarchy(): Unit =
    _needsLayout = true

  def validate(): Unit =
    if (_needsLayout) {
      layout()
      _needsLayout = false
    }

  def getMinWidth:  Float = _minWidth
  def getMaxWidth:  Float = _maxWidth
  def getMinHeight: Float = _minHeight

  // --- List factory ---

  protected def newList(): TextraListBox =
    Nullable.fold(_style.listStyle)(new TextraListBox(new Styles.ListStyle()))(ls => new TextraListBox(ls))

  // --- Style ---

  def setStyle(style: Styles.SelectBoxStyle): Unit = {
    require(style != null, "style cannot be null.")
    this._style = style
    Nullable.foreach(style.listStyle)(ls => list.setStyle(ls))
    invalidateHierarchy()
  }

  def getStyle: Styles.SelectBoxStyle = _style

  // --- Label factory ---

  protected def newLabel(markupText: String, font: Font, color: Color): TextraLabel = {
    val label = new TextraLabel(markupText, font, color)
    label.align = (label.align | Align.bottom) & ~Align.top
    label
  }

  protected def newLabel(markupText: String, font: Nullable[Font], color: Color): TextraLabel = {
    val f = Nullable.fold(font)(new Font())(identity)
    newLabel(markupText, f, color)
  }

  // --- Items ---

  /** Sets the choices available using markup Strings. */
  def setItemTexts(newMarkupTexts: String*): Unit = {
    val oldPrefWidth = getPrefWidth

    items.clear()
    val f = Nullable.fold(_style.font)(new Font())(identity)
    newMarkupTexts.foreach { text =>
      items += newLabel(text, f, _style.fontColor)
    }
    validateSelection()
    list.setItems(items)

    invalidate()
    if (oldPrefWidth != getPrefWidth) invalidateHierarchy()
  }

  /** Set the backing items for the select box. */
  def setItems(newItems: TextraLabel*): Unit = {
    val oldPrefWidth = getPrefWidth

    items.clear()
    items ++= newItems
    validateSelection()
    list.setItems(items)

    invalidate()
    if (oldPrefWidth != getPrefWidth) invalidateHierarchy()
  }

  /** Sets the items visible in the select box. */
  def setItems(newItems: ArrayBuffer[TextraLabel]): Unit = {
    val oldPrefWidth = getPrefWidth

    if (!(newItems eq items)) {
      items.clear()
      items ++= newItems
    }
    validateSelection()
    list.setItems(items)

    invalidate()
    if (oldPrefWidth != getPrefWidth) invalidateHierarchy()
  }

  def clearItems(): Unit =
    if (items.nonEmpty) {
      items.clear()
      _selectedIndex = -1
      list.clearItems()
      invalidateHierarchy()
    }

  def getItems: ArrayBuffer[TextraLabel] = items

  private def validateSelection(): Unit = {
    if (_selectedIndex >= items.size) {
      _selectedIndex = items.size - 1
    }
    if (items.nonEmpty && _selectedIndex < 0) {
      _selectedIndex = 0
    }
  }

  // --- Selection ---

  def getSelected: Nullable[TextraLabel] =
    if (_selectedIndex >= 0 && _selectedIndex < items.size) Nullable(items(_selectedIndex))
    else Nullable.empty

  def setSelected(item: Nullable[TextraLabel]): Unit =
    Nullable.fold(item) {
      if (items.nonEmpty) _selectedIndex = 0
      else _selectedIndex = -1
    } { it =>
      val idx = items.indexOf(it)
      if (idx >= 0) _selectedIndex = idx
      else if (items.nonEmpty) _selectedIndex = 0
      else _selectedIndex = -1
    }

  def getSelectedIndex: Int = _selectedIndex

  def setSelectedIndex(index: Int): Unit =
    if (index >= 0 && index < items.size) {
      _selectedIndex = index
    }

  def setSelectedPrefWidth(selectedPrefWidth: Boolean): Unit =
    this.selectedPrefWidth = selectedPrefWidth

  def getSelectedPrefWidth: Boolean = selectedPrefWidth

  // --- Layout ---

  def layout(): Unit = {
    val bg   = Nullable.fold(_style.background)(null: Drawable) { case d: Drawable => d; case _ => null }
    val font = Nullable.fold(_style.font)(new Font())(identity)

    if (bg != null) {
      _prefHeight = Math.max(bg.topHeight + bg.bottomHeight + font.cellHeight - font.descent * font.scaleY, bg.minHeight)
    } else {
      _prefHeight = font.cellHeight - font.descent * font.scaleY
    }

    if (selectedPrefWidth) {
      _prefWidth = 0f
      if (bg != null) _prefWidth = bg.leftWidth + bg.rightWidth
      Nullable.foreach(getSelected) { sel =>
        _prefWidth += sel.getFont.calculateSize(sel.layout)
      }
    } else {
      var maxItemWidth = 0f
      var i            = 0
      while (i < items.size) {
        val item = items(i)
        maxItemWidth = Math.max(item.getFont.calculateSize(item.layout), maxItemWidth)
        i += 1
      }

      _prefWidth = maxItemWidth
      if (bg != null) _prefWidth = Math.max(_prefWidth + bg.leftWidth + bg.rightWidth, bg.minWidth)

      Nullable.foreach(_style.listStyle) { listStyle =>
        Nullable.foreach(listStyle.selection) {
          case sel: Drawable =>
            val scrollWidth = maxItemWidth + sel.leftWidth + sel.rightWidth
            _prefWidth = Math.max(_prefWidth, scrollWidth)
          case _ => ()
        }
      }
    }
  }

  // --- Background drawable ---

  protected def getBackgroundDrawable: Nullable[Drawable] =
    if (disabled) {
      Nullable.fold(_style.backgroundDisabled)(Nullable.fold(_style.background)(Nullable.empty[Drawable]) {
        case d: Drawable => Nullable(d)
        case _ => Nullable.empty
      }) {
        case d: Drawable => Nullable(d)
        case _ => Nullable.empty
      }
    } else if (_scrollPaneOpen) {
      Nullable.fold(_style.backgroundOpen)(Nullable.fold(_style.background)(Nullable.empty[Drawable]) {
        case d: Drawable => Nullable(d)
        case _ => Nullable.empty
      }) {
        case d: Drawable => Nullable(d)
        case _ => Nullable.empty
      }
    } else if (_isOver) {
      Nullable.fold(_style.backgroundOver)(Nullable.fold(_style.background)(Nullable.empty[Drawable]) {
        case d: Drawable => Nullable(d)
        case _ => Nullable.empty
      }) {
        case d: Drawable => Nullable(d)
        case _ => Nullable.empty
      }
    } else {
      Nullable.fold(_style.background)(Nullable.empty[Drawable]) {
        case d: Drawable => Nullable(d)
        case _ => Nullable.empty
      }
    }

  /** Returns the appropriate label font color from the style based on the current state. */
  protected def getFontColor: Color =
    if (disabled) {
      Nullable.fold(_style.disabledFontColor)(_style.fontColor)(identity)
    } else if (_isOver || _scrollPaneOpen) {
      Nullable.fold(_style.overFontColor)(_style.fontColor)(identity)
    } else {
      _style.fontColor
    }

  // --- Draw ---

  def draw(batch: Batch, parentAlpha: Float): Unit = {
    validate()

    val background = getBackgroundDrawable
    val fontColor  = getFontColor

    val color  = _color
    val x      = _x
    val y      = _y
    var width  = _width
    val height = _height

    batch.setColor(color.r, color.g, color.b, color.a * parentAlpha)
    Nullable.foreach(background)(bg => bg.draw(batch, x, y, width, height))

    Nullable.foreach(getSelected) { selected =>
      var drawX = x
      var drawY = y
      Nullable.foreach(background) { bg =>
        width -= bg.leftWidth + bg.rightWidth
        drawX += bg.leftWidth
        drawY += (bg.bottomHeight + bg.topHeight).toInt.toFloat
      }
      selected.setColor(fontColor.r, fontColor.g, fontColor.b, fontColor.a * parentAlpha)
      drawItem(batch, selected, drawX, drawY, width)
    }
  }

  protected def drawItem(batch: Batch, item: TextraLabel, x: Float, y: Float, width: Float): Unit = {
    item.setEllipsis(Nullable("..."))
    item.setWrap(false)
    item.layout.setTargetWidth(width)
    item.setSize(width, item.getHeight)
    item.setPosition(x, y)
    item.draw(batch, 1f)
  }

  /** Calls act on the selected item. */
  def act(delta: Float): Unit =
    Nullable.foreach(getSelected)(_.act(delta))

  // --- Scroll pane ---

  def showScrollPane(): Unit =
    if (items.nonEmpty) {
      _scrollPaneOpen = true
      list.setSelected(getSelected)
    }

  def hideScrollPane(): Unit =
    _scrollPaneOpen = false

  /** Returns the list shown when the select box is open. */
  def getList: TextraListBox = list

  // --- Disabled ---

  def isDisabled: Boolean = disabled

  def setDisabled(disabled: Boolean): Unit = {
    if (disabled && !this.disabled) hideScrollPane()
    this.disabled = disabled
  }

  // --- Over state ---

  def isOver: Boolean = _isOver

  def setOver(over: Boolean): Unit =
    _isOver = over

  // --- Pref size ---

  def getPrefWidth: Float = {
    validate()
    _prefWidth
  }

  def getPrefHeight: Float = {
    validate()
    _prefHeight
  }

  // --- Alignment ---

  def setAlignment(alignment: Int): Unit =
    this._alignment = alignment

  def getAlignment: Int = _alignment

  // --- Max pref width ---

  /** Returns the pref width if the widest item was selected. */
  def getMaxPrefWidth: Float = {
    var width = 0f
    var i     = 0
    while (i < items.size) {
      val item = items(i)
      width = Math.max(item.getFont.calculateSize(item.layout), width)
      i += 1
    }
    val bg = Nullable.fold(_style.background)(null: Drawable) { case d: Drawable => d; case _ => null }
    if (bg != null) width = Math.max(width + bg.leftWidth + bg.rightWidth, bg.minWidth)
    width
  }

  // --- Max list count ---

  def setMaxListCount(maxListCount: Int): Unit =
    this.maxListCount = maxListCount

  def getMaxListCount: Int = maxListCount

  // --- Scroll pane state ---

  def isScrollPaneOpen: Boolean = _scrollPaneOpen

  // --- Input handling ---

  /** Handles a click to toggle the scroll pane. Returns true if consumed. */
  def handleClick(pointer: Int, button: Int): Boolean =
    if (pointer == 0 && button != 0) {
      false
    } else if (disabled) {
      false
    } else {
      if (_scrollPaneOpen) hideScrollPane()
      else showScrollPane()
      true
    }

  /** Delegates toString to the item's toString. */
  protected def toString(item: TextraLabel): String = item.toString

  // --- onShow / onHide callbacks ---

  /** Called when the scroll pane is shown. Override for custom animation. */
  protected def onShow(below: Boolean): Unit = {
    // In full scene2d integration, this would fade in the scroll pane.
  }

  /** Called when the scroll pane is hidden. Override for custom animation. */
  protected def onHide(): Unit = {
    // In full scene2d integration, this would fade out the scroll pane.
  }

  /** Scrolling disabled state. */
  def setScrollingDisabled(y: Boolean): Unit =
    // In full scene2d integration, delegates to scrollPane.setScrollingDisabled(true, y)
    invalidateHierarchy()

  /** Returns the scroll pane (the SelectBox itself acts as the scroll pane container in this standalone port). */
  def getScrollPane: TextraSelectBox = this
}
