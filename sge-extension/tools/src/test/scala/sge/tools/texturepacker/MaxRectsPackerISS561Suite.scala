/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * ISS-561 (batch F): packing-geometry coverage for MaxRectsPacker.scala
 * (sge-extension/tools). Before this suite the only tools test was the
 * settings-file red suite; the MaxRects bin-packing algorithm itself had
 * zero tests.
 *
 * These tests drive the public Packer API (MaxRectsPacker.pack) with
 * synthetic Rects built from known width/height (no image I/O) and assert
 * EXACT placements / strong geometric invariants, so production mutations in
 * splitFreeNode / pruneFreeList / scoreRect / the padding math FAIL.
 *
 * Source reference:
 *   original-src/libgdx/extensions/gdx-tools/src/com/badlogic/gdx/tools/
 *   texturepacker/MaxRectsPacker.java
 *   - pack/packPage/packAtSize (lines 53-244), BinarySearch (252-294),
 *   - MaxRects.init/insert/pack/getResult (306-...),
 *   - splitFreeNode / pruneFreeList / scoreRect heuristics.
 *
 * Determinism notes:
 *   - MaxRectsPacker.pack MUTATES inputRects in place: each rect's
 *     width/height gets settings.paddingX/paddingY ADDED up front
 *     (MaxRectsPacker.scala lines 60-65 / .java 59-63). Every test therefore
 *     builds fresh Rects.
 *   - Page.width/height (getResult, .scala 437-453 / .java) are the TIGHT
 *     bounding box of placed rects = max(x+width)/max(y+height), NOT the bin
 *     search size, so they are deterministic regardless of which page size
 *     the binary search converges on.
 *   - The packer tries all 5 heuristics and keeps the highest-occupancy
 *     result. Where multiple full packings tie on occupancy the exact
 *     coordinate could vary, so those tests constrain the geometry (small
 *     maxWidth/maxHeight) until exactly one full packing exists, OR assert
 *     the heuristic-independent invariants (no overlap, in bounds, all
 *     placed, exact page count) plus at least one exact coordinate.
 */
package sge
package tools
package texturepacker

import sge.tools.texturepacker.TexturePacker.*

import scala.collection.mutable.ArrayBuffer

class MaxRectsPackerISS561Suite extends munit.FunSuite {

  // The packer is pure offscreen geometry, but Settings touches AWT-adjacent
  // enums; keep headless to avoid any toolkit init on macOS forked JVMs.
  System.setProperty("java.awt.headless", "true")

  /** A predictable Settings: no power-of-two rounding, no padding, no edge padding, no rotation, not square, not fast, silent. minWidth/minHeight are tiny so page sizing is driven by the rects, not
    * the floor.
    */
  private def baseSettings(maxW: Int, maxH: Int): Settings = {
    val s = new Settings()
    s.pot = false
    s.multipleOfFour = false
    s.paddingX = 0
    s.paddingY = 0
    s.edgePadding = false
    s.duplicatePadding = false
    s.rotation = false
    s.square = false
    s.fast = false
    s.silent = true
    s.minWidth = 1
    s.minHeight = 1
    s.maxWidth = maxW
    s.maxHeight = maxH
    s
  }

  /** Synthetic rect with a known name + width/height (no BufferedImage). */
  private def rect(name: String, w: Int, h: Int): Rect = {
    val r = new Rect()
    r.name = lowlevel.Nullable(name)
    r.width = w
    r.height = h
    r.regionWidth = w
    r.regionHeight = h
    r.originalWidth = w
    r.originalHeight = h
    r.canRotate = false
    r
  }

  private def buf(rs: Rect*): ArrayBuffer[Rect] = ArrayBuffer.from(rs)

  /** All placed rects across all pages. */
  private def allPlaced(pages: ArrayBuffer[Page]): ArrayBuffer[Rect] =
    pages.flatMap(_.outputRects)

