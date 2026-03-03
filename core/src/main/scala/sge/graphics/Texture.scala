/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/Texture.java
 * Original authors: badlogicgames@gmail.com
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge.graphics

import sge.{ Application, Sge }
import sge.graphics.Pixmap.Format
import sge.graphics.glutils.FileTextureData
import sge.graphics.glutils.PixmapTextureData
import sge.files.FileHandle
import sge.utils.{ Nullable, SgeError }
import sge.assets.AssetManager
import sge.utils.DynamicArray
import scala.collection.mutable.Map

/** A Texture wraps a standard OpenGL ES texture. <p> A Texture can be managed. If the OpenGL context is lost all managed textures get invalidated. This happens when a user switches to another
  * application or receives an incoming call. Managed textures get reloaded automatically. <p> A Texture has to be bound via the {@link Texture#bind()} method in order for it to be applied to
  * geometry. The texture will be bound to the currently active texture unit specified via {@link GL20#glActiveTexture(int)} . <p> You can draw {@link Pixmap} s to a texture at any time. The changes
  * will be automatically uploaded to texture memory. This is of course not extremely fast so use it with care. It also only works with unmanaged textures. <p> A Texture must be disposed when it is no
  * longer used
  * @author
  *   badlogicgames@gmail.com
  */
class Texture(glTarget: Int, glHandle: TextureHandle, data: TextureData)(using Sge) extends GLTexture(glTarget, glHandle) {

  // TextureFilter and TextureWrap enums are now defined in TextureEnums.scala

  private val textureData = data

  def this(internalPath: String)(using Sge) =
    this(
      GL20.GL_TEXTURE_2D,
      TextureHandle(Sge().graphics.gl.glGenTexture()),
      TextureData.Factory.loadFromFile(Sge().files.internal(internalPath), Nullable.empty, false)
    )

  def this(file: FileHandle)(using Sge) =
    this(
      GL20.GL_TEXTURE_2D,
      TextureHandle(Sge().graphics.gl.glGenTexture()),
      TextureData.Factory.loadFromFile(file, Nullable.empty, false)
    )

  def this(file: FileHandle, useMipMaps: Boolean)(using Sge) =
    this(
      GL20.GL_TEXTURE_2D,
      TextureHandle(Sge().graphics.gl.glGenTexture()),
      TextureData.Factory.loadFromFile(file, Nullable.empty, useMipMaps)
    )

  def this(file: FileHandle, format: Format, useMipMaps: Boolean)(using Sge) =
    this(
      GL20.GL_TEXTURE_2D,
      TextureHandle(Sge().graphics.gl.glGenTexture()),
      TextureData.Factory.loadFromFile(file, Nullable(format), useMipMaps)
    )

  def this(pixmap: Pixmap)(using Sge) =
    this(
      GL20.GL_TEXTURE_2D,
      TextureHandle(Sge().graphics.gl.glGenTexture()),
      new PixmapTextureData(pixmap, Nullable.empty, false, false, false)
    )

  def this(pixmap: Pixmap, useMipMaps: Boolean)(using Sge) =
    this(
      GL20.GL_TEXTURE_2D,
      TextureHandle(Sge().graphics.gl.glGenTexture()),
      new PixmapTextureData(pixmap, Nullable.empty, useMipMaps, false, false)
    )

  def this(pixmap: Pixmap, format: Format, useMipMaps: Boolean)(using Sge) =
    this(
      GL20.GL_TEXTURE_2D,
      TextureHandle(Sge().graphics.gl.glGenTexture()),
      new PixmapTextureData(pixmap, format, useMipMaps, false)
    )

  def this(width: Int, height: Int, format: Format)(using Sge) =
    this(
      GL20.GL_TEXTURE_2D,
      TextureHandle(Sge().graphics.gl.glGenTexture()),
      new PixmapTextureData(new Pixmap(width, height, format), Nullable.empty, false, true, false)
    )

  def this(data: TextureData)(using Sge) =
    this(GL20.GL_TEXTURE_2D, TextureHandle(Sge().graphics.gl.glGenTexture()), data)

  load(data)
  if (data.isManaged) Texture.addManagedTexture(summon[Sge].application, this)

  def load(data: TextureData)(using Sge): Unit = {
    Nullable(this.textureData).foreach { existing =>
      if (data.isManaged != existing.isManaged)
        throw SgeError.GraphicsError("New data must have the same managed status as the old data")
    }

    if (!data.isPrepared) data.prepare()

    bind()
    uploadImageData(GL20.GL_TEXTURE_2D, data)

    unsafeSetFilter(minFilter, magFilter, true)
    unsafeSetWrap(uWrap, vWrap, true)
    unsafeSetAnisotropicFilter(anisotropicFilterLevel, true)
    Sge().graphics.gl.glBindTexture(glTarget, 0)
  }

  /** Used internally to reload after context loss. Creates a new GL handle then calls {@link #load(TextureData)} . Use this only if you know what you do!
    */
  override protected def reload(): Unit = {
    if (!isManaged) throw SgeError.GraphicsError("Tried to reload unmanaged Texture")
    glHandle = TextureHandle(Sge().graphics.gl.glGenTexture())
    load(textureData)
  }

  /** Draws the given {@link Pixmap} to the texture at position x, y. No clipping is performed so you have to make sure that you draw only inside the texture region. Note that this will only draw to
    * mipmap level 0!
    *
    * @param pixmap
    *   The Pixmap
    * @param x
    *   The x coordinate in pixels
    * @param y
    *   The y coordinate in pixels
    */
  def draw(pixmap: Pixmap, x: Int, y: Int)(using Sge): Unit = {
    if (textureData.isManaged) throw SgeError.GraphicsError("can't draw to a managed texture")

    bind()
    Sge().graphics.gl.glTexSubImage2D(glTarget, 0, x, y, pixmap.getWidth(), pixmap.getHeight(), pixmap.getGLFormat(), pixmap.getGLType(), pixmap.getPixels())
  }

