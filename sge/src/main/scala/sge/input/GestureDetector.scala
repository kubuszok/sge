/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/input/GestureDetector.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: InputAdapter -> InputProcessor (no InputAdapter in SGE); dst -> distance
 *   Convention: Java static inner classes -> companion object members; Gdx singleton -> implicit Sge
 *   Idiom: boundary/break (7 return), split packages
 *   TODOs: 0
 *   Convention: anonymous (using Sge) + Sge() accessor
 *   Convention: opaque Pixels for screen coordinate params in InputProcessor overrides; opaque Key/Button for key/button params
 *   Convention: opaque Seconds for tapCountInterval, longPressDuration, maxFlingDelay; opaque Nanos for internal timing
 *   Audited: 2026-03-04
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package input

import sge.Input.{ Button, Key }
import sge.math.Vector2
import sge.utils.{ Nanos, Seconds, TimeUtils, Timer }
import sge.utils.Timer.Task
import sge.InputProcessor
import scala.math.min

/** {@link InputProcessor} implementation that detects gestures (tap, long press, fling, pan, zoom, pinch) and hands them to a {@link GestureListener} .
  * @author
  *   mzechner (original implementation)
  */
class GestureDetector(
  halfTapRectangleWidth:  Float = 20f,
  halfTapRectangleHeight: Float = 20f,
  tapCountInterval:       Seconds = Seconds(0.4f),
  longPressDuration:      Seconds = Seconds(1.1f),
  maxFlingDelay:          Seconds = Seconds(Integer.MAX_VALUE),
  val listener:           GestureDetector.GestureListener
)(using Sge)
    extends InputProcessor {

  def this(listener: GestureDetector.GestureListener)(using Sge) = {
    this(20f, 20f, Seconds(0.4f), Seconds(1.1f), Seconds(Integer.MAX_VALUE), listener)
  }

  def this(halfTapSquareSize: Float, tapCountInterval: Seconds, longPressDuration: Seconds, maxFlingDelay: Seconds, listener: GestureDetector.GestureListener)(using Sge) = {
    this(halfTapSquareSize, halfTapSquareSize, tapCountInterval, longPressDuration, maxFlingDelay, listener)
  }

  private var tapRectangleWidth:     Float   = halfTapRectangleWidth
  private var tapRectangleHeight:    Float   = halfTapRectangleHeight
  private var tapCountIntervalNanos: Nanos   = Nanos((tapCountInterval.toFloat * 1000000000L).toLong)
  private var longPressSeconds:      Seconds = longPressDuration
  private var maxFlingDelayNanos:    Nanos   = Nanos((maxFlingDelay.toFloat * 1000000000L).toLong)

  private var inTapRectangle: Boolean = false
  private var tapCount:       Int     = 0
  private var lastTapTime:    Nanos   = Nanos.zero
  private var lastTapX:       Float   = 0f
  private var lastTapY:       Float   = 0f
  private var lastTapButton:  Button  = Button(0)
  private var lastTapPointer: Int     = 0
  var longPressFired:         Boolean = false
  private var pinching:       Boolean = false
  private var panning:        Boolean = false

  private val tracker = GestureDetector.VelocityTracker()
  private var tapRectangleCenterX: Float = 0f
  private var tapRectangleCenterY: Float = 0f
  private var touchDownTime:       Nanos = Nanos.zero
  val pointer1                = Vector2()
  private val pointer2        = Vector2()
  private val initialPointer1 = Vector2()
  private val initialPointer2 = Vector2()

  private val longPressTask = new Task() {
    override def run(): Unit =
      if (!longPressFired) longPressFired = listener.longPress(pointer1.x, pointer1.y)
  }

  override def touchDown(x: Pixels, y: Pixels, pointer: Int, button: Button): Boolean =
    touchDown(x.toFloat, y.toFloat, pointer, button)

  def touchDown(x: Float, y: Float, pointer: Int, button: Button): Boolean = scala.util.boundary {
    if (pointer > 1) scala.util.boundary.break(false)

    if (pointer == 0) {
      pointer1.set(x, y)
      touchDownTime = Sge().input.currentEventTime
      tracker.start(x, y, touchDownTime)
      if (Sge().input.isTouched(1)) {
        // Start pinch.
        inTapRectangle = false
        pinching = true
        initialPointer1.set(pointer1)
        initialPointer2.set(pointer2)
        longPressTask.cancel()
      } else {
        // Normal touch down.
        inTapRectangle = true
        pinching = false
        longPressFired = false
        tapRectangleCenterX = x
        tapRectangleCenterY = y
        if (!longPressTask.isScheduled) Timer.schedule(longPressTask, longPressSeconds)
      }
    } else {
      // Start pinch.
      pointer2.set(x, y)
      inTapRectangle = false
      pinching = true
      initialPointer1.set(pointer1)
      initialPointer2.set(pointer2)
      longPressTask.cancel()
    }
    listener.touchDown(x, y, pointer, button)
  }

  override def touchDragged(x: Pixels, y: Pixels, pointer: Int): Boolean =
    touchDragged(x.toFloat, y.toFloat, pointer)

  def touchDragged(x: Float, y: Float, pointer: Int): Boolean = scala.util.boundary {
    if (pointer > 1) scala.util.boundary.break(false)
    if (longPressFired) scala.util.boundary.break(false)

    if (pointer == 0)
      pointer1.set(x, y)
    else
      pointer2.set(x, y)

    // handle pinch zoom
    if (pinching) {
      val result = listener.pinch(initialPointer1, initialPointer2, pointer1, pointer2)
      scala.util.boundary.break(listener.zoom(initialPointer1.distance(initialPointer2), pointer1.distance(pointer2)) || result)
    }

    // update tracker
    tracker.update(x, y, Sge().input.currentEventTime)

    // check if we are still tapping.
    if (inTapRectangle && !isWithinTapRectangle(x, y, tapRectangleCenterX, tapRectangleCenterY)) {
      longPressTask.cancel()
      inTapRectangle = false
    }

    // if we have left the tap square, we are panning
    if (!inTapRectangle) {
      panning = true
      scala.util.boundary.break(listener.pan(x, y, tracker.deltaX, tracker.deltaY))
    }

    false
  }

  override def touchUp(x: Pixels, y: Pixels, pointer: Int, button: Button): Boolean =
    touchUp(x.toFloat, y.toFloat, pointer, button)

  def touchUp(x: Float, y: Float, pointer: Int, button: Button): Boolean = scala.util.boundary {
    if (pointer > 1) scala.util.boundary.break(false)

    // check if we are still tapping.
    if (inTapRectangle && !isWithinTapRectangle(x, y, tapRectangleCenterX, tapRectangleCenterY))
      inTapRectangle = false

    val wasPanning = panning
    panning = false

    longPressTask.cancel()
    if (longPressFired) scala.util.boundary.break(false)

    if (inTapRectangle) {
      // handle taps
      if (
        lastTapButton != button || lastTapPointer != pointer ||
        TimeUtils.nanoTime() - lastTapTime > tapCountIntervalNanos ||
        !isWithinTapRectangle(x, y, lastTapX, lastTapY)
      ) {
        tapCount = 0
      }
      tapCount += 1
      lastTapTime = TimeUtils.nanoTime()
      lastTapX = x
      lastTapY = y
      lastTapButton = button
      lastTapPointer = pointer
      touchDownTime = Nanos.zero
      scala.util.boundary.break(listener.tap(x, y, tapCount, button))
    }

    if (pinching) {
      // handle pinch end
      pinching = false
      listener.pinchStop()
      panning = true
      // we are in pan mode again, reset velocity tracker
      if (pointer == 0) {
        // first pointer has lifted off, set up panning to use the second pointer...
        tracker.start(pointer2.x, pointer2.y, Sge().input.currentEventTime)
      } else {
        // second pointer has lifted off, set up panning to use the first pointer...
        tracker.start(pointer1.x, pointer1.y, Sge().input.currentEventTime)
      }
      scala.util.boundary.break(false)
    }

    // handle no longer panning
    var handled = false
    if (wasPanning && !panning) handled = listener.panStop(x, y, pointer, button)

    // handle fling
    val time = Sge().input.currentEventTime
    if (time - touchDownTime <= maxFlingDelayNanos) {
      tracker.update(x, y, time)
      handled = listener.fling(tracker.getVelocityX(), tracker.getVelocityY(), button) || handled
    }
    touchDownTime = Nanos.zero
    handled
  }

  override def touchCancelled(screenX: Pixels, screenY: Pixels, pointer: Int, button: Button): Boolean = {
    cancel()
    false
  }

  /** No further gesture events will be triggered for the current touch, if any. */
  def cancel(): Unit = {
    longPressTask.cancel()
    longPressFired = true
  }

  /** @return whether the user touched the screen long enough to trigger a long press event. */
  def isLongPressed(): Boolean = isLongPressed(longPressSeconds)

  /** @param duration
    * @return
    *   whether the user touched the screen for as much or more than the given duration.
    */
  def isLongPressed(duration: Seconds): Boolean =
    if (touchDownTime == Nanos.zero) false
    else TimeUtils.nanoTime() - touchDownTime > Nanos((duration.toFloat * 1000000000L).toLong)

  def isPanning(): Boolean = panning

  def reset(): Unit = {
    longPressTask.cancel()
    touchDownTime = Nanos.zero
    panning = false
    inTapRectangle = false
    tracker.lastTime = Nanos.zero
  }

  private def isWithinTapRectangle(x: Float, y: Float, centerX: Float, centerY: Float): Boolean =
    Math.abs(x - centerX) < tapRectangleWidth && Math.abs(y - centerY) < tapRectangleHeight

  /** The tap square will no longer be used for the current touch. */
  def invalidateTapSquare(): Unit =
    inTapRectangle = false

  def setTapSquareSize(halfTapSquareSize: Float): Unit =
    setTapRectangleSize(halfTapSquareSize, halfTapSquareSize)

  def setTapRectangleSize(halfTapRectangleWidth: Float, halfTapRectangleHeight: Float): Unit = {
    this.tapRectangleWidth = halfTapRectangleWidth
    this.tapRectangleHeight = halfTapRectangleHeight
  }

  /** @param tapCountInterval
    *   time in seconds that must pass for two touch down/up sequences to be detected as consecutive taps.
    */
  def setTapCountInterval(tapCountInterval: Seconds): Unit =
    this.tapCountIntervalNanos = Nanos((tapCountInterval.toFloat * 1000000000L).toLong)

  def setLongPressSeconds(longPressSeconds: Seconds): Unit =
    this.longPressSeconds = longPressSeconds

  def setMaxFlingDelay(maxFlingDelay: Nanos): Unit =
    this.maxFlingDelayNanos = maxFlingDelay

  // Additional methods that might be missing from InputProcessor
  override def keyDown(keycode:    Key):                     Boolean = false
  override def keyUp(keycode:      Key):                     Boolean = false
  override def keyTyped(character: Char):                    Boolean = false
  override def mouseMoved(screenX: Pixels, screenY: Pixels): Boolean = false
  override def scrolled(amountX:   Float, amountY:  Float):  Boolean = false
}

