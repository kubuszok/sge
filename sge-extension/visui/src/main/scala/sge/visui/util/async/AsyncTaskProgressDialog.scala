/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 65
 * Covenant-baseline-methods: AsyncTaskProgressDialog,addListener,failed,finished,getStatus,messageChanged,progressBar,progressChanged,statusLabel
 * Covenant-source-reference: com/kotcrab/vis/ui/util/async/AsyncTaskProgressDialog.java
 * Covenant-verified: 2026-04-19
 */
package sge
package visui
package util
package async

import scala.language.implicitConversions

import sge.scenes.scene2d.Actor
import sge.utils.Nullable
import sge.visui.Locales.CommonText
import sge.visui.util.TableUtils
import sge.visui.util.dialog.Dialogs
import sge.visui.widget.{ VisLabel, VisProgressBar, VisWindow }

/** Dialog used to display progress of [[AsyncTask]] as standard VisUI window. Shows progress bar and status of currently executed task.
  * @author
  *   Kotcrab
  */
class AsyncTaskProgressDialog(title: String, val task: AsyncTask)(using Sge) extends VisWindow(title) {

  isModal = true
  TableUtils.setSpacingDefaults(this)

  private val statusLabel: VisLabel       = new VisLabel(CommonText.PleaseWait.get)
  private val progressBar: VisProgressBar = new VisProgressBar(0, 100, 1, false)

  defaults().padLeft(6).padRight(6)

  add(Nullable[Actor](statusLabel)).padTop(6).left().row()
  add(Nullable[Actor](progressBar)).width(300).padTop(6).padBottom(6)

  task.addListener(
    new AsyncTaskListener {
      override def progressChanged(newProgressPercent: Int): Unit =
        progressBar.setValue(newProgressPercent.toFloat)

      override def messageChanged(message: String): Unit =
        statusLabel.setText(message)

      override def finished(): Unit =
        fadeOut()

      override def failed(message: String, exception: Exception): Unit = {
        val msg = if (exception.getMessage == null) CommonText.UnknownErrorOccured.get else exception.getMessage // @nowarn -- Java interop
        Dialogs.showErrorDialog(stage.get, msg, exception)
      }
    }
  )

  pack()
  centerWindow()

  task.execute()

  def addListener(listener: AsyncTaskListener): Unit = task.addListener(listener)

  def getStatus: AsyncTask.Status = task.status
}
