/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/ui/SelectBox.java
 * Original authors: mzechner, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: getClickListener -> clickListener (Scala property)
 *   Convention: null -> Nullable; (using Sge) context; Java List<T> -> SgeList[T]; DynamicArray with MkArray.anyRef cast; boundary/break
 *   Idiom: split packages
 *   Note: Java-style getters/setters retained — setItems/setSelected/setSelectedIndex have validation/events logic; disabled/setDisabled from Disableable trait; style via Styleable
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
import sge.math.{ Interpolation, Vector2 }
import sge.scenes.scene2d.{ Actor, InputEvent, InputListener, Stage, Touchable }
import sge.scenes.scene2d.actions.Actions
import sge.scenes.scene2d.utils.{ ArraySelection, ClickListener, Disableable, Drawable }
import sge.Input.{ Button, Key }
import sge.utils.{ Align, DynamicArray, MkArray, Nullable, Seconds }

/** A select box (aka a drop-down list) allows a user to choose one of a number of values from a list. When inactive, the selected value is displayed. When activated, it shows the list of values that
  * may be selected. <p> {@link ChangeEvent} is fired when the selectbox selection changes. <p> The preferred size of the select box is determined by the maximum text bounds of the items and the size
  * of the {@link SelectBoxStyle#background}.
  * @author
  *   mzechner
  * @author
  *   Nathan Sweet
  */
class SelectBox[T](initialStyle: SelectBox.SelectBoxStyle)(using Sge) extends Widget with Disableable with Styleable[SelectBox.SelectBoxStyle] {

  import SelectBox._

  private var _style:         SelectBoxStyle         = scala.compiletime.uninitialized
  val items:                  DynamicArray[T]        = DynamicArray.createWithMk(MkArray.anyRef.asInstanceOf[MkArray[T]], 16, true)
  var scrollPane:             SelectBoxScrollPane[T] = scala.compiletime.uninitialized
  private var _prefWidth:     Float                  = 0f
  private var _prefHeight:    Float                  = 0f
  private var _clickListener: ClickListener          = scala.compiletime.uninitialized
  private var _disabled:      Boolean                = false
  private var _alignment:     Align                  = Align.left
  var selectedPrefWidth:      Boolean                = false

  val selection: ArraySelection[T] = new ArraySelection(items) {
    override def fireChangeEvent(): Boolean = {
      if (selectedPrefWidth) invalidateHierarchy()
      super.fireChangeEvent()
    }
  }

  def this(skin: Skin)(using Sge) = this(skin.get[SelectBox.SelectBoxStyle])
  def this(skin: Skin, styleName: String)(using Sge) = this(skin.get[SelectBox.SelectBoxStyle](styleName))

  setStyle(initialStyle)
  setSize(prefWidth, prefHeight)

  selection.setActor(Nullable(this))
  selection.required = true

  scrollPane = newScrollPane()

