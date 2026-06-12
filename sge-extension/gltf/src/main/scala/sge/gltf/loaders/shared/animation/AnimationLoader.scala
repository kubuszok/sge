/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 235
 * Covenant-baseline-methods: AnimationLoader,animMap,animation,animations,load,toSceneInterpolation
 * Covenant-source-reference: net/mgsx/gltf/loaders/shared/animation/AnimationLoader.java
 * Covenant-verified: 2026-06-12
 */
package sge
package gltf
package loaders
package shared
package animation

import scala.collection.mutable.{ ArrayBuffer, HashMap }
import sge.graphics.g3d.model.{ Animation, Node, NodeAnimation, NodeKeyframe }
import sge.gltf.data.animation.GLTFAnimation
import sge.gltf.loaders.exceptions.GLTFUnsupportedException
import sge.gltf.loaders.shared.GLTFTypes
import sge.gltf.loaders.shared.data.DataResolver
import sge.gltf.loaders.shared.scene.NodeResolver
import sge.gltf.scene3d.animation.{ Interpolation as SceneInterpolation, NodeAnimationHack }
import sge.gltf.scene3d.model.{ CubicQuaternion, CubicVector3, CubicWeightVector, NodePlus, WeightVector }
import sge.math.{ Quaternion, Vector3 }
import lowlevel.Nullable
import lowlevel.util.DynamicArray

class AnimationLoader {

  val animations: DynamicArray[Animation] = DynamicArray[Animation]()

  /** Maps loaders Interpolation to scene3d Interpolation for NodeAnimationHack mode fields. */
  private def toSceneInterpolation(interp: Interpolation): SceneInterpolation =
    interp match {
      case Interpolation.LINEAR      => SceneInterpolation.LINEAR
      case Interpolation.STEP        => SceneInterpolation.STEP
      case Interpolation.CUBICSPLINE => SceneInterpolation.CUBICSPLINE
    }

  def load(glAnimations: Nullable[ArrayBuffer[GLTFAnimation]], nodeResolver: NodeResolver, dataResolver: DataResolver): Unit =
    glAnimations.foreach { anims =>
      var i = 0
      while (i < anims.size) {
        val glAnimation = anims(i)
        val animation   = load(glAnimation, nodeResolver, dataResolver)
        animation.id = glAnimation.name.getOrElse("animation" + i)
        animations.add(animation)
        i += 1
      }
    }

