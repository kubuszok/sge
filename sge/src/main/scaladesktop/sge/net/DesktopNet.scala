/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backend-headless/.../HeadlessNet.java
 * Original authors: acoppes, Jon Renner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: HeadlessNet -> DesktopNet (reused by desktop backend)
 *   Renames: sendHttpRequest/cancelHttpRequest/isHttpRequestPending -> httpClient (SgeHttpClient)
 *   Convention: openURI uses java.awt.Desktop with headless fallback; takes Application for logging
 *   Idiom: split packages
 *   Audited: 2026-03-05
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package net

/** A [[sge.Net]] implementation for desktop and headless environments. HTTP is handled by [[SgeHttpClient]]; sockets by [[NetJavaServerSocketImpl]] / [[NetJavaSocketImpl]].
  *
  * @param app
  *   the application instance, used for error logging in [[openURI]]
  * @author
  *   acoppes (original implementation)
  * @author
  *   Jon Renner (original implementation)
  */
class DesktopNet(app: Application) extends sge.Net {

  override val httpClient: SgeHttpClient = SgeHttpClient()

  override def newServerSocket(protocol: Net.Protocol, hostname: String, port: Int, hints: ServerSocketHints): ServerSocket =
    NetJavaServerSocketImpl(protocol, sge.utils.Nullable(hostname), port, hints)

  override def newServerSocket(protocol: Net.Protocol, port: Int, hints: ServerSocketHints): ServerSocket =
    NetJavaServerSocketImpl(protocol, port, hints)

  override def newClientSocket(protocol: Net.Protocol, host: String, port: Int, hints: SocketHints): Socket =
    NetJavaSocketImpl(protocol, host, port, hints)

  override def openURI(URI: String): Boolean = {
    val osName = System.getProperty("os.name", "").toLowerCase
    val uri    = java.net.URI.create(URI).toString
    try
      if (osName.contains("mac")) {
        new ProcessBuilder("open", uri).start()
        true
      } else if (osName.contains("win")) {
        new ProcessBuilder("cmd", "/c", "start", uri).start()
        true
      } else if (osName.contains("linux") || osName.contains("nix") || osName.contains("nux")) {
        new ProcessBuilder("xdg-open", uri).start()
        true
      } else {
        app.error("DesktopNet", "Opening URIs on this environment is not supported. Ignoring.")
        false
      }
    catch {
      case t: Throwable =>
        app.error("DesktopNet", "Failed to open URI. ", t)
        false
    }
  }
}