object GestureDetector {

  /** Register an instance of this class with a {@link GestureDetector} to receive gestures such as taps, long presses, flings, panning or pinch zooming. Each method returns a boolean indicating if
    * the event should be handed to the next listener (false to hand it to the next listener, true otherwise).
    * @author
    *   mzechner
    */
  trait GestureListener {

    /** @see InputProcessor#touchDown(int, int, int, int) */
    def touchDown(x: Float, y: Float, pointer: Int, button: Button): Boolean

    /** Called when a tap occured. A tap happens if a touch went down on the screen and was lifted again without moving outside of the tap square. The tap square is a rectangular area around the
      * initial touch position as specified on construction time of the {@link GestureDetector} .
      * @param count
      *   the number of taps.
      */
    def tap(x: Float, y: Float, count: Int, button: Button): Boolean

    def longPress(x: Float, y: Float): Boolean

    /** Called when the user dragged a finger over the screen and lifted it. Reports the last known velocity of the finger in pixels per second.
      * @param velocityX
      *   velocity on x in seconds
      * @param velocityY
      *   velocity on y in seconds
      */
    def fling(velocityX: Float, velocityY: Float, button: Button): Boolean

    /** Called when the user drags a finger over the screen.
      * @param deltaX
      *   the difference in pixels to the last drag event on x.
      * @param deltaY
      *   the difference in pixels to the last drag event on y.
      */
    def pan(x: Float, y: Float, deltaX: Float, deltaY: Float): Boolean

