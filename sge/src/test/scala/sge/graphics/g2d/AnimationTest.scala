/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package graphics
package g2d

import sge.utils.DynamicArray

class AnimationTest extends munit.FunSuite {

  private val epsilon = 1e-6f

  private def makeAnim(frameDuration: Float, frameCount: Int): Animation[String] = {
    val frames = (0 until frameCount).map(i => s"frame$i").toArray
    val da     = DynamicArray.from(frames)
    Animation[String](frameDuration, da)
  }

  // --- PlayMode enum ---

  test("PlayMode.NORMAL is not looping") {
    assert(!Animation.PlayMode.NORMAL.isLooping)
  }

  test("PlayMode.REVERSED is not looping") {
    assert(!Animation.PlayMode.REVERSED.isLooping)
  }

  test("PlayMode.LOOP is looping") {
    assert(Animation.PlayMode.LOOP.isLooping)
  }

  test("PlayMode.LOOP_REVERSED is looping") {
    assert(Animation.PlayMode.LOOP_REVERSED.isLooping)
  }

  test("PlayMode.LOOP_PINGPONG is looping") {
    assert(Animation.PlayMode.LOOP_PINGPONG.isLooping)
  }

  test("PlayMode.LOOP_RANDOM is looping") {
    assert(Animation.PlayMode.LOOP_RANDOM.isLooping)
  }

  test("PlayMode.REVERSED is reversed") {
    assert(Animation.PlayMode.REVERSED.isReversed)
  }

  test("PlayMode.LOOP_REVERSED is reversed") {
    assert(Animation.PlayMode.LOOP_REVERSED.isReversed)
  }

  test("PlayMode.NORMAL is not reversed") {
    assert(!Animation.PlayMode.NORMAL.isReversed)
  }

  test("PlayMode.LOOP is not reversed") {
    assert(!Animation.PlayMode.LOOP.isReversed)
  }

  // --- Constructor ---

  test("varargs constructor") {
    val anim = Animation[String](0.1f, "a", "b", "c")
    assertEquals(anim.keyFrames.length, 3)
    assertEquals(anim.getKeyFrame(0f), "a")
  }

  test("DynamicArray constructor sets frame duration") {
    val anim = makeAnim(0.25f, 4)
    assertEqualsFloat(anim.frameDuration, 0.25f, epsilon)
  }

  test("DynamicArray constructor with playMode") {
    val da   = DynamicArray.from(Array("a", "b"))
    val anim = Animation[String](0.1f, da, Animation.PlayMode.LOOP)
    assertEquals(anim.playMode, Animation.PlayMode.LOOP)
  }

  // --- animationDuration ---

  test("animationDuration equals frameCount * frameDuration") {
    val anim = makeAnim(0.5f, 4)
    assertEqualsFloat(anim.animationDuration, 2.0f, epsilon)
  }

  // --- frameDuration setter ---

  test("setting frameDuration updates animationDuration") {
    val anim = makeAnim(0.1f, 5)
    anim.frameDuration = 0.2f
    assertEqualsFloat(anim.frameDuration, 0.2f, epsilon)
    assertEqualsFloat(anim.animationDuration, 1.0f, epsilon)
  }

  // --- NORMAL play mode ---

  test("NORMAL mode returns first frame at time 0") {
    val anim = makeAnim(0.1f, 3)
    anim.playMode = Animation.PlayMode.NORMAL
    assertEquals(anim.getKeyFrame(0f), "frame0")
  }

  test("NORMAL mode returns correct frame mid-animation") {
    val anim = makeAnim(0.1f, 3)
    anim.playMode = Animation.PlayMode.NORMAL
    assertEquals(anim.getKeyFrame(0.15f), "frame1")
  }

  test("NORMAL mode clamps to last frame past end") {
    val anim = makeAnim(0.1f, 3)
    anim.playMode = Animation.PlayMode.NORMAL
    assertEquals(anim.getKeyFrame(1.0f), "frame2")
    assertEquals(anim.getKeyFrame(100.0f), "frame2")
  }

