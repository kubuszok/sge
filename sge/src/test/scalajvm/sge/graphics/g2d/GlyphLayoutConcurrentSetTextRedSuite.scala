/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red test for ISS-797 (GlyphLayout's static glyphRunPool is a JVM-global,
 * non-thread-safe Pool shared by every GlyphLayout in the process).
 *
 * GlyphLayout.scala:532 declares
 *   `private[g2d] val glyphRunPool: Pool[GlyphRun] = Pool.Default[GlyphRun](() => GlyphRun())`
 * inside `object GlyphLayout` — one pool per JVM. Every setText() obtains
 * GlyphRuns from it (GlyphLayout.scala:147, :294, :371) and frees them back
 * (GlyphLayout.scala:165, :172, :349, :429, :504 via reset()). Pool's free
 * list is an unsynchronized DynamicArray (Pool.scala:53; ISS-603), so two
 * threads laying out text concurrently race pop()/add() on it. Observed
 * corruption modes:
 *   - the same GlyphRun instance is handed to two layouts at once, so one
 *     thread's reset() clears arrays the other is still appending to
 *     (wrong widths / glyph counts / NaNs);
 *   - DynamicArray._size underflows, after which EVERY subsequent obtain()
 *     throws ArrayIndexOutOfBoundsException(-1..-13) — this is the cascade
 *     that poisons later font suites in the same forked test JVM (suites
 *     pass in isolation, fail when run after a parallel-suite race).
 *
 * The original LibGDX has the identical static pool
 * (com/badlogic/gdx/graphics/g2d/GlyphLayout.java:48) and copes by CONTRACT,
 * not by code: "This class is not thread safe, even if synchronized
 * externally, and must only be used from the game thread."
 * (GlyphLayout.java:42). SGE inherited the static pool but not the
 * single-game-thread guarantee (parallel munit suites in one forked JVM, and
 * any game doing async loading/layout), so the pool must become safe or
 * per-context.
 *
 * The font fixture below is the same headless construction as
 * BitmapFontCacheDefaultAlignRedSuite (no file, no GL).
 *
 * This test is written by the reproducer agent and MUST NOT be modified by
 * the fixer: it encodes the invariant that concurrent setText on SEPARATE
 * GlyphLayout instances must not corrupt either layout.
 */
package sge
package graphics
package g2d

import java.util.concurrent.{ ConcurrentLinkedQueue, CyclicBarrier }
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.duration.{ Duration, DurationInt }

import lowlevel.Nullable
import lowlevel.util.DynamicArray

class GlyphLayoutConcurrentSetTextRedSuite extends munit.FunSuite {

  override def munitTimeout: Duration = 120.seconds

  private given Sge = SgeTestFixture.testSge()

  // --- Headless font construction (same fixture as BitmapFontCacheDefaultAlignRedSuite) ---

  /** Regular glyph: xoffset 0, yoffset 0, page 0, not fixed-width. */
  private def mkGlyph(ch: Char, xadvance: Int, width: Int): BitmapFont.Glyph = {
    val g = new BitmapFont.Glyph()
    g.id = ch.toInt
    g.xadvance = xadvance
    g.width = width
    g.height = 8
    g
  }

  /** capHeight=10, down=-12, scaleX=scaleY=1, spaceXadvance=10; glyph 'a': xadvance=10, width=9. Layout of "aaa" is a single run of width 0+10+10+9 = 29. */
  private def makeFont(): BitmapFont = {
    val data = new BitmapFontData()
    data.capHeight = 10f
    data.down = -12f
    data.spaceXadvance = 10f
    val regions = DynamicArray[TextureRegion]()
    regions.add(new TextureRegion())
    val font = new BitmapFont(data, Nullable(regions), true)
    // Register glyphs after the BitmapFont constructor ran (BitmapFont.load
    // must not call setGlyphRegion against the texture-less dummy region).
    data.setGlyph('a'.toInt, mkGlyph('a', 10, 9))
    font
  }

