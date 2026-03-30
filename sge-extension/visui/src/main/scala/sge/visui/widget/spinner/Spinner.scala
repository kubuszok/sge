/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package visui
package widget
package spinner

import scala.language.implicitConversions

import sge.Input.{ Key, Keys }
import sge.scenes.scene2d.{ Actor, InputEvent, InputListener }
import sge.scenes.scene2d.ui.Cell
import sge.scenes.scene2d.utils.{ ChangeListener, Disableable, Drawable, FocusListener }
import sge.utils.{ Nullable, Seconds, Timer }
import sge.visui.{ Sizes, VisUI }

/** Spinner can be used to select number or object using up and down buttons or by entering value into text field. Supports custom models that allows selecting either int, floats or even custom
  * objects.
  *
  * Fires [[ChangeListener.ChangeEvent]] when value has changed however unlike some other widgets canceling the event won't undo value change.
  * @author
  *   Kotcrab
  * @see
  *   [[SimpleFloatSpinnerModel]]
  * @see
  *   [[FloatSpinnerModel]]
  * @see
  *   [[IntSpinnerModel]]
  * @see
  *   [[ArraySpinnerModel]]
  * @since 1.0.2
  */
class Spinner(style: Spinner.SpinnerStyle, sizes: Sizes, name: String, private var _model: SpinnerModel)(using Sge) extends VisTable() with Disableable {

  // task is shared between two buttons
  private val buttonRepeatTask: Spinner.ButtonRepeatTask = new Spinner.ButtonRepeatTask(() => this)

  private val upButton:       VisImageButton                = new VisImageButton(style.up)
  private val downButton:     VisImageButton                = new VisImageButton(style.down)
  private var _textFieldCell: Cell[VisValidatableTextField] = scala.compiletime.uninitialized
  private var _labelCell:     Cell[VisLabel]                = scala.compiletime.uninitialized

  private var _textFieldEventPolicy:     Spinner.TextFieldEventPolicy = Spinner.TextFieldEventPolicy.ON_FOCUS_LOST
  private var _programmaticChangeEvents: Boolean                      = true
  private var _disabled:                 Boolean                      = false

  {
    val buttonsTable   = new VisTable()
    val textFieldLocal = createTextField()
    buttonsTable.add(Nullable[Actor](upButton)).height(sizes.spinnerButtonHeight).row()
    buttonsTable.add(Nullable[Actor](downButton)).height(sizes.spinnerButtonHeight)

    _labelCell = add(Nullable[Actor](new VisLabel(""))).asInstanceOf[Cell[VisLabel]]
    selectorName = name

    _textFieldCell = add(Nullable[Actor](textFieldLocal)).height(sizes.spinnerButtonHeight * 2).growX().asInstanceOf[Cell[VisValidatableTextField]]
    add(Nullable[Actor](buttonsTable))

    addButtonsListeners(upButton, downButton)

    _model.bind(this)
  }

  def this(name: String, model: SpinnerModel)(using Sge) =
    this(VisUI.getSkin.get[Spinner.SpinnerStyle], VisUI.getSizes, name, model)

  def this(styleName: String, name: String, model: SpinnerModel)(using Sge) =
    this(VisUI.getSkin.get[Spinner.SpinnerStyle](styleName), VisUI.getSizes, name, model)

  private def createTextField(): VisValidatableTextField = {
    val textFieldLocal = new VisValidatableTextField() {
      override def prefWidth: Float = sizes.spinnerFieldSize
    }
    textFieldLocal.setRestoreLastValid(true)
    textFieldLocal.setProgrammaticChangeEvents(false)
    addTextFieldListeners(textFieldLocal)
    textFieldLocal
  }

  def model: SpinnerModel = _model

  def model_=(model: SpinnerModel): Unit = {
    _model = model
    _textFieldCell.setActor(createTextField())
    model.bind(this)
  }

  private def addButtonsListeners(upBtn: VisImageButton, downBtn: VisImageButton): Unit = {
    upBtn.addListener(
      new ChangeListener() {
        override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit = {
          event.stop()
          stage.foreach(_.setScrollFocus(Nullable[Actor](textField)))
          incrementInternal(fireEvent = true)
        }
      }
    )

    downBtn.addListener(
      new ChangeListener() {
        override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit = {
          event.stop()
          stage.foreach(_.setScrollFocus(Nullable[Actor](textField)))
          decrementInternal(fireEvent = true)
        }
      }
    )

    upBtn.addListener(new Spinner.ButtonInputListener(buttonRepeatTask, advance = true))
    downBtn.addListener(new Spinner.ButtonInputListener(buttonRepeatTask, advance = false))
  }

