/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/Net.java
 * Original authors: mzechner, noblemaster, arielsan
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `HttpRequest` class removed — replaced by `SgeHttpRequest` in `sge.net`
 *   Renames: `sendHttpRequest`/`cancelHttpRequest`/`isHttpRequestPending` → `httpClient: SgeHttpClient`
 *   Convention: sttp-backed HTTP client replaces HttpURLConnection-based NetJavaImpl
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 206
 * Covenant-baseline-methods: HttpMethod,HttpResponse,HttpResponseListener,Net,Protocol,cancelled,failed,getHeader,handleHttpResponse,headers,httpClient,newClientSocket,newServerSocket,openURI,result,resultAsStream,resultAsString,status
 * Covenant-source-reference: com/badlogic/gdx/Net.java
 * Covenant-verified: 2026-04-19
 */
package sge

import sge.net._
import sge.utils.Nullable
import java.io.InputStream
import java.util.{ List, Map }

/** Provides methods to perform networking operations, such as simple HTTP get and post requests, and TCP server/client socket communication. </p>
  *
  * To perform an HTTP request, obtain a request from the [[httpClient]], configure it, and send it:
  *
  * {{{
  *   val client  = sge.net.httpClient
  *   val request = client.obtainRequest()
  *   request.withMethod(Net.HttpMethod.GET).withUrl("https://example.com")
  *   client.send(request, myListener)
  * }}}
  *
  * To create a TCP client socket to communicate with a remote TCP server, invoke the {@link #newClientSocket(Protocol, String, int, SocketHints)} method. The returned {@link Socket} offers an
  * {@link InputStream} and {@link OutputStream} to communicate with the end point. </p>
  *
  * To create a TCP server socket that waits for incoming connections, invoke the {@link #newServerSocket(Protocol, int, ServerSocketHints)} method. The returned {@link ServerSocket} offers an
  * {@link ServerSocket#accept(SocketHints options)} method that waits for an incoming connection.
  *
  * @author
  *   mzechner
  * @author
  *   noblemaster
  * @author
  *   arielsan
  */
trait Net {

  import Net._

  /** Returns the HTTP client for sending requests. The client manages a pool of reusable [[SgeHttpRequest]] objects and dispatches them via sttp.
    */
  def httpClient: SgeHttpClient

  /** Creates a new server socket on the given address and port, using the given {@link Protocol} , waiting for incoming connections.
    *
    * @param hostname
    *   the hostname or ip address to bind the socket to
    * @param port
    *   the port to listen on
    * @param hints
    *   additional {@link ServerSocketHints} used to create the socket. Input null to use the default setting provided by the system.
    * @return
    *   the {@link ServerSocket}
    * @throws GdxRuntimeException
    *   in case the socket couldn't be opened
    */
  def newServerSocket(protocol: Protocol, hostname: String, port: Int, hints: ServerSocketHints): ServerSocket

  /** Creates a new server socket on the given port, using the given {@link Protocol} , waiting for incoming connections.
    *
    * @param port
    *   the port to listen on
    * @param hints
    *   additional {@link ServerSocketHints} used to create the socket. Input null to use the default setting provided by the system.
    * @return
    *   the {@link ServerSocket}
    * @throws GdxRuntimeException
    *   in case the socket couldn't be opened
    */
  def newServerSocket(protocol: Protocol, port: Int, hints: ServerSocketHints): ServerSocket

  /** Creates a new TCP client socket that connects to the given host and port.
    *
    * @param host
    *   the host address
    * @param port
    *   the port
    * @param hints
    *   additional {@link SocketHints} used to create the socket. Input null to use the default setting provided by the system.
    * @throws GdxRuntimeException
    *   in case the socket couldn't be opened
    */
  def newClientSocket(protocol: Protocol, host: String, port: Int, hints: SocketHints): Socket

  /** Launches the default browser to display a URI. If the default browser is not able to handle the specified URI, the application registered for handling URIs of the specified type is invoked. The
    * application is determined from the protocol and path of the URI. A best effort is made to open the given URI; however, since external applications are involved, no guarantee can be made as to
    * whether the URI was actually opened. If it is known that the URI was not opened, false will be returned; otherwise, true will be returned.
    *
    * @param URI
    *   the URI to be opened.
    * @return
    *   false if it is known the uri was not opened, true otherwise.
    */
  def openURI(URI: String): Boolean
}

object Net {

  /** Protocol used by {@link Net#newServerSocket(Protocol, int, ServerSocketHints)} and {@link Net#newClientSocket(Protocol, String, int, SocketHints)} .
    * @author
    *   mzechner
    */
  enum Protocol {
    case TCP
  }

