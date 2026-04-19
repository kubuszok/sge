/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 190
 * Covenant-baseline-methods: FADE_TIME,VisWindow,act,addCloseButton,centerOnAdd,centerWindow,changed,close,closeButton,closeOnEscape,draw,fadeIn,fadeOut,fadeOutActionRunning,isKeepWithinParent,keepWithinParent,keyDown,keyUp,moveToCenter,setCenterOnAdd,setKeepWithinParent,setPosition,setStage,this,touchDown
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/VisWindow.java
 * Covenant-verified: 2026-04-19
 */
package sge
package visui
package widget

import scala.language.implicitConversions

import sge.Input.Key
import sge.graphics.g2d.Batch
import sge.math.Interpolation
import sge.scenes.scene2d.{ Action, Actor, InputEvent, InputListener, Stage, Touchable }
import sge.scenes.scene2d.actions.Actions
import sge.scenes.scene2d.ui.Window
import sge.scenes.scene2d.ui.Window.WindowStyle
import sge.scenes.scene2d.utils.{ ChangeListener, ClickListener }
import sge.utils.{ Align, Nullable, Seconds }
import sge.visui.{ FocusManager, VisUI }

/** Extends functionality of standard scene2d.ui [[Window]].
  * @author
  *   Kotcrab
  * @see
  *   [[Window]]
  */
class VisWindow(title: String, windowStyle: WindowStyle)(using Sge) extends Window(title, windowStyle) {

  private var centerOnAdd:          Boolean = false
  private var keepWithinParent:     Boolean = false
  private var fadeOutActionRunning: Boolean = false

  titleLabel.setAlignment(VisUI.defaultTitleAlign)

  def this(title: String)(using Sge) =
    this(title, VisUI.getSkin.get[WindowStyle])

  def this(title: String, showWindowBorder: Boolean)(using Sge) =
    this(title, VisUI.getSkin.get[WindowStyle](if (showWindowBorder) "default" else "noborder"))

  def this(title: String, styleName: String)(using Sge) =
    this(title, VisUI.getSkin.get[WindowStyle](styleName))

  override def setPosition(x: Float, y: Float): Unit =
    super.setPosition(x.toInt.toFloat, y.toInt.toFloat)

  /** Centers this window, if it has parent it will be done instantly, if it does not have parent it will be centered when it will be added to stage.
    * @return
    *   true when window was centered, false when window will be centered when added to stage
    */
  def centerWindow(): Boolean =
    if (parent.isEmpty) {
      centerOnAdd = true
      false
    } else {
      moveToCenter()
      true
    }

  def setCenterOnAdd(center: Boolean): Unit = centerOnAdd = center

  override protected[sge] def setStage(stageArg: Nullable[Stage]): Unit = {
    super.setStage(stageArg)
    stageArg.foreach { s =>
      s.setKeyboardFocus(Nullable[Actor](this))
      if (centerOnAdd) {
        centerOnAdd = false
        moveToCenter()
      }
    }
  }

  private def moveToCenter(): Unit =
    this.stage.foreach { s =>
      setPosition((s.width - this.width) / 2, (s.height - this.height) / 2)
    }

  /** Fade outs this window, when fade out animation is completed, window is removed from Stage. Calling this for the second time won't have any effect if previous animation is still running.
    */
  def fadeOut(time: Float): Unit =
    if (fadeOutActionRunning) {
      // already running
    } else {
      fadeOutActionRunning = true
      val previousTouchable = touchable
      touchable = Touchable.disabled
      this.stage.foreach { s =>
        s.keyboardFocus.foreach { kf =>
          if (kf.isDescendantOf(this)) FocusManager.resetFocus(Nullable(s))
        }
      }
      addAction(
        Actions.sequence(
          Actions.fadeOut(Seconds(time), Nullable(Interpolation.fade)),
          new Action() {
            override def act(delta: Seconds): Boolean = {
              touchable = previousTouchable
              remove()
              color.a = 1f
              fadeOutActionRunning = false
              true
            }
          }
        )
      )
    }

  /** @return this window for the purpose of chaining methods eg. stage.addActor(new MyWindow(stage).fadeIn(0.3f)) */
  def fadeIn(time: Float): VisWindow = {
    color.set(1, 1, 1, 0)
    addAction(Actions.fadeIn(Seconds(time), Nullable(Interpolation.fade)))
    this
  }

  /** Fade outs this window, when fade out animation is completed, window is removed from Stage. */
  def fadeOut(): Unit = fadeOut(VisWindow.FADE_TIME)

  /** @return this window for the purpose of chaining methods eg. stage.addActor(new MyWindow(stage).fadeIn()) */
  def fadeIn(): VisWindow = fadeIn(VisWindow.FADE_TIME)

  /** Called by window when close button was pressed (added using [[addCloseButton()]]) or escape key was pressed (for close on escape [[closeOnEscape()]] have to be called). Default close behaviour
    * is to fade out window, this can be changed by overriding this function.
    */
  protected def close(): Unit = fadeOut()

  /** Adds close button to window, next to window title. After pressing that button, [[close()]] is called. If nothing else was added to title table, and current title alignment is center then the
    * title will be automatically centered.
    */
  def addCloseButton(): Unit = {
    val closeButton = new VisImageButton("close-window")
    titleTable.add(Nullable[Actor](closeButton)).padRight(-getPadRight + 0.7f)
    closeButton.addListener(new ChangeListener() {
      override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit = close()
    })
    closeButton.addListener(
      new ClickListener() {
        override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: sge.Input.Button): Boolean = {
          event.cancel()
          true
        }
      }
    )

    if (titleLabel.labelAlign == Align.center && titleTable.children.size == 2)
      titleTable.getCell(titleLabel).foreach(_.padLeft(closeButton.width * 2))
  }

  /** Will make this window close when escape key or back key was pressed. After pressing escape or back, [[close()]] is called. */
  def closeOnEscape(): Unit =
    addListener(
      new InputListener() {
        override def keyDown(event: InputEvent, keycode: Key): Boolean =
          if (keycode == Input.Keys.ESCAPE) {
            close()
            true
          } else false

        override def keyUp(event: InputEvent, keycode: Key): Boolean =
          if (keycode == Input.Keys.BACK) {
            close()
            true
          } else false
      }
    )

  def isKeepWithinParent:                 Boolean = keepWithinParent
  def setKeepWithinParent(keep: Boolean): Unit    = keepWithinParent = keep

  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    if (keepWithinParent) {
      parent.foreach { p =>
        val parentWidth  = p.width
        val parentHeight = p.height
        if (this.x < 0) setX(0)
        if (right > parentWidth) setX(parentWidth - this.width)
        if (this.y < 0) setY(0)
        if (top > parentHeight) setY(parentHeight - this.height)
      }
    }
    super.draw(batch, parentAlpha)
  }
}

object VisWindow {
  var FADE_TIME: Float = 0.3f
}
