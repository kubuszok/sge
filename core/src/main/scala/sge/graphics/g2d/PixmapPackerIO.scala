/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g2d/PixmapPackerIO.java
 * Original authors: jshapcott
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g2d

import java.io.{ IOException, Writer }
import java.util.regex.Matcher
import sge.files.FileHandle
import sge.graphics.PixmapIO
import sge.graphics.Texture.TextureFilter
import sge.utils.SgeError

/** Saves PixmapPackers to files.
  * @author
  *   jshapcott
  */
class PixmapPackerIO {

  /** Saves the provided PixmapPacker to the provided file. The resulting file will use the standard TextureAtlas file format and can be loaded by TextureAtlas as if it had been created using
    * TexturePacker. Default {@link SaveParameters} will be used.
    *
    * @param file
    *   the file to which the atlas descriptor will be written, images will be written as siblings
    * @param packer
    *   the PixmapPacker to be written
    * @throws SgeError.FileWriteError
    *   if the atlas file can not be written
    */
  def save(file: FileHandle, packer: PixmapPacker): Unit =
    save(file, packer, new PixmapPackerIO.SaveParameters())

  /** Saves the provided PixmapPacker to the provided file. The resulting file will use the standard TextureAtlas file format and can be loaded by TextureAtlas as if it had been created using
    * TexturePacker.
    *
    * @param file
    *   the file to which the atlas descriptor will be written, images will be written as siblings
    * @param packer
    *   the PixmapPacker to be written
    * @param parameters
    *   the SaveParameters specifying how to save the PixmapPacker
    * @throws SgeError.FileWriteError
    *   if the atlas file can not be written
    */
  def save(file: FileHandle, packer: PixmapPacker, parameters: PixmapPackerIO.SaveParameters): Unit = {
    val writer = file.writer(false)
    var index  = 0

    try
      for (page <- packer.pages)
        if (page.rects.size > 0) {
          index += 1
          val pageFile = file.sibling(file.nameWithoutExtension() + "_" + index + parameters.format.getExtension())

          parameters.format match {
            case PixmapPackerIO.ImageFormat.CIM =>
              PixmapIO.writeCIM(pageFile, page.image)
            case PixmapPackerIO.ImageFormat.PNG =>
              PixmapIO.writePNG(pageFile, page.image)
          }

          writer.write("\n")
          writer.write(pageFile.name() + "\n")
          writer.write("size: " + page.image.getWidth() + "," + page.image.getHeight() + "\n")
          writer.write("format: " + packer.pageFormat.toString + "\n")
          writer.write("filter: " + parameters.minFilter.toString + "," + parameters.magFilter.toString + "\n")
          writer.write("repeat: none" + "\n")

          for (name <- page.rects.keys) {
            var imageIndex = -1
            var imageName  = name

            if (parameters.useIndexes) {
              val matcher = PixmapPacker.indexPattern.matcher(imageName)
              if (matcher.matches()) {
                imageName = matcher.group(1)
                imageIndex = Integer.parseInt(matcher.group(2))
              }
            }

            writer.write(imageName + "\n")
            val rect = page.rects(name)
            writer.write("  rotate: false" + "\n")
            writer.write("  xy: " + rect.getX() + "," + rect.getY() + "\n")
            writer.write("  size: " + rect.getWidth() + "," + rect.getHeight() + "\n")

            if (rect.splits != null) {
              writer.write(
                "  split: " + rect.splits(0) + ", " + rect.splits(1) + ", " + rect.splits(2) + ", " + rect.splits(3) + "\n"
              )
              if (rect.pads != null) {
                writer.write(
                  "  pad: " + rect.pads(0) + ", " + rect.pads(1) + ", " + rect.pads(2) + ", " + rect.pads(3) + "\n"
                )
              }
            }

            writer.write("  orig: " + rect.originalWidth + ", " + rect.originalHeight + "\n")
            writer.write("  offset: " + rect.offsetX + ", " + (rect.originalHeight - rect.getHeight() - rect.offsetY) + "\n")
            writer.write("  index: " + imageIndex + "\n")
          }
        }
    catch {
      case ex: IOException => throw SgeError.FileWriteError(file, "Error writing atlas file", Some(ex))
    } finally
      writer.close()
  }
}

object PixmapPackerIO {

  /** Image formats which can be used when saving a PixmapPacker. */
  enum ImageFormat(private val extension: String) {

    /** A simple compressed image format which is libgdx specific. */
    case CIM extends ImageFormat(".cim")

    /** A standard compressed image format which is not libgdx specific. */
    case PNG extends ImageFormat(".png")

    /** Returns the file extension for the image format. */
    def getExtension(): String = extension
  }

  /** Additional parameters which will be used when writing a PixmapPacker. */
  class SaveParameters {
    var format:     ImageFormat   = ImageFormat.PNG
    var minFilter:  TextureFilter = TextureFilter.Nearest
    var magFilter:  TextureFilter = TextureFilter.Nearest
    var useIndexes: Boolean       = false
  }
}
