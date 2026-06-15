/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/TextraWindow.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: TextraWindow extends sge.scenes.scene2d.ui.Table
 *     (Table extends WidgetGroup extends Actor), so the window joins the scene
 *     graph, carries the real Actor action/listener/stage infrastructure, and
 *     lays out the title bar in a real Cell. titleTable is a real scene2d Table
 *     (anonymous subclass that only draws when drawTitleTable is set, mirroring
 *     upstream); titleLabel is a TextraLabel (Widget). The drag/resize/modal
 *     machinery is installed via addCaptureListener(InputListener)/InternalListener
 *     (InputListener) exactly as upstream. keepWithinStage uses the real Stage.
 *     The textra Styles.WindowStyle background is Nullable[AnyRef] (a Drawable);
 *     it is bridged to the inherited Table.setBackground(Nullable[Drawable]).
 *     Upstream's inherited Java getters (getX/getY/getWidth/getHeight/getColor/
 *     getRight/getTop) are exposed as thin shims over the renamed scene2d
 *     properties so the rest of the textra package keeps compiling.
 *   Convention: Window dragging/resizing/modal behavior fully ported.
 *   Idiom: Nullable[A] for nullable fields; no return statements; boundary/break for early returns.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 517
 * Covenant-baseline-methods: InternalListener,MOVE,TextraWindow,_isModal,_isMovable,_isResizable,_keepWithinStage,_resizeBorder,_style,c,dragging,draw,drawBackground,drawStageBackground,drawTitleTable,edge,font,getBackground,getColor,getHeight,getMaxHeight,getMaxWidth,getMinHeight,getMinWidth,getPrefWidth,getResizeBorder,getRight,getStyle,getTitleLabel,getTitleTable,getTop,getWidth,getX,getY,height,hit,hitResult,isDragging,isModal,isMovable,isResizable,keepWithinStage,keyDown,keyTyped,keyUp,labelFont,lastX,lastY,mouseMoved,newLabel,padLeft2,padTop2,prefWidth,scrolled,self,setBackgroundFromStyle,setColor,setKeepWithinStage,setModal,setMovable,setPadBottom,setPadLeft,setPadRight,setPadTop,setResizable,setResizeBorder,setStyle,skipToTheEnd,startX,startY,this,titleLabel,titleTable,tmpPosition,tmpSize,touchDown,touchDragged,touchUp,updateEdge
 * Covenant-source-reference: com/github/tommyettinger/textra/TextraWindow.java
 * Covenant-verified: 2026-06-15
 *
 * upstream-commit: 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4
 */
package sge
package textra

import sge.graphics.{ Color, OrthographicCamera }
import sge.graphics.g2d.Batch
import sge.math.Vector2
import sge.scenes.scene2d.{ Actor, InputEvent, InputListener, Touchable }
import sge.scenes.scene2d.ui.{ Skin, Table }
import sge.scenes.scene2d.utils.Drawable
import sge.Input.{ Button, Key }
import lowlevel.Nullable
import sge.utils.Align

/** A table that can be dragged and act as a modal window. The top padding is used as the window's title height. <p> The preferred size of a window is the preferred size of the title text and the
  * children as laid out by the table. After adding children to the window, it can be convenient to call {@link #pack()} to size the window to the size of the children.
  *
  * @author
  *   Nathan Sweet
  */
class TextraWindow(title: String, style: Styles.WindowStyle, replacementFont: Font, scaleTitleFont: Boolean)(using Sge) extends Table(Nullable.empty) {

  require(title != null, "title cannot be null.")
  require(replacementFont != null, "replacementFont cannot be null.")

  private var _style:           Styles.WindowStyle = style
  private var _isMovable:       Boolean            = true
  private var _isModal:         Boolean            = false
  private var _isResizable:     Boolean            = false
  private var _resizeBorder:    Int                = 8
  private var _keepWithinStage: Boolean            = true
  var titleLabel:               TextraLabel        = newLabel(title, replacementFont, Nullable.fold(style.titleFontColor)(null: Color)(identity))
  var titleTable:               Table              = scala.compiletime.uninitialized
  var drawTitleTable:           Boolean            = false

