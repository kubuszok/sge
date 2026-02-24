/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/ui/SelectBox.java
 * Original authors: mzechner, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package scenes
package scene2d
package ui

import scala.collection.mutable.ArrayBuffer

import sge.graphics.Color
import sge.graphics.g2d.{ Batch, BitmapFont, GlyphLayout }
import sge.math.{ Interpolation, Vector2 }
import sge.scenes.scene2d.{ Actor, InputEvent, InputListener, Stage, Touchable }
import sge.scenes.scene2d.actions.Actions
import sge.scenes.scene2d.utils.{ ArraySelection, ClickListener, Disableable, Drawable }
import sge.utils.{ Align, Nullable }

/** A select box (aka a drop-down list) allows a user to choose one of a number of values from a list. When inactive, the selected value is displayed. When activated, it shows the list of values that
  * may be selected. <p> {@link ChangeEvent} is fired when the selectbox selection changes. <p> The preferred size of the select box is determined by the maximum text bounds of the items and the size
  * of the {@link SelectBoxStyle#background}.
  * @author
  *   mzechner
  * @author
  *   Nathan Sweet
  */
class SelectBox[T](style: SelectBox.SelectBoxStyle)(using sge: Sge) extends Widget with Disableable with Styleable[SelectBox.SelectBoxStyle] {

  import SelectBox._

  private var _style:        SelectBoxStyle         = scala.compiletime.uninitialized
  val items:                 ArrayBuffer[T]         = ArrayBuffer.empty
  var scrollPane:            SelectBoxScrollPane[T] = scala.compiletime.uninitialized
  private var prefWidth:     Float                  = 0f
  private var prefHeight:    Float                  = 0f
  private var clickListener: ClickListener          = scala.compiletime.uninitialized
  var disabled:              Boolean                = false
  private var alignment:     Align                  = Align.left
  var selectedPrefWidth:     Boolean                = false

  val selection: ArraySelection[T] = new ArraySelection(items) {
    override def fireChangeEvent(): Boolean = {
      if (selectedPrefWidth) invalidateHierarchy()
      super.fireChangeEvent()
    }
  }

  // TODO: uncomment when Skin is ported
  // def this(skin: Skin)(using sge: Sge) = this(skin.get(classOf[SelectBoxStyle]))
  // def this(skin: Skin, styleName: String)(using sge: Sge) = this(skin.get(styleName, classOf[SelectBoxStyle]))

  setStyle(style)
  setSize(getPrefWidth, getPrefHeight)

  selection.setActor(Nullable(this))
  selection.setRequired(true)

  scrollPane = newScrollPane()

  private val self = this
  addListener {
    clickListener = new ClickListener() {
      override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean =
        if (pointer == 0 && button != 0) false
        else if (self.isDisabled) false
        else {
          if (self.scrollPane.hasParent)
            self.hideScrollPane()
          else
            self.showScrollPane()
          true
        }
    }
    clickListener
  }

  /** Allows a subclass to customize the scroll pane shown when the select box is open. */
  protected def newScrollPane(): SelectBoxScrollPane[T] =
    new SelectBoxScrollPane(this)

  /** Set the max number of items to display when the select box is opened. Set to 0 (the default) to display as many as fit in the stage height.
    */
  def setMaxListCount(maxListCount: Int): Unit =
    scrollPane.maxListCount = maxListCount

  /** @return Max number of items to display when the box is opened, or <= 0 to display them all. */
  def getMaxListCount: Int =
    scrollPane.maxListCount

  override protected[scene2d] def setStage(stage: Nullable[Stage]): Unit = {
    if (stage.isEmpty) scrollPane.hide()
    super.setStage(stage)
  }

  override def setStyle(style: SelectBoxStyle): Unit = {
    if (style == null) throw new IllegalArgumentException("style cannot be null.")
    this._style = style

    if (scrollPane != null) {
      scrollPane.setStyle(style.scrollStyle)
      scrollPane.list.setStyle(style.listStyle)
    }
    invalidateHierarchy()
  }

  /** Returns the select box's style. Modifying the returned style may not have an effect until {@link #setStyle(SelectBoxStyle)} is called.
    */
  override def getStyle: SelectBoxStyle = _style

  /** Set the backing Array that makes up the choices available in the SelectBox */
  def setItems(newItems: T*): Unit = {
    if (newItems == null) throw new IllegalArgumentException("newItems cannot be null.")
    val oldPrefWidth = getPrefWidth

    items.clear()
    items.addAll(newItems)
    selection.validate()
    scrollPane.list.setItems(items)

    invalidate()
    if (oldPrefWidth != getPrefWidth) invalidateHierarchy()
  }

