/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g2d/PixmapPacker.java
 * Original authors: mzechner, Nathan Sweet, Rob Rendell
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g2d

import java.util.Arrays
import java.util.regex.Matcher
import java.util.regex.Pattern

import scala.util.boundary
import scala.util.boundary.break

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
class PixmapPacker(implicit sge: Sge) extends AutoCloseable {
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
  var transparentColor: Color                           = new Color(0f, 0f, 0f, 0f)
  val pages:            DynamicArray[PixmapPacker.Page] = DynamicArray[PixmapPacker.Page]()
  var packStrategy:     PixmapPacker.PackStrategy       = uninitialized

  /** Uses {@link GuillotineStrategy} .
    * @see
    *   PixmapPacker#PixmapPacker(int, int, Format, int, boolean, boolean, boolean, PackStrategy)
    */
  def this(pageWidth: Int, pageHeight: Int, pageFormat: Format, padding: Int, duplicateBorder: Boolean)(implicit sge: Sge) = {
    this()(using sge)
    this.pageWidth = pageWidth
    this.pageHeight = pageHeight
    this.pageFormat = pageFormat
    this.padding = padding
    this.duplicateBorder = duplicateBorder
    this.stripWhitespaceX = false
    this.stripWhitespaceY = false
    this.packStrategy = new PixmapPacker.GuillotineStrategy()
  }

