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
 *   SGE keeps that per-instance class — ported as sge.utils.Logger, which still
 *   gates by level and forwards its survivors here — and replaces only the
 *   Gdx.app.log/debug/error SINK with this facade over a proper logging
 *   framework (scribe), which adds its own per-category level filtering,
 *   structured logging, and pluggable output targets. Per-tag filtering is thus
 *   available both via Logger instances and scribe's standard mechanisms.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 33
 * Covenant-baseline-methods: Log,debug,error,info,isDebugEnabled,trace,warn
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package utils

/** SGE's logging facade — the sanctioned stand-in for LibGDX's `Gdx.app.log` / `debug` / `error` application logging. Call it directly (`Log.info(...)`, `Log.error(...)`); the message argument is
  * by-name, so it is only built when the backend decides to emit.
  *
  * Messages forward to a per-platform backend: scribe on JVM, Native, and JS; and `android.util.Log` (via reflection) on Android, where scribe is excluded from the DEX. Level filtering is configured
  * through the backend, not per call site.
  *
  * @note
  *   LibGDX: replaces the `Gdx.app.log` / `debug` / `error` sink. The per-instance `com.badlogic.gdx.utils.Logger` is itself ported as [[Logger]], which gates by level and then forwards its surviving
  *   messages here (see the architecture note above).
  * @note
  *   Platform: the backend is `android.util.Log` on Android and scribe on every other platform.
  */
object Log {
  inline def info(msg:  => String):               Unit    = LogPlatform.info(msg)
  inline def warn(msg:  => String):               Unit    = LogPlatform.warn(msg)
  inline def error(msg: => String):               Unit    = LogPlatform.error(msg)
  inline def error(msg: => String, t: Throwable): Unit    = LogPlatform.error(msg, t)
  inline def debug(msg: => String):               Unit    = LogPlatform.debug(msg)
  inline def trace(msg: => String):               Unit    = LogPlatform.trace(msg)
  inline def isDebugEnabled:                      Boolean = LogPlatform.isDebugEnabled
}
