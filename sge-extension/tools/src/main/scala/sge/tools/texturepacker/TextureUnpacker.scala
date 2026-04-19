/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/tools/texturepacker/TextureUnpacker.java
 * Original authors: Geert Konijnendijk, Nathan Sweet, Michael Bazos
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
 * Covenant-baseline-loc: 312
 * Covenant-baseline-methods: ATLAS_FILE_EXTENSION,DEFAULT_OUTPUT_PATH,HELP,NINEPATCH_PADDING,OUTPUT_TYPE,TextureUnpacker,atlasFile,checkDirectoryValidity,checkFile,endX,endY,extractImage,extractNinePatch,g2,imageDir,main,numArgs,outputDir,outputDirFile,pads,pageIter,parseArguments,printExceptionAndExit,quiet,setQuiet,splitAtlas,splitImage,splits,startX,startY,unpacker
 * Covenant-source-reference: com/badlogic/gdx/tools/texturepacker/TextureUnpacker.java
 * Covenant-verified: 2026-04-19
 */
package sge
package tools
package texturepacker

import sge.files.{ FileHandle, FileType }
import sge.graphics.g2d.TextureAtlas.TextureAtlasData
import sge.utils.Nullable

import java.awt.{ Color, Graphics2D }
import java.awt.geom.AffineTransform
import java.awt.image.{ AffineTransformOp, BufferedImage }
import java.io.{ File, IOException }
import javax.imageio.ImageIO

/** Unpacks a texture atlas into individual image files.
  * @author
  *   Geert Konijnendijk
  * @author
  *   Nathan Sweet
  * @author
  *   Michael Bazos
  */
class TextureUnpacker {
  import TextureUnpacker.*

  private var quiet: Boolean = false

  /** Checks the command line arguments for correctness.
    * @return
    *   0 If arguments are invalid, Number of arguments otherwise.
    */
  private def parseArguments(args: Array[String]): Int = {
    val numArgs = args.length
    // check if number of args is right
    if (numArgs < 1) 0
    else {
      // check if the input file's extension is right
      val extension = args(0).substring(args(0).length - ATLAS_FILE_EXTENSION.length).equals(ATLAS_FILE_EXTENSION)
      // check if the directory names are valid
      var directory = true
      if (numArgs >= 2) directory &= checkDirectoryValidity(args(1))
      if (numArgs == 3) directory &= checkDirectoryValidity(args(2))
      if (extension && directory) numArgs else 0
    }
  }

  private def checkDirectoryValidity(directory: String): Boolean = {
    val checkFile = new File(directory)
    // try to get the canonical path, if this fails the path is not valid
    try {
      checkFile.getCanonicalPath()
      true
    } catch {
      case _: Exception => false
    }
  }

