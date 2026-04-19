/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 216
 * Covenant-baseline-methods: ToastManager,UNTIL_CLOSED,alignment,bottom,center,clear,existingToast,getAlignment,getMessagePadding,getScreenPaddingX,getScreenPaddingY,i,idx,left,messagePadding,remove,removed,resize,run,screenPaddingX,screenPaddingY,setAlignment,setMessagePadding,setScreenPadding,setScreenPaddingX,setScreenPaddingY,show,table,this,timersTasks,toFront,toastMainTable,toasts,updateToastsPositions,y
 * Covenant-source-reference: com/kotcrab/vis/ui/util/ToastManager.java
 * Covenant-verified: 2026-04-19
 */
package sge
package visui
package util

import scala.language.implicitConversions

import sge.scenes.scene2d.{ Actor, Group, Stage, Touchable }
import sge.scenes.scene2d.ui.{ Table, WidgetGroup }
import sge.utils.{ Align, Nullable, Seconds, Timer }
import sge.visui.widget.VisTable
import sge.visui.widget.toast.{ Toast, ToastTable }

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

/** Utility for displaying toast messages at corner of application screen (by default top right). Toasts can be closed by users or they can automatically disappear after a period of time. Typically
  * only one instance of ToastManager is used per application window.
  *
  * To properly support window resize [[resize]] must be called when application resize has occurred.
  *
  * Most show methods are taking [[VisTable]] however [[ToastTable]] should be preferred because it provides access to enclosing [[Toast]] instance.
  * @author
  *   Kotcrab
  * @see
  *   [[Toast]]
  * @see
  *   [[ToastTable]]
  * @see
  *   [[MessageToast]]
  * @since 1.1.0
  */
class ToastManager(protected val root: Group)(using Sge) {

  protected var screenPaddingX: Int   = 20
  protected var screenPaddingY: Int   = 20
  protected var messagePadding: Int   = 5
  protected var alignment:      Align = Align.topRight

  protected val toasts:      ArrayBuffer[Toast]         = ArrayBuffer.empty
  protected val timersTasks: HashMap[Toast, Timer.Task] = HashMap.empty

  /** Toast manager will create own group to host toasts and put it into the stage root. */
  def this(stage: Stage)(using Sge) =
    this(
      {
        val widgetGroup = new WidgetGroup()
        widgetGroup.fillParent = true
        widgetGroup.touchable = Touchable.childrenOnly
        stage.addActor(widgetGroup)
        widgetGroup
      }
    )

  /** Displays basic toast with provided text as message. Toast will be displayed until it is closed by user. */
  def show(text: String): Unit = show(text, ToastManager.UNTIL_CLOSED)

  /** Displays basic toast with provided text as message. Toast will be displayed for given amount of seconds. */
  def show(text: String, timeSec: Seconds): Unit = {
    val table = new VisTable()
    table.add(Nullable[Actor](new sge.visui.widget.VisLabel(text))).grow()
    show(table, timeSec)
  }

  /** Displays toast with provided table as toast's content. Toast will be displayed until it is closed by user. */
  def show(table: Table): Unit = show(table, ToastManager.UNTIL_CLOSED)

  /** Displays toast with provided table as toast's content. Toast will be displayed for given amount of seconds. */
  def show(table: Table, timeSec: Seconds): Unit = show(new Toast(table), timeSec)

  /** Displays toast with provided table as toast's content. If this toast was already displayed then it reuses stored [[Toast]] instance. Toast will be displayed until it is closed by user.
    */
  def show(toastTable: ToastTable): Unit = show(toastTable, ToastManager.UNTIL_CLOSED)

  /** Displays toast with provided table as toast's content. If this toast was already displayed then it reuses stored [[Toast]] instance. Toast will be displayed for given amount of seconds.
    */
  def show(toastTable: ToastTable, timeSec: Seconds): Unit = {
    val existingToast = toastTable.getToast
    if (existingToast.isDefined) {
      show(existingToast.get, timeSec)
    } else {
      show(new Toast(toastTable), timeSec)
    }
  }

