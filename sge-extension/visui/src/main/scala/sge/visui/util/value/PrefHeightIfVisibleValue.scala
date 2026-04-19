/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 35
 * Covenant-baseline-methods: INSTANCE,PrefHeightIfVisibleValue,get
 * Covenant-source-reference: com/kotcrab/vis/ui/util/value/PrefHeightIfVisibleValue.java
 * Covenant-verified: 2026-04-19
 */
package sge
package visui
package util
package value

import sge.scenes.scene2d.Actor
import sge.scenes.scene2d.ui.{ Table, Value => SceneValue, Widget }
import sge.utils.Nullable

/** Value that returns widget preferred height if it's visible. If widget is invisible then returns 0. This can be only added to classes extending [[Widget]] or [[Table]], if you try to add it to any
  * other class you will get [[IllegalStateException]] during runtime.
  * @author
  *   Kotcrab
  * @since 0.9.3
  */
class PrefHeightIfVisibleValue extends SceneValue {
  override def get(context: Nullable[Actor]): Float = context
    .map {
      case widget: Widget => if (widget.visible) widget.prefHeight else 0f
      case table:  Table  => if (table.visible) table.prefHeight else 0f
      case actor => throw new IllegalStateException("Unsupported actor type for PrefHeightIfVisibleValue: " + actor.getClass)
    }
    .getOrElse(0f)
}

object PrefHeightIfVisibleValue {
  val INSTANCE: PrefHeightIfVisibleValue = new PrefHeightIfVisibleValue()
}
