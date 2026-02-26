/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/utils/BaseAnimationController.java
 * Original authors: Xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d
package utils

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.boundary
import scala.util.boundary.break

import sge.graphics.g3d.model.{ Animation, Node, NodeAnimation, NodeKeyframe }
import sge.math.{ Matrix4, Quaternion, Vector3 }
import sge.utils.{ Nullable, Pool, SgeError }

/** Base class for applying one or more {@link Animation}s to a {@link ModelInstance}. This class only applies the actual {@link Node} transformations, it does not manage animations or keep track of
  * animation states. See {@link AnimationController} for an implementation of this class which does manage animations.
  *
  * @author
  *   Xoppa
  */
class BaseAnimationController(
  /** The {@link ModelInstance} on which the animations are being performed. */
  val target: ModelInstance
) {

  private val transformPool: Pool[BaseAnimationController.Transform] =
    new Pool.Default[BaseAnimationController.Transform](() => new BaseAnimationController.Transform())

  private var applying: Boolean = false

  /** Begin applying multiple animations to the instance, must followed by one or more calls to { {@link #apply(Animation, float, float)} and finally {{@link #end()}.
    */
  protected def begin(): Unit = {
    if (applying) throw SgeError.InvalidInput("You must call end() after each call to begin()")
    applying = true
  }

  /** Apply an animation, must be called between {{@link #begin()} and {{@link #end()}.
    * @param weight
    *   The blend weight of this animation relative to the previous applied animations.
    */
  protected def apply(animation: Animation, time: Float, weight: Float): Unit = {
    if (!applying) throw SgeError.InvalidInput("You must call begin() before adding an animation")
    BaseAnimationController.applyAnimation(BaseAnimationController.transforms, transformPool, weight, animation, time)
  }

  /** End applying multiple animations to the instance and update it to reflect the changes. */
  protected def end(): Unit = {
    if (!applying) throw SgeError.InvalidInput("You must call begin() first")
    for ((node, transform) <- BaseAnimationController.transforms) {
      transform.toMatrix4(node.localTransform)
      transformPool.free(transform)
    }
    BaseAnimationController.transforms.clear()
    target.calculateTransforms()
    applying = false
  }

  /** Apply a single animation to the {@link ModelInstance} and update the it to reflect the changes. */
  protected def applyAnimation(animation: Animation, time: Float): Unit = {
    if (applying) throw SgeError.InvalidInput("Call end() first")
    BaseAnimationController.applyAnimation(null, null, 1f, animation, time)
    target.calculateTransforms()
  }

  /** Apply two animations, blending the second onto to first using weight. */
  protected def applyAnimations(anim1: Animation, time1: Float, anim2: Animation, time2: Float, weight: Float): Unit =
    if (anim2 == null || weight == 0f)
      applyAnimation(anim1, time1)
    else if (anim1 == null || weight == 1f)
      applyAnimation(anim2, time2)
    else if (applying)
      throw SgeError.InvalidInput("Call end() first")
    else {
      begin()
      apply(anim1, time1, 1f)
      apply(anim2, time2, weight)
      end()
    }

  /** Remove the specified animation, by marking the affected nodes as not animated. When switching animation, this should be call prior to applyAnimation(s).
    */
  protected def removeAnimation(animation: Animation): Unit =
    for (nodeAnim <- animation.nodeAnimations)
      nodeAnim.node.isAnimated = false
}

object BaseAnimationController {

  class Transform extends Pool.Poolable {
    val translation: Vector3    = new Vector3()
    val rotation:    Quaternion = new Quaternion()
    val scale:       Vector3    = new Vector3(1, 1, 1)

    def idt(): Transform = {
      translation.set(0, 0, 0)
      rotation.idt()
      scale.set(1, 1, 1)
      this
    }

    def set(t: Vector3, r: Quaternion, s: Vector3): Transform = {
      translation.set(t)
      rotation.set(r)
      scale.set(s)
      this
    }

    def set(other: Transform): Transform =
      set(other.translation, other.rotation, other.scale)

    def lerp(target: Transform, alpha: Float): Transform =
      lerp(target.translation, target.rotation, target.scale, alpha)

    def lerp(targetT: Vector3, targetR: Quaternion, targetS: Vector3, alpha: Float): Transform = {
      translation.lerp(targetT, alpha)
      rotation.slerp(targetR, alpha)
      scale.lerp(targetS, alpha)
      this
    }

    def toMatrix4(out: Matrix4): Matrix4 =
      out.set(translation, rotation, scale)

    override def reset(): Unit =
      idt()

    override def toString: String =
      translation.toString + " - " + rotation.toString + " - " + scale.toString
  }

  private val transforms: mutable.Map[Node, Transform] = mutable.Map.empty
  private val tmpT:       Transform                    = new Transform()

