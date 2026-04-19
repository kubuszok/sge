/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/tools/texturepacker/ImageProcessor.java
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
 * Covenant-baseline-loc: 502
 * Covenant-baseline-methods: ImageProcessor,addImage,alphaRaster,bottom,breakA,clear,crcs,currentImage,dotIndex,emptyImage,end,endX,endY,getPads,getSplitPoint,getSplits,hash,hashInt,height,image,images,index,indexPattern,isPatch,name,next,pads,processImage,raster,rect,rects,resampling,rgba,right,scale,splitError,splits,startX,startY,stripWhitespace,width,x,y
 * Covenant-source-reference: com/badlogic/gdx/tools/texturepacker/ImageProcessor.java
 * Covenant-verified: 2026-04-19
 */
package sge
package tools
package texturepacker

import sge.tools.texturepacker.TexturePacker.{ Alias, Rect, Resampling, Settings }
import sge.utils.Nullable

import java.awt.{ Graphics2D, Image, RenderingHints }
import java.awt.image.{ BufferedImage, WritableRaster }
import java.io.{ File, IOException }
import java.math.BigInteger
import java.security.{ MessageDigest, NoSuchAlgorithmException }
import java.util.regex.{ Matcher, Pattern }
import javax.imageio.ImageIO
import scala.collection.mutable.{ ArrayBuffer, HashMap }
import scala.util.boundary
import scala.util.boundary.break

class ImageProcessor(val settings: Settings) {
  import ImageProcessor.*

  private val crcs:  HashMap[String, Rect] = HashMap.empty
  private val rects: ArrayBuffer[Rect]     = ArrayBuffer.empty
  var scale:         Float                 = 1f
  var resampling:    Resampling            = Resampling.bicubic

  /** The image won't be kept in-memory during packing if [[Settings.limitMemory]] is true.
    * @param rootPath
    *   Used to strip the root directory prefix from image file names, can be null.
    */
  def addImage(file: File, rootPath: Nullable[String]): Nullable[Rect] = {
    val image: BufferedImage =
      try ImageIO.read(file)
      catch {
        case ex: IOException =>
          throw new RuntimeException("Error reading image: " + file, ex)
      }
    if (image == null) throw new RuntimeException("Unable to read image: " + file) // @nowarn("msg=deprecated") ImageIO returns null

    var name: String =
      try file.getCanonicalPath()
      catch { case _: IOException => file.getAbsolutePath() }
    name = name.replace('\\', '/')

    // Strip root dir off front of image path.
    rootPath.foreach { rp =>
      if (!name.startsWith(rp)) throw new RuntimeException("Path '" + name + "' does not start with root: " + rp)
      name = name.substring(rp.length)
    }

    // Strip extension.
    val dotIndex = name.lastIndexOf('.')
    if (dotIndex != -1) name = name.substring(0, dotIndex)

    val rect = addImage(image, name)
    rect.foreach { r =>
      if (settings.limitMemory) r.unloadImage(file)
    }
    rect
  }

  /** The image will be kept in-memory during packing.
    * @see
    *   [[addImage(File, String)]]
    */
  def addImage(image: BufferedImage, name: String): Nullable[Rect] = {
    val rect = processImage(image, name)

    if (rect.isEmpty) {
      if (!settings.silent) System.out.println("Ignoring blank input image: " + name)
      Nullable.empty
    } else {
      val r = rect.get
      if (settings.alias) {
        val crc      = hash(r.getImage(this))
        val existing = crcs.get(crc)
        if (existing.isDefined) {
          if (!settings.silent) {
            val rectName     = r.name.get + (if (r.index != -1) "_" + r.index else "")
            val existingName = existing.get.name.get + (if (existing.get.index != -1) "_" + existing.get.index else "")
            System.out.println(rectName + " (alias of " + existingName + ")")
          }
          existing.get.aliases += Alias(r)
          Nullable.empty
        } else {
          crcs.put(crc, r)
          rects += r
          rect
        }
      } else {
        rects += r
        rect
      }
    }
  }

  def images: ArrayBuffer[Rect] = rects

  def clear(): Unit = {
    rects.clear()
    crcs.clear()
  }

