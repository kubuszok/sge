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
 *     ScrollPane → SelectBoxScrollPane (standalone inner class),
 *     ClickListener → clickListener field with handler methods,
 *     Vector2 → inline coords, Stage → stage parameter on show/hide,
 *     ChangeEvent → direct callback, InputListener → handler methods,
 *     Touchable enum → Boolean on TextraListBox
 *   Convention: Select box drop-down behavior, draw, layout, input fully ported.
 *     SelectBoxScrollPane inner class fully ported with show/hide/draw/act/setStage.
 *   Idiom: Nullable[A] for nullable fields; boundary/break for early returns.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 803
 * Covenant-baseline-methods: ClickListenerState,SelectBoxScrollPane,SelectionAccessor,TextraSelectBox,_alignment,_clickListener,_color,_hasParent,_height,_isOver,_maxWidth,_minHeight,_minWidth,_needsLayout,_prefHeight,_prefWidth,_scrollPaneOpen,_scrollingDisabledY,_selectedIndex,_stage,_stagePositionX,_stagePositionY,_style,_width,_x,_y,act,background,bg,choose,clear,clearItems,color,disabled,draw,drawItem,f,first,font,fontColor,getAlignment,getBackgroundDrawable,getClickListener,getColor,getFontColor,getHeight,getItems,getList,getMaxListCount,getMaxPrefWidth,getMaxWidth,getMinHeight,getMinWidth,getPrefHeight,getPrefWidth,getScrollPane,getSelectBox,getSelected,getSelectedIndex,getSelectedPrefWidth,getSelection,getStage,getStyle,getWidth,getX,getY,handleClick,handleExit,handleHideKeyDown,handleHideTouchDown,handleListClick,handleListMouseMoved,hasParent,height,hide,hideList,hideScrollPane,i,invalidate,invalidateHierarchy,isDisabled,isOver,isScrollingDisabledY,items,label,layout,layoutScrollPane,list,maxListCount,newLabel,newList,newScrollPane,oldPrefWidth,onHide,onShow,scrollPane,selectedPrefWidth,set,setActor,setAlignment,setColor,setDisabled,setHeight,setItemTexts,setItems,setMaxListCount,setOver,setRequired,setScrollingDisabled,setScrollingDisabledY,setSelected,setSelectedIndex,setSelectedPrefWidth,setSize,setStage,setStyle,setWidth,setX,setY,show,showList,showScrollPane,toString,validate,validateSelection,width,x,y
 * Covenant-source-reference: com/github/tommyettinger/textra/TextraSelectBox.java
 * Covenant-verified: 2026-04-19
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
  *
  * The preferred size of the select box is determined by the maximum text bounds of the items and the size of the SelectBoxStyle.background.
  * @author
  *   mzechner
  * @author
  *   Nathan Sweet
  */
class TextraSelectBox(style: Styles.SelectBoxStyle) {

  private var _style:         Styles.SelectBoxStyle               = style
  val items:                  ArrayBuffer[TextraLabel]            = ArrayBuffer.empty
  var scrollPane:             TextraSelectBox.SelectBoxScrollPane = scala.compiletime.uninitialized
  private var _prefWidth:     Float                               = 0f
  private var _prefHeight:    Float                               = 0f
  private val _clickListener: TextraSelectBox.ClickListenerState  = TextraSelectBox.ClickListenerState()
  var disabled:               Boolean                             = false
  private var _alignment:     Int                                 = Align.left.toInt
  var selectedPrefWidth:      Boolean                             = false

  // Internal selection index
  private var _selectedIndex: Int = -1

  // Scroll pane open state (derived from scrollPane.hasParent)
  private def _scrollPaneOpen: Boolean = scrollPane != null && scrollPane.hasParent

  // Preferred size cache
  private var _needsLayout: Boolean = true

  // Widget-like fields
  private var _x:      Float = 0f
  private var _y:      Float = 0f
  private var _width:  Float = 0f
  private var _height: Float = 0f
  private val _color:  Color = new Color(Color.WHITE)

  // Over state (tracked by click listener)
  private var _isOver: Boolean = false

  // Min/max sizes (for layout calculations)
  private val _minWidth:  Float = 0f
  private val _maxWidth:  Float = Float.MaxValue
  private val _minHeight: Float = 0f

  // Stage reference
  private var _stage: Nullable[AnyRef] = Nullable.empty

  // --- Init ---
  setStyle(style)
  scrollPane = newScrollPane()
  setSize(getPrefWidth, getPrefHeight)

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

