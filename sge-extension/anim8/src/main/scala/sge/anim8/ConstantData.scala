/*
 * Ported from anim8-gdx - https://github.com/tommyettinger/anim8-gdx
 * Original authors: Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package anim8

/** Meant to store large constant arrays, for internal use. Stores a large palette preload code, [[ENCODED_SNUGGLY]], as well as the blue noise data used by BLUE_NOISE, CHAOTIC_NOISE, NEUE, SCATTER,
  * DODGY, and WREN dithers.
  *
  * Data is loaded from binary resource files packaged alongside the class.
  */
object ConstantData {

  private def loadResource(name: String): Array[Byte] = {
    val is = getClass.getResourceAsStream(name)
    if (is == null) {
      throw new RuntimeException(s"anim8: Missing resource: $name")
    }
    try
      is.readAllBytes()
    finally
      is.close()
  }

  /** The encoded (many) bytes of the palette mapping for Snuggly255, a quasi-random-then-optimized palette. Note: Uses the OklabCareful metric from ColorWeaver, not the simple RGB one used in
    * PaletteReducer.
    */
  val ENCODED_SNUGGLY: Array[Byte] = loadResource("encoded_snuggly.bin")

  /** A byte array as a 64x64 grid of bytes. When arranged into a grid, the bytes will follow a blue noise frequency (in this case, they will have a triangular distribution for its bytes, so values
    * near 0 are much more common). This is used to create [[TRI_BLUE_NOISE_MULTIPLIERS]].
    */
  val TRI_BLUE_NOISE: Array[Byte] = loadResource("tri_blue_noise.bin")

  /** Like [[TRI_BLUE_NOISE]], but with a different ordering of values. */
  val TRI_BLUE_NOISE_B: Array[Byte] = loadResource("tri_blue_noise_b.bin")

  /** Like [[TRI_BLUE_NOISE]], but with yet another different ordering of values. */
  val TRI_BLUE_NOISE_C: Array[Byte] = loadResource("tri_blue_noise_c.bin")

  /** A float array used as multipliers for the TRI_BLUE_NOISE array. */
  val TRI_BLUE_NOISE_MULTIPLIERS: Array[Float] = computeMultipliers(TRI_BLUE_NOISE)

  /** A float array used as multipliers for the TRI_BLUE_NOISE_B array. */
  val TRI_BLUE_NOISE_MULTIPLIERS_B: Array[Float] = computeMultipliers(TRI_BLUE_NOISE_B)

  /** A float array used as multipliers for the TRI_BLUE_NOISE_C array. */
  val TRI_BLUE_NOISE_MULTIPLIERS_C: Array[Float] = computeMultipliers(TRI_BLUE_NOISE_C)

  private def computeMultipliers(noise: Array[Byte]): Array[Float] = {
    val arr = new Array[Float](noise.length)
    var i   = 0
    while (i < noise.length) {
      arr(i) = Math.exp(OtherMath.probit((noise(i) + 128.5) * (1.0 / 256.0)) * 0.5).toFloat
      i += 1
    }
    arr
  }
}
