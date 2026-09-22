/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/ApplicationAdapter.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: ApplicationAdapter -> ApplicationListenerDefaults
 *   Convention: a trait of empty defaults that a listener mixes in, instead of an abstract class it extends
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 22
 * Covenant-baseline-methods: ApplicationListenerDefaults,create,dispose,pause,render,resize,resume
 * Covenant-source-reference: com/badlogic/gdx/ApplicationAdapter.java
 * Covenant-verified: 2026-09-22
 */
package sge

trait ApplicationListenerDefaults extends ApplicationListener {
  def create(): Unit  = ()
  def resize(width: sge.Pixels, height: sge.Pixels): Unit = ()
  def render(): Unit  = ()
  def pause(): Unit   = ()
  def resume(): Unit  = ()
  def dispose(): Unit = ()
}
