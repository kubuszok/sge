/*
 * SGE logging facade — zero-overhead inline wrapper over platform-specific backends.
 *
 * JVM desktop / Native: scribe
 * JS: scribe
 * Android: android.util.Log (via reflection, scribe excluded from DEX)
 *
 * The inline methods eliminate the Log object indirection at compile time —
 * call sites compile directly to LogPlatform.xxx invocations.  The message
 * parameter is by-name so string construction is deferred until the backend
 * decides to actually log.
 *
 * Architecture divergence from LibGDX:
 *   LibGDX's Logger is a per-instance class with a tag and per-tag log level
 *   that filters messages before forwarding to Gdx.app.log/debug/error.
 *   SGE replaces this with a global singleton backed by a proper logging
 *   framework (scribe), which provides its own per-category level filtering,
 *   structured logging, and pluggable output targets. Per-tag filtering is
 *   configured through scribe's standard mechanisms rather than per-instance
 *   Logger objects.
 */
package sge
package utils

object Log {
  inline def info(msg:  => String):               Unit    = LogPlatform.info(msg)
  inline def warn(msg:  => String):               Unit    = LogPlatform.warn(msg)
  inline def error(msg: => String):               Unit    = LogPlatform.error(msg)
  inline def error(msg: => String, t: Throwable): Unit    = LogPlatform.error(msg, t)
  inline def debug(msg: => String):               Unit    = LogPlatform.debug(msg)
  inline def trace(msg: => String):               Unit    = LogPlatform.trace(msg)
  inline def isDebugEnabled:                      Boolean = LogPlatform.isDebugEnabled
}
