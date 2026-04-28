/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 115
 * Covenant-baseline-methods: Toast,ToastStyle,_mainTable,_toastManager,act,background,changed,close,closeButton,closeButtonStyle,createMainTable,fadeIn,fadeOut,mainTable,this,toast,toastManager,toastManager_
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/toast/Toast.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
 */
package sge
package visui
package widget
package toast

import scala.language.implicitConversions

import sge.math.Interpolation
import sge.scenes.scene2d.{ Action, Actor }
import sge.scenes.scene2d.actions.Actions
import sge.scenes.scene2d.ui.Table
import sge.scenes.scene2d.utils.{ ChangeListener, Drawable }
import sge.utils.{ Nullable, Seconds }
import sge.visui.VisUI
import sge.visui.util.ToastManager

/** Base class for all toasts. Toast is a wrapper around actual toast content table. It has close button and reference to [[ToastManager]]. To create your own toast you should generally extend
  * [[ToastTable]] class.
  *
  * If you want further customization and modify other aspects of toast (such as close button) override [[createMainTable]].
  * @author
  *   Kotcrab
  * @see
  *   [[MessageToast]]
  * @see
  *   [[ToastTable]]
  * @since 1.1.0
  */
class Toast(style: Toast.ToastStyle, val contentTable: Table)(using Sge) {

  private var _toastManager: Nullable[ToastManager] = Nullable.empty

  contentTable match {
    case tt: ToastTable => tt.setToast(this)
    case _ =>
  }

  private var _mainTable: Table = scala.compiletime.uninitialized

  createMainTable()

  def this(content: Table)(using Sge) = this(VisUI.getSkin.get[Toast.ToastStyle], content)

  def this(styleName: String, content: Table)(using Sge) = this(VisUI.getSkin.get[Toast.ToastStyle](styleName), content)

  protected def createMainTable(): Unit = {
    _mainTable = new VisTable()
    _mainTable.setBackground(Nullable(style.background))

    val closeButton = new VisImageButton(style.closeButtonStyle)
    closeButton.addListener(new ChangeListener() {
      override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit =
        close()
    })

    _mainTable.add(Nullable[Actor](contentTable)).pad(3).fill().expand()
    _mainTable.add(Nullable[Actor](closeButton)).top()
  }

  /** Called when close button was pressed by default call [[fadeOut]] */
  protected def close(): Unit = fadeOut()

  def fadeOut(): Unit = {
    val toast = this
    _mainTable.addAction(
      Actions.sequence(
        Actions.fadeOut(Seconds(VisWindow.FADE_TIME), Interpolation.fade),
        new Action() {
          override def act(delta: Seconds): Boolean = {
            toast._toastManager.get.remove(toast)
            true
          }
        }
      )
    )
  }

  def fadeIn(): Table = {
    _mainTable.color.set(1, 1, 1, 0)
    _mainTable.addAction(Actions.fadeIn(Seconds(VisWindow.FADE_TIME), Interpolation.fade))
    _mainTable
  }

  def mainTable: Table = _mainTable

  def toastManager_=(toastManager: ToastManager): Unit                   = _toastManager = Nullable(toastManager)
  def toastManager:                               Nullable[ToastManager] = _toastManager
}

object Toast {

  class ToastStyle {
    var background:       Drawable                           = scala.compiletime.uninitialized
    var closeButtonStyle: VisImageButton.VisImageButtonStyle = scala.compiletime.uninitialized

    def this(style: ToastStyle) = {
      this()
      this.background = style.background
      this.closeButtonStyle = style.closeButtonStyle
    }

    def this(background: Drawable, closeButtonStyle: VisImageButton.VisImageButtonStyle) = {
      this()
      this.background = background
      this.closeButtonStyle = closeButtonStyle
    }
  }
}
