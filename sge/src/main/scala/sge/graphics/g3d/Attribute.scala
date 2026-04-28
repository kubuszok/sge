/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/Attribute.java
 * Original authors: Xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   - Comparable -> Ordered[Attribute].
 *   - register() visibility: Java protected -> Scala private[g3d] (callers are companion objects).
 *   - getAttributeAlias returns Nullable[String] (Java returns null).
 *   - toString uses .getOrElse("unknown") instead of returning null.
 *   - equals(Any) handles null via pattern match (no bare null check).
 *   - Audit: pass (2026-03-03)
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 107
 * Covenant-baseline-methods: Attribute,MAX_ATTRIBUTE_COUNT,copy,equals,getAttributeAlias,getAttributeType,hashCode,i,idx,register,result,toString,typeBit,types
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/Attribute.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 6e3aa711f82fdf44f0fad645e248f84cc0734548
 */
package sge
package graphics
package g3d

import scala.util.boundary
import scala.util.boundary.break

import sge.utils.{ DynamicArray, Nullable, SgeError }

/** Extend this class to implement a material attribute. Register the attribute type by statically calling the [[Attribute.register]] method, whose return value should be used to instantiate the
  * attribute. A class can implement multiple types
  * @author
  *   Xoppa (original implementation)
  */
abstract class Attribute(
  /** The type of this attribute */
  val `type`: Long
) extends Ordered[Attribute] {

  private val typeBit: Int = java.lang.Long.numberOfTrailingZeros(`type`)

  /** @return An exact copy of this attribute */
  def copy(): Attribute

  protected def equals(other: Attribute): Boolean =
    other.hashCode() == hashCode()

  override def equals(obj: Any): Boolean =
    obj match {
      case null                                      => false
      case same if same.asInstanceOf[AnyRef] eq this => true
      case other: Attribute =>
        if (this.`type` != other.`type`) false
        else equals(other)
      case _ => false
    }

  override def toString: String =
    Attribute.getAttributeAlias(`type`).getOrElse("unknown")

  override def hashCode(): Int =
    7489 * typeBit
}

object Attribute {

  /** The registered type aliases */
  private val types: DynamicArray[String] = DynamicArray[String]()

  /** The long bitmask is limited to 64 bits */
  private val MAX_ATTRIBUTE_COUNT: Int = 64

  /** @return The ID of the specified attribute type, or zero if not available */
  def getAttributeType(alias: String): Long = boundary {
    var i = 0
    while (i < types.size) {
      if (types(i).compareTo(alias) == 0) break(1L << i)
      i += 1
    }
    0L
  }

  /** @return The alias of the specified attribute type, or null if not available. */
  def getAttributeAlias(`type`: Long): Nullable[String] = {
    var idx = -1
    while (`type` != 0 && { idx += 1; idx } < 63 && (((`type` >> idx) & 1) == 0)) ()
    if (idx >= 0 && idx < types.size) Nullable(types(idx))
    else Nullable.empty
  }

  /** Call this method to register a custom attribute type, see the wiki for an example. If the alias already exists, then that ID will be reused. The alias should be unambiguously and will by default
    * be returned by the call to [[toString]]. A maximum of 64 attributes can be registered as a long bitmask can only hold 64 bits.
    * @param alias
    *   The alias of the type to register, must be different for each direct type, will be used for debugging
    * @return
    *   the ID of the newly registered type, or the ID of the existing type if the alias was already registered
    * @throws SgeError.InvalidInput
    *   if maximum attribute count reached
    */
  private[sge] def register(alias: String): Long = {
    val result = getAttributeType(alias)
    if (result > 0) result
    else {
      if (types.size >= MAX_ATTRIBUTE_COUNT) {
        throw SgeError.InvalidInput("Cannot register " + alias + ", maximum registered attribute count reached.")
      }
      types.add(alias)
      1L << (types.size - 1)
    }
  }
}
