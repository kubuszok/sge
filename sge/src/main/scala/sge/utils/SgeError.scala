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
 * Covenant-baseline-loc: 45
 * Covenant-baseline-methods: SgeError
 * Covenant-source-reference: com/badlogic/gdx/utils/GdxRuntimeException.java
 * Covenant-verified: 2026-07-17
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
 */
package sge
package utils

/** SGE's typed error hierarchy: a Scala 3 `enum` of the exception variants the engine raises, each carrying a message and an optional cause. Match on a variant to react to a specific failure, or
  * catch [[SgeError]] to handle any of them.
  *
  * Capability-not-available conditions are, by convention (ISS-771), signalled through [[Unsupported]] rather than a raw JDK runtime exception or [[InvalidInput]] (which means bad user input); some
  * residual sites still throw a raw JDK exception for the same condition. See [[Unsupported]]'s own note for the convention.
  *
  * @note
  *   LibGDX: replaces `com.badlogic.gdx.utils.GdxRuntimeException`, merged with `com.badlogic.gdx.utils.SerializationException` (now [[SerializationError]]).
  */
enum SgeError(message: String, cause: Option[Throwable]) extends Exception(message, cause.orNull) {
  case FileReadError(file: files.FileHandle, message: String, cause: Option[Throwable] = None) extends SgeError(message, cause)
  case FileWriteError(file: files.FileHandle, message: String, cause: Option[Throwable] = None) extends SgeError(message, cause)
  case MathError(message: String, cause: Option[Throwable] = None) extends SgeError(message, cause)
  case NetworkError(message: String, cause: Option[Throwable] = None) extends SgeError(message, cause)
  case SerializationError(message: String, cause: Option[Throwable] = None) extends SgeError(message, cause)
  case InvalidInput(message: String, cause: Option[Throwable] = None) extends SgeError(message, cause)
  case GraphicsError(message: String, cause: Option[Throwable] = None) extends SgeError(message, cause)
  case AudioError(message: String, cause: Option[Throwable] = None) extends SgeError(message, cause)

  /** Signals that a requested capability is not available on the current platform or backend — for example audio recording or TCP sockets in the browser, or external/local files on a filesystem-less
    * platform.
    *
    * PROJECT-WIDE CONVENTION (ISS-771): every capability-not-available site must signal via this variant — never a raw JDK `java.lang` runtime exception (which sits outside SGE's own error hierarchy)
    * and never [[InvalidInput]] (which means *bad user input*, a semantically distinct condition). The `message` names the capability that is unavailable.
    */
  case Unsupported(message: String, cause: Option[Throwable] = None) extends SgeError(message, cause)
}
