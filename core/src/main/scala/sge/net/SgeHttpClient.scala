/*
 * SGE - Scala Game Engine
 * copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (replaces NetJavaImpl with sttp-backed HTTP client)
 *   Convention: pooled requests, Future-based dispatch, callback on completion
 *   Idiom: split packages, Nullable, boundary/break
 */
package sge
package net

import scala.collection.mutable
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }
import sge.utils.{ Nullable, Pool }
import sttp.client4.basicRequest
import sttp.model.{ Method, Uri }

/** HTTP client that manages a pool of reusable [[SgeHttpRequest]] objects and dispatches them via an sttp backend. Requests are obtained from the pool, configured, sent, and automatically returned to
  * the pool upon completion or cancellation.
  *
  * {{{
  *   val client  = SgeHttpClient()
  *   val req     = client.obtainRequest()
  *   req.withMethod(Net.HttpMethod.GET).withUrl("https://example.com")
  *   client.send(req, myListener)
  *   // ...
  *   client.close()
  * }}}
  */
final class SgeHttpClient private[sge] (backend: HttpBackendFactory, poolCapacity: Int, poolMax: Int) extends AutoCloseable {

  private given ExecutionContext = ExecutionContext.global

  private val requestPool: Pool[SgeHttpRequest] =
    Pool.Default[SgeHttpRequest](() => new SgeHttpRequest(), poolCapacity, poolMax)

  final private class PendingEntry(
    val future:              Future[?],
    val listener:            Nullable[Net.HttpResponseListener],
    @volatile var cancelled: Boolean = false
  )

  private val pending: mutable.Map[SgeHttpRequest, PendingEntry] = mutable.Map.empty

  /** Obtains a fresh (reset) request from the pool. */
  def obtainRequest(): SgeHttpRequest = requestPool.obtain()

  /** Sends the request asynchronously. When the response arrives (or an error occurs), the listener is called and the request is returned to the pool.
    *
    * @param request
    *   a request obtained via [[obtainRequest]]
    * @param listener
    *   optional callback for response/failure/cancellation
    */
  def send(request: SgeHttpRequest, listener: Nullable[Net.HttpResponseListener]): Unit = {
    val sttpRequest = buildSttpRequest(request)
    val future      = backend.send(sttpRequest)

    val entry = new PendingEntry(future, listener)
    pending.synchronized {
      pending.put(request, entry)
    }

    future.onComplete { result =>
      if (!entry.cancelled) {
        result match {
          case Success(response) =>
            listener.foreach(_.handleHttpResponse(new SgeHttpResponse(response)))
          case Failure(ex) =>
            listener.foreach(_.failed(ex))
        }
      }
      freeRequest(request)
    }
  }

  /** Cancels a pending request. Invokes `listener.cancelled()` and returns the request to the pool.
    */
  def cancel(request: SgeHttpRequest): Unit =
    pending
      .synchronized {
        pending.remove(request)
      }
      .foreach { entry =>
        entry.cancelled = true
        entry.listener.foreach(_.cancelled())
        requestPool.free(request)
      }

  /** Returns true if the given request is still pending. */
  def isPending(request: SgeHttpRequest): Boolean =
    pending.synchronized {
      pending.contains(request)
    }

  /** Cancels all pending requests, clears the pool, and closes the backend. */
  override def close(): Unit = {
    val entries = pending.synchronized {
      val snapshot = pending.toSeq
      pending.clear()
      snapshot
    }
    for ((req, entry) <- entries) {
      entry.cancelled = true
      entry.listener.foreach(_.cancelled())
      requestPool.free(req)
    }
    requestPool.clear()
    backend.close()
  }

  private def freeRequest(request: SgeHttpRequest): Unit = {
    pending.synchronized {
      pending.remove(request)
    }
    requestPool.free(request)
  }

  private def buildSttpRequest(req: SgeHttpRequest): sttp.client4.Request[Either[String, String]] = {
    val uri    = Uri.unsafeParse(req.url)
    val method = toSttpMethod(req.method)

    var r = basicRequest.method(method, uri).followRedirects(req.followRedirects)

    // Set timeout
    if (req.timeoutMs > 0) {
      r = r.readTimeout(scala.concurrent.duration.Duration(req.timeoutMs.toLong, "ms"))
    }

    // Set headers
    for ((name, value) <- req.headers)
      r = r.header(name, value)

    // Set body — string content takes precedence
    req.content.foreach { c =>
      r = r.body(c)
    }
    if (req.content.isEmpty) {
      req.contentBytes.foreach { bytes =>
        r = r.body(bytes)
      }
    }

    r
  }

  private def toSttpMethod(m: Net.HttpMethod): Method = m match {
    case Net.HttpMethod.GET    => Method.GET
    case Net.HttpMethod.POST   => Method.POST
    case Net.HttpMethod.PUT    => Method.PUT
    case Net.HttpMethod.DELETE => Method.DELETE
    case Net.HttpMethod.HEAD   => Method.HEAD
    case Net.HttpMethod.PATCH  => Method.PATCH
  }
}

object SgeHttpClient {

  /** Creates a new client backed by the platform's sttp backend. */
  def apply(poolCapacity: Int = 16, poolMax: Int = 64): SgeHttpClient =
    new SgeHttpClient(HttpBackendFactoryImpl, poolCapacity, poolMax)

  /** Creates a no-op client for testing. Requests are pooled but never actually sent. */
  def noop(): SgeHttpClient =
    new SgeHttpClient(NoopBackendFactory, 4, 16)

  private object NoopBackendFactory extends HttpBackendFactory {
    override def send(request: sttp.client4.Request[Either[String, String]]): Future[sttp.client4.Response[Either[String, String]]] =
      Future.failed(new UnsupportedOperationException("noop HTTP backend"))
    override def close(): Unit = ()
  }
}
