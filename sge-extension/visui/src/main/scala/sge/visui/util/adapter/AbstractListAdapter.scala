/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 249
 * Covenant-baseline-methods: AbstractListAdapter,DEFAULT_KEY,ListClickListener,ListSelection,ListSelectionListener,SelectionMode,_itemsComparator,_listener,_programmaticChangeEvents,_selection,_selectionMode,clickListener,clickListenerOpt,clicked,deselect,deselectAll,deselectView,deselected,doSelect,fillTable,getGroupMultiSelectKey,getItemsSorter,getListener,getMultiSelectKey,getSelection,getSelectionManager,getSelectionMode,groupMultiSelectKey,i,isGroupMultiSelectKeyPressed,isMultiSelectKeyPressed,isProgrammaticChangeEvents,itemAdded,itemRemoved,itemsChanged,itemsDataChanged,listenerMissing,ls,multiSelectKey,prepareViewBeforeAddingToTable,select,selectGroup,selectView,selected,selection,setGroupMultiSelectKey,setItemClickListener,setItemsSorter,setListView,setListener,setMultiSelectKey,setProgrammaticChangeEvents,setSelectionMode,sort,touchDown,updateView,view,viewListener,viewsCache
 * Covenant-source-reference: com/kotcrab/vis/ui/util/adapter/AbstractListAdapter.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
 */
package sge
package visui
package util
package adapter

import java.util.Comparator

import sge.scenes.scene2d.{ Actor, InputEvent, Touchable }
import sge.scenes.scene2d.utils.{ ClickListener, UIUtils }
import sge.utils.Nullable
import sge.visui.widget.ListView
import sge.visui.widget.VisTable

/** Basic [[ListAdapter]] implementation using [[CachedItemAdapter]]. Supports item selection.
  * @author
  *   Kotcrab
  * @since 1.0.0
  */
abstract class AbstractListAdapter[ItemT, ViewT <: Actor](using Sge) extends CachedItemAdapter[ItemT, ViewT] with ListAdapter[ItemT] {
  protected var view:         Nullable[ListView[ItemT]]                     = Nullable.empty
  protected var viewListener: Nullable[ListView.ListAdapterListener[ItemT]] = Nullable.empty

  private var clickListener: Nullable[ListView.ItemClickListener[ItemT]] = Nullable.empty

  private var _selectionMode: AbstractListAdapter.SelectionMode               = AbstractListAdapter.SelectionMode.DISABLED
  private val selection:      AbstractListAdapter.ListSelection[ItemT, ViewT] = new AbstractListAdapter.ListSelection[ItemT, ViewT](this)

  private var _itemsComparator: Nullable[Comparator[ItemT]] = Nullable.empty

  override def fillTable(itemsTable: VisTable): Unit = {
    _itemsComparator.foreach(sort)
    for (item <- iterable) {
      val v = getView(item).asInstanceOf[ViewT]
      prepareViewBeforeAddingToTable(item, v)
      itemsTable.add(Nullable[Actor](v)).growX()
      itemsTable.row()
    }
  }

  protected def prepareViewBeforeAddingToTable(item: ItemT, view: ViewT): Unit = {
    var listenerMissing = true
    val ls              = view.listeners
    var i               = 0
    while (i < ls.size) {
      ls(i) match {
        case _: AbstractListAdapter.ListClickListener[?, ?] => listenerMissing = false
        case _ => ()
      }
      i += 1
    }
    if (listenerMissing) {
      view.touchable = Touchable.enabled
      view.addListener(new AbstractListAdapter.ListClickListener(this, view, item))
    }
  }

  override def setListView(view: ListView[ItemT], viewListener: ListView.ListAdapterListener[ItemT]): Unit = {
    if (this.view.isDefined) throw new IllegalStateException("Adapter was already assigned to ListView")
    this.view = Nullable(view)
    this.viewListener = Nullable(viewListener)
  }

  override def setItemClickListener(listener: Nullable[ListView.ItemClickListener[ItemT]]): Unit =
    clickListener = listener

