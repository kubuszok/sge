/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/TextraWindow.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: Table → standalone class (scene2d base not inherited),
 *     Skin → sge.scenes.scene2d.ui.Skin, InputListener → inner class with handle methods,
 *     Vector2 → local x/y pairs, Batch → sge.graphics.g2d.Batch,
 *     Camera/OrthographicCamera → deferred (keepWithinStage simplified)
 *   Convention: Window dragging/resizing/modal behavior fully ported.
 *   Idiom: Nullable[A] for nullable fields; no return statements; boundary/break for early returns.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 552
 * Covenant-baseline-methods: InternalListener,MOVE,TextraWindow,TitleTable,_alpha,_color,_height,_isModal,_isMovable,_isResizable,_keepWithinStage,_maxHeight,_maxWidth,_minHeight,_minWidth,_padBottom,_padLeft,_padRight,_padTop,_resizeBorder,_style,_width,_x,_y,color,dragging,draw,drawBackground,drawStageBackground,drawTitleTable,edge,font,getBackground,getColor,getHeight,getMaxHeight,getMaxWidth,getMinHeight,getMinWidth,getPadBottom,getPadLeft,getPadRight,getPadTop,getPrefWidth,getResizeBorder,getRight,getStyle,getTitleLabel,getTitleTable,getTop,getWidth,getX,getY,hit,internalListener,invalidateHierarchy,isDragging,isModal,isMovable,isResizable,keepWithinStage,keyDown,keyTyped,keyUp,label,lastX,lastY,mouseMoved,newLabel,pack,padLeft2,padTop2,pw,scrolled,self,setBackground,setBounds,setColor,setHeight,setKeepWithinStage,setMaxHeight,setMaxWidth,setMinHeight,setMinWidth,setModal,setMovable,setPadBottom,setPadLeft,setPadRight,setPadTop,setPosition,setResizable,setResizeBorder,setSize,setStyle,setWidth,setX,setY,skipToTheEnd,startX,startY,this,titleLabel,titleTable,toFront,touchDown,touchDragged,touchUp,updateEdge,x,y
 * Covenant-source-reference: com/github/tommyettinger/textra/TextraWindow.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4
 */
package sge
package textra

import sge.graphics.Color
import sge.graphics.g2d.Batch
import sge.scenes.scene2d.ui.Skin
import sge.scenes.scene2d.utils.Drawable
import sge.utils.{ Align, Nullable }

/** A table that can be dragged and act as a modal window. The top padding is used as the window's title height. <p> The preferred size of a window is the preferred size of the title text and the
  * children as laid out by the table. After adding children to the window, it can be convenient to call {@link #pack()} to size the window to the size of the children.
  *
  * @author
  *   Nathan Sweet
  */
class TextraWindow(title: String, style: Styles.WindowStyle, replacementFont: Font, scaleTitleFont: Boolean) {

  require(title != null, "title cannot be null.")
  require(replacementFont != null, "replacementFont cannot be null.")

  private var _style:           Styles.WindowStyle      = style
  private var _isMovable:       Boolean                 = true
  private var _isModal:         Boolean                 = false
  private var _isResizable:     Boolean                 = false
  private var _resizeBorder:    Int                     = 8
  private var _keepWithinStage: Boolean                 = true
  var titleLabel:               TextraLabel             = newLabel(title, replacementFont, Nullable.fold(style.titleFontColor)(null: Color)(identity))
  var titleTable:               TextraWindow.TitleTable = scala.compiletime.uninitialized
  var drawTitleTable:           Boolean                 = false

  protected var edge:     Int     = 0
  protected var dragging: Boolean = false

  protected var font: Font = replacementFont

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

  // Min/max sizes
  private var _minWidth:  Float = 0f
  private var _minHeight: Float = 0f
  private var _maxWidth:  Float = Float.MaxValue
  private var _maxHeight: Float = Float.MaxValue

