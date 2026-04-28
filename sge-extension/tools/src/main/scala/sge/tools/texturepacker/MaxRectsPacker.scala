/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/tools/texturepacker/MaxRectsPacker.java
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
 * Covenant-baseline-loc: 881
 * Covenant-baseline-methods: BinarySearch,FreeRectChoiceHeuristic,MaxRects,MaxRectsPacker,adjustX,adjustY,bestResult,binHeight,binWidth,commonIntervalLength,contactPointScoreNode,current,edgePadX,edgePadY,findPositionForNewNodeBestAreaFit,findPositionForNewNodeBestLongSideFit,findPositionForNewNodeBestShortSideFit,findPositionForNewNodeBottomLeft,findPositionForNewNodeContactPoint,freeRectangles,fuzziness,getBest,getOccupancy,getResult,high,i,ii,init,insert,isContainedIn,low,max,maxHeight,maxRects,maxWidth,methods,min,minHeight,minWidth,n,next,nn,pack,packAtSize,packPage,paddingX,paddingY,pages,placeRect,pruneFreeList,rectComparator,rectanglesToCheckWhenPruning,remaining,reset,scoreRect,splitFreeNode,usedRectangles
 * Covenant-source-reference: com/badlogic/gdx/tools/texturepacker/MaxRectsPacker.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 435da504c87b735778e89dca44ba1812fadffb55
 */
package sge
package tools
package texturepacker

import sge.math.MathUtils
import sge.tools.texturepacker.TexturePacker.*
import sge.utils.Nullable

import scala.collection.mutable.{ ArrayBuffer, HashSet }
import scala.util.boundary
import scala.util.boundary.break

/** Packs pages of images using the maximal rectangles bin packing algorithm by Jukka Jylanki. A brute force binary search is used to pack into the smallest bin possible.
  * @author
  *   Nathan Sweet
  */
class MaxRectsPacker(val settings: Settings) extends Packer {
  import MaxRectsPacker.*

  private val methods:  Array[FreeRectChoiceHeuristic] = FreeRectChoiceHeuristic.values
  private val maxRects: MaxRects                       = MaxRects(settings)

  private val rectComparator: Ordering[Rect] = Ordering.by(r => Rect.getAtlasName(r.name.get, settings.flattenPaths))

  {
    if (settings.minWidth > settings.maxWidth) throw new RuntimeException("Page min width cannot be higher than max width.")
    if (settings.minHeight > settings.maxHeight)
      throw new RuntimeException("Page min height cannot be higher than max height.")
  }

  override def pack(inputRects: ArrayBuffer[Rect]): ArrayBuffer[Page] =
    pack(null, inputRects) // @nowarn("msg=deprecated") null progress for simple overload

  override def pack(progress: ProgressListener, inputRects: ArrayBuffer[Rect]): ArrayBuffer[Page] = {
    val n = inputRects.size
    var i = 0
    while (i < n) {
      val rect = inputRects(i)
      rect.width += settings.paddingX
      rect.height += settings.paddingY
      i += 1
    }

    if (settings.fast) {
      if (settings.rotation) {
        // Sort by longest side if rotation is enabled.
        inputRects.sortInPlace()(using
          Ordering.fromLessThan { (o1, o2) =>
            val n1 = Math.max(o1.width, o1.height)
            val n2 = Math.max(o2.width, o2.height)
            n2 - n1 < 0
          }
        )
      } else {
        // Sort only by width (largest to smallest) if rotation is disabled.
        inputRects.sortInPlace()(using
          Ordering.fromLessThan { (o1, o2) =>
            o2.width - o1.width < 0
          }
        )
      }
    }

    val pages     = ArrayBuffer.empty[Page]
    var remaining = inputRects
    boundary {
      while (remaining.nonEmpty) {
        if (progress != null) { // @nowarn("msg=deprecated") progress can be null
          progress.count = n - remaining.size + 1
          if (progress.update(progress.count, n)) break()
        }

        val result = packPage(remaining)
        pages += result
        remaining = result.remainingRects
      }
    }
    pages
  }

