package sge
package visui
package util

class OsUtilsSuite extends munit.FunSuite {

  test("exactly one of isWindows, isMac, isUnix is true (or none on exotic OS)") {
    val count = List(OsUtils.isWindows, OsUtils.isMac, OsUtils.isUnix).count(identity)
    assert(count <= 1, s"At most one OS flag should be true, got $count")
  }

  test("at least one desktop OS is detected on standard platforms") {
    // This test runs on CI (linux, mac, windows) so at least one should be true
    val anyDetected = OsUtils.isWindows || OsUtils.isMac || OsUtils.isUnix
    assert(anyDetected, "Expected at least one of isWindows/isMac/isUnix to be true")
  }

  test("OS detection is consistent across calls") {
    assertEquals(OsUtils.isWindows, OsUtils.isWindows)
    assertEquals(OsUtils.isMac, OsUtils.isMac)
    assertEquals(OsUtils.isUnix, OsUtils.isUnix)
  }

  test("on macOS, isMac is true") {
    val os = System.getProperty("os.name", "").toLowerCase
    if (os.contains("mac")) {
      assert(OsUtils.isMac)
      assert(!OsUtils.isWindows)
      assert(!OsUtils.isUnix)
    }
  }

  test("on Linux, isUnix is true") {
    val os = System.getProperty("os.name", "").toLowerCase
    if (os.contains("nux") || os.contains("nix")) {
      assert(OsUtils.isUnix)
      assert(!OsUtils.isWindows)
      assert(!OsUtils.isMac)
    }
  }

  test("on Windows, isWindows is true") {
    val os = System.getProperty("os.name", "").toLowerCase
    if (os.contains("win")) {
      assert(OsUtils.isWindows)
      assert(!OsUtils.isMac)
      assert(!OsUtils.isUnix)
    }
  }
}
