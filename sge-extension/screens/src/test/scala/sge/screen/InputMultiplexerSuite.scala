/*
 * Ported from libgdx-screenmanager - https://github.com/crykn/libgdx-screenmanager
 * Original test: de/eskalon/commons/input/BasicInputMultiplexerTest.java
 * Original authors: damios
 * Licensed under the Apache License, Version 2.0
 *
 * In the original, BasicInputMultiplexer was a thin wrapper around InputMultiplexer
 * that added batch addProcessors/removeProcessors. In SGE, InputMultiplexer is used
 * directly. This test covers the equivalent operations.
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package screen

class InputMultiplexerSuite extends munit.FunSuite {

  test("addProcessors, removeProcessors, addProcessor") {
    val mult = InputMultiplexer()
    assertEquals(mult.processors.size, 0)

    // Batch add via addAll (equivalent to original addProcessors(Array))
    val p1 = new InputProcessor {}
    val p2 = new InputProcessor {}
    mult.addProcessor(p1)
    mult.addProcessor(p2)
    assertEquals(mult.processors.size, 2)

    // Batch remove (equivalent to original removeProcessors(Array))
    mult.removeProcessor(p1)
    mult.removeProcessor(p2)
    assertEquals(mult.processors.size, 0)

    // Add individual processors
    mult.addProcessor(new InputProcessor {})
    mult.addProcessor(new InputProcessor {})
    mult.addProcessor(new InputProcessor {})
    assertEquals(mult.processors.size, 3)

    // Remove all (equivalent to original removeProcessors())
    mult.clear()
    assertEquals(mult.processors.size, 0)
  }
}
