/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/math/FloatCounter.java
 * Original authors: xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: mean field changed from nullable WindowedMean to Option[WindowedMean]
 *   Idiom: split packages
 *   Convention: extends Pool.Poolable — given Poolable[FloatCounter] auto-derived via Poolable.fromTrait
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * AUDIT: PASS — All fields/methods ported: count, total, min, max, average, latest, value,
 * mean, put, reset, toString. INTENTIONAL: mean is Option[WindowedMean] instead of nullable.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 94
 * Covenant-baseline-methods: FloatCounter,average,count,latest,max,mean,min,put,reset,toString,total,value
 * Covenant-source-reference: com/badlogic/gdx/math/FloatCounter.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
 */
package sge
package math

import sge.utils.Pool.Poolable

/** Track properties of a stream of float values. The properties (total value, minimum, etc) are updated as values are {@link #put(float)} into the stream.
  *
  * @author
  *   xoppa (original implementation)
  */
class FloatCounter(windowSize: Int) extends Poolable {

  /** The amount of values added */
  var count: Int = 0

  /** The sum of all values */
  var total: Float = 0f

  /** The smallest value */
  var min: Float = 0f

  /** The largest value */
  var max: Float = 0f

  /** The average value (total / count) */
  var average: Float = 0f

  /** The latest raw value */
  var latest: Float = 0f

  /** The current windowed mean value */
  var value: Float = 0f

  /** Provides access to the WindowedMean if any (can be null) */
  val mean: Option[WindowedMean] = if (windowSize > 1) Some(WindowedMean(windowSize)) else None

  reset()

  /** Add a value and update all fields.
    * @param value
    *   The value to add
    */
  def put(value: Float): Unit = {
    latest = value
    total += value
    count += 1
    average = total / count

    mean match {
      case Some(m) =>
        m.addValue(value)
        this.value = m.mean
      case None =>
        this.value = latest
    }

    if (mean.isEmpty || mean.get.hasEnoughData()) {
      if (this.value < min) min = this.value
      if (this.value > max) max = this.value
    }
  }

  /** Reset all values to their default value. */
  override def reset(): Unit = {
    count = 0
    total = 0f
    min = Float.MaxValue
    max = -Float.MaxValue
    average = 0f
    latest = 0f
    value = 0f
    mean.foreach(_.clear())
  }

  override def toString: String =
    s"FloatCounter{count=$count, total=$total, min=$min, max=$max, average=$average, latest=$latest, value=$value}"
}
