/*
 * SGE - Scala Game Engine
 * copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (type aliases decoupling sge.net from sttp namespaces)
 *   Convention: private[sge] so tests in sge.net can also use them
 *   Idiom: split packages
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 41
 * Covenant-baseline-methods: SttpHeader,SttpMethod,SttpResponse,SttpStatusCode,SttpUri,sttpAsByteArrayAlways,sttpBasicRequest
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-06-12
 */
package sge
package net

// sttp client types
private[sge] type SttpResponse[T] = sttp.client4.Response[T]
private[sge] val SttpResponse: sttp.client4.Response.type = sttp.client4.Response
private[sge] type SttpRequest[T] = sttp.client4.Request[T]
private[sge] inline def sttpBasicRequest = sttp.client4.basicRequest
// Raw-bytes response description used on both success and error paths — the
// LibGDX getResult() contract (NetJavaImpl.java:62-78 reads the connection
// stream into a byte[] with no charset), keeping binary downloads byte-exact
// instead of round-tripping through a UTF-8 String (ISS-521).
private[sge] inline def sttpAsByteArrayAlways = sttp.client4.asByteArrayAlways

// sttp model types
private[sge] type SttpMethod = sttp.model.Method
private[sge] val SttpMethod: sttp.model.Method.type = sttp.model.Method
private[sge] type SttpUri = sttp.model.Uri
private[sge] val SttpUri: sttp.model.Uri.type = sttp.model.Uri
private[sge] type SttpHeader = sttp.model.Header
private[sge] val SttpHeader: sttp.model.Header.type = sttp.model.Header
private[sge] type SttpStatusCode = sttp.model.StatusCode
private[sge] val SttpStatusCode: sttp.model.StatusCode.type = sttp.model.StatusCode
private[sge] type SttpRequestMetadata = sttp.model.RequestMetadata
