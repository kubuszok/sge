/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 577
 * Covenant-baseline-methods: BUTTON_DETAILS,BUTTON_OK,ConfirmDialog,ConfirmDialogListener,DetailsDialog,Dialogs,InputDialog,InputDialogAdapter,InputDialogListener,OptionDialog,OptionDialogAdapter,OptionDialogListener,OptionDialogType,Text,_detailsVisible,addListeners,addValidatableFieldListener,buttonBar,cancel,cancelButton,canceled,changed,close,copyButton,createScrollPane,detailsCell,detailsLabel,detailsTable,dialog,field,finished,format,get,getStackTrace,isCopyDetailsButtonVisible,isDetailsVisible,keyDown,msg,name,no,okButton,result,run,sb,scrollPane,setCancelButtonText,setCopyDetailsButtonVisible,setDetailsVisible,setNoButtonText,setStage,setText,setWrapDetails,setYesButtonText,showConfirmDialog,showDetailsDialog,showErrorDialog,showInputDialog,showOKDialog,showOptionDialog,toString,yes
 * Covenant-source-reference: com/kotcrab/vis/ui/util/dialog/Dialogs.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
 */
package sge
package visui
package util
package dialog

import sge.Input.{ Key, Keys }
import sge.scenes.scene2d.{ Actor, InputEvent, InputListener, Stage }
import sge.scenes.scene2d.ui.Cell
import sge.scenes.scene2d.utils.ChangeListener
import sge.utils.{ Align, Nullable }
import sge.visui.{ Locales, VisUI }
import sge.visui.i18n.BundleText
import sge.visui.util.{ InputValidator, TableUtils }
import sge.visui.widget.{ ButtonBar, VisDialog, VisLabel, VisScrollPane, VisTable, VisTextButton, VisTextField, VisValidatableTextField, VisWindow }

/** Utilities for displaying various type of dialogs. Equivalent of JOptionPane from Swing.
  * @author
  *   Kotcrab
  * @since 0.2.0
  */
object Dialogs {
  private val BUTTON_OK      = 1
  private val BUTTON_DETAILS = 2

  /** Dialog with given text and single OK button.
    * @param title
    *   dialog title
    */
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

  /** Dialog with text and buttons like Yes, No, Cancel.
    * @param title
    *   dialog title
    * @param dialogType
    *   specifies what types of buttons will this dialog have
    * @param listener
    *   dialog buttons listener.
    * @return
    *   dialog for the purpose of changing buttons text.
    * @see
    *   [[OptionDialog]]
    * @since 0.6.0
    */
  def showOptionDialog(stage: Stage, title: String, text: String, dialogType: OptionDialogType, listener: OptionDialogListener)(using Sge): OptionDialog = {
    val dialog = new OptionDialog(title, text, dialogType, listener)
    stage.addActor(dialog.fadeIn())
    dialog
  }

  /** Dialog with title, text and n amount of buttons. If you need dialog with only buttons like Yes, No, Cancel then see [[showOptionDialog]].
    *
    * @param title
    *   dialog title.
    * @param listener
    *   button listener for this dialog. This dialog is generic, listener type will depend on 'returns' param type.
    * @since 0.7.0
    */
  def showConfirmDialog[T](stage: Stage, title: String, text: String, buttons: Array[String], returns: Array[T], listener: ConfirmDialogListener[T])(using Sge): ConfirmDialog[T] = {
    val dialog = new ConfirmDialog[T](title, text, buttons, returns, listener)
    stage.addActor(dialog.fadeIn())
    dialog
  }

  /** Dialog with text and text field for user input. Cannot be canceled.
    * @param title
    *   dialog title.
    * @param fieldTitle
    *   displayed before input field, may be null.
    * @param listener
    *   dialog buttons listener.
    */
  def showInputDialog(stage: Stage, title: String, fieldTitle: String, listener: InputDialogListener)(using Sge): InputDialog = {
    val dialog = new InputDialog(title, fieldTitle, true, Nullable.empty[InputValidator], listener)
    stage.addActor(dialog.fadeIn())
    dialog
  }

  /** Dialog with text and text field for user input. Cannot be canceled.
    * @param title
    *   dialog title.
    * @param fieldTitle
    *   displayed before input field, may be null.
    * @param validator
    *   used to validate user input. Eg. limit input to integers only.
    * @param listener
    *   dialog buttons listener.
    */
  def showInputDialog(stage: Stage, title: String, fieldTitle: String, validator: InputValidator, listener: InputDialogListener)(using Sge): InputDialog = {
    val dialog = new InputDialog(title, fieldTitle, true, Nullable(validator), listener)
    stage.addActor(dialog.fadeIn())
    dialog
  }

