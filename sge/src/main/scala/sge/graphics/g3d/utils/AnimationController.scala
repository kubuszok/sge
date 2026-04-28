/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/utils/AnimationController.java
 * Original authors: Xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   - Inner classes AnimationListener (interface->trait) and AnimationDesc (static class->companion class)
 *   - AnimationDesc moved to companion object AnimationController
 *   - Pool anonymous subclass -> Pool.Default with lambda
 *   - All null fields -> Nullable (current, queued, previous, listener, animation)
 *   - GdxRuntimeException -> SgeError.InvalidInput
 *   - No return statements -> boundary/break in update() and AnimationDesc.update()
 *   - obtain() methods handle Nullable[Animation] / Nullable[String] via fold
 *   - All 6 setAnimation, 7 animate, 3 queue, 4 action overloads ported
 *   - Convention: opaque Seconds for update(delta), AnimationDesc time/offset/duration
 *   - Audit: pass (2026-03-03)
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 405
 * Covenant-baseline-methods: AnimationController,AnimationDesc,AnimationListener,action,allowSameAnimation,animate,animation,animationPool,current,duration,inAction,justChangedAnimation,listener,loopCount,obtain,obtainByName,obtainFrom,offset,onEnd,onLoop,paused,previous,queue,queued,queuedTransitionTime,setAnimation,speed,time,transitionCurrentTime,transitionTargetTime,update
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/utils/AnimationController.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: fb7187756da5e2026426d9b1770412ad4b94d8eb
 */
package sge
package graphics
package g3d
package utils

import scala.language.implicitConversions
import scala.util.boundary
import scala.util.boundary.break

import sge.graphics.g3d.model.Animation
import sge.math.MathUtils
import sge.utils.{ Nullable, Pool, Seconds, SgeError }

/** Class to control one or more {@link Animation}s on a {@link ModelInstance}. Use the {@link #setAnimation(String, int, float, AnimationListener)} method to change the current animation. Use the
  * {@link #animate(String, int, float, AnimationListener, float)} method to start an animation, optionally blending onto the current animation. Use the
  * {@link #queue(String, int, float, AnimationListener, float)} method to queue an animation to be played when the current animation is finished. Use the
  * {@link #action(String, int, float, AnimationListener, float)} method to play a (short) animation on top of the current animation.
  *
  * You can use multiple AnimationControllers on the same ModelInstance, as long as they don't interfere with each other (don't affect the same {@link Node}s).
  *
  * @author
  *   Xoppa
  */
class AnimationController(target: ModelInstance) extends BaseAnimationController(target) {
  import AnimationController._

  protected val animationPool: Pool[AnimationDesc] =
    Pool.Default[AnimationDesc](() => AnimationDesc())

  /** The animation currently playing. Do not alter this value. */
  var current: Nullable[AnimationDesc] = Nullable.empty

  /** The animation queued to be played when the {@link #current} animation is completed. Do not alter this value. */
  var queued: Nullable[AnimationDesc] = Nullable.empty

  /** The transition time which should be applied to the queued animation. Do not alter this value. */
  var queuedTransitionTime: Seconds = Seconds.zero

  /** The animation which previously played. Do not alter this value. */
  var previous: Nullable[AnimationDesc] = Nullable.empty

  /** The current transition time. Do not alter this value. */
  var transitionCurrentTime: Seconds = Seconds.zero

  /** The target transition time. Do not alter this value. */
  var transitionTargetTime: Seconds = Seconds.zero

  /** Whether an action is being performed. Do not alter this value. */
  var inAction: Boolean = false

  /** When true a call to {@link #update(float)} will not be processed. */
  var paused: Boolean = false

  /** Whether to allow the same animation to be played while playing that animation. */
  var allowSameAnimation: Boolean = false

  private var justChangedAnimation: Boolean = false

  private def obtain(anim: Nullable[Animation], offset: Seconds, duration: Seconds, loopCount: Int, speed: Float, listener: Nullable[AnimationListener]): Nullable[AnimationDesc] =
    anim.map { a =>
      val result = animationPool.obtain()
      result.animation = Nullable(a)
      result.listener = listener
      result.loopCount = loopCount
      result.speed = speed
      result.offset = offset
      result.duration = if (duration < Seconds.zero) Seconds(a.duration) - offset else duration
      result.time = if (speed < 0) result.duration else Seconds.zero
      result
    }

