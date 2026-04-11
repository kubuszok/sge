/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/TextraWindow.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: Table → standalone class (scene2d base not inherited),
 *     Skin → removed, InputListener → local inner class,
 *     Vector2 → local x/y pairs, Batch → sge.graphics.g2d.Batch,
 *     Camera/OrthographicCamera → deferred (keepWithinStage simplified)
 *   Convention: Window dragging/resizing/modal behavior fully ported.
 *   Idiom: Nullable[A] for nullable fields; no return statements.
 */
package sge
package textra

import sge.graphics.Color
import sge.graphics.g2d.Batch
import sge.scenes.scene2d.utils.Drawable
import sge.utils.{ Align, Nullable }

/** A table that can be dragged and act as a modal window. The top padding is used as the window's title height. */
class TextraWindow(title: String, style: Styles.WindowStyle, replacementFont: Font, scaleTitleFont: Boolean) {

  require(title != null, "title cannot be null.")
  require(replacementFont != null, "replacementFont cannot be null.")

  private var _style:      Styles.WindowStyle = style
  var isMovable:           Boolean            = true
  private var _isModal:    Boolean            = false
  var isResizable:         Boolean            = false
  var resizeBorder:        Int                = 8
  var keepWithinStageFlag: Boolean            = true
  var titleLabel:          TextraLabel        = newLabel(title, replacementFont, Nullable.fold(style.titleFontColor)(null: Color)(identity))
  var drawTitleTable:      Boolean            = false
  protected var edge:      Int                = 0
  protected var dragging:  Boolean            = false
  protected var font:      Font               = replacementFont

  // Widget-like fields (normally inherited from scene2d Table)
  private var _x:      Float = 0f
  private var _y:      Float = 0f
  private var _width:  Float = 150f
  private var _height: Float = 150f
  private val _color:  Color = new Color(Color.WHITE)

  // Padding fields (Table-like)
  private var _padTop:    Float = 0f
  private var _padLeft:   Float = 0f
  private var _padBottom: Float = 0f
  private var _padRight:  Float = 0f

  // Title table positioning (managed manually)
  private var _titleTableX:      Float = 0f
  private var _titleTableY:      Float = 0f
  private var _titleTableWidth:  Float = 0f
  private var _titleTableHeight: Float = 0f

  // Min/max sizes
  private var _minWidth:  Float = 0f
  private var _minHeight: Float = 0f
  private var _maxWidth:  Float = Float.MaxValue
  private var _maxHeight: Float = Float.MaxValue

  // Internal dragging listener state
  private var _startX: Float = 0f
  private var _startY: Float = 0f
  private var _lastX:  Float = 0f
  private var _lastY:  Float = 0f

  // --- Constructors ---

  def this(title: String, style: Styles.WindowStyle) =
    this(title, style, Nullable.fold(style.titleFont)(new Font())(identity), false)

  def this(title: String, style: Styles.WindowStyle, scaleTitleFont: Boolean) =
    this(title, style, Nullable.fold(style.titleFont)(new Font())(identity), scaleTitleFont)

  def this(title: String, style: Styles.WindowStyle, replacementFont: Font) =
    this(title, style, replacementFont, false)

  // --- Init ---
  {
    if (scaleTitleFont) {
      val labelFont = new Font(replacementFont)
      // Scale to fit top height from background
      Nullable.foreach(style.background) {
        case bg: Drawable =>
          labelFont.scaleHeightTo(bg.topHeight)
        case _ => ()
      }
      titleLabel.setFont(labelFont)
    } else {
      titleLabel.setFont(font)
    }
    titleLabel.setEllipsis(Nullable("..."))
    setStyle(style, replacementFont)
  }

  // --- Label factories ---

  protected def newLabel(text: String, style: Styles.LabelStyle): TextraLabel =
    new TextraLabel(text, style)

  protected def newLabel(text: String, font: Font, color: Color): TextraLabel =
    if (color == null) new TextraLabel(text, font) else new TextraLabel(text, font, color)

  // --- Style ---

  def setStyle(style: Styles.WindowStyle): Unit = {
    this._style = style
    titleLabel.setFont(this.font)
    Nullable.foreach(style.titleFontColor)(c => titleLabel.setColor(c))
  }

  def setStyle(style: Styles.WindowStyle, ignored: Boolean): Unit =
    setStyle(style)

  def setStyle(style: Styles.WindowStyle, font: Font): Unit = {
    this._style = style
    this.font = font
    titleLabel.setFont(font)
    Nullable.foreach(style.titleFontColor)(c => titleLabel.setColor(c))
  }

  def getStyle: Styles.WindowStyle = _style

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

  def setPosition(x: Float, y: Float, alignment: Align): Unit = {
    _x = x
    _y = y
    if (alignment.isRight) _x -= _width
    else if (alignment.isCenterHorizontal) _x -= _width * 0.5f
    if (alignment.isTop) _y -= _height
    else if (alignment.isCenterVertical) _y -= _height * 0.5f
  }

