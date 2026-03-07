/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/utils/BaseAnimationController.java
 * Original authors: Xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   - Transform inner class: static in Java -> in companion object
 *   - Poolable -> Pool.Poolable
 *   - ObjectMap<Node,Transform> -> scala.collection.mutable.Map[Node,Transform]
 *   - Static fields (transforms, tmpT) -> companion object private vals
 *   - Static helper methods -> companion object private methods
 *   - applyAnimations: parameters Nullable[Animation] for null-safety
 *   - applyAnimation: parameters Nullable[mutable.Map] / Nullable[Pool] for null-safe dispatch
 *   - removeAnimation: iterates nodeAnimations via DynamicArray for-comprehension
 *   - getFirstKeyframeIndexAtTime: uses boundary/break instead of return
 *   - All methods fully ported; logic matches Java source
 *   - Minor: transforms is mutable.Map (not ObjectMap) -- equivalent behavior
 *   - TODO: Transform extends Pool.Poolable → define given Poolable[Transform]
 *   - Convention: opaque Seconds for apply(animation, time, weight) time param
 *   - Audit: pass (2026-03-03)
 */
package sge
package graphics
package g3d
package utils

import scala.collection.mutable
import scala.language.implicitConversions

import sge.graphics.g3d.model.{ Animation, Node, NodeAnimation, NodeKeyframe }
import sge.math.{ Matrix4, Quaternion, Vector3 }
import sge.utils.{ DynamicArray, Nullable, Pool, Seconds, SgeError }

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
    Pool.Default[BaseAnimationController.Transform](() => BaseAnimationController.Transform())

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
  protected def apply(animation: Animation, time: Seconds, weight: Float): Unit = {
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
  protected def applyAnimation(animation: Animation, time: Seconds): Unit = {
    if (applying) throw SgeError.InvalidInput("Call end() first")
    BaseAnimationController.applyAnimation(Nullable.empty, Nullable.empty, 1f, animation, time)
    target.calculateTransforms()
  }

  /** Apply two animations, blending the second onto to first using weight. */
  protected def applyAnimations(anim1: Nullable[Animation], time1: Seconds, anim2: Nullable[Animation], time2: Seconds, weight: Float): Unit =
    if (anim2.isEmpty || weight == 0f)
      anim1.foreach(a => applyAnimation(a, time1))
    else if (anim1.isEmpty || weight == 1f)
      anim2.foreach(a => applyAnimation(a, time2))
    else if (applying)
      throw SgeError.InvalidInput("Call end() first")
    else {
      begin()
      anim1.foreach(a => apply(a, time1, 1f))
      anim2.foreach(a => apply(a, time2, weight))
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
    val translation: Vector3    = Vector3()
    val rotation:    Quaternion = Quaternion()
    val scale:       Vector3    = Vector3(1, 1, 1)

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
  private val tmpT:       Transform                    = Transform()

  /** Find first key frame index just before a given time
    * @param arr
    *   Key frames ordered by time ascending
    * @param time
    *   Time to search
    * @return
    *   key frame index, 0 if time is out of key frames time range
    */
  def getFirstKeyframeIndexAtTime[T](arr: DynamicArray[NodeKeyframe[T]], time: Seconds): Int = scala.util.boundary {
    val lastIndex = arr.size - 1

    // edges cases : time out of range always return first index
    if (lastIndex <= 0 || time < Seconds(arr(0).keytime) || time > Seconds(arr(lastIndex).keytime)) {
      0
    } else {
      // binary search
      var minIndex = 0
      var maxIndex = lastIndex

      while (minIndex < maxIndex) {
        val i = (minIndex + maxIndex) / 2
        if (time > Seconds(arr(i + 1).keytime)) {
          minIndex = i + 1
        } else if (time < Seconds(arr(i).keytime)) {
          maxIndex = i - 1
        } else {
          scala.util.boundary.break(i)
        }
      }
      minIndex
    }
  }

  private def getTranslationAtTime(nodeAnim: NodeAnimation, time: Seconds, out: Vector3): Vector3 =
    nodeAnim.translation.fold(out.set(nodeAnim.node.translation)) { trans =>
      if (trans.size == 1) out.set(trans(0).value)
      else {
        var index         = getFirstKeyframeIndexAtTime(trans, time)
        val firstKeyframe = trans(index)
        out.set(firstKeyframe.value)
        index += 1
        if (index < trans.size) {
          val secondKeyframe = trans(index)
          val t              = (time - Seconds(firstKeyframe.keytime)) / (Seconds(secondKeyframe.keytime) - Seconds(firstKeyframe.keytime))
          out.lerp(secondKeyframe.value, t)
        }
        out
      }
    }

  private def getRotationAtTime(nodeAnim: NodeAnimation, time: Seconds, out: Quaternion): Quaternion =
    nodeAnim.rotation.fold(out.set(nodeAnim.node.rotation)) { rot =>
      if (rot.size == 1) out.set(rot(0).value)
      else {
        var index         = getFirstKeyframeIndexAtTime(rot, time)
        val firstKeyframe = rot(index)
        out.set(firstKeyframe.value)
        index += 1
        if (index < rot.size) {
          val secondKeyframe = rot(index)
          val t              = (time - Seconds(firstKeyframe.keytime)) / (Seconds(secondKeyframe.keytime) - Seconds(firstKeyframe.keytime))
          out.slerp(secondKeyframe.value, t)
        }
        out
      }
    }

  private def getScalingAtTime(nodeAnim: NodeAnimation, time: Seconds, out: Vector3): Vector3 =
    nodeAnim.scaling.fold(out.set(nodeAnim.node.scale)) { scl =>
      if (scl.size == 1) out.set(scl(0).value)
      else {
        var index         = getFirstKeyframeIndexAtTime(scl, time)
        val firstKeyframe = scl(index)
        out.set(firstKeyframe.value)
        index += 1
        if (index < scl.size) {
          val secondKeyframe = scl(index)
          val t              = (time - Seconds(firstKeyframe.keytime)) / (Seconds(secondKeyframe.keytime) - Seconds(firstKeyframe.keytime))
          out.lerp(secondKeyframe.value, t)
        }
        out
      }
    }

  private def getNodeAnimationTransform(nodeAnim: NodeAnimation, time: Seconds): Transform = {
    val transform = tmpT
    getTranslationAtTime(nodeAnim, time, transform.translation)
    getRotationAtTime(nodeAnim, time, transform.rotation)
    getScalingAtTime(nodeAnim, time, transform.scale)
    transform
  }

  private def applyNodeAnimationDirectly(nodeAnim: NodeAnimation, time: Seconds): Unit = {
    val node = nodeAnim.node
    node.isAnimated = true
    val transform = getNodeAnimationTransform(nodeAnim, time)
    transform.toMatrix4(node.localTransform)
  }

  private def applyNodeAnimationBlending(nodeAnim: NodeAnimation, out: mutable.Map[Node, Transform], pool: Pool[Transform], alpha: Float, time: Seconds): Unit = {

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
  protected def applyAnimation(out: Nullable[mutable.Map[Node, Transform]], pool: Nullable[Pool[Transform]], alpha: Float, animation: Animation, time: Seconds): Unit =

    if (out.isEmpty) {
      for (nodeAnim <- animation.nodeAnimations)
        applyNodeAnimationDirectly(nodeAnim, time)
    } else {
      val m = out.getOrElse(throw SgeError.InvalidInput("out must be defined"))
      val p = pool.getOrElse(throw SgeError.InvalidInput("pool must be defined"))
      for (node <- m.keys)
        node.isAnimated = false
      for (nodeAnim <- animation.nodeAnimations)
        applyNodeAnimationBlending(nodeAnim, m, p, alpha, time)
      for ((node, transform) <- m)
        if (!node.isAnimated) {
          node.isAnimated = true
          transform.lerp(node.translation, node.rotation, node.scale, alpha)
        }
    }
}
