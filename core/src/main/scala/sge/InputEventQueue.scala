package sge

import scala.collection.mutable.ArrayBuffer
import sge.utils.{ Nullable, NumberUtils }

/** Queues events that are later passed to an {@link InputProcessor} .
  * @author
  *   Nathan Sweet (original implementation)
  */
class InputEventQueue {
  import InputEventQueue.*

  private val queue           = ArrayBuffer[Int]()
  private val processingQueue = ArrayBuffer[Int]()
  private var currentEventTime: Long = 0L

  def drain(processor: Nullable[InputProcessor]): Unit = scala.util.boundary {
    synchronized {
      processor.fold {
        queue.clear()
        scala.util.boundary.break()
      } { proc =>
        processingQueue.clear()
        processingQueue.addAll(queue)
        queue.clear()
      }
    }

    processor.fold(()) { proc =>
      val q = processingQueue.toArray
      var i = 0
      val n = processingQueue.size
      while (i < n) {
        val eventType = q(i)
        i += 1
        currentEventTime = (q(i).toLong << 32) | (q(i + 1) & 0xffffffffL)
        i += 2
        eventType match {
          case SKIP =>
            i += q(i)
          case KEY_DOWN =>
            proc.keyDown(q(i))
            i += 1
          case KEY_UP =>
            proc.keyUp(q(i))
            i += 1
          case KEY_TYPED =>
            proc.keyTyped(q(i).toChar)
            i += 1
          case TOUCH_DOWN =>
            proc.touchDown(q(i), q(i + 1), q(i + 2), q(i + 3))
            i += 4
          case TOUCH_UP =>
            proc.touchUp(q(i), q(i + 1), q(i + 2), q(i + 3))
            i += 4
          case TOUCH_DRAGGED =>
            proc.touchDragged(q(i), q(i + 1), q(i + 2))
            i += 3
          case MOUSE_MOVED =>
            proc.mouseMoved(q(i), q(i + 1))
            i += 2
          case SCROLLED =>
            proc.scrolled(NumberUtils.intBitsToFloat(q(i)), NumberUtils.intBitsToFloat(q(i + 1)))
            i += 2
          case _ =>
            throw new RuntimeException()
        }
      }
      processingQueue.clear()
    }
  }

  private def next(nextType: Int, startIndex: Int): Int =
    synchronized {
      scala.util.boundary {
        val q = queue.toArray
        var i = startIndex
        val n = queue.size
        while (i < n) {
          val eventType = q(i)
          if (eventType == nextType) scala.util.boundary.break(i)
          i += 3
          eventType match {
            case SKIP =>
              i += q(i)
            case KEY_DOWN =>
              i += 1
            case KEY_UP =>
              i += 1
            case KEY_TYPED =>
              i += 1
            case TOUCH_DOWN =>
              i += 4
            case TOUCH_UP =>
              i += 4
            case TOUCH_DRAGGED =>
              i += 3
            case MOUSE_MOVED =>
              i += 2
            case SCROLLED =>
              i += 2
            case _ =>
              throw new RuntimeException()
          }
        }
        -1
      }
    }

  private def queueTime(time: Long): Unit = {
    queue.addOne((time >> 32).toInt)
    queue.addOne(time.toInt)
  }

  def keyDown(keycode: Int, time: Long): Boolean =
    synchronized {
      queue.addOne(KEY_DOWN)
      queueTime(time)
      queue.addOne(keycode)
      false
    }

  def keyUp(keycode: Int, time: Long): Boolean =
    synchronized {
      queue.addOne(KEY_UP)
      queueTime(time)
      queue.addOne(keycode)
      false
    }

  def keyTyped(character: Char, time: Long): Boolean =
    synchronized {
      queue.addOne(KEY_TYPED)
      queueTime(time)
      queue.addOne(character.toInt)
      false
    }

  def touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int, time: Long): Boolean =
    synchronized {
      queue.addOne(TOUCH_DOWN)
      queueTime(time)
      queue.addOne(screenX)
      queue.addOne(screenY)
      queue.addOne(pointer)
      queue.addOne(button)
      false
    }

  def touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int, time: Long): Boolean =
    synchronized {
      queue.addOne(TOUCH_UP)
      queueTime(time)
      queue.addOne(screenX)
      queue.addOne(screenY)
      queue.addOne(pointer)
      queue.addOne(button)
      false
    }

  def touchDragged(screenX: Int, screenY: Int, pointer: Int, time: Long): Boolean =
    synchronized {
      // Skip any queued touch dragged events for the same pointer.
      var i = next(TOUCH_DRAGGED, 0)
      while (i >= 0) {
        if (queue(i + 5) == pointer) {
          queue(i) = SKIP
          queue(i + 3) = 3
        }
        i = next(TOUCH_DRAGGED, i + 6)
      }
      queue.addOne(TOUCH_DRAGGED)
      queueTime(time)
      queue.addOne(screenX)
      queue.addOne(screenY)
      queue.addOne(pointer)
      false
    }

  def mouseMoved(screenX: Int, screenY: Int, time: Long): Boolean =
    synchronized {
      // Skip any queued mouse moved events.
      var i = next(MOUSE_MOVED, 0)
      while (i >= 0) {
        queue(i) = SKIP
        queue(i + 3) = 2
        i = next(MOUSE_MOVED, i + 5)
      }
      queue.addOne(MOUSE_MOVED)
      queueTime(time)
      queue.addOne(screenX)
      queue.addOne(screenY)
      false
    }

  def scrolled(amountX: Float, amountY: Float, time: Long): Boolean =
    synchronized {
      queue.addOne(SCROLLED)
      queueTime(time)
      queue.addOne(NumberUtils.floatToIntBits(amountX))
      queue.addOne(NumberUtils.floatToIntBits(amountY))
      false
    }

  def getCurrentEventTime(): Long = currentEventTime
}

object InputEventQueue {
  final private val SKIP          = -1
  final private val KEY_DOWN      = 0
  final private val KEY_UP        = 1
  final private val KEY_TYPED     = 2
  final private val TOUCH_DOWN    = 3
  final private val TOUCH_UP      = 4
  final private val TOUCH_DRAGGED = 5
  final private val MOUSE_MOVED   = 6
  final private val SCROLLED      = 7
}
