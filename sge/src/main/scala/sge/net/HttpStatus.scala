/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/net/HttpStatus.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: Java `class HttpStatus(int)` → `opaque type HttpStatus = Int` with extension methods
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 203
 * Covenant-baseline-methods: HttpStatus,SC_ACCEPTED,SC_BAD_GATEWAY,SC_BAD_REQUEST,SC_CONFLICT,SC_CONTINUE,SC_CREATED,SC_EXPECTATION_FAILED,SC_FAILED_DEPENDENCY,SC_FORBIDDEN,SC_GATEWAY_TIMEOUT,SC_GONE,SC_HTTP_VERSION_NOT_SUPPORTED,SC_INSUFFICIENT_SPACE_ON_RESOURCE,SC_INSUFFICIENT_STORAGE,SC_INTERNAL_SERVER_ERROR,SC_LENGTH_REQUIRED,SC_LOCKED,SC_METHOD_FAILURE,SC_METHOD_NOT_ALLOWED,SC_MOVED_PERMANENTLY,SC_MOVED_TEMPORARILY,SC_MULTIPLE_CHOICES,SC_MULTI_STATUS,SC_NON_AUTHORITATIVE_INFORMATION,SC_NOT_ACCEPTABLE,SC_NOT_FOUND,SC_NOT_IMPLEMENTED,SC_NOT_MODIFIED,SC_NO_CONTENT,SC_OK,SC_PARTIAL_CONTENT,SC_PAYMENT_REQUIRED,SC_PRECONDITION_FAILED,SC_PROCESSING,SC_PROXY_AUTHENTICATION_REQUIRED,SC_REQUESTED_RANGE_NOT_SATISFIABLE,SC_REQUEST_TIMEOUT,SC_REQUEST_TOO_LONG,SC_REQUEST_URI_TOO_LONG,SC_RESET_CONTENT,SC_SEE_OTHER,SC_SERVICE_UNAVAILABLE,SC_SWITCHING_PROTOCOLS,SC_TEMPORARY_REDIRECT,SC_UNAUTHORIZED,SC_UNPROCESSABLE_ENTITY,SC_UNSUPPORTED_MEDIA_TYPE,SC_USE_PROXY,apply,statusCode
 * Covenant-source-reference: com/badlogic/gdx/net/HttpStatus.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
 */
package sge
package net

/** This implementation is widely based Apache's HttpStatus which uses license (Apache license 2.0) - http://www.apache.org/licenses/LICENSE-2.0 For more information on the Apache Software Foundation,
  * please see <http://www.apache.org/>.
  *
  * Contains information about the HTTP status line returned with the {@link HttpResponse} after a {@link HttpRequest} was performed. Also contains constants enumerating the HTTP Status Codes. All
  * status codes defined in RFC1945 (HTTP/1.0), RFC2616 (HTTP/1.1), and RFC2518 (WebDAV) are listed.
  */
opaque type HttpStatus = Int
object HttpStatus {

  def apply(statusCode: Int): HttpStatus = statusCode

  given utils.MkArray[HttpStatus] = utils.MkArray.mkInt.asInstanceOf[utils.MkArray[HttpStatus]]

  extension (status: HttpStatus) {

    /** Returns the status code of the HTTP response, normally 2xx status codes indicate success while 4xx and 5xx indicate client and server errors, respectively (see <a
      * href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html">HTTP/1.1: Status Code Definitions</a> for more information about HTTP status codes).
      */
    def statusCode: Int = status
  }

  /*
   * Constants enumerating the HTTP status codes. All status codes defined in RFC1945 (HTTP/1.0), RFC2616 (HTTP/1.1), and RFC2518
   * (WebDAV) are listed.
   */

  // --- 1xx Informational ---

  /** <tt>100 Continue</tt> (HTTP/1.1 - RFC 2616) */
  val SC_CONTINUE = 100

