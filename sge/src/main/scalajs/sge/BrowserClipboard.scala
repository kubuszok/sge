/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backends-gwt/.../GwtClipboard.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: GwtClipboard -> BrowserClipboard
 *   Convention: navigator.clipboard.writeText via scalajs-dom (async, fire-and-forget)
 *   Convention: paste only works within the app (browser security restriction)
 *   Idiom: Nullable (0 null), split packages
 *   Audited: 2026-03-08
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge

import sge.utils.{ Clipboard, Nullable }
import org.scalajs.dom

/** Browser clipboard implementation. Reading from the system clipboard is not supported due to browser security restrictions — paste only works within the application. Writing uses the Navigator
  * Clipboard API when available.
  */
class BrowserClipboard extends Clipboard {

  private var content: String = ""

  override def hasContents: Boolean = {
    val c = contents
    c.exists(_.nonEmpty)
  }

  override def contents: Nullable[String] = Nullable(content)

  override def contents_=(value: Nullable[String]): Unit =
    value.foreach { c =>
      content = c
      // Fire-and-forget write to the system clipboard via Navigator Clipboard API.
      // This is async and may silently fail if the user hasn't granted permission.
      val nav       = dom.window.navigator.asInstanceOf[scala.scalajs.js.Dynamic]
      val clipboard = nav.clipboard
      if (!scala.scalajs.js.isUndefined(clipboard)) {
        clipboard.writeText(c)
      }
    }
}
