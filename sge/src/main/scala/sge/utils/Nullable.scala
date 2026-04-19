/*
 * Migration notes:
 *   SGE-original file, no LibGDX counterpart
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 157
 * Covenant-baseline-methods: NestedNone,None,Nullable,apply,cache,contains,empty,emptyConversion,exists,filter,flatMap,flatten,fold,forall,foreach,fromOption,get,getOrElse,isDefined,isEmpty,map,nonEmptyConversion,orElse,orNull
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package utils

/** `Option` alternative for LibGDX that does not allocate memory for `Some` case.
  *
  * Inspired by Kyo's allocation-free `Option` type.
  */
type Nullable[A] = Nullable.Impl[A]

object Nullable {
  opaque type Impl[A] = A | Nullable.NestedNone

  /** TypeTest so that pattern matching `case a: A` inside Nullable's union type works correctly on Scala Native. Without this, Scala Native generates a `Product with Serializable` cast check for the
    * `NestedNone` branch, which fails when `A` is `Class[?]` or other types that aren't `Serializable`.
    *
    * The test is simple: a value is `A` if it's not `NestedNone`.
    */
  private given nullableTypeTest[A]: scala.reflect.TypeTest[A | NestedNone, A] =
    (x: A | NestedNone) =>
      x match {
        case _: NestedNone => scala.None
        case _ => Some(x.asInstanceOf[x.type & A])
      }

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

    /** Force-unwraps the value, throwing NullPointerException if empty. Named `get` to avoid shadowing Scala 3's built-in `.nn` extension on `T | Null`.
      */
    def get: A = maybe match {
      case `None` => throw new NullPointerException("Nullable.get called on empty value")
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

    def orElse(alternative: => Nullable[A]): Nullable[A] = maybe match {
      case `None` => alternative
      case _      => maybe
    }

    def exists(p: A => Boolean): Boolean = maybe match {
      case `None` => false
      case a: A => p(a)
    }

    def forall(p: A => Boolean): Boolean = maybe match {
      case `None` => true
      case a: A => p(a)
    }

    def contains[A1 >: A](elem: A1): Boolean = maybe match {
      case `None` => false
      case a: A => a == elem
    }

    def filter(p: A => Boolean): Nullable[A] = maybe match {
      case `None` => None
      case a: A => if (p(a)) maybe else None
    }
  }
  extension [A](maybe: Nullable[Nullable[A]]) {

    def flatten: Nullable[A] = maybe match {
      case `None`           => None
      case _ @NestedNone(n) => NestedNone(n - 1)
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
      if (value >= 0 && value < cache.length) cache(value)
      else new NestedNone(value)

    private val cache = IArray.from((0 until 10).map(new NestedNone(_)))
  }
}