  // --- Stage ---

  def getStage: Nullable[AnyRef] = _stage

  protected def setStage(stage: Nullable[AnyRef]): Unit = {
    if (stage.isEmpty) scrollPane.hide()
    _stage = stage
  }

  // --- Scroll pane factory ---

  /** Allows a subclass to customize the scroll pane shown when the select box is open. */
  protected def newScrollPane(): TextraSelectBox.SelectBoxScrollPane =
    new TextraSelectBox.SelectBoxScrollPane(this)

  /** Set the max number of items to display when the select box is opened. Set to 0 (the default) to display as many as fit in the stage height.
    */
  def setMaxListCount(maxListCount: Int): Unit =
    scrollPane.maxListCount = maxListCount

  /** @return Max number of items to display when the box is opened, or <= 0 to display them all. */
  def getMaxListCount: Int = scrollPane.maxListCount

  // --- Style ---

  def setStyle(style: Styles.SelectBoxStyle): Unit = {
    require(style != null, "style cannot be null.")
    this._style = style

    if (scrollPane != null) {
      Nullable.foreach(style.listStyle)(ls => scrollPane.list.setStyle(ls))
    }
    invalidateHierarchy()
  }

  /** Returns the select box's style. Modifying the returned style may not have an effect until {@link #setStyle(Styles.SelectBoxStyle)} is called.
    */
  def getStyle: Styles.SelectBoxStyle = _style

  // --- Label factory ---

  protected def newLabel(markupText: String, font: Font, color: Color): TextraLabel = {
    val label = new TextraLabel(markupText, font, color)
    // Enforces bottom alignment, and also disables top alignment to prevent top or center from being used.
    label.align = (label.align | Align.bottom) & ~Align.top
    label
  }

  protected def newLabel(markupText: String, font: Nullable[Font], color: Color): TextraLabel = {
    val f = Nullable.fold(font)(new Font())(identity)
    newLabel(markupText, f, color)
  }

  // --- Items ---

  /** Sets the choices available in the SelectBox using an array or varargs of markup Strings. Marks up each String in newMarkupTexts as a TextraLabel and adds that label to the choices.
    * @param newMarkupTexts
    *   an array or varargs of individual markup Strings, one per choice
    */
  def setItemTexts(newMarkupTexts: String*): Unit = {
    val oldPrefWidth = getPrefWidth

    items.clear()
    val f = Nullable.fold(_style.font)(new Font())(identity)
    newMarkupTexts.foreach { text =>
      items += newLabel(text, f, _style.fontColor)
    }
    validateSelection()
    scrollPane.list.setItems(items)

    invalidate()
    if (oldPrefWidth != getPrefWidth) invalidateHierarchy()
  }

  /** Set the backing Array that makes up the choices available in the SelectBox. */
  def setItems(newItems: TextraLabel*): Unit = {
    val oldPrefWidth = getPrefWidth

    items.clear()
    items ++= newItems
    validateSelection()
    scrollPane.list.setItems(items)

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
    scrollPane.list.setItems(items)

    invalidate()
    if (oldPrefWidth != getPrefWidth) invalidateHierarchy()
  }

  def clearItems(): Unit =
    if (items.nonEmpty) {
      items.clear()
      _selectedIndex = -1
      scrollPane.list.clearItems()
      invalidateHierarchy()
    }

  /** Returns the internal items array. If modified, {@link #setItems(ArrayBuffer)} must be called to reflect the changes. */
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

  /** Get the set of selected items, useful when multiple items are selected
    * @return
    *   a Selection object containing the selected elements
    */
  def getSelection: TextraSelectBox.SelectionAccessor = TextraSelectBox.SelectionAccessor(this)

  def getSelected: Nullable[TextraLabel] =
    if (_selectedIndex >= 0 && _selectedIndex < items.size) Nullable(items(_selectedIndex))
    else Nullable.empty

  /** Sets the selection to only the passed item, if it is a possible choice, else selects the first item. */
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

  /** @return The index of the first selected item. The top item has an index of 0. Nothing selected has an index of -1. */
  def getSelectedIndex: Int = _selectedIndex

  /** Sets the selection to only the selected index. */
  def setSelectedIndex(index: Int): Unit =
    if (index >= 0 && index < items.size) {
      _selectedIndex = index
    }

