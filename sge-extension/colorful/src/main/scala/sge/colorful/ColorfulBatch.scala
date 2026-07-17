/*
 * SGE-original file - no colorful-gdx counterpart. `ColorfulBatchLike` is an SGE marker
 * trait introduced to unify the per-color-space batch implementations; colorful-gdx has no
 * equivalent type (its ColorfulBatch classes are per-space). The actual colorful-gdx ports
 * (Original authors: Tommy Ettinger) live in the per-color-space sub-packages:
 * sge.colorful.oklab.ColorfulBatch and sge.colorful.rgb.ColorfulBatch, which carry the
 * colorful-gdx attribution and source references. This file therefore is SGE-original, matching
 * its `Covenant-source-reference: SGE-original` below.
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 14
 * Covenant-baseline-methods: ColorfulBatchLike
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package colorful

/** Marker trait for ColorfulBatch implementations in the colorful extension. See [[oklab.ColorfulBatch]] and [[rgb.ColorfulBatch]] for the actual shader constants and batch creation utilities for
  * each color space.
  */
trait ColorfulBatchLike
