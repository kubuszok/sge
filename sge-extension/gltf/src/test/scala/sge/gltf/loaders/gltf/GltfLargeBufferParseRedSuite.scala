/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red suite for ISS-626 — GLTFJsonParser.parse uses jsoniter-scala's DEFAULT
 * ReaderConfig, whose maxCharBufSize is 4194304 chars (jsoniter-scala-core
 * 2.39.1, ReaderConfig object defaults). A real-world glTF with a large embedded
 * base64 buffer URI (e.g. original-src/gdx-gltf/gltf/testRes/benchmark/embeded/
 * four-spheres.gltf, 4.3 MB) contains a single JSON string longer than that cap,
 * so parsing dies with
 *   JsonReaderException: "too long string exceeded maxCharBufSize".
 *
 * The original gdx-gltf (libgdx JsonReader, net/mgsx/gltf/loaders/glb + gltf)
 * has no such per-string cap and loads these documents. GLTFJsonParser must pass
 * an explicit ReaderConfig with raised limits.
 *
 * This test synthesises the smallest reproducing document: a valid glTF whose one
 * buffer URI is a base64 data URI with a payload just over the default cap. It
 * reproduces the exact failing constraint (a single string token > 4194304 chars)
 * while staying well under maxBufSize (33554432 bytes), so ONLY maxCharBufSize is
 * exercised. Any reasonable raise of the char-buffer limit makes it parse.
 *
 * Platform scope: shared (src/test/scala) — the parser and the char-buffer cap
 * are identical on JVM, JS and Native.
 */
package sge
package gltf
package loaders
package gltf

import sge.gltf.loaders.gltf.GLTFJsonParser
import lowlevel.Nullable

class GltfLargeBufferParseRedSuite extends munit.FunSuite {

  // jsoniter-scala default maxCharBufSize is 4194304; go comfortably over it but
  // stay far below maxBufSize (33554432) so the byte buffer is never the constraint.
  private val payloadChars = 4500000

  private def gltfWithLargeBuffer(): String = {
    val sb = new StringBuilder(payloadChars + 256)
    sb.append("""{"asset":{"version":"2.0"},"buffers":[{"uri":"data:application/octet-stream;base64,""")
    var i = 0
    // 'A' is a valid base64 character; the parser only reads it as a string token.
    while (i < payloadChars) { sb.append('A'); i += 1 }
    sb.append("""","byteLength":3}]}""")
    sb.toString
  }

  test("ISS-626: a glTF with a >4MB embedded base64 buffer URI parses (raised ReaderConfig maxCharBufSize)") {
    val gltf = GLTFJsonParser.parse(gltfWithLargeBuffer())
    assertEquals(gltf.buffers.get.size, 1)
    assert(Nullable.isDefined(gltf.buffers.get(0).uri), "the large buffer URI must survive parsing")
    assertEquals(gltf.buffers.get(0).byteLength, 3)
  }

  // GREEN control: a small buffer URI parses today — proves the fixture shape is valid glTF and
  // isolates the failure above to the string-length cap, not the document structure.
  test("ISS-626 control: a small embedded base64 buffer URI parses today (GREEN)") {
    val gltf = GLTFJsonParser.parse(
      """{"asset":{"version":"2.0"},"buffers":[{"uri":"data:application/octet-stream;base64,AAAA","byteLength":3}]}"""
    )
    assertEquals(gltf.buffers.get.size, 1)
    assert(Nullable.isDefined(gltf.buffers.get(0).uri))
  }
}
