/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 141
 * Covenant-baseline-methods: ItemClickListener,ListAdapterListener,ListView,ListViewTable,UpdatePolicy,_clickListener,_footer,_header,_mainTable,_scrollPane,_updatePolicy,checkDrawInvalidation,clicked,dataInvalidated,draw,footer,footer_,getAdapter,getClickListener,getMainTable,getScrollPane,header,header_,invalidateDataFromAdapter,invalidateDataSet,itemsTable,rebuildView,scrollTable,setItemClickListener,this,updatePolicy,updatePolicy_
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/ListView.java
 * Covenant-verified: 2026-04-19
 */
package sge
package visui
package widget

import scala.language.implicitConversions

import sge.graphics.g2d.Batch
import sge.scenes.scene2d.Actor
import sge.utils.Nullable
import sge.visui.VisUI
import sge.visui.util.adapter.ListAdapter

/** ListView displays list of scrollable items. Item views are created by using [[ListAdapter]]s.
  * @author
  *   Kotcrab
  * @see
  *   [[ListAdapter]]
  * @since 1.0.0
  */
class ListView[ItemT](adapter: ListAdapter[ItemT], listViewStyle: ListViewStyle)(using Sge) {

  private var _clickListener:  Nullable[ListView.ItemClickListener[ItemT]] = Nullable.empty
  private var _updatePolicy:   ListView.UpdatePolicy                       = ListView.UpdatePolicy.IMMEDIATELY
  private var dataInvalidated: Boolean                                     = false

  private val _mainTable:  ListView.ListViewTable[ItemT] = new ListView.ListViewTable[ItemT](this)
  private val scrollTable: VisTable                      = new VisTable()
  private val itemsTable:  VisTable                      = new VisTable()

  private var _header: Nullable[Actor] = Nullable.empty
  private var _footer: Nullable[Actor] = Nullable.empty

  private val _scrollPane: VisScrollPane = new VisScrollPane(scrollTable, listViewStyle.scrollPaneStyle.get)
  _scrollPane.setOverscroll(false, true)
  _scrollPane.setFlickScroll(false)
  _scrollPane.setFadeScrollBars(false)
  _mainTable.add(Nullable[Actor](_scrollPane)).grow()

  adapter.setListView(this, new ListView.ListAdapterListener(this))
  rebuildView(full = true)

  def this(adapter: ListAdapter[ItemT], styleName: String)(using Sge) =
    this(adapter, VisUI.getSkin.get[ListViewStyle](styleName))

  def this(adapter: ListAdapter[ItemT])(using Sge) = this(adapter, "default")

  def rebuildView(): Unit = rebuildView(full = true)

  private def rebuildView(full: Boolean): Unit = {
    scrollTable.clearChildren()
    scrollTable.top
    _header.foreach { h =>
      scrollTable.add(Nullable[Actor](h)).growX()
      scrollTable.row()
    }

    if (full) {
      itemsTable.clearChildren()
      adapter.fillTable(itemsTable)
    }

    scrollTable.add(Nullable[Actor](itemsTable)).growX()
    scrollTable.row()

    _footer.foreach { f =>
      scrollTable.add(Nullable[Actor](f)).growX()
      scrollTable.row()
    }
  }

  def getAdapter: ListAdapter[ItemT] = adapter

  /** @return main table containing scroll pane and all items view */
  def getMainTable: ListView.ListViewTable[ItemT] = _mainTable

  /** @return internal [[VisScrollPane]]. Do NOT add this scroll pane directly to Stage use [[getMainTable]] instead. Use this only for changing scroll pane properties. */
  def getScrollPane: VisScrollPane = _scrollPane

  def setItemClickListener(listener: ListView.ItemClickListener[ItemT]): Unit = {
    _clickListener = Nullable(listener)
    adapter.setItemClickListener(Nullable(listener))
  }

  def getClickListener: Nullable[ListView.ItemClickListener[ItemT]] = _clickListener

  def header:             Nullable[Actor] = _header
  def header_=(h: Actor): Unit            = { _header = Nullable(h); rebuildView(full = false) }

  def footer:             Nullable[Actor] = _footer
  def footer_=(f: Actor): Unit            = { _footer = Nullable(f); rebuildView(full = false) }

  def updatePolicy:                                  ListView.UpdatePolicy = _updatePolicy
  def updatePolicy_=(policy: ListView.UpdatePolicy): Unit                  = _updatePolicy = policy

  private[widget] def invalidateDataFromAdapter(): Unit = {
    if (_updatePolicy == ListView.UpdatePolicy.IMMEDIATELY) rebuildView(full = true)
    if (_updatePolicy == ListView.UpdatePolicy.ON_DRAW) dataInvalidated = true
  }

  private[widget] def checkDrawInvalidation(): Unit =
    if (_updatePolicy == ListView.UpdatePolicy.ON_DRAW && dataInvalidated) rebuildView(full = true)
}

object ListView {

  trait ItemClickListener[ItemT] {
    def clicked(item: ItemT): Unit
  }

  class ListAdapterListener[ItemT](listView: ListView[ItemT]) {
    def invalidateDataSet(): Unit = listView.invalidateDataFromAdapter()
  }

  /** Controls when list view's views are updated after underlying data was invalidated. */
  enum UpdatePolicy extends java.lang.Enum[UpdatePolicy] {

    /** If list data was was invalidated then views are updated before drawing list. */
    case ON_DRAW

    /** If list data was was invalidated then views are updated immediately after data invalidation. */
    case IMMEDIATELY

    /** In manual mode ListView must be rebuild manually by calling rebuildView(). */
    case MANUAL
  }

  /** ListView main table. */
  class ListViewTable[ItemT](val listView: ListView[ItemT])(using Sge) extends VisTable() {
    override def draw(batch: Batch, parentAlpha: Float): Unit = {
      listView.checkDrawInvalidation()
      super.draw(batch, parentAlpha)
    }
  }
}
