// SGE — Android preferences implementation
//
// Implements PreferencesOps using android.content.SharedPreferences.

package sge
package platform
package android

import _root_.android.content.SharedPreferences

/** Android preferences backed by SharedPreferences. */
class AndroidPreferencesImpl(private val sharedPrefs: SharedPreferences) extends PreferencesOps {

  private var editor: SharedPreferences.Editor | Null = null

  private def edit(): SharedPreferences.Editor = {
    if (editor == null) editor = sharedPrefs.edit()
    editor.nn
  }

  override def putBoolean(key: String, value: Boolean): Unit = { edit().putBoolean(key, value); () }
  override def putInteger(key: String, value: Int): Unit     = { edit().putInt(key, value); () }
  override def putLong(key: String, value: Long): Unit       = { edit().putLong(key, value); () }
  override def putFloat(key: String, value: Float): Unit     = { edit().putFloat(key, value); () }
  override def putString(key: String, value: String): Unit   = { edit().putString(key, value); () }

  override def getBoolean(key: String, defValue: Boolean): Boolean = sharedPrefs.getBoolean(key, defValue)
  override def getInteger(key: String, defValue: Int): Int         = sharedPrefs.getInt(key, defValue)
  override def getLong(key: String, defValue: Long): Long          = sharedPrefs.getLong(key, defValue)
  override def getFloat(key: String, defValue: Float): Float       = sharedPrefs.getFloat(key, defValue)
  override def getString(key: String, defValue: String): String    = sharedPrefs.getString(key, defValue)

  override def getAll: java.util.Map[String, ?] = sharedPrefs.getAll()

  override def contains(key: String): Boolean = sharedPrefs.contains(key)

  override def clear(): Unit                 = { edit().clear(); () }
  override def remove(key: String): Unit     = { edit().remove(key); () }

  override def flush(): Unit = {
    if (editor != null) {
      editor.nn.apply()
      editor = null
    }
  }
}
