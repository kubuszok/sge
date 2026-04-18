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
import scala.util.boundary
import scala.util.boundary.break

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

  /** Returns the old value associated with the specified key, or the specified default value. */
  def put(key: String, value: Int, defaultValue: Int): Int = {
    val lk       = key.toLowerCase
    val oldValue = map.getOrElse(lk, defaultValue)
    map.put(lk, value)
    oldValue
  }

  /** Puts keys with values in sequential pairs from the two arrays given, until either array is exhausted. */
  def putAll(keys: Array[String], values: Array[Int]): Unit = {
    val len = Math.min(keys.length, values.length)
    var i   = 0
    while (i < len) {
      val key = keys(i)
      if (key != null) put(key, values(i))
      i += 1
    }
  }

  def get(key: String, defaultValue: Int): Int =
    map.getOrElse(key.toLowerCase, defaultValue)

  /** Returns the key's current value and increments the stored value. If the key is not in the map, defaultValue + increment is put into the map and defaultValue is returned.
    */
  def getAndIncrement(key: String, defaultValue: Int, increment: Int): Int = {
    val lk = key.toLowerCase
    map.get(lk) match {
      case Some(oldValue) =>
        map.put(lk, oldValue + increment)
        oldValue
      case None =>
        map.put(lk, defaultValue + increment)
        defaultValue
    }
  }

  def containsKey(key: String): Boolean =
    map.contains(key.toLowerCase)

  /** Returns true if the specified value is in the map. Note this traverses the entire map and compares every value, which may be an expensive operation.
    */
  def containsValue(value: Int): Boolean =
    map.valuesIterator.contains(value)

  /** Returns the key for the specified value, or null if it is not in the map. Note this traverses the entire map and compares every value, which may be an expensive operation.
    */
  def findKey(value: Int): String = boundary {
    val it = map.iterator
    while (it.hasNext) {
      val (k, v) = it.next()
      if (v == value) break(k)
    }
    null: @annotation.nowarn("msg=null") // matches Java API returning null when not found
  }

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

  /** Returns an iterable over the values in the map. */
  def values: Iterable[Int] = map.values

  /** Simple 32-bit multiplicative hashing. Treats input as if uppercase. */
  def hashCodeIgnoreCase(data: CharSequence): Int =
    CaseInsensitiveIntMap.hashCodeIgnoreCase(data)

  override def hashCode(): Int = {
    var h = map.size
    map.foreachEntry { (key, value) =>
      h ^= CaseInsensitiveIntMap.hashCodeIgnoreCase(key) ^ value
    }
    h
  }

  override def equals(obj: Any): Boolean = obj match {
    case that: CaseInsensitiveIntMap =>
      if (that.size != this.size) false
      else {
        var equal = true
        map.foreachEntry { (key, value) =>
          if (equal) {
            if (!that.containsKey(key)) equal = false
            else if (that.get(key, 0) != value) equal = false
          }
        }
        equal
      }
    case _ => false
  }

  override def toString: String = toString(", ", braces = true)

  def toString(separator: String): String = toString(separator, braces = false)

  def toString(separator: String, braces: Boolean): String =
    if (map.isEmpty) {
      if (braces) "{}" else ""
    } else {
      val buffer = new StringBuilder(32)
      if (braces) buffer.append('{')
      var first = true
      map.foreachEntry { (key, value) =>
        if (!first) buffer.append(separator)
        buffer.append(key)
        buffer.append('=')
        buffer.append(value)
        first = false
      }
      if (braces) buffer.append('}')
      buffer.toString
    }
}

object CaseInsensitiveIntMap {

  def hashCodeIgnoreCase(data: CharSequence): Int = hashCodeIgnoreCase(data, 908697017)

  def hashCodeIgnoreCase(data: CharSequence, seedIn: Int): Int =
    if (data == null) 0
    else {
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
