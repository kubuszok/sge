/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/model/ModelInstanceHack.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 *
 * ModelInstance hack for morph targets:
 * - copy animations with NodeAnimationHack
 * - pass morph targets to shader via Renderable userData
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 131
 * Covenant-baseline-methods: ModelInstanceHack,animation,copyAnimation,getRenderable,i,this
 * Covenant-source-reference: net/mgsx/gltf/scene3d/model/ModelInstanceHack.java
 * Covenant-verified: 2026-06-12
 */
package sge
package gltf
package scene3d
package model

import sge.gltf.scene3d.animation.NodeAnimationHack
import sge.graphics.g3d.{ Model, ModelInstance, Renderable }
import sge.graphics.g3d.model.{ Animation, Node, NodeAnimation, NodeKeyframe, NodePart }
import sge.math.{ Quaternion, Vector3 }
import lowlevel.Nullable
import lowlevel.util.DynamicArray

// ModelInstanceHack.java:25-31 — the primary ctor (Model) forwards `super(model)`
// (whole-model copy, via the base `null`-ids path); the (Model, String...) ctor
// forwards `super(model, rootNodeIds)`. The base ModelInstance routes a non-empty
// id set through copyNodesById, keeping only the named root nodes. Here the SGE
// primary ctor takes the `Nullable[Seq[String]]` ids and extends the matching base
// ctor directly, so both Java overloads land on the correct base path.
class ModelInstanceHack(model: Model, rootNodeIds: Nullable[Seq[String]]) extends ModelInstance(model, rootNodeIds) {

  // ModelInstanceHack.java:25-27 — `public ModelInstanceHack(Model model){ super(model); }`
  def this(model: Model) =
    this(model, Nullable.empty)

  // ModelInstanceHack.java:29-31 — `public ModelInstanceHack(Model model, String... rootNodeIds){ super(model, rootNodeIds); }`
  // Forward the ids to the base ModelInstance, which copies only the named root nodes.
  def this(model: Model, rootNodeIds: String*) =
    this(model, Nullable(rootNodeIds))

  override def copyAnimation(anim: Animation, shareKeyframes: Boolean): Unit = {
    val animation = Animation()
    animation.id = anim.id
    animation.duration = anim.duration
    var i = 0
    while (i < anim.nodeAnimations.size) {
      val nanim = anim.nodeAnimations(i)
      val node  = getNode(nanim.node.id)
      if (node.isDefined) {
        val nodeAnim = NodeAnimationHack()
        nodeAnim.node = node.get

        nanim match {
          case nah: NodeAnimationHack =>
            nodeAnim.translationMode = nah.translationMode
            nodeAnim.rotationMode = nah.rotationMode
            nodeAnim.scalingMode = nah.scalingMode
            nodeAnim.weightsMode = nah.weightsMode
          case _ => ()
        }

        if (shareKeyframes) {
          nodeAnim.translation = nanim.translation
          nodeAnim.rotation = nanim.rotation
          nodeAnim.scaling = nanim.scaling
          nanim match {
            case nah: NodeAnimationHack => nodeAnim.weights = nah.weights
            case _ => ()
          }
        } else {
          if (nanim.translation.isDefined) {
            val trans    = nanim.translation.get
            val newTrans = DynamicArray[NodeKeyframe[Vector3]]()
            var j        = 0
            while (j < trans.size) {
              val kf = trans(j)
              newTrans.add(NodeKeyframe(kf.keytime, kf.value))
              j += 1
            }
            nodeAnim.translation = Nullable(newTrans)
          }
          if (nanim.rotation.isDefined) {
            val rot    = nanim.rotation.get
            val newRot = DynamicArray[NodeKeyframe[Quaternion]]()
            var j      = 0
            while (j < rot.size) {
              val kf = rot(j)
              newRot.add(NodeKeyframe(kf.keytime, kf.value))
              j += 1
            }
            nodeAnim.rotation = Nullable(newRot)
          }
          if (nanim.scaling.isDefined) {
            val scl    = nanim.scaling.get
            val newScl = DynamicArray[NodeKeyframe[Vector3]]()
            var j      = 0
            while (j < scl.size) {
              val kf = scl(j)
              newScl.add(NodeKeyframe(kf.keytime, kf.value))
              j += 1
            }
            nodeAnim.scaling = Nullable(newScl)
          }
          nanim match {
            case nah: NodeAnimationHack if nah.weights != null => // @nowarn — nullable field
              val newWeights = DynamicArray[NodeKeyframe[WeightVector]]()
              var j          = 0
              while (j < nah.weights.size) {
                val kf = nah.weights(j)
                newWeights.add(NodeKeyframe(kf.keytime, kf.value))
                j += 1
              }
              nodeAnim.weights = newWeights
            case _ => ()
          }
        }

        if (
          nodeAnim.translation.isDefined || nodeAnim.rotation.isDefined || nodeAnim.scaling.isDefined ||
          (nanim.isInstanceOf[NodeAnimationHack] && nanim.asInstanceOf[NodeAnimationHack].weights != null)
        ) {
          animation.nodeAnimations.add(nodeAnim)
        }
      }
      i += 1
    }
    if (animation.nodeAnimations.size > 0) animations.add(animation)
  }

  override def getRenderable(out: Renderable, node: Node, nodePart: NodePart): Renderable = {
    super.getRenderable(out, node, nodePart)
    nodePart match {
      case npp: NodePartPlus =>
        npp.morphTargets.foreach { mt =>
          out.userData = Nullable(mt.asInstanceOf[AnyRef])
        }
      case _ => ()
    }
    out
  }
}
