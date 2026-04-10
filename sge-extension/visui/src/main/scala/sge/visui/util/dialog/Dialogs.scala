/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package visui
package util
package dialog

import sge.Input.{ Key, Keys }
import sge.scenes.scene2d.{ Actor, InputEvent, InputListener, Stage }
import sge.utils.Nullable
import sge.visui.{ Locales, VisUI }
import sge.visui.i18n.BundleText
import sge.visui.util.InputValidator
import sge.visui.widget.{ ButtonBar, VisDialog, VisTextField, VisValidatableTextField }

/** Utilities for displaying various type of dialogs. Equivalent of JOptionPane from Swing.
  * @author
  *   Kotcrab
  * @since 0.2.0
  */
object Dialogs {

  /** Dialog with given text and single OK button. */
  def showOKDialog(stage: Stage, title: String, text: String)(using Sge): VisDialog = {
    val dialog = new VisDialog(title)
    dialog.closeOnEscape()
    dialog.text(text)
    dialog.button(ButtonBar.ButtonType.OK.text).padBottom(3)
    dialog.pack()
    dialog.centerWindow()
    dialog.addListener(
      new InputListener() {
        override def keyDown(event: InputEvent, keycode: Key): Boolean =
          if (keycode == Keys.ENTER) {
            dialog.fadeOut()
            true
          } else {
            false
          }
      }
    )
    stage.addActor(dialog.fadeIn())
    dialog
  }

  /** Dialog with title "Error" and provided text. */
  def showErrorDialog(stage: Stage, text: String)(using Sge): VisDialog =
    showErrorDialog(stage, text, Nullable.empty[String])

  /** Dialog with title "Error", provided text and exception stacktrace available after pressing 'Details' button. */
  def showErrorDialog(stage: Stage, text: String, exception: Throwable)(using Sge): VisDialog =
    if (exception == null) { // @nowarn -- Java interop boundary
      showErrorDialog(stage, text, Nullable.empty[String])
    } else {
      showErrorDialog(stage, text, Nullable(getStackTrace(exception)))
    }

  /** Dialog with title "Error", provided text, and provided details available after pressing 'Details' button. */
  def showErrorDialog(stage: Stage, text: String, details: Nullable[String])(using Sge): VisDialog = {
    val dialog = new VisDialog(Text.ERROR.get)
    dialog.text(text)
    dialog.button(ButtonBar.ButtonType.OK.text).padBottom(3)
    dialog.pack()
    dialog.centerWindow()
    stage.addActor(dialog.fadeIn())
    dialog
  }

  enum OptionDialogType {
    case YES_NO, YES_NO_CANCEL, YES_CANCEL
  }

  private def getStackTrace(throwable: Throwable): String = {
    val sb = new StringBuilder()
    getStackTrace(throwable, sb)
    sb.toString()
  }

  private def getStackTrace(throwable: Throwable, builder: StringBuilder): Unit = {
    val msg = throwable.getMessage
    if (msg != null) { // @nowarn -- Java interop boundary
      builder.append(msg)
      builder.append("\n\n")
    }

    for (element <- throwable.getStackTrace) {
      builder.append(element.toString)
      builder.append("\n")
    }

    if (throwable.getCause != null) { // @nowarn -- Java interop boundary
      builder.append("\nCaused by: ")
      getStackTrace(throwable.getCause, builder)
    }
  }

  /** Dialog with Yes/No (or Yes/No/Cancel) buttons and a callback. */
  def showOptionDialog(stage: Stage, title: String, text: String, dialogType: OptionDialogType, listener: OptionDialogListener)(using Sge): VisDialog = {
    val dialog = new VisDialog(title) {
      override protected def result(obj: Nullable[AnyRef]): Unit =
        obj.foreach {
          case "yes"    => listener.yes()
          case "no"     => listener.no()
          case "cancel" => listener.cancel()
          case _        => ()
        }
    }
    dialog.closeOnEscape()
    dialog.text(text)

    dialogType match {
      case OptionDialogType.YES_NO =>
        dialog.button(ButtonBar.ButtonType.YES.text, Nullable[AnyRef]("yes")).padBottom(3)
        dialog.button(ButtonBar.ButtonType.NO.text, Nullable[AnyRef]("no")).padBottom(3)
      case OptionDialogType.YES_NO_CANCEL =>
        dialog.button(ButtonBar.ButtonType.YES.text, Nullable[AnyRef]("yes")).padBottom(3)
        dialog.button(ButtonBar.ButtonType.NO.text, Nullable[AnyRef]("no")).padBottom(3)
        dialog.button(ButtonBar.ButtonType.CANCEL.text, Nullable[AnyRef]("cancel")).padBottom(3)
      case OptionDialogType.YES_CANCEL =>
        dialog.button(ButtonBar.ButtonType.YES.text, Nullable[AnyRef]("yes")).padBottom(3)
        dialog.button(ButtonBar.ButtonType.CANCEL.text, Nullable[AnyRef]("cancel")).padBottom(3)
    }

    dialog.pack()
    dialog.centerWindow()
    stage.addActor(dialog.fadeIn())
    dialog
  }

