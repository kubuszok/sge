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
 * - Renames: getTapSquareSize/setTapSquareSize → var tapSquareSize, getButton/setButton → var button,
 *   getTouchDownX/Y → def touchDownX/touchDownY, getStageTouchDownX/Y → def stageTouchDownX/stageTouchDownY,
 *   getDragStartX/Y/setDragStartX/Y → var dragStartX/dragStartY, getDragX/Y → def dragX/dragY,
 *   getDeltaX/Y → def deltaX/deltaY, getDragDistance → def dragDistance, isDragging → def dragging
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
  var tapSquareSize:            Float   = 14
  private var _touchDownX:      Float   = -1
  private var _touchDownY:      Float   = -1
  private var _stageTouchDownX: Float   = -1
  private var _stageTouchDownY: Float   = -1
  var dragStartX:               Float   = 0
  var dragStartY:               Float   = 0
  private var dragLastX:        Float   = 0
  private var dragLastY:        Float   = 0
  private var _dragX:           Float   = 0
  private var _dragY:           Float   = 0
  private var pressedPointer:   Int     = -1
  var button:                   Int     = 0
  private var _dragging:        Boolean = false

  override def touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean =
    if (pressedPointer != -1) false
    else if (pointer == 0 && this.button != -1 && button != this.button) false
    else {
      pressedPointer = pointer
      _touchDownX = x
      _touchDownY = y
      _stageTouchDownX = event.stageX
      _stageTouchDownY = event.stageY
      true
    }

  override def touchDragged(event: InputEvent, x: Float, y: Float, pointer: Int): Unit =
    if (pointer == pressedPointer) {
      if (!_dragging && (Math.abs(_touchDownX - x) > tapSquareSize || Math.abs(_touchDownY - y) > tapSquareSize)) {
        _dragging = true
        dragStartX = x
        dragStartY = y
        dragStart(event, x, y, pointer)
        _dragX = x
        _dragY = y
      }
      if (_dragging) {
        dragLastX = _dragX
        dragLastY = _dragY
        _dragX = x
        _dragY = y
        drag(event, x, y, pointer)
      }
    }

  override def touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Unit =
    if (pointer == pressedPointer && (this.button == -1 || button == this.button)) {
      if (_dragging) dragStop(event, x, y, pointer)
      cancel()
    }

  def dragStart(event: InputEvent, x: Float, y: Float, pointer: Int): Unit = {}

  def drag(event: InputEvent, x: Float, y: Float, pointer: Int): Unit = {}

  def dragStop(event: InputEvent, x: Float, y: Float, pointer: Int): Unit = {}

  /* If a drag is in progress, no further drag methods will be called until a new drag is started. */
  def cancel(): Unit = {
    _dragging = false
    pressedPointer = -1
  }

  /** Returns true if a touch has been dragged outside the tap square. */
  def dragging: Boolean = _dragging

  def touchDownX: Float = _touchDownX

  def touchDownY: Float = _touchDownY

  def stageTouchDownX: Float = _stageTouchDownX

  def stageTouchDownY: Float = _stageTouchDownY

  def dragX: Float = _dragX

  def dragY: Float = _dragY

  /** The distance from drag start to the current drag position. */
  def dragDistance: Float = {
    val dx = _dragX - dragStartX
    val dy = _dragY - dragStartY
    Math.sqrt(dx * dx + dy * dy).toFloat
  }

  /** Returns the amount on the x axis that the touch has been dragged since the last drag event. */
  def deltaX: Float = _dragX - dragLastX

  /** Returns the amount on the y axis that the touch has been dragged since the last drag event. */
  def deltaY: Float = _dragY - dragLastY
}
