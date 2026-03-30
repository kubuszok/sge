/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/utils/random/ (20 Java files)
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.utils.random` -> `sge.ai.utils.random`; `MathUtils` -> `sge.math.MathUtils`
 *   Merged with: All 20 distribution files consolidated into one
 *   Idiom: Java class hierarchy -> Scala trait + abstract classes + final classes
 *   Convention: split packages
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package ai
package utils
package random

import sge.math.MathUtils

// ── Distribution trait ──────────────────────────────────────────────────

/** @author davebaol (original implementation) */
trait Distribution {
  def nextInt():    Int
  def nextLong():   Long
  def nextFloat():  Float
  def nextDouble(): Double
}

// ── Float distributions ─────────────────────────────────────────────────

/** Base class for distributions whose primary type is `Float`. */
abstract class FloatDistribution extends Distribution {
  override def nextInt():    Int    = nextFloat().toInt
  override def nextLong():   Long   = nextFloat().toLong
  override def nextDouble(): Double = nextFloat().toDouble
}

/** A distribution that always returns the same float value. */
final class ConstantFloatDistribution(val value: Float) extends FloatDistribution {
  override def nextFloat(): Float = value
}

object ConstantFloatDistribution {
  val NegativeOne:   ConstantFloatDistribution = new ConstantFloatDistribution(-1)
  val Zero:          ConstantFloatDistribution = new ConstantFloatDistribution(0)
  val One:           ConstantFloatDistribution = new ConstantFloatDistribution(1)
  val ZeroPointFive: ConstantFloatDistribution = new ConstantFloatDistribution(0.5f)
}

/** A uniform float distribution in the range [low, high]. */
final class UniformFloatDistribution(val low: Float, val high: Float) extends FloatDistribution {
  def this(high: Float) = this(0, high)
  override def nextFloat(): Float = MathUtils.random(low, high)
}

/** A Gaussian (normal) float distribution. */
final class GaussianFloatDistribution(val mean: Float, val standardDeviation: Float) extends FloatDistribution {
  override def nextFloat(): Float = mean + MathUtils.randomGenerator.nextGaussian().toFloat * standardDeviation
}

object GaussianFloatDistribution {
  val StandardNormal: GaussianFloatDistribution = new GaussianFloatDistribution(0, 1)
}

/** A triangular float distribution. */
final class TriangularFloatDistribution(val low: Float, val high: Float, val mode: Float) extends FloatDistribution {
  def this(high: Float) = this(-high, high, 0f)
  def this(low:  Float, high: Float) = this(low, high, (low + high) * 0.5f)

  override def nextFloat(): Float =
    if (-low == high && mode == 0) MathUtils.randomTriangular(high) // It's faster
    else MathUtils.randomTriangular(low, high, mode)
}

// ── Double distributions ────────────────────────────────────────────────

/** Base class for distributions whose primary type is `Double`. */
abstract class DoubleDistribution extends Distribution {
  override def nextInt():   Int   = nextDouble().toInt
  override def nextLong():  Long  = nextDouble().toLong
  override def nextFloat(): Float = nextDouble().toFloat
}

/** A distribution that always returns the same double value. */
final class ConstantDoubleDistribution(val value: Double) extends DoubleDistribution {
  override def nextDouble(): Double = value
}

/** A uniform double distribution in the range [low, high]. */
final class UniformDoubleDistribution(val low: Double, val high: Double) extends DoubleDistribution {
  def this(high: Double) = this(0, high)
  override def nextDouble(): Double = low + MathUtils.randomGenerator.nextDouble() * (high - low)
}

/** A Gaussian (normal) double distribution. */
final class GaussianDoubleDistribution(val mean: Double, val standardDeviation: Double) extends DoubleDistribution {
  override def nextDouble(): Double = mean + MathUtils.randomGenerator.nextGaussian() * standardDeviation
}

object GaussianDoubleDistribution {
  val StandardNormal: GaussianDoubleDistribution = new GaussianDoubleDistribution(0, 1)
}

/** A triangular double distribution. */
final class TriangularDoubleDistribution(val low: Double, val high: Double, val mode: Double) extends DoubleDistribution {
  def this(high: Double) = this(-high, high, 0.0)
  def this(low:  Double, high: Double) = this(low, high, (low + high) * 0.5)

  override def nextDouble(): Double = {
    val u = MathUtils.randomGenerator.nextDouble()
    val d = high - low
    if (u <= (mode - low) / d) low + Math.sqrt(u * d * (mode - low))
    else high - Math.sqrt((1 - u) * d * (high - mode))
  }
}

// ── Integer distributions ───────────────────────────────────────────────

/** Base class for distributions whose primary type is `Int`. */
abstract class IntegerDistribution extends Distribution {
  override def nextLong():   Long   = nextInt().toLong
  override def nextFloat():  Float  = nextInt().toFloat
  override def nextDouble(): Double = nextInt().toDouble
}

/** A distribution that always returns the same integer value. */
final class ConstantIntegerDistribution(val value: Int) extends IntegerDistribution {
  override def nextInt(): Int = value
}

object ConstantIntegerDistribution {
  val NegativeOne: ConstantIntegerDistribution = new ConstantIntegerDistribution(-1)
  val Zero:        ConstantIntegerDistribution = new ConstantIntegerDistribution(0)
  val One:         ConstantIntegerDistribution = new ConstantIntegerDistribution(1)
}

/** A uniform integer distribution in the range [low, high]. */
final class UniformIntegerDistribution(val low: Int, val high: Int) extends IntegerDistribution {
  def this(high: Int) = this(0, high)
  override def nextInt(): Int = MathUtils.random(low, high)
}

/** A triangular integer distribution. */
final class TriangularIntegerDistribution(val low: Int, val high: Int, val mode: Float) extends IntegerDistribution {
  def this(high: Int) = this(-high, high, 0f)
  def this(low:  Int, high: Int) = this(low, high, (low + high) * 0.5f)

  override def nextInt(): Int =
    if (-low == high && mode == 0) MathUtils.randomTriangular(high.toFloat).round
    else MathUtils.randomTriangular(low.toFloat, high.toFloat, mode).round
}

// ── Long distributions ──────────────────────────────────────────────────

/** Base class for distributions whose primary type is `Long`. */
abstract class LongDistribution extends Distribution {
  override def nextInt():    Int    = nextLong().toInt
  override def nextFloat():  Float  = nextLong().toFloat
  override def nextDouble(): Double = nextLong().toDouble
}

/** A distribution that always returns the same long value. */
final class ConstantLongDistribution(val value: Long) extends LongDistribution {
  override def nextLong(): Long = value
}

object ConstantLongDistribution {
  val NegativeOne: ConstantLongDistribution = new ConstantLongDistribution(-1)
  val Zero:        ConstantLongDistribution = new ConstantLongDistribution(0)
  val One:         ConstantLongDistribution = new ConstantLongDistribution(1)
}

/** A uniform long distribution in the range [low, high]. */
final class UniformLongDistribution(val low: Long, val high: Long) extends LongDistribution {
  def this(high: Long) = this(0, high)
  override def nextLong(): Long = {
    val d = high - low
    if (d >= 0) low + (MathUtils.randomGenerator.nextDouble() * d).toLong
    else low + ((MathUtils.randomGenerator.nextDouble() * 0.5 + 0.5) * d).toLong // overflow-safe
  }
}

/** A triangular long distribution. */
final class TriangularLongDistribution(val low: Long, val high: Long, val mode: Double) extends LongDistribution {
  def this(high: Long) = this(-high, high, 0.0)
  def this(low:  Long, high: Long) = this(low, high, (low + high) * 0.5)

  override def nextLong(): Long = {
    val u = MathUtils.randomGenerator.nextDouble()
    val d = (high - low).toDouble
    if (u <= (mode - low) / d) (low + Math.sqrt(u * d * (mode - low))).toLong
    else (high - Math.sqrt((1 - u) * d * (high - mode))).toLong
  }
}
