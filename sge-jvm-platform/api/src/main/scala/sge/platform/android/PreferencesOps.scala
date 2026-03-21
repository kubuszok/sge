// SGE — Android preferences operations interface
//
// Self-contained (JDK types only). Implemented in sge-jvm-platform-android
// using android.content.SharedPreferences.

package sge
package platform
package android

/** Preferences operations for Android. Uses only JDK types.
  *
  * Mirrors the sge.Preferences API but without referencing it, so implementations need only JDK + Android SDK on classpath.
  */
trait PreferencesOps {

  def putBoolean(key: String, value: Boolean): Unit
  def putInteger(key: String, value: Int):     Unit
  def putLong(key:    String, value: Long):    Unit
  def putFloat(key:   String, value: Float):   Unit
  def putString(key:  String, value: String):  Unit

  def getBoolean(key: String, defValue: Boolean): Boolean
  def getInteger(key: String, defValue: Int):     Int
  def getLong(key:    String, defValue: Long):    Long
  def getFloat(key:   String, defValue: Float):   Float
  def getString(key:  String, defValue: String):  String

  /** Returns all preferences as a Java map. Values are Boolean, Integer, Long, Float, or String. */
  def getAll: java.util.Map[String, ?]

  def contains(key: String): Boolean
  def clear():               Unit
  def remove(key:   String): Unit

  /** Persists pending changes. */
  def flush(): Unit
}
