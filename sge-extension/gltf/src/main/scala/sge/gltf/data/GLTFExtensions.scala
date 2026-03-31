/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gltf
package data

import scala.collection.mutable.HashMap
import sge.utils.{ Json, Nullable }

/** Holds extension data from a GLTF JSON element. In the original Java, this implements Json.Serializable and uses reflection-based deserialization. In SGE we store the raw JSON and deserialize on
  * demand.
  *
  * TODO: Implement typed deserialization once GLTF JSON codecs are available.
  */
class GLTFExtensions {

  private var value:      Nullable[Json]          = Nullable.empty
  private val extentions: HashMap[String, AnyRef] = HashMap.empty

  def get[T <: AnyRef](tpe: Class[T], ext: String): Nullable[T] =
    extentions.get(ext) match {
      case Some(result) => Nullable(result.asInstanceOf[T])
      case scala.None   =>
        // TODO: deserialize from JSON when codecs are available
        Nullable.empty[T]
    }

  def set(ext: String, obj: AnyRef): Unit =
    extentions.put(ext, obj)

  /** Set the raw JSON value (called during parsing). */
  def setRawJson(json: Json): Unit =
    value = Nullable(json)
}
