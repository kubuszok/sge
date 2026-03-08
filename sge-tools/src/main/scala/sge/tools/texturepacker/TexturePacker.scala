/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/tools/texturepacker/TexturePacker.java
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
 */
package sge
package tools
package texturepacker

import sge.files.{ FileHandle, FileType }
import sge.graphics.Pixmap.Format
import sge.graphics.Texture.{ TextureFilter, TextureWrap }
import sge.graphics.g2d.TextureAtlas.TextureAtlasData
import sge.math.MathUtils
import sge.utils.Nullable

import java.awt.{ Color, Graphics2D, RenderingHints }
import java.awt.image.BufferedImage
import java.io.{ File, FileOutputStream, IOException, OutputStreamWriter, Writer }
import javax.imageio.{ IIOImage, ImageIO, ImageWriteParam, ImageWriter }
import javax.imageio.stream.ImageOutputStream
import scala.collection.mutable.ArrayBuffer
import scala.util.boundary
import scala.util.boundary.break

/** @author
  *   Nathan Sweet
  */
class TexturePacker(rootDir: File, val settings: TexturePacker.Settings) {
  import TexturePacker.*

  var rootPath:               Nullable[String]           = Nullable.empty
  private var packer:         Packer                     = scala.compiletime.uninitialized
  private val imageProcessor: ImageProcessor             = newImageProcessor(settings)
  private val inputImages:    ArrayBuffer[InputImage]    = ArrayBuffer.empty
  private var progress:       Nullable[ProgressListener] = Nullable.empty

  {
    if (settings.pot) {
      if (settings.maxWidth != MathUtils.nextPowerOfTwo(settings.maxWidth))
        throw new RuntimeException("If pot is true, maxWidth must be a power of two: " + settings.maxWidth)
      if (settings.maxHeight != MathUtils.nextPowerOfTwo(settings.maxHeight))
        throw new RuntimeException("If pot is true, maxHeight must be a power of two: " + settings.maxHeight)
    }

    if (settings.multipleOfFour) {
      if (settings.maxWidth % 4 != 0)
        throw new RuntimeException("If mod4 is true, maxWidth must be evenly divisible by 4: " + settings.maxWidth)
      if (settings.maxHeight % 4 != 0)
        throw new RuntimeException("If mod4 is true, maxHeight must be evenly divisible by 4: " + settings.maxHeight)
    }

    if (settings.grid)
      packer = GridPacker(settings)
    else
      packer = MaxRectsPacker(settings)

    setRootDir(rootDir)
  }

  def this(settings: TexturePacker.Settings) = {
    this(null, settings) // @nowarn("msg=deprecated") null at Java boundary for optional rootDir
  }

  protected def newImageProcessor(settings: Settings): ImageProcessor =
    ImageProcessor(settings)

  /** @param rootDir
    *   Used to strip the root directory prefix from image file names, can be null.
    */
  def setRootDir(rootDir: File): Unit =
    if (rootDir == null) { // @nowarn("msg=deprecated") null at Java boundary
      rootPath = Nullable.empty
    } else {
      var path =
        try rootDir.getCanonicalPath()
        catch { case _: IOException => rootDir.getAbsolutePath() }
      path = path.replace('\\', '/')
      if (!path.endsWith("/")) path += "/"
      rootPath = Nullable(path)
    }

  def addImage(file: File): Unit = {
    val inputImage = InputImage()
    inputImage.file = Nullable(file)
    inputImage.rootPath = rootPath
    inputImages += inputImage
  }

  def addImage(image: BufferedImage, name: String): Unit = {
    val inputImage = InputImage()
    inputImage.image = Nullable(image)
    inputImage.name = Nullable(name)
    inputImages += inputImage
  }

  def setPacker(packer: Packer): Unit =
    this.packer = packer

  def pack(outputDir: File, packFileNameArg: String): Unit = {
    var packFileName = packFileNameArg
    if (packFileName.endsWith(settings.atlasExtension))
      packFileName = packFileName.substring(0, packFileName.length - settings.atlasExtension.length)
    outputDir.mkdirs()

    val prog: ProgressListener = progress.getOrElse {
      val p = new ProgressListener {
        def progress(progress: Float): Unit = {}
      }
      this.progress = Nullable(p)
      p
    }

    prog.start(1)
    val n = settings.scale.length
    var i = 0
    boundary {
      while (i < n) {
        prog.start(1f / n)

        imageProcessor.scale = settings.scale(i)

        if (settings.scaleResampling != null && settings.scaleResampling.length > i && settings.scaleResampling(i) != null)
          imageProcessor.resampling = settings.scaleResampling(i)

        prog.start(0.35f)
        prog.count = 0
        prog.total = inputImages.size
        var ii = 0
        val nn = inputImages.size
        while (ii < nn) {
          val inputImage = inputImages(ii)
          if (inputImage.file.isDefined)
            imageProcessor.addImage(inputImage.file.get, inputImage.rootPath)
          else
            imageProcessor.addImage(inputImage.image.get, inputImage.name.getOrElse(""))
          if (prog.update(ii + 1, nn)) break()
          ii += 1
          prog.count += 1
        }
        prog.end()

        prog.start(0.35f)
        prog.count = 0
        prog.total = imageProcessor.images.size
        val pages = packer.pack(prog, imageProcessor.images)
        prog.end()

        prog.start(0.29f)
        prog.count = 0
        prog.total = pages.size
        val scaledPackFileName = settings.getScaledPackFileName(packFileName, i)
        writeImages(outputDir, scaledPackFileName, pages)
        prog.end()

        prog.start(0.01f)
        try
          writePackFile(outputDir, scaledPackFileName, pages)
        catch {
          case ex: IOException =>
            throw new RuntimeException("Error writing pack file.", ex)
        }
        imageProcessor.clear()
        prog.end()

        prog.end()

        if (prog.update(i + 1, n)) break()
        i += 1
      }
    }
    prog.end()
  }