  private def packPage(inputRects: ArrayBuffer[Rect]): Page = {
    val paddingX = settings.paddingX
    val paddingY = settings.paddingY
    var maxWidth:  Float = settings.maxWidth
    var maxHeight: Float = settings.maxHeight
    var edgePadX = false
    var edgePadY = false
    if (settings.edgePadding) {
      if (settings.duplicatePadding) {
        maxWidth -= paddingX
        maxHeight -= paddingY
      } else {
        maxWidth -= paddingX * 2
        maxHeight -= paddingY * 2
      }
      edgePadX = paddingX > 0
      edgePadY = paddingY > 0
    }

    // Find min size.
    var minWidth  = Int.MaxValue
    var minHeight = Int.MaxValue
    var ii        = 0
    val nn        = inputRects.size
    while (ii < nn) {
      val rect   = inputRects(ii)
      val width  = rect.width - paddingX
      val height = rect.height - paddingY
      minWidth = Math.min(minWidth, width)
      minHeight = Math.min(minHeight, height)
      if (settings.rotation) {
        if ((width > maxWidth || height > maxHeight) && (width > maxHeight || height > maxWidth)) {
          val paddingMessage =
            if (edgePadX || edgePadY) " and edge padding " + paddingX + "*2," + paddingY + "*2" else ""
          throw new RuntimeException(
            "Image does not fit within max page size " + settings.maxWidth + "x"
              + settings.maxHeight + paddingMessage + ": " + rect.name.get + " " + width + "x" + height
          )
        }
      } else {
        if (width > maxWidth) {
          val paddingMessage = if (edgePadX) " and X edge padding " + paddingX + "*2" else ""
          throw new RuntimeException(
            "Image does not fit within max page width " + settings.maxWidth + paddingMessage + ": "
              + rect.name.get + " " + width + "x" + height
          )
        }
        if (height > maxHeight) {
          val paddingMessage = if (edgePadY) " and Y edge padding " + paddingY + "*2" else ""
          throw new RuntimeException(
            "Image does not fit within max page height " + settings.maxHeight + paddingMessage
              + ": " + rect.name.get + " " + width + "x" + height
          )
        }
      }
      ii += 1
    }
    minWidth = Math.max(minWidth, settings.minWidth)
    minHeight = Math.max(minHeight, settings.minHeight)

    // BinarySearch uses the max size. Rects are packed with right and top padding, so the max size is increased to match.
    // After packing the padding is subtracted from the page size.
    var adjustX = paddingX
    var adjustY = paddingY
    if (settings.edgePadding) {
      if (settings.duplicatePadding) {
        adjustX -= paddingX
        adjustY -= paddingY
      } else {
        adjustX -= paddingX * 2
        adjustY -= paddingY * 2
      }
    }

    if (!settings.silent) System.out.print("Packing")

    // Find the minimal page size that fits all rects.
    var bestResult: Nullable[Page] = Nullable.empty
    if (settings.square) {
      val minSize    = Math.max(minWidth, minHeight)
      val maxSize    = Math.min(settings.maxWidth, settings.maxHeight)
      val sizeSearch =
        BinarySearch(minSize, maxSize, if (settings.fast) 25 else 15, settings.pot, settings.multipleOfFour)
      var size = sizeSearch.reset()
      var idx  = 0
      while (size != -1) {
        val result = packAtSize(true, size + adjustX, size + adjustY, inputRects)
        if (!settings.silent) {
          idx += 1
          if (idx % 70 == 0) System.out.println()
          System.out.print(".")
        }
        bestResult = getBest(bestResult, result)
        size = sizeSearch.next(result.isEmpty)
      }
      if (!settings.silent) System.out.println()
      // Rects don't fit on one page. Fill a whole page and return.
      if (bestResult.isEmpty)
        bestResult = packAtSize(false, maxSize + adjustX, maxSize + adjustY, inputRects)
      val best = bestResult.get
      best.outputRects.sortInPlace()(using rectComparator)
      best.width = Math.max(best.width, best.height) - paddingX
      best.height = Math.max(best.width, best.height) - paddingY
      best
    } else {
      val widthSearch =
        BinarySearch(minWidth, settings.maxWidth, if (settings.fast) 25 else 15, settings.pot, settings.multipleOfFour)
      val heightSearch = BinarySearch(
        minHeight,
        settings.maxHeight,
        if (settings.fast) 25 else 15,
        settings.pot,
        settings.multipleOfFour
      )
      var width  = widthSearch.reset()
      var idx    = 0
      var height = if (settings.square) width else heightSearch.reset()
      boundary {
        while (true) {
          var bestWidthResult: Nullable[Page] = Nullable.empty
          while (width != -1) {
            val result = packAtSize(true, width + adjustX, height + adjustY, inputRects)
            if (!settings.silent) {
              idx += 1
              if (idx % 70 == 0) System.out.println()
              System.out.print(".")
            }
            bestWidthResult = getBest(bestWidthResult, result)
            width = widthSearch.next(result.isEmpty)
            if (settings.square) height = width
          }
          bestResult = getBest(bestResult, bestWidthResult)
          if (settings.square) break()
          height = heightSearch.next(bestWidthResult.isEmpty)
          if (height == -1) break()
          width = widthSearch.reset()
        }
      }
      if (!settings.silent) System.out.println()
      // Rects don't fit on one page. Fill a whole page and return.
      if (bestResult.isEmpty)
        bestResult = packAtSize(false, settings.maxWidth + adjustX, settings.maxHeight + adjustY, inputRects)
      val best = bestResult.get
      best.outputRects.sortInPlace()(using rectComparator)
      best.width -= paddingX
      best.height -= paddingY
      best
    }
  }

