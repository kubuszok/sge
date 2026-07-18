// SGE Native Ops — per-axis availability policy (JVM)
//
// ISS-724 c1, wave 2026-07-18-H territory H2. The BufferOps/ETC1Ops shared suites
// probe whether the native-ops capability is present before running. This value tells
// those suites whether the current platform axis GUARANTEES the capability (so a false
// probe is a wiring/ABI regression that must fail loudly) or merely OFFERS it (so a
// false probe is a legitimate environment gap that may skip).
//
// JVM: the Rust native library (libsge_native_ops) ships in the pnm-provider-sge-desktop
// provider JAR and is extracted + dlopen'd lazily by NativeLibLoader the first time
// PlatformOps.buffer / PlatformOps.etc1 is touched. On native-less legs (local dev without
// a native build, native-less CI legs) that provider JAR is legitimately absent, so the
// capability is OPTIONAL on this axis and a documented skip is allowed (ISS-724 c1).

package sge
package platform

private[platform] object NativeOpsAvailabilityPolicy {

  /** Whether the native-ops capability is guaranteed present on this platform axis. */
  val guaranteed: Boolean = false
}