  private def writeImages(outputDir: File, scaledPackFileName: String, pages: ArrayBuffer[Page]): Unit = {
    val packFileNoExt = new File(outputDir, scaledPackFileName)
    val packDir       = packFileNoExt.getParentFile()
    val imageName     = packFileNoExt.getName()

    var fileIndex = 1
    val pn        = pages.size
    var p         = 0
    boundary {
      while (p < pn) {
        val page = pages(p)

        var width    = page.width
        var height   = page.height
        var edgePadX = 0
        var edgePadY = 0
        if (settings.edgePadding) {
          edgePadX = settings.paddingX
          edgePadY = settings.paddingY
          if (settings.duplicatePadding) {
            edgePadX /= 2
            edgePadY /= 2
          }
          page.x = edgePadX
          page.y = edgePadY
          width += edgePadX * 2
          height += edgePadY * 2
        }
        if (settings.pot) {
          width = MathUtils.nextPowerOfTwo(width)
          height = MathUtils.nextPowerOfTwo(height)
        }
        if (settings.multipleOfFour) {
          width = if (width % 4 == 0) width else width + 4 - (width % 4)
          height = if (height % 4 == 0) height else height + 4 - (height % 4)
        }
        width = Math.max(settings.minWidth, width)
        height = Math.max(settings.minHeight, height)
        page.imageWidth = width
        page.imageHeight = height

        var outputFile: File = null // @nowarn("msg=deprecated") assigned in loop below
        boundary {
          while (true) {
            var name = imageName
            if (fileIndex > 1) {
              // Last character is a digit or a digit + 'x'.
              val last = name.charAt(name.length - 1)
              if (
                Character.isDigit(last)
                || (name.length > 3 && last == 'x' && Character.isDigit(name.charAt(name.length - 2)))
              ) {
                name += "-"
              }
              name += fileIndex
            }
            fileIndex += 1
            outputFile = new File(packDir, name + "." + settings.outputFormat)
            if (!outputFile.exists()) break()
          }
        }
        FileHandle(outputFile, FileType.Absolute).parent().mkdirs()
        page.imageName = Nullable(outputFile.getName())

        val canvas = new BufferedImage(width, height, getBufferedImageType(settings.format))
        var g      = canvas.getGraphics().asInstanceOf[Graphics2D]

        if (!settings.silent) System.out.println("Writing " + canvas.getWidth() + "x" + canvas.getHeight() + ": " + outputFile)

        val prog = progress.get
        prog.start(1 / pn.toFloat)
        val rn = page.outputRects.size
        var r  = 0
        boundary {
          while (r < rn) {
            val rect  = page.outputRects(r)
            val image = rect.getImage(imageProcessor)
            val iw    = image.getWidth()
            val ih    = image.getHeight()
            val rectX = page.x + rect.x
            val rectY = page.y + page.height - rect.y - (rect.height - settings.paddingY)
            if (settings.duplicatePadding) {
              val amountX = settings.paddingX / 2
              val amountY = settings.paddingY / 2
              if (rect.rotated) {
                // Copy corner pixels to fill corners of the padding.
                var ci = 1
                while (ci <= amountX) {
                  var cj = 1
                  while (cj <= amountY) {
                    plot(canvas, rectX - cj, rectY + iw - 1 + ci, image.getRGB(0, 0))
                    plot(canvas, rectX + ih - 1 + cj, rectY + iw - 1 + ci, image.getRGB(0, ih - 1))
                    plot(canvas, rectX - cj, rectY - ci, image.getRGB(iw - 1, 0))
                    plot(canvas, rectX + ih - 1 + cj, rectY - ci, image.getRGB(iw - 1, ih - 1))
                    cj += 1
                  }
                  ci += 1
                }
                // Copy edge pixels into padding.
                ci = 1
                while (ci <= amountY) {
                  var cj = 0
                  while (cj < iw) {
                    plot(canvas, rectX - ci, rectY + iw - 1 - cj, image.getRGB(cj, 0))
                    plot(canvas, rectX + ih - 1 + ci, rectY + iw - 1 - cj, image.getRGB(cj, ih - 1))
                    cj += 1
                  }
                  ci += 1
                }
                ci = 1
                while (ci <= amountX) {
                  var cj = 0
                  while (cj < ih) {
                    plot(canvas, rectX + cj, rectY - ci, image.getRGB(iw - 1, cj))
                    plot(canvas, rectX + cj, rectY + iw - 1 + ci, image.getRGB(0, cj))
                    cj += 1
                  }
                  ci += 1
                }
              } else {
                // Copy corner pixels to fill corners of the padding.
                var ci = 1
                while (ci <= amountX) {
                  var cj = 1
                  while (cj <= amountY) {
                    plot(canvas, rectX - ci, rectY - cj, image.getRGB(0, 0))
                    plot(canvas, rectX - ci, rectY + ih - 1 + cj, image.getRGB(0, ih - 1))
                    plot(canvas, rectX + iw - 1 + ci, rectY - cj, image.getRGB(iw - 1, 0))
                    plot(canvas, rectX + iw - 1 + ci, rectY + ih - 1 + cj, image.getRGB(iw - 1, ih - 1))
                    cj += 1
                  }
                  ci += 1
                }
                // Copy edge pixels into padding.
                ci = 1
                while (ci <= amountY) {
                  copy(image, 0, 0, iw, 1, canvas, rectX, rectY - ci, rect.rotated)
                  copy(image, 0, ih - 1, iw, 1, canvas, rectX, rectY + ih - 1 + ci, rect.rotated)
                  ci += 1
                }
                ci = 1
                while (ci <= amountX) {
                  copy(image, 0, 0, 1, ih, canvas, rectX - ci, rectY, rect.rotated)
                  copy(image, iw - 1, 0, 1, ih, canvas, rectX + iw - 1 + ci, rectY, rect.rotated)
                  ci += 1
                }
              }
            }
            copy(image, 0, 0, iw, ih, canvas, rectX, rectY, rect.rotated)
            if (settings.debug) {
              g.setColor(Color.magenta)
              g.drawRect(rectX, rectY, rect.width - settings.paddingX - 1, rect.height - settings.paddingY - 1)
            }

            if (prog.update(r + 1, rn)) break()
            r += 1
          }
        }
        prog.end()

        var finalCanvas = canvas
        if (
          settings.bleed && !settings.premultiplyAlpha
          && !(settings.outputFormat.equalsIgnoreCase("jpg") || settings.outputFormat.equalsIgnoreCase("jpeg"))
        ) {
          finalCanvas = ColorBleedEffect().processImage(finalCanvas, settings.bleedIterations)
          g = finalCanvas.getGraphics().asInstanceOf[Graphics2D]
        }

        if (settings.debug) {
          g.setColor(Color.magenta)
          g.drawRect(0, 0, width - 1, height - 1)
        }

        var ios: Nullable[ImageOutputStream] = Nullable.empty
        try
          if (settings.outputFormat.equalsIgnoreCase("jpg") || settings.outputFormat.equalsIgnoreCase("jpeg")) {
            val newImage = new BufferedImage(finalCanvas.getWidth(), finalCanvas.getHeight(), BufferedImage.TYPE_3BYTE_BGR)
            newImage.getGraphics().drawImage(finalCanvas, 0, 0, null) // @nowarn("msg=deprecated") AWT observer null
            finalCanvas = newImage

            val writers = ImageIO.getImageWritersByFormatName("jpg")
            val writer  = writers.next()
            val param   = writer.getDefaultWriteParam()
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT)
            param.setCompressionQuality(settings.jpegQuality)
            val iosVal = ImageIO.createImageOutputStream(outputFile)
            ios = Nullable(iosVal)
            writer.setOutput(iosVal)
            writer.write(null, IIOImage(finalCanvas, null, null), param) // @nowarn("msg=deprecated") IIO null params
          } else {
            if (settings.premultiplyAlpha) finalCanvas.getColorModel().coerceData(finalCanvas.getRaster(), true)
            ImageIO.write(finalCanvas, "png", outputFile)
          }
        catch {
          case ex: IOException =>
            throw new RuntimeException("Error writing file: " + outputFile, ex)
        } finally
          ios.foreach { s =>
            try s.close()
            catch { case _: Exception => }
          }

        if (prog.update(p + 1, pn)) break()
        prog.count += 1
        p += 1
      }
    }
  }

  private def writePackFile(outputDir: File, scaledPackFileName: String, pages: ArrayBuffer[Page]): Unit = {
    val packFile = new File(outputDir, scaledPackFileName + settings.atlasExtension)
    val packDir  = packFile.getParentFile()
    packDir.mkdirs()

    if (packFile.exists()) {
      // Make sure there aren't duplicate names.
      val textureAtlasData = TextureAtlasData(
        FileHandle(packFile, FileType.Absolute),
        FileHandle(packFile, FileType.Absolute),
        false
      )
      for (page <- pages)
        for (rect <- page.outputRects) {
          val rectName = Rect.getAtlasName(rect.name.get, settings.flattenPaths)
          val regIter  = textureAtlasData.regions.iterator
          while (regIter.hasNext) {
            val region = regIter.next()
            if (region.name.equals(rectName)) {
              throw new RuntimeException(
                "A region with the name \"" + rectName + "\" has already been packed: " + rect.name.get
              )
            }
          }
        }
    }

    val appending = packFile.exists()
    val writer    = new OutputStreamWriter(new FileOutputStream(packFile, true), "UTF-8")
    try {
      var i = 0
      val n = pages.size
      while (i < n) {
        val page = pages(i)

        if (settings.legacyOutput)
          writePageLegacy(writer, page)
        else {
          if (i != 0 || appending) writer.write("\n")
          writePage(writer, appending, page)
        }

        page.outputRects.sortInPlace()
        for (rect <- page.outputRects) {
          if (settings.legacyOutput)
            writeRectLegacy(writer, page, rect, rect.name.get)
          else
            writeRect(writer, page, rect, rect.name.get)
          val aliases = ArrayBuffer.from(rect.aliases)
          aliases.sortInPlace()
          for (alias <- aliases) {
            val aliasRect = Rect()
            aliasRect.set(rect)
            alias.apply(aliasRect)
            if (settings.legacyOutput)
              writeRectLegacy(writer, page, aliasRect, alias.name)
            else
              writeRect(writer, page, aliasRect, alias.name)
          }
        }
        i += 1
      }
    } finally
      writer.close()
  }

  private def writePage(writer: OutputStreamWriter, appending: Boolean, page: Page): Unit = {
    val tab   = if (settings.prettyPrint) "\t" else ""
    val colon = if (settings.prettyPrint) ": " else ":"
    val comma = if (settings.prettyPrint) ", " else ","

    writer.write(page.imageName.get + "\n")
    writer.write(tab + "size" + colon + page.imageWidth + comma + page.imageHeight + "\n")

    if (settings.format != Format.RGBA8888) writer.write(tab + "format" + colon + settings.format + "\n")

    if (settings.filterMin != TextureFilter.Nearest || settings.filterMag != TextureFilter.Nearest)
      writer.write(tab + "filter" + colon + settings.filterMin + comma + settings.filterMag + "\n")

    val repeatValue = getRepeatValue()
    repeatValue.foreach(rv => writer.write(tab + "repeat" + colon + rv + "\n"))

    if (settings.premultiplyAlpha) writer.write(tab + "pma" + colon + "true\n")
  }

  private def writeRect(writer: Writer, page: Page, rect: Rect, name: String): Unit = {
    val tab   = if (settings.prettyPrint) "\t" else ""
    val colon = if (settings.prettyPrint) ": " else ":"
    val comma = if (settings.prettyPrint) ", " else ","

    writer.write(Rect.getAtlasName(name, settings.flattenPaths) + "\n")
    if (rect.index != -1) writer.write(tab + "index" + colon + rect.index + "\n")

    writer.write(
      tab + "bounds" + colon //
        + (page.x + rect.x) + comma + (page.y + page.height - rect.y - (rect.height - settings.paddingY)) + comma //
        + rect.regionWidth + comma + rect.regionHeight + "\n"
    )

    val offsetY = rect.originalHeight - rect.regionHeight - rect.offsetY
    if (
      rect.offsetX != 0 || offsetY != 0 //
      || rect.originalWidth != rect.regionWidth || rect.originalHeight != rect.regionHeight
    ) {
      writer.write(
        tab + "offsets" + colon //
          + rect.offsetX + comma + offsetY + comma //
          + rect.originalWidth + comma + rect.originalHeight + "\n"
      )
    }

    if (rect.rotated) writer.write(tab + "rotate" + colon + rect.rotated + "\n")

    rect.splits.foreach { s =>
      writer.write(
        tab + "split" + colon //
          + s(0) + comma + s(1) + comma //
          + s(2) + comma + s(3) + "\n"
      )
    }

    rect.pads.foreach { pd =>
      if (rect.splits.isEmpty) writer.write(tab + "split" + colon + "0" + comma + "0" + comma + "0" + comma + "0\n")
      writer.write(
        tab + "pad" + colon + pd(0) + comma + pd(1) + comma + pd(2) + comma + pd(3) + "\n"
      )
    }
  }

  private def writePageLegacy(writer: OutputStreamWriter, page: Page): Unit = {
    writer.write("\n" + page.imageName.get + "\n")
    writer.write("size: " + page.imageWidth + ", " + page.imageHeight + "\n")
    writer.write("format: " + settings.format + "\n")
    writer.write("filter: " + settings.filterMin + ", " + settings.filterMag + "\n")
    val repeatValue = getRepeatValue()
    writer.write("repeat: " + repeatValue.getOrElse("none") + "\n")
  }

  private def writeRectLegacy(writer: Writer, page: Page, rect: Rect, name: String): Unit = {
    writer.write(Rect.getAtlasName(name, settings.flattenPaths) + "\n")
    writer.write("  rotate: " + rect.rotated + "\n")
    writer.write(
      "  xy: " + (page.x + rect.x) + ", " + (page.y + page.height - rect.y - (rect.height - settings.paddingY)) + "\n"
    )

    writer.write("  size: " + rect.regionWidth + ", " + rect.regionHeight + "\n")
    rect.splits.foreach { s =>
      writer.write(
        "  split: " //
          + s(0) + ", " + s(1) + ", " + s(2) + ", " + s(3) + "\n"
      )
    }
    rect.pads.foreach { pd =>
      if (rect.splits.isEmpty) writer.write("  split: 0, 0, 0, 0\n")
      writer.write("  pad: " + pd(0) + ", " + pd(1) + ", " + pd(2) + ", " + pd(3) + "\n")
    }
    writer.write("  orig: " + rect.originalWidth + ", " + rect.originalHeight + "\n")
    writer.write("  offset: " + rect.offsetX + ", " + (rect.originalHeight - rect.regionHeight - rect.offsetY) + "\n")
    writer.write("  index: " + rect.index + "\n")
  }

  private def getRepeatValue(): Nullable[String] =
    if (settings.wrapX == TextureWrap.Repeat && settings.wrapY == TextureWrap.Repeat) Nullable("xy")
    else if (settings.wrapX == TextureWrap.Repeat && settings.wrapY == TextureWrap.ClampToEdge) Nullable("x")
    else if (settings.wrapX == TextureWrap.ClampToEdge && settings.wrapY == TextureWrap.Repeat) Nullable("y")
    else Nullable.empty

  private def getBufferedImageType(format: Format): Int =
    settings.format match {
      case Format.RGBA8888 | Format.RGBA4444 => BufferedImage.TYPE_INT_ARGB
      case Format.RGB565 | Format.RGB888     => BufferedImage.TYPE_INT_RGB
      case Format.Alpha                      => BufferedImage.TYPE_BYTE_GRAY
      case _                                 => throw new RuntimeException("Unsupported format: " + settings.format)
    }

  /** @param progressListener
    *   May be null.
    */
  def setProgressListener(progressListener: ProgressListener): Unit =
    this.progress = Nullable(progressListener)
}

