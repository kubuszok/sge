/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/utils/ClickListener.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - No return statements, uses if/else expressions
 * - @Null Actor -> Nullable[Actor] for enter/exit fromActor/toActor params
 * - actor.hit returns Nullable[Actor] -> uses fold for null-safe check
 * - visualPressedDuration static field -> companion object var
 * - All methods faithfully ported
 * - Renames: getTapSquareSize/setTapSquareSize → var tapSquareSize, getTapCount/setTapCount → var tapCount,
 *   getButton/setButton → var button, getTouchDownX/Y → def touchDownX/touchDownY,
 *   getPressedButton/getPressedPointer → def pressedButton/pressedPointer,
 *   isPressed → def pressed, isOver → def over
 */
package sge
package scenes
package scene2d
package utils

import sge.utils.{ Millis, Nanos, Nullable, TimeUtils }

/** Detects mouse over, mouse or finger touch presses, and clicks on an actor. A touch must go down over the actor and is considered pressed as long as it is over the actor or within the
  * {@link #setTapSquareSize(float) tap square}. This behavior makes it easier to press buttons on a touch interface when the initial touch happens near the edge of the actor. Double clicks can be
  * detected using {@link #getTapCount()}. Any touch (not just the first) will trigger this listener. While pressed, other touch downs are ignored.
  * @author
  *   Nathan Sweet
  */
class ClickListener(var button: Int = 0) extends InputListener {

  var tapSquareSize:             Float   = 14
  private var _touchDownX:       Float   = -1
  private var _touchDownY:       Float   = -1
  private var _pressedPointer:   Int     = -1
  private var _pressedButton:    Int     = -1
  private var _pressed:          Boolean = false
  private var _over:             Boolean = false
  private var cancelled:         Boolean = false
  private var visualPressedTime: Millis  = Millis.zero
  private var tapCountInterval:  Nanos   = Nanos((0.4f * 1000000000L).toLong)
  var tapCount:                  Int     = 0
  private var lastTapTime:       Nanos   = Nanos.zero

  override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean =
    if (_pressed) false
    else if (pointer == 0 && this.button != -1 && button != this.button) false
    else {
      _pressed = true
      _pressedPointer = pointer
      _pressedButton = button
      _touchDownX = x
      _touchDownY = y
      setVisualPressed(true)
      true
    }

  override def touchDragged(event: InputEvent, x: Float, y: Float, pointer: Int): Unit =
    if (pointer == _pressedPointer && !cancelled) {
      _pressed = event.listenerActor.exists(isOver(_, x, y))
      if (!_pressed) {
        // Once outside the tap square, don't use the tap square anymore.
        invalidateTapSquare()
      }
    }

  override def touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Unit =
    if (pointer == _pressedPointer) {
      if (!cancelled) {
        var touchUpOver = event.listenerActor.exists(isOver(_, x, y))
        // Ignore touch up if the wrong mouse button.
        if (touchUpOver && pointer == 0 && this.button != -1 && button != this.button) touchUpOver = false
        if (touchUpOver) {
          val time = TimeUtils.nanoTime()
          if (time - lastTapTime > tapCountInterval) tapCount = 0
          tapCount += 1
          lastTapTime = time
          clicked(event, x, y)
        }
      }
      _pressed = false
      _pressedPointer = -1
      _pressedButton = -1
      cancelled = false
    }

  override def enter(event: InputEvent, x: Float, y: Float, pointer: Int, fromActor: Nullable[Actor]): Unit =
    if (pointer == -1 && !cancelled) _over = true

  override def exit(event: InputEvent, x: Float, y: Float, pointer: Int, toActor: Nullable[Actor]): Unit =
    if (pointer == -1 && !cancelled) _over = false

  /** If a touch down is being monitored, the drag and touch up events are ignored until the next touch up. */
  def cancel(): Unit =
    if (_pressedPointer == -1) ()
    else {
      cancelled = true
      _pressed = false
    }

  def clicked(event: InputEvent, x: Float, y: Float): Unit = {}

  /** Returns true if the specified position is over the specified actor or within the tap square. */
  def isOver(actor: Actor, x: Float, y: Float): Boolean = {
    val hit = actor.hit(x, y, true)
    hit.fold(inTapSquare(x, y)) { h =>
      if (!h.isDescendantOf(actor)) inTapSquare(x, y) else true
    }
  }

  def inTapSquare(x: Float, y: Float): Boolean =
    if (_touchDownX == -1 && _touchDownY == -1) false
    else Math.abs(x - _touchDownX) < tapSquareSize && Math.abs(y - _touchDownY) < tapSquareSize

  /** Returns true if a touch is within the tap square. */
  def inTapSquare(): Boolean =
    _touchDownX != -1

  /** The tap square will no longer be used for the current touch. */
  def invalidateTapSquare(): Unit = {
    _touchDownX = -1
    _touchDownY = -1
  }

  /** Returns true if a touch is over the actor or within the tap square. */
  def pressed: Boolean = _pressed

  /** Returns true if a touch is over the actor or within the tap square or has been very recently. This allows the UI to show a press and release that was so fast it occurred within a single frame.
    */
  def isVisualPressed: Boolean =
    if (_pressed) true
    else if (visualPressedTime <= Millis.zero) false
    else if (visualPressedTime > TimeUtils.millis()) true
    else {
      visualPressedTime = Millis.zero
      false
    }

  /** If true, sets the visual pressed time to now. If false, clears the visual pressed time. */
  def setVisualPressed(visualPressed: Boolean): Unit =
    if (visualPressed)
      visualPressedTime = TimeUtils.millis() + Millis((ClickListener.visualPressedDuration * 1000).toLong)
    else
      visualPressedTime = Millis.zero

  /** Returns true if the mouse or touch is over the actor or pressed and within the tap square. */
  def over: Boolean = _over || _pressed

  /** @param tapCountInterval
    *   time in seconds that must pass for two touch down/up sequences to be detected as consecutive taps.
    */
  def setTapCountInterval(tapCountInterval: Float): Unit =
    this.tapCountInterval = Nanos((tapCountInterval * 1000000000L).toLong)

  def touchDownX: Float = _touchDownX

  def touchDownY: Float = _touchDownY

  /** The button that initially pressed this button or -1 if the button is not pressed. */
  def pressedButton: Int = _pressedButton

  /** The pointer that initially pressed this button or -1 if the button is not pressed. */
  def pressedPointer: Int = _pressedPointer
}

object ClickListener {

  /** Time in seconds {@link #isVisualPressed()} reports true after a press resulting in a click is released. */
  var visualPressedDuration: Float = 0.1f
}