  /** Dialog with a text input field and OK/Cancel buttons. */
  def showInputDialog(stage: Stage, title: String, fieldTitle: String, listener: InputDialogListener)(using Sge): VisDialog =
    showInputDialog(stage, title, fieldTitle, cancelable = true, Nullable.empty[InputValidator], listener)

  /** Dialog with a text input field, validator, and OK/Cancel buttons. */
  def showInputDialog(stage: Stage, title: String, fieldTitle: String, validator: InputValidator, listener: InputDialogListener)(using Sge): VisDialog =
    showInputDialog(stage, title, fieldTitle, cancelable = true, Nullable(validator), listener)

  /** Dialog with a text input field and OK/Cancel buttons, with cancelable option. */
  def showInputDialog(stage: Stage, title: String, fieldTitle: String, cancelable: Boolean, listener: InputDialogListener)(using Sge): VisDialog =
    showInputDialog(stage, title, fieldTitle, cancelable, Nullable.empty[InputValidator], listener)

  /** Dialog with a text input field, validator, and OK/Cancel buttons, with cancelable option. */
  def showInputDialog(stage: Stage, title: String, fieldTitle: String, cancelable: Boolean, validator: Nullable[InputValidator], listener: InputDialogListener)(using Sge): VisDialog = {
    val textField = validator.fold(new VisTextField(""): VisTextField) { v =>
      new VisValidatableTextField(v)
    }
    val dialog = new VisDialog(title) {
      override protected def result(obj: Nullable[AnyRef]): Unit =
        obj.foreach {
          case "ok" => listener.finished(textField.text)
          case _    => listener.canceled()
        }
    }
    if (cancelable) dialog.closeOnEscape()
    dialog.text(fieldTitle)
    dialog.getContentTable.row()
    dialog.getContentTable.add(Nullable[Actor](textField)).expandX().fillX().pad(3)
    dialog.button(ButtonBar.ButtonType.OK.text, Nullable[AnyRef]("ok")).padBottom(3)
    dialog.button(ButtonBar.ButtonType.CANCEL.text, Nullable[AnyRef]("cancel")).padBottom(3)
    dialog.pack()
    dialog.centerWindow()
    stage.addActor(dialog.fadeIn())
    dialog
  }

  /** Listener for option dialogs (Yes/No/Cancel). */
  trait OptionDialogListener {
    def yes():    Unit = ()
    def no():     Unit = ()
    def cancel(): Unit = ()
  }

  /** Convenience adapter for OptionDialogListener with no-op defaults. */
  class OptionDialogAdapter extends OptionDialogListener

  /** Listener for input dialogs. */
  trait InputDialogListener {
    def finished(input: String): Unit
    def canceled():              Unit = ()
  }

  /** Convenience adapter for InputDialogListener. */
  class InputDialogAdapter extends InputDialogListener {
    override def finished(input: String): Unit = ()
  }

  /** Dialogs I18N properties. */
  private enum Text(val entryName: String) extends BundleText {
    case DETAILS extends Text("details")
    case DETAILS_COLON extends Text("detailsColon")
    case COPY extends Text("copy")
    case COPIED extends Text("copied")
    case ERROR extends Text("error")

    override def name:                       String = entryName
    override def get:                        String = Locales.getDialogsBundle(using VisUI.sgeInstance).get(entryName)
    override def format():                   String = Locales.getDialogsBundle(using VisUI.sgeInstance).format(entryName)
    override def format(arguments: AnyRef*): String = Locales.getDialogsBundle(using VisUI.sgeInstance).format(entryName, arguments*)
    override def toString:                   String = get
  }
}
