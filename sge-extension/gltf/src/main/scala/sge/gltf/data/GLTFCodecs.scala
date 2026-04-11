/*
 * JSON codecs for the GLTF data model.
 *
 * Provides jsoniter-scala codecs for all GLTF 2.0 data classes, enabling
 * deserialization from JSON strings using `readFromString[GLTF](json)`.
 *
 * Since the GLTF data model uses mutable `class` (not `case class`) with `var`
 * fields and `Nullable`, Kindlings derivation macros cannot be used directly.
 * Instead, codecs are written manually using jsoniter-scala's low-level API.
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gltf
package data

import scala.collection.mutable.{ ArrayBuffer, HashMap }

import com.github.plokhotnyuk.jsoniter_scala.core.{ JsonReader, JsonValueCodec, JsonWriter }

import sge.gltf.data.animation.{ GLTFAnimation, GLTFAnimationChannel, GLTFAnimationSampler, GLTFAnimationTarget }
import sge.gltf.data.camera.{ GLTFCamera, GLTFOrthographic, GLTFPerspective }
import sge.gltf.data.data.{ GLTFAccessor, GLTFAccessorSparse, GLTFAccessorSparseIndices, GLTFAccessorSparseValues, GLTFBuffer, GLTFBufferView }
import sge.gltf.data.extensions._
import sge.gltf.data.geometry.{ GLTFMesh, GLTFMorphTarget, GLTFPrimitive }
import sge.gltf.data.material.{ GLTFMaterial, GLTFpbrMetallicRoughness }
import sge.gltf.data.scene.{ GLTFNode, GLTFScene, GLTFSkin }
import sge.gltf.data.texture.{ GLTFImage, GLTFNormalTextureInfo, GLTFOcclusionTextureInfo, GLTFSampler, GLTFTexture, GLTFTextureInfo }
import sge.utils.{ Json, JsonCodec, Nullable, given_JsonCodec_Json }

/** Central registry of GLTF JSON codecs. Import `GLTFCodecs.given` to bring all codecs into scope for `readFromString[GLTF](json)`.
  */
object GLTFCodecs {

  // ── Nullable codec ──────────────────────────────────────────────────

  given nullableCodec[A](using inner: JsonValueCodec[A]): JsonValueCodec[Nullable[A]] = new JsonValueCodec[Nullable[A]] {
    override def decodeValue(in: JsonReader, default: Nullable[A]): Nullable[A] =
      if (in.isNextToken('n')) {
        in.readNullOrError(Nullable.empty[A], "expected null")
        Nullable.empty[A]
      } else {
        in.rollbackToken()
        Nullable(inner.decodeValue(in, null.asInstanceOf[A]))
      }

    override def encodeValue(x: Nullable[A], out: JsonWriter): Unit =
      if (Nullable.isEmpty(x)) out.writeNull()
      else inner.encodeValue(x.get, out)

    override def nullValue: Nullable[A] = Nullable.empty[A]
  }

  // ── Helpers ──────────────────────────────────────────────────────────

  private def readNullableInt(in: JsonReader, current: Nullable[Int]): Nullable[Int] =
    if (in.isNextToken('n')) { in.readNullOrError(Nullable.empty[Int], "expected null"); Nullable.empty[Int] }
    else { in.rollbackToken(); Nullable(in.readInt()) }

  private def readNullableFloat(in: JsonReader, current: Nullable[Float]): Nullable[Float] =
    if (in.isNextToken('n')) { in.readNullOrError(Nullable.empty[Float], "expected null"); Nullable.empty[Float] }
    else { in.rollbackToken(); Nullable(in.readFloat()) }

  private def readNullableBoolean(in: JsonReader, current: Nullable[Boolean]): Nullable[Boolean] =
    if (in.isNextToken('n')) { in.readNullOrError(Nullable.empty[Boolean], "expected null"); Nullable.empty[Boolean] }
    else { in.rollbackToken(); Nullable(in.readBoolean()) }

  private def readNullableString(in: JsonReader, current: Nullable[String]): Nullable[String] =
    if (in.isNextToken('n')) { in.readNullOrError(Nullable.empty[String], "expected null"); Nullable.empty[String] }
    else { in.rollbackToken(); Nullable(in.readString(null)) }

  private def readNullableFloatArray(in: JsonReader): Nullable[Array[Float]] =
    if (in.isNextToken('n')) { in.readNullOrError(Nullable.empty[Array[Float]], "expected null"); Nullable.empty[Array[Float]] }
    else {
      in.rollbackToken()
      val buf = ArrayBuffer[Float]()
      if (in.isNextToken('[')) {
        if (!in.isNextToken(']')) {
          in.rollbackToken()
          buf += in.readFloat()
          while (in.isNextToken(',')) buf += in.readFloat()
        }
      }
      Nullable(buf.toArray)
    }

  private def readNullableIntArray(in: JsonReader): Nullable[ArrayBuffer[Int]] =
    if (in.isNextToken('n')) { in.readNullOrError(Nullable.empty[ArrayBuffer[Int]], "expected null"); Nullable.empty[ArrayBuffer[Int]] }
    else {
      in.rollbackToken()
      val buf = ArrayBuffer[Int]()
      if (in.isNextToken('[')) {
        if (!in.isNextToken(']')) {
          in.rollbackToken()
          buf += in.readInt()
          while (in.isNextToken(',')) buf += in.readInt()
        }
      }
      Nullable(buf)
    }

  private def readNullableStringArray(in: JsonReader): Nullable[ArrayBuffer[String]] =
    if (in.isNextToken('n')) { in.readNullOrError(Nullable.empty[ArrayBuffer[String]], "expected null"); Nullable.empty[ArrayBuffer[String]] }
    else {
      in.rollbackToken()
      val buf = ArrayBuffer[String]()
      if (in.isNextToken('[')) {
        if (!in.isNextToken(']')) {
          in.rollbackToken()
          buf += in.readString(null)
          while (in.isNextToken(',')) buf += in.readString(null)
        }
      }
      Nullable(buf)
    }

  private def readNullableObj[A](in: JsonReader, current: Nullable[A])(using codec: JsonValueCodec[A]): Nullable[A] =
    if (in.isNextToken('n')) { in.readNullOrError(Nullable.empty[A], "expected null"); Nullable.empty[A] }
    else { in.rollbackToken(); Nullable(codec.decodeValue(in, null.asInstanceOf[A])) }

  private def readNullableArray[A](in: JsonReader)(using codec: JsonValueCodec[A]): Nullable[ArrayBuffer[A]] =
    if (in.isNextToken('n')) { in.readNullOrError(Nullable.empty[ArrayBuffer[A]], "expected null"); Nullable.empty[ArrayBuffer[A]] }
    else {
      in.rollbackToken()
      val buf = ArrayBuffer[A]()
      if (in.isNextToken('[')) {
        if (!in.isNextToken(']')) {
          in.rollbackToken()
          buf += codec.decodeValue(in, null.asInstanceOf[A])
          while (in.isNextToken(',')) buf += codec.decodeValue(in, null.asInstanceOf[A])
        }
      }
      Nullable(buf)
    }

