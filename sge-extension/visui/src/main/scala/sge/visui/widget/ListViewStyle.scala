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

import sge.scenes.scene2d.ui.ScrollPane
import sge.utils.Nullable

/** @author Kotcrab */
class ListViewStyle {
  var scrollPaneStyle: Nullable[ScrollPane.ScrollPaneStyle] = Nullable.empty

  def this(style: ListViewStyle) = {
    this()
    if (style.scrollPaneStyle.isDefined) this.scrollPaneStyle = Nullable(new ScrollPane.ScrollPaneStyle(style.scrollPaneStyle.get))
  }
}
