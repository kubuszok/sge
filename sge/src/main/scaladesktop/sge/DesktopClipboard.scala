/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backend-lwjgl3/.../Lwjgl3Clipboard.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: Lwjgl3Clipboard -> DesktopClipboard
 *   Convention: GLFW calls replaced by constructor-injected functions (FFI-agnostic)
 *   Convention: property syntax (contents/contents_=) instead of getContents/setContents
 *   Idiom: split packages; Nullable for optional clipboard content
 *   Audited: 2026-03-08
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge

import sge.utils.{ Clipboard, Nullable }

/** Desktop clipboard implementation that delegates to windowing system functions.
  *
  * The actual clipboard read/write functions are injected via constructor parameters, making this class independent of any specific FFI mechanism (Panama, @extern, etc.). The desktop application
  * creates this with appropriate GLFW/SDL3 bindings.
  *
  * @param getClipboardString
  *   reads the clipboard text (e.g. wraps `glfwGetClipboardString`)
  * @param setClipboardString
  *   writes text to the clipboard (e.g. wraps `glfwSetClipboardString`)
  * @author
  *   mzechner (original implementation)
  */
class DesktopClipboard(
  getClipboardString: () => Nullable[String],
  setClipboardString: String => Unit
) extends Clipboard {

  override def hasContents: Boolean =
    contents.exists(_.nonEmpty)

  override def contents: Nullable[String] =
    getClipboardString()

  override def contents_=(content: Nullable[String]): Unit =
    content.foreach(setClipboardString)
}
