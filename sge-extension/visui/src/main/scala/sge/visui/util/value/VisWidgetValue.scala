/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 36
 * Covenant-baseline-methods: VisWidgetValue,WidgetValueGetter,get
 * Covenant-source-reference: com/kotcrab/vis/ui/util/value/VisWidgetValue.java
 * Covenant-verified: 2026-04-19
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
