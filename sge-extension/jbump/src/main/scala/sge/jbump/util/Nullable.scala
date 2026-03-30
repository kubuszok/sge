/*
 * Ported from jbump - https://github.com/tommyettinger/jbump
 * Licensed under the MIT License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package jbump
package util

/** Minimal null-safe wrapper for use within jbump (standalone, no sge core dependency). */
opaque type Nullable[+A] = A | Null

object Nullable {

  val Null: Nullable[Nothing] = null

  def apply[A](a: A): Nullable[A] = a

  extension [A](self: Nullable[A]) {

    def isDefined: Boolean = self != null

    def isEmpty: Boolean = self == null

    def get: A = {
      if (self == null) throw NullPointerException("Nullable.get called on empty value")
      self.asInstanceOf[A]
    }

    def getOrElse(default: => A): A = {
      if (self == null) default
      else self.asInstanceOf[A]
    }

    def fold[B](onEmpty: => B)(onSome: A => B): B = {
      if (self == null) onEmpty
      else onSome(self.asInstanceOf[A])
    }
  }

  given [A]: Conversion[A, Nullable[A]] = (a: A) => a
}
