/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: partial-port
 * Covenant-source-reference: gdx-gltf/gltf/src/net/mgsx/gltf/loaders/shared/GLTFTypes.java
 * Covenant-verified: 2026-04-08
 *
 * Partial-port debt:
 *   - One inherited TODO at line 106 ("is it the proper way to do it?") — needs upstream
 *     review against the original Java to confirm semantics.
 */
package sge
package gltf
package loaders
package shared

import sge.{ Sge, WorldUnits }
import sge.graphics.{ Camera, Color, GL20, OrthographicCamera, PerspectiveCamera, Texture }
import sge.graphics.Texture.{ TextureFilter, TextureWrap }
import sge.graphics.g3d.utils.TextureDescriptor
import sge.gltf.data.camera.GLTFCamera
import sge.gltf.data.data.GLTFAccessor
import sge.gltf.data.texture.GLTFSampler
import sge.gltf.loaders.exceptions.{ GLTFIllegalException, GLTFUnsupportedException }
import sge.gltf.loaders.shared.animation.Interpolation
import sge.gltf.scene3d.model.{ CubicQuaternion, CubicVector3, CubicWeightVector, WeightVector }
import sge.math.{ MathUtils, Quaternion, Vector3 }
import sge.utils.Nullable

object GLTFTypes {

  // https://github.com/KhronosGroup/glTF/blob/master/specification/2.0/README.md#accessor-element-size

  val TYPE_SCALAR: String = "SCALAR"
  val TYPE_VEC2:   String = "VEC2"
  val TYPE_VEC3:   String = "VEC3"
  val TYPE_VEC4:   String = "VEC4"
  val TYPE_MAT2:   String = "MAT2"
  val TYPE_MAT3:   String = "MAT3"
  val TYPE_MAT4:   String = "MAT4"

  val C_BYTE:   Int = 5120
  val C_UBYTE:  Int = 5121
  val C_SHORT:  Int = 5122
  val C_USHORT: Int = 5123
  val C_UINT:   Int = 5125
  val C_FLOAT:  Int = 5126

  /** https://github.com/KhronosGroup/glTF/tree/master/specification/2.0#primitivemode */
  def mapPrimitiveMode(glMode: Nullable[Int]): Int =
    glMode.fold(GL20.GL_TRIANGLES) {
      case 0 => GL20.GL_POINTS
      case 1 => GL20.GL_LINES
      case 2 => GL20.GL_LINE_LOOP
      case 3 => GL20.GL_LINE_STRIP
      case 4 => GL20.GL_TRIANGLES
      case 5 => GL20.GL_TRIANGLE_STRIP
      case 6 => GL20.GL_TRIANGLE_FAN
      case m => throw new GLTFIllegalException("unsupported mode " + m)
    }

  def mapColor(c: Nullable[Array[Float]], defaultColor: Color): Color =
    c.fold(new Color(defaultColor)) { arr =>
      if (arr.length < 4) new Color(arr(0), arr(1), arr(2), 1f)
      else new Color(arr(0), arr(1), arr(2), arr(3))
    }

  def map(q: Quaternion, fv: Array[Float]): Quaternion =
    q.set(fv(0), fv(1), fv(2), fv(3))

  def map(q: Quaternion, fv: Array[Float], offset: Int): Quaternion =
    q.set(fv(offset), fv(offset + 1), fv(offset + 2), fv(offset + 3))

  def map(v: Vector3, fv: Array[Float]): Vector3 =
    v.set(fv(0), fv(1), fv(2))

  def map(v: Vector3, fv: Array[Float], offset: Int): Vector3 =
    v.set(fv(offset), fv(offset + 1), fv(offset + 2))

  def map(v: CubicVector3, fv: Array[Float], offset: Int): CubicVector3 = {
    v.tangentIn.set(fv(offset + 0), fv(offset + 1), fv(offset + 2))
    v.value.set(fv(offset + 3), fv(offset + 4), fv(offset + 5))
    v.tangentOut.set(fv(offset + 6), fv(offset + 7), fv(offset + 8))
    v
  }

