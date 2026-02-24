/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/VertexAttribute.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
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
  val `type`: Int,
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
  def this(usage: Int, numComponents: Int, alias: String) = {
    this(usage, numComponents, false, if (usage == Usage.ColorPacked) GL20.GL_UNSIGNED_BYTE else GL20.GL_FLOAT, alias, 0)
  }

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
  def this(usage: Int, numComponents: Int, alias: String, unit: Int) = {
    this(
      usage,
      numComponents,
      usage == Usage.ColorPacked,
      if (usage == Usage.ColorPacked) GL20.GL_UNSIGNED_BYTE else GL20.GL_FLOAT,
      alias,
      unit
    )
  }

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
  def this(usage: Int, numComponents: Int, `type`: Int, normalized: Boolean, alias: String) = {
    this(usage, numComponents, normalized, `type`, alias, 0)
  }

  /** @return
    *   A copy of this VertexAttribute with the same parameters. The {@link #offset} is not copied and must be recalculated, as is typically done by the {@linkplain VertexAttributes} that owns the
    *   VertexAttribute.
    */
  def copy(): VertexAttribute =
    new VertexAttribute(usage, numComponents, normalized, `type`, alias, unit)

  /** Tests to determine if the passed object was created with the same parameters */
  override def equals(obj: Any): Boolean =
    obj match {
      case other: VertexAttribute => equals(other)
      case _ => false
    }

  def equals(other: VertexAttribute): Boolean =
    other != null && usage == other.usage && numComponents == other.numComponents && `type` == other.`type` &&
      normalized == other.normalized && alias.equals(other.alias) && unit == other.unit

  /** @return A unique number specifying the usage index (3 MSB) and unit (1 LSB). */
  def getKey(): Int =
    (usageIndex << 8) + (unit & 0xff)

  /** @return How many bytes this attribute uses. */
  def getSizeInBytes(): Int =
    `type` match {
      case GL20.GL_FLOAT | GL20.GL_FIXED          => 4 * numComponents
      case GL20.GL_UNSIGNED_BYTE | GL20.GL_BYTE   => numComponents
      case GL20.GL_UNSIGNED_SHORT | GL20.GL_SHORT => 2 * numComponents
      case _                                      => 0
    }

  override def hashCode(): Int = {
    var result = getKey()
    result = 541 * result + numComponents
    result = 541 * result + alias.hashCode()
    result
  }
}

object VertexAttribute {
  def Position(): VertexAttribute =
    new VertexAttribute(Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE)

  def TexCoords(unit: Int): VertexAttribute =
    new VertexAttribute(Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + unit, unit)

  def Normal(): VertexAttribute =
    new VertexAttribute(Usage.Normal, 3, ShaderProgram.NORMAL_ATTRIBUTE)

  def ColorPacked(): VertexAttribute =
    new VertexAttribute(Usage.ColorPacked, 4, GL20.GL_UNSIGNED_BYTE, true, ShaderProgram.COLOR_ATTRIBUTE)

  def ColorUnpacked(): VertexAttribute =
    new VertexAttribute(Usage.ColorUnpacked, 4, GL20.GL_FLOAT, false, ShaderProgram.COLOR_ATTRIBUTE)

  def Tangent(): VertexAttribute =
    new VertexAttribute(Usage.Tangent, 3, ShaderProgram.TANGENT_ATTRIBUTE)

  def Binormal(): VertexAttribute =
    new VertexAttribute(Usage.BiNormal, 3, ShaderProgram.BINORMAL_ATTRIBUTE)

  def BoneWeight(unit: Int): VertexAttribute =
    new VertexAttribute(Usage.BoneWeight, 2, ShaderProgram.BONEWEIGHT_ATTRIBUTE + unit, unit)
}
