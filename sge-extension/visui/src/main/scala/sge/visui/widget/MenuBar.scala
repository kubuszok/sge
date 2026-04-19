/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 125
 * Covenant-baseline-methods: MenuBar,MenuBarListener,MenuBarStyle,addMenu,background,closeMenu,currentMenu,getCurrentMenu,getTable,i,insertMenu,mainTable,menuClosed,menuItems,menuListener,menuOpened,menus,rebuild,removeMenu,removed,setCurrentMenu,setMenuListener,sizeChanged,this
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/MenuBar.java
 * Covenant-verified: 2026-04-19
 */
package sge
package visui
package widget

import sge.scenes.scene2d.ui.Table
import sge.scenes.scene2d.utils.Drawable
import sge.utils.{ DynamicArray, Nullable }
import sge.visui.VisUI

/** Bar with expandable menus available after pressing button, usually displayed on top of the stage.
  * @author
  *   Kotcrab
  */
class MenuBar(style: MenuBar.MenuBarStyle)(using Sge) {

  private val mainTable: VisTable = new VisTable() {
    override protected def sizeChanged(): Unit = {
      super.sizeChanged()
      closeMenu()
    }
  }
  private val menuItems: VisTable = new VisTable()

  private var currentMenu:  Nullable[Menu]                    = Nullable.empty
  private val menus:        DynamicArray[Menu]                = DynamicArray[Menu]()
  private var menuListener: Nullable[MenuBar.MenuBarListener] = Nullable.empty

  mainTable.left()
  mainTable.add(Nullable[sge.scenes.scene2d.Actor](menuItems))
  mainTable.setBackground(Nullable(style.background))

  def this()(using Sge) = this(VisUI.getSkin.get[MenuBar.MenuBarStyle])

  def this(styleName: String)(using Sge) = this(VisUI.getSkin.get[MenuBar.MenuBarStyle](styleName))

  def addMenu(menu: Menu): Unit = {
    menus.add(menu)
    menu.setMenuBar(Nullable(this))
    menuItems.add(Nullable[sge.scenes.scene2d.Actor](menu.getOpenButton))
  }

  def removeMenu(menu: Menu): Boolean = {
    val removed = menus.removeValue(menu)
    if (removed) {
      menu.setMenuBar(Nullable.empty)
      menuItems.removeActor(menu.getOpenButton)
    }
    removed
  }

  def insertMenu(index: Int, menu: Menu): Unit = {
    menus.insert(index, menu)
    menu.setMenuBar(Nullable(this))
    rebuild()
  }

  private def rebuild(): Unit = {
    menuItems.clear()
    var i = 0
    while (i < menus.size) {
      menuItems.add(Nullable[sge.scenes.scene2d.Actor](menus(i).getOpenButton))
      i += 1
    }
  }

  /** Closes currently opened menu (if any). Used by framework and typically there is no need to call this manually */
  def closeMenu(): Unit = {
    currentMenu.foreach { menu =>
      menu.deselectButton()
      menu.remove()
    }
    currentMenu = Nullable.empty
  }

  private[widget] def getCurrentMenu: Nullable[Menu] = currentMenu

  private[widget] def setCurrentMenu(newMenu: Nullable[Menu]): Unit =
    if (currentMenu == newMenu) ()
    else {
      currentMenu.foreach { cm =>
        cm.deselectButton()
        menuListener.foreach(_.menuClosed(cm))
      }
      newMenu.foreach { nm =>
        nm.selectButton()
        menuListener.foreach(_.menuOpened(nm))
      }
      currentMenu = newMenu
    }

  def setMenuListener(listener: MenuBar.MenuBarListener): Unit =
    this.menuListener = Nullable(listener)

  /** Returns table containing all menus that should be added to Stage, typically with expandX and fillX properties. */
  def getTable: Table = mainTable
}

object MenuBar {

  class MenuBarStyle {
    var background: Drawable = scala.compiletime.uninitialized

    def this(style: MenuBarStyle) = {
      this()
      this.background = style.background
    }

    def this(background: Drawable) = {
      this()
      this.background = background
    }
  }

  trait MenuBarListener {
    def menuOpened(menu: Menu): Unit
    def menuClosed(menu: Menu): Unit
  }
}
