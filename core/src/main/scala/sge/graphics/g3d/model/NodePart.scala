/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/model/NodePart.java
 * Original authors: badlogic, Xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - All public methods/fields match Java source
 * - invBoneBindTransforms/bones: Java nullable -> Nullable[]
 * - setRenderable: wraps material in Nullable() since Renderable.material is Nullable[Material]
 * - set(): uses Nullable fold pattern vs Java null checks (equivalent logic)
 * - Java ArrayMap constructor uses `Node[]::new, Matrix4[]::new` array creators; Scala uses
 *   `ArrayMap[Node, Matrix4](true, size)` — relies on SGE ArrayMap handling
 * - FIXME comment preserved from Java source
 * - Status: pass
 */
package sge
package graphics
package g3d
package model

import sge.math.Matrix4
import sge.utils.{ ArrayMap, Nullable }

/** A combination of {@link MeshPart} and {@link Material}, used to represent a {@link Node}'s graphical properties. A NodePart is the smallest visible part of a {@link Model}, each NodePart implies a
  * render call.
  * @author
  *   badlogic, Xoppa
  */
class NodePart {

  /** The MeshPart (shape) to render. Must not be null. */
  var meshPart: MeshPart = scala.compiletime.uninitialized

  /** The Material used to render the {@link #meshPart}. Must not be null. */
  var material: Material = scala.compiletime.uninitialized

  /** Mapping to each bone (node) and the inverse transform of the bind pose. Will be used to fill the {@link #bones} array. May be null.
    */
  var invBoneBindTransforms: Nullable[ArrayMap[Node, Matrix4]] = Nullable.empty

  /** The current transformation (relative to the bind pose) of each bone, may be null. When the part is skinned, this will be updated by a call to {@link ModelInstance#calculateTransforms()}. Do not
    * set or change this value manually.
    */
  var bones: Nullable[Array[Matrix4]] = Nullable.empty

  /** true by default. If set to false, this part will not participate in rendering and bounding box calculation. */
  var enabled: Boolean = true

  /** Construct a new NodePart referencing the provided {@link MeshPart} and {@link Material}.
    * @param meshPart
    *   The MeshPart to reference.
    * @param material
    *   The Material to reference.
    */
  def this(meshPart: MeshPart, material: Material) = {
    this()
    this.meshPart = meshPart
    this.material = material
  }

  // FIXME add copy constructor and override #equals.

  /** Convenience method to set the material, mesh, meshPartOffset, meshPartSize, primitiveType and bones members of the specified Renderable. The other member of the provided {@link Renderable}
    * remain untouched. Note that the material, mesh and bones members are referenced, not copied. Any changes made to those objects will be reflected in both the NodePart and Renderable object.
    * @param out
    *   The Renderable of which to set the members to the values of this NodePart.
    */
  def setRenderable(out: Renderable): Renderable = {
    out.material = Nullable(material)
    out.meshPart.set(meshPart)
    out.bones = bones
    out
  }

  def copy(): NodePart = new NodePart().set(this)

  protected def set(other: NodePart): NodePart = {
    meshPart = new MeshPart(other.meshPart)
    material = other.material
    enabled = other.enabled
    other.invBoneBindTransforms.fold {
      invBoneBindTransforms = Nullable.empty
      bones = Nullable.empty
    } { otherBindTransforms =>
      val map = invBoneBindTransforms.fold {
        ArrayMap[Node, Matrix4](true, otherBindTransforms.size)
      } { existing =>
        existing.clear()
        existing
      }
      map.putAll(otherBindTransforms)
      invBoneBindTransforms = Nullable(map)

      val neededSize = map.size
      val boneArray  = bones.fold(new Array[Matrix4](neededSize)) { existing =>
        if (existing.length != neededSize) new Array[Matrix4](neededSize) else existing
      }
      for (i <- boneArray.indices)
        if (Nullable(boneArray(i)).isEmpty) boneArray(i) = new Matrix4()
      bones = Nullable(boneArray)
    }
    this
  }
}