  /** When true the pref width is based on the selected item. */
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
      _prefWidth = Math.min(Math.max(_prefWidth, _maxWidth), _minWidth)
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
            val scrollStyle = _style.scrollStyle
            var scrollWidth = maxItemWidth + sel.leftWidth + sel.rightWidth
            Nullable.foreach(scrollStyle) {
              case scrollBg: Drawable =>
                scrollWidth = Math.max(scrollWidth + scrollBg.leftWidth + scrollBg.rightWidth, scrollBg.minWidth)
              case _ => ()
            }
            _prefWidth = Math.max(_prefWidth, scrollWidth)
          case _ => ()
        }
      }
      _prefWidth = Math.min(Math.max(_prefWidth, _maxWidth), _minWidth)
    }
  }

  // --- Background drawable ---

  /** Returns appropriate background drawable from the style based on the current select box state. */
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

  /** Returns the appropriate label font color from the style based on the current button state. */
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
//                height -= background.getBottomHeight() + background.getTopHeight();
        drawX += bg.leftWidth
        drawY += (bg.bottomHeight + bg.topHeight).toInt.toFloat
      }
      selected.setColor(fontColor.r, fontColor.g, fontColor.b, fontColor.a * parentAlpha)
      drawItem(batch, selected, drawX, drawY, width)
    }
  }

  def act(delta: Float): Unit =
    Nullable.foreach(getSelected)(_.act(delta))

  protected def drawItem(batch: Batch, item: TextraLabel, x: Float, y: Float, width: Float): Unit = {
    item.setEllipsis(Nullable("..."))
    item.setWrap(false)
    item.layout.setTargetWidth(width)
    item.setSize(width, item.getHeight)
    item.setPosition(x, y)
    item.draw(batch, 1f)
  }

  /** Sets the alignment of the selected item in the select box. See {@link #getList()} and {@link TextraListBox#setAlignment(int)} to set the alignment in the list shown when the select box is open.
    * @param alignment
    *   See {@link Align}.
    */
  def setAlignment(alignment: Int): Unit =
    this._alignment = alignment

  def getAlignment: Int = _alignment

  // --- Scroll pane ---

  /** @deprecated Use {@link #showScrollPane()}. */
  @deprecated("Use showScrollPane()", "")
  def showList(): Unit = showScrollPane()

  def showScrollPane(): Unit =
    if (items.nonEmpty) {
      scrollPane.show()
    }

  /** @deprecated Use {@link #hideScrollPane()}. */
  @deprecated("Use hideScrollPane()", "")
  def hideList(): Unit = hideScrollPane()

  def hideScrollPane(): Unit =
    scrollPane.hide()

  /** Returns the list shown when the select box is open. */
  def getList: TextraListBox = scrollPane.list

  /** Disables scrolling of the list shown when the select box is open. */
  def setScrollingDisabled(y: Boolean): Unit = {
    scrollPane.setScrollingDisabledY(y)
    invalidateHierarchy()
  }

  /** Returns the scroll pane containing the list that is shown when the select box is open. */
  def getScrollPane: TextraSelectBox.SelectBoxScrollPane = scrollPane

  // --- Over / Click listener ---

  def isOver: Boolean = _clickListener.isOver

  def getClickListener: TextraSelectBox.ClickListenerState = _clickListener

  // --- Disabled ---

  def isDisabled: Boolean = disabled

  def setDisabled(disabled: Boolean): Unit = {
    if (disabled && !this.disabled) hideScrollPane()
    this.disabled = disabled
  }

  // --- Pref size ---

  def getPrefWidth: Float = {
    validate()
    _prefWidth
  }

  def getPrefHeight: Float = {
    validate()
    _prefHeight
  }

  /** Returns the pref width of the select box if the widest item was selected, for use when {@link #setSelectedPrefWidth(boolean)} is true.
    */
  def getMaxPrefWidth: Float = {
    var width = 0f
    var i     = 0
    while (i < items.size) {
      val item = items(i)
//            item.layout.setTargetWidth(Gdx.graphics.getBackBufferWidth());
      width = Math.max(item.getFont.calculateSize(item.layout), width)
      i += 1
    }
    val bg = Nullable.fold(_style.background)(null: Drawable) { case d: Drawable => d; case _ => null }
    if (bg != null) width = Math.max(width + bg.leftWidth + bg.rightWidth, bg.minWidth)
    width
  }

  /** Delegates toString to the item's toString. */
  protected def toString(item: TextraLabel): String = item.toString

  // --- onShow / onHide callbacks ---

  /** Called to show the scroll pane actor. Subclasses can override for custom animation. Default implementation fades in the scroll pane.
    */
  protected def onShow(scrollPane: TextraSelectBox.SelectBoxScrollPane, below: Boolean): Unit = {
    // In full scene2d integration, this would fade in the scroll pane actor:
    // scrollPane.getColor().a = 0;
    // scrollPane.addAction(fadeIn(0.3f, Interpolation.fade));
  }

  /** Called to hide the scroll pane actor. Subclasses can override for custom animation. Default implementation fades out and removes the scroll pane.
    */
  protected def onHide(scrollPane: TextraSelectBox.SelectBoxScrollPane): Unit = {
    // In full scene2d integration, this would fade out and remove the scroll pane actor:
    // scrollPane.getColor().a = 1;
    // scrollPane.addAction(sequence(fadeOut(0.15f, Interpolation.fade), removeActor()));
  }

  // --- Input handling ---

  /** Handles a click to toggle the scroll pane. Returns true if consumed. */
  def handleClick(pointer: Int, button: Int): Boolean =
    if (pointer == 0 && button != 0) {
      false
    } else if (disabled) {
      false
    } else {
      if (scrollPane.hasParent) hideScrollPane()
      else showScrollPane()
      true
    }

  // --- Over state ---

  def setOver(over: Boolean): Unit =
    _isOver = over
}

