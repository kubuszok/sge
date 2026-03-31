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
import scala.util.boundary
import scala.util.boundary.break

import sge.Input.{ Key, Keys }
import sge.math.Vector2
import sge.graphics.g2d.Batch
import sge.scenes.scene2d.{ Actor, InputEvent, InputListener, Stage, Touchable }
import sge.scenes.scene2d.ui.{ Cell, Table }
import sge.scenes.scene2d.utils.{ ChangeListener, Drawable }
import sge.utils.Nullable
import sge.visui.{ Sizes, VisUI }
import sge.visui.util.ActorUtils

/** Standard popup menu that can be displayed anywhere on stage. Menu is automatically removed when user clicked outside menu, or clicked menu item. For proper behaviour menu should be displayed in
  * touchUp event. If you want to display menu from touchDown you have to call event.stop() otherwise menu will by immediately closed.
  *
  * Since 1.0.2 arrow keys can be used to navigate menu hierarchy.
  * @author
  *   Kotcrab
  */
class PopupMenu(sizes: Sizes, val style: PopupMenu.PopupMenuStyle)(using Sge) extends Table(Nullable(VisUI.getSkin)) {

  private var _listener:            Nullable[PopupMenu.PopupMenuListener] = Nullable.empty
  private var stageListener:        InputListener                         = scala.compiletime.uninitialized
  private var defaultInputListener: Nullable[InputListener]               = Nullable.empty

  private var sharedMenuItemInputListener:  InputListener  = scala.compiletime.uninitialized
  private var sharedMenuItemChangeListener: ChangeListener = scala.compiletime.uninitialized

  /** The parent sub-menu, that this popup menu belongs to or null if this sub menu is root */
  private[widget] var parentSubMenu: Nullable[PopupMenu] = Nullable.empty

  /** The current sub-menu, set by MenuItem */
  private[widget] var activeSubMenu: Nullable[PopupMenu] = Nullable.empty

  private var _activeItem: Nullable[MenuItem] = Nullable.empty

  touchable = Touchable.enabled
  pad(0)
  setBackground(Nullable(style.background))
  createListeners()

  def this()(using Sge) = this(VisUI.getSizes, VisUI.getSkin.get[PopupMenu.PopupMenuStyle])

  def this(styleName: String)(using Sge) = this(VisUI.getSizes, VisUI.getSkin.get[PopupMenu.PopupMenuStyle](styleName))

  def this(style: PopupMenu.PopupMenuStyle)(using Sge) = this(VisUI.getSizes, style)

  private def createListeners(): Unit = {
    stageListener = new InputListener() {
      override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: sge.Input.Button): Boolean = {
        if (!getRootMenu.subMenuStructureContains(x, y)) {
          remove()
        }
        true
      }

      override def keyDown(event: InputEvent, keycode: Key): Boolean = {
        val ch = PopupMenu.this.children
        if (ch.size == 0 || activeSubMenu.isDefined) false
        else if (keycode == Keys.DOWN) {
          selectNextItem()
          true
        } else if (keycode == Keys.UP) {
          selectPreviousItem()
          true
        } else if (_activeItem.isEmpty) {
          false
        } else if (keycode == Keys.LEFT && _activeItem.get.containerMenu.isDefined && _activeItem.get.containerMenu.get.parentSubMenu.isDefined) {
          _activeItem.get.containerMenu.get.parentSubMenu.get.setActiveSubMenu(Nullable.empty)
          true
        } else if (keycode == Keys.RIGHT && _activeItem.get.subMenu.isDefined) {
          _activeItem.get.showSubMenu()
          activeSubMenu.foreach(_.selectNextItem())
          true
        } else if (keycode == Keys.ENTER) {
          _activeItem.get.fireChangeEvent()
          true
        } else {
          false
        }
      }
    }

