/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/utils/BaseDrawable.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - toString uses getClass.getSimpleName (Java uses ClassReflection.getSimpleName)
 * - name field: @Null String -> Nullable[String]
 * - All methods faithfully ported, no API changes
 * - Renames: getLeftWidth→leftWidth, setLeftWidth→leftWidth_=, etc.; getName→name (public var)
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 55
 * Covenant-baseline-methods: BaseDrawable,bottomHeight,draw,leftWidth,minHeight,minWidth,name,rightWidth,this,toString,topHeight
 * Covenant-source-reference: com/badlogic/gdx/scenes/scene2d/utils/BaseDrawable.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 6cc6837a90cf1033741695a9ff8fe42a06c4f0fe
 */
package sge
package scenes
package scene2d
package utils

import sge.graphics.g2d.Batch
import sge.utils.Nullable

/** Drawable that stores the size information but doesn't draw anything.
  * @author
  *   Nathan Sweet
  */
class BaseDrawable() extends Drawable {
  var name:         Nullable[String] = Nullable.empty
  var leftWidth:    Float            = 0
  var rightWidth:   Float            = 0
  var topHeight:    Float            = 0
  var bottomHeight: Float            = 0
  var minWidth:     Float            = 0
  var minHeight:    Float            = 0

  /** Creates a new empty drawable with the same sizing information as the specified drawable. */
  def this(drawable: Drawable) = {
    this()
    drawable match {
      case bd: BaseDrawable => name = bd.name
      case _ =>
    }
    leftWidth = drawable.leftWidth
    rightWidth = drawable.rightWidth
    topHeight = drawable.topHeight
    bottomHeight = drawable.bottomHeight
    minWidth = drawable.minWidth
    minHeight = drawable.minHeight
  }

  def draw(batch: Batch, x: Float, y: Float, width: Float, height: Float): Unit = {}

  override def toString: String =
    name.getOrElse(getClass.getSimpleName)
}