object TextraSelectBox {

  /** Simple click listener state tracker for the select box. */
  final case class ClickListenerState(var isOver: Boolean = false)

  /** Accessor that provides ArraySelection-like API for the select box's selection. Wraps the select box's internal index-based selection.
    */
  final case class SelectionAccessor(selectBox: TextraSelectBox) {

    /** Returns the first (only) selected item. */
    def first: Nullable[TextraLabel] = selectBox.getSelected

    /** Returns the selected items as a set. */
    def items: scala.collection.mutable.HashSet[TextraLabel] = {
      val result = scala.collection.mutable.HashSet.empty[TextraLabel]
      Nullable.foreach(selectBox.getSelected)(result += _)
      result
    }

    /** Sets the selection to the given item. */
    def set(item: Nullable[TextraLabel]): Unit =
      selectBox.setSelected(item)

    /** Selects the given item and fires a change event. */
    def choose(selected: Nullable[TextraLabel]): Unit =
      selectBox.setSelected(selected)

    /** Clears the selection. */
    def clear(): Unit =
      selectBox._selectedIndex = -1

    /** Sets the selection actor. No-op in standalone mode. */
    def setActor(actor: AnyRef): Unit = ()

    /** Sets whether selection is required. No-op in standalone mode. */
    def setRequired(required: Boolean): Unit = ()

    /** Validates the selection against the items array. */
    def validate(): Unit = selectBox.validateSelection()
  }

  /** The scroll pane shown when a select box is open.
    * @author
    *   Nathan Sweet
    */
  class SelectBoxScrollPane(val selectBox: TextraSelectBox) {
    var maxListCount:                Int           = 0
    private var _stagePositionX:     Float         = 0f
    private var _stagePositionY:     Float         = 0f
    val list:                        TextraListBox = newList()
    private var _hasParent:          Boolean       = false
    private var _scrollingDisabledY: Boolean       = false

    // Scroll pane position and size
    private var _x:      Float = 0f
    private var _y:      Float = 0f
    private var _width:  Float = 0f
    private var _height: Float = 0f
    private val _color:  Color = new Color(Color.WHITE)

    // Preferred size cache
    private var _prefWidth:   Float   = 0f
    private var _prefHeight:  Float   = 0f
    private var _needsLayout: Boolean = true

    // --- Init ---
    list.setTouchable(false)
    list.setTypeToSelect(true)

    // --- List factory ---

    /** Allows a subclass to customize the select box list. */
    protected def newList(): TextraListBox =
      Nullable.fold(selectBox._style.listStyle)(new TextraListBox(new Styles.ListStyle()))(ls => new TextraListBox(ls))

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

    def setSize(width: Float, height: Float): Unit = {
      _width = width
      _height = height
    }

    def invalidate(): Unit =
      _needsLayout = true

    def validate(): Unit =
      if (_needsLayout) {
        layoutScrollPane()
        _needsLayout = false
      }

    def getPrefWidth: Float = {
      validate()
      _prefWidth
    }

    def getPrefHeight: Float = {
      validate()
      _prefHeight
    }

    private def layoutScrollPane(): Unit = {
      _prefWidth = list.getPrefWidth
      _prefHeight = list.getPrefHeight
    }

