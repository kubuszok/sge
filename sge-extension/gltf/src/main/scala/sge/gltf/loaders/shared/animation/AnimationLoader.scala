/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
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
import sge.math.{ Quaternion, Vector3 }
import sge.utils.{ DynamicArray, Nullable }

class AnimationLoader {

  val animations: DynamicArray[Animation] = DynamicArray[Animation]()

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
                                                      val na = new NodeAnimation()
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
          val translationKf = DynamicArray[NodeKeyframe[Vector3]]()
          // copy first frame if not at zero time
          if (inputData(0) > 0) {
            translationKf.add(new NodeKeyframe[Vector3](0, GLTFTypes.map(new Vector3(), outputData, dataOffset * 3)))
          }
          var k = 0
          while (k < inputData.length) {
            translationKf.add(new NodeKeyframe[Vector3](inputData(k), GLTFTypes.map(new Vector3(), outputData, (dataOffset + (k * dataStride)) * 3)))
            k += 1
          }
          nodeAnimation.translation = translationKf
        } else if ("rotation" == property) {
          val rotationKf = DynamicArray[NodeKeyframe[Quaternion]]()
          if (inputData(0) > 0) {
            rotationKf.add(new NodeKeyframe[Quaternion](0, GLTFTypes.map(new Quaternion(), outputData, dataOffset * 4)))
          }
          var k = 0
          while (k < inputData.length) {
            rotationKf.add(new NodeKeyframe[Quaternion](inputData(k), GLTFTypes.map(new Quaternion(), outputData, (dataOffset + (k * dataStride)) * 4)))
            k += 1
          }
          nodeAnimation.rotation = rotationKf
        } else if ("scale" == property) {
          val scalingKf = DynamicArray[NodeKeyframe[Vector3]]()
          if (inputData(0) > 0) {
            scalingKf.add(new NodeKeyframe[Vector3](0, GLTFTypes.map(new Vector3(), outputData, dataOffset * 3)))
          }
          var k = 0
          while (k < inputData.length) {
            scalingKf.add(new NodeKeyframe[Vector3](inputData(k), GLTFTypes.map(new Vector3(), outputData, (dataOffset + (k * dataStride)) * 3)))
            k += 1
          }
          nodeAnimation.scaling = scalingKf
        } else if ("weights" == property) {
          // Weight animation requires scene3d types (NodePlus, WeightVector) not yet ported
          throw new GLTFUnsupportedException("weight animation not yet supported in SGE")
        } else {
          throw new GLTFUnsupportedException("unsupported " + property)
        }
      }
    }

    animation
  }
}
