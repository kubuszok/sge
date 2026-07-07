/*
 * SGE — ISS-741 behavioral suppression test for sge.utils.Logger level gating.
 *
 * Follow-up to ISS-733, which restored the per-instance Logger. That suite
 * (AssetManagerLoggerIss733Suite) only pins the accessor presence + default
 * level VALUE via reflection; it never exercises the actual message
 * SUPPRESSION behaviour, so a mutation that breaks the level gate
 * (`if (level >= X)` -> `if (true)`) stays green.
 *
 * This suite installs a capturing scribe sink (the JVM backend of the global
 * sge.utils.Log — see sge/utils/LogPlatform.scala:46,54,62 routing to
 * scribe.info/error/debug) as the ONLY handler at Level.Trace, so scribe's own
 * level filter is wide open and the ONLY gate under test is Logger's own:
 *   sge/utils/Logger.scala
 *     :33  if (level >= Logger.DEBUG) Log.debug(tag + ": " + message)
 *     :39  if (level >= Logger.INFO)  Log.info (tag + ": " + message)
 *     :45  if (level >= Logger.ERROR) Log.error(tag + ": " + message)
 *   with constants NONE=0 < ERROR=1 < INFO=2 < DEBUG=3 (Logger.scala:54-63).
 *
 * gdx Logger semantics (com/badlogic/gdx/utils/Logger.java): LOG_NONE is
 * silent, LOG_ERROR permits only errors, LOG_INFO permits info + errors,
 * LOG_DEBUG permits everything. We assert on the CAPTURED records (count +
 * scribe.Level + rendered message) for each of the four levels.
 */
package sge
package utils

import scala.collection.mutable

import munit.FunSuite

class LoggerBehavioralIss741Suite extends FunSuite {

  /** The distinctive tag every scenario uses; the message the gate forwards is `tag + ": " + message` (Logger.scala:33,39,45). */
  private val Tag = "ISS-741"

  /** Installs a capturing scribe writer as the SOLE root handler at Level.Trace (so scribe never filters — Logger.scala's gate is the only filter), exercises a fresh `Logger(Tag, level)`, then
    * restores scribe's default configuration and returns the captured records.
    */
  private def captureAt(level: Int)(exercise: Logger => Unit): List[scribe.LogRecord] = {
    val captured = mutable.ListBuffer.empty[scribe.LogRecord]
    val writer   = new scribe.writer.Writer {
      override def write(record: scribe.LogRecord, output: scribe.output.LogOutput, outputFormat: scribe.output.format.OutputFormat): Unit = {
        captured += record
        ()
      }
    }
    scribe.Logger.root.clearHandlers().clearModifiers().withMinimumLevel(scribe.Level.Trace).withHandler(writer = writer, minimumLevel = Some(scribe.Level.Trace)).replace()
    try {
      exercise(new Logger(Tag, level))
      captured.toList
    } finally
      // Restore scribe to its default handler so sibling suites are unaffected.
      scribe.Logger.reset()
  }

  /** Emits one of each severity through the Logger; the level gate decides which survive to the sink. */
  private def emitAll(log: Logger): Unit = {
    log.debug("dbg")
    log.info("nfo")
    log.error("err")
  }

  // ── Logger.NONE (0) — gdx LOG_NONE: silent. ──────────────────────────────
  // Logger.scala:33/39/45 all require level >= {DEBUG=3,INFO=2,ERROR=1}; at
  // level 0 every gate is false, so NOTHING reaches the sink.
  test("ISS-741: Logger(tag, NONE) suppresses debug + info + error entirely (Logger.scala:33,39,45; gdx LOG_NONE)") {
    val records = captureAt(Logger.NONE)(emitAll)
    assertEquals(
      records.size,
      0,
      s"Logger at NONE (level 0) must forward NOTHING to Log; the gates `level >= ERROR/INFO/DEBUG` (Logger.scala:33,39,45) are all false. Captured: ${records.map(_.level.name)}."
    )
  }

