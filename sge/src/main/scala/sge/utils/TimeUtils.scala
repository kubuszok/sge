/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/TimeUtils.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: Java `final class` with static methods -> Scala `object`; opaque Millis/Nanos for type-safe time values
 *   Idiom: split packages
 *   TODO: delegate to scala-java-time (io.github.cquiroz %%% "scala-java-time" % "2.6.0") -- cross-platform JVM/JS/Native; see docs/improvements/dependencies.md B1
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package utils

/** Wrapper around System.nanoTime() and System.currentTimeMillis(). Use this if you want to be compatible across all platforms!
  * @author
  *   mzechner (original implementation)
  */
object TimeUtils {

  /** @return The current value of the system timer, in nanoseconds. */
  def nanoTime(): Nanos = Nanos(System.nanoTime())

  /** @return the difference, measured in milliseconds, between the current time and midnight, January 1, 1970 UTC. */
  def millis(): Millis = Millis(System.currentTimeMillis())

  /** Convert nanoseconds time to milliseconds
    * @param nanos
    *   must be nanoseconds
    * @return
    *   time value in milliseconds
    */
  def nanosToMillis(nanos: Nanos): Millis = nanos.toMillis

  /** Convert milliseconds time to nanoseconds
    * @param millis
    *   must be milliseconds
    * @return
    *   time value in nanoseconds
    */
  def millisToNanos(millis: Millis): Nanos = millis.toNanos

  /** Get the time in nanos passed since a previous time
    * @param prevTime
    *   \- must be nanoseconds
    * @return
    *   \- time passed since prevTime in nanoseconds
    */
  def timeSinceNanos(prevTime: Nanos): Nanos = nanoTime() - prevTime

  /** Get the time in millis passed since a previous time
    * @param prevTime
    *   \- must be milliseconds
    * @return
    *   \- time passed since prevTime in milliseconds
    */
  def timeSinceMillis(prevTime: Millis): Millis = millis() - prevTime
}
