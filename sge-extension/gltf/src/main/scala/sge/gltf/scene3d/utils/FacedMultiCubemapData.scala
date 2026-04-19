/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Original source: net/mgsx/gltf/scene3d/utils/FacedMultiCubemapData.java
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port for SGE
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 152
 * Covenant-baseline-methods: FacedMultiCubemapData,consumeCubemapData,data,getTextureData,gl,h,height,i,isComplete,isManaged,isPrepared,level,levels,nx,ny,nz,prepare,px,py,pz,result,this,w,width
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package gltf
package scene3d
package utils

import sge.{ Pixels, Sge }
import sge.files.FileHandle
import sge.graphics.{ Cubemap, CubemapData, DataType, GL20, PixelFormat, Pixmap, TextureData, TextureTarget }
import sge.graphics.glutils.PixmapTextureData
import sge.utils.SgeError

class FacedMultiCubemapData(
  protected val data: Array[TextureData],
  private val levels: Int
)(using Sge)
    extends CubemapData {

  def this(files: Array[FileHandle], levels: Int)(using Sge) = {
    this(new Array[TextureData](6 * levels), levels)
    var level = 0
    while (level < levels) {
      var face = 0
      while (face < 6) {
        val index = level * 6 + face
        data(index) = PixmapTextureData(Pixmap(files(index)), null, false, true) // @nowarn — null format = auto-detect
        face += 1
      }
      level += 1
    }
  }

  def this(pixmaps: Array[Pixmap], levels: Int)(using Sge) = {
    this(new Array[TextureData](6 * levels), levels)
    var level = 0
    while (level < levels) {
      var face = 0
      while (face < 6) {
        val index = level * 6 + face
        data(index) = PixmapTextureData(pixmaps(index), null, false, true) // @nowarn — null format = auto-detect
        face += 1
      }
      level += 1
    }
  }

  override def isManaged: Boolean = {
    var i      = 0
    var result = true
    while (i < data.length && result) {
      if (!data(i).isManaged) result = false
      i += 1
    }
    result
  }

  def isComplete: Boolean = {
    var i      = 0
    var result = true
    while (i < data.length && result) {
      if (data(i) == null) result = false
      i += 1
    }
    result
  }

  def getTextureData(side: Cubemap.CubemapSide): TextureData =
    data(side.index)

  override def width: Int = {
    var w  = 0
    val pz = Cubemap.CubemapSide.PositiveZ.index
    val nz = Cubemap.CubemapSide.NegativeZ.index
    val py = Cubemap.CubemapSide.PositiveY.index
    val ny = Cubemap.CubemapSide.NegativeY.index
    if (data(pz) != null && data(pz).width > w) w = data(pz).width // @nowarn
    if (data(nz) != null && data(nz).width > w) w = data(nz).width // @nowarn
    if (data(py) != null && data(py).width > w) w = data(py).width // @nowarn
    if (data(ny) != null && data(ny).width > w) w = data(ny).width // @nowarn
    w
  }

  override def height: Int = {
    var h  = 0
    val pz = Cubemap.CubemapSide.PositiveZ.index
    val nz = Cubemap.CubemapSide.NegativeZ.index
    val px = Cubemap.CubemapSide.PositiveX.index
    val nx = Cubemap.CubemapSide.NegativeX.index
    if (data(pz) != null && data(pz).height > h) h = data(pz).height // @nowarn
    if (data(nz) != null && data(nz).height > h) h = data(nz).height // @nowarn
    if (data(px) != null && data(px).height > h) h = data(px).height // @nowarn
    if (data(nx) != null && data(nx).height > h) h = data(nx).height // @nowarn
    h
  }

  override def isPrepared: Boolean = false

  override def prepare(): Unit = {
    if (!isComplete) throw SgeError.InvalidInput("You need to complete your cubemap data before using it")
    var i = 0
    while (i < data.length) {
      if (!data(i).isPrepared) data(i).prepare()
      i += 1
    }
  }

  override def consumeCubemapData(): Unit = {
    val gl    = Sge().graphics.gl
    var level = 0
    while (level < levels) {
      var i = 0
      while (i < 6) {
        val index = level * 6 + i
        if (data(index).dataType == TextureData.TextureDataType.Custom) {
          data(index).consumeCustomData(TextureTarget(GL20.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i))
        } else {
          var pixmap        = data(index).consumePixmap()
          var shouldDispose = data(index).disposePixmap
          if (data(index).getFormat != pixmap.format) {
            val tmp = Pixmap(pixmap.width.toInt, pixmap.height.toInt, data(index).getFormat)
            tmp.setBlending(Pixmap.Blending.None)
            tmp.drawPixmap(pixmap, Pixels.zero, Pixels.zero, Pixels.zero, Pixels.zero, pixmap.width, pixmap.height)
            if (data(index).disposePixmap) pixmap.close()
            pixmap = tmp
            shouldDispose = true
          }
          gl.glPixelStorei(GL20.GL_UNPACK_ALIGNMENT, 1)
          gl.glTexImage2D(
            TextureTarget(GL20.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i),
            level,
            pixmap.gLInternalFormat,
            pixmap.width,
            pixmap.height,
            0,
            PixelFormat(pixmap.gLFormat),
            DataType(pixmap.glType),
            pixmap.pixels
          )
          if (shouldDispose) pixmap.close()
        }
        i += 1
      }
      level += 1
    }
  }
}
