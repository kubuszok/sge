/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package visui
package widget

import scala.language.implicitConversions

import sge.Input.Key
import sge.graphics.Color
import sge.graphics.g2d.Batch
import sge.math.Vector2
import sge.scenes.scene2d.{ Actor, Group, InputEvent, InputListener, Stage }
import sge.scenes.scene2d.ui.{ Button, Cell, Image, Label }
import sge.scenes.scene2d.ui.Label.LabelStyle
import sge.scenes.scene2d.ui.TextButton.TextButtonStyle
import sge.scenes.scene2d.utils.{ ChangeListener, Drawable }
import sge.utils.{ Align, Nullable, Scaling }
import sge.visui.VisUI
import sge.visui.util.OsUtils

/** MenuItem displayed in [[Menu]] and [[PopupMenu]]. MenuItem contains text or text with icon. Best icon size is 22px. MenuItem can also have a hotkey text.
  *
  * When listening for menu item press [[ChangeListener]] should be always preferred (instead of [[ClickListener]]). [[ClickListener]] does not support disabling menu item and will still report item
  * clicks.
  * @author
  *   Kotcrab
  */
class MenuItem private (text: String, initImage: Nullable[Image], initStyle: MenuItem.MenuItemStyle)(using Sge) extends Button(initStyle) {

  private var _style:                 MenuItem.MenuItemStyle = initStyle
  private val _image:                 Nullable[Image]        = initImage
  private var imageCell:              Cell[Image]            = scala.compiletime.uninitialized
  private var _generateDisabledImage: Boolean                = true
  private var _label:                 Label                  = scala.compiletime.uninitialized
  private var shortcutLabelColor:     Nullable[Color]        = Nullable.empty
  private var _shortcutLabel:         VisLabel               = scala.compiletime.uninitialized
  private var subMenuImage:           Image                  = scala.compiletime.uninitialized
  private var subMenuIconCell:        Cell[Image]            = scala.compiletime.uninitialized
  private var _subMenu:               Nullable[PopupMenu]    = Nullable.empty

  /** Menu that this item belongs to */
  private[widget] var containerMenu: Nullable[PopupMenu] = Nullable.empty

  {
    setSkin(VisUI.getSkin)
    val sizes = VisUI.getSizes

    defaults().space(3)

    _image.foreach(_.scaling = Scaling.fit)
    imageCell = add(_image).size(sizes.menuItemIconSize)

    _label = new Label(Nullable(text), new LabelStyle(_style.font, _style.fontColor))
    _label.setAlignment(Align.left)
    add(Nullable[Actor](_label)).expand().fill()

    _shortcutLabel = new VisLabel("", "menuitem-shortcut")
    add(Nullable[Actor](_shortcutLabel)).padLeft(10).right()
    shortcutLabelColor = _shortcutLabel.style.fontColor

    subMenuImage = new Image(Nullable(_style.subMenu))
    subMenuIconCell = add(Nullable(subMenuImage)).padLeft(3).padRight(3).size(_style.subMenu.minWidth, _style.subMenu.minHeight)
    subMenuIconCell.setActor(Nullable.empty)

    addListener(
      new ChangeListener() {
        override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit =
          if (_subMenu.isDefined) { // makes submenu item not clickable
            event.stop()
          }
      }
    )

    addListener(
      new InputListener() {
        override def enter(event: InputEvent, x: Float, y: Float, pointer: Int, fromActor: Nullable[Actor]): Unit = {
          if (_subMenu.isDefined) { // removes selection of child submenu if mouse moved to parent submenu
            _subMenu.get.setActiveItem(Nullable.empty, changedByKeyboard = false)
            _subMenu.get.setActiveSubMenu(Nullable.empty)
          }

          if (_subMenu.isEmpty || disabled) { // hides last visible submenu (if any)
            hideSubMenu()
          } else {
            showSubMenu()
          }
        }
      }
    )
  }

