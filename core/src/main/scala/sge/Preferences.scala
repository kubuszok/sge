/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/Preferences.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge

import scala.collection.Map

/** <p> A Preference instance is a hash map holding different values. It is stored alongside your application (SharedPreferences on Android, LocalStorage on GWT, on the desktop a Java Preferences file
  * in a ".prefs" directory will be created, and on iOS an NSMutableDictionary will be written to the given file). CAUTION: On the desktop platform, all libGDX applications share the same ".prefs"
  * directory. To avoid collisions use specific names like "com.myname.game1.settings" instead of "settings". </p>
  *
  * <p> To persist changes made to a preferences instance {@link #flush()} has to be invoked. With the exception of Android, changes are cached in memory prior to flushing. On iOS changes are not
  * synchronized between different preferences instances. </p>
  *
  * <p> Use {@link Application#getPreferences(String)} to look up a specific preferences instance. Note that on several backends the preferences name will be used as the filename, so make sure the
  * name is valid for a filename. </p>
  *
  * @author
  *   mzechner (original implementation)
  */
trait Preferences {
  def putBoolean(key: String, value: Boolean): Preferences

  def putInteger(key: String, value: Int): Preferences

  def putLong(key: String, value: Long): Preferences

  def putFloat(key: String, value: Float): Preferences

  def putString(key: String, value: String): Preferences

  def put(vals: Map[String, Boolean | Int | Long | Float | String]): Preferences

  def getBoolean(key: String): Boolean

  def getInteger(key: String): Int

  def getLong(key: String): Long

  def getFloat(key: String): Float

  def getString(key: String): String

  def getBoolean(key: String, defValue: Boolean): Boolean

  def getInteger(key: String, defValue: Int): Int

  def getLong(key: String, defValue: Long): Long

  def getFloat(key: String, defValue: Float): Float

  def getString(key: String, defValue: String): String

  /** Returns a read only Map<String, Object> with all the key, objects of the preferences. */
  def get(): Map[String, Boolean | Int | Long | Float | String]

  def contains(key: String): Boolean

  def clear(): Unit

  def remove(key: String): Unit

  /** Makes sure the preferences are persisted. */
  def flush(): Unit
}
