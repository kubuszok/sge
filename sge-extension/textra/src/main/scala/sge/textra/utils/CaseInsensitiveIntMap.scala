/*
 * Ported from TextraTypist - https://github.com/tommyettinger/textratypist
 * Original source: com/github/tommyettinger/textra/utils/CaseInsensitiveIntMap.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package textra
package utils

import scala.collection.mutable.HashMap

/** A case-insensitive map from Strings to ints. Uses Scala's mutable HashMap internally with lowercased keys for case-insensitive lookup.
  */
class CaseInsensitiveIntMap {

  private val map = new HashMap[String, Int]()

  def this(keys: Array[String], values: Array[Int]) = {
    this()
    val len = Math.min(keys.length, values.length)
    var i   = 0
    while (i < len) {
      if (keys(i) != null) put(keys(i), values(i))
      i += 1
    }
  }

  def put(key: String, value: Int): Unit =
    map.put(key.toLowerCase, value)

  def get(key: String, defaultValue: Int): Int =
    map.getOrElse(key.toLowerCase, defaultValue)

  def containsKey(key: String): Boolean =
    map.contains(key.toLowerCase)

  def remove(key: String, defaultValue: Int): Int =
    map.remove(key.toLowerCase).getOrElse(defaultValue)

  def size:     Int     = map.size
  def isEmpty:  Boolean = map.isEmpty
  def notEmpty: Boolean = map.nonEmpty
  def clear():  Unit    = map.clear()

  def foreachEntry(f: (String, Int) => Unit): Unit =
    map.foreachEntry(f)

  def ensureCapacity(capacity: Int): Unit = { val _ = capacity }

  def putAll(other: CaseInsensitiveIntMap): Unit =
    other.foreachEntry((k, v) => put(k, v))

  def keys: Iterable[String] = map.keys

  /** Simple 32-bit multiplicative hashing. Treats input as if uppercase. */
  def hashCodeIgnoreCase(data: CharSequence): Int =
    CaseInsensitiveIntMap.hashCodeIgnoreCase(data)
}

object CaseInsensitiveIntMap {

  def hashCodeIgnoreCase(data: CharSequence): Int = hashCodeIgnoreCase(data, 908697017)

  def hashCodeIgnoreCase(data: CharSequence, seedIn: Int): Int = {
    if (data == null) return 0
    val len  = data.length()
    var seed = seedIn ^ len
    var p    = 0
    while (p < len) {
      seed = -594347645 * (seed + Character.toUpperCase(data.charAt(p)))
      p += 1
    }
    seed ^ (seed << 27 | seed >>> 5) ^ (seed << 9 | seed >>> 23)
  }
}
