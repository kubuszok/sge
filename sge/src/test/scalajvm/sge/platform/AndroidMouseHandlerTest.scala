/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package platform

import sge.platform.android.TouchInputOps
import scala.collection.mutable.ArrayBuffer

class AndroidMouseHandlerTest extends munit.FunSuite {

  // Reuse FakeMotionEvent and stub classes from touch handler test
  final case class FakeMotionEvent(
    actionMasked: Int,
    actionIndex:  Int = 0,
    pointers:     Array[(Int, Int, Int, Float)] = Array((0, 0, 0, 0f)),
    buttonState:  Int = 0,
    source:       Int = TouchInputOps.SOURCE_CLASS_POINTER,
    axisVScroll:  Float = 0f,
    axisHScroll:  Float = 0f
  )

  private class StubTouchInputOps extends TouchInputOps {
    override def getActionMasked(event: AnyRef):                    Int   = event.asInstanceOf[FakeMotionEvent].actionMasked
    override def getActionIndex(event:  AnyRef):                    Int   = event.asInstanceOf[FakeMotionEvent].actionIndex
    override def getPointerCount(event: AnyRef):                    Int   = event.asInstanceOf[FakeMotionEvent].pointers.length
    override def getPointerId(event:    AnyRef, pointerIndex: Int): Int   = event.asInstanceOf[FakeMotionEvent].pointers(pointerIndex)._1
    override def getX(event:            AnyRef, pointerIndex: Int): Int   = event.asInstanceOf[FakeMotionEvent].pointers(pointerIndex)._2
    override def getY(event:            AnyRef, pointerIndex: Int): Int   = event.asInstanceOf[FakeMotionEvent].pointers(pointerIndex)._3
    override def getPressure(event:     AnyRef, pointerIndex: Int): Float = event.asInstanceOf[FakeMotionEvent].pointers(pointerIndex)._4
    override def getButtonState(event:  AnyRef):                    Int   = event.asInstanceOf[FakeMotionEvent].buttonState
    override def getSource(event:       AnyRef):                    Int   = event.asInstanceOf[FakeMotionEvent].source
    override def getAxisValue(event: AnyRef, axis: Int):            Float = {
      val e = event.asInstanceOf[FakeMotionEvent]
      if (axis == TouchInputOps.AXIS_VSCROLL) e.axisVScroll
      else if (axis == TouchInputOps.AXIS_HSCROLL) e.axisHScroll
      else 0f
    }
    override def supportsMultitouch: Boolean = true
  }

  final case class TouchEventRecord(eventType: Int, x: Int, y: Int, pointer: Int, button: Int, timeStamp: Long)
  final case class ScrollEventRecord(scrollX: Int, scrollY: Int, timeStamp: Long)

  private class StubInputState extends AndroidInputState {
    val touchX   = Array.fill(AndroidInputState.NUM_TOUCHES)(0)
    val touchY   = Array.fill(AndroidInputState.NUM_TOUCHES)(0)
    val deltaX   = Array.fill(AndroidInputState.NUM_TOUCHES)(0)
    val deltaY   = Array.fill(AndroidInputState.NUM_TOUCHES)(0)
    val touched  = Array.fill(AndroidInputState.NUM_TOUCHES)(false)
    val button   = Array.fill(AndroidInputState.NUM_TOUCHES)(0)
    val pressure = Array.fill(AndroidInputState.NUM_TOUCHES)(0f)
    val realId   = Array.fill(AndroidInputState.NUM_TOUCHES)(-1)

    val touchEvents  = ArrayBuffer.empty[TouchEventRecord]
    val scrollEvents = ArrayBuffer.empty[ScrollEventRecord]