  /** Splits an atlas into separate image and ninepatch files. */
  def splitAtlas(atlas: TextureAtlasData, outputDir: String): Unit = {
    // create the output directory if it did not exist yet
    val outputDirFile = new File(outputDir)
    if (!outputDirFile.exists()) {
      outputDirFile.mkdirs()
      if (!quiet) System.out.println(String.format("Creating directory: %s", outputDirFile.getPath()))
    }

    val pageIter = atlas.pages.iterator
    while (pageIter.hasNext) {
      val page = pageIter.next()
      // load the image file belonging to this page as a Buffered Image
      val file = page.textureFile.get.internalFile
      if (!file.exists()) throw new RuntimeException("Unable to find atlas image: " + file.getAbsolutePath())
      var img: Nullable[BufferedImage] = Nullable.empty
      try
        img = Nullable(ImageIO.read(file))
      catch {
        case e: IOException =>
          printExceptionAndExit(e)
      }

      val regionIter = atlas.regions.iterator
      while (regionIter.hasNext) {
        val region = regionIter.next()
        if (!quiet) {
          System.out.println(
            String.format(
              "Processing image for %s: x[%s] y[%s] w[%s] h[%s], rotate[%s]",
              region.name,
              region.left.asInstanceOf[AnyRef],
              region.top.asInstanceOf[AnyRef],
              region.width.asInstanceOf[AnyRef],
              region.height.asInstanceOf[AnyRef],
              region.rotate.asInstanceOf[AnyRef]
            )
          )
        }

        // check if the page this region is in is currently loaded in a Buffered Image
        if (region.page eq page) {
          var splitImage: Nullable[BufferedImage] = Nullable.empty
          var extension:  Nullable[String]        = Nullable.empty

          // check if the region is a ninepatch or a normal image and delegate accordingly
          if (region.findValue("split").isEmpty) {
            splitImage = Nullable(extractImage(img.get, region, outputDirFile, 0))
            if (region.width != region.originalWidth || region.height != region.originalHeight) {
              val originalImg =
                new BufferedImage(region.originalWidth, region.originalHeight, img.get.getType())
              val g2 = originalImg.createGraphics()
              g2.drawImage(
                splitImage.get,
                region.offsetX.toInt,
                (region.originalHeight - region.height - region.offsetY).toInt,
                null // @nowarn("msg=deprecated") AWT observer null
              )
              g2.dispose()
              splitImage = Nullable(originalImg)
            }
            extension = Nullable(OUTPUT_TYPE)
          } else {
            splitImage = Nullable(extractNinePatch(img.get, region, outputDirFile))
            extension = Nullable(String.format("9.%s", OUTPUT_TYPE))
          }

          // check if the parent directories of this image file exist and create them if not
          val imgOutput = new File(
            outputDirFile,
            String.format(
              "%s.%s",
              if (region.index == -1) region.name else region.name + "_" + region.index,
              extension.get
            )
          )
          val imgDir = imgOutput.getParentFile()
          if (!imgDir.exists()) {
            if (!quiet) System.out.println(String.format("Creating directory: %s", imgDir.getPath()))
            imgDir.mkdirs()
          }

          // save the image
          try
            ImageIO.write(splitImage.get, OUTPUT_TYPE, imgOutput)
          catch {
            case e: Exception =>
              printExceptionAndExit(e)
          }
        }
      }
    }
  }

  /** Extract an image from a texture atlas.
    * @param page
    *   The image file related to the page the region is in
    * @param region
    *   The region to extract
    * @param outputDirFile
    *   The output directory
    * @param padding
    *   padding (in pixels) to apply to the image
    * @return
    *   The extracted image
    */
  private def extractImage(
    page:          BufferedImage,
    region:        TextureAtlasData#Region,
    outputDirFile: File,
    padding:       Int
  ): BufferedImage = {
    val splitImage: BufferedImage =
      // get the needed part of the page and rotate if needed
      if (region.rotate) {
        val srcImage = page.getSubimage(region.left, region.top, region.height, region.width)
        val rotated  = new BufferedImage(region.width, region.height, page.getType())

        val transform = new AffineTransform()
        transform.rotate(Math.toRadians(90.0))
        transform.translate(0, -region.width)
        val op = new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR)
        op.filter(srcImage, rotated)
        rotated
      } else {
        page.getSubimage(region.left, region.top, region.width, region.height)
      }

