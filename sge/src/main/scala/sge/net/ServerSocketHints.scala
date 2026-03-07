/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/net/ServerSocketHints.java
 * Original authors: mzechner, noblemaster
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package net

/** Options for {@link ServerSocket} instances.
  *
  * @author
  *   mzechner (original implementation)
  * @author
  *   noblemaster (original implementation)
  */
class ServerSocketHints {

  /** The listen backlog length. Needs to be greater than 0, otherwise the system default is used. backlog is the maximum queue length for incoming connection, i.e. maximum number of connections
    * waiting for accept(...). If a connection indication arrives when the queue is full, the connection is refused.
    */
  var backlog: Int = 16

  /** Performance preferences are described by three integers whose values indicate the relative importance of short connection time, low latency, and high bandwidth. The absolute values of the
    * integers are irrelevant; in order to choose a protocol the values are simply compared, with larger values indicating stronger preferences. Negative values represent a lower priority than
    * positive values. If the application prefers short connection time over both low latency and high bandwidth, for example, then it could invoke this method with the values (1, 0, 0). If the
    * application prefers high bandwidth above low latency, and low latency above short connection time, then it could invoke this method with the values (0, 1, 2).
    */
  var performancePrefConnectionTime: Int = 0

  /** See performancePrefConnectionTime for details. */
  var performancePrefLatency: Int = 1 // low latency
  /** See performancePrefConnectionTime for details. */
  var performancePrefBandwidth: Int = 0

  /** Enable/disable the SO_REUSEADDR socket option. */
  var reuseAddress: Boolean = true

  /** The SO_TIMEOUT in milliseconds for how long to wait during server.accept(). Enter 0 for infinite wait. */
  var acceptTimeout: Int = 5000

  /** The SO_RCVBUF (receive buffer) size in bytes for server.accept(). */
  var receiveBufferSize: Int = 4096
}