  /** @param fully
    *   If true, the only results that pack all rects will be considered. If false, all results are considered, not all rects may be packed.
    */
  private def packAtSize(fully: Boolean, width: Int, height: Int, inputRects: ArrayBuffer[Rect]): Nullable[Page] = {
    var bestResult: Nullable[Page] = Nullable.empty
    for (method <- methods) {
      maxRects.init(width, height)
      val result: Page =
        if (!settings.fast) {
          maxRects.pack(inputRects, method)
        } else {
          val remaining = ArrayBuffer.empty[Rect]
          var idx       = 0
          val nn        = inputRects.size
          boundary {
            while (idx < nn) {
              val rect = inputRects(idx)
              if (maxRects.insert(rect, method).isEmpty) {
                while (idx < nn) {
                  remaining += inputRects(idx)
                  idx += 1
                }
              }
              idx += 1
            }
          }
          val r = maxRects.getResult()
          r.remainingRects = remaining
          r
        }
      if (!(fully && result.remainingRects.nonEmpty) && result.outputRects.nonEmpty) {
        bestResult = getBest(bestResult, Nullable(result))
      }
    }
    bestResult
  }

  private def getBest(result1: Nullable[Page], result2: Nullable[Page]): Nullable[Page] =
    if (result1.isEmpty) result2
    else if (result2.isEmpty) result1
    else if (result1.get.occupancy > result2.get.occupancy) result1
    else result2
}

object MaxRectsPacker {

  class BinarySearch(min0: Int, max0: Int, fuzziness0: Int, val pot: Boolean, val mod4: Boolean) {
    val min: Int =
      if (pot) (Math.log(MathUtils.nextPowerOfTwo(min0).toDouble) / Math.log(2)).toInt
      else if (mod4) { if (min0 % 4 == 0) min0 else min0 + 4 - (min0 % 4) }
      else min0
    val max: Int =
      if (pot) (Math.log(MathUtils.nextPowerOfTwo(max0).toDouble) / Math.log(2)).toInt
      else if (mod4) { if (max0 % 4 == 0) max0 else max0 + 4 - (max0 % 4) }
      else max0
    val fuzziness: Int = if (pot) 0 else fuzziness0
    var low:       Int = 0
    var high:      Int = 0
    var current:   Int = 0

    def reset(): Int = {
      low = min
      high = max
      current = (low + high) >>> 1
      if (pot) Math.pow(2, current).toInt
      else if (mod4) { if (current % 4 == 0) current else current + 4 - (current % 4) }
      else current
    }

