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
