/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g2d/PixmapPacker.java
 * Original authors: mzechner, Nathan Sweet, Rob Rendell
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: Disposable -> AutoCloseable; dispose() -> close()
 *   Convention: Nullable for null safety; MutableMap instead of OrderedMap; using Sge context parameter
 *   Idiom: boundary/break, Nullable, split packages
 *   Fixes: Raw null eliminated from pack(), getRect(), getPage(), getSplits(), getPads() using Nullable types.
 *   Fixes: Added missing SkylineStrategy/SkylinePage inner classes (row-based bin packing).
 *   Fixes: Named context parameter (implicit sge: Sge) → anonymous (using Sge).
 *   Fixes: Java-style getters/setters → removed redundant getters for public vars (pageWidth, pageHeight,
 *     pageFormat, padding, duplicateBorder, packToTexture, transparentColor); removed getPages() (pages is public val);
 *     removed Page.getPixmap()/getRects()/getTexture() (public vals); PixmapPackerRectangle.getX()/getY()/
 *     getWidth()/getHeight() → property accessors x/y/width/height
 *   Improvement: opaque Pixels for pageWidth/pageHeight params -- see docs/improvements/opaque-types.md
 *   Audited: 2026-03-04
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g2d

import java.util.Arrays
import java.util.regex.Pattern

import sge.graphics.{ Color, Pixmap }
import sge.graphics.Pixmap.{ Blending, Format }
import sge.graphics.Texture
import sge.graphics.g2d.{ TextureAtlas, TextureRegion }
import sge.graphics.Texture.TextureFilter
import sge.graphics.glutils.PixmapTextureData
import sge.utils.{ DynamicArray, Nullable, SgeError }
import scala.collection.mutable.Map as MutableMap
import scala.compiletime.uninitialized
import scala.language.implicitConversions
import scala.util.boundary
import scala.util.boundary.break

/** Packs {@link Pixmap pixmaps} into one or more {@link Page pages} to generate an atlas of pixmap instances. Provides means to directly convert the pixmap atlas to a {@link TextureAtlas} . The
  * packer supports padding and border pixel duplication, specified during construction. The packer supports incremental inserts and updates of TextureAtlases generated with this class. How bin
  * packing is performed can be customized via {@link PackStrategy} . <p> All methods can be called from any thread unless otherwise noted. <p> One-off usage:
  *
  * <pre> // 512x512 pixel pages, RGB565 format, 2 pixels of padding, border duplication PixmapPacker packer = new PixmapPacker(512, 512, Format.RGB565, 2, true); packer.pack(&quot;First Pixmap&quot;,
  * pixmap1); packer.pack(&quot;Second Pixmap&quot;, pixmap2); TextureAtlas atlas = packer.generateTextureAtlas(TextureFilter.Nearest, TextureFilter.Nearest, false); packer.dispose(); // ...
  * atlas.dispose(); </pre>
  *
  * With this usage pattern, disposing the packer will not dispose any pixmaps used by the texture atlas. The texture atlas must also be disposed when no longer needed.
  *
  * Incremental texture atlas usage:
  *
  * <pre> // 512x512 pixel pages, RGB565 format, 2 pixels of padding, no border duplication PixmapPacker packer = new PixmapPacker(512, 512, Format.RGB565, 2, false); TextureAtlas atlas = new
  * TextureAtlas();
  *
  * // potentially on a separate thread, e.g. downloading thumbnails packer.pack(&quot;thumbnail&quot;, thumbnail);
  *
  * // on the rendering thread, every frame packer.updateTextureAtlas(atlas, TextureFilter.Linear, TextureFilter.Linear, false);
  *
  * // once the atlas is no longer needed, make sure you get the final additions. This might // be more elaborate depending on your threading model. packer.updateTextureAtlas(atlas,
  * TextureFilter.Linear, TextureFilter.Linear, false); // ... atlas.dispose(); </pre>
  *
  * Pixmap-only usage:
  *
  * <pre> PixmapPacker packer = new PixmapPacker(512, 512, Format.RGB565, 2, true); packer.pack(&quot;First Pixmap&quot;, pixmap1); packer.pack(&quot;Second Pixmap&quot;, pixmap2);
  *
  * // do something interesting with the resulting pages for (Page page : packer.getPages()) { // ... }
  *
  * packer.close(); </pre>
  *
  * @author
  *   mzechner
  * @author
  *   Nathan Sweet
  * @author
  *   Rob Rendell
  */
