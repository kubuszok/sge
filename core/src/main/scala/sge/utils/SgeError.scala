/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/GdxRuntimeException.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
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