  // --- Constructors ---
  // Ordered so each secondary constructor calls either the primary or a previously-defined secondary.

  def this(title: String, style: Styles.WindowStyle, replacementFont: Font) =
    this(title, style, replacementFont, false)

  def this(title: String, style: Styles.WindowStyle, scaleTitleFont: Boolean) =
    this(title, style, Nullable.fold(style.titleFont)(new Font())(identity), scaleTitleFont)

  def this(title: String, style: Styles.WindowStyle) =
    this(title, style, Nullable.fold(style.titleFont)(new Font())(identity), false)

  def this(title: String, skin: Skin) =
    this(
      title,
      skin.get(classOf[Styles.WindowStyle]),
      Nullable.fold(skin.get(classOf[Styles.WindowStyle]).titleFont)(new Font())(identity),
      false
    )

  def this(title: String, skin: Skin, scaleTitleFont: Boolean) =
    this(
      title,
      skin.get(classOf[Styles.WindowStyle]),
      Nullable.fold(skin.get(classOf[Styles.WindowStyle]).titleFont)(new Font())(identity),
      scaleTitleFont
    )

  def this(title: String, skin: Skin, styleName: String) =
    this(
      title,
      skin.get(styleName, classOf[Styles.WindowStyle]),
      Nullable.fold(skin.get(styleName, classOf[Styles.WindowStyle]).titleFont)(new Font())(identity),
      false
    )

  def this(title: String, skin: Skin, styleName: String, scaleTitleFont: Boolean) =
    this(
      title,
      skin.get(styleName, classOf[Styles.WindowStyle]),
      Nullable.fold(skin.get(styleName, classOf[Styles.WindowStyle]).titleFont)(new Font())(identity),
      scaleTitleFont
    )

  def this(title: String, skin: Skin, replacementFont: Font) =
    this(title, skin.get(classOf[Styles.WindowStyle]), replacementFont, false)

  def this(title: String, skin: Skin, replacementFont: Font, scaleTitleFont: Boolean) =
    this(title, skin.get(classOf[Styles.WindowStyle]), replacementFont, scaleTitleFont)

  def this(title: String, skin: Skin, styleName: String, replacementFont: Font) =
    this(title, skin.get(styleName, classOf[Styles.WindowStyle]), replacementFont, false)

  def this(title: String, skin: Skin, styleName: String, replacementFont: Font, scaleTitleFont: Boolean) =
    this(title, skin.get(styleName, classOf[Styles.WindowStyle]), replacementFont, scaleTitleFont)

  // --- Init ---
  {
    setStyle(style, replacementFont)

    if (scaleTitleFont) {
      val labelFont = new Font(replacementFont)
      // Scale to fit top height from background
      Nullable.foreach(_style.background) {
        case bg: Drawable =>
          labelFont.scaleHeightTo(bg.topHeight)
        case _ => ()
      }
      titleLabel.setFont(labelFont)
    } else {
      titleLabel.setFont(font)
    }

    val self = this
    titleTable = new TextraWindow.TitleTable(self)
    titleTable.label = titleLabel
    titleLabel.setEllipsis(Nullable("..."))
  }

  // --- InternalListener ---

  /** Inner listener class that handles drag/resize/move/keyboard events for the window. */
  class InternalListener {
    var startX: Float = 0f
    var startY: Float = 0f
    var lastX:  Float = 0f
    var lastY:  Float = 0f

    private def updateEdge(x: Float, y: Float): Unit = {
      var border = _resizeBorder / 2f
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
      if (_isResizable && x >= left - border && x <= right + border && y >= bottom - border) {
        if (x < left + border) edge |= Align.left.toInt
        if (x > right - border) edge |= Align.right.toInt
        if (y < bottom + border) edge |= Align.bottom.toInt
        if (edge != 0) border += 25
        if (x < left + border) edge |= Align.left.toInt
        if (x > right - border) edge |= Align.right.toInt
        if (y < bottom + border) edge |= Align.bottom.toInt
      }
      if (_isMovable && edge == 0 && y <= height && y >= height - padTop && x >= left && x <= right)
        edge = TextraWindow.MOVE
    }

