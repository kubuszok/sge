/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/GdxRuntimeException.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Merged with: `SerializationException.java` -> `SgeError.SerializationError`
 *   Renames: `GdxRuntimeException` -> `SgeError` enum; `SerializationException` -> `SgeError.SerializationError`
 *   Convention: Scala 3 `enum` extending `Exception`; typed error variants instead of generic runtime exception; `cause` uses `Option[Throwable]`
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 35
 * Covenant-baseline-methods: SgeError
 * Covenant-source-reference: com/badlogic/gdx/utils/GdxRuntimeException.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
 */
package sge
package utils

enum SgeError(message: String, cause: Option[Throwable]) extends Exception(message, cause.orNull) {
  case FileReadError(file: files.FileHandle, message: String, cause: Option[Throwable] = None) extends SgeError(message, cause)
  case FileWriteError(file: files.FileHandle, message: String, cause: Option[Throwable] = None) extends SgeError(message, cause)
  case MathError(message: String, cause: Option[Throwable] = None) extends SgeError(message, cause)
  case NetworkError(message: String, cause: Option[Throwable] = None) extends SgeError(message, cause)
  case SerializationError(message: String, cause: Option[Throwable] = None) extends SgeError(message, cause)
  case InvalidInput(message: String, cause: Option[Throwable] = None) extends SgeError(message, cause)
  case GraphicsError(message: String, cause: Option[Throwable] = None) extends SgeError(message, cause)
  case AudioError(message: String, cause: Option[Throwable] = None) extends SgeError(message, cause)
}