  /** Best-effort repair of the JVM-global glyphRunPool after the hammer, so the corruption this red test provokes does not cascade into unrelated font suites running later in the same forked JVM —
    * that cascade is exactly the ISS-797 symptom, and it must stay confined to this suite. Reflection is used because Pool keeps freeObjects private (Pool.scala:53); DynamicArray.setSize(0) resets
    * even a negative `_size`. This touches no production code and runs after the assertions have already recorded the corruption.
    */
  private def drainGlyphRunPool(): Unit = {
    val pool: AnyRef   = GlyphLayout.glyphRunPool
    var cls:  Class[?] = pool.getClass
    while (cls != null) { // Java reflection interop boundary: getSuperclass returns null at the top of the hierarchy
      cls.getDeclaredFields.foreach { f =>
        f.setAccessible(true)
        f.get(pool) match {
          case da: DynamicArray[?] => da.setSize(0)
          case _ => ()
        }
      }
      cls = cls.getSuperclass
    }
  }

  test("ISS-797: concurrent setText on two GlyphLayout instances must not corrupt the shared static glyphRunPool") {
    // Three runs per layout ("aaa" x 3 lines) -> every setText call does 3
    // obtains, and the next call's reset() frees all 3 back — constant
    // cross-thread obtain/free traffic on the ONE static pool.
    val text  = "aaa\naaa\naaa"
    val fontA = makeFont()
    val fontB = makeFont()

    // Single-threaded baseline: with a healthy pool this is what EVERY
    // setText of `text` must produce (deterministic float arithmetic).
    val baseline = new GlyphLayout()
    baseline.setText(fontA, text)
    val expectedRuns   = baseline.runs.size
    val expectedGlyphs = baseline.glyphCount
    val expectedWidth  = baseline.width
    val expectedHeight = baseline.height
    assertEquals(expectedRuns, 3, "fixture sanity: one run per line")
    assertEquals(expectedGlyphs, 9, "fixture sanity: three 'a' glyphs per line")
    baseline.reset() // return the baseline's runs to the pool

    val iterations = 20000
    val barrier    = new CyclicBarrier(2)
    val failures   = new AtomicInteger(0)
    val samples    = new ConcurrentLinkedQueue[String]()

    def record(msg: String): Unit = {
      failures.incrementAndGet()
      if (samples.size < 8) samples.add(msg)
    }

    def worker(font: BitmapFont, name: String): Thread = {
      val t = new Thread(
        () => {
          // Each thread has its OWN layout and its OWN font — the static
          // glyphRunPool is the only shared state.
          val layout = new GlyphLayout()
          var i      = 0
          while (i < iterations) {
            if ((i & 1023) == 0) barrier.await() // re-align periodically so the loops stay overlapped
            try {
              layout.setText(font, text)
              if (
                layout.runs.size != expectedRuns || layout.glyphCount != expectedGlyphs ||
                layout.width != expectedWidth || layout.height != expectedHeight
              ) {
                record(
                  s"$name iter $i: corrupted layout — runs=${layout.runs.size} (expected $expectedRuns), " +
                    s"glyphCount=${layout.glyphCount} (expected $expectedGlyphs), width=${layout.width} (expected $expectedWidth), " +
                    s"height=${layout.height} (expected $expectedHeight)"
                )
              }
            } catch {
              case e: Throwable =>
                record(s"$name iter $i: ${e.getClass.getName}: ${e.getMessage}")
            }
            i += 1
          }
        },
        name
      )
      t.setDaemon(true)
      t
    }

    try {
      val wa = worker(fontA, "iss797-worker-a")
      val wb = worker(fontB, "iss797-worker-b")
      wa.start()
      wb.start()
      wa.join(90000)
      wb.join(90000)
      assert(!wa.isAlive && !wb.isAlive, "ISS-797 workers did not finish in time")

      // A negative free count is the smoking gun for the AIOOBE cascade: once
      // freeObjects._size underflows, every later obtain() in this JVM throws.
      val poolFree = GlyphLayout.glyphRunPool.free
      assert(
        failures.get() == 0 && poolFree >= 0,
        s"ISS-797: shared static glyphRunPool (GlyphLayout.scala:532) corrupted by concurrent setText — " +
          s"${failures.get()} corrupted/throwing iterations out of ${2 * iterations}; glyphRunPool.free=$poolFree; " +
          s"first failures:\n  ${samples.toArray.mkString("\n  ")}"
      )
    } finally
      // Confine the provoked corruption to this suite (see scaladoc above).
      drainGlyphRunPool()
  }
}