  private def readNullableStringIntMap(in: JsonReader): Nullable[HashMap[String, Int]] =
    if (in.isNextToken('n')) { in.readNullOrError(Nullable.empty[HashMap[String, Int]], "expected null"); Nullable.empty[HashMap[String, Int]] }
    else {
      in.rollbackToken()
      val map = HashMap[String, Int]()
      if (in.isNextToken('{')) {
        if (!in.isNextToken('}')) {
          in.rollbackToken()
          map.put(in.readKeyAsString(), in.readInt())
          while (in.isNextToken(',')) map.put(in.readKeyAsString(), in.readInt())
        }
      }
      Nullable(map)
    }

  /** Read fields common to GLTFObject: extensions, extras. Returns true if the field was handled. */
  private def readObjectField(field: String, obj: GLTFObject, in: JsonReader)(using
    extCodec:    JsonValueCodec[GLTFExtensions],
    extrasCodec: JsonValueCodec[GLTFExtras]
  ): Boolean = field match {
    case "extensions" => obj.extensions = readNullableObj[GLTFExtensions](in, obj.extensions); true
    case "extras"     => obj.extras = readNullableObj[GLTFExtras](in, obj.extras); true
    case _            => false
  }

  /** Read fields common to GLTFEntity: name + GLTFObject fields. Returns true if the field was handled. */
  private def readEntityField(field: String, obj: GLTFEntity, in: JsonReader)(using
    extCodec:    JsonValueCodec[GLTFExtensions],
    extrasCodec: JsonValueCodec[GLTFExtras]
  ): Boolean = field match {
    case "name" => obj.name = readNullableString(in, obj.name); true
    case _      => readObjectField(field, obj, in)
  }

  /** Reads a JSON object calling fieldHandler for each key. Skips unknown fields. */
  private inline def readFields[A](in: JsonReader, obj: A)(fieldHandler: (String, A, JsonReader) => Unit): A = {
    if (in.isNextToken('{')) {
      if (!in.isNextToken('}')) {
        in.rollbackToken()
        var continue = true
        while (continue) {
          val key = in.readKeyAsString()
          fieldHandler(key, obj, in)
          continue = in.isNextToken(',')
        }
      }
    } else in.readNullOrTokenError(obj, '{')
    obj
  }

  // ── GLTFExtensions codec — typed dispatch for known extensions ───────

  given gltfExtensionsCodec(using jsonCodec: JsonValueCodec[Json]): JsonValueCodec[GLTFExtensions] = new JsonValueCodec[GLTFExtensions] {
    override def decodeValue(in: JsonReader, default: GLTFExtensions): GLTFExtensions = {
      val ext = if (default != null) default else new GLTFExtensions()
      if (in.isNextToken('{')) {
        if (!in.isNextToken('}')) {
          in.rollbackToken()
          var continue = true
          while (continue) {
            val key = in.readKeyAsString()
            key match {
              case KHRMaterialsEmissiveStrength.EXT      => ext.set(key, khrEmissiveStrengthCodec.decodeValue(in, null.asInstanceOf[KHRMaterialsEmissiveStrength]))
              case KHRMaterialsIOR.EXT                   => ext.set(key, khrIORCodec.decodeValue(in, null.asInstanceOf[KHRMaterialsIOR]))
              case KHRMaterialsIridescence.EXT           => ext.set(key, khrIridescenceCodec.decodeValue(in, null.asInstanceOf[KHRMaterialsIridescence]))
              case KHRMaterialsPBRSpecularGlossiness.EXT => ext.set(key, khrPBRSpecGlossCodec.decodeValue(in, null.asInstanceOf[KHRMaterialsPBRSpecularGlossiness]))
              case KHRMaterialsSpecular.EXT              => ext.set(key, khrSpecularCodec.decodeValue(in, null.asInstanceOf[KHRMaterialsSpecular]))
              case KHRMaterialsTransmission.EXT          => ext.set(key, khrTransmissionCodec.decodeValue(in, null.asInstanceOf[KHRMaterialsTransmission]))
              case KHRMaterialsUnlit.EXT                 => ext.set(key, khrUnlitCodec.decodeValue(in, null.asInstanceOf[KHRMaterialsUnlit]))
              case KHRMaterialsVolume.EXT                => ext.set(key, khrVolumeCodec.decodeValue(in, null.asInstanceOf[KHRMaterialsVolume]))
              case KHRTextureTransform.EXT               => ext.set(key, khrTextureTransformCodec.decodeValue(in, null.asInstanceOf[KHRTextureTransform]))
              case KHRLightsPunctual.EXT                 =>
                // KHR_lights_punctual appears at root level (GLTFLights with "lights" array)
                // and at node level (GLTFLightNode with "light" int). Both use same extension name.
                // We probe the first key to determine which type, using raw JSON fallback.
                val rawJson = jsonCodec.decodeValue(in, null.asInstanceOf[Json])
                if (rawJson != null) {
                  // Store raw JSON and let get[T] return it via cast — the caller knows which type to expect
                  ext.set(key, rawJson.asInstanceOf[AnyRef])
                }
              case _ =>
                // Unknown extension — store raw JSON for forward compatibility
                val rawJson = jsonCodec.decodeValue(in, null.asInstanceOf[Json])
                if (rawJson != null) ext.set(key, rawJson.asInstanceOf[AnyRef])
            }
            continue = in.isNextToken(',')
          }
        }
      } else in.readNullOrTokenError(ext, '{')
      ext
    }

    override def encodeValue(x: GLTFExtensions, out: JsonWriter): Unit =
      out.writeNull()

    override def nullValue: GLTFExtensions = null.asInstanceOf[GLTFExtensions] // @nowarn — Nullable wraps this
  }

  // ── GLTFExtras codec (raw Json AST) ─────────────────────────────────

  given gltfExtrasCodec(using jsonCodec: JsonValueCodec[Json]): JsonValueCodec[GLTFExtras] = new JsonValueCodec[GLTFExtras] {
    override def decodeValue(in: JsonReader, default: GLTFExtras): GLTFExtras = {
      val extras = if (default != null) default else new GLTFExtras()
      val json   = jsonCodec.decodeValue(in, null.asInstanceOf[Json])
      if (json != null) extras.value = Nullable(json)
      extras
    }

    override def encodeValue(x: GLTFExtras, out: JsonWriter): Unit =
      out.writeNull()

    override def nullValue: GLTFExtras = null.asInstanceOf[GLTFExtras] // @nowarn — Nullable wraps this
  }

  // ── GLTFMorphTarget codec (HashMap[String, Int]) ────────────────────

