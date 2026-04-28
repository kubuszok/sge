/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 98
 * Covenant-baseline-methods: SortingPopupMenu,build,changed,selectedMenuItem,sortByAscending,sortByAscendingImage,sortByDate,sortByDateImage,sortByDescending,sortByDescendingImage,sortByName,sortByNameImage,sortBySize,sortBySizeImage
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/file/internal/SortingPopupMenu.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
 */
package sge
package visui
package widget
package file
package internal

import sge.scenes.scene2d.Actor
import sge.scenes.scene2d.ui.Image
import sge.scenes.scene2d.utils.{ ChangeListener, Drawable }
import sge.utils.{ Nullable, Scaling }
import sge.visui.widget.{ MenuItem, PopupMenu }

/** @author Kotcrab */
class SortingPopupMenu(chooser: FileChooser)(using Sge) extends PopupMenu(chooser.getChooserStyle.popupMenuStyle.get) {
  private val selectedMenuItem: Drawable = chooser.getChooserStyle.contextMenuSelectedItem.get

  private val sortByName: MenuItem = new MenuItem(
    FileChooserText.SORT_BY_NAME.get,
    selectedMenuItem,
    new ChangeListener() {
      override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit = chooser.setSorting(FileChooser.FileSorting.NAME, sortingOrderAscending = true)
    }
  )
  private val sortByDate: MenuItem = new MenuItem(
    FileChooserText.SORT_BY_DATE.get,
    selectedMenuItem,
    new ChangeListener() {
      override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit = chooser.setSorting(FileChooser.FileSorting.MODIFIED_DATE, sortingOrderAscending = false)
    }
  )
  private val sortBySize: MenuItem = new MenuItem(
    FileChooserText.SORT_BY_SIZE.get,
    selectedMenuItem,
    new ChangeListener() {
      override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit = chooser.setSorting(FileChooser.FileSorting.SIZE, sortingOrderAscending = true)
    }
  )

  private val sortByAscending: MenuItem = new MenuItem(
    FileChooserText.SORT_BY_ASCENDING.get,
    selectedMenuItem,
    new ChangeListener() {
      override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit = chooser.sortingOrderAscending_=(true)
    }
  )
  private val sortByDescending: MenuItem = new MenuItem(
    FileChooserText.SORT_BY_DESCENDING.get,
    selectedMenuItem,
    new ChangeListener() {
      override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit = chooser.sortingOrderAscending_=(false)
    }
  )

  private val sortByNameImage:       Image = sortByName.getImage.get
  private val sortByDateImage:       Image = sortByDate.getImage.get
  private val sortBySizeImage:       Image = sortBySize.getImage.get
  private val sortByAscendingImage:  Image = sortByAscending.getImage.get
  private val sortByDescendingImage: Image = sortByDescending.getImage.get

  {
    addItem(sortByName)
    addItem(sortByDate)
    addItem(sortBySize)
    addSeparator()
    addItem(sortByAscending)
    addItem(sortByDescending)

    sortByNameImage.scaling = Scaling.none
    sortByDateImage.scaling = Scaling.none
    sortBySizeImage.scaling = Scaling.none
    sortByAscendingImage.scaling = Scaling.none
    sortByDescendingImage.scaling = Scaling.none
  }

  def build(): Unit = {
    sortByNameImage.drawable = Nullable.empty
    sortByDateImage.drawable = Nullable.empty
    sortBySizeImage.drawable = Nullable.empty
    sortByAscendingImage.drawable = Nullable.empty
    sortByDescendingImage.drawable = Nullable.empty

    chooser.getSorting match {
      case FileChooser.FileSorting.NAME          => sortByNameImage.drawable = Nullable(selectedMenuItem)
      case FileChooser.FileSorting.MODIFIED_DATE => sortByDateImage.drawable = Nullable(selectedMenuItem)
      case FileChooser.FileSorting.SIZE          => sortBySizeImage.drawable = Nullable(selectedMenuItem)
    }

    if (chooser.isSortingOrderAscending) sortByAscendingImage.drawable = Nullable(selectedMenuItem)
    else sortByDescendingImage.drawable = Nullable(selectedMenuItem)
  }
}
