// SGE Native Ops — shared native-library load seam (ISS-857)
//
// One load of "sge_native_ops" per JVM, shared by every Panama-based op that
// delegates to the Rust C ABI native lib (ETC1OpsPanama, BufferOpsPanama). Both
// ops force their own lazy `lib` SymbolLookup independently (PlatformOps forces
// `etc1` and `buffer` as two separate lazy vals), so before this seam each op
// issued its OWN multiarch.core.NativeLibLoader.load("sge_native_ops") call —
// the SAME logical load performed twice in one process.
//
// That repeated load is NOT harmless on multiarch 0.4.0. NativeLibLoader.load
// has a non-atomic check-then-act race: it reads the per-name cache (~line 95)
// but only populates it AFTER resolving (~line 100), so two concurrent first-time
// loads of "sge_native_ops" both miss the cache and race into extractStream's
// Files.copy. Despite StandardCopyOption.REPLACE_EXISTING, the JDK's copy takes a
// delete-then-CREATE_NEW path and throws java.nio.file.FileAlreadyExistsException
// (extractStream:317) when both threads target the same temp file — a real,
// reproduced crash (the train-19 failure), not a hypothetical.
//
// Memoizing the resolved path in a single `lazy val` here fixes that precisely
// because Scala serializes lazy-val initialization: this thunk runs
// NativeLibLoader.load exactly once per JVM regardless of concurrency, and every
// later access (and the second op) sees the already-resolved Path. SGE's own code
// thus owns the load-once guarantee rather than leaning on the third-party loader
// being concurrency-safe for a repeated name.
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
