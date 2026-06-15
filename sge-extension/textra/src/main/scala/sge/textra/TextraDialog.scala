/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/TextraDialog.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: TextraDialog extends TextraWindow (which now extends
 *     sge.scenes.scene2d.ui.Table extends WidgetGroup extends Actor), so the
 *     dialog joins the scene graph and carries the real Actor action/listener/
 *     stage infrastructure. contentTable/buttonTable are real scene2d Tables;
 *     text(...)/typing(...)/button(...) add real Cells. show(Stage)/hide(Action)
 *     use the real scene2d Actions (fadeIn/fadeOut/sequence/alpha/removeListener/
 *     removeActor) and the inherited addAction/clearActions/remove. The
 *     ignoreTouchDown InputListener and the modal FocusListener are real scene2d
 *     listeners; buttonTable carries a real ChangeListener that drives result(..)
 *     and hide(). ObjectMap -> scala.collection.mutable.Map; @Null -> Nullable.
 *   Convention: Dialog show/hide/key/result behavior fully ported.
 *   Idiom: Nullable[A] for nullable fields; boundary/break for early returns.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 413
 * Covenant-baseline-methods: TextraDialog,_skin,btn,button,buttonTable,cancel,cancelHide,changed,contentTable,focusChanged,focusListener,getButtonTable,getContentTable,hide,ignoreTouchDown,initialize,kbActor,key,keyDown,keyboardFocusChanged,newLabel,newTypingLabel,previousKeyboardFocus,previousScrollFocus,result,run,s,scrollActor,scrollFocusChanged,setObject,setStage,show,text,this,touchDown,typing,values
 * Covenant-source-reference: com/github/tommyettinger/textra/TextraDialog.java
 * Covenant-verified: 2026-06-15
 *
 * upstream-commit: 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4
 */
package sge
package textra

import scala.collection.mutable

import sge.graphics.Color
import sge.math.Interpolation
import sge.scenes.scene2d.{ Action, Actor, InputEvent, InputListener, Stage }
import sge.scenes.scene2d.actions.Actions
import sge.scenes.scene2d.ui.{ Skin, Table }
import sge.scenes.scene2d.utils.{ ChangeListener, FocusListener }
import sge.Input.Key
import lowlevel.Nullable
import sge.utils.Seconds

/** Displays a dialog, which is a window with a title, a content table, and a button table. Methods are provided to add a label to the content table and buttons to the button table, but any widgets
  * can be added. When a button is clicked, {@link #result(Object)} is called and the dialog is removed from the stage.
  *
  * @author
  *   Nathan Sweet
  */
class TextraDialog(title: String, style: Styles.WindowStyle, replacementFont: Font)(using Sge) extends TextraWindow(title, style, replacementFont) {

  var contentTable: Table = scala.compiletime.uninitialized
  var buttonTable:  Table = scala.compiletime.uninitialized

  private var _skin: Nullable[Skin] = Nullable.empty

  val values:     mutable.Map[Actor, Nullable[AnyRef]] = mutable.Map.empty
  var cancelHide: Boolean                              = false

  var previousKeyboardFocus: Nullable[Actor] = Nullable.empty
  var previousScrollFocus:   Nullable[Actor] = Nullable.empty
  var focusListener:         FocusListener   = scala.compiletime.uninitialized

