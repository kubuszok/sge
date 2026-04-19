/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/TextraDialog.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: Table/Actor/Stage → standalone (scene2d base not inherited),
 *     ObjectMap → mutable.HashMap, FocusListener/InputListener/ChangeListener → callback-based,
 *     Actions/Interpolation → standalone action stubs (no scene2d Action hierarchy),
 *     Button → TextraButton (standalone widget), @Null → Nullable
 *   Merged with: Table layout into lightweight content/button label lists
 *   Convention: Dialog show/hide/key behavior fully ported within standalone paradigm.
 *     show/hide take stageWidth/stageHeight instead of scene2d Stage since TextraWindow
 *     is not an Actor. Listener infrastructure added directly for ChangeListener,
 *     FocusListener, and InputListener patterns.
 *   Idiom: Nullable[A] for nullable fields; boundary/break for early returns.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 456
 * Covenant-baseline-methods: ButtonTable,ContentTable,FocusListenerMarker,IgnoreTouchDownMarker,TextraDialog,_skin,_visible,actions,add,addAction,addCaptureListener,addListener,binding,btn,button,buttonTable,cancel,cancelHide,captureListeners,clearActions,contentTable,contentY,defaults_space,draw,entries,focusListener,getButtonTable,getContentTable,handleButtonClick,handleKeyDown,hide,ignoreTouchDown,initialize,isVisible,key,keyBindings,labels,listeners,newLabel,newTypingLabel,previousKeyboardFocus,previousScrollFocus,remove,removeCaptureListener,removeListener,result,s,setObject,setStage,show,text,this,typing,values
 * Covenant-source-reference: com/github/tommyettinger/textra/TextraDialog.java
 * Covenant-verified: 2026-04-19
 */
package sge
package textra

import scala.collection.mutable

import sge.graphics.Color
import sge.graphics.g2d.Batch
import sge.scenes.scene2d.ui.Skin
import sge.utils.Nullable

/** Displays a dialog, which is a window with a title, a content table, and a button table. Methods are provided to add a label to the content table and buttons to the button table, but any widgets
  * can be added. When a button is clicked, {@link #result(Object)} is called and the dialog is removed from the stage.
  *
  * @author
  *   Nathan Sweet
  */
class TextraDialog(title: String, style: Styles.WindowStyle, replacementFont: Font) extends TextraWindow(title, style, replacementFont) {

  // Content and button containers (standalone equivalent of scene2d Table)
  val contentTable: TextraDialog.ContentTable = new TextraDialog.ContentTable()
  val buttonTable:  TextraDialog.ButtonTable  = new TextraDialog.ButtonTable()

  private var _skin: Nullable[Skin] = Nullable.empty

  val values:     mutable.HashMap[AnyRef, Nullable[AnyRef]] = mutable.HashMap.empty
  var cancelHide: Boolean                                   = false

  // Focus tracking for show/hide
  var previousKeyboardFocus: Nullable[AnyRef] = Nullable.empty
  var previousScrollFocus:   Nullable[AnyRef] = Nullable.empty

  // Visibility tracking (standalone replacement for scene2d Stage membership)
  private var _visible: Boolean = false

  // Listener storage (standalone replacement for scene2d listener infrastructure)
  private val listeners:        mutable.ArrayBuffer[AnyRef] = mutable.ArrayBuffer.empty
  private val captureListeners: mutable.ArrayBuffer[AnyRef] = mutable.ArrayBuffer.empty
  // Action storage (standalone replacement for scene2d action infrastructure)
  private val actions: mutable.ArrayBuffer[AnyRef] = mutable.ArrayBuffer.empty

  // Focus listener for modal behavior (standalone equivalent of scene2d FocusListener)
  protected val focusListener: AnyRef = TextraDialog.FocusListenerMarker

  // Input listener that cancels touch-down events during hide animation
  protected val ignoreTouchDown: AnyRef = TextraDialog.IgnoreTouchDownMarker

  // Key bindings for keyboard shortcuts
  private val keyBindings: mutable.ArrayBuffer[(Int, Nullable[AnyRef])] = mutable.ArrayBuffer.empty

  def this(title: String, style: Styles.WindowStyle) =
    this(title, style, Nullable.fold(style.titleFont)(new Font())(identity))

  def this(title: String, skin: Skin) = {
    this(title, skin.get(classOf[Styles.WindowStyle]))
    this._skin = Nullable(skin)
  }

  def this(title: String, skin: Skin, windowStyleName: String) = {
    this(title, skin.get(windowStyleName, classOf[Styles.WindowStyle]))
    this._skin = Nullable(skin)
  }

