/*
 * Port-written (PROGRESS.md §13.30 step 3, ADJUSTMENTS.tsv): the port keeps java's Application
 * logging surface (log level, ApplicationLogger, log/error/debug), which sge replaced by its Log;
 * sge's applications gain it here, over sge's Log, together with sge's frame-hook host that the
 * port's Application does not extend.
 */
package sge

trait JavaLoggingApplication extends Application with FrameHookHost {
  private var _logLevel: Int                        = Application.LOG_INFO
  private var _applicationLogger: ApplicationLogger = JavaLoggingApplication.LogBacked

  override def logLevel: Int                                        = _logLevel
  override def logLevel_=(logLevel: Int): Unit                      = _logLevel = logLevel
  override def applicationLogger: ApplicationLogger                 = _applicationLogger
  override def applicationLogger_=(logger: ApplicationLogger): Unit = _applicationLogger = logger
  override def log(tag: String, message: String): Unit =
    if (_logLevel >= Application.LOG_INFO) _applicationLogger.log(tag, message)
  override def log(tag: String, message: String, exception: Throwable): Unit =
    if (_logLevel >= Application.LOG_INFO) _applicationLogger.log(tag, message, exception)
  override def error(tag: String, message: String): Unit =
    if (_logLevel >= Application.LOG_ERROR) _applicationLogger.error(tag, message)
  override def error(tag: String, message: String, exception: Throwable): Unit =
    if (_logLevel >= Application.LOG_ERROR) _applicationLogger.error(tag, message, exception)
  override def debug(tag: String, message: String): Unit =
    if (_logLevel >= Application.LOG_DEBUG) _applicationLogger.debug(tag, message)
  override def debug(tag: String, message: String, exception: Throwable): Unit =
    if (_logLevel >= Application.LOG_DEBUG) _applicationLogger.debug(tag, message, exception)
}

object JavaLoggingApplication {
  /** java's ApplicationLogger over sge's Log. */
  object LogBacked extends ApplicationLogger {
    override def log(tag: String, message: String): Unit                       = utils.Log.info(s"[$tag] $message")
    override def log(tag: String, message: String, exception: Throwable): Unit = utils.Log.error(s"[$tag] $message", exception)
    override def error(tag: String, message: String): Unit                     = utils.Log.error(s"[$tag] $message")
    override def error(tag: String, message: String, exception: Throwable): Unit = utils.Log.error(s"[$tag] $message", exception)
    override def debug(tag: String, message: String): Unit                     = utils.Log.debug(s"[$tag] $message")
    override def debug(tag: String, message: String, exception: Throwable): Unit = utils.Log.debug(s"[$tag] $message: $exception")
  }
}