class PixmapPacker(using Sge) extends AutoCloseable {
  var packToTexture:    Boolean                         = false
  var disposed:         Boolean                         = false
  var pageWidth:        Int                             = uninitialized
  var pageHeight:       Int                             = uninitialized
  var pageFormat:       Format                          = uninitialized
  var padding:          Int                             = uninitialized
  var duplicateBorder:  Boolean                         = false
  var stripWhitespaceX: Boolean                         = false
  var stripWhitespaceY: Boolean                         = false
  var alphaThreshold:   Int                             = uninitialized
  var transparentColor: Color                           = Color(0f, 0f, 0f, 0f)
  val pages:            DynamicArray[PixmapPacker.Page] = DynamicArray[PixmapPacker.Page]()
  var packStrategy:     PixmapPacker.PackStrategy       = uninitialized

  /** Uses {@link GuillotineStrategy} .
    * @see
    *   PixmapPacker#PixmapPacker(int, int, Format, int, boolean, boolean, boolean, PackStrategy)
    */
  def this(pageWidth: Int, pageHeight: Int, pageFormat: Format, padding: Int, duplicateBorder: Boolean)(using Sge) = {
    this()
    this.pageWidth = pageWidth
    this.pageHeight = pageHeight
    this.pageFormat = pageFormat
    this.padding = padding
    this.duplicateBorder = duplicateBorder
    this.stripWhitespaceX = false
    this.stripWhitespaceY = false
    this.packStrategy = PixmapPacker.GuillotineStrategy()
  }

  /** Uses {@link GuillotineStrategy} .
    * @see
    *   PixmapPacker#PixmapPacker(int, int, Format, int, boolean, boolean, boolean, PackStrategy)
    */
  def this(pageWidth: Int, pageHeight: Int, pageFormat: Format, padding: Int, duplicateBorder: Boolean, packStrategy: PixmapPacker.PackStrategy)(using Sge) = {
    this()
    this.pageWidth = pageWidth
    this.pageHeight = pageHeight
    this.pageFormat = pageFormat
    this.padding = padding
    this.duplicateBorder = duplicateBorder
    this.stripWhitespaceX = false
    this.stripWhitespaceY = false
    this.packStrategy = packStrategy
  }

  /** Creates a new PixmapPacker which will insert all supplied pixmaps into one or more <code>pageWidth</code> by <code>pageHeight</code> pixmaps using the specified strategy.
    * @param padding
    *   the number of blank pixels to insert between pixmaps.
    * @param duplicateBorder
    *   duplicate the border pixels of the inserted images to avoid seams when rendering with bi-linear filtering on.
    * @param stripWhitespaceX
    *   strip whitespace in x axis
    * @param stripWhitespaceY
    *   strip whitespace in y axis
    */
  def this(pageWidth: Int, pageHeight: Int, pageFormat: Format, padding: Int, duplicateBorder: Boolean, stripWhitespaceX: Boolean, stripWhitespaceY: Boolean, packStrategy: PixmapPacker.PackStrategy)(
    using Sge
  ) = {
    this()
    this.pageWidth = pageWidth
    this.pageHeight = pageHeight
    this.pageFormat = pageFormat
    this.padding = padding
    this.duplicateBorder = duplicateBorder
    this.stripWhitespaceX = stripWhitespaceX
    this.stripWhitespaceY = stripWhitespaceY
    this.packStrategy = packStrategy
  }

  /** Sorts the images to the optimzal order they should be packed. Some packing strategies rely heavily on the images being sorted.
    */
  def sort(images: DynamicArray[Pixmap]): Unit =
    packStrategy.sort(images)

  /** Inserts the pixmap without a name. It cannot be looked up by name.
    * @see
    *   #pack(String, Pixmap)
    */
  def pack(image: Pixmap): Nullable[PixmapPacker.PixmapPackerRectangle] =
    pack(Nullable.empty, image)

