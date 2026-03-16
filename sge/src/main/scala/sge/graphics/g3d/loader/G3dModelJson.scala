/*
 * SGE - Scala Game Engine
 * copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (JSON DTO case classes for .g3dj format)
 *   Convention: mirrors .g3dj JSON structure for jsoniter-scala codec derivation
 *   Idiom: split packages, final case class, Option for optional fields
 */
package sge
package graphics
package g3d
package loader

import com.github.plokhotnyuk.jsoniter_scala.macros.{ CodecMakerConfig, named }
import hearth.kindlings.ubjsonderivation.annotations.fieldName
import sge.utils.{ JsonCodec, UBJsonCodec }

/** JSON DTO case classes for the .g3dj 3D model format (LibGDX G3D).
  *
  * These match the raw JSON structure exactly so jsoniter-scala can derive codecs. The loader transforms these DTOs into the mutable `ModelData` domain objects.
  */
final case class G3dModelJson(
  version:    List[Short],
  id:         String = "",
  meshes:     List[G3dMeshJson] = Nil,
  materials:  List[G3dMaterialJson] = Nil,
  nodes:      List[G3dNodeJson] = Nil,
  animations: List[G3dAnimationJson] = Nil
)

final case class G3dMeshJson(
  id:         String = "",
  attributes: List[String],
  vertices:   List[Float],
  parts:      List[G3dMeshPartJson]
)

final case class G3dMeshPartJson(
  id:                                    String,
  @named("type") @fieldName("type") tpe: String,
  indices:                               List[Short]
)

final case class G3dMaterialJson(
  id:         String,
  diffuse:    Option[List[Float]] = None,
  ambient:    Option[List[Float]] = None,
  emissive:   Option[List[Float]] = None,
  specular:   Option[List[Float]] = None,
  reflection: Option[List[Float]] = None,
  shininess:  Float = 0f,
  opacity:    Float = 1f,
  textures:   List[G3dTextureJson] = Nil
)

final case class G3dTextureJson(
  id:                                    String,
  filename:                              String,
  uvTranslation:                         Option[List[Float]] = None,
  uvScaling:                             Option[List[Float]] = None,
  @named("type") @fieldName("type") tpe: String
)

final case class G3dNodeJson(
  id:          String,
  translation: Option[List[Float]] = None,
  rotation:    Option[List[Float]] = None,
  scale:       Option[List[Float]] = None,
  mesh:        Option[String] = None,
  parts:       List[G3dNodePartJson] = Nil,
  children:    List[G3dNodeJson] = Nil
)

final case class G3dNodePartJson(
  meshpartid: String,
  materialid: String,
  bones:      List[G3dBoneJson] = Nil
)

final case class G3dBoneJson(
  node:        String,
  translation: Option[List[Float]] = None,
  rotation:    Option[List[Float]] = None,
  scale:       Option[List[Float]] = None
)

final case class G3dAnimationJson(
  id:    String = "",
  bones: List[G3dAnimBoneJson] = Nil
)

/** A single bone's animation data. Supports both v0.1 (combined keyframes) and v0.2 (split channels). */
final case class G3dAnimBoneJson(
  boneId:      String,
  keyframes:   Option[List[G3dKeyframeV1Json]] = None,
  translation: Option[List[G3dKeyframeV2Json]] = None,
  rotation:    Option[List[G3dKeyframeV2Json]] = None,
  scaling:     Option[List[G3dKeyframeV2Json]] = None
)

/** v0.1 keyframe: combined translation/rotation/scale at a given time. */
final case class G3dKeyframeV1Json(
  keytime:     Float = 0f,
  translation: Option[List[Float]] = None,
  rotation:    Option[List[Float]] = None,
  scale:       Option[List[Float]] = None
)

/** v0.2 keyframe: single value at a given time (used for split channels). */
final case class G3dKeyframeV2Json(
  keytime: Float = 0f,
  value:   List[Float]
)

object G3dModelJson {
  given codec: sge.utils.JsonCodec[G3dModelJson] = JsonCodec.make(
    CodecMakerConfig.withAllowRecursiveTypes(true)
  )

  given ubJsonCodec: sge.utils.UBJsonCodec[G3dModelJson] = UBJsonCodec.derive[G3dModelJson]
}
