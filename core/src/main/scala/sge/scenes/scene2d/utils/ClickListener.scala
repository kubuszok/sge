/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/utils/ClickListener.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - No return statements, uses if/else expressions
 * - @Null Actor -> Nullable[Actor] for enter/exit fromActor/toActor params
 * - actor.hit returns Nullable[Actor] -> uses fold for null-safe check
 * - visualPressedDuration static field -> companion object var
 * - All methods faithfully ported
 * - TODO: Java-style getters/setters — getTapSquareSize/setTapSquareSize, getTapCount/setTapCount, getTouchDownX/Y, getButton/setButton, isPressed, isOver
 */
package sge
package scenes
package scene2d
package utils

import sge.utils.{ Nullable, TimeUtils }

/** Detects mouse over, mouse or finger touch presses, and clicks on an actor. A touch must go down over the actor and is considered pressed as long as it is over the actor or within the
  * {@link #setTapSquareSize(float) tap square}. This behavior makes it easier to press buttons on a touch interface when the initial touch happens near the edge of the actor. Double clicks can be
  * detected using {@link #getTapCount()}. Any touch (not just the first) will trigger this listener. While pressed, other touch downs are ignored.
  * @author
  *   Nathan Sweet
  */
class ClickListener(private var button: Int) extends InputListener {

  private var tapSquareSize:     Float   = 14
  private var touchDownX:        Float   = -1
  private var touchDownY:        Float   = -1
  private var pressedPointer:    Int     = -1
  private var pressedButton:     Int     = -1
  private var pressed:           Boolean = false
  private var over:              Boolean = false
  private var cancelled:         Boolean = false
  private var visualPressedTime: Long    = 0
  private var tapCountInterval:  Long    = (0.4f * 1000000000L).toLong
  private var tapCount:          Int     = 0
  private var lastTapTime:       Long    = 0

  /** Create a listener where {@link #clicked(InputEvent, float, float)} is only called for left clicks.
    * @see
    *   #ClickListener(int)
    */
  def this() = this(0)

  override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean =
    if (pressed) false
    else if (pointer == 0 && this.button != -1 && button != this.button) false
    else {
      pressed = true
      pressedPointer = pointer
      pressedButton = button
      touchDownX = x
      touchDownY = y
      setVisualPressed(true)
      true
    }

  override def touchDragged(event: InputEvent, x: Float, y: Float, pointer: Int): Unit =
    if (pointer == pressedPointer && !cancelled) {
      pressed = isOver(event.getListenerActor, x, y)
      if (!pressed) {
        // Once outside the tap square, don't use the tap square anymore.
        invalidateTapSquare()
      }
    }

  override def touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Unit =
    if (pointer == pressedPointer) {
      if (!cancelled) {
        var touchUpOver = isOver(event.getListenerActor, x, y)
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
      pressed = false
      pressedPointer = -1
      pressedButton = -1
      cancelled = false
    }

  override def enter(event: InputEvent, x: Float, y: Float, pointer: Int, fromActor: Nullable[Actor]): Unit =
    if (pointer == -1 && !cancelled) over = true

  override def exit(event: InputEvent, x: Float, y: Float, pointer: Int, toActor: Nullable[Actor]): Unit =
    if (pointer == -1 && !cancelled) over = false

  /** If a touch down is being monitored, the drag and touch up events are ignored until the next touch up. */
  def cancel(): Unit =
    if (pressedPointer == -1) ()
    else {
      cancelled = true
      pressed = false
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
    if (touchDownX == -1 && touchDownY == -1) false
    else Math.abs(x - touchDownX) < tapSquareSize && Math.abs(y - touchDownY) < tapSquareSize

  /** Returns true if a touch is within the tap square. */
  def inTapSquare(): Boolean =
    touchDownX != -1

  /** The tap square will no longer be used for the current touch. */
  def invalidateTapSquare(): Unit = {
    touchDownX = -1
    touchDownY = -1
  }

  /** Returns true if a touch is over the actor or within the tap square. */
  def isPressed: Boolean = pressed

  /** Returns true if a touch is over the actor or within the tap square or has been very recently. This allows the UI to show a press and release that was so fast it occurred within a single frame.
    */
  def isVisualPressed: Boolean =
    if (pressed) true
    else if (visualPressedTime <= 0) false
    else if (visualPressedTime > TimeUtils.millis()) true
    else {
      visualPressedTime = 0
      false
    }

  /** If true, sets the visual pressed time to now. If false, clears the visual pressed time. */
  def setVisualPressed(visualPressed: Boolean): Unit =
    if (visualPressed)
      visualPressedTime = TimeUtils.millis() + (ClickListener.visualPressedDuration * 1000).toLong
    else
      visualPressedTime = 0

  /** Returns true if the mouse or touch is over the actor or pressed and within the tap square. */
  def isOver: Boolean = over || pressed

  def setTapSquareSize(halfTapSquareSize: Float): Unit = tapSquareSize = halfTapSquareSize

  def getTapSquareSize: Float = tapSquareSize

  /** @param tapCountInterval
    *   time in seconds that must pass for two touch down/up sequences to be detected as consecutive taps.
    */
  def setTapCountInterval(tapCountInterval: Float): Unit =
    this.tapCountInterval = (tapCountInterval * 1000000000L).toLong

  /** Returns the number of taps within the tap count interval for the most recent click event. */
  def getTapCount: Int = tapCount

  def setTapCount(tapCount: Int): Unit = this.tapCount = tapCount

  def getTouchDownX: Float = touchDownX

  def getTouchDownY: Float = touchDownY

  /** The button that initially pressed this button or -1 if the button is not pressed. */
  def getPressedButton: Int = pressedButton

  /** The pointer that initially pressed this button or -1 if the button is not pressed. */
  def getPressedPointer: Int = pressedPointer

  /** @see #setButton(int) */
  def getButton: Int = button

  /** Sets the button to listen for, all other buttons are ignored. Default is {@link Buttons#LEFT}. Use -1 for any button. */
  def setButton(button: Int): Unit = this.button = button
}

object ClickListener {

  /** Time in seconds {@link #isVisualPressed()} reports true after a press resulting in a click is released. */
  var visualPressedDuration: Float = 0.1f
}