  /** Provides common HTTP methods to use when creating a {@link HttpRequest} . <ul> <li><b>HEAD</b> Asks for a response identical to that of a GET request but without the response body.</li>
    * <li><b>GET</b> requests a representation of the specified resource. Requests using GET should only retrieve data.</li> <li><b>POST</b> is used to submit an entity to the specified resource,
    * often causing a change in state or side effects on the server.</li> <li><b>PUT</b> replaces all current representations of the target resource with the request payload.</li> <li><b>PATCH</b>
    * method is used to apply partial modifications to a resource.</li> <li><b>DELETE</b> deletes the specified resource.</li> </ul>
    */
  enum HttpMethod(val value: String) {

    /** The HEAD method asks for a response identical to that of a GET request, but without the response body. * */
    case HEAD extends HttpMethod("HEAD")

    /** The GET method requests a representation of the specified resource. Requests using GET should only retrieve data. * */
    case GET extends HttpMethod("GET")

    /** The POST method is used to submit an entity to the specified resource, often causing a change in state or side effects on the server. *
      */
    case POST extends HttpMethod("POST")

    /** The PUT method replaces all current representations of the target resource with the request payload. * */
    case PUT extends HttpMethod("PUT")

    /** The PATCH method is used to apply partial modifications to a resource. * */
    case PATCH extends HttpMethod("PATCH")

    /** The DELETE method deletes the specified resource. * */
    case DELETE extends HttpMethod("DELETE")
  }

  /** HTTP response interface with methods to get the response data as a byte[], a {@link String} or an {@link InputStream}. */
  trait HttpResponse {

    /** Returns the data of the HTTP response as a byte[]. <p> <b>Note</b>: This method may only be called once per response. </p>
      * @return
      *   the result as a byte[] or null in case of a timeout or if the operation was canceled/terminated abnormally. The timeout is specified when creating the HTTP request, with
      *   {@link HttpRequest#setTimeOut(int)}
      */
    def result: Array[Byte]

    /** Returns the data of the HTTP response as a {@link String} . <p> <b>Note</b>: This method may only be called once per response. </p>
      * @return
      *   the result as a string or null in case of a timeout or if the operation was canceled/terminated abnormally. The timeout is specified when creating the HTTP request, with
      *   {@link HttpRequest#setTimeOut(int)}
      */
    def resultAsString: String

    /** Returns the data of the HTTP response as an {@link InputStream} . <b><br> Warning:</b> Do not store a reference to this InputStream outside of
      * {@link HttpResponseListener#handleHttpResponse(HttpResponse)} . The underlying HTTP connection will be closed after that callback finishes executing. Reading from the InputStream after it's
      * connection has been closed will lead to exception.
      * @return
      *   An {@link InputStream} with the {@link HttpResponse} data.
      */
    def resultAsStream: InputStream

    /** Returns the {@link HttpStatus} containing the statusCode of the HTTP response. */
    def status: HttpStatus

    /** Returns the value of the header with the given name as a {@link String} , or Nullable.empty if the header is not set. See {@link HttpResponseHeader} .
      */
    def getHeader(name: String): Nullable[String]

    /** Returns a Map of the headers. The keys are Strings that represent the header name. Each values is a List of Strings that represent the corresponding header values. See
      * {@link HttpResponseHeader} .
      */
    def headers: Map[String, List[String]]
  }

  /** Listener to be able to do custom logic once the {@link HttpResponse} is ready to be processed, register it with {@link SgeHttpClient#send} .
    */
  trait HttpResponseListener {

    /** Called when the {@link HttpRequest} has been processed and there is a {@link HttpResponse} ready. Passing data to the rendering thread should be done using
      * {@link Application#postRunnable(java.lang.Runnable runnable)} {@link HttpResponse} contains the {@link HttpStatus} and should be used to determine if the request was successful or not (see
      * more info at {@link HttpStatus#getStatusCode()} ). For example:
      *
      * <pre> HttpResponseListener listener = new HttpResponseListener() { public void handleHttpResponse (HttpResponse httpResponse) { HttpStatus status = httpResponse.getStatus(); if
      * (status.getStatusCode() >= 200 && status.getStatusCode() < 300) { // it was successful } else { // do something else } } } </pre>
      *
      * @param httpResponse
      *   The {@link HttpResponse} with the HTTP response values.
      */
    def handleHttpResponse(httpResponse: HttpResponse): Unit

    /** Called if the {@link HttpRequest} failed because an exception when processing the HTTP request, could be a timeout any other reason (not an HTTP error).
      * @param t
      *   If the HTTP request failed because an Exception, t encapsulates it to give more information.
      */
    def failed(t: Throwable): Unit

    def cancelled(): Unit
  }
}
