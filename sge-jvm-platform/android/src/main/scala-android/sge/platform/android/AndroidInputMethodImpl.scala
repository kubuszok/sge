// SGE — Android input method implementation
//
// Uses android.view.inputmethod.InputMethodManager for keyboard control,
// AlertDialog for native text input dialogs, and AutoCompleteTextView for
// inline native text input fields.
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
import _root_.android.content.{ Context, DialogInterface }
import _root_.android.graphics.Color
import _root_.android.os.Handler
import _root_.android.text.{ Editable, InputFilter, InputType => AndroidInputType, TextWatcher }
import _root_.android.text.method.PasswordTransformationMethod
import _root_.android.view.{ KeyEvent, View }
import _root_.android.view.inputmethod.{ EditorInfo, InputMethodManager }
import _root_.android.widget.{ ArrayAdapter, AutoCompleteTextView, EditText, RelativeLayout, TextView }
import scala.compiletime.uninitialized

class AndroidInputMethodImpl(context: Context, handler: Handler) extends InputMethodOps {

  private val imm: InputMethodManager =
    context.getSystemService(Context.INPUT_METHOD_SERVICE).asInstanceOf[InputMethodManager]

  private var _view: View = uninitialized
  private var _keyboardShown  = false
  private var _keyboardHeight = 0

  // ── Native text field state ──────────────────────────────────────────

  private var _nativeFieldOpen = false
  private var _nativeLayout:   RelativeLayout             = uninitialized
  private var _nativeEditText: AutoCompleteTextView       = uninitialized
  private var _onTextChanged:  (String, Int, Int) => Unit = uninitialized
  private var _onClose:        Boolean => Boolean         = uninitialized

  override def showKeyboard(inputType: Int): Unit =
    if (_view != null) {
      handler.post(
        new Runnable {
          override def run(): Unit = {
            _view.setFocusable(true)
            _view.setFocusableInTouchMode(true)
            if (_view.requestFocus()) {
              imm.showSoftInput(_view, InputMethodManager.SHOW_IMPLICIT)
              _keyboardShown = true
            }
          }
        }
      )
    }

  override def hideKeyboard(): Unit =
    if (_view != null) {
      handler.post(
        new Runnable {
          override def run(): Unit = {
            imm.hideSoftInputFromWindow(_view.getWindowToken, 0)
            _keyboardShown = false
            _keyboardHeight = 0
          }
        }
      )
    }

  override def isKeyboardShown: Boolean = _keyboardShown

  override def keyboardHeight: Int = _keyboardHeight

  override def setKeyboardHeight(height: Int): Unit = {
    _keyboardHeight = height
    _keyboardShown = height > 0
  }

