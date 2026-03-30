/*
 * Ported from colorful-gdx - https://github.com/tommyettinger/colorful-gdx
 * Original authors: Tommy Ettinger
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package colorful

/** Marker trait for ColorfulBatch implementations in the colorful extension. See [[oklab.ColorfulBatch]] and [[rgb.ColorfulBatch]] for the actual shader constants and batch creation utilities for
  * each color space.
  */
trait ColorfulBatchLike
