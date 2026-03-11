// SGE — Android cursor implementation
//
// Maps system cursor type ordinals to Android PointerIcon types.
// Requires Android N (API 24) or later for PointerIcon support.
//
// Migration notes:
//   Source:  com.badlogic.gdx.backends.android.AndroidCursor
//   Renames: AndroidCursor → AndroidCursorImpl
//   Convention: ops interface pattern; _root_.android.* imports; Build.VERSION checks
//   Audited: 2026-03-08

package sge
package platform
package android

import _root_.android.os.Build
import _root_.android.view.{ PointerIcon, View }

object AndroidCursorImpl extends CursorOps {

  override def setSystemCursor(view: AnyRef, cursorType: Int): Unit =
    if (Build.VERSION.SDK_INT >= 24) { // Build.VERSION_CODES.N
      val androidView = view.asInstanceOf[View]
      val iconType    = cursorType match {
        case CursorOps.Arrow            => PointerIcon.TYPE_ARROW
        case CursorOps.Ibeam            => PointerIcon.TYPE_TEXT
        case CursorOps.Crosshair        => PointerIcon.TYPE_CROSSHAIR
        case CursorOps.Hand             => PointerIcon.TYPE_HAND
        case CursorOps.HorizontalResize => PointerIcon.TYPE_HORIZONTAL_DOUBLE_ARROW
        case CursorOps.VerticalResize   => PointerIcon.TYPE_VERTICAL_DOUBLE_ARROW
        case CursorOps.NWSEResize       => PointerIcon.TYPE_TOP_LEFT_DIAGONAL_DOUBLE_ARROW
        case CursorOps.NESWResize       => PointerIcon.TYPE_TOP_RIGHT_DIAGONAL_DOUBLE_ARROW
        case CursorOps.AllResize        => PointerIcon.TYPE_ALL_SCROLL
        case CursorOps.NotAllowed       => PointerIcon.TYPE_NO_DROP
        case CursorOps.None             => PointerIcon.TYPE_NULL
        case other                      => throw new IllegalArgumentException(s"Unknown cursor type: $other")
      }
      androidView.setPointerIcon(PointerIcon.getSystemIcon(androidView.getContext, iconType))
    }
}
