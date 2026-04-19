/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 230
 * Covenant-baseline-methods: ChannelExporter,GLTFAnimationExporter,a,channelExporterQuaternion,channelExporterVector3,channelExporterWeights,exportAnimation,exportAnimations,exportChannel,getOutput,i,mapInterpolation,numComponents,numElements,outputType,rotationInterpolation,scaleInterpolation,translationInterpolation
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package gltf
package exporters

import java.nio.FloatBuffer

import scala.collection.mutable.ArrayBuffer

import sge.graphics.g3d.model.{ Animation, NodeAnimation, NodeKeyframe }
import sge.gltf.data.animation.{ GLTFAnimation, GLTFAnimationChannel, GLTFAnimationSampler, GLTFAnimationTarget }
import sge.gltf.data.data.GLTFAccessor
import sge.gltf.loaders.exceptions.GLTFIllegalException
import sge.gltf.loaders.shared.GLTFTypes
import sge.gltf.scene3d.animation.{ Interpolation, NodeAnimationHack }
import sge.gltf.scene3d.model.{ CubicQuaternion, CubicVector3, CubicWeightVector, WeightVector }
import sge.math.{ Quaternion, Vector3 }
import sge.utils.{ DynamicArray, Nullable }

private[exporters] class GLTFAnimationExporter(private val base: GLTFExporter) {

  def exportAnimations(animations: DynamicArray[Animation]): Unit = {
    var i = 0
    while (i < animations.size) {
      exportAnimation(animations(i))
      i += 1
    }
  }

  private def exportAnimation(animation: Animation): Unit = {
    val a = new GLTFAnimation()
    a.name = Nullable(animation.id)
    if (base.root.animations.isEmpty) base.root.animations = Nullable(ArrayBuffer[GLTFAnimation]())
    base.root.animations.get += a

    var i = 0
    while (i < animation.nodeAnimations.size) {
      val nodeAnim = animation.nodeAnimations(i)
      val nodeID   = base.nodeMapping.indexOfByRef(nodeAnim.node)

      nodeAnim.translation.foreach { trans =>
        GLTFAnimationExporter.channelExporterVector3.exportChannel(base, a, nodeID, trans, "translation", translationInterpolation(nodeAnim))
      }
      nodeAnim.rotation.foreach { rot =>
        GLTFAnimationExporter.channelExporterQuaternion.exportChannel(base, a, nodeID, rot, "rotation", rotationInterpolation(nodeAnim))
      }
      nodeAnim.scaling.foreach { scl =>
        GLTFAnimationExporter.channelExporterVector3.exportChannel(base, a, nodeID, scl, "scale", scaleInterpolation(nodeAnim))
      }
      nodeAnim match {
        case nodeAnimMorph: NodeAnimationHack =>
          if (nodeAnimMorph.weights != null) { // @nowarn — DynamicArray may be uninitialized when no morph targets
            val count = nodeAnimMorph.weights(0).value.count
            GLTFAnimationExporter.channelExporterWeights(count).exportChannel(base, a, nodeID, nodeAnimMorph.weights, "weights", nodeAnimMorph.weightsMode)
          }
        case _ => // nothing
      }
      i += 1
    }
  }

  private def translationInterpolation(nodeAnim: NodeAnimation): Nullable[Interpolation] =
    nodeAnim match {
      case nah: NodeAnimationHack => nah.translationMode
      case _ => Nullable.empty
    }

  private def rotationInterpolation(nodeAnim: NodeAnimation): Nullable[Interpolation] =
    nodeAnim match {
      case nah: NodeAnimationHack => nah.rotationMode
      case _ => Nullable.empty
    }

  private def scaleInterpolation(nodeAnim: NodeAnimation): Nullable[Interpolation] =
    nodeAnim match {
      case nah: NodeAnimationHack => nah.scalingMode
      case _ => Nullable.empty
    }
}

