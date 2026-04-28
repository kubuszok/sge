/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 46
 * Covenant-baseline-methods: CursorManager,_defaultCursor,_defaultSystemCursor,_systemCursorAsDefault,restoreDefaultCursor,setDefaultCursor
 * Covenant-source-reference: com/kotcrab/vis/ui/util/CursorManager.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
 */
package sge
package visui
package util

import sge.graphics.Cursor
import sge.utils.Nullable

/** Manages default cursor of VisUI app. If you are using custom cursor you must set it here otherwise some VisUI widget that changes cursors will reset it to default system cursor.
  * @author
  *   Kotcrab
  * @since 1.1.2
  */
object CursorManager {
  private var _defaultCursor:         Nullable[Cursor]    = Nullable.empty
  private var _defaultSystemCursor:   Cursor.SystemCursor = Cursor.SystemCursor.Arrow
  private var _systemCursorAsDefault: Boolean             = true

  /** Sets cursor that will be used as default when [[restoreDefaultCursor]] is called, for example by VisUI widget. */
  def setDefaultCursor(defaultCursor: Cursor): Unit = {
    require(defaultCursor != null, "defaultCursor can't be null") // @nowarn -- Java interop boundary
    _defaultCursor = Nullable(defaultCursor)
    _systemCursorAsDefault = false
  }

  /** Sets cursor that will be used as default when [[restoreDefaultCursor]] is called, for example by Vis widget. */
  def setDefaultCursor(defaultCursor: Cursor.SystemCursor): Unit = {
    require(defaultCursor != null, "defaultCursor can't be null") // @nowarn -- Java interop boundary
    _defaultSystemCursor = defaultCursor
    _systemCursorAsDefault = true
  }

  /** Restores currently used cursor to default one. */
  def restoreDefaultCursor()(using Sge): Unit =
    if (_systemCursorAsDefault) {
      Sge().graphics.setSystemCursor(_defaultSystemCursor)
    } else {
      Sge().graphics.setCursor(_defaultCursor.get)
    }
}
