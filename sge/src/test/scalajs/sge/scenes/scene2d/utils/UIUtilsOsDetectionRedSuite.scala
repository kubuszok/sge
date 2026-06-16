// SGE — RED regression test for ISS-548 (Scala.js / browser only)
//
// The bug: UIUtils computes its OS flags from System.getProperty("os.name"):
//   val isMac     = System.getProperty("os.name","").toLowerCase.contains("mac")
//   val isWindows = ... contains("windows")
//   ... etc.
// On Scala.js (browser) System.getProperty("os.name") is empty (a JS stub), so
// EVERY flag is false even on a macOS browser. Consequence: UIUtils.ctrl()
// (TextField copy/paste/undo, multi-select) checks CONTROL instead of Keys.SYM
// (Cmd) for Mac browser users.
//
// The fix: LibGDX's GWT backend super-sources UIUtils to read
// window.navigator.platform (Navigator.getPlatform()). The JS shim
// UIUtilsPlatform derives the flags from that string using the EXACT GWT
// tokens (capitalized, NOT lowercased):
//   isAndroid = platform contains "Android"
//   isMac     = platform contains "Mac"
//   isWindows = platform contains "Win"
//   isLinux   = platform contains "Linux" || platform contains "FreeBSD"
//   isIos     = platform contains "iPhone" || "iPod" || "iPad"
//
// Why this suite tests the pure `*For(platform: String)` seam rather than the
// live UIUtils.isMac val: UIUtils.isMac (and friends) are CACHE-ONCE vals that
// initialize from the live navigator on first access. In the shared jsdom realm
// of the full JS test run, some other suite touches UIUtils.ctrl() first,
// pinning isMac under the default navigator BEFORE this suite could stub one —
// so a navigator-stub behavioral assertion is ORDER-DEPENDENT (passes under
// --only, flakes/fails in the full run). The token logic is therefore exposed
// as pure predicates, and this suite pins THAT logic with literal platform
// strings: order-independent, no navigator/window mutation (which would risk
// contaminating other suites in the shared realm), CI-safe in the full run.
// Pre-fix the predicates do not exist, so this suite cannot compile/run — the
// legitimate "capability absent" red for a JS-only added-capability bug.

package sge
package scenes
package scene2d
package utils

import munit.FunSuite

class UIUtilsOsDetectionRedSuite extends FunSuite {

  import UIUtilsPlatform.*

  test("isMacFor pins the GWT 'Mac' token") {
    assert(isMacFor("MacIntel"), "MacIntel must be detected as Mac")
    assert(!isMacFor("Win32"), "Win32 must not be detected as Mac")
  }

  test("isWindowsFor pins the GWT 'Win' token") {
    assert(isWindowsFor("Win32"), "Win32 must be detected as Windows")
    assert(!isWindowsFor("MacIntel"), "MacIntel must not be detected as Windows")
  }

  test("isLinuxFor pins the GWT 'Linux' || 'FreeBSD' tokens") {
    assert(isLinuxFor("Linux x86_64"), "Linux x86_64 must be detected as Linux")
    assert(isLinuxFor("FreeBSD amd64"), "FreeBSD amd64 must be detected as Linux (|| FreeBSD branch)")
    assert(!isLinuxFor("MacIntel"), "MacIntel must not be detected as Linux")
  }

  test("isIosFor pins the GWT 'iPhone' || 'iPod' || 'iPad' tokens") {
    assert(isIosFor("iPhone"), "iPhone must be detected as iOS")
    assert(isIosFor("iPad"), "iPad must be detected as iOS")
    assert(isIosFor("iPod"), "iPod must be detected as iOS")
    assert(!isIosFor("Win32"), "Win32 must not be detected as iOS")
  }

  test("isAndroidFor pins the GWT 'Android' token") {
    assert(isAndroidFor("Android"), "Android must be detected as Android")
    assert(!isAndroidFor("MacIntel"), "MacIntel must not be detected as Android")
  }
}
