// SGE — Android cursor operations interface
//
// Self-contained (JDK types only). Maps system cursor types to
// platform-specific pointer icons. Implemented in sge-jvm-platform-android
// using android.view.PointerIcon.

package sge
package platform
package android

/** Cursor operations for Android. Uses only JDK types.
  *
  * Cursor type constants match sge's Cursor.SystemCursor ordinals: 0=Arrow, 1=Ibeam, 2=Crosshair, 3=Hand, 4=HorizontalResize, 5=VerticalResize, 6=NWSEResize, 7=NESWResize, 8=AllResize, 9=NotAllowed,
  * 10=None.
  */
trait CursorOps {

  /** Sets the system cursor on the given view.
    * @param view
    *   the Android View (as AnyRef)
    * @param cursorType
    *   system cursor type ordinal (see above)
    */
  def setSystemCursor(view: AnyRef, cursorType: Int): Unit
}

object CursorOps {
  // Constants matching Cursor.SystemCursor ordinals
  final val Arrow            = 0
  final val Ibeam            = 1
  final val Crosshair        = 2
  final val Hand             = 3
  final val HorizontalResize = 4
  final val VerticalResize   = 5
  final val NWSEResize       = 6
  final val NESWResize       = 7
  final val AllResize        = 8
  final val NotAllowed       = 9
  final val None             = 10
}
