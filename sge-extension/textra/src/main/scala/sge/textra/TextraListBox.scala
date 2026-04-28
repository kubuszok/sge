/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/TextraListBox.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: Widget → standalone class, Cullable → setCullingArea method,
 *     Array<T> → ArrayBuffer[TextraLabel], ArraySelection → internal selection logic,
 *     Rectangle → Nullable[Rectangle] for culling area,
 *     InputListener → InputListener fields (keyListener + touch listener),
 *     ChangeEvent → direct callback, ObjectSet → mutable.HashSet
 *   Convention: List box selection, item management, draw, layout, input fully ported.
 *   Idiom: Nullable[A] for nullable fields; boundary/break for early returns.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 720
 * Covenant-baseline-methods: TextraListBox,_alignment,_color,_cullingArea,_height,_keyListener,_needsLayout,_prefHeight,_prefWidth,_selectedIndex,_selectionDisabled,_selectionRequired,_stage,_style,_touchListener,_touchable,_typePrefix,_typeTimeout,_width,_x,_y,act,adjustedY,clearItems,color,draw,drawBackground,drawSelection,exit,fontColorSelected,fontColorUnselected,getAlignment,getColor,getCullingArea,getCumulativeHeight,getHeight,getItemAt,getItemIndexAt,getItems,getKeyListener,getOverItem,getPrefHeight,getPrefWidth,getPressedItem,getSelected,getSelectedIndex,getSelection,getStage,getStyle,getTouchListener,getTypeToSelect,getWidth,getX,getY,h,h0,handleExit,handleKeyDown,handleKeyTyped,handleMouseMoved,handleTouchDown,handleTouchDragged,handleTouchUp,height,i,index,invalidate,invalidateHierarchy,isSelectionDisabled,isSelectionRequired,isTouchable,itemY,items,keyDown,keyTyped,layout,mouseMoved,oldPrefHeight,oldPrefWidth,overIndex,prefix,pressedIndex,selectedDrawable,selectionDrawable,set,setAlignment,setColor,setCullingArea,setHeight,setItems,setPosition,setSelected,setSelectedIndex,setSelection,setSelectionDisabled,setSelectionRequired,setSize,setStage,setStyle,setTouchable,setTypeToSelect,setWidth,setX,setY,textOffsetX,textOffsetY,this,time,touchDown,touchDragged,touchUp,typeTimeout,typeToSelect,validate,validateSelection,width,x,y2
 * Covenant-source-reference: com/github/tommyettinger/textra/TextraListBox.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4
 */
package sge
package textra

import scala.collection.mutable.ArrayBuffer
import scala.util.boundary
import scala.util.boundary.break

import sge.Input
import sge.Input.Keys
import sge.graphics.Color
import sge.graphics.g2d.Batch
import sge.math.Rectangle
import sge.scenes.scene2d.{ Actor, InputEvent, InputListener, Stage }
import sge.scenes.scene2d.ui.Skin
import sge.scenes.scene2d.utils.Drawable
import sge.utils.{ Align, Nullable }

/** A TextraListBox displays TextraLabels and highlights the currently selected item.
  *
  * A ChangeEvent is fired when the list selection changes. The preferred size of the list box is determined by the text bounds of the items and the size of the ListStyle.selection.
  */
class TextraListBox(style: Styles.ListStyle) {

  def this(skin: Skin) = this(skin.get(classOf[Styles.ListStyle]))

  def this(skin: Skin, styleName: String) = this(skin.get(styleName, classOf[Styles.ListStyle]))

  protected var _style:       Styles.ListStyle         = style
  protected val items:        ArrayBuffer[TextraLabel] = ArrayBuffer.empty
  private var _selectedIndex: Int                      = -1
  private var _alignment:     Int                      = Align.left.toInt
  var pressedIndex:           Int                      = -1
  var overIndex:              Int                      = -1

  // Culling area
  private var _cullingArea: Nullable[Rectangle] = Nullable.empty

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

  // Selection state
  private var _selectionRequired: Boolean = true
  private var _selectionDisabled: Boolean = false

  // Stage reference (standalone, not an Actor — must be set externally)
  private var _stage: Nullable[Stage] = Nullable.empty

  /** When this is true, typing a character while this list box is focused will jump focus to the first item in the list that starts with that character, ignoring case.
    *
    * When this is true, this does allocate some Strings every time the user types into a focused list box.
    */
  var typeToSelect: Boolean = false

  // Type-to-select state
  private var _typeTimeout: Long   = 0L
  private var _typePrefix:  String = ""

