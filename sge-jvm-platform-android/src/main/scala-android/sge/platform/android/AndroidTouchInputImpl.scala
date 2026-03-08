// SGE — Android touch input implementation
//
// Extracts data from android.view.MotionEvent via TouchInputOps interface.
// Compiled against android.jar. Never loaded on Desktop JVM.

package sge
package platform
package android

import _root_.android.content.Context
import _root_.android.view.{ InputDevice, MotionEvent }

/** Concrete TouchInputOps backed by android.view.MotionEvent. */
class AndroidTouchInputImpl(context: Context) extends TouchInputOps {

  override def getActionMasked(event: AnyRef): Int =
    event.asInstanceOf[MotionEvent].getAction() & MotionEvent.ACTION_MASK

  override def getActionIndex(event: AnyRef): Int = {
    val e = event.asInstanceOf[MotionEvent]
    (e.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT
  }

  override def getPointerCount(event: AnyRef): Int =
    event.asInstanceOf[MotionEvent].getPointerCount()

  override def getPointerId(event: AnyRef, pointerIndex: Int): Int =
    event.asInstanceOf[MotionEvent].getPointerId(pointerIndex)

  override def getX(event: AnyRef, pointerIndex: Int): Int =
    event.asInstanceOf[MotionEvent].getX(pointerIndex).toInt

  override def getY(event: AnyRef, pointerIndex: Int): Int =
    event.asInstanceOf[MotionEvent].getY(pointerIndex).toInt

  override def getPressure(event: AnyRef, pointerIndex: Int): Float =
    event.asInstanceOf[MotionEvent].getPressure(pointerIndex)

  override def getButtonState(event: AnyRef): Int =
    event.asInstanceOf[MotionEvent].getButtonState()

  override def getSource(event: AnyRef): Int =
    event.asInstanceOf[MotionEvent].getSource()

  override def getAxisValue(event: AnyRef, axis: Int): Float =
    event.asInstanceOf[MotionEvent].getAxisValue(axis)

  override def supportsMultitouch: Boolean =
    context.getPackageManager().hasSystemFeature("android.hardware.touchscreen.multitouch")
}
