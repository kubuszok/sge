// SGE — Integration test: FilesOps API interface
//
// Tests that the FilesOps trait and provider factory method have expected shapes.

package sge
package platform
package android

import munit.FunSuite

class AndroidOpsFileHandleTest extends FunSuite {

  // ── FilesOps ──────────────────────────────────────────────────────────

  test("FilesOps has openInternal method") {
    val cls = classOf[FilesOps]
    assert(cls.getMethod("openInternal", classOf[String]) != null)
  }

  test("FilesOps has listInternal method") {
    val cls = classOf[FilesOps]
    assert(cls.getMethod("listInternal", classOf[String]) != null)
  }

  test("FilesOps has openInternalFd method") {
    val cls = classOf[FilesOps]
    assert(cls.getMethod("openInternalFd", classOf[String]) != null)
  }

  test("FilesOps has internalFileLength method") {
    val cls = classOf[FilesOps]
    assert(cls.getMethod("internalFileLength", classOf[String]) != null)
  }

  test("FilesOps has storage path accessors") {
    val cls = classOf[FilesOps]
    assert(cls.getMethod("localStoragePath") != null)
    assert(cls.getMethod("externalStoragePath") != null)
  }

  // ── Provider factory ─────────────────────────────────────────────────

  test("AndroidPlatformProvider has createFiles factory method") {
    val cls = classOf[AndroidPlatformProvider]
    assert(cls.getMethod("createFiles", classOf[AnyRef], classOf[Boolean]) != null)
  }
}
