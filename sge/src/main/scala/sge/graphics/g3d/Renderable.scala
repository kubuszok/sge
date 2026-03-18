/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/Renderable.java
 * Original authors: badlogic, xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   - All fields match Java source. null -> Nullable.
 *   - worldTransform and meshPart are val (final in Java).
 *   - material, environment, bones, shader, userData are Nullable.
 *   - set() returns this (no explicit return).
 *   - Audit: pass (2026-03-03)
 */
package sge
package graphics
package g3d

import sge.graphics.g3d.model.MeshPart
import sge.math.Matrix4
import sge.utils.Nullable

/** A Renderable contains all information about a single render instruction (typically a draw call).
  *
  * It defines what (the shape), how (the material) and where (the transform) should be rendered by which shader.
  *
  * The shape is defined using the mesh, meshPartOffset, meshPartSize and primitiveType members. This matches the members of the [[MeshPart]] class. The meshPartOffset is used to specify the offset
  * within the mesh and the meshPartSize is used to specify the part (in total number of vertices) to render. If the mesh is indexed (which is when [[Mesh.numIndices]] > 0) then both values are in
  * number of indices within the indices array of the mesh, otherwise they are in number of vertices within the vertices array of the mesh. Note that some classes might require the mesh to be indexed.
  *
  * The [[material]] and (optional) [[environment]] values are combined to specify how the shape should look like. Typically these are used to specify uniform values or other OpenGL state changes.
  * When a value is present in both the [[material]] and [[environment]], then the value of the [[material]] will be used.
  *
  * Renderables can be rendered directly using a [[Shader]] (in which case the [[shader]] member is ignored). Though more typically Renderables are rendered via a ModelBatch, either directly, or by
  * passing a [[RenderableProvider]] like ModelInstance to the RenderBatch.
  *
  * A ModelInstance returns all Renderables via its getRenderables method. In which case the value of ModelInstance.userData will be set to the [[userData]] member. The [[userData]] member can be used
  * to pass additional data to the shader. However, in most scenarios it is advised to use the [[material]] or [[environment]] member with custom [[Attribute]]s to pass data to the shader.
  *
  * In some cases, (for example for non-hierarchical basic game objects requiring only a single draw call) it is possible to extend the Renderable class and add additional fields to pass to the
  * shader. While extending the Renderable class can be useful, the shader should not rely on it. Similar to the [[userData]] member it is advised to use the [[material]] and [[environment]] members
  * to pass data to the shader.
  *
  * When using a ModelBatch to render a Renderable, The Renderable and all its values must not be changed in between the call to ModelBatch.begin and ModelBatch.end. Therefore Renderable instances
  * cannot be reused for multiple render calls.
  *
  * When the [[shader]] member of the Renderable is set, the ShaderProvider of the ModelBatch may decide to use that shader instead of the default shader. Therefore, to assure the default shader is
  * used, the [[shader]] member must be set to Nullable.empty.
  *
  * @author
  *   badlogic, xoppa
  */
class Renderable {

  /** Used to specify the transformations (like translation, scale and rotation) to apply to the shape. In other words: it is used to transform the vertices from model space into world space.
    */
  val worldTransform: Matrix4 = Matrix4()

  /** The [[MeshPart]] that contains the shape to render */
  val meshPart: MeshPart = MeshPart()

  /** The [[Material]] to be applied to the shape (part of the mesh), must not be null.
    * @see
    *   [[environment]]
    */
  var material: Nullable[Material] = Nullable.empty

  /** The [[Environment]] to be used to render this Renderable, may be null. When specified it will be combined by the shader with the [[material]]. When both the material and environment contain an
    * attribute of the same type, the attribute of the material will be used.
    */
  var environment: Nullable[Environment] = Nullable.empty

  /** The bone transformations used for skinning, or null if not applicable. When specified and the mesh contains one or more [[sge.graphics.VertexAttributes.Usage.BoneWeight]] vertex attributes, then
    * the BoneWeight index is used as index in the array. If the array isn't large enough then the identity matrix is used. Each BoneWeight weight is used to combine multiple bones into a single
    * transformation matrix, which is used to transform the vertex to model space. In other words: the bone transformation is applied prior to the [[worldTransform]].
    */
  var bones: Nullable[Array[Matrix4]] = Nullable.empty

  /** The [[Shader]] to be used to render this Renderable using a ModelBatch, may be null. It is not guaranteed that the shader will be used, the used ShaderProvider is responsible for actually
    * choosing the correct shader to use.
    */
  var shader: Nullable[Shader] = Nullable.empty

  /** User definable value, may be null. */
  var userData: Nullable[Any] = Nullable.empty

  def set(renderable: Renderable): Renderable = {
    worldTransform.set(renderable.worldTransform)
    material = renderable.material
    meshPart.set(renderable.meshPart)
    bones = renderable.bones
    environment = renderable.environment
    shader = renderable.shader
    userData = renderable.userData
    this
  }
}
