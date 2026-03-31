/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package input

import sge.Input.{ Button, Key }

class InputMultiplexerTest extends munit.FunSuite {

  /** A test processor that records calls and optionally consumes events. */
  private class RecordingProcessor(val consumeEvents: Boolean = false) extends InputProcessor {
    var keyDownCount:    Int = 0
    var keyUpCount:      Int = 0
    var touchDownCount:  Int = 0
    var touchUpCount:    Int = 0
    var mouseMovedCount: Int = 0
    var scrolledCount:   Int = 0
    var lastKey:         Key = Key(0)

    override def keyDown(keycode:        Key):                                                   Boolean = { keyDownCount += 1; lastKey = keycode; consumeEvents }
    override def keyUp(keycode:          Key):                                                   Boolean = { keyUpCount += 1; consumeEvents }
    override def keyTyped(character:     Char):                                                  Boolean = consumeEvents
    override def touchDown(screenX:      Pixels, screenY: Pixels, pointer: Int, button: Button): Boolean = { touchDownCount += 1; consumeEvents }
    override def touchUp(screenX:        Pixels, screenY: Pixels, pointer: Int, button: Button): Boolean = { touchUpCount += 1; consumeEvents }
    override def touchCancelled(screenX: Pixels, screenY: Pixels, pointer: Int, button: Button): Boolean = consumeEvents
    override def touchDragged(screenX:   Pixels, screenY: Pixels, pointer: Int):                 Boolean = consumeEvents
    override def mouseMoved(screenX:     Pixels, screenY: Pixels):                               Boolean = { mouseMovedCount += 1; consumeEvents }
    override def scrolled(amountX:       Float, amountY:  Float):                                Boolean = { scrolledCount += 1; consumeEvents }
  }

  test("empty multiplexer returns false for all events") {
    val mux = new InputMultiplexer()
    assertEquals(mux.keyDown(Key(42)), false)
    assertEquals(mux.touchDown(Pixels(0), Pixels(0), 0, Button(0)), false)
    assertEquals(mux.mouseMoved(Pixels(0), Pixels(0)), false)
  }

  test("events are dispatched to all processors when not consumed") {
    val p1  = new RecordingProcessor(consumeEvents = false)
    val p2  = new RecordingProcessor(consumeEvents = false)
    val mux = new InputMultiplexer(p1, p2)

    mux.keyDown(Key(65))
    assertEquals(p1.keyDownCount, 1)
    assertEquals(p2.keyDownCount, 1)
    assertEquals(p1.lastKey, Key(65))
  }

  test("events stop propagating when consumed") {
    val p1  = new RecordingProcessor(consumeEvents = true) // consumes
    val p2  = new RecordingProcessor(consumeEvents = false)
    val mux = new InputMultiplexer(p1, p2)

    val result = mux.keyDown(Key(65))
    assertEquals(result, true)
    assertEquals(p1.keyDownCount, 1)
    assertEquals(p2.keyDownCount, 0) // never reached
  }

  test("touchDown stops at first consumer") {
    val p1  = new RecordingProcessor(consumeEvents = false)
    val p2  = new RecordingProcessor(consumeEvents = true)
    val p3  = new RecordingProcessor(consumeEvents = false)
    val mux = new InputMultiplexer(p1, p2, p3)

    mux.touchDown(Pixels(100), Pixels(200), 0, Button(0))
    assertEquals(p1.touchDownCount, 1)
    assertEquals(p2.touchDownCount, 1)
    assertEquals(p3.touchDownCount, 0)
  }

  test("addProcessor and removeProcessor modify the list") {
    val p1  = new RecordingProcessor()
    val p2  = new RecordingProcessor()
    val mux = new InputMultiplexer()

    mux.addProcessor(p1)
    mux.addProcessor(p2)
    assertEquals(mux.size(), 2)

    mux.removeProcessor(p1)
    assertEquals(mux.size(), 1)

    mux.keyDown(Key(1))
    assertEquals(p1.keyDownCount, 0) // removed
    assertEquals(p2.keyDownCount, 1) // still there
  }

  test("clear removes all processors") {
    val p1  = new RecordingProcessor()
    val mux = new InputMultiplexer(p1)
    assertEquals(mux.size(), 1)
    mux.clear()
    assertEquals(mux.size(), 0)
  }

  test("scrolled dispatches to all non-consuming processors") {
    val p1  = new RecordingProcessor(consumeEvents = false)
    val p2  = new RecordingProcessor(consumeEvents = false)
    val mux = new InputMultiplexer(p1, p2)

    mux.scrolled(0f, 1f)
    assertEquals(p1.scrolledCount, 1)
    assertEquals(p2.scrolledCount, 1)
  }

  test("mouseMoved dispatches correctly") {
    val p1  = new RecordingProcessor(consumeEvents = false)
    val mux = new InputMultiplexer(p1)

    mux.mouseMoved(Pixels(50), Pixels(75))
    assertEquals(p1.mouseMovedCount, 1)
  }
}
