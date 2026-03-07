/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backends-gwt/.../GwtNet.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: GwtNet -> BrowserNet
 *   Convention: HTTP handled by SgeHttpClient (sttp, cross-platform) — no GWT RequestBuilder
 *   Convention: openURI uses window.open/location.assign via scalajs-dom
 *   Convention: sockets not supported in browser (throw UnsupportedOperationException)
 *   Idiom: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge

import sge.net._
import org.scalajs.dom

/** Browser [[Net]] implementation. HTTP requests are handled by [[SgeHttpClient]] (sttp with Fetch backend). TCP sockets are not supported in browser environments.
  *
  * @param config
  *   browser application configuration, used for [[openURI]] behavior
  */
class BrowserNet(config: BrowserApplicationConfig) extends Net {

  override val httpClient: SgeHttpClient = SgeHttpClient()

  override def newServerSocket(protocol: Net.Protocol, hostname: String, port: Int, hints: ServerSocketHints): ServerSocket =
    throw UnsupportedOperationException("Server sockets are not supported in the browser")

  override def newServerSocket(protocol: Net.Protocol, port: Int, hints: ServerSocketHints): ServerSocket =
    throw UnsupportedOperationException("Server sockets are not supported in the browser")

  override def newClientSocket(protocol: Net.Protocol, host: String, port: Int, hints: SocketHints): Socket =
    throw UnsupportedOperationException("Client sockets are not supported in the browser")

  override def openURI(URI: String): Boolean = {
    if (config.openURLInNewWindow) {
      dom.window.open(URI, "_blank")
    } else {
      dom.window.location.assign(URI)
    }
    true
  }
}
