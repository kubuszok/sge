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
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package utils

enum SgeError extends Exception {
  case FileReadError(file: files.FileHandle, message: String, cause: Option[Throwable] = None)
  case FileWriteError(file: files.FileHandle, message: String, cause: Option[Throwable] = None)
  case MathError(message: String, cause: Option[Throwable] = None)
  case NetworkError(message: String, cause: Option[Throwable] = None)
  case SerializationError(message: String, cause: Option[Throwable] = None)
  case InvalidInput(message: String, cause: Option[Throwable] = None)
  case GraphicsError(message: String, cause: Option[Throwable] = None)
}