  private val self = this
  addListener {
    _clickListener = new ClickListener() {
      override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Button): Boolean =
        if (pointer == 0 && button != Button(0)) false
        else if (self.disabled) false
        else {
          if (self.scrollPane.hasParent)
            self.hideScrollPane()
          else
            self.showScrollPane()
          true
        }
    }
    _clickListener
  }

  /** Allows a subclass to customize the scroll pane shown when the select box is open. */
  protected def newScrollPane(): SelectBoxScrollPane[T] =
    SelectBoxScrollPane(this)

  /** Set the max number of items to display when the select box is opened. Set to 0 (the default) to display as many as fit in the stage height.
    */
  def setMaxListCount(maxListCount: Int): Unit =
    scrollPane.maxListCount = maxListCount

  /** @return Max number of items to display when the box is opened, or <= 0 to display them all. */
  def maxListCount: Int =
    scrollPane.maxListCount

  override protected[scene2d] def setStage(stage: Nullable[Stage]): Unit = {
    if (stage.isEmpty) scrollPane.hide()
    super.setStage(stage)
  }

  override def setStyle(style: SelectBoxStyle): Unit = {
    this._style = style

    Nullable(scrollPane).foreach { sp =>
      sp.setStyle(style.scrollStyle)
      sp.list.setStyle(style.listStyle)
    }
    invalidateHierarchy()
  }

  /** Returns the select box's style. Modifying the returned style may not have an effect until {@link #setStyle(SelectBoxStyle)} is called.
    */
  override def style: SelectBoxStyle = _style

  /** Set the backing Array that makes up the choices available in the SelectBox */
  def setItems(newItems: T*): Unit = {
    val oldPrefWidth = prefWidth

    items.clear()
    items.addAll(newItems)
    selection.validate()
    scrollPane.list.setItems(items)

    invalidate()
    if (oldPrefWidth != prefWidth) invalidateHierarchy()
  }

  /** Sets the items visible in the select box. */
  def setItems(newItems: DynamicArray[T]): Unit = {
    val oldPrefWidth = prefWidth

    if (newItems ne items) {
      items.clear()
      items.addAll(newItems)
    }
    selection.validate()
    scrollPane.list.setItems(items)

    invalidate()
    if (oldPrefWidth != prefWidth) invalidateHierarchy()
  }

  def clearItems(): Unit =
    if (items.isEmpty) ()
    else {
      items.clear()
      selection.clear()
      scrollPane.list.clearItems()
      invalidateHierarchy()
    }

  // getItems removed — items is a public val

  override def layout(): Unit = {
    val bg   = _style.background
    val font = _style.font

    bg.fold {
      _prefHeight = font.capHeight - font.descent * 2
    } { background =>
      _prefHeight = Math.max(background.topHeight + background.bottomHeight + font.capHeight - font.descent * 2, background.minHeight)
    }

    val layoutPool  = Actor.POOLS.pool[GlyphLayout]
    val glyphLayout = layoutPool.obtain()
    if (selectedPrefWidth) {
      _prefWidth = 0
      bg.foreach { background => _prefWidth = background.leftWidth + background.rightWidth }
      val sel0 = selected
      sel0.foreach { sel =>
        glyphLayout.setText(font, itemToString(sel))
        _prefWidth += glyphLayout.width
      }
    } else {
      var maxItemWidth = 0f
      var i            = 0
      while (i < items.size) {
        glyphLayout.setText(font, itemToString(items(i)))
        maxItemWidth = Math.max(glyphLayout.width, maxItemWidth)
        i += 1
      }

      _prefWidth = maxItemWidth
      bg.foreach { background =>
        _prefWidth = Math.max(_prefWidth + background.leftWidth + background.rightWidth, background.minWidth)
      }

      val listStyle   = _style.listStyle
      val scrollStyle = _style.scrollStyle
      var scrollWidth = maxItemWidth + listStyle.selection.leftWidth + listStyle.selection.rightWidth
      scrollStyle.background.foreach { scrollBg =>
        scrollWidth = Math.max(scrollWidth + scrollBg.leftWidth + scrollBg.rightWidth, scrollBg.minWidth)
      }
      if (Nullable(scrollPane).forall(!_.disableY)) {
        scrollWidth += Math.max(
          _style.scrollStyle.vScroll.map(_.minWidth).getOrElse(0f),
          _style.scrollStyle.vScrollKnob.map(_.minWidth).getOrElse(0f)
        )
      }
      _prefWidth = Math.max(_prefWidth, scrollWidth)
    }
    layoutPool.free(glyphLayout)
  }

  /** Returns appropriate background drawable from the style based on the current select box state. */
  protected def backgroundDrawable: Nullable[Drawable] =
    if (disabled && _style.backgroundDisabled.isDefined) _style.backgroundDisabled
    else if (scrollPane.hasParent && _style.backgroundOpen.isDefined) _style.backgroundOpen
    else if (isOver && _style.backgroundOver.isDefined) _style.backgroundOver
    else _style.background

  /** Returns the appropriate label font color from the style based on the current button state. */
  protected def fontColor: Color =
    if (disabled && _style.disabledFontColor.isDefined) _style.disabledFontColor.getOrElse(_style.fontColor)
    else if (_style.overFontColor.isDefined && (isOver || scrollPane.hasParent)) _style.overFontColor.getOrElse(_style.fontColor)
    else _style.fontColor

  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    validate()

    val background = backgroundDrawable
    val fc         = this.fontColor
    val font       = _style.font

    val c  = this.color
    var bx = this.x
    var by = this.y
    var bw = this.width
    var bh = this.height

    batch.setColor(c.r, c.g, c.b, c.a * parentAlpha)
    background.foreach(_.draw(batch, bx, by, bw, bh))

    val selected: Nullable[T] = selection.first
    selected.foreach { sel =>
      background.fold {
        by += (bh / 2 + font.data.capHeight / 2).toInt.toFloat
      } { bg =>
        bw -= bg.leftWidth + bg.rightWidth
        bh -= bg.bottomHeight + bg.topHeight
        bx += bg.leftWidth
        by += (bh / 2 + bg.bottomHeight + font.data.capHeight / 2).toInt.toFloat
      }
      font.color.set(fc.r, fc.g, fc.b, fc.a * parentAlpha)
      drawItem(batch, font, sel, bx, by, bw)
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
  def alignment_=(value: Align): Unit =
    this._alignment = value

  def alignment: Align = _alignment

  /** Get the set of selected items, useful when multiple items are selected
    * @return
    *   a Selection object containing the selected elements
    */
  // getSelection removed — selection is a public val

  /** Returns the first selected item, or null. For multiple selections use {@link SelectBox#selection}. */
  def selected: Nullable[T] = selection.first

  /** Sets the selection to only the passed item, if it is a possible choice, else selects the first item. */
  def setSelected(item: Nullable[T]): Unit =
    item.fold {
      if (items.nonEmpty) selection.set(items.first)
      else selection.clear()
    } { i =>
      if (items.contains(i)) selection.set(i)
      else if (items.nonEmpty) selection.set(items.first)
      else selection.clear()
    }

  /** @return The index of the first selected item. The top item has an index of 0. Nothing selected has an index of -1. */
  def selectedIndex: Int = {
    val sel = selection.items
    if (sel.isEmpty) -1 else items.indexOf(sel.head)
  }

  /** Sets the selection to only the selected index. */
  def setSelectedIndex(index: Int): Unit =
    selection.set(items(index))

  /** When true the pref width is based on the selected item. */
  def setSelectedPrefWidth(selectedPrefWidth: Boolean): Unit =
    this.selectedPrefWidth = selectedPrefWidth

  // getSelectedPrefWidth removed — selectedPrefWidth is a public var

  /** Returns the pref width of the select box if the widest item was selected, for use when {@link #setSelectedPrefWidth(boolean)} is true.
    */
  def maxSelectedPrefWidth: Float = {
    val layoutPool  = Actor.POOLS.pool[GlyphLayout]
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
      width = Math.max(width + bg.leftWidth + bg.rightWidth, bg.minWidth)
    }
    width
  }

  def disabled_=(value: Boolean): Unit = {
    if (value && !this._disabled) hideScrollPane()
    this._disabled = value
  }

  def disabled: Boolean = _disabled

  override def prefWidth: Float = {
    validate()
    _prefWidth
  }

  override def prefHeight: Float = {
    validate()
    _prefHeight
  }

  protected def itemToString(item: T): String = item.toString

  def showScrollPane(): Unit =
    if (items.isEmpty) ()
    else stage.foreach(stage => scrollPane.show(stage))

  def hideScrollPane(): Unit =
    scrollPane.hide()

  /** Returns the list shown when the select box is open. */
  def list: SgeList[T] = scrollPane.list

  /** Disables scrolling of the list shown when the select box is open. */
  def setScrollingDisabled(y: Boolean): Unit = {
    scrollPane.setScrollingDisabled(true, y)
    invalidateHierarchy()
  }

  // getScrollPane removed — scrollPane is a public var

  def isOver: Boolean = clickListener.over

  /** Returns the click listener used to detect clicks on the select box. */
  def clickListener: ClickListener = _clickListener

  protected def onShow(scrollPane: Actor, below: Boolean): Unit = {
    scrollPane.color.a = 0
    scrollPane.addAction(Actions.fadeIn(Seconds(0.3f), Nullable(Interpolation.fade)))
  }

  protected def onHide(scrollPane: Actor): Unit = {
    scrollPane.color.a = 1
    scrollPane.addAction(Actions.sequence(Actions.fadeOut(Seconds(0.15f), Nullable(Interpolation.fade)), Actions.removeActor()))
  }
}

