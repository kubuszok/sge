/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 68
 * Covenant-baseline-methods: FileListAdapter,createView,fillTable,getOrderedViews,getViews,gridGroup,orderedViews,viewMode
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/file/internal/FileListAdapter.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
 */
package sge
package visui
package widget
package file
package internal

import sge.files.FileHandle
import sge.scenes.scene2d.Actor
import scala.collection.mutable

import sge.utils.{ DynamicArray, Nullable }
import sge.visui.layout.GridGroup
import sge.visui.util.adapter.ArrayAdapter
import sge.visui.widget.VisTable

/** @author Kotcrab */
class FileListAdapter(val chooser: FileChooser, files: DynamicArray[FileHandle])(using Sge) extends ArrayAdapter[FileHandle, FileChooser#FileItem](files) {

  private val orderedViews: DynamicArray[FileChooser#FileItem] = DynamicArray[FileChooser#FileItem]()
  private val gridGroup:    GridGroup                          = new GridGroup(128f, 2f)

  override protected def createView(item: FileHandle): FileChooser#FileItem =
    chooser.createFileItem(item, chooser.getViewMode)

  override def fillTable(itemsTable: VisTable): Unit = {
    getViews.clear() // clear cache
    orderedViews.clear()
    gridGroup.clear()

    getItemsSorter.foreach(s => sort(s))

    val viewMode = chooser.getViewMode

    if (viewMode.isGridMode) {
      viewMode.setupGridGroup(chooser.getSizes, gridGroup)
      val iter = iterable.iterator
      while (iter.hasNext) {
        val item = iter.next()
        val view = getView(item)
        orderedViews.add(view)
        prepareViewBeforeAddingToTable(item, view)
        gridGroup.addActor(view)
      }
      itemsTable.add(Nullable[Actor](gridGroup)).growX().minWidth(0)
    } else {
      val iter = iterable.iterator
      while (iter.hasNext) {
        val item = iter.next()
        val view = getView(item)
        orderedViews.add(view)
        prepareViewBeforeAddingToTable(item, view)
        itemsTable.add(Nullable[Actor](view)).growX()
        itemsTable.row()
      }
    }
  }

  override def getViews: mutable.HashMap[FileHandle, FileChooser#FileItem] = super.getViews

  def getOrderedViews: DynamicArray[FileChooser#FileItem] = orderedViews
}
