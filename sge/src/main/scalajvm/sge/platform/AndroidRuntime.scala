// SGE — Android runtime detection utility
//
// Centralised check for whether the JVM is actually an Android VM (Dalvik/ART).
// android.jar may be on the classpath for cross-compilation even on desktop JVM,
// so Class.forName("android.*") is NOT a reliable indicator. We check the VM
// name and vendor system properties instead.

package sge
package platform

/** Detects whether we are running on an actual Android VM (Dalvik/ART).
  *
  * '''Do not use `Class.forName("android.*")` for Android detection''' — android.jar is on the classpath during cross-compilation. Use this object instead.
  */
private[sge] object AndroidRuntime {

  /** `true` only when the JVM is Dalvik or ART (actual Android device/emulator). */
  val isAndroid: Boolean = {
    val vmName     = System.getProperty("java.vm.name", "").toLowerCase
    val vendor     = System.getProperty("java.vm.vendor", "").toLowerCase
    val jVendor    = System.getProperty("java.vendor", "").toLowerCase
    val specVendor = System.getProperty("java.specification.vendor", "").toLowerCase
    vmName.contains("dalvik") || vmName.contains("art") ||
    vendor.contains("android") || jVendor.contains("android") ||
    specVendor.contains("android")
  }
}
