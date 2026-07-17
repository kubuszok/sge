/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backend-lwjgl3/.../DefaultLwjgl3Input.java (getTextInput)
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: Scala Native half of the DefaultDesktopInput.getTextInput platform split (ISS-815 clause 3).
 *   Convention: no Swing / AWT on Scala Native and no native text-input dialog backend, so the
 *     capability is signaled as unavailable via SgeError.Unsupported (ISS-771 convention), never a
 *     raw JDK exception. Crucially this file uses NO java.lang reflection (Class.forName /
 *     Method.invoke / Field.get): those symbols are unreachable on Scala Native and would break the
 *     native link — the whole point of splitting the JVM Swing path out of shared scaladesktop code.
 *   Idiom: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge

/** Scala Native half of [[DefaultDesktopInput.getTextInput]] (ISS-815 clause 3).
  *
  * There is no Swing/AWT and no native text-input dialog on Scala Native, so this signals the missing capability through the project error hierarchy. No reflection symbols are referenced, so the
  * Native reachable set stays link-clean (a native test calls this and asserts [[sge.utils.SgeError.Unsupported]]).
  */
private[sge] object DesktopTextInputPlatform {

  def getTextInput(
    postRunnable: Runnable => Unit,
    listener:     Input.TextInputListener,
    title:        String,
    text:         String,
    hint:         String
  ): Unit =
    throw sge.utils.SgeError.Unsupported(
      "text input dialog (getTextInput) is not supported on Scala Native (no Swing/AWT)"
    )
}