  def this(text: String)(using Sge) = this(text, Nullable.empty, VisUI.getSkin.get[MenuItem.MenuItemStyle])
  def this(text: String, styleName: String)(using Sge) = this(text, Nullable.empty, VisUI.getSkin.get[MenuItem.MenuItemStyle](styleName))
  def this(text: String, changeListener: ChangeListener)(using Sge) = {
    this(text, Nullable.empty, VisUI.getSkin.get[MenuItem.MenuItemStyle])
    addListener(changeListener)
  }
  def this(text: String, drawable: Drawable)(using Sge) = this(text, Nullable(new Image(Nullable(drawable))), VisUI.getSkin.get[MenuItem.MenuItemStyle])
  def this(text: String, drawable: Drawable, changeListener: ChangeListener)(using Sge) = {
    this(text, Nullable(new Image(Nullable(drawable))), VisUI.getSkin.get[MenuItem.MenuItemStyle])
    addListener(changeListener)
  }
  def this(text: String, drawable: Drawable, styleName: String)(using Sge) =
    this(text, Nullable(new Image(Nullable(drawable))), VisUI.getSkin.get[MenuItem.MenuItemStyle](styleName))
  def this(text: String, image: Image)(using Sge) = this(text, Nullable(image), VisUI.getSkin.get[MenuItem.MenuItemStyle])
  def this(text: String, image: Image, changeListener: ChangeListener)(using Sge) = {
    this(text, Nullable(image), VisUI.getSkin.get[MenuItem.MenuItemStyle])
    addListener(changeListener)
  }
  def this(text: String, image: Image, styleName: String)(using Sge) =
    this(text, Nullable(image), VisUI.getSkin.get[MenuItem.MenuItemStyle](styleName))
  def this(text: String, drawable: Drawable, menuItemStyle: MenuItem.MenuItemStyle)(using Sge) =
    this(text, Nullable(new Image(Nullable(drawable))), menuItemStyle)

  def subMenu: Nullable[PopupMenu] = _subMenu

  def subMenu_=(subMenu: Nullable[PopupMenu]): Unit = {
    _subMenu = subMenu
    if (subMenu.isEmpty) subMenuIconCell.setActor(Nullable.empty)
    else subMenuIconCell.setActor(Nullable(subMenuImage))
  }

  private[widget] def packContainerMenu(): Unit =
    containerMenu.foreach(_.pack())

  override protected[sge] def setParent(parent: Nullable[Group]): Unit = {
    super.setParent(parent)
    parent match {
      case pm if pm.isDefined && pm.get.isInstanceOf[PopupMenu] => containerMenu = Nullable(pm.get.asInstanceOf[PopupMenu])
      case _                                                    => containerMenu = Nullable.empty
    }
  }

  private[widget] def hideSubMenu(): Unit =
    containerMenu.foreach(_.setActiveSubMenu(Nullable.empty))

  private[widget] def showSubMenu(): Unit =
    stage.foreach { stg =>
      val pos = localToStageCoordinates(MenuItem.tmpVector.setZero())
      val sm  = _subMenu.get

      val availableSpaceLeft  = pos.x
      val availableSpaceRight = stg.width - (pos.x + width)
      val canFitOnTheRight    = pos.x + width + sm.width <= stg.width
      val subMenuX            =
        if (canFitOnTheRight || availableSpaceRight > availableSpaceLeft) pos.x + width - 1
        else pos.x - sm.width + 1

      containerMenu.foreach { cm =>
        if (cm.getActiveSubMenu.isEmpty || cm.getActiveSubMenu.get != sm) {
          val hasEnoughBottomSpace = stg.height - (pos.y + height) + sm.height <= stg.height
          val heightCorrection     = if (hasEnoughBottomSpace) height else 0

          sm.showMenu(stg, subMenuX, pos.y + heightCorrection)
          cm.setActiveSubMenu(Nullable(sm))
        }
      }
    }

  private[widget] def fireChangeEvent(): Unit = {
    val changeEvent = new ChangeListener.ChangeEvent()
    fire(changeEvent)
  }

  override def style: MenuItem.MenuItemStyle = _style