  given gltfMorphTargetCodec: JsonValueCodec[GLTFMorphTarget] = new JsonValueCodec[GLTFMorphTarget] {
    override def decodeValue(in: JsonReader, default: GLTFMorphTarget): GLTFMorphTarget = {
      val target = if (default != null) default else new GLTFMorphTarget()
      if (in.isNextToken('{')) {
        if (!in.isNextToken('}')) {
          in.rollbackToken()
          target.put(in.readKeyAsString(), in.readInt())
          while (in.isNextToken(',')) target.put(in.readKeyAsString(), in.readInt())
        }
      } else in.readNullOrTokenError(target, '{')
      target
    }

    override def encodeValue(x: GLTFMorphTarget, out: JsonWriter): Unit = {
      out.writeObjectStart()
      x.foreach { case (k, v) => out.writeKey(k); out.writeVal(v) }
      out.writeObjectEnd()
    }

    override def nullValue: GLTFMorphTarget = null.asInstanceOf[GLTFMorphTarget] // @nowarn — Nullable wraps this
  }

  // ── texture ─────────────────────────────────────────────────────────

  given gltfTextureInfoCodec: JsonValueCodec[GLTFTextureInfo] = new JsonValueCodec[GLTFTextureInfo] {
    override def decodeValue(in: JsonReader, default: GLTFTextureInfo): GLTFTextureInfo =
      readFields(in, if (default != null) default else new GLTFTextureInfo()) { (key, obj, in) =>
        key match {
          case "index"    => obj.index = readNullableInt(in, obj.index)
          case "texCoord" => obj.texCoord = in.readInt()
          case _          => if (!readObjectField(key, obj, in)) in.skip()
        }
      }
    override def encodeValue(x: GLTFTextureInfo, out: JsonWriter): Unit            = out.writeNull()
    override def nullValue:                                        GLTFTextureInfo = null.asInstanceOf[GLTFTextureInfo]
  }

  given gltfNormalTextureInfoCodec: JsonValueCodec[GLTFNormalTextureInfo] = new JsonValueCodec[GLTFNormalTextureInfo] {
    override def decodeValue(in: JsonReader, default: GLTFNormalTextureInfo): GLTFNormalTextureInfo =
      readFields(in, if (default != null) default else new GLTFNormalTextureInfo()) { (key, obj, in) =>
        key match {
          case "index"    => obj.index = readNullableInt(in, obj.index)
          case "texCoord" => obj.texCoord = in.readInt()
          case "scale"    => obj.scale = in.readFloat()
          case _          => if (!readObjectField(key, obj, in)) in.skip()
        }
      }
    override def encodeValue(x: GLTFNormalTextureInfo, out: JsonWriter): Unit                  = out.writeNull()
    override def nullValue:                                              GLTFNormalTextureInfo = null.asInstanceOf[GLTFNormalTextureInfo]
  }

  given gltfOcclusionTextureInfoCodec: JsonValueCodec[GLTFOcclusionTextureInfo] = new JsonValueCodec[GLTFOcclusionTextureInfo] {
    override def decodeValue(in: JsonReader, default: GLTFOcclusionTextureInfo): GLTFOcclusionTextureInfo =
      readFields(in, if (default != null) default else new GLTFOcclusionTextureInfo()) { (key, obj, in) =>
        key match {
          case "index"    => obj.index = readNullableInt(in, obj.index)
          case "texCoord" => obj.texCoord = in.readInt()
          case "strength" => obj.strength = in.readFloat()
          case _          => if (!readObjectField(key, obj, in)) in.skip()
        }
      }
    override def encodeValue(x: GLTFOcclusionTextureInfo, out: JsonWriter): Unit                     = out.writeNull()
    override def nullValue:                                                 GLTFOcclusionTextureInfo = null.asInstanceOf[GLTFOcclusionTextureInfo]
  }

  given gltfImageCodec: JsonValueCodec[GLTFImage] = new JsonValueCodec[GLTFImage] {
    override def decodeValue(in: JsonReader, default: GLTFImage): GLTFImage =
      readFields(in, if (default != null) default else new GLTFImage()) { (key, obj, in) =>
        key match {
          case "uri"        => obj.uri = readNullableString(in, obj.uri)
          case "mimeType"   => obj.mimeType = readNullableString(in, obj.mimeType)
          case "bufferView" => obj.bufferView = readNullableInt(in, obj.bufferView)
          case _            => if (!readEntityField(key, obj, in)) in.skip()
        }
      }
    override def encodeValue(x: GLTFImage, out: JsonWriter): Unit      = out.writeNull()
    override def nullValue:                                  GLTFImage = null.asInstanceOf[GLTFImage]
  }

  given gltfSamplerCodec: JsonValueCodec[GLTFSampler] = new JsonValueCodec[GLTFSampler] {
    override def decodeValue(in: JsonReader, default: GLTFSampler): GLTFSampler =
      readFields(in, if (default != null) default else new GLTFSampler()) { (key, obj, in) =>
        key match {
          case "minFilter" => obj.minFilter = readNullableInt(in, obj.minFilter)
          case "magFilter" => obj.magFilter = readNullableInt(in, obj.magFilter)
          case "wrapS"     => obj.wrapS = readNullableInt(in, obj.wrapS)
          case "wrapT"     => obj.wrapT = readNullableInt(in, obj.wrapT)
          case _           => if (!readEntityField(key, obj, in)) in.skip()
        }
      }
    override def encodeValue(x: GLTFSampler, out: JsonWriter): Unit        = out.writeNull()
    override def nullValue:                                    GLTFSampler = null.asInstanceOf[GLTFSampler]
  }

  given gltfTextureCodec: JsonValueCodec[GLTFTexture] = new JsonValueCodec[GLTFTexture] {
    override def decodeValue(in: JsonReader, default: GLTFTexture): GLTFTexture =
      readFields(in, if (default != null) default else new GLTFTexture()) { (key, obj, in) =>
        key match {
          case "source"  => obj.source = readNullableInt(in, obj.source)
          case "sampler" => obj.sampler = readNullableInt(in, obj.sampler)
          case _         => if (!readEntityField(key, obj, in)) in.skip()
        }
      }
    override def encodeValue(x: GLTFTexture, out: JsonWriter): Unit        = out.writeNull()
    override def nullValue:                                    GLTFTexture = null.asInstanceOf[GLTFTexture]
  }

  // ── data (accessor, buffer) ─────────────────────────────────────────

  given gltfAccessorSparseIndicesCodec: JsonValueCodec[GLTFAccessorSparseIndices] = new JsonValueCodec[GLTFAccessorSparseIndices] {
    override def decodeValue(in: JsonReader, default: GLTFAccessorSparseIndices): GLTFAccessorSparseIndices =
      readFields(in, if (default != null) default else new GLTFAccessorSparseIndices()) { (key, obj, in) =>
        key match {
          case "bufferView"    => obj.bufferView = in.readInt()
          case "byteOffset"    => obj.byteOffset = in.readInt()
          case "componentType" => obj.componentType = in.readInt()
          case _               => if (!readObjectField(key, obj, in)) in.skip()
        }
      }
    override def encodeValue(x: GLTFAccessorSparseIndices, out: JsonWriter): Unit                      = out.writeNull()
    override def nullValue:                                                  GLTFAccessorSparseIndices = null.asInstanceOf[GLTFAccessorSparseIndices]
  }

