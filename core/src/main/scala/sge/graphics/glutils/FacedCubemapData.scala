/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/glutils/FacedCubemapData.java
 * Original authors: Vincent Nousquet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package glutils

import sge.files.FileHandle
import sge.graphics.Cubemap.CubemapSide
import sge.graphics.Pixmap.Blending
import sge.graphics.Pixmap.Format
import sge.graphics.{ CubemapData, GL20, Pixmap, TextureData }
import sge.graphics.TextureData.TextureDataType
import sge.utils.{ Nullable, SgeError }
import sge.Sge

/** A FacedCubemapData holds a cubemap data definition based on a {@link TextureData} per face.
  *
  * @author
  *   Vincent Nousquet
  */
class FacedCubemapData(using sge: Sge) extends CubemapData {

  protected val data: Array[Nullable[TextureData]] = Array.fill(6)(Nullable.empty)

  /** Construct a Cubemap with the specified texture files for the sides, optionally generating mipmaps. */
  def this(positiveX: FileHandle, negativeX: FileHandle, positiveY: FileHandle, negativeY: FileHandle, positiveZ: FileHandle, negativeZ: FileHandle)(using sge: Sge) = {
    this()
    data(0) = Nullable(TextureData.Factory.loadFromFile(positiveX, false))
    data(1) = Nullable(TextureData.Factory.loadFromFile(negativeX, false))
    data(2) = Nullable(TextureData.Factory.loadFromFile(positiveY, false))
    data(3) = Nullable(TextureData.Factory.loadFromFile(negativeY, false))
    data(4) = Nullable(TextureData.Factory.loadFromFile(positiveZ, false))
    data(5) = Nullable(TextureData.Factory.loadFromFile(negativeZ, false))
  }

  /** Construct a Cubemap with the specified texture files for the sides, optionally generating mipmaps. */
  def this(positiveX: FileHandle, negativeX: FileHandle, positiveY: FileHandle, negativeY: FileHandle, positiveZ: FileHandle, negativeZ: FileHandle, useMipMaps: Boolean)(using sge: Sge) = {
    this()
    data(0) = Nullable(TextureData.Factory.loadFromFile(positiveX, useMipMaps))
    data(1) = Nullable(TextureData.Factory.loadFromFile(negativeX, useMipMaps))
    data(2) = Nullable(TextureData.Factory.loadFromFile(positiveY, useMipMaps))
    data(3) = Nullable(TextureData.Factory.loadFromFile(negativeY, useMipMaps))
    data(4) = Nullable(TextureData.Factory.loadFromFile(positiveZ, useMipMaps))
    data(5) = Nullable(TextureData.Factory.loadFromFile(negativeZ, useMipMaps))
  }

  /** Construct a Cubemap with the specified {@link Pixmap}s for the sides, does not generate mipmaps. */
  def this(positiveX: Nullable[Pixmap], negativeX: Nullable[Pixmap], positiveY: Nullable[Pixmap], negativeY: Nullable[Pixmap], positiveZ: Nullable[Pixmap], negativeZ: Nullable[Pixmap])(using
    sge: Sge
  ) = {
    this()
    data(0) = positiveX.fold(Nullable.empty[TextureData])(px => Nullable(new PixmapTextureData(px, null, false, false)))
    data(1) = negativeX.fold(Nullable.empty[TextureData])(px => Nullable(new PixmapTextureData(px, null, false, false)))
    data(2) = positiveY.fold(Nullable.empty[TextureData])(px => Nullable(new PixmapTextureData(px, null, false, false)))
    data(3) = negativeY.fold(Nullable.empty[TextureData])(px => Nullable(new PixmapTextureData(px, null, false, false)))
    data(4) = positiveZ.fold(Nullable.empty[TextureData])(px => Nullable(new PixmapTextureData(px, null, false, false)))
    data(5) = negativeZ.fold(Nullable.empty[TextureData])(px => Nullable(new PixmapTextureData(px, null, false, false)))
  }

  /** Construct a Cubemap with the specified {@link Pixmap}s for the sides, optionally generating mipmaps. */
  def this(
    positiveX:  Nullable[Pixmap],
    negativeX:  Nullable[Pixmap],
    positiveY:  Nullable[Pixmap],
    negativeY:  Nullable[Pixmap],
    positiveZ:  Nullable[Pixmap],
    negativeZ:  Nullable[Pixmap],
    useMipMaps: Boolean
  )(using sge: Sge) = {
    this()
    data(0) = positiveX.fold(Nullable.empty[TextureData])(px => Nullable(new PixmapTextureData(px, null, useMipMaps, false)))
    data(1) = negativeX.fold(Nullable.empty[TextureData])(px => Nullable(new PixmapTextureData(px, null, useMipMaps, false)))
    data(2) = positiveY.fold(Nullable.empty[TextureData])(px => Nullable(new PixmapTextureData(px, null, useMipMaps, false)))
    data(3) = negativeY.fold(Nullable.empty[TextureData])(px => Nullable(new PixmapTextureData(px, null, useMipMaps, false)))
    data(4) = positiveZ.fold(Nullable.empty[TextureData])(px => Nullable(new PixmapTextureData(px, null, useMipMaps, false)))
    data(5) = negativeZ.fold(Nullable.empty[TextureData])(px => Nullable(new PixmapTextureData(px, null, useMipMaps, false)))
  }

