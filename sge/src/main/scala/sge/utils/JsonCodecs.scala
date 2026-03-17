/*
 * SGE - Scala Game Engine
 * copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (type aliases + extension for jsoniter-scala codec derivation)
 *   Convention: re-exports jsoniter-scala types so consumers don't import plokhotnyuk directly
 *   Idiom: split packages, extension method on FileHandle
 */
package sge
package utils

import com.github.plokhotnyuk.jsoniter_scala.core.{ JsonValueCodec, readFromStream }
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import hearth.kindlings.ubjsonderivation.UBJsonValueCodec
import hearth.kindlings.ubjsonderivation.internal.runtime.UBJsonDerivationUtils

/** Type alias for jsoniter-scala's codec. Consumers derive codecs with:
  * {{{
  *   given JsonCodec[MyType] = JsonCodec.make
  * }}}
  */
type JsonCodec[A] = JsonValueCodec[A]

/** Factory for deriving codecs at compile time. Delegates to jsoniter-scala's [[JsonCodecMaker]].
  *
  * Uses `inline def` so that (1) codecs are derived at compile time via macro expansion, and (2) the Scala.js linker doesn't trace into the `Provided`-scoped `jsoniter-scala-macros` module at link
  * time (which would fail with "Cannot access module for non-module JsonCodecMaker$").
  */
inline def JsonCodec: JsonCodecMaker.type = JsonCodecMaker

/** Re-export of kindlings' JSON AST type for polymorphic JSON fields. */
type Json = hearth.kindlings.jsoniterjson.Json

/** Companion for pattern matching and construction (`Json.Str`, `Json.Num`, etc.). */
val Json: hearth.kindlings.jsoniterjson.Json.type = hearth.kindlings.jsoniterjson.Json

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
    *   given JsonCodec[MyModel] = JsonCodec.make
    *   val model = fileHandle.readJson[MyModel]
    * }}}
    */
  def readJson[T](using codec: JsonCodec[T]): T = {
    val stream = fh.read()
    try readFromStream[T](stream)
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