  def map(v: CubicQuaternion, fv: Array[Float], offset: Int): CubicQuaternion = {
    v.tangentIn.set(fv(offset + 0), fv(offset + 1), fv(offset + 2), fv(offset + 3))
    v.value.set(fv(offset + 4), fv(offset + 5), fv(offset + 6), fv(offset + 7))
    v.tangentOut.set(fv(offset + 8), fv(offset + 9), fv(offset + 10), fv(offset + 11))
    v
  }

  def map(w: WeightVector, outputData: Array[Float], offset: Int): WeightVector = {
    var i = 0
    while (i < w.count) {
      w.values(i) = outputData(offset + i)
      i += 1
    }
    w
  }

  /** https://github.com/KhronosGroup/glTF/blob/master/specification/2.0/README.md#animations end of chapter : When used with CUBICSPLINE interpolation, tangents (ak, bk) and values (vk) are grouped
    * within keyframes: a1,a2,...an,v1,v2,...vn,b1,b2,...bn
    */
  def map(w: CubicWeightVector, outputData: Array[Float], offset: Int): CubicWeightVector = {
    var off = offset
    var i   = 0
    while (i < w.count) {
      w.tangentIn.values(i) = outputData(off + i)
      i += 1
    }
    off += w.count
    i = 0
    while (i < w.count) {
      w.values(i) = outputData(off + i)
      i += 1
    }
    off += w.count
    i = 0
    while (i < w.count) {
      w.tangentOut.values(i) = outputData(off + i)
      i += 1
    }
    w
  }

  // https://github.com/KhronosGroup/glTF/tree/master/specification/2.0#accessor-element-size
  def accessorTypeSize(accessor: GLTFAccessor): Int =
    accessor.`type`.fold(throw new GLTFIllegalException("accessor type is null")) {
      case TYPE_SCALAR => 1
      case TYPE_VEC2   => 2
      case TYPE_VEC3   => 3
      case TYPE_VEC4   => 4
      case TYPE_MAT2   => 4
      case TYPE_MAT3   => 9
      case TYPE_MAT4   => 16
      case t           => throw new GLTFIllegalException("illegal accessor type: " + t)
    }

  def accessorComponentTypeSize(accessor: GLTFAccessor): Int =
    accessor.componentType match {
      case C_UBYTE | C_BYTE   => 1
      case C_SHORT | C_USHORT => 2
      case C_UINT | C_FLOAT   => 4
      case _                  => throw new GLTFIllegalException("illegal accessor component type: " + accessor.componentType)
    }

  def accessorStrideSize(accessor: GLTFAccessor): Int =
    accessorTypeSize(accessor) * accessorComponentTypeSize(accessor)

  def accessorSize(accessor: GLTFAccessor): Int =
    accessorStrideSize(accessor) * accessor.count

  def map(glCamera: GLTFCamera)(using sge: Sge): Camera =
    glCamera.`type`.fold(throw new GLTFIllegalException("camera type is null")) {
      case "perspective" =>
        // see https://github.com/KhronosGroup/glTF/tree/master/specification/2.0#perspectivezfar
        // emulate an infinite matrix (based on 16 bits depth buffer)
        // TODO is it the proper way to do it?
        val znear = glCamera.perspective.get.znear
        val zfar  = glCamera.perspective.get.zfar.getOrElse(znear * 16384f)

        // convert scale ratio to canvas size
        val canvasRatio = sge.graphics.width.toFloat / sge.graphics.height.toFloat
        val aspectRatio = glCamera.perspective.get.aspectRatio.getOrElse(canvasRatio)
        val yfov        = (scala.math.atan(scala.math.tan(glCamera.perspective.get.yfov * 0.5) * aspectRatio / canvasRatio) * 2.0).toFloat

        val camera = new PerspectiveCamera()
        camera.fieldOfView = yfov * MathUtils.radiansToDegrees
        camera.near = znear
        camera.far = zfar
        camera.viewportWidth = WorldUnits(sge.graphics.width.toFloat)
        camera.viewportHeight = WorldUnits(sge.graphics.height.toFloat)
        camera

      case "orthographic" =>
        val camera = new OrthographicCamera()
        camera.near = glCamera.orthographic.get.znear.get
        camera.far = glCamera.orthographic.get.zfar.get
        val canvasRatio = sge.graphics.width.toFloat / sge.graphics.height.toFloat
        camera.viewportWidth = WorldUnits(glCamera.orthographic.get.xmag.get)
        camera.viewportHeight = WorldUnits(glCamera.orthographic.get.ymag.get / canvasRatio)
        camera

      case t =>
        throw new GLTFIllegalException("unknown camera type " + t)
    }