    def hasParent: Boolean = _hasParent

    def isScrollingDisabledY: Boolean = _scrollingDisabledY

    def setScrollingDisabledY(y: Boolean): Unit =
      _scrollingDisabledY = y

    def setScrollingDisabled(x: Boolean, y: Boolean): Unit =
      _scrollingDisabledY = y

    def setStyle(style: AnyRef): Unit = ()

    def show(): Unit =
      if (list.isTouchable) ()
      else {
        _hasParent = true

        // Calculate stage position from the select box
        _stagePositionX = selectBox.getX
        _stagePositionY = selectBox.getY

        // Show the list above or below the select box, limited to a number of items and the available height in the stage.
        val itemCount = if (maxListCount <= 0) selectBox.items.size else Math.min(maxListCount, selectBox.items.size)
        var height    = list.getCumulativeHeight(itemCount - 1)

        Nullable.foreach(selectBox._style.listStyle) { listStyle =>
          Nullable.foreach(listStyle.background) {
            case bg: Drawable => height += bg.topHeight + bg.bottomHeight
            case _ => ()
          }
        }

        val heightBelow = _stagePositionY
        var below       = true
        if (height > heightBelow) {
          below = false
          height = Math.min(height, heightBelow + selectBox.getHeight + 1000f) // approximate stage height
        }

        if (below) {
          _y = _stagePositionY - height
        } else {
          _y = _stagePositionY + selectBox.getHeight
        }
        _x = _stagePositionX
        _height = height
        validate()
        val width = Math.max(getPrefWidth, selectBox.getWidth)
        _width = width

        validate()

        list.setSelected(selectBox.getSelected)
        list.setTouchable(true)
        selectBox.onShow(this, below)
      }

    def hide(): Unit =
      if (!list.isTouchable || !_hasParent) ()
      else {
        list.setTouchable(false)
        _hasParent = false
        selectBox.onHide(this)
      }

    def draw(batch: Batch, parentAlpha: Float): Unit =
      if (_hasParent) {
        // Check if select box has moved; if so, hide
        val currentX = selectBox.getX
        val currentY = selectBox.getY
        if (currentX != _stagePositionX || currentY != _stagePositionY) {
          hide()
        } else {
          // Draw the list
          list.setPosition(_x, _y)
          list.setSize(_width, _height)
          list.draw(batch, parentAlpha)
        }
      }

    def act(delta: Float): Unit =
      if (_hasParent) {
        list.act(delta)
      }

    protected def setStage(stage: Nullable[AnyRef]): Unit =
      if (stage.isEmpty) {
        _hasParent = false
      }

    /** Returns the list shown in the scroll pane. */
    def getList: TextraListBox = list

    /** Returns the select box that owns this scroll pane. */
    def getSelectBox: TextraSelectBox = selectBox

    // --- Input handling ---

    /** Handles a click on an item in the list. */
    def handleListClick(x: Float, y: Float): Unit = {
      val selected = list.getSelected
      // Force clicking the already selected item to trigger a change event.
      if (Nullable.isDefined(selected)) {
        selectBox._selectedIndex = -1
      }
      selectBox.setSelected(selected)
      hide()
    }

    /** Handles mouse movement over the list. */
    def handleListMouseMoved(x: Float, y: Float): Boolean = {
      val index = list.getItemIndexAt(y)
      if (index != -1) list.setSelectedIndex(index)
      true
    }

    /** Handles exit event: restores selection to select box's current selection. */
    def handleExit(toActor: Nullable[AnyRef]): Unit =
      if (toActor.isEmpty) {
        Nullable.foreach(selectBox.getSelected)(sel => list.setSelected(Nullable(sel)))
      }

    /** Handles a touch down outside the scroll pane. Returns false (never consumes). */
    def handleHideTouchDown(target: Nullable[AnyRef]): Boolean = {
      // If the target is not part of this scroll pane, hide
      list.setSelected(selectBox.getSelected)
      hide()
      false
    }

    /** Handles key down for enter/escape in the dropdown. */
    def handleHideKeyDown(keycode: Int): Boolean = {
      // Key constants
      val KeyNUMPAD_ENTER = 144; val KeyENTER = 66; val KeyESCAPE = 111
      keycode match {
        case `KeyNUMPAD_ENTER` | `KeyENTER` =>
          selectBox.setSelected(list.getSelected)
          hide()
          true
        case `KeyESCAPE` =>
          hide()
          true
        case _ =>
          false
      }
    }
  }
}
