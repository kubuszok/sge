/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/VertexAttributes.java
 * Original authors: mzechner, Xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: Usage constants in companion object; ReadonlyIterator/ReadonlyIterable wrappers
 *   Idiom: split packages
 *   TODO: Java-style getters/setters — getOffset, getMask, getMaskWithSizePacked
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics

import sge.utils.Nullable
import java.util.NoSuchElementException
import scala.util.boundary, boundary.break

/** Instances of this class specify the vertex attributes of a mesh. VertexAttributes are used by {@link Mesh} instances to define its vertex structure. Vertex attributes have an order. The order is
  * specified by the order they are added to this class.
  *
  * @author
  *   mzechner, Xoppa (original implementation)
  */
final class VertexAttributes(attributes: VertexAttribute*) extends Iterable[VertexAttribute] with Comparable[VertexAttributes] {

  if (attributes.length == 0) throw new IllegalArgumentException("attributes must be >= 1")

  /** the attributes in the order they were specified * */
  private val attributesArray: Array[VertexAttribute] = attributes.toArray

  /** the size of a single vertex in bytes * */
  val vertexSize: Int = calculateOffsets()

  /** cache of the value calculated by {@link #getMask()} * */
  private var mask: Long = -1

  /** cache for bone weight units. */
  private var boneWeightUnits: Int = -1

  /** cache for texture coordinate units. */
  private var textureCoordinates: Int = -1

  private var iterable: ReadonlyIterable[VertexAttribute] = scala.compiletime.uninitialized

  /** Returns the offset for the first VertexAttribute with the specified usage.
    * @param usage
    *   The usage of the VertexAttribute.
    */
  def getOffset(usage: Int, defaultIfNotFound: Int): Int =
    findByUsage(usage).fold(defaultIfNotFound)(_.offset / 4)

  /** Returns the offset for the first VertexAttribute with the specified usage.
    * @param usage
    *   The usage of the VertexAttribute.
    */
  def getOffset(usage: Int): Int =
    getOffset(usage, 0)

  /** Returns the first VertexAttribute for the given usage.
    * @param usage
    *   The usage of the VertexAttribute to find.
    */
  def findByUsage(usage: Int): Nullable[VertexAttribute] = {
    val len = size
    boundary {
      for (i <- 0 until len)
        if (get(i).usage == usage) break(Nullable(get(i)))
      Nullable.empty
    }
  }

  private def calculateOffsets(): Int = {
    var count = 0
    for (i <- attributesArray.indices) {
      val attribute = attributesArray(i)
      attribute.offset = count
      count += attribute.getSizeInBytes()
    }
    count
  }

  /** @return the number of attributes */
  override def size: Int =
    attributesArray.length

  /** @param index
    *   the index
    * @return
    *   the VertexAttribute at the given index
    */
  def get(index: Int): VertexAttribute =
    attributesArray(index)

  override def toString(): String = {
    val builder = new StringBuilder()
    builder.append("[")
    for (i <- attributesArray.indices) {
      builder.append("(")
      builder.append(attributesArray(i).alias)
      builder.append(", ")
      builder.append(attributesArray(i).usage)
      builder.append(", ")
      builder.append(attributesArray(i).numComponents)
      builder.append(", ")
      builder.append(attributesArray(i).offset)
      builder.append(")")
      builder.append("\n")
    }
    builder.append("]")
    builder.toString()
  }

  override def equals(obj: Any): Boolean =
    if (this == obj) true
    else
      obj match {
        case other: VertexAttributes =>
          if (this.attributesArray.length != other.attributesArray.length) false
          else
            boundary {
              for (i <- attributesArray.indices)
                if (!attributesArray(i).equals(other.attributesArray(i))) break(false)
              true
            }
        case _ => false
      }

  override def hashCode(): Int = {
    var result = 61L * attributesArray.length
    for (i <- attributesArray.indices)
      result = result * 61 + attributesArray(i).hashCode()
    (result ^ (result >> 32)).toInt
  }