  // https://github.com/KhronosGroup/glTF/tree/master/specification/2.0#sampler
  def mapTextureSampler(textureDescriptor: TextureDescriptor[Texture], glSampler: GLTFSampler): Unit = {
    textureDescriptor.minFilter = mapTextureMinFilter(glSampler.minFilter)
    textureDescriptor.magFilter = mapTextureMagFilter(glSampler.magFilter)
    textureDescriptor.uWrap = mapTextureWrap(glSampler.wrapS)
    textureDescriptor.vWrap = mapTextureWrap(glSampler.wrapT)
  }

  // https://github.com/KhronosGroup/glTF/tree/master/specification/2.0#samplerwraps
  // https://github.com/KhronosGroup/glTF/tree/master/specification/2.0#samplerwrapt
  private def mapTextureWrap(wrap: Nullable[Int]): TextureWrap =
    wrap.fold(TextureWrap.Repeat) {
      case 33071 => TextureWrap.ClampToEdge
      case 33648 => TextureWrap.MirroredRepeat
      case 10497 => TextureWrap.Repeat
      case w     => throw new GLTFIllegalException("unexpected texture wrap " + w)
    }

  // https://github.com/KhronosGroup/glTF/tree/master/specification/2.0#samplermagfilter
  def mapTextureMagFilter(filter: Nullable[Int]): TextureFilter =
    filter.fold(TextureFilter.Linear) {
      case 9728 => TextureFilter.Nearest
      case 9729 => TextureFilter.Linear
      case f    => throw new GLTFIllegalException("unexpected texture mag filter " + f)
    }

  // https://github.com/KhronosGroup/glTF/tree/master/specification/2.0#samplerminfilter
  def mapTextureMinFilter(filter: Nullable[Int]): TextureFilter =
    filter.fold(TextureFilter.Linear) {
      case 9728 => TextureFilter.Nearest
      case 9729 => TextureFilter.Linear
      case 9984 => TextureFilter.MipMapNearestNearest
      case 9985 => TextureFilter.MipMapLinearNearest
      case 9986 => TextureFilter.MipMapNearestLinear
      case 9987 => TextureFilter.MipMapLinearLinear
      case f    => throw new GLTFIllegalException("unexpected texture min filter " + f)
    }

  def isMipMapFilter(sampler: GLTFSampler): Boolean = {
    val filter = mapTextureMinFilter(sampler.minFilter)
    filter match {
      case TextureFilter.Nearest | TextureFilter.Linear                                                                                                  => false
      case TextureFilter.MipMapNearestNearest | TextureFilter.MipMapLinearNearest | TextureFilter.MipMapNearestLinear | TextureFilter.MipMapLinearLinear =>
        true
      case _ => throw new GLTFIllegalException("unexpected texture min filter " + filter)
    }
  }

  // https://github.com/KhronosGroup/glTF/tree/master/specification/2.0#animation-samplerinterpolation
  def mapInterpolation(`type`: Nullable[String]): Interpolation =
    `type`.fold(Interpolation.LINEAR) {
      case "LINEAR"      => Interpolation.LINEAR
      case "STEP"        => Interpolation.STEP
      case "CUBICSPLINE" => Interpolation.CUBICSPLINE
      case t             => throw new GLTFIllegalException("unexpected interpolation type " + t)
    }
}