  /** Returns a rect for the image describing the texture region to be packed, or null if the image should not be packed. */
  def processImage(image: BufferedImage, nameArg: String): Nullable[Rect] = {
    if (scale <= 0) throw new IllegalArgumentException("scale cannot be <= 0: " + scale)

    var name   = nameArg
    var width  = image.getWidth()
    var height = image.getHeight()

    var currentImage = image
    if (currentImage.getType() != BufferedImage.TYPE_4BYTE_ABGR) {
      val newImage = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR)
      newImage.getGraphics().drawImage(currentImage, 0, 0, null) // @nowarn("msg=deprecated") AWT observer null
      currentImage = newImage
    }

    val isPatch = name.endsWith(".9")
    var splits: Nullable[Array[Int]] = Nullable.empty
    var pads:   Nullable[Array[Int]] = Nullable.empty
    if (isPatch) {
      // Strip ".9" from file name, read ninepatch split pixels, and strip ninepatch split pixels.
      name = name.substring(0, name.length - 2)
      splits = getSplits(currentImage, name)
      pads = getPads(currentImage, name, splits)
      // Strip split pixels.
      width -= 2
      height -= 2
      val newImage = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR)
      newImage.getGraphics().drawImage(currentImage, 0, 0, width, height, 1, 1, width + 1, height + 1, null) // @nowarn("msg=deprecated") AWT observer null
      currentImage = newImage
    }

    // Scale image.
    if (scale != 1) {
      width = Math.max(1, Math.round(width * scale))
      height = Math.max(1, Math.round(height * scale))
      val newImage = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR)
      if (scale < 1) {
        newImage
          .getGraphics()
          .drawImage(
            currentImage.getScaledInstance(width, height, Image.SCALE_AREA_AVERAGING),
            0,
            0,
            null // @nowarn("msg=deprecated") AWT observer null
          )
      } else {
        val g = newImage.getGraphics().asInstanceOf[Graphics2D]
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, resampling.value)
        g.drawImage(currentImage, 0, 0, width, height, null) // @nowarn("msg=deprecated") AWT observer null
      }
      currentImage = newImage
    }

    // Strip digits off end of name and use as index.
    var index = -1
    if (settings.useIndexes) {
      val matcher = indexPattern.matcher(name)
      if (matcher.matches()) {
        name = matcher.group(1)
        index = Integer.parseInt(matcher.group(2))
      }
    }

    var rect: Nullable[Rect] = Nullable.empty
    if (isPatch) {
      // Ninepatches aren't rotated or whitespace stripped.
      val r = Rect(currentImage, 0, 0, width, height, true)
      r.splits = splits
      r.pads = pads
      r.canRotate = false
      rect = Nullable(r)
    } else {
      rect = stripWhitespace(name, currentImage)
      if (rect.isEmpty) Nullable.empty
    }

    rect.foreach { r =>
      r.name = Nullable(name)
      r.index = index
    }
    rect
  }

  /** Strips whitespace and returns the rect, or null if the image should be ignored. */
  protected def stripWhitespace(name: String, source: BufferedImage): Nullable[Rect] = {
    val alphaRaster = source.getAlphaRaster()
    if (alphaRaster == null || (!settings.stripWhitespaceX && !settings.stripWhitespaceY)) // @nowarn("msg=deprecated") AWT returns null
      Nullable(Rect(source, 0, 0, source.getWidth(), source.getHeight(), false))
    else {
      val a      = new Array[Byte](1)
      var top    = 0
      var bottom = source.getHeight()
      if (settings.stripWhitespaceY) {
        // Find top
        boundary {
          var y = 0
          while (y < source.getHeight()) {
            var x = 0
            while (x < source.getWidth()) {
              alphaRaster.getDataElements(x, y, a)
              var alpha = a(0).toInt
              if (alpha < 0) alpha += 256
              if (alpha > settings.alphaThreshold) break()
              x += 1
            }
            top += 1
            y += 1
          }
        }
        // Find bottom
        boundary {
          var y = source.getHeight() - 1
          while (y >= top) {
            var x = 0
            while (x < source.getWidth()) {
              alphaRaster.getDataElements(x, y, a)
              var alpha = a(0).toInt
              if (alpha < 0) alpha += 256
              if (alpha > settings.alphaThreshold) break()
              x += 1
            }
            bottom -= 1
            y -= 1
          }
        }
        // Leave 1px so nothing is copied into padding.
        if (settings.duplicatePadding) {
          if (top > 0) top -= 1
          if (bottom < source.getHeight()) bottom += 1
        }
      }
      var left  = 0
      var right = source.getWidth()
      if (settings.stripWhitespaceX) {
        // Find left
        boundary {
          var x = 0
          while (x < source.getWidth()) {
            var y = top
            while (y < bottom) {
              alphaRaster.getDataElements(x, y, a)
              var alpha = a(0).toInt
              if (alpha < 0) alpha += 256
              if (alpha > settings.alphaThreshold) break()
              y += 1
            }
            left += 1
            x += 1
          }
        }
        // Find right
        boundary {
          var x = source.getWidth() - 1
          while (x >= left) {
            var y = top
            while (y < bottom) {
              alphaRaster.getDataElements(x, y, a)
              var alpha = a(0).toInt
              if (alpha < 0) alpha += 256
              if (alpha > settings.alphaThreshold) break()
              y += 1
            }
            right -= 1
            x -= 1
          }
        }
        // Leave 1px so nothing is copied into padding.
        if (settings.duplicatePadding) {
          if (left > 0) left -= 1
          if (right < source.getWidth()) right += 1
        }
      }
      val newWidth  = right - left
      val newHeight = bottom - top
      if (newWidth <= 0 || newHeight <= 0) {
        if (settings.ignoreBlankImages) Nullable.empty
        else Nullable(Rect(emptyImage, 0, 0, 1, 1, false))
      } else {
        Nullable(Rect(source, left, top, newWidth, newHeight, false))
      }
    }
  }

  /** Returns the splits, or null if the image had no splits or the splits were only a single region. Splits are an int[4] that has left, right, top, bottom.
    */
  private def getSplits(image: BufferedImage, name: String): Nullable[Array[Int]] = {
    val raster = image.getRaster()

    var startX = getSplitPoint(raster, name, 1, 0, startPoint = true, xAxis = true)
    var endX   = getSplitPoint(raster, name, startX, 0, startPoint = false, xAxis = true)
    var startY = getSplitPoint(raster, name, 0, 1, startPoint = true, xAxis = false)
    var endY   = getSplitPoint(raster, name, 0, startY, startPoint = false, xAxis = false)

    // Ensure pixels after the end are not invalid.
    getSplitPoint(raster, name, endX + 1, 0, startPoint = true, xAxis = true)
    getSplitPoint(raster, name, 0, endY + 1, startPoint = true, xAxis = false)

    // No splits, or all splits.
    if (startX == 0 && endX == 0 && startY == 0 && endY == 0) Nullable.empty
    else {
      // Subtraction here is because the coordinates were computed before the 1px border was stripped.
      if (startX != 0) {
        startX -= 1
        endX = raster.getWidth() - 2 - (endX - 1)
      } else {
        // If no start point was ever found, we assume full stretch.
        endX = raster.getWidth() - 2
      }
      if (startY != 0) {
        startY -= 1
        endY = raster.getHeight() - 2 - (endY - 1)
      } else {
        // If no start point was ever found, we assume full stretch.
        endY = raster.getHeight() - 2
      }

      if (scale != 1) {
        startX = Math.round(startX * scale)
        endX = Math.round(endX * scale)
        startY = Math.round(startY * scale)
        endY = Math.round(endY * scale)
      }

      Nullable(Array(startX, endX, startY, endY))
    }
  }

  /** Returns the pads, or Nullable.empty if the image had no pads or the pads match the splits. Pads are an int[4] that has left, right, top, bottom.
    */
  private def getPads(image: BufferedImage, name: String, splits: Nullable[Array[Int]]): Nullable[Array[Int]] = {
    val raster = image.getRaster()

    val bottom = raster.getHeight() - 1
    val right  = raster.getWidth() - 1

    var startX = getSplitPoint(raster, name, 1, bottom, startPoint = true, xAxis = true)
    var startY = getSplitPoint(raster, name, right, 1, startPoint = true, xAxis = false)

    // No need to hunt for the end if a start was never found.
    var endX = 0
    var endY = 0
    if (startX != 0) endX = getSplitPoint(raster, name, startX + 1, bottom, startPoint = false, xAxis = true)
    if (startY != 0) endY = getSplitPoint(raster, name, right, startY + 1, startPoint = false, xAxis = false)

    // Ensure pixels after the end are not invalid.
    getSplitPoint(raster, name, endX + 1, bottom, startPoint = true, xAxis = true)
    getSplitPoint(raster, name, right, endY + 1, startPoint = true, xAxis = false)

    // No pads.
    if (startX == 0 && endX == 0 && startY == 0 && endY == 0) {
      Nullable.empty
    } else {
      // -2 here is because the coordinates were computed before the 1px border was stripped.
      if (startX == 0 && endX == 0) {
        startX = -1
        endX = -1
      } else {
        if (startX > 0) {
          startX -= 1
          endX = raster.getWidth() - 2 - (endX - 1)
        } else {
          // If no start point was ever found, we assume full stretch.
          endX = raster.getWidth() - 2
        }
      }
      if (startY == 0 && endY == 0) {
        startY = -1
        endY = -1
      } else {
        if (startY > 0) {
          startY -= 1
          endY = raster.getHeight() - 2 - (endY - 1)
        } else {
          // If no start point was ever found, we assume full stretch.
          endY = raster.getHeight() - 2
        }
      }

      if (scale != 1) {
        startX = Math.round(startX * scale)
        endX = Math.round(endX * scale)
        startY = Math.round(startY * scale)
        endY = Math.round(endY * scale)
      }

      val pads = Array(startX, endX, startY, endY)

      if (splits.isDefined && java.util.Arrays.equals(pads, splits.get)) {
        Nullable.empty
      } else Nullable(pads)
    }
  }
}