  given gltfAccessorSparseValuesCodec: JsonValueCodec[GLTFAccessorSparseValues] = new JsonValueCodec[GLTFAccessorSparseValues] {
    override def decodeValue(in: JsonReader, default: GLTFAccessorSparseValues): GLTFAccessorSparseValues =
      readFields(in, if (default != null) default else new GLTFAccessorSparseValues()) { (key, obj, in) =>
        key match {
          case "bufferView" => obj.bufferView = in.readInt()
          case "byteOffset" => obj.byteOffset = in.readInt()
          case _            => if (!readObjectField(key, obj, in)) in.skip()
        }
      }
    override def encodeValue(x: GLTFAccessorSparseValues, out: JsonWriter): Unit                     = out.writeNull()
    override def nullValue:                                                 GLTFAccessorSparseValues = null.asInstanceOf[GLTFAccessorSparseValues]
  }

  given gltfAccessorSparseCodec: JsonValueCodec[GLTFAccessorSparse] = new JsonValueCodec[GLTFAccessorSparse] {
    override def decodeValue(in: JsonReader, default: GLTFAccessorSparse): GLTFAccessorSparse =
      readFields(in, if (default != null) default else new GLTFAccessorSparse()) { (key, obj, in) =>
        key match {
          case "count"   => obj.count = in.readInt()
          case "indices" => obj.indices = readNullableObj[GLTFAccessorSparseIndices](in, obj.indices)
          case "values"  => obj.values = readNullableObj[GLTFAccessorSparseValues](in, obj.values)
          case _         => if (!readObjectField(key, obj, in)) in.skip()
        }
      }
    override def encodeValue(x: GLTFAccessorSparse, out: JsonWriter): Unit               = out.writeNull()
    override def nullValue:                                           GLTFAccessorSparse = null.asInstanceOf[GLTFAccessorSparse]
  }

  given gltfAccessorCodec: JsonValueCodec[GLTFAccessor] = new JsonValueCodec[GLTFAccessor] {
    override def decodeValue(in: JsonReader, default: GLTFAccessor): GLTFAccessor =
      readFields(in, if (default != null) default else new GLTFAccessor()) { (key, obj, in) =>
        key match {
          case "bufferView"    => obj.bufferView = readNullableInt(in, obj.bufferView)
          case "normalized"    => obj.normalized = in.readBoolean()
          case "byteOffset"    => obj.byteOffset = in.readInt()
          case "componentType" => obj.componentType = in.readInt()
          case "count"         => obj.count = in.readInt()
          case "type"          => obj.`type` = readNullableString(in, obj.`type`)
          case "min"           => obj.min = readNullableFloatArray(in)
          case "max"           => obj.max = readNullableFloatArray(in)
          case "sparse"        => obj.sparse = readNullableObj[GLTFAccessorSparse](in, obj.sparse)
          case _               => if (!readEntityField(key, obj, in)) in.skip()
        }
      }
    override def encodeValue(x: GLTFAccessor, out: JsonWriter): Unit         = out.writeNull()
    override def nullValue:                                     GLTFAccessor = null.asInstanceOf[GLTFAccessor]
  }

  given gltfBufferCodec: JsonValueCodec[GLTFBuffer] = new JsonValueCodec[GLTFBuffer] {
    override def decodeValue(in: JsonReader, default: GLTFBuffer): GLTFBuffer =
      readFields(in, if (default != null) default else new GLTFBuffer()) { (key, obj, in) =>
        key match {
          case "uri"        => obj.uri = readNullableString(in, obj.uri)
          case "byteLength" => obj.byteLength = in.readInt()
          case _            => if (!readEntityField(key, obj, in)) in.skip()
        }
      }
    override def encodeValue(x: GLTFBuffer, out: JsonWriter): Unit       = out.writeNull()
    override def nullValue:                                   GLTFBuffer = null.asInstanceOf[GLTFBuffer]
  }

  given gltfBufferViewCodec: JsonValueCodec[GLTFBufferView] = new JsonValueCodec[GLTFBufferView] {
    override def decodeValue(in: JsonReader, default: GLTFBufferView): GLTFBufferView =
      readFields(in, if (default != null) default else new GLTFBufferView()) { (key, obj, in) =>
        key match {
          case "byteOffset" => obj.byteOffset = in.readInt()
          case "byteLength" => obj.byteLength = in.readInt()
          case "buffer"     => obj.buffer = readNullableInt(in, obj.buffer)
          case "byteStride" => obj.byteStride = readNullableInt(in, obj.byteStride)
          case "target"     => obj.target = readNullableInt(in, obj.target)
          case _            => if (!readEntityField(key, obj, in)) in.skip()
        }
      }
    override def encodeValue(x: GLTFBufferView, out: JsonWriter): Unit           = out.writeNull()
    override def nullValue:                                       GLTFBufferView = null.asInstanceOf[GLTFBufferView]
  }

  // ── material ────────────────────────────────────────────────────────

  given gltfPbrMetallicRoughnessCodec: JsonValueCodec[GLTFpbrMetallicRoughness] = new JsonValueCodec[GLTFpbrMetallicRoughness] {
    override def decodeValue(in: JsonReader, default: GLTFpbrMetallicRoughness): GLTFpbrMetallicRoughness =
      readFields(in, if (default != null) default else new GLTFpbrMetallicRoughness()) { (key, obj, in) =>
        key match {
          case "baseColorFactor"          => obj.baseColorFactor = readNullableFloatArray(in)
          case "metallicFactor"           => obj.metallicFactor = in.readFloat()
          case "roughnessFactor"          => obj.roughnessFactor = in.readFloat()
          case "baseColorTexture"         => obj.baseColorTexture = readNullableObj[GLTFTextureInfo](in, obj.baseColorTexture)
          case "metallicRoughnessTexture" => obj.metallicRoughnessTexture = readNullableObj[GLTFTextureInfo](in, obj.metallicRoughnessTexture)
          case _                          => if (!readObjectField(key, obj, in)) in.skip()
        }
      }
    override def encodeValue(x: GLTFpbrMetallicRoughness, out: JsonWriter): Unit                     = out.writeNull()
    override def nullValue:                                                 GLTFpbrMetallicRoughness = null.asInstanceOf[GLTFpbrMetallicRoughness]
  }