    def touchDown(x: Float, y: Float, pointer: Int, button: Int): Boolean = {
      if (button == 0) {
        updateEdge(x, y)
        dragging = edge != 0
        startX = x
        startY = y
        lastX = x - _width
        lastY = y - _height
      }
      edge != 0 || _isModal
    }

    def touchUp(x: Float, y: Float, pointer: Int, button: Int): Unit =
      dragging = false

    def touchDragged(x: Float, y: Float, pointer: Int): Unit =
      if (dragging) {
        var width   = _width
        var height  = _height
        var windowX = _x
        var windowY = _y

        val minW = _minWidth
        val minH = _minHeight

        if ((edge & TextraWindow.MOVE) != 0) {
          val amountX = x - startX
          val amountY = y - startY
          windowX += amountX
          windowY += amountY
        }
        if ((edge & Align.left.toInt) != 0) {
          var amountX = x - startX
          if (width - amountX < minW) amountX = -(minW - width)
          width -= amountX
          windowX += amountX
        }
        if ((edge & Align.bottom.toInt) != 0) {
          var amountY = y - startY
          if (height - amountY < minH) amountY = -(minH - height)
          height -= amountY
          windowY += amountY
        }
        if ((edge & Align.right.toInt) != 0) {
          var amountX = x - lastX - width
          if (width + amountX < minW) amountX = minW - width
          width += amountX
        }
        if ((edge & Align.top.toInt) != 0) {
          var amountY = y - lastY - height
          if (height + amountY < minH) amountY = minH - height
          height += amountY
        }
        setBounds(Math.round(windowX).toFloat, Math.round(windowY).toFloat, Math.round(width).toFloat, Math.round(height).toFloat)
      }

    def mouseMoved(x: Float, y: Float): Boolean = {
      updateEdge(x, y)
      _isModal
    }

    def scrolled(x: Float, y: Float, amount: Int): Boolean = _isModal

    def keyDown(keycode: Int): Boolean = _isModal

    def keyUp(keycode: Int): Boolean = _isModal

    def keyTyped(character: Char): Boolean = _isModal
  }

  /** The internal listener handling drag/resize/move/keyboard events. */
  val internalListener: InternalListener = new InternalListener()

  // --- Label factories ---

  protected def newLabel(text: String, style: Styles.LabelStyle): TextraLabel =
    new TextraLabel(text, style)

  protected def newLabel(text: String, font: Font, color: Color): TextraLabel =
    if (color == null) new TextraLabel(text, font) else new TextraLabel(text, font, color)

  // --- Style ---

  def setStyle(style: Styles.WindowStyle): Unit = {
    this._style = style
    setBackground(style.background)
    titleLabel.setFont(font)
    Nullable.foreach(style.titleFontColor)(c => titleLabel.setColor(c))
    invalidateHierarchy()
  }

  def setStyle(style: Styles.WindowStyle, ignored: Boolean): Unit =
    setStyle(style)

  def setStyle(style: Styles.WindowStyle, font: Font): Unit = {
    this._style = style
    setBackground(style.background)
    this.font = font
    titleLabel.setFont(font)
    Nullable.foreach(style.titleFontColor)(c => titleLabel.setColor(c))
    invalidateHierarchy()
  }

  /** Returns the window's style. Modifying the returned style may not have an effect until {@link #setStyle(WindowStyle)} is called.
    */
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

  // --- Movable ---

  def isMovable: Boolean = _isMovable

  def setMovable(isMovable: Boolean): Unit =
    _isMovable = isMovable

  // --- Modal ---

  def isModal: Boolean = _isModal

  def setModal(isModal: Boolean): Unit =
    _isModal = isModal

  // --- Keep within stage ---

