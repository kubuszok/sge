/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/scenes/scene2d/ui/Value.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: abstract class -> SAM trait (b336fb6); null -> Nullable; Fixed.valueOf cache uses Nullable[Fixed]
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 126
 * Covenant-baseline-methods: Fixed,Value,cache,get,maxHeight,maxWidth,minHeight,minWidth,percentHeight,percentWidth,prefHeight,prefWidth,toString,valueOf,zero
 * Covenant-source-reference: com/badlogic/gdx/scenes/scene2d/ui/Value.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 1ccbdf4da0c6869f404093ff338733597a28ed0c
 */
package sge
package scenes
package scene2d
package ui

import sge.scenes.scene2d.utils.Layout
import sge.utils.Nullable

/** A lazily-computed value, allowing the value to be resolved on request. Values can be provided an actor for context to reduce the number of value instances that need to be created and reduce
  * verbosity in code that specifies values.
  * @author
  *   Nathan Sweet
  */
trait Value {

  /** @param context
    *   May be null.
    */
  def get(context: Nullable[Actor] = Nullable.empty): Float
}

object Value {

  /** A value that is always zero. */
  val zero: Fixed = Fixed(0)

  /** A fixed value that is not computed each time it is used.
    * @author
    *   Nathan Sweet
    */
  class Fixed(private val value: Float) extends Value {

    def get(context: Nullable[Actor]): Float = value

    override def toString: String = value.toString
  }

  object Fixed {
    private val cache: Array[Nullable[Fixed]] = Array.fill[Nullable[Fixed]](111)(Nullable.empty)

    def valueOf(value: Float): Fixed =
      if (value == 0) zero
      else if (value >= -10 && value <= 100 && value == value.toInt.toFloat) {
        val idx = value.toInt + 10
        cache(idx).getOrElse {
          val fixed = Fixed(value)
          cache(idx) = Nullable(fixed)
          fixed
        }
      } else Fixed(value)
  }

  /** Value that is the minWidth of the actor in the cell. */
  val minWidth: Value = context =>
    context.fold(0f) {
      case l: Layout => l.minWidth
      case a => a.width
    }

  /** Value that is the minHeight of the actor in the cell. */
  val minHeight: Value = context =>
    context.fold(0f) {
      case l: Layout => l.minHeight
      case a => a.height
    }

  /** Value that is the prefWidth of the actor in the cell. */
  val prefWidth: Value = context =>
    context.fold(0f) {
      case l: Layout => l.prefWidth
      case a => a.width
    }

  /** Value that is the prefHeight of the actor in the cell. */
  val prefHeight: Value = context =>
    context.fold(0f) {
      case l: Layout => l.prefHeight
      case a => a.height
    }

  /** Value that is the maxWidth of the actor in the cell. */
  val maxWidth: Value = context =>
    context.fold(0f) {
      case l: Layout => l.maxWidth
      case a => a.width
    }

  /** Value that is the maxHeight of the actor in the cell. */
  val maxHeight: Value = context =>
    context.fold(0f) {
      case l: Layout => l.maxHeight
      case a => a.height
    }

  /** Returns a value that is a percentage of the actor's width. */
  def percentWidth(percent: Float): Value = context => context.map(_.width * percent).getOrElse(0f)

  /** Returns a value that is a percentage of the actor's height. */
  def percentHeight(percent: Float): Value = context => context.map(_.height * percent).getOrElse(0f)

  /** Returns a value that is a percentage of the specified actor's width. The context actor is ignored. */
  def percentWidth(percent: Float, actor: Actor): Value = _ => actor.width * percent

  /** Returns a value that is a percentage of the specified actor's height. The context actor is ignored. */
  def percentHeight(percent: Float, actor: Actor): Value = _ => actor.height * percent
}
