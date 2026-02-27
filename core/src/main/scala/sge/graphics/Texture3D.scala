/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/Texture3D.java
 * Original authors: mgsx
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics

import scala.collection.mutable
import sge.utils.{ DynamicArray, Nullable }

import sge.graphics.GLTexture
import sge.graphics.Texture.TextureWrap
import sge.graphics.glutils.CustomTexture3DData
import sge.utils.SgeError

/** Open GLES wrapper for Texture3D
  * @author
  *   mgsx
  */
class Texture3D(data: Texture3DData)(using sde: Sge) extends GLTexture(GL30.GL_TEXTURE_3D, TextureHandle(sde.graphics.gl.glGenTexture())) {

  private var textureData: Texture3DData = scala.compiletime.uninitialized
  protected var rWrap:     TextureWrap   = TextureWrap.ClampToEdge

  def this(width: Int, height: Int, depth: Int, glFormat: Int, glInternalFormat: Int, glType: Int)(using sde: Sge) =
    this(new CustomTexture3DData(width, height, depth, 0, glFormat, glInternalFormat, glType))

  if (sde.graphics.gl30.isEmpty) {
    throw SgeError.GraphicsError("Texture3D requires a device running with GLES 3.0 compatibility")
  }

  load(data)(using sde)

  if (data.isManaged()) {
    Texture3D.addManagedTexture(sde.application, this)
  }

  private def load(data: Texture3DData)(using sde: Sge): Unit = {
    Nullable(this.textureData).foreach { existing =>
      if (data.isManaged() != existing.isManaged())
        throw SgeError.GraphicsError("New data must have the same managed status as the old data")
    }
    this.textureData = data

    bind()

    if (!data.isPrepared()) data.prepare()

    data.consume3DData()

    setFilter(minFilter, magFilter)
    setWrap(uWrap, vWrap, rWrap)

    sde.graphics.gl.glBindTexture(glTarget, 0)
  }

  def getData(): Texture3DData = textureData

  def upload(): Unit = {
    bind()
    textureData.consume3DData()
  }

  override def getWidth: Int = textureData.getWidth()

  override def getHeight: Int = textureData.getHeight()

  override def getDepth: Int = textureData.getDepth()

  override def isManaged: Boolean = textureData.isManaged()

  override protected def reload(): Unit = {
    if (!isManaged) throw SgeError.GraphicsError("Tried to reload an unmanaged TextureArray")
    glHandle = TextureHandle(sde.graphics.gl.glGenTexture())
    load(textureData)
  }

  def setWrap(u: TextureWrap, v: TextureWrap, r: TextureWrap): Unit = {
    this.rWrap = r
    super.setWrap(u, v)
    sde.graphics.gl.glTexParameteri(glTarget, GL30.GL_TEXTURE_WRAP_R, r.getGLEnum())
  }

  def unsafeSetWrap(u: TextureWrap, v: TextureWrap, r: TextureWrap, force: Boolean): Unit = {
    unsafeSetWrap(u, v, force)
    if (force || rWrap != r) {
      sde.graphics.gl.glTexParameteri(glTarget, GL30.GL_TEXTURE_WRAP_R, r.getGLEnum())
      rWrap = r
    }
  }

  def unsafeSetWrap(u: TextureWrap, v: TextureWrap, r: TextureWrap): Unit =
    unsafeSetWrap(u, v, r, false)
}

object Texture3D {
  private val managedTexture3Ds: mutable.Map[Application, DynamicArray[Texture3D]] = mutable.Map()

  private def addManagedTexture(app: Application, texture: Texture3D): Unit = {
    val managedTextureArray = managedTexture3Ds.getOrElseUpdate(app, DynamicArray[Texture3D]())
    managedTextureArray.add(texture)
  }

  /** Clears all managed TextureArrays. This is an internal method. Do not use it! */
  def clearAllTextureArrays(app: Application): Unit =
    managedTexture3Ds.remove(app)

  /** Invalidate all managed TextureArrays. This is an internal method. Do not use it! */
  def invalidateAllTextureArrays(app: Application): Unit =
    managedTexture3Ds.get(app).foreach { managedTextureArray =>
      managedTextureArray.foreach(_.reload())
    }

  def getManagedStatus(): String = {
    val builder = StringBuilder()
    builder.append("Managed TextureArrays/app: { ")
    for ((app, textures) <- managedTexture3Ds) {
      builder.append(textures.size)
      builder.append(" ")
    }
    builder.append("}")
    builder.toString()
  }

  /** @return the number of managed Texture3D currently loaded */
  def getNumManagedTextures3D()(using sde: Sge): Int =
    managedTexture3Ds.get(sde.application).map(_.size).getOrElse(0)
}
