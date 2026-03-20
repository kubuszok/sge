/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backend-headless/.../HeadlessPreferences.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: HeadlessPreferences -> DesktopPreferences (reused by desktop backend)
 *   Convention: Java Properties XML storage; put() uses Scala 3 union type Map; get() returns union-typed Map
 *   Idiom: split packages; StreamUtils.closeQuietly for resource safety
 *   Audited: 2026-03-05
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package files

import java.io.{ BufferedInputStream, BufferedOutputStream, File }
import java.util.Properties
import sge.utils.StreamUtils

/** A [[sge.Preferences]] implementation backed by `java.util.Properties` with XML persistence. Used by headless and desktop backends.
  */
class DesktopPreferences(fileHandle: FileHandle) extends sge.Preferences {

  def this(name: String, directory: String, externalStoragePath: String) = {
    this(DesktopFileHandle(new File(directory, name), FileType.External, externalStoragePath))
  }

  private val properties: Properties = new Properties()

  // Load existing preferences if the file exists
  if (fileHandle.exists()) {
    try {
      val in = new BufferedInputStream(fileHandle.read())
      try properties.loadFromXML(in)
      finally StreamUtils.closeQuietly(in)
    } catch {
      case t: Throwable => t.printStackTrace()
    }
  }

  override def putBoolean(key: String, value: Boolean): sge.Preferences = {
    properties.put(key, value.toString)
    this
  }

  override def putInteger(key: String, value: Int): sge.Preferences = {
    properties.put(key, value.toString)
    this
  }

  override def putLong(key: String, value: Long): sge.Preferences = {
    properties.put(key, value.toString)
    this
  }

  override def putFloat(key: String, value: Float): sge.Preferences = {
    properties.put(key, value.toString)
    this
  }

  override def putString(key: String, value: String): sge.Preferences = {
    properties.put(key, value)
    this
  }

  override def put(vals: scala.collection.Map[String, Boolean | Int | Long | Float | String]): sge.Preferences = {
    vals.foreach { (key, value) =>
      value match {
        case v: Boolean => putBoolean(key, v)
        case v: Int     => putInteger(key, v)
        case v: Long    => putLong(key, v)
        case v: Float   => putFloat(key, v)
        case v: String  => putString(key, v)
      }
    }
    this
  }

  override def getBoolean(key: String): Boolean = getBoolean(key, false)

  override def getInteger(key: String): Int = getInteger(key, 0)

  override def getLong(key: String): Long = getLong(key, 0L)

  override def getFloat(key: String): Float = getFloat(key, 0.0f)

  override def getString(key: String): String = getString(key, "")

  override def getBoolean(key: String, defValue: Boolean): Boolean =
    java.lang.Boolean.parseBoolean(properties.getProperty(key, defValue.toString))

  override def getInteger(key: String, defValue: Int): Int =
    Integer.parseInt(properties.getProperty(key, defValue.toString))

  override def getLong(key: String, defValue: Long): Long =
    java.lang.Long.parseLong(properties.getProperty(key, defValue.toString))

  override def getFloat(key: String, defValue: Float): Float =
    java.lang.Float.parseFloat(properties.getProperty(key, defValue.toString))

  override def getString(key: String, defValue: String): String =
    properties.getProperty(key, defValue)

  override def get(): scala.collection.Map[String, Boolean | Int | Long | Float | String] = {
    val map     = scala.collection.mutable.HashMap.empty[String, Boolean | Int | Long | Float | String]
    val entries = properties.entrySet().iterator()
    while (entries.hasNext) {
      val entry = entries.next()
      map.put(entry.getKey().asInstanceOf[String], entry.getValue.asInstanceOf[String])
    }
    map
  }

  override def contains(key: String): Boolean = properties.containsKey(key)

  override def clear(): Unit = properties.clear()

  override def remove(key: String): Unit = { properties.remove(key); () }

  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  override def flush(): Unit = {
    val out = new BufferedOutputStream(fileHandle.write(false))
    try
      properties.storeToXML(out, null: String) // null comment is the Java Properties API convention
    catch {
      case ex: Exception =>
        throw utils.SgeError.FileWriteError(fileHandle, s"Error writing preferences: $fileHandle", Some(ex))
    } finally
      StreamUtils.closeQuietly(out)
  }
}
