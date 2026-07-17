/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backend-lwjgl3/.../DefaultLwjgl3Input.java (getTextInput)
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: JVM half of the DefaultDesktopInput.getTextInput platform split (ISS-815 clause 3).
 *   Deviation: upstream DefaultLwjgl3Input.getTextInput is a FIXME no-op (DefaultLwjgl3Input.java:305-308);
 *     the Swing JOptionPane dialog here is an SGE improvement following the older lwjgl2 backend's
 *     DefaultLwjglInput lineage (which pops a Swing input dialog), not a port of the lwjgl3 body.
 *   Convention: Swing is a JVM-only capability. The shared scaladesktop DefaultDesktopInput used
 *     java.lang reflection (Class.forName / Method.invoke / Field.get) to reach javax.swing so the
 *     single scaladesktop source could compile for Scala Native too — but those reflection symbols
 *     are UNREACHABLE on Scala Native and break the native link (DCE only hid them). Splitting the
 *     capability into per-platform sources (this JVM one uses Swing directly; the scalanative twin
 *     signals SgeError.Unsupported) removes every reflection symbol from the Native reachable set.
 *   Idiom: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge

import javax.swing.{ JOptionPane, SwingUtilities }

/** JVM half of [[DefaultDesktopInput.getTextInput]]: shows a Swing input dialog (ISS-815 clause 3).
  *
  * Upstream `DefaultLwjgl3Input.getTextInput` is a FIXME no-op (DefaultLwjgl3Input.java:305-308); this Swing dialog is an SGE improvement following the lwjgl2 `DefaultLwjglInput` lineage: pop a
  * `JOptionPane` input dialog on the AWT event thread, then post the result back to the rendering thread via `postRunnable`. Takes the poster as a function so this helper does not depend on
  * [[DesktopWindow]], keeping the seam small (its Native twin is trivially exercised by a link-proof test).
  */
private[sge] object DesktopTextInputPlatform {

  def getTextInput(
    postRunnable: Runnable => Unit,
    listener:     Input.TextInputListener,
    title:        String,
    text:         String,
    hint:         String
  ): Unit =
    // Guard the whole dispatch: a headless / no-Swing environment reports "canceled" rather than
    // propagating (preserves the pre-split scaladesktop behavior, where a missing Swing class fell
    // back to listener.canceled()).
    try
      SwingUtilities.invokeLater { () =>
        val message = if (hint.nonEmpty) hint else title
        // null args are the Swing (Java) interop contract: no parent Component, no Icon, no
        // selectionValues combo — a plain single-line text field seeded with `text`.
        val result = JOptionPane.showInputDialog(null, message, title, JOptionPane.PLAIN_MESSAGE, null, null, text)
        postRunnable(() =>
          if (result != null) listener.input(result.toString)
          else listener.canceled()
        )
      }
    catch {
      case _: Exception =>
        listener.canceled()
    }
}