  override def showTextInputDialog(
    title:     String,
    text:      String,
    hint:      String,
    maxLength: Int,
    inputType: Int,
    callback:  InputDialogCallback
  ): Unit =
    handler.post(
      new Runnable {
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
              override def onClick(dialog: DialogInterface, which: Int): Unit =
                callback.onInput(input.getText.toString)
            }
          )
          builder.setNegativeButton(
            "Cancel",
            new DialogInterface.OnClickListener {
              override def onClick(dialog: DialogInterface, which: Int): Unit =
                callback.onCancel()
            }
          )
          builder.setOnCancelListener(new DialogInterface.OnCancelListener {
            override def onCancel(dialog: DialogInterface): Unit =
              callback.onCancel()
          })
          builder.show()
        }
      }
    )

  // ── Native text input field ──────────────────────────────────────────

  override def openNativeTextField(
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
  ): Unit = {
    handler.post(
      new Runnable {
        override def run(): Unit = {
          _onTextChanged = onTextChanged
          _onClose = onClose

          ensureNativeFieldCreated()

          val editText = _nativeEditText

          // Remove existing text watchers by setting empty filters first, then text
          editText.setFilters(Array.empty[InputFilter])
          editText.removeTextChangedListener(null) // no-op but safe

          // Transformation method must be set before input type (Android quirk)
          if (maskInput) {
            editText.setTransformationMethod(PasswordTransformationMethod.getInstance())
          } else {
            editText.setTransformationMethod(null)
          }

          // Build input type flags
          var effectiveInputType = inputType
          if (multiLine) {
            effectiveInputType |= AndroidInputType.TYPE_TEXT_FLAG_MULTI_LINE
          }
          if (preventCorrection) {
            effectiveInputType |= AndroidInputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
          } else {
            effectiveInputType |= (AndroidInputType.TYPE_TEXT_FLAG_CAP_SENTENCES | AndroidInputType.TYPE_TEXT_FLAG_AUTO_CORRECT)
          }
          editText.setInputType(effectiveInputType)

          // IME options
          editText.setImeOptions(EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_FULLSCREEN)

          // Multiline sizing
          if (multiLine) {
            editText.setSingleLine(false)
            editText.setMaxLines(3)
          } else {
            editText.setSingleLine(true)
            editText.setMaxLines(1)
          }

          // Set text and hint
          if (text != null) editText.setText(text)
          if (placeholder != null) editText.setHint(placeholder)

          // Build input filters
          val filters = scala.collection.mutable.ArrayBuffer.empty[InputFilter]

          // Validator filter: validates each proposed character change
          if (validator != null) {
            filters += new InputFilter {
              override def filter(
                source: CharSequence,
                start:  Int,
                end:    Int,
                dest:   _root_.android.text.Spanned,
                dstart: Int,
                dend:   Int
              ): CharSequence = {
                val sb = new StringBuilder(dest.toString)
                sb.replace(dstart, dend, source.subSequence(start, end).toString)
                if (validator(sb.toString)) null // accept
                else "" // reject
              }
            }
          }

          // Max length filter
          if (maxLength > 0) {
            filters += new InputFilter.LengthFilter(maxLength)
          }
          editText.setFilters(filters.toArray)

          // Auto-complete adapter
          if (autoComplete != null && autoComplete.length > 0) {
            editText.setAdapter(
              new ArrayAdapter[String](
                context,
                _root_.android.R.layout.simple_dropdown_item_1line,
                autoComplete
              )
            )
            editText.setThreshold(1)
          } else {
            editText.setAdapter(null.asInstanceOf[ArrayAdapter[String]])
          }

          // Editor action listener for IME_ACTION_DONE
          editText.setOnEditorActionListener(
            new TextView.OnEditorActionListener {
              override def onEditorAction(v: TextView, actionId: Int, event: KeyEvent): Boolean =
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                  closeNativeTextField(true)
                  true
                } else {
                  false
                }
            }
          )

          // Text change listener — relay to callback
          editText.addTextChangedListener(
            new TextWatcher {
              override def beforeTextChanged(s: CharSequence, start: Int, count:  Int, after: Int): Unit = ()
              override def onTextChanged(s:     CharSequence, start: Int, before: Int, count: Int): Unit = ()
              override def afterTextChanged(s: Editable):                                           Unit = {
                val cb = _onTextChanged
                if (cb != null) {
                  cb(s.toString, editText.getSelectionStart, editText.getSelectionEnd)
                }
              }
            }
          )

          // Restore selection — must be done after all transformation/input type changes
          val len = editText.getText.length
          val ss  = Math.min(selectionStart, len)
          val se  = Math.min(selectionEnd, len)
          editText.setSelection(Math.max(0, ss), Math.max(0, se))

          // Show the field and keyboard
          _nativeLayout.setVisibility(View.VISIBLE)
          editText.requestFocus()
          imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)

          _nativeFieldOpen = true
          _keyboardShown = true
        }
      }
    )
  }

  override def closeNativeTextField(confirmative: Boolean): Unit =
    handler.post(
      new Runnable {
        override def run(): Unit = if (_nativeFieldOpen) {
          val editText = _nativeEditText
          val text     = editText.getText.toString
          val ss       = editText.getSelectionStart
          val se       = editText.getSelectionEnd

          // Notify text changed callback with final state
          val tcb = _onTextChanged
          if (tcb != null) {
            tcb(text, ss, se)
          }

          // Call close callback to check if keyboard should stay open
          val closeCb  = _onClose
          val keepOpen = if (closeCb != null) closeCb(confirmative) else false

          if (!keepOpen) {
            imm.hideSoftInputFromWindow(editText.getWindowToken, 0)
            _nativeLayout.setVisibility(View.GONE)
            editText.clearFocus()
            _nativeFieldOpen = false
            _keyboardShown = false
          }
        }
      }
    )

  override def isNativeTextFieldOpen: Boolean = _nativeFieldOpen

  /** Creates the native RelativeLayout + AutoCompleteTextView on first use. */
  private def ensureNativeFieldCreated(): Unit = if (_nativeLayout == null) {
    val layout = new RelativeLayout(context)
    layout.setBackgroundColor(Color.TRANSPARENT)
    layout.setVisibility(View.GONE)

    val lp = new RelativeLayout.LayoutParams(
      _root_.android.view.ViewGroup.LayoutParams.MATCH_PARENT,
      _root_.android.view.ViewGroup.LayoutParams.WRAP_CONTENT
    )
    lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)

    val editText = new AutoCompleteTextView(context) {
      override def onKeyPreIme(keyCode: Int, event: KeyEvent): Boolean =
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction == KeyEvent.ACTION_UP) {
          closeNativeTextField(false)
          true
        } else {
          super.onKeyPreIme(keyCode, event)
        }
    }
    editText.setLayoutParams(lp)
    layout.addView(editText)

    // Add the layout to the root view hierarchy
    if (_view != null) {
      val rootView = _view.getRootView
      rootView match {
        case vg: _root_.android.view.ViewGroup =>
          val rootLp = new _root_.android.widget.FrameLayout.LayoutParams(
            _root_.android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            _root_.android.view.ViewGroup.LayoutParams.MATCH_PARENT
          )
          vg.addView(layout, rootLp)
        case _ => () // cannot attach — view hierarchy not a ViewGroup
      }
    }

    _nativeLayout = layout
    _nativeEditText = editText
  }

  override def setView(view: AnyRef): Unit =
    _view = view.asInstanceOf[View]
}
