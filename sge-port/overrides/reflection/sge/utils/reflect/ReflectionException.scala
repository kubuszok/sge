package sge.utils.reflect

/** INJECTED SCALA (Substitutions.inject) — see [[ClassReflection]] for why the `reflect` package
  * is substituted rather than ported. */
class ReflectionException(message: String, cause: Throwable) extends Exception(message, cause) {
  def this() = this(null, null)
  def this(message: String) = this(message, null)
  def this(cause: Throwable) = this(null, cause)
}