    /** Called when no longer panning. */
    def panStop(x: Float, y: Float, pointer: Int, button: Button): Boolean

    /** Called when the user performs a pinch zoom gesture. The original distance is the distance in pixels when the gesture started.
      * @param initialDistance
      *   distance between fingers when the gesture started.
      * @param distance
      *   current distance between fingers.
      */
    def zoom(initialDistance: Float, distance: Float): Boolean

    /** Called when a user performs a pinch zoom gesture. Reports the initial positions of the two involved fingers and their current positions.
      * @param initialPointer1
      * @param initialPointer2
      * @param pointer1
      * @param pointer2
      */
    def pinch(initialPointer1: Vector2, initialPointer2: Vector2, pointer1: Vector2, pointer2: Vector2): Boolean

    /** Called when no longer pinching. */
    def pinchStop(): Unit
  }

  /** Derrive from this if you only want to implement a subset of {@link GestureListener} .
    * @author
    *   mzechner
    */
  class GestureAdapter extends GestureListener {
    override def touchDown(x:           Float, y:                 Float, pointer:    Int, button:       Button):  Boolean = false
    override def tap(x:                 Float, y:                 Float, count:      Int, button:       Button):  Boolean = false
    override def longPress(x:           Float, y:                 Float):                                         Boolean = false
    override def fling(velocityX:       Float, velocityY:         Float, button:     Button):                     Boolean = false
    override def pan(x:                 Float, y:                 Float, deltaX:     Float, deltaY:     Float):   Boolean = false
    override def panStop(x:             Float, y:                 Float, pointer:    Int, button:       Button):  Boolean = false
    override def zoom(initialDistance:  Float, distance:          Float):                                         Boolean = false
    override def pinch(initialPointer1: Vector2, initialPointer2: Vector2, pointer1: Vector2, pointer2: Vector2): Boolean = false
    override def pinchStop():                                                                                     Unit    = {}
  }

