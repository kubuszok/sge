/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package visui
package widget
package file
package internal

import scala.language.implicitConversions

import sge.files.FileHandle
import sge.scenes.scene2d.{ Actor, Stage }
import sge.scenes.scene2d.utils.ChangeListener
import sge.utils.DynamicArray
import sge.visui.widget.VisTextField

/** @author Kotcrab */
class FileSuggestionPopup(chooser: FileChooser)(using Sge) extends AbstractSuggestionPopup(chooser) {

  def pathFieldKeyTyped(stage: Stage, files: DynamicArray[FileHandle], pathField: VisTextField): Unit = {
    if (pathField.text.length == 0) {
      remove()
    } else {
      val suggestions = createSuggestions(files, pathField)
      if (suggestions == 0) {
        remove()
      } else {
        showMenu(stage, pathField)
      }
    }
  }

  private def createSuggestions(files: DynamicArray[FileHandle], fileNameField: VisTextField): Int = {
    clearChildren()
    var suggestions = 0
    val iter        = files.iterator
    while (iter.hasNext) {
      val file = iter.next()
      if (file.name.startsWith(fileNameField.text) && !file.name.equals(fileNameField.text)) {
        val item = createMenuItem(getTrimmedName(file.name))
        item.addListener(new ChangeListener() {
          override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit = {
            chooser.highlightFiles(file)
          }
        })
        addItem(item)
        suggestions += 1
      }

      if (suggestions == AbstractSuggestionPopup.MAX_SUGGESTIONS) {
        return suggestions // @nowarn -- early return
      }
    }

    if (chooser.getMode == FileChooser.Mode.SAVE && suggestions == 0
      && chooser.getActiveFileTypeFilterRule != null // @nowarn -- Java interop boundary
      && fileNameField.text.matches(".*\\.")) {
      val rule = chooser.getActiveFileTypeFilterRule

      val extIter = rule.getExtensions.iterator
      while (extIter.hasNext) {
        val extension     = extIter.next()
        val arbitraryPath = fileNameField.text + extension
        val item          = createMenuItem(arbitraryPath)
        item.addListener(new ChangeListener() {
          override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit = {
            fileNameField.setText(arbitraryPath)
            fileNameField.setCursorPosition(fileNameField.text.length)
          }
        })
        addItem(item)
        suggestions += 1
      }
    }

    suggestions
  }

  private def getTrimmedName(name: String): String = {
    if (name.length > 40) name.substring(0, 40) + "..."
    else name
  }
}