  protected var edge:     Int     = 0
  protected var dragging: Boolean = false

  protected var font: Font = replacementFont

  // --- Constructors ---
  // Ordered so each secondary constructor calls either the primary or a previously-defined secondary.

  def this(title: String, style: Styles.WindowStyle, replacementFont: Font)(using Sge) =
    this(title, style, replacementFont, false)

  def this(title: String, style: Styles.WindowStyle, scaleTitleFont: Boolean)(using Sge) =
    this(title, style, Nullable.fold(style.titleFont)(new Font())(identity), scaleTitleFont)

  def this(title: String, style: Styles.WindowStyle)(using Sge) =
    this(title, style, Nullable.fold(style.titleFont)(new Font())(identity), false)

  def this(title: String, skin: Skin)(using Sge) =
    this(
      title,
      skin.get(classOf[Styles.WindowStyle]),
      Nullable.fold(skin.get(classOf[Styles.WindowStyle]).titleFont)(new Font())(identity),
      false
    )

  def this(title: String, skin: Skin, scaleTitleFont: Boolean)(using Sge) =
    this(
      title,
      skin.get(classOf[Styles.WindowStyle]),
      Nullable.fold(skin.get(classOf[Styles.WindowStyle]).titleFont)(new Font())(identity),
      scaleTitleFont
    )

  def this(title: String, skin: Skin, styleName: String)(using Sge) =
    this(
      title,
      skin.get(styleName, classOf[Styles.WindowStyle]),
      Nullable.fold(skin.get(styleName, classOf[Styles.WindowStyle]).titleFont)(new Font())(identity),
      false
    )

  def this(title: String, skin: Skin, styleName: String, scaleTitleFont: Boolean)(using Sge) =
    this(
      title,
      skin.get(styleName, classOf[Styles.WindowStyle]),
      Nullable.fold(skin.get(styleName, classOf[Styles.WindowStyle]).titleFont)(new Font())(identity),
      scaleTitleFont
    )

  def this(title: String, skin: Skin, replacementFont: Font)(using Sge) =
    this(title, skin.get(classOf[Styles.WindowStyle]), replacementFont, false)

  def this(title: String, skin: Skin, replacementFont: Font, scaleTitleFont: Boolean)(using Sge) =
    this(title, skin.get(classOf[Styles.WindowStyle]), replacementFont, scaleTitleFont)

  def this(title: String, skin: Skin, styleName: String, replacementFont: Font)(using Sge) =
    this(title, skin.get(styleName, classOf[Styles.WindowStyle]), replacementFont, false)

  def this(title: String, skin: Skin, styleName: String, replacementFont: Font, scaleTitleFont: Boolean)(using Sge) =
    this(title, skin.get(styleName, classOf[Styles.WindowStyle]), replacementFont, scaleTitleFont)

  // --- Init ---
  // Mirrors upstream ctor (TextraWindow.java:117-155): touchable, clip, title
  // label, style, optional title-font scaling, the real title Table laid out in
  // a Cell, default size, and the drag/resize capture + internal listeners.
  touchable = Touchable.enabled
  setClip(true)

  setStyle(style, replacementFont)

  font = replacementFont
  if (scaleTitleFont) {
    val labelFont = new Font(replacementFont)
    // Scale to fit top height from background
    getBackground.foreach(bg => labelFont.scaleHeightTo(bg.topHeight))
    titleLabel.setFont(labelFont)
  } else {
    titleLabel.setFont(font)
  }

  private val self: TextraWindow = this
  titleTable = new Table(Nullable.empty) {
    override def draw(batch: Batch, parentAlpha: Float): Unit =
      if (self.drawTitleTable) super.draw(batch, parentAlpha)
  }
  titleTable.add(Nullable[Actor](titleLabel)).expandX().fillX().minWidth(0)
  titleLabel.setEllipsis(Nullable("..."))

