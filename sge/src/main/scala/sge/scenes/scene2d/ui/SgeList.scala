/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/ui/List.java
 * Original authors: mzechner, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: class List<T> -> SgeList[T] (avoids clash with scala.List); toString(T) -> itemToString(T)
 *   Convention: null -> Nullable; (using Sge) context; DynamicArray with MkArray.anyRef cast
 *   Idiom: split packages
 *   Note: Java-style getters/setters retained — setItems/setSelected/setSelectedIndex have validation/events logic; style via Styleable
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package scenes
package scene2d
package ui

import sge.graphics.Color
import sge.graphics.g2d.{ Batch, BitmapFont, GlyphLayout }
import sge.math.Rectangle
import sge.scenes.scene2d.{ Actor, InputEvent, InputListener }
import sge.scenes.scene2d.utils.{ ArraySelection, Cullable, Drawable, UIUtils }
import sge.Input.{ Button, Key }
import sge.utils.{ Align, DynamicArray, MkArray, Nullable }

/** A list (aka list box) displays textual items and highlights the currently selected item. <p> {@link ChangeEvent} is fired when the list selection changes. <p> The preferred size of the list is
  * determined by the text bounds of the items and the size of the {@link ListStyle#selection}.
  * @author
  *   mzechner
  * @author
  *   Nathan Sweet
  */
class SgeList[T](initialStyle: SgeList.ListStyle)(using Sge) extends Widget with Cullable with Styleable[SgeList.ListStyle] {
  import SgeList._

  private var _style:       ListStyle           = scala.compiletime.uninitialized
  val items:                DynamicArray[T]     = DynamicArray.createWithMk(MkArray.anyRef.asInstanceOf[MkArray[T]], 16, true)
  var selection:            ArraySelection[T]   = ArraySelection(items)
  private var _cullingArea: Nullable[Rectangle] = Nullable.empty
  private var _prefWidth:   Float               = 0f
  private var _prefHeight:  Float               = 0f
  var itemHeight:           Float               = 0f
  private var _alignment:   Align               = Align.left
  var pressedIndex:         Int                 = -1
  var overIndex:            Int                 = -1
  private var _keyListener: InputListener       = scala.compiletime.uninitialized
  var typeToSelect:         Boolean             = false

  selection.setActor(Nullable(this))
  selection.required = true

  setStyle(initialStyle)
  setSize(prefWidth, prefHeight)

  val self = this
  _keyListener = new InputListener() {
    var typeTimeout: Long   = 0L
    var prefix:      String = ""

    override def keyDown(event: InputEvent, keycode: Key): Boolean = scala.util.boundary {
      if (self.items.isEmpty) scala.util.boundary.break(false)
      keycode match {
        case Input.Keys.A =>
          if (UIUtils.ctrl() && self.selection.multiple) {
            self.selection.clear()
            self.selection.addAll(self.items)
            scala.util.boundary.break(true)
          }
        case Input.Keys.HOME =>
          self.setSelectedIndex(0)
          scala.util.boundary.break(true)
        case Input.Keys.END =>
          self.setSelectedIndex(self.items.size - 1)
          scala.util.boundary.break(true)
        case Input.Keys.DOWN =>
          var index = self.selected.map(s => self.items.indexOf(s)).getOrElse(-1) + 1
          if (index >= self.items.size) index = 0
          self.setSelectedIndex(index)
          scala.util.boundary.break(true)
        case Input.Keys.UP =>
          var index = self.selected.map(s => self.items.indexOf(s)).getOrElse(-1) - 1
          if (index < 0) index = self.items.size - 1
          self.setSelectedIndex(index)
          scala.util.boundary.break(true)
        case Input.Keys.ESCAPE =>
          self.stage.foreach(_.setKeyboardFocus(Nullable.empty))
          scala.util.boundary.break(true)
        case _ =>
      }
      false
    }

    override def keyTyped(event: InputEvent, character: Char): Boolean = scala.util.boundary {
      if (!self.typeToSelect) scala.util.boundary.break(false)
      val time = System.currentTimeMillis()
      if (time > typeTimeout) prefix = ""
      typeTimeout = time + 300
      prefix += character.toLower
      var i = 0
      while (i < self.items.size) {
        if (self.itemToString(self.items(i)).toLowerCase().startsWith(prefix)) {
          self.setSelectedIndex(i)
          scala.util.boundary.break(false)
        }
        i += 1
      }
      false
    }
  }
  addListener(_keyListener)

