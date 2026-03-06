/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/ui/Window.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: keepWithinStage() method -> keepWithinStageMethod() (avoids collision with keepWithinStage Boolean field)
 *   Convention: null -> Nullable; (using Sge) context; boundary/break; Skin constructors present
 *   Idiom: split packages
 *   TODO: Java-style getters/setters — setMovable, setModal, setKeepWithinStage, setResizable, isDragging, getTitleTable, getTitleLabel, getStyle/setStyle
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package scenes
package scene2d
package ui

import sge.graphics.{ Color, OrthographicCamera }
import sge.graphics.g2d.{ Batch, BitmapFont }
import sge.math.Vector2
import sge.scenes.scene2d.{ Actor, InputEvent, InputListener, Touchable }
import sge.scenes.scene2d.ui.Label.LabelStyle
import sge.scenes.scene2d.utils.Drawable
import sge.utils.{ Align, Nullable }

/** A table that can be dragged and act as a modal window. The top padding is used as the window's title height. <p> The preferred size of a window is the preferred size of the title text and the
  * children as laid out by the table. After adding children to the window, it can be convenient to call {@link #pack()} to size the window to the size of the children.
  * @author
  *   Nathan Sweet
  */
class Window(title: String, style: Window.WindowStyle)(using Sge) extends Table(Nullable.empty) with Styleable[Window.WindowStyle] {
  import Window._

  private var _style:  WindowStyle = scala.compiletime.uninitialized
  var isMovable:       Boolean     = true
  var isModal:         Boolean     = false
  var isResizable:     Boolean     = false
  var resizeBorder:    Int         = 8
  var keepWithinStage: Boolean     = true
  var titleLabel:      Label       = scala.compiletime.uninitialized
  var titleTable:      Table       = scala.compiletime.uninitialized
  var drawTitleTable:  Boolean     = false

  protected var edge:     Int     = 0
  protected var dragging: Boolean = false

  setTouchable(Touchable.enabled)
  setClip(true)

  titleLabel = newLabel(title, LabelStyle(style.titleFont, style.titleFontColor))
  titleLabel.setEllipsis(true)

  val self = this
  titleTable = new Table(Nullable.empty) {
    override def draw(batch: Batch, parentAlpha: Float): Unit =
      if (self.drawTitleTable) super.draw(batch, parentAlpha)
  }
  titleTable.add(Nullable[Actor](titleLabel)).growX().minWidth(0)
  addActor(titleTable)

  setStyle(style)
  setWidth(150)
  setHeight(150)

  addCaptureListener(
    new InputListener() {
      override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean = {
        toFront()
        false
      }
    }
  )