  /** Calculates a mask based on the contained {@link VertexAttribute} instances. The mask is a bit-wise or of each attributes {@link VertexAttribute#usage} .
    * @return
    *   the mask
    */
  def getMask(): Long = {
    if (mask == -1) {
      var result = 0L
      for (i <- attributesArray.indices)
        result |= attributesArray(i).usage
      mask = result
    }
    mask
  }

  /** Calculates the mask based on {@link VertexAttributes#getMask()} and packs the attributes count into the last 32 bits.
    * @return
    *   the mask with attributes count packed into the last 32 bits.
    */
  def getMaskWithSizePacked(): Long =
    getMask() | (attributesArray.length.toLong << 32)

  /** @return Number of bone weights based on {@link VertexAttribute#unit} */
  def getBoneWeights(): Int = {
    if (boneWeightUnits < 0) {
      boneWeightUnits = 0
      for (i <- attributesArray.indices) {
        val a = attributesArray(i)
        if (a.usage == VertexAttributes.Usage.BoneWeight) {
          boneWeightUnits = Math.max(boneWeightUnits, a.unit + 1)
        }
      }
    }
    boneWeightUnits
  }

  /** @return Number of texture coordinates based on {@link VertexAttribute#unit} */
  def getTextureCoordinates(): Int = {
    if (textureCoordinates < 0) {
      textureCoordinates = 0
      for (i <- attributesArray.indices) {
        val a = attributesArray(i)
        if (a.usage == VertexAttributes.Usage.TextureCoordinates) {
          textureCoordinates = Math.max(textureCoordinates, a.unit + 1)
        }
      }
    }
    textureCoordinates
  }

  override def compareTo(o: VertexAttributes): Int =
    if (attributesArray.length != o.attributesArray.length)
      attributesArray.length - o.attributesArray.length
    else {
      val m1 = getMask()
      val m2 = o.getMask()
      if (m1 != m2)
        if (m1 < m2) -1 else 1
      else
        boundary {
          for (i <- attributesArray.length - 1 to 0 by -1) {
            val va0 = attributesArray(i)
            val va1 = o.attributesArray(i)
            if (va0.usage != va1.usage) break(va0.usage - va1.usage)
            if (va0.unit != va1.unit) break(va0.unit - va1.unit)
            if (va0.numComponents != va1.numComponents) break(va0.numComponents - va1.numComponents)
            if (va0.normalized != va1.normalized) break(if (va0.normalized) 1 else -1)
            if (va0.`type` != va1.`type`) break(va0.`type` - va1.`type`)
          }
          0
        }
    }

  override def iterator(): Iterator[VertexAttribute] = {
    if (Nullable(iterable).isEmpty) iterable = new ReadonlyIterable[VertexAttribute](attributesArray)
    iterable.iterator()
  }

  private class ReadonlyIterator[T](array: Array[T]) extends Iterator[T] {
    private var index: Int     = 0
    private val valid: Boolean = true

    override def hasNext: Boolean = {
      if (!valid) throw new RuntimeException("#iterator() cannot be used nested.")
      index < array.length
    }

    override def next(): T = {
      if (index >= array.length) throw new NoSuchElementException(index.toString)
      if (!valid) throw new RuntimeException("#iterator() cannot be used nested.")
      val result = array(index)
      index += 1
      result
    }

    def reset(): Unit =
      index = 0
  }

  private class ReadonlyIterable[T](array: Array[T]) extends Iterable[T] {
    override def iterator(): Iterator[T] =
      // For now, always create new iterators to avoid complexity
      new ReadonlyIterator(array)
  }
}

object VertexAttributes {

  /** The usage of a vertex attribute.
    *
    * @author
    *   mzechner
    */
  object Usage {
    final val Position           = 1
    final val ColorUnpacked      = 2
    final val ColorPacked        = 4
    final val Normal             = 8
    final val TextureCoordinates = 16
    final val Generic            = 32
    final val BoneWeight         = 64
    final val Tangent            = 128
    final val BiNormal           = 256
  }
}
