/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/net/SocketHints.java
 * Original authors: mzechner, noblemaster
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 64
 * Covenant-baseline-methods: SocketHints,connectTimeout,keepAlive,linger,lingerDuration,performancePrefBandwidth,performancePrefConnectionTime,performancePrefLatency,receiveBufferSize,sendBufferSize,socketTimeout,tcpNoDelay,trafficClass
 * Covenant-source-reference: com/badlogic/gdx/net/SocketHints.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
 */
package sge
package net

/** Options for {@link Socket} instances.
  *
  * @author
  *   mzechner (original implementation)
  * @author
  *   noblemaster (original implementation)
  */
class SocketHints {

  /** The connection timeout in milliseconds. Not used for sockets created via server.accept(). */
  var connectTimeout: Int = 5000

  /** Performance preferences are described by three integers whose values indicate the relative importance of short connection time, low latency, and high bandwidth. The absolute values of the
    * integers are irrelevant; in order to choose a protocol the values are simply compared, with larger values indicating stronger preferences. Negative values represent a lower priority than
    * positive values. If the application prefers short connection time over both low latency and high bandwidth, for example, then it could invoke this method with the values (1, 0, 0). If the
    * application prefers high bandwidth above low latency, and low latency above short connection time, then it could invoke this method with the values (0, 1, 2).
    */
  var performancePrefConnectionTime: Int = 0
  var performancePrefLatency:        Int = 1 // low latency
  var performancePrefBandwidth:      Int = 0

  /** The traffic class describes the type of connection that shall be established. The traffic class must be in the range 0 <= trafficClass <= 255. <p> The traffic class is bitset created by
    * bitwise-or'ing values such the following : <ul> <li>IPTOS_LOWCOST (0x02) - cheap! <li>IPTOS_RELIABILITY (0x04) - reliable connection with little package loss. <li>IPTOS_THROUGHPUT (0x08) - lots
    * of data being sent. <li>IPTOS_LOWDELAY (0x10) - low delay. </ul>
    */
  var trafficClass: Int = 0x14 // low delay + reliable
  /** True to enable SO_KEEPALIVE. */
  var keepAlive: Boolean = true

  /** True to enable TCP_NODELAY (disable/enable Nagle's algorithm). */
  var tcpNoDelay: Boolean = true

  /** The SO_SNDBUF (send buffer) size in bytes. */
  var sendBufferSize: Int = 4096

  /** The SO_RCVBUF (receive buffer) size in bytes. */
  var receiveBufferSize: Int = 4096

  /** Enable/disable SO_LINGER with the specified linger time in seconds. Only affects socket close. */
  var linger: Boolean = false

  /** The linger duration in seconds (NOT milliseconds!). Only used if linger is true! */
  var lingerDuration: Int = 0

  /** Enable/disable SO_TIMEOUT with the specified timeout, in milliseconds. With this option set to a non-zero timeout, a read() call on the InputStream associated with this Socket will block for
    * only this amount of time
    */
  var socketTimeout: Int = 0
}
