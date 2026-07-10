/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/Logger.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: Gdx.app.log/debug/error sink -> global sge.utils.Log facade
 *   Convention: int level field + getLevel()/setLevel() -> public `var level`;
 *     getTag() -> public `val tag`; NONE/ERROR/INFO/DEBUG log levels as
 *     companion `final val` constants (mirror Application.LOG_* values)
 *   Idiom: by-name message parameters defer string construction until the
 *     level gate passes (matches sge.utils.Log)
 *
 * Covenant: full-port
 * Covenant-baseline-loc: 70
 * Covenant-baseline-methods: DEBUG,ERROR,INFO,Logger,NONE,debug,error,info
 * Covenant-source-reference: com/badlogic/gdx/utils/Logger.java
 * Covenant-verified: 2026-07-10
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package utils

/** Per-instance logger with a tag and a log level that filters messages before forwarding the survivors to the global [[Log]] sink. Mirrors libGDX's `com.badlogic.gdx.utils.Logger`: a level of
  * [[Logger.NONE]] suppresses every message (silent), [[Logger.ERROR]] permits only errors, [[Logger.INFO]] permits info and errors, and [[Logger.DEBUG]] permits everything. Callers own an instance
  * and adjust its [[level]] to raise or silence its output without touching the shared [[Log]] configuration.
  *
  * @author
  *   Nathan Sweet (original implementation)
  */
final class Logger(val tag: String, var level: Int) {

  /** Creates a logger defaulting to [[Logger.ERROR]] (matches libGDX). */
  def this(tag: String) = this(tag, Logger.ERROR)

  def debug(message: => String): Unit =
    if (level >= Logger.DEBUG) Log.debug(tag + ": " + message)

  def debug(message: => String, exception: Throwable): Unit =
    if (level >= Logger.DEBUG) Log.debug(tag + ": " + message + "\n" + exception)

  def info(message: => String): Unit =
    if (level >= Logger.INFO) Log.info(tag + ": " + message)

  def info(message: => String, exception: Throwable): Unit =
    if (level >= Logger.INFO) Log.info(tag + ": " + message + "\n" + exception)

  def error(message: => String): Unit =
    if (level >= Logger.ERROR) Log.error(tag + ": " + message)

  def error(message: => String, exception: Throwable): Unit =
    if (level >= Logger.ERROR) Log.error(tag + ": " + message, exception)
}

object Logger {

  /** No logging (silent). Matches `Application.LOG_NONE`. */
  final val NONE: Int = 0

  /** Only errors are logged. Matches `Application.LOG_ERROR`. */
  final val ERROR: Int = 1

  /** Info and errors are logged. Matches `Application.LOG_INFO`. */
  final val INFO: Int = 2

  /** Debug, info and errors are logged. Matches `Application.LOG_DEBUG`. */
  final val DEBUG: Int = 3
}
