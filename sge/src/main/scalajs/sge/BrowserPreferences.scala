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
 *   Audited: 2026-03-08
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

  override def put(vals: scala.collection.mutable.Map[String, ?]): Preferences = {
    vals.foreach((k, v) => values.put(k, v.asInstanceOf[Boolean | Int | Long | Float | String]))
    this
  }

  override def getBoolean(key: String): Boolean = getBoolean(key, false)

  override def getInteger(key: String): Int = getInteger(key, 0)

  override def getLong(key: String): Long = getLong(key, 0L)

  override def getFloat(key: String): Float = getFloat(key, 0f)

  override def getString(key: String): String = getString(key, "")

  // String-parse-lenient reads, matching the reference DesktopPreferences (java.util.Properties,
  // which stores every value as a String and parses it on read) and the upstream GWT backend
  // (localStorage strings + a type suffix). A present value is parsed from its string form; the
  // declared runtime type is NOT consulted, so cross-type reads coerce identically on every
  // platform (e.g. getLong on an Int-stored key widens to the value; getInteger on an unparseable
  // value throws NumberFormatException exactly as on JVM). defValue is honoured only when the key
  // is absent — a present value always wins, mirroring DesktopPreferences.
  //
  // Residual (Scala.js only, extreme edge): a whole Float renders as "5" via toString (JVM: "5.0"),
  // so getInteger/getLong on a whole-Float-stored key parse successfully here whereas JVM throws.
  // A Scala.js Float.toString artifact, not a logic divergence; not worth custom float formatting.
  override def getBoolean(key: String, defValue: Boolean): Boolean = values.get(key) match {
    case Some(v) => java.lang.Boolean.parseBoolean(v.toString)
    case _       => defValue
  }

  override def getInteger(key: String, defValue: Int): Int = values.get(key) match {
    case Some(v) => Integer.parseInt(v.toString)
    case _       => defValue
  }

  override def getLong(key: String, defValue: Long): Long = values.get(key) match {
    case Some(v) => java.lang.Long.parseLong(v.toString)
    case _       => defValue
  }

  override def getFloat(key: String, defValue: Float): Float = values.get(key) match {
    case Some(v) => java.lang.Float.parseFloat(v.toString)
    case _       => defValue
  }

  override def getString(key: String, defValue: String): String = values.get(key) match {
    case Some(v) => v.toString
    case _       => defValue
  }

  override def get(): scala.collection.mutable.Map[String, ?] =
    scala.collection.mutable.Map.from(values)

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
