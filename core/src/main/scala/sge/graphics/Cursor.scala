/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/Cursor.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics

/** <p> Represents a mouse cursor. Create a cursor via {@link Graphics#newCursor(Pixmap, int, int)} . To set the cursor use {@link Graphics#setCursor(Cursor)} . To use one of the system cursors, call
  * Graphics#setSystemCursor </p>
  */
trait Cursor extends AutoCloseable

object Cursor {

  enum SystemCursor {
    case Arrow, Ibeam, Crosshair, Hand, HorizontalResize, VerticalResize, NWSEResize, NESWResize, AllResize, NotAllowed, None
  }
}
