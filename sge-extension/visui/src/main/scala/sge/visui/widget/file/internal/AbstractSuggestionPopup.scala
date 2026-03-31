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
package file
package internal

import sge.visui.widget.{ MenuItem, PopupMenu }

/** @author Kotcrab */
class AbstractSuggestionPopup(val chooser: FileChooser)(using Sge) extends PopupMenu(chooser.getChooserStyle.popupMenuStyle.get) {

  protected def createMenuItem(name: String): MenuItem = {
    val item = new MenuItem(name)
    item.getImageCell.size(0)
    item.getShortcutCell.foreach(_.space(0).pad(0))
    item.getSubMenuIconCell.size(0).space(0).pad(0)
    item
  }
}

object AbstractSuggestionPopup {
  val MAX_SUGGESTIONS: Int = 10
}
