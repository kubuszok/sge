/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/utils/DragListener.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - getDragDistance: Java uses Vector2.len(dx,dy), Scala uses Math.sqrt (equivalent)
 * - No return statements, uses if/else expressions
 * - All methods faithfully ported
 * - TODO: Java-style getters/setters — getTapSquareSize/setTapSquareSize, getTouchDownX/Y, getDragStartX/Y, getDragX/Y, getDeltaX/Y, getButton/setButton, isDragging
 */
package sge
package scenes
package scene2d
package utils

/** Detects mouse or finger touch drags on an actor. A touch must go down over the actor and a drag won't start until it is moved outside the {@link #setTapSquareSize(float) tap square}. Any touch
  * (not just the first) will trigger this listener. While pressed, other touch downs are ignored.
  * @author
  *   Nathan Sweet
  */
class DragListener extends InputListener {
  private var tapSquareSize:   Float   = 14
  private var touchDownX:      Float   = -1
  private var touchDownY:      Float   = -1
  private var stageTouchDownX: Float   = -1
  private var stageTouchDownY: Float   = -1
  private var dragStartX:      Float   = 0
  private var dragStartY:      Float   = 0
  private var dragLastX:       Float   = 0
  private var dragLastY:       Float   = 0
  private var _dragX:          Float   = 0
  private var _dragY:          Float   = 0
  private var pressedPointer:  Int     = -1
  private var button:          Int     = 0
  private var dragging:        Boolean = false

  override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean =
    if (pressedPointer != -1) false
    else if (pointer == 0 && this.button != -1 && button != this.button) false
    else {
      pressedPointer = pointer
      touchDownX = x
      touchDownY = y
      stageTouchDownX = event.stageX
      stageTouchDownY = event.stageY
      true
    }

  override def touchDragged(event: InputEvent, x: Float, y: Float, pointer: Int): Unit =
    if (pointer == pressedPointer) {
      if (!dragging && (Math.abs(touchDownX - x) > tapSquareSize || Math.abs(touchDownY - y) > tapSquareSize)) {
        dragging = true
        dragStartX = x
        dragStartY = y
        dragStart(event, x, y, pointer)
        _dragX = x
        _dragY = y
      }
      if (dragging) {
        dragLastX = _dragX
        dragLastY = _dragY
        _dragX = x
        _dragY = y
        drag(event, x, y, pointer)
      }
    }

  override def touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Unit =
    if (pointer == pressedPointer && (this.button == -1 || button == this.button)) {
      if (dragging) dragStop(event, x, y, pointer)
      cancel()
    }

  def dragStart(event: InputEvent, x: Float, y: Float, pointer: Int): Unit = {}

  def drag(event: InputEvent, x: Float, y: Float, pointer: Int): Unit = {}

  def dragStop(event: InputEvent, x: Float, y: Float, pointer: Int): Unit = {}

  /* If a drag is in progress, no further drag methods will be called until a new drag is started. */
  def cancel(): Unit = {
    dragging = false
    pressedPointer = -1
  }

  /** Returns true if a touch has been dragged outside the tap square. */
  def isDragging: Boolean = dragging

  def setTapSquareSize(halfTapSquareSize: Float): Unit = tapSquareSize = halfTapSquareSize

  def getTapSquareSize: Float = tapSquareSize

  def getTouchDownX: Float = touchDownX

  def getTouchDownY: Float = touchDownY

  def getStageTouchDownX: Float = stageTouchDownX

  def getStageTouchDownY: Float = stageTouchDownY

  def getDragStartX: Float = dragStartX

  def setDragStartX(dragStartX: Float): Unit = this.dragStartX = dragStartX

  def getDragStartY: Float = dragStartY

  def setDragStartY(dragStartY: Float): Unit = this.dragStartY = dragStartY

  def getDragX: Float = _dragX

  def getDragY: Float = _dragY

  /** The distance from drag start to the current drag position. */
  def getDragDistance: Float = {
    val dx = _dragX - dragStartX
    val dy = _dragY - dragStartY
    Math.sqrt(dx * dx + dy * dy).toFloat
  }

  /** Returns the amount on the x axis that the touch has been dragged since the last drag event. */
  def getDeltaX: Float = _dragX - dragLastX

  /** Returns the amount on the y axis that the touch has been dragged since the last drag event. */
  def getDeltaY: Float = _dragY - dragLastY

  def getButton: Int = button

  /** Sets the button to listen for, all other buttons are ignored. Default is {@link Buttons#LEFT}. Use -1 for any button. */
  def setButton(button: Int): Unit = this.button = button
}