  given gltfMaterialCodec: JsonValueCodec[GLTFMaterial] = new JsonValueCodec[GLTFMaterial] {
    override def decodeValue(in: JsonReader, default: GLTFMaterial): GLTFMaterial =
      readFields(in, if (default != null) default else new GLTFMaterial()) { (key, obj, in) =>
        key match {
          case "emissiveFactor"       => obj.emissiveFactor = readNullableFloatArray(in)
          case "normalTexture"        => obj.normalTexture = readNullableObj[GLTFNormalTextureInfo](in, obj.normalTexture)
          case "occlusionTexture"     => obj.occlusionTexture = readNullableObj[GLTFOcclusionTextureInfo](in, obj.occlusionTexture)
          case "emissiveTexture"      => obj.emissiveTexture = readNullableObj[GLTFTextureInfo](in, obj.emissiveTexture)
          case "alphaMode"            => obj.alphaMode = readNullableString(in, obj.alphaMode)
          case "alphaCutoff"          => obj.alphaCutoff = readNullableFloat(in, obj.alphaCutoff)
          case "doubleSided"          => obj.doubleSided = readNullableBoolean(in, obj.doubleSided)
          case "pbrMetallicRoughness" => obj.pbrMetallicRoughness = readNullableObj[GLTFpbrMetallicRoughness](in, obj.pbrMetallicRoughness)
          case _                      => if (!readEntityField(key, obj, in)) in.skip()
        }
      }
    override def encodeValue(x: GLTFMaterial, out: JsonWriter): Unit         = out.writeNull()
    override def nullValue:                                     GLTFMaterial = null.asInstanceOf[GLTFMaterial]
  }

  // ── geometry ────────────────────────────────────────────────────────

  given gltfPrimitiveCodec: JsonValueCodec[GLTFPrimitive] = new JsonValueCodec[GLTFPrimitive] {
    override def decodeValue(in: JsonReader, default: GLTFPrimitive): GLTFPrimitive =
      readFields(in, if (default != null) default else new GLTFPrimitive()) { (key, obj, in) =>
        key match {
          case "attributes" => obj.attributes = readNullableStringIntMap(in)
          case "indices"    => obj.indices = readNullableInt(in, obj.indices)
          case "mode"       => obj.mode = readNullableInt(in, obj.mode)
          case "material"   => obj.material = readNullableInt(in, obj.material)
          case "targets"    => obj.targets = readNullableArray[GLTFMorphTarget](in)
          case _            => if (!readObjectField(key, obj, in)) in.skip()
        }
      }
    override def encodeValue(x: GLTFPrimitive, out: JsonWriter): Unit          = out.writeNull()
    override def nullValue:                                      GLTFPrimitive = null.asInstanceOf[GLTFPrimitive]
  }

  given gltfMeshCodec: JsonValueCodec[GLTFMesh] = new JsonValueCodec[GLTFMesh] {
    override def decodeValue(in: JsonReader, default: GLTFMesh): GLTFMesh =
      readFields(in, if (default != null) default else new GLTFMesh()) { (key, obj, in) =>
        key match {
          case "primitives" => obj.primitives = readNullableArray[GLTFPrimitive](in)
          case "weights"    => obj.weights = readNullableFloatArray(in)
          case _            => if (!readEntityField(key, obj, in)) in.skip()
        }
      }
    override def encodeValue(x: GLTFMesh, out: JsonWriter): Unit     = out.writeNull()
    override def nullValue:                                 GLTFMesh = null.asInstanceOf[GLTFMesh]
  }

  // ── camera ──────────────────────────────────────────────────────────

  given gltfOrthographicCodec: JsonValueCodec[GLTFOrthographic] = new JsonValueCodec[GLTFOrthographic] {
    override def decodeValue(in: JsonReader, default: GLTFOrthographic): GLTFOrthographic =
      readFields(in, if (default != null) default else new GLTFOrthographic()) { (key, obj, in) =>
        key match {
          case "znear" => obj.znear = readNullableFloat(in, obj.znear)
          case "zfar"  => obj.zfar = readNullableFloat(in, obj.zfar)
          case "xmag"  => obj.xmag = readNullableFloat(in, obj.xmag)
          case "ymag"  => obj.ymag = readNullableFloat(in, obj.ymag)
          case _       => if (!readObjectField(key, obj, in)) in.skip()
        }
      }
    override def encodeValue(x: GLTFOrthographic, out: JsonWriter): Unit             = out.writeNull()
    override def nullValue:                                         GLTFOrthographic = null.asInstanceOf[GLTFOrthographic]
  }

  given gltfPerspectiveCodec: JsonValueCodec[GLTFPerspective] = new JsonValueCodec[GLTFPerspective] {
    override def decodeValue(in: JsonReader, default: GLTFPerspective): GLTFPerspective =
      readFields(in, if (default != null) default else new GLTFPerspective()) { (key, obj, in) =>
        key match {
          case "yfov"        => obj.yfov = in.readFloat()
          case "znear"       => obj.znear = in.readFloat()
          case "aspectRatio" => obj.aspectRatio = readNullableFloat(in, obj.aspectRatio)
          case "zfar"        => obj.zfar = readNullableFloat(in, obj.zfar)
          case _             => if (!readObjectField(key, obj, in)) in.skip()
        }
      }
    override def encodeValue(x: GLTFPerspective, out: JsonWriter): Unit            = out.writeNull()
    override def nullValue:                                        GLTFPerspective = null.asInstanceOf[GLTFPerspective]
  }

  given gltfCameraCodec: JsonValueCodec[GLTFCamera] = new JsonValueCodec[GLTFCamera] {
    override def decodeValue(in: JsonReader, default: GLTFCamera): GLTFCamera =
      readFields(in, if (default != null) default else new GLTFCamera()) { (key, obj, in) =>
        key match {
          case "type"         => obj.`type` = readNullableString(in, obj.`type`)
          case "perspective"  => obj.perspective = readNullableObj[GLTFPerspective](in, obj.perspective)
          case "orthographic" => obj.orthographic = readNullableObj[GLTFOrthographic](in, obj.orthographic)
          case _              => if (!readEntityField(key, obj, in)) in.skip()
        }
      }
    override def encodeValue(x: GLTFCamera, out: JsonWriter): Unit       = out.writeNull()
    override def nullValue:                                   GLTFCamera = null.asInstanceOf[GLTFCamera]
  }

  // ── animation ───────────────────────────────────────────────────────

  given gltfAnimationTargetCodec: JsonValueCodec[GLTFAnimationTarget] = new JsonValueCodec[GLTFAnimationTarget] {
    override def decodeValue(in: JsonReader, default: GLTFAnimationTarget): GLTFAnimationTarget =
      readFields(in, if (default != null) default else new GLTFAnimationTarget()) { (key, obj, in) =>
        key match {
          case "node" => obj.node = readNullableInt(in, obj.node)
          case "path" => obj.path = readNullableString(in, obj.path)
          case _      => if (!readObjectField(key, obj, in)) in.skip()
        }
      }
    override def encodeValue(x: GLTFAnimationTarget, out: JsonWriter): Unit                = out.writeNull()
    override def nullValue:                                            GLTFAnimationTarget = null.asInstanceOf[GLTFAnimationTarget]
  }

  given gltfAnimationSamplerCodec: JsonValueCodec[GLTFAnimationSampler] = new JsonValueCodec[GLTFAnimationSampler] {
    override def decodeValue(in: JsonReader, default: GLTFAnimationSampler): GLTFAnimationSampler =
      readFields(in, if (default != null) default else new GLTFAnimationSampler()) { (key, obj, in) =>
        key match {
          case "input"         => obj.input = readNullableInt(in, obj.input)
          case "output"        => obj.output = readNullableInt(in, obj.output)
          case "interpolation" => obj.interpolation = readNullableString(in, obj.interpolation)
          case _               => if (!readObjectField(key, obj, in)) in.skip()
        }
      }
    override def encodeValue(x: GLTFAnimationSampler, out: JsonWriter): Unit                 = out.writeNull()
    override def nullValue:                                             GLTFAnimationSampler = null.asInstanceOf[GLTFAnimationSampler]
  }

