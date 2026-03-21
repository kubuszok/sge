/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/tools/texturepacker/ColorBleedEffect.java
 * Original authors: Ruben Garat, Ariel Coppes, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: com.badlogic.gdx.tools -> sge.tools
 *   Convention: Java collections -> Scala collections
 *   Idiom: Scala 3 enums, boundary/break for control flow
 *   Audited: 2026-03-08
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package tools
package texturepacker

import java.awt.image.BufferedImage
import java.util.NoSuchElementException

/** @author
  *   Ruben Garat
  * @author
  *   Ariel Coppes
  * @author
  *   Nathan Sweet
  */
class ColorBleedEffect {
  import ColorBleedEffect.*

  def processImage(image: BufferedImage, maxIterations: Int): BufferedImage = {
    val width  = image.getWidth()
    val height = image.getHeight()

    val processedImage =
      if (image.getType() == BufferedImage.TYPE_INT_ARGB) image
      else new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val rgb  = image.getRGB(0, 0, width, height, null, 0, width) // @nowarn("msg=deprecated") AWT null array for auto-alloc
    val mask = Mask(rgb)

    var iterations  = 0
    var lastPending = -1
    while (mask.pendingSize > 0 && mask.pendingSize != lastPending && iterations < maxIterations) {
      lastPending = mask.pendingSize
      executeIteration(rgb, mask, width, height)
      iterations += 1
    }

    processedImage.setRGB(0, 0, width, height, rgb, 0, width)
    processedImage
  }

  private def executeIteration(rgb: Array[Int], mask: Mask, width: Int, height: Int): Unit = {
    val iterator = mask.MaskIterator()
    while (iterator.hasNext()) {
      val pixelIndex = iterator.next()
      val x          = pixelIndex % width
      val y          = pixelIndex / width
      var r          = 0
      var g          = 0
      var b          = 0
      var count      = 0

      var i = 0
      val n = offsets.length
      while (i < n) {
        var column = x + offsets(i)
        var row    = y + offsets(i + 1)
        if (column < 0 || column >= width || row < 0 || row >= height) {
          column = x
          row = y
        } else {
          val currentPixelIndex = getPixelIndex(width, column, row)
          if (!mask.isBlank(currentPixelIndex)) {
            val argb = rgb(currentPixelIndex)
            r += red(argb)
            g += green(argb)
            b += blue(argb)
            count += 1
          }
        }
        i += 2
      }

      if (count != 0) {
        rgb(pixelIndex) = argb(0, r / count, g / count, b / count)
        iterator.markAsInProgress()
      }
    }

    iterator.reset()
  }
}

object ColorBleedEffect {
  private val offsets: Array[Int] = Array(-1, -1, 0, -1, 1, -1, -1, 0, 1, 0, -1, 1, 0, 1, 1, 1)

  private def getPixelIndex(width: Int, x: Int, y: Int): Int = y * width + x

  private def red(argb: Int): Int = (argb >> 16) & 0xff

  private def green(argb: Int): Int = (argb >> 8) & 0xff

  private def blue(argb: Int): Int = argb & 0xff

  private def argb(a: Int, r: Int, g: Int, b: Int): Int = {
    if (a < 0 || a > 255 || r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255)
      throw new IllegalArgumentException("Invalid RGBA: " + r + ", " + g + "," + b + "," + a)
    ((a & 0xff) << 24) | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff)
  }

  private def alpha(argb: Int): Int = (argb >> 24) & 0xff

  private class Mask(rgb: Array[Int]) {
    private val n:    Int            = rgb.length
    val blank:        Array[Boolean] = new Array[Boolean](n)
    val pending:      Array[Int]     = new Array[Int](n)
    val changing:     Array[Int]     = new Array[Int](n)
    var pendingSize:  Int            = 0
    var changingSize: Int            = 0

    {
      var i = 0
      while (i < n) {
        if (alpha(rgb(i)) == 0) {
          blank(i) = true
          pending(pendingSize) = i
          pendingSize += 1
        }
        i += 1
      }
    }

    def isBlank(index: Int): Boolean = blank(index)

    def removeIndex(index: Int): Int = {
      if (index >= pendingSize) throw new IndexOutOfBoundsException(String.valueOf(index))
      val value = pending(index)
      pendingSize -= 1
      pending(index) = pending(pendingSize)
      value
    }

    class MaskIterator {
      private var index: Int = 0

      def hasNext(): Boolean = index < pendingSize

      def next(): Int = {
        if (index >= pendingSize) throw new NoSuchElementException(String.valueOf(index))
        val result = pending(index)
        index += 1
        result
      }

      def markAsInProgress(): Unit = {
        index -= 1
        changing(changingSize) = removeIndex(index)
        changingSize += 1
      }

      def reset(): Unit = {
        index = 0
        var i  = 0
        val cn = changingSize
        while (i < cn) {
          blank(changing(i)) = false
          i += 1
        }
        changingSize = 0
      }
    }
  }
}
