package sge.utils

/** INJECTED SCALA (Substitutions.inject): the operating-system tag libGDX's
  * removed `SharedLibraryLoader` exposed. Dropped upstream in sge along with the
  * loader; supplied here so the ported corpus that still branches on the running
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 9
 * Covenant-baseline-methods: Os
 * Covenant-source-reference: injected (no upstream)
 * Covenant-verified: 2026-09-23
  * platform (e.g. `UIUtils`) resolves. Ready-made, compiles standalone. */
enum Os {
  case Android, MacOsX, Windows, Linux, IOS
}
