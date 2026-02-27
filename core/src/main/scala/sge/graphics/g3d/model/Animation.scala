/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/model/Animation.java
 * Original authors: badlogic
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
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
