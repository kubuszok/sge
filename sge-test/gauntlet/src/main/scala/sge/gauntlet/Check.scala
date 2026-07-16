/*
 * SGE Gauntlet — single named assertion produced by a FeatureProbe.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gauntlet

/** A single named assertion produced by [[FeatureProbe.verify]].
  *
  * @param name
  *   short kebab-case check name, unique within the probe (e.g. "center-pixel-red")
  * @param passed
  *   whether the assertion held
  * @param expected
  *   human-readable expected value
  * @param actual
  *   human-readable actual value
  */
final case class Check(name: String, passed: Boolean, expected: String, actual: String)

object Check {

  /** An equality check rendered with `toString` on both sides. */
  def eq[A](name: String, expected: A, actual: A): Check =
    Check(name, expected == actual, expected.toString, actual.toString)

  /** A boolean condition check. */
  def cond(name: String, passed: Boolean, expected: String, actual: String): Check =
    Check(name, passed, expected, actual)
}