  protected def itemAdded(item: ItemT): Unit =
    viewListener.foreach(_.invalidateDataSet())

  protected def itemRemoved(item: ItemT): Unit = {
    selection.deselect(item)
    getViews.remove(item)
    viewListener.foreach(_.invalidateDataSet())
  }

  def itemsChanged(): Unit = {
    selection.deselectAll()
    getViews.clear()
    viewListener.foreach(_.invalidateDataSet())
  }

  def itemsDataChanged(): Unit =
    viewListener.foreach(_.invalidateDataSet())

  override protected def updateView(view: ViewT, item: ItemT): Unit = ()

  def getSelectionMode: AbstractListAdapter.SelectionMode = _selectionMode

  def setSelectionMode(selectionMode: AbstractListAdapter.SelectionMode): Unit =
    _selectionMode = selectionMode

  def setItemsSorter(comparator: Comparator[ItemT]): Unit =
    _itemsComparator = Nullable(comparator)

  def getItemsSorter: Nullable[Comparator[ItemT]] = _itemsComparator

  def getSelection: scala.collection.mutable.ArrayBuffer[ItemT] = selection.getSelection

  def getSelectionManager: AbstractListAdapter.ListSelection[ItemT, ViewT] = selection

  protected def selectView(view: ViewT): Unit =
    if (_selectionMode != AbstractListAdapter.SelectionMode.DISABLED)
      throw new UnsupportedOperationException("selectView must be implemented when selectionMode is not DISABLED")

  protected def deselectView(view: ViewT): Unit =
    if (_selectionMode != AbstractListAdapter.SelectionMode.DISABLED)
      throw new UnsupportedOperationException("deselectView must be implemented when selectionMode is not DISABLED")

  private[adapter] def clickListenerOpt: Nullable[ListView.ItemClickListener[ItemT]] = clickListener

  /** Provides access to the internal views cache for selection management. */
  private[adapter] def viewsCache: scala.collection.mutable.HashMap[ItemT, ViewT] = getViews

  protected def sort(comparator: Comparator[ItemT]): Unit
}

object AbstractListAdapter {

  enum SelectionMode extends java.lang.Enum[SelectionMode] {
    case DISABLED, SINGLE, MULTIPLE
  }

  private class ListClickListener[ItemT, ViewT <: Actor](adapter: AbstractListAdapter[ItemT, ViewT], view: ViewT, item: ItemT) extends ClickListener() {
    override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: sge.Input.Button): Boolean = {
      super.touchDown(event, x, y, pointer, button)
      adapter.getSelectionManager.touchDown(view, item)
      true
    }

