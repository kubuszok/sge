/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g2d/Animation.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g2d

import sge.math.MathUtils
import sge.utils.DynamicArray
import scala.compiletime.uninitialized
import scala.reflect.ClassTag

/** <p> An Animation stores a list of objects representing an animated sequence, e.g. for running or jumping. Each object in the Animation is called a key frame, and multiple key frames make up the
  * animation. <p> The animation's type is the class representing a frame of animation. For example, a typical 2D animation could be made up of
  * {@link com.badlogic.gdx.graphics.g2d.TextureRegion TextureRegions} and would be specified as: <p> <code>Animation&lt;TextureRegion&gt; myAnimation = new Animation&lt;TextureRegion&gt;(...);</code>
  *
  * @author
  *   mzechner (original implementation)
  */
class Animation[T: ClassTag](frameDuration: Float, keyFrames: DynamicArray[? <: T]) {

  /** Length must not be modified without updating {@link #animationDuration}. See {@link #setKeyFrames(T[])}. */
  private var keyFramesArray:    Array[T] = uninitialized
  private var frameDurationVar:  Float    = frameDuration
  private var animationDuration: Float    = uninitialized
  private var lastFrameNumber:   Int      = 0
  private var lastStateTime:     Float    = 0f

  private var playMode: Animation.PlayMode = Animation.PlayMode.NORMAL

  // Initialize keyframes from the DynamicArray
  val frames = keyFrames.toArray.asInstanceOf[Array[T]]
  setKeyFrames(frames*)

  /** Constructor, storing the frame duration and key frames.
    *
    * @param frameDuration
    *   the time between frames in seconds.
    * @param keyFrames
    *   the objects representing the frames. If this Array is type-aware, {@link #getKeyFrames()} can return the correct type of array. Otherwise, it returns an Object[].
    */
  def this(frameDuration: Float, keyFrames: DynamicArray[? <: T], playMode: Animation.PlayMode) = {
    this(frameDuration, keyFrames)
    setPlayMode(playMode)
  }

  /** Constructor, storing the frame duration and key frames.
    *
    * @param frameDuration
    *   the time between frames in seconds.
    * @param keyFrames
    *   the objects representing the frames.
    */
  def this(frameDuration: Float, keyFrames: T*) =
    this(frameDuration, DynamicArray.wrapRefUnchecked(keyFrames.toArray))

  /** Returns a frame based on the so called state time. This is the amount of seconds an object has spent in the state this Animation instance represents, e.g. running, jumping and so on. The mode
    * specifies whether the animation is looping or not.
    *
    * @param stateTime
    *   the time spent in the state represented by this animation.
    * @param looping
    *   whether the animation is looping or not.
    * @return
    *   the frame of animation for the given state time.
    */
  def getKeyFrame(stateTime: Float, looping: Boolean): T = {
    // we set the play mode by overriding the previous mode based on looping
    // parameter value
    val oldPlayMode = playMode
    if (looping && (playMode == Animation.PlayMode.NORMAL || playMode == Animation.PlayMode.REVERSED)) {
      if (playMode == Animation.PlayMode.NORMAL)
        playMode = Animation.PlayMode.LOOP
      else
        playMode = Animation.PlayMode.LOOP_REVERSED
    } else if (!looping && !(playMode == Animation.PlayMode.NORMAL || playMode == Animation.PlayMode.REVERSED)) {
      if (playMode == Animation.PlayMode.LOOP_REVERSED)
        playMode = Animation.PlayMode.REVERSED
      else
        playMode = Animation.PlayMode.LOOP
    }

    val frame = getKeyFrame(stateTime)
    playMode = oldPlayMode
    frame
  }

  /** Returns a frame based on the so called state time. This is the amount of seconds an object has spent in the state this Animation instance represents, e.g. running, jumping and so on using the
    * mode specified by {@link #setPlayMode(PlayMode)} method.
    *
    * @param stateTime
    * @return
    *   the frame of animation for the given state time.
    */
  def getKeyFrame(stateTime: Float): T = {
    val frameNumber = getKeyFrameIndex(stateTime)
    keyFramesArray(frameNumber)
  }