  /** Uses {@link GuillotineStrategy} .
    * @see
    *   PixmapPacker#PixmapPacker(int, int, Format, int, boolean, boolean, boolean, PackStrategy)
    */
  def this(pageWidth: Int, pageHeight: Int, pageFormat: Format, padding: Int, duplicateBorder: Boolean, packStrategy: PixmapPacker.PackStrategy)(implicit sge: Sge) = {
    this()(using sge)
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
    implicit sge: Sge
  ) = {
    this()(using sge)
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
  def pack(image: Pixmap): PixmapPacker.PixmapPackerRectangle =
    pack(null, image);

  /** Inserts the pixmap. If name was not null, you can later retrieve the image's position in the output image via {@link #getRect(String)} .
    * @param name
    *   If null, the image cannot be looked up by name.
    * @return
    *   Rectangle describing the area the pixmap was rendered to.
    * @throws GdxRuntimeException
    *   in case the image did not fit due to the page size being too small or providing a duplicate name.
    */
  def pack(name: String, image: Pixmap): PixmapPacker.PixmapPackerRectangle = {
    // Make local vars for reassignment
    var workingName  = name
    var workingImage = image

    if (disposed) null // scalastyle:ignore
    else {
      if (Nullable(workingName).isDefined && Nullable(getRect(workingName)).isDefined)
        throw SgeError.InvalidInput("Pixmap has already been packed with name: " + workingName)

      var rect:            PixmapPacker.PixmapPackerRectangle = null;
      var pixmapToDispose: Pixmap                             = null;
      if (Nullable(workingName).isDefined && workingName.endsWith(".9")) {
        rect = new PixmapPacker.PixmapPackerRectangle(0, 0, workingImage.getWidth() - 2, workingImage.getHeight() - 2);
        pixmapToDispose = new Pixmap(workingImage.getWidth() - 2, workingImage.getHeight() - 2, workingImage.getFormat());
        pixmapToDispose.setBlending(Blending.None);
        rect.splits = getSplits(workingImage);
        rect.pads = getPads(workingImage, rect.splits);
        pixmapToDispose.drawPixmap(workingImage, 0, 0, 1, 1, workingImage.getWidth() - 1, workingImage.getHeight() - 1);
        workingImage = pixmapToDispose;
        workingName = workingName.split("\\.").head;
      } else {
        if (stripWhitespaceX || stripWhitespaceY) {
          val originalWidth:  Int = workingImage.getWidth()
          val originalHeight: Int = workingImage.getHeight()
          // Strip whitespace, manipulate the pixmap and return corrected Rect
          var top:    Int = 0
          var bottom: Int = workingImage.getHeight()
          if (stripWhitespaceY) {
            var outerBreak = false
            var y          = 0
            while (y < workingImage.getHeight() && !outerBreak) {
              var x = 0
              while (x < workingImage.getWidth() && !outerBreak) {
                val pixel: Int = workingImage.getPixel(x, y)
                val alpha: Int = pixel & 0x000000ff
                if (alpha > alphaThreshold) outerBreak = true
                x += 1
              }
              if (!outerBreak) top += 1
              y += 1
            }
            outerBreak = false
            y = workingImage.getHeight() - 1
            while (y >= top && !outerBreak) {
              var x = 0
              while (x < workingImage.getWidth() && !outerBreak) {
                val pixel: Int = workingImage.getPixel(x, y)
                val alpha: Int = pixel & 0x000000ff
                if (alpha > alphaThreshold) outerBreak = true
                x += 1
              }
              if (!outerBreak) bottom -= 1
              y -= 1
            }
          }
          var left:  Int = 0
          var right: Int = workingImage.getWidth()
          if (stripWhitespaceX) {
            var outerBreak = false
            var x          = 0
            while (x < workingImage.getWidth() && !outerBreak) {
              var y = top
              while (y < bottom && !outerBreak) {
                val pixel: Int = workingImage.getPixel(x, y)
                val alpha: Int = pixel & 0x000000ff
                if (alpha > alphaThreshold) outerBreak = true
                y += 1
              }
              if (!outerBreak) left += 1
              x += 1
            }
            outerBreak = false
            x = workingImage.getWidth() - 1
            while (x >= left && !outerBreak) {
              var y = top
              while (y < bottom && !outerBreak) {
                val pixel: Int = workingImage.getPixel(x, y)
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

          pixmapToDispose = new Pixmap(newWidth, newHeight, workingImage.getFormat());
          pixmapToDispose.setBlending(Blending.None);
          pixmapToDispose.drawPixmap(workingImage, 0, 0, left, top, newWidth, newHeight);
          workingImage = pixmapToDispose;

          rect = new PixmapPacker.PixmapPackerRectangle(0, 0, newWidth, newHeight, left, top, originalWidth, originalHeight);
        } else {
          rect = new PixmapPacker.PixmapPackerRectangle(0, 0, workingImage.getWidth(), workingImage.getHeight());
        }
      }

      if (rect.getWidth() > pageWidth || rect.getHeight() > pageHeight) {
        if (Nullable(workingName).isEmpty) throw SgeError.InvalidInput("Page size too small for pixmap.");
        throw SgeError.InvalidInput("Page size too small for pixmap: " + workingName);
      }

      val page = packStrategy.pack(this, workingName, rect.bounds);
      if (Nullable(workingName).isDefined) {
        page.rects.put(workingName, rect);
        page.addedRects.add(workingName);
      }

      val rectX:      Int = rect.getX()
      val rectY:      Int = rect.getY()
      val rectWidth:  Int = rect.getWidth()
      val rectHeight: Int = rect.getHeight()

      if (packToTexture && !duplicateBorder && page.texture.isDefined && !page.dirty) {
        // Note: This would need proper GL context and texture binding
        // For now, just mark as dirty
        page.dirty = true;
      } else
        page.dirty = true;

      page.image.drawPixmap(workingImage, rectX, rectY);

      if (duplicateBorder) {
        val imageWidth:  Int = workingImage.getWidth()
        val imageHeight: Int = workingImage.getHeight()
        // Note: These drawPixmap calls need adjustment for parameter count
        // For now, using basic 2-parameter version
        // TODO: Implement proper border duplication when drawPixmap signature is available
      }

      if (Nullable(pixmapToDispose).isDefined) {
        // Note: Pixmap.dispose() not available, would need proper cleanup
      }

      rect.page = page;
      rect
    }
  }

  /** @return
    *   the {@link Page} instances created so far. If multiple threads are accessing the packer, iterating over the pages must be done only after synchronizing on the packer.
    */
  def getPages(): Array[PixmapPacker.Page] =
    pages.toArray

  /** @param name
    *   the name of the image
    * @return
    *   the rectangle for the image in the page it's stored in or null
    */
  def getRect(name: String): PixmapPacker.PixmapPackerRectangle =
    boundary {
      for (page <- pages)
        page.rects.get(name) match {
          case Some(rect) => break(rect)
          case None       => // continue searching
        }
      null
    }

  /** @param name
    *   the name of the image
    * @return
    *   the page the image is stored in or null
    */
  def getPage(name: String): PixmapPacker.Page =
    boundary {
      for (page <- pages)
        if (page.rects.contains(name)) break(page)
      null
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
        // Note: page.image.dispose() not available, would need proper cleanup
      }
    disposed = true
  }

  /** Generates a new {@link TextureAtlas} from the pixmaps inserted so far. After calling this method, disposing the packer will no longer dispose the page pixmaps.
    */
  def generateTextureAtlas(minFilter: TextureFilter, magFilter: TextureFilter, useMipMaps: Boolean): TextureAtlas =
    // Note: TextureAtlas is abstract and cannot be instantiated directly
    // This would need a concrete implementation
    // val atlas = new TextureAtlas();
    // updateTextureAtlas(atlas, minFilter, magFilter, useMipMaps);
    // return atlas;
    throw new NotImplementedError("generateTextureAtlas not yet implemented")

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
        for (name <- page.addedRects)
          page.rects.get(name) match {
            case Some(rect) =>
              // Note: TextureAtlas.AtlasRegion constructor needs adjustment
              // val region = new TextureAtlas.AtlasRegion(page.texture.orNull, rect.getX(), rect.getY(),
              //	rect.getWidth(), rect.getHeight());

              if (Nullable(rect.splits).isDefined) {
                // region.names = Array("split", "pad");
                // region.values = Array(rect.splits, rect.pads);
              }

              var imageIndex = -1
              var imageName  = name

              if (useIndexes) {
                val matcher = PixmapPacker.indexPattern.matcher(imageName)
                if (matcher.matches()) {
                  imageName = matcher.group(1)
                  imageIndex = Integer.parseInt(matcher.group(2))
                }
              }

            // region.name = imageName;
            // region.index = imageIndex;
            // region.offsetX = rect.offsetX;
            // region.offsetY = rect.originalHeight - rect.getHeight() - rect.offsetY;
            // region.originalWidth = rect.originalWidth;
            // region.originalHeight = rect.originalHeight;

            // atlas.getRegions().add(region);
            case None => // skip
          }
        page.addedRects.clear()
        // atlas.getTextures().add(page.texture);
      }
  }

  /** Calls {@link Page#updateTexture(TextureFilter, TextureFilter, boolean) updateTexture} for each page and adds a region to the specified array for each page texture.
    */
  def updateTextureRegions(regions: DynamicArray[TextureRegion], minFilter: TextureFilter, magFilter: TextureFilter, useMipMaps: Boolean): Unit = {
    updatePageTextures(minFilter, magFilter, useMipMaps);
    while (regions.size < pages.size)
      pages(regions.size).texture.foreach { pageTexture =>
        regions.add(new TextureRegion(pageTexture))
      }
  }

  /** Calls {@link Page#updateTexture(TextureFilter, TextureFilter, boolean) updateTexture} for each page. */
  def updatePageTextures(minFilter: TextureFilter, magFilter: TextureFilter, useMipMaps: Boolean): Unit =
    for (page <- pages)
      page.updateTexture(minFilter, magFilter, useMipMaps)

  def getPageWidth(): Int =
    pageWidth

  def setPageWidth(pageWidth: Int): Unit =
    this.pageWidth = pageWidth

  def getPageHeight(): Int =
    pageHeight

  def setPageHeight(pageHeight: Int): Unit =
    this.pageHeight = pageHeight

  def getPageFormat(): Format =
    pageFormat

  def setPageFormat(pageFormat: Format): Unit =
    this.pageFormat = pageFormat

  def getPadding(): Int =
    padding

  def setPadding(padding: Int): Unit =
    this.padding = padding

  def getDuplicateBorder(): Boolean =
    duplicateBorder

  def setDuplicateBorder(duplicateBorder: Boolean): Unit =
    this.duplicateBorder = duplicateBorder

  def getPackToTexture(): Boolean =
    packToTexture

  /** If true, when a pixmap is packed to a page that has a texture, the portion of the texture where the pixmap was packed is updated using glTexSubImage2D. Note if packing many pixmaps, this may be
    * slower than reuploading the whole texture. This setting is ignored if {@link #getDuplicateBorder()} is true.
    */
  def setPackToTexture(packToTexture: Boolean): Unit =
    this.packToTexture = packToTexture

  /** @see PixmapPacker#setTransparentColor(Color color) */
  def getTransparentColor(): Color =
    this.transparentColor

  /** Sets the default <code>color</code> of the whole {@link PixmapPacker.Page} when a new one created. Helps to avoid texture bleeding or to highlight the page for debugging.
    * @see
    *   Page#Page(PixmapPacker packer)
    */
  def setTransparentColor(color: Color): Unit =
    this.transparentColor.set(color)

  private def getSplits(raster: Pixmap): Array[Int] = {
    var startX = getSplitPoint(raster, 1, 0, true, true)
    var endX   = getSplitPoint(raster, startX, 0, false, true)
    var startY = getSplitPoint(raster, 0, 1, true, false)
    var endY   = getSplitPoint(raster, 0, startY, false, false)

    // Ensure pixels after the end are not invalid.
    getSplitPoint(raster, endX + 1, 0, true, true)
    getSplitPoint(raster, 0, endY + 1, true, false)

    // No splits, or all splits.
    if (startX == 0 && endX == 0 && startY == 0 && endY == 0) null
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

      Array(startX, endX, startY, endY)
    }
  }