  /** Inserts the pixmap. If name was not null, you can later retrieve the image's position in the output image via {@link #getRect(String)} .
    * @param name
    *   If null, the image cannot be looked up by name.
    * @return
    *   Rectangle describing the area the pixmap was rendered to.
    * @throws GdxRuntimeException
    *   in case the image did not fit due to the page size being too small or providing a duplicate name.
    */
  def pack(name: Nullable[String], image: Pixmap): Nullable[PixmapPacker.PixmapPackerRectangle] = boundary {
    var workingName  = name
    var workingImage = image

    if (disposed) break(Nullable.empty)

    workingName.foreach { wn =>
      if (getRect(wn).isDefined)
        throw SgeError.InvalidInput("Pixmap has already been packed with name: " + wn)
    }

    var pixmapToDispose: Nullable[Pixmap] = Nullable.empty
    val rect = if (workingName.exists(_.endsWith(".9"))) {
      val r = PixmapPacker.PixmapPackerRectangle(0, 0, workingImage.getWidth().toInt - 2, workingImage.getHeight().toInt - 2)
      val p = Pixmap(workingImage.getWidth().toInt - 2, workingImage.getHeight().toInt - 2, workingImage.getFormat())
      p.setBlending(Blending.None)
      r.splits = getSplits(workingImage)
      r.pads = getPads(workingImage, r.splits)
      p.drawPixmap(
        workingImage,
        Pixels(0),
        Pixels(0),
        Pixels(1),
        Pixels(1),
        workingImage.getWidth() - Pixels(1),
        workingImage.getHeight() - Pixels(1)
      )
      workingImage = p
      pixmapToDispose = p
      workingName = workingName.map(_.split("\\.").head)
      r
    } else if (stripWhitespaceX || stripWhitespaceY) {
      val originalWidth:  Int = workingImage.getWidth().toInt
      val originalHeight: Int = workingImage.getHeight().toInt
      // Strip whitespace, manipulate the pixmap and return corrected Rect
      var top:    Int = 0
      var bottom: Int = workingImage.getHeight().toInt
      if (stripWhitespaceY) {
        var outerBreak = false
        var y          = 0
        while (y < workingImage.getHeight().toInt && !outerBreak) {
          var x = 0
          while (x < workingImage.getWidth().toInt && !outerBreak) {
            val pixel: Int = workingImage.getPixel(Pixels(x), Pixels(y))
            val alpha: Int = pixel & 0x000000ff
            if (alpha > alphaThreshold) outerBreak = true
            x += 1
          }
          if (!outerBreak) top += 1
          y += 1
        }
        outerBreak = false
        y = workingImage.getHeight().toInt - 1
        while (y >= top && !outerBreak) {
          var x = 0
          while (x < workingImage.getWidth().toInt && !outerBreak) {
            val pixel: Int = workingImage.getPixel(Pixels(x), Pixels(y))
            val alpha: Int = pixel & 0x000000ff
            if (alpha > alphaThreshold) outerBreak = true
            x += 1
          }
          if (!outerBreak) bottom -= 1
          y -= 1
        }
      }
      var left:  Int = 0
      var right: Int = workingImage.getWidth().toInt
      if (stripWhitespaceX) {
        var outerBreak = false
        var x          = 0
        while (x < workingImage.getWidth().toInt && !outerBreak) {
          var y = top
          while (y < bottom && !outerBreak) {
            val pixel: Int = workingImage.getPixel(Pixels(x), Pixels(y))
            val alpha: Int = pixel & 0x000000ff
            if (alpha > alphaThreshold) outerBreak = true
            y += 1
          }
          if (!outerBreak) left += 1
          x += 1
        }
        outerBreak = false
        x = workingImage.getWidth().toInt - 1
        while (x >= left && !outerBreak) {
          var y = top
          while (y < bottom && !outerBreak) {
            val pixel: Int = workingImage.getPixel(Pixels(x), Pixels(y))
            val alpha: Int = pixel & 0x000000ff
            if (alpha > alphaThreshold) outerBreak = true
            y += 1
          }
          if (!outerBreak) right -= 1
          x -= 1
        }
      }

      val newWidth:  Int = right - left
      val newHeight: Int = bottom - top

      val p = Pixmap(newWidth, newHeight, workingImage.getFormat())
      p.setBlending(Blending.None)
      p.drawPixmap(workingImage, Pixels(0), Pixels(0), Pixels(left), Pixels(top), Pixels(newWidth), Pixels(newHeight))
      workingImage = p
      pixmapToDispose = p

      PixmapPacker.PixmapPackerRectangle(0, 0, newWidth, newHeight, left, top, originalWidth, originalHeight)
    } else {
      PixmapPacker.PixmapPackerRectangle(0, 0, workingImage.getWidth().toInt, workingImage.getHeight().toInt)
    }

    if (rect.width > pageWidth || rect.height > pageHeight) {
      throw SgeError.InvalidInput("Page size too small for pixmap" + workingName.map(n => ": " + n).getOrElse(""))
    }

    val page = packStrategy.pack(this, workingName, rect.bounds)
    workingName.foreach { wn =>
      page.rects.put(wn, rect)
      page.addedRects.add(wn)
    }

    val rectX: Int = rect.x
    val rectY: Int = rect.y

    if (packToTexture && !duplicateBorder && page.texture.isDefined && !page.dirty) {
      page.texture.foreach { tex =>
        tex.bind()
        val rectWidth  = rect.width
        val rectHeight = rect.height
        Sge().graphics.gl.glTexSubImage2D(
          tex.glTarget,
          0,
          Pixels(rectX),
          Pixels(rectY),
          Pixels(rectWidth),
          Pixels(rectHeight),
          PixelFormat(workingImage.getGLFormat()),
          DataType(workingImage.getGLType()),
          workingImage.getPixels()
        )
      }
    } else {
      page.dirty = true
    }

    page.image.drawPixmap(workingImage, Pixels(rectX), Pixels(rectY))

    if (duplicateBorder) {
      val imageWidth  = workingImage.getWidth()
      val imageHeight = workingImage.getHeight()
      // Copy corner pixels to fill corners of the padding.
      page.image.drawPixmap(workingImage, Pixels(0), Pixels(0), Pixels(1), Pixels(1), Pixels(rectX - 1), Pixels(rectY - 1), Pixels(1), Pixels(1))
      page.image.drawPixmap(
        workingImage,
        imageWidth - Pixels(1),
        Pixels(0),
        Pixels(1),
        Pixels(1),
        Pixels(rectX + rect.width),
        Pixels(rectY - 1),
        Pixels(1),
        Pixels(1)
      )
      page.image.drawPixmap(
        workingImage,
        Pixels(0),
        imageHeight - Pixels(1),
        Pixels(1),
        Pixels(1),
        Pixels(rectX - 1),
        Pixels(rectY + rect.height),
        Pixels(1),
        Pixels(1)
      )
      page.image.drawPixmap(
        workingImage,
        imageWidth - Pixels(1),
        imageHeight - Pixels(1),
        Pixels(1),
        Pixels(1),
        Pixels(rectX + rect.width),
        Pixels(rectY + rect.height),
        Pixels(1),
        Pixels(1)
      )
      // Copy edge pixels into padding.
      page.image.drawPixmap(workingImage, Pixels(0), Pixels(0), imageWidth, Pixels(1), Pixels(rectX), Pixels(rectY - 1), Pixels(rect.width), Pixels(1))
      page.image.drawPixmap(
        workingImage,
        Pixels(0),
        imageHeight - Pixels(1),
        imageWidth,
        Pixels(1),
        Pixels(rectX),
        Pixels(rectY + rect.height),
        Pixels(rect.width),
        Pixels(1)
      )
      page.image.drawPixmap(workingImage, Pixels(0), Pixels(0), Pixels(1), imageHeight, Pixels(rectX - 1), Pixels(rectY), Pixels(1), Pixels(rect.height))
      page.image.drawPixmap(
        workingImage,
        imageWidth - Pixels(1),
        Pixels(0),
        Pixels(1),
        imageHeight,
        Pixels(rectX + rect.width),
        Pixels(rectY),
        Pixels(1),
        Pixels(rect.height)
      )
    }

    pixmapToDispose.foreach(_.close())

    rect.page = page
    Nullable(rect)
  }