  /** Returns the current frame number.
    * @param stateTime
    * @return
    *   current frame number
    */
  def getKeyFrameIndex(stateTime: Float): Int = {
    if (keyFramesArray.length == 1) return 0

    var frameNumber = (stateTime / frameDurationVar).toInt
    frameNumber = playMode match {
      case Animation.PlayMode.NORMAL =>
        Math.min(keyFramesArray.length - 1, frameNumber)
      case Animation.PlayMode.LOOP =>
        frameNumber % keyFramesArray.length
      case Animation.PlayMode.LOOP_PINGPONG =>
        frameNumber = frameNumber % ((keyFramesArray.length * 2) - 2)
        if (frameNumber >= keyFramesArray.length)
          keyFramesArray.length - 2 - (frameNumber - keyFramesArray.length)
        else
          frameNumber
      case Animation.PlayMode.LOOP_RANDOM =>
        val lastFrameNumberTemp = (lastStateTime / frameDurationVar).toInt
        if (lastFrameNumberTemp != frameNumber) {
          MathUtils.random(keyFramesArray.length - 1)
        } else {
          this.lastFrameNumber
        }
      case Animation.PlayMode.REVERSED =>
        Math.max(keyFramesArray.length - frameNumber - 1, 0)
      case Animation.PlayMode.LOOP_REVERSED =>
        frameNumber = frameNumber % keyFramesArray.length
        keyFramesArray.length - frameNumber - 1
    }

    lastFrameNumber = frameNumber
    lastStateTime = stateTime

    frameNumber
  }

  /** Returns the keyframes[] array where all the frames of the animation are stored.
    * @return
    *   The keyframes[] field. This array is an Object[] if the animation was instantiated with an Array that was not type-aware.
    */
  def getKeyFrames(): Array[T] =
    keyFramesArray

  protected def setKeyFrames(keyFrames: T*): Unit = {
    this.keyFramesArray = keyFrames.toArray
    this.animationDuration = keyFrames.length * frameDurationVar
  }

  /** Returns the animation play mode. */
  def getPlayMode(): Animation.PlayMode =
    playMode

  /** Sets the animation play mode.
    *
    * @param playMode
    *   The animation {@link PlayMode} to use.
    */
  def setPlayMode(playMode: Animation.PlayMode): Unit =
    this.playMode = playMode

  /** Whether the animation would be finished if played without looping (PlayMode#NORMAL), given the state time.
    * @param stateTime
    * @return
    *   whether the animation is finished.
    */
  def isAnimationFinished(stateTime: Float): Boolean = {
    val frameNumber = (stateTime / frameDurationVar).toInt
    keyFramesArray.length - 1 < frameNumber
  }

  /** Sets duration a frame will be displayed.
    * @param frameDuration
    *   in seconds
    */
  def setFrameDuration(frameDuration: Float): Unit = {
    this.frameDurationVar = frameDuration
    this.animationDuration = keyFramesArray.length * frameDuration
  }

  /** @return the duration of a frame in seconds */
  def getFrameDuration(): Float =
    frameDurationVar

  /** @return the duration of the entire animation, number of frames times frame duration, in seconds */
  def getAnimationDuration(): Float =
    animationDuration
}

object Animation {

  /** Defines possible playback modes for an {@link Animation}. */
  enum PlayMode {
    case NORMAL, REVERSED, LOOP, LOOP_REVERSED, LOOP_PINGPONG, LOOP_RANDOM

    def isLooping: Boolean = this match {
      case NORMAL | REVERSED => false
      case _                 => true
    }

    def isReversed: Boolean = this match {
      case REVERSED | LOOP_REVERSED => true
      case _                        => false
    }
  }
}
