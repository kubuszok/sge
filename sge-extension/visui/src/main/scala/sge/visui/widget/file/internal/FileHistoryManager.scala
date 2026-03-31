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

import sge.Input.Buttons
import sge.files.FileHandle
import sge.scenes.scene2d.{ Actor, InputEvent, Stage }
import sge.scenes.scene2d.utils.{ ChangeListener, ClickListener }
import sge.utils.DynamicArray
import sge.visui.util.dialog.Dialogs
import sge.visui.widget.{ VisImageButton, VisTable }

/** Manages [[FileChooser]] history of directories that user navigated into. This is internal VisUI API however this class is also reused by VisEditor.
  * @author
  *   Kotcrab
  */
class FileHistoryManager(style: FileChooserStyle, callback: FileHistoryManager.FileHistoryCallback)(using Sge) {

  private val history:         DynamicArray[FileHandle] = DynamicArray[FileHandle]()
  private val _historyForward: DynamicArray[FileHandle] = DynamicArray[FileHandle]()

  private val buttonsTable:  VisTable       = new VisTable(true)
  private val backButton:    VisImageButton = new VisImageButton(style.iconArrowLeft.get)
  private val forwardButton: VisImageButton = new VisImageButton(style.iconArrowRight.get)

  {
    backButton.setGenerateDisabledImage(true)
    backButton.disabled = true
    forwardButton.setGenerateDisabledImage(true)
    forwardButton.disabled = true

    buttonsTable.add(backButton)
    buttonsTable.add(forwardButton)

    backButton.addListener(new ChangeListener() {
      override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit = historyBack()
    })

    forwardButton.addListener(new ChangeListener() {
      override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit = historyForward()
    })
  }

  def getDefaultClickListener: ClickListener = new ClickListener() {
    override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: sge.Input.Button): Boolean =
      if (button == Buttons.BACK || button == Buttons.FORWARD) true
      else super.touchDown(event, x, y, pointer, button)

    override def touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: sge.Input.Button): Unit =
      if (button == Buttons.BACK && hasHistoryBack) historyBack()
      else if (button == Buttons.FORWARD && hasHistoryForward) historyForward()
      else super.touchUp(event, x, y, pointer, button)
  }

  def getButtonsTable: VisTable = buttonsTable

  def historyClear(): Unit = {
    history.clear()
    _historyForward.clear()
    forwardButton.disabled = true
    backButton.disabled = true
  }

  def historyAdd(): Unit = {
    history.add(callback.getCurrentDirectory)
    _historyForward.clear()
    backButton.disabled = false
    forwardButton.disabled = true
  }

  def historyBack(): Unit = {
    val dir = history.pop()
    _historyForward.add(callback.getCurrentDirectory)
    if (!setDirectoryFromHistory(dir)) _historyForward.pop()
    if (!hasHistoryBack) backButton.disabled = true
    forwardButton.disabled = false
  }

  def historyForward(): Unit = {
    val dir = _historyForward.pop()
    history.add(callback.getCurrentDirectory)
    if (!setDirectoryFromHistory(dir)) history.pop()
    if (!hasHistoryForward) forwardButton.disabled = true
    backButton.disabled = false
  }

  private def setDirectoryFromHistory(dir: FileHandle): Boolean =
    if (dir.exists()) {
      callback.setDirectory(dir, FileChooser.HistoryPolicy.IGNORE)
      true
    } else {
      Dialogs.showErrorDialog(callback.getHistoryStage, FileChooserText.DIRECTORY_NO_LONGER_EXISTS.get)
      false
    }

  /** @return returns `true` if a forward-history is available */
  private def hasHistoryForward: Boolean = _historyForward.size != 0

  /** @return returns `true` if a back-history is available */
  private def hasHistoryBack: Boolean = history.size != 0
}

object FileHistoryManager {
  trait FileHistoryCallback {
    def getCurrentDirectory:                                                    FileHandle
    def setDirectory(directory: FileHandle, policy: FileChooser.HistoryPolicy): Unit
    def getHistoryStage:                                                        Stage
  }
}