/** @author
  *   Nathan Sweet
  */
object TexturePacker {

  private def plot(dst: BufferedImage, x: Int, y: Int, argb: Int): Unit =
    if (0 <= x && x < dst.getWidth() && 0 <= y && y < dst.getHeight()) dst.setRGB(x, y, argb)

  private def copy(
    src:     BufferedImage,
    x:       Int,
    y:       Int,
    w:       Int,
    h:       Int,
    dst:     BufferedImage,
    dx:      Int,
    dy:      Int,
    rotated: Boolean
  ): Unit =
    if (rotated) {
      var i = 0
      while (i < w) {
        var j = 0
        while (j < h) {
          plot(dst, dx + j, dy + w - i - 1, src.getRGB(x + i, y + j))
          j += 1
        }
        i += 1
      }
    } else {
      var i = 0
      while (i < w) {
        var j = 0
        while (j < h) {
          plot(dst, dx + i, dy + j, src.getRGB(x + i, y + j))
          j += 1
        }
        i += 1
      }
    }

  /** @author
    *   Nathan Sweet
    */
  class Page {
    var imageName:      Nullable[String]  = Nullable.empty
    var outputRects:    ArrayBuffer[Rect] = ArrayBuffer.empty
    var remainingRects: ArrayBuffer[Rect] = ArrayBuffer.empty
    var occupancy:      Float             = 0f
    var x:              Int               = 0
    var y:              Int               = 0
    var width:          Int               = 0
    var height:         Int               = 0
    var imageWidth:     Int               = 0
    var imageHeight:    Int               = 0
  }

