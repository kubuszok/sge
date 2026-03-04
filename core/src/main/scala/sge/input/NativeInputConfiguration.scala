/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/input/NativeInputConfiguration.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: showUnmaskButton -> showPasswordButton (semantics differ from Java)
 *   Convention: maxLength: int (-1 sentinel) -> Option[Int]; autoComplete: String[] -> Nullable[Array[String]]
 *   Idiom: Nullable (1 null), split packages
 *   TODOs: 0
 *   Missing: maskInput field + isMaskInput/setMaskInput; NativeInputCloseCallback interface + closeCallback field + get/set
 *   Missing: validate() null-safety checks for type/placeholder/closeCallback (non-applicable in Scala, but semantics diverge)
 *   TODO: Java-style getters/setters — getType/setType, getMaxLength/setMaxLength, getValidator/setValidator, getPlaceholder/setPlaceholder, etc.
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package input

import sge.utils.Nullable

class NativeInputConfiguration {

  private var inputType:         sge.Input.OnscreenKeyboardType.OnscreenKeyboardType = sge.Input.OnscreenKeyboardType.Default
  private var preventCorrection: Boolean                                             = false

  private var textInputWrapper:   TextInputWrapper               = scala.compiletime.uninitialized
  private var multiLine:          Boolean                        = false
  private var maxLength:          Option[Int]                    = None
  private var validator:          sge.Input.InputStringValidator = scala.compiletime.uninitialized
  private var placeholder:        String                         = ""
  private var showPasswordButton: Boolean                        = false
  private var autoComplete:       Nullable[Array[String]]        = Nullable.empty

  def getType(): sge.Input.OnscreenKeyboardType.OnscreenKeyboardType =
    inputType

  /** @param type which type of keyboard we wish to display. */
  def setType(`type`: sge.Input.OnscreenKeyboardType.OnscreenKeyboardType): NativeInputConfiguration = {
    this.inputType = `type`
    this
  }

  def isPreventCorrection(): Boolean =
    preventCorrection

  /** @param preventCorrection Disable autocomplete/correction */
  def setPreventCorrection(preventCorrection: Boolean): NativeInputConfiguration = {
    this.preventCorrection = preventCorrection
    this
  }

  def getTextInputWrapper(): input.TextInputWrapper =
    textInputWrapper

  /** @param textInputWrapper Should provide access to the backed input field. */
  def setTextInputWrapper(textInputWrapper: input.TextInputWrapper): NativeInputConfiguration = {
    this.textInputWrapper = textInputWrapper
    this
  }

  def isMultiLine(): Boolean =
    multiLine

  /** @param multiLine whether the keyboard should accept multiple lines. */
  def setMultiLine(multiLine: Boolean): NativeInputConfiguration = {
    this.multiLine = multiLine
    this
  }

  def getMaxLength(): Option[Int] =
    maxLength

  /** @param maxLength What the text length limit should be. */
  def setMaxLength(maxLength: Option[Int]): NativeInputConfiguration = {
    this.maxLength = maxLength
    this
  }

  def getValidator(): sge.Input.InputStringValidator =
    validator

  /** @param validator Can validate the input from the keyboard and reject. */
  def setValidator(validator: sge.Input.InputStringValidator): NativeInputConfiguration = {
    this.validator = validator
    this
  }

  def getPlaceholder(): String =
    placeholder

  /** @param placeholder String to show, if nothing is inputted yet. */
  def setPlaceholder(placeholder: String): NativeInputConfiguration = {
    this.placeholder = placeholder
    this
  }

  def isShowPasswordButton(): Boolean =
    showPasswordButton

  /** @param showPasswordButton Whether to show a button to show unhidden password */
  def setShowPasswordButton(showPasswordButton: Boolean): NativeInputConfiguration = {
    this.showPasswordButton = showPasswordButton
    this
  }

  def getAutoComplete(): Nullable[Array[String]] =
    autoComplete

  def setAutoComplete(autoComplete: Nullable[Array[String]]): NativeInputConfiguration = {
    this.autoComplete = autoComplete
    this
  }

  def validate(): Unit = {
    var message: Nullable[String] = Nullable.empty
    if (Nullable(textInputWrapper).isEmpty) message = Nullable("TextInputWrapper needs to be non null")
    if (showPasswordButton && inputType != sge.Input.OnscreenKeyboardType.Password)
      message = Nullable("ShowPasswordButton only works with OnscreenKeyboardType.Password")
    if (autoComplete.isDefined && inputType != sge.Input.OnscreenKeyboardType.Default)
      message = Nullable("AutoComplete should only be used with OnscreenKeyboardType.Default")
    if (autoComplete.isDefined && multiLine) message = Nullable("AutoComplete shouldn't be used with multiline")

    message.foreach { m =>
      throw new IllegalArgumentException("NativeInputConfiguration validation failed: " + m)
    }
  }
}