    def next(result: Boolean): Int =
      if (low >= high) -1
      else {
        if (result) low = current + 1
        else high = current - 1
        current = (low + high) >>> 1
        if (Math.abs(low - high) < fuzziness) -1
        else if (pot) Math.pow(2, current).toInt
        else if (mod4) { if (current % 4 == 0) current else current + 4 - (current % 4) }
        else current
      }
  }

  /** Maximal rectangles bin packing algorithm. Adapted from this C++ public domain source: http://clb.demon.fi/projects/even-more-rectangle-bin-packing
    * @author
    *   Jukka Jylanki
    * @author
    *   Nathan Sweet
    */
  class MaxRects(val settings: Settings) {
    private var binWidth:                     Int               = 0
    private var binHeight:                    Int               = 0
    private val usedRectangles:               ArrayBuffer[Rect] = ArrayBuffer.empty
    private val freeRectangles:               ArrayBuffer[Rect] = ArrayBuffer.empty
    private val rectanglesToCheckWhenPruning: ArrayBuffer[Rect] = ArrayBuffer.empty

    def init(width: Int, height: Int): Unit = {
      binWidth = width
      binHeight = height

      usedRectangles.clear()
      freeRectangles.clear()
      val n = Rect()
      n.x = 0
      n.y = 0
      n.width = width
      n.height = height
      freeRectangles += n
    }

    /** Packs a single image. Order is defined externally. */
    def insert(rect: Rect, method: FreeRectChoiceHeuristic): Nullable[Rect] = {
      val newNode = scoreRect(rect, method)
      if (newNode.height == 0) Nullable.empty
      else {

        var numRectanglesToProcess = freeRectangles.size
        var i                      = 0
        while (i < numRectanglesToProcess) {
          if (splitFreeNode(freeRectangles(i), newNode)) {
            freeRectangles.remove(i)
            i -= 1
            numRectanglesToProcess -= 1
          }
          i += 1
        }

        pruneFreeList()

        val bestNode = Rect()
        bestNode.set(rect)
        bestNode.score1 = newNode.score1
        bestNode.score2 = newNode.score2
        bestNode.x = newNode.x
        bestNode.y = newNode.y
        bestNode.width = newNode.width
        bestNode.height = newNode.height
        bestNode.rotated = newNode.rotated

        usedRectangles += bestNode
        Nullable(bestNode)
      }
    }

    /** For each rectangle, packs each one then chooses the best and packs that. Slow! */
    def pack(rects: ArrayBuffer[Rect], method: FreeRectChoiceHeuristic): Page = {
      val remaining = ArrayBuffer.from(rects)
      boundary {
        while (remaining.nonEmpty) {
          var bestRectIndex = -1
          val bestNode      = Rect()
          bestNode.score1 = Int.MaxValue
          bestNode.score2 = Int.MaxValue

          // Find the next rectangle that packs best.
          var i = 0
          while (i < remaining.size) {
            val newNode = scoreRect(remaining(i), method)
            if (newNode.score1 < bestNode.score1 || (newNode.score1 == bestNode.score1 && newNode.score2 < bestNode.score2)) {
              bestNode.set(remaining(i))
              bestNode.score1 = newNode.score1
              bestNode.score2 = newNode.score2
              bestNode.x = newNode.x
              bestNode.y = newNode.y
              bestNode.width = newNode.width
              bestNode.height = newNode.height
              bestNode.rotated = newNode.rotated
              bestRectIndex = i
            }
            i += 1
          }

          if (bestRectIndex == -1) break()

          placeRect(bestNode)
          remaining.remove(bestRectIndex)
        }
      }

      val result = getResult()
      result.remainingRects = remaining
      result
    }

    def getResult(): Page = {
      var w = 0
      var h = 0
      var i = 0
      while (i < usedRectangles.size) {
        val rect = usedRectangles(i)
        w = Math.max(w, rect.x + rect.width)
        h = Math.max(h, rect.y + rect.height)
        i += 1
      }
      val result = Page()
      result.outputRects = ArrayBuffer.from(usedRectangles)
      result.occupancy = getOccupancy()
      result.width = w
      result.height = h
      result
    }

