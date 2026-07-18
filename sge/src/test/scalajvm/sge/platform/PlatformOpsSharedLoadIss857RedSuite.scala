/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Environmental runtime red — ISS-857 (major), wave 2026-07-18-I, territory I1.
 * Reproducer-authored: MUST NOT be modified by the fixer.
 *
 * DEFECT (design fragility). ETC1OpsPanama and BufferOpsPanama EACH call
 * multiarch.core.NativeLibLoader.load("sge_native_ops") in their constructors
 * (ETC1OpsPanama.scala:26, BufferOpsPanama.scala:29). PlatformOps forces those
 * two constructors independently, via the two lazy vals
 *   lazy val etc1:   ETC1Ops   = new ETC1OpsPanama(panama)   // PlatformOps.scala:21
 *   lazy val buffer: BufferOps = new BufferOpsPanama(panama) // PlatformOps.scala:22
 * so a JVM app that touches BOTH etc1 and buffer issues the SAME native-lib load
 * TWICE in one process. SGE's own code therefore relies on multiarch's loader
 * being idempotent for the same logical name — an implementation detail of a
 * third-party dependency, not a contract SGE controls.
 *
 * ISS-857 (filed from wave-H H2 de-theatered probes) reported the second load
 * throwing java.nio.file.FileAlreadyExistsException on legacy-layout providers
 * ("Files.copy without REPLACE_EXISTING in multiarch"). This suite pins the
 * app-facing contract that survives regardless of the loader's internals:
 *
 *   touching PlatformOps.etc1 THEN PlatformOps.buffer (both lazily initializing
 *   in the same JVM, ETC1 first) must SUCCEED.
 *
 * WHY THIS RUNTIME TEST IS ENVIRONMENT-DEPENDENT (and why the DETERMINISTIC red
 * lives in the sibling compile-shape suite):
 *   The multiarch version this build resolves (project/Versions.scala:31 =
 *   "0.4.0") already makes the double load idempotent by TWO independent means,
 *   verified against the 0.4.0 release sources jar
 *   (multiarch-core_3-0.4.0-sources.jar, NativeLibLoader.scala):
 *     1. load(libName) caches the resolved Path in a ConcurrentHashMap keyed by
 *        the logical name (lines 94-102): the SECOND load("sge_native_ops") is a
 *        cache HIT and never re-extracts.
 *     2. even on a cache miss, extractStream copies with
 *        StandardCopyOption.REPLACE_EXISTING (line 317), so a re-extraction would
 *        overwrite rather than throw FileAlreadyExistsException.
 *   The desktop provider on the local classpath (pnm-provider-sge-desktop 0.1.2)
 *   IS the legacy layout ISS-857 names (native/<classifier>/libsge_native_ops.*,
 *   resolved via extractFromClasspathLegacy), yet the load stays idempotent for
 *   the reasons above. So this test is GREEN here and on CI with multiarch 0.4.0;
 *   it only turns RED against a loader that re-extracts non-idempotently (an
 *   older multiarch, or a build that regresses either safeguard).
 *
 * The DETERMINISTIC red for this wave is the sibling
 * PlatformOpsSharedLoadIss857CompileRedSuite, which pins the fix's design seam
 * (a single memoized load via `private[platform] object SgeNativeOpsLib`) as a
 * compile-shape contract that is red on every axis until the fix lands.
 *
 * On the OPTIONAL JVM axis (native-less legs where the provider JAR is
 * legitimately absent) this suite skips via `assume`, mirroring
 * BufferOpsSuite/ETC1OpsSuite (ISS-724 c1 / ISS-856 policy). It hard-FAILS only
 * when the library is present but the second lazy init throws — the ISS-857
 * crash shape — because a FileAlreadyExistsException is NOT one of the
 * "library unavailable" throwables the probe swallows, so it propagates.
 *
 * Post-fix (both ops consuming SgeNativeOpsLib.path = one load) this stays GREEN
 * everywhere.
 */
package sge
package platform

class PlatformOpsSharedLoadIss857RedSuite extends munit.FunSuite {

  // Narrowed to the "library unavailable" failure shapes so a genuine ABI /
  // symbol regression surfaces as red instead of a silent skip (mirrors
  // BufferOpsSuite/ETC1OpsSuite). Crucially, java.nio.file.FileAlreadyExistsException
  // — the ISS-857 double-extraction crash — is NOT matched here, so it propagates
  // and reds this suite instead of being mistaken for an absent library.
  private def isLibraryUnavailable(t: Throwable): Boolean = t match {
    case _: UnsatisfiedLinkError        => true
    case e: ExceptionInInitializerError => Option(e.getCause).exists(isLibraryUnavailable)
    case _ => false
  }

  // Probe on ETC1 FIRST — the issue's reproduction order (ETC1 lazy init issues
  // the first load, Buffer the second). If the library is genuinely absent this
  // records the optional-axis skip; if present it forces the first load here.
  private val nativeLibAvailable: Boolean =
    try { PlatformOps.etc1; true }
    catch { case t: Throwable if isLibraryUnavailable(t) => false }

  override def munitTestTransforms: List[TestTransform] =
    super.munitTestTransforms :+ new TestTransform(
      "requireNativeLib",
      { test =>
        test.withBody { () =>
          if (NativeOpsAvailabilityPolicy.guaranteed)
            assert(
              nativeLibAvailable,
              "native ops capability MUST be present on this axis but the availability probe " +
                "returned false — this is a wiring/ABI regression, not an environment gap (ISS-724 c1)"
            )
          else
            assume(
              nativeLibAvailable,
              "Rust native library not available (JVM native-less leg; ISS-856 standing optional-axis policy, ISS-724 c1 heritage)"
            )
          test.body()
        }
      }
    )

  test("ISS-857: touching PlatformOps.etc1 then PlatformOps.buffer in one JVM must both initialize successfully") {
    // First lazy init (already forced by the probe) = load #1.
    val etc1: ETC1Ops = PlatformOps.etc1
    // SECOND lazy init = load #2 — the ISS-857 crash site. Merely evaluating this
    // lazy val runs BufferOpsPanama's constructor, which issues the second
    // NativeLibLoader.load("sge_native_ops") and resolves every downcall handle.
    // A non-idempotent loader throws FileAlreadyExistsException HERE (wrapped in
    // ExceptionInInitializerError), which — not being a "library unavailable"
    // shape — propagates and reds this test.
    val buffer: BufferOps = PlatformOps.buffer

    // End-to-end sanity that BOTH native impls are wired after the double load:
    // an ETC1 downcall and a Buffer native alloc/free both exercise real symbols.
    assertEquals(etc1.getCompressedDataSize(4, 4), 8, "ETC1 native downcall must work after the shared load")
    val buf = buffer.newDisposableByteBuffer(16)
    try assert(buf.capacity() >= 16, "Buffer native alloc must work after the shared load")
    finally buffer.freeMemory(buf)
  }
}
