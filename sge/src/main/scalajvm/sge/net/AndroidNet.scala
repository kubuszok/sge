/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: backends/gdx-backend-android/.../AndroidNet.java
 * Original authors: acoppes
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: AndroidNet (same name, different package)
 *   Renames: sendHttpRequest/cancelHttpRequest → httpClient (SgeHttpClient)
 *   Convention: openURI delegates to AndroidPlatformProvider; sockets use NetJava* (pure JDK)
 *   Idiom: split packages
 *   Audited: 2026-03-08
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package net

import sge.platform.android.AndroidPlatformProvider

/** A [[sge.Net]] implementation for Android.
  *
  * HTTP is handled by [[SgeHttpClient]]; sockets by [[NetJavaServerSocketImpl]] / [[NetJavaSocketImpl]]. URI opening delegates to the Android platform provider (Intent-based).
  *
  * @param provider
  *   the Android platform provider for Intent-based URI opening
  * @param context
  *   the Android Context (as AnyRef) passed to the provider
  */
class AndroidNet(provider: AndroidPlatformProvider, context: AnyRef) extends sge.Net {

  override val httpClient: SgeHttpClient = SgeHttpClient()

  override def newServerSocket(protocol: Net.Protocol, hostname: String, port: Int, hints: ServerSocketHints): ServerSocket =
    NetJavaServerSocketImpl(protocol, sge.utils.Nullable(hostname), port, hints)

  override def newServerSocket(protocol: Net.Protocol, port: Int, hints: ServerSocketHints): ServerSocket =
    NetJavaServerSocketImpl(protocol, port, hints)

  override def newClientSocket(protocol: Net.Protocol, host: String, port: Int, hints: SocketHints): Socket =
    NetJavaSocketImpl(protocol, host, port, hints)

  override def openURI(URI: String): Boolean =
    provider.openURI(context, URI)
}