private[exporters] object GLTFAnimationExporter {

  abstract class ChannelExporter[T](
    val numComponents: Int,
    val outputType:    String,
    val numElements:   Int = 1
  ) {

    def exportChannel(
      base:          GLTFExporter,
      a:             GLTFAnimation,
      nodeID:        Int,
      keyFrames:     DynamicArray[NodeKeyframe[T]],
      chanName:      String,
      interpolation: Nullable[Interpolation]
    ): Unit = {
      if (a.channels.isEmpty) a.channels = Nullable(ArrayBuffer[GLTFAnimationChannel]())
      val chan = new GLTFAnimationChannel()
      a.channels.get += chan
      if (a.samplers.isEmpty) a.samplers = Nullable(ArrayBuffer[GLTFAnimationSampler]())
      val sampler = new GLTFAnimationSampler()
      a.samplers.get += sampler
      chan.sampler = Nullable(a.samplers.get.size - 1)
      sampler.interpolation = mapInterpolation(interpolation)

      chan.target = Nullable(new GLTFAnimationTarget())
      chan.target.get.node = Nullable(nodeID)
      chan.target.get.path = Nullable(chanName)
      val numKeyframes        = keyFrames.size
      val inputs              = new Array[Float](numKeyframes)
      val cubic               = interpolation.exists(_ == Interpolation.CUBICSPLINE)
      val interpolationFactor = if (cubic) 3 else 1
      val outputCount         = numKeyframes * interpolationFactor * numElements
      val outputFloats        = outputCount * numComponents
      val outputs             = base.binManager.beginFloats(outputFloats)
      var i                   = 0
      while (i < numKeyframes) {
        val kf = keyFrames(i)
        inputs(i) = kf.keytime
        getOutput(outputs, kf.value)
        i += 1
      }
      val outputAccessor = base.obtainAccessor()
      outputAccessor.bufferView = Nullable(base.binManager.end())
      outputAccessor.componentType = GLTFTypes.C_FLOAT
      outputAccessor.count = outputCount
      outputAccessor.`type` = Nullable(outputType)
      sampler.output = Nullable(base.root.accessors.get.size - 1)

      val inputBuffer = base.binManager.beginFloats(numKeyframes)
      inputBuffer.put(inputs)

      val inputAccessor = base.obtainAccessor()
      inputAccessor.componentType = GLTFTypes.C_FLOAT
      inputAccessor.count = numKeyframes
      inputAccessor.`type` = Nullable(GLTFTypes.TYPE_SCALAR)
      inputAccessor.bufferView = Nullable(base.binManager.end())
      // min max are mandatory for sampler inputs
      inputAccessor.min = Nullable(Array(inputs(0)))
      inputAccessor.max = Nullable(Array(inputs(inputs.length - 1)))
      sampler.input = Nullable(base.root.accessors.get.size - 1)
    }

    protected def getOutput(outputs: FloatBuffer, value: T): Unit
  }

  /** Exports Vector3 keyframe values. CubicVector3 values (stored via type erasure cast) are detected by casting to AnyRef and checking isInstanceOf[CubicVector3].
    */
  val channelExporterVector3: ChannelExporter[Vector3] = new ChannelExporter[Vector3](3, GLTFTypes.TYPE_VEC3) {
    override protected def getOutput(outputs: FloatBuffer, value: Vector3): Unit = {
      // CubicVector3 is stored as Vector3 via asInstanceOf erasure cast in the loader
      val asRef = value.asInstanceOf[AnyRef]
      if (asRef.isInstanceOf[CubicVector3]) {
        val cubic = asRef.asInstanceOf[CubicVector3]
        outputs.put(cubic.tangentIn.x)
        outputs.put(cubic.tangentIn.y)
        outputs.put(cubic.tangentIn.z)
        outputs.put(cubic.value.x)
        outputs.put(cubic.value.y)
        outputs.put(cubic.value.z)
        outputs.put(cubic.tangentOut.x)
        outputs.put(cubic.tangentOut.y)
        outputs.put(cubic.tangentOut.z)
      } else {
        outputs.put(value.x)
        outputs.put(value.y)
        outputs.put(value.z)
      }
    }
  }

  /** Exports Quaternion keyframe values. CubicQuaternion values (stored via type erasure cast) are detected by casting to AnyRef and checking isInstanceOf[CubicQuaternion].
    */
  val channelExporterQuaternion: ChannelExporter[Quaternion] = new ChannelExporter[Quaternion](4, GLTFTypes.TYPE_VEC4) {
    override protected def getOutput(outputs: FloatBuffer, value: Quaternion): Unit = {
      // CubicQuaternion is stored as Quaternion via asInstanceOf erasure cast in the loader
      val asRef = value.asInstanceOf[AnyRef]
      if (asRef.isInstanceOf[CubicQuaternion]) {
        val cubic = asRef.asInstanceOf[CubicQuaternion]
        outputs.put(cubic.tangentIn.x)
        outputs.put(cubic.tangentIn.y)
        outputs.put(cubic.tangentIn.z)
        outputs.put(cubic.tangentIn.w)
        outputs.put(cubic.value.x)
        outputs.put(cubic.value.y)
        outputs.put(cubic.value.z)
        outputs.put(cubic.value.w)
        outputs.put(cubic.tangentOut.x)
        outputs.put(cubic.tangentOut.y)
        outputs.put(cubic.tangentOut.z)
        outputs.put(cubic.tangentOut.w)
      } else {
        outputs.put(value.x)
        outputs.put(value.y)
        outputs.put(value.z)
        outputs.put(value.w)
      }
    }
  }

  def channelExporterWeights(count: Int): ChannelExporter[WeightVector] =
    new ChannelExporter[WeightVector](1, GLTFTypes.TYPE_SCALAR, count) {
      override protected def getOutput(outputs: FloatBuffer, value: WeightVector): Unit =
        if (value.isInstanceOf[CubicWeightVector]) {

          /** https://github.com/KhronosGroup/glTF/blob/master/specification/2.0/README.md#animations end of chapter : When used with CUBICSPLINE interpolation, tangents (ak, bk) and values (vk) are
            * grouped within keyframes: a1,a2,...an,v1,v2,...vn,b1,b2,...bn
            */
          val cubic = value.asInstanceOf[CubicWeightVector]
          outputs.put(cubic.tangentIn.values, 0, value.count)
          outputs.put(value.values, 0, value.count)
          outputs.put(cubic.tangentOut.values, 0, value.count)
        } else {
          outputs.put(value.values, 0, value.count)
        }
    }

  def mapInterpolation(interpolation: Nullable[Interpolation]): Nullable[String] =
    interpolation.fold(Nullable.empty[String]) {
      case Interpolation.LINEAR      => Nullable.empty // default "LINEAR"
      case Interpolation.STEP        => Nullable("STEP")
      case Interpolation.CUBICSPLINE => Nullable("CUBICSPLINE")
      case other                     => throw new GLTFIllegalException("unexpected interpolation type " + other)
    }
}
