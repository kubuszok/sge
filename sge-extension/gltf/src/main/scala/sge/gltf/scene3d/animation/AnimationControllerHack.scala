/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/animation/AnimationControllerHack.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 *
 * AnimationController hack to run morph targets animations
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 433
 * Covenant-baseline-methods: AnimationControllerHack,Transform,apply,applyAnimation,applyAnimationPlus,applyAnimations,applyNodeAnimationBlending,applyNodeAnimationDirectly,applying,begin,calculateTransforms,cubicQ,cubicV3,cubicW,d,end,existingOpt,getFirstKeyframeIndexAtTime,getMorphTargetAtTime,getNodeAnimationTransform,getRotationAtTime,getScalingAtTime,getTranslationAtTime,i,idt,index,initialCapacity,interpolation,lerp,max,n,newObject,node,q1,q2,q3,q4,reset,rot,rotation,scale,scl,set,setAnimation,setAnimationDesc,t2,t3,tmpT,toMatrix4,toString,trans,transform,transformPool,transforms,translation,weights
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package gltf
package scene3d
package animation

import scala.util.boundary
import scala.util.boundary.break

import sge.gltf.scene3d.model.{ CubicQuaternion, CubicVector3, CubicWeightVector, NodePartPlus, NodePlus, WeightVector }
import sge.graphics.g3d.ModelInstance
import sge.graphics.g3d.model.{ Animation, Node, NodeAnimation, NodeKeyframe, NodePart }
import sge.graphics.g3d.utils.AnimationController
import sge.utils.{ Nullable, Seconds }
import sge.math.{ Matrix4, Quaternion, Vector3 }
import sge.utils.{ DynamicArray, ObjectMap, Pool, Seconds, SgeError }

/** AnimationController hack to run morph targets animations */
class AnimationControllerHack(target: ModelInstance) extends AnimationController(target) {

  import AnimationControllerHack.*

  private val transformPool: Pool[Transform] = new Pool[Transform] {
    override protected val initialCapacity: Int       = 16
    override protected val max:             Int       = Int.MaxValue
    override protected def newObject():     Transform = Transform()
  }

  private var applying:    Boolean = false
  var calculateTransforms: Boolean = true

  override protected def begin(): Unit = {
    if (applying) throw SgeError.InvalidInput("You must call end() after each call to begin()")
    applying = true
  }

  override protected def apply(animation: Animation, time: Seconds, weight: Float): Unit = {
    if (!applying) throw SgeError.InvalidInput("You must call begin() before adding an animation")
    applyAnimationPlus(transforms, transformPool, weight, animation, time.toFloat)
  }

  override protected def end(): Unit = {
    if (!applying) throw SgeError.InvalidInput("You must call begin() first")
    transforms.foreachEntry { (key, value) =>
      value.toMatrix4(key.localTransform)
      transformPool.free(value)
    }
    transforms.clear()
    if (calculateTransforms) target.calculateTransforms()
    applying = false
  }

  override protected def applyAnimation(animation: Animation, time: Seconds): Unit = {
    if (applying) throw SgeError.InvalidInput("Call end() first")
    applyAnimationPlus(null, null, 1f, animation, time.toFloat) // @nowarn — null means direct apply (no blending)
    if (calculateTransforms) target.calculateTransforms()
  }

  override protected def applyAnimations(anim1: Nullable[Animation], time1: Seconds, anim2: Nullable[Animation], time2: Seconds, weight: Float): Unit =
    if (anim2.isEmpty || weight == 0f) {
      anim1.foreach(a => applyAnimation(a, time1))
    } else if (anim1.isEmpty || weight == 1f) {
      anim2.foreach(a => applyAnimation(a, time2))
    } else if (applying) {
      throw SgeError.InvalidInput("Call end() first")
    } else {
      begin()
      anim1.foreach(a => apply(a, time1, 1f))
      anim2.foreach(a => apply(a, time2, weight))
      end()
    }

  def setAnimationDesc(anim: AnimationController.AnimationDesc): Unit =
    anim.animation.foreach { a =>
      setAnimation(a, anim.offset, anim.duration, anim.loopCount, anim.speed, anim.listener)
    }

