/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red suite for ISS-717 — GLTFExtras dropped the public keys()/entries() API.
 *
 * Original (original-src/gdx-gltf/gltf/src/net/mgsx/gltf/data/GLTFExtras.java:25-42):
 *   public Array<String>    keys()    // one entry name per child of `value`
 *   public Array<JsonValue> entries() // one JsonValue per child of `value`
 * The port (GLTFExtras.scala) carries only `value`, so a consumer cannot
 * enumerate the extra properties without hand-walking the JSON AST. The covenant
 * baseline-methods list on the port reads `GLTFExtras,value`, i.e. it was
 * re-stamped to the shrunk surface, masking the drop (see report — covenant
 * correction is out-of-territory).
 *
 * This is an API-presence (compile-red) test: it references
 * GLTFExtras.keys / GLTFExtras.entries, which do not exist yet, so this file does
 * not compile until the methods are restored. It is committed AFTER the runtime
 * red suites (which are independently verifiable at the previous commit) exactly
 * because it breaks module compilation until the fix lands.
 *
 * Method names are parens-less per this codebase's convention for zero-arg Java
 * methods (cf. Scene.getDirectionalLightCount). keys returns String elements;
 * entries is checked only for cardinality to avoid over-pinning its element type.
 *
 * Platform scope: shared (src/test/scala) — pure data model, no GL.
 */
package sge
package gltf
package data

import sge.utils.{ Json, readFromString }
import sge.utils.given
import lowlevel.Nullable

class GLTFExtrasKeysEntriesRedSuite extends munit.FunSuite {

  private def extrasOf(json: String): GLTFExtras = {
    val e = new GLTFExtras()
    e.value = Nullable(readFromString[Json](json))
    e
  }

  test("ISS-717: GLTFExtras.keys enumerates the extra-property names (GLTFExtras.java:25-31)") {
    val extras = extrasOf("""{"author":"me","frames":3}""")
    val ks     = extras.keys
    assertEquals(ks.size, 2)
    val names = scala.collection.mutable.Set.empty[String]
    var i     = 0
    while (i < ks.size) {
      names += ks(i)
      i += 1
    }
    assertEquals(names.toSet, Set("author", "frames"))
  }

  test("ISS-717: GLTFExtras.entries enumerates one value per extra property (GLTFExtras.java:33-42)") {
    val extras = extrasOf("""{"author":"me","frames":3}""")
    assertEquals(extras.entries.size, 2)
  }
}
