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
import sge.scenes.scene2d.ui.{ Value => SceneValue, Widget }
import sge.utils.Nullable

/** Allows to use libGDX [[SceneValue]] with lambdas for scene2d.ui widgets. Note that this cannot be added to actors, only widgets are supported, if you try to do so you will get
  * [[ClassCastException]] when this Value has been invoked.
  * @author
  *   Kotcrab
  * @see
  *   [[VisValue]]
  * @see
  *   [[PrefHeightIfVisibleValue]]
  * @since 0.9.3
  */
class VisWidgetValue(protected val getter: VisWidgetValue.WidgetValueGetter) extends SceneValue {
  override def get(context: Nullable[Actor]): Float =
    getter.get(context.get.asInstanceOf[Widget])
}

object VisWidgetValue {
  trait WidgetValueGetter {
    def get(context: Widget): Float
  }
}