  def setAnimation(animation: Animation): Unit =
    setAnimation(animation, 1)

  /** @param animation
    *   animation to play
    * @param loopCount
    *   loop count : 0 paused, -1 infinite, n for n loops
    */
  def setAnimation(animation: Animation, loopCount: Int): Unit =
    setAnimation(animation, Seconds.zero, Seconds(animation.duration), loopCount, 1f, sge.utils.Nullable.empty) // duration is raw Float in Animation
}

object AnimationControllerHack {

  class Transform extends Pool.Poolable {
    val translation: Vector3      = Vector3()
    val rotation:    Quaternion   = Quaternion()
    val scale:       Vector3      = Vector3(1f, 1f, 1f)
    val weights:     WeightVector = WeightVector()

    def idt(): Transform = {
      translation.set(0f, 0f, 0f)
      rotation.idt()
      scale.set(1f, 1f, 1f)
      weights.set()
      this
    }

    def set(t: Vector3, r: Quaternion, s: Vector3, w: WeightVector): Transform = {
      translation.set(t)
      rotation.set(r)
      scale.set(s)
      if (w != null) weights.set(w) else weights.set() // @nowarn — w may be null
      this
    }

    def set(other: Transform): Transform =
      set(other.translation, other.rotation, other.scale, other.weights)

    def lerp(target: Transform, alpha: Float): Transform =
      lerp(target.translation, target.rotation, target.scale, target.weights, alpha)

    def lerp(targetT: Vector3, targetR: Quaternion, targetS: Vector3, targetW: WeightVector, alpha: Float): Transform = {
      translation.lerp(targetT, alpha)
      rotation.slerp(targetR, alpha)
      scale.lerp(targetS, alpha)
      if (targetW != null) weights.lerp(targetW, alpha) // @nowarn — w may be null
      this
    }

    def toMatrix4(out: Matrix4): Matrix4 =
      out.set(translation, rotation, scale)

    override def reset(): Unit = { idt(); () }

    override def toString: String =
      s"${translation} - ${rotation} - ${scale} - ${weights}"
  }

  private val transforms: ObjectMap[Node, Transform] = ObjectMap[Node, Transform]()
  private val tmpT:       Transform                  = Transform()

  private def getFirstKeyframeIndexAtTime[T](arr: DynamicArray[NodeKeyframe[T]], time: Float): Int = boundary {
    val n = arr.size - 1
    var i = 0
    while (i < n) {
      if (time >= arr(i).keytime && time <= arr(i + 1).keytime) {
        break(i)
      }
      i += 1
    }
    n
  }

  private def getTranslationAtTime(nodeAnim: NodeAnimation, time: Float, out: Vector3): Vector3 = boundary {
    if (nodeAnim.translation.isEmpty) break(out.set(nodeAnim.node.translation))
    val trans = nodeAnim.translation.get
    if (trans.size == 1) break(out.set(trans(0).value))

    val index = getFirstKeyframeIndexAtTime(trans, time)

    val interpolation: Interpolation = nodeAnim match {
      case nah: NodeAnimationHack => nah.translationMode.getOrElse(null).asInstanceOf[Interpolation] // @nowarn
      case _ => null.asInstanceOf[Interpolation] // @nowarn
    }

    if (interpolation == Interpolation.STEP) {
      val firstKeyframe = trans(index)
      out.set(firstKeyframe.value)
    } else if (interpolation == Interpolation.LINEAR) {
      val firstKeyframe = trans(index)
      out.set(firstKeyframe.value)
      val nextIndex = index + 1
      if (nextIndex < trans.size) {
        val secondKeyframe = trans(nextIndex)
        val t              = (time - firstKeyframe.keytime) / (secondKeyframe.keytime - firstKeyframe.keytime)
        out.lerp(secondKeyframe.value, t)
      } else {
        out.set(firstKeyframe.value)
      }
    } else if (interpolation == Interpolation.CUBICSPLINE) {
      val firstKeyframe = trans(index)
      val nextIndex     = index + 1
      if (nextIndex < trans.size) {
        val secondKeyframe = trans(nextIndex)
        val t              = (time - firstKeyframe.keytime) / (secondKeyframe.keytime - firstKeyframe.keytime)
        val firstCV        = firstKeyframe.value.asInstanceOf[AnyRef].asInstanceOf[CubicVector3]
        val secondCV       = secondKeyframe.value.asInstanceOf[AnyRef].asInstanceOf[CubicVector3]
        cubicV3(out, t, firstCV.value, firstCV.tangentOut, secondCV.value, secondCV.tangentIn)
      } else {
        out.set(firstKeyframe.value)
      }
    }
    out
  }