  /** <tt>101 Switching Protocols</tt> (HTTP/1.1 - RFC 2616) */
  val SC_SWITCHING_PROTOCOLS = 101

  /** <tt>102 Processing</tt> (WebDAV - RFC 2518) */
  val SC_PROCESSING = 102

  // --- 2xx Success ---

  /** <tt>200 OK</tt> (HTTP/1.0 - RFC 1945) */
  val SC_OK = 200

  /** <tt>201 Created</tt> (HTTP/1.0 - RFC 1945) */
  val SC_CREATED = 201

  /** <tt>202 Accepted</tt> (HTTP/1.0 - RFC 1945) */
  val SC_ACCEPTED = 202

  /** <tt>203 Non Authoritative Information</tt> (HTTP/1.1 - RFC 2616) */
  val SC_NON_AUTHORITATIVE_INFORMATION = 203

  /** <tt>204 No Content</tt> (HTTP/1.0 - RFC 1945) */
  val SC_NO_CONTENT = 204

  /** <tt>205 Reset Content</tt> (HTTP/1.1 - RFC 2616) */
  val SC_RESET_CONTENT = 205

  /** <tt>206 Partial Content</tt> (HTTP/1.1 - RFC 2616) */
  val SC_PARTIAL_CONTENT = 206

  /** <tt>207 Multi-Status</tt> (WebDAV - RFC 2518) or <tt>207 Partial Update OK</tt> (HTTP/1.1 - draft-ietf-http-v11-spec-rev-01?)
    */
  val SC_MULTI_STATUS = 207

  // --- 3xx Redirection ---

  /** <tt>300 Mutliple Choices</tt> (HTTP/1.1 - RFC 2616) */
  val SC_MULTIPLE_CHOICES = 300

  /** <tt>301 Moved Permanently</tt> (HTTP/1.0 - RFC 1945) */
  val SC_MOVED_PERMANENTLY = 301

  /** <tt>302 Moved Temporarily</tt> (Sometimes <tt>Found</tt>) (HTTP/1.0 - RFC 1945) */
  val SC_MOVED_TEMPORARILY = 302

  /** <tt>303 See Other</tt> (HTTP/1.1 - RFC 2616) */
  val SC_SEE_OTHER = 303

  /** <tt>304 Not Modified</tt> (HTTP/1.0 - RFC 1945) */
  val SC_NOT_MODIFIED = 304

  /** <tt>305 Use Proxy</tt> (HTTP/1.1 - RFC 2616) */
  val SC_USE_PROXY = 305

  /** <tt>307 Temporary Redirect</tt> (HTTP/1.1 - RFC 2616) */
  val SC_TEMPORARY_REDIRECT = 307

  // --- 4xx Client Error ---

  /** <tt>400 Bad Request</tt> (HTTP/1.1 - RFC 2616) */
  val SC_BAD_REQUEST = 400

  /** <tt>401 Unauthorized</tt> (HTTP/1.0 - RFC 1945) */
  val SC_UNAUTHORIZED = 401

  /** <tt>402 Payment Required</tt> (HTTP/1.1 - RFC 2616) */
  val SC_PAYMENT_REQUIRED = 402

  /** <tt>403 Forbidden</tt> (HTTP/1.0 - RFC 1945) */
  val SC_FORBIDDEN = 403

  /** <tt>404 Not Found</tt> (HTTP/1.0 - RFC 1945) */
  val SC_NOT_FOUND = 404

  /** <tt>405 Method Not Allowed</tt> (HTTP/1.1 - RFC 2616) */
  val SC_METHOD_NOT_ALLOWED = 405

  /** <tt>406 Not Acceptable</tt> (HTTP/1.1 - RFC 2616) */
  val SC_NOT_ACCEPTABLE = 406

  /** <tt>407 Proxy Authentication Required</tt> (HTTP/1.1 - RFC 2616) */
  val SC_PROXY_AUTHENTICATION_REQUIRED = 407