    private def placeRect(node: Rect): Unit = {
      var numRectanglesToProcess = freeRectangles.size
      var i                      = 0
      while (i < numRectanglesToProcess) {
        if (splitFreeNode(freeRectangles(i), node)) {
          freeRectangles.remove(i)
          i -= 1
          numRectanglesToProcess -= 1
        }
        i += 1
      }

      pruneFreeList()

      usedRectangles += node
    }

    private def scoreRect(rect: Rect, method: FreeRectChoiceHeuristic): Rect = {
      val width         = rect.width
      val height        = rect.height
      val rotatedWidth  = height - settings.paddingY + settings.paddingX
      val rotatedHeight = width - settings.paddingX + settings.paddingY
      val rotate        = rect.canRotate && settings.rotation

      val newNode: Rect = method match {
        case FreeRectChoiceHeuristic.BestShortSideFit =>
          findPositionForNewNodeBestShortSideFit(width, height, rotatedWidth, rotatedHeight, rotate)
        case FreeRectChoiceHeuristic.BottomLeftRule =>
          findPositionForNewNodeBottomLeft(width, height, rotatedWidth, rotatedHeight, rotate)
        case FreeRectChoiceHeuristic.ContactPointRule =>
          val n = findPositionForNewNodeContactPoint(width, height, rotatedWidth, rotatedHeight, rotate)
          n.score1 = -n.score1 // Reverse since we are minimizing, but for contact point score bigger is better.
          n
        case FreeRectChoiceHeuristic.BestLongSideFit =>
          findPositionForNewNodeBestLongSideFit(width, height, rotatedWidth, rotatedHeight, rotate)
        case FreeRectChoiceHeuristic.BestAreaFit =>
          findPositionForNewNodeBestAreaFit(width, height, rotatedWidth, rotatedHeight, rotate)
      }

      // Cannot fit the current rectangle.
      if (newNode.height == 0) {
        newNode.score1 = Int.MaxValue
        newNode.score2 = Int.MaxValue
      }

      newNode
    }

    // Computes the ratio of used surface area.
    private def getOccupancy(): Float = {
      var usedSurfaceArea = 0
      var i               = 0
      while (i < usedRectangles.size) {
        usedSurfaceArea += usedRectangles(i).width * usedRectangles(i).height
        i += 1
      }
      usedSurfaceArea.toFloat / (binWidth * binHeight)
    }

    private def findPositionForNewNodeBottomLeft(
      width:         Int,
      height:        Int,
      rotatedWidth:  Int,
      rotatedHeight: Int,
      rotate:        Boolean
    ): Rect = {
      val bestNode = Rect()
      bestNode.score1 = Int.MaxValue // best y, score2 is best x

      var i = 0
      while (i < freeRectangles.size) {
        val free = freeRectangles(i)
        // Try to place the rectangle in upright (non-rotated) orientation.
        if (free.width >= width && free.height >= height) {
          val topSideY = free.y + height
          if (topSideY < bestNode.score1 || (topSideY == bestNode.score1 && free.x < bestNode.score2)) {
            bestNode.x = free.x
            bestNode.y = free.y
            bestNode.width = width
            bestNode.height = height
            bestNode.score1 = topSideY
            bestNode.score2 = free.x
            bestNode.rotated = false
          }
        }
        if (rotate && free.width >= rotatedWidth && free.height >= rotatedHeight) {
          val topSideY = free.y + rotatedHeight
          if (topSideY < bestNode.score1 || (topSideY == bestNode.score1 && free.x < bestNode.score2)) {
            bestNode.x = free.x
            bestNode.y = free.y
            bestNode.width = rotatedWidth
            bestNode.height = rotatedHeight
            bestNode.score1 = topSideY
            bestNode.score2 = free.x
            bestNode.rotated = true
          }
        }
        i += 1
      }
      bestNode
    }