  /** @param name
    *   the name of the image
    * @return
    *   the rectangle for the image in the page it's stored in or null
    */
  def getRect(name: String): Nullable[PixmapPacker.PixmapPackerRectangle] =
    boundary {
      for (page <- pages)
        page.rects.get(name) match {
          case Some(rect) => break(Nullable(rect))
          case None       => // continue searching
        }
      Nullable.empty
    }

  /** @param name
    *   the name of the image
    * @return
    *   the page the image is stored in or null
    */
  def getPage(name: String): Nullable[PixmapPacker.Page] =
    boundary {
      for (page <- pages)
        if (page.rects.contains(name)) break(Nullable(page))
      Nullable.empty
    }

  /** Returns the index of the page containing the given packed rectangle.
    * @param name
    *   the name of the image
    * @return
    *   the index of the page the image is stored in or -1
    */
  def getPageIndex(name: String): Int =
    boundary {
      for (i <- 0 until pages.size)
        if (pages(i).rects.contains(name)) break(i)
      -1
    }

  /** Disposes any pixmap pages which don't have a texture. Page pixmaps that have a texture will not be disposed until their texture is disposed.
    */
  def dispose() = {
    for (page <- pages)
      if (page.texture.isEmpty) {
        page.image.close()
      }
    disposed = true
  }