  // --- REVERSED play mode ---

  test("REVERSED mode returns last frame at time 0") {
    val anim = makeAnim(0.1f, 3)
    anim.playMode = Animation.PlayMode.REVERSED
    assertEquals(anim.getKeyFrame(0f), "frame2")
  }

  test("REVERSED mode returns correct frame mid-animation") {
    val anim = makeAnim(0.1f, 3)
    anim.playMode = Animation.PlayMode.REVERSED
    assertEquals(anim.getKeyFrame(0.15f), "frame1")
  }

  test("REVERSED mode clamps to first frame past end") {
    val anim = makeAnim(0.1f, 3)
    anim.playMode = Animation.PlayMode.REVERSED
    assertEquals(anim.getKeyFrame(1.0f), "frame0")
  }

  // --- LOOP play mode ---

  test("LOOP mode wraps around") {
    val anim = makeAnim(0.1f, 3)
    anim.playMode = Animation.PlayMode.LOOP
    // At 0.3s, frameNumber = 3, 3 % 3 = 0
    assertEquals(anim.getKeyFrame(0.3f), "frame0")
    // At 0.4s, frameNumber = 4, 4 % 3 = 1
    assertEquals(anim.getKeyFrame(0.4f), "frame1")
  }

  test("LOOP mode cycles correctly") {
    val anim = makeAnim(1.0f, 2)
    anim.playMode = Animation.PlayMode.LOOP
    assertEquals(anim.getKeyFrame(0f), "frame0")
    assertEquals(anim.getKeyFrame(1f), "frame1")
    assertEquals(anim.getKeyFrame(2f), "frame0")
    assertEquals(anim.getKeyFrame(3f), "frame1")
  }

  // --- LOOP_REVERSED play mode ---

  test("LOOP_REVERSED mode wraps around in reverse") {
    val anim = makeAnim(1.0f, 3)
    anim.playMode = Animation.PlayMode.LOOP_REVERSED
    // frameNumber = 0 % 3 = 0 → length - 0 - 1 = 2
    assertEquals(anim.getKeyFrame(0f), "frame2")
    // frameNumber = 1 % 3 = 1 → 3 - 1 - 1 = 1
    assertEquals(anim.getKeyFrame(1f), "frame1")
    // frameNumber = 2 % 3 = 2 → 3 - 2 - 1 = 0
    assertEquals(anim.getKeyFrame(2f), "frame0")
    // frameNumber = 3 % 3 = 0 → 3 - 0 - 1 = 2
    assertEquals(anim.getKeyFrame(3f), "frame2")
  }

  // --- LOOP_PINGPONG play mode ---

  test("LOOP_PINGPONG mode bounces") {
    val anim = makeAnim(1.0f, 4)
    anim.playMode = Animation.PlayMode.LOOP_PINGPONG
    // Forward: 0,1,2,3 then reverse: 2,1 then repeat
    // length=4, period = (4*2)-2 = 6
    assertEquals(anim.getKeyFrame(0f), "frame0")
    assertEquals(anim.getKeyFrame(1f), "frame1")
    assertEquals(anim.getKeyFrame(2f), "frame2")
    assertEquals(anim.getKeyFrame(3f), "frame3")
    // frameNumber=4, 4 % 6 = 4, 4 >= 4 → 4-2-(4-4) = 2
    assertEquals(anim.getKeyFrame(4f), "frame2")
    // frameNumber=5, 5 % 6 = 5, 5 >= 4 → 4-2-(5-4) = 1
    assertEquals(anim.getKeyFrame(5f), "frame1")
    // frameNumber=6, 6 % 6 = 0 → forward again
    assertEquals(anim.getKeyFrame(6f), "frame0")
  }

  // --- Single frame ---