  /** Sets the items visible in the select box. */
  def setItems(newItems: ArrayBuffer[T]): Unit = {
    if (newItems == null) throw new IllegalArgumentException("newItems cannot be null.")
    val oldPrefWidth = getPrefWidth

    if (newItems ne items) {
      items.clear()
      items.addAll(newItems)
    }
    selection.validate()
    scrollPane.list.setItems(items)

    invalidate()
    if (oldPrefWidth != getPrefWidth) invalidateHierarchy()
  }

  def clearItems(): Unit =
    if (items.isEmpty) ()
    else {
      items.clear()
      selection.clear()
      scrollPane.list.clearItems()
      invalidateHierarchy()
    }

  /** Returns the internal items array. If modified, {@link #setItems(ArrayBuffer)} must be called to reflect the changes. */
  def getItems: ArrayBuffer[T] = items

  override def layout(): Unit = {
    val bg   = _style.background
    val font = _style.font

    bg.fold {
      prefHeight = font.getCapHeight() - font.getDescent() * 2
    } { background =>
      prefHeight = Math.max(background.getTopHeight + background.getBottomHeight + font.getCapHeight() - font.getDescent() * 2, background.getMinHeight)
    }

    val layoutPool  = Actor.POOLS.getPool(classOf[GlyphLayout])
    val glyphLayout = layoutPool.obtain()
    if (selectedPrefWidth) {
      prefWidth = 0
      bg.foreach { background => prefWidth = background.getLeftWidth + background.getRightWidth }
      val selected = getSelected
      selected.foreach { sel =>
        glyphLayout.setText(font, itemToString(sel))
        prefWidth += glyphLayout.width
      }
    } else {
      var maxItemWidth = 0f
      var i            = 0
      while (i < items.size) {
        glyphLayout.setText(font, itemToString(items(i)))
        maxItemWidth = Math.max(glyphLayout.width, maxItemWidth)
        i += 1
      }

      prefWidth = maxItemWidth
      bg.foreach { background =>
        prefWidth = Math.max(prefWidth + background.getLeftWidth + background.getRightWidth, background.getMinWidth)
      }

      val listStyle   = _style.listStyle
      val scrollStyle = _style.scrollStyle
      var scrollWidth = maxItemWidth + listStyle.selection.getLeftWidth + listStyle.selection.getRightWidth
      scrollStyle.background.foreach { scrollBg =>
        scrollWidth = Math.max(scrollWidth + scrollBg.getLeftWidth + scrollBg.getRightWidth, scrollBg.getMinWidth)
      }
      if (scrollPane == null || !scrollPane.disableY) {
        scrollWidth += Math.max(_style.scrollStyle.vScroll.fold(0f)(_.getMinWidth), _style.scrollStyle.vScrollKnob.fold(0f)(_.getMinWidth))
      }
      prefWidth = Math.max(prefWidth, scrollWidth)
    }
    layoutPool.free(glyphLayout)
  }

  /** Returns appropriate background drawable from the style based on the current select box state. */
  protected def getBackgroundDrawable(): Nullable[Drawable] =
    if (isDisabled && _style.backgroundDisabled.isDefined) _style.backgroundDisabled
    else if (scrollPane.hasParent && _style.backgroundOpen.isDefined) _style.backgroundOpen
    else if (isOver && _style.backgroundOver.isDefined) _style.backgroundOver
    else _style.background

  /** Returns the appropriate label font color from the style based on the current button state. */
  protected def getFontColor(): Color =
    if (isDisabled && _style.disabledFontColor.isDefined) _style.disabledFontColor.orNull
    else if (_style.overFontColor.isDefined && (isOver || scrollPane.hasParent)) _style.overFontColor.orNull
    else _style.fontColor

  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    validate()

    val background = getBackgroundDrawable()
    val fontColor  = getFontColor()
    val font       = _style.font

    val color  = getColor
    var x      = getX
    var y      = getY
    var width  = getWidth
    var height = getHeight

    batch.setColor(color.r, color.g, color.b, color.a * parentAlpha)
    background.foreach(_.draw(batch, x, y, width, height))

