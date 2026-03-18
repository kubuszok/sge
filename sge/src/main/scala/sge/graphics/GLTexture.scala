/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/GLTexture.java
 * Original authors: badlogic, Xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: uses TextureHandle opaque type (SGE improvement)
 *   Idiom: split packages
 *   Convention: close() calls delete() for GL cleanup (Java dispose() -> close())
 *   Convention: typed GL enums — TextureTarget for glTarget, PixelFormat/DataType for upload
 *   Audited: 2026-03-04
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics

import sge.graphics.Pixmap.Blending
import sge.graphics.glutils.MipMapGenerator
import sge.graphics.Texture.{ TextureFilter, TextureWrap }
import sge.utils.BufferUtils
import sge.math.MathUtils
import java.nio.Buffer
import scala.util.boundary
import scala.util.boundary.break

/** Class representing an OpenGL texture by its target and handle. Keeps track of its state like the TextureFilter and TextureWrap. Also provides some (protected) static methods to create TextureData
  * and upload image data.
  * @author
  *   badlogic, Xoppa
  */
abstract class GLTexture(val glTarget: TextureTarget, private[graphics] var glHandle: TextureHandle)(using Sge) extends AutoCloseable {

  protected var _minFilter:             TextureFilter = TextureFilter.Nearest
  protected var _magFilter:             TextureFilter = TextureFilter.Nearest
  protected var _uWrap:                 TextureWrap   = TextureWrap.ClampToEdge
  protected var _vWrap:                 TextureWrap   = TextureWrap.ClampToEdge
  protected var anisotropicFilterLevel: Float         = 1.0f

  /** @return the width of the texture in pixels */
  def width: Pixels

  /** @return the height of the texture in pixels */
  def height: Pixels

  /** @return the depth of the texture in pixels */
  def depth: Int

  /** Generates a new OpenGL texture with the specified target. */
  def this(glTarget: TextureTarget)(using Sge) = {
    this(glTarget, TextureHandle(Sge().graphics.gl.glGenTexture()))
  }

  /** @return whether this texture is managed or not. */
  def managed: Boolean

  protected def reload(): Unit

  /** Binds this texture. The texture will be bound to the currently active texture unit specified via {@link GL20#glActiveTexture(int)} .
    */
  def bind(): Unit =
    Sge().graphics.gl.glBindTexture(glTarget, glHandle.toInt)

  /** Binds the texture to the given texture unit. Sets the currently active texture unit via {@link GL20#glActiveTexture(int)} .
    * @param unit
    *   the unit (0 to MAX_TEXTURE_UNITS).
    */
  def bind(unit: Int): Unit = {
    Sge().graphics.gl.glActiveTexture(GL20.GL_TEXTURE0 + unit)
    Sge().graphics.gl.glBindTexture(glTarget, glHandle.toInt)
  }

  /** @return The {@link TextureFilter} used for minification. */
  def minFilter: TextureFilter = _minFilter

  /** @return The {@link TextureFilter} used for magnification. */
  def magFilter: TextureFilter = _magFilter

  /** @return The {@link TextureWrap} used for horizontal (U) texture coordinates. */
  def uWrap: TextureWrap = _uWrap

  /** @return The {@link TextureWrap} used for vertical (V) texture coordinates. */
  def vWrap: TextureWrap = _vWrap

  /** @return The OpenGL handle for this texture. */
  def textureObjectHandle: TextureHandle = glHandle

  /** Sets the {@link TextureWrap} for this texture on the u and v axis. Assumes the texture is bound and active!
    * @param u
    *   the u wrap
    * @param v
    *   the v wrap
    */
  def unsafeSetWrap(u: TextureWrap, v: TextureWrap): Unit =
    unsafeSetWrap(u, v, false)

