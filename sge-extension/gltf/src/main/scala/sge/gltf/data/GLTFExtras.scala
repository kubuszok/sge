/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 20
 * Covenant-baseline-methods: GLTFExtras,value
 * Covenant-source-reference: net/mgsx/gltf/data/GLTFExtras.java
 * Covenant-verified: 2026-04-19
 */
package sge
package gltf
package data

import scala.collection.mutable.ArrayBuffer

import lowlevel.Nullable
import sge.utils.Json

/** Holds extra properties from a GLTF JSON element. In the original Java, this implements Json.Serializable; in SGE we store the raw JSON AST instead since SGE uses jsoniter-scala, not LibGDX
  * reflection-based Json.
  */
class GLTFExtras {

  /** The raw JSON value of the "extras" field. */
  var value: Nullable[Json] = Nullable.empty

  /** A new array of extra properties keys.
    *
    * GLTFExtras.java:25-31 iterates `value.child`/`entry.next` and collects each `entry.name`. Here `value` is the parsed JSON AST, so the extra-property names are the member keys of the enclosing
    * JSON object (a non-object or empty `value` yields an empty array — mirroring a JsonValue whose `child` is null).
    */
  def keys: ArrayBuffer[String] = {
    val result = ArrayBuffer.empty[String]
    value.foreach {
      case Json.Obj(obj) => obj.fields.foreach { case (name, _) => result += name }
      case _             => ()
    }
    result
  }

  /** A new array of extra properties.
    *
    * GLTFExtras.java:33-42 iterates `value.child`/`entry.next` and collects each `entry` JsonValue. Here that is the member values of the enclosing JSON object.
    */
  def entries: ArrayBuffer[Json] = {
    val result = ArrayBuffer.empty[Json]
    value.foreach {
      case Json.Obj(obj) => obj.fields.foreach { case (_, v) => result += v }
      case _             => ()
    }
    result
  }
}
