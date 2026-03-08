/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package platform

import sge.platform.android.TouchInputOps
import scala.collection.mutable.ArrayBuffer

class AndroidTouchHandlerTest extends munit.FunSuite {

  // ── Stub TouchInputOps ────────────────────────────────────────────────

  /** A mock MotionEvent represented as a simple case class. */
  final case class FakeMotionEvent(
    actionMasked: Int,
    actionIndex:  Int = 0,
    pointers:     Array[(Int, Int, Int, Float)] = Array.empty, // (pointerId, x, y, pressure)
    buttonState:  Int = 0,
    source:       Int = 0,
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

  // ── Stub AndroidInputState ────────────────────────────────────────────

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

    override def getTouchX(pointer: Int): Int = touchX(pointer)
    override def getTouchY(pointer: Int): Int = touchY(pointer)
    override def getButton(pointer: Int): Int = button(pointer)

    override def setTouchX(pointer:   Int, value:     Int):     Unit = touchX(pointer) = value
    override def setTouchY(pointer:   Int, value:     Int):     Unit = touchY(pointer) = value
    override def setDeltaX(pointer:   Int, value:     Int):     Unit = deltaX(pointer) = value
    override def setDeltaY(pointer:   Int, value:     Int):     Unit = deltaY(pointer) = value
    override def setTouched(pointer:  Int, value:     Boolean): Unit = touched(pointer) = value
    override def setButton(pointer:   Int, value:     Int):     Unit = button(pointer) = value
    override def setPressure(pointer: Int, value:     Float):   Unit = pressure(pointer) = value
    override def setRealId(pointer:   Int, pointerId: Int):     Unit = realId(pointer) = pointerId

    override def getFreePointerIndex(): Int = {
      var i = 0
      while (i < realId.length) {
        if (realId(i) == -1) return i
        i += 1
      }
      -1 // simplified — no resize in test
    }

    override def lookUpPointerIndex(pointerId: Int): Int = {
      var i = 0
      while (i < realId.length) {
        if (realId(i) == pointerId) return i
        i += 1
      }
      -1
    }

    override def cancelAllPointers(): Unit = {
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
  private val handler = AndroidTouchHandler(ops)

  test("ACTION_DOWN posts TOUCH_DOWN event and updates state") {
    val state = new StubInputState
    val event = FakeMotionEvent(
      actionMasked = TouchInputOps.ACTION_DOWN,
      pointers = Array((0, 100, 200, 0.5f)),
      buttonState = 1 // LEFT
    )
    handler.onTouch(event, state)

    assertEquals(state.touchEvents.size, 1)
    assertEquals(state.touchEvents(0).eventType, TouchInputOps.TOUCH_DOWN)
    assertEquals(state.touchEvents(0).x, 100)
    assertEquals(state.touchEvents(0).y, 200)
    assertEquals(state.touchEvents(0).pointer, 0)
    assertEquals(state.touchX(0), 100)
    assertEquals(state.touchY(0), 200)
    assert(state.touched(0))
    assertEquals(state.pressure(0), 0.5f)
  }

  test("ACTION_UP posts TOUCH_UP event and clears state") {
    val state = new StubInputState
    // First touch down
    state.realId(0) = 0
    state.touched(0) = true
    state.button(0) = 0 // LEFT

    val event = FakeMotionEvent(
      actionMasked = TouchInputOps.ACTION_UP,
      pointers = Array((0, 150, 250, 0f))
    )
    handler.onTouch(event, state)

    assertEquals(state.touchEvents.size, 1)
    assertEquals(state.touchEvents(0).eventType, TouchInputOps.TOUCH_UP)
    assert(!state.touched(0))
    assertEquals(state.realId(0), -1)
  }

  test("ACTION_CANCEL posts TOUCH_CANCELLED and cancels all pointers") {
    val state = new StubInputState
    state.realId(0) = 0
    state.realId(1) = 1
    state.touched(0) = true
    state.touched(1) = true
    state.button(0) = 0
    state.button(1) = 0

    val event = FakeMotionEvent(
      actionMasked = TouchInputOps.ACTION_CANCEL,
      pointers = Array((0, 100, 200, 0f))
    )
    handler.onTouch(event, state)

    assertEquals(state.touchEvents.size, 1)
    assertEquals(state.touchEvents(0).eventType, TouchInputOps.TOUCH_CANCELLED)
    // All pointers should be cancelled
    assert(!state.touched(0))
    assert(!state.touched(1))
    assertEquals(state.realId(0), -1)
    assertEquals(state.realId(1), -1)
  }

  test("ACTION_MOVE posts TOUCH_DRAGGED for active pointers") {
    val state = new StubInputState
    state.realId(0) = 0
    state.touchX(0) = 100
    state.touchY(0) = 200
    state.button(0) = 0 // LEFT

    val event = FakeMotionEvent(
      actionMasked = TouchInputOps.ACTION_MOVE,
      pointers = Array((0, 110, 220, 0.8f))
    )
    handler.onTouch(event, state)

    assertEquals(state.touchEvents.size, 1)
    assertEquals(state.touchEvents(0).eventType, TouchInputOps.TOUCH_DRAGGED)
    assertEquals(state.deltaX(0), 10)
    assertEquals(state.deltaY(0), 20)
    assertEquals(state.touchX(0), 110)
    assertEquals(state.touchY(0), 220)
  }

  test("ACTION_MOVE posts TOUCH_MOVED when no button active") {
    val state = new StubInputState
    state.realId(0) = 0
    state.button(0) = -1 // no button

    val event = FakeMotionEvent(
      actionMasked = TouchInputOps.ACTION_MOVE,
      pointers = Array((0, 50, 60, 0.3f))
    )
    handler.onTouch(event, state)

    assertEquals(state.touchEvents.size, 1)
    assertEquals(state.touchEvents(0).eventType, TouchInputOps.TOUCH_MOVED)
  }

  test("multitouch: POINTER_DOWN assigns new pointer index") {
    val state = new StubInputState
    // First pointer already active
    state.realId(0) = 0
    state.touched(0) = true

    val event = FakeMotionEvent(
      actionMasked = TouchInputOps.ACTION_POINTER_DOWN,
      actionIndex = 1,
      pointers = Array((0, 100, 200, 0.5f), (1, 300, 400, 0.6f)),
      buttonState = 1
    )
    handler.onTouch(event, state)

    assertEquals(state.touchEvents.size, 1)
    assertEquals(state.touchEvents(0).eventType, TouchInputOps.TOUCH_DOWN)
    // Second pointer assigned to index 1
    assertEquals(state.realId(1), 1)
    assertEquals(state.touchX(1), 300)
    assertEquals(state.touchY(1), 400)
  }

  test("unknown pointer ID on UP is ignored") {
    val state = new StubInputState
    val event = FakeMotionEvent(
      actionMasked = TouchInputOps.ACTION_UP,
      pointers = Array((99, 0, 0, 0f))
    )
    handler.onTouch(event, state)
    assertEquals(state.touchEvents.size, 0)
  }
}
