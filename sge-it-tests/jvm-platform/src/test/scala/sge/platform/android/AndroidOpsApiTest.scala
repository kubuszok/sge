// SGE — Integration test: Android ops API interfaces
//
// Verifies that the self-contained ops interfaces are usable without
// any Android SDK dependency. Tests the API shape and default config.

package sge
package platform
package android

import munit.FunSuite

class AndroidOpsApiTest extends FunSuite {

  test("AndroidConfigOps has sensible defaults") {
    val config = AndroidConfigOps()
    assertEquals(config.r, 8)
    assertEquals(config.g, 8)
    assertEquals(config.b, 8)
    assertEquals(config.a, 8)
    assertEquals(config.depth, 16)
    assertEquals(config.stencil, 0)
    assertEquals(config.numSamples, 0)
    assertEquals(config.useAccelerometer, true)
    assertEquals(config.useGyroscope, false)
    assertEquals(config.useCompass, true)
    assertEquals(config.useImmersiveMode, true)
    assertEquals(config.useGL30, false)
    assertEquals(config.disableAudio, false)
    assertEquals(config.maxSimultaneousSounds, 16)
  }

  test("AndroidConfigOps fields are mutable") {
    val config = AndroidConfigOps()
    config.useGL30 = true
    config.depth = 24
    config.numSamples = 4
    assertEquals(config.useGL30, true)
    assertEquals(config.depth, 24)
    assertEquals(config.numSamples, 4)
  }

  test("ClipboardOps trait is loadable") {
    // Verify the interface class exists and has expected methods
    val cls = classOf[ClipboardOps]
    assert(cls.getMethod("hasContents") != null)
    assert(cls.getMethod("getContents") != null)
    assert(cls.getMethod("setContents", classOf[String]) != null)
  }

  test("PreferencesOps trait is loadable") {
    val cls = classOf[PreferencesOps]
    assert(cls.getMethod("putBoolean", classOf[String], classOf[Boolean]) != null)
    assert(cls.getMethod("getBoolean", classOf[String], classOf[Boolean]) != null)
    assert(cls.getMethod("putString", classOf[String], classOf[String]) != null)
    assert(cls.getMethod("getString", classOf[String], classOf[String]) != null)
    assert(cls.getMethod("flush") != null)
    assert(cls.getMethod("clear") != null)
    assert(cls.getMethod("remove", classOf[String]) != null)
    assert(cls.getMethod("getAll") != null)
  }

  test("AndroidPlatformProvider trait is loadable") {
    val cls = classOf[AndroidPlatformProvider]
    assert(cls.getMethod("createClipboard", classOf[AnyRef]) != null)
    assert(cls.getMethod("createPreferences", classOf[AnyRef], classOf[String]) != null)
    assert(cls.getMethod("openURI", classOf[AnyRef], classOf[String]) != null)
    assert(cls.getMethod("defaultConfig") != null)
  }

  test("stub ClipboardOps implementation works") {
    val stub = new ClipboardOps {
      private var content:                    String | Null = null
      override def hasContents:               Boolean       = content != null
      override def getContents:               String | Null = content
      override def setContents(text: String): Unit          = content = text
    }
    assert(!stub.hasContents)
    stub.setContents("hello")
    assert(stub.hasContents)
    assertEquals(stub.getContents, "hello")
  }

  test("stub PreferencesOps implementation works") {
    val stub = new PreferencesOps {
      private val map = new java.util.HashMap[String, Any]()
      override def putBoolean(key: String, value: Boolean):    Unit    = map.put(key, value)
      override def putInteger(key: String, value: Int):        Unit    = map.put(key, value)
      override def putLong(key:    String, value: Long):       Unit    = map.put(key, value)
      override def putFloat(key:   String, value: Float):      Unit    = map.put(key, value)
      override def putString(key:  String, value: String):     Unit    = map.put(key, value)
      override def getBoolean(key: String, defValue: Boolean): Boolean =
        if (map.containsKey(key)) map.get(key).asInstanceOf[Boolean] else defValue
      override def getInteger(key: String, defValue: Int): Int =
        if (map.containsKey(key)) map.get(key).asInstanceOf[Int] else defValue
      override def getLong(key: String, defValue: Long): Long =
        if (map.containsKey(key)) map.get(key).asInstanceOf[Long] else defValue
      override def getFloat(key: String, defValue: Float): Float =
        if (map.containsKey(key)) map.get(key).asInstanceOf[Float] else defValue
      override def getString(key: String, defValue: String): String =
        if (map.containsKey(key)) map.get(key).asInstanceOf[String] else defValue
      override def getAll:                java.util.Map[String, ?] = java.util.Collections.unmodifiableMap(map)
      override def contains(key: String): Boolean                  = map.containsKey(key)
      override def clear():               Unit                     = map.clear()
      override def remove(key:   String): Unit                     = map.remove(key)
      override def flush():               Unit                     = () // no-op
    }
    stub.putString("name", "SGE")
    stub.putInteger("version", 1)
    stub.putBoolean("debug", true)
    assertEquals(stub.getString("name", ""), "SGE")
    assertEquals(stub.getInteger("version", 0), 1)
    assertEquals(stub.getBoolean("debug", false), true)
    assertEquals(stub.getString("missing", "default"), "default")
    assert(stub.contains("name"))
    assert(!stub.contains("missing"))
    stub.remove("name")
    assert(!stub.contains("name"))
    stub.clear()
    assertEquals(stub.getAll.size(), 0)
  }
}
