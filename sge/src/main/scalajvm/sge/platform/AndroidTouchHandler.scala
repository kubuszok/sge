/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backend-android/.../AndroidTouchHandler.java
 * Original authors: badlogicgames@gmail.com
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: AndroidTouchHandler (same name, different package)
 *   Convention: uses TouchInputOps for MotionEvent data extraction; mutates AndroidInputState
 *   Idiom: split packages; no return (boundary/break)
 *   Audited: 2026-03-08
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package platform

import sge.platform.android.TouchInputOps

/** Multitouch handler that processes Android touch events via [[TouchInputOps]].
  *
  * Translates raw touch event data into structured touch events posted to an [[AndroidInputState]]. The handler manages pointer ID → index mapping and per-pointer state updates.
  */
class AndroidTouchHandler(ops: TouchInputOps) {

  import TouchInputOps._

  /** Process a touch motion event.
    * @param event
    *   the Android MotionEvent (as AnyRef)
    * @param state
    *   the input state to update
    */
  def onTouch(event: AnyRef, state: AndroidInputState): Unit = {
    val action       = ops.getActionMasked(event)
    val pointerIndex = ops.getActionIndex(event)
    val pointerId    = ops.getPointerId(event, pointerIndex)

    var x:                Int  = 0
    var y:                Int  = 0
    var realPointerIndex: Int  = 0
    var button:           Int  = 0
    val timeStamp:        Long = System.nanoTime()

    state.synchronized {
      action match {
        case ACTION_DOWN | ACTION_POINTER_DOWN =>
          realPointerIndex = state.getFreePointerIndex()
          if (realPointerIndex < AndroidInputState.NUM_TOUCHES) {
            state.setRealId(realPointerIndex, pointerId)
            x = ops.getX(event, pointerIndex)
            y = ops.getY(event, pointerIndex)
            button = toSgeButton(ops.getButtonState(event))
            if (button != -1) {
              state.postTouchEvent(TOUCH_DOWN, x, y, realPointerIndex, button, timeStamp)
            }
            state.setTouchX(realPointerIndex, x)
            state.setTouchY(realPointerIndex, y)
            state.setDeltaX(realPointerIndex, 0)
            state.setDeltaY(realPointerIndex, 0)
            state.setTouched(realPointerIndex, button != -1)
            state.setButton(realPointerIndex, button)
            state.setPressure(realPointerIndex, ops.getPressure(event, pointerIndex))
          }

        case ACTION_UP | ACTION_POINTER_UP | ACTION_OUTSIDE | ACTION_CANCEL =>
          realPointerIndex = state.lookUpPointerIndex(pointerId)
          if (realPointerIndex != -1 && realPointerIndex < AndroidInputState.NUM_TOUCHES) {
            state.setRealId(realPointerIndex, -1)
            x = ops.getX(event, pointerIndex)
            y = ops.getY(event, pointerIndex)
            button = state.getButton(realPointerIndex)
            if (button != -1) {
              if (action == ACTION_CANCEL) {
                state.postTouchEvent(TOUCH_CANCELLED, x, y, realPointerIndex, button, timeStamp)
              } else {
                state.postTouchEvent(TOUCH_UP, x, y, realPointerIndex, button, timeStamp)
              }
            }
            state.setTouchX(realPointerIndex, x)
            state.setTouchY(realPointerIndex, y)
            state.setDeltaX(realPointerIndex, 0)
            state.setDeltaY(realPointerIndex, 0)
            state.setTouched(realPointerIndex, false)
            state.setButton(realPointerIndex, 0)
            state.setPressure(realPointerIndex, 0f)
            if (action == ACTION_CANCEL) {
              state.cancelAllPointers()
            }
          }

        case ACTION_MOVE =>
          val pointerCount = ops.getPointerCount(event)
          var i            = 0
          while (i < pointerCount) {
            val pid = ops.getPointerId(event, i)
            x = ops.getX(event, i)
            y = ops.getY(event, i)
            realPointerIndex = state.lookUpPointerIndex(pid)
            if (realPointerIndex != -1 && realPointerIndex < AndroidInputState.NUM_TOUCHES) {
              button = state.getButton(realPointerIndex)
              if (button != -1) {
                state.postTouchEvent(TOUCH_DRAGGED, x, y, realPointerIndex, button, timeStamp)
              } else {
                state.postTouchEvent(TOUCH_MOVED, x, y, realPointerIndex, 0, timeStamp)
              }
              state.setDeltaX(realPointerIndex, x - state.getTouchX(realPointerIndex))
              state.setDeltaY(realPointerIndex, y - state.getTouchY(realPointerIndex))
              state.setTouchX(realPointerIndex, x)
              state.setTouchY(realPointerIndex, y)
              state.setPressure(realPointerIndex, ops.getPressure(event, i))
            }
            i += 1
          }

        case _ => // unknown action, ignore
      }
    }
  }
}
