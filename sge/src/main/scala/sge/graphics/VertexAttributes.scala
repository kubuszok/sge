/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/VertexAttributes.java
 * Original authors: mzechner, Xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: Usage constants in companion object; ReadonlyIterator/ReadonlyIterable wrappers
 *   Idiom: split packages
 *   Renames: getOffset → offset; getMask() → mask; getMaskWithSizePacked() → maskWithSizePacked
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 274
 * Covenant-baseline-methods: BiNormal,BoneWeight,ColorPacked,ColorUnpacked,Generic,Normal,Position,ReadonlyIterable,ReadonlyIterator,Tangent,TextureCoordinates,Usage,VertexAttributes,_textureCoordinates,attributesArray,boneWeightUnits,boneWeights,builder,cachedMask,calculateOffsets,compareTo,count,equals,findByUsage,get,hasNext,hashCode,index,iterable,iterator,iterator1,iterator2,len,mask,maskWithSizePacked,next,offset,reset,result,size,textureCoordinates,toString,valid,vertexSize
 * Covenant-source-reference: com/badlogic/gdx/graphics/VertexAttributes.java
 * Covenant-verified: 2026-04-19
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
  private var cachedMask: Long = -1

  /** cache for bone weight units. */
  private var boneWeightUnits: Int = -1

  /** cache for texture coordinate units. */
  private var _textureCoordinates: Int = -1

  private var iterable: ReadonlyIterable[VertexAttribute] = scala.compiletime.uninitialized

  /** Returns the offset for the first VertexAttribute with the specified usage.
    * @param usage
    *   The usage of the VertexAttribute.
    */
  def offset(usage: Int, defaultIfNotFound: Int): Int =
    findByUsage(usage).map(_.offset / 4).getOrElse(defaultIfNotFound)

  /** Returns the offset for the first VertexAttribute with the specified usage.
    * @param usage
    *   The usage of the VertexAttribute.
    */
  def offset(usage: Int): Int =
    offset(usage, 0)

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
      count += attribute.sizeInBytes
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
    if (this eq obj.asInstanceOf[AnyRef]) true
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
  def mask: Long = {
    if (cachedMask == -1) {
      var result = 0L
      for (i <- attributesArray.indices)
        result |= attributesArray(i).usage
      cachedMask = result
    }
    cachedMask
  }

  /** Calculates the mask based on {@link VertexAttributes#getMask()} and packs the attributes count into the last 32 bits.
    * @return
    *   the mask with attributes count packed into the last 32 bits.
    */
  def maskWithSizePacked: Long =
    mask | (attributesArray.length.toLong << 32)

  /** @return Number of bone weights based on {@link VertexAttribute#unit} */
  def boneWeights: Int = {
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
  def textureCoordinates: Int = {
    if (_textureCoordinates < 0) {
      _textureCoordinates = 0
      for (i <- attributesArray.indices) {
        val a = attributesArray(i)
        if (a.usage == VertexAttributes.Usage.TextureCoordinates) {
          _textureCoordinates = Math.max(_textureCoordinates, a.unit + 1)
        }
      }
    }
    _textureCoordinates
  }

  override def compareTo(o: VertexAttributes): Int =
    if (attributesArray.length != o.attributesArray.length)
      attributesArray.length - o.attributesArray.length
    else {
      val m1 = mask
      val m2 = o.mask
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
            if (va0.`type` != va1.`type`) break(va0.`type`.toInt - va1.`type`.toInt)
          }
          0
        }
    }

  override def iterator(): Iterator[VertexAttribute] = {
    if (Nullable(iterable).isEmpty) iterable = new ReadonlyIterable[VertexAttribute](attributesArray)
    iterable.iterator()
  }

  final private class ReadonlyIterator[T](array: Array[T]) extends Iterator[T] {
    private var index: Int     = 0
    var valid:         Boolean = true

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

  final private class ReadonlyIterable[T](array: Array[T]) extends Iterable[T] {
    private val iterator1: ReadonlyIterator[T] = ReadonlyIterator(array)
    private val iterator2: ReadonlyIterator[T] = ReadonlyIterator(array)

    override def iterator(): Iterator[T] =
      if (!iterator1.valid) {
        iterator1.reset()
        iterator1.valid = true
        iterator2.valid = false
        iterator1
      } else {
        iterator2.reset()
        iterator2.valid = true
        iterator1.valid = false
        iterator2
      }
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