  /** Generates a new {@link TextureAtlas} from the pixmaps inserted so far. After calling this method, disposing the packer will no longer dispose the page pixmaps.
    */
  def generateTextureAtlas(minFilter: TextureFilter, magFilter: TextureFilter, useMipMaps: Boolean): TextureAtlas = {
    val atlas = TextureAtlas()
    updateTextureAtlas(atlas, minFilter, magFilter, useMipMaps)
    atlas
  }

  /** Updates the {@link TextureAtlas} , adding any new {@link Pixmap} instances packed since the last call to this method. This can be used to insert Pixmap instances on a separate thread via
    * {@link #pack(String, Pixmap)} and update the TextureAtlas on the rendering thread. This method must be called on the rendering thread. After calling this method, disposing the packer will no
    * longer dispose the page pixmaps. Has useIndexes on by default so as to keep backwards compatibility
    */
  def updateTextureAtlas(atlas: TextureAtlas, minFilter: TextureFilter, magFilter: TextureFilter, useMipMaps: Boolean): Unit =
    updateTextureAtlas(atlas, minFilter, magFilter, useMipMaps, true)

  /** Updates the {@link TextureAtlas} , adding any new {@link Pixmap} instances packed since the last call to this method. This can be used to insert Pixmap instances on a separate thread via
    * {@link #pack(String, Pixmap)} and update the TextureAtlas on the rendering thread. This method must be called on the rendering thread. After calling this method, disposing the packer will no
    * longer dispose the page pixmaps.
    */
  def updateTextureAtlas(atlas: TextureAtlas, minFilter: TextureFilter, magFilter: TextureFilter, useMipMaps: Boolean, useIndexes: Boolean): Unit = {
    updatePageTextures(minFilter, magFilter, useMipMaps)
    for (page <- pages)
      if (page.addedRects.size > 0) {
        page.texture.foreach { pageTexture =>
          for (name <- page.addedRects)
            page.rects.get(name) match {
              case Some(rect) =>
                var imageIndex = -1
                var imageName  = name

                if (useIndexes) {
                  val matcher = PixmapPacker.indexPattern.matcher(imageName)
                  if (matcher.matches()) {
                    imageName = matcher.group(1)
                    imageIndex = Integer.parseInt(matcher.group(2))
                  }
                }

                val region = atlas.addRegion(imageName, pageTexture, rect.x, rect.y, rect.width, rect.height)
                region.index = imageIndex
                region.offsetX = rect.offsetX.toFloat
                region.offsetY = (rect.originalHeight - rect.height - rect.offsetY).toFloat
                region.originalWidth = rect.originalWidth
                region.originalHeight = rect.originalHeight

                rect.splits.foreach { s =>
                  region.names = Nullable(Array("split", "pad"))
                  region.values = Nullable(Array(s, rect.pads.getOrElse(Array(0, 0, 0, 0))))
                }
              case None => // skip
            }
          page.addedRects.clear()
        }
      }
  }

  /** Calls {@link Page#updateTexture(TextureFilter, TextureFilter, boolean) updateTexture} for each page and adds a region to the specified array for each page texture.
    */
  def updateTextureRegions(regions: DynamicArray[TextureRegion], minFilter: TextureFilter, magFilter: TextureFilter, useMipMaps: Boolean): Unit = {
    updatePageTextures(minFilter, magFilter, useMipMaps);
    while (regions.size < pages.size)
      pages(regions.size).texture.foreach { pageTexture =>
        regions.add(TextureRegion(pageTexture))
      }
  }

  /** Calls {@link Page#updateTexture(TextureFilter, TextureFilter, boolean) updateTexture} for each page. */
  def updatePageTextures(minFilter: TextureFilter, magFilter: TextureFilter, useMipMaps: Boolean): Unit =
    for (page <- pages)
      page.updateTexture(minFilter, magFilter, useMipMaps)

  /** Sets the default <code>color</code> of the whole {@link PixmapPacker.Page} when a new one created. Helps to avoid texture bleeding or to highlight the page for debugging.
    * @see
    *   Page#Page(PixmapPacker packer)
    */
  def setTransparentColor(color: Color): Unit =
    this.transparentColor.set(color)

