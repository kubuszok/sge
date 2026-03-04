/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/Logger.java
 * Original authors: mzechner, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `Gdx.app` -> `sge.Sge` context parameter
 *   Convention: methods take `(using Sge)` context parameter; constants in companion object
 *   Idiom: split packages
 *   TODO: Java-style getters/setters -- redundant setLevel/getLevel wraps public var level
 *   TODO: named context parameter (implicit/using sge/sde: Sge) → anonymous (using Sge) + Sge() accessor
 *   TODO: evaluate scribe (com.outr %%% "scribe" % "3.17.0") as replacement -- cross-platform, compile-time optimization; see docs/improvements/dependencies.md B2
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package utils

/** Simple logger that uses the {@link Application} logging facilities to output messages. The log level set with {@link Application#setLogLevel(int)} overrides the log level set here.
  * @author
  *   mzechner (original implementation)
  * @author
  *   Nathan Sweet (original implementation)
  */
class Logger(val tag: String, var level: Int = Logger.ERROR) {

  def debug(message: String)(using sde: sge.Sge): Unit =
    if (level >= Logger.DEBUG) sde.application.debug(tag, message)

  def debug(message: String, exception: Exception)(using sde: sge.Sge): Unit =
    if (level >= Logger.DEBUG) sde.application.debug(tag, message, exception)

  def info(message: String)(using sde: sge.Sge): Unit =
    if (level >= Logger.INFO) sde.application.log(tag, message)

  def info(message: String, exception: Exception)(using sde: sge.Sge): Unit =
    if (level >= Logger.INFO) sde.application.log(tag, message, exception)

  def error(message: String)(using sde: sge.Sge): Unit =
    if (level >= Logger.ERROR) sde.application.error(tag, message)

  def error(message: String, exception: Throwable)(using sde: sge.Sge): Unit =
    if (level >= Logger.ERROR) sde.application.error(tag, message, exception)

  /** Sets the log level. {@link #NONE} will mute all log output. {@link #ERROR} will only let error messages through. {@link #INFO} will let all non-debug messages through, and {@link #DEBUG} will
    * let all messages through.
    * @param level
    *   {@link #NONE} , {@link #ERROR} , {@link #INFO} , {@link #DEBUG} .
    */
  def setLevel(level: Int): Unit =
    this.level = level

  def getLevel: Int = level
}

object Logger {
  final val NONE  = 0
  final val ERROR = 1
  final val INFO  = 2
  final val DEBUG = 3
}
