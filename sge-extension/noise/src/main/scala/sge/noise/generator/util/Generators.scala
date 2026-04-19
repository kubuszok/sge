/*
 * Ported from noise4j - https://github.com/czyzby/noise4j
 * Original authors: czyzby
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 160
 * Covenant-baseline-methods: Calculator,DefaultSeedBitLength,Generators,calculator,cos,getCalculator,getRandom,i,random,randomElement,randomIndex,randomInt,randomPercent,rng,rollSeed,setCalculator,setRandom,shuffle,sin
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package noise
package generator
package util

import java.math.BigInteger
import java.util.{ List as JList, Random }

/** Utilities for map generators.
  *
  * When used in SGE applications, it is a good idea to replace [[Random]] and [[Calculator]] instances with values and methods of `MathUtils` class, which provides both more efficient random
  * implementation and sin/cos look-up tables. See [[setRandom]] and [[setCalculator]].
  *
  * @author
  *   MJ
  */
object Generators {

  /** Length of generated random seeds with [[rollSeed()]]. Depending on the algorithm, this value might vary - in which case [[rollSeed(Int)]] method should be used instead.
    */
  val DefaultSeedBitLength: Int = 16

  private var random:     Random     = scala.compiletime.uninitialized
  private var calculator: Calculator = scala.compiletime.uninitialized

  /** @return
    *   [[Random]] instance shared by the generators. Not thread-safe, unless modified with [[setRandom]].
    */
  def getRandom: Random = {
    if (random == null) {
      random = new Random()
    }
    random
  }

  /** @param random
    *   will be available through [[getRandom]] instance. This method allows to provide a thread-safe, secure or specialized random instance.
    */
  def setRandom(random: Random): Unit =
    this.random = random

  /** @return
    *   static instance of immutable, thread-safe [[Calculator]], providing common math functions.
    */
  def getCalculator: Calculator = {
    if (calculator == null) {
      calculator = new Calculator {
        override def sin(radians: Float): Float = Math.sin(radians.toDouble).toFloat
        override def cos(radians: Float): Float = Math.cos(radians.toDouble).toFloat
      }
    }
    calculator
  }

  /** @param calculator
    *   instance of immutable, thread-safe [[Calculator]], providing common math functions.
    */
  def setCalculator(calculator: Calculator): Unit =
    this.calculator = calculator

  /** @return
    *   a random probable prime with [[DefaultSeedBitLength]] bits.
    * @see
    *   [[BigInteger.probablePrime]]
    */
  def rollSeed(): Int = rollSeed(DefaultSeedBitLength)

  /** @param seedBitLength
    *   bits length of the generated seed.
    * @return
    *   a random probable prime.
    * @see
    *   [[BigInteger.probablePrime]]
    */
  def rollSeed(seedBitLength: Int): Int =
    BigInteger.probablePrime(seedBitLength, getRandom).intValue()

  /** @param min
    *   minimum possible random value.
    * @param max
    *   maximum possible random value.
    * @return
    *   random value in the specified range.
    */
  def randomInt(min: Int, max: Int): Int =
    min + getRandom.nextInt(max - min + 1)

  /** @param list
    *   a list of elements. Cannot be null or empty.
    * @return
    *   random list element.
    */
  def randomElement[A](list: JList[A]): A =
    list.get(randomIndex(list))

  /** @param list
    *   a list of elements. Cannot be null or empty.
    * @return
    *   random index of an element stored in the list.
    */
  def randomIndex(list: JList[?]): Int =
    getRandom.nextInt(list.size())

  /** @return
    *   a random float in range of 0f (inclusive) to 1f (exclusive).
    */
  def randomPercent(): Float =
    getRandom.nextFloat()

  /** Collection shuffling method.
    *
    * @param list
    *   its elements will be shuffled.
    * @return
    *   passed list, for chaining.
    */
  def shuffle[A](list: JList[A]): JList[A] = {
    val rng = getRandom
    var i   = list.size()
    while (i > 1) {
      val swap = rng.nextInt(i)
      list.set(swap, list.set(i - 1, list.get(swap)))
      i -= 1
    }
    list
  }

  /** Allows to calculate common generators' functions. By implementing this trait, you can replace these common functions with more efficient solutions - for example, a look-up table.
    *
    * Default implementation uses [[Math]] methods.
    *
    * @author
    *   MJ
    * @see
    *   [[Generators.setCalculator]]
    */
  trait Calculator {

    /** @param radians
      *   angle in radians.
      * @return
      *   sin value.
      */
    def sin(radians: Float): Float

    /** @param radians
      *   angle in radians.
      * @return
      *   cos value.
      */
    def cos(radians: Float): Float
  }
}
