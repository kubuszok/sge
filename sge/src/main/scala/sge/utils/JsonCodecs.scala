/*
 * SGE - Scala Game Engine
 * copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (type aliases + extension for jsoniter-scala codec derivation)
 *   Convention: re-exports jsoniter-scala types so consumers don't import plokhotnyuk directly
 *   Idiom: split packages, extension method on FileHandle
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 126
 * Covenant-baseline-methods: Json,JsonCodec,JsonObject,JsoniterConfig,UBJsonCodec,WriterConfig,bytes,readFromStream,readFromString,readJson,readUBJson,stream,writeToString,writeUBJson
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package utils

import com.github.plokhotnyuk.jsoniter_scala.core.{ JsonValueCodec, readFromStream as _readFromStream, readFromString as _readFromString, writeToString as _writeToString }
import hearth.kindlings.jsoniterderivation.KindlingsJsonValueCodec
import hearth.kindlings.ubjsonderivation.UBJsonValueCodec
import hearth.kindlings.ubjsonderivation.internal.runtime.UBJsonDerivationUtils

// Re-export kindlings Json codec given so consumers can `import sge.utils.given`
given JsonCodec[Json] = hearth.kindlings.jsoniterjson.codec.JsonCodec.jsonValueCodec

/** Type alias for jsoniter-scala's codec. Consumers derive codecs with:
  * {{{
  *   given JsonCodec[MyType] = JsonCodec.derive[MyType]
  * }}}
  */
type JsonCodec[A] = JsonValueCodec[A]

/** Factory for deriving codecs at compile time. Delegates to kindlings' [[KindlingsJsonValueCodec]].
  *
  * Uses `inline def` so that codecs are derived at compile time via macro expansion.
  */
inline def JsonCodec: KindlingsJsonValueCodec.type = KindlingsJsonValueCodec

/** Re-export of kindlings' `@fieldName` annotation for overriding JSON/UBJSON field names. */
type fieldName = hearth.kindlings.jsoniterderivation.annotations.fieldName

/** Re-export of kindlings' `@transientField` annotation for excluding fields from serialization. */
type transientField = hearth.kindlings.jsoniterderivation.annotations.transientField

/** Re-export of kindlings' `@stringified` annotation for numeric-to-string field encoding. */
type stringified = hearth.kindlings.jsoniterderivation.annotations.stringified

/** Re-export of kindlings' `JsoniterConfig` for customising codec derivation. */
type JsoniterConfig = hearth.kindlings.jsoniterderivation.JsoniterConfig
val JsoniterConfig: hearth.kindlings.jsoniterderivation.JsoniterConfig.type = hearth.kindlings.jsoniterderivation.JsoniterConfig

/** Default codec config: use Scala default values for absent fields, omit None/empty/default on write. Matches jsoniter-scala-macros default behaviour.
  */
given JsoniterConfig = hearth.kindlings.jsoniterderivation.JsoniterConfig.default.withTransientDefault.withTransientNone.withTransientEmpty

/** Re-export of jsoniter-scala's `WriterConfig` for customising JSON serialization. */
type WriterConfig = com.github.plokhotnyuk.jsoniter_scala.core.WriterConfig
val WriterConfig: com.github.plokhotnyuk.jsoniter_scala.core.WriterConfig.type = com.github.plokhotnyuk.jsoniter_scala.core.WriterConfig

/** Serialize a value to a JSON string. Inline to preserve macro scope for Scala.js. */
inline def writeToString[A](value: A)(using codec: JsonCodec[A]): String = _writeToString[A](value)(using codec)

/** Serialize a value to a JSON string with custom writer config. Inline to preserve macro scope for Scala.js. */
inline def writeToString[A](value: A, config: WriterConfig)(using codec: JsonCodec[A]): String = _writeToString[A](value, config)(using codec)

/** Deserialize a JSON string to a typed value. Inline to preserve macro scope for Scala.js. */
inline def readFromString[A](json: String)(using codec: JsonCodec[A]): A = _readFromString[A](json)(using codec)

/** Deserialize a JSON input stream to a typed value. Inline to preserve macro scope for Scala.js. */
inline def readFromStream[A](stream: java.io.InputStream)(using codec: JsonCodec[A]): A = _readFromStream[A](stream)(using codec)

/** Re-export of kindlings' JSON AST type for polymorphic JSON fields. */
type Json = hearth.kindlings.jsoniterjson.Json

/** Companion for pattern matching and construction (`Json.Str`, `Json.Num`, etc.). */
val Json: hearth.kindlings.jsoniterjson.Json.type = hearth.kindlings.jsoniterjson.Json

/** Re-export of kindlings' `JsonObject` for building JSON objects from key-value pairs. */
type JsonObject = hearth.kindlings.jsoniterjson.JsonObject
val JsonObject: hearth.kindlings.jsoniterjson.JsonObject.type = hearth.kindlings.jsoniterjson.JsonObject

/** Type alias for kindlings' UBJSON codec. Consumers derive codecs with:
  * {{{
  *   given UBJsonCodec[MyType] = UBJsonCodec.derive[MyType]
  * }}}
  */
type UBJsonCodec[A] = UBJsonValueCodec[A]

/** Factory for deriving UBJSON codecs at compile time. */
val UBJsonCodec: UBJsonValueCodec.type = UBJsonValueCodec

extension (fh: sge.files.FileHandle) {

  /** Decodes JSON content of this file directly into a typed value.
    *
    * {{{
    *   given JsonCodec[MyModel] = JsonCodec.derive[MyModel]
    *   val model = fileHandle.readJson[MyModel]
    * }}}
    */
  def readJson[T](using codec: JsonCodec[T]): T = {
    val stream = fh.read()
    try _readFromStream[T](stream)
    finally stream.close()
  }

  /** Decodes UBJSON (Universal Binary JSON) content of this file directly into a typed value.
    *
    * {{{
    *   given UBJsonCodec[MyModel] = UBJsonCodec.derive[MyModel]
    *   val model = fileHandle.readUBJson[MyModel]
    * }}}
    */
  def readUBJson[T](using codec: UBJsonCodec[T]): T = {
    val bytes = fh.readBytes()
    UBJsonDerivationUtils.readFromBytes[T](bytes)(codec)
  }

  /** Encodes a typed value as UBJSON (Universal Binary JSON) and writes it to this file.
    *
    * {{{
    *   given UBJsonCodec[MyModel] = UBJsonCodec.derive[MyModel]
    *   fileHandle.writeUBJson(model)
    * }}}
    */
  def writeUBJson[T](value: T)(using codec: UBJsonCodec[T]): Unit = {
    val bytes = UBJsonDerivationUtils.writeToBytes[T](value)(codec)
    fh.writeBytes(bytes, false)
  }
}