  given gltfAnimationChannelCodec: JsonValueCodec[GLTFAnimationChannel] = new JsonValueCodec[GLTFAnimationChannel] {
    override def decodeValue(in: JsonReader, default: GLTFAnimationChannel): GLTFAnimationChannel =
      readFields(in, if (default != null) default else new GLTFAnimationChannel()) { (key, obj, in) =>
        key match {
          case "sampler" => obj.sampler = readNullableInt(in, obj.sampler)
          case "target"  => obj.target = readNullableObj[GLTFAnimationTarget](in, obj.target)
          case _         => if (!readObjectField(key, obj, in)) in.skip()
        }
      }
    override def encodeValue(x: GLTFAnimationChannel, out: JsonWriter): Unit                 = out.writeNull()
    override def nullValue:                                             GLTFAnimationChannel = null.asInstanceOf[GLTFAnimationChannel]
  }

  given gltfAnimationCodec: JsonValueCodec[GLTFAnimation] = new JsonValueCodec[GLTFAnimation] {
    override def decodeValue(in: JsonReader, default: GLTFAnimation): GLTFAnimation =
      readFields(in, if (default != null) default else new GLTFAnimation()) { (key, obj, in) =>
        key match {
          case "channels" => obj.channels = readNullableArray[GLTFAnimationChannel](in)
          case "samplers" => obj.samplers = readNullableArray[GLTFAnimationSampler](in)
          case _          => if (!readEntityField(key, obj, in)) in.skip()
        }
      }
    override def encodeValue(x: GLTFAnimation, out: JsonWriter): Unit          = out.writeNull()
    override def nullValue:                                      GLTFAnimation = null.asInstanceOf[GLTFAnimation]
  }

  // ── scene ───────────────────────────────────────────────────────────

  given gltfNodeCodec: JsonValueCodec[GLTFNode] = new JsonValueCodec[GLTFNode] {
    override def decodeValue(in: JsonReader, default: GLTFNode): GLTFNode =
      readFields(in, if (default != null) default else new GLTFNode()) { (key, obj, in) =>
        key match {
          case "children"    => obj.children = readNullableIntArray(in)
          case "matrix"      => obj.matrix = readNullableFloatArray(in)
          case "translation" => obj.translation = readNullableFloatArray(in)
          case "rotation"    => obj.rotation = readNullableFloatArray(in)
          case "scale"       => obj.scale = readNullableFloatArray(in)
          case "mesh"        => obj.mesh = readNullableInt(in, obj.mesh)
          case "camera"      => obj.camera = readNullableInt(in, obj.camera)
          case "skin"        => obj.skin = readNullableInt(in, obj.skin)
          case "weights"     => obj.weights = readNullableFloatArray(in)
          case _             => if (!readEntityField(key, obj, in)) in.skip()
        }
      }
    override def encodeValue(x: GLTFNode, out: JsonWriter): Unit     = out.writeNull()
    override def nullValue:                                 GLTFNode = null.asInstanceOf[GLTFNode]
  }

  given gltfSceneCodec: JsonValueCodec[GLTFScene] = new JsonValueCodec[GLTFScene] {
    override def decodeValue(in: JsonReader, default: GLTFScene): GLTFScene =
      readFields(in, if (default != null) default else new GLTFScene()) { (key, obj, in) =>
        key match {
          case "nodes" => obj.nodes = readNullableIntArray(in)
          case _       => if (!readEntityField(key, obj, in)) in.skip()
        }
      }
    override def encodeValue(x: GLTFScene, out: JsonWriter): Unit      = out.writeNull()
    override def nullValue:                                  GLTFScene = null.asInstanceOf[GLTFScene]
  }

  given gltfSkinCodec: JsonValueCodec[GLTFSkin] = new JsonValueCodec[GLTFSkin] {
    override def decodeValue(in: JsonReader, default: GLTFSkin): GLTFSkin =
      readFields(in, if (default != null) default else new GLTFSkin()) { (key, obj, in) =>
        key match {
          case "inverseBindMatrices" => obj.inverseBindMatrices = readNullableInt(in, obj.inverseBindMatrices)
          case "joints"              => obj.joints = readNullableIntArray(in)
          case "skeleton"            => obj.skeleton = readNullableInt(in, obj.skeleton)
          case _                     => if (!readEntityField(key, obj, in)) in.skip()
        }
      }
    override def encodeValue(x: GLTFSkin, out: JsonWriter): Unit     = out.writeNull()
    override def nullValue:                                 GLTFSkin = null.asInstanceOf[GLTFSkin]
  }

  // ── KHR extensions ──────────────────────────────────────────────────

  given gltfSpotLightCodec: JsonValueCodec[KHRLightsPunctual.GLTFSpotLight] = new JsonValueCodec[KHRLightsPunctual.GLTFSpotLight] {
    override def decodeValue(in: JsonReader, default: KHRLightsPunctual.GLTFSpotLight): KHRLightsPunctual.GLTFSpotLight =
      readFields(in, if (default != null) default else new KHRLightsPunctual.GLTFSpotLight()) { (key, obj, in) =>
        key match {
          case "innerConeAngle" => obj.innerConeAngle = in.readFloat()
          case "outerConeAngle" => obj.outerConeAngle = in.readFloat()
          case _                => in.skip()
        }
      }
    override def encodeValue(x: KHRLightsPunctual.GLTFSpotLight, out: JsonWriter): Unit                            = out.writeNull()
    override def nullValue:                                                        KHRLightsPunctual.GLTFSpotLight = null.asInstanceOf[KHRLightsPunctual.GLTFSpotLight]
  }

  given gltfLightCodec: JsonValueCodec[KHRLightsPunctual.GLTFLight] = new JsonValueCodec[KHRLightsPunctual.GLTFLight] {
    override def decodeValue(in: JsonReader, default: KHRLightsPunctual.GLTFLight): KHRLightsPunctual.GLTFLight =
      readFields(in, if (default != null) default else new KHRLightsPunctual.GLTFLight()) { (key, obj, in) =>
        key match {
          case "name"      => obj.name = in.readString(obj.name)
          case "color"     => obj.color = readNullableFloatArray(in).getOrElse(obj.color)
          case "intensity" => obj.intensity = in.readFloat()
          case "type"      => obj.`type` = readNullableString(in, obj.`type`)
          case "range"     => obj.range = readNullableFloat(in, obj.range)
          case "spot"      => obj.spot = readNullableObj[KHRLightsPunctual.GLTFSpotLight](in, obj.spot)
          case _           => if (!readObjectField(key, obj, in)) in.skip()
        }
      }
    override def encodeValue(x: KHRLightsPunctual.GLTFLight, out: JsonWriter): Unit                        = out.writeNull()
    override def nullValue:                                                    KHRLightsPunctual.GLTFLight = null.asInstanceOf[KHRLightsPunctual.GLTFLight]
  }