  private def addTextFieldListeners(textFieldLocal: VisTextField): Unit = {
    textFieldLocal.addListener(
      new ChangeListener() {
        override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit = {
          event.stop()
          _model.textChanged()
          if (textField.isInputValid && _textFieldEventPolicy == Spinner.TextFieldEventPolicy.ON_KEY_TYPED) {
            notifyValueChanged(true)
          }
        }
      }
    )

    textFieldLocal.addListener(
      new FocusListener() {
        override def keyboardFocusChanged(event: FocusListener.FocusEvent, actor: Actor, focused: Boolean): Unit =
          if (!focused) {
            stage.foreach(_.setScrollFocus(Nullable.empty))
            if (_textFieldEventPolicy == Spinner.TextFieldEventPolicy.ON_FOCUS_LOST) {
              notifyValueChanged(true)
            }
          }
      }
    )

    textFieldLocal.addListener(
      new InputListener() {
        override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: sge.Input.Button): Boolean = {
          stage.foreach(_.setScrollFocus(Nullable[Actor](textField)))
          true
        }

        override def scrolled(event: InputEvent, x: Float, y: Float, amountX: Float, amountY: Float): Boolean = {
          if (_disabled) return false
          if (amountY >= 1) {
            decrementInternal(fireEvent = true)
          } else if (amountY <= -1) {
            incrementInternal(fireEvent = true)
          }
          true
        }

        override def keyDown(event: InputEvent, keycode: Key): Boolean = {
          if (keycode == Keys.ENTER) {
            notifyValueChanged(true)
            return true
          }
          false
        }
      }
    )
  }

  override def disabled_=(disabled: Boolean): Unit = {
    _disabled = disabled
    upButton.disabled = disabled
    downButton.disabled = disabled
    textField.disabled = disabled
  }

  override def disabled: Boolean = _disabled

  def selectorName_=(name: String): Unit = {
    _labelCell.getActor.foreach(_.setText(Nullable(name)))
    if (name == null || name.isEmpty) { // @nowarn -- Java interop boundary for setText
      _labelCell.padRight(0)
    } else {
      _labelCell.padRight(6)
    }
  }

  def selectorName: String = _labelCell.getActor.map(_.text.toString).getOrElse("")

  def increment(): Unit = _model.increment(_programmaticChangeEvents)

  private def incrementInternal(fireEvent: Boolean): Unit = _model.increment(fireEvent)

  def decrement(): Unit = _model.decrement(_programmaticChangeEvents)

  private def decrementInternal(fireEvent: Boolean): Unit = _model.decrement(fireEvent)

  /** If false, methods changing spinner value from code won't trigger change event, it will be fired only when user has changed value.
    */
  def programmaticChangeEvents_=(value: Boolean): Unit    = _programmaticChangeEvents = value
  def programmaticChangeEvents:                   Boolean = _programmaticChangeEvents

  def textFieldEventPolicy_=(policy: Spinner.TextFieldEventPolicy): Unit                         = _textFieldEventPolicy = policy
  def textFieldEventPolicy:                                         Spinner.TextFieldEventPolicy = _textFieldEventPolicy

  def maxLength:                   Int  = textField.getMaxLength
  def maxLength_=(maxLength: Int): Unit = textField.setMaxLength(maxLength)

  /** Called by [[SpinnerModel]]. Notifies when underlying model value has changed and spinner text field must updated. Typically there is no need to call this method manually.
    * @param fireEvent
    *   if true then [[ChangeListener.ChangeEvent]] will be fired
    */
  def notifyValueChanged(fireEvent: Boolean): Unit = {
    val tf     = textField
    val cursor = tf.cursorPosition
    tf.setCursorPosition(0)
    tf.setText(Nullable(_model.text))
    tf.setCursorPosition(cursor)

    if (fireEvent) {
      val changeEvent = Actor.POOLS.obtain[ChangeListener.ChangeEvent]
      fire(changeEvent)
      Actor.POOLS.free(changeEvent)
    }
  }

  def textField: VisValidatableTextField = _textFieldCell.getActor.get
}

object Spinner {

  class SpinnerStyle {
    var up:   Drawable = scala.compiletime.uninitialized
    var down: Drawable = scala.compiletime.uninitialized

    def this(style: SpinnerStyle) = {
      this()
      this.up = style.up
      this.down = style.down
    }

    def this(up: Drawable, down: Drawable) = {
      this()
      this.up = up
      this.down = down
    }
  }

  private class ButtonRepeatTask(spinnerRef: () => Spinner)(using Sge) extends Timer.Task {
    var advance: Boolean = false

    override def run(): Unit = {
      val spinner = spinnerRef()
      if (advance) {
        spinner.incrementInternal(fireEvent = true)
      } else {
        spinner.decrementInternal(fireEvent = true)
      }
    }
  }

  /** Allows to configure how [[Spinner]] will fire [[ChangeListener.ChangeEvent]] after user interaction with Spinner text field.
    * @since 1.1.6
    */
  enum TextFieldEventPolicy {

    /** Spinner change event will be only fired after user has pressed enter in text field. This mode is the default one prior to VisUI 1.1.6
      */
    case ON_ENTER_ONLY

    /** Spinner change event will be always fired after text field has lost focus and entered value is valid. Note that event will be fired even if user has not changed actual value of spinner. Event
      * won't be fired if current model determined that entered value is invalid. This mode is the default one.
      */
    case ON_FOCUS_LOST

    /** Spinner change event will be fired right after user has typed something in the text field and model has determined that entered value is valid. Event won't be fired if entered value is
      * invalid.
      */
    case ON_KEY_TYPED
  }

  private class ButtonInputListener(buttonRepeatTask: ButtonRepeatTask, advance: Boolean)(using Sge) extends InputListener {
    private val buttonRepeatInitialTime: Seconds = Seconds(0.4f)
    private val buttonRepeatTime:        Seconds = Seconds(0.08f)

    override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: sge.Input.Button): Boolean = {
      if (!buttonRepeatTask.isScheduled) {
        buttonRepeatTask.advance = advance
        buttonRepeatTask.cancel()
        Timer.schedule(buttonRepeatTask, buttonRepeatInitialTime, buttonRepeatTime)
      }
      true
    }

    override def touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: sge.Input.Button): Unit =
      buttonRepeatTask.cancel()
  }
}
