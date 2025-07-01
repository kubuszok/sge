package sge
package utils

opaque type Nullable[A] = A
object Nullable {

  inline def apply[A](a: A): Nullable[A] = a

  inline def empty[A]: Nullable[A] = null.asInstanceOf[Nullable[A]]

  extension [A](a: Nullable[A]) {

    inline def fold[B](onEmpty: => B)(onValue: A => B): B =
      if (a == null) onEmpty else onValue(a)

    inline def foreach(f: A => Unit): Unit =
      if (a != null) f(a)

    inline def isEmpty:   Boolean = a == null
    inline def isDefined: Boolean = a != null

    inline def getOrElse[B >: A](default: => B): B =
      if (a == null) default else a

    inline def orNull: A = a
  }

  private val nonEmptyConversion: Conversion[Any, Nullable[Any]] = new {
    def apply(a: Any): Nullable[Any] = a
  }
  inline given [A]: Conversion[A, Nullable[A]] = nonEmptyConversion.asInstanceOf[Conversion[A, Nullable[A]]]

  private val emptyConversion: Conversion[Null, Nullable[Any]] = new {
    def apply(a: Null): Nullable[Any] = null.asInstanceOf[Nullable[Any]]
  }
  inline given [A]: Conversion[Null, Nullable[A]] = emptyConversion.asInstanceOf[Conversion[Null, Nullable[A]]]
}
