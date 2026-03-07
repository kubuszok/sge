/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/utils/TextureDescriptor.java
 * Original authors: (see AUTHORS file)
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
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
    var result: Long = texture.map(_.glTarget.toInt.toLong).getOrElse(0L)
    result = 811 * result + texture.map(_.getTextureObjectHandle().toInt.toLong).getOrElse(0L)
    result = 811 * result + minFilter.map(_.getGLEnum().toLong).getOrElse(0L)
    result = 811 * result + magFilter.map(_.getGLEnum().toLong).getOrElse(0L)
    result = 811 * result + uWrap.map(_.getGLEnum().toLong).getOrElse(0L)
    result = 811 * result + vWrap.map(_.getGLEnum().toLong).getOrElse(0L)
    (result ^ (result >> 32)).toInt
  }

  override def compare(that: TextureDescriptor[T]): Int = boundary {
    if (that eq this) break(0)
    val t1 = texture.map(_.glTarget.toInt).getOrElse(0)
    val t2 = that.texture.map(_.glTarget.toInt).getOrElse(0)
    if (t1 != t2) break(t1 - t2)
    val h1 = texture.map(_.getTextureObjectHandle().toInt).getOrElse(0)
    val h2 = that.texture.map(_.getTextureObjectHandle().toInt).getOrElse(0)
    if (h1 != h2) break(h1 - h2)
    if (minFilter != that.minFilter)
      break(minFilter.map(_.getGLEnum()).getOrElse(0) - that.minFilter.map(_.getGLEnum()).getOrElse(0))
    if (magFilter != that.magFilter)
      break(magFilter.map(_.getGLEnum()).getOrElse(0) - that.magFilter.map(_.getGLEnum()).getOrElse(0))
    if (uWrap != that.uWrap)
      break(uWrap.map(_.getGLEnum()).getOrElse(0) - that.uWrap.map(_.getGLEnum()).getOrElse(0))
    if (vWrap != that.vWrap)
      break(vWrap.map(_.getGLEnum()).getOrElse(0) - that.vWrap.map(_.getGLEnum()).getOrElse(0))
    0
  }
}
