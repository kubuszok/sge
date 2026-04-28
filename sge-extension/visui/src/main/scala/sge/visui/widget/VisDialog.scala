/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Nathan Sweet, Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 253
 * Covenant-baseline-methods: VisDialog,button,buttonTable,cancel,cancelHide,changed,contentTable,focusChanged,focusListener,getButtonsTable,getContentTable,hide,ignoreTouchDown,initialize,key,keyDown,keyboardFocusChanged,previousKeyboardFocus,previousScrollFocus,result,scrollFocusChanged,setObject,setStage,show,skin,text,this,touchDown,values
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/VisDialog.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
 */
package sge
package visui
package widget

import scala.collection.mutable
import scala.language.implicitConversions

import sge.Input.Key
import sge.math.Interpolation
import sge.scenes.scene2d.{ Action, Actor, InputEvent, InputListener, Stage }
import sge.scenes.scene2d.actions.Actions
import sge.scenes.scene2d.ui.{ Button, Label, Skin, Table }
import sge.scenes.scene2d.ui.Window.WindowStyle
import sge.scenes.scene2d.utils.{ ChangeListener, FocusListener }
import sge.utils.{ Nullable, Seconds }

/** Displays a dialog, which is a modal window containing a content table with a button table underneath it.
  *
  * Due to scope of changes made this widget is not compatible with standard Dialog.
  * @author
  *   Nathan Sweet, Kotcrab
  */
class VisDialog(title: String, windowStyle: WindowStyle)(using Sge) extends VisWindow(title, windowStyle) {

  var contentTable: Table = scala.compiletime.uninitialized
  var buttonTable:  Table = scala.compiletime.uninitialized
  private var skin: Skin  = scala.compiletime.uninitialized

  val values:                        mutable.Map[Actor, Nullable[AnyRef]] = mutable.Map.empty
  var cancelHide:                    Boolean                              = false
  private var previousKeyboardFocus: Nullable[Actor]                      = Nullable.empty
  private var previousScrollFocus:   Nullable[Actor]                      = Nullable.empty
  private var focusListener:         FocusListener                        = scala.compiletime.uninitialized

