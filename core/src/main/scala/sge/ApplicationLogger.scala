/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/ApplicationLogger.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: Java interface -> Scala trait
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge

/** The ApplicationLogger provides an interface for a libGDX Application to log messages and exceptions. A default implementations is provided for each backend, custom implementations can be provided
  * and set using {@link Application#setApplicationLogger(ApplicationLogger)}
  */
trait ApplicationLogger {

  /** Logs a message with a tag */
  def log(tag: String, message: String): Unit

  /** Logs a message and exception with a tag */
  def log(tag: String, message: String, exception: Throwable): Unit

  /** Logs an error message with a tag */
  def error(tag: String, message: String): Unit

  /** Logs an error message and exception with a tag */
  def error(tag: String, message: String, exception: Throwable): Unit

  /** Logs a debug message with a tag */
  def debug(tag: String, message: String): Unit

  /** Logs a debug message and exception with a tag */
  def debug(tag: String, message: String, exception: Throwable): Unit
}
