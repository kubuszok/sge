/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 84
 * Covenant-baseline-methods: FileChooserText,format,get,getBundle,name,toString
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/file/internal/FileChooserText.java
 * Covenant-verified: 2026-04-19
 */
package sge
package visui
package widget
package file
package internal

import sge.utils.I18NBundle
import sge.visui.Locales
import sge.visui.i18n.BundleText

/** Contains texts for chooser access via I18NBundle.
  * @author
  *   Kotcrab
  * @since 0.7.0
  */
enum FileChooserText(val entryName: String) extends BundleText {
  case TITLE_CHOOSE_FILES extends FileChooserText("titleChooseFiles")
  case TITLE_CHOOSE_DIRECTORIES extends FileChooserText("titleChooseDirectories")
  case TITLE_CHOOSE_FILES_AND_DIRECTORIES extends FileChooserText("titleChooseFilesAndDirectories")
  case CANCEL extends FileChooserText("cancel")
  case FILE_NAME extends FileChooserText("fileName")
  case FILE_TYPE extends FileChooserText("fileType")
  case ALL_FILES extends FileChooserText("allFiles")
  case DESKTOP extends FileChooserText("desktop")
  case COMPUTER extends FileChooserText("computer")
  case OPEN extends FileChooserText("open")
  case SAVE extends FileChooserText("save")
  case BACK extends FileChooserText("back")
  case FORWARD extends FileChooserText("forward")
  case PARENT_DIRECTORY extends FileChooserText("parentDirectory")
  case NEW_DIRECTORY extends FileChooserText("newDirectory")
  case DIRECTORY_NO_LONGER_EXISTS extends FileChooserText("directoryNoLongerExists")
  case POPUP_TITLE extends FileChooserText("popupTitle")
  case POPUP_CHOOSE_FILE extends FileChooserText("popupChooseFile")
  case POPUP_SELECTED_FILE_DOES_NOT_EXIST extends FileChooserText("popupSelectedFileDoesNotExist")
  case POPUP_DIRECTORY_DOES_NOT_EXIST extends FileChooserText("popupDirectoryDoesNotExist")
  case POPUP_ONLY_DIRECTORIES extends FileChooserText("popupOnlyDirectories")
  case POPUP_FILENAME_INVALID extends FileChooserText("popupFilenameInvalid")
  case POPUP_FILE_EXIST_OVERWRITE extends FileChooserText("popupFileExistOverwrite")
  case POPUP_MULTIPLE_FILE_EXIST_OVERWRITE extends FileChooserText("popupMultipleFileExistOverwrite")
  case POPUP_DELETE_FILE_FAILED extends FileChooserText("popupDeleteFileFailed")
  case CONTEXT_MENU_DELETE extends FileChooserText("contextMenuDelete")
  case CONTEXT_MENU_MOVE_TO_TRASH extends FileChooserText("contextMenuMoveToTrash")
  case CONTEXT_MENU_SHOW_IN_EXPLORER extends FileChooserText("contextMenuShowInExplorer")
  case CONTEXT_MENU_REFRESH extends FileChooserText("contextMenuRefresh")
  case CONTEXT_MENU_ADD_TO_FAVORITES extends FileChooserText("contextMenuAddToFavorites")
  case CONTEXT_MENU_REMOVE_FROM_FAVORITES extends FileChooserText("contextMenuRemoveFromFavorites")
  case CONTEXT_MENU_DELETE_WARNING extends FileChooserText("contextMenuDeleteWarning")
  case CONTEXT_MENU_MOVE_TO_TRASH_WARNING extends FileChooserText("contextMenuMoveToTrashWarning")
  case CONTEXT_MENU_NEW_DIRECTORY extends FileChooserText("contextMenuNewDirectory")
  case CONTEXT_MENU_SORT_BY extends FileChooserText("contextMenuSortBy")
  case SORT_BY_NAME extends FileChooserText("sortByName")
  case SORT_BY_DATE extends FileChooserText("sortByDate")
  case SORT_BY_SIZE extends FileChooserText("sortBySize")
  case SORT_BY_ASCENDING extends FileChooserText("sortByAscending")
  case SORT_BY_DESCENDING extends FileChooserText("sortByDescending")
  case NEW_DIRECTORY_DIALOG_TITLE extends FileChooserText("newDirectoryDialogTitle")
  case NEW_DIRECTORY_DIALOG_TEXT extends FileChooserText("newDirectoryDialogText")
  case NEW_DIRECTORY_DIALOG_ILLEGAL_CHARACTERS extends FileChooserText("newDirectoryDialogIllegalCharacters")
  case NEW_DIRECTORY_DIALOG_ALREADY_EXISTS extends FileChooserText("newDirectoryDialogAlreadyExists")
  case CHANGE_VIEW_MODE extends FileChooserText("changeViewMode")
  case VIEW_MODE_LIST extends FileChooserText("viewModeList")
  case VIEW_MODE_DETAILS extends FileChooserText("viewModeDetails")
  case VIEW_MODE_BIG_ICONS extends FileChooserText("viewModeBigIcons")
  case VIEW_MODE_MEDIUM_ICONS extends FileChooserText("viewModeMediumIcons")
  case VIEW_MODE_SMALL_ICONS extends FileChooserText("viewModeSmallIcons")

  override def name:                       String = entryName
  override def get:                        String = FileChooserText.getBundle.get(entryName)
  override def format():                   String = FileChooserText.getBundle.format(entryName)
  override def format(arguments: AnyRef*): String = FileChooserText.getBundle.format(entryName, arguments*)
  override def toString:                   String = get
}

object FileChooserText {
  private def getBundle: I18NBundle = Locales.getFileChooserBundle(using VisUI.sgeInstance)
}
