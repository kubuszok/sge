/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/TimeUtils.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package utils

/** Wrapper around System.nanoTime() and System.currentTimeMillis(). Use this if you want to be compatible across all platforms!
  * @author
  *   mzechner (original implementation)
  */
object TimeUtils {

  /** @return The current value of the system timer, in nanoseconds. */
  def nanoTime(): Long = System.nanoTime()

  /** @return the difference, measured in milliseconds, between the current time and midnight, January 1, 1970 UTC. */
  def millis(): Long = System.currentTimeMillis()

  private val nanosPerMilli = 1000000L

  /** Convert nanoseconds time to milliseconds
    * @param nanos
    *   must be nanoseconds
    * @return
    *   time value in milliseconds
    */
  def nanosToMillis(nanos: Long): Long = nanos / nanosPerMilli

  /** Convert milliseconds time to nanoseconds
    * @param millis
    *   must be milliseconds
    * @return
    *   time value in nanoseconds
    */
  def millisToNanos(millis: Long): Long = millis * nanosPerMilli

  /** Get the time in nanos passed since a previous time
    * @param prevTime
    *   \- must be nanoseconds
    * @return
    *   \- time passed since prevTime in nanoseconds
    */
  def timeSinceNanos(prevTime: Long): Long = nanoTime() - prevTime

  /** Get the time in millis passed since a previous time
    * @param prevTime
    *   \- must be milliseconds
    * @return
    *   \- time passed since prevTime in milliseconds
    */
  def timeSinceMillis(prevTime: Long): Long = millis() - prevTime
}
