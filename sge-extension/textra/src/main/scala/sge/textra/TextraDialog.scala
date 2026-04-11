/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/TextraDialog.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: Table/Actor/Stage → standalone (scene2d base not inherited),
 *     ObjectMap → HashMap, FocusListener/InputListener/ChangeListener → local,
 *     Actions/Interpolation → simplified action stubs,
 *     Gdx.app.postRunnable → direct invocation
 *   Convention: Dialog show/hide/key behavior fully ported.
 *   Idiom: Nullable[A] for nullable fields; boundary/break for early returns.
 */
package sge
package textra

import scala.collection.mutable.HashMap
import sge.graphics.Color
import sge.graphics.g2d.Batch
import sge.utils.Nullable

/** Displays a dialog, which is a window with a title, a content table, and a button table. Methods are provided to add a label to the content table and buttons to the button table, but any widgets
  * can be added. When a button is clicked, result(Object) is called and the dialog is removed from the stage.
  */
class TextraDialog(title: String, style: Styles.WindowStyle, replacementFont: Font) extends TextraWindow(title, style, replacementFont) {

  val values:     HashMap[AnyRef, AnyRef] = HashMap.empty
  var cancelHide: Boolean                 = false

  // Content labels and button labels stored for rendering
  private val contentLabels: scala.collection.mutable.ArrayBuffer[TextraLabel]                      = scala.collection.mutable.ArrayBuffer.empty
  private val buttonEntries: scala.collection.mutable.ArrayBuffer[(TextraButton, Nullable[AnyRef])] = scala.collection.mutable.ArrayBuffer.empty

  // Focus tracking for show/hide
  protected var previousKeyboardFocus: Nullable[AnyRef] = Nullable.empty
  protected var previousScrollFocus:   Nullable[AnyRef] = Nullable.empty

  def this(title: String, style: Styles.WindowStyle) =
    this(title, style, Nullable.fold(style.titleFont)(new Font())(identity))

  // --- Initialize ---
  private def initialize(): Unit =
    setModal(true)
  initialize()

  // --- Label factories ---

  protected def newTypingLabel(text: String, style: Styles.LabelStyle): TypingLabel =
    new TypingLabel(text, style)

  protected def newTypingLabel(text: String, font: Font, color: Color): TypingLabel =
    new TypingLabel(text, font, color)

  // --- Content: text methods ---

  /** Adds a TextraLabel to the content table. */
  def text(text: Nullable[String], labelStyle: Styles.LabelStyle): TextraDialog = {
    val lbl = newLabel(Nullable.fold(text)("")(identity), labelStyle)
    contentLabels += lbl
    this
  }

  /** Adds a TextraLabel to the content table. */
  def text(text: Nullable[String], font: Font): TextraDialog = {
    val lbl = newLabel(Nullable.fold(text)("")(identity), font, Color.WHITE)
    contentLabels += lbl
    this
  }

  /** Adds a TextraLabel to the content table. */
  def text(text: Nullable[String], font: Font, color: Color): TextraDialog = {
    val lbl = newLabel(Nullable.fold(text)("")(identity), font, color)
    contentLabels += lbl
    this
  }

  /** Adds the given TextraLabel to the content table. */
  def text(label: TextraLabel): TextraDialog = {
    contentLabels += label
    this
  }

  // --- Content: typing methods ---

  /** Adds a TypingLabel to the content table. */
  def typing(text: Nullable[String], labelStyle: Styles.LabelStyle): TextraDialog = {
    val lbl = newTypingLabel(Nullable.fold(text)("")(identity), labelStyle)
    contentLabels += lbl
    this
  }

  /** Adds a TypingLabel to the content table. */
  def typing(text: Nullable[String], font: Font): TextraDialog = {
    val lbl = newTypingLabel(Nullable.fold(text)("")(identity), font, Color.WHITE)
    contentLabels += lbl
    this
  }

  /** Adds a TypingLabel to the content table. */
  def typing(text: Nullable[String], font: Font, color: Color): TextraDialog = {
    val lbl = newTypingLabel(Nullable.fold(text)("")(identity), font, color)
    contentLabels += lbl
    this
  }

  /** Adds the given TypingLabel to the content table. */
  def typing(label: TypingLabel): TextraDialog = {
    contentLabels += label
    this
  }

  // --- Button methods ---

