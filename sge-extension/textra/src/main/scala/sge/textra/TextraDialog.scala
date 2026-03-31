/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/TextraDialog.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: Table/Actor/Stage → deferred, ObjectMap → HashMap,
 *     FocusListener/InputListener/ChangeListener → deferred,
 *     Actions/Interpolation → deferred
 *   Convention: Dialog show/hide/key behavior preserved in API,
 *     but actual scene2d integration deferred.
 */
package sge
package textra

import scala.collection.mutable.HashMap
import sge.graphics.Color
import sge.utils.Nullable

/** Displays a dialog, which is a window with a title, a content table, and a button table. Methods are provided to add a label to the content table and buttons to the button table, but any widgets
  * can be added. When a button is clicked, result(Object) is called and the dialog is removed from the stage.
  */
class TextraDialog(title: String, style: Styles.WindowStyle, replacementFont: Font) extends TextraWindow(title, style, replacementFont) {

  val values:     HashMap[AnyRef, AnyRef] = HashMap.empty
  var cancelHide: Boolean                 = false

  def this(title: String, style: Styles.WindowStyle) =
    this(title, style, Nullable.fold(style.titleFont)(new Font())(identity))

  private def initialize(): Unit =
    setModal(true)
  initialize()

  protected def newTypingLabel(text: String, style: Styles.LabelStyle): TypingLabel =
    new TypingLabel(text, style)

  protected def newTypingLabel(text: String, font: Font, color: Color): TypingLabel =
    new TypingLabel(text, font, color)

  /** Adds a TextraLabel to the content table. */
  def text(text: Nullable[String], font: Font): TextraDialog = {
    // In full implementation, contentTable.add(newLabel(...))
    newLabel(Nullable.fold(text)("")(identity), font, Color.WHITE)
    this
  }

  /** Adds a TextraLabel to the content table. */
  def text(text: Nullable[String], font: Font, color: Color): TextraDialog = {
    // In full implementation, contentTable.add(newLabel(...))
    newLabel(Nullable.fold(text)("")(identity), font, color)
    this
  }

  /** Adds a TextraLabel to the content table. */
  def text(label: TextraLabel): TextraDialog =
    // In full implementation, contentTable.add(label)
    this

  /** Adds a TypingLabel to the content table. */
  def typing(text: Nullable[String], font: Font): TextraDialog = {
    // In full implementation, contentTable.add(newTypingLabel(...))
    newTypingLabel(Nullable.fold(text)("")(identity), font, Color.WHITE)
    this
  }

  /** Adds a TypingLabel to the content table. */
  def typing(text: Nullable[String], font: Font, color: Color): TextraDialog = {
    // In full implementation, contentTable.add(newTypingLabel(...))
    newTypingLabel(Nullable.fold(text)("")(identity), font, color)
    this
  }

  /** Adds the given TypingLabel to the content table. */
  def typing(label: TypingLabel): TextraDialog =
    // In full implementation, contentTable.add(label)
    this

  /** Adds a text button to the button table. */
  def button(text: Nullable[String], obj: Nullable[AnyRef], buttonStyle: Styles.TextButtonStyle): TextraDialog = {
    val btn = if (font != null) {
      new TextraButton(text, buttonStyle, font)
    } else {
      new TextraButton(text, buttonStyle)
    }
    Nullable.foreach(obj)(o => values.put(btn.asInstanceOf[AnyRef], o))
    this
  }

  def setObject(actor: AnyRef, obj: Nullable[AnyRef]): Unit =
    Nullable.foreach(obj)(o => values.put(actor, o))

  /** Called when a button is clicked. The dialog will be hidden after this method returns unless cancel() is called. */
  protected def result(obj: Nullable[AnyRef]): Unit = {}

  def cancel(): Unit =
    cancelHide = true

  /** Hides the dialog. */
  def hide(): Unit = {
    // In full implementation, uses fade-out action and removes from stage.
  }
}
