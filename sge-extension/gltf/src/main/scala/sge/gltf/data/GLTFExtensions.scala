/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 95
 * Covenant-baseline-methods: GLTFExtensions,decode,decoders,extentions,get,rawEntries,rawValues,registerDecoder,set,setRaw,typedEntries
 * Covenant-source-reference: net/mgsx/gltf/data/GLTFExtensions.java
 * Covenant-verified: 2026-06-12
 *
 * upstream-commit: 683054a88382f71e8472abbc1c29931277c1cf22
 */
package sge
package gltf
package data

import scala.collection.mutable.HashMap
import lowlevel.Nullable
import sge.utils.Json

/** Holds extension data from a GLTF JSON element. Known extensions (KHR_materials_*, KHR_lights_punctual, KHR_texture_transform) are deserialized into typed objects during JSON parsing. Unknown
  * extensions are stored as raw JSON for forward compatibility.
  */
class GLTFExtensions {

  /** Raw JSON AST per extension name, mirroring the original Java `JsonValue value` (net/mgsx/gltf/data/GLTFExtensions.java:13). Some extensions (notably KHR_lights_punctual) are stored raw and
    * parsed lazily on [[get]], because the very same extension name maps to a different type depending on the call site (GLTFLights at the GLTF root, GLTFLightNode at a node).
    */
  private val rawValues:  HashMap[String, Json]   = HashMap.empty
  private val extentions: HashMap[String, AnyRef] = HashMap.empty

  /** Returns the typed extension object for the given extension name, or empty if not present.
    *
    * Faithful port of net/mgsx/gltf/data/GLTFExtensions.java:28-36: known extensions pre-parsed during deserialization are returned from the typed cache; otherwise, if a raw JSON value was stored for
    * this extension, it is lazily parsed into the REQUESTED type (`tpe`) via the decoder registered in [[GLTFExtensions.registerDecoder]] and cached, so the same extension name yields different types
    * per call site (GLTFLights vs GLTFLightNode).
    */
  def get[T <: AnyRef](tpe: Class[T], ext: String): Nullable[T] =
    extentions.get(ext) match {
      case Some(result) => Nullable(result.asInstanceOf[T])
      case scala.None   =>
        rawValues.get(ext) match {
          case Some(raw) =>
            GLTFExtensions.decode(tpe, raw) match {
              case Some(parsed) =>
                // Mirror Java GLTFExtensions.java:30-34: once the raw JsonValue is parsed into the
                // requested type it is cached in `extentions`, and Java's write() (16-21) only ever
                // iterates `extentions` — the captured `value` map is never re-written. So we drop
                // the raw entry on a successful parse; otherwise [[rawEntries]] and [[typedEntries]]
                // would both emit this extension name and produce a duplicate object key.
                rawValues.remove(ext)
                extentions.put(ext, parsed)
                Nullable(parsed.asInstanceOf[T])
              case scala.None => Nullable.empty[T]
            }
          case scala.None => Nullable.empty[T]
        }
    }

  def set(ext: String, obj: AnyRef): Unit =
    extentions.put(ext, obj)

  /** Stores the raw JSON value for an extension, to be parsed lazily into the requested type on [[get]]. Mirrors the original `read(Json, JsonValue)` storing the whole JsonValue
    * (net/mgsx/gltf/data/GLTFExtensions.java:23-26), except keyed per extension name so a single element's extensions can each be parsed independently.
    */
  def setRaw(ext: String, json: Json): Unit =
    rawValues.put(ext, json)

  /** Iterates the typed extension entries in insertion-independent order. Used by [[sge.gltf.data.GLTFCodecs.gltfExtensionsCodec]] when re-encoding, mirroring the original `write(Json)` which writes
    * every entry of the `extentions` map (net/mgsx/gltf/data/GLTFExtensions.java:16-21).
    */
  def typedEntries: Iterator[(String, AnyRef)] =
    extentions.iterator

  /** Iterates the raw (unparsed) extension entries, so re-encoding can round-trip extensions that were stored raw (e.g. KHR_lights_punctual) and never lazily parsed. */
  def rawEntries: Iterator[(String, Json)] =
    rawValues.iterator
}

object GLTFExtensions {

  /** Registry of decoders that turn a raw [[Json]] AST into a typed extension object, keyed by the target class. Populated by [[sge.gltf.data.GLTFCodecs]] (which owns the jsoniter codecs) so that
    * [[GLTFExtensions]] need not depend on the codecs object — mirroring the original Java `json.readValue(type, value.get(ext))` reflection (net/mgsx/gltf/data/GLTFExtensions.java:32), but without
    * runtime reflection so the same code runs on Scala.js and Scala Native.
    */
  private val decoders: HashMap[Class[?], Json => AnyRef] = HashMap.empty

  /** Registers a decoder for the given target class. Called from [[sge.gltf.data.GLTFCodecs]] object initialisation. */
  def registerDecoder[T <: AnyRef](tpe: Class[T], decoder: Json => T): Unit =
    decoders.put(tpe, decoder.asInstanceOf[Json => AnyRef])

  /** Decodes a raw [[Json]] AST into the requested type using the registered decoder, or empty if no decoder is registered for that type. */
  private def decode[T <: AnyRef](tpe: Class[T], raw: Json): Option[AnyRef] =
    decoders.get(tpe).map(_(raw))
}