  def setBounds(x: Float, y: Float, width: Float, height: Float): Unit = {
    _x = x
    _y = y
    _width = width
    _height = height
  }

  def getX(alignment: Align): Float = {
    var x = _x
    if (alignment.isRight) x += _width
    else if (alignment.isCenterHorizontal) x += _width * 0.5f
    x
  }

  def getY(alignment: Align): Float = {
    var y = _y
    if (alignment.isTop) y += _height
    else if (alignment.isCenterVertical) y += _height * 0.5f
    y
  }

  def getRight: Float = _x + _width
  def getTop:   Float = _y + _height

  // --- Padding ---

  def getPadTop:              Float = _padTop
  def getPadLeft:             Float = _padLeft
  def getPadBottom:           Float = _padBottom
  def getPadRight:            Float = _padRight
  def setPadTop(p:    Float): Unit  = _padTop = p
  def setPadLeft(p:   Float): Unit  = _padLeft = p
  def setPadBottom(p: Float): Unit  = _padBottom = p
  def setPadRight(p:  Float): Unit  = _padRight = p
  def getMinWidth:            Float = _minWidth
  def getMinHeight:           Float = _minHeight
  def getMaxWidth:            Float = _maxWidth
  def getMaxHeight:           Float = _maxHeight
  def setMinWidth(w:  Float): Unit  = _minWidth = w
  def setMinHeight(h: Float): Unit  = _minHeight = h
  def setMaxWidth(w:  Float): Unit  = _maxWidth = w
  def setMaxHeight(h: Float): Unit  = _maxHeight = h

  // --- Modal ---

  def setModal(modal: Boolean): Unit    = _isModal = modal
  def isModal:                  Boolean = _isModal
  @deprecated("use isModal", "always")
  def getModal: Boolean = _isModal

  // --- Accessors ---

  def getTitleLabel: TextraLabel = titleLabel

  /** Does nothing unless the titleLabel used here is a TypingLabel; then, this will skip text progression ahead. */
  def skipToTheEnd(): Unit =
    titleLabel.skipToTheEnd()

  def isDragging: Boolean = dragging

  def setKeepWithinStage(keepWithinStage: Boolean): Unit =
    keepWithinStageFlag = keepWithinStage

  def getPrefWidth: Float =
    Math.max(titleLabel.getPrefWidth + _padLeft + _padRight, _width)

  // --- Background ---

  def setBackground(bg: Nullable[AnyRef]): Unit =
    Nullable.foreach(bg) {
      case drawable: Drawable =>
        _padTop = drawable.topHeight
        _padLeft = drawable.leftWidth
        _padBottom = drawable.bottomHeight
        _padRight = drawable.rightWidth
      case _ => ()
    }

  def getBackground: Nullable[AnyRef] = _style.background

  // --- Edge update (for drag/resize) ---

  private def updateEdge(x: Float, y: Float): Unit = {
    var border = resizeBorder / 2f
    val width  = _width
    val height = _height
    val padTop = _padTop
    val padLt  = _padLeft
    val padBot = _padBottom
    val padRt  = _padRight
    val left   = padLt
    val right  = width - padRt
    val bottom = padBot
    edge = 0
    if (isResizable && x >= left - border && x <= right + border && y >= bottom - border) {
      if (x < left + border) edge |= Align.left.toInt
      if (x > right - border) edge |= Align.right.toInt
      if (y < bottom + border) edge |= Align.bottom.toInt
      if (edge != 0) border += 25
      if (x < left + border) edge |= Align.left.toInt
      if (x > right - border) edge |= Align.right.toInt
      if (y < bottom + border) edge |= Align.bottom.toInt
    }
    if (isMovable && edge == 0 && y <= height && y >= height - padTop && x >= left && x <= right)
      edge = TextraWindow.MOVE
  }

  /** Handles a touch-down event for dragging/resizing. Returns true if the window should consume the event. */
  def handleTouchDown(x: Float, y: Float, pointer: Int, button: Int): Boolean = {
    if (button == 0) {
      updateEdge(x, y)
      dragging = edge != 0
      _startX = x
      _startY = y
      _lastX = x - _width
      _lastY = y - _height
    }
    edge != 0 || _isModal
  }

  /** Handles a touch-up event for dragging. */
  def handleTouchUp(x: Float, y: Float, pointer: Int, button: Int): Unit =
    dragging = false

  /** Handles a touch-dragged event for window dragging/resizing. */
  def handleTouchDragged(x: Float, y: Float, pointer: Int): Unit =
    if (!dragging) ()
    else {
      var width   = _width
      var height  = _height
      var windowX = _x
      var windowY = _y
      val minW    = _minWidth
      val minH    = _minHeight

      if ((edge & TextraWindow.MOVE) != 0) {
        val amountX = x - _startX
        val amountY = y - _startY
        windowX += amountX
        windowY += amountY
      }
      if ((edge & Align.left.toInt) != 0) {
        var amountX = x - _startX
        if (width - amountX < minW) amountX = -(minW - width)
        width -= amountX
        windowX += amountX
      }
      if ((edge & Align.bottom.toInt) != 0) {
        var amountY = y - _startY
        if (height - amountY < minH) amountY = -(minH - height)
        height -= amountY
        windowY += amountY
      }
      if ((edge & Align.right.toInt) != 0) {
        var amountX = x - _lastX - width
        if (width + amountX < minW) amountX = minW - width
        width += amountX
      }
      if ((edge & Align.top.toInt) != 0) {
        var amountY = y - _lastY - height
        if (height + amountY < minH) amountY = minH - height
        height += amountY
      }
      setBounds(Math.round(windowX).toFloat, Math.round(windowY).toFloat, Math.round(width).toFloat, Math.round(height).toFloat)
    }