  private def obtainByName(id: Nullable[String], offset: Seconds, duration: Seconds, loopCount: Int, speed: Float, listener: Nullable[AnimationListener]): Nullable[AnimationDesc] =
    id.flatMap { name =>
      val anim = target.getAnimation(name)
      if (anim.isEmpty) throw SgeError.InvalidInput("Unknown animation: " + name)
      anim.flatMap { a =>
        obtain(Nullable(a), offset, duration, loopCount, speed, listener)
      }
    }

  private def obtainFrom(anim: AnimationDesc): Nullable[AnimationDesc] =
    obtain(anim.animation, anim.offset, anim.duration, anim.loopCount, anim.speed, anim.listener)

  /** Update any animations currently being played.
    * @param delta
    *   The time elapsed since last update, change this to alter the overall speed (can be negative).
    */
  def update(delta: Seconds): Unit = boundary {
    if (paused) break(())
    previous.foreach { prev =>
      transitionCurrentTime += delta
      if (transitionCurrentTime >= transitionTargetTime) {
        prev.animation.foreach(removeAnimation(_))
        justChangedAnimation = true
        animationPool.free(prev)
        previous = Nullable.empty
      }
    }
    if (justChangedAnimation) {
      target.calculateTransforms()
      justChangedAnimation = false
    }
    current.fold(break(())) { cur =>
      if (cur.loopCount == 0 || cur.animation.isEmpty) break(())
      val remain = cur.update(delta)
      if (remain >= Seconds.zero) {
        queued.foreach { q =>
          inAction = false
          animate(Nullable(q), queuedTransitionTime)
          queued = Nullable.empty
          if (remain > Seconds.zero) { update(remain); break(()) }
          break(())
        }
      }
      previous.fold {
        cur.animation.foreach(anim => applyAnimation(anim, cur.offset + cur.time))
      } { prev =>
        applyAnimations(prev.animation, prev.offset + prev.time, cur.animation, cur.offset + cur.time, transitionCurrentTime / transitionTargetTime)
      }
    }
  }

  /** Set the active animation, replacing any current animation.
    * @param id
    *   The ID of the {@link Animation} within the {@link ModelInstance}.
    * @return
    *   The {@link AnimationDesc} which can be read to get the progress of the animation. Will be invalid when the animation is completed.
    */
  def setAnimation(id: String): Nullable[AnimationDesc] =
    setAnimation(id, 1, 1.0f, Nullable.empty)

  /** Set the active animation, replacing any current animation. */
  def setAnimation(id: String, loopCount: Int): Nullable[AnimationDesc] =
    setAnimation(id, loopCount, 1.0f, Nullable.empty)

  /** Set the active animation, replacing any current animation. */
  def setAnimation(id: String, listener: Nullable[AnimationListener]): Nullable[AnimationDesc] =
    setAnimation(id, 1, 1.0f, listener)

  /** Set the active animation, replacing any current animation. */
  def setAnimation(id: String, loopCount: Int, listener: Nullable[AnimationListener]): Nullable[AnimationDesc] =
    setAnimation(id, loopCount, 1.0f, listener)

  /** Set the active animation, replacing any current animation. */
  def setAnimation(id: String, loopCount: Int, speed: Float, listener: Nullable[AnimationListener]): Nullable[AnimationDesc] =
    setAnimation(id, Seconds.zero, Seconds(-1f), loopCount, speed, listener)

  /** Set the active animation, replacing any current animation. */
  def setAnimation(id: String, offset: Seconds, duration: Seconds, loopCount: Int, speed: Float, listener: Nullable[AnimationListener]): Nullable[AnimationDesc] =
    setAnimation(obtainByName(id, offset, duration, loopCount, speed, listener))

  /** Set the active animation, replacing any current animation. */
  protected def setAnimation(anim: Animation, offset: Seconds, duration: Seconds, loopCount: Int, speed: Float, listener: Nullable[AnimationListener]): Nullable[AnimationDesc] =
    setAnimation(obtain(anim, offset, duration, loopCount, speed, listener))

  /** Set the active animation, replacing any current animation. */
  protected def setAnimation(anim: Nullable[AnimationDesc]): Nullable[AnimationDesc] = {
    current.fold {
      current = anim
    } { cur =>
      anim.fold {
        cur.animation.foreach(removeAnimation(_))
        animationPool.free(cur)
        current = Nullable.empty
      } { a =>
        if (!allowSameAnimation && cur.animation == a.animation)
          a.time = cur.time
        else
          cur.animation.foreach(removeAnimation(_))
        animationPool.free(cur)
        current = Nullable(a)
      }
    }
    justChangedAnimation = true
    anim
  }

