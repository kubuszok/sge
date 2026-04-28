/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 24
 * Covenant-baseline-methods: ColorPickerAdapter,canceled,changed,finished,reset
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/color/ColorPickerAdapter.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
 */
package sge
package visui
package widget
package color

import sge.graphics.Color

/** Empty implementation of [[ColorPickerListener]].
  * @author
  *   Kotcrab
  */
class ColorPickerAdapter extends ColorPickerListener {
  override def canceled(oldColor:   Color):                  Unit = ()
  override def changed(newColor:    Color):                  Unit = ()
  override def reset(previousColor: Color, newColor: Color): Unit = ()
  override def finished(newColor:   Color):                  Unit = ()
}
