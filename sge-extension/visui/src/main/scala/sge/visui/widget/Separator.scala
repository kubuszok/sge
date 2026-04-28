/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 56
 * Covenant-baseline-methods: Separator,SeparatorStyle,background,c,draw,prefHeight,prefWidth,style,thickness,this
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/Separator.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
 */
package sge
package visui
package widget

import sge.graphics.g2d.Batch
import sge.scenes.scene2d.ui.Widget
import sge.scenes.scene2d.utils.Drawable

/** A separator widget (horizontal or vertical bar) that can be used in menus, tables or other widgets, typically added to new row with growX() (if creating horizontal separator) OR growY() (if
  * creating vertical separator). [[PopupMenu]] and [[VisTable]] provide utility addSeparator() methods that add new separators.
  * @author
  *   Kotcrab
  */
class Separator(private val _style: Separator.SeparatorStyle)(using Sge) extends Widget() {
  import Separator._

  def this()(using Sge) = this(VisUI.getSkin.get[Separator.SeparatorStyle])
  def this(styleName: String)(using Sge) = this(VisUI.getSkin.get[Separator.SeparatorStyle](styleName))

  override def prefHeight: Float = _style.thickness.toFloat
  override def prefWidth:  Float = _style.thickness.toFloat

  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    val c = color
    batch.setColor(c.r, c.g, c.b, c.a * parentAlpha)
    _style.background.draw(batch, x, y, width, height)
  }

  def style: SeparatorStyle = _style
}

object Separator {
  class SeparatorStyle() {
    var background: Drawable = scala.compiletime.uninitialized
    var thickness:  Int      = 0

    def this(style: SeparatorStyle) = {
      this()
      this.background = style.background
      this.thickness = style.thickness
    }

    def this(bg: Drawable, thickness: Int) = {
      this()
      this.background = bg
      this.thickness = thickness
    }
  }
}
