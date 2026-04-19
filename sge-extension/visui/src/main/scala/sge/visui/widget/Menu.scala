/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 111
 * Covenant-baseline-methods: Menu,MenuStyle,_menuBar,buttonDefault,deselectButton,enter,getOpenButton,openButton,openButtonStyle,remove,result,selectButton,setMenuBar,showMenu,switchMenu,this,touchDown
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/Menu.java
 * Covenant-verified: 2026-04-19
 */
package sge
package visui
package widget

import sge.math.Vector2
import sge.scenes.scene2d.{ Actor, InputEvent, InputListener }
import sge.scenes.scene2d.ui.TextButton
import sge.scenes.scene2d.utils.Drawable
import sge.utils.Nullable
import sge.visui.VisUI

/** Menu used in MenuBar, it is a standard [[PopupMenu]] with title displayed in MenuBar.
  * @author
  *   Kotcrab
  */
class Menu(val title: String, menuStyle: Menu.MenuStyle)(using Sge) extends PopupMenu(menuStyle) {

  private var _menuBar: Nullable[MenuBar] = Nullable.empty

  val openButton:    VisTextButton      = new VisTextButton(title, new VisTextButton.VisTextButtonStyle(menuStyle.openButtonStyle))
  val buttonDefault: Nullable[Drawable] = openButton.style.asInstanceOf[VisTextButton.VisTextButtonStyle].up

  openButton.addListener(
    new InputListener() {
      override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: sge.Input.Button): Boolean = {
        _menuBar.foreach { bar =>
          if (bar.getCurrentMenu.exists(_ eq Menu.this)) {
            bar.closeMenu()
          } else {
            switchMenu()
            event.stop()
          }
        }
        true
      }

      override def enter(event: InputEvent, x: Float, y: Float, pointer: Int, fromActor: Nullable[Actor]): Unit =
        _menuBar.foreach { bar =>
          if (bar.getCurrentMenu.isDefined && !bar.getCurrentMenu.exists(_ eq Menu.this)) switchMenu()
        }
    }
  )

  def this(title: String)(using Sge) = this(title, VisUI.getSkin.get[Menu.MenuStyle])

  def this(title: String, styleName: String)(using Sge) = this(title, VisUI.getSkin.get[Menu.MenuStyle](styleName))

  private def switchMenu(): Unit =
    _menuBar.foreach { bar =>
      bar.closeMenu()
      showMenu()
    }

  private def showMenu(): Unit =
    _menuBar.foreach { _ =>
      val pos = openButton.localToStageCoordinates(new Vector2(0, 0))
      setPosition(pos.x, pos.y - height)
      openButton.stage.foreach { s =>
        s.addActor(this)
      }
      _menuBar.foreach(_.setCurrentMenu(Nullable(this)))
    }

  override def remove(): Boolean = {
    val result = super.remove()
    _menuBar.foreach(_.setCurrentMenu(Nullable.empty))
    result
  }

  def selectButton(): Unit =
    openButton.style.asInstanceOf[VisTextButton.VisTextButtonStyle].up = openButton.style.asInstanceOf[VisTextButton.VisTextButtonStyle].over

  def deselectButton(): Unit =
    openButton.style.asInstanceOf[VisTextButton.VisTextButtonStyle].up = buttonDefault

  /** Called by MenuBar when this menu is added to it */
  private[widget] def setMenuBar(menuBar: Nullable[MenuBar]): Unit = {
    if (_menuBar.isDefined && menuBar.isDefined) throw new IllegalStateException("Menu was already added to MenuBar")
    _menuBar = menuBar
  }

  def getOpenButton: TextButton = openButton
}

object Menu {

  class MenuStyle extends PopupMenu.PopupMenuStyle {
    var openButtonStyle: VisTextButton.VisTextButtonStyle = scala.compiletime.uninitialized

    def this(style: MenuStyle) = {
      this()
      this.background = style.background
      this.border = style.border
      this.openButtonStyle = style.openButtonStyle
    }

    def this(background: Drawable, border: Nullable[Drawable], openButtonStyle: VisTextButton.VisTextButtonStyle) = {
      this()
      this.background = background
      this.border = border
      this.openButtonStyle = openButtonStyle
    }
  }
}
