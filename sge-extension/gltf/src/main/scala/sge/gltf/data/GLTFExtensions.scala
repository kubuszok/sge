/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-source-reference: net/mgsx/gltf/data/GLTFExtensions.java
 * Covenant: full-port
 * Covenant-verified: 2026-04-11
 * Covenant-verified: 2026-04-11
 *
 * upstream-commit: 683054a88382f71e8472abbc1c29931277c1cf22
 */
package sge
package gltf
package data

import scala.collection.mutable.HashMap
import sge.utils.{ Json, Nullable }

/** Holds extension data from a GLTF JSON element. Known extensions (KHR_materials_*, KHR_lights_punctual, KHR_texture_transform) are deserialized into typed objects during JSON parsing. Unknown
  * extensions are stored as raw JSON for forward compatibility.
  */
class GLTFExtensions {

  private var value:      Nullable[Json]          = Nullable.empty
  private val extentions: HashMap[String, AnyRef] = HashMap.empty

  /** Returns the typed extension object for the given extension name, or empty if not present. Known extensions are pre-parsed during deserialization by [[GLTFCodecs.gltfExtensionsCodec]].
    */
  def get[T <: AnyRef](tpe: Class[T], ext: String): Nullable[T] =
    extentions.get(ext) match {
      case Some(result) => Nullable(result.asInstanceOf[T])
      case scala.None   => Nullable.empty[T]
    }

  def set(ext: String, obj: AnyRef): Unit =
    extentions.put(ext, obj)

  /** Set the raw JSON value (called during parsing for backward compatibility). */
  def setRawJson(json: Json): Unit =
    value = Nullable(json)
}