  /** https://github.com/KhronosGroup/glTF/blob/master/specification/2.0/README.md#appendix-c-spline-interpolation */
  private def cubicV3(out: Vector3, t: Float, p0: Vector3, m0: Vector3, p1: Vector3, m1: Vector3): Unit = {
    // p(t) = (2t3 - 3t2 + 1)p0 + (t3 - 2t2 + t)m0 + (-2t3 + 3t2)p1 + (t3 - t2)m1
    val t2 = t * t
    val t3 = t2 * t
    out.set(p0).scl(2 * t3 - 3 * t2 + 1).mulAdd(m0, t3 - 2 * t2 + t).mulAdd(p1, -2 * t3 + 3 * t2).mulAdd(m1, t3 - t2)
  }

  private val q1: Quaternion = Quaternion()
  private val q2: Quaternion = Quaternion()
  private val q3: Quaternion = Quaternion()
  private val q4: Quaternion = Quaternion()

  private def cubicQ(out: Quaternion, t: Float, delta: Float, p0: Quaternion, m0: Quaternion, p1: Quaternion, m1: Quaternion): Unit = {
    val d  = -delta
    val t2 = t * t
    val t3 = t2 * t
    q1.set(p0).mul(2 * t3 - 3 * t2 + 1)
    q2.set(m0).mul(d).mul(t3 - 2 * t2 + t)
    q3.set(p1).mul(-2 * t3 + 3 * t2)
    q4.set(m1).mul(d).mul(t3 - t2)
    out.set(q1).add(q2).add(q3).add(q4).nor()
  }

  private def cubicW(out: WeightVector, t: Float, p0: WeightVector, m0: WeightVector, p1: WeightVector, m1: WeightVector): Unit = {
    val t2 = t * t
    val t3 = t2 * t
    out.set(p0).scl(2 * t3 - 3 * t2 + 1).mulAdd(m0, t3 - 2 * t2 + t).mulAdd(p1, -2 * t3 + 3 * t2).mulAdd(m1, t3 - t2)
  }

  private def getRotationAtTime(nodeAnim: NodeAnimation, time: Float, out: Quaternion): Quaternion = boundary {
    if (nodeAnim.rotation.isEmpty) break(out.set(nodeAnim.node.rotation))
    val rot = nodeAnim.rotation.get
    if (rot.size == 1) break(out.set(rot(0).value))

    val index = getFirstKeyframeIndexAtTime(rot, time)

    val interpolation: Interpolation = nodeAnim match {
      case nah: NodeAnimationHack => nah.rotationMode.getOrElse(null).asInstanceOf[Interpolation] // @nowarn
      case _ => null.asInstanceOf[Interpolation] // @nowarn
    }

    if (interpolation == Interpolation.STEP) {
      out.set(rot(index).value)
    } else if (interpolation == Interpolation.LINEAR) {
      val firstKeyframe = rot(index)
      out.set(firstKeyframe.value)
      val nextIndex = index + 1
      if (nextIndex < rot.size) {
        val secondKeyframe = rot(nextIndex)
        val t              = (time - firstKeyframe.keytime) / (secondKeyframe.keytime - firstKeyframe.keytime)
        out.slerp(secondKeyframe.value, t)
      } else {
        out.set(firstKeyframe.value)
      }
    } else if (interpolation == Interpolation.CUBICSPLINE) {
      val firstKeyframe = rot(index)
      val nextIndex     = index + 1
      if (nextIndex < rot.size) {
        val secondKeyframe = rot(nextIndex)
        val t              = (time - firstKeyframe.keytime) / (secondKeyframe.keytime - firstKeyframe.keytime)
        val firstCV        = firstKeyframe.value.asInstanceOf[AnyRef].asInstanceOf[CubicQuaternion]
        val secondCV       = secondKeyframe.value.asInstanceOf[AnyRef].asInstanceOf[CubicQuaternion]
        cubicQ(out, t, secondKeyframe.keytime - firstKeyframe.keytime, firstCV.value, firstCV.tangentOut, secondCV.value, secondCV.tangentIn)
      } else {
        out.set(firstKeyframe.value)
      }
    }
    out
  }

