/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backend-android/.../AndroidMouseHandler.java
 * Original authors: Richard Martin
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: AndroidMouseHandler (same name, different package)
 *   Convention: uses TouchInputOps for MotionEvent data extraction; mutates AndroidInputState
 *   Idiom: split packages
 *   Audited: 2026-03-08
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package platform

import sge.platform.android.TouchInputOps

/** Mouse/trackpad handler that processes Android generic motion events via [[TouchInputOps]].
  *
  * Handles hover movement and scroll wheel events, posting them to an [[AndroidInputState]].
  */
class AndroidMouseHandler(ops: TouchInputOps) {

  import TouchInputOps._

  private var lastX: Int = 0
  private var lastY: Int = 0

  /** Process a generic motion event (hover/scroll).
    * @param event
    *   the Android MotionEvent (as AnyRef)
    * @param state
    *   the input state to update
    * @return
    *   true if the event was handled
    */
  def onGenericMotion(event: AnyRef, state: AndroidInputState): Boolean = {
    if ((ops.getSource(event) & SOURCE_CLASS_POINTER) == 0) return false

    val action    = ops.getActionMasked(event)
    val timeStamp = System.nanoTime()

    state.synchronized {
      action match {
        case ACTION_HOVER_MOVE =>
          val x = ops.getX(event, 0)
          val y = ops.getY(event, 0)
          if (x != lastX || y != lastY) {
            state.postTouchEvent(TOUCH_MOVED, x, y, 0, 0, timeStamp)
            lastX = x
            lastY = y
          }

        case ACTION_SCROLL =>
          val scrollAmountY = -Math.signum(ops.getAxisValue(event, AXIS_VSCROLL)).toInt
          val scrollAmountX = -Math.signum(ops.getAxisValue(event, AXIS_HSCROLL)).toInt
          state.postScrollEvent(scrollAmountX, scrollAmountY, timeStamp)

        case _ => // unknown action, ignore
      }
    }
    true
  }
}
