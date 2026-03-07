/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/math/RandomXS128.java
 * Original authors: Inferno, davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * AUDIT: PASS — All methods ported: nextLong, next, nextInt, nextInt(n), nextLong(n),
 * nextDouble, nextFloat, nextBoolean, nextBytes, setSeed, setState, getState, murmurHash3.
 * Constructors: no-arg, (seed), (seed0, seed1).
 */
package sge
package math

import java.util.Random

/** This class implements the xorshift128+ algorithm that is a very fast, top-quality 64-bit pseudo-random number generator. The quality of this PRNG is much higher than {@link Random} 's, and its
  * cycle length is 2<sup>128</sup>&nbsp;&minus;&nbsp;1, which is more than enough for any single-thread application. More details and algorithms can be found <a
  * href="http://xorshift.di.unimi.it/">here</a>. <p> Instances of RandomXS128 are not thread-safe.
  *
  * @author
  *   Inferno (original implementation)
  * @author
  *   davebaol (original implementation)
  */
class RandomXS128() extends Random {

  /** Normalization constant for double. */
  private val NORM_DOUBLE = 1.0 / (1L << 53)

  /** Normalization constant for float. */
  private val NORM_FLOAT = 1.0 / (1L << 24)

  /** The first half of the internal state of this pseudo-random number generator. */
  private var seed0: Long = scala.compiletime.uninitialized

  /** The second half of the internal state of this pseudo-random number generator. */
  private var seed1: Long = scala.compiletime.uninitialized

  // Default initialization
  setSeed(new Random().nextLong())

  /** Creates a new random number generator using a single {@code long} seed.
    * @param seed
    *   the initial seed
    */
  def this(seed: Long) = {
    this()
    setSeed(seed)
  }

  /** Creates a new random number generator using two {@code long} seeds.
    * @param seed0
    *   the first part of the initial seed
    * @param seed1
    *   the second part of the initial seed
    */
  def this(seed0: Long, seed1: Long) = {
    this()
    setState(seed0, seed1)
  }

  /** Returns the next pseudo-random, uniformly distributed {@code long} value from this random number generator's sequence. <p> Subclasses should override this, as this is used by all other methods.
    */
  override def nextLong(): Long = {
    var s1 = this.seed0
    val s0 = this.seed1
    this.seed0 = s0
    s1 ^= s1 << 23
    this.seed1 = s1 ^ s0 ^ (s1 >>> 17) ^ (s0 >>> 26)
    this.seed1 + s0
  }

  /** This protected method is final because, contrary to the superclass, it's not used anymore by the other methods. */
  final override protected def next(bits: Int): Int =
    (nextLong() & ((1L << bits) - 1)).toInt

  /** Returns the next pseudo-random, uniformly distributed {@code int} value from this random number generator's sequence. <p> This implementation uses {@link #nextLong()} internally.
    */
  override def nextInt(): Int =
    nextLong().toInt

  /** Returns a pseudo-random, uniformly distributed {@code int} value between 0 (inclusive) and the specified value (exclusive), drawn from this random number generator's sequence. <p> This
    * implementation uses {@link #nextLong()} internally.
    * @param n
    *   the positive bound on the random number to be returned.
    * @return
    *   the next pseudo-random {@code int} value between {@code 0} (inclusive) and {@code n} (exclusive).
    */
  override def nextInt(n: Int): Int =
    nextLong(n).toInt

  /** Returns a pseudo-random, uniformly distributed {@code long} value between 0 (inclusive) and the specified value (exclusive), drawn from this random number generator's sequence. The algorithm
    * used to generate the value guarantees that the result is uniform, provided that the sequence of 64-bit values produced by this generator is. <p> This implementation uses {@link #nextLong()}
    * internally.
    * @param n
    *   the positive bound on the random number to be returned.
    * @return
    *   the next pseudo-random {@code long} value between {@code 0} (inclusive) and {@code n} (exclusive).
    */
  override def nextLong(n: Long): Long = {
    if (n <= 0) throw new IllegalArgumentException("n must be positive")
    var bits:  Long = 0
    var value: Long = 0
    var continue = true
    while (continue) {
      bits = nextLong() >>> 1
      value = bits % n
      continue = bits - value + (n - 1) < 0
    }
    value
  }

  /** Returns a pseudo-random, uniformly distributed {@code double} value between 0.0 and 1.0 from this random number generator's sequence. <p> This implementation uses {@link #nextLong()} internally.
    */
  override def nextDouble(): Double =
    (nextLong() >>> 11) * NORM_DOUBLE

  /** Returns a pseudo-random, uniformly distributed {@code float} value between 0.0 and 1.0 from this random number generator's sequence. <p> This implementation uses {@link #nextLong()} internally.
    */
  override def nextFloat(): Float =
    ((nextLong() >>> 40) * NORM_FLOAT).toFloat

  /** Returns a pseudo-random, uniformly distributed {@code boolean} value from this random number generator's sequence. <p> This implementation uses {@link #nextLong()} internally.
    */
  override def nextBoolean(): Boolean =
    (nextLong() & 1) != 0

  /** Generates random bytes and places them into a user-supplied byte array. The number of random bytes produced is equal to the length of the byte array. <p> This implementation uses
    * {@link #nextLong()} internally.
    */
  override def nextBytes(bytes: Array[Byte]): Unit = {
    var n = 0
    var i = bytes.length
    while (i != 0) {
      n = if (i < 8) i else 8 // min(i, 8);
      var bits = nextLong()
      var j    = n
      while (j > 0) {
        j -= 1
        i -= 1
        bytes(i) = bits.toByte
        bits >>= 8
      }
    }
  }

  /** Sets the internal seed of this generator based on the given {@code long} value. <p> The given seed is passed twice through a hash function. This way, if the user passes a small value we avoid
    * the short irregular transient associated with states having a very small number of bits set.
    * @param seed
    *   a nonzero seed for this generator (if zero, the generator will be seeded with {@link Long#MIN_VALUE} ).
    */
  override def setSeed(seed: Long): Unit = {
    val seedValue = if (seed == 0) Long.MinValue else seed
    val seed0     = murmurHash3(seedValue)
    setState(seed0, murmurHash3(seed0))
  }

  /** Sets the internal state of this generator.
    * @param seed0
    *   the first part of the internal state
    * @param seed1
    *   the second part of the internal state
    */
  def setState(seed0: Long, seed1: Long): Unit = {
    this.seed0 = seed0
    this.seed1 = seed1
  }

  /** Returns the internal seeds to allow state saving.
    * @param seed
    *   must be 0 or 1, designating which of the 2 long seeds to return
    * @return
    *   the internal seed that can be used in setState
    */
  def getState(seed: Int): Long =
    if (seed == 0) seed0 else seed1

  private def murmurHash3(x: Long): Long = {
    var hash = x
    hash ^= hash >>> 33
    hash *= 0xff51afd7ed558ccdL
    hash ^= hash >>> 33
    hash *= 0xc4ceb9fe1a85ec53L
    hash ^= hash >>> 33
    hash
  }
}
