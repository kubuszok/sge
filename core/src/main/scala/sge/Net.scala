/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/Net.java
 * Original authors: mzechner, noblemaster, arielsan
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge

import sge.utils.Nullable
import sge.net._
import java.io.{ InputStream, OutputStream }
import java.util.{ List, Map }
import scala.collection.mutable

/** Provides methods to perform networking operations, such as simple HTTP get and post requests, and TCP server/client socket communication. </p>
  *
  * To perform an HTTP request create a {@link HttpRequest} with the HTTP method (see {@link HttpMethods} for common methods) and invoke {@link #sendHttpRequest(HttpRequest, HttpResponseListener)}
  * with it and a {@link HttpResponseListener} . After the HTTP request was processed, the {@link HttpResponseListener} is called with a {@link HttpResponse} with the HTTP response values and an
  * status code to determine if the request was successful or not. </p>
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

  /** Process the specified {@link HttpRequest} and reports the {@link HttpResponse} to the specified {@link HttpResponseListener} .
    * @param httpRequest
    *   The {@link HttpRequest} to be performed.
    * @param httpResponseListener
    *   The {@link HttpResponseListener} to call once the HTTP response is ready to be processed. Could be null, in that case no listener is called.
    */
  def sendHttpRequest(httpRequest: HttpRequest, httpResponseListener: Nullable[HttpResponseListener]): Unit

  def cancelHttpRequest(httpRequest: HttpRequest): Unit

  def isHttpRequestPending(httpRequest: HttpRequest): Boolean

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
  object HttpMethods {

    /** The HEAD method asks for a response identical to that of a GET request, but without the response body. * */
    val HEAD = "HEAD"

    /** The GET method requests a representation of the specified resource. Requests using GET should only retrieve data. * */
    val GET = "GET"

    /** The POST method is used to submit an entity to the specified resource, often causing a change in state or side effects on the server. *
      */
    val POST = "POST"

    /** The PUT method replaces all current representations of the target resource with the request payload. * */
    val PUT = "PUT"

    /** The PATCH method is used to apply partial modifications to a resource. * */
    val PATCH = "PATCH"

    /** The DELETE method deletes the specified resource. * */
    val DELETE = "DELETE"
  }

  /** HTTP response interface with methods to get the response data as a byte[], a {@link String} or an {@link InputStream}. */
  trait HttpResponse {

    /** Returns the data of the HTTP response as a byte[]. <p> <b>Note</b>: This method may only be called once per response. </p>
      * @return
      *   the result as a byte[] or null in case of a timeout or if the operation was canceled/terminated abnormally. The timeout is specified when creating the HTTP request, with
      *   {@link HttpRequest#setTimeOut(int)}
      */
    def getResult(): Array[Byte]

    /** Returns the data of the HTTP response as a {@link String} . <p> <b>Note</b>: This method may only be called once per response. </p>
      * @return
      *   the result as a string or null in case of a timeout or if the operation was canceled/terminated abnormally. The timeout is specified when creating the HTTP request, with
      *   {@link HttpRequest#setTimeOut(int)}
      */
    def getResultAsString(): String

    /** Returns the data of the HTTP response as an {@link InputStream} . <b><br> Warning:</b> Do not store a reference to this InputStream outside of
      * {@link HttpResponseListener#handleHttpResponse(HttpResponse)} . The underlying HTTP connection will be closed after that callback finishes executing. Reading from the InputStream after it's
      * connection has been closed will lead to exception.
      * @return
      *   An {@link InputStream} with the {@link HttpResponse} data.
      */
    def getResultAsStream(): InputStream

    /** Returns the {@link HttpStatus} containing the statusCode of the HTTP response. */
    def getStatus(): HttpStatus

    /** Returns the value of the header with the given name as a {@link String} , or null if the header is not set. See {@link HttpResponseHeader} .
      */
    def getHeader(name: String): String

    /** Returns a Map of the headers. The keys are Strings that represent the header name. Each values is a List of Strings that represent the corresponding header values. See
      * {@link HttpResponseHeader} .
      */
    def getHeaders(): Map[String, List[String]]
  }

  /** Contains getters and setters for the following parameters: <ul> <li><strong>httpMethod:</strong> GET or POST are most common, can use {@link Net.HttpMethods HttpMethods} for static
    * references</li> <li><strong>url:</strong> the url</li> <li><strong>headers:</strong> a map of the headers, setter can be called multiple times</li> <li><strong>timeout:</strong> time spent
    * trying to connect before giving up</li> <li><strong>content:</strong> A string containing the data to be used when processing the HTTP request.</li> </ul>
    *
    * Abstracts the concept of a HTTP Request:
    *
    * <pre> Map<String, String> parameters = new HashMap<String, String>(); parameters.put("user", "myuser");
    *
    * HttpRequest httpGet = new HttpRequest(HttpMethods.Get); httpGet.setUrl("http://somewhere.net"); httpGet.setContent(HttpParametersUtils.convertHttpParameters(parameters)); ...
    * Gdx.net.sendHttpRequest (httpGet, new HttpResponseListener() { public void handleHttpResponse(HttpResponse httpResponse) { status = httpResponse.getResultAsString(); //do stuff here based on
    * response }
    *
    * public void failed(Throwable t) { status = "failed"; //do stuff here based on the failed attempt } }); </pre>
    */
  class HttpRequest() extends sge.utils.Pool.Poolable {

    private var httpMethod: String                      = scala.compiletime.uninitialized
    private var url:        String                      = scala.compiletime.uninitialized
    private var headers:    mutable.Map[String, String] = mutable.Map[String, String]()
    private var timeOut:    Int                         = 0

    private var content:       String      = scala.compiletime.uninitialized
    private var contentStream: InputStream = scala.compiletime.uninitialized
    private var contentLength: Long        = 0

    private var followRedirects:    Boolean = true
    private var includeCredentials: Boolean = false

    /** Creates a new HTTP request with the specified HTTP method, see {@link HttpMethods} .
      * @param httpMethod
      *   This is the HTTP method for the request, see {@link HttpMethods}
      */
    def this(httpMethod: String) = {
      this()
      this.httpMethod = httpMethod
    }

    /** Sets the URL of the HTTP request.
      * @param url
      *   The URL to set.
      */
    def setUrl(url: String): Unit =
      this.url = url

    /** Sets a header to this HTTP request, see {@link HttpRequestHeader} .
      * @param name
      *   the name of the header.
      * @param value
      *   the value of the header.
      */
    def setHeader(name: String, value: String): Unit =
      headers.put(name, value)

    /** Sets the content to be used in the HTTP request.
      * @param content
      *   A string encoded in the corresponding Content-Encoding set in the headers, with the data to send with the HTTP request. For example, in case of HTTP GET, the content is used as the query
      *   string of the GET while on a HTTP POST it is used to send the POST data.
      */
    def setContent(content: String): Unit =
      this.content = content

    /** Sets the content as a stream to be used for a POST for example, to transmit custom data.
      * @param contentStream
      *   The stream with the content data.
      */
    def setContent(contentStream: InputStream, contentLength: Long): Unit = {
      this.contentStream = contentStream
      this.contentLength = contentLength
    }

    /** Sets the time to wait for the HTTP request to be processed, use 0 block until it is done. The timeout is used for both the timeout when establishing TCP connection, and the timeout until the
      * first byte of data is received.
      * @param timeOut
      *   the number of milliseconds to wait before giving up, 0 or negative to block until the operation is done
      */
    def setTimeOut(timeOut: Int): Unit =
      this.timeOut = timeOut

    /** Sets whether 301 and 302 redirects are followed. By default true. Can't be changed in the GWT backend because this uses XmlHttpRequests which always redirect.
      * @param followRedirects
      *   whether to follow redirects.
      * @exception
      *   IllegalArgumentException if redirection is disabled on the WebGL backend.
      */
    def setFollowRedirects(followRedirects: Boolean): Unit =
      // TODO: Add proper platform detection when available
      this.followRedirects = followRedirects

    /** Sets whether a cross-origin request will include credentials. Only used on GWT backend to allow cross-origin requests to include credentials such as cookies, authorization headers, etc...
      */
    def setIncludeCredentials(includeCredentials: Boolean): Unit =
      this.includeCredentials = includeCredentials

    /** Sets the HTTP method of the HttpRequest. */
    def setMethod(httpMethod: String): Unit =
      this.httpMethod = httpMethod

    /** Returns the timeOut of the HTTP request.
      * @return
      *   the timeOut.
      */
    def getTimeOut(): Int = timeOut

    /** Returns the HTTP method of the HttpRequest. */
    def getMethod(): String = httpMethod

    /** Returns the URL of the HTTP request. */
    def getUrl(): String = url

    /** Returns the content string to be used for the HTTP request. */
    def getContent(): String = content

    /** Returns the content stream. */
    def getContentStream(): InputStream = contentStream

    /** Returns the content length in case content is a stream. */
    def getContentLength(): Long = contentLength

    /** Returns a Map<String, String> with the headers of the HTTP request. */
    def getHeaders(): mutable.Map[String, String] = headers

    /** Returns whether 301 and 302 redirects are followed. By default true. Whether to follow redirects. */
    def getFollowRedirects(): Boolean = followRedirects

    /** Returns whether a cross-origin request will include credentials. By default false. */
    def getIncludeCredentials(): Boolean = includeCredentials

    override def reset(): Unit = {
      httpMethod = null
      url = null
      headers.clear()
      timeOut = 0

      content = null
      contentStream = null
      contentLength = 0

      followRedirects = true
    }
  }

  /** Listener to be able to do custom logic once the {@link HttpResponse} is ready to be processed, register it with {@link Net#sendHttpRequest(HttpRequest, HttpResponseListener)} .
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