  given gltfLightsCodec: JsonValueCodec[KHRLightsPunctual.GLTFLights] = new JsonValueCodec[KHRLightsPunctual.GLTFLights] {
    override def decodeValue(in: JsonReader, default: KHRLightsPunctual.GLTFLights): KHRLightsPunctual.GLTFLights =
      readFields(in, if (default != null) default else new KHRLightsPunctual.GLTFLights()) { (key, obj, in) =>
        key match {
          case "lights" => obj.lights = readNullableArray[KHRLightsPunctual.GLTFLight](in)
          case _        => in.skip()
        }
      }
    override def encodeValue(x: KHRLightsPunctual.GLTFLights, out: JsonWriter): Unit                         = out.writeNull()
    override def nullValue:                                                     KHRLightsPunctual.GLTFLights = null.asInstanceOf[KHRLightsPunctual.GLTFLights]
  }

  given gltfLightNodeCodec: JsonValueCodec[KHRLightsPunctual.GLTFLightNode] = new JsonValueCodec[KHRLightsPunctual.GLTFLightNode] {
    override def decodeValue(in: JsonReader, default: KHRLightsPunctual.GLTFLightNode): KHRLightsPunctual.GLTFLightNode =
      readFields(in, if (default != null) default else new KHRLightsPunctual.GLTFLightNode()) { (key, obj, in) =>
        key match {
          case "light" => obj.light = readNullableInt(in, obj.light)
          case _       => in.skip()
        }
      }
    override def encodeValue(x: KHRLightsPunctual.GLTFLightNode, out: JsonWriter): Unit                            = out.writeNull()
    override def nullValue:                                                        KHRLightsPunctual.GLTFLightNode = null.asInstanceOf[KHRLightsPunctual.GLTFLightNode]
  }

  given khrEmissiveStrengthCodec: JsonValueCodec[KHRMaterialsEmissiveStrength] = new JsonValueCodec[KHRMaterialsEmissiveStrength] {
    override def decodeValue(in: JsonReader, default: KHRMaterialsEmissiveStrength): KHRMaterialsEmissiveStrength =
      readFields(in, if (default != null) default else new KHRMaterialsEmissiveStrength()) { (key, obj, in) =>
        key match { case "emissiveStrength" => obj.emissiveStrength = in.readFloat(); case _ => in.skip() }
      }
    override def encodeValue(x: KHRMaterialsEmissiveStrength, out: JsonWriter): Unit                         = out.writeNull()
    override def nullValue:                                                     KHRMaterialsEmissiveStrength = null.asInstanceOf[KHRMaterialsEmissiveStrength]
  }

  given khrIORCodec: JsonValueCodec[KHRMaterialsIOR] = new JsonValueCodec[KHRMaterialsIOR] {
    override def decodeValue(in: JsonReader, default: KHRMaterialsIOR): KHRMaterialsIOR =
      readFields(in, if (default != null) default else new KHRMaterialsIOR()) { (key, obj, in) =>
        key match { case "ior" => obj.ior = in.readFloat(); case _ => in.skip() }
      }
    override def encodeValue(x: KHRMaterialsIOR, out: JsonWriter): Unit            = out.writeNull()
    override def nullValue:                                        KHRMaterialsIOR = null.asInstanceOf[KHRMaterialsIOR]
  }

  given khrIridescenceCodec: JsonValueCodec[KHRMaterialsIridescence] = new JsonValueCodec[KHRMaterialsIridescence] {
    override def decodeValue(in: JsonReader, default: KHRMaterialsIridescence): KHRMaterialsIridescence =
      readFields(in, if (default != null) default else new KHRMaterialsIridescence()) { (key, obj, in) =>
        key match {
          case "iridescenceFactor"           => obj.iridescenceFactor = in.readFloat()
          case "iridescenceTexture"          => obj.iridescenceTexture = readNullableObj[GLTFTextureInfo](in, obj.iridescenceTexture)
          case "iridescenceIor"              => obj.iridescenceIor = in.readFloat()
          case "iridescenceThicknessMinimum" => obj.iridescenceThicknessMinimum = in.readFloat()
          case "iridescenceThicknessMaximum" => obj.iridescenceThicknessMaximum = in.readFloat()
          case "iridescenceThicknessTexture" => obj.iridescenceThicknessTexture = readNullableObj[GLTFTextureInfo](in, obj.iridescenceThicknessTexture)
          case _                             => in.skip()
        }
      }
    override def encodeValue(x: KHRMaterialsIridescence, out: JsonWriter): Unit                    = out.writeNull()
    override def nullValue:                                                KHRMaterialsIridescence = null.asInstanceOf[KHRMaterialsIridescence]
  }

  given khrPBRSpecGlossCodec: JsonValueCodec[KHRMaterialsPBRSpecularGlossiness] = new JsonValueCodec[KHRMaterialsPBRSpecularGlossiness] {
    override def decodeValue(in: JsonReader, default: KHRMaterialsPBRSpecularGlossiness): KHRMaterialsPBRSpecularGlossiness =
      readFields(in, if (default != null) default else new KHRMaterialsPBRSpecularGlossiness()) { (key, obj, in) =>
        key match {
          case "diffuseFactor"             => obj.diffuseFactor = readNullableFloatArray(in)
          case "specularFactor"            => obj.specularFactor = readNullableFloatArray(in)
          case "glossinessFactor"          => obj.glossinessFactor = in.readFloat()
          case "diffuseTexture"            => obj.diffuseTexture = readNullableObj[GLTFTextureInfo](in, obj.diffuseTexture)
          case "specularGlossinessTexture" => obj.specularGlossinessTexture = readNullableObj[GLTFTextureInfo](in, obj.specularGlossinessTexture)
          case _                           => in.skip()
        }
      }
    override def encodeValue(x: KHRMaterialsPBRSpecularGlossiness, out: JsonWriter): Unit                              = out.writeNull()
    override def nullValue:                                                          KHRMaterialsPBRSpecularGlossiness = null.asInstanceOf[KHRMaterialsPBRSpecularGlossiness]
  }

  given khrSpecularCodec: JsonValueCodec[KHRMaterialsSpecular] = new JsonValueCodec[KHRMaterialsSpecular] {
    override def decodeValue(in: JsonReader, default: KHRMaterialsSpecular): KHRMaterialsSpecular =
      readFields(in, if (default != null) default else new KHRMaterialsSpecular()) { (key, obj, in) =>
        key match {
          case "specularFactor"       => obj.specularFactor = in.readFloat()
          case "specularTexture"      => obj.specularTexture = readNullableObj[GLTFTextureInfo](in, obj.specularTexture)
          case "specularColorFactor"  => obj.specularColorFactor = readNullableFloatArray(in).getOrElse(obj.specularColorFactor)
          case "specularColorTexture" => obj.specularColorTexture = readNullableObj[GLTFTextureInfo](in, obj.specularColorTexture)
          case _                      => in.skip()
        }
      }
    override def encodeValue(x: KHRMaterialsSpecular, out: JsonWriter): Unit                 = out.writeNull()
    override def nullValue:                                             KHRMaterialsSpecular = null.asInstanceOf[KHRMaterialsSpecular]
  }