  private def getScalingAtTime(nodeAnim: NodeAnimation, time: Float, out: Vector3): Vector3 = boundary {
    if (nodeAnim.scaling.isEmpty) break(out.set(nodeAnim.node.scale))
    val scl = nodeAnim.scaling.get
    if (scl.size == 1) break(out.set(scl(0).value))

    val index = getFirstKeyframeIndexAtTime(scl, time)

    val interpolation: Interpolation = nodeAnim match {
      case nah: NodeAnimationHack => nah.scalingMode.getOrElse(null).asInstanceOf[Interpolation] // @nowarn
      case _ => null.asInstanceOf[Interpolation] // @nowarn
    }

    if (interpolation == Interpolation.STEP) {
      out.set(scl(index).value)
    } else if (interpolation == Interpolation.LINEAR) {
      val firstKeyframe = scl(index)
      out.set(firstKeyframe.value)
      val nextIndex = index + 1
      if (nextIndex < scl.size) {
        val secondKeyframe = scl(nextIndex)
        val t              = (time - firstKeyframe.keytime) / (secondKeyframe.keytime - firstKeyframe.keytime)
        out.lerp(secondKeyframe.value, t)
      } else {
        out.set(firstKeyframe.value)
      }
    } else if (interpolation == Interpolation.CUBICSPLINE) {
      val firstKeyframe = scl(index)
      val nextIndex     = index + 1
      if (nextIndex < scl.size) {
        val secondKeyframe = scl(nextIndex)
        val t              = (time - firstKeyframe.keytime) / (secondKeyframe.keytime - firstKeyframe.keytime)
        val firstCV        = firstKeyframe.value.asInstanceOf[AnyRef].asInstanceOf[CubicVector3]
        val secondCV       = secondKeyframe.value.asInstanceOf[AnyRef].asInstanceOf[CubicVector3]
        cubicV3(out, t, firstCV.value, firstCV.tangentOut, secondCV.value, secondCV.tangentIn)
      } else {
        out.set(firstKeyframe.value)
      }
    }
    out
  }

  private def getMorphTargetAtTime(nodeAnim: NodeAnimationHack, time: Float, out: WeightVector): WeightVector = boundary {
    if (nodeAnim.weights == null) break(out.set())
    if (nodeAnim.weights.size == 1) break(out.set(nodeAnim.weights(0).value))

    val index = getFirstKeyframeIndexAtTime(nodeAnim.weights, time)

    val interpolation: Interpolation = nodeAnim.weightsMode.getOrElse(null).asInstanceOf[Interpolation] // @nowarn

    if (interpolation == Interpolation.STEP) {
      out.set(nodeAnim.weights(index).value)
    } else if (interpolation == Interpolation.LINEAR) {
      val firstKeyframe = nodeAnim.weights(index)
      out.set(firstKeyframe.value)
      val nextIndex = index + 1
      if (nextIndex < nodeAnim.weights.size) {
        val secondKeyframe = nodeAnim.weights(nextIndex)
        val t              = (time - firstKeyframe.keytime) / (secondKeyframe.keytime - firstKeyframe.keytime)
        out.lerp(secondKeyframe.value, t)
      } else {
        out.set(firstKeyframe.value)
      }
    } else if (interpolation == Interpolation.CUBICSPLINE) {
      val firstKeyframe = nodeAnim.weights(index)
      val nextIndex     = index + 1
      if (nextIndex < nodeAnim.weights.size) {
        val secondKeyframe = nodeAnim.weights(nextIndex)
        val t              = (time - firstKeyframe.keytime) / (secondKeyframe.keytime - firstKeyframe.keytime)
        val firstCV        = firstKeyframe.value.asInstanceOf[CubicWeightVector]
        val secondCV       = secondKeyframe.value.asInstanceOf[CubicWeightVector]
        cubicW(out, t, firstCV, firstCV.tangentOut, secondCV, secondCV.tangentIn)
      } else {
        out.set(firstKeyframe.value)
      }
    }
    out
  }

