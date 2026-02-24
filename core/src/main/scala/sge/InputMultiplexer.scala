/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/InputMultiplexer.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge

import scala.collection.mutable.ArrayBuffer
import sge.utils.Nullable

/** An {@link InputProcessor} that delegates to an ordered list of other InputProcessors. Delegation for an event stops if a processor returns true, which indicates that the event was handled.
  * @author
  *   Nathan Sweet (original implementation)
  */
class InputMultiplexer extends InputProcessor {
  private var processors = ArrayBuffer[InputProcessor]()
  private var processingCopy: ArrayBuffer[InputProcessor] = ArrayBuffer()
  private var inProcessing = false

  def this(processors: InputProcessor*) = {
    this()
    this.processors.addAll(processors)
  }

  def addProcessor(index: Int, processor: InputProcessor): Unit = {
    if (processor == null) throw new IllegalArgumentException("processor cannot be null")
    processors.insert(index, processor)
  }

  def removeProcessor(index: Int): Unit =
    processors.remove(index)

  def addProcessor(processor: InputProcessor): Unit = {
    if (processor == null) throw new IllegalArgumentException("processor cannot be null")
    processors.addOne(processor)
  }

  def removeProcessor(processor: InputProcessor): Unit =
    processors.subtractOne(processor)

  /** @return the number of processors in this multiplexer */
  def size(): Int =
    processors.size

  def clear(): Unit =
    processors.clear()

  def setProcessors(processors: InputProcessor*): Unit = {
    this.processors.clear()
    this.processors.addAll(processors)
  }

  def setProcessors(processors: ArrayBuffer[InputProcessor]): Unit = {
    this.processors.clear()
    this.processors.addAll(processors)
  }

  def getProcessors(): ArrayBuffer[InputProcessor] =
    processors

  private def processEvent[T](event: InputProcessor => Boolean): Boolean = {
    // Create a copy for safe iteration
    processingCopy.clear()
    processingCopy.addAll(processors)

    processingCopy.exists(event)
  }

  override def keyDown(keycode: Int): Boolean =
    processEvent(_.keyDown(keycode))

  override def keyUp(keycode: Int): Boolean =
    processEvent(_.keyUp(keycode))

  override def keyTyped(character: Char): Boolean =
    processEvent(_.keyTyped(character))

  override def touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean =
    processEvent(_.touchDown(screenX, screenY, pointer, button))

  override def touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean =
    processEvent(_.touchUp(screenX, screenY, pointer, button))

  override def touchCancelled(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean =
    processEvent(_.touchCancelled(screenX, screenY, pointer, button))

  override def touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean =
    processEvent(_.touchDragged(screenX, screenY, pointer))

  override def mouseMoved(screenX: Int, screenY: Int): Boolean =
    processEvent(_.mouseMoved(screenX, screenY))

  override def scrolled(amountX: Float, amountY: Float): Boolean =
    processEvent(_.scrolled(amountX, amountY))
}
