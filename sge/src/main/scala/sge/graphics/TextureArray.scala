/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/TextureArray.java
 * Original authors: Tomski
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: managed lifecycle; @nowarn for orNull at GL30 interop boundary
 *   Idiom: split packages
 *   Renames: isManaged → managed
 *   Convention: typed GL enums — TextureTarget, PixelFormat, DataType for GL calls
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics

import sge.files.FileHandle
import sge.utils.{ Nullable, SgeError }
import scala.annotation.nowarn
import sge.graphics.Pixmap.Format

import scala.collection.mutable
import sge.utils.DynamicArray

/** Open GLES wrapper for TextureArray
  * @author
  *   Tomski
  */
class TextureArray(data: TextureArrayData)(using Sge) extends GLTexture(TextureTarget.Texture2DArray, TextureHandle(Sge().graphics.gl.glGenTexture())) {

  private var textureData: TextureArrayData = scala.compiletime.uninitialized

  // Constructor that takes internal paths as strings
  def this(internalPaths: Array[String])(using Sge) = {
    this(TextureArrayData.Factory.loadFromFiles(Format.RGBA8888, false, TextureArray.getInternalHandles(internalPaths*)*))
  }

  // Constructor with useMipMaps, format, and FileHandles - calls primary constructor
  def this(useMipMaps: Boolean, format: Format, files: Array[FileHandle])(using Sge) = {
    this(TextureArrayData.Factory.loadFromFiles(format, useMipMaps, files*))
  }

  // Constructor with useMipMaps flag and FileHandles - calls the one above
  def this(useMipMaps: Boolean, files: Array[FileHandle])(using Sge) = {
    this(useMipMaps, Format.RGBA8888, files)
  }

  // Constructor that takes FileHandles - calls the one above
  def this(files: Array[FileHandle])(using Sge) = {
    this(false, files)
  }

  if (Sge().graphics.gl30.isEmpty) {
    throw SgeError.GraphicsError("TextureArray requires a device running with GLES 3.0 compatibility")
  }

  load(data)

  if (data.isManaged) TextureArray.addManagedTexture(Sge().application, this)

  private def load(data: TextureArrayData): Unit = {
    Nullable(this.textureData).foreach { existing =>
      if (data.isManaged != existing.isManaged)
        throw SgeError.GraphicsError("New data must have the same managed status as the old data")
    }
    this.textureData = data

    bind()
    // orNull required: GL30 Java API needs direct reference for OpenGL call
    @nowarn("msg=deprecated") val gl30 = Sge().graphics.gl30.orNull
    gl30.glTexImage3D(
      TextureTarget.Texture2DArray,
      0,
      data.internalFormat,
      data.width,
      data.height,
      data.depth,
      0,
      PixelFormat(data.internalFormat),
      DataType(data.glType),
      null
    )

    if (!data.isPrepared) data.prepare()

    data.consumeTextureArrayData()

    setFilter(minFilter, magFilter)
    setWrap(uWrap, vWrap)
    Sge().graphics.gl.glBindTexture(glTarget, 0)
  }

  override def width: Pixels = Pixels(textureData.width)

  override def height: Pixels = Pixels(textureData.height)

  override def depth: Int = textureData.depth

  override def managed: Boolean = textureData.isManaged

  override protected def reload(): Unit = {
    if (!managed) throw SgeError.GraphicsError("Tried to reload an unmanaged TextureArray")
    glHandle = TextureHandle(Sge().graphics.gl.glGenTexture())
    load(textureData)
  }
}

object TextureArray {
  final val managedTextureArrays: mutable.Map[Application, DynamicArray[TextureArray]] =
    mutable.Map[Application, DynamicArray[TextureArray]]()

  private def getInternalHandles(internalPaths: String*)(using Sge): Array[FileHandle] = {
    val handles = new Array[FileHandle](internalPaths.length)
    for (i <- internalPaths.indices)
      handles(i) = Sge().files.internal(internalPaths(i))
    handles
  }

  private def addManagedTexture(app: Application, texture: TextureArray): Unit = {
    val managedTextureArray = managedTextureArrays.getOrElseUpdate(app, DynamicArray[TextureArray]())
    managedTextureArray.add(texture)
  }

  /** Clears all managed TextureArrays. This is an internal method. Do not use it! */
  def clearAllTextureArrays(app: Application): Unit =
    managedTextureArrays.remove(app)

  /** Invalidate all managed TextureArrays. This is an internal method. Do not use it! */
  def invalidateAllTextureArrays(app: Application): Unit = {
    val managedTextureArray = managedTextureArrays.get(app)
    if (managedTextureArray.isDefined) {
      var i = 0
      val n = managedTextureArray.get.size
      while (i < n) {
        val textureArray = managedTextureArray.get(i)
        textureArray.reload()
        i += 1
      }
    }
  }

  def managedStatus: String = {
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
  def numManagedTextureArrays(using Sge): Int =
    managedTextureArrays.get(Sge().application).map(_.size).getOrElse(0)
}