  /** Dialog with text and text field for user input.
    * @param title
    *   dialog title.
    * @param cancelable
    *   if true dialog may be canceled by user.
    * @param fieldTitle
    *   displayed before input field, may be null.
    * @param listener
    *   dialog buttons listener.
    */
  def showInputDialog(stage: Stage, title: String, fieldTitle: String, cancelable: Boolean, listener: InputDialogListener)(using Sge): InputDialog = {
    val dialog = new InputDialog(title, fieldTitle, cancelable, Nullable.empty[InputValidator], listener)
    stage.addActor(dialog.fadeIn())
    dialog
  }

  /** Dialog with text and text field for user input.
    * @param title
    *   dialog title
    * @param validator
    *   used to validate user input, can be used to easily limit input to int etc.
    * @param cancelable
    *   if true dialog may be canceled.
    * @param fieldTitle
    *   displayed before input field, may be null.
    */
  def showInputDialog(stage: Stage, title: String, fieldTitle: String, cancelable: Boolean, validator: InputValidator, listener: InputDialogListener)(using Sge): InputDialog = {
    val dialog = new InputDialog(title, fieldTitle, cancelable, Nullable(validator), listener)
    stage.addActor(dialog.fadeIn())
    dialog
  }

  /** Dialog with title "Error" and provided text. */
  def showErrorDialog(stage: Stage, text: String)(using Sge): DetailsDialog =
    showErrorDialog(stage, text, Nullable.empty[String])

  /** Dialog with title "Error", provided text and exception stacktrace available after pressing 'Details' button. */
  def showErrorDialog(stage: Stage, text: String, exception: Throwable)(using Sge): DetailsDialog =
    if (exception == null) { // @nowarn -- Java interop boundary
      showErrorDialog(stage, text, Nullable.empty[String])
    } else {
      showErrorDialog(stage, text, Nullable(getStackTrace(exception)))
    }

  /** Dialog with title "Error", provided text, and provided details available after pressing 'Details' button. */
  def showErrorDialog(stage: Stage, text: String, details: Nullable[String])(using Sge): DetailsDialog = {
    val dialog = new DetailsDialog(text, Text.ERROR.get, details)
    stage.addActor(dialog.fadeIn())
    dialog
  }

  /** Dialog with given title, provided text, and more details available after pressing 'Details' button. */
  def showDetailsDialog(stage: Stage, text: String, title: String, details: String)(using Sge): DetailsDialog =
    showDetailsDialog(stage, text, title, details, expandDetails = false)

  /** Dialog with given title, provided text, and more details available after pressing 'Details' button.
    * @param expandDetails
    *   if true details will be visible without need to press 'Details' button
    */
  def showDetailsDialog(stage: Stage, text: String, title: String, details: String, expandDetails: Boolean)(using Sge): DetailsDialog = {
    val dialog = new DetailsDialog(text, title, Nullable(details))
    dialog.setDetailsVisible(expandDetails)
    stage.addActor(dialog.fadeIn())
    dialog
  }

