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
 *   Issues: setTapCountInterval/setMaxFlingDelay are no-ops (tapCountIntervalNanos/maxFlingDelayNanos are val);
 *     setMaxFlingDelay signature changed from long to Float; VelocityTracker.getSum omitted (unused in Java);
 *     VelocityTracker.getAverage(Long[]) returns Float instead of long; implicit instead of using
 *   TODO: Java-style getters/setters -- isPanning, setLongPressSeconds
 *   TODO: named context parameter (implicit/using sge/sde: Sge) → anonymous (using Sge) + Sge() accessor
 *   TODO: opaque Pixels for screen coordinate params in touch methods -- see docs/improvements/opaque-types.md
 *   TODO: opaque Seconds for tapCountInterval, longPressDuration, maxFlingDelay; opaque Nanos for internal timing -- see docs/improvements/opaque-types.md
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package input

import sge.math.Vector2
import sge.utils.{ TimeUtils, Timer }
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
  tapCountInterval:       Float = 0.4f,
  longPressDuration:      Float = 1.1f,
  maxFlingDelay:          Float = Integer.MAX_VALUE,
  val listener:           GestureDetector.GestureListener
)(implicit sde: sge.Sge)
    extends InputProcessor {

  def this(listener: GestureDetector.GestureListener)(implicit sde: sge.Sge) = {
    this(20f, 20f, 0.4f, 1.1f, Integer.MAX_VALUE, listener)
  }

  def this(halfTapSquareSize: Float, tapCountInterval: Float, longPressDuration: Float, maxFlingDelay: Float, listener: GestureDetector.GestureListener)(implicit sde: sge.Sge) = {
    this(halfTapSquareSize, halfTapSquareSize, tapCountInterval, longPressDuration, maxFlingDelay, listener)
  }

  private var tapRectangleWidth:     Float = halfTapRectangleWidth
  private var tapRectangleHeight:    Float = halfTapRectangleHeight
  private val tapCountIntervalNanos: Long  = (tapCountInterval * 1000000000L).toLong
  private var longPressSeconds:      Float = longPressDuration
  private val maxFlingDelayNanos:    Long  = (maxFlingDelay * 1000000000L).toLong

  private var inTapRectangle: Boolean = false
  private var tapCount:       Int     = 0
  private var lastTapTime:    Long    = 0L
  private var lastTapX:       Float   = 0f
  private var lastTapY:       Float   = 0f
  private var lastTapButton:  Int     = 0
  private var lastTapPointer: Int     = 0
  var longPressFired:         Boolean = false
  private var pinching:       Boolean = false
  private var panning:        Boolean = false

  private val tracker = new GestureDetector.VelocityTracker()
  private var tapRectangleCenterX: Float = 0f
  private var tapRectangleCenterY: Float = 0f
  private var touchDownTime:       Long  = 0L
  val pointer1                = new Vector2()
  private val pointer2        = new Vector2()
  private val initialPointer1 = new Vector2()
  private val initialPointer2 = new Vector2()

  private val longPressTask = new Task() {
    override def run(): Unit =
      if (!longPressFired) longPressFired = listener.longPress(pointer1.x, pointer1.y)
  }

  override def touchDown(x: Int, y: Int, pointer: Int, button: Int): Boolean =
    touchDown(x.toFloat, y.toFloat, pointer, button)

  def touchDown(x: Float, y: Float, pointer: Int, button: Int): Boolean = scala.util.boundary {
    if (pointer > 1) scala.util.boundary.break(false)

    if (pointer == 0) {
      pointer1.set(x, y)
      touchDownTime = sde.input.getCurrentEventTime()
      tracker.start(x, y, touchDownTime)
      if (sde.input.isTouched(1)) {
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

  override def touchDragged(x: Int, y: Int, pointer: Int): Boolean =
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
    tracker.update(x, y, sde.input.getCurrentEventTime())

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

  override def touchUp(x: Int, y: Int, pointer: Int, button: Int): Boolean =
    touchUp(x.toFloat, y.toFloat, pointer, button)

  def touchUp(x: Float, y: Float, pointer: Int, button: Int): Boolean = scala.util.boundary {
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
      touchDownTime = 0
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
        tracker.start(pointer2.x, pointer2.y, sde.input.getCurrentEventTime())
      } else {
        // second pointer has lifted off, set up panning to use the first pointer...
        tracker.start(pointer1.x, pointer1.y, sde.input.getCurrentEventTime())
      }
      scala.util.boundary.break(false)
    }

    // handle no longer panning
    var handled = false
    if (wasPanning && !panning) handled = listener.panStop(x, y, pointer, button)

    // handle fling
    val time = sde.input.getCurrentEventTime()
    if (time - touchDownTime <= maxFlingDelayNanos) {
      tracker.update(x, y, time)
      handled = listener.fling(tracker.getVelocityX(), tracker.getVelocityY(), button) || handled
    }
    touchDownTime = 0
    handled
  }

  override def touchCancelled(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean = {
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
  def isLongPressed(duration: Float): Boolean =
    if (touchDownTime == 0) false
    else TimeUtils.nanoTime() - touchDownTime > (duration * 1000000000L).toLong

  def isPanning(): Boolean = panning

  def reset(): Unit = {
    longPressTask.cancel()
    touchDownTime = 0
    panning = false
    inTapRectangle = false
    tracker.lastTime = 0
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
  def setTapCountInterval(tapCountInterval: Float): Unit = {
    // this.tapCountIntervalNanos = (tapCountInterval * 1000000000L).toLong
  }

  def setLongPressSeconds(longPressSeconds: Float): Unit =
    this.longPressSeconds = longPressSeconds

  def setMaxFlingDelay(maxFlingDelay: Float): Unit = {
    // this.maxFlingDelayNanos = (maxFlingDelay * 1000000000L).toLong
  }

  // Additional methods that might be missing from InputProcessor
  override def keyDown(keycode:    Int):                   Boolean = false
  override def keyUp(keycode:      Int):                   Boolean = false
  override def keyTyped(character: Char):                  Boolean = false
  override def mouseMoved(screenX: Int, screenY:   Int):   Boolean = false
  override def scrolled(amountX:   Float, amountY: Float): Boolean = false
}

object GestureDetector {

  /** Register an instance of this class with a {@link GestureDetector} to receive gestures such as taps, long presses, flings, panning or pinch zooming. Each method returns a boolean indicating if
    * the event should be handed to the next listener (false to hand it to the next listener, true otherwise).
    * @author
    *   mzechner
    */
  trait GestureListener {

    /** @see InputProcessor#touchDown(int, int, int, int) */
    def touchDown(x: Float, y: Float, pointer: Int, button: Int): Boolean

    /** Called when a tap occured. A tap happens if a touch went down on the screen and was lifted again without moving outside of the tap square. The tap square is a rectangular area around the
      * initial touch position as specified on construction time of the {@link GestureDetector} .
      * @param count
      *   the number of taps.
      */
    def tap(x: Float, y: Float, count: Int, button: Int): Boolean

    def longPress(x: Float, y: Float): Boolean

    /** Called when the user dragged a finger over the screen and lifted it. Reports the last known velocity of the finger in pixels per second.
      * @param velocityX
      *   velocity on x in seconds
      * @param velocityY
      *   velocity on y in seconds
      */
    def fling(velocityX: Float, velocityY: Float, button: Int): Boolean

    /** Called when the user drags a finger over the screen.
      * @param deltaX
      *   the difference in pixels to the last drag event on x.
      * @param deltaY
      *   the difference in pixels to the last drag event on y.
      */
    def pan(x: Float, y: Float, deltaX: Float, deltaY: Float): Boolean

    /** Called when no longer panning. */
    def panStop(x: Float, y: Float, pointer: Int, button: Int): Boolean

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
    override def touchDown(x:           Float, y:                 Float, pointer:    Int, button:       Int):     Boolean = false
    override def tap(x:                 Float, y:                 Float, count:      Int, button:       Int):     Boolean = false
    override def longPress(x:           Float, y:                 Float):                                         Boolean = false
    override def fling(velocityX:       Float, velocityY:         Float, button:     Int):                        Boolean = false
    override def pan(x:                 Float, y:                 Float, deltaX:     Float, deltaY:     Float):   Boolean = false
    override def panStop(x:             Float, y:                 Float, pointer:    Int, button:       Int):     Boolean = false
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
    var lastTime:   Long         = 0L
    var numSamples: Int          = 0
    val meanX:      Array[Float] = Array.ofDim[Float](sampleSize)
    val meanY:      Array[Float] = Array.ofDim[Float](sampleSize)
    val meanTime:   Array[Long]  = Array.ofDim[Long](sampleSize)

    def start(x: Float, y: Float, timeStamp: Long): Unit = {
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

    def update(x: Float, y: Float, currTime: Long): Unit = {
      deltaX = x - lastX
      deltaY = y - lastY
      lastX = x
      lastY = y
      val deltaTime = currTime - lastTime
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