    val selected: Nullable[T] = selection.first
    if (selected.isDefined) {
      val sel = selected.orNull
      if (background.isDefined) {
        val bg = background.orNull
        width -= bg.getLeftWidth + bg.getRightWidth
        height -= bg.getBottomHeight + bg.getTopHeight
        x += bg.getLeftWidth
        y += (height / 2 + bg.getBottomHeight + font.getData().capHeight / 2).toInt.toFloat
      } else {
        y += (height / 2 + font.getData().capHeight / 2).toInt.toFloat
      }
      font.setColor(fontColor.r, fontColor.g, fontColor.b, fontColor.a * parentAlpha)
      drawItem(batch, font, sel, x, y, width)
    }
  }

  protected def drawItem(batch: Batch, font: BitmapFont, item: T, x: Float, y: Float, width: Float): GlyphLayout = {
    val string = itemToString(item)
    font.draw(batch, string, x, y, 0, string.length(), width, alignment.toInt, false, Nullable("..."))
  }

  /** Sets the alignment of the selected item in the select box. See {@link #getList()} and {@link List#setAlignment(int)} to set the alignment in the list shown when the select box is open.
    * @param alignment
    *   See {@link Align}.
    */
  def setAlignment(alignment: Align): Unit =
    this.alignment = alignment

  /** Get the set of selected items, useful when multiple items are selected
    * @return
    *   a Selection object containing the selected elements
    */
  def getSelection: ArraySelection[T] = selection

  /** Returns the first selected item, or null. For multiple selections use {@link SelectBox#getSelection()}. */
  def getSelected: Nullable[T] = selection.first

  /** Sets the selection to only the passed item, if it is a possible choice, else selects the first item. */
  def setSelected(item: Nullable[T]): Unit =
    if (item.fold(false)(i => items.contains(i)))
      selection.set(item.orNull)
    else if (items.nonEmpty)
      selection.set(items.head)
    else
      selection.clear()

  /** @return The index of the first selected item. The top item has an index of 0. Nothing selected has an index of -1. */
  def getSelectedIndex: Int = {
    val selected = selection.items
    if (selected.isEmpty) -1 else items.indexOf(selected.head)
  }

  /** Sets the selection to only the selected index. */
  def setSelectedIndex(index: Int): Unit =
    selection.set(items(index))

  /** When true the pref width is based on the selected item. */
  def setSelectedPrefWidth(selectedPrefWidth: Boolean): Unit =
    this.selectedPrefWidth = selectedPrefWidth

  def getSelectedPrefWidth: Boolean = selectedPrefWidth

  /** Returns the pref width of the select box if the widest item was selected, for use when {@link #setSelectedPrefWidth(boolean)} is true.
    */
  def getMaxSelectedPrefWidth: Float = {
    val layoutPool  = Actor.POOLS.getPool(classOf[GlyphLayout])
    val glyphLayout = layoutPool.obtain()
    var width       = 0f
    var i           = 0
    while (i < items.size) {
      glyphLayout.setText(_style.font, itemToString(items(i)))
      width = Math.max(glyphLayout.width, width)
      i += 1
    }
    layoutPool.free(glyphLayout)
    _style.background.foreach { bg =>
      width = Math.max(width + bg.getLeftWidth + bg.getRightWidth, bg.getMinWidth)
    }
    width
  }

  def setDisabled(disabled: Boolean): Unit = {
    if (disabled && !this.disabled) hideScrollPane()
    this.disabled = disabled
  }

  def isDisabled: Boolean = disabled

  override def getPrefWidth: Float = {
    validate()
    prefWidth
  }

  override def getPrefHeight: Float = {
    validate()
    prefHeight
  }

  protected def itemToString(item: T): String = item.toString

  /** @deprecated Use {@link #showScrollPane()}. */
  @deprecated("Use showScrollPane", "")
  def showList(): Unit = showScrollPane()

  def showScrollPane(): Unit =
    if (items.isEmpty) ()
    else getStage.foreach(stage => scrollPane.show(stage))

  /** @deprecated Use {@link #hideScrollPane()}. */
  @deprecated("Use hideScrollPane", "")
  def hideList(): Unit = hideScrollPane()

  def hideScrollPane(): Unit =
    scrollPane.hide()

  /** Returns the list shown when the select box is open. */
  def getList: SgeList[T] = scrollPane.list

  /** Disables scrolling of the list shown when the select box is open. */
  def setScrollingDisabled(y: Boolean): Unit = {
    scrollPane.setScrollingDisabled(true, y)
    invalidateHierarchy()
  }

  /** Returns the scroll pane containing the list that is shown when the select box is open. */
  def getScrollPane: SelectBoxScrollPane[T] = scrollPane

  def isOver: Boolean = clickListener.isOver

  def getClickListener: ClickListener = clickListener

  protected def onShow(scrollPane: Actor, below: Boolean)(using sge: Sge): Unit = {
    scrollPane.getColor.a = 0
    scrollPane.addAction(Actions.fadeIn(0.3f, Nullable(Interpolation.fade)))
  }

  protected def onHide(scrollPane: Actor)(using sge: Sge): Unit = {
    scrollPane.getColor.a = 1
    scrollPane.addAction(Actions.sequence(Actions.fadeOut(0.15f, Nullable(Interpolation.fade)), Actions.removeActor()))
  }
}

