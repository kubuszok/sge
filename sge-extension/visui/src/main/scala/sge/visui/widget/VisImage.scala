/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package visui
package widget

import sge.graphics.Texture
import sge.graphics.g2d.{ NinePatch, TextureRegion }
import sge.scenes.scene2d.ui.Image
import sge.scenes.scene2d.utils.{ Drawable, NinePatchDrawable, TextureRegionDrawable }
import sge.utils.{ Align, Nullable, Scaling }

/** Compatible with [[Image]]. Does not provide additional features.
  * @author
  *   Kotcrab
  * @see
  *   [[Image]]
  */
class VisImage(initialDrawable: Nullable[Drawable], initialScaling: Scaling, initialAlign: Align)(using Sge) extends Image(initialDrawable, initialScaling, initialAlign) {

  def this()(using Sge) = this(Nullable.empty, Scaling.stretch, Align.center)

  def this(patch: NinePatch)(using Sge) = this(Nullable[Drawable](new NinePatchDrawable(patch)), Scaling.stretch, Align.center)

  def this(region: TextureRegion)(using Sge) = this(Nullable[Drawable](new TextureRegionDrawable(region)), Scaling.stretch, Align.center)

  def this(texture: Texture)(using Sge) = this(Nullable[Drawable](new TextureRegionDrawable(new TextureRegion(texture))), Scaling.stretch, Align.center)

  def this(drawableName: String)(using Sge) = this(Nullable[Drawable](VisUI.getSkin.getDrawable(drawableName)), Scaling.stretch, Align.center)

  def this(d: Drawable)(using Sge) = this(Nullable[Drawable](d), Scaling.stretch, Align.center)

  def this(d: Drawable, s: Scaling)(using Sge) = this(Nullable[Drawable](d), s, Align.center)

  def setDrawableFromTexture(texture: Texture): Unit =
    this.drawable = Nullable[Drawable](new TextureRegionDrawable(new TextureRegion(texture)))
}