  class VelocityTracker {
    val sampleSize = 10
    var lastX:      Float        = 0f
    var lastY:      Float        = 0f
    var deltaX:     Float        = 0f
    var deltaY:     Float        = 0f
    var lastTime:   Nanos        = Nanos.zero
    var numSamples: Int          = 0
    val meanX:      Array[Float] = Array.ofDim[Float](sampleSize)
    val meanY:      Array[Float] = Array.ofDim[Float](sampleSize)
    val meanTime:   Array[Long]  = Array.ofDim[Long](sampleSize)

    def start(x: Float, y: Float, timeStamp: Nanos): Unit = {
      lastX = x
      lastY = y
      deltaX = 0
      deltaY = 0
      numSamples = 0
      for (i <- 0 until sampleSize) {
        meanX(i) = 0
        meanY(i) = 0
        meanTime(i) = 0
      }
      lastTime = timeStamp
    }

    def update(x: Float, y: Float, currTime: Nanos): Unit = {
      deltaX = x - lastX
      deltaY = y - lastY
      lastX = x
      lastY = y
      val deltaTime = (currTime - lastTime).toLong
      lastTime = currTime
      val index = numSamples % sampleSize
      meanX(index) = deltaX
      meanY(index) = deltaY
      meanTime(index) = deltaTime
      numSamples += 1
    }

    def getVelocityX(): Float = {
      val meanX    = getAverage(this.meanX, numSamples)
      val meanTime = getAverage(this.meanTime, numSamples) / 1000000000.0f
      if (meanTime == 0) 0
      else meanX / meanTime
    }

    def getVelocityY(): Float = {
      val meanY    = getAverage(this.meanY, numSamples)
      val meanTime = getAverage(this.meanTime, numSamples) / 1000000000.0f
      if (meanTime == 0) 0
      else meanY / meanTime
    }

    private def getAverage(values: Array[Float], numSamples: Int): Float = {
      val numSamplesUsed = min(sampleSize, numSamples)
      var sum            = 0f
      for (i <- 0 until numSamplesUsed)
        sum += values(i)
      sum / numSamplesUsed
    }

    private def getAverage(values: Array[Long], numSamples: Int): Float = {
      val numSamplesUsed = min(sampleSize, numSamples)
      if (numSamplesUsed == 0) 0
      else {
        var sum = 0L
        for (i <- 0 until numSamplesUsed)
          sum += values(i)
        sum.toFloat / numSamplesUsed
      }
    }

  }
}