object SelectBox {

  private val temp: Vector2 = new Vector2()

  /** The scroll pane shown when a select box is open.
    * @author
    *   Nathan Sweet
    */
  class SelectBoxScrollPane[T](val selectBox: SelectBox[T])(using sge: Sge) extends ScrollPane(Nullable.empty, selectBox._style.scrollStyle) {

    var maxListCount:                Int             = 0
    private val stagePosition:       Vector2         = new Vector2()
    val list:                        SgeList[T]      = newList()
    private var hideListener:        InputListener   = scala.compiletime.uninitialized
    private var previousScrollFocus: Nullable[Actor] = Nullable.empty

    setOverscroll(false, false)
    setFadeScrollBars(false)
    setScrollingDisabled(true, false)

    list.setTouchable(Touchable.disabled)
    list.setTypeToSelect(true)
    setActor(Nullable(list))

    list.addListener(
      new ClickListener() {
        override def clicked(event: InputEvent, x: Float, y: Float): Unit = {
          val selected = list.getSelected
          // Force clicking the already selected item to trigger a change event.
          if (selected.isDefined) selectBox.selection.items.clear()
          selected.foreach(sel => selectBox.selection.choose(sel))
          hide()
        }

        override def mouseMoved(event: InputEvent, x: Float, y: Float): Boolean = {
          val index = list.getItemIndexAt(y)
          if (index != -1) list.setSelectedIndex(index)
          true
        }
      }
    )

    val scrollPaneSelf = this
    addListener(
      new InputListener() {
        override def exit(event: InputEvent, x: Float, y: Float, pointer: Int, toActor: Nullable[Actor]): Unit =
          if (toActor.fold(true)(a => !scrollPaneSelf.isAscendantOf(a))) {
            val selected = selectBox.getSelected
            selected.foreach(sel => list.selection.set(sel))
          }
      }
    )

    hideListener = new InputListener() {
      override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean = {
        val target = event.getTarget
        if (scrollPaneSelf.isAscendantOf(target)) ()
        else {
          selectBox.getSelected.foreach(sel => list.selection.set(sel))
          hide()
        }
        false
      }

      override def keyDown(event: InputEvent, keycode: Int): Boolean =
        keycode match {
          case Input.Keys.NUMPAD_ENTER | Input.Keys.ENTER =>
            list.getSelected.foreach(sel => selectBox.selection.choose(sel))
            // Fall thru.
            hide()
            event.stop()
            true
          case Input.Keys.ESCAPE =>
            hide()
            event.stop()
            true
          case _ =>
            false
        }
    }

    /** Allows a subclass to customize the select box list. The default implementation returns a list that delegates {@link List#toString(Object)} to {@link SelectBox#toString(Object)}.
      */
    protected def newList(): SgeList[T] =
      new SgeList[T](selectBox._style.listStyle) {
        override def itemToString(obj: T): String =
          selectBox.itemToString(obj)
      }

