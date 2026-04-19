/*
 * Ported from noise4j - https://github.com/czyzby/noise4j
 * Original authors: czyzby
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 195
 * Covenant-baseline-methods: DefaultNoiseAlgorithmProvider,NoiseAlgorithmProvider,NoiseGenerator,PI,_algorithmProvider,algorithmProvider,algorithmProvider_,bottomInterpolation,consume,factorialX,factorialY,finalInterpolation,gen,generate,getInstance,instance,interpolate,modifier,noise,noiseBottom,noiseBottomRight,noiseCenter,noiseRight,radius,regionX,regionY,seed,smoothNoise,topInterpolation
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package noise
package generator
package noise

import sge.noise.generator.util.Generators

/** Divides grid into equal regions. Assigns semi-random value to each region using a noise function. Interpolates the value according to neighbor regions' values. Unless regions are too small, this
  * usually results in a smooth map with logical region transitions. During map generation, you usually trigger the [[radius]] and [[modifier]], invoking generation multiple times - each time with
  * lower modifier. This allows to generate a map with logical transitions, while keeping the map interesting thanks to further iterations with lower radius.
  *
  * @author
  *   MJ
  */
class NoiseGenerator extends AbstractGenerator with Grid.CellConsumer {

  private var _algorithmProvider: NoiseGenerator.NoiseAlgorithmProvider = new NoiseGenerator.DefaultNoiseAlgorithmProvider

  /** Size of a single generation region. The bigger, the more smooth the map seems. Setting it to one effectively turns the map into semi-random noise with no smoothing whatsoever.
    */
  var radius: Int = 0

  /** Relevance of the generation stage. Each grid cell will be increased with a semi-random value on scale from 0 to this modifier.
    */
  var modifier: Float = 0f

  /** Prime number. Random seed used by the noise function. */
  var seed: Int = 0

  /** @param algorithmProvider
    *   handles interpolation and noise math.
    * @see
    *   [[NoiseGenerator.DefaultNoiseAlgorithmProvider]]
    */
  def algorithmProvider_=(algorithmProvider: NoiseGenerator.NoiseAlgorithmProvider): Unit =
    _algorithmProvider = algorithmProvider

  def algorithmProvider: NoiseGenerator.NoiseAlgorithmProvider = _algorithmProvider

  override def generate(grid: Grid): Unit = {
    if (seed == 0) {
      seed = Generators.rollSeed()
    }
    grid.forEach(this)
  }

  override def consume(grid: Grid, x: Int, y: Int, value: Float): Boolean = {
    // Region index:
    val regionX = x / radius
    val regionY = y / radius
    // Distance from the start of the region:
    val factorialX = x / radius.toFloat - regionX
    val factorialY = y / radius.toFloat - regionY
    // Generated noises. Top and left noises are handled (already interpolated) by the other neighbors.
    val noiseCenter      = _algorithmProvider.smoothNoise(this, regionX, regionY)
    val noiseRight       = _algorithmProvider.smoothNoise(this, regionX + 1, regionY)
    val noiseBottom      = _algorithmProvider.smoothNoise(this, regionX, regionY + 1)
    val noiseBottomRight = _algorithmProvider.smoothNoise(this, regionX + 1, regionY + 1)
    // Noise interpolations:
    val topInterpolation    = _algorithmProvider.interpolate(noiseCenter, noiseRight, factorialX)
    val bottomInterpolation = _algorithmProvider.interpolate(noiseBottom, noiseBottomRight, factorialX)
    val finalInterpolation  = _algorithmProvider.interpolate(topInterpolation, bottomInterpolation, factorialY)
    // Modifying current cell value according to the generation mode:
    modifyCell(grid, x, y, (finalInterpolation + 1f) / 2f * modifier)
    Grid.CellConsumer.Continue
  }
}

object NoiseGenerator {

  private var instance: NoiseGenerator = scala.compiletime.uninitialized