  addListener(
    new InputListener() {
      var startX: Float = 0f
      var startY: Float = 0f
      var lastX:  Float = 0f
      var lastY:  Float = 0f

      private def updateEdge(x: Float, y: Float): Unit = {
        var border    = self.resizeBorder / 2f
        val width     = getWidth
        val height    = getHeight
        val padTop    = getPadTop
        val padLeft   = getPadLeft
        val padBottom = getPadBottom
        val padRight  = getPadRight
        val left      = padLeft
        val right     = width - padRight
        val bottom    = padBottom
        self.edge = 0
        if (self.isResizable && x >= left - border && x <= right + border && y >= bottom - border) {
          if (x < left + border) self.edge |= Align.left.toInt
          if (x > right - border) self.edge |= Align.right.toInt
          if (y < bottom + border) self.edge |= Align.bottom.toInt
          if (self.edge != 0) border += 25
          if (x < left + border) self.edge |= Align.left.toInt
          if (x > right - border) self.edge |= Align.right.toInt
          if (y < bottom + border) self.edge |= Align.bottom.toInt
        }
        if (self.isMovable && self.edge == 0 && y <= height && y >= height - padTop && x >= left && x <= right)
          self.edge = MOVE
      }

      override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean = {
        if (button == 0) {
          updateEdge(x, y)
          self.dragging = self.edge != 0
          startX = x
          startY = y
          lastX = x - getWidth
          lastY = y - getHeight
        }
        self.edge != 0 || self.isModal
      }

      override def touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Unit =
        self.dragging = false

      override def touchDragged(event: InputEvent, x: Float, y: Float, pointer: Int): Unit =
        if (!self.dragging) ()
        else {
          var width   = getWidth
          var height  = getHeight
          var windowX = getX
          var windowY = getY

          val minWidth      = getMinWidth
          val minHeight     = getMinHeight
          val stage         = getStage
          val clampPosition = self.keepWithinStage && stage.exists(s => getParent.exists(_ eq s.getRoot))

          if ((self.edge & MOVE) != 0) {
            val amountX = x - startX
            val amountY = y - startY
            windowX += amountX
            windowY += amountY
          }
          if ((self.edge & Align.left.toInt) != 0) {
            var amountX = x - startX
            if (width - amountX < minWidth) amountX = -(minWidth - width)
            if (clampPosition && windowX + amountX < 0) amountX = -windowX
            width -= amountX
            windowX += amountX
          }
          if ((self.edge & Align.bottom.toInt) != 0) {
            var amountY = y - startY
            if (height - amountY < minHeight) amountY = -(minHeight - height)
            if (clampPosition && windowY + amountY < 0) amountY = -windowY
            height -= amountY
            windowY += amountY
          }
          if ((self.edge & Align.right.toInt) != 0) {
            var amountX = x - lastX - width
            if (width + amountX < minWidth) amountX = minWidth - width
            if (clampPosition) stage.foreach { s =>
              if (windowX + width + amountX > s.getWidth) amountX = s.getWidth - windowX - width
            }
            width += amountX
          }
          if ((self.edge & Align.top.toInt) != 0) {
            var amountY = y - lastY - height
            if (height + amountY < minHeight) amountY = minHeight - height
            if (clampPosition) stage.foreach { s =>
              if (windowY + height + amountY > s.getHeight) amountY = s.getHeight - windowY - height
            }
            height += amountY
          }
          setBounds(Math.round(windowX).toFloat, Math.round(windowY).toFloat, Math.round(width).toFloat, Math.round(height).toFloat)
        }

      override def mouseMoved(event: InputEvent, x: Float, y: Float): Boolean = {
        updateEdge(x, y)
        self.isModal
      }

      override def scrolled(event: InputEvent, x: Float, y: Float, amountX: Float, amountY: Float): Boolean =
        self.isModal

      override def keyDown(event: InputEvent, keycode: Int): Boolean =
        self.isModal

      override def keyUp(event: InputEvent, keycode: Int): Boolean =
        self.isModal

      override def keyTyped(event: InputEvent, character: Char): Boolean =
        self.isModal
    }
  )

  def this(title: String, skin: Skin)(using Sge) =
    this(title, skin.get(classOf[Window.WindowStyle]))

  def this(title: String, skin: Skin, styleName: String)(using Sge) =
    this(title, skin.get(styleName, classOf[Window.WindowStyle]))

  protected def newLabel(text: String, style: LabelStyle): Label =
    Label(Nullable(text), style)

  override def setStyle(style: WindowStyle): Unit = {
    this._style = style

    setBackground(Nullable(style.background))
    titleLabel.setStyle(LabelStyle(style.titleFont, style.titleFontColor))
    invalidateHierarchy()
  }

  /** Returns the window's style. Modifying the returned style may not have an effect until {@link #setStyle(WindowStyle)} is called.
    */
  override def getStyle: WindowStyle = _style