  override def setStyle(style: Button.ButtonStyle): Unit = {
    if (!style.isInstanceOf[MenuItem.MenuItemStyle]) throw new IllegalArgumentException("style must be a MenuItemStyle.")
    super.setStyle(style)
    _style = style.asInstanceOf[MenuItem.MenuItemStyle]
    if (_label != null) { // @nowarn -- called during super init
      val textButtonStyle = style.asInstanceOf[TextButtonStyle]
      val labelStyle      = new LabelStyle(_label.style)
      labelStyle.font = textButtonStyle.font
      labelStyle.fontColor = textButtonStyle.fontColor
      _label.setStyle(labelStyle)
    }
  }

  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    val fontColor: Nullable[Color] =
      if (disabled && _style.disabledFontColor.isDefined) _style.disabledFontColor
      else if (isPressed && _style.downFontColor.isDefined) _style.downFontColor
      else if (checked && _style.checkedFontColor.isDefined) {
        if (isOver && _style.checkedOverFontColor.isDefined) _style.checkedOverFontColor
        else _style.checkedFontColor
      } else if (isOver && _style.overFontColor.isDefined) _style.overFontColor
      else _style.fontColor

    fontColor.foreach { c =>
      val ls = new LabelStyle(_label.style)
      ls.fontColor = Nullable(c)
      _label.setStyle(ls)
    }

    if (disabled) {
      val ls = new LabelStyle(_shortcutLabel.style)
      ls.fontColor = _style.disabledFontColor
      _shortcutLabel.setStyle(ls)
    } else {
      val ls = new LabelStyle(_shortcutLabel.style)
      ls.fontColor = shortcutLabelColor
      _shortcutLabel.setStyle(ls)
    }

    if (_generateDisabledImage && _image.isDefined) {
      if (disabled) _image.get.color.set(Color.GRAY)
      else _image.get.color.set(Color.WHITE)
    }

    super.draw(batch, parentAlpha)
  }

  override def isOver: Boolean =
    if (containerMenu.isEmpty || containerMenu.get.getActiveItem.isEmpty) super.isOver
    else containerMenu.get.getActiveItem.get == this

  def generateDisabledImage:                   Boolean = _generateDisabledImage
  def generateDisabledImage_=(value: Boolean): Unit    = _generateDisabledImage = value

  def setShortcut(keycode: Key): MenuItem = {
    val keyName: String = OsUtils.getShortcutFor(keycode)
    _shortcutLabel.setText(Nullable[CharSequence](keyName))
    packContainerMenu()
    this
  }

  def getShortcut: String = _shortcutLabel.text.toString

  def setShortcut(text: String): MenuItem = {
    _shortcutLabel.setText(text)
    packContainerMenu()
    this
  }

  def setShortcut(keycodes: Key*): MenuItem = {
    _shortcutLabel.setText(Nullable[CharSequence](OsUtils.getShortcutFor(keycodes*)))
    packContainerMenu()
    this
  }

  override protected[sge] def setStage(stage: Nullable[Stage]): Unit = {
    super.setStage(stage)
    _label.invalidate()
  }

  def getImage:              Nullable[Image]          = _image
  def getImageCell:          Cell[Image]              = imageCell
  def getLabel:              Label                    = _label
  def getLabelCell:          Nullable[Cell[Label]]    = getCell(_label)
  def getText:               String                   = _label.text.toString
  def setText(text: String): Unit                     = _label.setText(text)
  def getSubMenuIconCell:    Cell[Image]              = subMenuIconCell
  def getShortcutCell:       Nullable[Cell[VisLabel]] = getCell(_shortcutLabel)
}

object MenuItem {
  private val tmpVector = new Vector2()

  class MenuItemStyle extends TextButtonStyle {
    var subMenu: Drawable = scala.compiletime.uninitialized

    def this(subMenu: Drawable) = {
      this()
      this.subMenu = subMenu
    }

    def this(style: MenuItemStyle) = {
      this()
      this.font = style.font
      this.fontColor = style.fontColor
      this.up = style.up
      this.down = style.down
      this.over = style.over
      this.checked = style.checked
      this.checkedOver = style.checkedOver
      this.disabled = style.disabled
      this.downFontColor = style.downFontColor
      this.overFontColor = style.overFontColor
      this.checkedFontColor = style.checkedFontColor
      this.checkedOverFontColor = style.checkedOverFontColor
      this.disabledFontColor = style.disabledFontColor
      this.subMenu = style.subMenu
    }
  }
}
