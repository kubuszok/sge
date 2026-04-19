/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 62
 * Covenant-baseline-methods: SimpleListAdapter,SimpleListAdapterStyle,background,createView,deselectView,selectView,selection,table,this
 * Covenant-source-reference: com/kotcrab/vis/ui/util/adapter/SimpleListAdapter.java
 * Covenant-verified: 2026-04-19
 */
package sge
package visui
package util
package adapter

import sge.scenes.scene2d.Actor
import sge.scenes.scene2d.utils.Drawable
import sge.utils.{ DynamicArray, Nullable }
import sge.visui.VisUI
import sge.visui.widget.{ VisLabel, VisTable }

/** Very simple default implementation of adapter for [[sge.visui.widget.ListView]]. Uses [[Object#toString]] to create text view for item.
  * @author
  *   Kotcrab
  */
class SimpleListAdapter[ItemT](array: DynamicArray[ItemT], simpleStyle: SimpleListAdapter.SimpleListAdapterStyle)(using Sge) extends ArrayAdapter[ItemT, VisTable](array) {

  def this(array: DynamicArray[ItemT], styleName: String)(using Sge) =
    this(array, VisUI.getSkin.get[SimpleListAdapter.SimpleListAdapterStyle](styleName))

  def this(array: DynamicArray[ItemT])(using Sge) = this(array, "default")

  override protected def createView(item: ItemT): VisTable = {
    val table = new VisTable()
    table.left()
    table.add(Nullable[Actor](new VisLabel(item.toString)))
    table
  }

  override protected def selectView(view: VisTable): Unit =
    view.setBackground(Nullable(simpleStyle.selection))

  override protected def deselectView(view: VisTable): Unit =
    view.setBackground(Nullable(simpleStyle.background))
}

object SimpleListAdapter {

  class SimpleListAdapterStyle {
    var background: Drawable = scala.compiletime.uninitialized
    var selection:  Drawable = scala.compiletime.uninitialized

    def this(background: Drawable, selection: Drawable) = {
      this()
      this.background = background
      this.selection = selection
    }

    def this(style: SimpleListAdapterStyle) = {
      this()
      this.background = style.background
      this.selection = style.selection
    }
  }
}