  /** Changes the current animation by blending the new on top of the old during the transition time. */
  def animate(id: String, transitionTime: Seconds): Nullable[AnimationDesc] =
    animate(id, 1, 1.0f, Nullable.empty, transitionTime)

  /** Changes the current animation by blending the new on top of the old during the transition time. */
  def animate(id: String, listener: Nullable[AnimationListener], transitionTime: Seconds): Nullable[AnimationDesc] =
    animate(id, 1, 1.0f, listener, transitionTime)

  /** Changes the current animation by blending the new on top of the old during the transition time. */
  def animate(id: String, loopCount: Int, listener: Nullable[AnimationListener], transitionTime: Seconds): Nullable[AnimationDesc] =
    animate(id, loopCount, 1.0f, listener, transitionTime)

  /** Changes the current animation by blending the new on top of the old during the transition time. */
  def animate(id: String, loopCount: Int, speed: Float, listener: Nullable[AnimationListener], transitionTime: Seconds): Nullable[AnimationDesc] =
    animate(id, Seconds.zero, Seconds(-1f), loopCount, speed, listener, transitionTime)

  /** Changes the current animation by blending the new on top of the old during the transition time. */
  def animate(id: String, offset: Seconds, duration: Seconds, loopCount: Int, speed: Float, listener: Nullable[AnimationListener], transitionTime: Seconds): Nullable[AnimationDesc] =
    animate(obtainByName(id, offset, duration, loopCount, speed, listener), transitionTime)

  /** Changes the current animation by blending the new on top of the old during the transition time. */
  protected def animate(anim: Animation, offset: Seconds, duration: Seconds, loopCount: Int, speed: Float, listener: Nullable[AnimationListener], transitionTime: Seconds): Nullable[AnimationDesc] =
    animate(obtain(anim, offset, duration, loopCount, speed, listener), transitionTime)

  /** Changes the current animation by blending the new on top of the old during the transition time. */
  protected def animate(anim: Nullable[AnimationDesc], transitionTime: Seconds): Nullable[AnimationDesc] = {
    current.fold {
      current = anim
    } { cur =>
      if (cur.loopCount == 0) {
        current = anim
      } else if (inAction) {
        queue(anim, transitionTime)
      } else {
        anim.fold {} { a =>
          if (!allowSameAnimation && cur.animation == a.animation) {
            a.time = cur.time
            animationPool.free(cur)
            current = Nullable(a)
          } else {
            previous.foreach { prev =>
              prev.animation.foreach(removeAnimation(_))
              animationPool.free(prev)
            }
            previous = current
            current = Nullable(a)
            transitionCurrentTime = Seconds.zero
            transitionTargetTime = transitionTime
          }
        }
      }
    }
    anim
  }

  /** Queue an animation to be applied when the {@link #current} animation is finished. If the current animation is continuously looping it will be synchronized on next loop.
    */
  def queue(id: String, loopCount: Int, speed: Float, listener: Nullable[AnimationListener], transitionTime: Seconds): Nullable[AnimationDesc] =
    queue(id, Seconds.zero, Seconds(-1f), loopCount, speed, listener, transitionTime)

  /** Queue an animation to be applied when the {@link #current} animation is finished. If the current animation is continuously looping it will be synchronized on next loop.
    */
  def queue(id: String, offset: Seconds, duration: Seconds, loopCount: Int, speed: Float, listener: Nullable[AnimationListener], transitionTime: Seconds): Nullable[AnimationDesc] =
    queue(obtainByName(id, offset, duration, loopCount, speed, listener), transitionTime)

  /** Queue an animation to be applied when the current is finished. If current is continuous it will be synced on next loop. */
  protected def queue(anim: Animation, offset: Seconds, duration: Seconds, loopCount: Int, speed: Float, listener: Nullable[AnimationListener], transitionTime: Seconds): Nullable[AnimationDesc] =
    queue(obtain(anim, offset, duration, loopCount, speed, listener), transitionTime)

  /** Queue an animation to be applied when the current is finished. If current is continuous it will be synced on next loop. */
  protected def queue(anim: Nullable[AnimationDesc], transitionTime: Seconds): Nullable[AnimationDesc] =
    current.fold {
      animate(anim, transitionTime)
    } { cur =>
      if (cur.loopCount == 0) {
        animate(anim, transitionTime)
      } else {
        queued.foreach(animationPool.free)
        queued = anim
        queuedTransitionTime = transitionTime
        if (cur.loopCount < 0) cur.loopCount = 1
        anim
      }
    }

