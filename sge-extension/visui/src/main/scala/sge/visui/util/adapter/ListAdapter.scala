/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package visui
package util
package adapter

import sge.utils.Nullable
import sge.visui.widget.ListView
import sge.visui.widget.VisTable

/** Adapter used to display items list in [[ListView]]. Classes implementing this interface should store array and provide delegates to methods that change array state, such as add/remove etc. Those
  * delegates should call [[ListView.ListAdapterListener#invalidateDataSet]]. Single instance of ListAdapter can only be used for one ListView. Implementations must support setting item click listener.
  * @author
  *   Kotcrab
  * @see
  *   [[ArrayAdapter]]
  * @see
  *   [[ArrayListAdapter]]
  * @since 1.0.0
  */
trait ListAdapter[ItemT] {

  /** Called by [[ListView]] when this adapter is assigned to it. */
  def setListView(view: ListView[ItemT], viewListener: ListView.ListAdapterListener[ItemT]): Unit

  /** Called by [[ListView]] when this adapter should create and add all views to provided itemsTable. */
  def fillTable(itemsTable: VisTable): Unit

  /** Called by [[ListView]] when it's item click listener changed. */
  def setItemClickListener(listener: Nullable[ListView.ItemClickListener[ItemT]]): Unit

  /** @return iterable for internal collection */
  def iterable: Iterable[ItemT]

  /** @return size of internal collection */
  def size: Int

  /** @return index of element in internal collection */
  def indexOf(item: ItemT): Int

  /** Adds item to internal collection */
  def add(item: ItemT): Unit

  /** @return element for given index */
  def get(index: Int): ItemT
}
