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

/** Allows to use libGDX [[SceneValue]] with lambdas.
  * @author
  *   Kotcrab
  * @see
  *   [[VisWidgetValue]]
  * @since 0.9.3
  */
class VisValue(getter: VisValue.ValueGetter) extends SceneValue {
  override def get(context: Nullable[Actor]): Float = getter.get(context)
}

object VisValue {
  trait ValueGetter {
    def get(context: Nullable[Actor]): Float
  }
}