  private def load(glAnimation: GLTFAnimation, nodeResolver: NodeResolver, dataResolver: DataResolver): Animation = {
    val animMap   = HashMap[Node, NodeAnimation]()
    val animation = new Animation()

    glAnimation.channels.foreach { channels =>
      for (glChannel <- channels) {
        val glSampler = glAnimation.samplers.get(glChannel.sampler.get)
        val node      = nodeResolver.get(glChannel.target.get.node.get).get

        val nodeAnimation = animMap.getOrElseUpdate(node, {
                                                      val na = new NodeAnimationHack()
                                                      na.node = node
                                                      animation.nodeAnimations.add(na)
                                                      na
                                                    }
        )

        val inputData  = dataResolver.readBufferFloat(glSampler.input.get)
        val outputData = dataResolver.readBufferFloat(glSampler.output.get)

        val interpolation = GLTFTypes.mapInterpolation(glSampler.interpolation)

        // case of cubic spline, we skip anchor vectors if cubic is disabled.
        var dataOffset = 0
        var dataStride = 1
        if (interpolation == Interpolation.CUBICSPLINE) {
          dataOffset = 1
          dataStride = 3
        }

        val inputAccessor = dataResolver.getAccessor(glSampler.input.get)
        inputAccessor.max.foreach { maxArr =>
          animation.duration = scala.math.max(animation.duration, maxArr(0))
        }

        val property = glChannel.target.get.path.get
        if ("translation" == property) {

          val nah = nodeAnimation.asInstanceOf[NodeAnimationHack]
          nah.translationMode = Nullable(toSceneInterpolation(interpolation))

          val translationKf = DynamicArray[NodeKeyframe[Vector3]]()
          if (interpolation == Interpolation.CUBICSPLINE) {
            // Vector3 is a final case class in SGE, so CubicVector3 cannot subtype it (see CubicVector3.scala):
            // the keyframe carries the plain value Vector3 while the CubicVector3 (value + tangents) is kept
            // in a parallel array on NodeAnimationHack, index-aligned with translationKf.
            val translationCubic = DynamicArray[CubicVector3]()
            // copy first frame if not at zero time
            if (inputData(0) > 0) {
              val cv = GLTFTypes.map(CubicVector3(), outputData, 0)
              translationKf.add(new NodeKeyframe[Vector3](0, cv.value))
              translationCubic.add(cv)
            }
            var k = 0
            while (k < inputData.length) {
              val cv = GLTFTypes.map(CubicVector3(), outputData, k * dataStride * 3)
              translationKf.add(new NodeKeyframe[Vector3](inputData(k), cv.value))
              translationCubic.add(cv)
              k += 1
            }
            nah.translationCubic = Nullable(translationCubic)
          } else {
            // copy first frame if not at zero time
            if (inputData(0) > 0) {
              translationKf.add(new NodeKeyframe[Vector3](0, GLTFTypes.map(new Vector3(), outputData, dataOffset * 3)))
            }
            var k = 0
            while (k < inputData.length) {
              translationKf.add(new NodeKeyframe[Vector3](inputData(k), GLTFTypes.map(new Vector3(), outputData, (dataOffset + (k * dataStride)) * 3)))
              k += 1
            }
          }
          nodeAnimation.translation = translationKf
        } else if ("rotation" == property) {

          nodeAnimation.asInstanceOf[NodeAnimationHack].rotationMode = Nullable(toSceneInterpolation(interpolation))

          val rotationKf = DynamicArray[NodeKeyframe[Quaternion]]()
          if (interpolation == Interpolation.CUBICSPLINE) {
            // CubicQuaternion extends Quaternion (CubicQuaternion.scala), exactly as in gdx-gltf
            // (AnimationLoader.java:110/113), so it is stored directly as the keyframe value with no cast.
            // copy first frame if not at zero time
            if (inputData(0) > 0) {
              rotationKf.add(new NodeKeyframe[Quaternion](0, GLTFTypes.map(new CubicQuaternion(), outputData, 0)))
            }
            var k = 0
            while (k < inputData.length) {
              rotationKf.add(new NodeKeyframe[Quaternion](inputData(k), GLTFTypes.map(new CubicQuaternion(), outputData, k * dataStride * 4)))
              k += 1
            }
          } else {
            // copy first frame if not at zero time
            if (inputData(0) > 0) {
              rotationKf.add(new NodeKeyframe[Quaternion](0, GLTFTypes.map(new Quaternion(), outputData, dataOffset * 4)))
            }
            var k = 0
            while (k < inputData.length) {
              rotationKf.add(new NodeKeyframe[Quaternion](inputData(k), GLTFTypes.map(new Quaternion(), outputData, (dataOffset + (k * dataStride)) * 4)))
              k += 1
            }
          }
          nodeAnimation.rotation = rotationKf
        } else if ("scale" == property) {

          val nahS = nodeAnimation.asInstanceOf[NodeAnimationHack]
          nahS.scalingMode = Nullable(toSceneInterpolation(interpolation))

          val scalingKf = DynamicArray[NodeKeyframe[Vector3]]()
          if (interpolation == Interpolation.CUBICSPLINE) {
            // see translation branch: cubic data kept in a parallel array on NodeAnimationHack (Vector3 is a final case class).
            val scalingCubic = DynamicArray[CubicVector3]()
            // copy first frame if not at zero time
            if (inputData(0) > 0) {
              val cv = GLTFTypes.map(CubicVector3(), outputData, 0)
              scalingKf.add(new NodeKeyframe[Vector3](0, cv.value))
              scalingCubic.add(cv)
            }
            var k = 0
            while (k < inputData.length) {
              val cv = GLTFTypes.map(CubicVector3(), outputData, k * dataStride * 3)
              scalingKf.add(new NodeKeyframe[Vector3](inputData(k), cv.value))
              scalingCubic.add(cv)
              k += 1
            }
            nahS.scalingCubic = Nullable(scalingCubic)
          } else {
            // copy first frame if not at zero time
            if (inputData(0) > 0) {
              scalingKf.add(new NodeKeyframe[Vector3](0, GLTFTypes.map(new Vector3(), outputData, dataOffset * 3)))
            }
            var k = 0
            while (k < inputData.length) {
              scalingKf.add(new NodeKeyframe[Vector3](inputData(k), GLTFTypes.map(new Vector3(), outputData, (dataOffset + (k * dataStride)) * 3)))
              k += 1
            }
          }
          nodeAnimation.scaling = scalingKf
        } else if ("weights" == property) {

          nodeAnimation.asInstanceOf[NodeAnimationHack].weightsMode = Nullable(toSceneInterpolation(interpolation))

          val np        = nodeAnimation.asInstanceOf[NodeAnimationHack]
          val nbWeights = node.asInstanceOf[NodePlus].weights.get.count
          val weightsKf = DynamicArray[NodeKeyframe[WeightVector]]()
          if (interpolation == Interpolation.CUBICSPLINE) {
            // copy first frame if not at zero time
            if (inputData(0) > 0) {
              weightsKf.add(new NodeKeyframe[WeightVector](0, GLTFTypes.map(new CubicWeightVector(nbWeights), outputData, 0)))
            }
            var k = 0
            while (k < inputData.length) {
              weightsKf.add(
                new NodeKeyframe[WeightVector](inputData(k), GLTFTypes.map(new CubicWeightVector(nbWeights), outputData, k * dataStride * nbWeights))
              )
              k += 1
            }
          } else {
            // copy first frame if not at zero time
            if (inputData(0) > 0) {
              weightsKf.add(new NodeKeyframe[WeightVector](0, GLTFTypes.map(new WeightVector(nbWeights), outputData, dataOffset * nbWeights)))
            }
            var k = 0
            while (k < inputData.length) {
              weightsKf.add(
                new NodeKeyframe[WeightVector](inputData(k), GLTFTypes.map(new WeightVector(nbWeights), outputData, (dataOffset + (k * dataStride)) * nbWeights))
              )
              k += 1
            }
          }
          np.weights = weightsKf
        } else {
          throw new GLTFUnsupportedException("unsupported " + property)
        }
      }
    }

    animation
  }
}
