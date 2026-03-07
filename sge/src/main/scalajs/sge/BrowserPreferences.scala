/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backends-gwt/.../GwtPreferences.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: GwtPreferences -> BrowserPreferences
 *   Convention: uses window.localStorage via scalajs-dom
 *   Convention: type suffix in storage keys (b/i/l/f/s) preserved from GWT original
 *   Idiom: Scala Map, union types for get(), split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge

import scala.collection.mutable
import org.scalajs.dom

/** Browser preferences implementation backed by `window.localStorage`. Each preference set uses a key prefix to avoid collisions. Values are type-tagged with a single-character suffix: `b` (Boolean),
  * `i` (Int), `l` (Long), `f` (Float), `s` (String).
  */
class BrowserPreferences(name: String) extends Preferences {

  private val prefix: String                                                     = name + ":"
  private val values: mutable.Map[String, Boolean | Int | Long | Float | String] = mutable.Map.empty

  // Load existing values from localStorage
  locally {
    val storage = dom.window.localStorage
    try {
      var i = 0
      while (i < storage.length) {
        val storageKey = storage.key(i)
        if (storageKey != null && storageKey.startsWith(prefix)) {
          val value = storage.getItem(storageKey)
          if (value != null) {
            val key = storageKey.substring(prefix.length, storageKey.length - 1)
            values.put(key, toObject(storageKey, value))
          }
        }
        i += 1
      }
    } catch {
      case _: Exception => values.clear()
    }
  }

  private def toObject(storageKey: String, value: String): Boolean | Int | Long | Float | String =
    if (storageKey.endsWith("b")) java.lang.Boolean.parseBoolean(value)
    else if (storageKey.endsWith("i")) java.lang.Integer.parseInt(value)
    else if (storageKey.endsWith("l")) java.lang.Long.parseLong(value)
    else if (storageKey.endsWith("f")) java.lang.Float.parseFloat(value)
    else value

  private def toStorageKey(key: String, value: Boolean | Int | Long | Float | String): String = {
    val suffix = value match {
      case _: Boolean => "b"
      case _: Int     => "i"
      case _: Long    => "l"
      case _: Float   => "f"
      case _: String  => "s"
    }
    prefix + key + suffix
  }

  override def putBoolean(key: String, value: Boolean): Preferences = { values.put(key, value); this }

  override def putInteger(key: String, value: Int): Preferences = { values.put(key, value); this }

  override def putLong(key: String, value: Long): Preferences = { values.put(key, value); this }

  override def putFloat(key: String, value: Float): Preferences = { values.put(key, value); this }

  override def putString(key: String, value: String): Preferences = { values.put(key, value); this }

  override def put(vals: scala.collection.Map[String, Boolean | Int | Long | Float | String]): Preferences = {
    vals.foreach((k, v) => values.put(k, v))
    this
  }

  override def getBoolean(key: String): Boolean = values.get(key) match {
    case Some(v: Boolean) => v
    case _                => false
  }

  override def getInteger(key: String): Int = values.get(key) match {
    case Some(v: Int) => v
    case _            => 0
  }

  override def getLong(key: String): Long = values.get(key) match {
    case Some(v: Long) => v
    case _             => 0L
  }

  override def getFloat(key: String): Float = values.get(key) match {
    case Some(v: Float) => v
    case _              => 0f
  }

  override def getString(key: String): String = values.get(key) match {
    case Some(v: String) => v
    case _               => ""
  }

  override def getBoolean(key: String, defValue: Boolean): Boolean = values.get(key) match {
    case Some(v: Boolean) => v
    case _                => defValue
  }

  override def getInteger(key: String, defValue: Int): Int = values.get(key) match {
    case Some(v: Int) => v
    case _            => defValue
  }

  override def getLong(key: String, defValue: Long): Long = values.get(key) match {
    case Some(v: Long) => v
    case _             => defValue
  }

  override def getFloat(key: String, defValue: Float): Float = values.get(key) match {
    case Some(v: Float) => v
    case _              => defValue
  }

  override def getString(key: String, defValue: String): String = values.get(key) match {
    case Some(v: String) => v
    case _               => defValue
  }

  override def get(): scala.collection.Map[String, Boolean | Int | Long | Float | String] =
    values.toMap

  override def contains(key: String): Boolean = values.contains(key)

  override def clear(): Unit = values.clear()

  override def remove(key: String): Unit = values.remove(key)

  override def flush(): Unit = {
    val storage = dom.window.localStorage
    try {
      // remove all old values with this prefix
      val keysToRemove = mutable.ArrayBuffer.empty[String]
      var i            = 0
      while (i < storage.length) {
        val storageKey = storage.key(i)
        if (storageKey != null && storageKey.startsWith(prefix)) {
          keysToRemove += storageKey
        }
        i += 1
      }
      keysToRemove.foreach(storage.removeItem)

      // push new values to localStorage
      values.foreach { (key, value) =>
        val storageKey = toStorageKey(key, value)
        storage.setItem(storageKey, value.toString)
      }
    } catch {
      case e: Exception =>
        throw sge.utils.SgeError.InvalidInput("Couldn't flush preferences", Some(e))
    }
  }
}
