/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/ui/Dialog.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package scenes
package scene2d
package ui

import scala.collection.mutable

import sge.math.Interpolation
import sge.scenes.scene2d.{ Action, Actor, InputEvent, InputListener, Stage }
import sge.scenes.scene2d.actions.Actions
import sge.scenes.scene2d.ui.Label.LabelStyle
import sge.scenes.scene2d.ui.TextButton.TextButtonStyle
import sge.scenes.scene2d.ui.Window.WindowStyle
import sge.scenes.scene2d.utils.{ ChangeListener, FocusListener }
import sge.utils.Nullable

/** Displays a dialog, which is a window with a title, a content table, and a button table. Methods are provided to add a label to the content table and buttons to the button table, but any widgets
  * can be added. When a button is clicked, {@link #result(Object)} is called and the dialog is removed from the stage.
  * @author
  *   Nathan Sweet
  */
class Dialog(title: String, windowStyle: WindowStyle)(using Sge) extends Window(title, windowStyle) {

  var contentTable:          Table                                = scala.compiletime.uninitialized
  var buttonTable:           Table                                = scala.compiletime.uninitialized
  private var skin:          Nullable[Skin]                       = Nullable.empty
  val values:                mutable.Map[Actor, Nullable[AnyRef]] = mutable.Map.empty
  var cancelHide:            Boolean                              = false
  var previousKeyboardFocus: Nullable[Actor]                      = Nullable.empty
  var previousScrollFocus:   Nullable[Actor]                      = Nullable.empty
  var focusListener:         FocusListener                        = scala.compiletime.uninitialized

  protected val ignoreTouchDown: InputListener = new InputListener() {
    override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean = {
      event.cancel()
      false
    }
  }

  def this(title: String, skin: Skin)(using Sge) = {
    this(title, skin.get(classOf[Window.WindowStyle]))
    this.skin = Nullable(skin)
  }
  def this(title: String, skin: Skin, windowStyleName: String)(using Sge) = {
    this(title, skin.get(windowStyleName, classOf[Window.WindowStyle]))
    this.skin = Nullable(skin)
  }

  initialize()

  private def initialize(): Unit = {
    setModal(true)

    defaults().space(6)
    contentTable = new Table(skin.map(s => s: Any))
    add(Nullable[Actor](contentTable)).grow()
    row()
    buttonTable = new Table(skin.map(s => s: Any))
    add(Nullable[Actor](buttonTable)).fillX()

    contentTable.defaults().space(6)
    buttonTable.defaults().space(6)

    buttonTable.addListener(
      new ChangeListener() {
        override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit =
          if (values.contains(actor)) {
            var current = actor
            while (current.getParent.fold(false)(_ ne buttonTable))
              current.getParent.foreach { p => current = p }
            result(values.getOrElse(current, Nullable.empty))
            if (!cancelHide) hide()
            cancelHide = false
          }
      }
    )

    focusListener = new FocusListener() {
      override def keyboardFocusChanged(event: FocusListener.FocusEvent, actor: Actor, focused: Boolean): Unit =
        if (!focused) focusChanged(event)

      override def scrollFocusChanged(event: FocusListener.FocusEvent, actor: Actor, focused: Boolean): Unit =
        if (!focused) focusChanged(event)

      private def focusChanged(event: FocusListener.FocusEvent): Unit =
        getStage.foreach { stage =>
          if (
            isModal && stage.getRoot.getChildren.nonEmpty
            && (stage.getRoot.getChildren.last eq Dialog.this)
          ) { // Dialog is top most actor.
            val newFocusedActor = event.getRelatedActor
            newFocusedActor.foreach { nfa =>
              if (
                !nfa.isDescendantOf(Dialog.this)
                && !(previousKeyboardFocus.fold(false)(_ eq nfa) || previousScrollFocus.fold(false)(_ eq nfa))
              ) {
                event.cancel()
              }
            }
          }
        }
    }
  }

  override protected[scene2d] def setStage(stage: Nullable[Stage]): Unit = {
    if (stage.isEmpty)
      addListener(focusListener)
    else
      removeListener(focusListener)
    super.setStage(stage)
  }

  def getContentTable: Table = contentTable

  def getButtonTable: Table = buttonTable

  /** Adds a label to the content table. The dialog must have been constructed with a skin to use this method. */
  def text(text: Nullable[String]): Dialog = {
    val s = skin.getOrElse(throw new IllegalStateException("This method may only be used if the dialog was constructed with a Skin."))
    this.text(text, s.get(classOf[LabelStyle]))
    this
  }

  /** Adds a label to the content table. */
  def text(text: Nullable[String], labelStyle: LabelStyle): Dialog =
    this.text(new Label(text.map(_.asInstanceOf[CharSequence]), labelStyle))

  /** Adds the given Label to the content table */
  def text(label: Label): Dialog = {
    contentTable.add(Nullable[Actor](label))
    this
  }

  /** Adds a text button to the button table. Null will be passed to {@link #result(Object)} if this button is clicked. The dialog must have been constructed with a skin to use this method.
    */
  def button(text: Nullable[String]): Dialog =
    button(text, Nullable.empty[AnyRef])

