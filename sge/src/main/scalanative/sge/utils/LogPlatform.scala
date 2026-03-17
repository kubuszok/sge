/*
 * Scala Native logging backend — delegates to scribe.
 */
package sge
package utils

private[sge] object LogPlatform {
  def info(msg:  => String):               Unit    = scribe.info(msg)
  def warn(msg:  => String):               Unit    = scribe.warn(msg)
  def error(msg: => String):               Unit    = scribe.error(msg)
  def error(msg: => String, t: Throwable): Unit    = scribe.error(msg, t)
  def debug(msg: => String):               Unit    = scribe.debug(msg)
  def trace(msg: => String):               Unit    = scribe.trace(msg)
  def isDebugEnabled:                      Boolean = scribe.includes(scribe.Level.Debug)
}
