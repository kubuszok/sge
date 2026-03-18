/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   SGE-original: extracted from LibGDX DefaultAndroidInput state management
 *   Convention: trait for input state that AndroidTouchHandler/AndroidMouseHandler write to
 *   Idiom: split packages
 *   Audited: 2026-03-08
 */
package sge
package platform

/** Mutable input state interface used by [[AndroidTouchHandler]] and [[AndroidMouseHandler]].
  *
  * Implementations maintain per-pointer state arrays and a touch event queue. The handlers call these methods (synchronized on this instance) to update state and post events.
  */
trait AndroidInputState {

  // ── Per-pointer state access ──────────────────────────────────────────

  def getTouchX(pointer: Int): Int
  def getTouchY(pointer: Int): Int
  def getButton(pointer: Int): Int

  def setTouchX(pointer:   Int, value:     Int):     Unit
  def setTouchY(pointer:   Int, value:     Int):     Unit
  def setDeltaX(pointer:   Int, value:     Int):     Unit
  def setDeltaY(pointer:   Int, value:     Int):     Unit
  def setTouched(pointer:  Int, value:     Boolean): Unit
  def setButton(pointer:   Int, value:     Int):     Unit
  def setPressure(pointer: Int, value:     Float):   Unit
  def setRealId(pointer:   Int, pointerId: Int):     Unit

  // ── Pointer ID mapping ────────────────────────────────────────────────

  /** Find a free pointer index. Returns an index >= 0, possibly growing internal arrays. */
  def freePointerIndex: Int

  /** Look up the internal pointer index for the given Android pointer ID.
    * @return
    *   the index, or -1 if not found
    */
  def lookUpPointerIndex(pointerId: Int): Int

  /** Cancel all active pointers (e.g. on ACTION_CANCEL). */
  def cancelAllPointers(): Unit

  // ── Event posting ─────────────────────────────────────────────────────

  /** Post a touch event to the event queue. */
  def postTouchEvent(eventType: Int, x: Int, y: Int, pointer: Int, button: Int, timeStamp: Long): Unit

  /** Post a scroll event to the event queue. */
  def postScrollEvent(scrollAmountX: Int, scrollAmountY: Int, timeStamp: Long): Unit
}

object AndroidInputState {

  /** Maximum number of simultaneous touch pointers (can grow dynamically). */
  val NUM_TOUCHES: Int = 20
}