  // ── Logger.ERROR (1) — gdx LOG_ERROR: only errors. ───────────────────────
  // Logger.scala:45 `level >= ERROR(1)` is true; :39 `>= INFO(2)` and
  // :33 `>= DEBUG(3)` are false.
  test("ISS-741: Logger(tag, ERROR) emits error only; debug + info suppressed (Logger.scala:45 vs :33,:39; gdx LOG_ERROR)") {
    val records = captureAt(Logger.ERROR)(emitAll)
    assertEquals(
      records.size,
      1,
      s"ERROR(1) must pass exactly the error gate (Logger.scala:45) and drop debug/info (:33,:39). Captured: ${records.map(_.level.name)}."
    )
    assertEquals(
      records.head.level,
      scribe.Level.Error,
      "the single surviving record must be at ERROR severity (Logger.error -> Log.error -> scribe.error, LogPlatform.scala:54)."
    )
    assert(
      records.head.logOutput.plainText.contains(Tag + ": err"),
      s"captured message must be the tagged error `$Tag: err` (Logger.scala:45 forwards `tag + \": \" + message`); got `${records.head.logOutput.plainText}`."
    )
    assert(
      !records.exists(_.level == scribe.Level.Debug),
      "no DEBUG record may leak at ERROR level (Logger.scala:33 gate `level >= DEBUG(3)` must be false for level 1)."
    )
    assert(
      !records.exists(_.level == scribe.Level.Info),
      "no INFO record may leak at ERROR level (Logger.scala:39 gate `level >= INFO(2)` must be false for level 1)."
    )
  }

  // ── Logger.INFO (2) — gdx LOG_INFO: info + errors. ───────────────────────
  // Logger.scala:39 `>= INFO(2)` and :45 `>= ERROR(1)` are true; :33
  // `>= DEBUG(3)` is false.
  test("ISS-741: Logger(tag, INFO) emits info + error; debug suppressed (Logger.scala:39,:45 vs :33; gdx LOG_INFO)") {
    val records = captureAt(Logger.INFO)(emitAll)
    assertEquals(
      records.size,
      2,
      s"INFO(2) must pass the info + error gates (Logger.scala:39,45) and drop debug (:33). Captured: ${records.map(_.level.name)}."
    )
    assert(
      records.exists(_.level == scribe.Level.Info),
      "INFO level must forward the info record (Logger.scala:39 gate `level >= INFO(2)` true)."
    )
    assert(
      records.exists(_.level == scribe.Level.Error),
      "INFO level must still forward the error record (Logger.scala:45 gate `level >= ERROR(1)` true)."
    )
    assert(
      !records.exists(_.level == scribe.Level.Debug),
      "no DEBUG record may leak at INFO level (Logger.scala:33 gate `level >= DEBUG(3)` must be false for level 2)."
    )
  }

  // ── Logger.DEBUG (3) — gdx LOG_DEBUG: everything. ────────────────────────
  // Logger.scala:33/39/45 gates `>= DEBUG(3)/INFO(2)/ERROR(1)` are all true.
  test("ISS-741: Logger(tag, DEBUG) emits debug + info + error (Logger.scala:33,39,45; gdx LOG_DEBUG)") {
    val records = captureAt(Logger.DEBUG)(emitAll)
    assertEquals(records.size, 3, s"DEBUG(3) must pass all three gates (Logger.scala:33,39,45). Captured: ${records.map(_.level.name)}.")
    assert(
      records.exists(_.level == scribe.Level.Debug),
      "DEBUG level must forward the debug record (Logger.scala:33 gate `level >= DEBUG(3)` true)."
    )
    assert(
      records.exists(_.level == scribe.Level.Info),
      "DEBUG level must forward the info record (Logger.scala:39 gate `level >= INFO(2)` true)."
    )
    assert(
      records.exists(_.level == scribe.Level.Error),
      "DEBUG level must forward the error record (Logger.scala:45 gate `level >= ERROR(1)` true)."
    )
  }
}
