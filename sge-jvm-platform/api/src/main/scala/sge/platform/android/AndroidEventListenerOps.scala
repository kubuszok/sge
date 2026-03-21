// SGE — Android event listener interface
//
// Callback for Activity result events. Allows extensions to hook into
// onActivityResult. Self-contained (JDK types only).
//
// Migration notes:
//   Source: com.badlogic.gdx.backends.android.AndroidEventListener
//   Convention: ops interface pattern; Intent passed as AnyRef
//   Audited: 2026-03-08

package sge
package platform
package android

/** Listener for special Android events such as onActivityResult.
  *
  * This can be used by extensions to plug into the Android system.
  *
  * @author
  *   noblemaster (original implementation)
  */
trait AndroidEventListenerOps {

  /** Called when the Activity's onActivityResult method is called.
    * @param requestCode
    *   the request code originally supplied to startActivityForResult
    * @param resultCode
    *   the result code returned by the child activity
    * @param data
    *   an Android Intent (as AnyRef) with result data, or null
    */
  def onActivityResult(requestCode: Int, resultCode: Int, data: AnyRef): Unit
}
