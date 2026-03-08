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

  /** Sets the view on which the keyboard operates.
    * @param view
    *   the Android View (as AnyRef)
    */
  def setView(view: AnyRef): Unit
}
