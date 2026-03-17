// SGE Native Ops — Scala Native platform bridge for shared tests
//
// Provides Native-specific ETC1Ops and BufferOps implementations
// to the shared test suites. Delegates to Rust via C ABI.
//
// Migration notes:
//   Origin: SGE-original (platform abstraction)
//   Convention: wires Native-specific impls (BufferOpsNative, ETC1OpsNative) into PlatformOps
//   Idiom: boundary/break (0 return), Nullable (0 null), split packages
//   Audited: 2026-03-03

package sge
package platform

private[sge] object PlatformOps {
  val etc1:        ETC1Ops        = ETC1OpsNative
  val buffer:      BufferOps      = BufferOpsNative
  val gdx2d:       Gdx2dOps       = Gdx2dOpsNative
  val concurrency: ConcurrencyOps = ConcurrencyOpsDesktop

  // Desktop backend FFI — set by the DesktopApplication during initialization.
  // Null until a desktop backend is running (headless mode doesn't need them).
  @volatile private[sge] var windowing: WindowingOps = scala.compiletime.uninitialized
  @volatile private[sge] var audio:     AudioOps     = scala.compiletime.uninitialized
  @volatile private[sge] var gl:        GlOps        = scala.compiletime.uninitialized
}
