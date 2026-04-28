/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/tools/texturepacker/GridPacker.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: com.badlogic.gdx.tools -> sge.tools
 *   Convention: Java collections -> Scala collections
 *   Idiom: Scala 3 enums, boundary/break for control flow
 *   Audited: 2026-03-08
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 132
 * Covenant-baseline-methods: GridPacker,adjustX,adjustY,cellHeight,cellWidth,i,j,maxHeight,maxWidth,n,pack,packPage,paddingX,paddingY,page,pages,reversed,x,y
 * Covenant-source-reference: com/badlogic/gdx/tools/texturepacker/GridPacker.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 62773d804463ceecdb53d87aa8d02025ae3ef2e5
 */
package sge
package tools
package texturepacker

import sge.tools.texturepacker.TexturePacker.*

import scala.collection.mutable.ArrayBuffer
import scala.util.boundary
import scala.util.boundary.break

/** @author
  *   Nathan Sweet
  */
class GridPacker(val settings: Settings) extends Packer {

  override def pack(inputRects: ArrayBuffer[Rect]): ArrayBuffer[Page] =
    pack(null, inputRects) // @nowarn("msg=deprecated") null progress for simple overload

  override def pack(progress: ProgressListener, inputRects: ArrayBuffer[Rect]): ArrayBuffer[Page] = {
    if (!settings.silent) System.out.print("Packing")

    // Rects are packed with right and top padding, so the max size is increased to match. After packing the padding is
    // subtracted from the page size.
    val paddingX = settings.paddingX
    val paddingY = settings.paddingY
    var adjustX  = paddingX
    var adjustY  = paddingY
    if (settings.edgePadding) {
      if (settings.duplicatePadding) {
        adjustX -= paddingX
        adjustY -= paddingY
      } else {
        adjustX -= paddingX * 2
        adjustY -= paddingY * 2
      }
    }
    val maxWidth  = settings.maxWidth + adjustX
    val maxHeight = settings.maxHeight + adjustY

    val n          = inputRects.size
    var cellWidth  = 0
    var cellHeight = 0
    var i          = 0
    while (i < n) {
      val rect = inputRects(i)
      cellWidth = Math.max(cellWidth, rect.width)
      cellHeight = Math.max(cellHeight, rect.height)
      i += 1
    }
    cellWidth += paddingX
    cellHeight += paddingY

    // Reverse so we can remove from end efficiently.
    val reversed = inputRects.reverse
    inputRects.clear()
    inputRects.addAll(reversed)

    val pages = ArrayBuffer.empty[Page]
    boundary {
      while (inputRects.nonEmpty) {
        if (progress != null) { // @nowarn("msg=deprecated") progress can be null
          progress.count = n - inputRects.size + 1
          if (progress.update(progress.count, n)) break()
        }

        val page = packPage(inputRects, cellWidth, cellHeight, maxWidth, maxHeight)
        page.width -= paddingX
        page.height -= paddingY
        pages += page
      }
    }
    pages
  }

  private def packPage(
    inputRects: ArrayBuffer[Rect],
    cellWidth:  Int,
    cellHeight: Int,
    maxWidth:   Int,
    maxHeight:  Int
  ): Page = {
    val page = Page()
    page.outputRects = ArrayBuffer.empty

    val n = inputRects.size
    var x = 0
    var y = 0
    var i = n - 1
    boundary {
      while (i >= 0) {
        if (x + cellWidth > maxWidth) {
          y += cellHeight
          if (y > maxHeight - cellHeight) break()
          x = 0
        }
        val rect = inputRects.remove(i)
        rect.x = x
        rect.y = y
        rect.width += settings.paddingX
        rect.height += settings.paddingY
        page.outputRects += rect
        x += cellWidth
        page.width = Math.max(page.width, x)
        page.height = Math.max(page.height, y + cellHeight)
        i -= 1
      }
    }

    // Flip so rows start at top.
    var j = page.outputRects.size - 1
    while (j >= 0) {
      val rect = page.outputRects(j)
      rect.y = page.height - rect.y - rect.height
      j -= 1
    }
    page
  }
}