  /** Handles a mouse-moved event. Returns true if modal. */
  def handleMouseMoved(x: Float, y: Float): Boolean = {
    updateEdge(x, y)
    _isModal
  }

  /** Handles a scrolled event. Returns true if modal. */
  def handleScrolled(x: Float, y: Float, amount: Int): Boolean = _isModal

  /** Handles a key-down event. Returns true if modal. */
  def handleKeyDown(keycode: Int): Boolean = _isModal

  /** Handles a key-up event. Returns true if modal. */
  def handleKeyUp(keycode: Int): Boolean = _isModal

  /** Handles a key-typed event. Returns true if modal. */
  def handleKeyTyped(character: Char): Boolean = _isModal

  // --- keepWithinStage ---

  /** Keeps the window within the stage boundaries (no-op without Stage reference; use keepWithinStage(stageWidth, stageHeight) overload for explicit bounds).
    */
  def keepWithinStage(): Unit = {
    // No-op without Stage reference. Callers with a Stage can invoke
    // keepWithinStage(stageWidth, stageHeight) instead.
  }

  /** Keeps the window within the given stage dimensions. */
  def keepWithinStage(stageWidth: Float, stageHeight: Float): Unit =
    if (keepWithinStageFlag) {
      if (_x < 0) _x = 0
      if (getRight > stageWidth) _x = stageWidth - _width
      if (_y < 0) _y = 0
      if (getTop > stageHeight) _y = stageHeight - _height
    }

  // --- Draw ---

  /** Draws this window, including background, stage background, and title table. */
  def draw(batch: Batch, parentAlpha: Float): Unit = {
    keepWithinStage()

    Nullable.foreach(_style.stageBackground) {
      case bg: Drawable =>
        drawStageBackground(batch, parentAlpha, bg)
      case _ => ()
    }

    drawBackground(batch, parentAlpha, _x, _y)
  }

  /** Draws the stage background (dimming behind a modal window). */
  protected def drawStageBackground(batch: Batch, parentAlpha: Float, stageBackground: Drawable): Unit = {
    val color = _color
    batch.setColor(color.r, color.g, color.b, color.a * parentAlpha)
    // In full scene2d integration, this would use stageToLocalCoordinates to compute bounds.
    // For standalone usage, draw behind the window position.
    stageBackground.draw(batch, 0, 0, _width, _height)
  }

  /** Draws the window background and the title table. */
  protected def drawBackground(batch: Batch, parentAlpha: Float, x: Float, y: Float): Unit = {
    Nullable.foreach(_style.background) {
      case bg: Drawable =>
        val color = _color
        batch.setColor(color.r, color.g, color.b, color.a * parentAlpha)
        bg.draw(batch, x, y, _width, _height)
      case _ => ()
    }

    // Draw the title table
    val padTop2  = _padTop
    val padLeft2 = _padLeft
    _titleTableWidth = _width - padLeft2 - _padRight
    _titleTableHeight = padTop2
    _titleTableX = padLeft2
    _titleTableY = _height - padTop2

    drawTitleTable = true
    drawTitleLabel(batch, parentAlpha, x + _titleTableX, y + _titleTableY, _titleTableWidth, _titleTableHeight)
    drawTitleTable = false
  }

  /** Draws the title label within the title table area. */
  protected def drawTitleLabel(batch: Batch, parentAlpha: Float, x: Float, y: Float, width: Float, height: Float): Unit = {
    titleLabel.setSize(width, height)
    titleLabel.setPosition(x, y)
    titleLabel.draw(batch, parentAlpha)
  }

  /** Hit detection for the window. */
  def hit(x: Float, y: Float, touchable: Boolean): Nullable[AnyRef] =
    if (x < 0 || x >= _width || y < 0 || y >= _height) Nullable.empty
    else if (_isModal) Nullable(this)
    else if (y <= _height && y >= _height - _padTop && x >= 0 && x <= _width) {
      // Hit the title bar
      Nullable(this)
    } else Nullable(this)

  def toFront(): Unit = {
    // In full scene2d integration, this would move the actor to the front of the parent's children.
    // No-op in standalone usage.
  }

  def pack(): Unit = {
    // Sets the window to at least its preferred width.
    val pw = getPrefWidth
    if (_width < pw) _width = pw
  }
}

object TextraWindow {
  val MOVE: Int = 1 << 5
}