    override def clicked(event: InputEvent, x: Float, y: Float): Unit =
      adapter.clickListenerOpt.foreach(_.clicked(item))
  }

  class ListSelection[ItemT, ViewT <: Actor](adapter: AbstractListAdapter[ItemT, ViewT])(using Sge) {
    val DEFAULT_KEY:                 Int = -1
    private var groupMultiSelectKey: Int = DEFAULT_KEY
    private var multiSelectKey:      Int = DEFAULT_KEY

    private val _selection: scala.collection.mutable.ArrayBuffer[ItemT] = scala.collection.mutable.ArrayBuffer[ItemT]()

    private var _programmaticChangeEvents: Boolean                             = true
    private var _listener:                 ListSelectionListener[ItemT, ViewT] = new ListSelectionAdapter[ItemT, ViewT]()

    def select(item: ItemT): Unit =
      adapter.viewsCache.get(item).foreach(v => select(item, v, programmaticChange = true))

    private[adapter] def select(item: ItemT, view: ViewT, programmaticChange: Boolean): Unit =
      if (adapter.getSelectionMode == SelectionMode.DISABLED) ()
      else {
        if (adapter.getSelectionMode == SelectionMode.SINGLE) deselectAll(programmaticChange)
        if (adapter.getSelectionMode == SelectionMode.MULTIPLE && _selection.nonEmpty && isGroupMultiSelectKeyPressed) {
          selectGroup(item)
        }
        doSelect(item, view, programmaticChange)
      }

    private def doSelect(item: ItemT, view: ViewT, programmaticChange: Boolean): Unit =
      if (!_selection.contains(item)) {
        adapter.selectView(view)
        _selection += item
        if (!programmaticChange || _programmaticChangeEvents) _listener.selected(item, view)
      }

    def deselect(item: ItemT): Unit =
      adapter.viewsCache.get(item).foreach(v => deselect(item, v, programmaticChange = true))

    def deselectAll(): Unit = deselectAll(programmaticChange = true)

    private def selectGroup(newItem: ItemT): Unit = {
      val thisSelectionIndex = adapter.indexOf(newItem)
      val lastSelectionIndex = adapter.indexOf(_selection.last)
      if (lastSelectionIndex == -1) ()
      else {
        val start = Math.min(thisSelectionIndex, lastSelectionIndex)
        val end   = Math.max(thisSelectionIndex, lastSelectionIndex)
        var i     = start
        while (i < end) {
          val itm = adapter.get(i)
          adapter.viewsCache.get(itm).foreach(vv => doSelect(itm, vv, programmaticChange = false))
          i += 1
        }
      }
    }

    private[adapter] def deselect(item: ItemT, view: ViewT, programmaticChange: Boolean): Unit =
      if (!_selection.contains(item)) ()
      else {
        adapter.deselectView(view)
        _selection -= item
        if (!programmaticChange || _programmaticChangeEvents) _listener.deselected(item, view)
      }

    private[adapter] def deselectAll(programmaticChange: Boolean): Unit = {
      val items = scala.collection.mutable.ArrayBuffer.from(_selection)
      items.foreach { item =>
        adapter.viewsCache.get(item).foreach(vv => deselect(item, vv, programmaticChange))
      }
    }

    def getSelection: scala.collection.mutable.ArrayBuffer[ItemT] =
      scala.collection.mutable.ArrayBuffer.from(_selection)

    private[adapter] def touchDown(view: ViewT, item: ItemT): Unit =
      if (adapter.getSelectionMode == SelectionMode.DISABLED) ()
      else {
        if (!isMultiSelectKeyPressed && !isGroupMultiSelectKeyPressed) {
          deselectAll(programmaticChange = false)
        }
        if (!_selection.contains(item)) {
          select(item, view, programmaticChange = false)
        } else {
          deselect(item, view, programmaticChange = false)
        }
      }

    def getMultiSelectKey:           Int  = multiSelectKey
    def setMultiSelectKey(key: Int): Unit = multiSelectKey = key

    def getGroupMultiSelectKey:           Int  = groupMultiSelectKey
    def setGroupMultiSelectKey(key: Int): Unit = groupMultiSelectKey = key

    def setListener(listener: ListSelectionListener[ItemT, ViewT]): Unit =
      _listener = if (listener == null) new ListSelectionAdapter[ItemT, ViewT]() else listener // @nowarn -- Java interop

    def getListener: ListSelectionListener[ItemT, ViewT] = _listener

    def isProgrammaticChangeEvents:                  Boolean = _programmaticChangeEvents
    def setProgrammaticChangeEvents(value: Boolean): Unit    = _programmaticChangeEvents = value

    private def isMultiSelectKeyPressed: Boolean =
      if (multiSelectKey == DEFAULT_KEY) UIUtils.ctrl()
      else Sge().input.isKeyPressed(sge.Input.Key(multiSelectKey))

    private def isGroupMultiSelectKeyPressed: Boolean =
      if (groupMultiSelectKey == DEFAULT_KEY) UIUtils.shift()
      else Sge().input.isKeyPressed(sge.Input.Key(groupMultiSelectKey))
  }

  trait ListSelectionListener[ItemT, ViewT] {
    def selected(item:   ItemT, view: ViewT): Unit
    def deselected(item: ItemT, view: ViewT): Unit
  }
}