  private def getSplits(raster: Pixmap): Nullable[Array[Int]] = {
    var startX = getSplitPoint(raster, 1, 0, true, true)
    var endX   = getSplitPoint(raster, startX, 0, false, true)
    var startY = getSplitPoint(raster, 0, 1, true, false)
    var endY   = getSplitPoint(raster, 0, startY, false, false)

    // Ensure pixels after the end are not invalid.
    getSplitPoint(raster, endX + 1, 0, true, true)
    getSplitPoint(raster, 0, endY + 1, true, false)

    // No splits, or all splits.
    if (startX == 0 && endX == 0 && startY == 0 && endY == 0) Nullable.empty
    else {
      // Subtraction here is because the coordinates were computed before the 1px border was stripped.
      if (startX != 0) {
        startX -= 1
        endX = raster.getWidth().toInt - 2 - (endX - 1)
      } else {
        // If no start point was ever found, we assume full stretch.
        endX = raster.getWidth().toInt - 2
      }
      if (startY != 0) {
        startY -= 1
        endY = raster.getHeight().toInt - 2 - (endY - 1)
      } else {
        // If no start point was ever found, we assume full stretch.
        endY = raster.getHeight().toInt - 2
      }

      Array(startX, endX, startY, endY)
    }
  }

  private def getPads(raster: Pixmap, splits: Nullable[Array[Int]]): Nullable[Array[Int]] = {
    val bottom = raster.getHeight().toInt - 1
    val right  = raster.getWidth().toInt - 1

    var startX = getSplitPoint(raster, 1, bottom, true, true)
    var startY = getSplitPoint(raster, right, 1, true, false)

    // No need to hunt for the end if a start was never found.
    var endX = 0
    var endY = 0
    if (startX != 0) endX = getSplitPoint(raster, startX + 1, bottom, false, true)
    if (startY != 0) endY = getSplitPoint(raster, right, startY + 1, false, false)

    // Ensure pixels after the end are not invalid.
    getSplitPoint(raster, endX + 1, bottom, true, true)
    getSplitPoint(raster, right, endY + 1, true, false)

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
          endX = raster.getWidth().toInt - 2 - (endX - 1)
        } else {
          // If no start point was ever found, we assume full stretch.
          endX = raster.getWidth().toInt - 2
        }
      }
      if (startY == 0 && endY == 0) {
        startY = -1
        endY = -1
      } else {
        if (startY > 0) {
          startY -= 1
          endY = raster.getHeight().toInt - 2 - (endY - 1)
        } else {
          // If no start point was ever found, we assume full stretch.
          endY = raster.getHeight().toInt - 2
        }
      }

      val pads = Array(startX, endX, startY, endY)

      if (splits.isDefined && splits.exists(Arrays.equals(pads, _))) {
        Nullable.empty
      } else {
        pads
      }
    }
  }

  private val c = Color()

  private def getSplitPoint(raster: Pixmap, startX: Int, startY: Int, startPoint: Boolean, xAxis: Boolean): Int = boundary {
    val rgba = Array.ofDim[Int](4)

    var next   = if (xAxis) startX else startY
    val end    = if (xAxis) raster.getWidth().toInt else raster.getHeight().toInt
    val breakA = if (startPoint) 255 else 0

    var x = startX
    var y = startY
    while (next != end) {
      if (xAxis)
        x = next
      else
        y = next

      val colint = raster.getPixel(Pixels(x), Pixels(y))
      c.set(colint)
      rgba(0) = (c.r * 255).toInt
      rgba(1) = (c.g * 255).toInt
      rgba(2) = (c.b * 255).toInt
      rgba(3) = (c.a * 255).toInt
      if (rgba(3) == breakA) break(next)

      if (!startPoint && (rgba(0) != 0 || rgba(1) != 0 || rgba(2) != 0 || rgba(3) != 255))
        println(s"$x  $y ${rgba.mkString(" ")} ")

      next += 1
    }

    0
  }
  override def close(): Unit = dispose()
}

object PixmapPacker {
  val indexPattern: Pattern = Pattern.compile("(.+)_(\\d+)$")

  /** Choose the page and location for each rectangle.
    * @author
    *   Nathan Sweet
    */
  trait PackStrategy {
    def sort(images: DynamicArray[Pixmap]): Unit

    /** Returns the page the bounds should be placed in and modifies the specified bounds position. */
    def pack(packer: PixmapPacker, name: Nullable[String], bounds: Bounds): Page
  }

  /** @author
    *   mzechner
    * @author
    *   Nathan Sweet
    * @author
    *   Rob Rendell
    */
  class Page(packer: PixmapPacker)(using Sge) {
    val rects:      MutableMap[String, PixmapPackerRectangle] = MutableMap()
    val image:      Pixmap                                    = Pixmap(packer.pageWidth, packer.pageHeight, packer.pageFormat)
    var texture:    Nullable[Texture]                         = Nullable.empty
    val addedRects: DynamicArray[String]                      = DynamicArray[String]()
    var dirty:      Boolean                                   = false