  def keepWithinStageMethod(): Unit =
    if (keepWithinStage) getStage.foreach { stage =>
      val camera = stage.getCamera
      camera match {
        case orthographicCamera: OrthographicCamera =>
          val parentWidth  = stage.getWidth
          val parentHeight = stage.getHeight
          if (getX(Align.right) - camera.position.x > parentWidth / 2 / orthographicCamera.zoom)
            setPosition(camera.position.x + parentWidth / 2 / orthographicCamera.zoom, getY(Align.right), Align.right)
          if (getX(Align.left) - camera.position.x < -parentWidth / 2 / orthographicCamera.zoom)
            setPosition(camera.position.x - parentWidth / 2 / orthographicCamera.zoom, getY(Align.left), Align.left)
          if (getY(Align.top) - camera.position.y > parentHeight / 2 / orthographicCamera.zoom)
            setPosition(getX(Align.top), camera.position.y + parentHeight / 2 / orthographicCamera.zoom, Align.top)
          if (getY(Align.bottom) - camera.position.y < -parentHeight / 2 / orthographicCamera.zoom)
            setPosition(getX(Align.bottom), camera.position.y - parentHeight / 2 / orthographicCamera.zoom, Align.bottom)
        case _ =>
          if (getParent.exists(_ eq stage.getRoot)) {
            val parentWidth  = stage.getWidth
            val parentHeight = stage.getHeight
            if (getX < 0) setX(0)
            if (getRight > parentWidth) setX(parentWidth - getWidth)
            if (getY < 0) setY(0)
            if (getTop > parentHeight) setY(parentHeight - getHeight)
          }
      }
    }

  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    getStage.foreach { stage =>
      if (stage.getKeyboardFocus.isEmpty) stage.setKeyboardFocus(Nullable(this))

      keepWithinStageMethod()

      _style.stageBackground.foreach { stageBackground =>
        stageToLocalCoordinates(tmpPosition.set(0, 0))
        stageToLocalCoordinates(tmpSize.set(stage.getWidth, stage.getHeight))
        drawStageBackground(batch, parentAlpha, getX + tmpPosition.x, getY + tmpPosition.y, getX + tmpSize.x, getY + tmpSize.y)
      }
    }
    super.draw(batch, parentAlpha)
  }

  protected def drawStageBackground(batch: Batch, parentAlpha: Float, x: Float, y: Float, width: Float, height: Float): Unit = {
    val color = getColor
    batch.setColor(color.r, color.g, color.b, color.a * parentAlpha)
    _style.stageBackground.foreach(_.draw(batch, x, y, width, height))
  }

  override protected def drawBackground(batch: Batch, parentAlpha: Float, x: Float, y: Float): Unit = {
    super.drawBackground(batch, parentAlpha, x, y)

    // Manually draw the title table before clipping is done.
    titleTable.getColor.a = getColor.a
    val padTop  = getPadTop
    val padLeft = getPadLeft
    titleTable.setSize(getWidth - padLeft - getPadRight, padTop)
    titleTable.setPosition(padLeft, getHeight - padTop)
    drawTitleTable = true
    titleTable.draw(batch, parentAlpha)
    drawTitleTable = false // Avoid drawing the title table again in drawChildren.
  }

  override def hit(x: Float, y: Float, touchable: Boolean): Nullable[Actor] = scala.util.boundary {
    if (!isVisible) scala.util.boundary.break(Nullable.empty)
    val hitResult = super.hit(x, y, touchable)
    if (hitResult.isEmpty && isModal && (!touchable || getTouchable == Touchable.enabled)) scala.util.boundary.break(Nullable(this: Actor))
    val height = getHeight
    if (hitResult.isEmpty || hitResult.exists(_ eq this)) scala.util.boundary.break(hitResult)
    if (y <= height && y >= height - getPadTop && x >= 0 && x <= getWidth) {
      // Hit the title bar, don't use the hit child if it is in the Window's table.
      hitResult.foreach { hr =>
        var current: Actor = hr
        while (!current.getParent.exists(_ eq this))
          current.getParent.foreach { p => current = p }
        if (getCell(current).isDefined) scala.util.boundary.break(Nullable(this: Actor))
      }
    }
    hitResult
  }

  def setMovable(isMovable:               Boolean): Unit    = this.isMovable = isMovable
  def setModal(isModal:                   Boolean): Unit    = this.isModal = isModal
  def setKeepWithinStage(keepWithinStage: Boolean): Unit    = this.keepWithinStage = keepWithinStage
  def setResizable(isResizable:           Boolean): Unit    = this.isResizable = isResizable
  def setResizeBorder(resizeBorder:       Int):     Unit    = this.resizeBorder = resizeBorder
  def isDragging:                                   Boolean = dragging

  override def getPrefWidth: Float =
    Math.max(super.getPrefWidth, titleTable.getPrefWidth + getPadLeft + getPadRight)

  def getTitleTable: Table = titleTable
  def getTitleLabel: Label = titleLabel
}

object Window {
  private val tmpPosition: Vector2 = Vector2()
  private val tmpSize:     Vector2 = Vector2()
  private val MOVE:        Int     = 1 << 5

  /** The style for a window, see {@link Window}.
    * @author
    *   Nathan Sweet
    */
  class WindowStyle() {
    var background:      Drawable           = scala.compiletime.uninitialized
    var titleFont:       BitmapFont         = scala.compiletime.uninitialized
    var titleFontColor:  Nullable[Color]    = Nullable(Color(1, 1, 1, 1))
    var stageBackground: Nullable[Drawable] = Nullable.empty

    def this(titleFont: BitmapFont, titleFontColor: Color, background: Drawable) = {
      this()
      this.titleFont = titleFont
      this.titleFontColor.foreach(_.set(titleFontColor))
      this.background = background
    }

    def this(style: WindowStyle) = {
      this()
      titleFont = style.titleFont
      titleFontColor = style.titleFontColor.map(c => Color(c))
      background = style.background
      stageBackground = style.stageBackground
    }
  }
}
