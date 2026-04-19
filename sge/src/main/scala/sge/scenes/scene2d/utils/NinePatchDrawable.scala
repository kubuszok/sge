/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/utils/NinePatchDrawable.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - Copy constructor: cannot call super(drawable) -> manual property copy
 * - setPatch null-check -> Nullable(patch).foreach
 * - All methods faithfully ported
 * - Renames: setPatch kept (has logic); getLeftWidth→leftWidth etc. via Drawable trait rename
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 82
 * Covenant-baseline-methods: NinePatchDrawable,_patch,draw,drawable,patch,setPatch,this,tint
 * Covenant-source-reference: com/badlogic/gdx/scenes/scene2d/utils/NinePatchDrawable.java
 * Covenant-verified: 2026-04-19
 */
package sge
package scenes
package scene2d
package utils

import sge.graphics.Color
import sge.graphics.g2d.{ Batch, NinePatch }
import sge.utils.Nullable

/** Drawable for a {@link NinePatch}. <p> The drawable sizes are set when the ninepatch is set, but they are separate values. Eg, {@link Drawable#getLeftWidth()} could be set to more than
  * {@link NinePatch#getLeftWidth()} in order to provide more space on the left than actually exists in the ninepatch. <p> The min size is set to the ninepatch total size by default. It could be set
  * to the left+right and top+bottom, excluding the middle size, to allow the drawable to be sized down as small as possible.
  * @author
  *   Nathan Sweet
  */
class NinePatchDrawable() extends BaseDrawable with TransformDrawable {
  private var _patch: NinePatch = scala.compiletime.uninitialized

  def this(patch: NinePatch) = {
    this()
    setPatch(patch)
  }

  def this(drawable: NinePatchDrawable) = {
    this()
    // Call super copy manually
    drawable match {
      case bd: BaseDrawable =>
        leftWidth = bd.leftWidth
        rightWidth = bd.rightWidth
        topHeight = bd.topHeight
        bottomHeight = bd.bottomHeight
        minWidth = bd.minWidth
        minHeight = bd.minHeight
        name = bd.name
    }
    this._patch = drawable.patch
  }

  override def draw(batch: Batch, x: Float, y: Float, width: Float, height: Float): Unit =
    _patch.draw(batch, x, y, width, height)

  override def draw(batch: Batch, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float): Unit =
    _patch.draw(batch, x, y, originX, originY, width, height, scaleX, scaleY, rotation)

  /** Sets this drawable's ninepatch and set the min width, min height, top height, right width, bottom height, and left width to the patch's padding.
    */
  def setPatch(patch: NinePatch): Unit = {
    this._patch = patch
    Nullable(patch).foreach { p =>
      minWidth = p.totalWidth
      minHeight = p.totalHeight
      topHeight = p.padTop
      rightWidth = p.padRight
      bottomHeight = p.padBottom
      leftWidth = p.padLeft
    }
  }

  def patch: NinePatch = _patch

  /** Creates a new drawable that renders the same as this drawable tinted the specified color. */
  def tint(tint: Color): NinePatchDrawable = {
    val drawable = NinePatchDrawable(this)
    drawable.setPatch(NinePatch(drawable.patch, tint))
    drawable
  }
}