    // Initialize page
    image.setBlending(Blending.None)
    image.setColor(packer.transparentColor)
    image.fill()

    /** Creates the texture if it has not been created, else reuploads the entire page pixmap to the texture if the pixmap has changed since this method was last called.
      * @return
      *   true if the texture was created or reuploaded.
      */
    def updateTexture(minFilter: TextureFilter, magFilter: TextureFilter, useMipMaps: Boolean): Boolean = scala.util.boundary {
      if (texture.isEmpty) {
        val tex = new Texture(PixmapTextureData(image, image.getFormat(), useMipMaps, false, true)) {
          override def close(): Unit = {
            super.close()
            image.close()
          }
        }
        texture = Nullable(tex)
        tex.setFilter(minFilter, magFilter)
      } else {
        if (!dirty) scala.util.boundary.break(false)
        texture.foreach(tex => tex.load(tex.textureData))
      }
      dirty = false
      true
    }
  }

  class Bounds() {
    var x:      Int = 0
    var y:      Int = 0
    var width:  Int = 0
    var height: Int = 0

    def this(x: Int, y: Int, width: Int, height: Int) = {
      this()
      this.x = x
      this.y = y
      this.width = width
      this.height = height
    }

    def set(x: Int, y: Int, width: Int, height: Int): Unit = {
      this.x = x
      this.y = y
      this.width = width
      this.height = height
    }
  }

  class PixmapPackerRectangle(initX: Int, initY: Int, initWidth: Int, initHeight: Int) {
    var page:           Page                 = uninitialized
    var splits:         Nullable[Array[Int]] = Nullable.empty
    var pads:           Nullable[Array[Int]] = Nullable.empty
    var offsetX:        Int                  = 0
    var offsetY:        Int                  = 0
    var originalWidth:  Int                  = initWidth
    var originalHeight: Int                  = initHeight
    val bounds:         Bounds               = Bounds(initX, initY, initWidth, initHeight)

    def this(x: Int, y: Int, width: Int, height: Int, left: Int, top: Int, originalWidth: Int, originalHeight: Int) = {
      this(x, y, width, height)
      this.offsetX = left
      this.offsetY = top
      this.originalWidth = originalWidth
      this.originalHeight = originalHeight
    }

    def x:      Int = bounds.x
    def y:      Int = bounds.y
    def width:  Int = bounds.width
    def height: Int = bounds.height
  }

  /** Does bin packing by inserting to the right or below previously packed rectangles. This is good at packing arbitrarily sized images.
    */
  class GuillotineStrategy()(using Sge) extends PackStrategy {
    private var comparator: Nullable[Ordering[Pixmap]] = Nullable.empty

    def sort(images: DynamicArray[Pixmap]): Unit = {
      if (comparator.isEmpty) {
        comparator = Nullable(
          Ordering.fromLessThan[Pixmap] { (o1, o2) =>
            Math.max(o1.getWidth().toInt, o1.getHeight().toInt) < Math.max(o2.getWidth().toInt, o2.getHeight().toInt)
          }
        )
      }
      comparator.foreach(images.sort(_))
    }

    def pack(packer: PixmapPacker, name: Nullable[String], rect: Bounds): Page = {
      var page: GuillotinePage = if (packer.pages.isEmpty) {
        // Add a page if empty.
        val p = GuillotinePage(packer)
        packer.pages.add(p)
        p
      } else {
        // Always try to pack into the last page.
        packer.pages.last.asInstanceOf[GuillotinePage]
      }

      val padding = packer.padding
      rect.width += padding
      rect.height += padding
      var node = insert(page.root, rect)
      if (node.isEmpty) {
        // Didn't fit, pack into a new page.
        page = GuillotinePage(packer)
        packer.pages.add(page)
        node = insert(page.root, rect)
      }
      node.foreach { n =>
        n.full = true
        rect.set(n.rect.x, n.rect.y, n.rect.width - padding, n.rect.height - padding)
      }
      page
    }