    override def getTouchX(pointer:            Int):                     Int  = touchX(pointer)
    override def getTouchY(pointer:            Int):                     Int  = touchY(pointer)
    override def getButton(pointer:            Int):                     Int  = button(pointer)
    override def setTouchX(pointer:            Int, value:     Int):     Unit = touchX(pointer) = value
    override def setTouchY(pointer:            Int, value:     Int):     Unit = touchY(pointer) = value
    override def setDeltaX(pointer:            Int, value:     Int):     Unit = deltaX(pointer) = value
    override def setDeltaY(pointer:            Int, value:     Int):     Unit = deltaY(pointer) = value
    override def setTouched(pointer:           Int, value:     Boolean): Unit = touched(pointer) = value
    override def setButton(pointer:            Int, value:     Int):     Unit = button(pointer) = value
    override def setPressure(pointer:          Int, value:     Float):   Unit = pressure(pointer) = value
    override def setRealId(pointer:            Int, pointerId: Int):     Unit = realId(pointer) = pointerId
    override def getFreePointerIndex():                                  Int  = realId.indexOf(-1)
    override def lookUpPointerIndex(pointerId: Int):                     Int  = realId.indexOf(pointerId)
    override def cancelAllPointers():                                    Unit = {
      java.util.Arrays.fill(realId, -1)
      java.util.Arrays.fill(touched, false)
    }
    override def postTouchEvent(eventType: Int, x: Int, y: Int, pointer: Int, button: Int, timeStamp: Long): Unit =
      touchEvents += TouchEventRecord(eventType, x, y, pointer, button, timeStamp)
    override def postScrollEvent(scrollAmountX: Int, scrollAmountY: Int, timeStamp: Long): Unit =
      scrollEvents += ScrollEventRecord(scrollAmountX, scrollAmountY, timeStamp)
  }

  // ── Tests ─────────────────────────────────────────────────────────────

  private val ops     = new StubTouchInputOps
  private val handler = AndroidMouseHandler(ops)

  test("HOVER_MOVE posts TOUCH_MOVED event") {
    val state = new StubInputState
    val event = FakeMotionEvent(
      actionMasked = TouchInputOps.ACTION_HOVER_MOVE,
      pointers = Array((0, 100, 200, 0f))
    )
    handler.onGenericMotion(event, state)

    assertEquals(state.touchEvents.size, 1)
    assertEquals(state.touchEvents(0).eventType, TouchInputOps.TOUCH_MOVED)
    assertEquals(state.touchEvents(0).x, 100)
    assertEquals(state.touchEvents(0).y, 200)
  }

  test("HOVER_MOVE with same position is deduplicated") {
    val state = new StubInputState
    val event = FakeMotionEvent(
      actionMasked = TouchInputOps.ACTION_HOVER_MOVE,
      pointers = Array((0, 50, 60, 0f))
    )
    // Use a fresh handler to control lastX/lastY
    val h = AndroidMouseHandler(ops)
    h.onGenericMotion(event, state)
    h.onGenericMotion(event, state) // same position

    assertEquals(state.touchEvents.size, 1) // only one event posted
  }

  test("SCROLL posts scroll event with negated signum") {
    val state = new StubInputState
    val event = FakeMotionEvent(
      actionMasked = TouchInputOps.ACTION_SCROLL,
      axisVScroll = 1.0f, // scroll down
      axisHScroll = -1.0f // scroll right
    )
    handler.onGenericMotion(event, state)

    assertEquals(state.scrollEvents.size, 1)
    assertEquals(state.scrollEvents(0).scrollY, -1) // negated
    assertEquals(state.scrollEvents(0).scrollX, 1) // negated
  }

  test("non-pointer source returns false") {
    val state = new StubInputState
    val event = FakeMotionEvent(
      actionMasked = TouchInputOps.ACTION_HOVER_MOVE,
      source = 0 // not a pointer source
    )
    val result = handler.onGenericMotion(event, state)
    assert(!result)
    assertEquals(state.touchEvents.size, 0)
  }

  test("returns true for pointer-source events") {
    val state = new StubInputState
    val event = FakeMotionEvent(
      actionMasked = TouchInputOps.ACTION_HOVER_MOVE,
      pointers = Array((0, 10, 20, 0f))
    )
    val h      = AndroidMouseHandler(ops)
    val result = h.onGenericMotion(event, state)
    assert(result)
  }
}
