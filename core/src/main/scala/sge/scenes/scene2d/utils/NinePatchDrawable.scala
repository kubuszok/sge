/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/utils/NinePatchDrawable.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package scenes
package scene2d
package utils

import sge.graphics.Color
import sge.graphics.g2d.{ Batch, NinePatch }

/** Drawable for a {@link NinePatch}. <p> The drawable sizes are set when the ninepatch is set, but they are separate values. Eg, {@link Drawable#getLeftWidth()} could be set to more than
  * {@link NinePatch#getLeftWidth()} in order to provide more space on the left than actually exists in the ninepatch. <p> The min size is set to the ninepatch total size by default. It could be set
  * to the left+right and top+bottom, excluding the middle size, to allow the drawable to be sized down as small as possible.
  * @author
  *   Nathan Sweet
  */
class NinePatchDrawable() extends BaseDrawable with TransformDrawable {
  private var patch: NinePatch = scala.compiletime.uninitialized

  def this(patch: NinePatch) = {
    this()
    setPatch(patch)
  }

  def this(drawable: NinePatchDrawable) = {
    this()
    // Call super copy manually
    drawable match {
      case bd: BaseDrawable =>
        setLeftWidth(bd.getLeftWidth)
        setRightWidth(bd.getRightWidth)
        setTopHeight(bd.getTopHeight)
        setBottomHeight(bd.getBottomHeight)
        setMinWidth(bd.getMinWidth)
        setMinHeight(bd.getMinHeight)
        setName(bd.getName)
    }
    this.patch = drawable.patch
  }

  override def draw(batch: Batch, x: Float, y: Float, width: Float, height: Float): Unit =
    patch.draw(batch, x, y, width, height)

  override def draw(batch: Batch, x: Float, y: Float, originX: Float, originY: Float, width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float): Unit =
    patch.draw(batch, x, y, originX, originY, width, height, scaleX, scaleY, rotation)

  /** Sets this drawable's ninepatch and set the min width, min height, top height, right width, bottom height, and left width to the patch's padding.
    */
  def setPatch(patch: NinePatch): Unit = {
    this.patch = patch
    if (patch != null) {
      setMinWidth(patch.getTotalWidth())
      setMinHeight(patch.getTotalHeight())
      setTopHeight(patch.getPadTop())
      setRightWidth(patch.getPadRight())
      setBottomHeight(patch.getPadBottom())
      setLeftWidth(patch.getPadLeft())
    }
  }

  def getPatch: NinePatch = patch

  /** Creates a new drawable that renders the same as this drawable tinted the specified color. */
  def tint(tint: Color): NinePatchDrawable = {
    val drawable = new NinePatchDrawable(this)
    drawable.patch = new NinePatch(drawable.getPatch, tint)
    drawable
  }
}
