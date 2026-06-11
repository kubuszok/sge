/*
 * SGE - Scala Game Engine
 * copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (replaces NetJavaImpl with sttp-backed HTTP client)
 *   Convention: pooled requests, Future-based dispatch, callback on completion
 *   Idiom: split packages, Nullable, boundary/break
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 229
 * Covenant-baseline-methods: NoopBackendFactory,PendingEntry,SgeHttpClient,apply,buildSttpRequest,cancel,claimFree,close,entries,entry,freeRequest,future,isPending,listener,method,noop,obtainRequest,pending,r,requestPool,send,sttpRequest,toSttpMethod,uri
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-06-11
 */
package sge
package net

import scala.collection.mutable
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }
import lowlevel.Nullable
import sge.utils.Pool

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
    Pool.Default[SgeHttpRequest](() => SgeHttpRequest(), poolCapacity, poolMax)

  // Single-ownership token for a request's pool lifecycle. `cancelled` suppresses
  // the late onComplete callbacks (the analog of NetJavaImpl checking the listeners
  // map before invoking handleHttpResponse — see NetJavaImpl.java:227-231). `freed`
  // guards the SOLE requestPool.free(): in the original libgdx NetJavaImpl there is
  // no pool, and removeFromConnectionsAndListeners (idempotent map remove) is the
  // release operation that fires at most once per terminal action. Here the pool's
  // contract is "the same object must not be freed multiple times" (Pool.scala:62),
  // so exactly one of {cancel, close, onComplete} must perform the free. `freed`
  // makes that release idempotent the way the map remove is idempotent upstream
  // (ISS-505). The flag lives on the entry, NOT on Pool — Pool's no-double-free
  // contract is left intact.
  final private class PendingEntry(
    val future:              Future[?],
    val listener:            Nullable[Net.HttpResponseListener],
    @volatile var cancelled: Boolean = false,
    @volatile var freed:     Boolean = false
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
      // Mirror NetJavaImpl.java:227-231: only deliver the result if the request
      // was not already cancelled (in the original, getFromListeners returns null
      // once the request has been removed, so handleHttpResponse is skipped).
      if (!entry.cancelled) {
        result match {
          case Success(response) =>
            listener.foreach(_.handleHttpResponse(SgeHttpResponse(response)))
          case Failure(ex) =>
            listener.foreach(_.failed(ex))
        }
      }
      // Sole free for the completion path: a no-op if cancel()/close() already
      // claimed ownership of this entry (ISS-505 double-free fix).
      freeRequest(request, entry)
    }
  }

  /** Cancels a pending request. Invokes `listener.cancelled()` and returns the request to the pool.
    *
    * Faithful to NetJavaImpl.cancelHttpRequest (NetJavaImpl.java:256-264): the cancellation only takes effect — `cancelled()` fires and the request is released — if the request is still pending (in
    * the original, only if `getFromListeners(httpRequest) != null`). If the in-flight completion already removed and freed the request, `pending.remove` returns nothing here and cancel is a no-op,
    * exactly as the upstream map remove makes the second cancel a no-op. The single free is guarded by [[claimFree]] so cancel and the future's onComplete never free the same request twice (ISS-505).
    */
  def cancel(request: SgeHttpRequest): Unit =
    pending
      .synchronized {
        pending.remove(request)
      }
      .foreach { entry =>
        entry.cancelled = true
        // cancelled() fires exactly once: this entry was just removed from `pending`,
        // so no other cancel() can re-enter here, and onComplete's path never calls
        // cancelled(). Matches NetJavaImpl firing cancelled() once per live request.
        entry.listener.foreach(_.cancelled())
        // Sole free for the cancel path — skipped if onComplete already claimed it
        // (it cannot have, since we just held the only live mapping, but the guard
        // keeps the discipline symmetric and survives reordered completions).
        if (claimFree(entry)) requestPool.free(request)
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
      // Same single-ownership discipline as cancel(): each entry was just removed
      // from `pending` by the snapshot-and-clear above, so a concurrent onComplete
      // can no longer find it via freeRequest's remove; claimFree still guards the
      // free so a completion already in flight for this entry frees at most once
      // overall (ISS-505).
      if (claimFree(entry)) requestPool.free(req)
    }
    requestPool.clear()
    backend.close()
  }

  /** Frees `request` back to the pool after its in-flight future completes — the sole free for the completion path, skipped if cancel()/close() already claimed ownership of `entry` (ISS-505). The
    * `pending.remove` mirrors NetJavaImpl.removeFromConnectionsAndListeners (NetJavaImpl.java:233): idempotent, so the entry may already be absent if it was cancelled.
    */
  private def freeRequest(request: SgeHttpRequest, entry: PendingEntry): Unit = {
    pending.synchronized {
      pending.remove(request)
    }
    if (claimFree(entry)) requestPool.free(request)
  }

  /** Atomically claims the right to perform the single `requestPool.free` for `entry`. Returns true to exactly one caller across the entry's whole lifecycle ({cancel, close, onComplete}); every
    * subsequent caller gets false and must not free. Synchronizing on `pending` (the same monitor that guards entry insertion/removal) makes the check-and-set atomic without touching Pool, whose own
    * no-double-free contract (Pool.scala:62) stays intact (ISS-505).
    */
  private def claimFree(entry: PendingEntry): Boolean =
    pending.synchronized {
      if (entry.freed) false
      else {
        entry.freed = true
        true
      }
    }

  private def buildSttpRequest(req: SgeHttpRequest): SttpRequest[Either[String, String]] = {
    val uri    = SttpUri.unsafeParse(req.url)
    val method = toSttpMethod(req.method)

    var r = sttpBasicRequest.method(method, uri).followRedirects(req.followRedirects)

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

  private def toSttpMethod(m: Net.HttpMethod): SttpMethod = m match {
    case Net.HttpMethod.GET    => SttpMethod.GET
    case Net.HttpMethod.POST   => SttpMethod.POST
    case Net.HttpMethod.PUT    => SttpMethod.PUT
    case Net.HttpMethod.DELETE => SttpMethod.DELETE
    case Net.HttpMethod.HEAD   => SttpMethod.HEAD
    case Net.HttpMethod.PATCH  => SttpMethod.PATCH
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
    override def send(request: SttpRequest[Either[String, String]]): Future[SttpResponse[Either[String, String]]] =
      Future.failed(new UnsupportedOperationException("noop HTTP backend"))
    override def close(): Unit = ()
  }
}