  /** Adds a text button to the button table. The dialog must have been constructed with a skin to use this method.
    * @param obj
    *   The object that will be passed to {@link #result(Object)} if this button is clicked. May be null.
    */
  def button(text: Nullable[String], obj: Nullable[AnyRef]): Dialog = {
    val s = skin.getOrElse(throw new IllegalStateException("This method may only be used if the dialog was constructed with a Skin."))
    this.button(text, obj, s.get(classOf[TextButtonStyle]))
    this
  }

  /** Adds a text button to the button table.
    * @param obj
    *   The object that will be passed to {@link #result(Object)} if this button is clicked. May be null.
    */
  def button(text: Nullable[String], obj: Nullable[AnyRef], buttonStyle: TextButtonStyle): Dialog =
    button(new TextButton(text, buttonStyle), obj)

  /** Adds the given button to the button table. */
  def button(button: Button): Dialog =
    this.button(button, Nullable.empty[AnyRef])

  /** Adds the given button to the button table.
    * @param obj
    *   The object that will be passed to {@link #result(Object)} if this button is clicked. May be null.
    */
  def button(button: Button, obj: Nullable[AnyRef]): Dialog = {
    buttonTable.add(Nullable[Actor](button))
    setObject(button, obj)
    this
  }

  /** {@link #pack() Packs} the dialog (but doesn't set the position), adds it to the stage, sets it as the keyboard and scroll focus, clears any actions on the dialog, and adds the specified action
    * to it. The previous keyboard and scroll focus are remembered so they can be restored when the dialog is hidden.
    * @param action
    *   May be null.
    */
  def show(stage: Stage, action: Nullable[Action]): Dialog = {
    clearActions()
    removeCaptureListener(ignoreTouchDown)

    previousKeyboardFocus = Nullable.empty
    val kbActor = stage.getKeyboardFocus
    kbActor.foreach { a =>
      if (!a.isDescendantOf(this)) previousKeyboardFocus = Nullable(a)
    }

    previousScrollFocus = Nullable.empty
    val scrollActor = stage.getScrollFocus
    scrollActor.foreach { a =>
      if (!a.isDescendantOf(this)) previousScrollFocus = Nullable(a)
    }

    stage.addActor(this)
    pack()
    stage.cancelTouchFocus()
    stage.setKeyboardFocus(Nullable[Actor](this))
    stage.setScrollFocus(Nullable[Actor](this))
    action.foreach(addAction)

    this
  }

  /** Centers the dialog in the stage and calls {@link #show(Stage, Action)} with a {@link Actions#fadeIn(float, Interpolation)} action.
    */
  def show(stage: Stage): Dialog = {
    show(stage, Nullable(Actions.sequence(Actions.alpha(0), Actions.fadeIn(0.4f, Nullable(Interpolation.fade)))))
    setPosition(Math.round((stage.getWidth - getWidth) / 2).toFloat, Math.round((stage.getHeight - getHeight) / 2).toFloat)
    this
  }

  /** Removes the dialog from the stage, restoring the previous keyboard and scroll focus, and adds the specified action to the dialog.
    * @param action
    *   If null, the dialog is removed immediately. Otherwise, the dialog is removed when the action completes. The dialog will not respond to touch down events during the action.
    */
  def hide(action: Nullable[Action]): Unit = {
    getStage.foreach { stage =>
      removeListener(focusListener)
      previousKeyboardFocus.foreach { pkf =>
        if (pkf.getStage.isEmpty) previousKeyboardFocus = Nullable.empty
      }
      val kbActor = stage.getKeyboardFocus
      if (kbActor.isEmpty || kbActor.fold(false)(_.isDescendantOf(this))) stage.setKeyboardFocus(previousKeyboardFocus)

      previousScrollFocus.foreach { psf =>
        if (psf.getStage.isEmpty) previousScrollFocus = Nullable.empty
      }
      val scrollActor = stage.getScrollFocus
      if (scrollActor.isEmpty || scrollActor.fold(false)(_.isDescendantOf(this))) stage.setScrollFocus(previousScrollFocus)
    }
    action.fold {
      remove()
      ()
    } { a =>
      addCaptureListener(ignoreTouchDown)
      addAction(Actions.sequence(a, Actions.removeListener(ignoreTouchDown, true), Actions.removeActor()))
    }
  }

  /** Hides the dialog. Called automatically when a button is clicked. The default implementation fades out the dialog over 400 milliseconds.
    */
  def hide(): Unit =
    hide(Nullable(Actions.fadeOut(0.4f, Nullable(Interpolation.fade))))

  def setObject(actor: Actor, obj: Nullable[AnyRef]): Unit =
    values(actor) = obj

  /** If this key is pressed, {@link #result(Object)} is called with the specified object.
    * @see
    *   Keys
    */
  def key(keycode: Int, obj: Nullable[AnyRef]): Dialog = {
    addListener(
      new InputListener() {
        override def keyDown(event: InputEvent, keycode2: Int): Boolean = {
          if (keycode == keycode2) {
            // Delay a frame to eat the keyTyped event.
            Sge().application.postRunnable(new Runnable() {
              def run(): Unit = {
                result(obj)
                if (!cancelHide) hide()
                cancelHide = false
              }
            })
          }
          false
        }
      }
    )
    this
  }

  /** Called when a button is clicked. The dialog will be hidden after this method returns unless {@link #cancel()} is called.
    * @param obj
    *   The object specified when the button was added.
    */
  protected def result(obj: Nullable[AnyRef]): Unit = {}

  def cancel(): Unit =
    cancelHide = true
}