    private def insert(node: GuillotineStrategy.Node, rect: Bounds): Nullable[GuillotineStrategy.Node] = boundary {
      if (!node.full && node.leftChild.isDefined && node.rightChild.isDefined) {
        val newNode = node.leftChild.flatMap(insert(_, rect))
        if (newNode.isDefined) break(newNode)
        else break(node.rightChild.flatMap(insert(_, rect)))
      }
      if (node.full) break(Nullable.empty)
      if (node.rect.width == rect.width && node.rect.height == rect.height) break(Nullable(node))
      if (node.rect.width < rect.width || node.rect.height < rect.height) break(Nullable.empty)

      val left  = GuillotineStrategy.Node()
      val right = GuillotineStrategy.Node()
      node.leftChild = Nullable(left)
      node.rightChild = Nullable(right)

      val deltaWidth  = node.rect.width - rect.width
      val deltaHeight = node.rect.height - rect.height
      if (deltaWidth > deltaHeight) {
        left.rect.set(node.rect.x, node.rect.y, rect.width, node.rect.height)
        right.rect.set(node.rect.x + rect.width, node.rect.y, node.rect.width - rect.width, node.rect.height)
      } else {
        left.rect.set(node.rect.x, node.rect.y, node.rect.width, rect.height)
        right.rect.set(node.rect.x, node.rect.y + rect.height, node.rect.width, node.rect.height - rect.height)
      }

      insert(left, rect)
    }
  }

  object GuillotineStrategy {
    final class Node() {
      var leftChild:  Nullable[Node] = Nullable.empty
      var rightChild: Nullable[Node] = Nullable.empty
      val rect:       Bounds         = Bounds()
      var full:       Boolean        = false
    }
  }

  class GuillotinePage(packer: PixmapPacker)(using Sge) extends Page(packer) {
    val root: GuillotineStrategy.Node = GuillotineStrategy.Node()
    root.rect.set(packer.padding, packer.padding, packer.pageWidth - packer.padding * 2, packer.pageHeight - packer.padding * 2)
  }

  /** Does bin packing by inserting in rows. This is good at packing images that have similar heights.
    * @author
    *   Nathan Sweet
    */
  class SkylineStrategy()(using Sge) extends PackStrategy {
    private var comparator: Nullable[Ordering[Pixmap]] = Nullable.empty

    def sort(images: DynamicArray[Pixmap]): Unit = {
      if (comparator.isEmpty) {
        comparator = Nullable(
          Ordering.fromLessThan[Pixmap] { (o1, o2) =>
            o1.getHeight().toInt < o2.getHeight().toInt
          }
        )
      }
      comparator.foreach(images.sort(_))
    }

    def pack(packer: PixmapPacker, name: Nullable[String], rect: Bounds): Page = boundary {
      val padding    = packer.padding
      val pageWidth  = packer.pageWidth - padding * 2
      val pageHeight = packer.pageHeight - padding * 2
      val rectWidth  = rect.width + padding
      val rectHeight = rect.height + padding
      for (i <- 0 until packer.pages.size) {
        val page = packer.pages(i).asInstanceOf[SkylinePage]
        var bestRow: Nullable[SkylineStrategy.Row] = Nullable.empty
        // Fit in any row before the last.
        for (ii <- 0 until (page.rows.size - 1)) {
          val row = page.rows(ii)
          if (row.x + rectWidth < pageWidth && row.y + rectHeight < pageHeight && rectHeight <= row.height) {
            if (bestRow.isEmpty || row.height < bestRow.map(_.height).getOrElse(Int.MaxValue))
              bestRow = Nullable(row)
          }
        }
        if (bestRow.isEmpty) {
          // Fit in last row, increasing height.
          val row = page.rows.last
          if (row.y + rectHeight < pageHeight) {
            if (row.x + rectWidth < pageWidth) {
              row.height = Math.max(row.height, rectHeight)
              bestRow = Nullable(row)
            } else if (row.y + row.height + rectHeight < pageHeight) {
              // Fit in new row.
              val newRow = SkylineStrategy.Row()
              newRow.y = row.y + row.height
              newRow.height = rectHeight
              page.rows.add(newRow)
              bestRow = Nullable(newRow)
            }
          }
        }
        bestRow.foreach { row =>
          rect.x = row.x
          rect.y = row.y
          row.x += rectWidth
          break(page)
        }
      }
      // Fit in new page.
      val page = SkylinePage(packer)
      packer.pages.add(page)
      val row = SkylineStrategy.Row()
      row.x = padding + rectWidth
      row.y = padding
      row.height = rectHeight
      page.rows.add(row)
      rect.x = padding
      rect.y = padding
      page
    }
  }

  object SkylineStrategy {
    final class Row() {
      var x:      Int = 0
      var y:      Int = 0
      var height: Int = 0
    }
  }

  class SkylinePage(packer: PixmapPacker)(using Sge) extends Page(packer) {
    val rows: DynamicArray[SkylineStrategy.Row] = DynamicArray[SkylineStrategy.Row]()
  }
}