    private def findPositionForNewNodeBestShortSideFit(
      width:         Int,
      height:        Int,
      rotatedWidth:  Int,
      rotatedHeight: Int,
      rotate:        Boolean
    ): Rect = {
      val bestNode = Rect()
      bestNode.score1 = Int.MaxValue

      var i = 0
      while (i < freeRectangles.size) {
        val free = freeRectangles(i)
        // Try to place the rectangle in upright (non-rotated) orientation.
        if (free.width >= width && free.height >= height) {
          val leftoverHoriz = Math.abs(free.width - width)
          val leftoverVert  = Math.abs(free.height - height)
          val shortSideFit  = Math.min(leftoverHoriz, leftoverVert)
          val longSideFit   = Math.max(leftoverHoriz, leftoverVert)

          if (shortSideFit < bestNode.score1 || (shortSideFit == bestNode.score1 && longSideFit < bestNode.score2)) {
            bestNode.x = free.x
            bestNode.y = free.y
            bestNode.width = width
            bestNode.height = height
            bestNode.score1 = shortSideFit
            bestNode.score2 = longSideFit
            bestNode.rotated = false
          }
        }

        if (rotate && free.width >= rotatedWidth && free.height >= rotatedHeight) {
          val flippedLeftoverHoriz = Math.abs(free.width - rotatedWidth)
          val flippedLeftoverVert  = Math.abs(free.height - rotatedHeight)
          val flippedShortSideFit  = Math.min(flippedLeftoverHoriz, flippedLeftoverVert)
          val flippedLongSideFit   = Math.max(flippedLeftoverHoriz, flippedLeftoverVert)

          if (
            flippedShortSideFit < bestNode.score1
            || (flippedShortSideFit == bestNode.score1 && flippedLongSideFit < bestNode.score2)
          ) {
            bestNode.x = free.x
            bestNode.y = free.y
            bestNode.width = rotatedWidth
            bestNode.height = rotatedHeight
            bestNode.score1 = flippedShortSideFit
            bestNode.score2 = flippedLongSideFit
            bestNode.rotated = true
          }
        }
        i += 1
      }

      bestNode
    }

    private def findPositionForNewNodeBestLongSideFit(
      width:         Int,
      height:        Int,
      rotatedWidth:  Int,
      rotatedHeight: Int,
      rotate:        Boolean
    ): Rect = {
      val bestNode = Rect()
      bestNode.score2 = Int.MaxValue

      var i = 0
      while (i < freeRectangles.size) {
        val free = freeRectangles(i)
        // Try to place the rectangle in upright (non-rotated) orientation.
        if (free.width >= width && free.height >= height) {
          val leftoverHoriz = Math.abs(free.width - width)
          val leftoverVert  = Math.abs(free.height - height)
          val shortSideFit  = Math.min(leftoverHoriz, leftoverVert)
          val longSideFit   = Math.max(leftoverHoriz, leftoverVert)

          if (longSideFit < bestNode.score2 || (longSideFit == bestNode.score2 && shortSideFit < bestNode.score1)) {
            bestNode.x = free.x
            bestNode.y = free.y
            bestNode.width = width
            bestNode.height = height
            bestNode.score1 = shortSideFit
            bestNode.score2 = longSideFit
            bestNode.rotated = false
          }
        }

        if (rotate && free.width >= rotatedWidth && free.height >= rotatedHeight) {
          val leftoverHoriz = Math.abs(free.width - rotatedWidth)
          val leftoverVert  = Math.abs(free.height - rotatedHeight)
          val shortSideFit  = Math.min(leftoverHoriz, leftoverVert)
          val longSideFit   = Math.max(leftoverHoriz, leftoverVert)

          if (longSideFit < bestNode.score2 || (longSideFit == bestNode.score2 && shortSideFit < bestNode.score1)) {
            bestNode.x = free.x
            bestNode.y = free.y
            bestNode.width = rotatedWidth
            bestNode.height = rotatedHeight
            bestNode.score1 = shortSideFit
            bestNode.score2 = longSideFit
            bestNode.rotated = true
          }
        }
        i += 1
      }
      bestNode
    }

