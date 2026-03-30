/*
 * Ported from noise4j - https://github.com/czyzby/noise4j
 * Original authors: czyzby
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package noise

import java.util.Random

import sge.noise.generator.noise.NoiseGenerator
import sge.noise.generator.util.Generators

class NoiseSuite extends munit.FunSuite {

  override def beforeEach(context: BeforeEach): Unit = {
    Generators.setRandom(new Random(42L))
  }

  test("NoiseGenerator produces values in [0,1] range with single pass") {
    val grid = new Grid(64, 64)
    val gen = new NoiseGenerator
    gen.radius = 16
    gen.modifier = 1f
    gen.seed = 0 // will be auto-rolled
    gen.generate(grid)

    val arr = grid.getArray
    var i = 0
    var min = Float.MaxValue
    var max = Float.MinValue
    while (i < arr.length) {
      if (arr(i) < min) min = arr(i)
      if (arr(i) > max) max = arr(i)
      i += 1
    }
    // With modifier=1, values should be roughly in [0,1] range (some slight overshoot possible with interpolation)
    assert(min >= -0.1f, s"min value $min is too low")
    assert(max <= 1.1f, s"max value $max is too high")
  }

  test("NoiseGenerator is seed-reproducible") {
    val seed = Generators.rollSeed()

    val grid1 = new Grid(32, 32)
    val gen1 = new NoiseGenerator
    gen1.radius = 8
    gen1.modifier = 1f
    gen1.seed = seed
    gen1.generate(grid1)

    Generators.setRandom(new Random(99L)) // different random, but seed is fixed
    val grid2 = new Grid(32, 32)
    val gen2 = new NoiseGenerator
    gen2.radius = 8
    gen2.modifier = 1f
    gen2.seed = seed
    gen2.generate(grid2)

    assertEquals(grid1, grid2)
  }

  test("NoiseGenerator produces non-uniform output") {
    val grid = new Grid(32, 32)
    val gen = new NoiseGenerator
    gen.radius = 8
    gen.modifier = 1f
    gen.seed = 12347
    gen.generate(grid)

    val arr = grid.getArray
    val firstVal = arr(0)
    var allSame = true
    var i = 1
    while (i < arr.length) {
      if (arr(i) != firstVal) {
        allSame = false
      }
      i += 1
    }
    assert(!allSame, "All grid values are identical - noise generation produced uniform output")
  }

  test("static generate convenience method works") {
    val grid = new Grid(32, 32)
    NoiseGenerator.generate(grid, 8, 1f)
    val arr = grid.getArray
    var hasNonZero = false
    var i = 0
    while (i < arr.length) {
      if (arr(i) != 0f) hasNonZero = true
      i += 1
    }
    assert(hasNonZero, "Static generate produced all-zero grid")
  }
}
