/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g2d/Animation.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: Array<T> -> DynamicArray[? <: T]; generic <T> -> [T: ClassTag]
 *   Convention: Java enum -> Scala 3 enum; return eliminated; match instead of switch
 *   Idiom: boundary/break, Nullable, split packages
 *   Fixes: Java-style getters/setters (getPlayMode/setPlayMode → var playMode, getFrameDuration/setFrameDuration → property, getAnimationDuration → def, getKeyFrames → def)
 *   Audited: 2026-03-04
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 208
 * Covenant-baseline-methods: Animation,PlayMode,_animationDuration,_frameDuration,_keyFrames,animationDuration,frame,frameDuration,frameDuration_,frameNumber,frames,getKeyFrame,getKeyFrameIndex,isAnimationFinished,isLooping,isReversed,keyFrames,lastFrameNumber,lastStateTime,oldPlayMode,playMode,setKeyFrames,this
 * Covenant-source-reference: com/badlogic/gdx/graphics/g2d/Animation.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 34cc595deb4ac09ee476c6b1aba1b805f4dc81a7
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
class Animation[T: ClassTag](initialFrameDuration: Float, initialKeyFrames: DynamicArray[? <: T]) {

  /** Length must not be modified without updating {@link #animationDuration}. See {@link #setKeyFrames(T[])}. */
  private var _keyFrames:         Array[T] = uninitialized
  private var _frameDuration:     Float    = initialFrameDuration
  private var _animationDuration: Float    = uninitialized
  private var lastFrameNumber:    Int      = 0
  private var lastStateTime:      Float    = 0f

  var playMode: Animation.PlayMode = Animation.PlayMode.NORMAL

  // Initialize keyframes from the DynamicArray
  val frames = initialKeyFrames.toArray.asInstanceOf[Array[T]]
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
    this.playMode = playMode
  }

  /** Constructor, storing the frame duration and key frames.
    *
    * @param frameDuration
    *   the time between frames in seconds.
    * @param keyFrames
    *   the objects representing the frames.
    */
  def this(initialFrameDuration: Float, keyFrames: T*) =
    this(initialFrameDuration, DynamicArray.wrapRefUnchecked(keyFrames.toArray))

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
    _keyFrames(frameNumber)
  }

  /** Returns the current frame number.
    * @param stateTime
    * @return
    *   current frame number
    */
  def getKeyFrameIndex(stateTime: Float): Int =
    if (_keyFrames.length == 1) 0
    else {
      var frameNumber = (stateTime / _frameDuration).toInt
      frameNumber = playMode match {
        case Animation.PlayMode.NORMAL =>
          Math.min(_keyFrames.length - 1, frameNumber)
        case Animation.PlayMode.LOOP =>
          frameNumber % _keyFrames.length
        case Animation.PlayMode.LOOP_PINGPONG =>
          frameNumber = frameNumber % ((_keyFrames.length * 2) - 2)
          if (frameNumber >= _keyFrames.length)
            _keyFrames.length - 2 - (frameNumber - _keyFrames.length)
          else
            frameNumber
        case Animation.PlayMode.LOOP_RANDOM =>
          val lastFrameNumberTemp = (lastStateTime / _frameDuration).toInt
          if (lastFrameNumberTemp != frameNumber) {
            MathUtils.random(_keyFrames.length - 1)
          } else {
            this.lastFrameNumber
          }
        case Animation.PlayMode.REVERSED =>
          Math.max(_keyFrames.length - frameNumber - 1, 0)
        case Animation.PlayMode.LOOP_REVERSED =>
          frameNumber = frameNumber % _keyFrames.length
          _keyFrames.length - frameNumber - 1
      }

      lastFrameNumber = frameNumber
      lastStateTime = stateTime

      frameNumber
    }

  /** Returns the keyframes[] array where all the frames of the animation are stored.
    * @return
    *   The keyframes[] field. This array is an Object[] if the animation was instantiated with an Array that was not type-aware.
    */
  def keyFrames: Array[T] =
    _keyFrames

  protected def setKeyFrames(keyFrames: T*): Unit = {
    this._keyFrames = keyFrames.toArray
    this._animationDuration = keyFrames.length * _frameDuration
  }

  /** Whether the animation would be finished if played without looping (PlayMode#NORMAL), given the state time.
    * @param stateTime
    * @return
    *   whether the animation is finished.
    */
  def isAnimationFinished(stateTime: Float): Boolean = {
    val frameNumber = (stateTime / _frameDuration).toInt
    _keyFrames.length - 1 < frameNumber
  }

  /** @return the duration of a frame in seconds */
  def frameDuration: Float =
    _frameDuration

  /** Sets duration a frame will be displayed.
    * @param frameDuration
    *   in seconds
    */
  def frameDuration_=(frameDuration: Float): Unit = {
    this._frameDuration = frameDuration
    this._animationDuration = _keyFrames.length * frameDuration
  }

  /** @return the duration of the entire animation, number of frames times frame duration, in seconds */
  def animationDuration: Float =
    _animationDuration
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
