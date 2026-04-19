/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: MJ
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 128
 * Covenant-baseline-methods: CellWidget,CellWidgetBuilder,EMPTY,_height,_minHeight,_minWidth,_width,align,alignment,appliedPadding,applyFillingData,applyPadding,applySizeData,buildCell,builder,cell,empty,expandX,expandY,fillX,fillY,getWidget,height,minHeight,minWidth,of,padding,this,useSpacing,using,widget,width,withExpandX,withExpandY,withFillX,withFillY,withSpacing,wrap
 * Covenant-source-reference: com/kotcrab/vis/ui/building/utilities/CellWidget.java
 * Covenant-verified: 2026-04-19
 */
package sge
package visui
package building
package utilities

import sge.scenes.scene2d.Actor
import sge.scenes.scene2d.ui.{ Cell, Table }
import sge.utils.Nullable

/** Wraps a Scene2D widget, allowing to store cell data for delayed Table creation.
  * @author
  *   MJ
  */
class CellWidget[W <: Actor] private (builder: CellWidget.CellWidgetBuilder[W]) {
  private val widget:     W                   = builder.widget.asInstanceOf[W]
  private val padding:    Nullable[Padding]   = builder.padding
  private val expandX:    Boolean             = builder.expandX
  private val expandY:    Boolean             = builder.expandY
  private val fillX:      Boolean             = builder.fillX
  private val fillY:      Boolean             = builder.fillY
  private val useSpacing: Boolean             = builder.useSpacing
  private val alignment:  Nullable[Alignment] = builder.alignment
  private val _width:     Int                 = builder.width
  private val _height:    Int                 = builder.height
  private val _minWidth:  Int                 = builder.minWidth
  private val _minHeight: Int                 = builder.minHeight

  def getWidget: W = widget

  def buildCell(table: Table): Cell[?] = buildCell(table, Nullable.empty)

  def buildCell(table: Table, defaultWidgetPadding: Nullable[Padding]): Cell[?] = {
    val cell = table.add(Nullable(widget))
    applyPadding(cell, defaultWidgetPadding)
    applySizeData(cell)
    applyFillingData(cell)
    cell
  }

  private def applyPadding(cell: Cell[?], defaultWidgetPadding: Nullable[Padding]): Unit = {
    val appliedPadding = if (padding.isDefined) padding else defaultWidgetPadding
    if (appliedPadding.isDefined) {
      if (useSpacing) appliedPadding.get.applySpacing(cell)
      else appliedPadding.get.applyPadding(cell)
    }
  }

  private def applySizeData(cell: Cell[?]): Unit = {
    if (_width > 0) cell.width(_width)
    if (_height > 0) cell.height(_height)
    if (_minWidth > 0) cell.minWidth(_minWidth)
    if (_minHeight > 0) cell.minHeight(_minHeight)
  }

  private def applyFillingData(cell: Cell[?]): Unit = {
    if (alignment.isDefined) alignment.get.apply(cell)
    cell.expand(expandX, expandY)
    cell.fill(fillX, fillY)
  }
}

object CellWidget {
  val EMPTY: CellWidget[?] = empty()

  def of[W <: Actor](widget: W): CellWidgetBuilder[W] = new CellWidgetBuilder[W](widget)

  def using[W <: Actor](widget: CellWidget[W]): CellWidgetBuilder[W] = new CellWidgetBuilder[W](widget)

  def wrap[W <: Actor](widget: W): CellWidget[W] = of(widget).wrap()

  def wrap(widgets: Actor*): scala.Array[CellWidget[?]] =
    widgets.map(w => CellWidget.of(w).wrap().asInstanceOf[CellWidget[?]]).toArray

  def empty(): CellWidget[?] = builder().wrap()

  def builder(): CellWidgetBuilder[Actor] = of(null.asInstanceOf[Actor]) // @nowarn -- Java interop boundary

  class CellWidgetBuilder[W <: Actor] private[CellWidget] (private[CellWidget] var widget: Actor) {
    private[CellWidget] var padding:    Nullable[Padding]   = Nullable.empty
    private[CellWidget] var expandX:    Boolean             = false
    private[CellWidget] var expandY:    Boolean             = false
    private[CellWidget] var fillX:      Boolean             = false
    private[CellWidget] var fillY:      Boolean             = false
    private[CellWidget] var useSpacing: Boolean             = false
    private[CellWidget] var alignment:  Nullable[Alignment] = Nullable.empty
    private[CellWidget] var width:      Int                 = 0
    private[CellWidget] var height:     Int                 = 0
    private[CellWidget] var minWidth:   Int                 = 0
    private[CellWidget] var minHeight:  Int                 = 0

    private[CellWidget] def this(cw: CellWidget[W]) = {
      this(cw.widget)
      padding = cw.padding
      expandX = cw.expandX
      expandY = cw.expandY
      fillX = cw.fillX
      fillY = cw.fillY
      useSpacing = cw.useSpacing
      alignment = cw.alignment
      width = cw._width
      height = cw._height
      minWidth = cw._minWidth
      minHeight = cw._minHeight
    }

    def wrap(): CellWidget[W] = new CellWidget[W](this)

    def widget(w:            W):         CellWidgetBuilder[W] = { this.widget = w; this }
    def padding(p:           Padding):   CellWidgetBuilder[W] = { this.padding = Nullable(p); this }
    def withSpacing():                   CellWidgetBuilder[W] = { this.useSpacing = true; this }
    def withExpandX():                   CellWidgetBuilder[W] = { this.expandX = true; this }
    def withExpandY():                   CellWidgetBuilder[W] = { this.expandY = true; this }
    def withFillX():                     CellWidgetBuilder[W] = { this.fillX = true; this }
    def withFillY():                     CellWidgetBuilder[W] = { this.fillY = true; this }
    def align(alignment:     Alignment): CellWidgetBuilder[W] = { this.alignment = Nullable(alignment); this }
    def width(width:         Int):       CellWidgetBuilder[W] = { this.width = width; this }
    def height(height:       Int):       CellWidgetBuilder[W] = { this.height = height; this }
    def minWidth(minWidth:   Int):       CellWidgetBuilder[W] = { this.minWidth = minWidth; this }
    def minHeight(minHeight: Int):       CellWidgetBuilder[W] = { this.minHeight = minHeight; this }
  }
}