  /** Not thread-safe. Uses static generator instance. Since this method provides only basic settings, creating or obtaining an instance of the generator is generally preferred.
    *
    * @param grid
    *   will contain generated values.
    * @param radius
    *   see [[NoiseGenerator.radius]]
    * @param modifier
    *   see [[NoiseGenerator.modifier]]
    */
  def generate(grid: Grid, radius: Int, modifier: Float): Unit =
    generate(grid, radius, modifier, Generators.rollSeed())

  /** Not thread-safe. Uses static generator instance.
    *
    * @param grid
    *   will contain generated values.
    * @param radius
    *   see [[NoiseGenerator.radius]]
    * @param modifier
    *   see [[NoiseGenerator.modifier]]
    * @param seed
    *   see [[NoiseGenerator.seed]]
    */
  private def generate(grid: Grid, radius: Int, modifier: Float, seed: Int): Unit = {
    val gen = getInstance()
    gen.radius = radius
    gen.modifier = modifier
    gen.seed = seed
    gen.generate(grid)
  }

  /** @return
    *   static instance of the generator. Not thread-safe.
    */
  def getInstance(): NoiseGenerator = {
    if (instance == null) {
      instance = new NoiseGenerator
    }
    instance
  }

  /** Interface providing functions necessary for map generation.
    *
    * @author
    *   MJ
    */
  trait NoiseAlgorithmProvider {

    /** Semi-random function. Consumes two parameters, always returning the same result for the same set of numbers.
      *
      * @param generator
      *   its settings should be honored. Random seed is usually used for the noise calculation.
      * @param x
      *   position on the X axis.
      * @param y
      *   position on the Y axis.
      * @return
      *   noise value.
      */
    def noise(generator: NoiseGenerator, x: Int, y: Int): Float

    /** Semi-random function. Consumes two parameters, always returning the same result for the same set of numbers. Noise is dependent on the neighbors, ensuring that drastic changes are rather rare.
      *
      * @param generator
      *   its settings should be honored. Random seed is usually used for the noise calculation.
      * @param x
      *   position on the X axis.
      * @param y
      *   position on the Y axis.
      * @return
      *   smoothed noise value.
      */
    def smoothNoise(generator: NoiseGenerator, x: Int, y: Int): Float

    /** @param start
      *   range start.
      * @param end
      *   range end.
      * @param factorial
      *   [0,1), distance of the current point from the start.
      * @return
      *   interpolated current value, [start,end].
      */
    def interpolate(start: Float, end: Float, factorial: Float): Float
  }

  /** Uses a custom noise function and cos interpolation.
    *
    * @author
    *   MJ
    */
  class DefaultNoiseAlgorithmProvider extends NoiseAlgorithmProvider {
    private val PI: Float = Math.PI.toFloat

    // HERE BE DRAGONS. AND MAGIC NUMBERS.
    override def noise(generator: NoiseGenerator, x: Int, y: Int): Float = {
      val n = x + generator.seed + y * generator.seed
      1.0f - (n * (n * n * 15731 + 789221) + 1376312589 & 0x7fffffff) / 1073741824.0f
    }

    override def smoothNoise(generator: NoiseGenerator, x: Int, y: Int): Float =
      // Corners:
      (noise(generator, x - 1, y - 1) + noise(generator, x + 1, y - 1) +
        noise(generator, x - 1, y + 1) + noise(generator, x + 1, y + 1)) / 16f +
        // Sides:
        (noise(generator, x - 1, y) + noise(generator, x + 1, y) +
          noise(generator, x, y - 1) + noise(generator, x, y + 1)) / 8f +
        // Center:
        noise(generator, x, y) / 4f

    override def interpolate(start: Float, end: Float, factorial: Float): Float = {
      val modificator = (1f - Generators.getCalculator.cos(factorial * PI)) * 0.5f
      start * (1f - modificator) + end * modificator
    }
  }
}