object SelectBox {

  private val temp: Vector2 = Vector2()

  /** The scroll pane shown when a select box is open.
    * @author
    *   Nathan Sweet
    */
  class SelectBoxScrollPane[T](val selectBox: SelectBox[T])(using Sge) extends ScrollPane(Nullable.empty, selectBox._style.scrollStyle) {

    var maxListCount:                Int             = 0
    private val stagePosition:       Vector2         = Vector2()
    val list:                        SgeList[T]      = newList()
    private var hideListener:        InputListener   = scala.compiletime.uninitialized
    private var previousScrollFocus: Nullable[Actor] = Nullable.empty

    setOverscroll(false, false)
    setFadeScrollBars(false)
    setScrollingDisabled(true, false)

    list.touchable = Touchable.disabled
    list.setTypeToSelect(true)
    setActor(Nullable(list))

    list.addListener(
      new ClickListener() {
        override def clicked(event: InputEvent, x: Float, y: Float): Unit = {
          val selected = list.selected
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
          if (toActor.forall(a => !scrollPaneSelf.isAscendantOf(a))) {
            val selected = selectBox.selected
            selected.foreach(sel => list.selection.set(sel))
          }
      }
    )

    hideListener = new InputListener() {
      override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Button): Boolean = {
        val target = event.target
        if (target.exists(scrollPaneSelf.isAscendantOf)) ()
        else {
          selectBox.selected.foreach(sel => list.selection.set(sel))
          hide()
        }
        false
      }

      override def keyDown(event: InputEvent, keycode: Key): Boolean =
        keycode match {
          case Input.Keys.NUMPAD_ENTER | Input.Keys.ENTER =>
            list.selected.foreach(sel => selectBox.selection.choose(sel))
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
        stage.addListener(list.keyListener)

        selectBox.localToStageCoordinates(stagePosition.set(0, 0))

        // Show the list above or below the select box, limited to a number of items and the available height in the stage.
        val itemHeight           = list.itemHeight
        var height               = itemHeight * (if (maxListCount <= 0) selectBox.items.size else Math.min(maxListCount, selectBox.items.size))
        val scrollPaneBackground = style.background
        scrollPaneBackground.foreach(bg => height += bg.topHeight + bg.bottomHeight)
        val listBackground = list.style.background
        listBackground.foreach(bg => height += bg.topHeight + bg.bottomHeight)

        val heightBelow = stagePosition.y
        val heightAbove = stage.height - heightBelow - selectBox.height
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
          setY(stagePosition.y + selectBox.height)

        setHeight(height)
        validate()
        val width = Math.max(prefWidth, selectBox.width)
        setWidth(width)

        var x = stagePosition.x
        if (x + width > stage.width) {
          x -= this.width - selectBox.width - 1
          if (x < 0) x = 0
        }
        setX(x)

        validate()
        scrollTo(0, list.height - selectBox.selectedIndex * itemHeight - itemHeight / 2, 0, 0, centerHorizontal = true, centerVertical = true)
        updateVisualScroll()

        previousScrollFocus = Nullable.empty
        val actor = stage.scrollFocus
        actor.foreach { a =>
          if (!a.isDescendantOf(this)) previousScrollFocus = Nullable(a)
        }
        stage.setScrollFocus(Nullable(this))

        selectBox.selected.foreach(sel => list.selection.set(sel))
        list.touchable = Touchable.enabled
        clearActions()
        selectBox.onShow(this, below)
      }

    def hide(): Unit =
      if (!list.isTouchable || !hasParent) ()
      else {
        list.touchable = Touchable.disabled

        this.stage.foreach { s =>
          s.removeCaptureListener(hideListener)
          s.removeListener(list.keyListener)
          previousScrollFocus.foreach { psf =>
            if (psf.stage.isEmpty) previousScrollFocus = Nullable.empty
          }
          val actor = s.scrollFocus
          if (actor.isEmpty || actor.exists(a => isAscendantOf(a))) {
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

    override def act(delta: Seconds): Unit = {
      super.act(delta)
      toFront()
    }

    override protected[scene2d] def setStage(stage: Nullable[Stage]): Unit = {
      this.stage.foreach { os =>
        os.removeCaptureListener(hideListener)
        os.removeListener(list.keyListener)
      }
      super.setStage(stage)
    }

    // getList removed — list is a public val
    // getSelectBox removed — selectBox is a public val
  }

  /** The style for a select box, see {@link SelectBox}.
    * @author
    *   mzechner
    * @author
    *   Nathan Sweet
    */
  class SelectBoxStyle() {
    var font:               BitmapFont                 = scala.compiletime.uninitialized
    var fontColor:          Color                      = Color(1, 1, 1, 1)
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

      style.overFontColor.foreach { c => overFontColor = Nullable(Color(c)) }
      style.disabledFontColor.foreach { c => disabledFontColor = Nullable(Color(c)) }

      background = style.background
      scrollStyle = ScrollPane.ScrollPaneStyle(style.scrollStyle)
      listStyle = SgeList.ListStyle(style.listStyle)

      backgroundOver = style.backgroundOver
      backgroundOpen = style.backgroundOpen
      backgroundDisabled = style.backgroundDisabled
    }
  }
}
