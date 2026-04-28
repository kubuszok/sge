/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 132
 * Covenant-baseline-methods: DirsSuggestionPopup,changed,createDirSuggestions,createRecentDirSuggestions,iter,listDirExecutor,listDirFuture,pathFieldKeyTyped,pathFieldText,run,showRecentDirectories,suggestions
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/file/internal/DirsSuggestionPopup.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
 */
package sge
package visui
package widget
package file
package internal

import java.util.concurrent.{ ExecutorService, Executors, Future }

import sge.files.FileHandle
import sge.scenes.scene2d.{ Actor, Stage }
import sge.scenes.scene2d.actions.Actions
import sge.scenes.scene2d.utils.ChangeListener
import sge.utils.{ DynamicArray, Seconds }
import sge.visui.widget.VisTextField

/** @author Kotcrab */
class DirsSuggestionPopup(chooser: FileChooser, pathField: VisTextField)(using sge: Sge) extends AbstractSuggestionPopup(chooser) {

  private val listDirExecutor: ExecutorService = Executors.newSingleThreadExecutor(new ServiceThreadFactory("FileChooserListDirThread"))
  private var listDirFuture:   Future[?]       = scala.compiletime.uninitialized

  def pathFieldKeyTyped(stage: Stage, width: Float): Unit =
    if (pathField.text.length == 0) {
      remove()
    } else {
      createDirSuggestions(stage, width)
    }

  private def createDirSuggestions(stage: Stage, width: Float): Unit = {
    val pathFieldText = pathField.text
    // quiet period before listing files takes too long and popup will be removed
    addAction(Actions.sequence(Actions.delay(Seconds(0.2f), Actions.removeActor())))

    if (listDirFuture != null) listDirFuture.cancel(true) // @nowarn -- Java interop boundary
    listDirFuture = listDirExecutor.submit(
      new Runnable {
        override def run(): Unit = {
          val enteredDir             = Sge().files.absolute(pathFieldText)
          val (listDir, partialPath) =
            if (enteredDir.exists()) (enteredDir, "")
            else (enteredDir.parent(), enteredDir.name)

          val files = listDir.list(chooser.getFileFilter)
          if (Thread.currentThread().isInterrupted) { return; } // @nowarn -- Java interop boundary for early return
          Sge().application.postRunnable(
            new Runnable {
              override def run(): Unit = scala.util.boundary {
                clearChildren()
                clearActions()
                var suggestions = 0

                for (file <- files)
                  if (
                    file.exists() && file.isDirectory() &&
                    file.name.startsWith(partialPath) && !file.name.equals(partialPath)
                  ) {
                    val item = createMenuItem(file.path)
                    item.getLabel.setEllipsis(true)
                    item.getLabelCell.foreach(_.width(width - 20))
                    addItem(item)

                    item.addListener(
                      new ChangeListener() {
                        override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit =
                          chooser.setDirectory(file, FileChooser.HistoryPolicy.ADD)
                      }
                    )

                    suggestions += 1
                    if (suggestions == AbstractSuggestionPopup.MAX_SUGGESTIONS) {
                      scala.util.boundary.break(())
                    }
                  }

                if (suggestions == 0) {
                  remove()
                } else {
                  showMenu(stage, pathField)
                  setWidth(width)
                  layout()
                }
              }
            }
          )
        }
      }
    )
  }

  def showRecentDirectories(stage: Stage, recentDirectories: DynamicArray[FileHandle], width: Float): Unit = {
    val suggestions = createRecentDirSuggestions(recentDirectories, width)
    if (suggestions == 0) {
      remove()
    } else {
      showMenu(stage, pathField)
      setWidth(width)
      layout()
    }
  }

  private def createRecentDirSuggestions(files: DynamicArray[FileHandle], width: Float): Int = {
    clearChildren()
    var suggestions = 0
    val iter        = files.iterator
    while (iter.hasNext && suggestions < AbstractSuggestionPopup.MAX_SUGGESTIONS) {
      val file = iter.next()
      if (file.exists()) {
        val item = createMenuItem(file.path)
        item.getLabel.setEllipsis(true)
        item.getLabelCell.foreach(_.width(width - 20))
        addItem(item)

        item.addListener(
          new ChangeListener() {
            override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit =
              chooser.setDirectory(file, FileChooser.HistoryPolicy.ADD)
          }
        )

        suggestions += 1
      }
    }
    suggestions
  }
}