  protected val ignoreTouchDown: InputListener = new InputListener() {
    override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: sge.Input.Button): Boolean = {
      event.cancel()
      false
    }
  }

  skin = VisUI.getSkin
  setSkin(Nullable(skin))
  initialize()

  def this(title: String)(using Sge) = this(title, VisUI.getSkin.get[WindowStyle])

  def this(title: String, windowStyleName: String)(using Sge) = this(title, VisUI.getSkin.get[WindowStyle](windowStyleName))

  private def initialize(): Unit = {
    isModal = true
    titleLabel.setAlignment(VisUI.defaultTitleAlign)

    defaults().space(6)
    contentTable = new Table(Nullable(skin))
    add(Nullable[Actor](contentTable)).expand().fill()
    row()
    buttonTable = new Table(Nullable(skin))
    add(Nullable[Actor](buttonTable))

    contentTable.defaults().space(2).padLeft(3).padRight(3)
    buttonTable.defaults().space(6).padBottom(3)

    buttonTable.addListener(
      new ChangeListener() {
        override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit =
          if (values.contains(actor)) {
            var a = actor
            while (!a.parent.exists(_ eq buttonTable)) a = a.parent.get
            result(values.getOrElse(a, Nullable.empty))
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
        VisDialog.this.stage.foreach { s =>
          if (
            isModal && s.root.children.size > 0
            && (s.root.children.peek.eq(VisDialog.this))
          ) {
            event.relatedActor.foreach { nfa =>
              if (!nfa.isDescendantOf(VisDialog.this)) event.cancel()
            }
          }
        }
    }
  }

  override protected[sge] def setStage(stageArg: Nullable[Stage]): Unit = {
    if (stageArg.isEmpty) addListener(focusListener)
    else removeListener(focusListener)
    super.setStage(stageArg)
  }

  def getContentTable: Table = contentTable
  def getButtonsTable: Table = buttonTable

  /** Adds a label to the content table. */
  def text(text: String): VisDialog = this.text(text, skin.get[Label.LabelStyle])

  /** Adds a label to the content table. */
  def text(text: String, labelStyle: Label.LabelStyle): VisDialog = this.text(new Label(Nullable(text: CharSequence), labelStyle))

  /** Adds the given Label to the content table. */
  def text(label: Label): VisDialog = {
    contentTable.add(Nullable[Actor](label))
    this
  }

  /** Adds a text button to the button table. Null will be passed to [[result]] if this button is clicked. */
  def button(text: String): VisDialog = button(text, Nullable.empty[AnyRef])

  /** Adds a text button to the button table.
    * @param obj
    *   The object that will be passed to [[result]] if this button is clicked. May be null.
    */
  def button(text: String, obj: Nullable[AnyRef]): VisDialog =
    button(text, obj, skin.get[VisTextButton.VisTextButtonStyle])

  /** Adds a text button to the button table.
    * @param obj
    *   The object that will be passed to [[result]] if this button is clicked. May be null.
    */
  def button(text: String, obj: Nullable[AnyRef], buttonStyle: VisTextButton.VisTextButtonStyle): VisDialog =
    button(new VisTextButton(text, buttonStyle), obj)

  /** Adds the given button to the button table. */
  def button(button: Button): VisDialog = this.button(button, Nullable.empty[AnyRef])

  /** Adds the given button to the button table.
    * @param obj
    *   The object that will be passed to [[result]] if this button is clicked. May be null.
    */
  def button(button: Button, obj: Nullable[AnyRef]): VisDialog = {
    buttonTable.add(Nullable[Actor](button))
    setObject(button, obj)
    this
  }

  /** Packs the dialog and adds it to the stage with custom action which can be null for instant show. */
  def show(stage: Stage, action: Nullable[Action]): VisDialog = {
    clearActions()
    removeCaptureListener(ignoreTouchDown)

    previousKeyboardFocus = Nullable.empty
    stage.keyboardFocus.foreach { actor =>
      if (!actor.isDescendantOf(this)) previousKeyboardFocus = Nullable(actor)
    }

    previousScrollFocus = Nullable.empty
    stage.scrollFocus.foreach { actor =>
      if (!actor.isDescendantOf(this)) previousScrollFocus = Nullable(actor)
    }

    pack()
    stage.addActor(this)
    stage.setKeyboardFocus(Nullable[Actor](this))
    stage.setScrollFocus(Nullable[Actor](this))
    action.foreach(addAction)

    this
  }

  /** Packs the dialog and adds it to the stage, centered with default fadeIn action. */
  def show(stage: Stage): VisDialog = {
    show(stage, Nullable(Actions.sequence(Actions.alpha(0), Actions.fadeIn(Seconds(0.4f), Nullable(Interpolation.fade)))))
    setPosition(Math.round((stage.width - this.width) / 2).toFloat, Math.round((stage.height - this.height) / 2).toFloat)
    this
  }

  /** Hides the dialog with the given action and then removes it from the stage. */
  def hide(action: Nullable[Action]): Unit = {
    this.stage.foreach { s =>
      removeListener(focusListener)
      previousKeyboardFocus.foreach { pkf =>
        if (pkf.stage.isEmpty) previousKeyboardFocus = Nullable.empty
      }
      val actor = s.keyboardFocus
      if (actor.isEmpty || actor.exists(_.isDescendantOf(this))) s.setKeyboardFocus(previousKeyboardFocus)

      previousScrollFocus.foreach { psf =>
        if (psf.stage.isEmpty) previousScrollFocus = Nullable.empty
      }
      val scrollActor = s.scrollFocus
      if (scrollActor.isEmpty || scrollActor.exists(_.isDescendantOf(this))) s.setScrollFocus(previousScrollFocus)
    }
    if (action.isEmpty) {
      remove()
    } else {
      addCaptureListener(ignoreTouchDown)
      addAction(Actions.sequence(action.get, Actions.removeListener(ignoreTouchDown, true), Actions.removeActor()))
    }
  }

  /** Hides the dialog. Called automatically when a button is clicked. The default implementation fades out the dialog over 400 milliseconds and then removes it from the stage.
    */
  def hide(): Unit =
    hide(
      Nullable(
        Actions.sequence(
          Actions.fadeOut(Seconds(VisWindow.FADE_TIME), Nullable(Interpolation.fade)),
          Actions.removeListener(ignoreTouchDown, true),
          Actions.removeActor()
        )
      )
    )

  def setObject(actor: Actor, obj: Nullable[AnyRef]): Unit = values(actor) = obj

  /** If this key is pressed, [[result]] is called with the specified object.
    * @see
    *   [[Key]]
    */
  def key(keycode: Key, obj: Nullable[AnyRef]): VisDialog = {
    addListener(
      new InputListener() {
        override def keyDown(event: InputEvent, keycode2: Key): Boolean = {
          if (keycode == keycode2) {
            result(obj)
            if (!cancelHide) hide()
            cancelHide = false
          }
          false
        }
      }
    )
    this
  }

  /** Called when a button is clicked. The dialog will be hidden after this method returns unless [[cancel()]] is called.
    * @param obj
    *   The object specified when the button was added.
    */
  protected def result(obj: Nullable[AnyRef]): Unit = {}

  def cancel(): Unit = cancelHide = true
}
