/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 66
 * Covenant-baseline-methods: FileChooserStyle,contextMenuSelectedItem,expandDropdown,highlight,iconArrowLeft,iconArrowRight,iconDrive,iconFileAudio,iconFileImage,iconFilePdf,iconFileText,iconFolder,iconFolderNew,iconFolderParent,iconFolderStar,iconListSettings,iconRefresh,iconStar,iconStarOutline,iconTrash,popupMenuStyle,this
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/file/FileChooserStyle.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
 */
package sge
package visui
package widget
package file

import sge.scenes.scene2d.utils.Drawable
import sge.utils.Nullable
import sge.visui.widget.PopupMenu

/** @author Kotcrab */
class FileChooserStyle() {
  var popupMenuStyle: Nullable[PopupMenu.PopupMenuStyle] = Nullable.empty

  var highlight:        Nullable[Drawable] = Nullable.empty
  var iconArrowLeft:    Nullable[Drawable] = Nullable.empty
  var iconArrowRight:   Nullable[Drawable] = Nullable.empty
  var iconFolder:       Nullable[Drawable] = Nullable.empty
  var iconFolderParent: Nullable[Drawable] = Nullable.empty
  var iconFolderStar:   Nullable[Drawable] = Nullable.empty
  var iconFolderNew:    Nullable[Drawable] = Nullable.empty
  var iconDrive:        Nullable[Drawable] = Nullable.empty
  var iconTrash:        Nullable[Drawable] = Nullable.empty
  var iconStar:         Nullable[Drawable] = Nullable.empty
  var iconStarOutline:  Nullable[Drawable] = Nullable.empty
  var iconRefresh:      Nullable[Drawable] = Nullable.empty
  var iconListSettings: Nullable[Drawable] = Nullable.empty

  var iconFileText:  Nullable[Drawable] = Nullable.empty
  var iconFileImage: Nullable[Drawable] = Nullable.empty
  var iconFilePdf:   Nullable[Drawable] = Nullable.empty
  var iconFileAudio: Nullable[Drawable] = Nullable.empty

  var contextMenuSelectedItem: Nullable[Drawable] = Nullable.empty
  var expandDropdown:          Nullable[Drawable] = Nullable.empty

  def this(style: FileChooserStyle) = {
    this()
    popupMenuStyle = style.popupMenuStyle
    highlight = style.highlight
    iconArrowLeft = style.iconArrowLeft
    iconArrowRight = style.iconArrowRight
    iconFolder = style.iconFolder
    iconFolderParent = style.iconFolderParent
    iconFolderStar = style.iconFolderStar
    iconFolderNew = style.iconFolderNew
    iconDrive = style.iconDrive
    iconTrash = style.iconTrash
    iconStar = style.iconStar
    iconStarOutline = style.iconStarOutline
    iconRefresh = style.iconRefresh
    iconListSettings = style.iconListSettings
    iconFileText = style.iconFileText
    iconFileImage = style.iconFileImage
    iconFilePdf = style.iconFilePdf
    iconFileAudio = style.iconFileAudio
    contextMenuSelectedItem = style.contextMenuSelectedItem
    expandDropdown = style.expandDropdown
  }
}
