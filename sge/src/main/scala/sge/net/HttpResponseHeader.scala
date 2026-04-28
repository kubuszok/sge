/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/net/HttpResponseHeader.java
 * Original authors: Daniel Holderbaum
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: Java `interface` with `static final` constants → `object` with `val`
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 171
 * Covenant-baseline-methods: AcceptRanges,AccessControlAllowOrigin,Age,Allow,CacheControl,Connection,ContentDisposition,ContentEncoding,ContentLanguage,ContentLength,ContentLocation,ContentMD5,ContentRange,ContentType,Date,ETag,Expires,HttpResponseHeader,LastModified,Link,Location,P3P,Pragma,ProxyAuthenticate,Refresh,RetryAfter,Server,SetCookie,Status,StrictTransportSecurity,Trailer,TransferEncoding,Upgrade,Vary,Via,WWWAuthenticate,Warning
 * Covenant-source-reference: com/badlogic/gdx/net/HttpResponseHeader.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
 */
package sge
package net

/** A list of common response header constants of the HTTP protocol. See http://en.wikipedia.org/wiki/List_of_HTTP_header_fields.
  * @author
  *   Daniel Holderbaum (original implementation)
  */
object HttpResponseHeader {

  /** Specifying which web sites can participate in cross-origin resource sharing. <p> Example: Access-Control-Allow-Origin: *
    */
  val AccessControlAllowOrigin = "Access-Control-Allow-Origin"

  /** What partial content range types this server supports. <p> Example: Accept-Ranges: bytes
    */
  val AcceptRanges = "Accept-Ranges"

  /** The age the object has been in a proxy cache in seconds. <p> Example: Age: 12
    */
  val Age = "Age"

  /** Valid actions for a specified resource. To be used for a 405 Method not allowed. <p> Example: Allow: GET, HEAD
    */
  val Allow = "Allow"

  /** Tells all caching mechanisms from server to client whether they may cache this object. It is measured in seconds. <p> Example: Cache-Control: max-age=3600
    */
  val CacheControl = "Cache-Control"

  /** Options that are desired for the connection. <p> Example: Connection: close
    */
  val Connection = "Connection"

  /** The type of encoding used on the data. See HTTP compression. <p> Example: Content-Encoding: gzip
    */
  val ContentEncoding = "Content-Encoding"

  /** The language the content is in. <p> Example: Content-Language: da
    */
  val ContentLanguage = "Content-Language"

  /** The length of the response body in octets (8-bit bytes). <p> Example: Content-Length: 348
    */
  val ContentLength = "Content-Length"

  /** An alternate location for the returned data. <p> Example: Content-Location: /index.htm
    */
  val ContentLocation = "Content-Location"

  /** A Base64-encoded binary MD5 sum of the content of the response. <p> Example: Content-MD5: Q2hlY2sgSW50ZWdyaXR5IQ==
    */
  val ContentMD5 = "Content-MD5"

  /** An opportunity to raise a "File Download" dialogue box for a known MIME type with binary format or suggest a filename for dynamic content. Quotes are necessary with special characters. <p>
    * Example: Content-Disposition: attachment; filename="fname.ext"
    */
  val ContentDisposition = "Content-Disposition"

  /** Where in a full body message this partial message belongs. <p> Example: Content-Range: bytes 21010-47021/47022
    */
  val ContentRange = "Content-Range"

  /** The MIME type of this content. <p> Example: Content-Type: text/html; charset=utf-8
    */
  val ContentType = "Content-Type"

  /** The date and time that the message was sent (in "HTTP-date" format as defined by RFC 7231). <p> Example: Date: Tue, 15 Nov 1994 08:12:31 GMT
    */
  val Date = "Date"

  /** An identifier for a specific version of a resource, often a message digest. <p> Example: ETag: "737060cd8c284d8af7ad3082f209582d"
    */
  val ETag = "ETag"

  /** Gives the date/time after which the response is considered stale. <p> Example: Expires: Thu, 01 Dec 1994 16:00:00 GMT
    */
  val Expires = "Expires"