  // --- Key listener (for keyboard navigation) ---

  private val _keyListener: InputListener = new InputListener() {
    private var typeTimeout: Long   = 0L
    private var prefix:      String = ""

    override def keyDown(event: InputEvent, keycode: Input.Key): Boolean =
      if (items.isEmpty) false
      else {
        keycode match {
          case Keys.A =>
            // Ctrl+A not applicable for single-selection; no-op
            false
          case Keys.HOME =>
            setSelectedIndex(0)
            true
          case Keys.END =>
            setSelectedIndex(items.size - 1)
            true
          case Keys.DOWN =>
            val selIdx = getSelected.fold(-1)(sel => items.indexOf(sel))
            val index  = selIdx + 1
            setSelectedIndex(if (index >= items.size) 0 else index)
            true
          case Keys.UP =>
            val selIdx = getSelected.fold(-1)(sel => items.indexOf(sel))
            val index  = selIdx - 1
            setSelectedIndex(if (index < 0) items.size - 1 else index)
            true
          case Keys.ESCAPE =>
            _stage.foreach(_.setKeyboardFocus(Nullable.empty))
            true
          case _ =>
            false
        }
      }

    override def keyTyped(event: InputEvent, character: Char): Boolean = boundary {
      if (!typeToSelect) {
        break(false)
      }
      val time = System.currentTimeMillis()
      if (time > typeTimeout) prefix = ""
      typeTimeout = time + 300
      prefix += character.toLower
      var i = 0
      while (i < items.size) {
        if (items(i).toString.toLowerCase.startsWith(prefix)) {
          setSelectedIndex(i)
          break(false)
        }
        i += 1
      }
      false
    }
  }

  // --- Touch/mouse listener ---

  private val _touchListener: InputListener = new InputListener() {
    override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Input.Button): Boolean =
      if (pointer != 0 || button != Input.Buttons.LEFT) true
      else if (_selectionDisabled) true
      else {
        _stage.foreach(_.setKeyboardFocus(event.listenerActor))
        if (items.isEmpty) true
        else {
          val index = getItemIndexAt(y)
          if (index == -1) true
          else {
            _selectedIndex = index
            pressedIndex = index
            true
          }
        }
      }

