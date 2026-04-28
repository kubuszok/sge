/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 125
 * Covenant-baseline-methods: FilePopupMenu,FilePopupMenuCallback,addToFavorites,build,buildForFavorite,changed,clicked,delete,file,fileDeleterChanged,isAddedToStage,newDirectory,refresh,removeFromFavorites,showFileDelDialog,showInExplorer,showNewDirDialog,sortBy,sortingPopupMenu,style
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/file/internal/FilePopupMenu.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
 */
package sge
package visui
package widget
package file
package internal

import java.io.File

import sge.files.FileHandle
import sge.files.FileType
import sge.scenes.scene2d.{ Actor, InputEvent }
import sge.scenes.scene2d.utils.{ ChangeListener, ClickListener }
import sge.utils.{ DynamicArray, Nullable }
import sge.visui.widget.{ MenuItem, PopupMenu }

/** @author Kotcrab */
class FilePopupMenu(chooser: FileChooser, callback: FilePopupMenu.FilePopupMenuCallback)(using Sge) extends PopupMenu(chooser.getChooserStyle.popupMenuStyle.get) {

  private val style: FileChooserStyle = chooser.getChooserStyle

  private val sortingPopupMenu: SortingPopupMenu = new SortingPopupMenu(chooser)

  private var file: Nullable[FileHandle] = Nullable.empty

  private val delete:              MenuItem = new MenuItem(FileChooserText.CONTEXT_MENU_DELETE.get, style.iconTrash.get)
  private val newDirectory:        MenuItem = new MenuItem(FileChooserText.CONTEXT_MENU_NEW_DIRECTORY.get, style.iconFolderNew.get)
  private val showInExplorer:      MenuItem = new MenuItem(FileChooserText.CONTEXT_MENU_SHOW_IN_EXPLORER.get)
  private val refresh:             MenuItem = new MenuItem(FileChooserText.CONTEXT_MENU_REFRESH.get, style.iconRefresh.get)
  private val addToFavorites:      MenuItem = new MenuItem(FileChooserText.CONTEXT_MENU_ADD_TO_FAVORITES.get, style.iconFolderStar.get)
  private val removeFromFavorites: MenuItem = new MenuItem(FileChooserText.CONTEXT_MENU_REMOVE_FROM_FAVORITES.get, style.iconFolderStar.get)
  private val sortBy:              MenuItem = new MenuItem(FileChooserText.CONTEXT_MENU_SORT_BY.get)

  {
    sortBy.subMenu = Nullable(sortingPopupMenu)

    delete.addListener(new ClickListener() {
      override def clicked(event: InputEvent, x: Float, y: Float): Unit = callback.showFileDelDialog(file.get)
    })

    newDirectory.addListener(new ClickListener() {
      override def clicked(event: InputEvent, x: Float, y: Float): Unit = callback.showNewDirDialog()
    })

    showInExplorer.addListener(
      new ClickListener() {
        override def clicked(event: InputEvent, x: Float, y: Float): Unit =
          try
            FileUtils.showDirInExplorer(file.get)
          catch {
            case e: java.io.IOException => e.printStackTrace()
          }
      }
    )

    refresh.addListener(new ChangeListener() {
      override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit = chooser.refresh()
    })

    addToFavorites.addListener(new ClickListener() {
      override def clicked(event: InputEvent, x: Float, y: Float): Unit = chooser.addFavorite(file.get)
    })

    removeFromFavorites.addListener(new ClickListener() {
      override def clicked(event: InputEvent, x: Float, y: Float): Unit = chooser.removeFavorite(file.get)
    })
  }

  def build(): Unit = {
    sortingPopupMenu.build()
    clearChildren()
    addItem(newDirectory)
    addItem(sortBy)
    addItem(refresh)
  }

  def build(favorites: DynamicArray[FileHandle], file: FileHandle): Unit = {
    sortingPopupMenu.build()
    this.file = Nullable(file)

    clearChildren()

    addItem(newDirectory)
    addItem(sortBy)
    addItem(refresh)
    addSeparator()

    if (file.fileType == FileType.Absolute || file.fileType == FileType.External) addItem(delete)

    if (file.fileType == FileType.Absolute) {
      addItem(showInExplorer)

      if (file.isDirectory()) {
        if (favorites.contains(file)) addItem(removeFromFavorites)
        else addItem(addToFavorites)
      }
    }
  }

  def buildForFavorite(favorites: DynamicArray[FileHandle], file: File): Unit = {
    this.file = Nullable(Sge().files.absolute(file.getAbsolutePath))

    clearChildren()
    addItem(showInExplorer)
    if (favorites.contains(this.file.get)) addItem(removeFromFavorites)
  }

  def isAddedToStage: Boolean = stage.isDefined

  def fileDeleterChanged(trashAvailable: Boolean): Unit =
    delete.setText(if (trashAvailable) FileChooserText.CONTEXT_MENU_MOVE_TO_TRASH.get else FileChooserText.CONTEXT_MENU_DELETE.get)
}

object FilePopupMenu {
  trait FilePopupMenuCallback {
    def showNewDirDialog():                  Unit
    def showFileDelDialog(file: FileHandle): Unit
  }
}