    def show(stage: Stage): Unit =
      if (list.isTouchable) ()
      else {
        stage.addActor(this)
        stage.addCaptureListener(hideListener)
        stage.addListener(list.getKeyListener)

        selectBox.localToStageCoordinates(stagePosition.set(0, 0))

        // Show the list above or below the select box, limited to a number of items and the available height in the stage.
        val itemHeight           = list.getItemHeight
        var height               = itemHeight * (if (maxListCount <= 0) selectBox.items.size else Math.min(maxListCount, selectBox.items.size))
        val scrollPaneBackground = getStyle.background
        scrollPaneBackground.foreach(bg => height += bg.getTopHeight + bg.getBottomHeight)
        val listBackground = list.getStyle.background
        listBackground.foreach(bg => height += bg.getTopHeight + bg.getBottomHeight)

        val heightBelow = stagePosition.y
        val heightAbove = stage.getHeight - heightBelow - selectBox.getHeight
        var below       = true
        if (height > heightBelow) {
          if (heightAbove > heightBelow) {
            below = false
            height = Math.min(height, heightAbove)
          } else {
            height = heightBelow
          }
        }

        if (below)
          setY(stagePosition.y - height)
        else
          setY(stagePosition.y + selectBox.getHeight)

        setHeight(height)
        validate()
        val width = Math.max(getPrefWidth, selectBox.getWidth)
        setWidth(width)

        var x = stagePosition.x
        if (x + width > stage.getWidth) {
          x -= getWidth - selectBox.getWidth - 1
          if (x < 0) x = 0
        }
        setX(x)

        validate()
        scrollTo(0, list.getHeight - selectBox.getSelectedIndex * itemHeight - itemHeight / 2, 0, 0, centerHorizontal = true, centerVertical = true)
        updateVisualScroll()

        previousScrollFocus = Nullable.empty
        val actor = stage.getScrollFocus
        actor.foreach { a =>
          if (!a.isDescendantOf(this)) previousScrollFocus = Nullable(a)
        }
        stage.setScrollFocus(Nullable(this))

        selectBox.getSelected.foreach(sel => list.selection.set(sel))
        list.setTouchable(Touchable.enabled)
        clearActions()
        selectBox.onShow(this, below)
      }

    def hide(): Unit =
      if (!list.isTouchable || !hasParent) ()
      else {
        list.setTouchable(Touchable.disabled)

        val stage = getStage
        stage.foreach { s =>
          s.removeCaptureListener(hideListener)
          s.removeListener(list.getKeyListener)
          previousScrollFocus.foreach { psf =>
            if (psf.getStage.isEmpty) previousScrollFocus = Nullable.empty
          }
          val actor = s.getScrollFocus
          if (actor.isEmpty || actor.fold(false)(a => isAscendantOf(a))) {
            s.setScrollFocus(previousScrollFocus)
          }
        }

        clearActions()
        selectBox.onHide(this)
      }

    override def draw(batch: Batch, parentAlpha: Float): Unit = {
      selectBox.localToStageCoordinates(temp.set(0, 0))
      if (temp != stagePosition) hide()
      super.draw(batch, parentAlpha)
    }

    override def act(delta: Float)(using sge: Sge): Unit = {
      super.act(delta)
      toFront()
    }

    override protected[scene2d] def setStage(stage: Nullable[Stage]): Unit = {
      val oldStage = getStage
      oldStage.foreach { os =>
        os.removeCaptureListener(hideListener)
        os.removeListener(list.getKeyListener)
      }
      super.setStage(stage)
    }

    def getList: SgeList[T] = list

    def getSelectBox: SelectBox[T] = selectBox
  }

  /** The style for a select box, see {@link SelectBox}.
    * @author
    *   mzechner
    * @author
    *   Nathan Sweet
    */
  class SelectBoxStyle() {
    var font:               BitmapFont                 = scala.compiletime.uninitialized
    var fontColor:          Color                      = new Color(1, 1, 1, 1)
    var overFontColor:      Nullable[Color]            = Nullable.empty
    var disabledFontColor:  Nullable[Color]            = Nullable.empty
    var background:         Nullable[Drawable]         = Nullable.empty
    var scrollStyle:        ScrollPane.ScrollPaneStyle = scala.compiletime.uninitialized
    var listStyle:          SgeList.ListStyle          = scala.compiletime.uninitialized
    var backgroundOver:     Nullable[Drawable]         = Nullable.empty
    var backgroundOpen:     Nullable[Drawable]         = Nullable.empty
    var backgroundDisabled: Nullable[Drawable]         = Nullable.empty

    def this(font: BitmapFont, fontColor: Color, background: Nullable[Drawable], scrollStyle: ScrollPane.ScrollPaneStyle, listStyle: SgeList.ListStyle) = {
      this()
      this.font = font
      this.fontColor.set(fontColor)
      this.background = background
      this.scrollStyle = scrollStyle
      this.listStyle = listStyle
    }

    def this(style: SelectBoxStyle) = {
      this()
      font = style.font
      fontColor.set(style.fontColor)

      style.overFontColor.foreach { c => overFontColor = Nullable(new Color(c)) }
      style.disabledFontColor.foreach { c => disabledFontColor = Nullable(new Color(c)) }

      background = style.background
      scrollStyle = new ScrollPane.ScrollPaneStyle(style.scrollStyle)
      listStyle = new SgeList.ListStyle(style.listStyle)

      backgroundOver = style.backgroundOver
      backgroundOpen = style.backgroundOpen
      backgroundDisabled = style.backgroundDisabled
    }
  }
}