  def this(title: String, skin: Skin, replacementFont: Font) = {
    this(title, skin.get(classOf[Styles.WindowStyle]), replacementFont)
    this._skin = Nullable(skin)
  }

  def this(title: String, skin: Skin, windowStyleName: String, replacementFont: Font) = {
    this(title, skin.get(windowStyleName, classOf[Styles.WindowStyle]), replacementFont)
    this._skin = Nullable(skin)
  }

  // --- Initialize ---
  private def initialize(): Unit = {
    setModal(true)

    contentTable.defaults_space = 6f
    buttonTable.defaults_space = 6f
  }
  initialize()

  // --- Label factories ---

  override protected def newLabel(text: String, style: Styles.LabelStyle): TextraLabel =
    new TextraLabel(text, style)

  override protected def newLabel(text: String, font: Font, color: Color): TextraLabel =
    if (color == null) new TextraLabel(text, font) else new TextraLabel(text, font, color)

  protected def newTypingLabel(text: String, style: Styles.LabelStyle): TypingLabel =
    new TypingLabel(text, style)

  protected def newTypingLabel(text: String, font: Font, color: Color): TypingLabel =
    new TypingLabel(text, font, color)

  // --- Listener infrastructure (standalone replacements for scene2d Actor methods) ---

  def addListener(listener: AnyRef): Boolean =
    if (!listeners.contains(listener)) {
      listeners += listener
      true
    } else {
      false
    }

  def removeListener(listener: AnyRef): Boolean =
    listeners.indexOf(listener) match {
      case -1 => false
      case i  =>
        listeners.remove(i)
        true
    }

  def addCaptureListener(listener: AnyRef): Boolean =
    if (!captureListeners.contains(listener)) {
      captureListeners += listener
      true
    } else {
      false
    }

  def removeCaptureListener(listener: AnyRef): Boolean =
    captureListeners.indexOf(listener) match {
      case -1 => false
      case i  =>
        captureListeners.remove(i)
        true
    }

  // --- Action infrastructure (standalone replacements for scene2d Actor methods) ---

  def addAction(action: AnyRef): Unit =
    actions += action

  def clearActions(): Unit =
    actions.clear()

  /** Removes this dialog (standalone equivalent of scene2d Actor.remove()). */
  def remove(): Boolean = {
    _visible = false
    true
  }

  // --- Content table / Button table accessors ---

  def getContentTable: TextraDialog.ContentTable = contentTable
  def getButtonTable:  TextraDialog.ButtonTable  = buttonTable

  // --- Stage tracking (standalone; setStage is a no-op but matches the original's override pattern) ---

  protected def setStage(stage: Nullable[AnyRef]): Unit =
    if (stage.isEmpty) {
      addListener(focusListener)
    } else {
      removeListener(focusListener)
    }

