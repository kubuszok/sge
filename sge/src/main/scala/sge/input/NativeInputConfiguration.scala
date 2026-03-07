/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/input/NativeInputConfiguration.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: maxLength: int (-1 sentinel) -> Option[Int]; autoComplete: String[] -> Nullable[Array[String]]
 *   Convention: NativeInputCloseCallback Java interface -> Scala trait (SAM)
 *   Convention: builder-pattern setters return NativeInputConfiguration (intentionally kept as methods)
 *   Idiom: Nullable (2 null), split packages
 *   Audited: 2026-03-04
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package input

import sge.utils.Nullable

class NativeInputConfiguration {

  private var inputType:         sge.Input.OnscreenKeyboardType = sge.Input.OnscreenKeyboardType.Default
  private var preventCorrection: Boolean                        = false

  private var textInputWrapper: TextInputWrapper               = scala.compiletime.uninitialized
  private var multiLine:        Boolean                        = false
  private var maxLength:        Option[Int]                    = None
  private var validator:        sge.Input.InputStringValidator = scala.compiletime.uninitialized
  private var placeholder:      String                         = ""
  private var maskInput:        Boolean                        = false
  private var showUnmaskButton: Boolean                        = false
  private var autoComplete:     Nullable[Array[String]]        = Nullable.empty

  private var closeCallback: NativeInputConfiguration.NativeInputCloseCallback = (_: Boolean) => false

  def getType(): sge.Input.OnscreenKeyboardType =
    inputType

  /** @param type which type of keyboard we wish to display. */
  def setType(`type`: sge.Input.OnscreenKeyboardType): NativeInputConfiguration = {
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

  /** @param maskInput Whether to hide the text input while typing (usually for passwords) */
  def setMaskInput(maskInput: Boolean): NativeInputConfiguration = {
    this.maskInput = maskInput
    this
  }

  def isMaskInput(): Boolean =
    maskInput

  def isShowUnmaskButton(): Boolean =
    showUnmaskButton

  /** @param showUnmaskButton Whether to show a button to show unhidden password */
  def setShowUnmaskButton(showUnmaskButton: Boolean): NativeInputConfiguration = {
    this.showUnmaskButton = showUnmaskButton
    this
  }

  def getAutoComplete(): Nullable[Array[String]] =
    autoComplete

  /** Sets a list of autocompletable strings to present the user while typing */
  def setAutoComplete(autoComplete: Nullable[Array[String]]): NativeInputConfiguration = {
    this.autoComplete = autoComplete
    this
  }

  def getCloseCallback(): NativeInputConfiguration.NativeInputCloseCallback =
    closeCallback

  /** Installing a callback for when the native input is closed. See {@link NativeInputCloseCallback} for more information */
  def setCloseCallback(closeCallback: NativeInputConfiguration.NativeInputCloseCallback): NativeInputConfiguration = {
    this.closeCallback = closeCallback
    this
  }

  def validate(): Unit = {
    var message: Nullable[String] = Nullable.empty
    if (Nullable(textInputWrapper).isEmpty) message = Nullable("TextInputWrapper needs to be non null")
    if (showUnmaskButton && !maskInput) message = Nullable("ShowUnmaskButton only works with MaskInput")
    if (autoComplete.isDefined && inputType != sge.Input.OnscreenKeyboardType.Default)
      message = Nullable("AutoComplete should only be used with OnscreenKeyboardType.Default")
    if (autoComplete.isDefined && multiLine) message = Nullable("AutoComplete shouldn't be used with multiline")

    message.foreach { m =>
      throw new IllegalArgumentException("NativeInputConfiguration validation failed: " + m)
    }
  }
}

object NativeInputConfiguration {

  /** This will be called on the main thread, when the closing of a native input is processed. This does not mean, that the keyboard is already hidden. You can schedule a new `openTextInputField` call
    * here.
    */
  trait NativeInputCloseCallback {

    /** @param confirmativeAction
      *   Whether the way the keyboard was closed can be considered a confirmative action e.g. to advance the UI
      * @return
      *   Whether the keyboard should be kept open to be opened again soon. e.g. when advancing through multiple text fields
      */
    def onClose(confirmativeAction: Boolean): Boolean
  }
}