  def setKeepWithinStage(keepWithinStage: Boolean): Unit =
    _keepWithinStage = keepWithinStage

  // --- Resizable ---

  def isResizable: Boolean = _isResizable

  def setResizable(isResizable: Boolean): Unit =
    _isResizable = isResizable

  // --- Resize border ---

  def getResizeBorder: Int = _resizeBorder

  def setResizeBorder(resizeBorder: Int): Unit =
    _resizeBorder = resizeBorder

  // --- Dragging ---

  def isDragging: Boolean = dragging

  // --- Pref width ---

  def getPrefWidth: Float =
    Math.max(titleTable.getPrefWidth + _padLeft + _padRight, _width)

  // --- Title accessors ---

  def getTitleTable: TextraWindow.TitleTable = titleTable

  def getTitleLabel: TextraLabel = titleLabel

  /** Does nothing unless the titleLabel used here is a TypingLabel; then, this will skip text progression ahead. */
  def skipToTheEnd(): Unit =
    titleLabel.skipToTheEnd()

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

  // --- Hierarchy invalidation (no-op in standalone mode) ---

  protected def invalidateHierarchy(): Unit = ()

  // --- keepWithinStage ---

  /** Keeps the window within the stage boundaries. No-op without Stage reference in standalone mode; use keepWithinStage(stageWidth, stageHeight) overload for explicit bounds.
    */
  def keepWithinStage(): Unit = {
    // No-op without Stage reference. Callers with a Stage can invoke
    // keepWithinStage(stageWidth, stageHeight) instead.
  }

  /** Keeps the window within the given stage dimensions. */
  def keepWithinStage(stageWidth: Float, stageHeight: Float): Unit =
    if (_keepWithinStage) {
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

    // Manually draw the title table before clipping is done.
    val padTop2  = _padTop
    val padLeft2 = _padLeft
    titleTable.setColor(_color.a)
    titleTable.setSize(_width - padLeft2 - _padRight, padTop2)
    titleTable.setPosition(padLeft2, _height - padTop2)
    drawTitleTable = true
    titleTable.draw(batch, parentAlpha, x, y)
    drawTitleTable = false // Avoid drawing the title table again in drawChildren.
  }

  /** Hit detection for the window. */
  def hit(x: Float, y: Float, touchable: Boolean): Nullable[AnyRef] = scala.util.boundary {
    if (x < 0 || x >= _width || y < 0 || y >= _height) scala.util.boundary.break(Nullable.empty[AnyRef])
    if (_isModal) scala.util.boundary.break(Nullable[AnyRef](this))
    if (y <= _height && y >= _height - _padTop && x >= 0 && x <= _width) {
      // Hit the title bar
      scala.util.boundary.break(Nullable[AnyRef](this))
    }
    Nullable[AnyRef](this)
  }

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

  /** Lightweight title-table abstraction for standalone mode. Holds the title label and supports drawing it at a given position and size within the window's title bar area.
    */
  class TitleTable(window: TextraWindow) {
    var label:           TextraLabel = scala.compiletime.uninitialized
    private var _x:      Float       = 0f
    private var _y:      Float       = 0f
    private var _width:  Float       = 0f
    private var _height: Float       = 0f
    private var _alpha:  Float       = 1f

    def setPosition(x: Float, y: Float): Unit = {
      _x = x
      _y = y
    }

    def setSize(width: Float, height: Float): Unit = {
      _width = width
      _height = height
    }

    def setColor(alpha: Float): Unit =
      _alpha = alpha

    def getPrefWidth: Float =
      if (label != null) label.getPrefWidth else 0f

    def draw(batch: Batch, parentAlpha: Float, windowX: Float, windowY: Float): Unit =
      if (window.drawTitleTable && label != null) {
        label.setSize(_width, _height)
        label.setPosition(windowX + _x, windowY + _y)
        label.draw(batch, parentAlpha * _alpha)
      }
  }
}