  /** True if two placed rects overlap (open intersection of their areas). */
  private def overlaps(a: Rect, b: Rect): Boolean =
    a.x < b.x + b.width && b.x < a.x + a.width &&
      a.y < b.y + b.height && b.y < a.y + a.height

  /** Asserts: no two rects on the same page overlap. */
  private def assertNoOverlap(pages: ArrayBuffer[Page]): Unit =
    pages.foreach { page =>
      val rs = page.outputRects
      var i  = 0
      while (i < rs.size) {
        var j = i + 1
        while (j < rs.size) {
          assert(
            !overlaps(rs(i), rs(j)),
            s"rects ${rs(i)} and ${rs(j)} overlap on the same page"
          )
          j += 1
        }
        i += 1
      }
    }

  /** Asserts every placed rect is within its page bounds (page.width/height is the tight bbox = max(x+width)/max(y+height), so this is x>=0, y>=0 plus x+width<=page.width, y+height<=page.height).
    */
  private def assertInBounds(pages: ArrayBuffer[Page]): Unit =
    pages.foreach { page =>
      page.outputRects.foreach { r =>
        assert(r.x >= 0 && r.y >= 0, s"rect $r has negative origin")
        assert(
          r.x + r.width <= page.width && r.y + r.height <= page.height,
          s"rect $r exceeds page bbox ${page.width}x${page.height}"
        )
      }
    }

  // --- single page, single rect: fully deterministic --------------------

  test("ISS561 single rect lands at (0,0); page bbox is exactly the rect") {
    val packer = new MaxRectsPacker(baseSettings(512, 512))
    val pages  = packer.pack(buf(rect("solo", 100, 50)))

    assertEquals(pages.size, 1, "one rect must use exactly one page")
    val page = pages(0)
    assertEquals(page.outputRects.size, 1)
    val r = page.outputRects(0)
    assertEquals((r.x, r.y), (0, 0), "the only rect must sit at the origin")
    // getResult tight bbox: max(x+width)=100, max(y+height)=50.
    assertEquals(page.width, 100, "page width = bounding box of placed rects")
    assertEquals(page.height, 50, "page height = bounding box of placed rects")
  }

  // --- single page, two rects forced side-by-side ----------------------
  //
  // Two 50x100 rects. maxWidth=100 fits them side-by-side (bbox 100x100).
  // maxHeight=120 makes a stacked layout (height 200) IMPOSSIBLE, so the only
  // full packing is side-by-side: x in {0,50}, y=0 for both. This pins
  // splitFreeNode's right-side split (.java/.scala splitFreeNode "right side
  // of the used node" branch): after placing the first 50-wide rect the free
  // region must start at x=50 width=50 for the second rect to land at (50,0).

  test("ISS561 two equal rects pack side-by-side at exactly x=0 and x=50") {
    val packer = new MaxRectsPacker(baseSettings(100, 120))
    val pages  = packer.pack(buf(rect("a", 50, 100), rect("b", 50, 100)))

    assertEquals(pages.size, 1, "both rects fit on one page")
    val page = pages(0)
    assertEquals(page.outputRects.size, 2)

    val byX = page.outputRects.sortBy(_.x).toList
    assertEquals(byX.map(r => (r.x, r.y)), List((0, 0), (50, 0)), "the right-side free split must place the second rect exactly at x=50,y=0")
    assertEquals(page.width, 100)
    assertEquals(page.height, 100)
    assertNoOverlap(pages)
    assertInBounds(pages)
  }

  // --- free-space split observable: small rect fills the notch ----------
  //
  // One 100x100 (big) plus a 100x40 (wide) and a 40x60 (small). Constrain the
  // page to 100 wide so the layout is a vertical stack: big at y=0 (h100),
  // wide at y=100 (h40). That leaves a free strip at y=140 of height up to
  // maxHeight; the small 40x60 must drop into x=0,y=140. maxHeight=200 forbids
  // any wider arrangement. This exercises the BOTTOM-side splitFreeNode branch
  // and pruneFreeList (the leftover strips below each placed rect). If
  // splitFreeNode dropped the bottom free node, the small rect would have
  // nowhere to go and spill to a second page.