  /** Find first key frame index just before a given time
    * @param arr
    *   Key frames ordered by time ascending
    * @param time
    *   Time to search
    * @return
    *   key frame index, 0 if time is out of key frames time range
    */
  def getFirstKeyframeIndexAtTime[T](arr: ArrayBuffer[NodeKeyframe[T]], time: Float): Int = scala.util.boundary {
    val lastIndex = arr.size - 1

    // edges cases : time out of range always return first index
    if (lastIndex <= 0 || time < arr(0).keytime || time > arr(lastIndex).keytime) {
      0
    } else {
      // binary search
      var minIndex = 0
      var maxIndex = lastIndex

      while (minIndex < maxIndex) {
        val i = (minIndex + maxIndex) / 2
        if (time > arr(i + 1).keytime) {
          minIndex = i + 1
        } else if (time < arr(i).keytime) {
          maxIndex = i - 1
        } else {
          scala.util.boundary.break(i)
        }
      }
      minIndex
    }
  }

  private def getTranslationAtTime(nodeAnim: NodeAnimation, time: Float, out: Vector3): Vector3 =
    nodeAnim.translation.fold(out.set(nodeAnim.node.translation)) { trans =>
      if (trans.size == 1) out.set(trans(0).value)
      else {
        var index         = getFirstKeyframeIndexAtTime(trans, time)
        val firstKeyframe = trans(index)
        out.set(firstKeyframe.value)
        index += 1
        if (index < trans.size) {
          val secondKeyframe = trans(index)
          val t              = (time - firstKeyframe.keytime) / (secondKeyframe.keytime - firstKeyframe.keytime)
          out.lerp(secondKeyframe.value, t)
        }
        out
      }
    }

  private def getRotationAtTime(nodeAnim: NodeAnimation, time: Float, out: Quaternion): Quaternion =
    nodeAnim.rotation.fold(out.set(nodeAnim.node.rotation)) { rot =>
      if (rot.size == 1) out.set(rot(0).value)
      else {
        var index         = getFirstKeyframeIndexAtTime(rot, time)
        val firstKeyframe = rot(index)
        out.set(firstKeyframe.value)
        index += 1
        if (index < rot.size) {
          val secondKeyframe = rot(index)
          val t              = (time - firstKeyframe.keytime) / (secondKeyframe.keytime - firstKeyframe.keytime)
          out.slerp(secondKeyframe.value, t)
        }
        out
      }
    }

  private def getScalingAtTime(nodeAnim: NodeAnimation, time: Float, out: Vector3): Vector3 =
    nodeAnim.scaling.fold(out.set(nodeAnim.node.scale)) { scl =>
      if (scl.size == 1) out.set(scl(0).value)
      else {
        var index         = getFirstKeyframeIndexAtTime(scl, time)
        val firstKeyframe = scl(index)
        out.set(firstKeyframe.value)
        index += 1
        if (index < scl.size) {
          val secondKeyframe = scl(index)
          val t              = (time - firstKeyframe.keytime) / (secondKeyframe.keytime - firstKeyframe.keytime)
          out.lerp(secondKeyframe.value, t)
        }
        out
      }
    }

  private def getNodeAnimationTransform(nodeAnim: NodeAnimation, time: Float): Transform = {
    val transform = tmpT
    getTranslationAtTime(nodeAnim, time, transform.translation)
    getRotationAtTime(nodeAnim, time, transform.rotation)
    getScalingAtTime(nodeAnim, time, transform.scale)
    transform
  }

  private def applyNodeAnimationDirectly(nodeAnim: NodeAnimation, time: Float): Unit = {
    val node = nodeAnim.node
    node.isAnimated = true
    val transform = getNodeAnimationTransform(nodeAnim, time)
    transform.toMatrix4(node.localTransform)
  }

  private def applyNodeAnimationBlending(nodeAnim: NodeAnimation, out: mutable.Map[Node, Transform], pool: Pool[Transform], alpha: Float, time: Float): Unit = {

    val node = nodeAnim.node
    node.isAnimated = true
    val transform = getNodeAnimationTransform(nodeAnim, time)

    out.get(node) match {
      case Some(t) =>
        if (alpha > 0.999999f)
          t.set(transform)
        else
          t.lerp(transform, alpha)
      case None =>
        if (alpha > 0.999999f)
          out.put(node, pool.obtain().set(transform))
        else
          out.put(node, pool.obtain().set(node.translation, node.rotation, node.scale).lerp(transform, alpha))
    }
  }

  /** Helper method to apply one animation to either an objectmap for blending or directly to the bones. */
  protected def applyAnimation(out: mutable.Map[Node, Transform], pool: Pool[Transform], alpha: Float, animation: Animation, time: Float): Unit =

    if (out == null) {
      for (nodeAnim <- animation.nodeAnimations)
        applyNodeAnimationDirectly(nodeAnim, time)
    } else {
      for (node <- out.keys)
        node.isAnimated = false
      for (nodeAnim <- animation.nodeAnimations)
        applyNodeAnimationBlending(nodeAnim, out, pool, alpha, time)
      for ((node, transform) <- out)
        if (!node.isAnimated) {
          node.isAnimated = true
          transform.lerp(node.translation, node.rotation, node.scale, alpha)
        }
    }
}
