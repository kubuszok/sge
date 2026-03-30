/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package visui
package util
package value

import sge.scenes.scene2d.Actor
import sge.scenes.scene2d.ui.{ Value => SceneValue }
import sge.utils.Nullable

/** Value that returns given fixed constant value if widget is visible. If actor is invisible then returns 0.
  * @author
  *   Kotcrab
  * @since 1.1.0
  */
class ConstantIfVisibleValue(private val overrideActor: Nullable[Actor], constant: Float) extends SceneValue {

  def this(constant: Float) = this(Nullable.empty, constant)

  def this(actor: Actor, constant: Float) = this(Nullable(actor), constant)

  override def get(context: Nullable[Actor]): Float = {
    val target = if (overrideActor.isDefined) overrideActor else context
    target.map(a => if (a.visible) constant else 0f).getOrElse(0f)
  }
}
