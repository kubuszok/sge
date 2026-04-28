/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/InputMultiplexer.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Convention: Int key/button params → opaque Key/Button types
 *   Audited: 2026-03-03
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 103
 * Covenant-baseline-methods: InputMultiplexer,addProcessor,clear,items,keyDown,keyTyped,keyUp,mouseMoved,processEvent,processors,removeProcessor,scrolled,setProcessors,size,this,touchCancelled,touchDown,touchDragged,touchUp
 * Covenant-source-reference: com/badlogic/gdx/InputMultiplexer.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 4e54c0a351d4f009a42973d70fdf164128b97c2b
 */
package sge

import Input.{ Button, Key }
import sge.utils.DynamicArray
import scala.util.boundary, boundary.break

/** An {@link InputProcessor} that delegates to an ordered list of other InputProcessors. Delegation for an event stops if a processor returns true, which indicates that the event was handled.
  * @author
  *   Nathan Sweet (original implementation)
  */
class InputMultiplexer extends InputProcessor {
  val processors = DynamicArray[InputProcessor]()

  def this(processors: InputProcessor*) = {
    this()
    this.processors.addAll(processors)
  }

  def addProcessor(index: Int, processor: InputProcessor): Unit =
    processors.insert(index, processor)

  def removeProcessor(index: Int): Unit =
    processors.removeIndex(index)

  def addProcessor(processor: InputProcessor): Unit =
    processors.add(processor)

  def removeProcessor(processor: InputProcessor): Unit =
    processors.removeValue(processor)

  /** @return the number of processors in this multiplexer */
  def size(): Int =
    processors.size

  def clear(): Unit =
    processors.clear()

  def setProcessors(processors: InputProcessor*): Unit = {
    this.processors.clear()
    this.processors.addAll(processors)
  }

  def setProcessors(processors: DynamicArray[InputProcessor]): Unit = {
    this.processors.clear()
    this.processors.addAll(processors)
  }

  private def processEvent(event: InputProcessor => Boolean): Boolean = {
    // Use snapshot iteration (begin/end) for re-entrant safety, matching Java's SnapshotArray usage
    val items = processors.begin()
    try
      boundary {
        val n = processors.size
        var i = 0
        while (i < n) {
          if (event(items(i))) break(true)
          i += 1
        }
        false
      }
    finally
      processors.end()
  }

  override def keyDown(keycode: Key): Boolean =
    processEvent(_.keyDown(keycode))

  override def keyUp(keycode: Key): Boolean =
    processEvent(_.keyUp(keycode))

  override def keyTyped(character: Char): Boolean =
    processEvent(_.keyTyped(character))

  override def touchDown(screenX: Pixels, screenY: Pixels, pointer: Int, button: Button): Boolean =
    processEvent(_.touchDown(screenX, screenY, pointer, button))

  override def touchUp(screenX: Pixels, screenY: Pixels, pointer: Int, button: Button): Boolean =
    processEvent(_.touchUp(screenX, screenY, pointer, button))

  override def touchCancelled(screenX: Pixels, screenY: Pixels, pointer: Int, button: Button): Boolean =
    processEvent(_.touchCancelled(screenX, screenY, pointer, button))

  override def touchDragged(screenX: Pixels, screenY: Pixels, pointer: Int): Boolean =
    processEvent(_.touchDragged(screenX, screenY, pointer))

  override def mouseMoved(screenX: Pixels, screenY: Pixels): Boolean =
    processEvent(_.mouseMoved(screenX, screenY))

  override def scrolled(amountX: Float, amountY: Float): Boolean =
    processEvent(_.scrolled(amountX, amountY))
}