  /** Sets the {@link TextureWrap} for this texture on the u and v axis. Assumes the texture is bound and active!
    * @param u
    *   the u wrap
    * @param v
    *   the v wrap
    * @param force
    *   True to always set the values, even if they are the same as the current values.
    */
  def unsafeSetWrap(u: TextureWrap, v: TextureWrap, force: Boolean): Unit = {
    if (force || _uWrap != u) {
      Sge().graphics.gl.glTexParameteri(glTarget, GL20.GL_TEXTURE_WRAP_S, u.glEnum)
      _uWrap = u
    }
    if (force || _vWrap != v) {
      Sge().graphics.gl.glTexParameteri(glTarget, GL20.GL_TEXTURE_WRAP_T, v.glEnum)
      _vWrap = v
    }
  }

  /** Sets the {@link TextureWrap} for this texture on the u and v axis. This will bind this texture!
    * @param u
    *   the u wrap
    * @param v
    *   the v wrap
    */
  def setWrap(u: TextureWrap, v: TextureWrap): Unit = {
    this._uWrap = u
    this._vWrap = v
    bind()
    Sge().graphics.gl.glTexParameteri(glTarget, GL20.GL_TEXTURE_WRAP_S, u.glEnum)
    Sge().graphics.gl.glTexParameteri(glTarget, GL20.GL_TEXTURE_WRAP_T, v.glEnum)
  }

  /** Sets the {@link TextureFilter} for this texture for minification and magnification. Assumes the texture is bound and active!
    * @param minFilter
    *   the minification filter
    * @param magFilter
    *   the magnification filter
    */
  def unsafeSetFilter(minFilter: TextureFilter, magFilter: TextureFilter): Unit =
    unsafeSetFilter(minFilter, magFilter, false)

  /** Sets the {@link TextureFilter} for this texture for minification and magnification. Assumes the texture is bound and active!
    * @param minFilter
    *   the minification filter
    * @param magFilter
    *   the magnification filter
    * @param force
    *   True to always set the values, even if they are the same as the current values.
    */
  def unsafeSetFilter(minFilter: TextureFilter, magFilter: TextureFilter, force: Boolean): Unit = {
    if (force || this._minFilter != minFilter) {
      Sge().graphics.gl.glTexParameteri(glTarget, GL20.GL_TEXTURE_MIN_FILTER, minFilter.glEnum)
      this._minFilter = minFilter
    }
    if (force || this._magFilter != magFilter) {
      Sge().graphics.gl.glTexParameteri(glTarget, GL20.GL_TEXTURE_MAG_FILTER, magFilter.glEnum)
      this._magFilter = magFilter
    }
  }

  /** Sets the {@link TextureFilter} for this texture for minification and magnification. This will bind this texture!
    * @param minFilter
    *   the minification filter
    * @param magFilter
    *   the magnification filter
    */
  def setFilter(minFilter: TextureFilter, magFilter: TextureFilter): Unit = {
    this._minFilter = minFilter
    this._magFilter = magFilter
    bind()
    Sge().graphics.gl.glTexParameteri(glTarget, GL20.GL_TEXTURE_MIN_FILTER, minFilter.glEnum)
    Sge().graphics.gl.glTexParameteri(glTarget, GL20.GL_TEXTURE_MAG_FILTER, magFilter.glEnum)
  }

  /** Sets the anisotropic filter level for the texture. Assumes the texture is bound and active!
    *
    * @param level
    *   The desired level of filtering. The maximum level supported by the device up to this value will be used.
    * @param force
    *   True to always set the value, even if it is the same as the current value.
    * @return
    *   The actual level set, which may be lower than the provided value due to device limitations.
    */
  def unsafeSetAnisotropicFilter(level: Float, force: Boolean): Float = boundary {
    val max = GLTexture.maxAnisotropicFilterLevel
    if (max == 1f) break(1f)
    val adjustedLevel = Math.min(level, max)
    if (!force && MathUtils.isEqual(adjustedLevel, anisotropicFilterLevel, 0.1f)) break(adjustedLevel)
    Sge().graphics.gl20.glTexParameterf(TextureTarget.Texture2D, GL20.GL_TEXTURE_MAX_ANISOTROPY_EXT, adjustedLevel)
    anisotropicFilterLevel = adjustedLevel
    adjustedLevel
  }

