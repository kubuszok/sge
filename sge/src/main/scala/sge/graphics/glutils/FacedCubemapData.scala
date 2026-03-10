/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/glutils/FacedCubemapData.java
 * Original authors: Vincent Nousquet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: data array is Array[Nullable[TextureData]] instead of nullable TextureData[]
 *   Idiom: split packages
 *   Convention: Nullable.empty passed as Format to PixmapTextureData (14 call sites fixed, no raw null remains)
 *   Convention: typed GL enums — TextureTarget, PixelFormat, DataType for consumeCubemapData GL calls
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
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
class FacedCubemapData(using Sge) extends CubemapData {

  protected val data: Array[Nullable[TextureData]] = Array.fill(6)(Nullable.empty)

  /** Construct a Cubemap with the specified texture files for the sides, optionally generating mipmaps. */
  def this(positiveX: FileHandle, negativeX: FileHandle, positiveY: FileHandle, negativeY: FileHandle, positiveZ: FileHandle, negativeZ: FileHandle)(using Sge) = {
    this()
    data(0) = Nullable(TextureData.Factory.loadFromFile(positiveX, false))
    data(1) = Nullable(TextureData.Factory.loadFromFile(negativeX, false))
    data(2) = Nullable(TextureData.Factory.loadFromFile(positiveY, false))
    data(3) = Nullable(TextureData.Factory.loadFromFile(negativeY, false))
    data(4) = Nullable(TextureData.Factory.loadFromFile(positiveZ, false))
    data(5) = Nullable(TextureData.Factory.loadFromFile(negativeZ, false))
  }

  /** Construct a Cubemap with the specified texture files for the sides, optionally generating mipmaps. */
  def this(positiveX: FileHandle, negativeX: FileHandle, positiveY: FileHandle, negativeY: FileHandle, positiveZ: FileHandle, negativeZ: FileHandle, useMipMaps: Boolean)(using Sge) = {
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
    data(0) = positiveX.map(px => PixmapTextureData(px, Nullable.empty, false, false, false))
    data(1) = negativeX.map(px => PixmapTextureData(px, Nullable.empty, false, false, false))
    data(2) = positiveY.map(px => PixmapTextureData(px, Nullable.empty, false, false, false))
    data(3) = negativeY.map(px => PixmapTextureData(px, Nullable.empty, false, false, false))
    data(4) = positiveZ.map(px => PixmapTextureData(px, Nullable.empty, false, false, false))
    data(5) = negativeZ.map(px => PixmapTextureData(px, Nullable.empty, false, false, false))
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
  )(using Sge) = {
    this()
    data(0) = positiveX.map(px => PixmapTextureData(px, Nullable.empty, useMipMaps, false, false))
    data(1) = negativeX.map(px => PixmapTextureData(px, Nullable.empty, useMipMaps, false, false))
    data(2) = positiveY.map(px => PixmapTextureData(px, Nullable.empty, useMipMaps, false, false))
    data(3) = negativeY.map(px => PixmapTextureData(px, Nullable.empty, useMipMaps, false, false))
    data(4) = positiveZ.map(px => PixmapTextureData(px, Nullable.empty, useMipMaps, false, false))
    data(5) = negativeZ.map(px => PixmapTextureData(px, Nullable.empty, useMipMaps, false, false))
  }

  /** Construct a Cubemap with {@link Pixmap}s for each side of the specified size. */
  def this(width: Int, height: Int, depth: Int, format: Format)(using Sge) = {
    this()
    data(0) = Nullable(PixmapTextureData(Pixmap(depth, height, format), Nullable.empty, false, true, false))
    data(1) = Nullable(PixmapTextureData(Pixmap(depth, height, format), Nullable.empty, false, true, false))
    data(2) = Nullable(PixmapTextureData(Pixmap(width, depth, format), Nullable.empty, false, true, false))
    data(3) = Nullable(PixmapTextureData(Pixmap(width, depth, format), Nullable.empty, false, true, false))
    data(4) = Nullable(PixmapTextureData(Pixmap(width, height, format), Nullable.empty, false, true, false))
    data(5) = Nullable(PixmapTextureData(Pixmap(width, height, format), Nullable.empty, false, true, false))
  }

  /** Construct a Cubemap with the specified {@link TextureData}'s for the sides */
  def this(positiveX: TextureData, negativeX: TextureData, positiveY: TextureData, negativeY: TextureData, positiveZ: TextureData, negativeZ: TextureData)(using Sge) = {
    this()
    data(0) = Nullable(positiveX)
    data(1) = Nullable(negativeX)
    data(2) = Nullable(positiveY)
    data(3) = Nullable(negativeY)
    data(4) = Nullable(positiveZ)
    data(5) = Nullable(negativeZ)
  }

  override def isManaged: Boolean =
    data.forall(d => d.exists(_.isManaged))

  /** Loads the texture specified using the {@link FileHandle} and sets it to specified side, overwriting any previous data set to that side. Note that you need to reload through
    * {@link Cubemap#load(CubemapData)} any cubemap using this data for the change to be taken in account.
    * @param side
    *   The {@link CubemapSide}
    * @param file
    *   The texture {@link FileHandle}
    */
  def load(side: CubemapSide, file: FileHandle): Unit =
    data(side.index) = Nullable(TextureData.Factory.loadFromFile(file, false))

  /** Sets the specified side of this cubemap to the specified {@link Pixmap} , overwriting any previous data set to that side. Note that you need to reload through {@link Cubemap#load(CubemapData)}
    * any cubemap using this data for the change to be taken in account.
    * @param side
    *   The {@link CubemapSide}
    * @param pixmap
    *   The {@link Pixmap}
    */
  def load(side: CubemapSide, pixmap: Nullable[Pixmap]): Unit =
    data(side.index) = pixmap.map(px => PixmapTextureData(px, Nullable.empty, false, false, false))

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
          di.consumeCustomData(CubemapSide.values(i).glEnum)
        } else {
          var pixmap        = di.consumePixmap()
          var disposePixmap = di.disposePixmap
          if (di.getFormat != pixmap.getFormat()) {
            val tmp = Pixmap(pixmap.getWidth().toInt, pixmap.getHeight().toInt, di.getFormat)
            tmp.setBlending(Blending.None)
            tmp.drawPixmap(pixmap, Pixels.zero, Pixels.zero, Pixels.zero, Pixels.zero, pixmap.getWidth(), pixmap.getHeight())
            if (di.disposePixmap) pixmap.close()
            pixmap = tmp
            disposePixmap = true
          }
          Sge().graphics.gl.glPixelStorei(GL20.GL_UNPACK_ALIGNMENT, 1)
          Sge().graphics.gl.glTexImage2D(
            CubemapSide.values(i).glEnum,
            0,
            pixmap.getGLInternalFormat(),
            pixmap.getWidth(),
            pixmap.getHeight(),
            0,
            PixelFormat(pixmap.getGLFormat()),
            DataType(pixmap.getGLType()),
            pixmap.getPixels()
          )
          if (disposePixmap) pixmap.close()
        }
      }
}