  given khrTransmissionCodec: JsonValueCodec[KHRMaterialsTransmission] = new JsonValueCodec[KHRMaterialsTransmission] {
    override def decodeValue(in: JsonReader, default: KHRMaterialsTransmission): KHRMaterialsTransmission =
      readFields(in, if (default != null) default else new KHRMaterialsTransmission()) { (key, obj, in) =>
        key match {
          case "transmissionFactor"  => obj.transmissionFactor = in.readFloat()
          case "transmissionTexture" => obj.transmissionTexture = readNullableObj[GLTFTextureInfo](in, obj.transmissionTexture)
          case _                     => in.skip()
        }
      }
    override def encodeValue(x: KHRMaterialsTransmission, out: JsonWriter): Unit                     = out.writeNull()
    override def nullValue:                                                 KHRMaterialsTransmission = null.asInstanceOf[KHRMaterialsTransmission]
  }

  given khrUnlitCodec: JsonValueCodec[KHRMaterialsUnlit] = new JsonValueCodec[KHRMaterialsUnlit] {
    override def decodeValue(in: JsonReader, default: KHRMaterialsUnlit): KHRMaterialsUnlit = {
      // KHR_materials_unlit is an empty object {}
      if (in.isNextToken('{')) { while (!in.isNextToken('}')) { in.readKeyAsString(); in.skip() } }
      else in.readNullOrTokenError(null.asInstanceOf[KHRMaterialsUnlit], '{')
      if (default != null) default else new KHRMaterialsUnlit()
    }
    override def encodeValue(x: KHRMaterialsUnlit, out: JsonWriter): Unit              = out.writeNull()
    override def nullValue:                                          KHRMaterialsUnlit = null.asInstanceOf[KHRMaterialsUnlit]
  }

  given khrVolumeCodec: JsonValueCodec[KHRMaterialsVolume] = new JsonValueCodec[KHRMaterialsVolume] {
    override def decodeValue(in: JsonReader, default: KHRMaterialsVolume): KHRMaterialsVolume =
      readFields(in, if (default != null) default else new KHRMaterialsVolume()) { (key, obj, in) =>
        key match {
          case "thicknessFactor"     => obj.thicknessFactor = in.readFloat()
          case "thicknessTexture"    => obj.thicknessTexture = readNullableObj[GLTFTextureInfo](in, obj.thicknessTexture)
          case "attenuationDistance" => obj.attenuationDistance = readNullableFloat(in, obj.attenuationDistance)
          case "attenuationColor"    => obj.attenuationColor = readNullableFloatArray(in).getOrElse(obj.attenuationColor)
          case _                     => in.skip()
        }
      }
    override def encodeValue(x: KHRMaterialsVolume, out: JsonWriter): Unit               = out.writeNull()
    override def nullValue:                                           KHRMaterialsVolume = null.asInstanceOf[KHRMaterialsVolume]
  }

  given khrTextureTransformCodec: JsonValueCodec[KHRTextureTransform] = new JsonValueCodec[KHRTextureTransform] {
    override def decodeValue(in: JsonReader, default: KHRTextureTransform): KHRTextureTransform =
      readFields(in, if (default != null) default else new KHRTextureTransform()) { (key, obj, in) =>
        key match {
          case "offset"   => obj.offset = readNullableFloatArray(in).getOrElse(obj.offset)
          case "rotation" => obj.rotation = in.readFloat()
          case "scale"    => obj.scale = readNullableFloatArray(in).getOrElse(obj.scale)
          case "texCoord" => obj.texCoord = readNullableInt(in, obj.texCoord)
          case _          => in.skip()
        }
      }
    override def encodeValue(x: KHRTextureTransform, out: JsonWriter): Unit                = out.writeNull()
    override def nullValue:                                            KHRTextureTransform = null.asInstanceOf[KHRTextureTransform]
  }

  // ── root ────────────────────────────────────────────────────────────

  given gltfAssetCodec: JsonValueCodec[GLTFAsset] = new JsonValueCodec[GLTFAsset] {
    override def decodeValue(in: JsonReader, default: GLTFAsset): GLTFAsset =
      readFields(in, if (default != null) default else new GLTFAsset()) { (key, obj, in) =>
        key match {
          case "generator"  => obj.generator = readNullableString(in, obj.generator)
          case "version"    => obj.version = readNullableString(in, obj.version)
          case "copyright"  => obj.copyright = readNullableString(in, obj.copyright)
          case "minVersion" => obj.minVersion = readNullableString(in, obj.minVersion)
          case _            => if (!readObjectField(key, obj, in)) in.skip()
        }
      }
    override def encodeValue(x: GLTFAsset, out: JsonWriter): Unit      = out.writeNull()
    override def nullValue:                                  GLTFAsset = null.asInstanceOf[GLTFAsset]
  }

  given gltfCodec: JsonValueCodec[GLTF] = new JsonValueCodec[GLTF] {
    override def decodeValue(in: JsonReader, default: GLTF): GLTF =
      readFields(in, if (default != null) default else new GLTF()) { (key, obj, in) =>
        key match {
          case "asset"              => obj.asset = readNullableObj[GLTFAsset](in, obj.asset)
          case "scene"              => obj.scene = in.readInt()
          case "scenes"             => obj.scenes = readNullableArray[GLTFScene](in)
          case "nodes"              => obj.nodes = readNullableArray[GLTFNode](in)
          case "cameras"            => obj.cameras = readNullableArray[GLTFCamera](in)
          case "meshes"             => obj.meshes = readNullableArray[GLTFMesh](in)
          case "images"             => obj.images = readNullableArray[GLTFImage](in)
          case "samplers"           => obj.samplers = readNullableArray[GLTFSampler](in)
          case "textures"           => obj.textures = readNullableArray[GLTFTexture](in)
          case "animations"         => obj.animations = readNullableArray[GLTFAnimation](in)
          case "skins"              => obj.skins = readNullableArray[GLTFSkin](in)
          case "accessors"          => obj.accessors = readNullableArray[GLTFAccessor](in)
          case "materials"          => obj.materials = readNullableArray[GLTFMaterial](in)
          case "bufferViews"        => obj.bufferViews = readNullableArray[GLTFBufferView](in)
          case "buffers"            => obj.buffers = readNullableArray[GLTFBuffer](in)
          case "extensionsUsed"     => obj.extensionsUsed = readNullableStringArray(in)
          case "extensionsRequired" => obj.extensionsRequired = readNullableStringArray(in)
          case _                    => if (!readObjectField(key, obj, in)) in.skip()
        }
      }
    override def encodeValue(x: GLTF, out: JsonWriter): Unit = out.writeNull()
    override def nullValue:                             GLTF = null.asInstanceOf[GLTF]
  }
}