    private def findPositionForNewNodeBestAreaFit(
      width:         Int,
      height:        Int,
      rotatedWidth:  Int,
      rotatedHeight: Int,
      rotate:        Boolean
    ): Rect = {
      val bestNode = Rect()
      bestNode.score1 = Int.MaxValue // best area fit, score2 is best short side fit

      var i = 0
      while (i < freeRectangles.size) {
        val free    = freeRectangles(i)
        val areaFit = free.width * free.height - width * height

        // Try to place the rectangle in upright (non-rotated) orientation.
        if (free.width >= width && free.height >= height) {
          val leftoverHoriz = Math.abs(free.width - width)
          val leftoverVert  = Math.abs(free.height - height)
          val shortSideFit  = Math.min(leftoverHoriz, leftoverVert)

          if (areaFit < bestNode.score1 || (areaFit == bestNode.score1 && shortSideFit < bestNode.score2)) {
            bestNode.x = free.x
            bestNode.y = free.y
            bestNode.width = width
            bestNode.height = height
            bestNode.score2 = shortSideFit
            bestNode.score1 = areaFit
            bestNode.rotated = false
          }
        }

        if (rotate && free.width >= rotatedWidth && free.height >= rotatedHeight) {
          val leftoverHoriz = Math.abs(free.width - rotatedWidth)
          val leftoverVert  = Math.abs(free.height - rotatedHeight)
          val shortSideFit  = Math.min(leftoverHoriz, leftoverVert)

          if (areaFit < bestNode.score1 || (areaFit == bestNode.score1 && shortSideFit < bestNode.score2)) {
            bestNode.x = free.x
            bestNode.y = free.y
            bestNode.width = rotatedWidth
            bestNode.height = rotatedHeight
            bestNode.score2 = shortSideFit
            bestNode.score1 = areaFit
            bestNode.rotated = true
          }
        }
        i += 1
      }
      bestNode
    }

    // Returns 0 if the two intervals i1 and i2 are disjoint, or the length of their overlap otherwise.
    private def commonIntervalLength(i1start: Int, i1end: Int, i2start: Int, i2end: Int): Int =
      if (i1end < i2start || i2end < i1start) 0
      else Math.min(i1end, i2end) - Math.max(i1start, i2start)

    private def contactPointScoreNode(x: Int, y: Int, width: Int, height: Int): Int = {
      var score = 0

      if (x == 0 || x + width == binWidth) score += height
      if (y == 0 || y + height == binHeight) score += width

      var i = 0
      val n = usedRectangles.size
      while (i < n) {
        val rect = usedRectangles(i)
        if (rect.x == x + width || rect.x + rect.width == x)
          score += commonIntervalLength(rect.y, rect.y + rect.height, y, y + height)
        if (rect.y == y + height || rect.y + rect.height == y)
          score += commonIntervalLength(rect.x, rect.x + rect.width, x, x + width)
        i += 1
      }
      score
    }

    private def findPositionForNewNodeContactPoint(
      width:         Int,
      height:        Int,
      rotatedWidth:  Int,
      rotatedHeight: Int,
      rotate:        Boolean
    ): Rect = {
      val bestNode = Rect()
      bestNode.score1 = -1 // best contact score

      var i = 0
      val n = freeRectangles.size
      while (i < n) {
        // Try to place the rectangle in upright (non-rotated) orientation.
        val free = freeRectangles(i)
        if (free.width >= width && free.height >= height) {
          val score = contactPointScoreNode(free.x, free.y, width, height)
          if (score > bestNode.score1) {
            bestNode.x = free.x
            bestNode.y = free.y
            bestNode.width = width
            bestNode.height = height
            bestNode.score1 = score
            bestNode.rotated = false
          }
        }
        if (rotate && free.width >= rotatedWidth && free.height >= rotatedHeight) {
          val score = contactPointScoreNode(free.x, free.y, rotatedWidth, rotatedHeight)
          if (score > bestNode.score1) {
            bestNode.x = free.x
            bestNode.y = free.y
            bestNode.width = rotatedWidth
            bestNode.height = rotatedHeight
            bestNode.score1 = score
            bestNode.rotated = true
          }
        }
        i += 1
      }
      bestNode
    }

