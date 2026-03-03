/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/utils/TextureDescriptor.java
 * Original authors: (see AUTHORS file)
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   - Comparable[TextureDescriptor[T]] -> Ordered[TextureDescriptor[T]] (compareTo -> compare)
 *   - Null fields -> Nullable[T] with fold-based null-safe access
 *   - No return statements -> boundary/break
 *   - All constructors, set(), equals(), hashCode(), compare() fully ported
 *   - Audit: pass (2026-03-03)
 */
package sge
package graphics
package g3d
package utils

import scala.util.boundary
import scala.util.boundary.break
import sge.graphics.Texture.{ TextureFilter, TextureWrap }
import sge.utils.Nullable

class TextureDescriptor[T <: GLTexture]() extends Ordered[TextureDescriptor[T]] {

  // TODO add other values, see http://www.opengl.org/sdk/docs/man/xhtml/glTexParameter.xml

  var texture:   Nullable[T]             = Nullable.empty
  var minFilter: Nullable[TextureFilter] = Nullable.empty
  var magFilter: Nullable[TextureFilter] = Nullable.empty
  var uWrap:     Nullable[TextureWrap]   = Nullable.empty
  var vWrap:     Nullable[TextureWrap]   = Nullable.empty

  def this(texture: T, minFilter: Nullable[TextureFilter], magFilter: Nullable[TextureFilter], uWrap: Nullable[TextureWrap], vWrap: Nullable[TextureWrap]) = {
    this()
    set(texture, minFilter, magFilter, uWrap, vWrap)
  }

  def this(texture: T) = {
    this()
    set(texture, Nullable.empty, Nullable.empty, Nullable.empty, Nullable.empty)
  }

  def set(texture: T, minFilter: Nullable[TextureFilter], magFilter: Nullable[TextureFilter], uWrap: Nullable[TextureWrap], vWrap: Nullable[TextureWrap]): Unit = {
    this.texture = Nullable(texture)
    this.minFilter = minFilter
    this.magFilter = magFilter
    this.uWrap = uWrap
    this.vWrap = vWrap
  }

  def set[V <: T](other: TextureDescriptor[V]): Unit = {
    texture = other.texture.asInstanceOf[Nullable[T]]
    minFilter = other.minFilter
    magFilter = other.magFilter
    uWrap = other.uWrap
    vWrap = other.vWrap
  }

  override def equals(obj: Any): Boolean = obj match {
    case null => false
    case that:  AnyRef if that eq this => true
    case other: TextureDescriptor[?]   =>
      other.texture == texture && other.minFilter == minFilter && other.magFilter == magFilter &&
      other.uWrap == uWrap && other.vWrap == vWrap
    case _ => false
  }

  override def hashCode(): Int = {
    var result: Long = texture.fold(0L)(_.glTarget.toLong)
    result = 811 * result + texture.fold(0L)(_.getTextureObjectHandle().toInt.toLong)
    result = 811 * result + minFilter.fold(0L)(_.getGLEnum().toLong)
    result = 811 * result + magFilter.fold(0L)(_.getGLEnum().toLong)
    result = 811 * result + uWrap.fold(0L)(_.getGLEnum().toLong)
    result = 811 * result + vWrap.fold(0L)(_.getGLEnum().toLong)
    (result ^ (result >> 32)).toInt
  }

  override def compare(that: TextureDescriptor[T]): Int = boundary {
    if (that eq this) break(0)
    val t1 = texture.fold(0)(_.glTarget)
    val t2 = that.texture.fold(0)(_.glTarget)
    if (t1 != t2) break(t1 - t2)
    val h1 = texture.fold(0)(_.getTextureObjectHandle().toInt)
    val h2 = that.texture.fold(0)(_.getTextureObjectHandle().toInt)
    if (h1 != h2) break(h1 - h2)
    if (minFilter != that.minFilter)
      break(minFilter.fold(0)(_.getGLEnum()) - that.minFilter.fold(0)(_.getGLEnum()))
    if (magFilter != that.magFilter)
      break(magFilter.fold(0)(_.getGLEnum()) - that.magFilter.fold(0)(_.getGLEnum()))
    if (uWrap != that.uWrap)
      break(uWrap.fold(0)(_.getGLEnum()) - that.uWrap.fold(0)(_.getGLEnum()))
    if (vWrap != that.vWrap)
      break(vWrap.fold(0)(_.getGLEnum()) - that.vWrap.fold(0)(_.getGLEnum()))
    0
  }
}
