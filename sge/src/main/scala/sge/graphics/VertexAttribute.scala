/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/VertexAttribute.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: factory methods in companion object (Position, TexCoords, Normal, ColorPacked, etc.)
 *   Idiom: split packages
 *   Renames: getKey() → key
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 163
 * Covenant-baseline-methods: Binormal,BoneWeight,ColorPacked,ColorUnpacked,Normal,Position,Tangent,TexCoords,VertexAttribute,alias,copy,equals,hashCode,key,normalized,numComponents,offset,result,sizeInBytes,t,this,unit,usage,usageIndex
 * Covenant-source-reference: com/badlogic/gdx/graphics/VertexAttribute.java
 * Covenant-verified: 2026-04-19
 */
package sge
package graphics

import sge.graphics.glutils.ShaderProgram
import sge.graphics.VertexAttributes.Usage
import sge.graphics.GL20
import scala.compiletime.uninitialized

/** A single vertex attribute defined by its {@link Usage} , its number of components and its shader alias. The Usage is used for uniquely identifying the vertex attribute from among its
  * {@linkplain VertexAttributes} siblings. The number of components defines how many components the attribute has. The alias defines to which shader attribute this attribute should bind. The alias is
  * used by a {@link Mesh} when drawing with a {@link ShaderProgram} . The alias can be changed at any time.
  *
  * @author
  *   mzechner
  */
final class VertexAttribute(
  /** The attribute {@link Usage}, used for identification. * */
  val usage: Int,
  /** the number of components this attribute has * */
  val numComponents: Int,
  /** For fixed types, whether the values are normalized to either -1f and +1f (signed) or 0f and +1f (unsigned) */
  val normalized: Boolean,
  /** the OpenGL type of each component, e.g. {@link GL20#GL_FLOAT} or {@link GL20#GL_UNSIGNED_BYTE} */
  val `type`: DataType,
  /** the alias for the attribute used in a {@link ShaderProgram} * */
  var alias: String,
  /** optional unit/index specifier, used for texture coordinates and bone weights * */
  val unit: Int
) {

  /** the offset of this attribute in bytes, don't change this! * */
  var offset:             Int = uninitialized
  private val usageIndex: Int = Integer.numberOfTrailingZeros(usage)

  /** Constructs a new VertexAttribute. The GL data type is automatically selected based on the usage.
    *
    * @param usage
    *   The attribute {@link Usage} , used to select the {@link #type} and for identification.
    * @param numComponents
    *   the number of components of this attribute, must be between 1 and 4.
    * @param alias
    *   the alias used in a shader for this attribute. Can be changed after construction.
    */
  def this(usage: Int, numComponents: Int, alias: String) =
    this(usage, numComponents, false, if (usage == Usage.ColorPacked) DataType.UnsignedByte else DataType.Float, alias, 0)

  /** Constructs a new VertexAttribute. The GL data type is automatically selected based on the usage.
    *
    * @param usage
    *   The attribute {@link Usage} , used to select the {@link #type} and for identification.
    * @param numComponents
    *   the number of components of this attribute, must be between 1 and 4.
    * @param alias
    *   the alias used in a shader for this attribute. Can be changed after construction.
    * @param unit
    *   Optional unit/index specifier, used for texture coordinates and bone weights
    */
  def this(usage: Int, numComponents: Int, alias: String, unit: Int) =
    this(
      usage,
      numComponents,
      usage == Usage.ColorPacked,
      if (usage == Usage.ColorPacked) DataType.UnsignedByte else DataType.Float,
      alias,
      unit
    )

  /** Constructs a new VertexAttribute.
    *
    * @param usage
    *   The attribute {@link Usage} , used for identification.
    * @param numComponents
    *   the number of components of this attribute, must be between 1 and 4.
    * @param type
    *   the OpenGL type of each component, e.g. {@link GL20#GL_FLOAT} or {@link GL20#GL_UNSIGNED_BYTE} . Since {@link Mesh} stores vertex data in 32bit floats, the total size of this attribute (type
    *   size times number of components) must be a multiple of four.
    * @param normalized
    *   For fixed types, whether the values are normalized to either -1f and +1f (signed) or 0f and +1f (unsigned)
    * @param alias
    *   The alias used in a shader for this attribute. Can be changed after construction.
    */
  def this(usage: Int, numComponents: Int, `type`: DataType, normalized: Boolean, alias: String) =
    this(usage, numComponents, normalized, `type`, alias, 0)

  /** @return
    *   A copy of this VertexAttribute with the same parameters. The {@link #offset} is not copied and must be recalculated, as is typically done by the {@linkplain VertexAttributes} that owns the
    *   VertexAttribute.
    */
  def copy(): VertexAttribute =
    VertexAttribute(usage, numComponents, normalized, `type`, alias, unit)

  /** Tests to determine if the passed object was created with the same parameters */
  override def equals(obj: Any): Boolean =
    obj match {
      case other: VertexAttribute => equals(other)
      case _ => false
    }

  def equals(other: VertexAttribute): Boolean =
    usage == other.usage && numComponents == other.numComponents && `type` == other.`type` &&
      normalized == other.normalized && alias.equals(other.alias) && unit == other.unit

  /** @return A unique number specifying the usage index (3 MSB) and unit (1 LSB). */
  def key: Int =
    (usageIndex << 8) + (unit & 0xff)

  /** @return How many bytes this attribute uses. */
  def sizeInBytes: Int = {
    val t = `type`.toInt
    if (t == GL20.GL_FLOAT || t == GL20.GL_FIXED) 4 * numComponents
    else if (t == GL20.GL_UNSIGNED_BYTE || t == GL20.GL_BYTE) numComponents
    else if (t == GL20.GL_UNSIGNED_SHORT || t == GL20.GL_SHORT) 2 * numComponents
    else if (t == GL20.GL_UNSIGNED_INT || t == GL20.GL_INT) 4 * numComponents
    else 0
  }

  override def hashCode(): Int = {
    var result = key
    result = 541 * result + numComponents
    result = 541 * result + alias.hashCode()
    result
  }
}

object VertexAttribute {
  def Position(): VertexAttribute =
    VertexAttribute(Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE)

  def TexCoords(unit: Int): VertexAttribute =
    VertexAttribute(Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + unit, unit)

  def Normal(): VertexAttribute =
    VertexAttribute(Usage.Normal, 3, ShaderProgram.NORMAL_ATTRIBUTE)

  def ColorPacked(): VertexAttribute =
    VertexAttribute(Usage.ColorPacked, 4, DataType.UnsignedByte, true, ShaderProgram.COLOR_ATTRIBUTE)

  def ColorUnpacked(): VertexAttribute =
    VertexAttribute(Usage.ColorUnpacked, 4, DataType.Float, false, ShaderProgram.COLOR_ATTRIBUTE)

  def Tangent(): VertexAttribute =
    VertexAttribute(Usage.Tangent, 3, ShaderProgram.TANGENT_ATTRIBUTE)

  def Binormal(): VertexAttribute =
    VertexAttribute(Usage.BiNormal, 3, ShaderProgram.BINORMAL_ATTRIBUTE)

  def BoneWeight(unit: Int): VertexAttribute =
    VertexAttribute(Usage.BoneWeight, 2, ShaderProgram.BONEWEIGHT_ATTRIBUTE + unit, unit)
}