  protected val ignoreTouchDown: InputListener = new InputListener() {
    override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Input.Button): Boolean = {
      event.cancel()
      false
    }
  }

  def this(title: String, style: Styles.WindowStyle)(using Sge) =
    this(title, style, Nullable.fold(style.titleFont)(new Font())(identity))

  def this(title: String, skin: Skin)(using Sge) = {
    this(title, skin.get(classOf[Styles.WindowStyle]))
    this._skin = Nullable(skin)
  }

  def this(title: String, skin: Skin, windowStyleName: String)(using Sge) = {
    this(title, skin.get(windowStyleName, classOf[Styles.WindowStyle]))
    this._skin = Nullable(skin)
  }

  def this(title: String, skin: Skin, replacementFont: Font)(using Sge) = {
    this(title, skin.get(classOf[Styles.WindowStyle]), replacementFont)
    this._skin = Nullable(skin)
  }

  def this(title: String, skin: Skin, windowStyleName: String, replacementFont: Font)(using Sge) = {
    this(title, skin.get(windowStyleName, classOf[Styles.WindowStyle]), replacementFont)
    this._skin = Nullable(skin)
  }

  initialize()

  // --- Initialize ---
  private def initialize(): Unit = {
    setModal(true)

    defaults().space(6)
    contentTable = new Table(_skin)
    add(Nullable[Actor](contentTable)).expand().fill()
    row()
    buttonTable = new Table(_skin)
    add(Nullable[Actor](buttonTable)).fillX()

    contentTable.defaults().space(6)
    buttonTable.defaults().space(6)

    buttonTable.addListener(
      new ChangeListener() {
        override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit =
          if (values.contains(actor)) {
            var current = actor
            while (current.parent.exists(_ ne buttonTable))
              current.parent.foreach { p => current = p }
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
        stage.foreach { stage =>
          if (
            isModal && stage.root.children.nonEmpty
            && (stage.root.children.last eq TextraDialog.this)
          ) { // TextraDialog is top most actor.
            val newFocusedActor = event.relatedActor
            newFocusedActor.foreach { nfa =>
              if (
                !nfa.isDescendantOf(TextraDialog.this)
                && !(previousKeyboardFocus.exists(_ eq nfa) || previousScrollFocus.exists(_ eq nfa))
              ) {
                event.cancel()
              }
            }
          }
        }
    }

  }

  /** Wires the modal FocusListener to the dialog's stage lifetime (TextraDialog.java:159-165 `setStage`, :161 `addListener(focusListener)`): the real FocusListener is added when the dialog is
    * attached to a stage (so it participates in the dialog's listener wiring while shown) and removed when it leaves it. hide() also removes it explicitly (TextraDialog.java:350).
    */
  override protected[sge] def setStage(stage: Nullable[Stage]): Unit = {
    if (stage.isEmpty) removeListener(focusListener)
    else addListener(focusListener)
    super.setStage(stage)
  }

  // --- Label factories ---

  override protected def newLabel(text: String, style: Styles.LabelStyle): TextraLabel =
    new TextraLabel(text, style)

  override protected def newLabel(text: String, font: Font, color: Color): TextraLabel =
    new TextraLabel(text, font, color)

  protected def newTypingLabel(text: String, style: Styles.LabelStyle): TypingLabel =
    new TypingLabel(text, style)

  protected def newTypingLabel(text: String, font: Font, color: Color): TypingLabel =
    new TypingLabel(text, font, color)

  // --- Content table / Button table accessors ---

  def getContentTable: Table = contentTable
  def getButtonTable:  Table = buttonTable

  // --- Content: text methods ---

  /** Adds a TextraLabel to the content table. The dialog must have been constructed with a skin to use this method. */
  def text(text: Nullable[String]): TextraDialog = {
    val s = _skin.getOrElse(throw new IllegalStateException("This method may only be used if the dialog was constructed with a Skin."))
    this.text(text, s.get(classOf[Styles.LabelStyle]))
  }

  /** Adds a TextraLabel to the content table. */
  def text(text: Nullable[String], labelStyle: Styles.LabelStyle): TextraDialog =
    this.text(newLabel(Nullable.fold(text)("")(identity), labelStyle))

  /** Adds a TextraLabel to the content table. */
  def text(text: Nullable[String], font: Font): TextraDialog =
    this.text(newLabel(Nullable.fold(text)("")(identity), font, Color.WHITE))

  /** Adds a TextraLabel to the content table. */
  def text(text: Nullable[String], font: Font, color: Color): TextraDialog =
    this.text(newLabel(Nullable.fold(text)("")(identity), font, color))

  /** Adds the given TextraLabel to the content table.
    * @param label
    *   a non-null TextraLabel
    */
  def text(label: TextraLabel): TextraDialog = {
    contentTable.add(Nullable[Actor](label))
    this
  }

  // --- Content: typing methods ---

  /** Adds a TypingLabel to the content table. The dialog must have been constructed with a skin to use this method. */
  def typing(text: Nullable[String]): TextraDialog = {
    val s = _skin.getOrElse(throw new IllegalStateException("This method may only be used if the dialog was constructed with a Skin."))
    this.typing(text, s.get(classOf[Styles.LabelStyle]))
  }

  /** Adds a TypingLabel to the content table. */
  def typing(text: Nullable[String], labelStyle: Styles.LabelStyle): TextraDialog =
    this.typing(newTypingLabel(Nullable.fold(text)("")(identity), labelStyle))

  /** Adds a TypingLabel to the content table. */
  def typing(text: Nullable[String], font: Font): TextraDialog =
    this.typing(newTypingLabel(Nullable.fold(text)("")(identity), font, Color.WHITE))

  /** Adds a TypingLabel to the content table. */
  def typing(text: Nullable[String], font: Font, color: Color): TextraDialog =
    this.typing(newTypingLabel(Nullable.fold(text)("")(identity), font, color))

  /** Adds the given TypingLabel to the content table.
    * @param label
    *   a non-null TypingLabel
    */
  def typing(label: TypingLabel): TextraDialog = {
    contentTable.add(Nullable[Actor](label))
    this
  }

  // --- Button methods ---

  /** Adds a text button to the button table. Null will be passed to {@link #result(Object)} if this button is clicked. The dialog must have been constructed with a skin to use this method.
    */
  def button(text: Nullable[String]): TextraDialog =
    button(text, Nullable.empty)

  /** Adds a text button to the button table. The dialog must have been constructed with a skin to use this method.
    *
    * @param obj
    *   The object that will be passed to {@link #result(Object)} if this button is clicked. May be null.
    */
  def button(text: Nullable[String], obj: Nullable[AnyRef]): TextraDialog = {
    val s = _skin.getOrElse(throw new IllegalStateException("This method may only be used if the dialog was constructed with a Skin."))
    button(text, obj, s.get(classOf[Styles.TextButtonStyle]))
  }

  /** Adds a text button to the button table.
    *
    * @param obj
    *   The object that will be passed to {@link #result(Object)} if this button is clicked. May be null.
    */
  def button(text: Nullable[String], obj: Nullable[AnyRef], buttonStyle: Styles.TextButtonStyle): TextraDialog = {
    val btn = if (font != null) {
      new TextraButton(text, buttonStyle, font)
    } else {
      new TextraButton(text, buttonStyle)
    }
    button(btn, obj)
  }

  /** Adds the given button to the button table. */
  def button(btn: TextraButton): TextraDialog =
    button(btn, Nullable.empty)

  /** Adds the given button to the button table.
    *
    * @param obj
    *   The object that will be passed to {@link #result(Object)} if this button is clicked. May be null.
    */
  def button(btn: TextraButton, obj: Nullable[AnyRef]): TextraDialog = {
    buttonTable.add(Nullable[Actor](btn))
    setObject(btn, obj)
    this
  }

  def setObject(actor: Actor, obj: Nullable[AnyRef]): Unit =
    values(actor) = obj

  // --- Show ---

  /** {@link #pack() Packs} the dialog (but doesn't set the position), adds it to the stage, sets it as the keyboard and scroll focus, clears any actions on the dialog, and adds the specified action
    * to it. The previous keyboard and scroll focus are remembered so they can be restored when the dialog is hidden.
    *
    * @param action
    *   May be null.
    */
  def show(stage: Stage, action: Nullable[Action]): TextraDialog = {
    clearActions()
    removeCaptureListener(ignoreTouchDown)

    previousKeyboardFocus = Nullable.empty
    val kbActor = stage.keyboardFocus
    kbActor.foreach { a =>
      if (!a.isDescendantOf(this)) previousKeyboardFocus = Nullable(a)
    }

    previousScrollFocus = Nullable.empty
    val scrollActor = stage.scrollFocus
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

  /** Centers the dialog in the stage and calls {@link #show(Stage, Action)} with a {@link Actions#fadeIn(float, Interpolation)} action (TextraDialog.java:334-338).
    */
  def show(stage: Stage): TextraDialog = {
    show(stage, Nullable(Actions.sequence(Actions.alpha(0f), Actions.fadeIn(Seconds(0.4f), Nullable(Interpolation.fade)))))
    setPosition(Math.round((stage.width - getWidth) / 2f).toFloat, Math.round((stage.height - getHeight) / 2f).toFloat)
    this
  }

  // --- Hide ---

  /** Removes the dialog from the stage, restoring the previous keyboard and scroll focus, and adds the specified action to the dialog.
    *
    * @param action
    *   If null, the dialog is removed immediately. Otherwise, the dialog is removed when the action completes. The dialog will not respond to touch down events during the action.
    */
  def hide(action: Nullable[Action]): Unit = {
    stage.foreach { stage =>
      removeListener(focusListener)
      previousKeyboardFocus.foreach { pkf =>
        if (pkf.stage.isEmpty) previousKeyboardFocus = Nullable.empty
      }
      val kbActor = stage.keyboardFocus
      if (kbActor.isEmpty || kbActor.exists(_.isDescendantOf(this))) stage.setKeyboardFocus(previousKeyboardFocus)

      previousScrollFocus.foreach { psf =>
        if (psf.stage.isEmpty) previousScrollFocus = Nullable.empty
      }
      val scrollActor = stage.scrollFocus
      if (scrollActor.isEmpty || scrollActor.exists(_.isDescendantOf(this))) stage.setScrollFocus(previousScrollFocus)
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
    hide(Nullable(Actions.fadeOut(Seconds(0.4f), Nullable(Interpolation.fade))))

  /** Hides the dialog. Called automatically when a button is clicked. The default implementation fades out the dialog over {@code durationSeconds} seconds.
    * @param durationSeconds
    *   how many seconds for the fade Action to last before this completely disappears
    */
  def hide(durationSeconds: Float): Unit =
    hide(Nullable(Actions.fadeOut(Seconds(durationSeconds), Nullable(Interpolation.fade))))

  // --- Key binding ---

  /** If this key is pressed, {@link #result(Object)} is called with the specified object.
    *
    * @see
    *   Keys
    */
  def key(keycode: Key, obj: Nullable[AnyRef]): TextraDialog = {
    addListener(
      new InputListener() {
        override def keyDown(event: InputEvent, keycode2: Key): Boolean = {
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

  // --- Result callback ---

  /** Called when a button is clicked. The dialog will be hidden after this method returns unless {@link #cancel()} is called.
    *
    * @param obj
    *   The object specified when the button was added.
    */
  protected def result(obj: Nullable[AnyRef]): Unit = {}

  def cancel(): Unit =
    cancelHide = true
}
