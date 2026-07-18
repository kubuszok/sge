// SGE Native Ops — shared native-library load seam (ISS-857)
//
// One load of "sge_native_ops" per JVM, shared by every Panama-based op that
// delegates to the Rust C ABI native lib (ETC1OpsPanama, BufferOpsPanama). Both
// ops force their own lazy `lib` SymbolLookup independently (PlatformOps forces
// `etc1` and `buffer` as two separate lazy vals), so before this seam each op
// issued its OWN multiarch.core.NativeLibLoader.load("sge_native_ops") call —
// the SAME logical load performed twice in one process.
//
// Memoizing the resolved path in a single `lazy val` here makes SGE's own code
// responsible for loading exactly once, rather than leaning on the loader being
// idempotent for a repeated name. multiarch 0.4.0 does happen to make the double
// load harmless (per-name ConcurrentHashMap cache + Files.copy with
// REPLACE_EXISTING into a per-JVM temp dir), but that is a third-party
// implementation detail, not a contract SGE controls — a loader that
// re-extracted non-idempotently would crash the second op's init. This seam
// removes that dependency.
//
// Each op still builds its OWN SymbolLookup from this path
// (p.SymbolLookup.libraryLookup(found, p.Arena.global())): the lookup is
// provider-path-dependent and Panama-provider-specific, so only the resolution
// of the on-disk path is shared, not the linker/lookup wiring.
//
// Alternatives considered (not taken): (a) loading the lib once eagerly at
// PlatformOps init — rejected because native-less JVM legs must stay lazy so
// the load is only forced when an op is actually touched; (b) a provider
// manifest-v2 bump so the loader's own auto-discovery (loadAll) performs the
// single load — that path is tracked under the ISS-751 provider-manifest family.
//
// Migration notes:
//   Origin: SGE-original (platform abstraction; ISS-857 memoized native-lib load)
//   Convention: single per-JVM load seam consumed by ETC1OpsPanama + BufferOpsPanama
//   Idiom: boundary/break (0 return), Nullable (0 null), split packages

package sge
package platform

private[platform] object SgeNativeOpsLib {

  /** The resolved on-disk path of the `sge_native_ops` native library, loaded exactly once per JVM and shared by every Panama op that consumes it. Lazy so the native load is forced only when an op is
    * actually touched (native-less JVM legs never evaluate this).
    */
  lazy val path: java.nio.file.Path =
    multiarch.core.NativeLibLoader.load("sge_native_ops")
}