    // draw the image to a bigger one if padding is needed
    if (padding > 0) {
      val paddedImage = new BufferedImage(
        splitImage.getWidth() + padding * 2,
        splitImage.getHeight() + padding * 2,
        page.getType()
      )
      val g2 = paddedImage.createGraphics()
      g2.drawImage(splitImage, padding, padding, null) // @nowarn("msg=deprecated") AWT observer null
      g2.dispose()
      paddedImage
    } else {
      splitImage
    }
  }

  /** Extract a ninepatch from a texture atlas, according to the android specification.
    * @see
    *   <a href="http://developer.android.com/guide/topics/graphics/2d-graphics.html#nine-patch">ninepatch specification</a>
    * @param page
    *   The image file related to the page the region is in
    * @param region
    *   The region to extract
    */
  private def extractNinePatch(
    page:          BufferedImage,
    region:        TextureAtlasData#Region,
    outputDirFile: File
  ): BufferedImage = {
    val splitImage = extractImage(page, region, outputDirFile, NINEPATCH_PADDING)
    val g2         = splitImage.createGraphics()
    g2.setColor(Color.BLACK)

    // Draw the four lines to save the ninepatch's padding and splits
    val splits = region.findValue("split").get
    val startX = splits(0) + NINEPATCH_PADDING
    val endX   = region.width - splits(1) + NINEPATCH_PADDING - 1
    val startY = splits(2) + NINEPATCH_PADDING
    val endY   = region.height - splits(3) + NINEPATCH_PADDING - 1
    if (endX >= startX) g2.drawLine(startX, 0, endX, 0)
    if (endY >= startY) g2.drawLine(0, startY, 0, endY)
    val pads = region.findValue("pad")
    pads.foreach { p =>
      val padStartX = p(0) + NINEPATCH_PADDING
      val padEndX   = region.width - p(1) + NINEPATCH_PADDING - 1
      val padStartY = p(2) + NINEPATCH_PADDING
      val padEndY   = region.height - p(3) + NINEPATCH_PADDING - 1
      g2.drawLine(padStartX, splitImage.getHeight() - 1, padEndX, splitImage.getHeight() - 1)
      g2.drawLine(splitImage.getWidth() - 1, padStartY, splitImage.getWidth() - 1, padEndY)
    }
    g2.dispose()

    splitImage
  }

  private def printExceptionAndExit(e: Exception): Unit = {
    e.printStackTrace()
    System.exit(1)
  }

  def setQuiet(quiet: Boolean): Unit =
    this.quiet = quiet
}

object TextureUnpacker {
  private val DEFAULT_OUTPUT_PATH  = "output"
  private val NINEPATCH_PADDING    = 1
  private val OUTPUT_TYPE          = "png"
  private val HELP                 = "Usage: atlasFile [imageDir] [outputDir]"
  private val ATLAS_FILE_EXTENSION = ".atlas"

  def main(args: Array[String]): Unit = {
    val unpacker = TextureUnpacker()

    var atlasFile: Nullable[String] = Nullable.empty
    var imageDir:  Nullable[String] = Nullable.empty
    var outputDir: Nullable[String] = Nullable.empty

    // parse the arguments and display the help text if there is a problem with the command line arguments
    unpacker.parseArguments(args) match {
      case 0 =>
        System.out.println(HELP)
      case 3 =>
        outputDir = Nullable(args(2))
        imageDir = Nullable(args(1))
        atlasFile = Nullable(args(0))
      case 2 =>
        imageDir = Nullable(args(1))
        atlasFile = Nullable(args(0))
      case 1 =>
        atlasFile = Nullable(args(0))
      case _ =>
    }

    if (atlasFile.isDefined) {
      val atlasFileHandle = new File(atlasFile.get).getAbsoluteFile()
      if (!atlasFileHandle.exists()) throw new RuntimeException("Atlas file not found: " + atlasFileHandle.getAbsolutePath())
      val atlasParentPath = atlasFileHandle.getParentFile().getAbsolutePath()

      // Set the directory variables to a default when they weren't given in the variables
      if (imageDir.isEmpty) imageDir = Nullable(atlasParentPath)
      if (outputDir.isEmpty) outputDir = Nullable(new File(atlasParentPath, DEFAULT_OUTPUT_PATH).getAbsolutePath())

      // Opens the atlas file from the specified filename
      val atlas = TextureAtlasData(
        FileHandle(new File(atlasFile.get), FileType.Absolute),
        FileHandle(new File(imageDir.get), FileType.Absolute),
        false
      )
      unpacker.splitAtlas(atlas, outputDir.get)
    }
  }
}