  /** Construct a Cubemap with {@link Pixmap}s for each side of the specified size. */
  def this(width: Int, height: Int, depth: Int, format: Format)(using sge: Sge) = {
    this()
    data(0) = Nullable(new PixmapTextureData(new Pixmap(depth, height, format), null, false, true))
    data(1) = Nullable(new PixmapTextureData(new Pixmap(depth, height, format), null, false, true))
    data(2) = Nullable(new PixmapTextureData(new Pixmap(width, depth, format), null, false, true))
    data(3) = Nullable(new PixmapTextureData(new Pixmap(width, depth, format), null, false, true))
    data(4) = Nullable(new PixmapTextureData(new Pixmap(width, height, format), null, false, true))
    data(5) = Nullable(new PixmapTextureData(new Pixmap(width, height, format), null, false, true))
  }

  /** Construct a Cubemap with the specified {@link TextureData}'s for the sides */
  def this(positiveX: TextureData, negativeX: TextureData, positiveY: TextureData, negativeY: TextureData, positiveZ: TextureData, negativeZ: TextureData)(using sge: Sge) = {
    this()
    data(0) = Nullable(positiveX)
    data(1) = Nullable(negativeX)
    data(2) = Nullable(positiveY)
    data(3) = Nullable(negativeY)
    data(4) = Nullable(positiveZ)
    data(5) = Nullable(negativeZ)
  }

  override def isManaged: Boolean =
    data.forall(d => d.fold(false)(_.isManaged))

  /** Loads the texture specified using the {@link FileHandle} and sets it to specified side, overwriting any previous data set to that side. Note that you need to reload through
    * {@link Cubemap#load(CubemapData)} any cubemap using this data for the change to be taken in account.
    * @param side
    *   The {@link CubemapSide}
    * @param file
    *   The texture {@link FileHandle}
    */
  def load(side: CubemapSide, file: FileHandle)(using sge: Sge): Unit =
    data(side.index) = Nullable(TextureData.Factory.loadFromFile(file, false))

  /** Sets the specified side of this cubemap to the specified {@link Pixmap} , overwriting any previous data set to that side. Note that you need to reload through {@link Cubemap#load(CubemapData)}
    * any cubemap using this data for the change to be taken in account.
    * @param side
    *   The {@link CubemapSide}
    * @param pixmap
    *   The {@link Pixmap}
    */
  def load(side: CubemapSide, pixmap: Nullable[Pixmap]): Unit =
    data(side.index) = pixmap.fold(Nullable.empty[TextureData])(px => Nullable(new PixmapTextureData(px, null, false, false)))

  /** @return True if all sides of this cubemap are set, false otherwise. */
  def isComplete(): Boolean =
    data.forall(_.isDefined)

  /** @return The {@link TextureData} for the specified side, can be null if the cubemap is incomplete. */
  def getTextureData(side: CubemapSide): Nullable[TextureData] =
    data(side.index)

  override def getWidth: Int = {
    var width = 0
    data(CubemapSide.PositiveZ.index).foreach { d =>
      val tmp = d.getWidth
      if (tmp > width) width = tmp
    }
    data(CubemapSide.NegativeZ.index).foreach { d =>
      val tmp = d.getWidth
      if (tmp > width) width = tmp
    }
    data(CubemapSide.PositiveY.index).foreach { d =>
      val tmp = d.getWidth
      if (tmp > width) width = tmp
    }
    data(CubemapSide.NegativeY.index).foreach { d =>
      val tmp = d.getWidth
      if (tmp > width) width = tmp
    }
    width
  }

  override def getHeight: Int = {
    var height = 0
    data(CubemapSide.PositiveZ.index).foreach { d =>
      val tmp = d.getHeight
      if (tmp > height) height = tmp
    }
    data(CubemapSide.NegativeZ.index).foreach { d =>
      val tmp = d.getHeight
      if (tmp > height) height = tmp
    }
    data(CubemapSide.PositiveX.index).foreach { d =>
      val tmp = d.getHeight
      if (tmp > height) height = tmp
    }
    data(CubemapSide.NegativeX.index).foreach { d =>
      val tmp = d.getHeight
      if (tmp > height) height = tmp
    }
    height
  }

  override def isPrepared: Boolean = false

  override def prepare(): Unit = {
    if (!isComplete()) throw SgeError.GraphicsError("You need to complete your cubemap data before using it")
    for (i <- data.indices)
      data(i).foreach(d => if (!d.isPrepared) d.prepare())
  }

  override def consumeCubemapData(): Unit =
    for (i <- data.indices)
      data(i).foreach { di =>
        if (di.getType() == TextureDataType.Custom) {
          di.consumeCustomData(GL20.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i)
        } else {
          var pixmap        = di.consumePixmap()
          var disposePixmap = di.disposePixmap
          if (di.getFormat != pixmap.getFormat()) {
            val tmp = new Pixmap(pixmap.getWidth(), pixmap.getHeight(), di.getFormat)
            tmp.setBlending(Blending.None)
            tmp.drawPixmap(pixmap, 0, 0, 0, 0, pixmap.getWidth(), pixmap.getHeight())
            if (di.disposePixmap) pixmap.close()
            pixmap = tmp
            disposePixmap = true
          }
          sge.graphics.gl.glPixelStorei(GL20.GL_UNPACK_ALIGNMENT, 1)
          sge.graphics.gl.glTexImage2D(
            GL20.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i,
            0,
            pixmap.getGLInternalFormat(),
            pixmap.getWidth(),
            pixmap.getHeight(),
            0,
            pixmap.getGLFormat(),
            pixmap.getGLType(),
            pixmap.getPixels()
          )
          if (disposePixmap) pixmap.close()
        }
      }
}
