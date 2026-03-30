/*
 * Ported from colorful-gdx - https://github.com/tommyettinger/colorful-gdx
 * Original authors: Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package colorful
package oklab

/** Stores gamut data for the Oklab color space, used to determine valid color boundaries. The GAMUT_DATA array contains 65536 bytes encoding the maximum chroma for each combination of lightness
  * (upper 8 bits of index) and hue angle (lower 8 bits of index).
  */
private[oklab] object Gamut {

  /** 65536-byte array of gamut boundary data for Oklab, loaded from Base64-encoded resource. */
  val GAMUT_DATA: Array[Byte] = {
    val stream = getClass.getResourceAsStream("/sge/colorful/oklab/gamut.b64")
    if (stream != null) {
      try {
        val b64String = new String(stream.readAllBytes(), "UTF-8")
        java.util.Base64.getDecoder.decode(b64String.trim)
      } finally
        stream.close()
    } else {
      // Fallback: empty gamut data (all colors treated as in-gamut)
      new Array[Byte](65536)
    }
  }
}
