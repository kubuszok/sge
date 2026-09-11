// SGE — Android touch/motion event operations interface
//
// Self-contained (JDK types only). Extracts data from Android
// MotionEvent objects without requiring android.* imports.
// Implemented in sge-jvm-platform-android using android.view.MotionEvent.

package sge
package platform
package android

/** Extracts data from Android MotionEvent objects. Uses only JDK types.
  *
  * All `event` parameters are `android.view.MotionEvent` passed as `AnyRef` to avoid android.* dependency.
  */
trait TouchInputOps {

  // ── Action extraction ─────────────────────────────────────────────────

  /** Returns the masked action (ACTION_DOWN, ACTION_UP, etc.). */
  def getActionMasked(event: AnyRef): Int

  /** Returns the pointer index associated with the action. */
  def getActionIndex(event: AnyRef): Int

  // ── Pointer data ──────────────────────────────────────────────────────

  /** Returns the number of pointers in this event. */
  def getPointerCount(event: AnyRef): Int

  /** Returns the pointer ID at the given pointer index. */
  def getPointerId(event: AnyRef, pointerIndex: Int): Int

  /** Returns the X coordinate (as int) at the given pointer index. */
  def getX(event: AnyRef, pointerIndex: Int): Int

  /** Returns the Y coordinate (as int) at the given pointer index. */
  def getY(event: AnyRef, pointerIndex: Int): Int

  /** Returns the pressure at the given pointer index (0.0 to 1.0+). */
  def getPressure(event: AnyRef, pointerIndex: Int): Float

  // ── Button / source / axis ────────────────────────────────────────────

  /** Returns the button state flags of the event. */
  def getButtonState(event: AnyRef): Int

  /** Returns the input source class flags. */
  def getSource(event: AnyRef): Int

  /** Returns the value of the specified axis. */
  def getAxisValue(event: AnyRef, axis: Int): Float

  // ── Capability ────────────────────────────────────────────────────────

  /** Whether the device supports multitouch. */
  def supportsMultitouch: Boolean
}

object TouchInputOps {

  // ── MotionEvent action constants ────────────────────────────────────

  val ACTION_DOWN:         Int = 0
  val ACTION_UP:           Int = 1
  val ACTION_MOVE:         Int = 2
  val ACTION_CANCEL:       Int = 3
  val ACTION_OUTSIDE:      Int = 4
  val ACTION_POINTER_DOWN: Int = 5
  val ACTION_POINTER_UP:   Int = 6
  val ACTION_HOVER_MOVE:   Int = 7
  val ACTION_SCROLL:       Int = 8

  // ── MotionEvent axis constants ──────────────────────────────────────

  val AXIS_VSCROLL: Int = 9
  val AXIS_HSCROLL: Int = 10

  // ── InputDevice source constants ────────────────────────────────────

  val SOURCE_CLASS_POINTER: Int = 0x00000002

  // ── Touch event type constants ──────────────────────────────────────

  val TOUCH_DOWN:      Int = 0
  val TOUCH_UP:        Int = 1
  val TOUCH_DRAGGED:   Int = 2
  val TOUCH_SCROLLED:  Int = 3
  val TOUCH_MOVED:     Int = 4
  val TOUCH_CANCELLED: Int = 5

  // ── Button mapping ──────────────────────────────────────────────────

  /** Converts Android button state flags to SGE button index.
    * @return
    *   SGE button index (0=LEFT, 1=RIGHT, 2=MIDDLE, 3=BACK, 4=FORWARD), or -1 if unknown
    */
  def toSgeButton(androidButtonState: Int): Int =
    if (androidButtonState == 0 || androidButtonState == 1) 0 // LEFT
    else if (androidButtonState == 2) 1 // RIGHT
    else if (androidButtonState == 4) 2 // MIDDLE
    else if (androidButtonState == 8) 3 // BACK
    else if (androidButtonState == 16) 4 // FORWARD
    else -1
}
