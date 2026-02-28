/*
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package utils

/** `Option` alternative for LibGDX that does not allocate memory for `Some` case.
  *
  * Insipred by Kyo's allocation-free `Option` type.
  */
type Nullable[A] = Nullable.Impl[A]

@scala.annotation.nowarn("msg=type test")
object Nullable {
  opaque type Impl[A] = A | Nullable.NestedNone

  def apply[A](a: A): Nullable[A] = a match {
    case _ if a == null => None
    case NestedNone(n)  => NestedNone(n + 1)
    case a              => a
  }
  def empty[A]: Nullable[A] = NestedNone(0)

  def fromOption[A](option: Option[A]): Nullable[A] = option.fold(empty[A])(apply)

  extension [A](maybe: Nullable[A]) {

    def map[B](f: A => B): Nullable[B] = maybe match {
      case `None` => None
      case a: A => apply(f(a))
    }

    def flatMap[B](f: A => Nullable[B]): Nullable[B] = map(f).flatten

    def foreach(f: A => Unit): Unit = maybe match {
      case `None` => ()
      case a: A => f(a)
    }

    def fold[B](onEmpty: => B)(onSome: A => B): B = maybe match {
      case `None` => onEmpty
      case a: A => onSome(a)
    }

    def getOrElse(onEmpty: => A): A = maybe match {
      case `None` => onEmpty
      case a: A => a
    }

    /** Unwraps to the raw value or null. NOT actually deprecated — the annotation is used to trigger -Werror with -deprecation, forcing callers to add @nowarn("msg=deprecated") with an explicit
      * comment explaining why orNull is needed (e.g., passing to a Java API that expects null). For all other cases, use fold, foreach, getOrElse, map, isDefined, or isEmpty instead.
      */
    @deprecated("orNull should only be used at Java interop boundaries; use fold/foreach/getOrElse instead", "always")
    def orNull: A = maybe match {
      case `None` => null.asInstanceOf[A]
      case a: A => a
    }

    def isDefined: Boolean = maybe match {
      case `None` => false
      case _      => true
    }

    def isEmpty: Boolean = maybe match {
      case `None` => true
      case _      => false
    }
  }
  extension [A](maybe: Nullable[Nullable[A]]) {

    def flatten: Nullable[A] = maybe match {
      case nn @ NestedNone(n) => NestedNone(n - 1)
      case a: A => a
    }
  }

  /** Implicit conversion from non-null values to `Nullable`. */
  given [A]: Conversion[A, Nullable[A]] = nonEmptyConversion.asInstanceOf[Conversion[A, Nullable[A]]]

  /** Implicit conversion from `null` to `Nullable`. */
  given [A]: Conversion[Null, Nullable[A]] = emptyConversion.asInstanceOf[Conversion[Null, Nullable[A]]]

  private val nonEmptyConversion: Conversion[Any, Nullable[Any]] = new {
    def apply(a: Any): Nullable[Any] = Nullable(a)
  }
  private val emptyConversion: Conversion[Null, Nullable[Any]] = new {
    def apply(a: Null): Nullable[Any] = Nullable.empty
  }

  /** Non-nested `None`. For this value we should use an empty branch, since it cannot model Some(None), Some(Some(None)) and so on.
    */
  private val None = NestedNone(0)

  /** Nested `None`, that traces the amount of nesting. Cached to avoid allocation.
    */
  private case class NestedNone private (value: Int) {

    assert(value >= 0, "None nesting level cannot be negative, got: " + value)
  }
  private object NestedNone {

    def apply(value: Int): NestedNone =
      if (value < cache.length) cache(value)
      else new NestedNone(value)

    private val cache = IArray.from((0 until 10).map(new NestedNone(_)))
  }
}
