/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/model/Animation.java
 * Original authors: badlogic
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - All fields match Java: id (String), duration (float), nodeAnimations (Array -> DynamicArray)
 * - `id` uses scala.compiletime.uninitialized (Java null default)
 * - `duration` initialized to 0f (matches Java default)
 * - Array -> DynamicArray is standard SGE collection mapping
 * - No API differences from Java source
 * - Status: pass
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 40
 * Covenant-baseline-methods: Animation,duration,id,nodeAnimations
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/model/Animation.java
 * Covenant-verified: 2026-04-19
 */
package sge
package graphics
package g3d
package model

import sge.utils.DynamicArray

/** An Animation has an id and a list of {@link NodeAnimation} instances. Each NodeAnimation animates a single {@link Node} in the {@link Model}. Every {@link NodeAnimation} is assumed to have the
  * same amount of keyframes, at the same timestamps, as all other node animations for faster keyframe searches.
  *
  * @author
  *   badlogic
  */
class Animation {

  /** the unique id of the animation * */
  var id: String = scala.compiletime.uninitialized

  /** the duration in seconds * */
  var duration: Float = 0f

  /** the animation curves for individual nodes * */
  var nodeAnimations: DynamicArray[NodeAnimation] = DynamicArray[NodeAnimation]()
}