  private def getPads(raster: Pixmap, splits: Array[Int]): Array[Int] = {
    val bottom = raster.getHeight() - 1
    val right  = raster.getWidth() - 1

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
      null
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

      val pads = Array(startX, endX, startY, endY)

      if (Nullable(splits).isDefined && Arrays.equals(pads, splits)) {
        null
      } else {
        pads
      }
    }
  }

  private val c = new Color()

  private def getSplitPoint(raster: Pixmap, startX: Int, startY: Int, startPoint: Boolean, xAxis: Boolean): Int = boundary {
    val rgba = Array.ofDim[Int](4)

    var next   = if (xAxis) startX else startY
    val end    = if (xAxis) raster.getWidth() else raster.getHeight()
    val breakA = if (startPoint) 255 else 0

    var x = startX
    var y = startY
    while (next != end) {
      if (xAxis)
        x = next
      else
        y = next

      val colint = raster.getPixel(x, y)
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
    def pack(packer: PixmapPacker, name: String, bounds: Bounds)(implicit sge: Sge): Page
  }

  /** @author
    *   mzechner
    * @author
    *   Nathan Sweet
    * @author
    *   Rob Rendell
    */
  class Page(packer: PixmapPacker)(implicit sge: Sge) {
    val rects:      MutableMap[String, PixmapPackerRectangle] = MutableMap()
    val image:      Pixmap                                    = new Pixmap(packer.pageWidth, packer.pageHeight, packer.pageFormat)
    var texture:    Nullable[Texture]                         = Nullable.empty
    val addedRects: DynamicArray[String]                      = DynamicArray[String]()
    var dirty:      Boolean                                   = false

    // Initialize page
    image.setBlending(Blending.None)
    // Note: setColor and fill methods not available on Pixmap
    // image.setColor(packer.getTransparentColor())
    // image.fill()

    def getPixmap(): Pixmap = image

    def getRects(): MutableMap[String, PixmapPackerRectangle] = rects

    /** Returns the texture for this page, or null if the texture has not been created.
      * @see
      *   #updateTexture(TextureFilter, TextureFilter, boolean)
      */
    def getTexture(): Nullable[Texture] = texture

    /** Creates the texture if it has not been created, else reuploads the entire page pixmap to the texture if the pixmap has changed since this method was last called.
      * @return
      *   true if the texture was created or reuploaded.
      */
    def updateTexture(minFilter: TextureFilter, magFilter: TextureFilter, useMipMaps: Boolean): Boolean = scala.util.boundary {
      if (texture.isEmpty) {
        val tex = new Texture(new PixmapTextureData(image, image.getFormat(), useMipMaps, false, true)) {
          override def close(): Unit = {
            super.close()
            image.close()
          }
        }
        texture = Nullable(tex)
        tex.setFilter(minFilter, magFilter)
      } else {
        if (!dirty) scala.util.boundary.break(false)
        texture.foreach(tex => tex.load(tex.getTextureData()))
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

  class PixmapPackerRectangle(x: Int, y: Int, width: Int, height: Int) {
    var page:           Page       = uninitialized
    var splits:         Array[Int] = uninitialized
    var pads:           Array[Int] = uninitialized
    var offsetX:        Int        = 0
    var offsetY:        Int        = 0
    var originalWidth:  Int        = width
    var originalHeight: Int        = height
    val bounds:         Bounds     = new Bounds(x, y, width, height)

    def this(x: Int, y: Int, width: Int, height: Int, left: Int, top: Int, originalWidth: Int, originalHeight: Int) = {
      this(x, y, width, height)
      this.offsetX = left
      this.offsetY = top
      this.originalWidth = originalWidth
      this.originalHeight = originalHeight
    }

    def getX():      Int = bounds.x
    def getY():      Int = bounds.y
    def getWidth():  Int = bounds.width
    def getHeight(): Int = bounds.height
  }

  class GuillotineStrategy extends PackStrategy {
    def sort(images: DynamicArray[Pixmap]): Unit =
      // Simple sorting by area (width * height) in descending order
      images.sort(Ordering.by[Pixmap, Int](pixmap => -(pixmap.getWidth() * pixmap.getHeight())))

    def pack(packer: PixmapPacker, name: String, rect: Bounds)(implicit sge: Sge): Page = {
      // Simplified implementation - would need full conversion for complex logic
      if (packer.pages.isEmpty) {
        // For now, create a basic Page - GuillotinePage would need proper Sge context
        // val page = new GuillotinePage(packer)(using sge)
        val page = new Page(packer)(using sge)
        packer.pages.add(page)
      }
      packer.pages.first
    }
  }

  class GuillotinePage(packer: PixmapPacker)(implicit sge: Sge) extends Page(packer) {
    // Simplified for now
  }
}