  /** Sets the anisotropic filter level for the texture. This will bind the texture!
    *
    * @param level
    *   The desired level of filtering. The maximum level supported by the device up to this value will be used.
    * @return
    *   The actual level set, which may be lower than the provided value due to device limitations.
    */
  def setAnisotropicFilter(level: Float): Float = boundary {
    val max = GLTexture.maxAnisotropicFilterLevel
    if (max == 1f) break(1f)
    val adjustedLevel = Math.min(level, max)
    if (MathUtils.isEqual(adjustedLevel, anisotropicFilterLevel, 0.1f)) break(adjustedLevel)
    bind()
    Sge().graphics.gl20.glTexParameterf(TextureTarget.Texture2D, GL20.GL_TEXTURE_MAX_ANISOTROPY_EXT, adjustedLevel)
    anisotropicFilterLevel = adjustedLevel
    adjustedLevel
  }

  /** @return The currently set anisotropic filtering level for the texture, or 1.0f if none has been set. */
  def anisotropicFilter: Float = anisotropicFilterLevel

  /** Destroys the OpenGL Texture as specified by the glHandle. */
  protected def delete(): Unit =
    if (glHandle != TextureHandle.none) {
      Sge().graphics.gl.glDeleteTexture(glHandle.toInt)
      glHandle = TextureHandle.none
    }

  override def close(): Unit =
    delete()

  def uploadImageData(target: TextureTarget, data: TextureData): Unit =
    GLTexture.uploadImageData(target, data, 0)
}

object GLTexture {
  private var maxAnisotropicFilterLevel = 0f

  /** @return The maximum supported anisotropic filtering level supported by the device. */
  def maxAnisotropicFilterLevel(using Sge): Float = boundary {
    if (maxAnisotropicFilterLevel > 0) break(maxAnisotropicFilterLevel)
    if (Sge().graphics.supportsExtension("GL_EXT_texture_filter_anisotropic")) {
      val buffer = BufferUtils.newFloatBuffer(16)
      buffer.asInstanceOf[Buffer].position(0)
      buffer.asInstanceOf[Buffer].limit(buffer.capacity())
      Sge().graphics.gl20.glGetFloatv(GL20.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, buffer)
      maxAnisotropicFilterLevel = buffer.get(0)
      maxAnisotropicFilterLevel
    } else {
      maxAnisotropicFilterLevel = 1f
      1f
    }
  }

  def uploadImageData(target: TextureTarget, data: TextureData, miplevel: Int)(using Sge): Unit = boundary {
    if (!data.isPrepared) data.prepare()

    val dataType = data.dataType
    if (dataType == TextureData.TextureDataType.Custom) {
      data.consumeCustomData(target)
      break()
    }

    var pixmap        = data.consumePixmap()
    var disposePixmap = data.disposePixmap
    if (data.getFormat != pixmap.format) {
      val tmp = Pixmap(pixmap.width.toInt, pixmap.height.toInt, data.getFormat)
      tmp.setBlending(Blending.None)
      tmp.drawPixmap(pixmap, Pixels.zero, Pixels.zero, Pixels.zero, Pixels.zero, pixmap.width, pixmap.height)
      if (data.disposePixmap) {
        pixmap.close()
      }
      pixmap = tmp
      disposePixmap = true
    }

    Sge().graphics.gl.glPixelStorei(GL20.GL_UNPACK_ALIGNMENT, 1)
    if (data.useMipMaps) {
      MipMapGenerator.generateMipMap(target, pixmap, pixmap.width.toInt, pixmap.height.toInt)
    } else {
      Sge().graphics.gl.glTexImage2D(
        target,
        miplevel,
        pixmap.gLInternalFormat,
        pixmap.width,
        pixmap.height,
        0,
        PixelFormat(pixmap.gLFormat),
        DataType(pixmap.glType),
        pixmap.pixels
      )
    }
    if (disposePixmap) pixmap.close()
  }
}
