/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backend-headless/.../HeadlessNet.java
 * Original authors: acoppes, Jon Renner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: HeadlessNet -> DesktopNet (reused by desktop backend)
 *   Renames: sendHttpRequest/cancelHttpRequest/isHttpRequestPending -> httpClient (SgeHttpClient)
 *   Convention: openURI uses java.awt.Desktop with headless fallback; error logging goes through the
 *     global utils.Log facade — SGE's mapping of the original's Gdx.app.error (see utils/Logger.scala
 *     migration note); the Application param is the explicit-context stand-in for the Gdx.app global
 *     and is otherwise unused (ISS-773)
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
  *   the application instance — the explicit-context replacement for the `Gdx.app` global the original consulted; [[openURI]] error logging itself goes through the global [[sge.utils.Log]] facade
  *   (SGE's mapping of `Gdx.app.error`, see the `utils/Logger.scala` migration note), so this reference is currently otherwise unused
  * @author
  *   acoppes (original implementation)
  * @author
  *   Jon Renner (original implementation)
  */
class DesktopNet(app: Application) extends sge.Net {

  override val httpClient: SgeHttpClient = SgeHttpClient()

  override def newServerSocket(protocol: Net.Protocol, hostname: String, port: Int, hints: ServerSocketHints): ServerSocket =
    NetJavaServerSocketImpl(protocol, lowlevel.Nullable(hostname), port, hints)

  override def newServerSocket(protocol: Net.Protocol, port: Int, hints: ServerSocketHints): ServerSocket =
    NetJavaServerSocketImpl(protocol, port, hints)

  override def newClientSocket(protocol: Net.Protocol, host: String, port: Int, hints: SocketHints): Socket =
    NetJavaSocketImpl(protocol, host, port, hints)

  override def openURI(URI: String): Boolean = {
    val osName = System.getProperty("os.name", "").toLowerCase
    try {
      // Faithful to Lwjgl3Net.openURI (Lwjgl3Net.java:74-98): macOS launches `open`, otherwise
      // prefer java.awt.Desktop.browse (which hands a java.net.URI straight to the OS handler —
      // never a shell), and fall back to a per-OS launcher command. The URI is NEVER routed
      // through cmd's `start` builtin, which re-parses `&`/`^` as command separators and would
      // truncate a query string (ISS-773).
      //
      // The URI is parsed INSIDE the try so an invalid URI is caught and yields `false` rather
      // than escaping openURI: upstream constructs `new URI(uri)` inside each try/catch(Throwable)
      // (Lwjgl3Net.java:76-95), so a malformed URI returns false. Parsing this above the try (as an
      // earlier port did) leaked the IllegalArgumentException from java.net.URI.create (ISS-834).
      val uri = java.net.URI.create(URI).toString
      if (osName.contains("mac")) {
        new ProcessBuilder(DesktopNet.openUriCommand(osName, uri)*).start()
        true
      } else if (java.awt.Desktop.isDesktopSupported && java.awt.Desktop.getDesktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
        java.awt.Desktop.getDesktop.browse(java.net.URI.create(uri))
        true
      } else {
        val command = DesktopNet.openUriCommand(osName, uri)
        if (command.nonEmpty) {
          new ProcessBuilder(command*).start()
          true
        } else {
          // Faithful to Gdx.app.error("HeadlessNet", "Opening URIs on this environment is not
          // supported. Ignoring.") (HeadlessNet.java:84) — tag folded into the message, routed
          // through the global Log facade (SGE's Gdx.app.error mapping, utils/Logger.scala note).
          utils.Log.error("DesktopNet: Opening URIs on this environment is not supported. Ignoring.")
          false
        }
      }
    } catch {
      case t: Throwable =>
        // Faithful to Gdx.app.error("HeadlessNet", "Failed to open URI. ", t)
        // (HeadlessNet.java:87): same tag convention and the Throwable-carrying overload.
        utils.Log.error("DesktopNet: Failed to open URI. ", t)
        false
    }
  }
}

object DesktopNet {

  /** The Windows `INTERNET_MAX_URL_LENGTH` (`wininet.h`): the longest URL `rundll32 url.dll,FileProtocolHandler` will faithfully carry. Beyond it the ANSI FileProtocolHandler entry silently truncates
    * the URL (and can mangle non-ASCII IRIs), so the Windows fallback refuses over-long URLs rather than opening a corrupted one (ISS-834).
    */
  final private[sge] val InternetMaxUrlLength: Int = 2083

  /** Pure, launch-free construction of the per-OS process argv used to open a URI when `java.awt.Desktop.browse` is unavailable (the non-AWT fallback path of [[DesktopNet.openURI]]).
    *
    * The URI is always passed as a SINGLE argument so its query — ampersands included — reaches the launched program intact. Windows uses `rundll32 url.dll,FileProtocolHandler <uri>` rather than
    * `cmd /c start <uri>`: cmd's `start` builtin re-parses `&`/`^` as command separators and would truncate the URI (ISS-773). macOS/Linux keep LibGDX's `open`/`xdg-open` (Lwjgl3Net.java:77,91),
    * which already receive the URI as one argument.
    *
    * The Windows `rundll32` branch additionally refuses URLs longer than [[InternetMaxUrlLength]] (`INTERNET_MAX_URL_LENGTH`, 2083): its ANSI FileProtocolHandler entry silently truncates and can
    * mangle non-ASCII IRIs beyond that length, so it returns `Nil` (making [[DesktopNet.openURI]] log and return `false`) instead of launching a corrupted URL. This guard only affects the
    * Windows-only, fallback-only path — real desktops open URIs through `java.awt.Desktop.browse`, which is not length-limited (ISS-834).
    *
    * @return
    *   the process argv, or `Nil` on an unsupported OS (or an over-long URL on the Windows `rundll32` fallback)
    */
  private[sge] def openUriCommand(osName: String, uri: String): List[String] = {
    val os = osName.toLowerCase
    if (os.contains("mac")) List("open", uri)
    else if (os.contains("win"))
      if (uri.length > InternetMaxUrlLength) Nil
      else List("rundll32", "url.dll,FileProtocolHandler", uri)
    else if (os.contains("linux") || os.contains("nix") || os.contains("nux")) List("xdg-open", uri)
    else Nil
  }
}