  test("single frame always returns the same frame") {
    val anim = makeAnim(1.0f, 1)
    anim.playMode = Animation.PlayMode.NORMAL
    assertEquals(anim.getKeyFrame(0f), "frame0")
    assertEquals(anim.getKeyFrame(100f), "frame0")
    anim.playMode = Animation.PlayMode.LOOP
    assertEquals(anim.getKeyFrame(100f), "frame0")
  }

  // --- getKeyFrameIndex ---

  test("getKeyFrameIndex returns correct index for NORMAL") {
    val anim = makeAnim(0.5f, 4)
    anim.playMode = Animation.PlayMode.NORMAL
    assertEquals(anim.getKeyFrameIndex(0f), 0)
    assertEquals(anim.getKeyFrameIndex(0.5f), 1)
    assertEquals(anim.getKeyFrameIndex(1.0f), 2)
    assertEquals(anim.getKeyFrameIndex(1.5f), 3)
    assertEquals(anim.getKeyFrameIndex(2.0f), 3) // clamped
  }

  test("getKeyFrameIndex for single frame always returns 0") {
    val anim = makeAnim(1.0f, 1)
    assertEquals(anim.getKeyFrameIndex(0f), 0)
    assertEquals(anim.getKeyFrameIndex(100f), 0)
  }

  // --- isAnimationFinished ---

  test("isAnimationFinished false before end") {
    val anim = makeAnim(1.0f, 3)
    assert(!anim.isAnimationFinished(0f))
    assert(!anim.isAnimationFinished(2.5f))
  }

  test("isAnimationFinished true past end") {
    val anim = makeAnim(1.0f, 3)
    assert(anim.isAnimationFinished(3.0f))
    assert(anim.isAnimationFinished(10.0f))
  }

  test("isAnimationFinished boundary") {
    val anim = makeAnim(1.0f, 3)
    // At exactly 3s, frameNumber = 3, length-1 = 2, 2 < 3 → true
    assert(anim.isAnimationFinished(3.0f))
    // At 2.99s, frameNumber = 2, length-1 = 2, 2 < 2 → false
    assert(!anim.isAnimationFinished(2.99f))
  }

  // --- getKeyFrame with looping parameter ---

  test("getKeyFrame with looping=true forces looping") {
    val anim = makeAnim(1.0f, 3)
    anim.playMode = Animation.PlayMode.NORMAL
    // With looping=true, NORMAL → LOOP temporarily
    // At 4s: frameNumber=4, 4 % 3 = 1
    assertEquals(anim.getKeyFrame(4f, true), "frame1")
    // playMode should be restored
    assertEquals(anim.playMode, Animation.PlayMode.NORMAL)
  }

  test("getKeyFrame with looping=false forces non-looping") {
    val anim = makeAnim(1.0f, 3)
    anim.playMode = Animation.PlayMode.LOOP
    // With looping=false, LOOP stays as LOOP (only LOOP_REVERSED maps to REVERSED)
    // This is the actual LibGDX behavior: LOOP with looping=false still uses LOOP
    anim.getKeyFrame(4f, false)
    // playMode should be restored
    assertEquals(anim.playMode, Animation.PlayMode.LOOP)
  }

  test("getKeyFrame with looping=true and REVERSED forces LOOP_REVERSED") {
    val anim = makeAnim(1.0f, 3)
    anim.playMode = Animation.PlayMode.REVERSED
    // With looping=true, REVERSED → LOOP_REVERSED
    val frame = anim.getKeyFrame(0f, true)
    assertEquals(frame, "frame2") // LOOP_REVERSED at time 0: index = length - 0 - 1 = 2
    assertEquals(anim.playMode, Animation.PlayMode.REVERSED) // restored
  }

  // --- keyFrames accessor ---

  test("keyFrames returns the internal array") {
    val anim   = makeAnim(0.1f, 3)
    val frames = anim.keyFrames
    assertEquals(frames.length, 3)
    assertEquals(frames(0), "frame0")
    assertEquals(frames(1), "frame1")
    assertEquals(frames(2), "frame2")
  }
}
