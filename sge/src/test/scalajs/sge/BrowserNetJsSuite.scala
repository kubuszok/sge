/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
// SGE — real JS coverage for BrowserNet's own surface (ISS-860).
//
// The live Fetch path (BrowserNet.httpClient, a real SgeHttpClient over sttp's Fetch backend)
// cannot be honestly unit-tested without a server: jsdom offers no loopback HTTP endpoint. This
// suite therefore pins the parts of BrowserNet that ARE deterministically testable in jsdom:
//   * the three socket factory methods are unsupported-capability signals (SgeError.Unsupported)
//   * httpClient is a live SgeHttpClient (the object BrowserNet delegates HTTP to)
// The request->response Fetch roundtrip is covered — via an injected mock backend, the honest
// substitute for live fetch — in sge.net.BrowserNetFetchRoundtripJsSuite. A full loopback IT that
// drives the real Fetch backend against a server remains a follow-up (browser IT smoke only today).
//
// HONEST GAP: openURI is NOT unit-tested here. Both of its branches (window.open for the new-window
// path, location.assign for the redirect path) are navigation APIs jsdom leaves unimplemented —
// jsdom's window.open THROWS "Not implemented: window.open" and terminates the JS test run, so the
// method cannot be honestly exercised under the jsdom jsEnv. openURI stays covered by the browser IT
// (a real browser); stubbing window.open would test the stub, not SGE.
package sge

import munit.FunSuite
import sge.net.{ ServerSocketHints, SocketHints }
import sge.utils.SgeError

class BrowserNetJsSuite extends FunSuite {

  private def net: BrowserNet = new BrowserNet(new BrowserApplicationConfig())

  test("newClientSocket signals unsupported capability via SgeError.Unsupported") {
    val thrown = intercept[SgeError.Unsupported](
      net.newClientSocket(Net.Protocol.TCP, "localhost", 8080, new SocketHints)
    )
    assertEquals(thrown.getMessage, "Client sockets are not supported in the browser")
  }

  test("newServerSocket(protocol, hostname, port, hints) signals unsupported via SgeError.Unsupported") {
    val thrown = intercept[SgeError.Unsupported](
      net.newServerSocket(Net.Protocol.TCP, "localhost", 8080, new ServerSocketHints)
    )
    assertEquals(thrown.getMessage, "Server sockets are not supported in the browser")
  }

  test("newServerSocket(protocol, port, hints) signals unsupported via SgeError.Unsupported") {
    val thrown = intercept[SgeError.Unsupported](
      net.newServerSocket(Net.Protocol.TCP, 8080, new ServerSocketHints)
    )
    assertEquals(thrown.getMessage, "Server sockets are not supported in the browser")
  }

  test("httpClient is a live SgeHttpClient that hands out reset requests") {
    val client = net.httpClient
    assert(client ne null, "BrowserNet must expose a non-null httpClient")
    val req = client.obtainRequest()
    // A freshly obtained request carries the documented defaults.
    assertEquals(req.method, Net.HttpMethod.GET)
    assertEquals(req.url, "")
    client.close()
  }
}