  /** @author
    *   Regnarock
    * @author
    *   Nathan Sweet
    */
  class Alias(rect: Rect) extends Ordered[Alias] {
    val name:           String               = rect.name.get
    val index:          Int                  = rect.index
    val splits:         Nullable[Array[Int]] = rect.splits
    val pads:           Nullable[Array[Int]] = rect.pads
    val offsetX:        Int                  = rect.offsetX
    val offsetY:        Int                  = rect.offsetY
    val originalWidth:  Int                  = rect.originalWidth
    val originalHeight: Int                  = rect.originalHeight

    def apply(rect: Rect): Unit = {
      rect.name = Nullable(name)
      rect.index = index
      rect.splits = splits
      rect.pads = pads
      rect.offsetX = offsetX
      rect.offsetY = offsetY
      rect.originalWidth = originalWidth
      rect.originalHeight = originalHeight
    }

    override def compare(that: Alias): Int = name.compareTo(that.name)
  }

  /** @author
    *   Nathan Sweet
    */
  class Rect extends Ordered[Rect] {
    var name:           Nullable[String]                        = Nullable.empty
    var offsetX:        Int                                     = 0
    var offsetY:        Int                                     = 0
    var regionWidth:    Int                                     = 0
    var regionHeight:   Int                                     = 0
    var originalWidth:  Int                                     = 0
    var originalHeight: Int                                     = 0
    var x:              Int                                     = 0
    var y:              Int                                     = 0
    var width:          Int                                     = 0 // Portion of page taken by this region, including padding.
    var height:         Int                                     = 0
    var index:          Int                                     = -1
    var rotated:        Boolean                                 = false
    var aliases:        scala.collection.mutable.HashSet[Alias] = scala.collection.mutable.HashSet.empty
    var splits:         Nullable[Array[Int]]                    = Nullable.empty
    var pads:           Nullable[Array[Int]]                    = Nullable.empty
    var canRotate:      Boolean                                 = true

