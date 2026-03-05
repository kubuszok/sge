/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package noop

class PrintApplicationLoggerTest extends munit.FunSuite {

  test("log does not throw") {
    PrintApplicationLogger.log("TAG", "message")
  }

  test("log with exception does not throw") {
    PrintApplicationLogger.log("TAG", "message", RuntimeException("test"))
  }

  test("error does not throw") {
    PrintApplicationLogger.error("TAG", "message")
  }

  test("error with exception does not throw") {
    PrintApplicationLogger.error("TAG", "message", RuntimeException("test"))
  }

  test("debug does not throw") {
    PrintApplicationLogger.debug("TAG", "message")
  }

  test("debug with exception does not throw") {
    PrintApplicationLogger.debug("TAG", "message", RuntimeException("test"))
  }

  test("PrintApplicationLogger is an ApplicationLogger") {
    val logger: ApplicationLogger = PrintApplicationLogger
    assert(logger != null)
  }
}