  /** Displays toast. Toast will be displayed until it is closed by user. */
  def show(toastObj: Toast): Unit = show(toastObj, ToastManager.UNTIL_CLOSED)

  /** Displays toast. Toast will be displayed for given amount of seconds. */
  def show(toastObj: Toast, timeSec: Seconds): Unit = {
    val toastMainTable = toastObj.mainTable
    if (toastMainTable.stage.isDefined) {
      remove(toastObj)
    }
    toasts += toastObj

    toastObj.toastManager = this
    toastObj.fadeIn()
    toastMainTable.pack()
    root.addActor(toastMainTable)

    updateToastsPositions()

    if (timeSec > Seconds.zero) {
      val fadeOutTask = new Timer.Task() {
        override def run(): Unit = {
          toastObj.fadeOut()
          timersTasks.remove(toastObj)
        }
      }
      timersTasks.put(toastObj, fadeOutTask)
      Timer.schedule(fadeOutTask, timeSec)
    }
  }

  /** Must be called after application window resize to properly update toast positions on screen. */
  def resize(): Unit = updateToastsPositions()

  /** Removes toast from screen.
    * @return
    *   true when toast was removed, false otherwise
    */
  def remove(toastObj: Toast): Boolean = {
    val idx     = toasts.indexOf(toastObj)
    val removed = idx >= 0
    if (removed) {
      toasts.remove(idx)
      toastObj.mainTable.remove()
      timersTasks.remove(toastObj).foreach(_.cancel())
      updateToastsPositions()
    }
    removed
  }

  def clear(): Unit = {
    toasts.foreach(_.mainTable.remove())
    toasts.clear()
    timersTasks.valuesIterator.foreach(_.cancel())
    timersTasks.clear()
    updateToastsPositions()
  }

  def toFront(): Unit = root.toFront()

  protected def updateToastsPositions(): Unit = {
    val bottom = alignment.isBottom
    val left   = alignment.isLeft
    val center = alignment.isCenter
    var y      = if (bottom) screenPaddingY.toFloat else root.height - screenPaddingY

    var i = 0
    while (i < toasts.size) {
      val toastObj = toasts(i)
      val table    = toastObj.mainTable
      val tableX   =
        if (left) screenPaddingX.toFloat
        else if (center) (root.width - table.width - screenPaddingX) / 2f
        else root.width - table.width - screenPaddingX

      table.setPosition(tableX, if (bottom) y else y - table.height)
      y += (table.height + messagePadding) * (if (bottom) 1 else -1)
      i += 1
    }
  }

  def getScreenPaddingX: Int = screenPaddingX
  def getScreenPaddingY: Int = screenPaddingY

  /** Sets padding of a message from window corner (actual corner used depends on current alignment settings). */
  def setScreenPadding(padding: Int): Unit = {
    screenPaddingX = padding
    screenPaddingY = padding
    updateToastsPositions()
  }

  def setScreenPadding(paddingX: Int, paddingY: Int): Unit = {
    screenPaddingX = paddingX
    screenPaddingY = paddingY
    updateToastsPositions()
  }

  def setScreenPaddingX(padding: Int): Unit = {
    screenPaddingX = padding
    updateToastsPositions()
  }

  def setScreenPaddingY(padding: Int): Unit = {
    screenPaddingY = padding
    updateToastsPositions()
  }

  def getMessagePadding: Int = messagePadding

  def setMessagePadding(padding: Int): Unit = {
    messagePadding = padding
    updateToastsPositions()
  }

  def getAlignment: Align = alignment

  /** Sets toast messages screen alignment. By default toasts are displayed in application top right corner */
  def setAlignment(alignment: Align): Unit = {
    this.alignment = alignment
    updateToastsPositions()
  }
}

object ToastManager {
  val UNTIL_CLOSED: Seconds = Seconds(-1f)
}