    private[texturepacker] var isPatch: Boolean                 = false
    private var image:                  Nullable[BufferedImage] = Nullable.empty
    private var file:                   Nullable[File]          = Nullable.empty
    var score1:                         Int                     = 0
    var score2:                         Int                     = 0

    def this(source: BufferedImage, left: Int, top: Int, newWidth: Int, newHeight: Int, isPatch: Boolean) = {
      this()
      image = Nullable(
        new BufferedImage(
          source.getColorModel(),
          source.getRaster().createWritableChild(left, top, newWidth, newHeight, 0, 0, null), // @nowarn("msg=deprecated") AWT null
          source.getColorModel().isAlphaPremultiplied(),
          null // @nowarn("msg=deprecated") AWT null properties
        )
      )
      offsetX = left
      offsetY = top
      regionWidth = newWidth
      regionHeight = newHeight
      originalWidth = source.getWidth()
      originalHeight = source.getHeight()
      width = newWidth
      height = newHeight
      this.isPatch = isPatch
    }

    /** Clears the image for this rect, which will be loaded from the specified file by [[getImage]]. */
    def unloadImage(file: File): Unit = {
      this.file = Nullable(file)
      image = Nullable.empty
    }

    def getImage(imageProcessor: ImageProcessor): BufferedImage =
      if (image.isDefined) image.get
      else {
        val loadedImage: BufferedImage =
          try ImageIO.read(file.get)
          catch {
            case ex: IOException =>
              throw new RuntimeException("Error reading image: " + file.get, ex)
          }
        if (loadedImage == null) throw new RuntimeException("Unable to read image: " + file.get) // @nowarn("msg=deprecated") ImageIO returns null
        var n = this.name.get
        if (isPatch) n += ".9"
        imageProcessor.processImage(loadedImage, n).get.getImage(null) // @nowarn("msg=deprecated") null imageProcessor in recursive call
      }

