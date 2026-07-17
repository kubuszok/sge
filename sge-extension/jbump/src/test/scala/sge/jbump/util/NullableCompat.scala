/*
 * Ported from jbump - https://github.com/tommyettinger/jbump
 * Licensed under the MIT License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package jbump
package util

/** Test-scope compatibility alias for the collapsed jbump `Nullable` (ISS-769).
  *
  * jbump used to ship its own bespoke null-safe wrapper at `sge.jbump.util.Nullable` ("for use within jbump — standalone, no sge core dependency"). ISS-769 collapsed it onto `lowlevel.Nullable` from
  * the standalone `lls` library — jbump still has no sge-core dependency — and the main sources now `import lowlevel.Nullable` directly, so only ONE `Nullable` ships in the published jbump API.
  *
  * This export keeps the historical `sge.jbump.util.Nullable` name resolvable for the existing test suites; it aliases the SAME type (`lowlevel.Nullable` is itself a transparent alias), so there is
  * no duplicate type and no conversion at any seam.
  */
export lowlevel.Nullable
