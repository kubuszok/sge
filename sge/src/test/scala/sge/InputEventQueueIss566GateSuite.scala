/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * ISS-566 gating suite for sge.InputEventQueue.
 *
 * ISS-566 is a behaviour-PRESERVING performance fix: drain()/next() currently
 * read the decode buffer via `processingQueue.toArray` / `queue.toArray`
 * (a fresh O(n) copy per call), and the fix switches them to the zero-copy
 * backing array `processingQueue.items` / `queue.items` — exactly matching the
 * original Java InputEventQueue.java:49 (`int[] q = processingQueue.items`)
 * and :89 (`int[] q = queue.items`).
 *
 * Because the read array has identical CONTENTS either way, these tests PASS on
 * the current (unfixed) code. Their job is to be a MUTATION GATE: the index
 * walk that decodes the flat int buffer must remain byte-for-byte correct.
 * If any read offset in the `q(i...)` walk of drain() (Java lines 50-84) or the
 * skip-scan stride in next() (Java lines 88-127) is mutated (off-by-one index,
 * wrong field offset, wrong stride), the decoded events corrupt and these
 * assertions fail.
 *
 * Expected behaviour is hand-traced from the original Java
 * (original-src/libgdx/gdx/src/com/badlogic/gdx/InputEventQueue.java):
 *
 *   Event widths in the flat queue (1 type word + 2 time words + payload):
 *     KEY_DOWN/KEY_UP/KEY_TYPED  payload 1  -> width 4   (Java 57-65, 98-105)
 *     TOUCH_DOWN/TOUCH_UP        payload 4  -> width 7   (Java 66-71, 107-111)
 *     TOUCH_DRAGGED              payload 3  -> width 6   (Java 72-74, 113-115)
 *     MOUSE_MOVED                payload 2  -> width 5   (Java 75-77, 116-118)
 *     SCROLLED                   payload 2  -> width 5   (Java 78-80, 119-121)
 *
 *   touchDragged (Java 175-189): before enqueuing, next(TOUCH_DRAGGED, ...)
 *     walks the queue; for every earlier touchDragged with the SAME pointer
 *     (queue.get(i+5) == pointer, Java 178) it rewrites the head to SKIP and
 *     stores the skip stride 3 at i+3 (Java 179-180), so drain() (Java 54-56)
 *     advances `i += q[i]` over exactly the 3 payload words.
 *
 *   mouseMoved (Java 191-202): before enqueuing, every earlier mouseMoved is
 *     turned into SKIP with stride 2 at i+3 (Java 193-196).
 *
 *   drain (Java 40-86): decodes each event in FIFO order, reading
 *     currentEventTime = (long)timeHi << 32 | timeLo & 0xFFFFFFFFL (Java 52),
 *     then dispatching payload words at the exact offsets above. SKIP events
 *     emit nothing.
 */
package sge

import Input.{ Button, Key }
import lowlevel.Nullable
import sge.utils.{ Nanos, NumberUtils }

class InputEventQueueIss566GateSuite extends munit.FunSuite {

  /** A processor that records every callback it receives, in order, as a fully-decoded tuple. A single wrong read offset anywhere in the drain() index walk corrupts one of these tuples.
    */
  final private class RecordingProcessor extends InputProcessor {
    val events: scala.collection.mutable.ListBuffer[String] = scala.collection.mutable.ListBuffer.empty

    override def keyDown(keycode:    Key):                                      Boolean = { events += s"keyDown(${keycode.toInt})"; false }
    override def keyUp(keycode:      Key):                                      Boolean = { events += s"keyUp(${keycode.toInt})"; false }
    override def keyTyped(character: Char):                                     Boolean = { events += s"keyTyped(${character.toInt})"; false }
    override def touchDown(x: Pixels, y: Pixels, pointer: Int, button: Button): Boolean = {
      events += s"touchDown(${x.toInt},${y.toInt},$pointer,${button.toInt})"; false
    }
    override def touchUp(x: Pixels, y: Pixels, pointer: Int, button: Button): Boolean = {
      events += s"touchUp(${x.toInt},${y.toInt},$pointer,${button.toInt})"; false
    }
    override def touchDragged(x: Pixels, y: Pixels, pointer: Int): Boolean = {
      events += s"touchDragged(${x.toInt},${y.toInt},$pointer)"; false
    }
    override def mouseMoved(x:     Pixels, y:      Pixels): Boolean = { events += s"mouseMoved(${x.toInt},${y.toInt})"; false }
    override def scrolled(amountX: Float, amountY: Float):  Boolean = { events += s"scrolled($amountX,$amountY)"; false }
  }

  // Times chosen with distinct, non-zero high AND low 32-bit halves so that the
  // two-word time decode (Java line 52) is exercised; a swapped/dropped time
  // word would corrupt currentEventTime below.
  private val tKeyDown   = Nanos(0x0000000100000002L)
  private val tTouchDown = Nanos(0x0000000300000004L)
  private val tDrag0a    = Nanos(0x0000000500000006L)
  private val tMouse0    = Nanos(0x0000000700000008L)
  private val tScroll    = Nanos(0x000000090000000aL)
  private val tDrag0b    = Nanos(0x0000000b0000000cL)
  private val tMouse1    = Nanos(0x0000000d0000000eL)
  private val tDrag1     = Nanos(0x0000000f00000010L)
  private val tKeyUp     = Nanos(0x0000001100000012L)
  private val tTouchUp   = Nanos(0x0000001300000014L)