  addActor(titleTable)

  setWidth(150)
  setHeight(150)

  addCaptureListener(
    new InputListener() {
      override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Button): Boolean = {
        toFront()
        false
      }
    }
  )
  addListener(new InternalListener())

  // --- InternalListener ---

  /** Inner listener class that handles drag/resize/move/keyboard events for the window. */
  class InternalListener extends InputListener {
    var startX: Float = 0f
    var startY: Float = 0f
    var lastX:  Float = 0f
    var lastY:  Float = 0f

    private def updateEdge(x: Float, y: Float): Unit = {
      var border = _resizeBorder / 2f
      val width  = getWidth
      val height = getHeight
      val padTop = getPadTop
      val padLt  = getPadLeft
      val padBot = getPadBottom
      val padRt  = getPadRight
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

    override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Button): Boolean = {
      if (button == Button(0)) {
        updateEdge(x, y)
        dragging = edge != 0
        startX = x
        startY = y
        lastX = x - getWidth
        lastY = y - getHeight
      }
      edge != 0 || _isModal
    }

    override def touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Button): Unit =
      dragging = false

    override def touchDragged(event: InputEvent, x: Float, y: Float, pointer: Int): Unit =
      if (dragging) {
        var width   = getWidth
        var height  = getHeight
        var windowX = getX
        var windowY = getY

        val minW          = getMinWidth
        val minH          = getMinHeight
        val clampPosition = _keepWithinStage && stage.exists(s => parent.exists(_ eq s.root))

        if ((edge & TextraWindow.MOVE) != 0) {
          val amountX = x - startX
          val amountY = y - startY
          windowX += amountX
          windowY += amountY
        }
        if ((edge & Align.left.toInt) != 0) {
          var amountX = x - startX
          if (width - amountX < minW) amountX = -(minW - width)
          if (clampPosition && windowX + amountX < 0) amountX = -windowX
          width -= amountX
          windowX += amountX
        }
        if ((edge & Align.bottom.toInt) != 0) {
          var amountY = y - startY
          if (height - amountY < minH) amountY = -(minH - height)
          if (clampPosition && windowY + amountY < 0) amountY = -windowY
          height -= amountY
          windowY += amountY
        }
        if ((edge & Align.right.toInt) != 0) {
          var amountX = x - lastX - width
          if (width + amountX < minW) amountX = minW - width
          if (clampPosition) stage.foreach { s =>
            if (windowX + width + amountX > s.width) amountX = s.width - windowX - width
          }
          width += amountX
        }
        if ((edge & Align.top.toInt) != 0) {
          var amountY = y - lastY - height
          if (height + amountY < minH) amountY = minH - height
          if (clampPosition) stage.foreach { s =>
            if (windowY + height + amountY > s.height) amountY = s.height - windowY - height
          }
          height += amountY
        }
        setBounds(Math.round(windowX).toFloat, Math.round(windowY).toFloat, Math.round(width).toFloat, Math.round(height).toFloat)
      }

    override def mouseMoved(event: InputEvent, x: Float, y: Float): Boolean = {
      updateEdge(x, y)
      _isModal
    }

    override def scrolled(event: InputEvent, x: Float, y: Float, amountX: Float, amountY: Float): Boolean = _isModal

    override def keyDown(event: InputEvent, keycode: Key): Boolean = _isModal

    override def keyUp(event: InputEvent, keycode: Key): Boolean = _isModal

    override def keyTyped(event: InputEvent, character: Char): Boolean = _isModal
  }

  // --- Label factories ---

  protected def newLabel(text: String, style: Styles.LabelStyle): TextraLabel =
    new TextraLabel(text, style)

  protected def newLabel(text: String, font: Font, color: Color): TextraLabel =
    if (color == null) new TextraLabel(text, font) else new TextraLabel(text, font, color)

  // --- Style ---

  def setStyle(style: Styles.WindowStyle): Unit = {
    this._style = style
    setBackgroundFromStyle(style.background)
    titleLabel.setFont(font)
    Nullable.foreach(style.titleFontColor)(c => titleLabel.setColor(c))
    invalidateHierarchy()
  }

  def setStyle(style: Styles.WindowStyle, ignored: Boolean): Unit =
    setStyle(style)

  def setStyle(style: Styles.WindowStyle, font: Font): Unit = {
    this._style = style
    setBackgroundFromStyle(style.background)
    this.font = font
    titleLabel.setFont(font)
    Nullable.foreach(style.titleFontColor)(c => titleLabel.setColor(c))
    invalidateHierarchy()
  }

  /** Returns the window's style. Modifying the returned style may not have an effect until {@link #setStyle(WindowStyle)} is called.
    */
  def getStyle: Styles.WindowStyle = _style

  // --- Widget-like accessors (Java-getter shims over the inherited scene2d Actor/Table properties) ---

  def getX:               Float = x
  def getY:               Float = y
  def getWidth:           Float = width
  def getHeight:          Float = height
  def getColor:           Color = color
  def setColor(c: Color): Unit  = if (c != null) color.set(c)

  def getRight: Float = x + width
  def getTop:   Float = y + height

  // --- Padding (Java-getter shims) ---

  def setPadTop(p:    Float): Unit = padTop(p)
  def setPadLeft(p:   Float): Unit = padLeft(p)
  def setPadBottom(p: Float): Unit = padBottom(p)
  def setPadRight(p:  Float): Unit = padRight(p)

  def getMinWidth:  Float = minWidth
  def getMinHeight: Float = minHeight
  def getMaxWidth:  Float = maxWidth
  def getMaxHeight: Float = maxHeight

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

  override def prefWidth: Float =
    Math.max(super.prefWidth, titleTable.prefWidth + getPadLeft + getPadRight)

  /** Returns the preferred width; thin Java-getter accessor over the inherited scene2d prefWidth for the textra package. */
  def getPrefWidth: Float = prefWidth

  // --- Title accessors ---

  def getTitleTable: Table = titleTable

  def getTitleLabel: TextraLabel = titleLabel

  /** Does nothing unless the titleLabel used here is a TypingLabel; then, this will skip text progression ahead. */
  def skipToTheEnd(): Unit =
    titleLabel.skipToTheEnd()

  // --- Background ---

  /** Bridges the textra Styles.WindowStyle background (Nullable[AnyRef] holding a Drawable) to the inherited Table.setBackground(Nullable[Drawable]). */
  private def setBackgroundFromStyle(bg: Nullable[AnyRef]): Unit =
    Nullable.fold(bg) {
      setBackground(Nullable.empty[Drawable])
    } {
      case drawable: Drawable => setBackground(Nullable(drawable))
      case _ => ()
    }

  /** Returns the inherited Table background drawable. */
  def getBackground: Nullable[Drawable] = background

  // --- keepWithinStage ---

  /** Keeps the window within the stage boundaries, using the real Stage's camera. */
  def keepWithinStage(): Unit =
    if (_keepWithinStage) stage.foreach { stage =>
      val camera = stage.camera
      camera match {
        case orthographicCamera: OrthographicCamera =>
          val parentWidth  = stage.width
          val parentHeight = stage.height
          if (getX(Align.right) - camera.position.x > parentWidth / 2 / orthographicCamera.zoom)
            setPosition(camera.position.x + parentWidth / 2 / orthographicCamera.zoom, getY(Align.right), Align.right)
          if (getX(Align.left) - camera.position.x < -parentWidth / 2 / orthographicCamera.zoom)
            setPosition(camera.position.x - parentWidth / 2 / orthographicCamera.zoom, getY(Align.left), Align.left)
          if (getY(Align.top) - camera.position.y > parentHeight / 2 / orthographicCamera.zoom)
            setPosition(getX(Align.top), camera.position.y + parentHeight / 2 / orthographicCamera.zoom, Align.top)
          if (getY(Align.bottom) - camera.position.y < -parentHeight / 2 / orthographicCamera.zoom)
            setPosition(getX(Align.bottom), camera.position.y - parentHeight / 2 / orthographicCamera.zoom, Align.bottom)
        case _ =>
          if (parent.exists(_ eq stage.root)) {
            val parentWidth  = stage.width
            val parentHeight = stage.height
            if (getX < 0) setX(0)
            if (getRight > parentWidth) setX(parentWidth - width)
            if (getY < 0) setY(0)
            if (getTop > parentHeight) setY(parentHeight - height)
          }
      }
    }

  // --- Draw ---

  /** Draws this window, including background, stage background, and title table. */
  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    stage.foreach { stage =>
      if (stage.keyboardFocus.isEmpty) stage.setKeyboardFocus(Nullable(this))

      keepWithinStage()

      Nullable.foreach(_style.stageBackground) {
        case _: Drawable =>
          stageToLocalCoordinates(TextraWindow.tmpPosition.set(0, 0))
          stageToLocalCoordinates(TextraWindow.tmpSize.set(stage.width, stage.height))
          drawStageBackground(
            batch,
            parentAlpha,
            getX + TextraWindow.tmpPosition.x,
            getY + TextraWindow.tmpPosition.y,
            getX + TextraWindow.tmpSize.x,
            getY + TextraWindow.tmpSize.y
          )
        case _ => ()
      }
    }
    super.draw(batch, parentAlpha)
  }

  /** Draws the stage background (dimming behind a modal window). */
  protected def drawStageBackground(batch: Batch, parentAlpha: Float, x: Float, y: Float, width: Float, height: Float): Unit = {
    val c = getColor
    batch.setColor(c.r, c.g, c.b, c.a * parentAlpha)
    Nullable.foreach(_style.stageBackground) {
      case bg: Drawable => bg.draw(batch, x, y, width, height)
      case _ => ()
    }
  }

  /** Draws the window background and the title table. */
  override protected def drawBackground(batch: Batch, parentAlpha: Float, x: Float, y: Float): Unit = {
    super.drawBackground(batch, parentAlpha, x, y)

    // Manually draw the title table before clipping is done.
    titleTable.color.a = getColor.a
    val padTop2  = getPadTop
    val padLeft2 = getPadLeft
    titleTable.setSize(getWidth - padLeft2 - getPadRight, padTop2)
    titleTable.setPosition(padLeft2, getHeight - padTop2)
    drawTitleTable = true
    titleTable.draw(batch, parentAlpha)
    drawTitleTable = false // Avoid drawing the title table again in drawChildren.
  }

  /** Hit detection for the window. */
  override def hit(x: Float, y: Float, touchable: Boolean): Nullable[Actor] = scala.util.boundary {
    if (!visible) scala.util.boundary.break(Nullable.empty)
    val hitResult = super.hit(x, y, touchable)
    if (hitResult.isEmpty && _isModal && (!touchable || this.touchable == Touchable.enabled)) scala.util.boundary.break(Nullable(this: Actor))
    val height = getHeight
    if (hitResult.isEmpty || hitResult.exists(_ eq this)) scala.util.boundary.break(hitResult)
    if (y <= height && y >= height - getPadTop && x >= 0 && x <= getWidth) {
      // Hit the title bar, don't use the hit child if it is in the TextraWindow's table.
      hitResult.foreach { hr =>
        var current: Actor = hr
        while (!current.parent.exists(_ eq this))
          current.parent.foreach { p => current = p }
        if (getCell(current).isDefined) scala.util.boundary.break(Nullable(this: Actor))
      }
    }
    hitResult
  }
}

object TextraWindow {
  private val tmpPosition: Vector2 = Vector2()
  private val tmpSize:     Vector2 = Vector2()
  val MOVE:                Int     = 1 << 5
}