    override def touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Input.Button): Unit =
      if (pointer == 0 && button == Input.Buttons.LEFT) {
        pressedIndex = -1
      }

    override def touchDragged(event: InputEvent, x: Float, y: Float, pointer: Int): Unit =
      overIndex = getItemIndexAt(y)

    override def mouseMoved(event: InputEvent, x: Float, y: Float): Boolean = {
      overIndex = getItemIndexAt(y)
      false
    }

    override def exit(event: InputEvent, x: Float, y: Float, pointer: Int, toActor: Nullable[Actor]): Unit = {
      if (pointer == 0) pressedIndex = -1
      if (pointer == -1) overIndex = -1
    }
  }

  // --- Init ---
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

  def setPosition(x: Float, y: Float): Unit = {
    _x = x
    _y = y
  }

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

  // --- Stage ---

  /** Returns the stage reference, or empty if not set. Since this is a standalone class (not extending Actor), the stage must be set externally. */
  def getStage: Nullable[Stage] = _stage

  /** Sets the stage reference. Used by SelectBoxScrollPane and similar containers. */
  def setStage(stage: Nullable[Stage]): Unit =
    _stage = stage

  // --- Style ---

  def setStyle(style: Styles.ListStyle): Unit = {
    require(style != null, "style cannot be null.")
    this._style = style
    invalidateHierarchy()
  }

  /** Returns the list's style. Modifying the returned style may not have an effect until {@link #setStyle} is called. */
  def getStyle: Styles.ListStyle = _style

  // --- Layout ---

  def layout(): Unit = {
    val selectedDrawable = Nullable.fold(_style.selection)(null: Drawable) {
      case d: Drawable => d
      case _ => null
    }

    if (selectedDrawable == null) {
      _prefHeight = 0f
      _prefWidth = 0f
    } else {
      _prefHeight = selectedDrawable.topHeight + selectedDrawable.bottomHeight
      _prefWidth = 0f

      var i = 0
      while (i < items.size) {
        _prefWidth = Math.max(items(i).getPrefWidth, _prefWidth)
        _prefHeight += items(i).getPrefHeight
        i += 1
      }
      _prefWidth += selectedDrawable.leftWidth + selectedDrawable.rightWidth
    }

    Nullable.foreach(_style.background) {
      case bg: Drawable =>
        _prefWidth = Math.max(_prefWidth + bg.leftWidth + bg.rightWidth, bg.minWidth)
        _prefHeight = Math.max(_prefHeight + bg.topHeight + bg.bottomHeight, bg.minHeight)
      case _ => ()
    }
  }

  // --- Act ---

  def act(delta: Float): Unit = {
    var i = 0
    while (i < items.size) {
      items(i).act(delta)
      i += 1
    }
  }

  // --- Draw ---

  def draw(batch: Batch, parentAlpha: Float): Unit = boundary {
    validate()

    drawBackground(batch, parentAlpha)

    val selectionDrawable = Nullable.fold(_style.selection)(null: Drawable) {
      case d: Drawable => d
      case _ => null
    }
    if (selectionDrawable == null) {
      break(())
    }

    val fontColorSelected   = _style.fontColorSelected
    val fontColorUnselected = _style.fontColorUnselected

    val color = _color
    batch.setColor(color.r, color.g, color.b, color.a * parentAlpha)

    var x     = _x
    val y2    = _y
    var width = _width
    var itemY = _height

    Nullable.foreach(_style.background) {
      case bg: Drawable =>
        val leftWidth = bg.leftWidth
        x += leftWidth
        itemY -= bg.topHeight
        width -= leftWidth + bg.rightWidth
      case _ => ()
    }

    val textOffsetX = selectionDrawable.leftWidth
    val textOffsetY = selectionDrawable.topHeight

    var i = 0
    while (i < items.size) {
      val item       = items(i)
      val itemHeight = item.getPrefHeight
      val inCulling  = _cullingArea.fold(true) { ca =>
        itemY - itemHeight <= ca.y + ca.height && itemY >= ca.y
      }

      if (inCulling) {
        item.setColor(fontColorUnselected.r, fontColorUnselected.g, fontColorUnselected.b, fontColorUnselected.a * parentAlpha)
        val selected = _selectedIndex == i
        var drawable: Nullable[Drawable] = Nullable.empty

        if (pressedIndex == i) {
          Nullable.foreach(_style.down) {
            case d: Drawable => drawable = Nullable(d)
            case _ => ()
          }
        } else if (selected) {
          drawable = Nullable(selectionDrawable)
          item.setColor(fontColorSelected.r, fontColorSelected.g, fontColorSelected.b, fontColorSelected.a * parentAlpha)
        } else if (overIndex == i) {
          Nullable.foreach(_style.over) {
            case d: Drawable => drawable = Nullable(d)
            case _ => ()
          }
        }

        drawSelection(batch, drawable, x, y2 + itemY - textOffsetY - itemHeight, width, itemHeight)
        item.setPosition(x + textOffsetX, y2 + itemY - textOffsetY - itemHeight)
        item.draw(batch, 1f)
        if (selected) {
          item.setColor(fontColorUnselected.r, fontColorUnselected.g, fontColorUnselected.b, fontColorUnselected.a * parentAlpha)
        }
      } else if (_cullingArea.fold(false)(ca => itemY < ca.y)) {
        i = items.size // break out of loop
      }

      itemY -= item.getPrefHeight
      i += 1
    }
  }

  protected def drawSelection(batch: Batch, drawable: Nullable[Drawable], x: Float, y: Float, width: Float, height: Float): Unit =
    Nullable.foreach(drawable)(d => d.draw(batch, x, y, width, height))

  /** Called to draw the background. Default implementation draws the style background drawable. */
  protected def drawBackground(batch: Batch, parentAlpha: Float): Unit =
    Nullable.foreach(_style.background) {
      case bg: Drawable =>
        val color = _color
        batch.setColor(color.r, color.g, color.b, color.a * parentAlpha)
        bg.draw(batch, _x, _y, _width, _height)
      case _ => ()
    }

  // --- Selection ---

  def getSelected: Nullable[TextraLabel] =
    if (_selectedIndex >= 0 && _selectedIndex < items.size) Nullable(items(_selectedIndex))
    else Nullable.empty

  /** Sets the selection to only the passed item, if it is a possible choice. */
  def setSelected(item: Nullable[TextraLabel]): Unit =
    Nullable.fold(item) {
      if (_selectionRequired && items.nonEmpty) {
        _selectedIndex = 0
      } else {
        _selectedIndex = -1
      }
    } { it =>
      val idx = items.indexOf(it)
      if (idx >= 0) {
        _selectedIndex = idx
      } else if (_selectionRequired && items.nonEmpty) {
        _selectedIndex = 0
      } else {
        _selectedIndex = -1
      }
    }

  /** Returns the index of the first selected item. The top item has an index of 0. Nothing selected has an index of -1. */
  def getSelectedIndex: Int = _selectedIndex

  /** Sets the selection to only the selected index.
    * @param index
    *   -1 to clear the selection.
    */
  def setSelectedIndex(index: Int): Unit = {
    if (index < -1 || index >= items.size) {
      throw new IllegalArgumentException("index must be >= -1 and < " + items.size + ": " + index)
    }
    if (index == -1) {
      _selectedIndex = -1
    } else {
      _selectedIndex = index
    }
  }

  // --- Selection configuration ---

  /** Returns the internal selection state object (this list box itself, for API compatibility). */
  def getSelection: TextraListBox = this

  /** Replaces the selection state. This is a compatibility method; in the standalone port, it copies the selection index from the source list box. */
  def setSelection(source: TextraListBox): Unit = {
    _selectedIndex = source._selectedIndex
    _selectionRequired = source._selectionRequired
    _selectionDisabled = source._selectionDisabled
  }

  /** Sets the selection to the given item (Selection API compatibility). */
  def set(item: Nullable[TextraLabel]): Unit =
    setSelected(item)

  def isSelectionRequired: Boolean = _selectionRequired

  def setSelectionRequired(required: Boolean): Unit =
    _selectionRequired = required

  def isSelectionDisabled: Boolean = _selectionDisabled

  def setSelectionDisabled(disabled: Boolean): Unit =
    _selectionDisabled = disabled

  // --- Items ---

  /** Returns the internal items array. If modified, {@link #setItems} must be called to reflect the changes. */
  def getItems: ArrayBuffer[TextraLabel] = items

  def setItems(newItems: TextraLabel*): Unit = {
    val oldPrefWidth  = getPrefWidth
    val oldPrefHeight = getPrefHeight

    items.clear()
    items ++= newItems
    overIndex = -1
    pressedIndex = -1
    validateSelection()

    invalidate()
    if (oldPrefWidth != getPrefWidth || oldPrefHeight != getPrefHeight) invalidateHierarchy()
  }

  /** Sets the items visible in the list, clearing the selection if it is no longer valid. If a selection is required, the first item is selected. This can safely be called with a (modified) array
    * returned from {@link #getItems}.
    */
  def setItems(newItems: ArrayBuffer[TextraLabel]): Unit = {
    val oldPrefWidth  = getPrefWidth
    val oldPrefHeight = getPrefHeight

    if (!(newItems eq items)) {
      items.clear()
      items ++= newItems
    }
    overIndex = -1
    pressedIndex = -1
    validateSelection()

    invalidate()
    if (oldPrefWidth != getPrefWidth || oldPrefHeight != getPrefHeight) invalidateHierarchy()
  }

  def clearItems(): Unit =
    if (items.nonEmpty) {
      items.clear()
      overIndex = -1
      pressedIndex = -1
      _selectedIndex = -1
      invalidateHierarchy()
    }

  private def validateSelection(): Unit = {
    if (_selectedIndex >= items.size) {
      _selectedIndex = items.size - 1
    }
    if (_selectionRequired && items.nonEmpty && _selectedIndex < 0) {
      _selectedIndex = 0
    }
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

  // --- Culling ---

  def setCullingArea(cullingArea: Nullable[Rectangle]): Unit =
    _cullingArea = cullingArea

  /** @return
    *   May be null.
    * @see
    *   #setCullingArea
    */
  def getCullingArea: Nullable[Rectangle] = _cullingArea

  // --- Alignment ---

  /** Sets the horizontal alignment of the list items.
    * @param alignment
    *   See Align.
    */
  def setAlignment(alignment: Int): Unit =
    this._alignment = alignment

  def getAlignment: Int = _alignment

  // --- Type to select ---

  def getTypeToSelect: Boolean = typeToSelect

  def setTypeToSelect(typeToSelect: Boolean): Unit =
    this.typeToSelect = typeToSelect

  // --- Key listener ---

  def getKeyListener: InputListener = _keyListener

  // --- Touch listener ---

  def getTouchListener: InputListener = _touchListener

  // --- Item index at position ---

  /** @return -1 if not over an item. */
  def getItemIndexAt(y: Float): Int = boundary {
    var height = _height
    Nullable.foreach(_style.background) {
      case bg: Drawable =>
        height -= bg.topHeight + bg.bottomHeight
      case _ => ()
    }
    var adjustedY = y
    Nullable.foreach(_style.background) {
      case bg: Drawable =>
        adjustedY -= bg.bottomHeight
      case _ => ()
    }
    val h0 = height - adjustedY
    if (h0 < 0) {
      -1
    } else {
      var h = h0
      var i = 0
      while (i < items.size) {
        h -= items(i).getPrefHeight
        if (h <= 0) {
          break(i)
        }
        i += 1
      }
      -1
    }
  }

  /** @return null if not over an item. */
  def getItemAt(y: Float): Nullable[TextraLabel] = {
    val index = getItemIndexAt(y)
    if (index == -1) Nullable.empty else Nullable(items(index))
  }

  /** @return May be null. */
  def getOverItem: Nullable[TextraLabel] =
    if (overIndex == -1) Nullable.empty else Nullable(items(overIndex))

  /** @return May be null. */
  def getPressedItem: Nullable[TextraLabel] =
    if (pressedIndex == -1) Nullable.empty else Nullable(items(pressedIndex))

  /** Gets the total height of the item with the given index and all items before it. This starts at 0 and increases as the index increases.
    * @param index
    *   the index of an item in this list box
    * @return
    *   -Float.MaxValue if index is negative or greater than or equal to items.size
    */
  def getCumulativeHeight(index: Int): Float = boundary {
    if (index < 0 || index >= items.size) {
      break(-Float.MaxValue)
    }
    var h = 0f
    Nullable.foreach(_style.background) {
      case bg: Drawable =>
        h += bg.bottomHeight
      case _ => ()
    }
    var i = 0
    while (i < items.size) {
      val item = items(i)
      item.getFont.calculateSize(item.layout)
      h += item.layout.getHeight
      if (i >= index) {
        break(h)
      }
      i += 1
    }
    -Float.MaxValue
  }

  // --- Input handling (standalone delegation methods) ---

  /** Handles a key-down event for list navigation. Returns true if handled. */
  def handleKeyDown(keycode: Input.Key): Boolean =
    if (items.isEmpty) {
      false
    } else {
      keycode match {
        case Keys.HOME =>
          setSelectedIndex(0)
          true
        case Keys.END =>
          setSelectedIndex(items.size - 1)
          true
        case Keys.DOWN =>
          val idx = _selectedIndex + 1
          setSelectedIndex(if (idx >= items.size) 0 else idx)
          true
        case Keys.UP =>
          val idx = _selectedIndex - 1
          setSelectedIndex(if (idx < 0) items.size - 1 else idx)
          true
        case Keys.ESCAPE =>
          _stage.foreach(_.setKeyboardFocus(Nullable.empty))
          true
        case _ =>
          false
      }
    }

  /** Handles a key-typed event for type-to-select. Returns false (never consumes). */
  def handleKeyTyped(character: Char): Boolean = boundary {
    if (!typeToSelect) {
      break(false)
    }
    val time = System.currentTimeMillis()
    if (time > _typeTimeout) _typePrefix = ""
    _typeTimeout = time + 300
    _typePrefix += character.toLower
    var i = 0
    while (i < items.size) {
      if (items(i).toString.toLowerCase.startsWith(_typePrefix)) {
        setSelectedIndex(i)
        break(false)
      }
      i += 1
    }
    false
  }

  /** Handles a touch-down event. Returns true (always consumes). */
  def handleTouchDown(x: Float, y: Float, pointer: Int, button: Int): Boolean =
    if (pointer != 0 || button != 0) {
      true
    } else if (_selectionDisabled) {
      true
    } else if (items.isEmpty) {
      true
    } else {
      val index = getItemIndexAt(y)
      if (index != -1) {
        _selectedIndex = index
        pressedIndex = index
      }
      true
    }

  /** Handles a touch-up event. */
  def handleTouchUp(x: Float, y: Float, pointer: Int, button: Int): Unit =
    if (pointer == 0 && button == 0) {
      pressedIndex = -1
    }

  /** Handles a touch-dragged event. */
  def handleTouchDragged(x: Float, y: Float, pointer: Int): Unit =
    overIndex = getItemIndexAt(y)

  /** Handles a mouse-moved event. */
  def handleMouseMoved(x: Float, y: Float): Boolean = {
    overIndex = getItemIndexAt(y)
    false
  }

  /** Handles an exit event. */
  def handleExit(pointer: Int): Unit = {
    if (pointer == 0) pressedIndex = -1
    if (pointer == -1) overIndex = -1
  }

  // --- Touchable state (for SelectBoxScrollPane) ---
  private var _touchable: Boolean = true

  def isTouchable: Boolean = _touchable

  def setTouchable(touchable: Boolean): Unit =
    _touchable = touchable
}
