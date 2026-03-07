/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/decals/GroupStrategy.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes (audited 2026-03-03):
 * - Java interface -> Scala trait: correct
 * - getGroupShader returns Nullable[ShaderProgram] (was nullable ShaderProgram): correct
 * - Array<Decal> -> DynamicArray[Decal]: correct
 * - All 6 methods faithfully ported with matching signatures
 * - Status: pass
 */
package sge
package graphics
package g3d
package decals

import sge.graphics.glutils.ShaderProgram
import sge.utils.DynamicArray
import sge.utils.Nullable

/** <p> This class provides hooks which are invoked by {@link DecalBatch} to evaluate the group a sprite falls into, as well as to adjust settings before and after rendering a group. </p> <p> A group
  * is identified by an integer. The {@link #beforeGroup(int, DynamicArray) beforeGroup()} method provides the strategy with a list of all the decals, which are contained in the group itself, and will
  * be rendered before the associated call to {@link #afterGroup(int)}.<br/> A call to {@code beforeGroup()} is always followed by a call to {@code afterGroup()}.<br/> <b>Groups are always invoked
  * based on their ascending int values</b>. Group -10 will be rendered before group -5, group -5 before group 0, group 0 before group 6 and so on.<br/> The call order for a single flush is always
  * {@code beforeGroups(), beforeGroup1(), afterGroup1(), ... beforeGroupN(), afterGroupN(), afterGroups()}. </p> <p> The contents of the {@code beforeGroup()} call can be modified at will to realize
  * view frustum culling, material & depth sorting, ... all based on the requirements of the current group. The batch itself does not change OpenGL settings except for whichever changes are entailed
  * {@link DecalMaterial#set()}. If the group requires a special shader, blending, {@link #getGroupShader(int)} should return it so that DecalBatch can apply it while rendering the group. </p>
  */
trait GroupStrategy {

  /** Returns the shader to be used for the group. Can be Nullable.empty in which case the GroupStrategy doesn't support GLES 2.0
    * @param group
    *   the group
    * @return
    *   the {@link ShaderProgram}
    */
  def getGroupShader(group: Int): Nullable[ShaderProgram]

  /** Assigns a group to a decal
    *
    * @param decal
    *   Decal to assign group to
    * @return
    *   group assigned
    */
  def decideGroup(decal: Decal): Int

  /** Invoked directly before rendering the contents of a group
    *
    * @param group
    *   Group that will be rendered
    * @param contents
    *   Array of entries of arrays containing all the decals in the group
    */
  def beforeGroup(group: Int, contents: DynamicArray[Decal]): Unit

  /** Invoked directly after rendering of a group has completed
    *
    * @param group
    *   Group which completed rendering
    */
  def afterGroup(group: Int): Unit

  /** Invoked before rendering any group */
  def beforeGroups(): Unit

  /** Invoked after having rendered all groups */
  def afterGroups(): Unit
}
