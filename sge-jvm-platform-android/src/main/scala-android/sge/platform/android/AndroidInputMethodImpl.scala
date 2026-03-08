// SGE — Android input method implementation
//
// Uses android.view.inputmethod.InputMethodManager for keyboard control
// and AlertDialog for native text input dialogs.
//
// Migration notes:
//   Source:  com.badlogic.gdx.backends.android.DefaultAndroidInput (keyboard/dialog part)
//   Renames: DefaultAndroidInput keyboard → AndroidInputMethodImpl
//   Convention: ops interface pattern; _root_.android.* imports; Build.VERSION checks
//   Audited: 2026-03-08

package sge
package platform
package android

import _root_.android.app.AlertDialog
import _root_.android.content.{Context, DialogInterface}
import _root_.android.os.Handler
import _root_.android.text.{InputFilter, InputType => AndroidInputType}
import _root_.android.view.View
import _root_.android.view.inputmethod.InputMethodManager
import _root_.android.widget.EditText

class AndroidInputMethodImpl(context: Context, handler: Handler) extends InputMethodOps {

  private val imm: InputMethodManager =
    context.getSystemService(Context.INPUT_METHOD_SERVICE).asInstanceOf[InputMethodManager]

  private var _view: View       = _
  private var _keyboardShown    = false
  private var _keyboardHeight   = 0

  override def showKeyboard(inputType: Int): Unit = {
    if (_view != null) {
      handler.post(new Runnable {
        override def run(): Unit = {
          _view.setFocusable(true)
          _view.setFocusableInTouchMode(true)
          if (_view.requestFocus()) {
            imm.showSoftInput(_view, InputMethodManager.SHOW_IMPLICIT)
            _keyboardShown = true
          }
        }
      })
    }
  }

  override def hideKeyboard(): Unit = {
    if (_view != null) {
      handler.post(new Runnable {
        override def run(): Unit = {
          imm.hideSoftInputFromWindow(_view.getWindowToken, 0)
          _keyboardShown = false
          _keyboardHeight = 0
        }
      })
    }
  }

  override def isKeyboardShown: Boolean = _keyboardShown

  override def keyboardHeight: Int = _keyboardHeight

  override def setKeyboardHeight(height: Int): Unit = {
    _keyboardHeight = height
    _keyboardShown = height > 0
  }

  override def showTextInputDialog(
      title: String,
      text: String,
      hint: String,
      maxLength: Int,
      inputType: Int,
      callback: InputDialogCallback
  ): Unit = {
    handler.post(new Runnable {
      override def run(): Unit = {
        val builder = new AlertDialog.Builder(context)
        builder.setTitle(title)
        val input = new EditText(context)
        if (text != null) input.setText(text)
        if (hint != null) input.setHint(hint)
        input.setInputType(inputType)
        if (maxLength > 0) {
          input.setFilters(Array[InputFilter](new InputFilter.LengthFilter(maxLength)))
        }
        builder.setView(input)
        builder.setPositiveButton(
          "OK",
          new DialogInterface.OnClickListener {
            override def onClick(dialog: DialogInterface, which: Int): Unit = {
              callback.onInput(input.getText.toString)
            }
          }
        )
        builder.setNegativeButton(
          "Cancel",
          new DialogInterface.OnClickListener {
            override def onClick(dialog: DialogInterface, which: Int): Unit = {
              callback.onCancel()
            }
          }
        )
        builder.setOnCancelListener(new DialogInterface.OnCancelListener {
          override def onCancel(dialog: DialogInterface): Unit = {
            callback.onCancel()
          }
        })
        builder.show()
      }
    })
  }

  override def setView(view: AnyRef): Unit = {
    _view = view.asInstanceOf[View]
  }
}
