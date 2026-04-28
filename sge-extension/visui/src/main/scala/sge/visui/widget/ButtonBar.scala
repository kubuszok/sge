/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 124
 * Covenant-baseline-methods: ButtonBar,ButtonType,LINUX_ORDER,OSX_ORDER,WINDOWS_ORDER,_ignoreSpacing,buttons,createTable,getButton,getDefaultOrder,getTextButton,i,ignoreSpacing,ignoreSpacing_,order,order_,setButton,spacingValid,table,text,this,toString
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/ButtonBar.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
 */
package sge
package visui
package widget

import scala.collection.mutable

import sge.scenes.scene2d.Actor
import sge.scenes.scene2d.ui.Button
import sge.scenes.scene2d.utils.ChangeListener
import sge.utils.Nullable
import sge.visui.{ Locales, Sizes, VisUI }
import sge.visui.util.OsUtils

/** Convenient class for creating button panels with buttons such as "Ok", "Cancel", "Yes" etc. Buttons are arranged in platform dependent order. Built-in orders support Windows, Mac, and Linux. When
  * no platform matches ButtonBar defaults to Linux order. User may specify custom order, see [[ButtonBar.ButtonType]] for buttons ids.
  * @author
  *   Kotcrab
  * @since 1.0.0
  */
class ButtonBar(sizes: Sizes, private var _order: String)(using Sge) {

  private val buttons:        mutable.HashMap[Char, Button] = mutable.HashMap.empty
  private var _ignoreSpacing: Boolean                       = false

  def this()(using Sge) = this(VisUI.getSizes, ButtonBar.getDefaultOrder)
  def this(order: String)(using Sge) = this(VisUI.getSizes, order)
  def this(sizes: Sizes)(using Sge) = this(sizes, ButtonBar.getDefaultOrder)

  def ignoreSpacing: Boolean = _ignoreSpacing

  /** @param ignoreSpacing if true spacing symbols in order will be ignored */
  def ignoreSpacing_=(value: Boolean): Unit = _ignoreSpacing = value

  def order: String = _order

  def order_=(order: String): Unit = {
    require(order != null, "order can't be null") // @nowarn -- Java interop boundary
    _order = order
  }

  def setButton(buttonType: ButtonBar.ButtonType, listener: ChangeListener): Unit =
    setButton(buttonType, buttonType.text, listener)

  def setButton(buttonType: ButtonBar.ButtonType, text: String, listener: ChangeListener): Unit =
    setButton(buttonType, new VisTextButton(text), listener)

  def setButton(buttonType: ButtonBar.ButtonType, button: Button): Unit =
    setButton(buttonType, button, null) // @nowarn -- Java interop boundary

  def setButton(buttonType: ButtonBar.ButtonType, button: Button, listener: ChangeListener): Unit = {
    require(buttonType != null, "type can't be null") // @nowarn -- Java interop boundary
    require(button != null, "button can't be null") // @nowarn -- Java interop boundary
    buttons(buttonType.id) = button
    if (listener != null) button.addListener(listener) // @nowarn -- Java interop boundary
  }

  def getButton(buttonType: ButtonBar.ButtonType): Button = buttons(buttonType.id)

  /** @return stored button casted to [[VisTextButton]]. */
  def getTextButton(buttonType: ButtonBar.ButtonType): VisTextButton = getButton(buttonType).asInstanceOf[VisTextButton]

  /** Builds and returns [[VisTable]] containing buttons in platform dependant order. Note that calling this multiple times will remove buttons from previous tables.
    */
  def createTable(): VisTable = {
    val table = new VisTable(true)
    table.left()

    var spacingValid = false
    var i            = 0
    while (i < _order.length) {
      val ch = _order.charAt(i)

      if (!_ignoreSpacing && ch == ' ' && spacingValid) {
        table.add(Nullable.empty[Actor]).width(sizes.buttonBarSpacing)
        spacingValid = false
      }

      buttons.get(ch).foreach { button =>
        table.add(Nullable[Actor](button))
        spacingValid = true
      }
      i += 1
    }

    table
  }
}

object ButtonBar {
  val WINDOWS_ORDER: String = "L H BEF YNOCA R"
  val OSX_ORDER:     String = "L H BEF NYCOA R"
  val LINUX_ORDER:   String = "L H NYACBEFO R"

  private def getDefaultOrder: String =
    if (OsUtils.isWindows) WINDOWS_ORDER
    else if (OsUtils.isMac) OSX_ORDER
    else LINUX_ORDER

  /** Defines possible button types for [[ButtonBar]] */
  enum ButtonType(val key: String, val id: Char) {
    case LEFT extends ButtonType("left", 'L')
    case RIGHT extends ButtonType("right", 'R')
    case HELP extends ButtonType("help", 'H')
    case NO extends ButtonType("no", 'N')
    case YES extends ButtonType("yes", 'Y')
    case CANCEL extends ButtonType("cancel", 'C')
    case BACK extends ButtonType("back", 'B')
    case NEXT extends ButtonType("next", 'E')
    case APPLY extends ButtonType("apply", 'A')
    case FINISH extends ButtonType("finish", 'F')
    case OK extends ButtonType("ok", 'O')

    def text(using Sge): String = Locales.getButtonBarBundle.get(key)

    override def toString: String = key
  }
}
