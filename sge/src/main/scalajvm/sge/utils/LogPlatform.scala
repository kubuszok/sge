/*
 * JVM logging backend — runtime platform detection.
 *
 * On Android: routes to android.util.Log via cached reflection handles.
 * Scribe JARs are excluded from the Android fat JAR, so the else branch
 * would throw ClassNotFoundException if reached — but isAndroid ensures
 * it never is.
 *
 * On desktop JVM: routes to scribe (present on the classpath).
 */
package sge
package utils

import java.lang.reflect.Method

private[sge] object LogPlatform {

  private val isAndroid: Boolean = try {
    Class.forName("android.app.Activity")
    true
  } catch { case _: ClassNotFoundException => false }

  // ── Android reflection handles (lazy — only initialized on Android) ──

  private lazy val androidLogClass: Class[?] = Class.forName("android.util.Log")

  private lazy val androidI: Method =
    androidLogClass.getMethod("i", classOf[String], classOf[String])
  private lazy val androidW: Method =
    androidLogClass.getMethod("w", classOf[String], classOf[String])
  private lazy val androidE: Method =
    androidLogClass.getMethod("e", classOf[String], classOf[String])
  private lazy val androidET: Method =
    androidLogClass.getMethod("e", classOf[String], classOf[String], classOf[Throwable])
  private lazy val androidD: Method =
    androidLogClass.getMethod("d", classOf[String], classOf[String])
  private lazy val androidV: Method =
    androidLogClass.getMethod("v", classOf[String], classOf[String])

  private val Tag = "SGE"

  // ── Public API ──

  def info(msg: => String): Unit =
    if (isAndroid) { androidI.invoke(null, Tag, msg); () }
    else scribe.info(msg)

  def warn(msg: => String): Unit =
    if (isAndroid) { androidW.invoke(null, Tag, msg); () }
    else scribe.warn(msg)

  def error(msg: => String): Unit =
    if (isAndroid) { androidE.invoke(null, Tag, msg); () }
    else scribe.error(msg)

  def error(msg: => String, t: Throwable): Unit =
    if (isAndroid) { androidET.invoke(null, Tag, msg, t); () }
    else scribe.error(msg, t)

  def debug(msg: => String): Unit =
    if (isAndroid) { androidD.invoke(null, Tag, msg); () }
    else scribe.debug(msg)

  def trace(msg: => String): Unit =
    if (isAndroid) { androidV.invoke(null, Tag, msg); () }
    else scribe.trace(msg)

  def isDebugEnabled: Boolean =
    if (isAndroid) true // android.util.Log.isLoggable checks could be added later
    else scribe.includes(scribe.Level.Debug)
}
