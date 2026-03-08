// SGE — Integration test: TouchInputOps API interface
//
// Tests that the TouchInputOps trait and provider factory method have expected shapes.

package sge
package platform
package android

import munit.FunSuite

class AndroidOpsTouchInputTest extends FunSuite {

  test("TouchInputOps has motion event extraction methods") {
    val cls = classOf[TouchInputOps]
    assert(cls.getMethod("getActionMasked", classOf[AnyRef]) != null)
    assert(cls.getMethod("getActionIndex", classOf[AnyRef]) != null)
    assert(cls.getMethod("getPointerCount", classOf[AnyRef]) != null)
    assert(cls.getMethod("getPointerId", classOf[AnyRef], classOf[Int]) != null)
    assert(cls.getMethod("getX", classOf[AnyRef], classOf[Int]) != null)
    assert(cls.getMethod("getY", classOf[AnyRef], classOf[Int]) != null)
    assert(cls.getMethod("getPressure", classOf[AnyRef], classOf[Int]) != null)
  }

  test("TouchInputOps has button/source/axis methods") {
    val cls = classOf[TouchInputOps]
    assert(cls.getMethod("getButtonState", classOf[AnyRef]) != null)
    assert(cls.getMethod("getSource", classOf[AnyRef]) != null)
    assert(cls.getMethod("getAxisValue", classOf[AnyRef], classOf[Int]) != null)
  }

  test("TouchInputOps has multitouch capability check") {
    val cls = classOf[TouchInputOps]
    assert(cls.getMethod("supportsMultitouch") != null)
  }

  test("TouchInputOps companion has action constants") {
    assert(TouchInputOps.ACTION_DOWN == 0)
    assert(TouchInputOps.ACTION_UP == 1)
    assert(TouchInputOps.ACTION_MOVE == 2)
    assert(TouchInputOps.ACTION_CANCEL == 3)
    assert(TouchInputOps.ACTION_POINTER_DOWN == 5)
    assert(TouchInputOps.ACTION_POINTER_UP == 6)
    assert(TouchInputOps.ACTION_HOVER_MOVE == 7)
    assert(TouchInputOps.ACTION_SCROLL == 8)
  }

  test("TouchInputOps companion has touch event type constants") {
    assert(TouchInputOps.TOUCH_DOWN == 0)
    assert(TouchInputOps.TOUCH_UP == 1)
    assert(TouchInputOps.TOUCH_DRAGGED == 2)
    assert(TouchInputOps.TOUCH_SCROLLED == 3)
    assert(TouchInputOps.TOUCH_MOVED == 4)
    assert(TouchInputOps.TOUCH_CANCELLED == 5)
  }

  test("TouchInputOps companion has button mapping") {
    assert(TouchInputOps.toSgeButton(0) == 0) // LEFT
    assert(TouchInputOps.toSgeButton(1) == 0) // LEFT
    assert(TouchInputOps.toSgeButton(2) == 1) // RIGHT
    assert(TouchInputOps.toSgeButton(4) == 2) // MIDDLE
    assert(TouchInputOps.toSgeButton(8) == 3) // BACK
    assert(TouchInputOps.toSgeButton(16) == 4) // FORWARD
    assert(TouchInputOps.toSgeButton(32) == -1) // unknown
  }

  test("AndroidPlatformProvider has createTouchInput factory method") {
    val cls = classOf[AndroidPlatformProvider]
    assert(cls.getMethod("createTouchInput", classOf[AnyRef]) != null)
  }
}