  private def createScrollPane(widget: Actor)(using Sge): VisScrollPane = {
    val scrollPane = new VisScrollPane(Nullable[Actor](widget))
    scrollPane.setOverscroll(false, true)
    scrollPane.setFadeScrollBars(false)
    scrollPane
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

  enum OptionDialogType {
    case YES_NO, YES_NO_CANCEL, YES_CANCEL
  }

  /** Dialog with input field and optional [[InputValidator]]. Can be used directly although you should use [[Dialogs]] showInputDialog methods.
    */
  class InputDialog(title: String, fieldTitle: String, cancelable: Boolean, validator: Nullable[InputValidator], private val listener: InputDialogListener)(using sge: Sge) extends VisWindow(title) {

    private val field: VisTextField = validator.fold(new VisTextField(): VisTextField) { v =>
      new VisValidatableTextField(v)
    }
    private val okButton:     VisTextButton = new VisTextButton(ButtonBar.ButtonType.OK.text)
    private val cancelButton: VisTextButton = new VisTextButton(ButtonBar.ButtonType.CANCEL.text)

    {
      TableUtils.setSpacingDefaults(this)
      isModal = true

      if (cancelable) {
        addCloseButton()
        closeOnEscape()
      }

      val buttonBar = new ButtonBar()
      buttonBar.ignoreSpacing = true
      buttonBar.setButton(ButtonBar.ButtonType.CANCEL, cancelButton)
      buttonBar.setButton(ButtonBar.ButtonType.OK, okButton)

      val fieldTable = new VisTable(true)

      if (fieldTitle != null) fieldTable.add(Nullable[Actor](new VisLabel(fieldTitle))) // @nowarn -- Java interop boundary

      fieldTable.add(Nullable[Actor](field)).expand().fill()

      add(Nullable[Actor](fieldTable)).padTop(3).spaceBottom(4)
      row()
      add(Nullable[Actor](buttonBar.createTable())).padBottom(3)

      addListeners()

      if (validator.isDefined) {
        addValidatableFieldListener(field)
        okButton.disabled = !field.isInputValid
      }

      pack()
      centerWindow()
    }

    override protected def close(): Unit = {
      super.close()
      listener.canceled()
    }

    override protected[sge] def setStage(stageArg: Nullable[Stage]): Unit = {
      super.setStage(stageArg)
      stageArg.foreach(_ => field.focusField())
    }

    def setText(text: String): InputDialog = setText(text, selectText = false)

    /** @param selectText
      *   if true text will be selected (this can be useful if you want to allow user quickly erase all text).
      */
    def setText(text: String, selectText: Boolean): InputDialog = {
      field.setText(Nullable(text))
      field.setCursorPosition(text.length)
      if (selectText) {
        field.selectAll()
      }
      this
    }

    private def addValidatableFieldListener(field: VisTextField): InputDialog = {
      field.addListener(
        new ChangeListener() {
          override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit =
            if (field.isInputValid) {
              okButton.disabled = false
            } else {
              okButton.disabled = true
            }
        }
      )
      this
    }

    private def addListeners(): Unit = {
      okButton.addListener(
        new ChangeListener() {
          override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit = {
            listener.finished(field.text)
            fadeOut()
          }
        }
      )

      cancelButton.addListener(new ChangeListener() {
        override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit =
          close()
      })

      field.addListener(
        new InputListener() {
          override def keyDown(event: InputEvent, keycode: Key): Boolean = {
            if (keycode == Keys.ENTER && !okButton.disabled) {
              listener.finished(field.text)
              fadeOut()
            }
            super.keyDown(event, keycode)
          }
        }
      )
    }
  }

  /** Dialog with text and buttons like Yes, No, Cancel. Can be used directly although you should use [[Dialogs]] showOptionDialog methods.
    */
  // NOTE: when updating this class, don't forget about Editor's DisableableOptionDialog
  class OptionDialog(title: String, text: String, dialogType: OptionDialogType, listener: OptionDialogListener)(using Sge) extends VisWindow(title) {

    private val buttonBar: ButtonBar = new ButtonBar()

    {
      isModal = true

      add(Nullable[Actor](new VisLabel(text, Align.center)))
      row()
      defaults().space(6)
      defaults().padBottom(3)

      buttonBar.ignoreSpacing = true

      val yesBtnListener = new ChangeListener() {
        override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit = {
          listener.yes()
          fadeOut()
        }
      }

      val noBtnListener = new ChangeListener() {
        override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit = {
          listener.no()
          fadeOut()
        }
      }

      val cancelBtnListener = new ChangeListener() {
        override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit = {
          listener.cancel()
          fadeOut()
        }
      }

      dialogType match {
        case OptionDialogType.YES_NO =>
          buttonBar.setButton(ButtonBar.ButtonType.YES, yesBtnListener)
          buttonBar.setButton(ButtonBar.ButtonType.NO, noBtnListener)
        case OptionDialogType.YES_CANCEL =>
          buttonBar.setButton(ButtonBar.ButtonType.YES, yesBtnListener)
          buttonBar.setButton(ButtonBar.ButtonType.CANCEL, cancelBtnListener)
        case OptionDialogType.YES_NO_CANCEL =>
          buttonBar.setButton(ButtonBar.ButtonType.YES, yesBtnListener)
          buttonBar.setButton(ButtonBar.ButtonType.NO, noBtnListener)
          buttonBar.setButton(ButtonBar.ButtonType.CANCEL, cancelBtnListener)
      }

      add(Nullable[Actor](buttonBar.createTable()))

      pack()
      centerWindow()
    }

    def setNoButtonText(text: String): OptionDialog = {
      buttonBar.getTextButton(ButtonBar.ButtonType.NO).setText(Nullable(text))
      pack()
      this
    }

    def setYesButtonText(text: String): OptionDialog = {
      buttonBar.getTextButton(ButtonBar.ButtonType.YES).setText(Nullable(text))
      pack()
      this
    }

    def setCancelButtonText(text: String): OptionDialog = {
      buttonBar.getTextButton(ButtonBar.ButtonType.CANCEL).setText(Nullable(text))
      pack()
      this
    }
  }

  /** Dialog with text and exception stacktrace available after pressing Details button. Can be used directly although you should use [[Dialogs]] showErrorDialog methods.
    */
  class DetailsDialog(text: String, title: String, details: Nullable[String])(using sge: Sge) extends VisDialog(title) {

    private val detailsTable:    VisTable      = new VisTable(true)
    private var detailsCell:     Cell[?]       = scala.compiletime.uninitialized
    private var _detailsVisible: Boolean       = false
    private var copyButton:      VisTextButton = scala.compiletime.uninitialized
    private var detailsLabel:    VisLabel      = scala.compiletime.uninitialized

    {
      this.text(text)

      details.foreach { det =>
        copyButton = new VisTextButton(Text.COPY.get)
        detailsLabel = new VisLabel(det)

        val sizes = VisUI.getSizes

        copyButton.addListener(
          new ChangeListener() {
            override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit = {
              sge.application.clipboard.contents = Nullable(detailsLabel.text.toString)
              copyButton.setText(Nullable(Text.COPIED.get))
            }
          }
        )

        detailsTable.add(Nullable[Actor](new VisLabel(Text.DETAILS_COLON.get))).left().expand().padTop(6)
        detailsTable.add(Nullable[Actor](copyButton))
        detailsTable.row()

        val innerDetailsTable = new VisTable()
        innerDetailsTable.add(Nullable[Actor](detailsLabel)).top().expand().fillX()
        detailsTable.add(Nullable[Actor](createScrollPane(innerDetailsTable))).colspan(2).minWidth(600 * sizes.scaleFactor).height(300 * sizes.scaleFactor)

        getContentTable.row()
        detailsCell = getContentTable.add(Nullable[Actor](detailsTable))
        detailsCell.setActor(Nullable.empty[Actor])
        button(Text.DETAILS.get, Nullable[AnyRef](Integer.valueOf(BUTTON_DETAILS)))
      }

      button(ButtonBar.ButtonType.OK.text, Nullable[AnyRef](Integer.valueOf(BUTTON_OK))).padBottom(3)
      pack()
      centerWindow()
    }

    override protected def result(obj: Nullable[AnyRef]): Unit =
      obj.foreach {
        case result: Integer if result.intValue() == BUTTON_DETAILS =>
          setDetailsVisible(!_detailsVisible)
          cancel()
        case _ => ()
      }

    def setWrapDetails(wrap: Boolean): Unit =
      detailsLabel.wrap = wrap

    def setCopyDetailsButtonVisible(visible: Boolean): Unit =
      copyButton.visible = visible

    def isCopyDetailsButtonVisible: Boolean =
      copyButton.visible

    /** Changes visibility of details pane. Note that Window must be added to Stage or Window won't be packed properly and it's size will be wrong. If Window is not added to Stage packing will be
      * performed next frame, if it is still not added at that point, Window size will be incorrect.
      */
    def setDetailsVisible(visible: Boolean): Unit =
      if (_detailsVisible == visible) {
        // already in desired state
      } else {
        _detailsVisible = visible
        detailsCell.setActor(if (detailsCell.hasActor) Nullable.empty[Actor] else Nullable[Actor](detailsTable))

        // looks like Stage is required to properly pack window
        // if it's null do packing next frame and hope that window have been already added to Stage at that point
        if (this.stage.isEmpty) {
          sge.application.postRunnable(new Runnable() {
            override def run(): Unit = {
              pack()
              centerWindow()
            }
          })
        } else {
          pack()
          centerWindow()
        }
      }

    def isDetailsVisible: Boolean = _detailsVisible
  }

  /** Dialog with title, text and n amount of buttons. Can be used directly although you should use [[Dialogs]] showConfirmDialog methods.
    * @author
    *   Javier
    * @author
    *   Kotcrab
    */
  class ConfirmDialog[T](title: String, text: String, buttons: Array[String], returns: Array[T], private val listener: ConfirmDialogListener[T])(using Sge) extends VisDialog(title) {

    {
      if (buttons.length != returns.length) {
        throw new IllegalStateException("buttons.length must be equal to returns.length")
      }

      this.text(new VisLabel(text, Align.center))
      defaults().padBottom(3)

      var i = 0
      while (i < buttons.length) {
        button(buttons(i), Nullable[AnyRef](returns(i).asInstanceOf[AnyRef]))
        i += 1
      }

      padBottom(3)
      pack()
      centerWindow()
    }

    override protected def result(obj: Nullable[AnyRef]): Unit =
      obj.foreach { o =>
        listener.result(o.asInstanceOf[T])
      }
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

  /** Used to get events from [[Dialogs]] confirm dialog. */
  trait ConfirmDialogListener[T] {

    /** Called when dialog button was pressed, type of results is generic and depends on created dialog. */
    def result(result: T): Unit
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
