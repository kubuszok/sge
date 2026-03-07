/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/glutils/MipMapGenerator.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Idiom: split packages, boundary/break
 *   Convention: (using Sge) context; Nullable
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics
package glutils

import sge.Application
import sge.graphics.Pixmap
import sge.graphics.Pixmap.Blending
import sge.utils.SgeError

object MipMapGenerator {

  private var useHWMipMap: Boolean = true

  def setUseHardwareMipMap(useHWMipMap: Boolean): Unit =
    MipMapGenerator.useHWMipMap = useHWMipMap

  /** Sets the image data of the {@link Texture} based on the {@link Pixmap}. The texture must be bound for this to work.
    * @param pixmap
    *   the Pixmap
    */
  def generateMipMap(pixmap: Pixmap, textureWidth: Int, textureHeight: Int)(using Sge): Unit =
    generateMipMap(TextureTarget.Texture2D, pixmap, textureWidth, textureHeight)

  /** Sets the image data of the {@link Texture} based on the {@link Pixmap}. The texture must be bound for this to work. */
  def generateMipMap(target: TextureTarget, pixmap: Pixmap, textureWidth: Int, textureHeight: Int)(using Sge): Unit =
    if (!useHWMipMap) {
      generateMipMapCPU(target, pixmap, textureWidth, textureHeight)
    } else {
      val appType = Sge().application.getType()
      if (
        appType == Application.ApplicationType.Android || appType == Application.ApplicationType.WebGL
        || appType == Application.ApplicationType.iOS
      ) {
        generateMipMapGLES20(target, pixmap)
      } else {
        generateMipMapDesktop(target, pixmap, textureWidth, textureHeight)
      }
    }

  private def generateMipMapGLES20(target: TextureTarget, pixmap: Pixmap)(using Sge): Unit = {
    Sge().graphics.gl.glTexImage2D(
      target,
      0,
      pixmap.getGLInternalFormat(),
      pixmap.getWidth(),
      pixmap.getHeight(),
      0,
      PixelFormat(pixmap.getGLFormat()),
      DataType(pixmap.getGLType()),
      pixmap.getPixels()
    )
    Sge().graphics.gl20.glGenerateMipmap(target)
  }

  private def generateMipMapDesktop(target: TextureTarget, pixmap: Pixmap, textureWidth: Int, textureHeight: Int)(using Sge): Unit =
    if (
      Sge().graphics.supportsExtension("GL_ARB_framebuffer_object")
      || Sge().graphics.supportsExtension("GL_EXT_framebuffer_object")
      || Sge().graphics.gl30.isDefined
    ) {
      Sge().graphics.gl.glTexImage2D(
        target,
        0,
        pixmap.getGLInternalFormat(),
        pixmap.getWidth(),
        pixmap.getHeight(),
        0,
        PixelFormat(pixmap.getGLFormat()),
        DataType(pixmap.getGLType()),
        pixmap.getPixels()
      )
      Sge().graphics.gl20.glGenerateMipmap(target)
    } else {
      generateMipMapCPU(target, pixmap, textureWidth, textureHeight)
    }

  private def generateMipMapCPU(target: TextureTarget, pixmap: Pixmap, textureWidth: Int, textureHeight: Int)(using Sge): Unit = {
    Sge().graphics.gl.glTexImage2D(
      target,
      0,
      pixmap.getGLInternalFormat(),
      pixmap.getWidth(),
      pixmap.getHeight(),
      0,
      PixelFormat(pixmap.getGLFormat()),
      DataType(pixmap.getGLType()),
      pixmap.getPixels()
    )
    if (textureWidth != textureHeight)
      throw SgeError.GraphicsError("texture width and height must be square when using mipmapping.")

    var currentPixmap = pixmap
    var width         = pixmap.getWidth() / 2
    var height        = pixmap.getHeight() / 2
    var level         = 1

    scala.util.boundary {
      while (true) {
        val tmp = Pixmap(width.toInt, height.toInt, pixmap.getFormat())
        tmp.setBlending(Blending.None)
        tmp.drawPixmap(
          currentPixmap,
          Pixels.zero,
          Pixels.zero,
          currentPixmap.getWidth(),
          currentPixmap.getHeight(),
          Pixels.zero,
          Pixels.zero,
          width,
          height
        )
        if (level > 1) currentPixmap.close()
        currentPixmap = tmp

        Sge().graphics.gl.glTexImage2D(
          target,
          level,
          currentPixmap.getGLInternalFormat(),
          currentPixmap.getWidth(),
          currentPixmap.getHeight(),
          0,
          PixelFormat(currentPixmap.getGLFormat()),
          DataType(currentPixmap.getGLType()),
          currentPixmap.getPixels()
        )

        width = currentPixmap.getWidth() / 2
        height = currentPixmap.getHeight() / 2

        // Break when we have exhausted all levels
        if (width == Pixels.zero && height == Pixels.zero) scala.util.boundary.break(())
        if (width == Pixels.zero) width = Pixels(1)
        if (height == Pixels.zero) height = Pixels(1)

        level += 1
      }
    }
  }
}