  /** The last modified date for the requested object (in "HTTP-date" format as defined by RFC 7231). <p> Example: Last-Modified: Tue, 15 Nov 1994 12:45:26 GMT
    */
  val LastModified = "Last-Modified"

  /** Used to express a typed relationship with another resource, where the relation type is defined by RFC 5988. <p> Example: Link: </feed>; rel="alternate"
    */
  val Link = "Link"

  /** Used in redirection, or when a new resource has been created. <p> Example: Location: http://www.w3.org/pub/WWW/People.html
    */
  val Location = "Location"

  /** This field is supposed to set P3P policy, in the form of P3P:CP="your_compact_policy". However, P3P did not take off, most browsers have never fully implemented it, a lot of websites set this
    * field with fake policy text, that was enough to fool browsers the existence of P3P policy and grant permissions for third party cookies. <p> Example: P3P: CP= "This is not a P3P policy! See
    * http://www.google.com/support/accounts/bin/answer.py?hl=en&answer=151657 for more info."
    */
  val P3P = "P3P"

  /** Implementation-specific fields that may have various effects anywhere along the request-response chain. <p> Example: Pragma: no-cache
    */
  val Pragma = "Pragma"

  /** Request authentication to access the proxy. <p> Example: Proxy-Authenticate: Basic
    */
  val ProxyAuthenticate = "Proxy-Authenticate"

  /** Used in redirection, or when a new resource has been created. This refresh redirects after 5 seconds. <p> Example: Refresh: 5; url=http://www.w3.org/pub/WWW/People.html
    */
  val Refresh = "Refresh"

  /** If an entity is temporarily unavailable, this instructs the client to try again later. Value could be a specified period of time (in seconds) or a HTTP-date. <p> Example: Example 1: Retry-After:
    * 120Example 2: Retry-After: Fri, 07 Nov 2014 23:59:59 GMT
    */
  val RetryAfter = "Retry-After"

  /** A name for the server. <p> Example: Server: Apache/2.4.1 (Unix)
    */
  val Server = "Server"

  /** An HTTP cookie. <p> Example: Set-Cookie: UserID=JohnDoe; Max-Age=3600; Version=1
    */
  val SetCookie = "Set-Cookie"

  /** CGI header field specifying the status of the HTTP response. Normal HTTP responses use a separate "Status-Line" instead, defined by RFC 7230. <p> Example: Status: 200 OK
    */
  val Status = "Status"

  /** A HSTS Policy informing the HTTP client how long to cache the HTTPS only policy and whether this applies to subdomains. <p> Example: Strict-Transport-Security: max-age=16070400;
    * includeSubDomains
    */
  val StrictTransportSecurity = "Strict-Transport-Security"

  /** The Trailer general field value indicates that the given set of header fields is present in the trailer of a message encoded with chunked transfer coding. <p> Example: Trailer: Max-Forwards
    */
  val Trailer = "Trailer"

  /** The form of encoding used to safely transfer the entity to the user. Currently defined methods are: chunked, compress, deflate, gzip, identity. <p> Example: Transfer-Encoding: chunked
    */
  val TransferEncoding = "Transfer-Encoding"

  /** Ask the client to upgrade to another protocol. <p> Example: Upgrade: HTTP/2.0, SHTTP/1.3, IRC/6.9, RTA/x11
    */
  val Upgrade = "Upgrade"

  /** Tells downstream proxies how to match future request headers to decide whether the cached response can be used rather than requesting a fresh one from the origin server. <p> Example: Vary: *
    */
  val Vary = "Vary"

  /** Informs the client of proxies through which the response was sent. <p> Example: Via: 1.0 fred, 1.1 example.com (Apache/1.1)
    */
  val Via = "Via"

  /** A general warning about possible problems with the entity body. <p> Example: Warning: 199 Miscellaneous warning
    */
  val Warning = "Warning"

  /** Indicates the authentication scheme that should be used to access the requested entity. <p> Example: WWW-Authenticate: Basic
    */
  val WWWAuthenticate = "WWW-Authenticate"
}