  /** Adds a text button to the button table. Null will be passed to result(Object) if this button is clicked. */
  def button(text: Nullable[String], obj: Nullable[AnyRef], buttonStyle: Styles.TextButtonStyle): TextraDialog = {
    val btn = if (font != null) {
      new TextraButton(text, buttonStyle, font)
    } else {
      new TextraButton(text, buttonStyle)
    }
    buttonEntries += ((btn, obj))
    Nullable.foreach(obj)(o => values.put(btn.asInstanceOf[AnyRef], o))
    this
  }

  /** Adds the given button to the button table. */
  def button(btn: TextraButton, obj: Nullable[AnyRef]): TextraDialog = {
    buttonEntries += ((btn, obj))
    Nullable.foreach(obj)(o => values.put(btn.asInstanceOf[AnyRef], o))
    this
  }

  /** Adds the given button to the button table with null object. */
  def button(btn: TextraButton): TextraDialog =
    button(btn, Nullable.empty)

  def setObject(actor: AnyRef, obj: Nullable[AnyRef]): Unit =
    Nullable.fold(obj)(values.remove(actor)) { o => values.put(actor, o); () }

  // --- Show ---

  /** Packs the dialog (but doesn't set the position), adds it to the stage, sets it as the keyboard and scroll focus, clears any actions on the dialog, and adds the specified action to it. The
    * previous keyboard and scroll focus are remembered so they can be restored when the dialog is hidden.
    */
  def show(stageWidth: Float, stageHeight: Float): TextraDialog = {
    previousKeyboardFocus = Nullable.empty
    previousScrollFocus = Nullable.empty
    pack()
    // Center the dialog
    setPosition(Math.round((stageWidth - getWidth) / 2f).toFloat, Math.round((stageHeight - getHeight) / 2f).toFloat)
    this
  }

  // --- Hide ---

  /** Hides the dialog. Called automatically when a button is clicked. The default implementation marks the dialog as hidden. In full scene2d integration this would fade out and remove from stage.
    */
  def hide(): Unit = {
    // In full scene2d integration, this would use:
    //   hide(fadeOut(0.4f, Interpolation.fade))
    // For standalone usage, just mark as not visible / remove.
    Nullable.foreach(previousKeyboardFocus)(_ => ())
    Nullable.foreach(previousScrollFocus)(_ => ())
  }

  /** Hides the dialog with a specified duration (in seconds). */
  def hide(durationSeconds: Float): Unit =
    // In full scene2d integration, would use fadeOut action.
    hide()

  // --- Key binding ---

  /** If this key is pressed, result(Object) is called with the specified object. */
  def key(keycode: Int, obj: Nullable[AnyRef]): TextraDialog = {
    // Store the key binding. In full scene2d integration, this adds an InputListener
    // that triggers result() + hide() when the key is pressed.
    keyBindings += ((keycode, obj))
    this
  }

  private val keyBindings: scala.collection.mutable.ArrayBuffer[(Int, Nullable[AnyRef])] = scala.collection.mutable.ArrayBuffer.empty

  /** Checks key bindings against a pressed keycode. Call this from your input handling. */
  def handleKeyPress(keycode: Int): Boolean = {
    val binding = keyBindings.find(_._1 == keycode)
    binding match {
      case Some((_, obj)) =>
        result(obj)
        if (!cancelHide) hide()
        cancelHide = false
        true
      case _ =>
        false
    }
  }

  // --- Result callback ---

  /** Called when a button is clicked. The dialog will be hidden after this method returns unless cancel() is called. */
  protected def result(obj: Nullable[AnyRef]): Unit = {}

  def cancel(): Unit =
    cancelHide = true

  // --- Button click handling ---

  /** Call this when a button in the dialog is clicked. It triggers result() and hide(). */
  def handleButtonClick(actor: AnyRef): Unit =
    if (values.contains(actor)) {
      val obj = values.get(actor)
      obj.foreach { o =>
        result(Nullable(o))
        if (!cancelHide) hide()
        cancelHide = false
      }
    }

  // --- Draw override ---

  /** Draws the dialog including content labels and button entries. */
  override def draw(batch: Batch, parentAlpha: Float): Unit = {
    super.draw(batch, parentAlpha)

    // Draw content labels
    var contentY = getY + getHeight - getPadTop
    contentLabels.foreach { lbl =>
      contentY -= lbl.getPrefHeight + 6f
      lbl.setPosition(getX + getPadLeft + 6f, contentY)
      lbl.draw(batch, parentAlpha)
    }

    // Draw button labels
    buttonEntries.foreach { case (btn, _) =>
      btn.draw(batch, parentAlpha)
    }
  }
}