  private def getNodeAnimationTransform(nodeAnim: NodeAnimation, time: Float): Transform = {
    val transform = tmpT
    getTranslationAtTime(nodeAnim, time, transform.translation)
    getRotationAtTime(nodeAnim, time, transform.rotation)
    getScalingAtTime(nodeAnim, time, transform.scale)
    nodeAnim match {
      case nah: NodeAnimationHack => getMorphTargetAtTime(nah, time, transform.weights)
      case _ => ()
    }
    transform
  }

  private def applyNodeAnimationDirectly(nodeAnim: NodeAnimation, time: Float): Unit = {
    val node = nodeAnim.node
    node.isAnimated = true
    val transform = getNodeAnimationTransform(nodeAnim, time)
    transform.toMatrix4(node.localTransform)
    node match {
      case np: NodePlus =>
        np.weights.foreach { w =>
          w.set(transform.weights)
          var i = 0
          while (i < np.parts.size) {
            np.parts(i).asInstanceOf[NodePartPlus].morphTargets.foreach(_.set(transform.weights))
            i += 1
          }
        }
      case _ => ()
    }
  }

  private def applyNodeAnimationBlending(nodeAnim: NodeAnimation, out: ObjectMap[Node, Transform], pool: Pool[Transform], alpha: Float, time: Float): Unit = {

    val node = nodeAnim.node
    node.isAnimated = true
    val transform = getNodeAnimationTransform(nodeAnim, time)

    val existingOpt = out.get(node)
    if (existingOpt.isDefined) {
      val existing = existingOpt.get
      if (alpha > 0.999999f) existing.set(transform)
      else existing.lerp(transform, alpha)
    } else {
      if (alpha > 0.999999f) {
        out.put(node, pool.obtain().set(transform))
      } else {
        val npWeights = node match {
          case np: NodePlus => np.weights.getOrElse(null).asInstanceOf[WeightVector] // @nowarn
          case _ => null.asInstanceOf[WeightVector] // @nowarn
        }
        out.put(node, pool.obtain().set(node.translation, node.rotation, node.scale, npWeights).lerp(transform, alpha))
      }
    }
  }

  /** Helper method to apply one animation to either an objectmap for blending or directly to the bones. */
  def applyAnimationPlus(out: ObjectMap[Node, Transform], pool: Pool[Transform], alpha: Float, animation: Animation, time: Float): Unit =

    if (out == null) { // @nowarn — null means direct apply
      var i = 0
      while (i < animation.nodeAnimations.size) {
        applyNodeAnimationDirectly(animation.nodeAnimations(i), time)
        i += 1
      }
    } else {
      out.foreachKey { key => key.isAnimated = false }
      var i = 0
      while (i < animation.nodeAnimations.size) {
        applyNodeAnimationBlending(animation.nodeAnimations(i), out, pool, alpha, time)
        i += 1
      }
      out.foreachEntry { (key, value) =>
        if (!key.isAnimated) {
          key.isAnimated = true
          val npWeights = key match {
            case np: NodePlus => np.weights.getOrElse(null).asInstanceOf[WeightVector] // @nowarn
            case _ => null.asInstanceOf[WeightVector] // @nowarn
          }
          value.lerp(key.translation, key.rotation, key.scale, npWeights, alpha)
        }
      }
    }
}