  addListener(
    new InputListener() {
      override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Button): Boolean = scala.util.boundary {
        if (pointer != 0 || button != Button(0)) scala.util.boundary.break(true)
        if (self.selection.isDisabled) scala.util.boundary.break(true)
        self.stage.foreach(_.setKeyboardFocus(Nullable(self)))
        if (self.items.isEmpty) scala.util.boundary.break(true)
        val index = self.getItemIndexAt(y)
        if (index == -1) scala.util.boundary.break(true)
        self.selection.choose(self.items(index))
        self.pressedIndex = index
        true
      }

      override def touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Button): Unit =
        if (pointer == 0 && button == Button(0)) {
          self.pressedIndex = -1
        }

      override def touchDragged(event: InputEvent, x: Float, y: Float, pointer: Int): Unit =
        self.overIndex = self.getItemIndexAt(y)

      override def mouseMoved(event: InputEvent, x: Float, y: Float): Boolean = {
        self.overIndex = self.getItemIndexAt(y)
        false
      }

      override def exit(event: InputEvent, x: Float, y: Float, pointer: Int, toActor: Nullable[Actor]): Unit = {
        if (pointer == 0) self.pressedIndex = -1
        if (pointer == -1) self.overIndex = -1
      }
    }
  )

  def this(skin: Skin)(using Sge) = this(skin.get[SgeList.ListStyle])
  def this(skin: Skin, styleName: String)(using Sge) = this(skin.get[SgeList.ListStyle](styleName))

  override def setStyle(style: ListStyle): Unit = {
    this._style = style
    invalidateHierarchy()
  }

  /** Returns the list's style. Modifying the returned style may not have an effect until {@link #setStyle(ListStyle)} is called.
    */
  override def style: ListStyle = _style

  override def layout(): Unit = {
    val font             = _style.font
    val selectedDrawable = _style.selection

    itemHeight = font.capHeight - font.descent * 2
    itemHeight += selectedDrawable.topHeight + selectedDrawable.bottomHeight

    _prefWidth = 0
    val layoutPool  = Actor.POOLS.pool[GlyphLayout]
    val glyphLayout = layoutPool.obtain()
    var i           = 0
    while (i < items.size) {
      glyphLayout.setText(font, itemToString(items(i)))
      _prefWidth = Math.max(glyphLayout.width, _prefWidth)
      i += 1
    }
    layoutPool.free(glyphLayout)
    _prefWidth += selectedDrawable.leftWidth + selectedDrawable.rightWidth
    _prefHeight = items.size * itemHeight

    _style.background.foreach { background =>
      _prefWidth = Math.max(_prefWidth + background.leftWidth + background.rightWidth, background.minWidth)
      _prefHeight = Math.max(_prefHeight + background.topHeight + background.bottomHeight, background.minHeight)
    }
  }

  override def draw(batch: Batch, parentAlpha: Float): Unit = scala.util.boundary {
    validate()

    drawBackground(batch, parentAlpha)

    val font                = _style.font
    val selectedDrawable    = _style.selection
    val fontColorSelected   = _style.fontColorSelected
    val fontColorUnselected = _style.fontColorUnselected

    val c = this.color
    batch.setColor(c.r, c.g, c.b, c.a * parentAlpha)

    var lx    = this.x
    val ly    = this.y
    var lw    = this.width
    val lh    = this.height
    var itemY = lh

    _style.background.foreach { background =>
      val leftWidth = background.leftWidth
      lx += leftWidth
      itemY -= background.topHeight
      lw -= leftWidth + background.rightWidth
    }

    val textOffsetX = selectedDrawable.leftWidth
    val textWidth   = lw - textOffsetX - selectedDrawable.rightWidth
    val textOffsetY = selectedDrawable.topHeight - font.descent

    font.setColor(fontColorUnselected.r, fontColorUnselected.g, fontColorUnselected.b, fontColorUnselected.a * parentAlpha)
    var i = 0
    while (i < items.size) {
      val canDraw = _cullingArea.forall { ca =>
        itemY - itemHeight <= ca.y + ca.height && itemY >= ca.y
      }
      if (canDraw) {
        val item     = items(i)
        val selected = selection.contains(Nullable(item))
        var drawable: Nullable[Drawable] = Nullable.empty
        if (pressedIndex == i && _style.down.isDefined)
          drawable = _style.down
        else if (selected) {
          drawable = Nullable(selectedDrawable)
          font.setColor(fontColorSelected.r, fontColorSelected.g, fontColorSelected.b, fontColorSelected.a * parentAlpha)
        } else if (overIndex == i && _style.over.isDefined)
          drawable = _style.over
        drawSelection(batch, drawable, lx, ly + itemY - itemHeight, lw, itemHeight)
        drawItem(batch, font, i, item, lx + textOffsetX, ly + itemY - textOffsetY, textWidth)
        if (selected) {
          font.setColor(fontColorUnselected.r, fontColorUnselected.g, fontColorUnselected.b, fontColorUnselected.a * parentAlpha)
        }
      } else if (_cullingArea.exists(ca => itemY < ca.y)) {
        scala.util.boundary.break()
      }
      itemY -= itemHeight
      i += 1
    }
  }

  protected def drawSelection(batch: Batch, drawable: Nullable[Drawable], x: Float, y: Float, width: Float, height: Float): Unit =
    drawable.foreach(_.draw(batch, x, y, width, height))

  /** Called to draw the background. Default implementation draws the style background drawable. */
  protected def drawBackground(batch: Batch, parentAlpha: Float): Unit =
    _style.background.foreach { background =>
      val c = this.color
      batch.setColor(c.r, c.g, c.b, c.a * parentAlpha)
      background.draw(batch, x, y, width, height)
    }

  protected def drawItem(batch: Batch, font: BitmapFont, index: Int, item: T, x: Float, y: Float, width: Float): GlyphLayout = {
    val string = itemToString(item)
    font.draw(batch, string, x, y, 0, string.length(), width, _alignment.toInt, false, Nullable("..."))
  }

  // getSelection removed — selection is a public var

  def setSelection(selection: ArraySelection[T]): Unit =
    this.selection = selection

  /** Returns the first selected item, or null. */
  def selected: Nullable[T] = selection.first

  /** Sets the selection to only the passed item, if it is a possible choice.
    * @param item
    *   May be null.
    */
  def setSelected(item: Nullable[T]): Unit =
    item.fold {
      if (selection.required && items.nonEmpty) selection.set(items.first)
      else selection.clear()
    } { i =>
      if (items.contains(i)) selection.set(i)
      else if (selection.required && items.nonEmpty) selection.set(items.first)
      else selection.clear()
    }

  /** @return The index of the first selected item. The top item has an index of 0. Nothing selected has an index of -1. */
  def selectedIndex: Int = {
    val sel = selection.items
    if (sel.isEmpty) -1 else items.indexOf(sel.head)
  }

  /** Sets the selection to only the selected index.
    * @param index
    *   -1 to clear the selection.
    */
  def setSelectedIndex(index: Int): Unit = {
    if (index < -1 || index >= items.size)
      throw new IllegalArgumentException("index must be >= -1 and < " + items.size + ": " + index)
    if (index == -1) {
      selection.clear()
    } else {
      selection.set(items(index))
    }
  }

  /** @return May be null. */
  def overItem: Nullable[T] =
    if (overIndex == -1) Nullable.empty else Nullable(items(overIndex))

  /** @return May be null. */
  def pressedItem: Nullable[T] =
    if (pressedIndex == -1) Nullable.empty else Nullable(items(pressedIndex))

  /** @return null if not over an item. */
  def getItemAt(y: Float): Nullable[T] = {
    val index = getItemIndexAt(y)
    if (index == -1) Nullable.empty else Nullable(items(index))
  }

  /** @return -1 if not over an item. */
  def getItemIndexAt(y: Float): Int = {
    var adjustedY = y
    val height    = this.height
    _style.background.foreach { background =>
      adjustedY -= background.bottomHeight
    }
    val bgHeight = _style.background.map(bg => height - bg.topHeight - bg.bottomHeight).getOrElse(height)
    val index    = ((bgHeight - adjustedY) / itemHeight).toInt
    if (index < 0 || index >= items.size) -1 else index
  }

  /** Sets the items visible in the list, clearing the selection if it is no longer valid. If a selection is {@link ArraySelection#getRequired()}, the first item is selected. This can safely be called
    * with a (modified) array returned from {@link #getItems()}.
    */
  def setItems(newItems: DynamicArray[T]): Unit = {
    val oldPrefWidth  = prefWidth
    val oldPrefHeight = prefHeight

    if (newItems ne items) {
      items.clear()
      items.addAll(newItems)
    }
    overIndex = -1
    pressedIndex = -1
    selection.validate()

    invalidate()
    if (oldPrefWidth != prefWidth || oldPrefHeight != prefHeight) invalidateHierarchy()
  }

  def clearItems(): Unit =
    if (items.nonEmpty) {
      items.clear()
      overIndex = -1
      pressedIndex = -1
      selection.clear()
      invalidateHierarchy()
    }

  // getItems removed — items is a public val

  // getItemHeight removed — itemHeight is a public var

  override def prefWidth: Float = { validate(); _prefWidth }

  override def prefHeight: Float = { validate(); _prefHeight }

  def itemToString(obj: T): String = obj.toString

  override def setCullingArea(cullingArea: Nullable[Rectangle]): Unit =
    this._cullingArea = cullingArea

  /** @return
    *   May be null.
    * @see
    *   #setCullingArea(Rectangle)
    */
  def getCullingArea: Nullable[Rectangle] = _cullingArea

  /** Sets the horizontal alignment of the list items.
    * @param alignment
    *   See {@link Align}.
    */
  def setAlignment(alignment: Align): Unit =
    this._alignment = alignment

  def alignment: Align = this._alignment

  def setTypeToSelect(typeToSelect: Boolean): Unit =
    this.typeToSelect = typeToSelect

  def keyListener: InputListener = _keyListener
}

object SgeList {

  /** The style for a list, see {@link SgeList}.
    * @author
    *   mzechner
    * @author
    *   Nathan Sweet
    */
  class ListStyle() {
    var font:                BitmapFont         = scala.compiletime.uninitialized
    var fontColorSelected:   Color              = Color(1, 1, 1, 1)
    var fontColorUnselected: Color              = Color(1, 1, 1, 1)
    var selection:           Drawable           = scala.compiletime.uninitialized
    var down:                Nullable[Drawable] = Nullable.empty
    var over:                Nullable[Drawable] = Nullable.empty
    var background:          Nullable[Drawable] = Nullable.empty

    def this(font: BitmapFont, fontColorSelected: Color, fontColorUnselected: Color, selection: Drawable) = {
      this()
      this.font = font
      this.fontColorSelected.set(fontColorSelected)
      this.fontColorUnselected.set(fontColorUnselected)
      this.selection = selection
    }

    def this(style: ListStyle) = {
      this()
      font = style.font
      fontColorSelected.set(style.fontColorSelected)
      fontColorUnselected.set(style.fontColorUnselected)
      selection = style.selection
      down = style.down
      over = style.over
      background = style.background
    }
  }
}