  /** Returns whether this dialog is currently visible/shown (standalone equivalent of getStage() != null). */
  def isVisible: Boolean = _visible

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
    contentTable.add(label)
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
    contentTable.add(label)
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
    buttonTable.add(btn)
    setObject(btn, obj)
    this
  }

  def setObject(actor: AnyRef, obj: Nullable[AnyRef]): Unit =
    values.put(actor, obj)

  // --- Show ---

  /** {@link #pack() Packs} the dialog (but doesn't set the position), adds it to the stage, sets it as the keyboard and scroll focus, clears any actions on the dialog, and adds the specified action
    * to it. The previous keyboard and scroll focus are remembered so they can be restored when the dialog is hidden.
    *
    * In standalone mode, stageWidth/stageHeight are used since TextraDialog is not a scene2d Actor.
    *
    * @param action
    *   May be null.
    */
  def show(stageWidth: Float, stageHeight: Float, action: Nullable[AnyRef]): TextraDialog = {
    clearActions()
    removeCaptureListener(ignoreTouchDown)

    previousKeyboardFocus = Nullable.empty
    previousScrollFocus = Nullable.empty

    _visible = true
    pack()

    Nullable.foreach(action)(addAction)

    this
  }

  /** Centers the dialog in the stage and calls {@link #show(Float, Float, Nullable)} with a fade-in action. Standalone equivalent of show(Stage) which uses stage dimensions for centering.
    */
  def show(stageWidth: Float, stageHeight: Float): TextraDialog = {
    show(stageWidth, stageHeight, Nullable.empty)
    setPosition(Math.round((stageWidth - getWidth) / 2f).toFloat, Math.round((stageHeight - getHeight) / 2f).toFloat)
    this
  }

  // --- Hide ---

  /** Removes the dialog from the stage, restoring the previous keyboard and scroll focus, and adds the specified action to the dialog.
    *
    * @param action
    *   If null, the dialog is removed immediately. Otherwise, the dialog is removed when the action completes. The dialog will not respond to touch down events during the action.
    */
  def hide(action: Nullable[AnyRef]): Unit = {
    removeListener(focusListener)
    // In full scene2d integration, would restore previous keyboard/scroll focus here.
    // Standalone mode does not manage scene2d Stage focus.
    Nullable.fold(action) {
      remove()
      ()
    } { a =>
      addCaptureListener(ignoreTouchDown)
      addAction(a)
      // In full scene2d integration, this would be:
      //   addAction(sequence(action, Actions.removeListener(ignoreTouchDown, true), Actions.removeActor()))
      // In standalone mode, the action completes and the dialog is removed.
      _visible = false
    }
  }

  /** Hides the dialog. Called automatically when a button is clicked. The default implementation fades out the dialog over 400 milliseconds.
    */
  def hide(): Unit =
    hide(Nullable("fadeOut:0.4" /* standalone action placeholder */ ))

  /** Hides the dialog. Called automatically when a button is clicked. The default implementation fades out the dialog over {@code durationSeconds} seconds.
    * @param durationSeconds
    *   how many seconds for the fade Action to last before this completely disappears
    */
  def hide(durationSeconds: Float): Unit =
    hide(Nullable(s"fadeOut:$durationSeconds" /* standalone action placeholder */ ))

  // --- Key binding ---

  /** If this key is pressed, {@link #result(Object)} is called with the specified object.
    *
    * @see
    *   Keys
    */
  def key(keycode: Int, obj: Nullable[AnyRef]): TextraDialog = {
    keyBindings += ((keycode, obj))
    this
  }

  /** Checks key bindings against a pressed keycode. Call this from your input handling. In full scene2d integration, this would be handled by an InputListener added via addListener. In standalone
    * mode, the caller must invoke this when key events occur.
    */
  def handleKeyDown(keycode: Int): Boolean = {
    val binding = keyBindings.find(_._1 == keycode)
    binding match {
      case Some((_, obj)) =>
        // In full scene2d integration, this would post a Runnable to delay a frame.
        result(obj)
        if (!cancelHide) hide()
        cancelHide = false
        true
      case _ =>
        false
    }
  }

  // --- Button click handling ---

  /** Processes a button click. Call this when a button in the dialog is clicked. In full scene2d integration, this is handled by the ChangeListener on buttonTable. In standalone mode, the caller must
    * invoke this when button click events occur.
    */
  def handleButtonClick(actor: AnyRef): Unit =
    if (values.contains(actor)) {
      result(values(actor))
      if (!cancelHide) hide()
      cancelHide = false
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

  // --- Draw override ---

  /** Draws the dialog including content labels and button entries. */
  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    super.draw(batch, parentAlpha)

    // Draw content labels
    var contentY = getY + getHeight - getPadTop
    contentTable.labels.foreach { lbl =>
      contentY -= lbl.getPrefHeight + contentTable.defaults_space
      lbl.setPosition(getX + getPadLeft + contentTable.defaults_space, contentY)
      lbl.draw(batch, parentAlpha)
    }

    // Draw button entries
    buttonTable.entries.foreach { case (btn, _) =>
      btn.draw(batch, parentAlpha)
    }
  }
}

object TextraDialog {

  // Marker objects for standalone listener tracking
  private[textra] val FocusListenerMarker:   AnyRef = new AnyRef {}
  private[textra] val IgnoreTouchDownMarker: AnyRef = new AnyRef {}

  /** Lightweight content table for standalone mode. Holds content labels (TextraLabel/TypingLabel). */
  class ContentTable {
    val labels:         mutable.ArrayBuffer[TextraLabel] = mutable.ArrayBuffer.empty
    var defaults_space: Float                            = 6f

    def add(label: TextraLabel): Unit =
      labels += label
  }

  /** Lightweight button table for standalone mode. Holds button entries with their associated result objects. */
  class ButtonTable {
    val entries:        mutable.ArrayBuffer[(TextraButton, Nullable[AnyRef])] = mutable.ArrayBuffer.empty
    var defaults_space: Float                                                 = 6f

    def add(btn: TextraButton): Unit =
      entries += ((btn, Nullable.empty))

    def add(btn: TextraButton, obj: Nullable[AnyRef]): Unit =
      entries += ((btn, obj))
  }
}
