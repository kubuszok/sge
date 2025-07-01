package sge
package input

class NativeInputConfiguration {

  private var inputType:         sge.Input.OnscreenKeyboardType.OnscreenKeyboardType = sge.Input.OnscreenKeyboardType.Default
  private var preventCorrection: Boolean                                             = false

  private var textInputWrapper:   TextInputWrapper               = scala.compiletime.uninitialized
  private var multiLine:          Boolean                        = false
  private var maxLength:          Option[Int]                    = None
  private var validator:          sge.Input.InputStringValidator = scala.compiletime.uninitialized
  private var placeholder:        String                         = ""
  private var showPasswordButton: Boolean                        = false
  private var autoComplete:       Array[String]                  = null

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

  def getAutoComplete(): Array[String] =
    autoComplete

  def setAutoComplete(autoComplete: Array[String]): NativeInputConfiguration = {
    this.autoComplete = autoComplete
    this
  }

  def validate(): Unit = {
    var message: String = null
    if (inputType == null) message = "OnscreenKeyboardType needs to be non null"
    if (textInputWrapper == null) message = "TextInputWrapper needs to be non null"
    if (showPasswordButton && inputType != sge.Input.OnscreenKeyboardType.Password)
      message = "ShowPasswordButton only works with OnscreenKeyboardType.Password"
    if (placeholder == null) message = "Placeholder needs to be non null"
    if (autoComplete != null && inputType != sge.Input.OnscreenKeyboardType.Default)
      message = "AutoComplete should only be used with OnscreenKeyboardType.Default"
    if (autoComplete != null && multiLine) message = "AutoComplete shouldn't be used with multiline"

    if (message != null) {
      throw new IllegalArgumentException("NativeInputConfiguration validation failed: " + message)
    }
  }
}