  /** Enqueue a dense, mixed sequence that drives BOTH skip-scan paths (touchDragged + mouseMoved use next()) interleaved with fixed-width events, then return the queue ready to drain.
    */
  private def loadMixedSequence(): InputEventQueue = {
    val q = new InputEventQueue

    q.keyDown(Key(0x2a), tKeyDown) // event #1, width 4
    q.touchDown(Pixels(10), Pixels(20), 0, Button(1), tTouchDown) // #2, width 7
    q.touchDragged(Pixels(11), Pixels(21), 0, tDrag0a) // #3, width 6, pointer 0
    q.mouseMoved(Pixels(30), Pixels(40), tMouse0) // #4, width 5
    q.scrolled(1.5f, -2.5f, tScroll) // #5, width 5

    // Second drag for pointer 0: next() scans and turns #3 into SKIP(3).
    q.touchDragged(Pixels(12), Pixels(22), 0, tDrag0b) // #6, width 6, pointer 0

    // Second mouseMoved: next() scans and turns #4 into SKIP(2).
    q.mouseMoved(Pixels(31), Pixels(41), tMouse1) // #7, width 5

    // Drag for a DIFFERENT pointer (1): next() must NOT skip the pointer-0 drag
    // (#6) — exercises the queue.get(i+5)==pointer guard (Java line 178).
    q.touchDragged(Pixels(13), Pixels(23), 1, tDrag1) // #8, width 6, pointer 1

    q.keyUp(Key(0x2a), tKeyUp) // #9, width 4
    q.touchUp(Pixels(10), Pixels(20), 0, Button(1), tTouchUp) // #10, width 7
    q
  }

  /** The exact, ordered callback sequence drain() must emit. #3 (drag, p0) and #4 (mouseMoved) are coalesced away by the skip-scan; everything else survives in FIFO order. Derived from the Java trace
    * documented in the header.
    */
  private val expectedEvents: List[String] = List(
    "keyDown(42)", // #1   0x2a == 42
    "touchDown(10,20,0,1)", // #2
    // #3 touchDragged(11,21,0) -> SKIP'd by #6
    // #4 mouseMoved(30,40)     -> SKIP'd by #7
    "scrolled(1.5,-2.5)", // #5
    "touchDragged(12,22,0)", // #6
    "mouseMoved(31,41)", // #7
    "touchDragged(13,23,1)", // #8
    "keyUp(42)", // #9
    "touchUp(10,20,0,1)" // #10
  )

  test("ISS-566 gate: drain decodes the coalesced mixed sequence to EXACTLY the right ordered callbacks") {
    val q    = loadMixedSequence()
    val proc = new RecordingProcessor
    q.drain(Nullable(proc))

    assertEquals(
      proc.events.toList,
      expectedEvents,
      "drain()'s flat-buffer index walk (Java InputEventQueue.java:50-84) must decode every payload word at the exact offset; " +
        "an off-by-one in the q(i...) read or a wrong SKIP stride corrupts this ordered set"
    )
  }

  test("ISS-566 gate: currentEventTime after drain equals the LAST processed event's two-word time") {
    val q    = loadMixedSequence()
    val proc = new RecordingProcessor
    q.drain(Nullable(proc))

    // Java line 52 recomputes currentEventTime for every decoded event; the
    // final value is the time of the last NON-skipped event (touchUp, #10).
    assertEquals(
      q.currentEventTime.toLong,
      tTouchUp.toLong,
      "currentEventTime must equal the time decoded for the last drained event; a swapped/dropped time word breaks the (hi<<32 | lo) reconstruction"
    )
  }

  test("ISS-566 gate: scrolled float bits round-trip through the flat int buffer") {
    // SCROLLED stores floatToIntBits and drain reads intBitsToFloat (Java 78-80);
    // pins that the two scroll payload words are read at i and i+1, not swapped.
    val q = new InputEventQueue
    q.scrolled(3.25f, -7.5f, tScroll)
    val proc = new RecordingProcessor
    q.drain(Nullable(proc))
    assertEquals(proc.events.toList, List("scrolled(3.25,-7.5)"))
    // Sanity: the encoding really is non-trivial (distinct from the int form).
    assert(NumberUtils.floatToIntBits(3.25f) != NumberUtils.floatToIntBits(-7.5f))
  }

  test("ISS-566 gate: a run of same-pointer drags coalesces to ONLY the last; other pointer survives") {
    // Three drags for pointer 0 then one for pointer 1. next()'s skip-scan
    // (Java 175-182) must turn the first two pointer-0 drags into SKIP and
    // leave the pointer-1 drag untouched (guard queue.get(i+5)==pointer).
    val q = new InputEventQueue
    q.touchDragged(Pixels(1), Pixels(2), 0, tDrag0a)
    q.touchDragged(Pixels(3), Pixels(4), 0, tDrag0b)
    q.touchDragged(Pixels(5), Pixels(6), 1, tDrag1)
    q.touchDragged(Pixels(7), Pixels(8), 0, tDrag1)
    val proc = new RecordingProcessor
    q.drain(Nullable(proc))
    assertEquals(
      proc.events.toList,
      List("touchDragged(5,6,1)", "touchDragged(7,8,0)"),
      "only the surviving (last) pointer-0 drag and the pointer-1 drag may remain; a wrong i+5 pointer offset or i+6 stride in next() corrupts this"
    )
  }

  test("ISS-566 gate: a run of mouseMoved coalesces to ONLY the last") {
    val q = new InputEventQueue
    q.mouseMoved(Pixels(1), Pixels(2), tMouse0)
    q.mouseMoved(Pixels(3), Pixels(4), tMouse1)
    q.mouseMoved(Pixels(5), Pixels(6), tScroll)
    val proc = new RecordingProcessor
    q.drain(Nullable(proc))
    assertEquals(
      proc.events.toList,
      List("mouseMoved(5,6)"),
      "all but the last mouseMoved must be SKIP'd; a wrong i+5 stride in next() or wrong SKIP stride in drain() corrupts this"
    )
  }
}
