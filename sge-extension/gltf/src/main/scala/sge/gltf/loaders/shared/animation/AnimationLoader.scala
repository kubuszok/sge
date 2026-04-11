/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant-source-reference: gdx-gltf/gltf/src/net/mgsx/gltf/loaders/shared/animation/AnimationLoader.java
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
import sge.utils.{ DynamicArray, Nullable }

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

          nodeAnimation.asInstanceOf[NodeAnimationHack].translationMode = Nullable(toSceneInterpolation(interpolation))

          val translationKf = DynamicArray[NodeKeyframe[Vector3]]()
          if (interpolation == Interpolation.CUBICSPLINE) {
            // copy first frame if not at zero time
            if (inputData(0) > 0) {
              translationKf.add(
                new NodeKeyframe[Vector3](0, GLTFTypes.map(CubicVector3(), outputData, 0).asInstanceOf[AnyRef].asInstanceOf[Vector3])
              ) // @nowarn — cubic stored as Vector3 for keyframe erasure
            }
            var k = 0
            while (k < inputData.length) {
              translationKf.add(
                new NodeKeyframe[Vector3](inputData(k), GLTFTypes.map(CubicVector3(), outputData, k * dataStride * 3).asInstanceOf[AnyRef].asInstanceOf[Vector3])
              ) // @nowarn — cubic stored as Vector3 for keyframe erasure
              k += 1
            }
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
            // copy first frame if not at zero time
            if (inputData(0) > 0) {
              rotationKf.add(
                new NodeKeyframe[Quaternion](0, GLTFTypes.map(CubicQuaternion(), outputData, 0).asInstanceOf[AnyRef].asInstanceOf[Quaternion])
              ) // @nowarn — cubic stored as Quaternion for keyframe erasure
            }
            var k = 0
            while (k < inputData.length) {
              rotationKf.add(
                new NodeKeyframe[Quaternion](inputData(k), GLTFTypes.map(CubicQuaternion(), outputData, k * dataStride * 4).asInstanceOf[AnyRef].asInstanceOf[Quaternion])
              ) // @nowarn — cubic stored as Quaternion for keyframe erasure
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

          nodeAnimation.asInstanceOf[NodeAnimationHack].scalingMode = Nullable(toSceneInterpolation(interpolation))

          val scalingKf = DynamicArray[NodeKeyframe[Vector3]]()
          if (interpolation == Interpolation.CUBICSPLINE) {
            // copy first frame if not at zero time
            if (inputData(0) > 0) {
              scalingKf.add(
                new NodeKeyframe[Vector3](0, GLTFTypes.map(CubicVector3(), outputData, 0).asInstanceOf[AnyRef].asInstanceOf[Vector3])
              ) // @nowarn — cubic stored as Vector3 for keyframe erasure
            }
            var k = 0
            while (k < inputData.length) {
              scalingKf.add(
                new NodeKeyframe[Vector3](inputData(k), GLTFTypes.map(CubicVector3(), outputData, k * dataStride * 3).asInstanceOf[AnyRef].asInstanceOf[Vector3])
              ) // @nowarn — cubic stored as Vector3 for keyframe erasure
              k += 1
            }
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