    private[texturepacker] def set(rect: Rect): Unit = {
      name = rect.name
      image = rect.image
      offsetX = rect.offsetX
      offsetY = rect.offsetY
      regionWidth = rect.regionWidth
      regionHeight = rect.regionHeight
      originalWidth = rect.originalWidth
      originalHeight = rect.originalHeight
      x = rect.x
      y = rect.y
      width = rect.width
      height = rect.height
      index = rect.index
      rotated = rect.rotated
      aliases = rect.aliases
      splits = rect.splits
      pads = rect.pads
      canRotate = rect.canRotate
      score1 = rect.score1
      score2 = rect.score2
      file = rect.file
      isPatch = rect.isPatch
    }

    private[texturepacker] def copyPositionFrom(rect: Rect): Unit = {
      x = rect.x
      y = rect.y
      width = rect.width
      height = rect.height
    }

    override def compare(that: Rect): Int = name.get.compareTo(that.name.get)

    override def equals(obj: Any): Boolean =
      obj match {
        case other: Rect =>
          if (this eq other) true
          else if (name.isEmpty && other.name.isEmpty) false
          else name.get == other.name.get
        case _ => false
      }

    override def hashCode(): Int = if (name.isDefined) name.get.hashCode() else super.hashCode()

    override def toString: String =
      name.get + (if (index != -1) "_" + index else "") + "[" + x + "," + y + " " + width + "x" + height + "]"
  }

  object Rect {
    def getAtlasName(name: String, flattenPaths: Boolean): String =
      if (flattenPaths) FileHandle(new File(name), FileType.Absolute).name() else name
  }

  enum Resampling(val value: AnyRef) {
    case nearest extends Resampling(RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
    case bilinear extends Resampling(RenderingHints.VALUE_INTERPOLATION_BILINEAR)
    case bicubic extends Resampling(RenderingHints.VALUE_INTERPOLATION_BICUBIC)
  }

  /** Packs using defaults settings.
    * @see
    *   [[TexturePacker.process(Settings, String, String, String)]]
    */
  def process(input: String, output: String, packFileName: String): Unit =
    process(Settings(), input, output, packFileName)

  def process(settings: Settings, input: String, output: String, packFileName: String): Unit =
    process(settings, input, output, packFileName, Nullable.empty)

