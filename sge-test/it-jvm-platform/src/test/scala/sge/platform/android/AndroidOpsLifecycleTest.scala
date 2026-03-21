// SGE — Integration test: AndroidLifecycleOps API interface
//
// Tests that the AndroidLifecycleOps trait and provider factory method have expected shapes.

package sge
package platform
package android

import munit.FunSuite

class AndroidOpsLifecycleTest extends FunSuite {

  test("AndroidLifecycleOps has UI thread method") {
    val cls = classOf[AndroidLifecycleOps]
    assert(cls.getMethod("runOnUiThread", classOf[Runnable]) != null)
  }

  test("AndroidLifecycleOps has immersive mode method") {
    val cls = classOf[AndroidLifecycleOps]
    assert(cls.getMethod("useImmersiveMode", classOf[Boolean]) != null)
  }

  test("AndroidLifecycleOps has version and heap methods") {
    val cls = classOf[AndroidLifecycleOps]
    assert(cls.getMethod("getAndroidVersion") != null)
    assert(cls.getMethod("getNativeHeapAllocatedSize") != null)
  }

  test("AndroidLifecycleOps has activity control methods") {
    val cls = classOf[AndroidLifecycleOps]
    assert(cls.getMethod("finish") != null)
    assert(cls.getMethod("hasHardwareKeyboard") != null)
  }

  test("AndroidLifecycleOps has GL surface view methods") {
    val cls = classOf[AndroidLifecycleOps]
    assert(cls.getMethod("setGLSurfaceView", classOf[GLSurfaceViewOps]) != null)
    assert(cls.getMethod("getGLSurfaceView") != null)
    assert(cls.getMethod("resumeGLSurfaceView") != null)
    assert(cls.getMethod("pauseGLSurfaceView") != null)
  }

  test("AndroidPlatformProvider has createLifecycle factory method") {
    val cls = classOf[AndroidPlatformProvider]
    assert(cls.getMethod("createLifecycle", classOf[AnyRef]) != null)
  }
}
