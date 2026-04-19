/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/Cursor.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: Disposable → AutoCloseable; SystemCursor as enum
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 27
 * Covenant-baseline-methods: Cursor,SystemCursor
 * Covenant-source-reference: com/badlogic/gdx/graphics/Cursor.java
 * Covenant-verified: 2026-04-19
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