  /** @param input
    *   Directory containing individual images to be packed.
    * @param output
    *   Directory where the pack file and page images will be written.
    * @param packFileName
    *   The name of the pack file. Also used to name the page images.
    * @param progress
    *   May be null.
    */
  def process(
    settings:     Settings,
    input:        String,
    output:       String,
    packFileName: String,
    progress:     Nullable[ProgressListener]
  ): Unit =
    try {
      val processor = TexturePackerFileProcessor(settings, packFileName, progress)
      processor.process(new File(input), new File(output))
    } catch {
      case ex: Exception =>
        throw new RuntimeException("Error packing images.", ex)
    }

  /** @return
    *   true if the output file does not yet exist or its last modification date is before the last modification date of the input file
    */
  def isModified(input: String, output: String, packFileName: String, settings: Settings): Boolean = {
    var packFullFileName = output
    if (!packFullFileName.endsWith("/")) packFullFileName += "/"
    packFullFileName += packFileName
    packFullFileName += settings.atlasExtension

    // Check against the only file we know for sure will exist and will be changed if any asset changes: the atlas file.
    val outputFile = new File(packFullFileName)
    if (!outputFile.exists()) true
    else {
      val inputFile = new File(input)
      if (!inputFile.exists()) throw new IllegalArgumentException("Input file does not exist: " + inputFile.getAbsolutePath())
      isModifiedRecursive(inputFile, outputFile.lastModified())
    }
  }

  private def isModifiedRecursive(file: File, lastModified: Long): Boolean =
    if (file.lastModified() > lastModified) true
    else {
      val children = file.listFiles()
      if (children != null) { // @nowarn("msg=deprecated") listFiles returns null
        var i     = 0
        var found = false
        while (i < children.length && !found) {
          if (isModifiedRecursive(children(i), lastModified)) found = true
          i += 1
        }
        found
      } else false
    }

  def processIfModified(input: String, output: String, packFileName: String): Boolean = {
    // Default settings (Needed to access the default atlas extension string)
    val settings = Settings()
    if (isModified(input, output, packFileName, settings)) {
      process(settings, input, output, packFileName)
      true
    } else false
  }

  def processIfModified(settings: Settings, input: String, output: String, packFileName: String): Boolean =
    if (isModified(input, output, packFileName, settings)) {
      process(settings, input, output, packFileName)
      true
    } else false

  trait Packer {
    def pack(inputRects: ArrayBuffer[Rect]):                               ArrayBuffer[Page]
    def pack(progress:   ProgressListener, inputRects: ArrayBuffer[Rect]): ArrayBuffer[Page]
  }

  final class InputImage {
    var file:     Nullable[File]          = Nullable.empty
    var rootPath: Nullable[String]        = Nullable.empty
    var name:     Nullable[String]        = Nullable.empty
    var image:    Nullable[BufferedImage] = Nullable.empty
  }

  abstract class ProgressListener {
    private var scale:             Float              = 1f
    private var lastUpdate:        Float              = 0f
    private val portions:          ArrayBuffer[Float] = ArrayBuffer.empty
    @volatile private var _cancel: Boolean            = false
    private var _message:          String             = ""
    var count:                     Int                = 0
    var total:                     Int                = 0

    def reset(): Unit = {
      scale = 1
      _message = ""
      count = 0
      total = 0
      progress(0)
    }

    def set(message: String): Unit = {}

    def start(portion: Float): Unit = {
      if (portion == 0) throw new IllegalArgumentException("portion cannot be 0.")
      portions += lastUpdate
      portions += scale * portion
      portions += scale
      scale *= portion
    }

    /** Returns true if cancelled. */
    def update(count: Int, total: Int): Boolean = {
      update(if (total == 0) 0 else count / total.toFloat)
      isCancelled()
    }

    def update(percent: Float): Unit = {
      lastUpdate = portions(portions.size - 3) + portions(portions.size - 2) * percent
      progress(lastUpdate)
    }

    def end(): Unit = {
      scale = portions.remove(portions.size - 1)
      val portion = portions.remove(portions.size - 1)
      lastUpdate = portions.remove(portions.size - 1) + portion
      progress(lastUpdate)
    }

    def cancel(): Unit =
      _cancel = true

    def isCancelled(): Boolean = _cancel

    def message: String = _message

    def message_=(message: String): Unit = {
      _message = message
      progress(lastUpdate)
    }

    def progress(progress: Float): Unit
  }

