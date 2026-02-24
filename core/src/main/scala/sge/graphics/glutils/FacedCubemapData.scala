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

  protected val data: Array[TextureData] = new Array[TextureData](6)

  /** Construct a Cubemap with the specified texture files for the sides, optionally generating mipmaps. */
  def this(positiveX: FileHandle, negativeX: FileHandle, positiveY: FileHandle, negativeY: FileHandle, positiveZ: FileHandle, negativeZ: FileHandle)(using sge: Sge) = {
    this()
    data(0) = TextureData.Factory.loadFromFile(positiveX, false)
    data(1) = TextureData.Factory.loadFromFile(negativeX, false)
    data(2) = TextureData.Factory.loadFromFile(positiveY, false)
    data(3) = TextureData.Factory.loadFromFile(negativeY, false)
    data(4) = TextureData.Factory.loadFromFile(positiveZ, false)
    data(5) = TextureData.Factory.loadFromFile(negativeZ, false)
  }

  /** Construct a Cubemap with the specified texture files for the sides, optionally generating mipmaps. */
  def this(positiveX: FileHandle, negativeX: FileHandle, positiveY: FileHandle, negativeY: FileHandle, positiveZ: FileHandle, negativeZ: FileHandle, useMipMaps: Boolean)(using sge: Sge) = {
    this()
    data(0) = TextureData.Factory.loadFromFile(positiveX, useMipMaps)
    data(1) = TextureData.Factory.loadFromFile(negativeX, useMipMaps)
    data(2) = TextureData.Factory.loadFromFile(positiveY, useMipMaps)
    data(3) = TextureData.Factory.loadFromFile(negativeY, useMipMaps)
    data(4) = TextureData.Factory.loadFromFile(positiveZ, useMipMaps)
    data(5) = TextureData.Factory.loadFromFile(negativeZ, useMipMaps)
  }

  /** Construct a Cubemap with the specified {@link Pixmap}s for the sides, does not generate mipmaps. */
  def this(positiveX: Pixmap, negativeX: Pixmap, positiveY: Pixmap, negativeY: Pixmap, positiveZ: Pixmap, negativeZ: Pixmap)(using sge: Sge) = {
    this()
    data(0) = if (positiveX == null) null else new PixmapTextureData(positiveX, null, false, false)
    data(1) = if (negativeX == null) null else new PixmapTextureData(negativeX, null, false, false)
    data(2) = if (positiveY == null) null else new PixmapTextureData(positiveY, null, false, false)
    data(3) = if (negativeY == null) null else new PixmapTextureData(negativeY, null, false, false)
    data(4) = if (positiveZ == null) null else new PixmapTextureData(positiveZ, null, false, false)
    data(5) = if (negativeZ == null) null else new PixmapTextureData(negativeZ, null, false, false)
  }

  /** Construct a Cubemap with the specified {@link Pixmap}s for the sides, optionally generating mipmaps. */
  def this(positiveX: Pixmap, negativeX: Pixmap, positiveY: Pixmap, negativeY: Pixmap, positiveZ: Pixmap, negativeZ: Pixmap, useMipMaps: Boolean)(using sge: Sge) = {
    this()
    data(0) = if (positiveX == null) null else new PixmapTextureData(positiveX, null, useMipMaps, false)
    data(1) = if (negativeX == null) null else new PixmapTextureData(negativeX, null, useMipMaps, false)
    data(2) = if (positiveY == null) null else new PixmapTextureData(positiveY, null, useMipMaps, false)
    data(3) = if (negativeY == null) null else new PixmapTextureData(negativeY, null, useMipMaps, false)
    data(4) = if (positiveZ == null) null else new PixmapTextureData(positiveZ, null, useMipMaps, false)
    data(5) = if (negativeZ == null) null else new PixmapTextureData(negativeZ, null, useMipMaps, false)
  }

  /** Construct a Cubemap with {@link Pixmap}s for each side of the specified size. */
  def this(width: Int, height: Int, depth: Int, format: Format)(using sge: Sge) = {
    this()
    data(0) = new PixmapTextureData(new Pixmap(depth, height, format), null, false, true)
    data(1) = new PixmapTextureData(new Pixmap(depth, height, format), null, false, true)
    data(2) = new PixmapTextureData(new Pixmap(width, depth, format), null, false, true)
    data(3) = new PixmapTextureData(new Pixmap(width, depth, format), null, false, true)
    data(4) = new PixmapTextureData(new Pixmap(width, height, format), null, false, true)
    data(5) = new PixmapTextureData(new Pixmap(width, height, format), null, false, true)
  }

  /** Construct a Cubemap with the specified {@link TextureData}'s for the sides */
  def this(positiveX: TextureData, negativeX: TextureData, positiveY: TextureData, negativeY: TextureData, positiveZ: TextureData, negativeZ: TextureData)(using sge: Sge) = {
    this()
    data(0) = positiveX
    data(1) = negativeX
    data(2) = positiveY
    data(3) = negativeY
    data(4) = positiveZ
    data(5) = negativeZ
  }

  override def isManaged: Boolean =
    data.forall(d => d != null && d.isManaged)

  /** Loads the texture specified using the {@link FileHandle} and sets it to specified side, overwriting any previous data set to that side. Note that you need to reload through
    * {@link Cubemap#load(CubemapData)} any cubemap using this data for the change to be taken in account.
    * @param side
    *   The {@link CubemapSide}
    * @param file
    *   The texture {@link FileHandle}
    */
  def load(side: CubemapSide, file: FileHandle)(using sge: Sge): Unit =
    data(side.index) = TextureData.Factory.loadFromFile(file, false)

  /** Sets the specified side of this cubemap to the specified {@link Pixmap} , overwriting any previous data set to that side. Note that you need to reload through {@link Cubemap#load(CubemapData)}
    * any cubemap using this data for the change to be taken in account.
    * @param side
    *   The {@link CubemapSide}
    * @param pixmap
    *   The {@link Pixmap}
    */
  def load(side: CubemapSide, pixmap: Pixmap): Unit =
    data(side.index) = if (pixmap == null) null else new PixmapTextureData(pixmap, null, false, false)

  /** @return True if all sides of this cubemap are set, false otherwise. */
  def isComplete(): Boolean =
    data.forall(_ != null)

  /** @return The {@link TextureData} for the specified side, can be null if the cubemap is incomplete. */
  def getTextureData(side: CubemapSide): TextureData =
    data(side.index)

  override def getWidth: Int = {
    var width = 0
    if (data(CubemapSide.PositiveZ.index) != null) {
      val tmp = data(CubemapSide.PositiveZ.index).getWidth
      if (tmp > width) width = tmp
    }
    if (data(CubemapSide.NegativeZ.index) != null) {
      val tmp = data(CubemapSide.NegativeZ.index).getWidth
      if (tmp > width) width = tmp
    }
    if (data(CubemapSide.PositiveY.index) != null) {
      val tmp = data(CubemapSide.PositiveY.index).getWidth
      if (tmp > width) width = tmp
    }
    if (data(CubemapSide.NegativeY.index) != null) {
      val tmp = data(CubemapSide.NegativeY.index).getWidth
      if (tmp > width) width = tmp
    }
    width
  }

  override def getHeight: Int = {
    var height = 0
    if (data(CubemapSide.PositiveZ.index) != null) {
      val tmp = data(CubemapSide.PositiveZ.index).getHeight
      if (tmp > height) height = tmp
    }
    if (data(CubemapSide.NegativeZ.index) != null) {
      val tmp = data(CubemapSide.NegativeZ.index).getHeight
      if (tmp > height) height = tmp
    }
    if (data(CubemapSide.PositiveX.index) != null) {
      val tmp = data(CubemapSide.PositiveX.index).getHeight
      if (tmp > height) height = tmp
    }
    if (data(CubemapSide.NegativeX.index) != null) {
      val tmp = data(CubemapSide.NegativeX.index).getHeight
      if (tmp > height) height = tmp
    }
    height
  }

  override def isPrepared: Boolean = false

  override def prepare(): Unit = {
    if (!isComplete()) throw SgeError.GraphicsError("You need to complete your cubemap data before using it")
    for (i <- data.indices)
      if (data(i) != null && !data(i).isPrepared) data(i).prepare()
  }

  override def consumeCubemapData(): Unit =
    for (i <- data.indices)
      if (data(i).getType() == TextureDataType.Custom) {
        data(i).consumeCustomData(GL20.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i)
      } else {
        var pixmap        = data(i).consumePixmap()
        var disposePixmap = data(i).disposePixmap
        if (data(i).getFormat != pixmap.getFormat()) {
          val tmp = new Pixmap(pixmap.getWidth(), pixmap.getHeight(), data(i).getFormat)
          tmp.setBlending(Blending.None)
          tmp.drawPixmap(pixmap, 0, 0, 0, 0, pixmap.getWidth(), pixmap.getHeight())
          if (data(i).disposePixmap) pixmap.close()
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
