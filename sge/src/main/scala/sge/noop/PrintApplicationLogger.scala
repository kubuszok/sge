/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Source: backends/gdx-backend-headless/.../HeadlessApplicationLogger.java
 *   Renames: HeadlessApplicationLogger -> PrintApplicationLogger
 *   Convention: reusable across headless + desktop backends
 *   Audited: 2026-03-05
 */
package sge
package noop

/** A simple [[ApplicationLogger]] that prints to stdout/stderr. Used by [[HeadlessApplication]] and available for any backend that doesn't need a platform-specific logger.
  */
object PrintApplicationLogger extends ApplicationLogger {

  override def log(tag: String, message: String): Unit =
    System.out.println(s"[$tag] $message")

  override def log(tag: String, message: String, exception: Throwable): Unit = {
    System.out.println(s"[$tag] $message")
    exception.printStackTrace(System.out)
  }

  override def error(tag: String, message: String): Unit =
    System.err.println(s"[$tag] $message")

  override def error(tag: String, message: String, exception: Throwable): Unit = {
    System.err.println(s"[$tag] $message")
    exception.printStackTrace(System.err)
  }

  override def debug(tag: String, message: String): Unit =
    System.out.println(s"[$tag] $message")

  override def debug(tag: String, message: String, exception: Throwable): Unit = {
    System.out.println(s"[$tag] $message")
    exception.printStackTrace(System.out)
  }
}