  /** @author
    *   Nathan Sweet
    */
  class Settings {
    var pot:                   Boolean           = true
    var multipleOfFour:        Boolean           = false
    var paddingX:              Int               = 2
    var paddingY:              Int               = 2
    var edgePadding:           Boolean           = true
    var duplicatePadding:      Boolean           = false
    var rotation:              Boolean           = false
    var minWidth:              Int               = 16
    var minHeight:             Int               = 16
    var maxWidth:              Int               = 1024
    var maxHeight:             Int               = 1024
    var square:                Boolean           = false
    var stripWhitespaceX:      Boolean           = false
    var stripWhitespaceY:      Boolean           = false
    var alphaThreshold:        Int               = 0
    var filterMin:             TextureFilter     = TextureFilter.Nearest
    var filterMag:             TextureFilter     = TextureFilter.Nearest
    var wrapX:                 TextureWrap       = TextureWrap.ClampToEdge
    var wrapY:                 TextureWrap       = TextureWrap.ClampToEdge
    var format:                Format            = Format.RGBA8888
    var alias:                 Boolean           = true
    var outputFormat:          String            = "png"
    var jpegQuality:           Float             = 0.9f
    var ignoreBlankImages:     Boolean           = true
    var fast:                  Boolean           = false
    var debug:                 Boolean           = false
    var silent:                Boolean           = false
    var combineSubdirectories: Boolean           = false
    var ignore:                Boolean           = false
    var flattenPaths:          Boolean           = false
    var premultiplyAlpha:      Boolean           = false
    var useIndexes:            Boolean           = true
    var bleed:                 Boolean           = true
    var bleedIterations:       Int               = 2
    var limitMemory:           Boolean           = true
    var grid:                  Boolean           = false
    var scale:                 Array[Float]      = Array(1f)
    var scaleSuffix:           Array[String]     = Array("")
    var scaleResampling:       Array[Resampling] = Array(Resampling.bicubic)
    var atlasExtension:        String            = ".atlas"
    var prettyPrint:           Boolean           = true
    var legacyOutput:          Boolean           = true

    /** @see
      *   [[set]]
      */
    def this(settings: Settings) = {
      this()
      set(settings)
    }

    /** Copies values from another instance to the current one */
    def set(settings: Settings): Unit = {
      fast = settings.fast
      rotation = settings.rotation
      pot = settings.pot
      multipleOfFour = settings.multipleOfFour
      minWidth = settings.minWidth
      minHeight = settings.minHeight
      maxWidth = settings.maxWidth
      maxHeight = settings.maxHeight
      paddingX = settings.paddingX
      paddingY = settings.paddingY
      edgePadding = settings.edgePadding
      duplicatePadding = settings.duplicatePadding
      alphaThreshold = settings.alphaThreshold
      ignoreBlankImages = settings.ignoreBlankImages
      stripWhitespaceX = settings.stripWhitespaceX
      stripWhitespaceY = settings.stripWhitespaceY
      alias = settings.alias
      format = settings.format
      jpegQuality = settings.jpegQuality
      outputFormat = settings.outputFormat
      filterMin = settings.filterMin
      filterMag = settings.filterMag
      wrapX = settings.wrapX
      wrapY = settings.wrapY
      debug = settings.debug
      silent = settings.silent
      combineSubdirectories = settings.combineSubdirectories
      ignore = settings.ignore
      flattenPaths = settings.flattenPaths
      premultiplyAlpha = settings.premultiplyAlpha
      square = settings.square
      useIndexes = settings.useIndexes
      bleed = settings.bleed
      bleedIterations = settings.bleedIterations
      limitMemory = settings.limitMemory
      grid = settings.grid
      scale = java.util.Arrays.copyOf(settings.scale, settings.scale.length)
      scaleSuffix = java.util.Arrays.copyOf(settings.scaleSuffix, settings.scaleSuffix.length)
      scaleResampling = java.util.Arrays.copyOf(settings.scaleResampling, settings.scaleResampling.length)
      atlasExtension = settings.atlasExtension
      prettyPrint = settings.prettyPrint
      legacyOutput = settings.legacyOutput
    }

    def getScaledPackFileName(packFileName: String, scaleIndex: Int): String = {
      var result = packFileName
      // Use suffix if not empty string.
      if (scaleIndex < scaleSuffix.length && scaleSuffix(scaleIndex).length > 0)
        result += scaleSuffix(scaleIndex)
      else {
        // Otherwise if scale != 1 or multiple scales, use subdirectory.
        val scaleValue = scale(scaleIndex)
        if (scale.length != 1) {
          result = (if (scaleValue == scaleValue.toInt) scaleValue.toInt.toString else scaleValue.toString) + "/" + result
        }
      }
      result
    }
  }

  def main(args: Array[String]): Unit = {
    var settings: Nullable[Settings] = Nullable.empty
    var input:    Nullable[String]   = Nullable.empty
    var output:   Nullable[String]   = Nullable.empty
    var packFileName = "pack.atlas"

    args.length match {
      case 4 =>
        // Settings file parsing would go here -- for now just use defaults
        packFileName = args(2)
        output = Nullable(args(1))
        input = Nullable(args(0))
      case 3 =>
        packFileName = args(2)
        output = Nullable(args(1))
        input = Nullable(args(0))
      case 2 =>
        output = Nullable(args(1))
        input = Nullable(args(0))
      case 1 =>
        input = Nullable(args(0))
      case _ =>
        System.out.println("Usage: inputDir [outputDir] [packFileName] [settingsFileName]")
        System.exit(0)
    }

    if (output.isEmpty) {
      val inputFile = new File(input.get)
      output = Nullable(new File(inputFile.getParentFile(), inputFile.getName() + "-packed").getAbsolutePath())
    }
    val s = settings.getOrElse(Settings())

    process(s, input.get, output.get, packFileName)
  }
}
