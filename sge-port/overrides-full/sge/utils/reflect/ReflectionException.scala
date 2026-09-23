package sge.utils.reflect

/** INJECTED SCALA (Substitutions.inject) — see [[ClassReflection]] for why the `reflect` package
  *
  * Covenant: full-port Covenant-baseline-spec-pass: 0 Covenant-baseline-loc: 9 Covenant-baseline-methods: ReflectionException,this Covenant-source-reference:
  * com/badlogic/gdx/utils/reflect/ReflectionException.java Covenant-verified: 2026-09-23 is substituted rather than ported.
  */
class ReflectionException(message: String, cause: Throwable) extends Exception(message, cause) {
  def this() = this(null, null)
  def this(message: String) = this(message, null)
  def this(cause:   Throwable) = this(null, cause)
}
