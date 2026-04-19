/*
 * Ported from gdx-vfx - https://github.com/crashinvaders/gdx-vfx
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 35
 * Covenant-baseline-methods: VfxGlViewport,height,set,toString,width,x,y
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package vfx
package gl

class VfxGlViewport {
  var x:      Int = 0
  var y:      Int = 0
  var width:  Int = 0
  var height: Int = 0

  def set(x: Int, y: Int, width: Int, height: Int): VfxGlViewport = {
    this.x = x
    this.y = y
    this.width = width
    this.height = height
    this
  }

  def set(viewport: VfxGlViewport): VfxGlViewport = {
    this.x = viewport.x
    this.y = viewport.y
    this.width = viewport.width
    this.height = viewport.height
    this
  }

  override def toString: String =
    "x=" + x + ", y=" + y + ", width=" + width + ", height=" + height
}