    private def splitFreeNode(freeNode: Rect, usedNode: Rect): Boolean =
      // Test with SAT if the rectangles even intersect.
      if (
        usedNode.x >= freeNode.x + freeNode.width || usedNode.x + usedNode.width <= freeNode.x
        || usedNode.y >= freeNode.y + freeNode.height || usedNode.y + usedNode.height <= freeNode.y
      ) {
        false
      } else {
        if (usedNode.x < freeNode.x + freeNode.width && usedNode.x + usedNode.width > freeNode.x) {
          // New node at the top side of the used node.
          if (usedNode.y > freeNode.y && usedNode.y < freeNode.y + freeNode.height) {
            val newNode = Rect()
            newNode.copyPositionFrom(freeNode)
            newNode.height = usedNode.y - newNode.y
            freeRectangles += newNode
            rectanglesToCheckWhenPruning += newNode
          }

          // New node at the bottom side of the used node.
          if (usedNode.y + usedNode.height < freeNode.y + freeNode.height) {
            val newNode = Rect()
            newNode.copyPositionFrom(freeNode)
            newNode.y = usedNode.y + usedNode.height
            newNode.height = freeNode.y + freeNode.height - (usedNode.y + usedNode.height)
            freeRectangles += newNode
            rectanglesToCheckWhenPruning += newNode
          }
        }

        if (usedNode.y < freeNode.y + freeNode.height && usedNode.y + usedNode.height > freeNode.y) {
          // New node at the left side of the used node.
          if (usedNode.x > freeNode.x && usedNode.x < freeNode.x + freeNode.width) {
            val newNode = Rect()
            newNode.copyPositionFrom(freeNode)
            newNode.width = usedNode.x - newNode.x
            freeRectangles += newNode
            rectanglesToCheckWhenPruning += newNode
          }

          // New node at the right side of the used node.
          if (usedNode.x + usedNode.width < freeNode.x + freeNode.width) {
            val newNode = Rect()
            newNode.copyPositionFrom(freeNode)
            newNode.x = usedNode.x + usedNode.width
            newNode.width = freeNode.x + freeNode.width - (usedNode.x + usedNode.width)
            freeRectangles += newNode
            rectanglesToCheckWhenPruning += newNode
          }
        }

        true
      }

    private def pruneFreeList(): Unit = {
      val freeRectanglesToRemove = HashSet.empty[Int]

      for (checkingRectangle <- rectanglesToCheckWhenPruning) {
        var i = 0
        while (i < freeRectangles.size) {
          val rect = freeRectangles(i)
          if (!(rect eq checkingRectangle)) {
            if (isContainedIn(rect, checkingRectangle)) {
              freeRectanglesToRemove += i
            }
          }
          i += 1
        }
      }

      rectanglesToCheckWhenPruning.clear()

      if (freeRectanglesToRemove.isEmpty) {}
      else {
        val temporaryFreeRectangles = ArrayBuffer.from(freeRectangles)
        freeRectangles.clear()
        var i = 0
        while (i < temporaryFreeRectangles.size) {
          if (!freeRectanglesToRemove.contains(i)) {
            freeRectangles += temporaryFreeRectangles(i)
          }
          i += 1
        }
      }
    }

    private def isContainedIn(a: Rect, b: Rect): Boolean =
      a.x >= b.x && a.y >= b.y && a.x + a.width <= b.x + b.width && a.y + a.height <= b.y + b.height
  }

  /** BSSF: Positions the rectangle against the short side of a free rectangle into which it fits the best. */
  enum FreeRectChoiceHeuristic {

    /** BSSF: Positions the rectangle against the short side of a free rectangle into which it fits the best. */
    case BestShortSideFit

    /** BLSF: Positions the rectangle against the long side of a free rectangle into which it fits the best. */
    case BestLongSideFit

    /** BAF: Positions the rectangle into the smallest free rect into which it fits. */
    case BestAreaFit

    /** BL: Does the Tetris placement. */
    case BottomLeftRule

    /** CP: Choosest the placement where the rectangle touches other rects as much as possible. */
    case ContactPointRule
  }
}