  test("ISS561 stacked rects: bottom-side free split places the third rect at y=140") {
    val packer = new MaxRectsPacker(baseSettings(100, 200))
    val pages  = packer.pack(buf(rect("big", 100, 100), rect("wide", 100, 40), rect("small", 40, 60)))

    assertEquals(pages.size, 1, "all three rects must fit on a single 100x200 page")
    val page   = pages(0)
    val byName = page.outputRects.map(r => r.name.get -> ((r.x, r.y))).toMap
    assertEquals(byName("big"), (0, 0))
    assertEquals(byName("wide"), (0, 100))
    assertEquals(byName("small"), (0, 140), "the bottom-side free split (below 'wide') must be available for 'small' at y=140")
    assertEquals(page.height, 200)
    assertNoOverlap(pages)
    assertInBounds(pages)
  }

  // --- multi-page overflow ---------------------------------------------
  //
  // Four 600x600 rects with maxWidth=maxHeight=1024: only ONE fits per page
  // (two side-by-side would need 1200 > 1024). Every input rect must land on
  // exactly one page; total page count == 4.

  test("ISS561 oversized rects overflow to one page each; every rect placed once") {
    val packer = new MaxRectsPacker(baseSettings(1024, 1024))
    val input  = buf(rect("r0", 600, 600), rect("r1", 600, 600), rect("r2", 600, 600), rect("r3", 600, 600))
    val pages  = packer.pack(input)

    assertEquals(pages.size, 4, "four 600x600 rects cannot share a 1024 page; one per page")
    pages.foreach(p => assertEquals(p.outputRects.size, 1, s"each page holds exactly one rect"))

    val placedNames = allPlaced(pages).map(_.name.get).toSet
    assertEquals(placedNames, Set("r0", "r1", "r2", "r3"), "every input rect placed exactly once")
    assertEquals(allPlaced(pages).size, 4, "no rect placed twice")
    pages.foreach { p =>
      val r = p.outputRects(0)
      assertEquals((r.x, r.y), (0, 0), "the single rect on each page sits at the origin")
    }
  }

  // --- larger multi-rect page: strong invariants + occupancy ------------
  //
  // Six varied rects that all fit on one generous page. The exact per-rect
  // coordinates depend on which heuristic wins, so assert the strong,
  // mutation-sensitive invariants: exactly one page, all six placed once, no
  // overlap, all in bounds. A pruneFreeList over-prune or a scoreRect
  // mutation that loses a free region would force a second page (or an
  // overlap), failing these.

  test("ISS561 six varied rects: single page, all placed, no overlap, in bounds") {
    val packer = new MaxRectsPacker(baseSettings(256, 256))
    val input  = buf(
      rect("p", 60, 60),
      rect("q", 100, 30),
      rect("r", 30, 100),
      rect("s", 70, 70),
      rect("t", 40, 90),
      rect("u", 90, 40)
    )
    val pages = packer.pack(input)

    assertEquals(pages.size, 1, "all six rects fit on one 256x256 page")
    assertEquals(allPlaced(pages).map(_.name.get).toSet, Set("p", "q", "r", "s", "t", "u"))
    assertEquals(allPlaced(pages).size, 6, "no rect placed twice or dropped")
    assertNoOverlap(pages)
    assertInBounds(pages)
  }

  // --- padding: paddingX/Y inflate each rect by exactly the pad ----------
  //
  // pack() adds paddingX to width and paddingY to height of every rect BEFORE
  // packing (MaxRectsPacker.scala 60-65 / .java 59-63), so the placed rect's
  // EXTENT grows by exactly the pad. A single 100x50 rect with paddingX=6,
  // paddingY=4 -> placed width 106, height 54 at (0,0). If padding were
  // ignored the rect would stay 100x50.
  //
  // Note: packPage subtracts paddingX/Y back off Page.width/height at the end
  // (MaxRectsPacker.scala 248-249), so the page bbox is 100x50 here — the
  // rect-level extent (106x54) is the padding signal, asserted directly.