object ImageProcessor {
  private val emptyImage:   BufferedImage = new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR)
  private val indexPattern: Pattern       = Pattern.compile("(.+)_(\\d+)$")

  private def splitError(x: Int, y: Int, rgba: Array[Int], name: String): String =
    throw new RuntimeException(
      "Invalid " + name + " ninepatch split pixel at " + x + ", " + y + ", rgba: " + rgba(0) + ", "
        + rgba(1) + ", " + rgba(2) + ", " + rgba(3)
    )

  /** Hunts for the start or end of a sequence of split pixels. Begins searching at (startX, startY) then follows along the x or y axis (depending on value of xAxis) for the first non-transparent
    * pixel if startPoint is true, or the first transparent pixel if startPoint is false. Returns 0 if none found, as 0 is considered an invalid split point being in the outer border which will be
    * stripped.
    */
  private def getSplitPoint(
    raster:     WritableRaster,
    name:       String,
    startX:     Int,
    startY:     Int,
    startPoint: Boolean,
    xAxis:      Boolean
  ): Int = {
    val rgba = new Array[Int](4)

    var next   = if (xAxis) startX else startY
    val end    = if (xAxis) raster.getWidth() else raster.getHeight()
    val breakA = if (startPoint) 255 else 0

    var x = startX
    var y = startY
    boundary {
      while (next != end) {
        if (xAxis) x = next
        else y = next

        raster.getPixel(x, y, rgba)
        if (rgba(3) == breakA) break(next)

        if (!startPoint && (rgba(0) != 0 || rgba(1) != 0 || rgba(2) != 0 || rgba(3) != 255))
          splitError(x, y, rgba, name)

        next += 1
      }
      0
    }
  }

  private def hash(image: BufferedImage): String =
    try {
      val digest = MessageDigest.getInstance("SHA1")

      // Ensure image is the correct format.
      val width  = image.getWidth()
      val height = image.getHeight()
      var img    = image
      if (img.getType() != BufferedImage.TYPE_INT_ARGB) {
        val newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        newImage.getGraphics().drawImage(img, 0, 0, null) // @nowarn("msg=deprecated") AWT observer null
        img = newImage
      }

      val raster = img.getRaster()
      val pixels = new Array[Int](width)
      var y      = 0
      while (y < height) {
        raster.getDataElements(0, y, width, 1, pixels)
        var x = 0
        while (x < width) {
          hashInt(digest, pixels(x))
          x += 1
        }
        y += 1
      }

      hashInt(digest, width)
      hashInt(digest, height)

      new BigInteger(1, digest.digest()).toString(16)
    } catch {
      case ex: NoSuchAlgorithmException =>
        throw new RuntimeException(ex)
    }

  private def hashInt(digest: MessageDigest, value: Int): Unit = {
    digest.update((value >> 24).toByte)
    digest.update((value >> 16).toByte)
    digest.update((value >> 8).toByte)
    digest.update(value.toByte)
  }
}
