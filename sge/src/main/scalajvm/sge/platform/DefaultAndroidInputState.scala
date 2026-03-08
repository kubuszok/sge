/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   SGE-original: extracted from LibGDX DefaultAndroidInput state management
 *   Convention: concrete implementation of AndroidInputState with per-pointer arrays and event queue
 *   Idiom: split packages
 *   Audited: 2026-03-08
 */
package sge
package platform

import sge.utils.DynamicArray

/** Concrete implementation of [[AndroidInputState]] with fixed-size per-pointer arrays and a ring-buffer event queue.
  */
class DefaultAndroidInputState extends AndroidInputState {

  import AndroidInputState.NUM_TOUCHES

  // ── Per-pointer state ────────────────────────────────────────────────

  private val touchX:   Array[Int]     = new Array[Int](NUM_TOUCHES)
  private val touchY:   Array[Int]     = new Array[Int](NUM_TOUCHES)
  private val deltaX:   Array[Int]     = new Array[Int](NUM_TOUCHES)
  private val deltaY:   Array[Int]     = new Array[Int](NUM_TOUCHES)
  private val touched:  Array[Boolean] = new Array[Boolean](NUM_TOUCHES)
  private val button:   Array[Int]     = new Array[Int](NUM_TOUCHES)
  private val pressure: Array[Float]   = new Array[Float](NUM_TOUCHES)
  private val realId:   Array[Int]     = Array.fill(NUM_TOUCHES)(-1)

  // ── Touch event queue ────────────────────────────────────────────────

  private[sge] val touchEvents:  DynamicArray[TouchEvent]  = DynamicArray[TouchEvent](64)
  private[sge] val scrollEvents: DynamicArray[ScrollEvent] = DynamicArray[ScrollEvent](8)

  // ── Per-pointer access ──────────────────────────────────────────────

  override def getTouchX(pointer: Int): Int = if (pointer < NUM_TOUCHES) touchX(pointer) else 0
  override def getTouchY(pointer: Int): Int = if (pointer < NUM_TOUCHES) touchY(pointer) else 0
  override def getButton(pointer: Int): Int = if (pointer < NUM_TOUCHES) button(pointer) else 0

  override def setTouchX(pointer: Int, value: Int): Unit =
    if (pointer < NUM_TOUCHES) touchX(pointer) = value

  override def setTouchY(pointer: Int, value: Int): Unit =
    if (pointer < NUM_TOUCHES) touchY(pointer) = value

  override def setDeltaX(pointer: Int, value: Int): Unit =
    if (pointer < NUM_TOUCHES) deltaX(pointer) = value

  override def setDeltaY(pointer: Int, value: Int): Unit =
    if (pointer < NUM_TOUCHES) deltaY(pointer) = value

  override def setTouched(pointer: Int, value: Boolean): Unit =
    if (pointer < NUM_TOUCHES) touched(pointer) = value

  override def setButton(pointer: Int, value: Int): Unit =
    if (pointer < NUM_TOUCHES) button(pointer) = value

  override def setPressure(pointer: Int, value: Float): Unit =
    if (pointer < NUM_TOUCHES) pressure(pointer) = value

  override def setRealId(pointer: Int, pointerId: Int): Unit =
    if (pointer < NUM_TOUCHES) realId(pointer) = pointerId

  // ── Query methods (used by AndroidInput) ───────────────────────────

  def getDeltaX(pointer:   Int): Int     = if (pointer < NUM_TOUCHES) deltaX(pointer) else 0
  def getDeltaY(pointer:   Int): Int     = if (pointer < NUM_TOUCHES) deltaY(pointer) else 0
  def isTouched(pointer:   Int): Boolean = if (pointer < NUM_TOUCHES) touched(pointer) else false
  def getPressure(pointer: Int): Float   = if (pointer < NUM_TOUCHES) pressure(pointer) else 0f

  // ── Pointer ID mapping ──────────────────────────────────────────────

  override def getFreePointerIndex(): Int = {
    var i = 0
    while (i < NUM_TOUCHES) {
      if (realId(i) == -1) return i
      i += 1
    }
    // All slots full — return NUM_TOUCHES to indicate overflow (caller checks bounds)
    NUM_TOUCHES
  }

  override def lookUpPointerIndex(pointerId: Int): Int = {
    var i = 0
    while (i < NUM_TOUCHES) {
      if (realId(i) == pointerId) return i
      i += 1
    }
    -1
  }

  override def cancelAllPointers(): Unit = {
    var i = 0
    while (i < NUM_TOUCHES) {
      realId(i) = -1
      touched(i) = false
      button(i) = 0
      pressure(i) = 0f
      i += 1
    }
  }

  // ── Event posting ───────────────────────────────────────────────────

  override def postTouchEvent(eventType: Int, x: Int, y: Int, pointer: Int, button: Int, timeStamp: Long): Unit =
    touchEvents += TouchEvent(eventType, x, y, pointer, button, timeStamp)

  override def postScrollEvent(scrollAmountX: Int, scrollAmountY: Int, timeStamp: Long): Unit =
    scrollEvents += ScrollEvent(scrollAmountX, scrollAmountY, timeStamp)

  /** Drain all queued touch events. Returns a snapshot and clears the queue. */
  def drainTouchEvents(): DynamicArray[TouchEvent] = {
    val snapshot = DynamicArray[TouchEvent](touchEvents.size)
    snapshot.addAll(touchEvents)
    touchEvents.clear()
    snapshot
  }

  /** Drain all queued scroll events. Returns a snapshot and clears the queue. */
  def drainScrollEvents(): DynamicArray[ScrollEvent] = {
    val snapshot = DynamicArray[ScrollEvent](scrollEvents.size)
    snapshot.addAll(scrollEvents)
    scrollEvents.clear()
    snapshot
  }
}

/** A touch/pointer event. */
final case class TouchEvent(
  eventType: Int,
  x:         Int,
  y:         Int,
  pointer:   Int,
  button:    Int,
  timeStamp: Long
)

/** A scroll event. */
final case class ScrollEvent(
  scrollAmountX: Int,
  scrollAmountY: Int,
  timeStamp:     Long
)