  test("ISS561 padding inflates each rect's packed extent by exactly the pad") {
    val s = baseSettings(512, 512)
    s.paddingX = 6
    s.paddingY = 4
    s.edgePadding = false
    val packer = new MaxRectsPacker(s)
    val pages  = packer.pack(buf(rect("pad", 100, 50)))

    assertEquals(pages.size, 1)
    val page = pages(0)
    val r    = page.outputRects(0)
    assertEquals((r.x, r.y), (0, 0))
    // width 100 + paddingX 6 = 106; height 50 + paddingY 4 = 54.
    assertEquals(r.width, 106, "paddingX must be added to the rect width before packing")
    assertEquals(r.height, 54, "paddingY must be added to the rect height before packing")
    // Page.width/height = bbox(106,54) minus paddingX/Y(6,4) = 100,50.
    assertEquals(page.width, 100, "page bbox = padded extent minus paddingX (packPage line 248)")
    assertEquals(page.height, 50, "page bbox = padded extent minus paddingY (packPage line 249)")
  }

  // --- padding + two rects: exact padded offset between placements -------
  //
  // Two 40x100 rects, paddingX=10 -> each becomes 50 wide. maxWidth=100 fits
  // exactly two side-by-side; maxHeight=120 forbids stacking (height 200>120).
  // So x in {0,50}: the second rect's x is shifted by the FULL padded width
  // (40+10), proving padding feeds into splitFreeNode's right-side offset.
  // Drop the padding and the second rect would sit at x=40, not x=50.

  test("ISS561 padded rects: second placement offset by the full padded width (x=50)") {
    val s = baseSettings(100, 120)
    s.paddingX = 10
    s.paddingY = 0
    s.edgePadding = false
    val packer = new MaxRectsPacker(s)
    val pages  = packer.pack(buf(rect("a", 40, 100), rect("b", 40, 100)))

    assertEquals(pages.size, 1, "two 50-wide (40+10 pad) rects fit side-by-side in width 100")
    val page = pages(0)
    val byX  = page.outputRects.sortBy(_.x).toList
    assertEquals(byX.map(_.x), List(0, 50), "right-side split offset must use the padded width 50, not the raw 40")
    byX.foreach(r => assertEquals(r.width, 50, "each rect is 40 + paddingX 10 wide"))
    // Non-overlap is padding-independent; in-bounds vs Page.width is skipped
    // here because packPage subtracts paddingX off Page.width at the end.
    assertNoOverlap(pages)
  }

  // --- BinarySearch sizing: smallest page is chosen ---------------------
  //
  // A single 200x200 rect with pot=false: getResult bbox is exactly 200x200
  // regardless of bin size, but occupancy = used/(bin) drives getBest to keep
  // the SMALLEST bin that still fits. We assert the placement is at (0,0) and
  // occupancy is high (>= 0.5): a 200x200 rect in any bin the search keeps
  // (it shrinks the bin toward 200) yields occupancy >= 200*200/(bin^2). If
  // the binary search failed to shrink (kept maxWidth=1024) occupancy would be
  // ~0.038, far below 0.5. This pins that packPage actually minimizes the bin.

  test("ISS561 binary search shrinks the bin: high occupancy for a single rect") {
    val packer = new MaxRectsPacker(baseSettings(1024, 1024))
    val pages  = packer.pack(buf(rect("z", 200, 200)))

    assertEquals(pages.size, 1)
    val page = pages(0)
    assertEquals((page.outputRects(0).x, page.outputRects(0).y), (0, 0))
    assertEquals(page.width, 200)
    assertEquals(page.height, 200)
    assert(
      page.occupancy >= 0.5f,
      s"binary search must shrink the bin toward the rect (occupancy ${page.occupancy} too low; " +
        s"if it kept the 1024 max it would be ~0.038)"
    )
  }
}
