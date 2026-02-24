/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/TextureArray.java
 * Original authors: Tomski
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics

import sge.files.FileHandle
import sge.utils.SgeError
import sge.graphics.Pixmap.Format
import scala.annotation.targetName

import scala.collection.mutable

/** Open GLES wrapper for TextureArray
  * @author
  *   Tomski
  */
class TextureArray(data: TextureArrayData)(using sge: Sge) extends GLTexture(GL30.GL_TEXTURE_2D_ARRAY, TextureHandle(sge.graphics.gl.glGenTexture())) {

  private var textureData: TextureArrayData = scala.compiletime.uninitialized

  // Constructor that takes internal paths as strings
  def this(internalPaths: Array[String])(using sge: Sge) = {
    this(TextureArrayData.Factory.loadFromFiles(Format.RGBA8888, false, TextureArray.getInternalHandles(internalPaths*)*))
  }

  // Constructor with useMipMaps, format, and FileHandles - calls primary constructor
  def this(useMipMaps: Boolean, format: Format, files: Array[FileHandle])(using sge: Sge) = {
    this(TextureArrayData.Factory.loadFromFiles(format, useMipMaps, files*))
  }

  // Constructor with useMipMaps flag and FileHandles - calls the one above
  def this(useMipMaps: Boolean, files: Array[FileHandle])(using sge: Sge) = {
    this(useMipMaps, Format.RGBA8888, files)
  }

  // Constructor that takes FileHandles - calls the one above
  def this(files: Array[FileHandle])(using sge: Sge) = {
    this(false, files)
  }

  if (sge.graphics.gl30.isEmpty) {
    throw SgeError.GraphicsError("TextureArray requires a device running with GLES 3.0 compatibility")
  }

  load(data)

  if (data.isManaged()) TextureArray.addManagedTexture(sge.application, this)

  private def load(data: TextureArrayData): Unit = {
    if (this.textureData != null && data.isManaged() != this.textureData.isManaged())
      throw SgeError.GraphicsError("New data must have the same managed status as the old data")
    this.textureData = data

    bind()
    sge.graphics.gl30.orNull.glTexImage3D(
      GL30.GL_TEXTURE_2D_ARRAY,
      0,
      data.getInternalFormat(),
      data.getWidth(),
      data.getHeight(),
      data.getDepth(),
      0,
      data.getInternalFormat(),
      data.getGLType(),
      null
    )

    if (!data.isPrepared()) data.prepare()

    data.consumeTextureArrayData()

    setFilter(minFilter, magFilter)
    setWrap(uWrap, vWrap)
    sge.graphics.gl.glBindTexture(glTarget, 0)
  }

  override def getWidth: Int = textureData.getWidth()

  override def getHeight: Int = textureData.getHeight()

  override def getDepth: Int = textureData.getDepth()

  override def isManaged: Boolean = textureData.isManaged()

  override protected def reload(): Unit = {
    if (!isManaged) throw SgeError.GraphicsError("Tried to reload an unmanaged TextureArray")
    glHandle = TextureHandle(sge.graphics.gl.glGenTexture())
    load(textureData)
  }
}

object TextureArray {
  final val managedTextureArrays: mutable.Map[Application, mutable.ArrayBuffer[TextureArray]] =
    mutable.Map[Application, mutable.ArrayBuffer[TextureArray]]()

  private def getInternalHandles(internalPaths: String*)(using sge: Sge): Array[FileHandle] = {
    val handles = new Array[FileHandle](internalPaths.length)
    for (i <- internalPaths.indices)
      handles(i) = sge.files.internal(internalPaths(i))
    handles
  }

  private def addManagedTexture(app: Application, texture: TextureArray): Unit = {
    var managedTextureArray = managedTextureArrays.get(app)
    if (managedTextureArray.isEmpty) {
      managedTextureArray = Some(mutable.ArrayBuffer[TextureArray]())
      managedTextureArrays.put(app, managedTextureArray.get)
    }
    managedTextureArray.get.addOne(texture)
  }

  /** Clears all managed TextureArrays. This is an internal method. Do not use it! */
  def clearAllTextureArrays(app: Application): Unit =
    managedTextureArrays.remove(app)

  /** Invalidate all managed TextureArrays. This is an internal method. Do not use it! */
  def invalidateAllTextureArrays(app: Application): Unit = {
    val managedTextureArray = managedTextureArrays.get(app)
    if (managedTextureArray.isEmpty) return

    for (i <- managedTextureArray.get.indices) {
      val textureArray = managedTextureArray.get(i)
      textureArray.reload()
    }
  }

  def getManagedStatus(): String = {
    val builder = new StringBuilder()
    builder.append("Managed TextureArrays/app: { ")
    for (app <- managedTextureArrays.keys) {
      builder.append(managedTextureArrays(app).size)
      builder.append(" ")
    }
    builder.append("}")
    builder.toString()
  }

  /** @return the number of managed TextureArrays currently loaded */
  def getNumManagedTextureArrays()(using sge: Sge): Int =
    managedTextureArrays.get(sge.application).map(_.size).getOrElse(0)
}
