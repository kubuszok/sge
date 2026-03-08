/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backends-gwt/.../GwtCursor.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: GwtCursor -> BrowserCursor
 *   Convention: CSS cursor property; system cursor mapping to CSS names
 *   Convention: custom Pixmap cursor deferred (needs browser-specific canvas Pixmap)
 *   Idiom: split packages
 *   Audited: 2026-03-08
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge

import sge.graphics.Cursor

/** Browser [[Cursor]] implementation using CSS cursor properties.
  *
  * System cursors are mapped to standard CSS cursor names. Custom pixmap cursors are deferred until a browser-specific Pixmap (backed by HTMLCanvasElement) is available.
  *
  * @param cssCursorProperty
  *   the CSS cursor value (e.g. "default", "pointer", "crosshair")
  */
class BrowserCursor(val cssCursorProperty: String) extends Cursor {
  override def close(): Unit = () // nothing to dispose for CSS cursors
}

object BrowserCursor {

  /** Returns the CSS cursor name for a [[Cursor.SystemCursor]].
    *
    * @param systemCursor
    *   the system cursor to map
    * @return
    *   the corresponding CSS cursor property value
    */
  def cssNameFor(systemCursor: Cursor.SystemCursor): String = systemCursor match {
    case Cursor.SystemCursor.Arrow            => "default"
    case Cursor.SystemCursor.Crosshair        => "crosshair"
    case Cursor.SystemCursor.Hand             => "pointer" // Don't use 'hand'; it's a non-standard holdover from IE5
    case Cursor.SystemCursor.HorizontalResize => "ew-resize"
    case Cursor.SystemCursor.VerticalResize   => "ns-resize"
    case Cursor.SystemCursor.Ibeam            => "text"
    case Cursor.SystemCursor.NWSEResize       => "nwse-resize"
    case Cursor.SystemCursor.NESWResize       => "nesw-resize"
    case Cursor.SystemCursor.AllResize        => "move"
    case Cursor.SystemCursor.NotAllowed       => "not-allowed"
    case Cursor.SystemCursor.None             => "none"
  }
}
