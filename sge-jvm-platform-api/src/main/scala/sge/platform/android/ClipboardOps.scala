// SGE — Android clipboard operations interface
//
// Self-contained (JDK types only). Implemented in sge-jvm-platform-android
// using android.content.ClipboardManager.

package sge
package platform
package android

/** Clipboard operations for Android. Uses only JDK types. */
trait ClipboardOps {

  /** Whether the clipboard has content. */
  def hasContents: Boolean

  /** Returns clipboard text, or null if empty. */
  def getContents: String | Null

  /** Sets clipboard text. */
  def setContents(text: String): Unit
}