  /** Apply an action animation on top of the current animation. */
  def action(id: String, loopCount: Int, speed: Float, listener: Nullable[AnimationListener], transitionTime: Seconds): Nullable[AnimationDesc] =
    action(id, Seconds.zero, Seconds(-1f), loopCount, speed, listener, transitionTime)

  /** Apply an action animation on top of the current animation. */
  def action(id: String, offset: Seconds, duration: Seconds, loopCount: Int, speed: Float, listener: Nullable[AnimationListener], transitionTime: Seconds): Nullable[AnimationDesc] =
    action(obtainByName(id, offset, duration, loopCount, speed, listener), transitionTime)

  /** Apply an action animation on top of the current animation. */
  protected def action(anim: Animation, offset: Seconds, duration: Seconds, loopCount: Int, speed: Float, listener: Nullable[AnimationListener], transitionTime: Seconds): Nullable[AnimationDesc] =
    action(obtain(anim, offset, duration, loopCount, speed, listener), transitionTime)

  /** Apply an action animation on top of the current animation. */
  protected def action(anim: Nullable[AnimationDesc], transitionTime: Seconds): Nullable[AnimationDesc] = {
    anim.foreach { a =>
      if (a.loopCount < 0) throw SgeError.InvalidInput("An action cannot be continuous")
    }
    current.fold {
      animate(anim, transitionTime)
    } { cur =>
      if (cur.loopCount == 0) {
        animate(anim, transitionTime)
      } else {
        val toQueue = if (inAction) Nullable.empty[AnimationDesc] else obtainFrom(cur)
        inAction = false
        animate(anim, transitionTime)
        inAction = true
        toQueue.foreach(q => queue(Nullable(q), transitionTime))
        anim
      }
    }
  }
}

object AnimationController {

  /** Listener that will be informed when an animation is looped or completed.
    * @author
    *   Xoppa
    */
  trait AnimationListener {

    /** Gets called when an animation is completed.
      * @param animation
      *   The animation which just completed.
      */
    def onEnd(animation: AnimationDesc): Unit

    /** Gets called when an animation is looped. The {@link AnimationDesc#loopCount} is updated prior to this call and can be read or written to alter the number of remaining loops.
      * @param animation
      *   The animation which just looped.
      */
    def onLoop(animation: AnimationDesc): Unit
  }

  /** Class describing how to play and {@link Animation}. You can read the values within this class to get the progress of the animation. Do not change the values. Only valid when the animation is
    * currently played.
    * @author
    *   Xoppa
    */
  class AnimationDesc {

    /** Listener which will be informed when the animation is looped or ended. */
    var listener: Nullable[AnimationListener] = Nullable.empty

    /** The animation to be applied. */
    var animation: Nullable[Animation] = Nullable.empty

    /** The speed at which to play the animation (can be negative), 1.0 for normal speed. */
    var speed: Float = 0f

    /** The current animation time. */
    var time: Seconds = Seconds.zero

    /** The offset within the animation (animation time = offsetTime + time) */
    var offset: Seconds = Seconds.zero

    /** The duration of the animation */
    var duration: Seconds = Seconds.zero

    /** The number of remaining loops, negative for continuous, zero if stopped. */
    var loopCount: Int = 0

    /** @param delta
      *   delta time, must be positive.
      * @return
      *   the remaining time or -1 if still animating.
      */
    protected[utils] def update(delta: Seconds): Seconds = boundary {
      if (loopCount != 0 && animation.isDefined) {
        var loops: Int = 0
        val diff = delta * speed
        if (!MathUtils.isZero(duration.toFloat)) {
          time = time + diff
          if (speed < 0) {
            var invTime = duration - time
            loops = Math.abs(invTime / duration).toInt
            invTime = (invTime % duration).abs
            time = duration - invTime
          } else {
            loops = Math.abs(time / duration).toInt
            time = (time % duration).abs
          }
        } else {
          loops = 1
        }
        for (i <- 0 until loops) {
          if (loopCount > 0) loopCount -= 1
          if (loopCount != 0) listener.foreach(_.onLoop(this))
          if (loopCount == 0) {
            val result = duration * ((loops - 1) - i).toFloat + (if (diff < Seconds.zero) duration - time else time)
            time = if (diff < Seconds.zero) Seconds.zero else duration
            listener.foreach(_.onEnd(this))
            break(result)
          }
        }
        break(Seconds(-1f))
      } else {
        break(delta)
      }
    }
  }
}
