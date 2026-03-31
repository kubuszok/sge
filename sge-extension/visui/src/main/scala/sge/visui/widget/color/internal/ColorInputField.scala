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
package color
package internal

import sge.scenes.scene2d.{ Actor, InputEvent, InputListener }
import sge.scenes.scene2d.ui.TextField
import sge.scenes.scene2d.utils.{ ChangeListener, FocusListener, UIUtils }
import sge.utils.Nullable
import sge.visui.util.InputValidator
import sge.visui.widget.VisValidatableTextField

/** Fields used to enter color numbers in color picker, verifies max allowed value provides quick increment/decrement of current value by pressing [shift +] plus or minus on numpad.
  * @author
  *   Kotcrab
  */
class ColorInputField(private val maxValue: Int, listener: ColorInputField.ColorInputFieldListener)(using Sge)
    extends VisValidatableTextField(new ColorInputField.ColorFieldValidator(maxValue)) {

  private var _value: Int = 0

  setProgrammaticChangeEvents(false)
  setMaxLength(3)
  setTextFieldFilter(Nullable(new ColorInputField.NumberFilter()))

  addListener(new ChangeListener() {
    override def changed(event: ChangeListener.ChangeEvent, actor: Actor): Unit = {
      if (text.length > 0) {
        _value = Integer.valueOf(text).intValue()
      }
    }
  })

  addListener(new InputListener() {
    override def keyTyped(event: InputEvent, character: Char): Boolean = {
      val field = event.listenerActor.get.asInstanceOf[ColorInputField]
      if (character == '+') field.changeValue(if (UIUtils.shift()) 10 else 1)
      if (character == '-') field.changeValue(if (UIUtils.shift()) -10 else -1)
      if (character != 0) listener.changed(getValue)
      true
    }
  })

  addListener(new FocusListener() {
    override def keyboardFocusChanged(event: FocusListener.FocusEvent, actor: Actor, focused: Boolean): Unit = {
      if (!focused && !isInputValid) {
        // only possibility on invalid field is that entered value will be bigger than maxValue so we set field value to maxValue
        setValue(maxValue)
      }
    }
  })

  def changeValue(byValue: Int): Unit = {
    _value += byValue
    if (_value > maxValue) _value = maxValue
    if (_value < 0) _value = 0
    updateUI()
  }

  def getValue: Int = _value

  def setValue(value: Int): Unit = {
    _value = value
    updateUI()
  }

  private def updateUI(): Unit = {
    setText(Nullable(String.valueOf(_value)))
    setCursorPosition(getMaxLength)
  }
}

object ColorInputField {

  trait ColorInputFieldListener {
    def changed(newValue: Int): Unit
  }

  private class NumberFilter extends TextField.TextFieldFilter {
    override def acceptChar(textField: TextField, c: Char): Boolean = {
      Character.isDigit(c)
    }
  }

  private class ColorFieldValidator(maxValue: Int) extends InputValidator {
    override def validateInput(input: String): Boolean = {
      if (input.isEmpty) false
      else {
        val number = Integer.parseInt(input)
        number <= maxValue
      }
    }
  }
}
