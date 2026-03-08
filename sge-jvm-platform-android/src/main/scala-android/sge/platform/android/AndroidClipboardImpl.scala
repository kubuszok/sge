// SGE — Android clipboard implementation
//
// Implements ClipboardOps using android.content.ClipboardManager.

package sge
package platform
package android

import _root_.android.content.{ ClipData, Context }

/** Android clipboard backed by ClipboardManager. */
class AndroidClipboardImpl(context: Context) extends ClipboardOps {

  private val clipboard: _root_.android.content.ClipboardManager =
    context.getSystemService(Context.CLIPBOARD_SERVICE).asInstanceOf[_root_.android.content.ClipboardManager]

  override def hasContents: Boolean =
    clipboard.hasPrimaryClip()

  override def getContents: String | Null = {
    val clip = clipboard.getPrimaryClip()
    if (clip == null) null
    else {
      val text = clip.getItemAt(0).getText()
      if (text == null) null
      else text.toString
    }
  }

  override def setContents(text: String): Unit = {
    val data = ClipData.newPlainText(text, text)
    clipboard.setPrimaryClip(data)
  }
}