  override def getWidth: Int = textureData.getWidth

  override def getHeight: Int = textureData.getHeight

  override def getDepth: Int = 0

  def getTextureData(): TextureData = textureData

  /** @return whether this texture is managed or not. */
  override def isManaged: Boolean = textureData.isManaged

  /** Disposes all resources associated with the texture */
  override def close(): Unit =
    // this is a hack. reason: we have to set the glHandle to 0 for textures that are
    // reloaded through the asset manager as we first remove (and thus dispose) the texture
    // and then reload it. the glHandle is set to 0 in invalidateAllTextures prior to
    // removal from the asset manager.
    if (glHandle != TextureHandle.none) {
      delete()
      if (textureData.isManaged)
        Texture.managedTextures.get(Sge().application).foreach(_.removeValue(this))
    }

  override def toString(): String =
    textureData match {
      case fileData: FileTextureData => fileData.toString()
      case _ => super.toString()
    }
}

object Texture {
  private var assetManager:    AssetManager                            = scala.compiletime.uninitialized
  private val managedTextures: Map[Application, DynamicArray[Texture]] = Map.empty

  private def addManagedTexture(app: Application, texture: Texture): Unit = {
    val managedTextureArray = managedTextures.getOrElseUpdate(app, DynamicArray[Texture]())
    managedTextureArray.add(texture)
  }

  /** Clears all managed textures. This is an internal method. Do not use it! */
  def clearAllTextures(app: Application): Unit =
    managedTextures.remove(app)

  /** Invalidate all managed textures. This is an internal method. Do not use it! */
  def invalidateAllTextures(app: Application)(using Sge): Unit =
    managedTextures.get(app) match {
      case None                      => ()
      case Some(managedTextureArray) =>
        if (Nullable(assetManager).isEmpty) {
          for (texture <- managedTextureArray)
            texture.reload()
        } else {
          // first we have to make sure the AssetManager isn't loading anything anymore,
          // otherwise the ref counting trick below wouldn't work (when a texture is
          // currently on the task stack of the manager.)
          // assetManager.finishLoading()

          // next we go through each texture and reload either directly or via the
          // asset manager.
          val textures = DynamicArray.from(managedTextureArray)
          for (texture <- textures)
            // val fileName = assetManager.getAssetFileName(texture)
            // if (fileName == null) {
            texture.reload()
          // } else {
          // Implementation for asset manager reloading would go here
          // }
          managedTextureArray.clear()
          managedTextureArray.addAll(textures)
        }
    }

  /** Sets the {@link AssetManager} . When the context is lost, textures managed by the asset manager are reloaded by the manager on a separate thread (provided that a suitable {@link AssetLoader} is
    * registered with the manager). Textures not managed by the AssetManager are reloaded via the usual means on the rendering thread.
    * @param manager
    *   the asset manager.
    */
  def setAssetManager(manager: AssetManager): Unit =
    Texture.assetManager = manager

  def getManagedStatus(): String = {
    val builder = new StringBuilder()
    builder.append("Managed textures/app: { ")
    for ((_, textures) <- managedTextures) {
      builder.append(textures.size)
      builder.append(" ")
    }
    builder.append("}")
    builder.toString()
  }

  /** @return the number of managed textures currently loaded */
  def getNumManagedTextures()(using Sge): Int =
    managedTextures.get(Sge().application).fold(0)(_.size)

  /** Defines the filtering mode for textures. */
  enum TextureFilter(val glEnum: Int) {

    /** Fetch the nearest texel that best maps to the pixel on screen. */
    case Nearest extends TextureFilter(GL20.GL_NEAREST)

    /** Fetch four nearest texels that best maps to the pixel on screen. */
    case Linear extends TextureFilter(GL20.GL_LINEAR)

    /** @see TextureFilter#MipMapLinearLinear */
    case MipMap extends TextureFilter(GL20.GL_LINEAR_MIPMAP_LINEAR)

    /** Fetch the best fitting image from the mip map chain based on the pixel/texel ratio and then sample the texels with a nearest filter.
      */
    case MipMapNearestNearest extends TextureFilter(GL20.GL_NEAREST_MIPMAP_NEAREST)

    /** Fetch the best fitting image from the mip map chain based on the pixel/texel ratio and then sample the texels with a linear filter.
      */
    case MipMapLinearNearest extends TextureFilter(GL20.GL_LINEAR_MIPMAP_NEAREST)

    /** Fetch the two best fitting images from the mip map chain and then sample the nearest texel from each of the two images, combining them to the final output pixel.
      */
    case MipMapNearestLinear extends TextureFilter(GL20.GL_NEAREST_MIPMAP_LINEAR)

    /** Fetch the two best fitting images from the mip map chain and then sample the four nearest texels from each of the two images, combining them to the final output pixel.
      */
    case MipMapLinearLinear extends TextureFilter(GL20.GL_LINEAR_MIPMAP_LINEAR)

    def isMipMap(): Boolean =
      glEnum != GL20.GL_NEAREST && glEnum != GL20.GL_LINEAR

    def getGLEnum(): Int = glEnum
  }

  /** Defines the wrapping mode for textures. */
  enum TextureWrap(val glEnum: Int) {
    case MirroredRepeat extends TextureWrap(GL20.GL_MIRRORED_REPEAT)
    case ClampToEdge extends TextureWrap(GL20.GL_CLAMP_TO_EDGE)
    case Repeat extends TextureWrap(GL20.GL_REPEAT)

    def getGLEnum(): Int = glEnum
  }
}
