// SGE — Android input method operations interface
//
// Self-contained (JDK types only). Abstracts soft keyboard control and
// native text input dialogs. Implemented in sge-jvm-platform-android using
// android.view.inputmethod.InputMethodManager and AlertDialog.

package sge
package platform
package android

/** Callback for native text input dialogs. */
trait InputDialogCallback {

  /** Called when the user confirms input. */
  def onInput(text: String): Unit

  /** Called when the user cancels the dialog. */
  def onCancel(): Unit
}

/** Input method operations for Android. Uses only JDK types. */
trait InputMethodOps {

  /** Shows the on-screen keyboard.
    * @param inputType
    *   Android InputType flags (int)
    */
  def showKeyboard(inputType: Int): Unit

  /** Hides the on-screen keyboard. */
  def hideKeyboard(): Unit

  /** Whether the on-screen keyboard is currently visible. */
  def isKeyboardShown: Boolean

  /** Current keyboard height in pixels, or 0 if hidden. */
  def keyboardHeight: Int

  /** Sets the keyboard height (called by keyboard height observer). */
  def setKeyboardHeight(height: Int): Unit

  /** Shows a native text input dialog.
    * @param title
    *   dialog title
    * @param text
    *   initial text value
    * @param hint
    *   placeholder hint text
    * @param maxLength
    *   maximum character count
    * @param inputType
    *   Android InputType flags
    * @param callback
    *   result callback
    */
  def showTextInputDialog(
    title:     String,
    text:      String,
    hint:      String,
    maxLength: Int,
    inputType: Int,
    callback:  InputDialogCallback
  ): Unit

  /** Opens a native text input field at the bottom of the screen.
    *
    * @param text
    *   initial text content
    * @param selectionStart
    *   initial selection start index
    * @param selectionEnd
    *   initial selection end index
    * @param inputType
    *   Android InputType flags (int)
    * @param maxLength
    *   maximum character count, or 0 for unlimited
    * @param placeholder
    *   hint text shown when empty
    * @param maskInput
    *   whether to mask input (password mode)
    * @param multiLine
    *   whether to allow multiline input
    * @param preventCorrection
    *   whether to disable auto-correct/suggestions
    * @param autoComplete
    *   optional list of auto-complete suggestions, or null
    * @param onTextChanged
    *   callback invoked when text changes: (text, selStart, selEnd)
    * @param onClose
    *   callback invoked on close: confirmative → keepOpen
    * @param validator
    *   optional character-by-character validator, or null. Returns true if the string is acceptable.
    */
  def openNativeTextField(
    text:              String,
    selectionStart:    Int,
    selectionEnd:      Int,
    inputType:         Int,
    maxLength:         Int,
    placeholder:       String,
    maskInput:         Boolean,
    multiLine:         Boolean,
    preventCorrection: Boolean,
    autoComplete:      Array[String],
    onTextChanged:     (String, Int, Int) => Unit,
    onClose:           Boolean => Boolean,
    validator:         String => Boolean
  ): Unit

  /** Closes the native text input field.
    * @param confirmative
    *   whether the close is a confirmative action (e.g. pressing Done)
    */
  def closeNativeTextField(confirmative: Boolean): Unit

  /** Whether a native text input field is currently open. */
  def isNativeTextFieldOpen: Boolean

  /** Sets the view on which the keyboard operates.
    * @param view
    *   the Android View (as AnyRef)
    */
  def setView(view: AnyRef): Unit
}