    sharedMenuItemInputListener = new InputListener() {
      override def enter(event: InputEvent, x: Float, y: Float, pointer: Int, fromActor: Nullable[Actor]): Unit =
        if (pointer == -1) {
          event.listenerActor.foreach {
            case item: MenuItem if !item.disabled =>
              setActiveItem(Nullable(item), changedByKeyboard = false)
            case _ => ()
          }
        }

      override def exit(event: InputEvent, x: Float, y: Float, pointer: Int, toActor: Nullable[Actor]): Unit =
        if (pointer == -1) {
          event.listenerActor.foreach {
            case item: MenuItem =>
              if (activeSubMenu.isEmpty && _activeItem.isDefined && _activeItem.get == item) {
                setActiveItem(Nullable.empty, changedByKeyboard = false)
              }
            case _ => ()
          }
        }
    }

    sharedMenuItemChangeListener = new ChangeListener() {
      override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit =
        if (!event.isStopped) removeHierarchy()
    }
  }

  private def getRootMenu: PopupMenu =
    if (parentSubMenu.isDefined) parentSubMenu.get.getRootMenu
    else this

  private def subMenuStructureContains(x: Float, y: Float): Boolean =
    if (contains(x, y)) true
    else if (activeSubMenu.isDefined) activeSubMenu.get.subMenuStructureContains(x, y)
    else false

  private[widget] def removeHierarchy(): Unit = {
    if (_activeItem.isDefined && _activeItem.get.containerMenu.isDefined && _activeItem.get.containerMenu.get.parentSubMenu.isDefined) {
      _activeItem.get.containerMenu.get.parentSubMenu.get.removeHierarchy()
    }
    remove()
  }

  private def selectNextItem(): Unit = {
    val ch = PopupMenu.this.children
    if (!hasSelectableMenuItems) ()
    else {
      val startIndex = if (_activeItem.isEmpty) 0 else ch.indexOf(_activeItem.get) + 1
      boundary {
        var i = startIndex
        while (true) {
          if (i >= ch.size) i = 0
          ch(i) match {
            case item: MenuItem if !item.disabled =>
              setActiveItem(Nullable(item), changedByKeyboard = true)
              break(())
            case _ => ()
          }
          i += 1
        }
      }
    }
  }

  private def selectPreviousItem(): Unit = {
    val ch = PopupMenu.this.children
    if (!hasSelectableMenuItems) ()
    else {
      val startIndex = if (_activeItem.isEmpty) ch.size - 1 else ch.indexOf(_activeItem.get) - 1
      boundary {
        var i = startIndex
        while (true) {
          if (i <= -1) i = ch.size - 1
          ch(i) match {
            case item: MenuItem if !item.disabled =>
              setActiveItem(Nullable(item), changedByKeyboard = true)
              break(())
            case _ => ()
          }
          i -= 1
        }
      }
    }
  }

  private def hasSelectableMenuItems: Boolean = {
    val ch    = PopupMenu.this.children
    var i     = 0
    var found = false
    while (i < ch.size && !found) {
      ch(i) match {
        case item: MenuItem if !item.disabled => found = true
        case _ => ()
      }
      i += 1
    }
    found
  }

  override def add[T <: Actor](actor: Nullable[T]): Cell[T] = {
    actor.foreach {
      case _: MenuItem => throw new IllegalArgumentException("MenuItems can be only added to PopupMenu by using addItem(MenuItem) method")
      case _ => ()
    }
    super.add(actor)
  }

  def addItem(item: MenuItem): Unit = {
    super.add(Nullable[Actor](item)).fillX().expandX().row()
    pack()
    item.addListener(sharedMenuItemChangeListener)
    item.addListener(sharedMenuItemInputListener)
  }

  def addSeparator(): Unit =
    super.add(Nullable[Actor](new Separator("menu"))).padTop(2).padBottom(2).fill().expand().row()

  /** Returns input listener that can be added to scene2d actor. When right mouse button is pressed on that actor, menu will be displayed
    */
  def getDefaultInputListener: InputListener = getDefaultInputListener(sge.Input.Buttons.RIGHT)

  def getDefaultInputListener(mouseButton: sge.Input.Button): InputListener = {
    if (defaultInputListener.isEmpty) {
      defaultInputListener = Nullable(
        new InputListener() {
          override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: sge.Input.Button): Boolean = true

          override def touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: sge.Input.Button): Unit =
            if (event.button == mouseButton) {
              showMenu(event.stage.get, event.stageX, event.stageY)
            }
        }
      )
    }
    defaultInputListener.get
  }

  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    super.draw(batch, parentAlpha)
    if (style.border.isDefined) style.border.get.draw(batch, x, y, width, height)
  }

  /** Shows menu as given stage coordinates */
  def showMenu(stage: Stage, x: Float, y: Float): Unit = {
    setPosition(x, y - height)
    if (stage.height - this.y > stage.height) this.y = this.y + height
    ActorUtils.keepWithinStage(stage, this)
    stage.addActor(this)
  }

  /** Shows menu below (or above if not enough space) given actor. */
  def showMenu(stage: Stage, actor: Actor): Unit = {
    val pos   = actor.localToStageCoordinates(new Vector2())
    val menuY =
      if (pos.y - height <= 0) pos.y + actor.height + height - sizes.borderSize
      else pos.y + sizes.borderSize
    showMenu(stage, pos.x, menuY)
  }

  def contains(x: Float, y: Float): Boolean =
    this.x < x && this.x + width > x && this.y < y && this.y + height > y

  /** Called by framework, when PopupMenu is added to MenuItem as submenu */
  private[widget] def setActiveSubMenu(newSubMenu: Nullable[PopupMenu]): Unit =
    if (activeSubMenu == newSubMenu) ()
    else {
      if (activeSubMenu.isDefined) activeSubMenu.get.remove()
      activeSubMenu = newSubMenu
      if (newSubMenu.isDefined) {
        newSubMenu.get.setParentMenu(this)
      }
    }

  def getActiveSubMenu: Nullable[PopupMenu] = activeSubMenu

  override protected[sge] def setStage(stage: Nullable[Stage]): Unit = {
    super.setStage(stage)
    stage.foreach(_.addListener(stageListener))
  }

  override def remove(): Boolean = {
    stage.foreach(_.removeListener(stageListener))
    if (activeSubMenu.isDefined) activeSubMenu.get.remove()
    setActiveItem(Nullable.empty, changedByKeyboard = false)
    parentSubMenu = Nullable.empty
    activeSubMenu = Nullable.empty
    super.remove()
  }

  private[widget] def setActiveItem(newItem: Nullable[MenuItem], changedByKeyboard: Boolean): Unit = {
    _activeItem = newItem
    _listener.foreach(_.activeItemChanged(newItem, changedByKeyboard))
  }

  def getActiveItem: Nullable[MenuItem] = _activeItem

  private[widget] def setParentMenu(parent: PopupMenu): Unit =
    this.parentSubMenu = Nullable(parent)

  def popupMenuListener:                                          Nullable[PopupMenu.PopupMenuListener] = _listener
  def popupMenuListener_=(listener: PopupMenu.PopupMenuListener): Unit                                  = _listener = Nullable(listener)
}

object PopupMenu {

  /** Removes every instance of [[PopupMenu]] from [[Stage]] actors.
    *
    * Generally called from resize to remove menus on resize event.
    */
  def removeEveryMenu(stage: Stage): Unit = {
    val actors = stage.actors
    var i      = 0
    while (i < actors.size) {
      actors(i) match {
        case menu: PopupMenu => menu.removeHierarchy()
        case _ => ()
      }
      i += 1
    }
  }

  /** Listener used to get events from [[PopupMenu]].
    * @since 1.0.2
    */
  trait PopupMenuListener {
    def activeItemChanged(newActiveItem: Nullable[MenuItem], changedByKeyboard: Boolean): Unit
  }

  class PopupMenuStyle {
    var background: Drawable           = scala.compiletime.uninitialized
    var border:     Nullable[Drawable] = Nullable.empty

    def this(background: Drawable, border: Nullable[Drawable]) = {
      this()
      this.background = background
      this.border = border
    }

    def this(style: PopupMenuStyle) = {
      this()
      this.background = style.background
      this.border = style.border
    }
  }
}