  /** <tt>408 Request Timeout</tt> (HTTP/1.1 - RFC 2616) */
  val SC_REQUEST_TIMEOUT = 408

  /** <tt>409 Conflict</tt> (HTTP/1.1 - RFC 2616) */
  val SC_CONFLICT = 409

  /** <tt>410 Gone</tt> (HTTP/1.1 - RFC 2616) */
  val SC_GONE = 410

  /** <tt>411 Length Required</tt> (HTTP/1.1 - RFC 2616) */
  val SC_LENGTH_REQUIRED = 411

  /** <tt>412 Precondition Failed</tt> (HTTP/1.1 - RFC 2616) */
  val SC_PRECONDITION_FAILED = 412

  /** <tt>413 Request Entity Too Large</tt> (HTTP/1.1 - RFC 2616) */
  val SC_REQUEST_TOO_LONG = 413

  /** <tt>414 Request-URI Too Long</tt> (HTTP/1.1 - RFC 2616) */
  val SC_REQUEST_URI_TOO_LONG = 414

  /** <tt>415 Unsupported Media Type</tt> (HTTP/1.1 - RFC 2616) */
  val SC_UNSUPPORTED_MEDIA_TYPE = 415

  /** <tt>416 Requested Range Not Satisfiable</tt> (HTTP/1.1 - RFC 2616) */
  val SC_REQUESTED_RANGE_NOT_SATISFIABLE = 416

  /** <tt>417 Expectation Failed</tt> (HTTP/1.1 - RFC 2616) */
  val SC_EXPECTATION_FAILED = 417

  /** Static constant for a 418 error. <tt>418 Unprocessable Entity</tt> (WebDAV drafts?) or <tt>418 Reauthentication Required</tt> (HTTP/1.1 drafts?)
    */
  // not used
  // val SC_UNPROCESSABLE_ENTITY = 418

  /** Static constant for a 419 error. <tt>419 Insufficient Space on Resource</tt> (WebDAV - draft-ietf-webdav-protocol-05?) or <tt>419 Proxy Reauthentication Required</tt> (HTTP/1.1 drafts?)
    */
  val SC_INSUFFICIENT_SPACE_ON_RESOURCE = 419

  /** Static constant for a 420 error. <tt>420 Method Failure</tt> (WebDAV - draft-ietf-webdav-protocol-05?) */
  val SC_METHOD_FAILURE = 420

  /** <tt>422 Unprocessable Entity</tt> (WebDAV - RFC 2518) */
  val SC_UNPROCESSABLE_ENTITY = 422

  /** <tt>423 Locked</tt> (WebDAV - RFC 2518) */
  val SC_LOCKED = 423

  /** <tt>424 Failed Dependency</tt> (WebDAV - RFC 2518) */
  val SC_FAILED_DEPENDENCY = 424

  // --- 5xx Server Error ---

  /** <tt>500 Server Error</tt> (HTTP/1.0 - RFC 1945) */
  val SC_INTERNAL_SERVER_ERROR = 500

  /** <tt>501 Not Implemented</tt> (HTTP/1.0 - RFC 1945) */
  val SC_NOT_IMPLEMENTED = 501

  /** <tt>502 Bad Gateway</tt> (HTTP/1.0 - RFC 1945) */
  val SC_BAD_GATEWAY = 502

  /** <tt>503 Service Unavailable</tt> (HTTP/1.0 - RFC 1945) */
  val SC_SERVICE_UNAVAILABLE = 503

  /** <tt>504 Gateway Timeout</tt> (HTTP/1.1 - RFC 2616) */
  val SC_GATEWAY_TIMEOUT = 504

  /** <tt>505 HTTP Version Not Supported</tt> (HTTP/1.1 - RFC 2616) */
  val SC_HTTP_VERSION_NOT_SUPPORTED = 505

  /** <tt>507 Insufficient Storage</tt> (WebDAV - RFC 2518) */
  val SC_INSUFFICIENT_STORAGE = 507
}
