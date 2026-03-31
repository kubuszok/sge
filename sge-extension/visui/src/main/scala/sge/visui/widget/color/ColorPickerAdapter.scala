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
